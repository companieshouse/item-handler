package uk.gov.companieshouse.itemhandler.mapper;

import java.time.format.DateTimeFormatter;

class OrderDataToCertificateOrderConfirmationMapperConstants {

    private OrderDataToCertificateOrderConfirmationMapperConstants() { }

    /** Dictates how the payment date time is rendered in a certificate order confirmation. */
    static final DateTimeFormatter TIME_OF_PAYMENT_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
}
