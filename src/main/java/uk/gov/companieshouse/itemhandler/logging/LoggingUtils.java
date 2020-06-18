package uk.gov.companieshouse.itemhandler.logging;

import java.util.HashMap;
import java.util.Map;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

public class LoggingUtils {

    private LoggingUtils() {
        throw new IllegalStateException("A utility class is not to be instantiated");
    }

    public static final String APPLICATION_NAMESPACE = "item-handler";
    public static final String TOPIC = "topic";
    public static final String OFFSET = "offset";
    public static final String KEY = "key";
    public static final String PARTITION = "partition";
    public static final String RETRY_ATTEMPT = "retry_attempt";
    public static final String EXCEPTION = "exception";
    public static final String MESSAGE = "message";
    public static final String CURRENT_TOPIC = "current_topic";
    public static final String NEXT_TOPIC = "next_topic";
    public static final String ORDER_RECEIVED_GROUP_ERROR = "order_received_error";
    public static final String ORDER_REFERENCE_NUMBER = "order_reference_number";
    public static final String ORDER_URI = "order_uri";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    public static Map<String, Object> createLogMap() {
        return new HashMap<>();
    }

    public static Map<String, Object> createLogMapWithKafkaMessage(Message message) {
        Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, TOPIC, message.getTopic());
        logIfNotNull(logMap, PARTITION, message.getPartition());
        logIfNotNull(logMap, OFFSET, message.getOffset());
        return logMap;
    }

    public static Map<String, Object> createLogMapWithOrderReference(String orderReference) {
        Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
        return logMap;
    }

    public static void logIfNotNull(Map<String, Object> logMap, String key, Object loggingObject) {
        if (loggingObject != null) {
            logMap.put(key, loggingObject);
        }
    }

    public static Map<String, Object> logWithOrderReference(String logMessage,
            String orderReference) {
        Map<String, Object> logMap = createLogMapWithOrderReference(orderReference);
        LOGGER.info(logMessage, logMap);
        return logMap;
    }

    public static Map<String, Object> logMessageWithOrderReference(Message message,
            String logMessage, String orderReference) {
        Map<String, Object> logMap = createLogMapWithKafkaMessage(message);
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
        LOGGER.info(logMessage, logMap);
        return logMap;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Map<String, Object> getMessageHeadersAsMap(
            org.springframework.messaging.Message<OrderReceived> message) {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        MessageHeaders messageHeaders = message.getHeaders();

        logIfNotNull(logMap, KEY, messageHeaders.get(KafkaHeaders.RECEIVED_MESSAGE_KEY));
        logIfNotNull(logMap, TOPIC, messageHeaders.get(KafkaHeaders.RECEIVED_TOPIC));
        logIfNotNull(logMap, OFFSET, messageHeaders.get(KafkaHeaders.OFFSET));
        logIfNotNull(logMap, PARTITION, messageHeaders.get(KafkaHeaders.RECEIVED_PARTITION_ID));

        return logMap;
    }
}
