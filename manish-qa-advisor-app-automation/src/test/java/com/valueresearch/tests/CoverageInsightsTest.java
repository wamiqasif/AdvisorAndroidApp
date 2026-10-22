package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.CoverageInsightsPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import java.lang.reflect.Method;
import java.util.Locale;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Dynamic smoke/regression coverage for Hub -> Stocks -> Coverage and insights.
 *
 * No article title, runtime category label/count, company name or publication date
 * is hardcoded. Top-level business labels (Coverage/Insights/Reports) are fixed UI contracts.
 * The page object resolves live Flutter tab semantics, discovers runtime categories, reads
 * live article/report semantics, locates Coverage search through the Inspector-proven
 * android.widget.EditText structure, and avoids redundant Hub-tab refreshes.
 */
public class CoverageInsightsTest extends BaseTest {

    private CoverageInsightsPage coverageInsightsPage;
    private AdvisorAccountSession accountSession;
    private boolean accountPrepared;
    private String currentCaseId;
    private ExtentTest currentReport;

    @BeforeClass(alwaysRun = true)
    public void resetAccountSession() {
        accountSession = null;
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void createCoverageInsightsPage(Method method, @Optional("PASSWORD") String loginMethod) {
        coverageInsightsPage = null;
        accountPrepared = false;
        currentCaseId = null;
        currentReport = null;
        Test annotation = method.getAnnotation(Test.class);
        String caseId = method.getName().substring(0, 6);
        startExtentCase(caseId, annotation.description(),
                "Shared account verification, configured login and biometric/PIN authentication before feature checks");
        try {
            AuthHelper.LoginMethod selectedMethod;
            try {
                selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
            }
            if (accountSession == null || !accountSession.isForDriver(driver)) {
                accountSession = createAccountSession(driver);
            }
            accountSession.prepare(requiredAccountFor(method), selectedMethod);
            accountSession.assertHubVisible();
            coverageInsightsPage = createPage(driver, accountSession);
            accountPrepared = true;
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            if (accountSession != null) accountSession.invalidate();
            ReportLogger.fail("App/account preparation failed: " + failure.getClass().getSimpleName()
                    + ". See the TestNG configuration failure trace.");
            attachFailureScreenshot();
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
    }

    protected AdvisorAccountSession createAccountSession(AndroidDriver currentDriver) {
        return new AdvisorAccountSession(currentDriver);
    }

    protected CoverageInsightsPage createPage(AndroidDriver currentDriver, AdvisorAccountSession session) {
        return new CoverageInsightsPage(currentDriver, session);
    }

    private AuthHelper.Account requiredAccountFor(Method method) {
        for (String group : method.getAnnotation(Test.class).groups()) {
            if ("lapsed-user".equals(group)) return AuthHelper.Account.LAPSED_USER;
        }
        return AuthHelper.Account.ACTIVE_SUBSCRIBER;
    }

    @Test(priority = 1, alwaysRun = true, description = "Open Coverage and insights and validate landing structure")
    public void CI_001_OpenModuleAndValidateLandingPage() {
        startExtentCase(
                "CI_001",
                "Open module and validate landing page",
                "Navigate through the exact Hub tile; validate All Stories default Coverage state, search control and live category rail. Top-level switching is exercised independently in CI_006 using semantic-or-relative fallback"
        );

        coverageInsightsPage.validateLandingPage();
        markPassed("CI_001", "Coverage and insights landing/default Coverage state validated");
    }

    @Test(priority = 2, alwaysRun = true, description = "Validate all Coverage categories dynamically")
    public void CI_002_ValidateEveryCoverageCategoryDynamically() {
        startExtentCase(
                "CI_002",
                "Validate every Coverage category dynamically",
                "Discover live '<label>, tab X of Y' semantics, horizontally reveal every runtime category, validate each feed/empty state and exercise bounded vertical scrolling without hardcoded category names"
        );

        coverageInsightsPage.validateEveryCoverageCategoryDynamically();
        markPassed("CI_002", "Every runtime Coverage category validated");
    }

    @Test(priority = 3, alwaysRun = true, description = "Validate Coverage company search control")
    public void CI_003_ValidateCoverageSearchControl() {
        startExtentCase(
                "CI_003",
                "Validate Coverage company search control",
                "Verify the Coverage company-search field using the live android.widget.EditText exposed by UiAutomator2 without entering a hardcoded company name"
        );

        coverageInsightsPage.validateCoverageSearchIsAvailable();
        markPassed("CI_003", "Coverage search control validated");
    }

    @Test(priority = 4, alwaysRun = true, description = "Validate all Insights categories dynamically")
    public void CI_004_ValidateEveryInsightsCategoryDynamically() {
        startExtentCase(
                "CI_004",
                "Validate every Insights category dynamically",
                "Discover the live Insights category count/labels, navigate every horizontal tab and validate live content or explicit empty states with bounded scroll checks"
        );

        coverageInsightsPage.validateEveryInsightsCategoryDynamically();
        markPassed("CI_004", "Every runtime Insights category validated");
    }

    @Test(priority = 5, alwaysRun = true, description = "Validate Reports feed and scrolling")
    public void CI_005_ValidateReportsFeedAndStableScroll() {
        startExtentCase(
                "CI_005",
                "Validate Reports feed and stable scroll",
                "Open Reports, wait through skeleton loading, capture live report semantics and verify bounded vertical-scroll behavior without hardcoding report titles"
        );

        coverageInsightsPage.validateReportsFeedAndStableScroll();
        markPassed("CI_005", "Reports live feed and scrolling validated");
    }

    @Test(priority = 6, alwaysRun = true, description = "Validate top-level Coverage Insights Reports switching")
    public void CI_006_ValidateTopLevelSectionSwitching() {
        startExtentCase(
                "CI_006",
                "Validate top-level section switching",
                "Switch Coverage -> Insights -> Reports -> Coverage and wait for each live section to resolve, guarding against stale content/session state"
        );

        coverageInsightsPage.validateTopLevelSwitching();
        markPassed("CI_006", "Top-level section switching validated");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupCoverageInsightsCase(ITestResult result) {
        Throwable original = result.getThrowable();
        if (accountPrepared && original != null) attachFailureScreenshot();
        Throwable cleanupFailure = null;
        try {
            if (accountPrepared && coverageInsightsPage != null) {
                coverageInsightsPage.recoverToHubSafely();
                accountSession.assertHubVisible();
            }
        } catch (RuntimeException | AssertionError failure) {
            cleanupFailure = failure;
            accountSession.invalidate();
        } finally {
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
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
            ReportLogger.fail("Coverage/Insights cleanup failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void attachFailureScreenshot() {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, currentCaseId + "_before_Hub_reset");
            if (path != null && currentReport != null) {
                currentReport.addScreenCaptureFromPath(path, "Failed state before Hub reset");
            }
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }

    private void startExtentCase(String caseId, String title, String validation) {
        if (caseId.equals(currentCaseId) && currentReport != null) {
            currentReport.info("Validation: " + validation);
            ExtentTestManager.setTest(currentReport);
            return;
        }
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
        currentCaseId = caseId;
        currentReport = test;
        test.info(
                "Module: VRSA Coverage and insights<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Page Build: " + CoverageInsightsPage.BUILD_VERSION + "<br>"
                        + "Data strategy: Live/dynamic; no article/category/company hardcoding<br>"
                        + "Validation: " + validation
        );
        ExtentTestManager.setTest(test);
    }

    private void markPassed(String caseId, String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + caseId + " - " + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + caseId + " - " + message);
    }
}