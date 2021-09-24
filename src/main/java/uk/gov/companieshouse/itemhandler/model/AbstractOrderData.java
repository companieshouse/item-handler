package uk.gov.companieshouse.itemhandler.model;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Factors out those data fields common to both {@link CheckoutData} and {@link OrderData}.
 */
public abstract class AbstractOrderData {
    private String paymentReference;

    private String etag;

    private DeliveryDetails deliveryDetails = new DeliveryDetails();

    private List<Item> items = new ArrayList<>();

    private String kind;

    private String totalOrderCost;

    private String reference;


    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public DeliveryDetails getDeliveryDetails() {
        return deliveryDetails;
    }

    public void setDeliveryDetails(DeliveryDetails deliveryDetails) {
        this.deliveryDetails = deliveryDetails;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTotalOrderCost() {
        return totalOrderCost;
    }

    public void setTotalOrderCost(String totalOrderCost) {
        this.totalOrderCost = totalOrderCost;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() { return new Gson().toJson(this); }
}
