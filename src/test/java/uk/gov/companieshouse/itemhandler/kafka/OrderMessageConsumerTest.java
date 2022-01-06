package uk.gov.companieshouse.itemhandler.kafka;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
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
    private OrderMessageHandler orderMessageHandler;
    @Mock
    private OrderProcessorService orderProcessorService;
    @Mock
    private OrderProcessResponseHandler orderProcessResponseHandler;

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

    @Test
    void responseHandlerServiceUnavailableCalledWhenOrderServiceReturnsOk() {
        // Given
        when(orderProcessorService.processOrderReceived(any())).thenReturn(
                OrderProcessResponse.newBuilder()
                .withStatus(OrderProcessResponse.Status.OK)
                .build());

        // When
        orderMessageHandler.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC_RETRY));

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
        orderMessageHandler.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC_RETRY));

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
        orderMessageHandler.handleMessage(createTestMessage(ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verify(orderProcessResponseHandler, times(1)).serviceError(any());
    }
}
