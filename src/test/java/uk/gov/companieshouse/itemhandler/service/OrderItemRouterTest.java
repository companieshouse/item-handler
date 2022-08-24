package uk.gov.companieshouse.itemhandler.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.order.item.CertificateItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.DeliveryTimescaleApi;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
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

    @Test
    @DisplayName("test certified cert standard delivery route to email service")
    void testRouteToEmailServiceCert() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        Item certSameDay = getExpectedItem("item#certificate", DeliveryTimescale.SAME_DAY);
        when(order.getItems()).thenReturn(Arrays.asList(cert, certSameDay));

        // when
        orderItemRouter.route(order);

        // then
        verify(emailService).sendOrderConfirmation(deliverableItemGroup);
        verify(deliverableItemGroup).setKind("item#certificate");
        verify(deliverableItemGroup).setTimescale(DeliveryTimescale.STANDARD);
    }

    @Test
    @DisplayName("test certified cert standard delivery route to email service")
    void testRouteToEmailServiceCopy() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        Item copy = getExpectedItem("item#certified-copy", DeliveryTimescale.STANDARD);
        Item certSameDay = getExpectedItem("item#certificate", DeliveryTimescale.SAME_DAY);
        Item copySameDay = getExpectedItem("item#certified-copy", DeliveryTimescale.SAME_DAY);
        when(order.getItems()).thenReturn(Arrays.asList(cert, copy, certSameDay, copySameDay));


        // when
        orderItemRouter.route(order);

        // then
        verify(emailService).sendOrderConfirmation(deliverableItemGroup);
    }

    @Test
    @DisplayName("test certified cert standard delivery route to email service")
    void testRouteToEmailServiceCertSameDay() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        Item copy = getExpectedItem("item#certified-copy", DeliveryTimescale.STANDARD);
        Item certSameDay = getExpectedItem("item#certificate", DeliveryTimescale.SAME_DAY);
        Item copySameDay = getExpectedItem("item#certified-copy", DeliveryTimescale.SAME_DAY);
        when(order.getItems()).thenReturn(Arrays.asList(cert, copy, certSameDay, copySameDay));


        // when
        orderItemRouter.route(order);

        // then
        verify(emailService).sendOrderConfirmation(deliverableItemGroup);
    }

    @Test
    @DisplayName("test certified cert standard delivery route to email service")
    void testRouteToEmailServiceCopySameDay() {
        // given
        Item cert = getExpectedItem("item#certificate", DeliveryTimescale.STANDARD);
        Item copy = getExpectedItem("item#certified-copy", DeliveryTimescale.STANDARD);
        Item certSameDay = getExpectedItem("item#certificate", DeliveryTimescale.SAME_DAY);
        Item copySameDay = getExpectedItem("item#certified-copy", DeliveryTimescale.SAME_DAY);
        when(order.getItems()).thenReturn(Arrays.asList(cert, copy, certSameDay, copySameDay));


        // when
        orderItemRouter.route(order);

        // then
        verify(emailService).sendOrderConfirmation(deliverableItemGroup);
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
