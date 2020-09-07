package uk.gov.companieshouse.itemhandler.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.OrderData;

/**
 * This service does the following:
 * <ol>
 *     <li>handles order received notification</li>
 *     <li>retrieves the order (data) from the Orders API</li>
 *     <li>sends a certificate order confirmation via the CHS Email Sender</li>
 * </ol>
 */
@Service
public class OrderProcessorService {

    private final OrdersApiClientService ordersApi;
    private final EmailService emailer;

    public OrderProcessorService(final OrdersApiClientService ordersApi, final EmailService emailer) {
        this.ordersApi = ordersApi;
        this.emailer = emailer;
    }

    /**
     * Implements all of the business logic required to process the notification of an order received.
     * @param orderUri the URI representing the order received
     */
    public void processOrderReceived(final String orderUri) {
        final OrderData order;
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        try {
            order = ordersApi.getOrderData(orderUri);
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_REFERENCE_NUMBER, order.getReference());
            LoggingUtils.getLogger().info("Processing order received", logMap);
            emailer.sendOrderConfirmation(order);
        } catch (Exception ex) {
            LoggingUtils.getLogger().error("Exception caught getting order data.", ex, logMap);
        }

    }
}
