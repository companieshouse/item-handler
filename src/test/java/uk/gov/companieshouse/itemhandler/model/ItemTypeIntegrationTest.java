package uk.gov.companieshouse.itemhandler.model;

import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.itemhandler.model.ItemType.CERTIFICATE;
import static uk.gov.companieshouse.itemhandler.model.ItemType.CERTIFIED_COPY;
import static uk.gov.companieshouse.itemhandler.model.ItemType.MISSING_IMAGE_DELIVERY;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.itemhandler.service.ChdItemSenderService;
import uk.gov.companieshouse.itemhandler.service.EmailService;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;

/**
 * Integration tests the {@link ItemType} enum.
 */
@SpringBootTest
class ItemTypeIntegrationTest {

    private static final String UNKNOWN_KIND = "item#unknown";

    @MockBean
    private EmailService emailer;

    @MockBean
    private ChdItemSenderService itemSender;

    @Mock
    private OrderData order;

    @Configuration
    @ComponentScan(basePackageClasses = ItemTypeIntegrationTest.class)
    static class Config { }

    @Test
    @DisplayName("Gets the correct item type for any known kind")
    void getItemTypeGetsExpectedItemType() {
        stream(ItemType.values()).forEach(type->
                assertThat(ItemType.getItemType(type.getKind()), is(type)));
    }

    @Test
    @DisplayName("Gets nothing for an unknown kind")
    void getItemTypeGetsNothingForUnknownKind() {
        assertThat(ItemType.getItemType(UNKNOWN_KIND), is(nullValue()));
    }

    @Test
    @DisplayName("Certificate order sent to email")
    void certificateOrderSentToEmail() throws Exception {

        OrderData orderData = new OrderData();
        Item certificate = new Item();
        DeliveryItemOptions itemOptions = new CertificateItemOptions();
        certificate.setItemOptions(itemOptions);
        itemOptions.setDeliveryTimescale(DeliveryTimescale.STANDARD);
        certificate.setKind("item#certificate");
        orderData.setItems(Collections.singletonList(certificate));

        // When
        CERTIFICATE.sendMessages(orderData);

        // Then
        verify(emailer).sendOrderConfirmation(new DeliverableItemGroup(orderData, "item#certificate", DeliveryTimescale.STANDARD, Collections.singletonList(certificate)));
    }

    @Test
    @DisplayName("Certified copy order sent to email")
    void certifiedCopyOrderSentToEmail() throws Exception {

        OrderData orderData = new OrderData();
        Item certCopy = new Item();
        DeliveryItemOptions itemOptions = new CertificateItemOptions();
        certCopy.setItemOptions(itemOptions);
        itemOptions.setDeliveryTimescale(DeliveryTimescale.SAME_DAY);
        certCopy.setKind("item#certified-copy");
        orderData.setItems(Collections.singletonList(certCopy));

        // When
        CERTIFIED_COPY.sendMessages(orderData);

        // Then
        verify(emailer).sendOrderConfirmation(new DeliverableItemGroup(orderData, "item#certified-copy", DeliveryTimescale.SAME_DAY, Collections.singletonList(certCopy)));
    }

    @Test
    @DisplayName("Missing image delivery order sent to CHD")
    void missingImageDeliveryOrderSentToChd() throws Exception {

        // When
        MISSING_IMAGE_DELIVERY.sendMessages(order);

        // Then
        verify(itemSender).sendItemsToChd(new ItemGroup(order, "item#missing-image-delivery"));
    }
}