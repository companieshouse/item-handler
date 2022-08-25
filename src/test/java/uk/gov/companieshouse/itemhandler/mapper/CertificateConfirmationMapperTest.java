package uk.gov.companieshouse.itemhandler.mapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.config.EmailConfig;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.CertificateEmailData;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertificateSummary;
import uk.gov.companieshouse.itemhandler.model.CertificateType;
import uk.gov.companieshouse.itemhandler.model.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryDetails;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.model.PaymentDetails;

@ExtendWith(MockitoExtension.class)
public class CertificateConfirmationMapperTest {

    @InjectMocks
    private CertificateConfirmationMapper mapper;

    @Mock
    private EmailConfig config;

    @Mock
    private DeliverableItemGroup deliverableItemGroup;

    @Mock
    private OrderData order;

    @Mock
    private ActionedBy actionedBy;

    @Mock
    private DeliveryDetails deliveryDetails;

    @Mock
    private Item item;

    @Mock
    private CertificateItemOptions itemOptions;

    @Test
    @DisplayName("Map certificates with standard delivery requested to CertificateEmailData object")
    void testMapStandardDeliveryCertificatesToCertificateEmailData() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getOrderedBy()).thenReturn(actionedBy);
        when(actionedBy.getEmail()).thenReturn("example@companieshouse.gov.uk");
        when(config.getStandardCertificateSubjectLine()).thenReturn("subject");
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123123-123123");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 8,25, 15, 18));
        when(deliverableItemGroup.getItems()).thenReturn(Collections.singletonList(item));
        when(item.getId()).thenReturn("CRT-123123-123123");
        when(item.getItemOptions()).thenReturn(itemOptions);
        when(itemOptions.getCertificateType()).thenReturn(CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES);
        when(item.getCompanyNumber()).thenReturn("12345678");
        when(item.getTotalItemCost()).thenReturn("15");
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.STANDARD);

        // when
        CertificateEmailData data = mapper.map(deliverableItemGroup);

        // then
        assertThat(data, is(equalTo(CertificateEmailData.builder()
                                                        .withTo("example@companieshouse.gov.uk")
                                                        .withSubject("subject")
                                                        .withOrderReference("ORD-123123-123123")
                                                        .withDeliveryDetails(deliveryDetails)
                                                        .withPaymentDetails(new PaymentDetails("payment reference", "25 August 2022 - 15:18:00"))
                                                        .addCertificate(new CertificateSummary("CRT-123123-123123", "Incorporation with all company name changes", "12345678", "£15"))
                                                        .build())));
    }

    @Test
    @DisplayName("Map certificates with express delivery requested to CertificateEmailData object")
    void testMapExpressDeliveryCertificatesToCertificateEmailData() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getOrderedBy()).thenReturn(actionedBy);
        when(actionedBy.getEmail()).thenReturn("example@companieshouse.gov.uk");
        when(config.getExpressCertificateSubjectLine()).thenReturn("subject");
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123123-123123");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 8,25, 15, 18));
        when(deliverableItemGroup.getItems()).thenReturn(Collections.singletonList(item));
        when(item.getId()).thenReturn("CRT-123123-123123");
        when(item.getItemOptions()).thenReturn(itemOptions);
        when(itemOptions.getCertificateType()).thenReturn(CertificateType.DISSOLUTION);
        when(item.getCompanyNumber()).thenReturn("12345678");
        when(item.getTotalItemCost()).thenReturn("15");
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.SAME_DAY);

        // when
        CertificateEmailData data = mapper.map(deliverableItemGroup);

        // then
        assertThat(data, is(equalTo(CertificateEmailData.builder()
                .withTo("example@companieshouse.gov.uk")
                .withSubject("subject")
                .withOrderReference("ORD-123123-123123")
                .withDeliveryDetails(deliveryDetails)
                .withPaymentDetails(new PaymentDetails("payment reference", "25 August 2022 - 15:18:00"))
                .addCertificate(new CertificateSummary("CRT-123123-123123", "Dissolution with all company name changes", "12345678", "£15"))
                .build())));
    }

    @Test
    @DisplayName("Map multiple certificate items to CertificateEmailData object")
    void testMapMultipleCertificatesToCertificateEmailData() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getOrderedBy()).thenReturn(actionedBy);
        when(actionedBy.getEmail()).thenReturn("example@companieshouse.gov.uk");
        when(config.getStandardCertificateSubjectLine()).thenReturn("subject");
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123123-123123");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 8,25, 15, 18));
        when(deliverableItemGroup.getItems()).thenReturn(Arrays.asList(item, item));
        when(item.getId()).thenReturn("CRT-123123-123123");
        when(item.getItemOptions()).thenReturn(itemOptions);
        when(itemOptions.getCertificateType()).thenReturn(CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES, CertificateType.DISSOLUTION);
        when(item.getCompanyNumber()).thenReturn("12345678");
        when(item.getTotalItemCost()).thenReturn("15");
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.STANDARD);

        // when
        CertificateEmailData data = mapper.map(deliverableItemGroup);

        // then
        assertThat(data, is(equalTo(CertificateEmailData.builder()
                .withTo("example@companieshouse.gov.uk")
                .withSubject("subject")
                .withOrderReference("ORD-123123-123123")
                .withDeliveryDetails(deliveryDetails)
                .withPaymentDetails(new PaymentDetails("payment reference", "25 August 2022 - 15:18:00"))
                .addCertificate(new CertificateSummary("CRT-123123-123123", "Incorporation with all company name changes", "12345678", "£15"))
                .addCertificate(new CertificateSummary("CRT-123123-123123", "Dissolution with all company name changes", "12345678", "£15"))
                .build())));
    }

    @Test
    @DisplayName("Throw NonRetryableException if certificate type unhandled")
    void testThrowNonRetryableExceptionIfCertificateTyoeUnhandled() {
        // given
        when(deliverableItemGroup.getOrder()).thenReturn(order);
        when(order.getOrderedBy()).thenReturn(actionedBy);
        when(actionedBy.getEmail()).thenReturn("example@companieshouse.gov.uk");
        when(config.getExpressCertificateSubjectLine()).thenReturn("subject");
        when(order.getDeliveryDetails()).thenReturn(deliveryDetails);
        when(order.getReference()).thenReturn("ORD-123123-123123");
        when(order.getOrderedAt()).thenReturn(LocalDateTime.of(2022, 8,25, 15, 18));
        when(deliverableItemGroup.getItems()).thenReturn(Collections.singletonList(item));
        when(item.getId()).thenReturn("CRT-123123-123123");
        when(item.getItemOptions()).thenReturn(itemOptions);
        when(itemOptions.getCertificateType()).thenReturn(CertificateType.INCORPORATION);
        when(order.getPaymentReference()).thenReturn("payment reference");
        when(deliverableItemGroup.getTimescale()).thenReturn(DeliveryTimescale.SAME_DAY);

        // when
        Executable executable = () -> mapper.map(deliverableItemGroup);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertThat(exception.getMessage(), is(equalTo("Unhandled certificate type: [INCORPORATION]")));
    }
}
