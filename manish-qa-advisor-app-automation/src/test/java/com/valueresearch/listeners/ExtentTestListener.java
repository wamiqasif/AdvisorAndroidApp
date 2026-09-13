package com.valueresearch.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.utils.DriverManager;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.IAnnotationTransformer;
import org.testng.IConfigurationListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.IRetryAnalyzer;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentTestListener implements ITestListener, ISuiteListener, IConfigurationListener, IAnnotationTransformer {

    private static final String SCREENSHOT_ATTACHED_KEY = "SCREENSHOT_ATTACHED_BY_LISTENER";

    /**
     * Globally attaches the infrastructure-only retry analyzer to @Test methods.
     * No separate retry/transformer Java file is required.
     */
    @Override
    @SuppressWarnings({"rawtypes"})
    public void transform(
            ITestAnnotation annotation,
            Class testClass,
            Constructor testConstructor,
            Method testMethod
    ) {
        if (annotation != null) {
            annotation.setRetryAnalyzer(InfrastructureRetryAnalyzer.class);
        }
    }

    /**
     * Retries a testcase at most once, only when Android/Appium/UiAutomator2
     * infrastructure is unhealthy.
     */
    public static class InfrastructureRetryAnalyzer implements IRetryAnalyzer {

        private static final int MAX_RETRIES = 1;
        private static final String RETRY_SCHEDULED_KEY =
                "INFRASTRUCTURE_RETRY_SCHEDULED";
        private static final String RETRY_REASON_KEY =
                "INFRASTRUCTURE_RETRY_REASON";

        private int retryCount = 0;

        @Override
        public boolean retry(ITestResult result) {
            if (result == null || retryCount >= MAX_RETRIES) {
                return false;
            }

            String reason = detectInfrastructureFailure(result);

            if (reason == null || reason.trim().isEmpty()) {
                return false;
            }

            retryCount++;

            result.setAttribute(RETRY_SCHEDULED_KEY, true);
            result.setAttribute(RETRY_REASON_KEY, reason);

            System.out.println(
                    "[RECOVERY] Infrastructure failure detected. Retrying testcase once"
                            + " | test=" + safeMethodName(result)
                            + " | reason=" + reason
            );

            return true;
        }

        public static boolean isInfrastructureRetryScheduled(ITestResult result) {
            return result != null
                    && Boolean.TRUE.equals(result.getAttribute(RETRY_SCHEDULED_KEY));
        }

        public static String getRetryReason(ITestResult result) {
            if (result == null) {
                return "";
            }

            Object value = result.getAttribute(RETRY_REASON_KEY);
            return value == null ? "" : String.valueOf(value);
        }

        private static String detectInfrastructureFailure(ITestResult result) {
            String throwableReason = findInfrastructureMessage(result.getThrowable());

            if (!throwableReason.isEmpty()) {
                return throwableReason;
            }

            /*
             * Covers the observed case where AuthHelper reports a generic
             * post-PIN timeout but the emulator has actually gone offline.
             */
            try {
                if (BaseTest.driver != null && !DriverManager.isDriverHealthy()) {
                    return "Android device / Appium / UiAutomator2 session is unhealthy";
                }
            } catch (Exception e) {
                String message = cleanStaticError(e.getMessage());

                return message.isEmpty()
                        ? "Driver health check failed"
                        : "Driver health check failed: " + message;
            }

            return "";
        }

        private static String findInfrastructureMessage(Throwable throwable) {
            Throwable current = throwable;

            while (current != null) {
                String message = cleanStaticError(current.getMessage());
                String lower = message.toLowerCase();

                if (containsInfrastructureToken(lower)) {
                    return message.isEmpty()
                            ? current.getClass().getSimpleName()
                            : message;
                }

                String className = current.getClass().getName().toLowerCase();

                if (className.contains("nosuchsession")
                        || className.contains("sessionnotcreated")
                        || className.contains("unreachablebrowser")
                        || className.contains("connection")) {
                    return message.isEmpty()
                            ? current.getClass().getSimpleName()
                            : message;
                }

                current = current.getCause();
            }

            return "";
        }

        private static boolean containsInfrastructureToken(String lower) {
            if (lower == null || lower.isEmpty()) {
                return false;
            }

            return lower.contains("android device is not online")
                    || lower.contains("device offline")
                    || lower.contains("device not found")
                    || lower.contains("uiautomator2 server is not running")
                    || lower.contains("instrumentation process is not running")
                    || lower.contains("cannot be proxied to uiautomator2 server")
                    || lower.contains("socket hang up")
                    || lower.contains("econnrefused")
                    || lower.contains("connection refused")
                    || lower.contains("invalid session id")
                    || lower.contains("session is either terminated or not started")
                    || lower.contains("no such session")
                    || lower.contains("failed to start android driver")
                    || lower.contains("failed to activate advisor app")
                    || (lower.contains("adb")
                        && (lower.contains("offline")
                            || lower.contains("not found")
                            || lower.contains("closed")));
        }

        private static String safeMethodName(ITestResult result) {
            try {
                if (result != null && result.getMethod() != null) {
                    return result.getMethod().getMethodName();
                }
            } catch (Exception ignored) {
                // Return fallback.
            }

            return "UnknownMethod";
        }

        private static String cleanStaticError(String message) {
            if (message == null) {
                return "";
            }

            return message.replaceAll("\\s+", " ").trim();
        }
    }

    @Override
    public void onStart(ISuite suite) {
        ExtentManager.getExtentReports();
        logToConsole("Extent report started for suite: " + suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        try {
            ExtentManager.getExtentReports().flush();
            logToConsole("Extent report flushed for suite: " + suite.getName());
        } finally {
            ExtentTestManager.unload();
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = getOrCreateTest(result);

        test.log(
                Status.PASS,
                "<span class='badge white-text green'>Test passed: "
                        + escapeHtml(getMethodName(result))
                        + "</span>"
        );

        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (discardIntermediateInfrastructureRetry(result)) {
            return;
        }

        ExtentTest test = getOrCreateTest(result);

        test.log(
                Status.FAIL,
                "<b>Test failed:</b> " + escapeHtml(getMethodName(result))
        );

        logThrowable(test, result.getThrowable());
        attachFailureScreenshot(result, "FAILED");

        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        if (discardIntermediateInfrastructureRetry(result)) {
            return;
        }

        ExtentTest test = getOrCreateTest(result);

        test.log(
                Status.FAIL,
                "<b>Test failed due to timeout:</b> " + escapeHtml(getMethodName(result))
        );

        logThrowable(test, result.getThrowable());
        attachFailureScreenshot(result, "TIMEOUT");

        ExtentTestManager.unload();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (discardIntermediateInfrastructureRetry(result)) {
            return;
        }

        ExtentTest test = getOrCreateTest(result);

        test.log(
                Status.SKIP,
                "<b>Test skipped:</b> " + escapeHtml(getMethodName(result))
        );

        logThrowable(test, result.getThrowable());

        ExtentTestManager.unload();
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        ExtentTest test = getOrCreateTest(result);

        test.log(
                Status.FAIL,
                "<b>Configuration failed:</b> " + escapeHtml(getMethodName(result))
        );

        logThrowable(test, result.getThrowable());
        attachFailureScreenshot(result, "CONFIG_FAILED");

        ExtentTestManager.unload();
    }

    @Override
    public void onConfigurationSkip(ITestResult result) {
        ExtentTest test = getOrCreateTest(result);

        test.log(
                Status.SKIP,
                "<b>Configuration skipped:</b> " + escapeHtml(getMethodName(result))
        );

        ExtentTestManager.unload();
    }

    /**
     * A failed infrastructure attempt is not a functional testcase failure.
     * Remove its Extent entry and allow the single recovery retry to become the
     * final visible result. TestNG can still show the retry attempt in its own
     * console/XML accounting, but the Extent dashboard stays focused on the
     * final testcase outcome.
     */
    private boolean discardIntermediateInfrastructureRetry(ITestResult result) {
        if (!InfrastructureRetryAnalyzer.isInfrastructureRetryScheduled(result)) {
            return false;
        }

        String reason = InfrastructureRetryAnalyzer.getRetryReason(result);
        ExtentTest currentTest = ExtentTestManager.getTest();

        if (currentTest != null) {
            try {
                ExtentManager.getExtentReports().removeTest(currentTest);
            } catch (Exception e) {
                logToConsole(
                        "Unable to remove intermediate infrastructure retry from Extent report: "
                                + e.getMessage()
                );
            }
        }

        logToConsole(
                "Infrastructure retry scheduled for "
                        + getMethodName(result)
                        + (reason == null || reason.trim().isEmpty()
                        ? ""
                        : " | reason=" + reason)
        );

        ExtentTestManager.unload();
        return true;
    }

    private ExtentTest getOrCreateTest(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();

        if (test != null) {
            return test;
        }

        test = ExtentManager
                .getExtentReports()
                .createTest(buildTestName(result));

        ExtentTestManager.setTest(test);

        return test;
    }

    private String buildTestName(ITestResult result) {
        String className = "UnknownClass";

        try {
            if (result.getTestClass() != null) {
                className = result.getTestClass().getName();
            }
        } catch (Exception ignored) {
            // fallback class name
        }

        return className + "." + getMethodName(result);
    }

    private String getMethodName(ITestResult result) {
        try {
            if (result != null && result.getMethod() != null) {
                return result.getMethod().getMethodName();
            }
        } catch (Exception ignored) {
            // fallback method name
        }

        return "UnknownMethod";
    }

    private void attachFailureScreenshot(ITestResult result, String failureType) {
        try {
            if (Boolean.TRUE.equals(result.getAttribute(SCREENSHOT_ATTACHED_KEY))) {
                return;
            }

            result.setAttribute(SCREENSHOT_ATTACHED_KEY, true);

            ExtentTest test = getOrCreateTest(result);

            AndroidDriver driver = BaseTest.driver;

            if (driver == null) {
                test.log(Status.WARNING, "Screenshot skipped because AndroidDriver is null.");
                return;
            }

            if (!DriverManager.isDriverHealthy()) {
                test.log(
                        Status.WARNING,
                        "Screenshot skipped because the emulator/UiAutomator2 session is not healthy."
                );
                return;
            }

            String screenshotName = cleanFileName(getMethodName(result) + "_" + failureType);

            String screenshotPath = ScreenshotUtils.captureScreenshot(driver, screenshotName);

            if (screenshotPath == null || screenshotPath.trim().isEmpty()) {
                test.log(Status.WARNING, "Screenshot capture failed. No screenshot path returned.");
                return;
            }

            test.addScreenCaptureFromPath(screenshotPath);

            test.log(
                    Status.INFO,
                    "<b>Failure screenshot attached:</b> " + escapeHtml(screenshotPath)
            );

            logToConsole("Failure screenshot captured: " + screenshotPath);

        } catch (Exception e) {
            try {
                ExtentTest test = getOrCreateTest(result);
                test.log(
                        Status.WARNING,
                        "Unable to attach failure screenshot: " + escapeHtml(e.getMessage())
                );
            } catch (Exception ignored) {
                // Do not fail listener
            }

            logToConsole("Unable to attach failure screenshot: " + e.getMessage());
        }
    }

    private void logThrowable(ExtentTest test, Throwable throwable) {
        if (throwable == null) {
            return;
        }

        test.log(Status.FAIL, "<b>Error:</b> " + escapeHtml(throwable.getMessage()));
        test.log(Status.FAIL, "<pre>" + escapeHtml(getStackTrace(throwable)) + "</pre>");
    }

    private String getStackTrace(Throwable throwable) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            throwable.printStackTrace(printWriter);

            return stringWriter.toString();
        } catch (Exception e) {
            return throwable.toString();
        }
    }

    private String cleanFileName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Failure_Screenshot";
        }

        return value.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void logToConsole(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                .format(new Date());

        System.out.println("[" + timestamp + "] [LISTENER] " + message);
    }
}