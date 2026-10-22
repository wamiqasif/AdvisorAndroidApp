package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.StockAnalystsChoiceChangebyProPage;
import com.valueresearch.pages.StockAnalystsChoiceChangebyProPage.Persona;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/** Common authentication runs before each selected case; every feature case restores Hub. */
public class StockAnalystsChoiceChangebyProTest extends BaseTest {

    private StockAnalystsChoiceChangebyProPage page;
    private String caseId;
    private boolean accountVerified;
    // These belong to the test-class run, not the per-method page object.
    private Persona verifiedPersona;
    private AndroidDriver verifiedDriver;
    private String verifiedSessionId;

    @BeforeClass(alwaysRun = true)
    public void resetVerifiedAccountCache() {
        clearVerifiedAccount();
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void prepareReportAndPage(Method method, @Optional("PASSWORD") String loginMethod) {
        // TestNG runs BaseTest's driver checkpoint first, then this common module setup.
        // Authentication is a configuration step; test bodies contain only module checks.
        caseId = method.getName().substring(0, 6);
        accountVerified = false;
        page = null;
        Test annotation = method.getAnnotation(Test.class);
        ExtentTest report = ExtentManager.getExtentReports().createTest(
                caseId + " - " + annotation.description());
        report.info("Module: Recommendations / Stock Analyst's Choice<br>"
                + "Page build: " + StockAnalystsChoiceChangebyProPage.BUILD_VERSION + "<br>"
                + "Setup: shared AuthHelper; verify once and reuse the matching persona/session<br>"
                + "Completion: return to Hub; restore shared timing settings");
        ExtentTestManager.setTest(report);
        try {
            AuthHelper.LoginMethod selectedMethod;
            try {
                selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException invalidMethod) {
                throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
            }
            page = createPage(selectedMethod);
            Persona requiredPersona = requiredPersonaFor(method);
            prepareRequiredAccount(requiredPersona);
            page.assertHubVisible();
            accountVerified = true;
        } catch (RuntimeException | AssertionError | LinkageError setupFailure) {
            clearVerifiedAccount();
            ReportLogger.fail("App/account preparation failed: "
                    + setupFailure.getClass().getSimpleName()
                    + ". See the TestNG configuration failure trace.");
            if (setupFailure instanceof LinkageError) {
                ReportLogger.fail("Check that the updated AuthHelper.java is installed and rebuild the project");
            }
            attachFailureScreenshot();
            if (page != null) {
                try {
                    page.restoreTimingSettings();
                } catch (RuntimeException | AssertionError restoreFailure) {
                    setupFailure.addSuppressed(restoreFailure);
                }
            }
            throw setupFailure; // TestNG records setup failure and skips this feature case.
        }
    }

    /** Override only for an isolated fixture; production always uses the current BaseTest driver. */
    protected StockAnalystsChoiceChangebyProPage createPage(AuthHelper.LoginMethod loginMethod) {
        return new StockAnalystsChoiceChangebyProPage(driver, loginMethod);
    }

    /** TC_012/TC_013 use the lapsed account; other cases use the active account. */
    private Persona requiredPersonaFor(Method method) {
        Test test = method.getAnnotation(Test.class);
        for (String group : test.groups()) {
            if ("lapsed-user".equals(group)) {
                return Persona.LAPSED_USER;
            }
        }
        return Persona.ACTIVE_SUBSCRIBER;
    }

    // TC_001: verified active account -> Funds (fresh login) -> Hub -> Stocks -> module.
    @Test(priority = 1, enabled = true, description = "Verify active-user navigation and Analyst's Choice landing")
    public void TC_001_VerifyActiveUserNavigation() {
        runModuleCase(() -> page.verifyHeading());
    }

    // TC_002: use the captured top UI Back button; recovery cannot turn a failure into a pass.
    @Test(priority = 2, enabled = true, description = "Verify the top Back button returns to Hub")
    public void TC_002_VerifyTopBackButtonAndReturnToHub() {
        runModuleCase(() -> page.verifyTopBackButtonAndReturnToHub());
    }

    // TC_003: separate heading check, as explicitly requested.
    @Test(priority = 3, enabled = true, description = "Verify the exact Analyst's Choice heading")
    public void TC_003_VerifyAnalystsChoiceHeading() {
        runModuleCase(() -> page.verifyHeading());
    }

    // TC_004: Know More -> expanded Buy -> completed content -> no Dividend definition.
    @Test(priority = 4, enabled = true, description = "Verify Dividend definition is absent from Buy")
    public void TC_004_VerifyBuyHasNoDividendDefinition() {
        runModuleCase(() -> page.verifyBuyHasNoDividendDefinition());
    }

    // TC_005: Know More -> Buy -> close Buy -> Hold -> both new headings and full copy.
    @Test(priority = 5, enabled = true, description = "Verify both new Hold definitions after closing Buy")
    public void TC_005_VerifyNewHoldDefinitions() {
        runModuleCase(() -> page.verifyNewHoldDefinitions());
    }

    // TC_006: Buy > All -> live stock drawer -> full screen -> top drag down -> Hub.
    @Test(priority = 6, enabled = true, description = "Verify stock drawer expands to full screen and dismisses by dragging its top down")
    public void TC_006_VerifyStockDrawerExpansionAndDismissal() {
        runModuleCase(() -> page.verifyStockDrawerExpansionAndDismissal());
    }

    @Test(priority = 7, enabled = true, description = "Verify the updated end-of-list widget on Buy, Hold and Sell without selecting stocks")
    public void TC_007_VerifyEndOfListWidgetOnAllTabs() {
        runModuleCase(() -> page.verifyEndOfListWidgetOnAllTabs());
    }

    @Test(priority = 8, enabled = true, description = "Verify Customise, Investment Planner and creation of a unique watchlist containing one selected stock")
    public void TC_008_VerifyNavigationAndCreateWatchlist() {
        runModuleCase(() -> page.verifyNavigationAndCreateWatchlist());
    }

    @Test(priority = 9, enabled = true, description = "Verify ascending and descending Reco Date sorting on Buy/Hold and Exit Date sorting on Sell")
    public void TC_009_VerifyDateSortingOnAllTabs() {
        runModuleCase(() -> page.verifyDateSortingOnAllTabs());
    }

    @Test(priority = 10, enabled = true, description = "Verify the last column is exactly Vs BSE 500 TRI Return on Buy, Hold and Sell")
    public void TC_010_VerifyLastColumnHeadingOnAllTabs() {
        runModuleCase(() -> page.verifyLastColumnHeadingOnAllTabs());
    }

    @Test(priority = 11, enabled = true, description = "Verify the watchlist Name helper includes Watchlist 1 before any additional examples")
    public void TC_011_VerifyWatchlistNameHelperText() {
        runModuleCase(() -> page.verifyWatchlistNameHelperText());
    }

    @Test(priority = 12, enabled = true, groups = "lapsed-user",
            description = "Verify lapsed-user subscription pitch presence on Buy, Hold and Sell")
    public void TC_012_VerifyLapsedSubscriptionPitchOnAllTabs() {
        runModuleCase(() -> page.verifyLapsedSubscriptionPitchOnAllTabs());
    }

    @Test(priority = 13, enabled = true, groups = "lapsed-user",
            description = "Verify actual lapsed-user table movement to the horizontal end and back on Buy, Hold and Sell")
    public void TC_013_VerifyLapsedHorizontalScrollingOnAllTabs() {
        runModuleCase(() -> page.verifyLapsedHorizontalScrollingOnAllTabs());
    }

    private void runModuleCase(Runnable validation) {
        runCase(validation);
    }

    private void runCase(Runnable validation) {
        Throwable originalFailure = null;
        try {
            if (!accountVerified || page == null) {
                throw new IllegalStateException("Common @BeforeMethod authentication setup must complete before module checks");
            }
            page.openFromHub();
            validation.run();
        } catch (RuntimeException | AssertionError failure) {
            originalFailure = failure;
            if (accountVerified) {
                attachFailureScreenshot(); // Capture the failed module state before returning to Hub.
            }
            throw failure;
        } finally {
            Throwable cleanupFailure = null;
            try {
                if (accountVerified || page.canReturnToHub()) {
                    page.returnToHub();
                    page.assertHubVisible();
                }
            } catch (RuntimeException | AssertionError failure) {
                cleanupFailure = failure;
            } finally {
                try {
                    page.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
            }
            if (cleanupFailure != null) {
                ReportLogger.fail(caseId + " cleanup failed: " + cleanupFailure.getClass().getSimpleName());
                if (originalFailure != null) {
                    originalFailure.addSuppressed(cleanupFailure);
                } else {
                    throw new AssertionError("Case validation completed, but Hub/settings reset failed", cleanupFailure);
                }
            }
        }
        ReportLogger.pass(caseId + " passed; app returned to Hub");
    }

    private void prepareRequiredAccount(Persona requiredPersona) {
        String sessionId = driver.getSessionId() == null ? "" : driver.getSessionId().toString();
        if (sessionId.isEmpty()) {
            clearVerifiedAccount();
            throw new IllegalStateException("An active Appium session is required before account preparation");
        }
        boolean sameVerifiedSession = verifiedPersona == requiredPersona
                && verifiedDriver == driver && sessionId.equals(verifiedSessionId);
        if (sameVerifiedSession && page.reuseVerifiedAccountSession(requiredPersona)) {
            ReportLogger.debug("Reusing verified " + requiredPersona + " session; account email check skipped");
            return;
        }

        // Common setup for the first selected test, a persona change, or lost/recreated
        // authentication: verify the actual email and switch accounts if needed.
        // Cache only after successful verification; failed setup must not be reused.
        clearVerifiedAccount();
        page.ensureRequiredAccount(requiredPersona);
        verifiedPersona = requiredPersona;
        verifiedDriver = driver;
        verifiedSessionId = sessionId;
    }

    private void clearVerifiedAccount() {
        verifiedPersona = null;
        verifiedDriver = null;
        verifiedSessionId = null;
    }

    private void attachFailureScreenshot() {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, caseId + "_before_Hub_reset");
            ExtentTest report = ExtentTestManager.getTest();
            if (path != null && report != null) {
                report.addScreenCaptureFromPath(path, "Failed module state before Hub reset");
            }
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }

    @AfterMethod(alwaysRun = true)
    public void restoreSettingsAfterUnexpectedFailure(ITestResult result) {
        if (page == null) {
            return;
        }
        // Normally a no-op: runModuleCase already restored settings before TestNG
        // records its result. This hook also covers an unexpectedly interrupted case.
        try {
            page.restoreTimingSettings();
        } catch (RuntimeException | AssertionError failure) {
            Throwable original = result.getThrowable();
            if (original != null) {
                original.addSuppressed(failure);
            } else {
                result.setThrowable(failure);
                result.setStatus(ITestResult.FAILURE);
            }
            ReportLogger.fail(caseId + " shared timing settings could not be restored");
        }
    }
}
