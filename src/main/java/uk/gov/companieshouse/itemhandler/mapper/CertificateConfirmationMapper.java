package uk.gov.companieshouse.itemhandler.mapper;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.config.EmailConfig;
import uk.gov.companieshouse.itemhandler.model.CertificateEmailData;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;

@Component
public class CertificateConfirmationMapper extends OrderConfirmationMapper<CertificateEmailData> {

    public CertificateConfirmationMapper(EmailConfig emailConfig) {
        super(emailConfig);
    }

    @Override
    protected CertificateEmailData newEmailDataInstance() {
        return new CertificateEmailData();
    }

    @Override
    protected void mapItems(DeliverableItemGroup itemGroup, CertificateEmailData certificateEmailData) {

    }
}
