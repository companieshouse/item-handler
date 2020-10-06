package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.email.OrderConfirmation;
import uk.gov.companieshouse.itemhandler.exception.ServiceException;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
// TODO GCI-1072 Remove? import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertifiedCopyOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToMissingImageDeliveryOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToItemOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.logging.Logger;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Communicates with <code>chs-email-sender</code> via the <code>send-email</code> Kafka topic to
 * trigger the sending of emails.
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private static final String CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.certificate-order-confirmation";
    private static final String CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "certificate_order_confirmation_email";
    private static final String CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.certified-copy-order-confirmation";
    private static final String CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "certified_copy_order_confirmation_email";
    private static final String MISSING_IMAGE_DELIVERY_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.missing-image-delivery-order-confirmation";
    private static final String MISSING_IMAGE_DELIVERY_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "missing_image_delivery_order_confirmation_email";
    private static final String ITEM_TYPE_CERTIFICATE = "certificate";
    private static final String ITEM_TYPE_CERTIFIED_COPY = "certified-copy";
    private static final String ITEM_TYPE_MISSING_IMAGE_DELIVERY = "missing-image-delivery";

    /**
     * This email address is supplied only to satisfy Avro contract.
     */
    private static final String TOKEN_EMAIL_ADDRESS = "chs-orders@ch.gov.uk";

    /** Convenient return type. */
    private static class OrderConfirmationAndEmail {
        private final OrderConfirmation confirmation;
        private final EmailSend email;

        public OrderConfirmationAndEmail(OrderConfirmation confirmation, EmailSend email) {
            this.confirmation = confirmation;
            this.email = email;
        }
    }

    private final OrderDataToCertificateOrderConfirmationMapper orderToCertificateOrderConfirmationMapper;
    // TODO GCI-1072 Remove? private final OrderDataToCertifiedCopyOrderConfirmationMapper orderToCertifiedCopyOrderConfirmationMapper;
    private final OrderDataToMissingImageDeliveryOrderConfirmationMapper orderDataToMissingImageDeliveryOrderConfirmationMapper;
    private final OrderDataToItemOrderConfirmationMapper orderToItemOrderConfirmationMapper;
    private final ObjectMapper objectMapper;
    private final EmailSendMessageProducer producer;

    @Value("${certificate.order.confirmation.recipient}")
    private String certificateOrderRecipient;
    @Value("${certified-copy.order.confirmation.recipient}")
    private String certifiedCopyOrderRecipient;
    @Value("${missing-image-delivery.order.confirmation.recipient}")
    private String missingImageDeliveryOrderRecipient;

    public EmailService(
            final OrderDataToCertificateOrderConfirmationMapper orderToConfirmationMapper,
            /* TODO GCI-1072 Remove? final OrderDataToCertifiedCopyOrderConfirmationMapper orderToCertifiedCopyOrderConfirmationMapper,*/
            final OrderDataToMissingImageDeliveryOrderConfirmationMapper orderDataToMissingImageDeliveryOrderConfirmationMapper,
            final OrderDataToItemOrderConfirmationMapper orderToItemOrderConfirmationMapper,
            final ObjectMapper objectMapper, final EmailSendMessageProducer producer) {
        this.orderToCertificateOrderConfirmationMapper = orderToConfirmationMapper;
        // TODO GCI-1072 Remove? this.orderToCertifiedCopyOrderConfirmationMapper = orderToCertifiedCopyOrderConfirmationMapper;
        this.orderDataToMissingImageDeliveryOrderConfirmationMapper
                 = orderDataToMissingImageDeliveryOrderConfirmationMapper;
        this.orderToItemOrderConfirmationMapper = orderToItemOrderConfirmationMapper;
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
        final OrderConfirmationAndEmail orderConfirmationAndEmail = buildOrderConfirmationAndEmail(order);
        final OrderConfirmation confirmation = orderConfirmationAndEmail.confirmation;
        final EmailSend email = orderConfirmationAndEmail.email;

        email.setEmailAddress(TOKEN_EMAIL_ADDRESS);
        email.setMessageId(UUID.randomUUID().toString());
        email.setData(objectMapper.writeValueAsString(confirmation));
        email.setCreatedAt(LocalDateTime.now().toString());

        String orderReference = confirmation.getOrderReferenceNumber();
        LoggingUtils.logWithOrderReference("Sending confirmation email for order", orderReference);
        producer.sendMessage(email, orderReference);
    }

    /**
     * Builds the order confirmation and email based on the order provided.
     * @param order the order for which an email confirmation is to be sent
     * @return a {@link OrderConfirmationAndEmail} holding both the confirmation and its email envelope
     */
    private OrderConfirmationAndEmail buildOrderConfirmationAndEmail(final OrderData order) {
        final String descriptionId = order.getItems().get(0).getDescriptionIdentifier();
        final EmailSend email = new EmailSend();
        final OrderConfirmation confirmation;
        switch (descriptionId) {
            case ITEM_TYPE_CERTIFICATE:
                confirmation = orderToCertificateOrderConfirmationMapper.orderToConfirmation(order);
                confirmation.setTo(certificateOrderRecipient);
                email.setAppId(CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID);
                email.setMessageType(CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
                return new OrderConfirmationAndEmail(confirmation, email);
            case ITEM_TYPE_CERTIFIED_COPY:
                // TODO GCI-1072 Which? confirmation = orderToCertifiedCopyOrderConfirmationMapper.orderToConfirmation(order);
                confirmation = orderToItemOrderConfirmationMapper.orderToConfirmation(order);
                confirmation.setTo(certifiedCopyOrderRecipient);
                email.setAppId(CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID);
                email.setMessageType(CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
                return new OrderConfirmationAndEmail(confirmation, email);
            case ITEM_TYPE_MISSING_IMAGE_DELIVERY:
                confirmation = orderToItemOrderConfirmationMapper.orderToConfirmation(order);
                confirmation.setTo(missingImageDeliveryOrderRecipient);
                email.setAppId(MISSING_IMAGE_DELIVERY_ORDER_NOTIFICATION_API_APP_ID);
                email.setMessageType(MISSING_IMAGE_DELIVERY_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
                return new OrderConfirmationAndEmail(confirmation, email);
            default:
                final Map<String, Object> logMap = LoggingUtils.createLogMapWithOrderReference(order.getReference());
                final String error = "Unable to determine order confirmation type from description ID " +
                        descriptionId + "!";
                LOGGER.error(error, logMap);
                throw new ServiceException(error);
        }
    }

}
