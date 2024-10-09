package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.kafka.support.SendResult;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.logging.Logger;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogMap;

public class ItemGroupOrderedMessageProducerCallback implements KafkaFuture.BiConsumer<SendResult<String, ItemGroupOrdered>, Throwable> {

    private final ItemGroupOrdered message;
    private final String itemGroupOrderedTopic;
    private final ItemGroup digitalItemGroup;
    private final Logger logger;

    public ItemGroupOrderedMessageProducerCallback(ItemGroupOrdered message, String itemGroupOrderedTopic,
                                                   ItemGroup digitalItemGroup, Logger logger) {
        this.message = message;
        this.itemGroupOrderedTopic = itemGroupOrderedTopic;
        this.digitalItemGroup = digitalItemGroup;
        this.logger = logger;
    }

    public void onSuccess(SendResult<String, ItemGroupOrdered> result) {
        final RecordMetadata metadata =  result.getRecordMetadata();
        final int partition = metadata.partition();
        final long offset = metadata.offset();
        logger.info("Message " + message + " delivered to topic " + itemGroupOrderedTopic
                        + " on partition " + partition + " with offset " + offset + ".",
                getLogMap(digitalItemGroup.getOrder().getReference()));
    }

    public void onFailure(Throwable ex) {
        final String error = "Unable to deliver message " + message + " for order " +
                digitalItemGroup.getOrder().getReference() + ". Error: " + ex.getMessage() + ".";
        if (ex instanceof Exception) {
            logger.error(error, (Exception) ex, getLogMap(digitalItemGroup.getOrder().getReference()));
        } else {
            logger.error(error, getLogMap(digitalItemGroup.getOrder().getReference()));
        }
    }

    @Override
    public void accept(SendResult<String, ItemGroupOrdered> stringItemGroupOrderedSendResult, Throwable throwable) {}
}
