package uk.gov.companieshouse.itemhandler.itemsummary;

import uk.gov.companieshouse.itemhandler.util.DateConstants;

import java.time.format.DateTimeFormatter;

public abstract class OrderConfirmationMapper<T extends EmailData> {

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
        emailData.setOrderReference(itemGroup.getOrder().getReference());
        emailData.setDeliveryDetails(itemGroup.getOrder().getDeliveryDetails());
        emailData.setPaymentDetails(new PaymentDetails(itemGroup.getOrder().getPaymentReference(), itemGroup.getOrder().getOrderedAt().format(DateTimeFormatter.ofPattern(DateConstants.PAYMENT_DATE_TIME_FORMAT))));
    }
}
