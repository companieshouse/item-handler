package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.*;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDataToCertificateOrderConfirmationMapper {

    DateTimeFormatter TIME_OF_PAYMENT_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' hh:mm");

    // Name/address mappings
    @Mapping(source = "deliveryDetails.forename", target="forename")
    @Mapping(source = "deliveryDetails.surname", target="surname")
    @Mapping(source = "deliveryDetails.addressLine1", target="addressLine1")
    @Mapping(source = "deliveryDetails.addressLine2", target="addressLine2")
    @Mapping(source = "deliveryDetails.locality", target="houseName")
    @Mapping(source = "deliveryDetails.premises", target="houseNumberStreetName")
    @Mapping(source = "deliveryDetails.region", target="city")
    @Mapping(source = "deliveryDetails.postalCode", target="postCode")

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
        confirmation.setTimeOfPayment(TIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt()));

        // Certificate details field mappings
        confirmation.setCompanyName(item.getCompanyName());
        confirmation.setCompanyNumber(item.getCompanyNumber());
        confirmation.setCertificateType(toSentenceCase(item.getItemOptions().getCertificateType().toString()));
        confirmation.setCertificateIncludes(getCertificateIncludes(item));
    }

    /**
     * Renders an enum value name type string as a sentence case title string.
     * @param enumValueName Java enum value name like string
     * @return the sentence case title equivalent
     */
    @Named("toSentenceCase") // TODO GCI-931 Can this be made clearer?
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
        if (options.getIncludeGoodStandingInformation() != null && options.getIncludeGoodStandingInformation()) {
            includes.add("Statement of good standing");
        }
        if (options.getRegisteredOfficeAddressDetails() != null) {
            includes.add("Registered office address");
        }
        if (options.getDirectorDetails() != null) {
            includes.add("Directors");
        }
        if (options.getSecretaryDetails() != null) {
            includes.add("Secretaries");
        }
        if (options.getIncludeCompanyObjectsInformation() != null && options.getIncludeCompanyObjectsInformation()) {
            includes.add("Company objects");
        }
        return includes.toArray(new String[0]);
    }
}
