package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageErrorConsumer {

    private final PartitionOffset errorRecoveryOffset;
    private final ErrorConsumerController errorConsumerController;

    private final Logger logger;
    private final OrderMessageHandler orderReceivedProcessor;

    private String errorGroup;
    private String errorTopic;

    public OrderMessageErrorConsumer(OrderMessageHandler orderReceivedProcessor,
                                     Logger logger,
                                     PartitionOffset errorRecoveryOffset,
                                     ErrorConsumerController errorConsumerController) {
        this.orderReceivedProcessor = orderReceivedProcessor;
        this.logger = logger;
        this.errorRecoveryOffset = errorRecoveryOffset;
        this.errorConsumerController = errorConsumerController;
    }

    /**
     * Error (`-error`) topic listener/consumer is enabled when the application is launched in error
     * mode (IS_ERROR_QUEUE_CONSUMER=true). Receives messages up to `errorRecoveryOffset` offset.
     * Calls `handleMessage` method to process received message. If the `retryable` processor is
     * unsuccessful with a `retryable` error, after maximum numbers of attempts allowed, the message
     * is republished to `-retry` topic for failover processing. This listener stops accepting
     * messages when the topic's offset reaches `errorRecoveryOffset`.
     *
     * @param message to be processed
     * @param offset Kafka offset of the current message
     * @param consumer Kafka consumer used by current consumer thread
     */
    @KafkaListener(id = "#{'${kafka.topics.order-received-error-group}'}",
            groupId = "#{'${kafka.topics.order-received-error-group}'}",
            topics = "#{'${kafka.topics.order-received-error}'}",
            autoStartup = "#{${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactoryError")
    public void processOrderReceived(
            Message<OrderReceived> message,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.CONSUMER) KafkaConsumer<String, OrderReceived> consumer) {

        // Configure recovery offset on first message received after application startup
        configureErrorRecoveryOffset(consumer);

        if (offset < errorRecoveryOffset.getOffset()) {
            orderReceivedProcessor.handleMessage(message);
        }

        // Stop consumer after offset reached
        if (offset >= errorRecoveryOffset.getOffset() - 1) {
            errorConsumerController.pauseConsumerThread();
        }
    }

    @EventListener
    public void consumerStopped(ConsumerStoppedEvent event) {
        Optional.ofNullable(event.getSource(KafkaMessageListenerContainer.class))
                .flatMap(s -> Optional.of(s.getBeanName()))
                .ifPresent(name -> {
                    if (name.startsWith(errorGroup)) {
                        errorRecoveryOffset.clear();
                    }
                });
    }

    /**
     * Lazily sets `errorRecoveryOffset` to last topic offset, before first message received is
     * consumed. This helps the error consumer to stop consuming messages when all messages up to
     * `errorRecoveryOffset` are processed.
     */
    void configureErrorRecoveryOffset(KafkaConsumer<String, OrderReceived> consumer) {
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

    @Autowired
    void setErrorGroup(@Value("${kafka.topics.order-received-error-group}") String errorGroup) {
        this.errorGroup = errorGroup;
    }

    @Autowired
    void setErrorTopic(@Value("${kafka.topics.order-received-error}") String errorTopic) {
        this.errorTopic = errorTopic;
    }
}