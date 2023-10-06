package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.kafka.ItemGroupOrderedMessageProducer;
import uk.gov.companieshouse.logging.Logger;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogMap;

/**
 * Service responsible for dispatching a message for each digital item group in an order for digital processing via
 * the <code>item-group-ordered</code> Kafka topic.
 */
@Service
public class DigitalItemGroupSenderService {
    private final Logger logger;

    private final ItemGroupOrderedMessageProducer itemGroupOrderedMessageProducer;

    public DigitalItemGroupSenderService(Logger logger,
                                         ItemGroupOrderedMessageProducer itemGroupOrderedMessageProducer) {
        this.logger = logger;
        this.itemGroupOrderedMessageProducer = itemGroupOrderedMessageProducer;
    }

    public void sendItemGroupForDigitalProcessing(final ItemGroup digitalItemGroup) {
        logger.info("Sending digital item group " + digitalItemGroup + " for digital processing.",
                getLogMap(digitalItemGroup.getOrder().getReference()));
        itemGroupOrderedMessageProducer.sendMessage(digitalItemGroup);
    }

}
