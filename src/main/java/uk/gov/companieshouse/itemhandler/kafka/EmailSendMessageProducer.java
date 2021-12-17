package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMapWithAcknowledgedKafkaMessage;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

import java.util.Map;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

public class EmailSendMessageProducer {

    private static final Logger LOGGER = LoggingUtils.getLogger();
    private static final String EMAIL_SEND_TOPIC = "email-send";

    private final MessageSerialiserFactory emailSendAvroSerializer;
    private final MessageProducer messageProducer;

    public EmailSendMessageProducer(final MessageSerialiserFactory avroSerializer,
                                    final MessageProducer messageProducer) {
        this.emailSendAvroSerializer = avroSerializer;
        this.messageProducer = messageProducer;
    }

    /**
     * Sends an email-send message to the Kafka producer.
     * @param email EmailSend object encapsulating the message content
     */
    public void sendMessage(final EmailSend email, String orderReference) {
        Map<String, Object> logMap = LoggingUtils.logWithOrderReference(
                "Sending message to kafka",
                orderReference);
        final Message message = emailSendAvroSerializer.createMessage(email, EMAIL_SEND_TOPIC);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, message.getTopic());
        LoggingUtils.logMessageWithOrderReference(message,"Sending message to Kafka", orderReference);
        messageProducer.sendMessage(message, recordMetadata ->
                logOffsetFollowingSendIngOfMessage(orderReference, recordMetadata));
    }

    /**
     * Logs the order reference, topic, partition and offset for the item message produced to a Kafka topic.
     * @param orderReference the order reference
     * @param recordMetadata the metadata for a record that has been acknowledged by the server for the message produced
     */
    void logOffsetFollowingSendIngOfMessage(final String orderReference,
                                            final RecordMetadata recordMetadata) {
        final Map<String, Object> logMapCallback =  createLogMapWithAcknowledgedKafkaMessage(recordMetadata);
        logIfNotNull(logMapCallback, ORDER_REFERENCE_NUMBER, orderReference);
        LOGGER.info("Message sent to Kafka", logMapCallback);
    }
}
