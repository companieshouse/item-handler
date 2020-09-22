package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.KafkaMessagingException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.OFFSET;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_URI;
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
     * TODO GCI-1428 Javadoc this
     * @param orderReference
     * @param itemId
     * @param item
     */
    public void sendMessage(final String orderReference,
                            final String itemId,
                            final Item item) {

        final Map<String, Object> logMap = LoggingUtils.createLogMap();
        logIfNotNull(logMap, ORDER_URI, orderReference);
        logIfNotNull(logMap, ITEM_ID, itemId);
        LOGGER.info("Sending message to kafka producer", logMap);

        try {
            final Message message = itemMessageFactory.createMessage(item);
            itemKafkaProducer.sendMessage(orderReference, itemId, message, recordMetadata -> {
                final long offset = recordMetadata.offset();
                final String topic = message.getTopic();
                final Map<String, Object> logMapCallback = new HashMap<>();
                logMapCallback.put(LoggingUtils.TOPIC, topic);
                logMapCallback.put(ORDER_REFERENCE_NUMBER, orderReference);
                logMapCallback.put(ITEM_ID, itemId);
                logMapCallback.put(OFFSET, offset);
                LOGGER.info("Message sent to Kafka topic", logMapCallback);
            });
        } catch (Exception e) {
            final String errorMessage = String.format(
                    "Kafka item message could not be sent for order URI %s item ID %s", orderReference, itemId);
            logMap.put(LoggingUtils.EXCEPTION, e);
            LOGGER.error(errorMessage, logMap);
            throw new KafkaMessagingException(errorMessage, e);
        }
    }
}
