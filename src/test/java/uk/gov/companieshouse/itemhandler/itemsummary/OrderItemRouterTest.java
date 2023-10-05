package uk.gov.companieshouse.itemhandler.itemsummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.MissingImageDeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.ChdItemSenderService;
import uk.gov.companieshouse.itemhandler.service.EmailService;

@ExtendWith(MockitoExtension.class)
class OrderItemRouterTest {

    @InjectMocks
    private OrderItemRouter orderItemRouter;

    @Mock
    private EmailService emailService;

    @Mock
    private ChdItemSenderService chdItemSenderService;

    @Mock
    private OrderData order;

    @Captor
    private ArgumentCaptor<DeliverableItemGroup> deliverableItemGroupCaptor;

    @Captor
    private ArgumentCaptor<ItemGroup> itemGroupCaptor;

    @Test
    @DisplayName("Router should route deliverable items to email service and missing image deliveries to chd item sender service")
    void testRouteToAllRequiredServices() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        Item certSameDay = getExpectedItem("item#certificate", DeliveryTimescale.SAME_DAY);
        Item copy = getExpectedItem("item#certified-copy", DeliveryTimescale.STANDARD);
        Item copySameDay = getExpectedItem("item#certified-copy", DeliveryTimescale.SAME_DAY);
        Item missingImageDelivery = getMissingImageDelivery();
        List<Item> items = Arrays.asList(cert, cert, copySameDay, copySameDay, certSameDay, certSameDay, copy, copy, missingImageDelivery, missingImageDelivery);
        Collections.shuffle(items);
        when(order.getItems()).thenReturn(items);

        // when
        orderItemRouter.route(order);

        // then
        verify(emailService, times(4)).sendOrderConfirmation(deliverableItemGroupCaptor.capture());
        verify(chdItemSenderService).sendItemsToChd(itemGroupCaptor.capture());
        List<DeliverableItemGroup> capturedValues = deliverableItemGroupCaptor.getAllValues();
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certificate", DeliveryTimescale.STANDARD, new ArrayList<>(Arrays.asList(cert, cert)))));
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certificate", DeliveryTimescale.SAME_DAY, new ArrayList<>(Arrays.asList(certSameDay, certSameDay)))));
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certified-copy", DeliveryTimescale.STANDARD, new ArrayList<>(Arrays.asList(copy, copy)))));
        assertTrue(capturedValues.contains(new DeliverableItemGroup(order, "item#certified-copy", DeliveryTimescale.SAME_DAY, new ArrayList<>(Arrays.asList(copySameDay, copySameDay)))));
        assertEquals(new ItemGroup(order, "item#missing-image-delivery", new ArrayList<>(Arrays.asList(missingImageDelivery, missingImageDelivery))), itemGroupCaptor.getValue());
    }

    @Test
    @DisplayName("Router should not send items to chd when there are no missing image delivery items")
    void testOrderContainsNoMissingImageDeliveryItems() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        when(order.getItems()).thenReturn(Collections.singletonList(cert));

        // when
        orderItemRouter.route(order);

        // then
        verify(emailService, times(1)).sendOrderConfirmation(deliverableItemGroupCaptor.capture());
        verifyNoInteractions(chdItemSenderService);
        assertEquals(new DeliverableItemGroup(order, "item#certificate", DeliveryTimescale.STANDARD, new ArrayList<>(Collections.singletonList(cert))), deliverableItemGroupCaptor.getValue());
    }

    @Test
    @DisplayName("Router should not send order confirmations when there are no deliverable items")
    void testOrderContainsNoDeliverableItems() {
        // given
        Item missingImageDelivery = getMissingImageDelivery();
        when(order.getItems()).thenReturn(Collections.singletonList(missingImageDelivery));

        // when
        orderItemRouter.route(order);

        // then
        verify(chdItemSenderService).sendItemsToChd(itemGroupCaptor.capture());
        verifyNoInteractions(emailService);
        assertEquals(new ItemGroup(order, "item#missing-image-delivery", new ArrayList<>(Collections.singletonList(missingImageDelivery))), itemGroupCaptor.getValue());
    }

    @Test
    @DisplayName("Router should not send order confirmations when order only contains digital items")
    void testOrderContainsOnlyDigitalItems() {
        // given
        final Item digitalCertificate = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        digitalCertificate.setPostalDelivery(false);
        final Item digitalCopy = getExpectedItem("item#certified-copy", DeliveryTimescale.STANDARD);
        digitalCopy.setPostalDelivery(false);
        when(order.getItems()).thenReturn(Arrays.asList(digitalCertificate, digitalCopy));

        // when
        orderItemRouter.route(order);

        // then
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Router should throw a non retryable exception when delivery timescale is null")
    void testOrderWithNullDeliveryTimescale() {
        // given
        Item cert = getExpectedItem("item#certificate", null);
        cert.setId("CRT-123123-123123");
        when(order.getItems()).thenReturn(Collections.singletonList(cert));

        // when
        Executable actual = () -> orderItemRouter.route(order);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, actual);
        assertEquals("Item [CRT-123123-123123] is missing a delivery timescale", exception.getMessage());
        verifyNoInteractions(emailService);
        verifyNoInteractions(chdItemSenderService);
    }

    private Item getMissingImageDelivery() {
        Item item = new Item();
        item.setKind("item#missing-image-delivery");

        item.setItemOptions(new MissingImageDeliveryItemOptions());
        item.setPostalDelivery(false);
        return item;
    }

    private Item getExpectedItem(String kind, DeliveryTimescale timescale) {
        Item item = new Item();
        item.setKind(kind);

        DeliveryItemOptions itemOptions = new DeliveryItemOptions();
        itemOptions.setDeliveryTimescale(timescale);
        item.setItemOptions(itemOptions);
        item.setPostalDelivery(true);
        return item;
    }
}
