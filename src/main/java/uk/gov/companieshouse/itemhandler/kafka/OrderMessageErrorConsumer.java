package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageErrorConsumer {

    private static AtomicLong errorRecoveryOffset;

    private final KafkaListenerEndpointRegistry registry;
    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final Logger logger;

    @Value("${kafka.topics.order-received-notification-error-group}")
    private String errorGroup;
    @Value("${kafka.topics.order-received-notification-error}")
    private String errorTopic;

    public OrderMessageErrorConsumer(KafkaListenerEndpointRegistry registry,
                                     final OrderProcessorService orderProcessorService,
                                     final OrderProcessResponseHandler orderProcessResponseHandler,
                                     Logger logger) {
        this.registry = registry;
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
        this.logger = logger;
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
    public void processOrderReceived(
            org.springframework.messaging.Message<OrderReceived> message,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.CONSUMER) KafkaConsumer<String, OrderReceived> consumer) throws InterruptedException {

        // Configure recovery offset on first message received after application startup
        configureErrorRecoveryOffset(consumer);

        if (offset <= errorRecoveryOffset.get()) {
            handleMessage(message);
        } else {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logMap.put(errorGroup, errorRecoveryOffset);
            logMap.put(LoggingUtils.TOPIC, errorTopic);
            logger.info("Pausing error consumer as error recovery offset reached.", logMap);
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
        logMessageReceived(message, orderReceivedUri);

        // Process message
        OrderProcessResponse response = orderProcessorService.processOrderReceived(orderReceivedUri);
        // Handle response
        response.getStatus().accept(orderProcessResponseHandler, message);
    }

    protected void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message,
                                      String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        logger.info("'order-received' message received", logMap);
    }

    /**
     * Lazily sets `errorRecoveryOffset` to last topic offset minus 1, before first message received
     * is consumed. This helps the error consumer to stop consuming messages when all messages up to
     * `errorRecoveryOffset` are processed.
     */
    private synchronized void configureErrorRecoveryOffset(KafkaConsumer<String, OrderReceived> consumer) {
        if (!isNull(errorRecoveryOffset)) {
            return;
        }
        // Get the end offsets for the consumers partitions i.e. the last un-committed [non-consumed] offsets
        // Note there should [will] only be one entry as this consumer is consuming from a single partition
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
        Long endOffset = endOffsets.get(new TopicPartition(errorTopic, 0));
        errorRecoveryOffset = new AtomicLong(endOffset - 1);
        logger.info(String.format("Setting Error Consumer Recovery Offset to '%1$d'", errorRecoveryOffset.get()));
    }
}