package uk.gov.companieshouse.itemhandler.kafka;

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
    private final Logger logger;

    public OrderMessageHandler(final OrderProcessorService orderProcessorService,
                               final OrderProcessResponseHandler orderProcessResponseHandler,
                               final Logger logger) {
        this.orderProcessorService = orderProcessorService;
        this.orderProcessResponseHandler = orderProcessResponseHandler;
        this.logger = logger;
    }

    /**
     * Handles processing of received message.
     *
     * @param message received
     */
    public void handleMessage(Message<OrderReceived> message) {
        // Log message
        logger.info("'order-received' message received", LoggingUtils.getMessageHeadersAsMap(message));

        // Process message
        OrderProcessResponse response = orderProcessorService.processOrderReceived(message.getPayload().getOrderUri());

        // Handle response
        response.getStatus().accept(orderProcessResponseHandler, message);
    }
}