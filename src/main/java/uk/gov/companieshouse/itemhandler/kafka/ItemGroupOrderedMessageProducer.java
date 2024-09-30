package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogMap;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.logging.Logger;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Component
public class ItemGroupOrderedMessageProducer {

    private final KafkaTemplate<String, ItemGroupOrdered> kafkaTemplate;
    private final ItemGroupOrderedFactory itemGroupOrderedFactory;

    private final String itemGroupOrderedTopic;

    private final Logger logger;

    public ItemGroupOrderedMessageProducer(KafkaTemplate<String, ItemGroupOrdered> kafkaTemplate,
                                           ItemGroupOrderedFactory itemGroupOrderedFactory,
                                           @Value("${kafka.topics.item-group-ordered}")
                                           String itemGroupOrderedTopic,
                                           Logger logger) {
        this.kafkaTemplate = kafkaTemplate;
        this.itemGroupOrderedFactory = itemGroupOrderedFactory;
        this.itemGroupOrderedTopic = itemGroupOrderedTopic;
        this.logger = logger;
    }

    public void sendMessage(final ItemGroup digitalItemGroup) {
        logger.info("Sending a message for item group " + digitalItemGroup + " from order "
                + digitalItemGroup.getOrder().getReference() + ".",
                getLogMap(digitalItemGroup.getOrder().getReference()));
        final ItemGroupOrdered message = itemGroupOrderedFactory.createMessage(digitalItemGroup);
        final CompletableFuture<SendResult<String, ItemGroupOrdered>> future =
                kafkaTemplate.send(itemGroupOrderedTopic, message);
        future.whenComplete((BiConsumer<? super SendResult<String, ItemGroupOrdered>, ? super Throwable>)
                new ItemGroupOrderedMessageProducerCallback(message, itemGroupOrderedTopic, digitalItemGroup, logger));
    }

}
