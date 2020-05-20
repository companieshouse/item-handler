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
import uk.gov.companieshouse.itemhandler.service.OrdersApiClientService;
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
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupMain;
    @Mock
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupRetry;
    @Mock
    private SerializerFactory serializerFactory;
    @Mock
    private AvroSerializer serializer;
    private static final String EXPECTED_MESSAGE_VALUE = "$/order/ORDER-12345";
    @Mock
    OrdersApiClientService ordersApi;

    @Test
    public void createRetryMessageBuildsMessageSuccessfully() throws SerializationException {
        // Given & When
        OrdersKafkaConsumer consumerUnderTest = new OrdersKafkaConsumer(new SerializerFactory(), ordersApi);
        Message actualMessage = consumerUnderTest.createRetryMessage(ORDER_RECEIVED_URI);
        byte[] actualMessageRawValue    = actualMessage.getValue();
        byte[] expectedMessageRawValue  = EXPECTED_MESSAGE_VALUE.getBytes();
        // Then
        Assert.assertThat(actualMessageRawValue, Matchers.is(expectedMessageRawValue));
    }

    @Test
    public void republishMessageToRetryTopicRunsSuccessfully() throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        ordersKafkaConsumer.setChKafkaConsumerGroupMain(chKafkaConsumerGroupMain);
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doCallRealMethod().when(ordersKafkaConsumer).republishMessageToTopic(anyString(), anyString(), anyString(), any());
        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY, new OrderProcessingException());
        // Then
        verify(chKafkaConsumerGroupMain, times(3)).retry(anyInt(), any());
    }

    @Test
    public void republishMessageToErrorTopicRunsSuccessfully() throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        ordersKafkaConsumer.setChKafkaConsumerGroupRetry(chKafkaConsumerGroupRetry);
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doCallRealMethod().when(ordersKafkaConsumer).republishMessageToTopic(anyString(), anyString(), anyString(), any());
        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR, new OrderProcessingException());
        // Then
        verify(chKafkaConsumerGroupRetry, times(3)).retry(anyInt(), any());
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
