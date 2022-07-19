package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageRetryConsumer {

    private final OrderMessageHandler orderReceivedProcessor;

    public OrderMessageRetryConsumer(OrderMessageHandler orderReceivedProcessor) {
        this.orderReceivedProcessor = orderReceivedProcessor;
    }

    /**
     * Retry (`-retry`) listener/consumer. Calls `handleMessage` method to process received message.
     *
     * @param message received
     */
    @KafkaListener(id = "#{'${kafka.topics.order-received-notification-retry-group}'}",
            groupId = "#{'${kafka.topics.order-received-notification-retry-group}'}",
            topics = "#{'${kafka.topics.order-received-notification-retry}'}",
            autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceived(Message<OrderReceived> message) {
        orderReceivedProcessor.handleMessage(message);
    }
}