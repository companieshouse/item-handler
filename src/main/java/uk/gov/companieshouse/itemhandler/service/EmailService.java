package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.itemsummary.CertificateEmailData;
import uk.gov.companieshouse.itemhandler.itemsummary.CertifiedCopyEmailData;
import uk.gov.companieshouse.itemhandler.itemsummary.ConfirmationMapperFactory;
import uk.gov.companieshouse.itemhandler.itemsummary.EmailMetadata;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.logging.Logger;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Communicates with <code>chs-email-sender</code> via the <code>send-email</code> Kafka topic to
 * trigger the sending of emails.
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggingUtils.getLogger();
    private static final String ITEM_KIND_CERTIFIED_COPY = "item#certified-copy";

    /**
     * This email address is supplied only to satisfy Avro contract.
     */
    public static final String TOKEN_EMAIL_ADDRESS = "chs-orders@ch.gov.uk";

    private final ObjectMapper objectMapper;
    private final EmailSendMessageProducer emailSendProducer;
    private final ConfirmationMapperFactory confirmationMapperFactory;

    public EmailService(
            final ObjectMapper objectMapper, final EmailSendMessageProducer emailSendProducer,
            final ConfirmationMapperFactory confirmationMapperFactory) {
        this.objectMapper = objectMapper;
        this.emailSendProducer = emailSendProducer;
        this.confirmationMapperFactory = confirmationMapperFactory;
    }

    /**
     * Sends out a certificate or certified copy order confirmation email.
     *
     * @param itemGroup a {@link DeliverableItemGroup group of deliverable items}.
     */
    public void sendOrderConfirmation(DeliverableItemGroup itemGroup) {
        try {
            EmailSend emailSend = createEmailSendBasedOnKind(itemGroup);
            emailSend.setEmailAddress(TOKEN_EMAIL_ADDRESS);
            emailSend.setMessageId(UUID.randomUUID().toString());
            emailSend.setCreatedAt(LocalDateTime.now().toString());

            String orderReference = itemGroup.getOrder().getReference();
            LoggingUtils.logWithOrderReference("Sending confirmation email for order", orderReference);
            emailSendProducer.sendMessage(emailSend, orderReference);
        } catch (JsonProcessingException exception) {
            String msg = String.format("Error converting order (%s) confirmation to JSON", itemGroup.getOrder().getReference());
            LOGGER.error(msg, exception);
            throw new NonRetryableException(msg);
        }
    }

    private EmailSend createEmailSendBasedOnKind(DeliverableItemGroup itemGroup) throws JsonProcessingException {
        EmailSend emailSend = new EmailSend();
        if (ITEM_KIND_CERTIFIED_COPY.equals(itemGroup.getKind())) {
            EmailMetadata<CertifiedCopyEmailData> emailMetadata = confirmationMapperFactory.getCertifiedCopyMapper().map(itemGroup);
            emailSend.setAppId(emailMetadata.getAppId());
            emailSend.setMessageType(emailMetadata.getMessageType());
            emailSend.setData(objectMapper.writeValueAsString(emailMetadata.getEmailData()));
        } else {
            EmailMetadata<CertificateEmailData> emailMetadata = confirmationMapperFactory.getCertificateMapper().map(itemGroup);
            emailSend.setAppId(emailMetadata.getAppId());
            emailSend.setMessageType(emailMetadata.getMessageType());
            emailSend.setData(objectMapper.writeValueAsString(emailMetadata.getEmailData()));
        }
        return emailSend;
    }
}
