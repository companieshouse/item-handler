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
import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;


@Service
public class TestEmailSendMessageConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String EMAIL_SEND_TOPIC = "email-send";
    private static final String GROUP_NAME = "message-send-consumer-group";
    private CHKafkaConsumerGroup consumerGroup;
    // TODO GCI-931 Was @Value("${kafka.broker.addresses}")
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
        consumerConfig.setTopics(singletonList(EMAIL_SEND_TOPIC));
        consumerConfig.setGroupName(GROUP_NAME);
        consumerConfig.setResetOffset(false);
        consumerConfig.setBrokerAddresses(kafkaBrokerAddresses.split(","));
        consumerConfig.setAutoCommit(true);

        consumerGroup = new CHKafkaConsumerGroup(consumerConfig);
    }
}
