package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class OrdersKafkaProducer extends KafkaProducer {

    /**
     * Sends message to Kafka topic
     * @param message message
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void sendMessage(final Message message) throws ExecutionException, InterruptedException {
        Map<String, Object> logMap = LoggingUtils.createLogMapWithKafkaMessage(message);
        LoggingUtils.getLogger().info("Sending message to kafka topic", logMap);
        getChKafkaProducer().send(message);
    }

    @Override
    protected void modifyProducerConfig(final ProducerConfig producerConfig) {
        producerConfig.setRequestTimeoutMilliseconds(3000);
    }
}
