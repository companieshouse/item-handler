package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.logging.Logger;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMap;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

@Service
public class ItemKafkaProducer extends KafkaProducer {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    /**
     * Sends (produces) message to the Kafka <code>chd-item-ordered</code> topic.
     * @param orderReference the reference of the order to which the item belongs
     * @param itemId the ID of the item that the message to be sent represents
     * @param message missing image delivery item message to be produced to the <code>chd-item-ordered</code> topic
     * @param asyncResponseLogger RecordMetadata {@link Consumer} that can be implemented to allow the logging of
     *                            the offset once the message has been produced
     * @throws ExecutionException should the production of the message to the topic error for some reason
     * @throws InterruptedException should the execution thread be interrupted
     */
    @Async
    public void sendMessage(final String orderReference,
                            final String itemId,
                            final Message message,
                            final Consumer<RecordMetadata> asyncResponseLogger)
            throws ExecutionException, InterruptedException {

        final Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
        logIfNotNull(logMap, ITEM_ID, itemId);
        LOGGER.info("Sending message to kafka topic", logMap);

        final Future<RecordMetadata> recordMetadataFuture = getChKafkaProducer().sendAndReturnFuture(message);
        asyncResponseLogger.accept(recordMetadataFuture.get());
    }

    @Override
    protected void modifyProducerConfig(final ProducerConfig producerConfig) {
        producerConfig.setMaxBlockMilliseconds(10000);
    }
}
