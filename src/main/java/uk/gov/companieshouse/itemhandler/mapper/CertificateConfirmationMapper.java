package uk.gov.companieshouse.itemhandler.mapper;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.config.EmailConfig;
import uk.gov.companieshouse.itemhandler.model.CertificateEmailData;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertificateSummary;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;

@Component
public class CertificateConfirmationMapper extends OrderConfirmationMapper<CertificateEmailData> {

    private EmailConfig config;

    public CertificateConfirmationMapper(EmailConfig emailConfig) {
        super(emailConfig);
        this.config = emailConfig;
    }

    @Override
    protected CertificateEmailData newEmailDataInstance() {
        return new CertificateEmailData();
    }

    @Override
    protected void mapItems(DeliverableItemGroup itemGroup, CertificateEmailData certificateEmailData) {
        certificateEmailData.setSubject(config.getStandardCertificateSubjectLine());
        itemGroup.getItems().stream()
                .map(item -> new CertificateSummary(item.getId(),
                        ((CertificateItemOptions) item.getItemOptions()).getCertificateType(),
                        item.getCompanyNumber(),
                        "£" + item.getTotalItemCost())
                ).forEach(certificateEmailData::add);
    }
}
