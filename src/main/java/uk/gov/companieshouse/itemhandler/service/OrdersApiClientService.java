package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.order.PrivateOrderResourceHandler;
import uk.gov.companieshouse.api.handler.order.request.PrivateOrderURIPattern;
import uk.gov.companieshouse.api.handler.regex.URIValidator;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.client.ApiClient;
import uk.gov.companieshouse.itemhandler.exception.ServiceException;
import uk.gov.companieshouse.itemhandler.mapper.OrdersApiToOrderDataMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Service
public class OrdersApiClientService {
    private final OrdersApiToOrderDataMapper ordersApiToOrderDataMapper;

    private final ApiClient apiClient;

    public OrdersApiClientService(OrdersApiToOrderDataMapper mapper, ApiClient apiClient) {
        this.ordersApiToOrderDataMapper = mapper;
        this.apiClient = apiClient;
    }

    public OrderData getOrderData(String orderUri) throws Exception {
        if (URIValidator.validate(PrivateOrderURIPattern.getOrdersPattern(), orderUri)) {
            InternalApiClient internalApiClient = apiClient.getInternalApiClient();
            PrivateOrderResourceHandler privateOrderResourceHandler = internalApiClient.privateOrderResourceHandler();
            OrdersApi ordersApi = privateOrderResourceHandler.getOrder(orderUri).execute().getData();

            return ordersApiToOrderDataMapper.ordersApiToOrderData(ordersApi);
        } else {
            throw new ServiceException("Unrecognised uri pattern for "+orderUri);
        }
    }
}
