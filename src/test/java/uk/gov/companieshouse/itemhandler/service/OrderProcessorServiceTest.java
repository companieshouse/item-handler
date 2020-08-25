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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        verify(emailer).sendOrderConfirmation(any(OrderData.class));
    }

    @Test
    void doesNotPropagateJsonProcessingException() throws Exception {
        doesNotPropagateException(TestJsonProcessingException::new);
    }

    @Test
    void doesNotPropagateSerializationException() throws Exception {
        doesNotPropagateException(SerializationException::new);
    }

    @Test
    void doesNotPropagateExecutionException() throws Exception {
        doesNotPropagateException(TestExecutionException::new);
    }

    @Test
    void doesNotPropagateInterruptedException() throws Exception {
        doesNotPropagateException(InterruptedException::new);
    }

    private void doesNotPropagateException(final Function<String, Exception> constructor)  throws Exception {

        // Given
        givenSendCertificateOrderConfirmationThrowsException(TestJsonProcessingException::new);

        // When and then
        assertThatCode(() -> orderProcessorUnderTest.processOrderReceived(ORDER_URI))
                .doesNotThrowAnyException();
    }

    /**
     * Sets up mocks to throw the exception for which the constructor is provided when the service calls
     * {@link EmailService#sendOrderConfirmation(OrderData)}.
     * @param constructor the Exception constructor to use
     * @throws Exception should something unexpected happen
     */
    private void givenSendCertificateOrderConfirmationThrowsException(final Function<String, Exception> constructor) throws Exception {
        when(ordersApi.getOrderData(ORDER_URI)).thenReturn(order);
        when(order.getReference()).thenReturn(ORDER_REFERENCE_NUMBER);
        doThrow(constructor.apply(TEST_EXCEPTION_MESSAGE)).when(emailer).sendOrderConfirmation(any(OrderData.class));
    }


}
