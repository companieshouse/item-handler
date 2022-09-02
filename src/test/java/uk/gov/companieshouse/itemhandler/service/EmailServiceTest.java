package uk.gov.companieshouse.itemhandler.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.model.DeliveryTimescale.SAME_DAY;
import static uk.gov.companieshouse.itemhandler.model.DeliveryTimescale.STANDARD;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.config.FeatureOptions;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.email.ItemOrderConfirmation;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.itemsummary.CertificateEmailData;
import uk.gov.companieshouse.itemhandler.itemsummary.ConfirmationMapperFactory;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.itemsummary.EmailMetadata;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToItemOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.DeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

/** Unit tests the {@link EmailService} class. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String EMAIL_CONTENT = "Hi there!";
    private static final String TEST_EXCEPTION_MESSAGE = "Test message!";
    private final static String ITEM_TYPE_CERTIFICATE = "certificate";
    private final static String ITEM_TYPE_CERTIFIED_COPY = "certified-copy";
    private final static String ITEM_TYPE_MISSING_IMAGE_DELIVERY = "missing-image-delivery";
    private final static String ITEM_TYPE_UNKNOWN = "unknown";

    @InjectMocks
    private EmailService emailServiceUnderTest;

    @Mock
    private OrderDataToCertificateOrderConfirmationMapper orderToCertificateOrderConfirmationMapper;

    @Mock
    private OrderDataToItemOrderConfirmationMapper orderToItemOrderConfirmationMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmailSendMessageProducer producer;

    @Mock
    private OrderData order;

    @Mock
    private FeatureOptions featureOptions;

    @Mock
    private List<Item> items;

    @Mock
    private Item item;

    @Mock
    private CertificateOrderConfirmation certificateOrderConfirmation;

    @Mock
    private ItemOrderConfirmation itemOrderConfirmation;

    @Mock
    private DeliveryItemOptions deliveryItemOptions;

    @Captor
    private ArgumentCaptor<EmailSend> emailCaptor;

    @Mock
    private DeliverableItemGroup itemGroup;

    @Mock
    private ConfirmationMapperFactory confirmationMapperFactory;

    @Mock
    private OrderConfirmationMapper<CertificateEmailData> certificateConfirmationMapper;

    @Mock
    private CertificateEmailData data;

    @Mock
    private EmailMetadata<CertificateEmailData> metadata;

    /** Extends {@link JsonProcessingException} so it can be instantiated in these tests. */
    private static class TestJsonProcessingException extends JsonProcessingException {

        protected TestJsonProcessingException(String msg) {
            super(msg);
        }
    }

    @Test
    @DisplayName("Email service handles certified certificate emails correctly")
    void serviceCallsMapMethodOnCertificateConfirmationMapper() throws JsonProcessingException {
        // given
        when(confirmationMapperFactory.getCertificateMapper()).thenReturn(certificateConfirmationMapper);
        when(certificateConfirmationMapper.map(any())).thenReturn(metadata);
        when(metadata.getEmailData()).thenReturn(data);
        when(metadata.getAppId()).thenReturn("appId");
        when(metadata.getMessageType()).thenReturn("messageType");
        when(objectMapper.writeValueAsString(any())).thenReturn("data");
        when(order.getReference()).thenReturn("ORD-123123-123123");

        // when
        emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certificate", STANDARD));

        // then
        verify(producer).sendMessage(emailCaptor.capture(), eq("ORD-123123-123123"));
        assertThat(emailCaptor.getValue().getAppId(), is(equalTo("appId")));
        assertThat(emailCaptor.getValue().getMessageType(), is(equalTo("messageType")));
        assertThat(emailCaptor.getValue().getData(), is(equalTo("data")));
        assertThat(emailCaptor.getValue().getMessageId(), is(notNullValue()));
        assertThat(emailCaptor.getValue().getCreatedAt(), is(notNullValue()));
    }

    @Test
    @DisplayName("Sends certified copy order confirmation successfully")
    void sendsCertifiedCopyOrderConfirmation() throws Exception {

        // Given
        final LocalDateTime intervalStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        when(orderToItemOrderConfirmationMapper.orderToConfirmation(order))
                .thenReturn(itemOrderConfirmation);
        when(objectMapper.writeValueAsString(itemOrderConfirmation)).thenReturn(EMAIL_CONTENT);
        when(itemOrderConfirmation.getOrderReferenceNumber()).thenReturn("456");
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(((DeliveryItemOptions) item.getItemOptions())).thenReturn(deliveryItemOptions);
        when(deliveryItemOptions.getDeliveryTimescale()).thenReturn(STANDARD);

        // When
        emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certified-copy", STANDARD));
        final LocalDateTime intervalEnd = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusNanos(1000000);

        // Then
        verify(producer).sendMessage(emailCaptor.capture(), any(String.class));
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.certified-copy-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("certified_copy_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        verifyCreationTimestampWithinExecutionInterval(emailSent, intervalStart, intervalEnd);
    }

    @Test
    @DisplayName("Sends same day certified copy order confirmation successfully")
    void sendsSameDayCertifiedCopyOrderConfirmation() throws Exception {

        // Given
        final LocalDateTime intervalStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        when(orderToItemOrderConfirmationMapper.orderToConfirmation(order))
                .thenReturn(itemOrderConfirmation);
        when(objectMapper.writeValueAsString(itemOrderConfirmation)).thenReturn(EMAIL_CONTENT);
        when(itemOrderConfirmation.getOrderReferenceNumber()).thenReturn("456");
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(((DeliveryItemOptions) item.getItemOptions())).thenReturn(deliveryItemOptions);
        when(deliveryItemOptions.getDeliveryTimescale()).thenReturn(SAME_DAY);

        // When
        emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certified-copy", SAME_DAY));
        final LocalDateTime intervalEnd = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusNanos(1000000);

        // Then
        verify(producer).sendMessage(emailCaptor.capture(), any(String.class));
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.same-day-certified-copy-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("same_day_certified_copy_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        verifyCreationTimestampWithinExecutionInterval(emailSent, intervalStart, intervalEnd);
    }

    @Test
    void propagatesNonRetryableExceptionIfJsonProcessExceptionThrownWhenSerialisingCertificate() throws Exception  {

        // Given
        when(confirmationMapperFactory.getCertificateMapper()).thenReturn(certificateConfirmationMapper);
        when(certificateConfirmationMapper.map(any())).thenReturn(metadata);
        when(metadata.getEmailData()).thenReturn(data);
        when(objectMapper.writeValueAsString(data)).thenThrow(new TestJsonProcessingException(TEST_EXCEPTION_MESSAGE));
        when(order.getReference()).thenReturn("ORD-123456-123456");

        // When
        Executable executable = () -> emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certificate", STANDARD));

        // Then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Error converting order (ORD-123456-123456) confirmation to JSON", actual.getMessage());
    }

    @Test
    void propagatesNonRetryableExceptionIfJsonProcessExceptionThrownWhenSerialisingCertifiedCopy() throws Exception  {

        // Given
        when(order.getItems()).thenReturn(Collections.singletonList(item));
        when(item.getItemOptions()).thenReturn(deliveryItemOptions);
        when(deliveryItemOptions.getDeliveryTimescale()).thenReturn(STANDARD);
        when(orderToItemOrderConfirmationMapper.orderToConfirmation(order)).thenReturn(itemOrderConfirmation);
        when(objectMapper.writeValueAsString(itemOrderConfirmation)).thenThrow(new TestJsonProcessingException(TEST_EXCEPTION_MESSAGE));
        when(order.getReference()).thenReturn("ORD-123456-123456");

        // When
        Executable executable = () -> emailServiceUnderTest.sendOrderConfirmation(new DeliverableItemGroup(order, "item#certified-copy", STANDARD));

        // Then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Error converting order (ORD-123456-123456) confirmation to JSON", actual.getMessage());
    }

    /**
     * Verifies that the email created at timestamp is within the expected interval
     * for the creation.
     * @param emailSent the email
     */
    private void verifyCreationTimestampWithinExecutionInterval(final EmailSend emailSent,
                                                                final LocalDateTime intervalStart,
                                                                final LocalDateTime intervalEnd) {
        final LocalDateTime createdAt = LocalDateTime.parse(emailSent.getCreatedAt());
        assertThat(createdAt.isAfter(intervalStart) || createdAt.isEqual(intervalStart), is(true));
        assertThat(createdAt.isBefore(intervalEnd) || createdAt.isEqual(intervalEnd), is(true));
    }
}