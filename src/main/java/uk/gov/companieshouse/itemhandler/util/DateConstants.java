package uk.gov.companieshouse.itemhandler.util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateConstants {
    /** Dictates how the payment date time is rendered in a certificate order confirmation. */
    public static final DateTimeFormatter DATETIME_OF_PAYMENT_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");

    /** Dictates how the date filed date is rendered in a certified copy order confirmation. */
    public static final DateTimeFormatter DATE_FILED_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(Locale.US);

    /** Dictates how the date in the filing history description is displayed */
    public static final DateTimeFormatter FILING_HISTORY_DATE_DESCRIPTION_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public static final String PAYMENT_DATE_TIME_FORMAT = "dd MMMM yyyy - HH:mm:ss";

    private DateConstants() {
    }
}
