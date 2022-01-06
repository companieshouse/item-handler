package uk.gov.companieshouse.itemhandler.kafka;

public class KafkaTopics {
    private String emailSend;
    private String orderReceived;
    private String orderReceivedNotificationRetry;
    private String orderReceivedNotificationError;
    private String chdItemOrdered;

    public void setEmailSend(String emailSend) {
        this.emailSend = emailSend;
    }

    public void setOrderReceived(String orderReceived) {
        this.orderReceived = orderReceived;
    }

    public void setOrderReceivedNotificationRetry(String orderReceivedNotificationRetry) {
        this.orderReceivedNotificationRetry = orderReceivedNotificationRetry;
    }

    public void setOrderReceivedNotificationError(String orderReceivedNotificationError) {
        this.orderReceivedNotificationError = orderReceivedNotificationError;
    }

    public void setChdItemOrdered(String chdItemOrdered) {
        this.chdItemOrdered = chdItemOrdered;
    }

    public String getEmailSend() {
        return emailSend;
    }

    public String getOrderReceived() {
        return orderReceived;
    }

    public String getOrderReceivedNotificationRetry() {
        return orderReceivedNotificationRetry;
    }

    public String getOrderReceivedNotificationError() {
        return orderReceivedNotificationError;
    }

    public String getChdItemOrdered() {
        return chdItemOrdered;
    }
}
