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
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Integration tests the {@link EmailService} service. */
@SpringBootTest
@EmbeddedKafka
@TestPropertySource(properties="certificate.order.confirmation.recipient = nobody@nowhere.com")
public class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailServiceUnderTest;

    @MockBean
    private OrderDataToCertificateOrderConfirmationMapper orderToConfirmationMapper;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private EmailSendMessageProducer producer;

    @MockBean
    private OrderData order;

    @MockBean
    private CertificateOrderConfirmation confirmation;

    @Test
    @DisplayName("EmailService sets the to line on the confirmation to the configured certificate.order.confirmation.recipient value")
    void usesConfiguredRecipientsValue() throws Exception {

        // Given
        when(orderToConfirmationMapper.orderToConfirmation(order)).thenReturn(confirmation);

        // When
        emailServiceUnderTest.sendCertificateOrderConfirmation(order);

        // Then
        verify(confirmation).setTo("nobody@nowhere.com");
    }

}
