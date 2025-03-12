package uk.gov.companieshouse.itemhandler.logging;

import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;
import java.util.Map;


public class LogMessageBuilder {

    private final Map<String, Object> logData;
    private final Logger logger;

    public LogMessageBuilder(Logger logger) {
        this.logger = logger;
        this.logData = new HashMap<>();
    }

    public LogMessageBuilder addContext(String name, Object value) {
        this.logData.put(name, value);
        return this;
    }

    public void logDebug(String message) {
        logger.debug(message, logData);
    }

    public void logInfo(String message) {
        logger.info(message, logData);
    }

    public void logError(String message) {
        logger.error(message, logData);
    }

    public static LogMessageBuilder builder(Logger logger) {
        return new LogMessageBuilder(logger);
    }
}
