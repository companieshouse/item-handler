package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.ProducerConfigException;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.logging.Logger;

public abstract class KafkaProducer implements InitializingBean {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private CHKafkaProducer chKafkaProducer;

    // TODO GCI-1428 Does this property name need rationalising?
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;

    @Override
    public void afterPropertiesSet() {
        LOGGER.trace("Configuring CH Kafka producer");
        final ProducerConfig config = new ProducerConfig();
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
        modifyProducerConfig(config);
        chKafkaProducer = new CHKafkaProducer(config);
    }

    /**
     * Extending classes implement this to provide any specific producer configuration modifications required.
     * @param producerConfig the producer configuration to be modified
     */
    protected void modifyProducerConfig(final ProducerConfig producerConfig) {
        // Does nothing here
    }

    protected CHKafkaProducer getChKafkaProducer() {
        return chKafkaProducer;
    }
}
