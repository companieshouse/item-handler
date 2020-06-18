package uk.gov.companieshouse.itemhandler.service;

import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import java.util.concurrent.ExecutionException;


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
    public void processOrderReceived(final String orderUri)
            throws JsonProcessingException,
                   URIValidationException,
                   ExecutionException,
                   InterruptedException,
                   ApiErrorResponseException,
                   SerializationException {
        final OrderData order;
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        try {
            order = ordersApi.getOrderData(orderUri);
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_REFERENCE_NUMBER, order.getReference());
            LoggingUtils.getLogger().info("Processing order received", logMap);
            emailer.sendCertificateOrderConfirmation(order);
        } catch (Exception ex) {
            logMap.put(LoggingUtils.EXCEPTION, ex);
            // TODO GCI-1182 Is it useful to log this here?
            LoggingUtils.getLogger().error("Exception caught getting order data.", logMap);
            // TODO GCI-1182 Should ALL exceptions be propagated here?
            throw ex;
        }

    }
}
