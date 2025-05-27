package uk.gov.companieshouse.itemhandler.itemsummary;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.ChdItemSenderService;
import uk.gov.companieshouse.itemhandler.service.EmailService;
import uk.gov.companieshouse.itemhandler.service.Routable;
import uk.gov.companieshouse.logging.Logger;

@Component
public class OrderItemRouter implements Routable {

    private final EmailService emailService;
    private final ChdItemSenderService chdItemSenderService;
    private final Logger logger;

    public OrderItemRouter(EmailService emailService, ChdItemSenderService chdItemSenderService, Logger logger) {
        this.emailService = emailService;
        this.chdItemSenderService = chdItemSenderService;
        this.logger = logger;
    }

    @Override
    public void route(final OrderData order) {
        logger.trace(format("route(%s) method called.", order));

        Map<String, Map<DeliveryTimescale, DeliverableItemGroup>> deliverableItemGroupMap = deliverableItemsByKindAndDeliveryTimescale(order);
        logDeliverableItems(deliverableItemGroupMap);

        deliverableItemGroupMap.values().forEach(timescaleToGroup -> {
            logger.info(format("Sending deliverable item group (%d entries) to email service...", timescaleToGroup.size()));
            timescaleToGroup.values().forEach(emailService::sendOrderConfirmation);
        });

        ItemGroup missingImageDeliveryItems = byMissingImageDelivery(order);
        if (!missingImageDeliveryItems.empty()) {
            logger.info(format("Sending missing image delivery items (%d entries) to CHD...", missingImageDeliveryItems.getItems().size()));
            chdItemSenderService.sendItemsToChd(missingImageDeliveryItems);
        }
    }

    private Map<String, Map<DeliveryTimescale, DeliverableItemGroup>> deliverableItemsByKindAndDeliveryTimescale(final OrderData order) {
        logger.trace(format("deliverableItemsByKindAndDeliveryTimescale(%s) method called.", order));

        Map<String, Map<DeliveryTimescale, DeliverableItemGroup>> deliverableItems = order.getItems()
                .stream()
                .filter(item -> item.isPostalDelivery() && item.getItemOptions() instanceof DeliveryItemOptions)
                .collect(Collectors.groupingBy(Item::getKind, Collectors.toMap(
                        item -> Optional.ofNullable(
                                        ((DeliveryItemOptions) item.getItemOptions()).getDeliveryTimescale())
                                .orElseThrow(() -> new NonRetryableException(
                                        format("Item [%s] is missing a delivery timescale", item.getId()))),
                        item -> {
                            DeliverableItemGroup deliverableItemGroup =
                                    new DeliverableItemGroup(order,
                                            item.getKind(),
                                            ((DeliveryItemOptions) item.getItemOptions()).getDeliveryTimescale());
                            deliverableItemGroup.add(item);
                            return deliverableItemGroup;
                        },
                        (originalItemGroup, duplicateKeyItemGroup) -> {
                            originalItemGroup.addAll(duplicateKeyItemGroup.getItems());
                            return originalItemGroup;
                        })));

        logger.info(format("There are %d deliverable items.", deliverableItems.size()));

        return deliverableItems;
    }

    private ItemGroup byMissingImageDelivery(final OrderData order) {
        logger.trace(format("byMissingImageDelivery(%s) method called.", order));

        List<Item> missingImageDeliveryItems = order.getItems()
                .stream()
                .filter(item -> !(item.getItemOptions() instanceof DeliveryItemOptions))
                .collect(Collectors.toList());

        logger.info(format("There are %d missing image delivery items.", missingImageDeliveryItems.size()));

        return new ItemGroup(order, "item#missing-image-delivery", missingImageDeliveryItems);
    }

    private void logDeliverableItems(final Map<String, Map<DeliveryTimescale, DeliverableItemGroup>> deliverableItemGroupMap) {
        logger.trace(format("logDeliverableItems(%d entries) method called.", deliverableItemGroupMap.size()));

        for (Map.Entry<String, Map<DeliveryTimescale, DeliverableItemGroup>> outerEntry : deliverableItemGroupMap.entrySet()) {
            String outerKey = outerEntry.getKey();
            logger.info(format("Key (Item->getKind): [%s]", outerKey));

            Map<DeliveryTimescale, DeliverableItemGroup> innerMap = outerEntry.getValue();
            for (Map.Entry<DeliveryTimescale, DeliverableItemGroup> innerEntry : innerMap.entrySet()) {
                logger.info(format("-> Key: [DeliveryTimescale=%s], Value: [DeliverableItemGroup=%s]  ",
                        innerEntry.getKey(), innerEntry.getValue()));
            }
        }
    }
}
