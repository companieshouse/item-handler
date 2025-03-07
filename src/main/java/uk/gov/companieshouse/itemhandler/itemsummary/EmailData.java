package uk.gov.companieshouse.itemhandler.itemsummary;

import uk.gov.companieshouse.itemhandler.model.DeliveryDetails;

import java.util.Objects;

public class EmailData {
    private String to;
    private String subject;
    private String orderReference;
    private DeliveryDetails deliveryDetails;
    private PaymentDetails paymentDetails;

    public EmailData() {
    }

    public EmailData(String to, String subject, String orderReference, DeliveryDetails deliveryDetails, PaymentDetails paymentDetails) {
        this.to = to;
        this.subject = subject;
        this.orderReference = orderReference;
        this.deliveryDetails = deliveryDetails;
        this.paymentDetails = paymentDetails;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public DeliveryDetails getDeliveryDetails() {
        return deliveryDetails;
    }

    public void setDeliveryDetails(DeliveryDetails deliveryDetails) {
        this.deliveryDetails = deliveryDetails;
    }

    public PaymentDetails getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(PaymentDetails paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmailData emailData)) {
            return false;
        }
        return Objects.equals(getTo(), emailData.getTo())
                && Objects.equals(getSubject(), emailData.getSubject())
                && Objects.equals(getOrderReference(), emailData.getOrderReference())
                && Objects.equals(getDeliveryDetails(), emailData.getDeliveryDetails())
                && Objects.equals(getPaymentDetails(), emailData.getPaymentDetails());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTo(), getSubject(), getOrderReference(), getDeliveryDetails(), getPaymentDetails());
    }
}
