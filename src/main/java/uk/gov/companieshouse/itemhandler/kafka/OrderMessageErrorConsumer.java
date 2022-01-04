package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageErrorConsumer {

    private static final AtomicReference<Long> errorRecoveryOffset = new AtomicReference<>(0L);
    private static CountDownLatch prePostConstructLatch;
    private static CountDownLatch postConstructLatch;

    private final KafkaListenerEndpointRegistry registry;
    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final Map<String, Object> consumerConfigsError;
    private CountDownLatch preOrderReceivedEventLatch;
    private CountDownLatch postOrderReceivedEventLatch;
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

    public static CountDownLatch getPrePostConstructLatch() {
        return prePostConstructLatch;
    }

    public static void setPrePostConstructLatch(CountDownLatch prePostConstructLatch) {
        OrderMessageErrorConsumer.prePostConstructLatch = prePostConstructLatch;
    }

    public static CountDownLatch getPostConstructLatch() {
        return postConstructLatch;
    }

    public static void setPostConstructLatch(CountDownLatch postConstructLatch) {
        OrderMessageErrorConsumer.postConstructLatch = postConstructLatch;
    }

    public CountDownLatch getPreOrderReceivedEventLatch() {
        return preOrderReceivedEventLatch;
    }

    public void setPreOrderReceivedEventLatch(CountDownLatch preOrderReceivedEventLatch) {
        this.preOrderReceivedEventLatch = preOrderReceivedEventLatch;
    }

    public CountDownLatch getPostOrderReceivedEventLatch() {
        return postOrderReceivedEventLatch;
    }

    public void setPostOrderReceivedEventLatch(CountDownLatch postOrderReceivedEventLatch) {
        this.postOrderReceivedEventLatch = postOrderReceivedEventLatch;
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
            @Header(KafkaHeaders.OFFSET) Long offset) throws InterruptedException {

        if (!isNull(preOrderReceivedEventLatch)) {
            preOrderReceivedEventLatch.await(30, TimeUnit.SECONDS);
        }

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
        if (!isNull(postOrderReceivedEventLatch)) {
            postOrderReceivedEventLatch.countDown();
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
     */
    @PostConstruct
    public void postConstruct() throws InterruptedException {
        if (errorConsumerEnabled) {
            if (!isNull(prePostConstructLatch)) {
                prePostConstructLatch.await(30, TimeUnit.SECONDS);
            }
            try (KafkaConsumer<String, String> consumer =
                         new KafkaConsumer<>(consumerConfigsError)) {
                TopicPartition topicPartition = new TopicPartition(errorTopic, 0);
                consumer.assign(Collections.singleton(topicPartition));
                setErrorRecoveryOffset(consumer.position(topicPartition) - 1);
                LoggingUtils.getLogger()
                            .info(String.format(
                                    "Setting Error Consumer Recovery Offset to '%1$d'",
                                    errorRecoveryOffset.get()));
            }
            if (!isNull(postConstructLatch)) {
                postConstructLatch.countDown();
            }
        }
    }
}