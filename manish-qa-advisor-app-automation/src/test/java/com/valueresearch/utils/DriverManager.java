package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.lang.reflect.Method;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverManager {

    private static AndroidDriver driver;

    private static final String DEFAULT_APPIUM_SERVER_URL = "http://127.0.0.1:4723";
    private static final String DEFAULT_DEVICE_NAME = "adb-RZCT80J98ZA-tvFqgN._adb-tls-connect._tcp";
    private static final String DEFAULT_APP_PACKAGE = "com.valueresearch.advisor";
    private static final String DEFAULT_APP_ACTIVITY = ".MainActivity";

    public static synchronized AndroidDriver getDriver() {
        if (isDriverHealthy()) {
            return driver;
        }

        if (driver != null) {
            System.out.println("[RECOVERY] Existing AndroidDriver is not healthy. Discarding stale session.");
        }

        safelyDiscardDriver();

        String appiumServerUrl = getConfigValue("appiumServerUrl", DEFAULT_APPIUM_SERVER_URL);
        String deviceName = getConfigValue("deviceName", DEFAULT_DEVICE_NAME);
        String appPackage = getConfigValue("appPackage", DEFAULT_APP_PACKAGE);
        String appActivity = getConfigValue("appActivity", DEFAULT_APP_ACTIVITY);

        DeviceRecoveryManager.ensureInfrastructureReady(deviceName, appiumServerUrl);

        try {
            driver = createAndroidDriver(
                    appiumServerUrl,
                    deviceName,
                    appPackage,
                    appActivity
            );

            return driver;

        } catch (Exception firstFailure) {
            System.out.println("[RECOVERY] First AndroidDriver creation attempt failed: "
                    + cleanError(firstFailure.getMessage()));

            safelyDiscardDriver();

            DeviceRecoveryManager.forceCompleteRecovery(deviceName, appiumServerUrl);

            try {
                driver = createAndroidDriver(
                        appiumServerUrl,
                        deviceName,
                        appPackage,
                        appActivity
                );

                return driver;

            } catch (Exception recoveryFailure) {
                safelyDiscardDriver();

                throw new RuntimeException(
                        "Failed to start Android driver even after automatic emulator/UiAutomator2 recovery: "
                                + cleanError(recoveryFailure.getMessage()),
                        recoveryFailure
                );
            }
        }
    }

    public static synchronized boolean isDriverHealthy() {
        if (driver == null || driver.getSessionId() == null) {
            return false;
        }

        String deviceName = getConfigValue("deviceName", DEFAULT_DEVICE_NAME);

        if (!DeviceRecoveryManager.isDeviceOnline(deviceName)) {
            return false;
        }

        try {
            /*
             * This is a real UiAutomator2 command. A Java driver object can still
             * have a session ID even after the instrumentation process has died.
             */
            driver.getCurrentPackage();
            return true;

        } catch (Exception ignored) {
            return false;
        }
    }

    private static AndroidDriver createAndroidDriver(
            String appiumServerUrl,
            String deviceName,
            String appPackage,
            String appActivity
    ) throws Exception {
        System.out.println("Starting Android driver with:");
        System.out.println("Appium URL   : " + appiumServerUrl);
        System.out.println("Device Name  : " + deviceName);
        System.out.println("UDID         : " + deviceName);
        System.out.println("App Package  : " + appPackage);
        System.out.println("App Activity : " + appActivity);

        UiAutomator2Options options = new UiAutomator2Options();

        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");

        options.setDeviceName(deviceName);
        options.setUdid(deviceName);

        options.setAppPackage(appPackage);
        options.setAppActivity(appActivity);

        options.setNoReset(true);
        options.setAutoGrantPermissions(true);

        options.setCapability("appium:forceAppLaunch", true);
        options.setCapability("appium:appWaitActivity", "*");
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);
        options.setCapability("appium:skipLogcatCapture", true);

        /*
         * Give a freshly restarted emulator enough time to install and launch
         * the UiAutomator2 server.
         */
        options.setCapability("appium:adbExecTimeout", 60_000);
        options.setCapability("appium:uiautomator2ServerInstallTimeout", 60_000);
        options.setCapability("appium:uiautomator2ServerLaunchTimeout", 60_000);

        /*
         * Global UiAutomator2 speed settings for all modules.
         * These reduce real-device idle waiting and selector delay.
         */
        options.setCapability("appium:settings[waitForIdleTimeout]", 100);
        options.setCapability("appium:settings[waitForSelectorTimeout]", 1000);
        options.setCapability("appium:settings[actionAcknowledgmentTimeout]", 500);
        options.setCapability("appium:settings[scrollAcknowledgmentTimeout]", 500);

        options.setNewCommandTimeout(Duration.ofSeconds(600));

        AndroidDriver newDriver = new AndroidDriver(
                new URL(appiumServerUrl),
                options
        );

        try {
            applyAppiumSpeedSettings(newDriver);
            forceActivateAdvisorApp(newDriver, appPackage);
            applyAppiumSpeedSettings(newDriver);

            System.out.println("Android driver started and Advisor app activated");
            return newDriver;

        } catch (Exception e) {
            try {
                newDriver.quit();
            } catch (Exception ignored) {
                // Preserve the original session creation/activation failure.
            }

            throw e;
        }
    }

    private static void applyAppiumSpeedSettings(AndroidDriver activeDriver) {
        if (activeDriver == null) {
            return;
        }

        try {
            activeDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(0));
            System.out.println("Implicit wait set to 0 ms for faster Appium execution");
        } catch (Exception e) {
            System.out.println("Unable to set implicit wait to 0: " + cleanError(e.getMessage()));
        }

        /*
         * Reflection is used here so the project does not need any extra Appium
         * version-specific API dependency.
         */
        setAppiumSettingSafely(activeDriver, "waitForIdleTimeout", 100);
        setAppiumSettingSafely(activeDriver, "waitForSelectorTimeout", 1000);
        setAppiumSettingSafely(activeDriver, "actionAcknowledgmentTimeout", 500);
        setAppiumSettingSafely(activeDriver, "scrollAcknowledgmentTimeout", 500);
    }

    private static void setAppiumSettingSafely(
            AndroidDriver activeDriver,
            String settingName,
            Object value
    ) {
        try {
            Method setSettingMethod = activeDriver.getClass().getMethod(
                    "setSetting",
                    String.class,
                    Object.class
            );

            setSettingMethod.invoke(activeDriver, settingName, value);
            System.out.println("Applied Appium setting: " + settingName + "=" + value);

        } catch (NoSuchMethodException e) {
            System.out.println(
                    "Runtime Appium setting skipped because setSetting(String, Object) is unavailable: "
                            + settingName
            );

        } catch (Exception e) {
            System.out.println("Runtime Appium setting skipped: "
                    + settingName
                    + " | "
                    + cleanError(e.getMessage()));
        }
    }

    private static void forceActivateAdvisorApp(
            AndroidDriver activeDriver,
            String appPackage
    ) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("appId", appPackage);

            activeDriver.executeScript("mobile: activateApp", params);

            Thread.sleep(2_000);

            System.out.println("Advisor app activated");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Advisor app activation was interrupted", e);

        } catch (Exception e) {
            throw new RuntimeException("Failed to activate Advisor app: " + e.getMessage(), e);
        }
    }

    private static String getConfigValue(String key, String defaultValue) {
        try {
            String value = ConfigReader.get(key);

            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }

            return defaultValue;

        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return message.replaceAll("\\s+", " ").trim();
    }

    private static void safelyDiscardDriver() {
        AndroidDriver staleDriver = driver;
        driver = null;

        if (staleDriver == null) {
            return;
        }

        try {
            staleDriver.quit();
        } catch (Exception e) {
            System.out.println("[RECOVERY] Stale AndroidDriver quit skipped/failed: "
                    + cleanError(e.getMessage()));
        }
    }

    public static synchronized void quitDriver() {
        if (driver == null) {
            return;
        }

        safelyDiscardDriver();
        System.out.println("Android driver stopped");
    }
}