package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.Map;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_URI;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMap;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogger;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

/**
 * This service does the following:
 * <ol>
 *     <li>handles order received notification</li>
 *     <li>retrieves the order (data) from the Orders API</li>
 *     <li>sends a certificate or certified copy order confirmation via the CHS Email Sender OR</li>
 *     <li>sends a MID item message to the CHD Order Consumer</li>
 * </ol>
 */
@Service
public class OrderProcessorService {

    private final OrdersApiClientService ordersApi;
    private final OrderRouterService orderRouter;

    public OrderProcessorService(final OrdersApiClientService ordersApi, final OrderRouterService orderRouter) {
        this.ordersApi = ordersApi;
        this.orderRouter = orderRouter;
    }

    /**
     * Implements all of the business logic required to process the notification of an order received.
     * @param orderUri the URI representing the order received
     */
    public void processOrderReceived(final String orderUri) {
        Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, ORDER_URI, orderUri);
        final OrderData order = ordersApi.getOrderData(orderUri);

        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, order.getReference());
        getLogger().info("Processing order received", logMap);
        orderRouter.routeOrder(order);
    }
}
