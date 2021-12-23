package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageErrorConsumer implements ConsumerSeekAware {

    private static final AtomicReference<ConsumerSeekCallback> consumerSeekCallback = new AtomicReference<>();
    private static final AtomicReference<Long> errorRecoveryOffset = new AtomicReference<>(0L);

    private final KafkaListenerEndpointRegistry registry;
    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final Map<String, Object> consumerConfigsError;
    private CountDownLatch eventLatch;
    private CountDownLatch startupLatch;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${uk.gov.companieshouse.item-handler.error-consumer}")
    private boolean errorConsumerEnabled;
    @Value("kafka.topics.order-received-notification-error-group")
    private String errorGroup;
    @Value("kafka.topics.order-received-notification-error")
    private String errorTopic;

    public OrderMessageErrorConsumer(KafkaListenerEndpointRegistry registry,
                                     final OrderProcessorService orderProcessorService,
                                     final OrderProcessResponseHandler orderProcessResponseHandler,
                                     Supplier<Map<String, Object>> consumerConfigsErrorSupplier) {
        this.registry = registry;
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
        this.consumerConfigsError = consumerConfigsErrorSupplier.get();
    }

    public CountDownLatch getEventLatch() {
        return eventLatch;
    }

    public CountDownLatch getStartupLatch() {
        return startupLatch;
    }

    public void setEventLatch(CountDownLatch eventLatch) {
        this.eventLatch = eventLatch;
    }

    public void setStartupLatch(CountDownLatch startupLatch) {
        this.startupLatch = startupLatch;
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
            org.springframework.messaging.Message<OrderReceived> message,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        if (offset <= errorRecoveryOffset.get()) {
            handleMessage(message);
        } else {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logMap.put(errorGroup, errorRecoveryOffset);
            logMap.put(LoggingUtils.TOPIC, errorTopic);
            LoggingUtils.getLogger()
                    .info("Pausing error consumer as error recovery offset reached.",
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
        // Notify event latch
        if (!isNull(eventLatch)) {
            eventLatch.countDown();
        }
    }

    protected void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message,
                                      String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.getLogger().info("'order-received' message received", logMap);
    }

    // TODO: make sure logging behaviour is captured
    protected void logMessageProcessingFailureRecoverable(
            org.springframework.messaging.Message<OrderReceived> message, int attempt,
            Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        logMap.put(LoggingUtils.RETRY_ATTEMPT, attempt);
        LoggingUtils.getLogger()
                .error("'order-received' message processing failed with a recoverable exception",
                        exception, logMap);
    }

    protected void logMessageProcessingFailureNonRecoverable(
            org.springframework.messaging.Message<OrderReceived> message, Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.getLogger()
                .error("order-received message processing failed with a non-recoverable exception",
                        exception, logMap);
    }

    private void logMessageProcessed(org.springframework.messaging.Message<OrderReceived> message,
                                     String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.getLogger().info("Order received message processing completed", logMap);
    }

    private void setErrorRecoveryOffset(long offset) {
        errorRecoveryOffset.set(offset);
    }

    /**
     * Sets `errorRecoveryOffset` to latest topic offset (error topic) minus 1, before error
     * consumer starts. This helps the error consumer to stop consuming messages when all messages
     * up to `errorRecoveryOffset` are processed.
     *
     * @param map                  map of topics and partitions
     * @param consumerSeekCallback callback that allows a consumers offset position to be moved.
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> map,
                                     ConsumerSeekCallback consumerSeekCallback) {
        if (errorConsumerEnabled) {
            if (!isNull(startupLatch)) {
                try {
                    startupLatch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try (KafkaConsumer<String, String> consumer =
                         new KafkaConsumer<>(consumerConfigsError)) {
                final Map<TopicPartition, Long> topicPartitionsMap =
                        consumer.endOffsets(map.keySet());
                map.forEach((topic, action) -> {
                    long offset = topicPartitionsMap.get(topic);
                    if (offset > 0) {
                        setErrorRecoveryOffset(offset - 1);
                    }
                    LoggingUtils.getLogger()
                            .info(String.format(
                                    "Setting Error Consumer Recovery Offset to '%1$d'",
                                    errorRecoveryOffset));
                });
            }
        }
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback consumerSeekCallback) {
        OrderMessageErrorConsumer.consumerSeekCallback.set(consumerSeekCallback);
    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> map,
                                ConsumerSeekCallback consumerSeekCallback) {
        // Do nothing as not required for this implementation
    }

    public ConsumerSeekCallback getConsumerSeekCallback() {
        return consumerSeekCallback.get();
    }
}