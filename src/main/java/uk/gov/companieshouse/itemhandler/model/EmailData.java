package uk.gov.companieshouse.itemhandler.model;

public class EmailData {
    private String to;
    private String subject;
    private String emailAddress;
    private String orderReference;
    private String deliveryMethod;
    private DeliveryDetails deliveryDetails;
    private PaymentDetails paymentDetails;

    public EmailData() {
    }

    public EmailData(String to, String subject, String emailAddress, String orderReference, String deliveryMethod, DeliveryDetails deliveryDetails, PaymentDetails paymentDetails) {
        this.to = to;
        this.subject = subject;
        this.emailAddress = emailAddress;
        this.orderReference = orderReference;
        this.deliveryMethod = deliveryMethod;
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

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
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
}
