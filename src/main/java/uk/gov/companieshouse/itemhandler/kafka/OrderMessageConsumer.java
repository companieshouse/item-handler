package uk.gov.companieshouse.itemhandler.kafka;

import java.util.concurrent.CountDownLatch;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageConsumer {

    private final KafkaListenerEndpointRegistry registry;
    private final CountDownLatch eventLatch = new CountDownLatch(1);
    private final OrderMessageHandler orderReceivedProcessor;

    public OrderMessageConsumer(KafkaListenerEndpointRegistry registry, OrderMessageHandler orderReceivedProcessor) {
        this.registry = registry;
        this.orderReceivedProcessor = orderReceivedProcessor;
    }

    public CountDownLatch getEventLatch() {
        return eventLatch;
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
        orderReceivedProcessor.handleMessage(message);
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
        orderReceivedProcessor.handleMessage(message);
    }
}