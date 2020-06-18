package uk.gov.companieshouse.itemhandler.kafka;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;

@Service
public class EmailSendMessageProducer {

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
        Map<String, Object> logMap = LoggingUtils.logWithOrderReference("Sending message to kafka producer", orderReference);
        final Message message = emailSendAvroSerializer.createMessage(email, orderReference);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, message.getTopic());
        emailSendKafkaProducer.sendMessage(message, orderReference);
        LoggingUtils.getLogger().info("Message sent to Kafka producer", logMap);
    }
}
