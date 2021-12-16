package uk.gov.companieshouse.itemhandler.kafka;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import uk.gov.companieshouse.logging.Logger;

@Service
final class MessageProducer extends KafkaProducer {
    private static final Logger LOGGER = LoggingUtils.getLogger();

    /**
     * Sends message to Kafka topic
     * @param message message
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void sendMessage(final Message message) throws ExecutionException, InterruptedException {
        Map<String, Object> logMap = LoggingUtils.createLogMapWithKafkaMessage(message);
        LoggingUtils.getLogger().info("Sending message to kafka", logMap);
        getChKafkaProducer().send(message);
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
            final Future<RecordMetadata> recordMetadataFuture = getChKafkaProducer().sendAndReturnFuture(message);
            callback.accept(recordMetadataFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            String msg = String.format("Unexpected Kafka error: %s", e.getMessage());
            LOGGER.error(msg, e);
            throw new NonRetryableException(msg);
        }
    }

    @Override
    protected void modifyProducerConfig(final ProducerConfig producerConfig) {
        producerConfig.setRequestTimeoutMilliseconds(3000);
    }
}
