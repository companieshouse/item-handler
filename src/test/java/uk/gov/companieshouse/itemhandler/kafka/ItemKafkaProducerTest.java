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
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.util.TestConstants;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

/**
 * Unit tests the {@link ItemKafkaProducer} class.
 */
@RunWith(PowerMockRunner.class)
@ExtendWith(MockitoExtension.class)
@PrepareForTest({LoggingUtils.class, Logger.class})
@SuppressWarnings("squid:S5786") // public class access modifier required for JUnit 4 test
public class ItemKafkaProducerTest {

    @InjectMocks
    private ItemKafkaProducer producerUnderTest;

    @Mock
    private Message message;

    @Mock
    private Consumer consumer;

    @Mock
    private CHKafkaProducer chKafkaProducer;

    @Mock
    private Future<RecordMetadata> recordMetadataFuture;

    @Mock
    private Logger logger;

    @Mock
    private ProducerConfig producerConfig;


    @Test
    @DisplayName("sendMessage() delegates message sending to ChKafkaProducer")
    void sendMessageDelegatesToChKafkaProducer() throws ExecutionException, InterruptedException {

        // Given
        when(chKafkaProducer.sendAndReturnFuture(message)).thenReturn(recordMetadataFuture);

        // When
        producerUnderTest.sendMessage(ORDER_REFERENCE, TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID, message, consumer);

        // Then
        verify(chKafkaProducer).sendAndReturnFuture(message);

    }

    /**
     * This is a JUnit 4 test to take advantage of PowerMock.
     */
    @org.junit.Test
    public void sendMessageMeetsLoggingRequirements() throws Exception {

        // Given
        when(chKafkaProducer.sendAndReturnFuture(message)).thenReturn(recordMetadataFuture);
        setFinalStaticField(ItemKafkaProducer.class, "LOGGER", logger);
        mockStatic(LoggingUtils.class);

        // When
        producerUnderTest.sendMessage(ORDER_REFERENCE, TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID, message, consumer);

        // Then
        verifyLoggingBeforeMessageSendingIsAdequate();

    }

    @Test
    @DisplayName("modifyProducerConfig() modifies the producer config")
    void modifyProducerConfigModifiesConfig() {

        // When
        producerUnderTest.modifyProducerConfig(producerConfig);

        // Then
        verify(producerConfig).setMaxBlockMilliseconds(10000);

    }

    private void verifyLoggingBeforeMessageSendingIsAdequate() {

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.createLogMap();

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ORDER_REFERENCE_NUMBER), eq(ORDER_REFERENCE));

        PowerMockito.verifyStatic(LoggingUtils.class);
        LoggingUtils.logIfNotNull(any(Map.class), eq(ITEM_ID), eq(TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID));

        verify(logger).info(eq("Sending message to kafka topic"), any(Map.class));

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
