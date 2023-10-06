package uk.gov.companieshouse.itemhandler.kafka;

import email.email_send;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ItemGroupOrderedMessageProducer {

    // TODO DCAC-254 Replace email_send with ItemGroupOrdered
    private final KafkaTemplate<String, email_send> kafkaTemplate;
    private final EmailSendFactory emailSendFactory;

    private final String itemGroupOrderedTopic;

    private final Logger logger;

    public ItemGroupOrderedMessageProducer(KafkaTemplate<String, email_send> kafkaTemplate,
                                           EmailSendFactory emailSendFactory,
                                           @Value("${kafka.topics.item-group-ordered}")
                                           String itemGroupOrderedTopic,
                                           Logger logger) {
        this.kafkaTemplate = kafkaTemplate;
        this.emailSendFactory = emailSendFactory;
        this.itemGroupOrderedTopic = itemGroupOrderedTopic;
        this.logger = logger;
    }

    public void sendMessage(final ItemGroup digitalItemGroup) {
        // TODO DCAC-254 Structured logging
        logger.info("Sending a message for item group " + digitalItemGroup + " from order "
                + digitalItemGroup.getOrder().getReference() + ".");
        logger.info("topic = " + itemGroupOrderedTopic);
        final email_send message = emailSendFactory.buildMessage(digitalItemGroup);
        logger.info("message = " + message);
        kafkaTemplate.send(itemGroupOrderedTopic, message);
        // TODO DCAC-254 error handling, logging etc.
    }

}
