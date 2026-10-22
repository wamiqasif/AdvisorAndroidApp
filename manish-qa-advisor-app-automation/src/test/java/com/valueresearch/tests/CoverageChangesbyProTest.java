package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.CoverageChangesbyProPage;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Independent product-change cases; no default-account login in feature methods. */
public class CoverageChangesbyProTest extends BaseTest {
    private AdvisorAccountSession accountSession;
    private CoverageChangesbyProPage page;
    private ExtentTest report;
    private String caseId;
    private boolean accountPrepared;

    @BeforeClass(alwaysRun = true)
    public void resetCoverageAccountSession() { accountSession = null; }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void prepareCoverageChange(Method method, @Optional("PASSWORD") String loginMethod) {
        page = null;
        accountPrepared = false;
        caseId = method.getName().substring(0, 6);
        Test test = method.getAnnotation(Test.class);
        report = ExtentManager.getExtentReports().createTest(caseId + " - " + test.description());
        report.info("Module: Coverage, Insights & Reports product changes<br>Page build: "
                + CoverageChangesbyProPage.BUILD_VERSION);
        ExtentTestManager.setTest(report);
        try {
            if (!"PASSWORD".equalsIgnoreCase(loginMethod.trim())) {
                throw new IllegalArgumentException("Coverage changes require loginMethod=PASSWORD");
            }
            if (accountSession == null || !accountSession.isForDriver(driver)) {
                accountSession = new AdvisorAccountSession(driver);
            }
            AuthHelper.Account account = Arrays.asList(test.groups()).contains("lapsed-user")
                    ? AuthHelper.Account.LAPSED_USER : AuthHelper.Account.ACTIVE_SUBSCRIBER;
            // Verifies account email, reuses matching sessions, and switches only when needed.
            // AuthHelper also cancels Android biometrics before entering the configured PIN.
            accountSession.prepare(account, AuthHelper.LoginMethod.PASSWORD);
            page = new CoverageChangesbyProPage(driver, accountSession);
            accountPrepared = true;
            page.navigateFundsToHubStocks();
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            if (accountSession != null) accountSession.invalidate();
            ReportLogger.fail("Account/navigation setup failed: " + failure.getClass().getSimpleName());
            attachFailureScreenshot();
            restoreTimingsAfterSetupFailure(failure);
            throw failure;
        }
    }

    // TC_001
    @Test(priority = 1, groups = "active-user", description = "Verify Coverage, Insights and Reports Hub icons")
    public void TC_001_VerifyCoverageInsightsReportsIcons() {
        page.verifyCoverageInsightsReportsIcons();
    }

    // TC_002
    @Test(priority = 2, groups = "active-user", description = "Open a dynamic company drawer, expand and swipe down to close")
    public void TC_002_VerifyDynamicStockDetailsDrawer() {
        page.verifyDynamicStockDrawerExpansionAndDismissal();
    }

    // TC_003
    @Test(priority = 3, groups = "lapsed-user", description = "Verify exact lapsed-user Coverage and Insights subscription copy")
    public void TC_003_VerifyLapsedCoverageAndInsightsCopy() {
        page.verifyLapsedCoverageAndInsightsCopy();
    }

    @AfterMethod(alwaysRun = true)
    public void restoreHubAfterCoverageChange(ITestResult result) {
        Throwable original = result.getThrowable();
        if (original != null) attachFailureScreenshot();
        Throwable cleanupFailure = null;
        try {
            if (accountPrepared && page != null) page.returnToHub();
        } catch (RuntimeException | AssertionError failure) {
            cleanupFailure = failure;
            if (accountSession != null) accountSession.invalidate();
        } finally {
            if (accountSession != null) {
                try { accountSession.restoreTimingSettings(); }
                catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                    accountSession.invalidate();
                }
            }
            accountPrepared = false;
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else {
                result.setThrowable(cleanupFailure);
                result.setStatus(ITestResult.FAILURE);
                attachFailureScreenshot();
            }
            ReportLogger.fail("Hub/timing recovery failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void restoreTimingsAfterSetupFailure(Throwable original) {
        if (accountSession == null) return;
        try { accountSession.restoreTimingSettings(); }
        catch (RuntimeException | AssertionError failure) { original.addSuppressed(failure); }
    }

    private void attachFailureScreenshot() {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, caseId + "_before_Hub_reset");
            if (path != null && report != null) report.addScreenCaptureFromPath(path, "Failed state before Hub recovery");
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }
}
