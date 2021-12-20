package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
import uk.gov.companieshouse.itemhandler.exception.RetryableException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageConsumer implements ConsumerSeekAware {

    private static long errorRecoveryOffset = 0L;

    private static CountDownLatch eventLatch = new CountDownLatch(0);

    public static void setEventLatch(CountDownLatch eventLatch) {
        OrderMessageConsumer.eventLatch = eventLatch;
    }

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${uk.gov.companieshouse.item-handler.error-consumer}")
    private boolean errorConsumerEnabled;
    @Value("kafka.topics.order-received-notification-error-group")
    private String errorGroup;
    @Value("kafka.topics.order-received-notification-error")
    private String errorTopic;

    private final SerializerFactory serializerFactory;
    private final KafkaListenerEndpointRegistry registry;
    private final Map<String, Integer> retryCount;
    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final Map<String, Object> consumerConfigsError;

    public OrderMessageConsumer(SerializerFactory serializerFactory, KafkaListenerEndpointRegistry registry,
                                final OrderProcessorService orderProcessorService,
                                final OrderProcessResponseHandler orderProcessResponseHandler,
                                Map<String, Object> consumerConfigsError) {
        this.serializerFactory = serializerFactory;
        this.registry = registry;
        this.retryCount = new HashMap<>();
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
        this.consumerConfigsError = consumerConfigsError;
    }

    /**
     * Main listener/consumer. Calls `handleMessage` method to process received message.
     * 
     * @param message
     */
    @KafkaListener(id = "#{'${kafka.topics.order-received_group}'}",
            groupId = "#{'${kafka.topics.order-received_group}'}",
            topics = "#{'${kafka.topics.order-received}'}",
            autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message) {
        handleMessage(message);
    }

    /**
     * Retry (`-retry`) listener/consumer. Calls `handleMessage` method to process received message.
     * 
     * @param message
     */
    @KafkaListener(id = "#{'${kafka.topics.order-received-notification-retry-group}'}",
            groupId = "#{'${kafka.topics.order-received-notification-retry-group}'}",
            topics = "#{'${kafka.topics.order-received-notification-retry}'}",
            autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceivedRetry(
            org.springframework.messaging.Message<OrderReceived> message) {
        handleMessage(message);
    }

    /**
     * Error (`-error`) topic listener/consumer is enabled when the application is launched in error
     * mode (IS_ERROR_QUEUE_CONSUMER=true). Receives messages up to `errorRecoveryOffset` offset.
     * Calls `handleMessage` method to process received message. If the `retryable` processor is
     * unsuccessful with a `retryable` error, after maximum numbers of attempts allowed, the message
     * is republished to `-retry` topic for failover processing. This listener stops accepting
     * messages when the topic's offset reaches `errorRecoveryOffset`.
     * 
     * @param message
     */
    @KafkaListener(id = "#{'${kafka.topics.order-received-notification-error-group}'}",
            groupId = "#{'${kafka.topics.order-received-notification-error-group}'}",
            topics = "#{'${kafka.topics.order-received-notification-error}'}",
            autoStartup = "#{${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceivedError(
            org.springframework.messaging.Message<OrderReceived> message) {
        long offset = Long.parseLong("" + message.getHeaders().get("kafka_offset"));
        if (offset <= errorRecoveryOffset) {
            handleMessage(message);
        } else {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logMap.put(errorGroup, errorRecoveryOffset);
            logMap.put(LoggingUtils.TOPIC, errorTopic);
            LoggingUtils.getLogger().info("Pausing error consumer as error recovery offset reached.",
                    logMap);
            registry.getListenerContainer(errorGroup).pause();
        }
    }

    /**
     * Handles processing of received message.
     * 
     * @param message
     */
    protected void handleMessage(org.springframework.messaging.Message<OrderReceived> message) {
        OrderReceived orderReceived = message.getPayload();
        String orderReceivedUri = orderReceived.getOrderUri();
        MessageHeaders headers = message.getHeaders();
        String receivedTopic = headers.get(KafkaHeaders.RECEIVED_TOPIC).toString();
        logMessageReceived(message, orderReceivedUri);

        // Process message
        OrderProcessResponse response = orderProcessorService.processOrderReceived(orderReceivedUri);
        // Handle response
        response.getStatus().accept(orderProcessResponseHandler, message);
        // Trigger countdown latch
        eventLatch.countDown();
    }

    /**
     * Retries a message that failed processing with a `RetryableException`. Checks which topic
     * the message was received from and whether any retry attempts remain. The message is published
     * to the next topic for failover processing, if retries match or exceed `MAX_RETRY_ATTEMPTS`.
     * 
     * @param message
     * @param orderReceivedUri
     * @param receivedTopic
     * @param ex
     */
    private void retryMessage(org.springframework.messaging.Message<OrderReceived> message,
            String orderReceivedUri, String receivedTopic, RetryableException ex) {
//        String nextTopic = (receivedTopic.equals(ORDER_RECEIVED_TOPIC)
//                || receivedTopic.equals(ORDER_RECEIVED_TOPIC_ERROR)) ? ORDER_RECEIVED_TOPIC_RETRY
//                        : ORDER_RECEIVED_TOPIC_ERROR;
//        String counterKey = receivedTopic + "-" + orderReceivedUri;
//
//        if (receivedTopic.equals(ORDER_RECEIVED_TOPIC)
//                || retryCount.getOrDefault(counterKey, 1) >= MAX_RETRY_ATTEMPTS) {
//            republishMessageToTopic(orderReceivedUri, receivedTopic, nextTopic);
//            if (!receivedTopic.equals(ORDER_RECEIVED_TOPIC)) {
//                resetRetryCount(counterKey);
//            }
//        } else {
//            retryCount.put(counterKey, retryCount.getOrDefault(counterKey, 1) + 1);
//            logMessageProcessingFailureRecoverable(message, retryCount.get(counterKey), ex);
//            // retry
//            handleMessage(message);
//        }
    }

    /**
     * Resets retryCount for message identified by key `counterKey`
     * 
     * @param counterKey
     */
    private void resetRetryCount(String counterKey) {
        retryCount.remove(counterKey);
    }

    protected void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message,
            String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.getLogger().info("'order-received' message received", logMap);
    }

    protected void logMessageProcessingFailureRecoverable(
            org.springframework.messaging.Message<OrderReceived> message, int attempt,
            Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        logMap.put(LoggingUtils.RETRY_ATTEMPT, attempt);
        LoggingUtils.getLogger().error("'order-received' message processing failed with a recoverable exception",
                exception, logMap);
    }

    protected void logMessageProcessingFailureNonRecoverable(
            org.springframework.messaging.Message<OrderReceived> message, Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.getLogger().error("order-received message processing failed with a non-recoverable exception",
                exception, logMap);
    }

    private void logMessageProcessed(org.springframework.messaging.Message<OrderReceived> message,
            String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.getLogger().info("Order received message processing completed", logMap);
    }

//    protected void republishMessageToTopic(String orderUri, String currentTopic, String nextTopic) {
//        Map<String, Object> logMap = LoggingUtils.createLogMap();
//        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
//        LoggingUtils.logIfNotNull(logMap, LoggingUtils.CURRENT_TOPIC, currentTopic);
//        LoggingUtils.logIfNotNull(logMap, LoggingUtils.NEXT_TOPIC, nextTopic);
//        LoggingUtils.getLogger().info(String.format(
//                "Republishing message: \"%1$s\" received from topic: \"%2$s\" to topic: \"%3$s\"",
//                orderUri, currentTopic, nextTopic), logMap);
//        try {
//            kafkaProducer.sendMessage(createRetryMessage(orderUri, nextTopic));
//        } catch (ExecutionException | InterruptedException e) {
//            LoggingUtils.getLogger().error(String.format("Error sending message: \"%1$s\" to topic: \"%2$s\"",
//                    orderUri, nextTopic), e, logMap);
//            if (e instanceof InterruptedException) {
//                Thread.currentThread().interrupt();
//            }
//        }
//    }

//    protected Message createRetryMessage(String orderUri, String topic) {
//        final Message message = new Message();
//        AvroSerializer serializer =
//                serializerFactory.getGenericRecordSerializer(OrderReceived.class);
//        OrderReceived orderReceived = new OrderReceived();
//        orderReceived.setOrderUri(orderUri.trim());
//
//        message.setKey(ORDER_RECEIVED_KEY_RETRY);
//        try {
//            message.setValue(serializer.toBinary(orderReceived));
//        } catch (SerializationException e) {
//            Map<String, Object> logMap = LoggingUtils.createLogMap();
//            LoggingUtils.logIfNotNull(logMap, LoggingUtils.MESSAGE, orderUri);
//            LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, topic);
//            LoggingUtils.logIfNotNull(logMap, LoggingUtils.OFFSET, message.getOffset());
//            LoggingUtils.getLogger().error(String.format("Error serializing message: \"%1$s\" for topic: \"%2$s\"",
//                    orderUri, topic), e, logMap);
//            throw new ApplicationSerialisationException("Failed to serialise message");
//        }
//        message.setTopic(topic);
//        message.setTimestamp(new Date().getTime());
//
//        return message;
//    }

    private static void updateErrorRecoveryOffset(long offset) {
        errorRecoveryOffset = offset;
    }

    /**
     * Sets `errorRecoveryOffset` to latest topic offset (error topic) minus 1, before error
     * consumer starts. This helps the error consumer to stop consuming messages when all messages
     * up to `errorRecoveryOffset` are processed.
     * 
     * @param map map of topics and partitions
     * @param consumerSeekCallback callback that allows a consumers offset position to be moved.
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> map,
            ConsumerSeekCallback consumerSeekCallback) {
        if (errorConsumerEnabled) {
            try (KafkaConsumer<String, String> consumer =
                    new KafkaConsumer<>(consumerConfigsError)) {
                final Map<TopicPartition, Long> topicPartitionsMap =
                        consumer.endOffsets(map.keySet());
                map.forEach((topic, action) -> {
                    updateErrorRecoveryOffset(topicPartitionsMap.get(topic) - 1);
                    LoggingUtils.getLogger()
                            .info(String.format("Setting Error Consumer Recovery Offset to '%1$d'",
                                    errorRecoveryOffset));
                });
            }
        }
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback consumerSeekCallback) {
        // Do nothing as not required for this implementation
    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> map,
            ConsumerSeekCallback consumerSeekCallback) {
        // Do nothing as not required for this implementation
    }
}
