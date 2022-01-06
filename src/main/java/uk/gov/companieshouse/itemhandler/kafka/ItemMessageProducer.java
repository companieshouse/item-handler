package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.COMPANY_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.PAYMENT_REFERENCE;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMap;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMapWithAcknowledgedKafkaMessage;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

import java.util.Map;
import org.apache.kafka.clients.producer.RecordMetadata;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

public class ItemMessageProducer {
    private static final Logger LOGGER = LoggingUtils.getLogger();
    private final ItemMessageFactory itemMessageFactory;
    private final MessageProducer messageProducer;

    public ItemMessageProducer(final ItemMessageFactory itemMessageFactory,
                               final MessageProducer messageProducer) {
        this.itemMessageFactory = itemMessageFactory;
        this.messageProducer = messageProducer;
    }

    /**
     * Sends (produces) a message to the Kafka <code>chd-item-ordered</code> topic representing the missing image
     * delivery item provided.
     * @param order the {@link OrderData} instance retrieved from the Orders API
     * @param orderReference the reference of the order to which the item belongs
     * @param itemId the ID of the item that the message to be sent represents
     */
    public void sendMessage(final OrderData order,
                            final String orderReference,
                            final String itemId) {

        final Map<String, Object> logMap = createLogMap();
        populateChdMessageLogMap(order, logMap);
        LOGGER.info("Sending message to kafka", logMap);

        final Message message = itemMessageFactory.createMessage(order);
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
        logIfNotNull(logMap, ITEM_ID, itemId);
        LOGGER.debug("Sending message to kafka", logMap);
        messageProducer.sendMessage(message,
                recordMetadata -> logOffsetFollowingSendIngOfMessage(order, recordMetadata));
    }

    /**
     * Logs the order reference, item ID, topic, partition and offset for the item message produced to a Kafka topic.
     * @param order the originating order
     * @param recordMetadata the metadata for a record that has been acknowledged by the server for the message produced
     */
    void logOffsetFollowingSendIngOfMessage(final OrderData order,
                                            final RecordMetadata recordMetadata) {
        final Map<String, Object> logMapCallback =  createLogMapWithAcknowledgedKafkaMessage(recordMetadata);
        populateChdMessageLogMap(order, logMapCallback);
        LOGGER.info("Message sent to Kafka topic", logMapCallback);
    }

    /**
     * Populates the log map provided with key values for tracking of outgoing messages bound for CHD.
     * @param order the originating order
     * @param logMap the log map to populate values form the order with
     */
    private void populateChdMessageLogMap(final OrderData order, final Map<String, Object> logMap) {
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, order.getReference());
        logIfNotNull(logMap, PAYMENT_REFERENCE, order.getPaymentReference());
        final Item firstItem = order.getItems().get(0);
        logIfNotNull(logMap, ITEM_ID, firstItem.getId());
        logIfNotNull(logMap, COMPANY_NUMBER, firstItem.getCompanyNumber());
    }
}
