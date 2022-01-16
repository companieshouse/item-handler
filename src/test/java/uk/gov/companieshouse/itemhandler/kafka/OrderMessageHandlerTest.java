package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private MessageFilter<OrderReceived> messageFilter;

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

    @Mock
    private Message<OrderReceived> message;

    @Mock
    private OrderReceived orderReceived;

    @Mock
    private MessageHeaders messageHeaders;

    @Mock
    private OrderProcessResponse orderProcessResponse;

    @Mock
    private OrderProcessResponse.Status status;

    @InjectMocks
    private OrderMessageHandler orderMessageHandler;

    @Test
    void shouldHandleMessageWhenFilterIncludesMessage() {
        //given
        when(message.getHeaders()).thenReturn(messageHeaders);
        when(message.getPayload()).thenReturn(orderReceived);
        when(orderProcessResponse.getStatus()).thenReturn(status);
        when(orderProcessorService.processOrderReceived(any())).thenReturn(orderProcessResponse);
        doNothing().when(status).accept(any(), any());
        when(messageFilter.include(any())).thenReturn(true);

        //when
        orderMessageHandler.handleMessage(message);

        //then
        verify(orderProcessorService, times(1)).processOrderReceived(any());
    }

    @Test
    void shouldNotHandleMessageWhenFilterExcludesMessage() {
        //given
        when(message.getHeaders()).thenReturn(messageHeaders);
        when(message.getPayload()).thenReturn(orderReceived);
        when(messageFilter.include(any())).thenReturn(false);

        //when
        orderMessageHandler.handleMessage(message);

        //then
        verify(orderProcessorService, times(0)).processOrderReceived(any());
        verify(logger).debug(argumentCaptor.capture(), anyMap());
        assertEquals("'order-received' message is a duplicate", argumentCaptor.getValue());
    }
}