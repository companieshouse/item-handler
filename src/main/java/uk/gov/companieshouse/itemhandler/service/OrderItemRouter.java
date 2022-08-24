package uk.gov.companieshouse.itemhandler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
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
                Collectors.toMap(Item::getKind,
                        item -> new DeliverableItemGroup(order,
                                                         item.getKind(),
                                                         ((DeliveryItemOptions) item.getItemOptions()).getDeliveryTimescale())
                        , (itemGroup, itemGroup2) -> itemGroup));
    }


    private ItemGroup byMissingImageDelivery(OrderData order) {
        return null;
    }
}
