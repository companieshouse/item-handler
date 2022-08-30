package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.config.FeatureOptions;
import uk.gov.companieshouse.itemhandler.email.OrderConfirmation;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.itemsummary.ConfirmationMapperFactory;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToItemOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.logging.Logger;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Communicates with <code>chs-email-sender</code> via the <code>send-email</code> Kafka topic to
 * trigger the sending of emails.
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    public static final String CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.certificate-order-confirmation";
    public static final String CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "certificate_order_confirmation_email";
    public static final String SAME_DAY_CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.same-day-certificate-order-confirmation";
    public static final String SAME_DAY_CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "same_day_certificate_order_confirmation_email";
    public static final String CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.certified-copy-order-confirmation";
    public static final String CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "certified_copy_order_confirmation_email";
    public static final String SAME_DAY_CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.same-day-certified-copy-order-confirmation";
    public static final String SAME_DAY_CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "same_day_certified_copy_order_confirmation_email";
    public static final String MISSING_IMAGE_DELIVERY_ORDER_NOTIFICATION_API_APP_ID =
            "item-handler.missing-image-delivery-order-confirmation";
    public static final String MISSING_IMAGE_DELIVERY_ORDER_NOTIFICATION_API_MESSAGE_TYPE =
            "missing_image_delivery_order_confirmation_email";
    public static final String ITEM_TYPE_CERTIFICATE = "certificate";
    public static final String ITEM_TYPE_CERTIFIED_COPY = "certified-copy";
    public static final String ITEM_TYPE_MISSING_IMAGE_DELIVERY = "missing-image-delivery";
    public static final String STANDARD_DELIVERY = "standard";

    /**
     * This email address is supplied only to satisfy Avro contract.
     */
    public static final String TOKEN_EMAIL_ADDRESS = "chs-orders@ch.gov.uk";

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
    private final OrderDataToItemOrderConfirmationMapper orderToItemOrderConfirmationMapper;
    private final ObjectMapper objectMapper;
    private final EmailSendMessageProducer emailSendProducer;
    private final FeatureOptions featureOptions;
    private final ConfirmationMapperFactory confirmationMapperFactory;

    @Value("${certificate.order.confirmation.recipient}")
    private String certificateOrderRecipient;
    @Value("${certified-copy.order.confirmation.recipient}")
    private String certifiedCopyOrderRecipient;
    @Value("${missing-image-delivery.order.confirmation.recipient}")
    private String missingImageDeliveryOrderRecipient;

    public EmailService(
            final OrderDataToCertificateOrderConfirmationMapper orderToConfirmationMapper,
            final OrderDataToItemOrderConfirmationMapper orderToItemOrderConfirmationMapper,
            final ObjectMapper objectMapper, final EmailSendMessageProducer emailSendProducer,
            final FeatureOptions featureOptions, final ConfirmationMapperFactory confirmationMapperFactory) {
        this.orderToCertificateOrderConfirmationMapper = orderToConfirmationMapper;
        this.orderToItemOrderConfirmationMapper = orderToItemOrderConfirmationMapper;
        this.objectMapper = objectMapper;
        this.emailSendProducer = emailSendProducer;
        this.featureOptions = featureOptions;
        this.confirmationMapperFactory = confirmationMapperFactory;
    }

    /**
     * Sends out a certificate or certified copy order confirmation email.
     *
     * @param itemGroup a {@link DeliverableItemGroup group of deliverable items}.
     */
    public void sendOrderConfirmation(DeliverableItemGroup itemGroup) {
        try {
            // TODO: replace with call to abstract factory returning OrderConfirmationMapper parameterised by item kind
            if ("item#certified-copy".equals(itemGroup.getKind())) {
                final OrderConfirmationAndEmail orderConfirmationAndEmail = buildOrderConfirmationAndEmail(itemGroup.getOrder());
                final OrderConfirmation confirmation = orderConfirmationAndEmail.confirmation;
                final EmailSend email = orderConfirmationAndEmail.email;
                email.setEmailAddress(TOKEN_EMAIL_ADDRESS); // replace with noreply@companieshouse.gov.uk
                email.setMessageId(UUID.randomUUID().toString());
                email.setData(objectMapper.writeValueAsString(confirmation));
                email.setCreatedAt(LocalDateTime.now().toString());

                String orderReference = confirmation.getOrderReferenceNumber();
                LoggingUtils.logWithOrderReference("Sending confirmation email for order", orderReference);
                emailSendProducer.sendMessage(email, orderReference);
            } else {
                confirmationMapperFactory.getMapper(itemGroup);
            }

        } catch (JsonProcessingException exception) {
            String msg = String.format("Error converting order (%s) confirmation to JSON", itemGroup.getOrder().getReference());
            LOGGER.error(msg, exception);
            throw new NonRetryableException(msg);
        }
    }

    /**
     * Builds the order confirmation and email based on the order provided.
     * @param order the order for which an email confirmation is to be sent
     * @return a {@link OrderConfirmationAndEmail} holding both the confirmation and its email envelope
     */
    private OrderConfirmationAndEmail buildOrderConfirmationAndEmail(final OrderData order) {
        final String descriptionId = order.getItems().get(0).getDescriptionIdentifier();
        final String deliveryTimescale = ((DeliveryItemOptions) order.getItems().get(0).getItemOptions()).getDeliveryTimescale().getJsonName();
        final EmailSend email = new EmailSend();
        final OrderConfirmation confirmation;
        switch (descriptionId) {
            case ITEM_TYPE_CERTIFICATE:
                confirmation = orderToCertificateOrderConfirmationMapper.orderToConfirmation(order, featureOptions);
                confirmation.setTo(certificateOrderRecipient);
                email.setAppId(deliveryTimescale.equals(STANDARD_DELIVERY) ? CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID : SAME_DAY_CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID);
                email.setMessageType(deliveryTimescale.equals(STANDARD_DELIVERY) ? CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE : SAME_DAY_CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
                return new OrderConfirmationAndEmail(confirmation, email);
            case ITEM_TYPE_CERTIFIED_COPY:
                confirmation = orderToItemOrderConfirmationMapper.orderToConfirmation(order);
                confirmation.setTo(certifiedCopyOrderRecipient);
                email.setAppId(deliveryTimescale.equals(STANDARD_DELIVERY) ?
                        CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID :
                        SAME_DAY_CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID);
                email.setMessageType(deliveryTimescale.equals(STANDARD_DELIVERY) ?
                        CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE :
                        SAME_DAY_CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE);
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
                throw new NonRetryableException(error);
        }
    }

}
