package uk.gov.companieshouse.itemhandler.model;

import java.util.ArrayList;
import java.util.List;

public class KindAndDeliveryTimescaleGroup {

    private OrderData order;
    private String kind;
    private DeliveryTimescale timescale;
    private final List<Item> items;

    public KindAndDeliveryTimescaleGroup(OrderData order, String kind, DeliveryTimescale timescale) {
        this.order = order;
        this.kind = kind;
        this.timescale = timescale;
        this.items = new ArrayList<>();
    }

    public OrderData getOrder() {
        return order;
    }

    public void setOrder(OrderData order) {
        this.order = order;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public DeliveryTimescale getTimescale() {
        return timescale;
    }

    public void setTimescale(DeliveryTimescale timescale) {
        this.timescale = timescale;
    }

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        items.add(item);
    }
}
