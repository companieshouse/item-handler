package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests the {@link OrderProcessorService} class. */
@ExtendWith(MockitoExtension.class)
public class OrderProcessorServiceTest {

    private static final String ORDER_REFERENCE_NUMBER = "ORD-469415-911973";
    private static final String ORDER_URI = "/orders/" + ORDER_REFERENCE_NUMBER;
    private static final String TEST_EXCEPTION_MESSAGE = "Test message!";

    @InjectMocks
    private OrderProcessorService orderProcessorUnderTest;

    @Mock
    private OrdersApiClientService ordersApi;

    @Mock
    private EmailService emailer;

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
        verify(emailer).sendCertificateOrderConfirmation(any(OrderData.class));
    }

    @Test
    void propagatesJsonProcessingException() throws Exception {
        propagatesException(TestJsonProcessingException::new, TestJsonProcessingException.class);
    }

    @Test
    void propagatesSerializationException() throws Exception {
        propagatesException(SerializationException::new, SerializationException.class);
    }

    @Test
    void propagatesExecutionException() throws Exception {
        propagatesException(TestExecutionException::new, TestExecutionException.class);
    }

    @Test
    void propagatesInterruptedException() throws Exception {
        propagatesException(InterruptedException::new, InterruptedException.class);
    }

    /**
     * Asserts that the exception thrown is propagated
     * @param exceptionConstructor the exception constructor
     * @param exceptionClass the class of the exception
     * @throws Exception should something unexpected happen
     */
    private void propagatesException(final Function<String, Exception> exceptionConstructor,
                                     final Class<? extends Throwable> exceptionClass)
            throws Exception {

        // Given
        givenSendCertificateOrderConfirmationThrowsException(exceptionConstructor);

        // When and then
        assertThatExceptionOfType(exceptionClass).isThrownBy(() ->
                                                    orderProcessorUnderTest.processOrderReceived(ORDER_URI))
                                                    .withMessage(TEST_EXCEPTION_MESSAGE);
    }

    /**
     * Sets up mocks to throw the exception for which the constructor is provided when the service calls
     * {@link EmailService#sendCertificateOrderConfirmation(OrderData)}.
     * @param exceptionConstructor the exception constructor to use
     * @throws Exception should something unexpected happen
     */
    private void givenSendCertificateOrderConfirmationThrowsException(
            final Function<String, Exception> exceptionConstructor) throws Exception {
        when(ordersApi.getOrderData(ORDER_URI)).thenReturn(order);
        when(order.getReference()).thenReturn(ORDER_REFERENCE_NUMBER);
        doThrow(exceptionConstructor.apply(TEST_EXCEPTION_MESSAGE))
                .when(emailer).sendCertificateOrderConfirmation(any(OrderData.class));
    }


}
