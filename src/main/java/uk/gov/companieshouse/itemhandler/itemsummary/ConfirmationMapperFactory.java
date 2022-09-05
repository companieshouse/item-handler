package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;

@Component
public class ConfirmationMapperFactory {

    private final CertificateConfirmationMapper certificateConfirmationMapper;
    private final CertifiedCopyConfirmationMapper certifiedCopyConfirmationMapper;

    public ConfirmationMapperFactory(CertificateConfirmationMapper certificateConfirmationMapper,
                                     CertifiedCopyConfirmationMapper certifiedCopyConfirmationMapper) {
        this.certificateConfirmationMapper = certificateConfirmationMapper;
        this.certifiedCopyConfirmationMapper = certifiedCopyConfirmationMapper;
    }

    public OrderConfirmationMapper<CertificateEmailData> getCertificateMapper() {
        return certificateConfirmationMapper;
    }

    public OrderConfirmationMapper<CertifiedCopyEmailData> getCertifiedCopyMapper() {
        return certifiedCopyConfirmationMapper;
    }
}
