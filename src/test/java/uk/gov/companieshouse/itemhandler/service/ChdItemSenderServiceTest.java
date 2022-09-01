package uk.gov.companieshouse.itemhandler.service;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemPair;
import uk.gov.companieshouse.itemhandler.kafka.ItemMessageProducer;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

/**
 * Unit tests the {@link ChdItemSenderService} class.
 */
@ExtendWith(MockitoExtension.class)
class ChdItemSenderServiceTest {

    @InjectMocks
    private ChdItemSenderService serviceUnderTest;

    @Mock
    private ItemMessageProducer itemMessageProducer;

    @Mock
    private OrderData order;

    @Mock
    private Item item;

    @Mock
    private ItemGroup itemGroup;

    @Test
    @DisplayName("ChdItemSenderService delegates to ItemMessageProducer")
    void sendItemsToChdDelegatesToItemMessageProducer() {

        // Given
        when(order.getReference()).thenReturn(ORDER_REFERENCE);
        when(item.getId()).thenReturn(MISSING_IMAGE_DELIVERY_ITEM_ID);
        when(itemGroup.getItems()).thenReturn(Arrays.asList(item,item));
        when(itemGroup.getOrder()).thenReturn(order);

        // When
        serviceUnderTest.sendItemsToChd(itemGroup);

        // Then
        verify(order).getReference();
        verify(item, times(2)).getId();
        verify(itemMessageProducer, times(2)).sendMessage(new OrderItemPair(order, item));
    }
}
