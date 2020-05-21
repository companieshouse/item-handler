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
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import uk.gov.companieshouse.kafka.consumer.resilience.CHKafkaResilientConsumerGroup;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdersKafkaConsumerTest {
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";

    @Spy
    @InjectMocks
    private OrdersKafkaConsumer ordersKafkaConsumer;
    @Mock
    private OrdersKafkaProducer ordersKafkaProducer;
    @Mock
    private SerializerFactory serializerFactory;
    @Mock
    private AvroSerializer serializer;
    private static final String EXPECTED_MESSAGE_VALUE = "$/order/ORDER-12345";

    @Test
    public void createRetryMessageBuildsMessageSuccessfully() throws SerializationException {
        // Given & When
        OrdersKafkaConsumer consumerUnderTest =
                new OrdersKafkaConsumer(new SerializerFactory(), new OrdersKafkaProducer(), new KafkaListenerEndpointRegistry());
        Message actualMessage = consumerUnderTest.createRetryMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC);
        byte[] actualMessageRawValue    = actualMessage.getValue();
        byte[] expectedMessageRawValue  = EXPECTED_MESSAGE_VALUE.getBytes();
        // Then
        Assert.assertThat(actualMessageRawValue, Matchers.is(expectedMessageRawValue));
    }

    @Test
    public void republishMessageToRetryTopicRunsSuccessfully() throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doCallRealMethod().when(ordersKafkaConsumer).republishMessageToTopic(anyString(), anyString(), anyString());
        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY);
        // Then
        verify(ordersKafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void republishMessageToErrorTopicRunsSuccessfully() throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doCallRealMethod().when(ordersKafkaConsumer).republishMessageToTopic(anyString(), anyString(), anyString());
        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR);
        // Then
        verify(ordersKafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void mainListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).processOrderReceived(any());
        OrderProcessingException exception = Assertions.assertThrows(OrderProcessingException.class, () -> {
            ordersKafkaConsumer.processOrderReceived(any());
        });
        // Then
        String expectedMessage = "Order processing failed.";
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(expectedMessage));
        verify(ordersKafkaConsumer, times(1)).processOrderReceived(any());
    }

    @Test
    public void retryListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).processOrderReceivedRetry(any());
        OrderProcessingException exception = Assertions.assertThrows(OrderProcessingException.class, () -> {
            ordersKafkaConsumer.processOrderReceivedRetry(any());
        });
        // Then
        String expectedMessage = "Order processing failed.";
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(expectedMessage));
        verify(ordersKafkaConsumer, times(1)).processOrderReceivedRetry(any());
    }

    @Test
    public void errorListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).processOrderReceivedError(any());
        OrderProcessingException exception = Assertions.assertThrows(OrderProcessingException.class, () -> {
            ordersKafkaConsumer.processOrderReceivedError(any());
        });
        // Then
        String expectedMessage = "Order processing failed.";
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(expectedMessage));
        verify(ordersKafkaConsumer, times(1)).processOrderReceivedError(any());
    }
}
