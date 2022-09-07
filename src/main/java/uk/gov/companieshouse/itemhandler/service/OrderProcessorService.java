package uk.gov.companieshouse.itemhandler.service;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_URI;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMap;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogger;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.exception.RetryableException;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemRouter;
import uk.gov.companieshouse.itemhandler.model.OrderData;

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
    private final OrderItemRouter orderItemRouter;

    public OrderProcessorService(final OrdersApiClientService ordersApi, final OrderItemRouter orderItemRouter) {
        this.ordersApi = ordersApi;
        this.orderItemRouter = orderItemRouter;
    }

    /**
     * Process the notification of an order received.
     *
     * @param orderUri the URI representing the order received
     * @return OrderProcessStatus of this operation
     */
    public OrderProcessResponse processOrderReceived(final String orderUri) {
        OrderProcessResponse.Builder responseBuilder = OrderProcessResponse.newBuilder();
        responseBuilder.withOrderUri(orderUri);

        Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, ORDER_URI, orderUri);
        try {
            final OrderData order = ordersApi.getOrderData(orderUri);

            logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, order.getReference());
            getLogger().info("Processing order received", logMap);
            orderItemRouter.route(order);
            responseBuilder.withStatus(OrderProcessResponse.Status.OK);
        } catch (RetryableException exception) {
            String msg = String.format("Service unavailable %s", exception.getMessage());
            getLogger().info(msg, logMap);
            responseBuilder.withStatus(OrderProcessResponse.Status.SERVICE_UNAVAILABLE);
        } catch (NonRetryableException exception) {
            String msg = String.format("Service error %s", exception.getMessage());
            getLogger().info(msg, logMap);
            responseBuilder.withStatus(OrderProcessResponse.Status.SERVICE_ERROR);
        }

        return responseBuilder.build();
    }
}
