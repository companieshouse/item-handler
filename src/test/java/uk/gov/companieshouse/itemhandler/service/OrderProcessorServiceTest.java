package uk.gov.companieshouse.itemhandler.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.exception.ApiException;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.itemsummary.DigitalOrderItemRouter;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemRouter;
import uk.gov.companieshouse.itemhandler.model.OrderData;

/** Unit tests the {@link OrderProcessorService} class. */
@ExtendWith(MockitoExtension.class)
class OrderProcessorServiceTest {

    private static final String ORDER_URI = "/orders/" + ORDER_REFERENCE_NUMBER;

    @InjectMocks
    private OrderProcessorService orderProcessorUnderTest;

    @Mock
    private OrdersApiClientService ordersApi;

    @Mock
    private OrderData order;

    @Mock
    private OrderItemRouter orderItemRouter;

    @Mock
    private DigitalOrderItemRouter digitalOrderItemRouter;

    @Test
    void getsOrderAndSendsOutConfirmation() {

        // Given
        when(ordersApi.getOrderData(ORDER_URI)).thenReturn(order);
        when(order.getReference()).thenReturn(ORDER_REFERENCE_NUMBER);

        // When
        orderProcessorUnderTest.processOrderReceived(ORDER_URI);

        // Then
        verify(ordersApi).getOrderData(ORDER_URI);
        verify(orderItemRouter).route(order);
        verify(digitalOrderItemRouter).route(order);
    }

    @Test
    void testServiceUnavailable() {
        when(ordersApi.getOrderData(ORDER_URI)).thenThrow(ApiException.class);

        OrderProcessResponse actual = orderProcessorUnderTest.processOrderReceived(ORDER_URI);
        assertEquals(OrderProcessResponse.Status.SERVICE_UNAVAILABLE, actual.getStatus());
    }

    @Test
    void testServiceError() {
        when(ordersApi.getOrderData(ORDER_URI)).thenThrow(NonRetryableException.class);

        OrderProcessResponse actual = orderProcessorUnderTest.processOrderReceived(ORDER_URI);
        assertEquals(OrderProcessResponse.Status.SERVICE_ERROR, actual.getStatus());
    }

    @Test
    void testProcessOrderWithMultipleItems() {
        // given
        when(ordersApi.getOrderData(any())).thenReturn(order);

        // when
        OrderProcessResponse actual = orderProcessorUnderTest.processOrderReceived(ORDER_URI);

        // then
        assertThat(actual.getStatus(), is(equalTo(OrderProcessResponse.Status.OK)));
        verify(orderItemRouter).route(order);
        verify(digitalOrderItemRouter).route(order);
    }
}
