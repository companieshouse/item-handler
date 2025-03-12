package uk.gov.companieshouse.itemhandler.itemsummary;

import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

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
        if (!(o instanceof DeliverableItemGroup that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return getTimescale() == that.getTimescale();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTimescale());
    }
}
