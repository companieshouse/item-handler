package uk.gov.companieshouse.itemhandler.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.kafka.KafkaTopics;

@Component
public class KafkaTopicInitialiser implements InitializingBean {

    private EmbeddedKafkaBroker broker;
    private KafkaTopics kafkaTopics;

    @Override
    public void afterPropertiesSet() throws Exception {
        broker.addTopics(new NewTopic(kafkaTopics.getEmailSend(), 1, (short) 1));
        broker.addTopics(new NewTopic(kafkaTopics.getOrderReceived(), 1, (short) 1));
        broker.addTopics(new NewTopic(kafkaTopics.getOrderReceivedNotificationRetry(), 1, (short) 1));
        broker.addTopics(new NewTopic(kafkaTopics.getOrderReceivedNotificationError(), 1, (short) 1));
        broker.addTopics(new NewTopic(kafkaTopics.getChdItemOrdered(), 1, (short) 1));
    }

    @Autowired
    public void setBroker(EmbeddedKafkaBroker broker) {
        this.broker = broker;
    }

    @Autowired
    public void setKafkaTopics(KafkaTopics kafkaTopics) {
        this.kafkaTopics = kafkaTopics;
    }
}
