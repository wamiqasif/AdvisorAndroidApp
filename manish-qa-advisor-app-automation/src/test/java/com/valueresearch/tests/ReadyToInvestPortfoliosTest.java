package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.ReadyToInvestPortfoliosPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Locale;

/** Five original RTI regression cases, now targeting Stock Analyst's Choice. */
public class ReadyToInvestPortfoliosTest extends BaseTest {
    private ReadyToInvestPortfoliosPage readyPage;
    private AndroidDriver verifiedDriver;
    private String verifiedSession;
    private boolean accountVerified;
    private boolean completedCleanup;
    private String caseId;
    private Throwable setupFailure;

    @BeforeClass(alwaysRun = true)
    public void resetReadyToInvestSession() { invalidateSession(); }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void createReadyToInvestPage(Method method, @Optional("PASSWORD") String loginMethod) {
        readyPage = null;
        completedCleanup = false;
        accountVerified = false;
        setupFailure = null;
        caseId = method.getName().substring(0, 7);
        Test annotation = method.getAnnotation(Test.class);
        ExtentTest report = ExtentManager.getExtentReports().createTest(caseId + " - " + annotation.description());
        report.info("Module: Stock Analyst's Choice (legacy RTI regression)<br>Page Build: "
                + ReadyToInvestPortfoliosPage.BUILD_VERSION
                + "<br>Runtime data: stock names, dates, prices, tabs and secondary filters"
                + "<br>Account: configured active subscriber; shared password/OTP and biometric/PIN handling");
        ExtentTestManager.setTest(report);
        try {
            AuthHelper.LoginMethod selected;
            try { selected = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Suite loginMethod must be PASSWORD or OTP", invalid); }
            readyPage = createPage(selected);
            String session = sessionId();
            readyPage.prepareActiveAccount(verifiedDriver == driver && session.equals(verifiedSession));
            verifiedDriver = driver;
            verifiedSession = session;
            accountVerified = true;
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            setupFailure = failure;
            invalidateSession();
            captureFailure();
            if (readyPage != null) {
                try { readyPage.restoreTimingSettings(); }
                catch (RuntimeException | AssertionError restore) { failure.addSuppressed(restore); }
            }
            throw failure;
        }
    }

    protected ReadyToInvestPortfoliosPage createPage(AuthHelper.LoginMethod method) {
        return new ReadyToInvestPortfoliosPage(driver, method);
    }

    @Test(priority = 1, alwaysRun = true, description = "Open Stock Analyst's Choice and validate dynamic landing")
    public void RTI_001_OpenModuleAndValidateLanding() {
        runCase(() -> readyPage.validateLandingPageDynamically());
    }

    @Test(priority = 2, alwaysRun = true, description = "Validate every runtime top tab and secondary filter")
    public void RTI_002_ValidateAllTabsAndFiltersDynamically() {
        runCase(() -> readyPage.validateEveryTopSectionAndSecondaryFilterDynamically());
    }

    @Test(priority = 3, alwaysRun = true, description = "Verify horizontal table movement to the final column and back on every top tab")
    public void RTI_003_ValidateHorizontalTableScroll() {
        runCase(() -> readyPage.validateHorizontalTableScroll());
    }

    @Test(priority = 4, alwaysRun = true, description = "Validate vertical scrolling, current widget copy and Investment Planner CTA")
    public void RTI_004_ValidateVerticalScrollAndPortfolioPlannerCta() {
        runCase(() -> readyPage.validateVerticalScrollAndPortfolioPlannerCta());
    }

    @Test(priority = 5, alwaysRun = true, description = "Validate fast top-level Stock Analyst's Choice switching")
    public void RTI_005_ValidateTopLevelSwitchingFast() {
        runCase(() -> readyPage.validateTopLevelSwitchingFast());
    }

    private void runCase(Runnable validation) {
        if (!accountVerified || readyPage == null) {
            // TestNG can invoke alwaysRun tests after a configuration failure.
            // Preserve that real failure and avoid navigating/cleaning up on PIN.
            throw new SkipException(caseId + " blocked by incomplete account setup; see the configuration failure", setupFailure);
        }
        Throwable original = null;
        try {
            validation.run();
        } catch (RuntimeException | AssertionError failure) {
            original = failure;
            captureFailure(); // Preserve the failed UI before Hub cleanup.
            throw failure;
        } finally {
            finishCleanup(original);
        }
        ReportLogger.pass(caseId + " passed; app returned to Hub");
    }

    private void finishCleanup(Throwable original) {
        Throwable cleanup = null;
        try { readyPage.recoverToHubSafely(); }
        catch (RuntimeException | AssertionError failure) { cleanup = failure; invalidateSession(); }
        finally {
            try { readyPage.restoreTimingSettings(); }
            catch (RuntimeException | AssertionError failure) {
                if (cleanup == null) cleanup = failure; else cleanup.addSuppressed(failure);
            }
            completedCleanup = true;
        }
        if (cleanup != null) {
            if (original != null) original.addSuppressed(cleanup);
            else throw new AssertionError("Regression checks completed, but Hub/settings cleanup failed", cleanup);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupReadyToInvestState(ITestResult result) {
        if (readyPage == null || completedCleanup) return;
        // Covers a setup/feature interruption before the normal finally block.
        try {
            if (accountVerified) finishCleanup(result.getThrowable());
            else readyPage.restoreTimingSettings();
        } catch (RuntimeException | AssertionError failure) {
            if (result.getThrowable() != null) result.getThrowable().addSuppressed(failure);
            else { result.setThrowable(failure); result.setStatus(ITestResult.FAILURE); }
            invalidateSession();
        }
    }

    private String sessionId() {
        if (driver == null || driver.getSessionId() == null) throw new IllegalStateException("An active Appium session is required");
        return driver.getSessionId().toString();
    }

    private void invalidateSession() { verifiedDriver = null; verifiedSession = null; }

    private void captureFailure() {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, caseId + "_before_Hub_reset");
            ExtentTest report = ExtentTestManager.getTest();
            if (path != null && report != null) report.addScreenCaptureFromPath(path, "Failure before Hub reset");
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }
}
