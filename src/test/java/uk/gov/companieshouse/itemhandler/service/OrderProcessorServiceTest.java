package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.exception.KafkaMessagingException;
import uk.gov.companieshouse.itemhandler.exception.RetryableErrorException;
import uk.gov.companieshouse.itemhandler.exception.ServiceException;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;

/** Unit tests the {@link OrderProcessorService} class. */
@ExtendWith(MockitoExtension.class)
class OrderProcessorServiceTest {

    private static final String ORDER_URI = "/orders/" + ORDER_REFERENCE_NUMBER;
    private static final String TEST_EXCEPTION_MESSAGE = "Test message!";

    @InjectMocks
    private OrderProcessorService orderProcessorUnderTest;

    @Mock
    private OrdersApiClientService ordersApi;

    @Mock
    private OrderRouterService orderRouter;

    @Mock
    private OrderData order;

    /** Extends {@link JsonProcessingException} so it can be instantiated in these tests. */
    private static class TestJsonProcessingException extends JsonProcessingException {

        protected TestJsonProcessingException(String msg) {
            super(msg);
        }
    }

    /** Extends {@link ExecutionException} so it can be instantiated in these tests. */
    private static class TestExecutionException extends ExecutionException {

        protected TestExecutionException(String msg) {
            super(msg);
        }
    }

    @Test
    void getsOrderAndSendsOutConfirmation() throws Exception {

        // Given
        when(ordersApi.getOrderData(ORDER_URI)).thenReturn(order);
        when(order.getReference()).thenReturn(ORDER_REFERENCE_NUMBER);

        // When
        orderProcessorUnderTest.processOrderReceived(ORDER_URI);

        // Then
        verify(ordersApi).getOrderData(ORDER_URI);
        verify(orderRouter).routeOrder(any(OrderData.class));
    }

    @Test
    void propagatesJsonProcessingException() throws Exception {
        propagatesException(TestJsonProcessingException::new);
    }

    @Test
    void propagatesSerializationException() throws Exception {
        propagatesException(SerializationException::new);
    }

    @Test
    void propagatesExecutionException() throws Exception {
        propagatesException(TestExecutionException::new);
    }

    @Test
    void propagatesInterruptedException() throws Exception {
        propagatesException(InterruptedException::new);
    }

    @Test
    void propagatesServiceException() throws Exception {
        propagatesException(ServiceException::new);
    }

    @Test
    void propagatesKafkaMessagingException() throws Exception {
        propagatesException(KafkaMessagingException::new);
    }

    @Test
    void propagatesRetryableErrorException() throws Exception {
        propagatesException(RetryableErrorException::new);
    }

    private void propagatesException(final Function<String, Exception> constructor)  throws Exception {

        // Given
        givenSendCertificateOrderConfirmationThrowsException(constructor);

        // When and then
        assertThatExceptionOfType(constructor.apply(TEST_EXCEPTION_MESSAGE).getClass()).isThrownBy(() ->
                orderProcessorUnderTest.processOrderReceived(ORDER_URI))
                .withMessage(TEST_EXCEPTION_MESSAGE)
                .withNoCause();
    }

    /**
     * Sets up mocks to throw the exception for which the constructor is provided when the service calls
     * {@link OrderRouterService#routeOrder(OrderData)}.
     * @param constructor the Exception constructor to use
     * @throws Exception should something unexpected happen
     */
    private void givenSendCertificateOrderConfirmationThrowsException(final Function<String, Exception> constructor)
            throws Exception {
        when(ordersApi.getOrderData(ORDER_URI)).thenReturn(order);
        when(order.getReference()).thenReturn(ORDER_REFERENCE_NUMBER);
        doThrow(constructor.apply(TEST_EXCEPTION_MESSAGE)).when(orderRouter).routeOrder(any(OrderData.class));
    }


}
