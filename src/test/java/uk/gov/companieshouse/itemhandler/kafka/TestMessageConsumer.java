package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.consumer.CHKafkaConsumerGroup;
import uk.gov.companieshouse.kafka.consumer.ConsumerConfig;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.util.List;

import static java.util.Collections.singletonList;

public abstract class TestMessageConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private final String topic;
    private final String group;

    private CHKafkaConsumerGroup consumerGroup;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String kafkaBrokerAddresses;

    protected TestMessageConsumer(final String topic, final String group) {
        this.topic = topic;
        this.group = group;
    }

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

        final ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setTopics(singletonList(topic));
        consumerConfig.setGroupName(group);
        consumerConfig.setResetOffset(false);
        consumerConfig.setBrokerAddresses(kafkaBrokerAddresses.split(","));
        consumerConfig.setAutoCommit(true);

        consumerGroup = new CHKafkaConsumerGroup(consumerConfig);
    }
}
