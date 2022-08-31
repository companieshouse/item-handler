package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.itemsummary.CertificateConfirmationMapper;
import uk.gov.companieshouse.itemhandler.itemsummary.EmailData;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderConfirmationMapper;

@Component
class ConfirmationMapperFactory {

    private final CertificateConfirmationMapper certificateConfirmationMapper;

    ConfirmationMapperFactory(CertificateConfirmationMapper certificateConfirmationMapper) {
        this.certificateConfirmationMapper = certificateConfirmationMapper;
    }

    OrderConfirmationMapper<? extends EmailData> getMapper(ItemGroup itemGroup) {
        String kind = itemGroup.getKind();
        if ("item#certificate".equals(kind)) {
            return certificateConfirmationMapper;
        } else {
            throw new IllegalArgumentException(String.format("Kind [%s] unhandled", kind));
        }
    }
}
