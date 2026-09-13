package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.CoverageInsightsPage;
import com.valueresearch.utils.ExtentManager;
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

    @BeforeMethod(alwaysRun = true)
    public void createCoverageInsightsPage() {
        coverageInsightsPage = new CoverageInsightsPage(driver);
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
        safeCleanup();
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
        safeCleanup();
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
        safeCleanup();
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
        safeCleanup();
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
        safeCleanup();
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
        safeCleanup();
    }

    private void safeCleanup() {
        if (coverageInsightsPage == null) {
            return;
        }
        try {
            coverageInsightsPage.recoverToHubSafely();
        } catch (Exception e) {
            ReportLogger.debug("Coverage/Insights cleanup ignored so testcase result is preserved: " + e.getMessage());
        }
    }

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
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