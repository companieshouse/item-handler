package uk.gov.companieshouse.itemhandler.service;

import java.util.Map;
import java.util.stream.Collectors;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;

public class OrderItemRouter implements Routable {
    private EmailService emailService;
    private ChdItemSenderService chdItemSenderService;

    public OrderItemRouter(EmailService emailService, ChdItemSenderService chdItemSenderService) {
        this.emailService = emailService;
        this.chdItemSenderService = chdItemSenderService;
    }

    @Override
    public void route(OrderData order) {
        Map<String, DeliverableItemGroup> deliverableItemGroupMap = deliverableItemsByKindAndDeliveryTimescale(order);
        deliverableItemGroupMap.values().forEach(deliverableItemGroup -> emailService.sendOrderConfirmation(deliverableItemGroup));
    }

    private Map<String, DeliverableItemGroup> deliverableItemsByKindAndDeliveryTimescale(OrderData order) {
        return order.getItems().stream().collect(
                Collectors.toMap(
                        item -> item.getKind() + ((DeliveryItemOptions) item.getItemOptions()).getDeliveryTimescale().getJsonName(),
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
                        })
        );
    }

    private ItemGroup byMissingImageDelivery(OrderData order) {
        return null;
    }
}
