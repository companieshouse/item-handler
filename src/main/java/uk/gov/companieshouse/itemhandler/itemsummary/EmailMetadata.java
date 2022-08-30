package uk.gov.companieshouse.itemhandler.itemsummary;

import java.util.Objects;

public class EmailMetadata<T extends EmailData> {

    private String appId;
    private String messageType;
    private T emailData;

    public EmailMetadata(String appId, String messageType, T emailData) {
        this.appId = appId;
        this.messageType = messageType;
        this.emailData = emailData;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public T getEmailData() {
        return emailData;
    }

    public void setEmailData(T emailData) {
        this.emailData = emailData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailMetadata<?> that = (EmailMetadata<?>) o;
        return Objects.equals(appId, that.appId) &&
                Objects.equals(messageType, that.messageType) &&
                Objects.equals(emailData, that.emailData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, messageType, emailData);
    }
}
