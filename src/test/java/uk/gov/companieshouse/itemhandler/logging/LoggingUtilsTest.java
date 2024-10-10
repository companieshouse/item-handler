package uk.gov.companieshouse.itemhandler.logging;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingUtilsTest {
    
    private static final String TOPIC_VALUE = "topic";
    private static final String KEY_VALUE = "key";
    private static final int PARTITION_VALUE = 1;
    private static final long OFFSET_VALUE = 2L;
    private static final String ORDER_REFERENCE = "order reference";
    private static final String LOG_MESSAGE = "log message";
    @Mock
    org.springframework.messaging.Message<OrderReceived> orderReceivedMessage;
    @Mock
    MessageHeaders messageHeaders;

    @Mock
    private RecordMetadata acknowledgedMessage;
    @Mock
    private OrderReceived orderReceived;

    @Test
    @DisplayName("createLogMap returns a new log map")
    void createLogMapReturnsLogMap() {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        assertNotNull(logMap);
    }

    @Test
    @DisplayName("logIfNotNull populates a log map")
    void logIfNotNullPopulatesLogMap() {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        String key = KEY_VALUE;
        String testObject = "test";

        LoggingUtils.logIfNotNull(logMap, key, testObject);

        assertNotNull(logMap);
        assertEquals(logMap.get(key), testObject);
    }
    
    @Test
    @DisplayName("logWithOrderReference returns a populated map")
    void logWithOrderReferenceReturnsPopulatedMap() {
        Map<String, Object> logMap = LoggingUtils.logWithOrderReference(LOG_MESSAGE, ORDER_REFERENCE);
        assertNotNull(logMap);
        assertEquals(2, logMap.size());
        assertEquals(ORDER_REFERENCE, logMap.get(LoggingUtils.ORDER_REFERENCE_NUMBER));
        assertEquals(LOG_MESSAGE, logMap.get(LoggingUtils.MESSAGE));
    }
    
    @Test
    @DisplayName("createLogMapWithOrderReference returns a populated map")
    void createLogMapWithOrderReferenceReturnsPopulatedMap() {
        Map<String, Object> logMap = LoggingUtils.createLogMapWithOrderReference(ORDER_REFERENCE);
        assertNotNull(logMap);
        assertEquals(1, logMap.size());
        assertEquals(ORDER_REFERENCE, logMap.get(LoggingUtils.ORDER_REFERENCE_NUMBER));
    }
    
    @Test
    @DisplayName("createLogMapWithOrderReference returns an empty map")
    void createLogMapWithOrderReferenceReturnsEmptyMap() {
        Map<String, Object> logMap = LoggingUtils.createLogMapWithOrderReference(null);
        assertNotNull(logMap);
        assertEquals(0, logMap.size());
    }
    
    @Test
    @DisplayName("getLogger returns a logger object")
    void getLoggerReturnsLoggerObject() {
        Logger logger = LoggingUtils.getLogger();
        assertNotNull(logger);
    }
    
    @Test
    @DisplayName("createLogMapWithKafkaMessage returns a populated map")
    void createLogMapWithKafkaMessageAllInfo() {
        Message message = createMessageWithTopicAndOffset();
        message.setPartition(PARTITION_VALUE);
        Map<String, Object> logMap = LoggingUtils.createLogMapWithKafkaMessage(message);
        assertNotNull(logMap);
        assertEquals(3, logMap.size());
        assertEquals(TOPIC_VALUE, logMap.get(LoggingUtils.TOPIC));
        assertEquals(PARTITION_VALUE, logMap.get(LoggingUtils.PARTITION));
        assertEquals(OFFSET_VALUE, logMap.get(LoggingUtils.OFFSET));
    }
    
    @Test
    @DisplayName("createLogMapWithKafkaMessage returns a map populated with available info")
    void createLogMapWithKafkaMessageTopicAndOffset() {
        Message message = createMessageWithTopicAndOffset();
        Map<String, Object> logMap = LoggingUtils.createLogMapWithKafkaMessage(message);
        assertNotNull(logMap);
        assertEquals(2, logMap.size());
        assertEquals(TOPIC_VALUE, logMap.get(LoggingUtils.TOPIC));
        assertEquals(OFFSET_VALUE, logMap.get(LoggingUtils.OFFSET));
    }

    @Test
    @DisplayName("createLogMapWithAcknowledgedKafkaMessage returns a populated map")
    void createLogMapWithAcknowledgedKafkaMessageAllInfo() {

        // Given
        when(acknowledgedMessage.topic()).thenReturn(TOPIC_VALUE);
        when(acknowledgedMessage.partition()).thenReturn(PARTITION_VALUE);
        when(acknowledgedMessage.offset()).thenReturn(OFFSET_VALUE);

        // When
        final Map<String, Object> logMap = LoggingUtils.createLogMapWithAcknowledgedKafkaMessage(acknowledgedMessage);

        // Then
        assertNotNull(logMap);
        assertEquals(3, logMap.size());
        assertEquals(TOPIC_VALUE, logMap.get(LoggingUtils.TOPIC));
        assertEquals(PARTITION_VALUE, logMap.get(LoggingUtils.PARTITION));
        assertEquals(OFFSET_VALUE, logMap.get(LoggingUtils.OFFSET));
    }
    
    @Test
    @DisplayName("logMessageWithOrderReference returns a populated map")
    void logMessageWithOrderReferenceReturnsPopulatedMap() {
        Message message = createMessageWithTopicAndOffset();
        Map<String, Object> logMap = LoggingUtils.logMessageWithOrderReference(message, LOG_MESSAGE, ORDER_REFERENCE);
        assertNotNull(logMap);
        assertEquals(4, logMap.size());
        assertEquals(TOPIC_VALUE, logMap.get(LoggingUtils.TOPIC));
        assertEquals(OFFSET_VALUE, logMap.get(LoggingUtils.OFFSET));
        assertEquals(ORDER_REFERENCE, logMap.get(LoggingUtils.ORDER_REFERENCE_NUMBER));
        assertEquals(LOG_MESSAGE, logMap.get(LoggingUtils.MESSAGE));
    }

    @Test
    @DisplayName("getMessageHeadersAsMap returns a populated map")
    void getMessageHeadersAsMapReturnsPopulatedMap() {
        when(orderReceivedMessage.getHeaders()).thenReturn(messageHeaders);
        doReturn(KEY_VALUE).when(messageHeaders).get(KafkaHeaders.RECEIVED_KEY);
        doReturn(TOPIC_VALUE).when(messageHeaders).get(KafkaHeaders.RECEIVED_TOPIC);
        doReturn(OFFSET_VALUE).when(messageHeaders).get(KafkaHeaders.OFFSET);
        doReturn(PARTITION_VALUE).when(messageHeaders).get(KafkaHeaders.RECEIVED_PARTITION);
        when(orderReceivedMessage.getPayload()).thenReturn(orderReceived);
        when(orderReceived.getAttempt()).thenReturn(0);
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(orderReceivedMessage);
        assertNotNull(logMap);
        assertEquals(5, logMap.size());
        assertEquals(KEY_VALUE, logMap.get(LoggingUtils.KEY));
        assertEquals(TOPIC_VALUE, logMap.get(LoggingUtils.TOPIC));
        assertEquals(OFFSET_VALUE, logMap.get(LoggingUtils.OFFSET));
        assertEquals(PARTITION_VALUE, logMap.get(LoggingUtils.PARTITION));
        assertEquals(0, logMap.get(LoggingUtils.RETRY_ATTEMPT));
    }

    private Message createMessageWithTopicAndOffset() {
        Message message = new Message();
        message.setTopic(TOPIC_VALUE);
        message.setOffset(OFFSET_VALUE);
        return message;
    }
}