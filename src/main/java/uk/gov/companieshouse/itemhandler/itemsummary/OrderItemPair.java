package uk.gov.companieshouse.itemhandler.itemsummary;

import java.util.Objects;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

public class OrderItemPair {

    private final OrderData order;
    private final Item item;

    public OrderItemPair(OrderData order, Item item) {
        this.order = order;
        this.item = item;
    }

    public OrderData getOrder() {
        return order;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderItemPair that = (OrderItemPair) o;
        return Objects.equals(order, that.order) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, item);
    }
}
