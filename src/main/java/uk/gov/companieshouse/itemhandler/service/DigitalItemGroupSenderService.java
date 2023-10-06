package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.logging.Logger;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogMap;

/**
 * Service responsible for dispatching a message for each digital item group in an order for digital processing via
 * the <code>item-group-ordered</code> Kafka topic.
 */
@Service
public class DigitalItemGroupSenderService {
    private final Logger logger;

    public DigitalItemGroupSenderService(Logger logger) {
        this.logger = logger;
    }

    public void sendItemGroupForDigitalProcessing(final ItemGroup digitalItemGroup) {
        logger.info("Sending digital item group " + digitalItemGroup + " for digital processing.",
                getLogMap(digitalItemGroup.getOrder().getReference()));

        // TODO DCAC-254 Implement ItemGroupOrderedMessageProducer et al.
    }

}
