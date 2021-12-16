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
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.TOPIC;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

/**
 * Unit tests the {@link EmailSendMessageProducer} class.
 *
 * TODO: rework LoggingUtils and implement using JUnit5
 */
@RunWith(PowerMockRunner.class)
@ExtendWith(MockitoExtension.class)
@PrepareForTest({LoggingUtils.class, Logger.class})
@SuppressWarnings("squid:S5786") // public class access modifier required for JUnit 4 test
public class EmailSendMessageProducerTest {

    private static final long OFFSET_VALUE = 1L;
    private static final String TOPIC_NAME = "topic";
    private static final int PARTITION_VALUE = 0;
    private static final String EMAIL_SEND_TOPIC = "email-send";

    @InjectMocks
    private EmailSendMessageProducer messageProducerUnderTest;

    @Mock
    private MessageSerialiserFactory emailSendMessageFactory;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private Message message;

    @Mock
    private Logger logger;

    @Mock
    private RecordMetadata recordMetadata;

    @Mock
    private EmailSend emailSend;

    @Test
    @DisplayName("sendMessage delegates message creation to EmailSendMessageFactory")
    void sendMessageDelegatesMessageCreation() throws Exception {

        // Given
        when(emailSendMessageFactory.createMessage(emailSend, EMAIL_SEND_TOPIC)).thenReturn(message);

        // When
        messageProducerUnderTest.sendMessage(emailSend, EMAIL_SEND_TOPIC);

        // Then
        verify(emailSendMessageFactory).createMessage(emailSend, EMAIL_SEND_TOPIC);

    }

    @Test
    @DisplayName("sendMessage delegates message sending to EmailSendKafkaProducer")
    void sendMessageDelegatesMessageSending() throws Exception {

        // Given
        when(emailSendMessageFactory.createMessage(emailSend, EMAIL_SEND_TOPIC)).thenReturn(message);

        // When
        messageProducerUnderTest.sendMessage(emailSend, EMAIL_SEND_TOPIC);

        // Then
        verify(messageProducer).sendMessage(eq(message), any(Consumer.class));

    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @org.junit.Test
    public void sendMessageMeetsLoggingRequirements() throws Exception {

        // Given
        mockStatic(LoggingUtils.class);

        when(emailSendMessageFactory.createMessage(emailSend, EMAIL_SEND_TOPIC)).thenReturn(message);
        when(message.getTopic()).thenReturn(TOPIC_NAME);

        // When
        messageProducerUnderTest.sendMessage(emailSend, ORDER_REFERENCE);

        // Then
        verifyLoggingBeforeMessageSendingIsAdequate();

    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @org.junit.Test
    public void logOffsetFollowingSendIngOfMessageMeetsLoggingRequirements() throws ReflectiveOperationException {

        // Given
        setFinalStaticField(EmailSendMessageProducer.class, "LOGGER", logger);
        mockStatic(LoggingUtils.class);

        when(recordMetadata.topic()).thenReturn(TOPIC_NAME);
        when(recordMetadata.partition()).thenReturn(PARTITION_VALUE);
        when(recordMetadata.offset()).thenReturn(OFFSET_VALUE);

        // When
        messageProducerUnderTest.logOffsetFollowingSendIngOfMessage(ORDER_REFERENCE, recordMetadata);

        // Then
        verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate();

    }

    private void verifyLoggingBeforeMessageSendingIsAdequate() {

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logWithOrderReference("Sending message to kafka", ORDER_REFERENCE);

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(TOPIC), eq(TOPIC_NAME));

    }

    private void verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate() {

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata);

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE));

        verify(logger).info(eq("Message sent to Kafka"), any(Map.class));

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
