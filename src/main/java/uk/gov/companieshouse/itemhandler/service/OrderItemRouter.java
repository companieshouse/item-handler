package uk.gov.companieshouse.itemhandler.service;

import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderItemRouter implements Routable {
    private EmailService emailService;
    private ChdItemSenderService chdItemSenderService;

    public OrderItemRouter(EmailService emailService, ChdItemSenderService chdItemSenderService) {
        this.emailService = emailService;
        this.chdItemSenderService = chdItemSenderService;
    }

    @Override
    public void route(OrderData order) {
        Map<String, Map<DeliveryTimescale, DeliverableItemGroup>> deliverableItemGroupMap = deliverableItemsByKindAndDeliveryTimescale(order);
        ItemGroup missingImageDeliveryItems = byMissingImageDelivery(order);
        deliverableItemGroupMap.values().forEach(timescaleToGroup -> timescaleToGroup.values().forEach(g -> emailService.sendOrderConfirmation(g)));
        if (!missingImageDeliveryItems.empty()) {
            this.chdItemSenderService.sendItemsToChd(missingImageDeliveryItems);
        }
    }

    private Map<String, Map<DeliveryTimescale, DeliverableItemGroup>> deliverableItemsByKindAndDeliveryTimescale(OrderData order) {
        return order.getItems()
                .stream()
                .filter(item -> item.getItemOptions() instanceof DeliveryItemOptions)
                .collect(Collectors.groupingBy(Item::getKind, Collectors.toMap(
                        item -> Optional.ofNullable(((DeliveryItemOptions) item.getItemOptions()).getDeliveryTimescale())
                                .orElseThrow(() -> new NonRetryableException(String.format("Item [%s] is missing a delivery timescale", item.getId()))),
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
    }

    private ItemGroup byMissingImageDelivery(OrderData order) {
        return order.getItems()
                .stream()
                .filter(item -> !(item.getItemOptions() instanceof DeliveryItemOptions))
                .reduce(new ItemGroup(order, "item#missing-image-delivery"),
                        (a, b) -> {
                            a.add(b);
                            return a;
                        },
                        (a, b) -> {
                            a.addAll(b.getItems());
                            return a;
                        });
    }
}
