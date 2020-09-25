package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.KafkaMessagingException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.util.Map;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.OFFSET;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.TOPIC;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

@Service
public class ItemMessageProducer {
    private static final Logger LOGGER = LoggingUtils.getLogger();
    private final ItemMessageFactory itemMessageFactory;
    private final ItemKafkaProducer itemKafkaProducer;

    public ItemMessageProducer(final ItemMessageFactory itemMessageFactory,
                               final ItemKafkaProducer itemKafkaProducer) {
        this.itemMessageFactory = itemMessageFactory;
        this.itemKafkaProducer = itemKafkaProducer;
    }

    /**
     * Sends (produces) a message to the Kafka <code>chd-item-ordered</code> topic representing the missing image
     * delivery item provided.
     * @param orderReference the reference of the order to which the item belongs
     * @param itemId the ID of the item that the message to be sent represents
     * @param item the missing image delivery item
     */
    public void sendMessage(final String orderReference,
                            final String itemId,
                            final Item item) {

        final Map<String, Object> logMap = LoggingUtils.createLogMap();
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
        logIfNotNull(logMap, ITEM_ID, itemId);
        LOGGER.info("Sending message to kafka producer", logMap);

        try {
            final Message message = itemMessageFactory.createMessage(item);
            itemKafkaProducer.sendMessage(orderReference, itemId, message,
                    recordMetadata ->
                            logOffsetFollowingSendIngOfMessage(orderReference, itemId, message, recordMetadata));
        } catch (Exception e) {
            final String errorMessage = String.format(
                    "Kafka item message could not be sent for order reference %s item ID %s", orderReference, itemId);
            logMap.put(LoggingUtils.EXCEPTION, e);
            LOGGER.error(errorMessage, logMap);
            throw new KafkaMessagingException(errorMessage, e);
        }
    }

    /**
     * Logs the order reference, item ID and offset for the item message produced to a Kafka topic.
     * @param orderReference the order reference
     * @param itemId the item ID
     * @param message the item message
     * @param recordMetadata the metadata for a record that has been acknowledged by the server
     */
    void logOffsetFollowingSendIngOfMessage(final String orderReference,
                                            final String itemId,
                                            final Message message,
                                            final RecordMetadata recordMetadata) {
        final long offset = recordMetadata.offset();
        final String topic = message.getTopic();
        final Map<String, Object> logMapCallback = LoggingUtils.createLogMap();
        logIfNotNull(logMapCallback, TOPIC, topic);
        logIfNotNull(logMapCallback, ORDER_REFERENCE_NUMBER, orderReference);
        logIfNotNull(logMapCallback, ITEM_ID, itemId);
        logIfNotNull(logMapCallback, OFFSET, offset);
        LOGGER.info("Message sent to Kafka topic", logMapCallback);
    }
}
