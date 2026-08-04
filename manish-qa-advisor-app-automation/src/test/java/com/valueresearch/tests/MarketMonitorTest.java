package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.MarketMonitorPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MarketMonitorTest extends BaseTest {

    private MarketMonitorPage marketMonitorPage;

    @BeforeMethod(alwaysRun = true)
    public void setUpMarketMonitorPage() {
        marketMonitorPage = new MarketMonitorPage(driver);
    }

    @Test(priority = 1, alwaysRun = true, description = "Open Market Monitor from Stocks bottom tab")
    public void MM_001_OpenMarketMonitorFromStocks() {
        startExtentCase(
                "MM_001",
                "Open Market Monitor from Stocks",
                "Confirm login, open the Stocks bottom tab, and verify Market Monitor loads"
        );

        ReportLogger.step("Starting test case: MM_001 - Open Market Monitor from Stocks");
        ReportLogger.step("Loaded Market Monitor page build: " + MarketMonitorPage.BUILD_VERSION);
        ReportLogger.step("Checking Advisor login/session");

        AuthHelper authHelper = new AuthHelper(driver);
        authHelper.ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        marketMonitorPage.openMarketMonitorFromStocks();

        ReportLogger.pass("MM_001 - Market Monitor opened successfully");
        markExtentCasePassed("MM_001", "Open Market Monitor from Stocks");
    }

    @Test(
            priority = 2,
            alwaysRun = true,
            description = "Verify Market Monitor controls and initial table"
    )
    public void MM_002_VerifyMarketMonitorOverview() {
        startExtentCase(
                "MM_002",
                "Verify Market Monitor overview",
                "Validate Quick Search, market filters, period tabs, dynamic headers, index rows and numeric returns"
        );

        ReportLogger.step("Starting test case: MM_002 - Verify Market Monitor overview");
        marketMonitorPage.verifyMarketMonitorOverview();

        ReportLogger.pass("MM_002 - Market Monitor overview validated successfully");
        markExtentCasePassed("MM_002", "Verify Market Monitor overview");
    }

    @Test(
            priority = 3,
            alwaysRun = true,
            description = "Verify All market filter"
    )
    public void MM_003_VerifyAllMarketFilter() {
        startExtentCase(
                "MM_003",
                "Verify All market filter",
                "Tap All and validate that the dynamic Market Monitor table remains structurally valid"
        );

        ReportLogger.step("Starting test case: MM_003 - Verify All market filter");
        marketMonitorPage.verifyAllMarketFilter();

        ReportLogger.pass("MM_003 - All market filter validated successfully");
        markExtentCasePassed("MM_003", "Verify All market filter");
    }

    @Test(
            priority = 4,
            alwaysRun = true,
            description = "Verify Indian market filter"
    )
    public void MM_004_VerifyIndianMarketFilter() {
        startExtentCase(
                "MM_004",
                "Verify Indian market filter",
                "Tap Indian and validate live index rows and numeric return data without hardcoded values"
        );

        ReportLogger.step("Starting test case: MM_004 - Verify Indian market filter");
        marketMonitorPage.verifyIndianMarketFilter();

        ReportLogger.pass("MM_004 - Indian market filter validated successfully");
        markExtentCasePassed("MM_004", "Verify Indian market filter");
    }

    @Test(
            priority = 5,
            alwaysRun = true,
            description = "Verify Global market filter"
    )
    public void MM_005_VerifyGlobalMarketFilter() {
        startExtentCase(
                "MM_005",
                "Verify Global market filter",
                "Tap Global and validate live index rows and numeric return data without hardcoded values"
        );

        ReportLogger.step("Starting test case: MM_005 - Verify Global market filter");
        marketMonitorPage.verifyGlobalMarketFilter();

        ReportLogger.pass("MM_005 - Global market filter validated successfully");
        markExtentCasePassed("MM_005", "Verify Global market filter");
    }

    @Test(
            priority = 6,
            alwaysRun = true,
            description = "Verify Trailing period tab"
    )
    public void MM_006_VerifyTrailingPeriod() {
        startExtentCase(
                "MM_006",
                "Verify Trailing period tab",
                "Tap Trailing and validate trailing headers plus dynamic rows and return values"
        );

        ReportLogger.step("Starting test case: MM_006 - Verify Trailing period tab");
        marketMonitorPage.verifyTrailingPeriod();

        ReportLogger.pass("MM_006 - Trailing period validated successfully");
        markExtentCasePassed("MM_006", "Verify Trailing period tab");
    }

    @Test(
            priority = 7,
            alwaysRun = true,
            description = "Verify Annual period tab"
    )
    public void MM_007_VerifyAnnualPeriod() {
        startExtentCase(
                "MM_007",
                "Verify Annual period tab",
                "Tap Annual and validate dynamic annual table headers, index rows and return values"
        );

        ReportLogger.step("Starting test case: MM_007 - Verify Annual period tab");
        marketMonitorPage.verifyAnnualPeriod();

        ReportLogger.pass("MM_007 - Annual period validated successfully");
        markExtentCasePassed("MM_007", "Verify Annual period tab");
    }

    @Test(
            priority = 8,
            alwaysRun = true,
            description = "Verify Quarterly period tab"
    )
    public void MM_008_VerifyQuarterlyPeriod() {
        startExtentCase(
                "MM_008",
                "Verify Quarterly period tab",
                "Tap Quarterly and validate dynamic quarter/date headers, index rows and return values"
        );

        ReportLogger.step("Starting test case: MM_008 - Verify Quarterly period tab");
        marketMonitorPage.verifyQuarterlyPeriod();

        ReportLogger.pass("MM_008 - Quarterly period validated successfully");
        markExtentCasePassed("MM_008", "Verify Quarterly period tab");
    }

    @Test(
            priority = 9,
            alwaysRun = true,
            description = "Verify Monthly period tab"
    )
    public void MM_009_VerifyMonthlyPeriod() {
        startExtentCase(
                "MM_009",
                "Verify Monthly period tab",
                "Tap Monthly and validate dynamic month/date headers, index rows and return values"
        );

        ReportLogger.step("Starting test case: MM_009 - Verify Monthly period tab");
        marketMonitorPage.verifyMonthlyPeriod();

        ReportLogger.pass("MM_009 - Monthly period validated successfully");
        markExtentCasePassed("MM_009", "Verify Monthly period tab");
    }

    @Test(
            priority = 10,
            alwaysRun = true,
            description = "Verify Market Monitor vertical table scroll"
    )
    public void MM_010_VerifyMarketMonitorVerticalScroll() {
        startExtentCase(
                "MM_010",
                "Verify vertical table scroll",
                "Swipe vertically inside Market Monitor and verify that additional/different index rows become visible"
        );

        ReportLogger.step("Starting test case: MM_010 - Verify vertical table scroll");
        marketMonitorPage.verifyMarketMonitorVerticalScroll();

        ReportLogger.pass("MM_010 - Vertical table scrolling validated successfully");
        markExtentCasePassed("MM_010", "Verify vertical table scroll");
    }

    @Test(
            priority = 11,
            alwaysRun = true,
            description = "Verify Market Monitor horizontal table scroll"
    )
    public void MM_011_VerifyMarketMonitorHorizontalScroll() {
        startExtentCase(
                "MM_011",
                "Verify horizontal table scroll",
                "Swipe horizontally inside the Market Monitor table and verify that columns or visible values change"
        );

        ReportLogger.step("Starting test case: MM_011 - Verify horizontal table scroll");
        marketMonitorPage.verifyMarketMonitorHorizontalScroll();

        ReportLogger.pass("MM_011 - Horizontal table scrolling validated successfully");
        markExtentCasePassed("MM_011", "Verify horizontal table scroll");
    }

    @Test(
            priority = 12,
            alwaysRun = true,
            description = "Verify all Market Monitor filter and period combinations"
    )
    public void MM_012_VerifyAllMarketAndPeriodCombinations() {
        startExtentCase(
                "MM_012",
                "Verify all filter/period combinations",
                "Validate All, Indian and Global against Trailing, Annual, Quarterly and Monthly using dynamic live data"
        );

        ReportLogger.step("Starting test case: MM_012 - Verify all filter/period combinations");
        marketMonitorPage.verifyAllMarketAndPeriodCombinations();

        ReportLogger.pass("MM_012 - All filter/period combinations validated successfully");
        markExtentCasePassed("MM_012", "Verify all filter/period combinations");
    }

    // =========================================================
    // EXTENT REPORT HELPERS
    // =========================================================

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTestManager.setTest(
                ExtentManager.getExtentReports().createTest(caseId + " - " + title)
        );

        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> Market Monitor<br>"
                        + "<b>Case ID:</b> " + caseId + "<br>"
                        + "<b>Scenario:</b> " + title + "<br>"
                        + "<b>Validation:</b> " + validation
        );
    }

    private void markExtentCasePassed(String caseId, String title) {
        try {
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().pass(
                        "<span class='badge white-text green'>"
                                + caseId + " - " + title
                                + " completed successfully</span>"
                );
            }
        } catch (Exception ignored) {
            // Reporting must never affect test execution.
        }
    }
}