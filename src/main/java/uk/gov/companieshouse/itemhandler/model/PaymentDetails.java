package uk.gov.companieshouse.itemhandler.model;

public class PaymentDetails {
    private String reference;
    private String date;

    public PaymentDetails(String reference, String date) {
        this.reference = reference;
        this.date = date;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
