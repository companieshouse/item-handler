package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.email.ItemOrderConfirmation;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToItemOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Integration tests the {@link EmailService} service. */
@SpringBootTest
@EmbeddedKafka
@TestPropertySource(properties={ "certificate.order.confirmation.recipient = certificate-handler@nowhere.com",
                                "certified-copy.order.confirmation.recipient = certified-copy-handler@nowhere.com" })
public class EmailServiceIntegrationTest {

    private final static String ITEM_TYPE_CERTIFICATE = "certificate";
    private final static String ITEM_TYPE_CERTIFIED_COPY = "certified-copy";

    @Autowired
    private EmailService emailServiceUnderTest;

    @MockBean
    private OrderDataToCertificateOrderConfirmationMapper orderToCertificateConfirmationMapper;

    @MockBean
    private OrderDataToItemOrderConfirmationMapper orderToCertifiedCopyConfirmationMapper;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private EmailSendMessageProducer producer;

    @MockBean
    private OrderData order;

    @MockBean
    private List<Item> items;

    @MockBean
    private Item item;

    @MockBean
    private CertificateOrderConfirmation certificateOrderConfirmation;

    @MockBean
    private ItemOrderConfirmation itemOrderConfirmation;

    @Test
    @DisplayName("EmailService sets the to line on the confirmation to the configured " +
            "certificate.order.confirmation.recipient value")
    void usesConfiguredRecipientValueForCertificate() throws Exception {

        // Given
        when(orderToCertificateConfirmationMapper.orderToConfirmation(order)).thenReturn(certificateOrderConfirmation);

        // When
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFICATE);
        emailServiceUnderTest.sendOrderConfirmation(order);

        // Then
        verify(certificateOrderConfirmation).setTo("certificate-handler@nowhere.com");
    }

    @Test
    @DisplayName("EmailService sets the to line on the confirmation to the configured " +
            "certified-copy.order.confirmation.recipient value")
    void usesConfiguredRecipientValueForCertifiedCopy() throws Exception {

        // Given
        when(orderToCertifiedCopyConfirmationMapper.orderToConfirmation(order)).thenReturn(itemOrderConfirmation);

        // When
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFIED_COPY);
        emailServiceUnderTest.sendOrderConfirmation(order);

        // Then
        verify(itemOrderConfirmation).setTo("certified-copy-handler@nowhere.com");
    }
}
