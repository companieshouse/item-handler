package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;

import java.util.concurrent.ExecutionException;

@Service
public class EmailSendKafkaProducer extends KafkaProducer  {

    /**
     * Sends message to Kafka topic
     * @param message message
     * @throws ExecutionException should something unexpected happen
     * @throws InterruptedException should something unexpected happen
     */
    public void sendMessage(final Message message, String orderReference)
            throws ExecutionException, InterruptedException {
        LoggingUtils.logMessageWithOrderReference(message, "Sending message to Kafka", orderReference);
        getChKafkaProducer().send(message);
    }

}
