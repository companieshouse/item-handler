package uk.gov.companieshouse.itemhandler.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicInitialiser implements InitializingBean {

    private EmbeddedKafkaBroker broker;

    @Override
    public void afterPropertiesSet() throws Exception {
        broker.addTopics(new NewTopic("email-send", 1, (short) 1));
        broker.addTopics(new NewTopic("order-received", 1, (short) 1));
        broker.addTopics(new NewTopic("order-received-notification-retry", 1, (short) 1));
        broker.addTopics(new NewTopic("order-received-notification-error", 1, (short) 1));
    }

    @Autowired
    public void setBroker(EmbeddedKafkaBroker broker) {
        this.broker = broker;
    }
}
