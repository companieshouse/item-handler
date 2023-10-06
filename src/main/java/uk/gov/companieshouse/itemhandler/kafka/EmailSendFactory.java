package uk.gov.companieshouse.itemhandler.kafka;

import email.email_send;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;

@Component
public class EmailSendFactory {
    public email_send buildMessage(final ItemGroup digitalItemGroup) {
        return email_send.newBuilder()
                .setAppId("item-handler")
                .setMessageId(digitalItemGroup.getItems().get(0).getId())
                .setMessageType("TBD")
                .setData("TBD")
                .setCreatedAt("TBD")
                .setEmailAddress("unknown@unknown.com")
                .build();
    }
}
