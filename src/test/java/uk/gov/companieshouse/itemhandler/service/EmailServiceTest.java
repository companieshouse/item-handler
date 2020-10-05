package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.email.CertifiedCopyOrderConfirmation;
import uk.gov.companieshouse.itemhandler.email.MissingImageDeliveryOrderConfirmation;
import uk.gov.companieshouse.itemhandler.exception.ServiceException;
import uk.gov.companieshouse.itemhandler.email.ItemOrderConfirmation;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertifiedCopyOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToMissingImageDeliveryOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.mapper.OrderDataToItemOrderConfirmationMapper;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private OrderDataToMissingImageDeliveryOrderConfirmationMapper orderDataToMissingImageDeliveryOrderConfirmationMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmailSendMessageProducer producer;

    @Mock
    private OrderData order;

    @Mock
    private List<Item> items;

    @Mock
    private Item item;

    @Mock
    private CertificateOrderConfirmation certificateOrderConfirmation;

    @Mock
    private ItemOrderConfirmation itemOrderConfirmation;

    @Mock
    private MissingImageDeliveryOrderConfirmation missingImageDeliveryOrderConfirmation;

    @Captor
    ArgumentCaptor<EmailSend> emailCaptor;

    /** Extends {@link JsonProcessingException} so it can be instantiated in these tests. */
    private static class TestJsonProcessingException extends JsonProcessingException {

        protected TestJsonProcessingException(String msg) {
            super(msg);
        }
    }

    /** Extends {@link ExecutionException} so it can be instantiated in these tests. */
    private static class TestExecutionException extends ExecutionException {

        protected TestExecutionException(String msg) {
            super(msg);
        }
    }

    @Test
    @DisplayName("Sends certificate order confirmation successfully")
    void sendsCertificateOrderConfirmation() throws Exception {

        // Given
        final LocalDateTime intervalStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        when(orderToCertificateOrderConfirmationMapper.orderToConfirmation(order)).thenReturn(certificateOrderConfirmation);
        when(objectMapper.writeValueAsString(certificateOrderConfirmation)).thenReturn(EMAIL_CONTENT);
        when(certificateOrderConfirmation.getOrderReferenceNumber()).thenReturn("123");
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFICATE);

        // When
        emailServiceUnderTest.sendOrderConfirmation(order);
        final LocalDateTime intervalEnd = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusNanos(1000000);

        // Then
        verify(producer).sendMessage(emailCaptor.capture(), any(String.class));
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.certificate-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("certificate_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        verifyCreationTimestampWithinExecutionInterval(emailSent, intervalStart, intervalEnd);
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
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFIED_COPY);

        // When
        emailServiceUnderTest.sendOrderConfirmation(order);
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

    @Test void sendMissingImageDeliveryOrderConfirmation() throws Exception {

        // Given
        final LocalDateTime intervalStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        when(orderToItemOrderConfirmationMapper.orderToConfirmation(order))
            .thenReturn(itemOrderConfirmation);
        when(objectMapper.writeValueAsString(itemOrderConfirmation)).thenReturn(EMAIL_CONTENT);
        when(itemOrderConfirmation.getOrderReferenceNumber()).thenReturn("456");
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_MISSING_IMAGE_DELIVERY);

        // When
        emailServiceUnderTest.sendOrderConfirmation(order);
        final LocalDateTime intervalEnd = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusNanos(1000000);

        // Then
        verify(producer).sendMessage(emailCaptor.capture(), any(String.class));
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.missing-image-delivery-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("missing_image_delivery_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        verifyCreationTimestampWithinExecutionInterval(emailSent, intervalStart, intervalEnd);

    }

    @Test
    @DisplayName("Sends missing image delivery order confirmation successfully")
    void sendsMissingImageDeliveryOrderConfirmation() throws Exception {

        // Given
        final LocalDateTime intervalStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        when(orderDataToMissingImageDeliveryOrderConfirmationMapper.orderToConfirmation(order))
                .thenReturn(missingImageDeliveryOrderConfirmation);
        when(objectMapper.writeValueAsString(missingImageDeliveryOrderConfirmation)).thenReturn(EMAIL_CONTENT);
        when(missingImageDeliveryOrderConfirmation.getOrderReferenceNumber()).thenReturn("456");
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_MISSING_IMAGE_DELIVERY);

        // When
        emailServiceUnderTest.sendOrderConfirmation(order);
        final LocalDateTime intervalEnd = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusNanos(1000000);

        // Then
        verify(producer).sendMessage(emailCaptor.capture(), any(String.class));
        final EmailSend emailSent = emailCaptor.getValue();
        assertThat(emailSent.getAppId(), is("item-handler.missing-image-delivery-order-confirmation"));
        assertThat(emailSent.getMessageId(), is(notNullValue(String.class)));
        assertThat(emailSent.getMessageType(), is("missing_image_delivery_order_confirmation_email"));
        assertThat(emailSent.getData(), is(EMAIL_CONTENT));
        assertThat(emailSent.getEmailAddress(), is("chs-orders@ch.gov.uk"));
        verifyCreationTimestampWithinExecutionInterval(emailSent, intervalStart, intervalEnd);
    }

    @Test
    @DisplayName("Errors clearly for unknown description ID value (item type)")
    void errorsClearlyForUnknownItemType() {

        // Given
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_UNKNOWN);
        when(order.getReference()).thenReturn("456");

        // When and then
        assertThatExceptionOfType(ServiceException.class).isThrownBy(() ->
                emailServiceUnderTest.sendOrderConfirmation(order))
                .withMessage("Unable to determine order confirmation type from description ID unknown!")
                .withNoCause();

    }

    @Test
    @DisplayName("Propagates JsonProcessingException")
    void propagatesJsonProcessingException() throws Exception  {

        // Given
        when(orderToCertificateOrderConfirmationMapper.orderToConfirmation(order)).thenReturn(certificateOrderConfirmation);
        when(objectMapper.writeValueAsString(certificateOrderConfirmation)).thenThrow(new TestJsonProcessingException(TEST_EXCEPTION_MESSAGE));
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFICATE);

        // When and then
        thenExceptionIsPropagated(JsonProcessingException.class);
    }

    @Test
    @DisplayName("Propagates SerializationException")
    void propagatesSerializationException() throws Exception  {
        propagatesException(SerializationException::new, SerializationException.class);
    }

    @Test
    @DisplayName("Propagates ExecutionException")
    void propagatesExecutionException() throws Exception  {
        propagatesException(TestExecutionException::new, ExecutionException.class);
    }

    @Test
    @DisplayName("Propagates InterruptedException")
    void propagatesInterruptedException() throws Exception  {
        propagatesException(InterruptedException::new, InterruptedException.class);
    }

    /**
     * Verifies that an exception thrown by {@link EmailSendMessageProducer#sendMessage(EmailSend, String)} is propagated by
     * {@link EmailService#sendOrderConfirmation(OrderData)}.
     * @param constructor the Exception constructor to use
     * @param exception the class of the exception to be thrown
     * @throws Exception should something unexpected happen
     */
    private void propagatesException(final Function<String, Exception> constructor,
                                     final Class<? extends Throwable> exception) throws Exception {
        // Given
        givenSendMessageThrowsException(constructor);

        // When and then
        thenExceptionIsPropagated(exception);
    }

    /**
     * Sets up mocks to throw the exception for which the constructor is provided when the service calls
     * {@link EmailSendMessageProducer#sendMessage(EmailSend, String)}.
     * @param constructor the Exception constructor to use
     * @throws Exception should something unexpected happen
     */
    private void givenSendMessageThrowsException(final Function<String, Exception> constructor) throws Exception {
        when(orderToCertificateOrderConfirmationMapper.orderToConfirmation(order)).thenReturn(certificateOrderConfirmation);
        when(objectMapper.writeValueAsString(certificateOrderConfirmation)).thenReturn(EMAIL_CONTENT);
        when(certificateOrderConfirmation.getOrderReferenceNumber()).thenReturn("123");
        when(order.getItems()).thenReturn(items);
        when(items.get(0)).thenReturn(item);
        when(item.getDescriptionIdentifier()).thenReturn(ITEM_TYPE_CERTIFICATE);
        doThrow(constructor.apply(TEST_EXCEPTION_MESSAGE)).when(producer).sendMessage(any(EmailSend.class), any(String.class));
    }

    /**
     * Asserts that an exception of the type indicated is thrown by
     * {@link EmailService#sendOrderConfirmation(OrderData)}.
     * @param exception the class of the exception to be thrown
     */
    private void thenExceptionIsPropagated(final Class<? extends Throwable> exception) {
        assertThatExceptionOfType(exception).isThrownBy(() ->
        emailServiceUnderTest.sendOrderConfirmation(order))
                .withMessage(TEST_EXCEPTION_MESSAGE)
                .withNoCause();
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
