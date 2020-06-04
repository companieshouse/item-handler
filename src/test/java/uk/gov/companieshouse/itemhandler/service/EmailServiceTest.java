package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.util.TimestampedEntity;
import uk.gov.companieshouse.itemhandler.util.TimestampedEntityVerifier;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests the {@link EmailService} class. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String EMAIL_CONTENT = "Hi there!";

    @InjectMocks
    private EmailService emailServiceUnderTest;

    @Mock
    private OrderDataToCertificateOrderConfirmationMapper orderToConfirmationMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmailSendMessageProducer producer;

    @Mock
    private OrderData order;

    @Mock
    private CertificateOrderConfirmation confirmation;

    @Captor
    ArgumentCaptor<EmailSend> emailCaptor;

    private TimestampedEntityVerifier timestamps;

    @BeforeEach
    void setUp() {
        timestamps = new TimestampedEntityVerifier();
    }


    /** Implements {@link uk.gov.companieshouse.itemhandler.util.TimestampedEntity} to make
     * {@link EmailSend#getCreatedAt()} accessible to a {@link TimestampedEntityVerifier}.
     */
    private static class EmailSendTimestampedEntity implements TimestampedEntity {

        private EmailSend email;

        private EmailSendTimestampedEntity(EmailSend email) {
            this.email = email;
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return LocalDateTime.parse(email.getCreatedAt());
        }

        @Override
        public LocalDateTime getUpdatedAt() {
            return null; // not used here
        }
    }


    @Test
    void sendsCertificateOrderConfirmation() throws Exception {

        timestamps.start();

        // Given
        when(orderToConfirmationMapper.orderToConfirmation(order)).thenReturn(confirmation);
        when(objectMapper.writeValueAsString(confirmation)).thenReturn(EMAIL_CONTENT);
        when(confirmation.getOrderReferenceNumber()).thenReturn("123");

        // When
        emailServiceUnderTest.sendCertificateOrderConfirmation(order);

        timestamps.end();

        // Then
        verify(producer).sendMessage(emailCaptor.capture());
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.certificate-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("certificate_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        final EmailSendTimestampedEntity emailSendTimestampedEntity = new EmailSendTimestampedEntity(emailSent);
        timestamps.verifyCreationTimestampsWithinExecutionInterval(emailSendTimestampedEntity);
    }

}
