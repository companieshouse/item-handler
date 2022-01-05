package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageDefaultConsumer {

    private final OrderMessageHandler orderReceivedProcessor;

    public OrderMessageDefaultConsumer(OrderMessageHandler orderReceivedProcessor) {
        this.orderReceivedProcessor = orderReceivedProcessor;
    }

    /**
     * Main listener/consumer. Calls `handleMessage` method to process received message.
     *
     * @param message received
     */
    @KafkaListener(id = "#{'${kafka.topics.order-received_group}'}",
            groupId = "#{'${kafka.topics.order-received_group}'}",
            topics = "#{'${kafka.topics.order-received}'}",
            autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message) {
        orderReceivedProcessor.handleMessage(message);
    }
}