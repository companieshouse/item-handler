package uk.gov.companieshouse.itemhandler.kafka;

public class KafkaTopics {
    private String emailSend;
    private String orderReceived;
    private String orderReceivedRetry;
    private String orderReceivedError;
    private String chdItemOrdered;

    private String itemGroupOrdered;

    public void setEmailSend(String emailSend) {
        this.emailSend = emailSend;
    }

    public void setOrderReceived(String orderReceived) {
        this.orderReceived = orderReceived;
    }

    public void setOrderReceivedRetry(String orderReceivedRetry) {
        this.orderReceivedRetry = orderReceivedRetry;
    }

    public void setOrderReceivedError(String orderReceivedError) {
        this.orderReceivedError = orderReceivedError;
    }

    public void setChdItemOrdered(String chdItemOrdered) {
        this.chdItemOrdered = chdItemOrdered;
    }

    public void setItemGroupOrdered(String itemGroupOrdered) {
        this.itemGroupOrdered = itemGroupOrdered;
    }

    public String getEmailSend() {
        return emailSend;
    }

    public String getOrderReceived() {
        return orderReceived;
    }

    public String getOrderReceivedRetry() {
        return orderReceivedRetry;
    }

    public String getOrderReceivedError() {
        return orderReceivedError;
    }

    public String getChdItemOrdered() {
        return chdItemOrdered;
    }

    public String getItemGroupOrdered() {
        return itemGroupOrdered;
    }
}
