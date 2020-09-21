package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.ProducerConfigException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.logging.Logger;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;

@Service
public class ItemKafkaProducer implements InitializingBean {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private CHKafkaProducer chKafkaProducer;

    // TODO GCI-1428 Does this property name need rationalising?
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;

    /**
     * TODO GCI-1428 Javadoc this
     * Sends message to Kafka topic
     * @param orderUri
     * @param itemId
     * @param message
     * @param asyncResponseLogger
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Async
    public void sendMessage(final String orderUri,
                            final String itemId,
                            final Message message,
                            final Consumer<RecordMetadata> asyncResponseLogger)
            throws ExecutionException, InterruptedException {

        final Map<String, Object> logMap = LoggingUtils.createLogMap();
        logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        logIfNotNull(logMap, LoggingUtils.ITEM_ID, itemId);
        LOGGER.info("Sending message to kafka topic", logMap);

        final Future<RecordMetadata> recordMetadataFuture = chKafkaProducer.sendAndReturnFuture(message);
        asyncResponseLogger.accept(recordMetadataFuture.get());
    }

    @Override
    public void afterPropertiesSet() {
        LOGGER.trace("Configuring CH Kafka producer");
        ProducerConfig config = new ProducerConfig();
        if (brokerAddresses != null && !brokerAddresses.isEmpty()) {
            config.setBrokerAddresses(brokerAddresses.split(","));
        } else {
            throw new ProducerConfigException("Broker addresses for kafka broker missing, check if environment variable KAFKA_BROKER_ADDR is configured. " +
                    "[Hint: The property 'kafka.broker.addresses' uses the value of this environment variable in live environments " +
                    "and that of 'spring.embedded.kafka.brokers' property in test.]");
        }

        config.setRoundRobinPartitioner(true);
        config.setAcks(Acks.WAIT_FOR_ALL);
        config.setRetries(10);
        config.setMaxBlockMilliseconds(10000);
        chKafkaProducer = new CHKafkaProducer(config);
    }
}
