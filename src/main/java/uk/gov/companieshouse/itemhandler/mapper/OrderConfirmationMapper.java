package uk.gov.companieshouse.itemhandler.mapper;

import uk.gov.companieshouse.itemhandler.config.EmailConfig;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.EmailData;
import uk.gov.companieshouse.itemhandler.model.PaymentDetails;
import uk.gov.companieshouse.itemhandler.util.DateConstants;

import java.time.format.DateTimeFormatter;

public abstract class OrderConfirmationMapper<T extends EmailData> {

    private final EmailConfig emailConfig;

    public OrderConfirmationMapper(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }

    public T map(DeliverableItemGroup itemGroup) {
        T emailData = newEmailDataInstance();
        mapData(itemGroup, emailData);
        mapItems(itemGroup, emailData);
        return emailData;
    }

    protected abstract T newEmailDataInstance();

    protected abstract void mapItems(DeliverableItemGroup itemGroup, T emailData);

    private void mapData(DeliverableItemGroup itemGroup, T emailData) {
        emailData.setTo(itemGroup.getOrder().getOrderedBy().getEmail());
        emailData.setEmailAddress(this.emailConfig.getSenderEmail());
        emailData.setDeliveryDetails(itemGroup.getOrder().getDeliveryDetails());
        emailData.setPaymentDetails(new PaymentDetails(itemGroup.getOrder().getReference(), itemGroup.getOrder().getOrderedAt().format(DateTimeFormatter.ofPattern(DateConstants.PAYMENT_DATE_TIME_FORMAT))));
    }
}
