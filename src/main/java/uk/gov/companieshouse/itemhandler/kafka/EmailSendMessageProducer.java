package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

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
    public void sendMessage(final EmailSend email)
            throws SerializationException, ExecutionException, InterruptedException {
        LOGGER.trace("Sending message to kafka producer");
        final Message message = emailSendAvroSerializer.createMessage(email);
        emailSendKafkaProducer.sendMessage(message);
    }
}
