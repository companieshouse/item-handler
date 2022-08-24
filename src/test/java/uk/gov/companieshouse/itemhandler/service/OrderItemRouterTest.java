package uk.gov.companieshouse.itemhandler.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@ExtendWith(MockitoExtension.class)
public class OrderItemRouterTest {

    @InjectMocks
    private OrderItemRouter orderItemRouter;

    @Mock
    private EmailService emailService;

    @Mock
    private ChdItemSenderService chdItemSenderService;

    @Mock
    private OrderData order;

    @Mock
    private ItemGroup itemGroup;

    @Mock
    private DeliverableItemGroup deliverableItemGroup;

    @Captor
    private ArgumentCaptor<DeliverableItemGroup> itemGroupCaptor;

    @Test
    @DisplayName("test certified cert standard delivery route to email service")
    void testRouteToEmailServiceCert() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        Item certSameDay = getExpectedItem("item#certificate", DeliveryTimescale.SAME_DAY);
        Item copy = getExpectedItem("item#certified-copy", DeliveryTimescale.STANDARD);
        Item copySameDay = getExpectedItem("item#certified-copy", DeliveryTimescale.SAME_DAY);
        List<Item> items = Arrays.asList(cert, copySameDay, certSameDay, copy);
        Collections.shuffle(items);
        when(order.getItems()).thenReturn(items);

        // when
        orderItemRouter.route(order);

        // then
        verify(emailService, times(4)).sendOrderConfirmation(itemGroupCaptor.capture());
        List<DeliverableItemGroup> capturedValues = itemGroupCaptor.getAllValues();
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certificate", DeliveryTimescale.STANDARD, new ArrayList<>(Collections.singletonList(cert)))));
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certificate", DeliveryTimescale.SAME_DAY, new ArrayList<>(Collections.singletonList(certSameDay)))));
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certified-copy", DeliveryTimescale.STANDARD, new ArrayList<>(Collections.singletonList(copy)))));
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certified-copy", DeliveryTimescale.SAME_DAY, new ArrayList<>(Collections.singletonList(copySameDay)))));
    }

    private Item getExpectedItem(String kind, DeliveryTimescale timescale) {
        Item item = new Item();
        item.setKind(kind);

        DeliveryItemOptions itemOptions = new DeliveryItemOptions();
        itemOptions.setDeliveryTimescale(timescale);
        item.setItemOptions(itemOptions);
        return item;
    }
}
