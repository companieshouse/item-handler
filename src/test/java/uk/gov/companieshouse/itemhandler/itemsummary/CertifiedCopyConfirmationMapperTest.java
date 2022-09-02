package uk.gov.companieshouse.itemhandler.itemsummary;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryDetails;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class CertifiedCopyConfirmationMapperTest {

    @InjectMocks
    private CertifiedCopyConfirmationMapper mapper;

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private DeliverableItemGroup deliverableItemGroup;

    @Mock
    private OrderData order;

    @Mock
    private DeliveryDetails deliveryDetails;

    @Mock
    private CertifiedCopyEmailConfig certifiedCopyEmailConfig;

    @Mock
    private FilingHistoryDescriptionProviderService filingHistoryDescriptionProviderService;

    @Test
    @DisplayName("Map certified copies with standard delivery requested to CertifiedCopyEmailData object")
    void testMapStandardDeliveryCertifiedCopiesToCertifiedCopyEmailData() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123456-123456");
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 9,2, 12, 18));
        when(emailConfig.getCertifiedCopy()).thenReturn(certifiedCopyEmailConfig);
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.STANDARD);
        when(certifiedCopyEmailConfig.getRecipient()).thenReturn("example@companieshouse.gov.uk");
        when(certifiedCopyEmailConfig.getStandardSubjectLine()).thenReturn("standard delivery subject");
        when(certifiedCopyEmailConfig.getDateFiledFormat()).thenReturn("dd MMM yyyy");
        when(deliverableItemGroup.getItems()).thenReturn(Collections.singletonList(getItem("CCD-123456-123456",false)));
        when(filingHistoryDescriptionProviderService.mapFilingHistoryDescription(any(), any())).thenReturn("ad01-description");

        // when
        EmailMetadata<CertifiedCopyEmailData> emailMetadata = mapper.map(deliverableItemGroup);

        // then
        assertThat(emailMetadata.getEmailData(), is(equalTo(CertifiedCopyEmailData.builder()
                .withTo("example@companieshouse.gov.uk")
                .withSubject("standard delivery subject")
                .withOrderReference("ORD-123456-123456")
                .withDeliveryDetails(deliveryDetails)
                .withPaymentDetails(new PaymentDetails("payment reference", "02 September 2022 - 12:18:00"))
                .addCertifiedCopy(new CertifiedCopySummary("CCD-123456-123456", "ABC123456DEF", "02 Sep 2022",
                        "AD01", "ad01-description", "12345678", "£15"))
                .build())));
        assertThat(emailMetadata.getAppId(), is("item-handler.certified-copy-summary-order-confirmation"));
        assertThat(emailMetadata.getMessageType(), is("certified_copy_summary_order_confirmation"));
        verify(filingHistoryDescriptionProviderService).mapFilingHistoryDescription(
                getFilingHistoryDocument().getFilingHistoryDescription(),
                getFilingHistoryDocument().getFilingHistoryDescriptionValues());
    }

    @Test
    @DisplayName("Map certified copies with express delivery requested to CertifiedCopyEmailData object")
    void testMapExpressDeliveryCertifiedCopiesToCertifiedCopyEmailData() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123456-123456");
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 9,2, 12, 18));
        when(emailConfig.getCertifiedCopy()).thenReturn(certifiedCopyEmailConfig);
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.SAME_DAY);
        when(certifiedCopyEmailConfig.getRecipient()).thenReturn("example@companieshouse.gov.uk");
        when(certifiedCopyEmailConfig.getExpressSubjectLine()).thenReturn("express delivery subject");
        when(certifiedCopyEmailConfig.getDateFiledFormat()).thenReturn("dd MMM yyyy");
        when(deliverableItemGroup.getItems()).thenReturn(Collections.singletonList(getItem("CCD-123456-123456", true)));
        when(filingHistoryDescriptionProviderService.mapFilingHistoryDescription(any(), any())).thenReturn("ad01-description");

        // when
        EmailMetadata<CertifiedCopyEmailData> emailMetadata = mapper.map(deliverableItemGroup);

        // then
        assertThat(emailMetadata.getEmailData(), is(equalTo(CertifiedCopyEmailData.builder()
                .withTo("example@companieshouse.gov.uk")
                .withSubject("express delivery subject")
                .withOrderReference("ORD-123456-123456")
                .withDeliveryDetails(deliveryDetails)
                .withPaymentDetails(new PaymentDetails("payment reference", "02 September 2022 - 12:18:00"))
                .addCertifiedCopy(new CertifiedCopySummary("CCD-123456-123456", "ABC123456DEF", "02 Sep 2022",
                        "AD01", "ad01-description", "12345678", "£50"))
                .build())));
        assertThat(emailMetadata.getAppId(), is("item-handler.certified-copy-summary-order-confirmation"));
        assertThat(emailMetadata.getMessageType(), is("certified_copy_summary_order_confirmation"));
        verify(filingHistoryDescriptionProviderService).mapFilingHistoryDescription(
                getFilingHistoryDocument().getFilingHistoryDescription(),
                getFilingHistoryDocument().getFilingHistoryDescriptionValues());
    }

    @Test
    @DisplayName("Map multiple certified copies items to CertificateEmailData object")
    void testMapMultipleCertifiedCopiesToCertifiedCopyEmailData() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123456-123456");
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 9,2, 12, 18));
        when(emailConfig.getCertifiedCopy()).thenReturn(certifiedCopyEmailConfig);
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.SAME_DAY);
        when(certifiedCopyEmailConfig.getRecipient()).thenReturn("example@companieshouse.gov.uk");
        when(certifiedCopyEmailConfig.getExpressSubjectLine()).thenReturn("express delivery subject");
        when(certifiedCopyEmailConfig.getDateFiledFormat()).thenReturn("dd MMM yyyy");
        when(deliverableItemGroup.getItems()).thenReturn(Arrays.asList(
                getItem("CCD-123456-123456", true),
                getItem("CCD-456789-456789", true)));
        when(filingHistoryDescriptionProviderService.mapFilingHistoryDescription(any(), any()))
                .thenReturn("ad01-description", "ad01-description");

        // when
        EmailMetadata<CertifiedCopyEmailData> emailMetadata = mapper.map(deliverableItemGroup);

        // then
        assertThat(emailMetadata.getEmailData(), is(equalTo(CertifiedCopyEmailData.builder()
                .withTo("example@companieshouse.gov.uk")
                .withSubject("express delivery subject")
                .withOrderReference("ORD-123456-123456")
                .withDeliveryDetails(deliveryDetails)
                .withPaymentDetails(new PaymentDetails("payment reference", "02 September 2022 - 12:18:00"))
                .addCertifiedCopy(new CertifiedCopySummary("CCD-123456-123456", "ABC123456DEF", "02 Sep 2022",
                        "AD01", "ad01-description", "12345678", "£50"))
                .addCertifiedCopy(new CertifiedCopySummary("CCD-456789-456789", "ABC123456DEF", "02 Sep 2022",
                        "AD01", "ad01-description", "12345678", "£50"))
                .build())));
        assertThat(emailMetadata.getAppId(), is("item-handler.certified-copy-summary-order-confirmation"));
        assertThat(emailMetadata.getMessageType(), is("certified_copy_summary_order_confirmation"));
        verify(filingHistoryDescriptionProviderService, times(2)).mapFilingHistoryDescription(
                getFilingHistoryDocument().getFilingHistoryDescription(),
                getFilingHistoryDocument().getFilingHistoryDescriptionValues());
    }

    private Item getItem(String id, boolean isExpress) {
        Item item = new Item();
        item.setCompanyNumber("12345678");
        item.setId(id);
        if (isExpress) {
            item.setTotalItemCost("50");
        } else {
            item.setTotalItemCost("15");
        }
        CertifiedCopyItemOptions certifiedCopyItemOptions = new CertifiedCopyItemOptions();
        certifiedCopyItemOptions.setFilingHistoryDocuments(Collections.singletonList(getFilingHistoryDocument()));
        item.setItemOptions(certifiedCopyItemOptions);
        return item;
    }

    private FilingHistoryDocument getFilingHistoryDocument() {
        FilingHistoryDocument filingHistoryDocument = new FilingHistoryDocument();
        filingHistoryDocument.setFilingHistoryId("ABC123456DEF");
        filingHistoryDocument.setFilingHistoryDate("2022-09-02");
        filingHistoryDocument.setFilingHistoryType("AD01");
        filingHistoryDocument.setFilingHistoryDescription("ad01-description-key");
        filingHistoryDocument.setFilingHistoryDescriptionValues(Collections.singletonMap("ad01-desc-values-key", "ad01-desc-values-value"));
        return filingHistoryDocument;
    }

}


