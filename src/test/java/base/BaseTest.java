package base;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;

import driver.DriverFactory;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import pages.DashboardPage;
import pages.PinPage;
import utils.ConfigReader;
import utils.ExtentManager;
import utils.ScreenshotUtil;

public class BaseTest {

    protected static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

    protected boolean shouldManageDriverPerMethod() {
        return true;
    }

    protected void onClassReady() {
        // optional override
    }

    protected void recoverAppState(Method method) {
        // optional override
    }

    @BeforeClass(alwaysRun = true)
    public final void beforeClass() {
        if (shouldManageDriverPerMethod()) {
            return;
        }

        logger.info("beforeClass: initializing driver for {}", getClass().getSimpleName());

        if (!initializeDriver()) {
            throw new SkipException("Driver initialization failed");
        }

        logger.info("beforeClass: driver ready — running onClassReady()");
        onClassReady();
        logger.info("beforeClass: complete for {}", getClass().getSimpleName());
    }

    @BeforeMethod(alwaysRun = true)
    public final void setUp(Method method) {
        logger.info("=== setUp: {} ===", method.getName());

        createExtentTest(method);
        getExtentTest().info("Starting test: " + method.getName());

        boolean driverWasAlive = DriverFactory.isDriverAlive();
        if (!initializeDriver()) {
            throw new SkipException("Driver recovery failed");
        }
        if (!shouldManageDriverPerMethod() && !driverWasAlive) {
            logger.warn("Driver session recreated");
            onClassReady();
        }

        logger.info("setUp: ensuring app is running");
        ensureAppIsRunning();

        if (requiresPinScreen(method)) {
            logger.info("setUp: ensuring PIN screen");
            ensurePinScreenReady();
        } else {
            logger.info("setUp: ensuring dashboard ready");
           // safelyRecoverHomeState();
            ensureDashboardReady();
        }

        logger.info("setUp: running recoverAppState()");
        recoverAppState(method);

        logger.info("setUp: complete — {} ready to execute", method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public final void tearDown(ITestResult result) {
        logger.info("=== tearDown: {} ===", result.getName());

        ExtentTest test = getExtentTest();

        switch (result.getStatus()) {
            case ITestResult.SUCCESS:
                logger.info("tearDown: PASSED — {}", result.getName());
                test.pass("Test passed");
                break;

            case ITestResult.FAILURE:
                logger.warn("tearDown: FAILED — {}", result.getName());
                handleFailure(result, test);
                break;

            case ITestResult.SKIP:
                String skipMessage = result.getThrowable() != null
                        ? result.getThrowable().getMessage()
                        : "No skip reason";
                logger.info("tearDown: SKIPPED — {}", result.getName());
                test.skip(skipMessage);
                break;

            default:
                logger.warn("Unknown status: {}", result.getStatus());
        }

        // Restore app state for persistent session
      

        if (shouldManageDriverPerMethod()) {
            DriverFactory.quitDriver();
        }

        ExtentManager.removeTest();
    }

    @AfterSuite(alwaysRun = true)
    public final void afterSuite() {
        if (DriverFactory.isDriverAlive()) {
            DriverFactory.quitDriver();
        }

        ExtentManager.flush();
    }

    protected AndroidDriver getDriver() {
        return DriverFactory.getDriver();
    }

    protected ExtentTest getExtentTest() {
        return ExtentManager.getTest();
    }

    private boolean initializeDriver() {
        if (DriverFactory.isDriverAlive()) {
            logger.info("initializeDriver: session alive — reusing");
            return true;
        }

        logger.info("initializeDriver: no active session — creating driver");
        try {
            DriverFactory.initDriver();
            waitForFreshAppLaunch();
            logger.info("initializeDriver: driver created — Flutter stabilization complete");
            return true;
        } catch (Exception e) {
            logger.error("initializeDriver: failed — {}", e.getMessage());
            return false;
        }
    }

    private void ensureAppIsRunning() {

        String appPackage =
                ConfigReader.getInstance()
                        .getAppPackage();

        try {

            ApplicationState state =
                    getDriver().queryAppState(appPackage);

            if (state
                    == ApplicationState.RUNNING_IN_FOREGROUND) {

                logger.info("ensureAppIsRunning: app already in foreground");
                return;
            }

            logger.info("ensureAppIsRunning: app state is {} — activating", state);

        } catch (Exception ignored) {
        }

        try {

            getDriver().activateApp(appPackage);

            logger.info("ensureAppIsRunning: activateApp() called — waiting for Flutter stabilization");

            // VERY IMPORTANT
            waitForFreshAppLaunch();

            logger.info("ensureAppIsRunning: app activated and stable");

        } catch (Exception e) {

            logger.warn(
                    "ensureAppIsRunning: unable to activate app — {}",
                    e.getMessage());
        }
    }

    private void ensureDashboardReady() {

        DashboardPage dashboardPage =
                new DashboardPage(getDriver());

        PinPage pinPage =
                new PinPage(getDriver());

        // PIN screen — app just launched or session restored
        if (pinPage.isPinScreenDisplayed()) {

            logger.info("ensureDashboardReady: PIN screen detected — entering PIN");

            pinPage.enterPin(
                    ConfigReader.getInstance()
                            .getLoginPin());

            waitForUiToSettle();

            logger.info("ensureDashboardReady: PIN entered — Flutter stabilization complete");
        }

        // Bottom navigation visible — tap Hub to normalize starting state
        if (dashboardPage.isBottomNavigationVisible()) {

            logger.info("ensureDashboardReady: bottom navigation visible — tapping Hub tab");

            dashboardPage.tapHubTab();

            waitForUiToSettle();

            logger.info("ensureDashboardReady: Hub tab tapped — dashboard ready");

            return;
        }

        // Navigation hidden — unwind Flutter route stack (max 6 back presses)
        logger.info("ensureDashboardReady: bottom navigation hidden — starting back-press recovery");

        for (int i = 0; i < 15; i++) {

            logger.info("ensureDashboardReady: back press {} / 15", i + 1);

            getDriver().navigate().back();
            handleRecoveryPopupIfPresent();
            waitForUiToSettle(); 

            if (dashboardPage.isBottomNavigationVisible()) {

                logger.info("ensureDashboardReady: navigation restored after {} back press(es) — tapping Hub", i + 1);

                dashboardPage.tapHubTab();

                waitForUiToSettle();

                logger.info("ensureDashboardReady: Hub tab tapped — dashboard ready");

                return;
            }
        }

        logger.warn(
                "ensureDashboardReady: bottom navigation not visible after 6 back presses — test may fail");
    }
    private void handleRecoveryPopupIfPresent() {

        try {

            List<By> popupButtons =
                    Arrays.asList(

                            AppiumBy.accessibilityId("Allow"),
                            AppiumBy.accessibilityId("OK"),
                            AppiumBy.accessibilityId("Yes"),
                            AppiumBy.accessibilityId("Leave"),
                            AppiumBy.accessibilityId("Continue"),
                            AppiumBy.accessibilityId("Cancel"));

            for (By locator : popupButtons) {

                List<WebElement> elements =
                        getDriver().findElements(locator);

                if (!elements.isEmpty()
                        && elements.get(0).isDisplayed()) {

                    elements.get(0).click();

                    waitForUiToSettle();

                    logger.info(
                            "Recovery popup handled: {}",
                            locator);

                    return;
                }
            }

        } catch (Exception e) {

            logger.debug(
                    "No recovery popup present: {}",
                    e.getMessage());
        }
    }

    private void safelyRecoverHomeState() {

        try {

            DashboardPage dashboardPage =
                    new DashboardPage(getDriver());

            PinPage pinPage =
                    new PinPage(getDriver());

            // PIN recovery
            if (pinPage.isPinScreenDisplayed()) {

                pinPage.enterPin(
                        ConfigReader.getInstance()
                                .getLoginPin());

                waitForUiToSettle();
            }

            // Direct tab navigation recovery
            boolean recovered =
                    dashboardPage.forceNavigateToHome();

            if (recovered) {

                logger.info(
                        "safelyRecoverHomeState: Home restored");

                return;
            }

            // Final fallback
            ensureAppIsRunning();

            waitForUiToSettle();

            dashboardPage.forceNavigateToHome();

        } catch (Exception e) {

            logger.warn(
                    "safelyRecoverHomeState failed: {}",
                    e.getMessage());
        }
    }
  
    private void waitForFreshAppLaunch() {

        try {

            Thread.sleep(5000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }
    protected void waitForUiToSettle() {

        try {

            Thread.sleep(1500);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    private void ensurePinScreenReady() {
        PinPage pinPage = new PinPage(getDriver());
        Assert.assertTrue(
                pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before test execution");
    }

    private void createExtentTest(Method method) {
        org.testng.annotations.Test annotation = method.getAnnotation(org.testng.annotations.Test.class);
        String description = annotation != null && !annotation.description().isEmpty()
                ? annotation.description()
                : method.getName();
        ExtentManager.createTest(method.getName(), description);
    }

    private void handleFailure(ITestResult result, ExtentTest test) {
        String failureMessage = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Unknown failure";
        logger.error("FAILED: {} - {}", result.getName(), failureMessage);

        test.fail(failureMessage);

        if (!DriverFactory.isDriverAlive()) {
            logger.warn("Skipping screenshot - driver unavailable");
            return;
        }

        try {
            String screenshotPath = ScreenshotUtil.capture(getDriver(), result.getName());
            if (screenshotPath != null) {
                test.fail("Failure Screenshot",
                        MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
            }
        } catch (Exception e) {
            logger.warn("Screenshot capture failed: {}", e.getMessage());
        }
    }

    private boolean requiresPinScreen(Method method) {
        return getClass().isAnnotationPresent(RequiresPinScreen.class)
                || method.isAnnotationPresent(RequiresPinScreen.class);
    }
}
