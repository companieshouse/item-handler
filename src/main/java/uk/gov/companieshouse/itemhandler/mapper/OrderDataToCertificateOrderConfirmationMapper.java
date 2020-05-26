package uk.gov.companieshouse.itemhandler.mapper;

import org.apache.commons.lang.WordUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface OrderDataToCertificateOrderConfirmationMapper {

    DateTimeFormatter TIME_OF_PAYMENT_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' hh:mm");

    @Mapping(source = "deliveryDetails.forename", target="forename")
    @Mapping(source = "deliveryDetails.surname", target="surname")
    @Mapping(source = "deliveryDetails.addressLine1", target="addressLine1")
    @Mapping(source = "deliveryDetails.addressLine2", target="addressLine2")
    @Mapping(source = "deliveryDetails.locality", target="houseName")
    @Mapping(source = "deliveryDetails.premises", target="houseNumberStreetName")
    @Mapping(source = "deliveryDetails.region", target="city")
    @Mapping(source = "deliveryDetails.postalCode", target="postCode")

    @Mapping(source = "reference", target="orderReferenceNumber")
    @Mapping(source = "orderedBy.email", target="emailAddress")
    @Mapping(source = "totalOrderCost", target="feeAmount")
    CertificateOrderConfirmation orderToConfirmation(OrderData order);

    @AfterMapping
    default void specialMappings(final OrderData order, final @MappingTarget CertificateOrderConfirmation confirmation)
    {
        final String timescale = order.getItems().get(0).getItemOptions().getDeliveryTimescale().toString();
        confirmation.setDeliveryMethod(WordUtils.capitalizeFully(timescale));
        confirmation.setTimeOfPayment(TIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt()));
    }
}
