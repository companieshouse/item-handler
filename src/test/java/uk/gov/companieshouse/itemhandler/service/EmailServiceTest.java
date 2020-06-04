package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    @Test
    void sendsCertificateOrderConfirmation() throws Exception {

        // Given
        final LocalDateTime intervalStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        when(orderToConfirmationMapper.orderToConfirmation(order)).thenReturn(confirmation);
        when(objectMapper.writeValueAsString(confirmation)).thenReturn(EMAIL_CONTENT);
        when(confirmation.getOrderReferenceNumber()).thenReturn("123");

        // When
        emailServiceUnderTest.sendCertificateOrderConfirmation(order);
        final LocalDateTime intervalEnd = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusNanos(1000000);

        // Then
        verify(producer).sendMessage(emailCaptor.capture());
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.certificate-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("certificate_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        verifyCreationTimestampWithinExecutionInterval(emailSent, intervalStart, intervalEnd);
    }

    /**
     * Verifies that the email created at timestamp is within the expected interval
     * for the creation.
     * @param emailSent the email
     */
    private void verifyCreationTimestampWithinExecutionInterval(final EmailSend emailSent,
                                                                final LocalDateTime intervalStart,
                                                                final LocalDateTime intervalEnd) {
        final LocalDateTime createdAt = LocalDateTime.parse(emailSent.getCreatedAt());
        assertThat(createdAt.isAfter(intervalStart) || createdAt.isEqual(intervalStart), is(true));
        assertThat(createdAt.isBefore(intervalEnd) || createdAt.isEqual(intervalEnd), is(true));
    }

}
