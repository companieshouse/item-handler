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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.itemhandler.exception.RetryableErrorException;
import uk.gov.companieshouse.itemhandler.service.EmailService;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdersKafkaConsumerTest {
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_KEY = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String PROCESSING_ERROR_MESSAGE = "Order processing failed.";

    @Spy
    @InjectMocks
    private OrdersKafkaConsumer ordersKafkaConsumer;
    @Mock
    private OrdersKafkaProducer ordersKafkaProducer;
    @Mock
    private SerializerFactory serializerFactory;
    @Mock
    private KafkaListenerEndpointRegistry registry;
    @Mock
    private MessageListenerContainer container;
    @Mock
    private AvroSerializer serializer;
    private static final String EXPECTED_MESSAGE_VALUE = "$/order/ORDER-12345";

    @Test
    public void createRetryMessageBuildsMessageSuccessfully() throws SerializationException {
        // Given & When
        OrdersKafkaConsumer consumerUnderTest =
                new OrdersKafkaConsumer(new SerializerFactory(), new OrdersKafkaProducer(),
                        new KafkaListenerEndpointRegistry(), new EmailService());
        Message actualMessage = consumerUnderTest.createRetryMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC);
        byte[] actualMessageRawValue    = actualMessage.getValue();
        byte[] expectedMessageRawValue  = EXPECTED_MESSAGE_VALUE.getBytes();
        // Then
        Assert.assertThat(actualMessageRawValue, Matchers.is(expectedMessageRawValue));
    }

    @Test
    public void republishMessageToRetryTopicRunsSuccessfully()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doCallRealMethod().when(ordersKafkaConsumer).republishMessageToTopic(anyString(), anyString(), anyString());
        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY);
        // Then
        verify(ordersKafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void republishMessageToErrorTopicRunsSuccessfully()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doCallRealMethod().when(ordersKafkaConsumer).republishMessageToTopic(anyString(), anyString(), anyString());
        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR);
        // Then
        verify(ordersKafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void republishMessageSuccessfullyCalledOnRetryableErrorException()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(ordersKafkaConsumer).logMessageReceived(any());
        ordersKafkaConsumer.handleMessage(createTestMessage());
        // Then
        verify(ordersKafkaConsumer, times(1)).republishMessageToTopic(anyString(), anyString(), anyString());
    }

    @Test
    public void republishMessageNotCalledOnNonRetryableErrorException()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        doThrow(new OrderProcessingException()).when(ordersKafkaConsumer).logMessageReceived(any());
        ordersKafkaConsumer.handleMessage(createTestMessage());
        // Then
        verify(ordersKafkaConsumer, times(0)).republishMessageToTopic(anyString(), anyString(), anyString());
    }

    @Test
    public void mainListenerExceptionIsCorrectlyHandled()
            throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(ordersKafkaConsumer).processOrderReceived(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            ordersKafkaConsumer.processOrderReceived(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(PROCESSING_ERROR_MESSAGE));
        verify(ordersKafkaConsumer, times(1)).processOrderReceived(any());
    }

    @Test
    public void retryListenerExceptionIsCorrectlyHandled()
            throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(ordersKafkaConsumer).processOrderReceivedRetry(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            ordersKafkaConsumer.processOrderReceivedRetry(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(PROCESSING_ERROR_MESSAGE));
        verify(ordersKafkaConsumer, times(1)).processOrderReceivedRetry(any());
    }

    @Test
    public void errorListenerExceptionIsCorrectlyHandled()
            throws InterruptedException, ExecutionException, SerializationException {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(ordersKafkaConsumer).processOrderReceivedError(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            ordersKafkaConsumer.processOrderReceivedError(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(PROCESSING_ERROR_MESSAGE));
        verify(ordersKafkaConsumer, times(1)).processOrderReceivedError(any());
    }

    private static org.springframework.messaging.Message createTestMessage() {
        return new org.springframework.messaging.Message<OrderReceived>() {
            @Override
            public OrderReceived getPayload() {
                OrderReceived orderReceived = new OrderReceived();
                orderReceived.setOrderUri(ORDER_RECEIVED_URI);
                return orderReceived;
            }

            @Override
            public MessageHeaders getHeaders() {
                Map<String, Object> headerItems = new HashMap<>();
                headerItems.put("kafka_receivedTopic", ORDER_RECEIVED_TOPIC);
                headerItems.put("kafka_offset", 0);
                headerItems.put("kafka_receivedMessageKey", ORDER_RECEIVED_KEY);
                headerItems.put("kafka_receivedPartitionId", 0);
                MessageHeaders headers = new MessageHeaders(headerItems);
                return headers;
            }
        };
    }
}
