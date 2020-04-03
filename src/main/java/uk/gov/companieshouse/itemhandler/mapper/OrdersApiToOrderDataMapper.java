package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface OrdersApiToOrderDataMapper {
    OrderData ordersApiToOrderData(OrdersApi ordersApi);
}
