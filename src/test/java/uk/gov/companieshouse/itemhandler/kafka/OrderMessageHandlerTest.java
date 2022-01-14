package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@ExtendWith(MockitoExtension.class)
class OrderMessageHandlerTest {

    @Mock
    private OrderProcessorService orderProcessorService;

    @Mock
    private OrderProcessResponseHandler orderProcessResponseHandler;

    @Spy
    private MessageFilter<OrderReceived> duplicateMessageFilter = new DuplicateMessageFilter(1);

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

    @InjectMocks
    private OrderMessageHandler orderMessageHandler;

    @Test
    void shouldHandleOneMessage() {
        //given
        OrderProcessResponse orderProcessResponse = OrderProcessResponse.newBuilder().withStatus(
                        OrderProcessResponse.Status.OK)
                .withOrderUri("/order/ORD-111111-111111").build();
        Message<OrderReceived> message = new Message<OrderReceived>() {
            @Override
            public @NotNull OrderReceived getPayload() {
                OrderReceived orderReceived = new OrderReceived();
                orderReceived.setAttempt(0);
                orderReceived.setOrderUri("/order/ORD-111111-111111");
                return orderReceived;
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
        when(orderProcessorService.processOrderReceived(any())).thenReturn(orderProcessResponse);

        //when
        orderMessageHandler.handleMessage(message);

        //then
        verify(orderProcessorService, times(1)).processOrderReceived(any());
    }

    @Test
    void shouldNotHandleDuplicateMessage() {
        //given
        OrderProcessResponse orderProcessResponse = OrderProcessResponse.newBuilder().withStatus(
                        OrderProcessResponse.Status.OK)
                .withOrderUri("/order/ORD-111111-111111").build();
        Message<OrderReceived> message = new Message<OrderReceived>() {
            @Override
            public @NotNull OrderReceived getPayload() {
                OrderReceived orderReceived = new OrderReceived();
                orderReceived.setAttempt(0);
                orderReceived.setOrderUri("/order/ORD-111111-111111");
                return orderReceived;
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };

        when(orderProcessorService.processOrderReceived(any())).thenReturn(orderProcessResponse);

        //when
        // Two identical messages
        for (int i = 0; i < 2; ++i) {
            orderMessageHandler.handleMessage(message);
        }

        //then
        verify(orderProcessorService, times(1)).processOrderReceived(any());
        verify(logger).debug(argumentCaptor.capture(), anyMap());
        assertEquals("'order-received' message is a duplicate", argumentCaptor.getValue());
    }

    @Test
    void shouldHandleDifferentMessages() {
        //given
        OrderProcessResponse orderProcessResponse1 = OrderProcessResponse.newBuilder().withStatus(
                        OrderProcessResponse.Status.OK)
                .withOrderUri("/order/ORD-111111-111111").build();
        OrderProcessResponse orderProcessResponse2 = OrderProcessResponse.newBuilder().withStatus(
                        OrderProcessResponse.Status.OK)
                .withOrderUri("/order/ORD-111111-222222").build();
        Message<OrderReceived> message1 = new Message<OrderReceived>() {
            @Override
            public @NotNull OrderReceived getPayload() {
                OrderReceived orderReceived = new OrderReceived();
                orderReceived.setAttempt(0);
                orderReceived.setOrderUri("/order/ORD-111111-111111");
                return orderReceived;
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
        Message<OrderReceived> message2 = new Message<OrderReceived>() {
            @Override
            public @NotNull OrderReceived getPayload() {
                OrderReceived orderReceived = new OrderReceived();
                orderReceived.setAttempt(0);
                orderReceived.setOrderUri("/order/ORD-111111-222222");
                return orderReceived;
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
        when(orderProcessorService.processOrderReceived(message1.getPayload().getOrderUri())).thenReturn(orderProcessResponse1);
        when(orderProcessorService.processOrderReceived(message2.getPayload().getOrderUri())).thenReturn(orderProcessResponse2);

        //when
        orderMessageHandler.handleMessage(message1);
        orderMessageHandler.handleMessage(message2);

        //then
        verify(orderProcessorService, times(2)).processOrderReceived(any());
    }
}