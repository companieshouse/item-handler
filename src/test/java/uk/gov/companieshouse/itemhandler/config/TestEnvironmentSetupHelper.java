package uk.gov.companieshouse.itemhandler.config;

import java.lang.reflect.Field;
import java.util.Map;

public interface TestEnvironmentSetupHelper {

    static void setEnvironmentVariable(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable");
        }
    }
}
