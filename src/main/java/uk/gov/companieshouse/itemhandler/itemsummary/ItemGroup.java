package uk.gov.companieshouse.itemhandler.itemsummary;

import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemGroup {

    private OrderData order;
    private String kind;
    private final List<Item> items;

    public ItemGroup(OrderData order, String kind) {
        this(order, kind, new ArrayList<>());
    }

    public ItemGroup(OrderData order, String kind, List<Item> items) {
        this.order = order;
        this.kind = kind;
        this.items = items;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemGroup itemGroup)) {
            return false;
        }
        return Objects.equals(getOrder(), itemGroup.getOrder())
                && Objects.equals(getKind(), itemGroup.getKind())
                && Objects.equals(getItems(), itemGroup.getItems());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrder(), getKind(), getItems());
    }

    @Override
    public String toString() {
        final String numberOfItems = getItems().size() + (getItems().size() > 1 ? " items | " : " item | ");
        return getKind() + " | " + numberOfItems + getItems().stream()
                .map(Item::getId)
                .collect(Collectors.joining(" | "));
    }
}
