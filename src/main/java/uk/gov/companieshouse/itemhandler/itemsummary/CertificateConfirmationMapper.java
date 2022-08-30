package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertificateType;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;

@Component
public class CertificateConfirmationMapper extends OrderConfirmationMapper<CertificateEmailData> {

    private final EmailConfig config;

    public CertificateConfirmationMapper(EmailConfig emailConfig) {
        this.config = emailConfig;
    }

    @Override
    protected CertificateEmailData newEmailDataInstance() {
        return new CertificateEmailData();
    }

    @Override
    protected void mapItems(DeliverableItemGroup itemGroup, CertificateEmailData certificateEmailData) {
        certificateEmailData.setTo(config.getCertificate().getRecipient());
        if (itemGroup.getTimescale() == DeliveryTimescale.SAME_DAY) {
            certificateEmailData.setSubject(config.getCertificate().getExpressSubjectLine());
        } else {
            certificateEmailData.setSubject(config.getCertificate().getStandardSubjectLine());
        }
        itemGroup.getItems().stream()
                .map(item -> new CertificateSummary(item.getId(),
                        mapCertificateType(((CertificateItemOptions) item.getItemOptions()).getCertificateType()),
                        item.getCompanyNumber(),
                        "£" + item.getTotalItemCost())
                ).forEach(certificateEmailData::add);
    }

    private String mapCertificateType(CertificateType certificateType) {
        if (certificateType == CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES) {
            return "Incorporation with all company name changes";
        } else if (certificateType == CertificateType.DISSOLUTION) {
            return "Dissolution with all company name changes";
        } else {
            throw new NonRetryableException(String.format("Unhandled certificate type: [%s]", certificateType.toString()));
        }
    }
}
