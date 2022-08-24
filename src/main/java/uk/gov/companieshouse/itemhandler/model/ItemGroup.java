package uk.gov.companieshouse.itemhandler.model;

import java.util.ArrayList;
import java.util.List;

public class ItemGroup {

    private OrderData order;
    private String kind;
    private final List<Item> items;

    public ItemGroup(OrderData order, String kind) {
        this.order = order;
        this.kind = kind;
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

    public List<Item> getItems() {
        return items;
    }

    public void add(Item item) {
        items.add(item);
    }

    public void addAll(List<Item> itemList) {
        items.addAll(itemList);
    }

    public boolean empty() {
        return items.isEmpty();
    }
}
