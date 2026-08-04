package com.valueresearch.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.utils.DriverManager;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.IConfigurationListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentTestListener implements ITestListener, ISuiteListener, IConfigurationListener {

    private static final String SCREENSHOT_ATTACHED_KEY = "SCREENSHOT_ATTACHED_BY_LISTENER";

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