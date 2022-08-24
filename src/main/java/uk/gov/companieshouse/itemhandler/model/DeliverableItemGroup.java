package uk.gov.companieshouse.itemhandler.model;

public class DeliverableItemGroup extends ItemGroup {
    private DeliveryTimescale timescale;

    public DeliverableItemGroup(OrderData order, String kind, DeliveryTimescale timescale) {
        super(order, kind);
        this.timescale = timescale;
    }

    public DeliveryTimescale getTimescale() {
        return timescale;
    }

    public void setTimescale(DeliveryTimescale timescale) {
        this.timescale = timescale;
    }
}
