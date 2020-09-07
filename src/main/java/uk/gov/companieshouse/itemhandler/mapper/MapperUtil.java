package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.Named;

import java.time.LocalDateTime;

import static uk.gov.companieshouse.itemhandler.mapper.OrderDataToOrderConfirmationMapperConstants.DATETIME_OF_PAYMENT_FORMATTER;

public interface MapperUtil {

    /**
     * Renders an enum value name type string as a sentence case title string.
     * @param enumValueName Java enum value name like string
     * @return the sentence case title equivalent
     */
    @Named("toSentenceCase")
    default String toSentenceCase(final String enumValueName) {
        final String spaced = enumValueName.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase();
    }

    /**
     * Renders the {@link LocalDateTime} provided as a 24h date time string.
     * @param timeOfPayment the date/time of payment
     * @return 24h date time representation string
     */
    default String getTimeOfPayment(final LocalDateTime timeOfPayment) {
        return DATETIME_OF_PAYMENT_FORMATTER.format(timeOfPayment);
    }
}
