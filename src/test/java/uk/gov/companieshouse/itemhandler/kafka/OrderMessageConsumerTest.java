package uk.gov.companieshouse.itemhandler.kafka;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMessageConsumerTest {
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_KEY = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";

    @Mock
    private OrderProcessorService orderProcessorService;
    @Mock
    private OrderProcessResponseHandler orderProcessResponseHandler;
    @Mock
    private Logger logger;
    @InjectMocks
    private OrderMessageHandler orderMessageHandler;

    private static org.springframework.messaging.Message<OrderReceived> createTestMessage() {
        return new org.springframework.messaging.Message<OrderReceived>() {
            @Override
            public @NotNull OrderReceived getPayload() {
                OrderReceived orderReceived = new OrderReceived();
                orderReceived.setOrderUri(ORDER_RECEIVED_URI);
                return orderReceived;
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                Map<String, Object> headerItems = new HashMap<>();
                headerItems.put("kafka_receivedTopic", OrderMessageConsumerTest.ORDER_RECEIVED_TOPIC_RETRY);
                headerItems.put("kafka_offset", 0);
                headerItems.put("kafka_receivedMessageKey", ORDER_RECEIVED_KEY);
                headerItems.put("kafka_receivedPartitionId", 0);
                return new MessageHeaders(headerItems);
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
        orderMessageHandler.handleMessage(createTestMessage());

        // Then
        verify(orderProcessResponseHandler).serviceOk(any());
    }

    @Test
    void responseHandlerServiceUnavailableCalledWhenOrderServiceReturnsUnavailable() {
        // Given
        when(orderProcessorService.processOrderReceived(any())).thenReturn(
                OrderProcessResponse.newBuilder()
                .withStatus(OrderProcessResponse.Status.SERVICE_UNAVAILABLE)
                .build());

        // When
        orderMessageHandler.handleMessage(createTestMessage());

        // Then
        verify(orderProcessResponseHandler).serviceUnavailable(any());
    }

    @Test
    void responseHandlerServiceUnavailableCalledWhenOrderServiceReturnsError() {
        // Given
        when(orderProcessorService.processOrderReceived(any())).thenReturn(
                OrderProcessResponse.newBuilder()
                .withStatus(OrderProcessResponse.Status.SERVICE_ERROR)
                .build());

        // When
        orderMessageHandler.handleMessage(createTestMessage());

        // Then
        verify(orderProcessResponseHandler).serviceError(any());
    }
}