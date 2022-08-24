package uk.gov.companieshouse.itemhandler.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.gov.companieshouse.itemhandler.kafka.ItemMessageProducer;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

/**
 * Unit tests the {@link ChdItemSenderService} class.
 */
@RunWith(PowerMockRunner.class)
@ExtendWith(MockitoExtension.class)
@PrepareForTest(LoggingUtils.class)
@SuppressWarnings("squid:S5786") // public class access modifier required for JUnit 4 test
public class ChdItemSenderServiceTest {

    @InjectMocks
    private ChdItemSenderService serviceUnderTest;

    @Mock
    private ItemMessageProducer itemMessageProducer;

    @Mock
    private OrderData order;

    @Mock
    private List<Item> items;

    @Mock
    private Item item;

    @Test
    @DisplayName("ChdItemSenderService delegates to ItemMessageProducer")
    void sendItemsToChdDelegatesToItemMessageProducer() {

        // Given
        when(order.getReference()).thenReturn(ORDER_REFERENCE);
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getId()).thenReturn(MISSING_IMAGE_DELIVERY_ITEM_ID);

        // When
        serviceUnderTest.sendItemsToChd(null);

        // Then
        verify(order).getReference();
        verify(item).getId();
        verify(itemMessageProducer).sendMessage(order, ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID);

    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @org.junit.Test
    public void sendItemsToChdLogsOrderReference() {

        // Given
        final LoggingUtils logger = mock(LoggingUtils.class);
        mockStatic(LoggingUtils.class);
        when(order.getReference()).thenReturn(ORDER_REFERENCE);
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getId()).thenReturn(MISSING_IMAGE_DELIVERY_ITEM_ID);

        // When
        serviceUnderTest.sendItemsToChd(null);

        // Then
        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logWithOrderReference("Sending items for order to CHD", ORDER_REFERENCE);

    }
}
