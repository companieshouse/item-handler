package uk.gov.companieshouse.itemhandler.service;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;

import com.google.api.client.http.HttpStatusCodes;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.order.PrivateOrderResourceHandler;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.client.ApiClient;
import uk.gov.companieshouse.itemhandler.exception.ApiException;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.mapper.OrdersApiToOrderDataMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class OrdersApiClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    private final OrdersApiToOrderDataMapper ordersApiToOrderDataMapper;

    private final ApiClient apiClient;

    public OrdersApiClientService(OrdersApiToOrderDataMapper mapper, ApiClient apiClient) {
        this.ordersApiToOrderDataMapper = mapper;
        this.apiClient = apiClient;
    }

    /**
     * Gets an order using an orderUri identifier.
     *
     * @param orderUri order identifier
     * @return OrderData or null no order with supplied reference
     * @throws ApiException if the service is unavailable
     */
    public OrderData getOrderData(String orderUri) {
        LOGGER.debug(String.format("Order URI %s", orderUri));
        InternalApiClient internalApiClient = apiClient.getInternalApiClient();
        PrivateOrderResourceHandler privateOrderResourceHandler = internalApiClient.privateOrderResourceHandler();

        OrderData orderData;
        try {
            OrdersApi ordersApi = privateOrderResourceHandler.getOrder(orderUri)
                    .execute()
                    .getData();
            LOGGER.debug(String.format("Order API got order %s", ordersApi.getReference()));
            orderData = ordersApiToOrderDataMapper.ordersApiToOrderData(ordersApi);
        } catch (ApiErrorResponseException exception) {
            String message = String.format("Order URI %s, API exception %s, HTTP status %d",
                    orderUri,
                    exception.getMessage(),
                    exception.getStatusCode());
            if (exception.getStatusCode() != HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                LOGGER.info(message);
                throw new ApiException(message, exception);
            } else {
                LOGGER.error(message);
                throw new NonRetryableException(message);
            }
        } catch (URIValidationException exception) {
            String message = String.format("Invalid order URI %s", orderUri);
            LOGGER.error(message, exception);
            throw new NonRetryableException(message);
        }
        return orderData;
    }
}
