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
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

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
    private static long ERROR_RECOVERY_OFFSET = 0l;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${uk.gov.companieshouse.item-handler.error-consumer}")
    private boolean errorConsumerEnabled;
    private final SerializerFactory serializerFactory;
    private final OrdersKafkaProducer kafkaProducer;
    private final KafkaListenerEndpointRegistry registry;

    public OrdersKafkaConsumer(SerializerFactory serializerFactory,
                               OrdersKafkaProducer kafkaProducer,
                               KafkaListenerEndpointRegistry registry) {
        this.serializerFactory = serializerFactory;
        this.kafkaProducer = kafkaProducer;
        this.registry = registry;
    }

    /**
     * Main listener/consumer. Calls a `retryable` method to process received message.
     * If the `retryable` processor is unsuccessful with a `retryable` error, after maximum numbers of attempts allowed,
     * the message is published to `-retry` topic for failover processing.
     * @param message
     * @throws SerializationException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP, groupId = ORDER_RECEIVED_GROUP,
                    topics = ORDER_RECEIVED_TOPIC,
                    autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}")
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message)
            throws SerializationException, ExecutionException, InterruptedException {
        handleMessage(message);
    }

    /**
     * Retry (`-retry`) listener/consumer. Calls a `retryable` method to process received message.
     * If the `retryable` processor is unsuccessful with a `retryable` error, after maximum numbers of attempts allowed,
     * the message is published to `-error` topic for failover processing.
     * @param message
     * @throws SerializationException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP_RETRY, groupId = ORDER_RECEIVED_GROUP_RETRY,
                    topics = ORDER_RECEIVED_TOPIC_RETRY,
                    autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}")
    public void processOrderReceivedRetry(org.springframework.messaging.Message<OrderReceived> message)
            throws InterruptedException, ExecutionException, SerializationException {
        handleMessage(message);
    }

    /**
     * Error (`-error`) topic listener/consumer is enabled when the application is launched in error
     * mode (IS_ERROR_QUEUE_CONSUMER=true). Receives messages up to `ERROR_RECOVERY_OFFSET` offset. Calls a `retryable`
     * method to process received message. If the `retryable` processor is unsuccessful with a `retryable` error, after
     * maximum numbers of attempts allowed, the message is republished to `-retry` topic for failover processing.
     * This listener stops accepting messages when the topic's offset reaches `ERROR_RECOVERY_OFFSET`.
     * @param message
     * @throws SerializationException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP_ERROR, groupId = ORDER_RECEIVED_GROUP_ERROR,
                    topics = ORDER_RECEIVED_TOPIC_ERROR,
                    autoStartup = "${uk.gov.companieshouse.item-handler.error-consumer}")
    public void processOrderReceivedError(org.springframework.messaging.Message<OrderReceived> message)
            throws InterruptedException, ExecutionException, SerializationException {
        long offset = Long.parseLong("" + message.getHeaders().get("kafka_offset"));
        if (offset <= ERROR_RECOVERY_OFFSET) {
            handleMessage(message);
        }
        else {
            LOGGER.info(String.format("Pausing error consumer \"%1$s\" as error recovery offset '%2$d' reached.",
                    ORDER_RECEIVED_GROUP_ERROR, ERROR_RECOVERY_OFFSET));
            registry.getListenerContainer(ORDER_RECEIVED_GROUP_ERROR).pause();
        }
    }

    /**
     * Handles processing of received message.
     * @param message
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws SerializationException
     */
    protected void handleMessage(org.springframework.messaging.Message<OrderReceived> message)
            throws InterruptedException, ExecutionException, SerializationException {
        OrderReceived msg = message.getPayload();
        String orderReceivedUri = msg.getOrderUri();
        MessageHeaders headers = message.getHeaders();
        String receivedTopic = headers.get(KafkaHeaders.RECEIVED_TOPIC).toString();
        try {
            logMessageReceived(message);

            // process message

            logMessageProcessed(message);
        } catch (RetryableErrorException ex){
            String nextTopic = (receivedTopic.equals(ORDER_RECEIVED_TOPIC)
                                    || receivedTopic.equals(ORDER_RECEIVED_TOPIC_ERROR))
                                            ? ORDER_RECEIVED_TOPIC_RETRY : ORDER_RECEIVED_TOPIC_ERROR;
            logMessageProcessingFailureRecoverable(message, nextTopic, ex);
            republishMessageToTopic(orderReceivedUri, receivedTopic, nextTopic);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, x);
        }
    }

    protected void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message){
        LOGGER.info(String.format("'order-received' message received \"%1$s\".",
                getMessageHeadersAsMap(message).toString()));
    }

    protected void logMessageProcessingFailureRecoverable(org.springframework.messaging.Message<OrderReceived> message,
                                                        String nextTopic, Exception exception) {
        OrderReceived msg = message.getPayload();
        Map<String, String> dataMap = getMessageHeadersAsMap(message);
        dataMap.put("next_topic", nextTopic);
        dataMap.put("stack_trace", Arrays.toString(exception.getStackTrace()));
        LOGGER.error(
                String.format("'order-received' message processing failed with a recoverable exception. \n%1$s",
                        dataMap.toString())
        );
    }

    protected void logMessageProcessingFailureNonRecoverable(org.springframework.messaging.Message<OrderReceived> message,
                                                           Exception exception) {
        OrderReceived msg = message.getPayload();
        Map<String, String> dataMap = getMessageHeadersAsMap(message);
        dataMap.put("stack_trace", Arrays.toString(exception.getStackTrace()));
        LOGGER.error(
                String.format("order-received message processing failed with a non-recoverable exception. \n%1$s",
                        dataMap.toString())
        );
    }

    private void logMessageProcessed(org.springframework.messaging.Message<OrderReceived> message){
        LOGGER.info(String.format("Order received message successfully processed. %1$s",
                getMessageHeadersAsMap(message).toString()));
    }

    private Map<String, String> getMessageHeadersAsMap(org.springframework.messaging.Message<OrderReceived> message){
        Map<String, String> dataMap = new HashMap<>();
        MessageHeaders messageHeaders = message.getHeaders();
        dataMap.put("data.key", "" + messageHeaders.get(KafkaHeaders.RECEIVED_MESSAGE_KEY));
        dataMap.put("data.topic", "" + messageHeaders.get(KafkaHeaders.RECEIVED_TOPIC));
        dataMap.put("data.offset", "" + messageHeaders.get(KafkaHeaders.OFFSET));
        dataMap.put("data.partition", "" + messageHeaders.get(KafkaHeaders.RECEIVED_PARTITION_ID));

        return dataMap;
    }

    protected void republishMessageToTopic(String orderUri, String currentTopic, String nextTopic)
            throws SerializationException, ExecutionException, InterruptedException {
        LOGGER.info(String.format("Republishing message: \"%1$s\" received from topic: \"%2$s\" to topic: \"%3$s\"",
                orderUri, currentTopic, nextTopic));
        Message message = createRetryMessage(orderUri, nextTopic);
        kafkaProducer.sendMessage(message);
    }

    protected Message createRetryMessage(String orderUri, String topic) throws SerializationException {
        final Message message = new Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey(ORDER_RECEIVED_KEY_RETRY);
        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }

    private static void updateErrorRecoveryOffset(long offset){
        ERROR_RECOVERY_OFFSET = offset;
    }

    /**
     * Sets ERROR_RECOVERY_OFFSET to latest topic offset (error topic) minus 1, before error consumer starts. This
     * helps the error consumer to stop consuming messages when all messages up to ERROR_RECOVERY_OFFSET are processed.
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
                            consumerSeekCallback.seek(topic.topic(), topic.partition(), ERROR_RECOVERY_OFFSET);
                            LOGGER.info(String.format("Setting Error Consumer Recovery Offset to '%1$d'", ERROR_RECOVERY_OFFSET));
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

    private Map errorConsumerConfigs() {
        Map props = new HashMap();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, ORDER_RECEIVED_GROUP_ERROR);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return props;
    }
}
