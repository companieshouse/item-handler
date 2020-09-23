package uk.gov.companieshouse.itemhandler.kafka;

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
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
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
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_URI;

/**
 * Unit tests the {@link ItemMessageProducer} class.
 */
@RunWith(PowerMockRunner.class)
@ExtendWith(MockitoExtension.class)
@PrepareForTest({LoggingUtils.class, Logger.class})
@SuppressWarnings("squid:S5786") // public class access modifier required for JUnit 4 test
public class ItemMessageProducerTest {

    private static final String ORDER_REFERENCE = "ORD-432118-793830";
    private static final String SCAN_UPON_DEMAND_ITEM_ID = "SCD-242116-007650";
    private static final Item ITEM;

    static {
        ITEM = new Item();
        ITEM.setId(SCAN_UPON_DEMAND_ITEM_ID);
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

    @Test
    @DisplayName("sendMessage delegates message creation to ItemMessageFactory")
    void sendMessageDelegatesMessageCreation() throws Exception {

        // When
        messageProducerUnderTest.sendMessage(ORDER_REFERENCE, SCAN_UPON_DEMAND_ITEM_ID, ITEM);

        // Then
        verify(itemMessageFactory).createMessage(ITEM);

    }

    @Test
    @DisplayName("sendMessage delegates message sending to ItemKafkaProducer")
    void sendMessageDelegatesMessageSending() throws Exception {

        // Given
        when(itemMessageFactory.createMessage(ITEM)).thenReturn(message);

        // When
        messageProducerUnderTest.sendMessage(ORDER_REFERENCE, SCAN_UPON_DEMAND_ITEM_ID, ITEM);

        // Then
        verify(itemKafkaProducer).sendMessage(
                eq(ORDER_REFERENCE), eq(SCAN_UPON_DEMAND_ITEM_ID), eq(message), any(Consumer.class));

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
        messageProducerUnderTest.sendMessage(ORDER_REFERENCE, SCAN_UPON_DEMAND_ITEM_ID, ITEM);

        // Then
        // logging BEFORE message sent
        checkLoggingBeforeProductionIsAdequate();

        // logging AFTER message sent?
        // TODO GCI-1428 Can we do this?

    }

    private void checkLoggingBeforeProductionIsAdequate() {

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.createLogMap();

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_URI), eq(ORDER_REFERENCE));

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ITEM_ID), eq(SCAN_UPON_DEMAND_ITEM_ID));

        verify(logger).info(eq("Sending message to kafka producer"), any(Map.class));

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
