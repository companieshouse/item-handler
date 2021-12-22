package uk.gov.companieshouse.itemhandler.kafka;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.logging.LogMessageBuilder;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import uk.gov.companieshouse.logging.Logger;

public final class MessageProducer {

    private final CHKafkaProducer kafkaProducer;
    private final Logger logger;

    public MessageProducer(CHKafkaProducer kafkaProducer, Logger logger) {
        this.kafkaProducer = kafkaProducer;
        this.logger = logger;
    }

    /**
     * Sends message to Kafka topic
     * @param message message
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void sendMessage(final Message message) throws ExecutionException, InterruptedException {
        LogMessageBuilder.builder(logger)
                .addContext(LoggingUtils.TOPIC, message.getTopic())
                .addContext(LoggingUtils.PARTITION, message.getPartition())
                .addContext(LoggingUtils.OFFSET, message.getOffset())
                .logDebug("Sending message to kafka");
        kafkaProducer.send(message);
    }

    /**
     * Publishes message to Kafka topic.
     *
     * @param message serialised message
     * @param callback to handle meta data returned by the Kafka broker
     *
     * @throws NonRetryableException when Kafka broker is unavailable or message can't be sent
     */
    public void sendMessage(final Message message, Consumer<RecordMetadata> callback) {
        try {
            final Future<RecordMetadata> recordMetadataFuture = kafkaProducer.sendAndReturnFuture(message);
            callback.accept(recordMetadataFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            String msg = String.format("Unexpected Kafka error: %s", e.getMessage());
            logger.error(msg, e);
            throw new NonRetryableException(msg);
        }
    }
}
