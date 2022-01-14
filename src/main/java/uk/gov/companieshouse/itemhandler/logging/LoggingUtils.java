package uk.gov.companieshouse.itemhandler.logging;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;

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
    public static final String MESSAGE = "message";
    public static final String ORDER_REFERENCE_NUMBER = "order_reference_number";
    public static final String ORDER_URI = "order_uri";
    public static final String DESCRIPTION_LOG_KEY = "description_key";
    public static final String ITEM_ID = "item_id";
    public static final String PAYMENT_REFERENCE = "payment_reference";
    public static final String COMPANY_NUMBER = "company_number";

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

    /**
     * Creates a log map containing the required details to track the production of a message to a Kafka topic.
     * @param acknowledgedMessage the {@link RecordMetadata} the metadata for a record that has been acknowledged by
     *                            the server when a message has been produced to a Kafka topic.
     * @return the log map populated with Kafka message production details
     */
    public static Map<String, Object>
    createLogMapWithAcknowledgedKafkaMessage(final RecordMetadata acknowledgedMessage) {
        final Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, TOPIC, acknowledgedMessage.topic());
        logIfNotNull(logMap, PARTITION, acknowledgedMessage.partition());
        logIfNotNull(logMap, OFFSET, acknowledgedMessage.offset());
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
        logIfNotNull(logMap, MESSAGE, logMessage);
        return logMap;
    }

    public static Map<String, Object> logMessageWithOrderReference(Message message,
            String logMessage, String orderReference) {
        Map<String, Object> logMap = createLogMapWithKafkaMessage(message);
        logIfNotNull(logMap, MESSAGE, logMessage);
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
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
        logIfNotNull(logMap, RETRY_ATTEMPT, message.getPayload().getAttempt());

        return logMap;
    }
}