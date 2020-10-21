package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.gov.companieshouse.itemhandler.exception.RetryableErrorException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

/**
 * Unit tests the {@link ItemMessageProducer} class.
 */
@RunWith(PowerMockRunner.class)
@ExtendWith(MockitoExtension.class)
@PrepareForTest({LoggingUtils.class, Logger.class})
@SuppressWarnings("squid:S5786") // public class access modifier required for JUnit 4 test
public class ItemMessageProducerTest {

    private static final long OFFSET_VALUE = 1L;
    private static final String TOPIC_NAME = "topic";
    private static final int PARTITION_VALUE = 0;
    private static final String COMPANY_NUMBER      = "00006444";
    private static final String PAYMENT_REF         = "payment-ref-xyz";
    private static final Item ITEM;
    private static final RuntimeException KAFKA_EXCEPTION = new RuntimeException("Test exception");
    private static final OrderData ORDER;

    static {
        ORDER = new OrderData();
        ORDER.setReference(ORDER_REFERENCE);
        ORDER.setPaymentReference(PAYMENT_REF);
        ITEM = new Item();
        ITEM.setId(MISSING_IMAGE_DELIVERY_ITEM_ID);
        ITEM.setCompanyNumber(COMPANY_NUMBER);
        ORDER.setItems(singletonList(ITEM));
    }

    @InjectMocks
    private ItemMessageProducer messageProducerUnderTest;

    @Mock
    private ItemMessageFactory itemMessageFactory;

    @Mock
    private ItemKafkaProducer itemKafkaProducer;

    @Mock
    private Message message;

    @Mock
    private Logger logger;

    @Mock
    private RecordMetadata recordMetadata;

    @Test
    @DisplayName("sendMessage delegates message creation to ItemMessageFactory")
    void sendMessageDelegatesMessageCreation() {

        // When
        messageProducerUnderTest.sendMessage(ORDER, ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID);

        // Then
        verify(itemMessageFactory).createMessage(ORDER);

    }

    @Test
    @DisplayName("sendMessage delegates message sending to ItemKafkaProducer")
    void sendMessageDelegatesMessageSending() throws Exception {

        // Given
        when(itemMessageFactory.createMessage(ORDER)).thenReturn(message);

        // When
        messageProducerUnderTest.sendMessage(ORDER, ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID);

        // Then
        verify(itemKafkaProducer).sendMessage(
                eq(ORDER_REFERENCE), eq(MISSING_IMAGE_DELIVERY_ITEM_ID), eq(message), any(Consumer.class));

    }

    @Test
    @DisplayName("sendMessage propagates ItemKafkaProducer exception as a RetryableErrorException")
    void sendMessagePropagatesProductionExceptionAsRetryableErrorException() throws Exception {

        // Given
        when(itemMessageFactory.createMessage(ORDER)).thenReturn(message);

        doThrow(KAFKA_EXCEPTION).
        when(itemKafkaProducer).sendMessage(
                eq(ORDER_REFERENCE),
                eq(MISSING_IMAGE_DELIVERY_ITEM_ID),
                eq(message),
                any(Consumer.class));

        // When and then
        assertThatExceptionOfType(RetryableErrorException.class).isThrownBy(() ->
                messageProducerUnderTest.sendMessage(ORDER, ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID))
                .withMessage("Kafka item message could not be sent for order reference ORD-432118-793830 item " +
                        "ID MID-242116-007650")
                .withCause(KAFKA_EXCEPTION);

    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @org.junit.Test
    public void sendMessageMeetsLoggingRequirements() throws ReflectiveOperationException {

        // Given
        setFinalStaticField(ItemMessageProducer.class, "LOGGER", logger);
        mockStatic(LoggingUtils.class);

        // When
        messageProducerUnderTest.sendMessage(ORDER, ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID);

        // Then
        verifyLoggingBeforeMessageSendingIsAdequate();

    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @org.junit.Test
    public void logOffsetFollowingSendIngOfMessageMeetsLoggingRequirements() throws ReflectiveOperationException {

        // Given
        setFinalStaticField(ItemMessageProducer.class, "LOGGER", logger);
        mockStatic(LoggingUtils.class);

        when(recordMetadata.topic()).thenReturn(TOPIC_NAME);
        when(recordMetadata.partition()).thenReturn(PARTITION_VALUE);
        when(recordMetadata.offset()).thenReturn(OFFSET_VALUE);

        // When
        messageProducerUnderTest.logOffsetFollowingSendIngOfMessage(ORDER, recordMetadata);

        // Then
        verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate();

    }

    private void verifyLoggingBeforeMessageSendingIsAdequate() {

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.createLogMap();

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE));

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ITEM_ID), eq(MISSING_IMAGE_DELIVERY_ITEM_ID));

        verify(logger).info(eq("Sending message to kafka producer"), any(Map.class));

    }

    private void verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate() {

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata);

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE));

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ITEM_ID), eq(MISSING_IMAGE_DELIVERY_ITEM_ID));

        verify(logger).info(eq("Message sent to Kafka topic"), any(Map.class));

    }

    /**
     * Utility method (hack) to allow us to change a private static final field.
     * See https://dzone.com/articles/how-to-change-private-static-final-fields
     * @param clazz the class holding the field
     * @param fieldName the name of the private static final field to set
     * @param value the value to set the field to
     * @throws ReflectiveOperationException should something unexpected happen
     */
    private static void setFinalStaticField(Class<?> clazz, String fieldName, Object value)
            throws ReflectiveOperationException {
        final Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        final Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

}
