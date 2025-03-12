package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.PAYMENT_REFERENCE;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemPair;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

/**
 * Unit tests the {@link ItemMessageProducer} class.
 */
@ExtendWith(MockitoExtension.class)
@Import({LoggingUtils.class, Logger.class})
class ItemMessageProducerTest {

    private static final String COMPANY_NUMBER = "00006444";
    private static final String PAYMENT_REF = "payment-ref-xyz";
    private static final Item ITEM;
    private static final OrderData ORDER;
    private static final OrderItemPair ORDER_ITEM_PAIR;

    static {
        ORDER = new OrderData();
        ORDER.setReference(ORDER_REFERENCE);
        ORDER.setPaymentReference(PAYMENT_REF);
        ITEM = new Item();
        ITEM.setId(MISSING_IMAGE_DELIVERY_ITEM_ID);
        ITEM.setCompanyNumber(COMPANY_NUMBER);
        ORDER.setItems(singletonList(ITEM));
        ORDER_ITEM_PAIR = new OrderItemPair(ORDER, ITEM);
    }

    @InjectMocks
    private ItemMessageProducer messageProducerUnderTest;

    @Mock
    private ItemMessageFactory itemMessageFactory;

    @Mock
    private Message message;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("sendMessage delegates message creation to ItemMessageFactory")
    void sendMessageDelegatesMessageCreation() {

        // When
        messageProducerUnderTest.sendMessage(ORDER_ITEM_PAIR);

        // Then
        verify(itemMessageFactory).createMessage(ORDER_ITEM_PAIR);
    }

    @Test
    void sendMessageCallsMessageProducer() {

        // Given
        when(itemMessageFactory.createMessage(ORDER_ITEM_PAIR)).thenReturn(message);

        messageProducerUnderTest.sendMessage(ORDER_ITEM_PAIR);

        verify(messageProducer).sendMessage(eq(message), any(Consumer.class));
    }

    @Test
    public void sendMessageMeetsLoggingRequirements() {

        try (MockedStatic<LoggingUtils> loggingUtilsMock = mockStatic(LoggingUtils.class)) {
            Map<String, Object> mockLogMap = new HashMap<>();

            loggingUtilsMock.when(LoggingUtils::createLogMap).thenReturn(mockLogMap);
            loggingUtilsMock.when(() -> LoggingUtils.logIfNotNull(eq(mockLogMap), eq(PAYMENT_REFERENCE), eq(PAYMENT_REF)))
                    .then(invocation -> {
                        logger.info("Sending message to kafka", mockLogMap);
                        return null;
                    });

            messageProducerUnderTest.sendMessage(ORDER_ITEM_PAIR);

            verifyLoggingBeforeMessageSendingIsAdequate(loggingUtilsMock);

        }
    }

    @Test
    public void logOffsetFollowingSendIngOfMessageMeetsLoggingRequirements() {

        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        try (MockedStatic<LoggingUtils> loggingUtilsMock = mockStatic(LoggingUtils.class)) {
            Map<String, Object> mockLogMap = new HashMap<>();

            loggingUtilsMock.when(() -> LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata))
                    .thenReturn(mockLogMap);

            loggingUtilsMock.when(() -> LoggingUtils.logIfNotNull(eq(mockLogMap), eq(PAYMENT_REFERENCE), eq(PAYMENT_REF)))
                    .then(invocation -> {
                        logger.info("Sending message to kafka", mockLogMap);
                        return null;
                    });

            messageProducerUnderTest.logOffsetFollowingSendIngOfMessage(ORDER_ITEM_PAIR, recordMetadata);

            verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate(loggingUtilsMock, recordMetadata);

        }
    }

    private void verifyLoggingBeforeMessageSendingIsAdequate(MockedStatic<LoggingUtils> loggingUtilsMock) {

        loggingUtilsMock.verify(LoggingUtils::createLogMap);
        loggingUtilsMock.verify(() -> LoggingUtils.logIfNotNull(any(), eq(PAYMENT_REFERENCE), eq(PAYMENT_REF)));
        loggingUtilsMock.verify(() -> LoggingUtils.logIfNotNull(any(), eq(ITEM_ID), eq(MISSING_IMAGE_DELIVERY_ITEM_ID)), times(2));
        verify(logger).info(eq("Sending message to kafka"), any());

    }

    private void verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate(MockedStatic<LoggingUtils> loggingUtilsMock, RecordMetadata recordMetadata) {

        loggingUtilsMock.verify(() -> LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata));
        loggingUtilsMock.verify(() -> LoggingUtils.logIfNotNull(any(), eq(PAYMENT_REFERENCE), eq(PAYMENT_REF)));
        loggingUtilsMock.verify(() -> LoggingUtils.logIfNotNull(any(), eq(ITEM_ID), eq(MISSING_IMAGE_DELIVERY_ITEM_ID)), times(1));

        verify(logger).info(eq("Sending message to kafka"), any(Map.class));

    }
}