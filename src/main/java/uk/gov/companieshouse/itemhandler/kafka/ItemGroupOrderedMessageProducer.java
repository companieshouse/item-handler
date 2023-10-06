package uk.gov.companieshouse.itemhandler.kafka;

import email.email_send;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.logging.Logger;

import org.springframework.kafka.support.SendResult;

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
        final email_send message = emailSendFactory.buildMessage(digitalItemGroup);
        final ListenableFuture<SendResult<String, email_send>> future =
                kafkaTemplate.send(itemGroupOrderedTopic, message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, email_send>>() {
            @Override
            public void onSuccess(SendResult<String, email_send> result) {
                final RecordMetadata metadata =  result.getRecordMetadata();
                final int partition = metadata.partition();
                final long offset = metadata.offset();
                logger.info("Message " + message + " delivered to topic " + itemGroupOrderedTopic
                                + " on partition " + partition + " with offset " + offset + ".");
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("Unable to deliver message " + message + ". Error: " + ex.getMessage() + ".");
            }
        });
    }

}
