package uk.gov.companieshouse.itemhandler.model;

import java.time.LocalDateTime;

public class OrderData extends AbstractOrderData {

    private LocalDateTime orderedAt;

    private ActionedBy orderedBy;

    private OrderLinks links;

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public ActionedBy getOrderedBy() {
        return orderedBy;
    }

    public void setOrderedBy(ActionedBy orderedBy) {
        this.orderedBy = orderedBy;
    }

    public OrderLinks getLinks() {
        return links;
    }

    public void setLinks(OrderLinks links) {
        this.links = links;
    }
}
