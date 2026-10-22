package com.valueresearch.base;

import com.valueresearch.utils.DriverManager;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/** Shared Appium session lifecycle for existing Advisor test modules. */
public class BaseTest {

    public static AndroidDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        driver = requireDriver("@BeforeClass");
    }

    /** Revalidate/recover UiAutomator2 before each test. */
    @BeforeMethod(alwaysRun = true)
    public void ensureHealthyDriverBeforeEveryTest() {
        driver = requireDriver("@BeforeMethod");
    }

    private AndroidDriver requireDriver(String phase) {
        try {
            AndroidDriver current = DriverManager.getDriver();
            if (current == null) {
                throw new IllegalStateException("DriverManager.getDriver() returned null");
            }
            if (current.getSessionId() == null) {
                throw new IllegalStateException("AndroidDriver has no Appium session ID");
            }
            System.out.println("[DRIVER] " + phase + " ready | Session ID: "
                    + current.getSessionId());
            return current;
        } catch (RuntimeException failure) {
            System.err.println("[DRIVER] " + phase + " failed: " + failure);
            failure.printStackTrace(System.err);
            // Keep the root failure as the cause rather than masking it with an NPE.
            throw new IllegalStateException(
                    "Failed to initialize/verify AndroidDriver during " + phase
                            + ". Check Appium server, ADB udid, and recovery logs.", failure);
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        try {
            DriverManager.quitDriver();
        } finally {
            driver = null;
        }
    }
}
