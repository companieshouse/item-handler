package uk.gov.companieshouse.itemhandler.service;

import java.util.Map;
import java.util.stream.Collectors;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.KindAndDeliveryTimescaleGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;

public class OrderItemAggregator {

    private Map<String, KindAndDeliveryTimescaleGroup> byItemKindAndDeliveryTimescale(OrderData order) {
        order.getItems().stream().collect(Collectors.groupingBy(Item::getKind,
            Collectors.groupingBy(item -> ((DeliveryItemOptions)item.getItemOptions()).getDeliveryTimescale())));
    }

}
