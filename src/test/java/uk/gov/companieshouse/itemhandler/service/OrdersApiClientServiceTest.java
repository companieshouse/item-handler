package uk.gov.companieshouse.itemhandler.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.order.PrivateOrderResourceHandler;
import uk.gov.companieshouse.api.handler.order.request.OrdersGet;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.client.ApiClient;
import uk.gov.companieshouse.itemhandler.exception.ApiException;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.mapper.OrdersApiToOrderDataMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@ExtendWith(MockitoExtension.class)
class OrdersApiClientServiceTest {
    private static final String ORDER_URL = "/orders/1234";
    private static final String ORDER_URL_INCORRECT = "/bad-orders/url";
    private static final String ORDER_ETAG = "abCxYz0324";

    @InjectMocks
    OrdersApiClientService serviceUnderTest;

    @Mock
    ApiResponse<OrdersApi> ordersResponse;

    @Mock
    OrdersApi ordersApi;

    @Mock
    OrdersApiToOrderDataMapper ordersApiToOrderDataMapper;

    @Mock
    ApiClient apiClient;

    @Mock
    InternalApiClient internalApiClient;

    @Mock
    PrivateOrderResourceHandler privateOrderResourceHandler;

    @Mock
    OrdersGet ordersGet;

    @Mock
    ApiErrorResponseException apiErrorResponseException;

    @Test
    void getOrderData() throws Exception {
        final OrderData expectedOrderData = new OrderData();
        expectedOrderData.setEtag(ORDER_ETAG);

        // Given OrdersApi returns valid details
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateOrderResourceHandler()).thenReturn(privateOrderResourceHandler);
        when(privateOrderResourceHandler.getOrder(ORDER_URL)).thenReturn(ordersGet);
        when(ordersGet.execute()).thenReturn(ordersResponse);
        when(ordersResponse.getData()).thenReturn(ordersApi);
        when(ordersApiToOrderDataMapper.ordersApiToOrderData(ordersApi)).thenReturn(expectedOrderData);

        // When & Then
        OrderData actualOrderData = serviceUnderTest.getOrderData(ORDER_URL);
        assertThat(actualOrderData.getEtag(), is(expectedOrderData.getEtag()));
        verify(ordersApiToOrderDataMapper).ordersApiToOrderData(ordersApi);
    }

    @Test
    void getOrderDataThrowsNonRetryableExceptionForOrderNotFound() throws Exception {
        // Given
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateOrderResourceHandler()).thenReturn(privateOrderResourceHandler);
        when(privateOrderResourceHandler.getOrder(ORDER_URL_INCORRECT)).thenReturn(ordersGet);
        when(ordersGet.execute()).thenThrow(apiErrorResponseException);
        when(apiErrorResponseException.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);

        // When
        Executable executable = () -> serviceUnderTest.getOrderData(ORDER_URL_INCORRECT);

        // Then
        Assertions.assertThrows(NonRetryableException.class, executable);
    }

    @Test
    void getOrderDataThrowsRetryableExceptionForOrderServerError() throws Exception {
        // Given
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateOrderResourceHandler()).thenReturn(privateOrderResourceHandler);
        when(privateOrderResourceHandler.getOrder(ORDER_URL)).thenReturn(ordersGet);
        when(ordersGet.execute()).thenThrow(apiErrorResponseException);
        when(apiErrorResponseException.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);

        // Then
        Executable executable = () -> serviceUnderTest.getOrderData(ORDER_URL);

        // When
        Assertions.assertThrows(ApiException.class, executable);
    }

    @Test
    void getOrderDataThrowsRetryableExceptionForInvalidURI() throws Exception {
        // Given
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateOrderResourceHandler()).thenReturn(privateOrderResourceHandler);
        when(privateOrderResourceHandler.getOrder(ORDER_URL_INCORRECT)).thenReturn(ordersGet);
        when(ordersGet.execute()).thenThrow(URIValidationException.class);

        // Then
        Executable executable = () -> serviceUnderTest.getOrderData(ORDER_URL_INCORRECT);

        // When
        NonRetryableException exception = Assertions.assertThrows(NonRetryableException.class, executable);
        assertThat(exception.getMessage(), is("Invalid order URI /bad-orders/url"));
    }
}