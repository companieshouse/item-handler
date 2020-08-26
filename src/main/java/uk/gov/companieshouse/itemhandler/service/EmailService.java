package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.email.OrderConfirmation;
import uk.gov.companieshouse.itemhandler.exception.OrderMappingException;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertifiedCopyOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Communicates with <code>chs-email-sender</code> via the ` <code>send-email</code> Kafka topic to
 * trigger the sending of emails.
 */
@Service
public class EmailService {

    private static final String CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.certificate-order-confirmation";
    private static final String CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "certificate_order_confirmation_email";
    private static final String CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.certified-copy-order-confirmation";
    private static final String CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "certified_copy_order_confirmation_email";
    private static final String ITEM_TYPE_CERTIFICATE = "certificate";
    private static final String ITEM_TYPE_CERTIFIED_COPY = "certified-copy";
    /**
     * This email address is supplied only to satisfy Avro contract.
     */
    private static final String TOKEN_EMAIL_ADDRESS = "chs-orders@ch.gov.uk";

    private final OrderDataToCertificateOrderConfirmationMapper orderToCertificateOrderConfirmationMapper;
    private final OrderDataToCertifiedCopyOrderConfirmationMapper orderToCertifiedCopyOrderConfirmationMapper;
    private final ObjectMapper objectMapper;
    private final EmailSendMessageProducer producer;

    @Value("${certificate.order.confirmation.recipient}")
    private String certificateOrderRecipient;
    @Value("${certified-copy.order.confirmation.recipient}")
    private String certifiedCopyOrderRecipient;

    public EmailService(
            final OrderDataToCertificateOrderConfirmationMapper orderToConfirmationMapper,
            final OrderDataToCertifiedCopyOrderConfirmationMapper orderToCertifiedCopyOrderConfirmationMapper,
            final ObjectMapper objectMapper, final EmailSendMessageProducer producer) {
        this.orderToCertificateOrderConfirmationMapper = orderToConfirmationMapper;
        this.orderToCertifiedCopyOrderConfirmationMapper = orderToCertifiedCopyOrderConfirmationMapper;
        this.objectMapper = objectMapper;
        this.producer = producer;
    }

    /**
     * Sends out a certificate order confirmation email.
     *
     * @param order the order information used to compose the order confirmation email.
     * @throws JsonProcessingException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws SerializationException
     */
    public void sendOrderConfirmation(final OrderData order)
            throws JsonProcessingException, InterruptedException, ExecutionException, SerializationException {
        String descriptionId = order.getItems().get(0).getDescriptionIdentifier();
        OrderConfirmation confirmation = getOrderConfirmation(order);
        if (confirmation == null) {
            String errorMessage = String.format("Failed to map order with reference %s to order confirmation.",
                    order.getReference());
            LoggingUtils.getLogger().error(errorMessage);
            throw new OrderMappingException(errorMessage);
        }
        final EmailSend email = new EmailSend();

        if (descriptionId.equals(ITEM_TYPE_CERTIFICATE)) {
            confirmation.setTo(certificateOrderRecipient);
            email.setAppId(CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID);
            email.setMessageType(CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
        }
        else if (descriptionId.equals(ITEM_TYPE_CERTIFIED_COPY)) {
            confirmation.setTo(certifiedCopyOrderRecipient);
            email.setAppId(CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID);
            email.setMessageType(CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
        }

        email.setEmailAddress(TOKEN_EMAIL_ADDRESS);
        email.setMessageId(UUID.randomUUID().toString());
        email.setData(objectMapper.writeValueAsString(confirmation));
        email.setCreatedAt(LocalDateTime.now().toString());

        String orderReference = confirmation.getOrderReferenceNumber();
        LoggingUtils.logWithOrderReference("Sending confirmation email for order", orderReference);
        producer.sendMessage(email, orderReference);
    }

    private OrderConfirmation getOrderConfirmation(OrderData orderData) {
        String descriptionId = orderData.getItems().get(0).getDescriptionIdentifier();
        if (descriptionId.equals(ITEM_TYPE_CERTIFICATE)) {
            return orderToCertificateOrderConfirmationMapper.orderToConfirmation(orderData);
        }
        else if (descriptionId.equals(ITEM_TYPE_CERTIFIED_COPY)) {
            return orderToCertifiedCopyOrderConfirmationMapper.orderToConfirmation(orderData);
        }
        return null;
    }
}
