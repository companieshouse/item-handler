package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.*;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapperConstants.TIME_OF_PAYMENT_FORMATTER;
import static uk.gov.companieshouse.itemhandler.model.IncludeAddressRecordsType.CURRENT;

@Mapper(componentModel = "spring")
public interface OrderDataToCertificateOrderConfirmationMapper {

    // Name/address mappings
    @Mapping(source = "deliveryDetails.forename", target="forename")
    @Mapping(source = "deliveryDetails.surname", target="surname")
    @Mapping(source = "deliveryDetails.addressLine1", target="addressLine1")
    @Mapping(source = "deliveryDetails.addressLine2", target="addressLine2")
    @Mapping(source = "deliveryDetails.locality", target="houseName")
    @Mapping(source = "deliveryDetails.premises", target="houseNumberStreetName")
    @Mapping(source = "deliveryDetails.region", target="city")
    @Mapping(source = "deliveryDetails.postalCode", target="postCode")
    @Mapping(source = "deliveryDetails.country", target="country")

    // Order details field mappings
    @Mapping(source = "reference", target="orderReferenceNumber")
    @Mapping(source = "orderedBy.email", target="emailAddress")
    @Mapping(source = "totalOrderCost", target="feeAmount")

    CertificateOrderConfirmation orderToConfirmation(OrderData order);

    /**
     * Implements the more complex mapping behaviour required for some fields.
     * @param order the order to be mapped
     * @param confirmation the confirmation mapped to
     */
    @AfterMapping
    default void specialMappings(final OrderData order, final @MappingTarget CertificateOrderConfirmation confirmation)
    {
        final Item item = order.getItems().get(0);

        // Order details field mappings
        final String timescale = item.getItemOptions().getDeliveryTimescale().toString();
        confirmation.setDeliveryMethod(toSentenceCase(timescale) + " delivery");
        confirmation.setTimeOfPayment(getTimeOfPayment(order.getOrderedAt()));

        // Certificate details field mappings
        confirmation.setCompanyName(item.getCompanyName());
        confirmation.setCompanyNumber(item.getCompanyNumber());
        confirmation.setCertificateType(toSentenceCase(item.getItemOptions().getCertificateType().toString()));
        confirmation.setCertificateIncludes(getCertificateIncludes(item));
    }

    /**
     * Renders an enum value name type string as a sentence case title string. Annotated
     * <code>@Named("toSentenceCase")</code> to prevent MapStruct from applying the mapping to all
     * <code>String</code>-><code>String</code> mappings. Instead, this method is applied manually in
     * {@link #specialMappings(OrderData, CertificateOrderConfirmation)}.
     * @param enumValueName Java enum value name like string
     * @return the sentence case title equivalent
     */
    @Named("toSentenceCase")
    default String toSentenceCase(final String enumValueName) {
        final String spaced = enumValueName.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase();
    }

    /**
     * Examines the certificate item provided to build up the certificate includes list of descriptions.
     * @param certificate the certificate item
     * @return the certificate includes
     */
    default String[] getCertificateIncludes(final Item certificate) {
        final List<String> includes = new ArrayList<>();
        final CertificateItemOptions options = certificate.getItemOptions();
        if (TRUE.equals(options.getIncludeGoodStandingInformation())) {
            includes.add("Statement of good standing");
        }
        final RegisteredOfficeAddressDetails office = options.getRegisteredOfficeAddressDetails();
        if (office != null && office.getIncludeAddressRecordsType() == CURRENT ) {
            includes.add("Registered office address");
        }
        final DirectorOrSecretaryDetails directors = options.getDirectorDetails();
        if (directors != null && TRUE.equals(directors.getIncludeBasicInformation())) {
            includes.add("Directors");
        }
        final DirectorOrSecretaryDetails secretaries = options.getSecretaryDetails();
        if (secretaries != null && TRUE.equals(secretaries.getIncludeBasicInformation())) {
            includes.add("Secretaries");
        }
        if (TRUE.equals(options.getIncludeCompanyObjectsInformation())) {
            includes.add("Company objects");
        }
        return includes.toArray(new String[0]);
    }

    /**
     * Renders the {@link LocalDateTime} provided as a 24h date time string.
     * @param timeOfPayment the date/time of payment
     * @return 24h date time representation string
     */
    default String getTimeOfPayment(final LocalDateTime timeOfPayment) {
        return TIME_OF_PAYMENT_FORMATTER.format(timeOfPayment);
    }
}
