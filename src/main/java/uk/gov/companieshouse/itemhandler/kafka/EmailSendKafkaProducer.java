package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.exception.SendKafkaMessageException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import uk.gov.companieshouse.logging.Logger;

@Service
public class EmailSendKafkaProducer extends KafkaProducer  {
    private static final Logger LOGGER = LoggingUtils.getLogger();

    /**
     * Sends message to Kafka topic
     * @param message certificate or certified copy order message to be produced to the <code>email-send</code> topic
     * @param orderReference the reference of the order
     * @param asyncResponseLogger RecordMetadata {@link Consumer} that can be implemented to allow the logging of
     *                            the offset once the message has been produced
     * @throws SendKafkaMessageException if Kafka is unavailable
     * @throws NonRetryableException if an unexpected Kafka error occurs
     */
    public void sendMessage(final Message message,
                            final String orderReference,
                            final Consumer<RecordMetadata> asyncResponseLogger) {
        LoggingUtils.logMessageWithOrderReference(message, "Sending message to Kafka", orderReference);

        try {
            final Future<RecordMetadata> recordMetadataFuture = getChKafkaProducer().sendAndReturnFuture(message);
            asyncResponseLogger.accept(recordMetadataFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            Throwable cause = e.getCause();
            // TODO: determine if Kafka broker unavailable
            if (1 == 1) {
                // TODO: if Kafka broker unavailable throw RetryableException
                String msg = String.format("Kafka broker unavailable %s", e.getMessage());
                LOGGER.error(msg, e);
                throw new SendKafkaMessageException(msg);
            } else {
                String msg = String.format("Unexpected Kafka error %s", e.getMessage());
                LOGGER.error(msg, e);
                throw new NonRetryableException(msg);
            }
        }
    }
}