package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.Mapper;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Mapper(componentModel = "spring")
public interface OrderDataToCertificateOrderConfirmationMapper {
    CertificateOrderConfirmation orderToConfirmation(OrderData order);
}
