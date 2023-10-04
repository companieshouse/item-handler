package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.logging.Logger;

import java.util.stream.Collectors;

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
        logger.info("Sending digital item group " + describeItemGroup(digitalItemGroup) + " for digital processing.");
        // TODO DCAC-254 Implement ItemGroupOrderedMessageProducer et al.
    }

    private String describeItemGroup(final ItemGroup itemGroup) {
        return itemGroup.getKind() + " | " + itemGroup.getItems().size() + " items | " + itemGroup.getItems().stream()
                .map(Item::getId)
                .collect(Collectors.joining(" | "));
    }

}
