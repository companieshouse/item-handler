package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.RetryableErrorException;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;

@Service
public class OrdersKafkaConsumer implements ConsumerSeekAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_KEY_RETRY = ORDER_RECEIVED_TOPIC_RETRY;
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_GROUP = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC;
    private static final String ORDER_RECEIVED_GROUP_RETRY = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC_RETRY;
    private static final String ORDER_RECEIVED_GROUP_ERROR = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC_ERROR;
    private static long errorRecoveryOffset = 0L;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${uk.gov.companieshouse.item-handler.error-consumer}")
    private boolean errorConsumerEnabled;
    private final SerializerFactory serializerFactory;
    private final OrdersKafkaProducer kafkaProducer;
    private final KafkaListenerEndpointRegistry registry;
    private final Map<String, Integer> retryCount;
    private final OrderProcessorService processor;

    public OrdersKafkaConsumer(SerializerFactory serializerFactory,
                               OrdersKafkaProducer kafkaProducer,
                               KafkaListenerEndpointRegistry registry,
                               final OrderProcessorService processor) {
        this.serializerFactory = serializerFactory;
        this.kafkaProducer = kafkaProducer;
        this.registry = registry;
        this.retryCount = new HashMap<>();
        this.processor = processor;
    }

    /**
     * Main listener/consumer. Calls `handleMessage` method to process received message.
     * @param message
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP, groupId = ORDER_RECEIVED_GROUP,
                    topics = ORDER_RECEIVED_TOPIC,
                    autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}",
                    containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message) {
        handleMessage(message);
    }

    /**
     * Retry (`-retry`) listener/consumer. Calls `handleMessage` method to process received message.
     * @param message
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP_RETRY, groupId = ORDER_RECEIVED_GROUP_RETRY,
                    topics = ORDER_RECEIVED_TOPIC_RETRY,
                    autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}",
                    containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceivedRetry(org.springframework.messaging.Message<OrderReceived> message) {
        handleMessage(message);
    }

    /**
     * Error (`-error`) topic listener/consumer is enabled when the application is launched in error
     * mode (IS_ERROR_QUEUE_CONSUMER=true). Receives messages up to `errorRecoveryOffset` offset. Calls `handleMessage`
     * method to process received message. If the `retryable` processor is unsuccessful with a `retryable` error, after
     * maximum numbers of attempts allowed, the message is republished to `-retry` topic for failover processing.
     * This listener stops accepting messages when the topic's offset reaches `errorRecoveryOffset`.
     * @param message
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP_ERROR, groupId = ORDER_RECEIVED_GROUP_ERROR,
                    topics = ORDER_RECEIVED_TOPIC_ERROR,
                    autoStartup = "${uk.gov.companieshouse.item-handler.error-consumer}",
                    containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceivedError(org.springframework.messaging.Message<OrderReceived> message) {
        long offset = Long.parseLong("" + message.getHeaders().get("kafka_offset"));
        if (offset <= errorRecoveryOffset) {
            handleMessage(message);
        }
        else {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logMap.put(LoggingUtils.ORDER_RECEIVED_GROUP_ERROR, errorRecoveryOffset);
            logMap.put(LoggingUtils.TOPIC, ORDER_RECEIVED_TOPIC_ERROR);
            LOGGER.info("Pausing error consumer as error recovery offset reached.", logMap);
            registry.getListenerContainer(ORDER_RECEIVED_GROUP_ERROR).pause();
        }
    }

    /**
     * Handles processing of received message.
     * @param message
     */
    protected void handleMessage(org.springframework.messaging.Message<OrderReceived> message) {
        OrderReceived msg = message.getPayload();
        String orderReceivedUri = msg.getOrderUri();
        MessageHeaders headers = message.getHeaders();
        String receivedTopic = headers.get(KafkaHeaders.RECEIVED_TOPIC).toString();
        try {
            logMessageReceived(message, orderReceivedUri);

            // process message
            processor.processOrderReceived(orderReceivedUri);

            // on successful processing remove counterKey from retryCount
            if (retryCount.containsKey(orderReceivedUri)) {
                resetRetryCount(receivedTopic + "-" + orderReceivedUri);
            }
            logMessageProcessed(message, orderReceivedUri);
        } catch (RetryableErrorException ex){
            retryMessage(message, orderReceivedUri, receivedTopic, ex);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, x);
        }
    }

    /**
     * Retries a message that failed processing with a `RetryableErrorException`.
     * Checks which topic the message was received from and whether any retry attempts remain.
     * The message is published to the next topic for failover processing, if retries match or exceed `MAX_RETRY_ATTEMPTS`.
     * @param message
     * @param orderReceivedUri
     * @param receivedTopic
     * @param ex
     */
    private void retryMessage(org.springframework.messaging.Message<OrderReceived> message,
                              String orderReceivedUri, String receivedTopic, RetryableErrorException ex) {
        String nextTopic = (receivedTopic.equals(ORDER_RECEIVED_TOPIC)
                || receivedTopic.equals(ORDER_RECEIVED_TOPIC_ERROR))
                ? ORDER_RECEIVED_TOPIC_RETRY : ORDER_RECEIVED_TOPIC_ERROR;
        String counterKey = receivedTopic + "-" + orderReceivedUri;

        if (receivedTopic.equals(ORDER_RECEIVED_TOPIC)
                || retryCount.getOrDefault(counterKey, 1) >= MAX_RETRY_ATTEMPTS) {
            republishMessageToTopic(orderReceivedUri, receivedTopic, nextTopic);
            if (!receivedTopic.equals(ORDER_RECEIVED_TOPIC)) {
                resetRetryCount(counterKey);
            }
        }
        else {
            retryCount.put(counterKey, retryCount.getOrDefault(counterKey, 1) + 1);
            logMessageProcessingFailureRecoverable(message, retryCount.get(counterKey), ex);
            // retry
            handleMessage(message);
        }
    }

    /**
     * Resets retryCount for message identified by key `counterKey`
     * @param counterKey
     */
    private void resetRetryCount(String counterKey) {
        retryCount.remove(counterKey);
    }

    protected void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message, String orderUri){
        Map<String, Object> logMap = getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LOGGER.info("'order-received' message received", logMap);
    }

    protected void logMessageProcessingFailureRecoverable(org.springframework.messaging.Message<OrderReceived> message,
                                                        int attempt, Exception exception) {
        Map<String, Object> logMap = getMessageHeadersAsMap(message);
        logMap.put(LoggingUtils.RETRY_ATTEMPT, attempt);
        logMap.put(LoggingUtils.EXCEPTION, exception);
        LOGGER.error("'order-received' message processing failed with a recoverable exception", logMap);
    }

    protected void logMessageProcessingFailureNonRecoverable(org.springframework.messaging.Message<OrderReceived> message,
                                                           Exception exception) {
        Map<String, Object> logMap = getMessageHeadersAsMap(message);
        logMap.put(LoggingUtils.EXCEPTION, exception);
        LOGGER.error("order-received message processing failed with a non-recoverable exception", logMap);
    }

    private void logMessageProcessed(org.springframework.messaging.Message<OrderReceived> message, String orderUri){
        Map<String, Object> logMap = getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LOGGER.info("Order received message successfully processed", logMap);
    }

    private Map<String, Object> getMessageHeadersAsMap(org.springframework.messaging.Message<OrderReceived> message){
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        MessageHeaders messageHeaders = message.getHeaders();

        LoggingUtils.logIfNotNull(logMap, LoggingUtils.KEY,  messageHeaders.get(KafkaHeaders.RECEIVED_MESSAGE_KEY));
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, messageHeaders.get(KafkaHeaders.RECEIVED_TOPIC));
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.OFFSET, messageHeaders.get(KafkaHeaders.OFFSET));
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.PARTITION, messageHeaders.get(KafkaHeaders.RECEIVED_PARTITION_ID));

        return logMap;
    }

    protected void republishMessageToTopic(String orderUri, String currentTopic, String nextTopic) {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.CURRENT_TOPIC, currentTopic);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.NEXT_TOPIC, nextTopic);
        LOGGER.info(String.format("Republishing message: \"%1$s\" received from topic: \"%2$s\" to topic: \"%3$s\"",
                orderUri, currentTopic, nextTopic), logMap);
        try {
            kafkaProducer.sendMessage(createRetryMessage(orderUri, nextTopic));
        } catch (ExecutionException | InterruptedException e) {
            logMap.put(LoggingUtils.EXCEPTION, e);
            LOGGER.error(String.format("Error sending message: \"%1$s\" to topic: \"%2$s\"",
                    orderUri, nextTopic), logMap);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected Message createRetryMessage(String orderUri, String topic) {
        final Message message = new Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey(ORDER_RECEIVED_KEY_RETRY);
        try {
            message.setValue(serializer.toBinary(orderReceived));
        } catch (SerializationException e) {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.MESSAGE, orderUri);
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, topic);
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.OFFSET, message.getOffset());
            logMap.put(LoggingUtils.EXCEPTION, e);
            LOGGER.error(String.format("Error serializing message: \"%1$s\" for topic: \"%2$s\"",
                    orderUri, topic), logMap);
        }
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }

    private static void updateErrorRecoveryOffset(long offset){
        errorRecoveryOffset = offset;
    }

    private Map<String, Object> errorConsumerConfigs() {
        Map<String, Object> props = new HashMap();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderReceivedDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, ORDER_RECEIVED_GROUP_ERROR);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return props;
    }

    /**
     * Sets `errorRecoveryOffset` to latest topic offset (error topic) minus 1, before error consumer starts. This
     * helps the error consumer to stop consuming messages when all messages up to `errorRecoveryOffset` are processed.
     * @param map map of topics and partitions
     * @param consumerSeekCallback callback that allows a consumers offset position to be moved.
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> map, ConsumerSeekCallback consumerSeekCallback) {
        if (errorConsumerEnabled) {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(errorConsumerConfigs())) {
                final Map<TopicPartition, Long> topicPartitionsMap = consumer.endOffsets(map.keySet());
                map.forEach(
                        (topic, action) ->
                        {
                            updateErrorRecoveryOffset(topicPartitionsMap.get(topic) - 1);
                            LOGGER.info(String.format("Setting Error Consumer Recovery Offset to '%1$d'", errorRecoveryOffset));
                        }
                );
            }
        }
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback consumerSeekCallback) {
        // Do nothing as not required for this implementation
    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> map, ConsumerSeekCallback consumerSeekCallback) {
        // Do nothing as not required for this implementation
    }
}
