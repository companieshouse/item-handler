package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageErrorConsumer {

    private final PartitionOffset errorRecoveryOffset;

    private final KafkaListenerEndpointRegistry registry;
    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final Logger logger;

    @Value("${kafka.topics.order-received-error-group}")
    private String errorGroup;
    @Value("${kafka.topics.order-received-error}")
    private String errorTopic;

    public OrderMessageErrorConsumer(KafkaListenerEndpointRegistry registry,
                                     final OrderProcessorService orderProcessorService,
                                     final OrderProcessResponseHandler orderProcessResponseHandler,
                                     Logger logger,
                                     PartitionOffset errorRecoveryOffset) {
        this.registry = registry;
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
        this.logger = logger;
        this.errorRecoveryOffset = errorRecoveryOffset;
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
    @KafkaListener(id = "#{'${kafka.topics.order-received-error-group}'}",
            groupId = "#{'${kafka.topics.order-received-error-group}'}",
            topics = "#{'${kafka.topics.order-received-error}'}",
            autoStartup = "#{${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceived(
            Message<OrderReceived> message,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.CONSUMER) KafkaConsumer<String, OrderReceived> consumer) {

        // Configure recovery offset on first message received after application startup
        configureErrorRecoveryOffset(consumer);

        if (offset < errorRecoveryOffset.getOffset()) {
            handleMessage(message);
        } else {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logMap.put(errorGroup, errorRecoveryOffset);
            logMap.put(LoggingUtils.TOPIC, errorTopic);
            logger.info("Pausing error consumer as error recovery offset reached.", logMap);
            registry.getListenerContainer(errorGroup).pause();
        }
    }

    @EventListener
    public void consumerStopped(ConsumerStoppedEvent event) {
        Optional.ofNullable(event.getSource(KafkaMessageListenerContainer.class))
                .flatMap(s -> Optional.ofNullable(s.getBeanName()))
                .ifPresent(name -> {
                    if (name.startsWith(errorGroup)) {
                        errorRecoveryOffset.clear();
                    }
                });
    }

    /**
     * Handles processing of received message.
     *
     * @param message containing order received
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
    private void configureErrorRecoveryOffset(KafkaConsumer<String, OrderReceived> consumer) {
        if (!isNull(errorRecoveryOffset.getOffset())) {
            return;
        }
        // Get the end offsets for the consumers partitions i.e. the last un-committed [non-consumed] offsets
        // Note there should [will] only be one entry as this consumer is consuming from a single partition
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
        Long endOffset = endOffsets.get(new TopicPartition(errorTopic, 0));
        errorRecoveryOffset.setOffset(endOffset);
        logger.info(String.format("Setting Error Consumer Recovery Offset to '%1$d'", errorRecoveryOffset.getOffset()));
    }
}