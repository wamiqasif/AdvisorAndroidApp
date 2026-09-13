package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;

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


    /**
     * Returns true only when the current failure is caused by Android/Appium
     * infrastructure rather than by a functional assertion.
     *
     * <p>The live driver health check is intentionally part of this decision.
     * A testcase can surface a generic timeout/assertion after the emulator or
     * UiAutomator2 died, so the throwable message alone is not sufficient.</p>
     */
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

        String deviceName = getConfigValue("deviceName", DEFAULT_DEVICE_NAME);

        /*
         * Layer 1:
         * Verify that Android itself is still reachable.
         */
        if (!DeviceRecoveryManager.isDeviceOnline(deviceName)) {
            System.out.println(
                    "[RECOVERY] Driver health check failed: Android device is not online: "
                            + deviceName
            );
            return false;
        }

        /*
         * Layer 2:
         * Verify the UiAutomator2 instrumentation itself.
         *
         * Do not rely only on getCurrentPackage(). A stale Java driver/session
         * can still have a session ID, and lightweight commands can appear alive
         * even after UiAutomator2 instrumentation has crashed.
         *
         * findElements() performs a real UI hierarchy request through the
         * UiAutomator2 server. We do not care whether FrameLayout elements are
         * returned; successful command execution is the health signal.
         */
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

        String lower = message.toLowerCase();

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
                || lower.contains("appium is not ready")
                || lower.contains("could not start a new session")
                || lower.contains("new session could not be created")
                || lower.contains("unable to connect to appium server");
    }

    private static boolean isDeadUiAutomator2Error(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lower = message.toLowerCase();

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
            String appPackage,
            String appActivity
    ) throws Exception {
        System.out.println("Starting Android driver with:");
        System.out.println("Appium URL   : " + appiumServerUrl);
        System.out.println("Device Name  : " + deviceName);
        System.out.println("UDID         : " + deviceName);
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
         * QA-safe UiAutomator2 synchronization settings.
         *
         * Do not override actionAcknowledgmentTimeout or
         * scrollAcknowledgmentTimeout globally. Their Appium defaults are safer
         * for a build whose response/render time varies during execution.
         */
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
            forceActivateAdvisorApp(newDriver, appPackage);
            applyAppiumStabilitySettings(newDriver);

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