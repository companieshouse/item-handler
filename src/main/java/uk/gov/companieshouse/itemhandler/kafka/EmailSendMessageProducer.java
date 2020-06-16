package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class EmailSendMessageProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final EmailSendMessageFactory emailSendAvroSerializer;
    private final EmailSendKafkaProducer emailSendKafkaProducer;

    public EmailSendMessageProducer(final EmailSendMessageFactory avroSerializer, final EmailSendKafkaProducer kafkaMessageProducer) {
        this.emailSendAvroSerializer = avroSerializer;
        this.emailSendKafkaProducer = kafkaMessageProducer;
    }

    /**
     * Sends an email-send message to the Kafka producer.
     * @param email EmailSend object
     * @throws SerializationException should there be a failure to serialize the EmailSend object
     * @throws ExecutionException should something unexpected happen
     * @throws InterruptedException should something unexpected happen
     */
    public void sendMessage(final EmailSend email, String orderReference)
            throws SerializationException, ExecutionException, InterruptedException {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_REFERENCE_NUMBER, orderReference);
        LOGGER.info("Sending message to kafka producer", logMap);
        final Message message = emailSendAvroSerializer.createMessage(email, orderReference);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, message.getTopic());
        emailSendKafkaProducer.sendMessage(message, orderReference);
        LOGGER.info("Message sent to Kafka producer", logMap);
    }
}
