package uk.gov.companieshouse.itemhandler.kafka;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.kafka.consumer.resilience.CHKafkaResilientConsumerGroup;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdersKafkaConsumerTest {
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";

    @Spy
    @InjectMocks
    private OrdersKafkaConsumer ordersKafkaConsumer;
    @Mock
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupMain;
    @Mock
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupRetry;
    private static final String expectedMessageValue = "$/order/ORDER-12345";

    @Test
    public void createRetryMessageBuildsMessageSuccessfully() throws SerializationException {
        // Given & When
        OrdersKafkaConsumer consumer = new OrdersKafkaConsumer(new SerializerFactory());
        Message actualMessage = consumer.createRetryMessage(ORDER_RECEIVED_URI);
        // Then
        Assert.assertThat(actualMessage.getValue(), Matchers.is(expectedMessageValue.getBytes()));
    }

    @Test
    public void mainListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).processOrderReceived(ORDER_RECEIVED_URI);
        OrderProcessingException exception = Assertions.assertThrows(OrderProcessingException.class, () -> {
            ordersKafkaConsumer.processOrderReceived(ORDER_RECEIVED_URI);
        });
        // Then
        String expectedMessage = "Order processing failed.";
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(expectedMessage));
        verify(ordersKafkaConsumer, times(1)).processOrderReceived(anyString());
    }

    @Test
    public void retryListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).processOrderReceivedRetry(ORDER_RECEIVED_URI);
        OrderProcessingException exception = Assertions.assertThrows(OrderProcessingException.class, () -> {
            ordersKafkaConsumer.processOrderReceivedRetry(ORDER_RECEIVED_URI);
        });
        // Then
        String expectedMessage = "Order processing failed.";
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(expectedMessage));
        verify(ordersKafkaConsumer, times(1)).processOrderReceivedRetry(anyString());
    }

    @Test
    public void errorListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).processOrderReceivedError(ORDER_RECEIVED_URI);
        OrderProcessingException exception = Assertions.assertThrows(OrderProcessingException.class, () -> {
            ordersKafkaConsumer.processOrderReceivedError(ORDER_RECEIVED_URI);
        });
        // Then
        String expectedMessage = "Order processing failed.";
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(expectedMessage));
        verify(ordersKafkaConsumer, times(1)).processOrderReceivedError(anyString());
    }
}
