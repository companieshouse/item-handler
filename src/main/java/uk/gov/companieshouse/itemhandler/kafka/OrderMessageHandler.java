package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageHandler {

    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final CountDownLatch eventLatch = new CountDownLatch(1);

    public OrderMessageHandler(final OrderProcessorService orderProcessorService,
                               final OrderProcessResponseHandler orderProcessResponseHandler) {
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
    }

    public CountDownLatch getEventLatch() {
        return eventLatch;
    }

    /**
     * Handles processing of received message.
     *
     * @param message
     */
    public void handleMessage(org.springframework.messaging.Message<OrderReceived> message) {
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
}