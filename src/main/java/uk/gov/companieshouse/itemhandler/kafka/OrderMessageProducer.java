package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMapWithAcknowledgedKafkaMessage;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

import java.util.Map;
import java.util.function.Consumer;
import org.apache.kafka.clients.producer.RecordMetadata;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

public class OrderMessageProducer {

    private final Logger logger;

    private final MessageSerialiserFactory<OrderReceived> messageSerialiserFactory;
    private final MessageProducer messageProducer;

    public OrderMessageProducer(MessageSerialiserFactory<OrderReceived> messageSerialiserFactory,
                                MessageProducer messageProducer,
                                Logger logger) {
        this.messageSerialiserFactory = messageSerialiserFactory;
        this.messageProducer = messageProducer;
        this.logger = logger;
    }

    public void sendMessage(OrderReceived payload, String topic) {

        Message message = messageSerialiserFactory.createMessage(payload, topic);
        Consumer<RecordMetadata> asyncResponseLogger = recordMetadata ->
                logOffsetFollowingSendIngOfMessage(payload.getOrderUri(), recordMetadata);
        messageProducer.sendMessage(message, asyncResponseLogger);
    }

    /**
     * Logs the order reference, topic, partition and offset for the item message produced to a Kafka topic.
     *
     * @param orderReference the order reference
     * @param recordMetadata the metadata for a record that has been acknowledged by the server for the message produced
     */
    private void logOffsetFollowingSendIngOfMessage(final String orderReference,
                                                    final RecordMetadata recordMetadata) {
        final Map<String, Object> logMapCallback = createLogMapWithAcknowledgedKafkaMessage(
                recordMetadata);
        logIfNotNull(logMapCallback, ORDER_REFERENCE_NUMBER, orderReference);
        logger.info("Message sent to Kafka topic", logMapCallback);
    }
}