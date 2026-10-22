package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.Batch1ChangesbyProPage;
import com.valueresearch.pages.Batch1ChangesbyProPage.Persona;
import com.valueresearch.pages.Batch1ChangesbyProPage.StockFeature;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.lang.reflect.Method;

/** TC_001–TC_005 from Module 1 testing, using the latest user-defined flows. */
public class Batch1ChangesbyProTest extends BaseTest {

    private Batch1ChangesbyProPage page;
    private boolean setupCompleted;
    private String currentTestCase;

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void prepareTestReport(Method testMethod, @Optional("PASSWORD") String loginMethod) {
        setupCompleted = false;
        page = null;
        String reportName;
        String description;
        switch (testMethod.getName()) {
            case "TC_001_VerifyStockFeaturesNavigation":
                currentTestCase = "TC_001";
                reportName = "Stocks features navigation";
                description = "Active subscriber: Stocks order, destination headings and Back to Hub";
                break;
            case "TC_002_VerifyWatchlistWebViewNavigation":
                currentTestCase = "TC_002";
                reportName = "Watchlist in-app WebView navigation";
                description = "Active subscriber: verified account, Hub > More > Watchlist, WebView and Back to Hub";
                break;
            case "TC_003_VerifyActiveSubscriberAccountAccess":
                currentTestCase = "TC_003";
                reportName = "Active subscriber account access";
                description = "Reuse matching session or login; verify active subscriber email and inner app access";
                break;
            case "TC_004_VerifyLapsedUserAccountAccess":
                currentTestCase = "TC_004";
                reportName = "Lapsed user account access";
                description = "Reuse matching session or login; verify lapsed user email and inner app access";
                break;
            case "TC_005_VerifyTeamWebViewSessionContinuity":
                currentTestCase = "TC_005";
                reportName = "Team in-app WebView navigation";
                description = "Active subscriber: verified account, Hub > Stocks > About the Team, native WebView and Back to Hub";
                break;
            default:
                throw new IllegalArgumentException("Unmapped Batch 1 test: " + testMethod.getName());
        }
        ExtentTest report = ExtentManager.getExtentReports().createTest(
                currentTestCase + " - " + reportName, description);
        ExtentTestManager.setTest(report);

        AuthHelper.LoginMethod selectedMethod;
        try {
            selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalidMethod) {
            throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD", invalidMethod);
        }
        // BaseTest's inherited recovery runs first; bind shared auth to its current driver.
        page = new Batch1ChangesbyProPage(driver, selectedMethod);
        report.info("Authentication: shared AuthHelper; login method: " + selectedMethod
                + "; cancel biometrics before entering the configured PIN");
    }

    // TC_001: Module 1 testing, item 1 - Hub > Stocks order and destination navigation.
    @Test(priority = 1, description = "Verify all nine Stocks features and return to Hub after each feature")
    public void TC_001_VerifyStockFeaturesNavigation() {
        prepareAccount(Persona.ACTIVE_SUBSCRIBER);
        page.scrollToStocksSection();
        SoftAssert checks = new SoftAssert();
        // Scrolling stopped at Investment Planner. Begin without another scroll pass.
        for (StockFeature feature : StockFeature.values()) {
            boolean headingVerified = false;
            try {
                page.openFeatureAndVerifyHeading(feature);
                headingVerified = true;
            } catch (AssertionError | RuntimeException e) {
                // Record the soft failure while the destination is still visible.
                recordFailure(feature.label(), e, checks);
            } finally {
                // Always attempt Back, even when the heading differs or is missing.
                try {
                    page.returnToHubAfterFeature(feature);
                    if (headingVerified) {
                        ReportLogger.pass(feature.label() + ": heading verified; Back returned to Hub");
                    } else {
                        ReportLogger.step("Hub restored after " + feature.label() + " verification failure");
                    }
                } catch (AssertionError | RuntimeException backFailure) {
                    recordFailure(feature.label() + " - Back to Hub", backFailure, checks);
                    // Recovery permits continuation only after Hub is verified; Back failure stays recorded.
                    try {
                        page.recoverToHub();
                    } catch (AssertionError | RuntimeException recovery) {
                        recovery.addSuppressed(backFailure);
                        throw recovery;
                    }
                }
            }
        }
        try {
            page.verifyStocksEntryOrder();
        } catch (AssertionError | RuntimeException e) {
            recordFailure("Stocks entry order", e, checks);
        }
        // The requested reset is Hub. Do not scroll back through every section to Profile.
        page.recoverToHub();
        page.assertHubVisible();
        checks.assertAll();
        ReportLogger.pass("TC_001 passed; all nine features verified and app reset to Hub");
    }

