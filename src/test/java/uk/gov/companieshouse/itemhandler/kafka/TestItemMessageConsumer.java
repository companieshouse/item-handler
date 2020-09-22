package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.consumer.CHKafkaConsumerGroup;
import uk.gov.companieshouse.kafka.consumer.ConsumerConfig;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.util.List;

import static java.util.Collections.singletonList;

@Service
public class TestItemMessageConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggingUtils.getLogger();
    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String GROUP_NAME = "chd-item-ordered-consumers";
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
    public void afterPropertiesSet() {
        LOGGER.debug("Initializing kafka consumer service " + this.toString());

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setTopics(singletonList(CHD_ITEM_ORDERED_TOPIC));
        consumerConfig.setGroupName(GROUP_NAME);
        consumerConfig.setResetOffset(false);
        consumerConfig.setBrokerAddresses(kafkaBrokerAddresses.split(","));
        consumerConfig.setAutoCommit(true);

        consumerGroup = new CHKafkaConsumerGroup(consumerConfig);
    }
}
