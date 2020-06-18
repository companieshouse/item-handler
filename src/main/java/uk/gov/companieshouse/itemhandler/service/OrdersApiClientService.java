package uk.gov.companieshouse.itemhandler.service;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.order.PrivateOrderResourceHandler;
import uk.gov.companieshouse.api.handler.order.request.PrivateOrderURIPattern;
import uk.gov.companieshouse.api.handler.regex.URIValidator;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.client.ApiClient;
import uk.gov.companieshouse.itemhandler.exception.OrdersApiException;
import uk.gov.companieshouse.itemhandler.exception.RetryableOrdersApiException;
import uk.gov.companieshouse.itemhandler.exception.ServiceException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
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

    public OrderData getOrderData(String orderUri) throws URIValidationException {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        if (URIValidator.validate(PrivateOrderURIPattern.getOrdersPattern(), orderUri)) {
            InternalApiClient internalApiClient = apiClient.getInternalApiClient();
            PrivateOrderResourceHandler privateOrderResourceHandler = internalApiClient.privateOrderResourceHandler();
            try {
                OrdersApi ordersApi = privateOrderResourceHandler.getOrder(orderUri).execute().getData();
                LOGGER.info("Order data returned from API Client", logMap);
                return ordersApiToOrderDataMapper.ordersApiToOrderData(ordersApi);
            } catch (ApiErrorResponseException apiError) {
                // TODO GCI-1182 Log here too?
                if (apiError.getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR /* 500 */) {
                    // TODO GCI-1182 Could more useful info could be provided?
                    throw new RetryableOrdersApiException(apiError.getMessage());
                } else {
                    // We are dealing with a 4xx error in this case
                    // TODO GCI-1182 Could more useful info could be provided?
                    throw new OrdersApiException(apiError.getMessage());
                }
            }
        } else {
            LOGGER.error("Unrecognised uri pattern", logMap);
            throw new ServiceException("Unrecognised uri pattern for "+orderUri);
        }
    }
}
