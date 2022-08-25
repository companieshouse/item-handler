package uk.gov.companieshouse.itemhandler.model;

import java.util.List;
import java.util.Objects;

public class DeliverableItemGroup extends ItemGroup {
    private DeliveryTimescale timescale;

    public DeliverableItemGroup(OrderData order, String kind, DeliveryTimescale timescale) {
        super(order, kind);
        this.timescale = timescale;
    }

    public DeliverableItemGroup(OrderData order, String kind, DeliveryTimescale timescale, List<Item> items) {
        super(order, kind, items);
        this.timescale = timescale;
    }

    public DeliveryTimescale getTimescale() {
        return timescale;
    }

    public void setTimescale(DeliveryTimescale timescale) {
        this.timescale = timescale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliverableItemGroup that = (DeliverableItemGroup) o;
        return timescale == that.timescale;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timescale);
    }
}
