package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemgroupordered.OrderedBy;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import static java.util.Collections.singletonList;

@Component
public class ItemGroupOrderedFactory {
    public ItemGroupOrdered buildMessage(final ItemGroup digitalItemGroup) {
        final OrderData order = digitalItemGroup.getOrder();
        final ActionedBy actionedBy = order.getOrderedBy();
        final OrderedBy orderedBy = new OrderedBy(actionedBy.getEmail(), actionedBy.getId());
        final Item item = order.getItems().get(0);
        final uk.gov.companieshouse.itemgroupordered.Item avroItem =
                new uk.gov.companieshouse.itemgroupordered.Item(
                        item.getCompanyName(),
                        item.getCompanyNumber(),
                        item.getCustomerReference(),
                        item.getDescription(),
                        item.getDescriptionIdentifier(),
                        item.getDescriptionValues(),
                        "digital document location"/* TODO DCAC-254 digital document location */,
                        item.getEtag(),
                        item.getId(),
                        null /* TODO DCAC-254 item costs */,
                        null /* TODO DCAC-254 item options */,
                        item.getItemUri(),
                        item.getKind(),
                        null /* TODO DCAC-254 item links */,
                        item.getPostageCost(),
                        item.isPostalDelivery(),
                        item.getQuantity(),
                        item.getTotalItemCost()
                );
        return ItemGroupOrdered.newBuilder()
                .setOrderId(order.getReference())
                .setOrderedAt(order.getOrderedAt().toString())
                .setOrderedBy(orderedBy)
                .setPaymentReference(order.getPaymentReference())
                .setReference(order.getReference())
                .setTotalOrderCost(order.getTotalOrderCost())
                .setItems(singletonList(avroItem))
                .build();
    }
}
