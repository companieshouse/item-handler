package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

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
        try {
            order = ordersApi.getOrderData(orderUri);
            LOGGER.info("Got order data for " + orderUri + ", order reference number = " + order.getReference());
            emailer.sendCertificateOrderConfirmation(order);
        } catch (Exception ex) {
            // TODO GCI-1182 Is it useful to log this here?
            LOGGER.error("Exception caught getting order data.", ex);
            // TODO GCI-1182 Should ALL exceptions be propagated here?
            throw ex;
        }

    }
}
