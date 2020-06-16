package uk.gov.companieshouse.itemhandler.logging;

import java.util.HashMap;
import java.util.Map;
import uk.gov.companieshouse.kafka.message.Message;

public class LoggingUtils {

    private LoggingUtils () {
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

    public static Map<String, Object> createLogMap() {
        return new HashMap<>();
    }
    
    public static Map<String,Object> createLogMapWithKafkaMessage(Message message){
        Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, TOPIC, message.getTopic());
        logIfNotNull(logMap, PARTITION, message.getPartition());
        logIfNotNull(logMap, OFFSET, message.getOffset());
        return logMap;
    }

    public static void logIfNotNull(Map<String, Object> logMap, String key, Object loggingObject) {
        if(loggingObject != null) {
            logMap.put(key, loggingObject);
        }
    }
}
