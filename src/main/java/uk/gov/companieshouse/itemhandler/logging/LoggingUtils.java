package uk.gov.companieshouse.itemhandler.logging;

import java.util.HashMap;
import java.util.Map;

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

    public static Map<String, Object> createLogMap() {
        return new HashMap<>();
    }

    public static void logIfNotNull(Map<String, Object> logMap, String key, Object loggingObject) {
        if(loggingObject != null) {
            logMap.put(key, loggingObject);
        }
    }
}
