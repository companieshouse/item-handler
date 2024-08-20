package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;
import uk.gov.companieshouse.itemhandler.util.DateConstants;

import java.time.LocalDate;

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

    public static final String FILING_HISTORY_VIEW_FORM_LINK =
            "%s/company/%s/filing-history/%s/document?format=pdf&download=0";

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
        String adminSubject = freeAdminCertifiedCopyOrderSubject(itemGroup.getOrder().getTotalOrderCost());
        if (itemGroup.getTimescale() == DeliveryTimescale.SAME_DAY) {
            certifiedCopyEmailData.setSubject(emailConfig.getCertifiedCopy().getExpressSubjectLine() + adminSubject);
        } else {
            certifiedCopyEmailData.setSubject(emailConfig.getCertifiedCopy().getStandardSubjectLine() + adminSubject);
        }

        itemGroup.getItems().stream()
                .map(item -> {
                    FilingHistoryDocument filingHistoryDocument =
                            ((CertifiedCopyItemOptions)item.getItemOptions()).getFilingHistoryDocuments().get(0);
                    return CertifiedCopySummary.builder()
                        .withItemNumber(item.getId())
                        .withDateFiled(mapDateFiledFormat(filingHistoryDocument))
                        .withType(filingHistoryDocument.getFilingHistoryType())
                        .withDescription(
                            filingHistoryDescriptionProviderService.mapFilingHistoryDescription(
                                    filingHistoryDocument.getFilingHistoryDescription(),
                                    filingHistoryDocument.getFilingHistoryDescriptionValues()
                            ))
                        .withCompanyNumber(item.getCompanyNumber())
                        .withFee("£" + item.getTotalItemCost())
                        .withViewFormLink(String.format(FILING_HISTORY_VIEW_FORM_LINK,
                                emailConfig.getOrdersAdminHost(),
                                item.getCompanyNumber(),
                                filingHistoryDocument.getFilingHistoryId()))
                    .build();
                }).forEach(certifiedCopyEmailData::add);
    }

    private String mapDateFiledFormat(FilingHistoryDocument filingHistoryDocument) {
        final LocalDate parsedDate = LocalDate.parse(filingHistoryDocument.getFilingHistoryDate());
        return parsedDate.format(DateConstants.DATE_FILED_FORMATTER);
    }

    private String freeAdminCertifiedCopyOrderSubject(String totalOrderCost){
        // if the total order cost is 0, then it is a free admin order
        if(totalOrderCost.equals("0")){
            return " - [FREE CERTIFIED COPY ADMIN ORDER]";
        }
        return "";
    }
}