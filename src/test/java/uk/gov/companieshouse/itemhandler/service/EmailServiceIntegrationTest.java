package uk.gov.companieshouse.itemhandler.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.model.DeliveryTimescale.STANDARD;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import uk.gov.companieshouse.itemhandler.config.FeatureOptions;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.email.ItemOrderConfirmation;
import uk.gov.companieshouse.itemhandler.itemsummary.ConfirmationMapperFactory;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToItemOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

/** Integration tests the {@link EmailService} service. */
@SpringBootTest(classes = {EmailService.class})
@EmbeddedKafka
class EmailServiceIntegrationTest {

    private final static String ITEM_TYPE_CERTIFICATE = "certificate";
    private final static String ITEM_TYPE_CERTIFIED_COPY = "certified-copy";
    private final static String ITEM_TYPE_MISSING_IMAGE_DELIVERY = "missing-image-delivery";

    @Autowired
    private EmailService emailServiceUnderTest;

    @MockBean
    private OrderDataToCertificateOrderConfirmationMapper orderToCertificateConfirmationMapper;

    @MockBean
    private OrderDataToItemOrderConfirmationMapper orderToItemOrderConfirmationMapper;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private EmailSendMessageProducer producer;

    @MockBean
    private OrderData order;

    @MockBean
    private FeatureOptions featureOptions;

    @MockBean
    private ConfirmationMapperFactory confirmationMapperFactory;

    @MockBean
    private List<Item> items;

    @MockBean
    private Item item;

    @MockBean
    private CertificateOrderConfirmation certificateOrderConfirmation;

    @MockBean
    private ItemOrderConfirmation itemOrderConfirmation;

    @MockBean
    private DeliveryItemOptions deliveryItemOptions;

    /*@Test
    @DisplayName("EmailService sets the to line on the confirmation to the configured " +
            "certificate.order.confirmation.recipient value")
    void usesConfiguredRecipientValueForCertificate() throws Exception {

        // Given
        when(orderToCertificateConfirmationMapper.orderToConfirmation(order, featureOptions)).thenReturn(certificateOrderConfirmation);

        // When
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFICATE);
        when(((DeliveryItemOptions) item.getItemOptions())).thenReturn(deliveryItemOptions);
        when(deliveryItemOptions.getDeliveryTimescale()).thenReturn(STANDARD);
        emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certificate", STANDARD));

        // Then
        verify(certificateOrderConfirmation).setTo("certificate-handler@nowhere.com");
    }*/

    @Test
    @DisplayName("EmailService sets the to line on the confirmation to the configured " +
            "certified-copy.order.confirmation.recipient value")
    void usesConfiguredRecipientValueForCertifiedCopy() throws Exception {

        // Given
        when(orderToItemOrderConfirmationMapper.orderToConfirmation(order)).thenReturn(itemOrderConfirmation);

        // When
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFIED_COPY);
        when(((DeliveryItemOptions) item.getItemOptions())).thenReturn(deliveryItemOptions);
        when(deliveryItemOptions.getDeliveryTimescale()).thenReturn(STANDARD);
        emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certified-copy", STANDARD));

        // Then
        verify(itemOrderConfirmation).setTo("certified-copy-handler@nowhere.com");
    }

    @Test
    @DisplayName("EmailService sets the to line on the confirmation to the configured " +
        "missing-image-delivery.order.confirmation.recipient value")
    void usesConfiguredRecipientValueForMissingImageDelivery() throws Exception {

        // Given
        when(orderToItemOrderConfirmationMapper.orderToConfirmation(order)).
                thenReturn(itemOrderConfirmation);

        // When
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_MISSING_IMAGE_DELIVERY);
        when(((DeliveryItemOptions) item.getItemOptions())).thenReturn(deliveryItemOptions);
        when(deliveryItemOptions.getDeliveryTimescale()).thenReturn(STANDARD);
        emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certified-copy", STANDARD));

        // Then
        verify(itemOrderConfirmation).setTo("certified-copy-handler@nowhere.com");
    }

}