    @Test(priority = 2, description = "Verify Watchlist opens inside Advisor and Back returns to Hub")
    public void TC_002_VerifyWatchlistWebViewNavigation() {
        prepareAccount(Persona.ACTIVE_SUBSCRIBER);
        SoftAssert checks = new SoftAssert();
        try {
            page.openWatchlistAndVerifyInAppWebView();
        } catch (AssertionError | RuntimeException failure) {
            recordFailure("Watchlist in-app WebView", failure, checks);
        } finally {
            try {
                page.returnToHubAfterWatchlist();
            } catch (AssertionError | RuntimeException backFailure) {
                recordFailure("Watchlist - Back to Hub", backFailure, checks);
                try {
                    page.recoverToHub();
                } catch (AssertionError | RuntimeException recovery) {
                    recovery.addSuppressed(backFailure);
                    throw recovery;
                }
            }
        }
        page.assertInnerAppAccessible();
        checks.assertAll();
        ReportLogger.pass("TC_002 passed; Watchlist opened in-app and Back returned to Hub");
    }

    @Test(priority = 3, description = "Verify the active subscriber account can reach the inner app on Hub")
    public void TC_003_VerifyActiveSubscriberAccountAccess() {
        prepareAccount(Persona.ACTIVE_SUBSCRIBER);
        page.assertInnerAppAccessible();
        ReportLogger.pass("Active VRSA only Subscriber is logged in; Test passed");
    }

    @Test(priority = 4, description = "Verify the lapsed user account can reach the inner app on Hub")
    public void TC_004_VerifyLapsedUserAccountAccess() {
        prepareAccount(Persona.LAPSED_USER);
        page.assertInnerAppAccessible();
        ReportLogger.pass("TC_004 passed; lapsed user account verified and app left on Hub");
    }

    @Test(priority = 5, description = "Verify Team opens in an in-app WebView and Back returns to Hub")
    public void TC_005_VerifyTeamWebViewSessionContinuity() {
        // Reuse TC_001's active subscriber persona and verify its actual email before navigation.
        prepareAccount(Persona.ACTIVE_SUBSCRIBER);
        SoftAssert checks = new SoftAssert();
        try {
            page.openTeamAndVerifyInAppWebView();
        } catch (AssertionError | RuntimeException failure) {
            recordFailure("Team in-app WebView", failure, checks);
        } finally {
            try {
                page.returnToHubAfterTeam();
            } catch (AssertionError | RuntimeException backFailure) {
                recordFailure("Team - Back to Hub", backFailure, checks);
                try {
                    page.recoverToHub();
                } catch (AssertionError | RuntimeException recovery) {
                    recovery.addSuppressed(backFailure);
                    throw recovery;
                }
            }
        }
        page.assertHubVisible();
        checks.assertAll();
        ReportLogger.pass("TC_005 passed; Team WebView verified and Back returned to Hub");
    }

    private void prepareAccount(Persona persona) {
        // Keep account assertions inside each test so auth failures are test failures, not suite skips.
        page.ensureRequiredAccount(persona);
        setupCompleted = true;
    }

    @AfterMethod(alwaysRun = true)
    public void resetHubAfterTest() {
        if (setupCompleted && page != null && driver != null && !page.isHubScreenVisible()) {
            // Failed authentication has no logged-in Hub to restore; preserve its original failure.
            // Deliberately propagate cleanup failure instead of marking a broken reset as success.
            page.recoverToHub();
            page.assertHubVisible();
        }
    }

    private void recordFailure(String step, Throwable failure, SoftAssert checks) {
        String message = currentTestCase + " - " + step + ": " + ReportLogger.summary(failure);
        checks.fail(message, failure);
        ReportLogger.fail(message);
        String screenshot;
        try {
            screenshot = ScreenshotUtils.captureScreenshot(driver, currentTestCase + "_" + step);
        } catch (RuntimeException e) {
            ReportLogger.debug("Step screenshot unavailable: " + ReportLogger.summary(e));
            return; // The original soft failure is already recorded; continue the feature loop.
        }
        ExtentTest report = ExtentTestManager.getTest();
        if (report != null && screenshot != null) {
            try {
                report.addScreenCaptureFromPath(screenshot, step);
            } catch (Exception e) {
                ReportLogger.debug("Could not attach the step screenshot: " + e.getClass().getSimpleName());
            }
        }
    }
}
