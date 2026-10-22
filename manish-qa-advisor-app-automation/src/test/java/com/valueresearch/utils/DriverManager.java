package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.lang.reflect.Method;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DriverManager {

    private static AndroidDriver driver;
    private static String activeAppPackage;
    private static String activeAppActivity;
    private static String activeDeviceName;
    private static String activeDeviceUdid;
    private static String activeAppiumServerUrl;

    private static final String DEFAULT_APPIUM_SERVER_URL = "http://127.0.0.1:4723";
    private static final String DEFAULT_APP_PACKAGE = "com.valueresearch.advisor";
    private static final String DEFAULT_APP_ACTIVITY = ".MainActivity";

    /** Existing modules continue using the Advisor configuration. */
    public static synchronized AndroidDriver getDriver() {
        return getDriverForApp(
                getConfigValue("appPackage", DEFAULT_APP_PACKAGE),
                getConfigValue("appActivity", DEFAULT_APP_ACTIVITY)
        );
    }

    /** Reads module-specific package/activity keys using ConfigReader. */
    public static synchronized AndroidDriver getDriver(
            String appPackageConfigKey,
            String appActivityConfigKey
    ) {
        return getDriverForApp(
                getRequiredConfigValue(appPackageConfigKey),
                getRequiredConfigValue(appActivityConfigKey)
        );
    }

    private static AndroidDriver getDriverForApp(
            String appPackage,
            String appActivity
    ) {
        String appiumServerUrl = getConfigValue("appiumServerUrl", DEFAULT_APPIUM_SERVER_URL);
        // Use the real ADB serial, NOT the human-readable deviceName, for routing.
        String deviceUdid = resolveDeviceUdid();
        String deviceName = getConfigValue("deviceName", deviceUdid);

        boolean sameTarget = appPackage.equals(activeAppPackage)
                && appActivity.equals(activeAppActivity)
                && deviceName.equals(activeDeviceName)
                && deviceUdid.equals(activeDeviceUdid)
                && appiumServerUrl.equals(activeAppiumServerUrl);

        if (sameTarget && isDriverHealthy()) {
            return driver;
        }

        if (driver != null) {
            System.out.println(sameTarget
                    ? "[RECOVERY] Existing AndroidDriver is not healthy. Discarding stale session."
                    : "[DRIVER] App/device/server target changed. Replacing the existing session.");
        }

        safelyDiscardDriver();

        System.out.println("[DRIVER] Preparing ADB UDID: " + deviceUdid
                + " | Device name: " + deviceName + " | Appium: " + appiumServerUrl);
        try {
            DeviceRecoveryManager.ensureInfrastructureReady(deviceUdid, appiumServerUrl);
        } catch (RuntimeException infrastructureFailure) {
            throw new IllegalStateException(
                    "Android device/Appium infrastructure is not ready for ADB UDID '"
                            + deviceUdid + "'. Run 'adb devices -l' and verify the active "
                            + "USB/wireless connection.", infrastructureFailure);
        }

        try {
            driver = createAndroidDriver(
                    appiumServerUrl,
                    deviceName,
                    deviceUdid,
                    appPackage,
                    appActivity
            );
            rememberTarget(appiumServerUrl, deviceName, deviceUdid, appPackage, appActivity);

            return driver;

        } catch (Exception firstFailure) {
            System.out.println("[RECOVERY] First AndroidDriver creation attempt failed: "
                    + cleanError(firstFailure.getMessage()));

            safelyDiscardDriver();

            DeviceRecoveryManager.forceCompleteRecovery(deviceUdid, appiumServerUrl);

            try {
                driver = createAndroidDriver(
                        appiumServerUrl,
                        deviceName,
                        deviceUdid,
                        appPackage,
                        appActivity
                );
                rememberTarget(appiumServerUrl, deviceName, deviceUdid, appPackage, appActivity);

                return driver;

            } catch (Exception recoveryFailure) {
                safelyDiscardDriver();

                RuntimeException combined = new RuntimeException(
                        "Failed to start Android driver for ADB UDID '" + deviceUdid
                                + "' even after device/UiAutomator2 recovery: "
                                + cleanError(recoveryFailure.getMessage()),
                        recoveryFailure);
                combined.addSuppressed(firstFailure);
                throw combined;
            }
        }
    }

    public static synchronized boolean isInfrastructureFailure(Throwable throwable) {
        if (!isDriverHealthy()) {
            return true;
        }

        Throwable current = throwable;

        while (current != null) {
            String className = current.getClass().getName();
            String message = cleanError(current.getMessage());

            if (className.endsWith("NoSuchSessionException")
                    || className.endsWith("SessionNotCreatedException")
                    || containsInfrastructureSignal(message)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    public static String describeInfrastructureFailure(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            String message = cleanError(current.getMessage());

            if (containsInfrastructureSignal(message)) {
                return shortRecoveryError(message);
            }

            String className = current.getClass().getName();
            if (className.endsWith("NoSuchSessionException")
                    || className.endsWith("SessionNotCreatedException")) {
                return className + (message.isEmpty() ? "" : ": " + shortRecoveryError(message));
            }

            current = current.getCause();
        }

        return "Android device/Appium/UiAutomator2 session became unhealthy during testcase execution";
    }

    public static synchronized boolean isDriverHealthy() {
        if (driver == null) {
            return false;
        }

        if (driver.getSessionId() == null) {
            System.out.println("[RECOVERY] Driver health check failed: session ID is null.");
            return false;
        }

        String deviceUdid = activeDeviceUdid != null
                ? activeDeviceUdid
                : resolveDeviceUdid();

        /*
         * Layer 1:
         * Verify that Android itself is still reachable.
         */
        if (!DeviceRecoveryManager.isDeviceOnline(deviceUdid)) {
            System.out.println(
                    "[RECOVERY] Driver health check failed: Android device is not online: "
                            + deviceUdid
            );
            return false;
        }

        try {
            driver.findElements(
                    By.className("android.widget.FrameLayout")
            );

            return true;

        } catch (Exception e) {
            String message = cleanError(e.getMessage());

            if (isDeadUiAutomator2Error(message)) {
                System.out.println(
                        "[RECOVERY] Dead UiAutomator2 instrumentation detected: "
                                + shortRecoveryError(message)
                );
            } else {
                System.out.println(
                        "[RECOVERY] AndroidDriver health check failed: "
                                + shortRecoveryError(message)
                );
            }

            return false;
        }
    }

    private static boolean containsInfrastructureSignal(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lower = message.toLowerCase(Locale.ROOT);

        return isDeadUiAutomator2Error(message)
                || lower.contains("android device is not online")
                || lower.contains("device offline")
                || lower.contains("device not found")
                || lower.contains("no devices/emulators found")
                || lower.contains("adb: device")
                || lower.contains("could not proxy command")
                || lower.contains("connection reset")
                || lower.contains("broken pipe")
                || lower.contains("failed to start android driver")
                || lower.contains("failed to activate advisor app")
                || lower.contains("failed to activate app")
                || lower.contains("appium is not ready")
                || lower.contains("could not start a new session")
                || lower.contains("new session could not be created")
                || lower.contains("unable to connect to appium server");
    }

    private static boolean isDeadUiAutomator2Error(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lower = message.toLowerCase(Locale.ROOT);

        return lower.contains("instrumentation process is not running")
                || lower.contains("cannot be proxied to uiautomator2 server")
                || lower.contains("uiautomator2 server is not running")
                || lower.contains("socket hang up")
                || lower.contains("econnrefused")
                || lower.contains("connection refused")
                || lower.contains("session is either terminated or not started")
                || lower.contains("invalid session id");
    }

    private static String shortRecoveryError(String message) {
        String cleaned = cleanError(message);

        if (cleaned.length() <= 350) {
            return cleaned;
        }

        return cleaned.substring(0, 350) + "...";
    }

    private static AndroidDriver createAndroidDriver(
            String appiumServerUrl,
            String deviceName,
            String deviceUdid,
            String appPackage,
            String appActivity
    ) throws Exception {
        System.out.println("Starting Android driver with:");
        System.out.println("Appium URL   : " + appiumServerUrl);
        System.out.println("Device Name  : " + deviceName);
        System.out.println("ADB UDID     : " + deviceUdid);
        System.out.println("App Package  : " + appPackage);
        System.out.println("App Activity : " + appActivity);
        System.out.println("Execution Profile : " + ExecutionProfile.getProfileName());
        System.out.println("Default Wait      : " + ExecutionProfile.defaultWait().getSeconds() + " sec");
        System.out.println("Long Wait         : " + ExecutionProfile.longWait().getSeconds() + " sec");
        System.out.println("Polling Interval  : " + ExecutionProfile.pollingInterval().toMillis() + " ms");

        int waitForIdleTimeout = ExecutionProfile.getWaitForIdleTimeoutMs();
        int waitForSelectorTimeout = ExecutionProfile.getWaitForSelectorTimeoutMs();

        UiAutomator2Options options = new UiAutomator2Options();

        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");

        options.setDeviceName(deviceName);
        options.setUdid(deviceUdid);

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

        options.setCapability(
                "appium:settings[waitForIdleTimeout]",
                waitForIdleTimeout
        );
        options.setCapability(
                "appium:settings[waitForSelectorTimeout]",
                waitForSelectorTimeout
        );

        options.setNewCommandTimeout(Duration.ofSeconds(600));

        AndroidDriver newDriver = new AndroidDriver(
                new URL(appiumServerUrl),
                options
        );

        try {
            applyAppiumStabilitySettings(newDriver);
            forceActivateApp(newDriver, appPackage);
            applyAppiumStabilitySettings(newDriver);

            System.out.println("Android driver started and app activated: " + appPackage);
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

    private static void applyAppiumStabilitySettings(AndroidDriver activeDriver) {
        if (activeDriver == null) {
            return;
        }

        try {
            activeDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(0));
            System.out.println("Implicit wait set to 0 ms");
        } catch (Exception e) {
            System.out.println("Unable to set implicit wait to 0: " + cleanError(e.getMessage()));
        }

        /*
         * Reflection is used here so the project does not need any extra Appium
         * version-specific API dependency.
         */
        setAppiumSettingSafely(
                activeDriver,
                "waitForIdleTimeout",
                ExecutionProfile.getWaitForIdleTimeoutMs()
        );

        setAppiumSettingSafely(
                activeDriver,
                "waitForSelectorTimeout",
                ExecutionProfile.getWaitForSelectorTimeoutMs()
        );
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

    private static void forceActivateApp(
            AndroidDriver activeDriver,
            String appPackage
    ) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("appId", appPackage);
            activeDriver.executeScript("mobile: activateApp", params);

            // Wait for the actual foreground app instead of a fixed sleep.
            new WebDriverWait(
                    activeDriver,
                    ExecutionProfile.longWait(),
                    ExecutionProfile.pollingInterval()
            ).until(current -> appPackage.equals(activeDriver.getCurrentPackage()));

            System.out.println("App activated: " + appPackage);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to activate app: " + appPackage + " | " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    /**
     * Select a physical USB/wireless device or emulator by its exact 'adb devices' serial.
     * Environment override is optional and helpful for CI or fast local switching.
     * Older projects with deviceName=<ADB serial> continue to work.
     */
    private static String resolveDeviceUdid() {
        String environmentUdid = System.getenv("APPIUM_DEVICE_UDID");
        if (environmentUdid != null && !environmentUdid.trim().isEmpty()) {
            return environmentUdid.trim();
        }
        String configUdid = getConfigValue("udid", "");
        if (!configUdid.isEmpty()) {
            return configUdid;
        }
        String legacyName = getConfigValue("deviceName", "");
        if (legacyName.isEmpty() || legacyName.contains(" ")) {
            throw new IllegalStateException(
                    "No valid ADB UDID configured. Set udid=<serial from 'adb devices'> "
                            + "in config.properties, or APPIUM_DEVICE_UDID in the environment. "
                            + "deviceName is a label, not an ADB identifier.");
        }
        System.out.println("[DRIVER] 'udid' not set; using legacy deviceName as ADB serial: "
                + legacyName);
        return legacyName;
    }

    private static String getRequiredConfigValue(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("App configuration key must not be blank");
        }

        String configKey = key.trim();
        try {
            String value = ConfigReader.get(configKey);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Configuration value is empty");
            }
            return value.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Missing or invalid app setting in config.properties: " + configKey,
                    e
            );
        }
    }

    private static void rememberTarget(
            String serverUrl,
            String deviceName,
            String deviceUdid,
            String appPackage,
            String appActivity
    ) {
        activeDeviceUdid = deviceUdid;
        activeAppiumServerUrl = serverUrl;
        activeDeviceName = deviceName;
        activeAppPackage = appPackage;
        activeAppActivity = appActivity;
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
        activeAppPackage = null;
        activeAppActivity = null;
        activeDeviceName = null;
        activeDeviceUdid = null;
        activeAppiumServerUrl = null;

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
        safelyDiscardDriver();
        System.out.println("Android driver stopped");
    }
}