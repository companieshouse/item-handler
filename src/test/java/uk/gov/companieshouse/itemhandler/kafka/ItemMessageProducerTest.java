package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemPair;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.message.Message;

/**
 * Unit tests the {@link ItemMessageProducer} class.
 */
@ExtendWith(MockitoExtension.class)
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

// TODO: refactor logging utils as Spring bean
//    /**
//     * This is a JUnit 4 test to take advantage of PowerMock.
//     */
//    @org.junit.Test
//    public void sendMessageMeetsLoggingRequirements() throws ReflectiveOperationException {
//
//        // Given
//        setFinalStaticField(ItemMessageProducer.class, "LOGGER", logger);
//        mockStatic(LoggingUtils.class);
//
//        // When
//        messageProducerUnderTest.sendMessage(ORDER,
//                ORDER_REFERENCE,
//                MISSING_IMAGE_DELIVERY_ITEM_ID);
//
//        // Then
//        verifyLoggingBeforeMessageSendingIsAdequate();
//
//    }
//
//    /**
//     * This is a JUnit 4 test to take advantage of PowerMock.
//     */
//    @org.junit.Test
//    public void logOffsetFollowingSendIngOfMessageMeetsLoggingRequirements() throws ReflectiveOperationException {
//
//        // Given
//        setFinalStaticField(ItemMessageProducer.class, "LOGGER", logger);
//        mockStatic(LoggingUtils.class);
//
//        when(recordMetadata.topic()).thenReturn(TOPIC_NAME);
//        when(recordMetadata.partition()).thenReturn(PARTITION_VALUE);
//        when(recordMetadata.offset()).thenReturn(OFFSET_VALUE);
//
//        // When
//        messageProducerUnderTest.logOffsetFollowingSendIngOfMessage(ORDER, recordMetadata);
//
//        // Then
//        verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate();
//
//    }
//
//    private void verifyLoggingBeforeMessageSendingIsAdequate() {
//
//        PowerMockito.verifyStatic(LoggingUtils.class);
//        LoggingUtils.createLogMap();
//
//        PowerMockito.verifyStatic(LoggingUtils.class);
//        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE));
//
//        PowerMockito.verifyStatic(LoggingUtils.class);
//        LoggingUtils.logIfNotNull(any(Map.class), eq(ITEM_ID), eq(MISSING_IMAGE_DELIVERY_ITEM_ID));
//
//        verify(logger).info(eq("Sending message to kafka"), any(Map.class));
//
//    }
//
//    private void verifyLoggingAfterMessageAcknowledgedByKafkaServerIsAdequate() {
//
//        PowerMockito.verifyStatic(LoggingUtils.class);
//        LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(recordMetadata);
//
//        PowerMockito.verifyStatic(LoggingUtils.class);
//        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE));
//
//        PowerMockito.verifyStatic(LoggingUtils.class);
//        LoggingUtils.logIfNotNull(any(Map.class), eq(ITEM_ID), eq(MISSING_IMAGE_DELIVERY_ITEM_ID));
//
//        verify(logger).info(eq("Message sent to Kafka topic"), any(Map.class));
//
//    }
}