package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.consumer.CHKafkaConsumerGroup;
import uk.gov.companieshouse.kafka.consumer.ConsumerConfig;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static java.util.Collections.singletonList;
import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class OrdersKafkaConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String GROUP_NAME = "order-received-consumers";
    private CHKafkaConsumerGroup chKafkaConsumerGroup;
    @Value("${kafka.broker.addresses}")
    private String brokerAddresses;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.debug("Initializing kafka consumer service " + this.toString());

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setTopics(singletonList(ORDER_RECEIVED_TOPIC));
        consumerConfig.setGroupName(GROUP_NAME);
        consumerConfig.setResetOffset(false);
        consumerConfig.setBrokerAddresses(brokerAddresses.split(","));
        consumerConfig.setAutoCommit(true);

        chKafkaConsumerGroup = new CHKafkaConsumerGroup(consumerConfig);
    }

    @KafkaListener(topics = ORDER_RECEIVED_TOPIC, groupId = GROUP_NAME)
    public String processOrderReceived(String orderReceivedMessage) {
        LOGGER.info("Message: " + orderReceivedMessage + " received on topic: " + ORDER_RECEIVED_TOPIC);

        return orderReceivedMessage;
    }
}
