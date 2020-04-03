package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.Mapper;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Mapper(componentModel = "spring")
public interface OrdersApiToOrderDataMapper {
    OrderData ordersApiToOrderData(OrdersApi ordersApi);
}
