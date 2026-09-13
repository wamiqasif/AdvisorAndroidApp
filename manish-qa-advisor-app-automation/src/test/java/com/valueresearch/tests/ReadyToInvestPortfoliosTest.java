package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.ReadyToInvestPortfoliosPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Dynamic regression for Hub -> Stocks -> Ready to invest portfolios -> Analyst Picks.
 *
 * The test class intentionally keeps live stock names, dates, prices, returns and
 * secondary filter lists out of the test code. The page object discovers them at runtime.
 */
public class ReadyToInvestPortfoliosTest extends BaseTest {

    private ReadyToInvestPortfoliosPage readyPage;

    @BeforeMethod(alwaysRun = true)
    public void createReadyToInvestPage() {
        readyPage = new ReadyToInvestPortfoliosPage(driver);
    }

    @Test(priority = 1, alwaysRun = true, description = "Open Ready to invest portfolios and validate dynamic Analyst Picks landing")
    public void RTI_001_OpenModuleAndValidateLanding() {
        startExtentCase(
                "RTI_001",
                "Open module and validate dynamic landing",
                "Open the exact Hub tile, validate Analyst Picks, discover the live top-tab group and capture the current secondary filters/table data without hardcoded stocks"
        );

        readyPage.validateLandingPageDynamically();
        markPassed("RTI_001", "Analyst Picks landing validated dynamically");
    }

    @Test(priority = 2, alwaysRun = true, description = "Validate all runtime top tabs and secondary filters")
    public void RTI_002_ValidateAllTabsAndFiltersDynamically() {
        startExtentCase(
                "RTI_002",
                "Validate all top tabs and secondary filters dynamically",
                "Discover every '<label>, tab X of Y' top tab at runtime, discover each tab's visible secondary chips from geometry, and validate live table/empty-state data for every state"
        );

        readyPage.validateEveryTopSectionAndSecondaryFilterDynamically();
        markPassed("RTI_002", "Every runtime top tab and secondary filter validated");
    }

    @Test(priority = 3, alwaysRun = true, description = "Validate horizontal Analyst Picks table scrolling")
    public void RTI_003_ValidateHorizontalTableScroll() {
        startExtentCase(
                "RTI_003",
                "Validate horizontal table scroll",
                "Use the live table HorizontalScrollView as the gesture anchor, verify horizontal progression when semantics expose shifted columns, and restore the starting position"
        );

        readyPage.validateHorizontalTableScroll();
        markPassed("RTI_003", "Horizontal table scrolling validated and restored");
    }

    @Test(priority = 4, alwaysRun = true, description = "Validate bounded vertical scrolling and Portfolio Planner CTA")
    public void RTI_004_ValidateVerticalScrollAndPortfolioPlannerCta() {
        startExtentCase(
                "RTI_004",
                "Validate vertical scroll and Portfolio Planner CTA",
                "Open the last runtime top tab, use bounded vertical scrolling with progress detection, find the live Portfolio Planner CTA, validate Investment Planner navigation, then return without refreshing Hub"
        );

        readyPage.validateVerticalScrollAndPortfolioPlannerCta();
        markPassed("RTI_004", "Vertical scrolling and Portfolio Planner CTA validated");
    }

    @Test(priority = 5, alwaysRun = true, description = "Validate fast top-level Analyst Picks switching")
    public void RTI_005_ValidateTopLevelSwitchingFast() {
        startExtentCase(
                "RTI_005",
                "Validate top-level switching",
                "Switch through every runtime-discovered top tab once, wait for each live table/empty state to resolve, and return to the first tab without repeating deep filter validation"
        );

        readyPage.validateTopLevelSwitchingFast();
        markPassed("RTI_005", "Top-level switching validated");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupReadyToInvestState() {
        // Always restore a clean Hub state, even when the testcase throws before
        // reaching its PASS marker. This prevents one failed/scrolled test from
        // contaminating the next testcase while preserving the Hub scroll position.
        safeCleanup();
    }

    private void safeCleanup() {
        if (readyPage == null) {
            return;
        }
        try {
            readyPage.recoverToHubSafely();
        } catch (Exception e) {
            ReportLogger.debug(
                    "Ready-to-invest cleanup ignored so testcase result is preserved: " + e.getMessage()
            );
        } catch (AssertionError e) {
            ReportLogger.debug(
                    "Ready-to-invest cleanup assertion ignored so testcase result is preserved: " + e.getMessage()
            );
        }
    }

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
        test.info(
                "Module: VRSA Ready to invest portfolios / Analyst Picks<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Page Build: " + ReadyToInvestPortfoliosPage.BUILD_VERSION + "<br>"
                        + "Data strategy: Live/dynamic; no stock/date/price/return/secondary-filter hardcoding<br>"
                        + "Navigation strategy: Preserve Hub state; skip redundant Hub tap/refresh<br>"
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