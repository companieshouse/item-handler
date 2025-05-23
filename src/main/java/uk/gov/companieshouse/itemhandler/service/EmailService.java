package uk.gov.companieshouse.itemhandler.service;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.client.EmailClient;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.itemsummary.ConfirmationMapperFactory;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.itemsummary.EmailMetadata;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.logging.Logger;

/**
 * Communicates with <code>chs-email-sender</code> via the <code>send-email</code> Kafka topic to
 * trigger the sending of emails.
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private static final String DEFAULT_APPLICATION_ID = "item-handler";
    private static final String ITEM_KIND_CERTIFIED_COPY = "item#certified-copy";
    private static final String ITEM_KIND_CERTIFICATE = "item#certificate";

    /**
     * This email address is supplied only to satisfy Avro contract.
     */
    public static final String TOKEN_EMAIL_ADDRESS = "chs-orders@ch.gov.uk";

    private final ObjectMapper objectMapper;
    private final ConfirmationMapperFactory confirmationMapperFactory;
    private final EmailClient emailClient;

    public EmailService(
            final ObjectMapper objectMapper,
            final ConfirmationMapperFactory confirmationMapperFactory,
            final EmailClient emailClient) {
        this.objectMapper = objectMapper;
        this.confirmationMapperFactory = confirmationMapperFactory;
        this.emailClient = emailClient;
    }

    /**
     * Sends out a certificate or certified copy order confirmation email.
     *
     * @param itemGroup a {@link DeliverableItemGroup group of deliverable items}.
     */
    public void sendOrderConfirmation(final DeliverableItemGroup itemGroup) {
        LOGGER.trace(format("sendOrderConfirmation(%s) method called.", itemGroup));

        try {
            EmailSend emailSend;
            if (ITEM_KIND_CERTIFIED_COPY.equals(itemGroup.getKind())) {
                emailSend = mapEmailSend(itemGroup, confirmationMapperFactory.getCertifiedCopyMapper());
            } else if (ITEM_KIND_CERTIFICATE.equals(itemGroup.getKind())) {
                emailSend = mapEmailSend(itemGroup, confirmationMapperFactory.getCertificateMapper());
            } else {
                throw new NonRetryableException(format("Unknown item kind: [%s]", itemGroup.getKind()));
            }

            String orderReference = itemGroup.getOrder().getReference();
            LoggingUtils.logWithOrderReference("Sending confirmation email for order", orderReference);

            emailClient.sendEmail(emailSend);

        } catch (JsonProcessingException exception) {
            String msg = format("Error converting order (%s) confirmation to JSON", itemGroup.getOrder().getReference());
            LOGGER.error(msg, exception);
            throw new NonRetryableException(msg);
        }
    }

    private EmailSend mapEmailSend(DeliverableItemGroup itemGroup, OrderConfirmationMapper<?> mapper) throws JsonProcessingException {
        LOGGER.trace(format("mapEmailSend(itemGroup=%s, mapperClass=%s) method called.",
                itemGroup, mapper.getClass().getSimpleName()));

        EmailMetadata<?> emailMetadata = mapper.map(itemGroup);

        String applicationId = ofNullable(emailMetadata.getAppId()).orElse("");

        EmailSend emailSend = new EmailSend();
        emailSend.setAppId(isNotBlank(applicationId) ? applicationId : DEFAULT_APPLICATION_ID);
        emailSend.setMessageType(emailMetadata.getMessageType());
        emailSend.setMessageId(UUID.randomUUID().toString());
        emailSend.setData(objectMapper.writeValueAsString(emailMetadata.getEmailData()));
        emailSend.setEmailAddress(TOKEN_EMAIL_ADDRESS);
        emailSend.setCreatedAt(LocalDateTime.now().toString());

        LOGGER.info(format("EmailSend: %s", objectMapper.writeValueAsString(emailSend)));

        return emailSend;
    }
}