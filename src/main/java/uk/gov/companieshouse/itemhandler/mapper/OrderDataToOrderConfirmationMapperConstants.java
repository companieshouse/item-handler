package uk.gov.companieshouse.itemhandler.mapper;

import java.time.format.DateTimeFormatter;

class OrderDataToOrderConfirmationMapperConstants {
    /** Dictates how the payment date time is rendered in a certificate order confirmation. */
    static final DateTimeFormatter DATETIME_OF_PAYMENT_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");

    /** Dictates how the date filed date is rendered in a certified copy order confirmation. */
    static final DateTimeFormatter DATE_FILED_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
}
