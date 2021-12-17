package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.avro.generic.IndexedRecord;
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
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.itemhandler.exception.ApplicationSerialisationException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@ExtendWith(MockitoExtension.class)
class OrderMessageConsumerTest {
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_KEY = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";

    @Spy
    @InjectMocks
    private OrderMessageConsumer ordersKafkaConsumer;
    @Mock
    private MessageProducer messageProducer;
    @Mock
    private SerializerFactory serializerFactory;
    @Mock
    private AvroSerializer serializer;
    @Mock
    private OrderProcessorService orderProcessorService;
    @Mock
    private OrderProcessResponseHandler orderProcessResponseHandler;
    @Mock
    private Logger logger;

    private static org.springframework.messaging.Message<OrderReceived> createTestMessage(String receivedTopic) {
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
                headerItems.put("kafka_receivedTopic", receivedTopic);
                headerItems.put("kafka_offset", 0);
                headerItems.put("kafka_receivedMessageKey", ORDER_RECEIVED_KEY);
                headerItems.put("kafka_receivedPartitionId", 0);
                MessageHeaders headers = new MessageHeaders(headerItems);
                return headers;
            }
        };
    }

//    @Test
//    void createRetryMessageBuildsMessageSuccessfully() {
//        // Given & When
//        OrderMessageConsumer consumerUnderTest =
//                new OrderMessageConsumer(new SerializerFactory(),
//                        new MessageProducer(logger),
//                        new KafkaListenerEndpointRegistry(),
//                        orderProcessorService,
//                        orderProcessResponseHandler);
//        Message actualMessage = consumerUnderTest.createRetryMessage(ORDER_RECEIVED_URI,
//                ORDER_RECEIVED_TOPIC);
//        byte[] actualMessageRawValue = actualMessage.getValue();
//        // Then
//        MessageDeserializer<IndexedRecord> deserializer = new MessageDeserializer<>();
//        String actualOrderReceived = (String) deserializer.deserialize(ORDER_RECEIVED_TOPIC,
//                actualMessageRawValue).get(0);
//        Assert.assertThat(actualOrderReceived, Matchers.is(ORDER_RECEIVED_URI));
//    }

//    @Test
//    void republishMessageToRetryTopicRunsSuccessfully()
//            throws ExecutionException, InterruptedException, SerializationException {
//        // Given & When
//        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(
//                serializer);
//        when(serializer.toBinary(any())).thenReturn(new byte[4]);
//        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI,
//                ORDER_RECEIVED_TOPIC,
//                ORDER_RECEIVED_TOPIC_RETRY);
//        // Then
//        verify(messageProducer, times(1)).sendMessage(any());
//    }

//    @Test
//    void republishMessageToRetryTopicThrowsSerializationException() throws SerializationException {
//        // Given & When
//        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(
//                serializer);
//        when(serializer.toBinary(any())).thenThrow(SerializationException.class);
//        ApplicationSerialisationException
//                exception = assertThrows(ApplicationSerialisationException.class,
//                () -> ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI,
//                        ORDER_RECEIVED_TOPIC,
//                        ORDER_RECEIVED_TOPIC_RETRY));
//        Assertions.assertEquals("Failed to serialise message", exception.getMessage());
//    }

//    @Test
//    void republishMessageSuccessfullyCalledForFirstMainMessageOnRetryableErrorException()
//            throws SerializationException {
//        // Given & When
//        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
//        when(serializer.toBinary(any())).thenReturn(new byte[4]);
//        doThrow(new RetryableException(PROCESSING_ERROR_MESSAGE)).when(ordersKafkaConsumer).logMessageReceived(any(), any());
//        ordersKafkaConsumer.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC));
//        // Then
//        verify(ordersKafkaConsumer, times(1)).republishMessageToTopic(orderUriArgument.capture(),
//                currentTopicArgument.capture(), nextTopicArgument.capture());
//        assertEquals(ORDER_RECEIVED_URI, orderUriArgument.getValue());
//        assertEquals(ORDER_RECEIVED_TOPIC, currentTopicArgument.getValue());
//        assertEquals(ORDER_RECEIVED_TOPIC_RETRY, nextTopicArgument.getValue());
//    }

//    @Test
//    void republishMessageToErrorTopicRunsSuccessfully()
//            throws ExecutionException, InterruptedException, SerializationException {
//        // Given & When
//        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
//        when(serializer.toBinary(any())).thenReturn(new byte[4]);
//        ordersKafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI,
//                ORDER_RECEIVED_TOPIC_RETRY,
//                ORDER_RECEIVED_TOPIC_ERROR);
//        // Then
//        verify(messageProducer, times(1)).sendMessage(any());
//    }

    @Test
    void responseHandlerServiceUnavailableCalledWhenOrderServiceReturnsOk() {
        // Given
        when(orderProcessorService.processOrderReceived(any())).thenReturn(
                OrderProcessResponse.newBuilder()
                .withStatus(OrderProcessResponse.Status.OK)
                .build());

        // When
        ordersKafkaConsumer.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verify(orderProcessResponseHandler, times(1)).serviceOk(any());
    }

    @Test
    void responseHandlerServiceUnavailableCalledWhenOrderServiceReturnsUnavailable() {
        // Given
        when(orderProcessorService.processOrderReceived(any())).thenReturn(
                OrderProcessResponse.newBuilder()
                .withStatus(OrderProcessResponse.Status.SERVICE_UNAVAILABLE)
                .build());

        // When
        ordersKafkaConsumer.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verify(orderProcessResponseHandler, times(1)).serviceUnavailable(any());
    }

    @Test
    void responseHandlerServiceUnavailableCalledWhenOrderServiceReturnsError() {
        // Given
        when(orderProcessorService.processOrderReceived(any())).thenReturn(
                OrderProcessResponse.newBuilder()
                .withStatus(OrderProcessResponse.Status.SERVICE_ERROR)
                .build());

        // When
        ordersKafkaConsumer.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verify(orderProcessResponseHandler, times(1)).serviceError(any());
    }
}
