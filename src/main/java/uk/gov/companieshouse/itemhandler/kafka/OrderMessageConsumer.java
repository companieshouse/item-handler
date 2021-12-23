package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageConsumer {

    private final KafkaListenerEndpointRegistry registry;
    private final OrderMessageHandler orderReceivedProcessor;
    private CountDownLatch preRetryEventLatch;
    private CountDownLatch postOrderReceivedEventLatch;
    private CountDownLatch postRetryEventLatch;

    public OrderMessageConsumer(KafkaListenerEndpointRegistry registry, OrderMessageHandler orderReceivedProcessor) {
        this.registry = registry;
        this.orderReceivedProcessor = orderReceivedProcessor;
    }

    public CountDownLatch getPreRetryEventLatch() {
        return preRetryEventLatch;
    }

    public void setPreRetryEventLatch(CountDownLatch countDownLatch) {
        this.preRetryEventLatch = countDownLatch;
    }

    public CountDownLatch getPostOrderReceivedEventLatch() {
        return postOrderReceivedEventLatch;
    }

    public void setPostOrderReceivedEventLatch(CountDownLatch postOrderReceivedEventLatch) {
        this.postOrderReceivedEventLatch = postOrderReceivedEventLatch;
    }

    public CountDownLatch getPostRetryEventLatch() {
        return postRetryEventLatch;
    }

    public void setPostRetryEventLatch(CountDownLatch postRetryEventLatch) {
        this.postRetryEventLatch = postRetryEventLatch;
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
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message)
            throws InterruptedException {
        orderReceivedProcessor.handleMessage(message);
        if (!isNull(postOrderReceivedEventLatch)) {
            postOrderReceivedEventLatch.countDown();
        }
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
            org.springframework.messaging.Message<OrderReceived> message)
            throws InterruptedException {
        if (!isNull(preRetryEventLatch)) {
            preRetryEventLatch.await(30, TimeUnit.SECONDS);
        }
        orderReceivedProcessor.handleMessage(message);
        if (!isNull(postRetryEventLatch)) {
            postRetryEventLatch.countDown();
        }
    }
}