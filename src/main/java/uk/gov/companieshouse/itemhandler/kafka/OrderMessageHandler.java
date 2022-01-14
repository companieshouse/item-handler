package uk.gov.companieshouse.itemhandler.kafka;

import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
public class OrderMessageHandler {

    private final OrderProcessorService orderProcessorService;
    private final OrderProcessResponseHandler orderProcessResponseHandler;
    private final MessageFilter messageFilter;
    private final Logger logger;

    public OrderMessageHandler(final OrderProcessorService orderProcessorService,
                               final OrderProcessResponseHandler orderProcessResponseHandler,
                               final MessageFilter<OrderReceived> messageFilter,
                               final Logger logger) {
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
        this.messageFilter = messageFilter;
        this.logger = logger;
    }

    /**
     * Handles processing of received message.
     *
     * @param message received
     */
    public void handleMessage(Message<OrderReceived> message) {
        OrderReceived orderReceived = message.getPayload();
        String orderReceivedUri = orderReceived.getOrderUri();

        if (messageFilter.include(message)) {
            logMessageReceived(message, orderReceivedUri);

            // Process message
            OrderProcessResponse response = orderProcessorService.processOrderReceived(
                    orderReceivedUri);
            // Handle response
            response.getStatus().accept(orderProcessResponseHandler, message);
        } else {
            logDuplicateMessage(message, orderReceivedUri);
        }
    }

    protected void logMessageReceived(Message<OrderReceived> message, String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        logger.info("'order-received' message received", logMap);
    }

    protected void logDuplicateMessage(Message<OrderReceived> message, String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        logger.debug("'order-received' message is a duplicate", logMap);
    }
}