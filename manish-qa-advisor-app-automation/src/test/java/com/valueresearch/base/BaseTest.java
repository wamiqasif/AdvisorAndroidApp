package com.valueresearch.base;

import com.valueresearch.utils.DriverManager;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public class BaseTest {

    public static AndroidDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        driver = DriverManager.getDriver();
    }

    /**
     * Global recovery checkpoint before every test method.
     *
     * <p>If the previous test killed UiAutomator2 or the emulator, the stale
     * session is discarded, the emulator is restarted when required, and a
     * fresh UiAutomator2 session is created before the next test begins.</p>
     */
    @BeforeMethod(alwaysRun = true)
    public void ensureHealthyDriverBeforeEveryTest() {
        driver = DriverManager.getDriver();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
        driver = null;
    }
}