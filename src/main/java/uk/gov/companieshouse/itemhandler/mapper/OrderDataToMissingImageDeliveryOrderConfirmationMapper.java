package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.Mapper;
import uk.gov.companieshouse.itemhandler.email.MissingImageDeliveryOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Mapper(componentModel = "spring")
public interface OrderDataToMissingImageDeliveryOrderConfirmationMapper {

    MissingImageDeliveryOrderConfirmation orderToConfirmation(OrderData order);

}
