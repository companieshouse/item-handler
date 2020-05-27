package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.*;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.time.format.DateTimeFormatter;

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
}
