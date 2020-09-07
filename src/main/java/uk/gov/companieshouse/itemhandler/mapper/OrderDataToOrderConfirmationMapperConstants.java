package uk.gov.companieshouse.itemhandler.mapper;

import java.time.format.DateTimeFormatter;

public class OrderDataToOrderConfirmationMapperConstants {
    /** Dictates how the payment date time is rendered in a certificate order confirmation. */
    static final DateTimeFormatter DATETIME_OF_PAYMENT_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
}
