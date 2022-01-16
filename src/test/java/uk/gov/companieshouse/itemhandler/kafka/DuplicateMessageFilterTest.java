package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.orders.OrderReceived;

@ExtendWith(MockitoExtension.class)
class DuplicateMessageFilterTest {
    @Mock
    private Message<OrderReceived> message1;

    @Mock
    private OrderReceived orderReceived1;

    @Mock
    private Message<OrderReceived> message2;

    @Mock
    private OrderReceived orderReceived2;

    private DuplicateMessageFilter duplicateMessageFilter;

    @BeforeEach
    void beforeEach() {
        duplicateMessageFilter = new DuplicateMessageFilter(1);
    }

    @Test
    void shouldReturnTrueWhenOrderReceivedForTheFirstTime() {
        //given
        when(orderReceived1.getAttempt()).thenReturn(0);
        when(orderReceived1.getOrderUri()).thenReturn("/order/ORD-111111-111111");
        when(message1.getPayload()).thenReturn(orderReceived1);

        //when
        boolean result = duplicateMessageFilter.include(message1);

        //then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenOrderReceivedIsADuplicate() {
        //given
        when(orderReceived1.getAttempt()).thenReturn(0);
        when(orderReceived1.getOrderUri()).thenReturn("/order/ORD-111111-111111");
        when(message1.getPayload()).thenReturn(orderReceived1);

        //when
        boolean result1 = duplicateMessageFilter.include(message1);
        // Message 1 again - SHOULD NOT be allowed
        boolean result2 = duplicateMessageFilter.include(message1);

        //then
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    void shouldAgeOutOrderReceivedWhenLruCacheIsFull() {
        //given
        when(orderReceived1.getAttempt()).thenReturn(1);
        when(orderReceived1.getOrderUri()).thenReturn("/order/ORD-111111-111111");
        when(message1.getPayload()).thenReturn(orderReceived1);

        when(orderReceived2.getAttempt()).thenReturn(0);
        when(orderReceived2.getOrderUri()).thenReturn("/order/ORD-111111-222222");
        when(message2.getPayload()).thenReturn(orderReceived2);

        //when
        boolean result1 = duplicateMessageFilter.include(message1);
        // Message 2 should age out message one
        boolean result2 = duplicateMessageFilter.include(message2);
        // Message 1 again - SHOULD be allowed
        boolean result3 = duplicateMessageFilter.include(message1);

        //then
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
    }
}