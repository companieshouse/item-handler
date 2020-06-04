package uk.gov.companieshouse.itemhandler.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LoggingUtilsTest {

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
        String key = "key";
        String testObject = "test";

        LoggingUtils.logIfNotNull(logMap, key, testObject);

        assertNotNull(logMap);
        assertEquals(logMap.get(key), testObject);
    }
}
