package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;

@Component
public class ConfirmationMapperFactory {

    private final CertificateConfirmationMapper certificateConfirmationMapper;

    public ConfirmationMapperFactory(CertificateConfirmationMapper certificateConfirmationMapper) {
        this.certificateConfirmationMapper = certificateConfirmationMapper;
    }

    public OrderConfirmationMapper<CertificateEmailData> getCertificateMapper() {
        return certificateConfirmationMapper;
    }
}
