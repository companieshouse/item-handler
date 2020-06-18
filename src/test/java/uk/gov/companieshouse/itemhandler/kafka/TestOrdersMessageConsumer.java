package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.consumer.CHKafkaConsumerGroup;
import uk.gov.companieshouse.kafka.consumer.ConsumerConfig;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

import static java.util.Collections.singletonList;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;

@Service
public class TestOrdersMessageConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String GROUP_NAME = "order-received-consumers";
    private CHKafkaConsumerGroup consumerGroup;
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String kafkaBrokerAddresses;

    public void connect() {
        LOGGER.trace("Connecting to Kafka consumer group '" + consumerGroup + "'");
        consumerGroup.connect();
    }

    public List<Message> pollConsumerGroup() {
        return consumerGroup.consume();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.debug("Initializing kafka consumer service " + this.toString());

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setTopics(singletonList(ORDER_RECEIVED_TOPIC));
        consumerConfig.setGroupName(GROUP_NAME);
        consumerConfig.setResetOffset(false);
        consumerConfig.setBrokerAddresses(kafkaBrokerAddresses.split(","));
        consumerConfig.setAutoCommit(true);

        consumerGroup = new CHKafkaConsumerGroup(consumerConfig);
    }
}
