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
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemPair;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
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
     * @param orderItemPair the {@link OrderItemPair} of order and MID order item the from orders API
     */
    public void sendMessage(final OrderItemPair orderItemPair) {
        final Map<String, Object> logMap = createLogMap();
        OrderData order = orderItemPair.getOrder();
        populateChdMessageLogMap(orderItemPair, logMap);
        LOGGER.info("Sending message to kafka", logMap);

        final Message message = itemMessageFactory.createMessage(orderItemPair);
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, order.getReference());
        logIfNotNull(logMap, ITEM_ID, orderItemPair.getItem().getId());
        LOGGER.debug("Sending message to kafka", logMap);
        messageProducer.sendMessage(message,
                recordMetadata -> logOffsetFollowingSendIngOfMessage(orderItemPair, recordMetadata));
    }

    /**
     * Logs the order reference, item ID, topic, partition and offset for the item message produced to a Kafka topic.
     * @param orderItemPair the pairing originating order and MID order item
     * @param recordMetadata the metadata for a record that has been acknowledged by the server for the message produced
     */
    void logOffsetFollowingSendIngOfMessage(final OrderItemPair orderItemPair,
                                            final RecordMetadata recordMetadata) {
        final Map<String, Object> logMapCallback =  createLogMapWithAcknowledgedKafkaMessage(recordMetadata);
        populateChdMessageLogMap(orderItemPair, logMapCallback);
        LOGGER.info("Message sent to Kafka topic", logMapCallback);
    }

    /**
     * Populates the log map provided with key values for tracking of outgoing messages bound for CHD.
     * @param orderItemPair the pairing originating order and MID order item
     * @param logMap the log map to populate values form the order with
     */
    private void populateChdMessageLogMap(final OrderItemPair orderItemPair, final Map<String, Object> logMap) {
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderItemPair.getOrder().getReference());
        logIfNotNull(logMap, PAYMENT_REFERENCE, orderItemPair.getOrder().getPaymentReference());
        logIfNotNull(logMap, ITEM_ID, orderItemPair.getItem().getId());
        logIfNotNull(logMap, COMPANY_NUMBER, orderItemPair.getItem().getCompanyNumber());
    }
}
