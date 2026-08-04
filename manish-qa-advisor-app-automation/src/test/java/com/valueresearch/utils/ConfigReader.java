package com.valueresearch.utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        try {
            InputStream inputStream = ConfigReader.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");

            if (inputStream == null) {
                throw new RuntimeException("config.properties file not found under src/test/resources");
            }

            properties.load(inputStream);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
        }
    }

    public static String get(String key) {
        String envValue = System.getenv(key);

        if (envValue == null || envValue.trim().isEmpty()) {
            envValue = System.getenv(key.toUpperCase());
        }

        if (envValue != null && !envValue.trim().isEmpty()) {
            return cleanValue(envValue);
        }

        String value = properties.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing or empty property: " + key);
        }

        return cleanValue(value);
    }

    public static String getOptional(String key, String defaultValue) {
        try {
            String value = get(key);

            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }

            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}