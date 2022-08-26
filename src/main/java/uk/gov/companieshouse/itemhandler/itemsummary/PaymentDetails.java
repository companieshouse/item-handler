package uk.gov.companieshouse.itemhandler.itemsummary;

import java.util.Objects;

public class PaymentDetails {
    private String reference;
    private String date;

    public PaymentDetails() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentDetails)) {
            return false;
        }
        PaymentDetails that = (PaymentDetails) o;
        return Objects.equals(getReference(), that.getReference()) && Objects.equals(getDate(), that.getDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReference(), getDate());
    }
}
