package com.automation.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Single source of truth for framework configuration.
 *
 * <p>Design notes for reviewers: values are loaded once into a static
 * {@link Properties} block (config is read-only for the lifetime of the JVM,
 * so there's no need to re-parse the file per test) and every getter checks
 * {@code System.getProperty} first. That means CI can override any setting
 * with a plain {@code -D} flag (e.g. {@code -Dheadless=false} when debugging
 * locally) without touching the checked-in defaults.</p>
 */
public final class ConfigReader {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException(
                        "config.properties not found on the test classpath (expected under src/test/resources)");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            // Fail fast: a missing/corrupt config file means every downstream test
            // would fail in confusing ways, so surface the real cause immediately.
            throw new IllegalStateException("Failed to load config.properties", e);
        }
    }

    private ConfigReader() {
        // Static utility class — no instances.
    }

    /**
     * Resolves a config value, giving precedence to a JVM system property
     * (set via -D on the command line) over the value baked into config.properties.
     */
    public static String get(String key) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override;
        }
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("No config value found for key: " + key);
        }
        return value;
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static String uiBaseUrl() {
        return get("ui.baseUrl");
    }

    public static String apiBaseUrl() {
        return get("api.baseUrl");
    }
}
