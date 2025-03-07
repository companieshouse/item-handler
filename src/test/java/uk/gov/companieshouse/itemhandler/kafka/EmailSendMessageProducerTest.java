package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.TOPIC;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

/**
 * Unit tests the {@link EmailSendMessageProducer} class.
 * TODO: rework LoggingUtils and implement using JUnit5
 */
@ExtendWith(MockitoExtension.class)
@Import({LoggingUtils.class, Logger.class})
class EmailSendMessageProducerTest {

    private static final String TOPIC_NAME = "topic";
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
    void sendMessageDelegatesMessageCreation() {

        // Given
        when(emailSendMessageFactory.createMessage(emailSend, EMAIL_SEND_TOPIC)).thenReturn(message);

        // When
        messageProducerUnderTest.sendMessage(emailSend, EMAIL_SEND_TOPIC);

        // Then
        verify(emailSendMessageFactory).createMessage(emailSend, EMAIL_SEND_TOPIC);

    }

    @Test
    @DisplayName("sendMessage delegates message sending to EmailSendKafkaProducer")
    void sendMessageDelegatesMessageSending() {

        // Given
        when(emailSendMessageFactory.createMessage(emailSend, EMAIL_SEND_TOPIC)).thenReturn(message);

        // When
        messageProducerUnderTest.sendMessage(emailSend, EMAIL_SEND_TOPIC);

        // Then
        verify(messageProducer).sendMessage(eq(message), any(Consumer.class));

    }


    @Test
    @DisplayName("sendMessage delegates message sending")
    public void sendMessageMeetsLoggingRequirements() {
        try (MockedStatic<LoggingUtils> loggingUtilsMock = mockStatic(LoggingUtils.class)) {

            // Given
            when(emailSendMessageFactory.createMessage(emailSend, EMAIL_SEND_TOPIC)).thenReturn(message);
            when(message.getTopic()).thenReturn(TOPIC_NAME);

            // When
            messageProducerUnderTest.sendMessage(emailSend, ORDER_REFERENCE);

            // Then
            verifyLoggingBeforeMessageSendingIsAdequate(loggingUtilsMock);

        }
    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @Test
    @DisplayName("log Off set Following SendIng Of Message Meets Logging Requirements")
    void logOffsetFollowingSendIngOfMessageMeetsLoggingRequirements() {
        //given
        try (MockedStatic<LoggingUtils> loggingUtilsMock = mockStatic(LoggingUtils.class)) {

            Map<String, Object> mockLogMap = new HashMap<>();
            loggingUtilsMock.when(() -> LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata))
                    .thenReturn(mockLogMap);

            loggingUtilsMock.when(() -> LoggingUtils.logIfNotNull(eq(mockLogMap), eq(ORDER_REFERENCE_NUMBER),eq(ORDER_REFERENCE)))
                    .then(invocation -> {
                        logger.info("Message sent to Kafka", mockLogMap);
                        return null;
                    });
            // When
            messageProducerUnderTest.logOffsetFollowingSendIngOfMessage(ORDER_REFERENCE, recordMetadata);

            // Then
            verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate(loggingUtilsMock);
        }
    }

    private void verifyLoggingBeforeMessageSendingIsAdequate(MockedStatic<LoggingUtils> loggingUtilsMock) {
        loggingUtilsMock.verify(() -> LoggingUtils.logWithOrderReference("Sending message to kafka", ORDER_REFERENCE));
        loggingUtilsMock.verify(() -> LoggingUtils.logIfNotNull(any(), eq(TOPIC), eq(TOPIC_NAME)));

    }

    private void verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate(MockedStatic<LoggingUtils> loggingUtilsMock) {

        loggingUtilsMock.verify(() -> LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata));
        loggingUtilsMock.verify(() -> LoggingUtils.logIfNotNull(any(), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE)));

        verify(logger).info(eq("Message sent to Kafka"), any());
    }
}