package com.valueresearch.utils;

import java.time.Duration;

/**
 * Central execution/timing profile for the automation framework.
 *
 * Keep environment-specific timing in config.properties instead of
 * hardcoding waits throughout page classes. QA is the safe default because
 * this project is primarily executed against the QA build.
 */
public final class ExecutionProfile {

    private static final String DEFAULT_PROFILE = "qa";

    private ExecutionProfile() {
    }

    public static String getProfileName() {
        return ConfigReader
                .getOptional("executionProfile", DEFAULT_PROFILE)
                .trim()
                .toLowerCase();
    }

    public static boolean isQa() {
        return "qa".equalsIgnoreCase(getProfileName());
    }

    public static Duration shortWait() {
        return Duration.ofSeconds(
                getPositiveInt("waitShortSeconds", 8)
        );
    }

    public static Duration defaultWait() {
        return Duration.ofSeconds(
                getPositiveInt("waitDefaultSeconds", 35)
        );
    }

    public static Duration longWait() {
        return Duration.ofSeconds(
                getPositiveInt("waitLongSeconds", 60)
        );
    }

    public static Duration pollingInterval() {
        return Duration.ofMillis(
                getPositiveInt("waitPollingMillis", 300)
        );
    }

    public static int getWaitForIdleTimeoutMs() {
        return getPositiveInt(
                "appiumWaitForIdleTimeoutMs",
                750
        );
    }

    public static int getWaitForSelectorTimeoutMs() {
        return getPositiveInt(
                "appiumWaitForSelectorTimeoutMs",
                4000
        );
    }

    /**
     * Reserved for modules that currently have their own FAST_MODE flags.
     * Those page classes can be migrated later without changing this class.
     */
    public static boolean isFastModeEnabled() {
        return Boolean.parseBoolean(
                ConfigReader.getOptional(
                        "fastModeEnabled",
                        "false"
                )
        );
    }

    private static int getPositiveInt(
            String key,
            int defaultValue
    ) {
        try {
            int value = Integer.parseInt(
                    ConfigReader.getOptional(
                            key,
                            String.valueOf(defaultValue)
                    ).trim()
            );

            return value > 0 ? value : defaultValue;

        } catch (Exception e) {
            return defaultValue;
        }
    }
}