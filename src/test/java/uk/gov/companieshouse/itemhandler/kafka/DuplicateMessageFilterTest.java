package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@ExtendWith(MockitoExtension.class)
class DuplicateMessageFilterTest {
    @Mock
    private MessageHeaders messageHeaders;

    @Mock
    private Message<OrderReceived> message;

    @Mock
    private OrderReceived orderReceived;

    @Mock
    private Message<OrderReceived> message2;

    @Mock
    private OrderReceived orderReceived2;

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

    private DuplicateMessageFilter duplicateMessageFilter;

    @BeforeEach
    void beforeEach() {
        duplicateMessageFilter = new DuplicateMessageFilter(1, logger);
    }

    @Test
    void shouldReturnTrueWhenOrderReceivedForTheFirstTime() {
        //given
        when(orderReceived.getAttempt()).thenReturn(0);
        when(orderReceived.getOrderUri()).thenReturn("/order/ORD-111111-111111");
        when(message.getPayload()).thenReturn(orderReceived);

        //when
        boolean result = duplicateMessageFilter.include(message);

        //then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenOrderReceivedIsADuplicate() {
        //given
        when(orderReceived.getAttempt()).thenReturn(0);
        when(orderReceived.getOrderUri()).thenReturn("/order/ORD-111111-111111");
        when(message.getHeaders()).thenReturn(messageHeaders);
        when(message.getPayload()).thenReturn(orderReceived);

        //when
        boolean result1 = duplicateMessageFilter.include(message);
        // Message 1 again - SHOULD NOT be allowed
        boolean result2 = duplicateMessageFilter.include(message);

        //then
        assertTrue(result1);
        assertFalse(result2);
        verify(logger).debug(argumentCaptor.capture(), anyMap());
        assertEquals("'order-received' message is a duplicate", argumentCaptor.getValue());
    }

    @Test
    void shouldAgeOutOrderReceivedWhenLruCacheIsFull() {
        //given
        when(orderReceived.getAttempt()).thenReturn(1);
        when(orderReceived.getOrderUri()).thenReturn("/order/ORD-111111-111111");
        when(message.getPayload()).thenReturn(orderReceived);

        when(orderReceived2.getAttempt()).thenReturn(0);
        when(orderReceived2.getOrderUri()).thenReturn("/order/ORD-111111-222222");
        when(message2.getPayload()).thenReturn(orderReceived2);

        //when
        boolean result1 = duplicateMessageFilter.include(message);
        // Message 2 should age out message one
        boolean result2 = duplicateMessageFilter.include(message2);
        // Message 1 again - SHOULD be allowed
        boolean result3 = duplicateMessageFilter.include(message);

        //then
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
    }
}