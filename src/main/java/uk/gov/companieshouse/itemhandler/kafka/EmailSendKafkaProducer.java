package uk.gov.companieshouse.itemhandler.kafka;

import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.ProducerConfigException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;


@Service
public class EmailSendKafkaProducer implements InitializingBean {
    
    private CHKafkaProducer chKafkaProducer;
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;

    /**
     * Sends message to Kafka topic
     * @param message message
     * @throws ExecutionException should something unexpected happen
     * @throws InterruptedException should something unexpected happen
     */
    public void sendMessage(final Message message, String orderReference) throws ExecutionException, InterruptedException {
        LoggingUtils.logMessageWithOrderReference(message, "Sending message to Kafka", orderReference);
        chKafkaProducer.send(message);
    }

    @Override
    public void afterPropertiesSet() {
        LoggingUtils.getLogger().trace("Configuring CH Kafka producer");
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
        chKafkaProducer = new CHKafkaProducer(config);
    }
}
