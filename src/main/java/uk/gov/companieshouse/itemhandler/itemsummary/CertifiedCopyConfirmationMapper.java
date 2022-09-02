package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class CertifiedCopyConfirmationMapper extends OrderConfirmationMapper<CertifiedCopyEmailData> {
    private final EmailConfig emailConfig;
    private final FilingHistoryDescriptionProviderService filingHistoryDescriptionProviderService;

    public CertifiedCopyConfirmationMapper(final EmailConfig emailConfig, final FilingHistoryDescriptionProviderService
            filingHistoryDescriptionProviderService) {
        this.emailConfig = emailConfig;
        this.filingHistoryDescriptionProviderService = filingHistoryDescriptionProviderService;
    }

    public static final String CERTIFIED_COPY_SUMMARY_ORDER_CONFIRMATION_APP_ID =
            "item-handler.certified-copy-summary-order-confirmation";

    public static final String CERTIFIED_COPY_SUMMARY_ORDER_CONFIRMATION_MESSAGE_TYPE =
            "certified_copy_summary_order_confirmation";

    @Override
    protected CertifiedCopyEmailData newEmailDataInstance() {
        return new CertifiedCopyEmailData();
    }

    @Override
    protected EmailMetadata<CertifiedCopyEmailData> newEmailMetadataInstance(CertifiedCopyEmailData emailData) {
        return new EmailMetadata<>(CERTIFIED_COPY_SUMMARY_ORDER_CONFIRMATION_APP_ID,
                CERTIFIED_COPY_SUMMARY_ORDER_CONFIRMATION_MESSAGE_TYPE, emailData);
    }

    @Override
    protected void mapItems(DeliverableItemGroup itemGroup, CertifiedCopyEmailData certifiedCopyEmailData) {
        certifiedCopyEmailData.setTo(emailConfig.getCertifiedCopy().getRecipient());
        if (itemGroup.getTimescale() == DeliveryTimescale.SAME_DAY) {
            certifiedCopyEmailData.setSubject(emailConfig.getCertifiedCopy().getExpressSubjectLine());
        } else {
            certifiedCopyEmailData.setSubject(emailConfig.getCertifiedCopy().getStandardSubjectLine());
        }

        itemGroup.getItems().stream()
                .map(item -> {
                    FilingHistoryDocument filingHistoryDocument =
                            ((CertifiedCopyItemOptions)item.getItemOptions()).getFilingHistoryDocuments().get(0);
                    return CertifiedCopySummary.builder()
                        .withItemNumber(item.getId())
                        .withFilingHistoryId(filingHistoryDocument.getFilingHistoryId())
                        .withDateFiled(mapDateFiledFormat(filingHistoryDocument))
                        .withType(filingHistoryDocument.getFilingHistoryType())
                        .withDescription(
                            filingHistoryDescriptionProviderService.mapFilingHistoryDescription(
                                    filingHistoryDocument.getFilingHistoryDescription(),
                                    filingHistoryDocument.getFilingHistoryDescriptionValues()
                            ))
                        .withCompanyNumber(item.getCompanyNumber())
                        .withFee("£" + item.getTotalItemCost())
                    .build();
                }).forEach(certifiedCopyEmailData::add);
    }

    private String mapDateFiledFormat(FilingHistoryDocument filingHistoryDocument) {
        final LocalDate parsedDate = LocalDate.parse(filingHistoryDocument.getFilingHistoryDate());
        return parsedDate.format(DateTimeFormatter.ofPattern(emailConfig.getCertifiedCopy().getDateFiledFormat()));
    }
}
