package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.EmailData;
import uk.gov.companieshouse.itemhandler.util.DateConstants;

@Mapper
public interface OrderConfirmationMapper {
    @Mapping(source = "order.deliveryDetails", target = "deliveryDetails")
    @Mapping(source = "order.paymentReference", target = "paymentDetails.reference")
    @Mapping(source = "order.orderedAt", target = "paymentDetails.date", dateFormat = DateConstants.PAYMENT_DATE_TIME_FORMAT)
    @Mapping(source = "order.orderedBy.email", target = "to")
    EmailData map(DeliverableItemGroup itemGroup);
}
