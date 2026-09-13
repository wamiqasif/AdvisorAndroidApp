package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.InvestmentPlannerPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * VRSA Investment Planner automation.
 *
 * The suite is intentionally independent test-by-test. Failed cases remain on
 * the real failure screen until the listener captures its screenshot; the next
 * case uses InvestmentPlannerPage pre-auth deep-screen recovery before starting
 * the common AuthHelper + Hub flow. Dynamic market data is read at runtime;
 * current stock names/prices/weights are not hardcoded.
 */
public class InvestmentPlannerTest extends BaseTest {

    private InvestmentPlannerPage investmentPlannerPage;

    private static final long DEFAULT_TEST_AMOUNT = Long.parseLong(
            System.getProperty("ip.amount", "50000")
    );

    @BeforeMethod(alwaysRun = true)
    public void setUpInvestmentPlannerPage() {
        investmentPlannerPage = new InvestmentPlannerPage(driver);
    }

    @Test(
            priority = 1,
            alwaysRun = true,
            description = "Open VRSA Investment Planner from Hub and validate landing page"
    )
    public void IP_001_OpenInvestmentPlannerAndValidateLandingPage() {
        startExtentCase(
                "IP_001",
                "Open Investment Planner and validate landing page",
                "Use exact Hub and Investment Planner accessibility IDs; validate built-in portfolios, own-list control, amount field and Generate my order"
        );

        ReportLogger.step("Starting test case: IP_001 - Open Investment Planner and validate landing page");
        ReportLogger.step("Investment Planner page build: " + InvestmentPlannerPage.BUILD_VERSION);

        investmentPlannerPage.openInvestmentPlannerFromHub();
        investmentPlannerPage.validateInvestmentPlannerLandingPage();

        markPassed("IP_001", "Investment Planner landing page validated successfully");
        safeCleanup();
    }

    @Test(
            priority = 2,
            alwaysRun = true,
            description = "Validate built-in All Weather, Long Term and Aggressive portfolio controls"
    )
    public void IP_002_ValidateBuiltInPortfolioSelection() {
        startExtentCase(
                "IP_002",
                "Validate built-in portfolio selection",
                "Select All Weather, Long Term and Aggressive by exact accessibility IDs without coordinates or absolute XPath"
        );

        ReportLogger.step("Starting test case: IP_002 - Validate built-in portfolio selection");

        investmentPlannerPage.openInvestmentPlannerFromHub();
        investmentPlannerPage.selectBuiltInPortfolio("All Weather");
        investmentPlannerPage.selectBuiltInPortfolio("Long Term");
        investmentPlannerPage.selectBuiltInPortfolio("Aggressive");

        markPassed("IP_002", "All built-in portfolio controls are selectable");
        safeCleanup();
    }

    @Test(
            priority = 3,
            alwaysRun = true,
            description = "Generate Long Term portfolio and validate Your Orders dynamically"
    )
    public void IP_003_GenerateLongTermOrderAndValidateAllocation() {
        startExtentCase(
                "IP_003",
                "Generate Long Term order and validate allocation",
                "Generate Long Term order; verify generated portfolio category, Amount invested, dynamic stock rows, weights and values; entered amount is a target/reference and is not a strict maximum"
        );

        ReportLogger.step("Starting test case: IP_003 - Generate Long Term order and validate allocation");

        investmentPlannerPage.generateBuiltInPortfolioOrder("Long Term", DEFAULT_TEST_AMOUNT);
        InvestmentPlannerPage.OrderValidationSummary summary =
                investmentPlannerPage.validateGeneratedOrderTable(DEFAULT_TEST_AMOUNT);

        ReportLogger.info(
                "IP_003 dynamic order summary"
                        + " | rows=" + summary.getCapturedRows()
                        + " | amountInvested=" + summary.getAmountInvested()
                        + " | capturedAllocation=" + summary.getCapturedAllocation()
                        + " | companies=" + summary.getCompanies()
        );

        markPassed("IP_003", "Long Term generated order validated successfully");
        safeCleanup();
    }

    @Test(
            priority = 4,
            alwaysRun = true,
            description = "Generate All Weather portfolio and verify generated category"
    )
    public void IP_004_GenerateAllWeatherOrderAndValidateCategory() {
        startExtentCase(
                "IP_004",
                "Generate All Weather order and validate category",
                "Select All Weather, generate the order and fail if Your Orders shows a different portfolio category; validate dynamic order data without treating entered amount as a strict maximum"
        );

        ReportLogger.step("Starting test case: IP_004 - Generate All Weather order and validate category");

        investmentPlannerPage.generateBuiltInPortfolioOrder("All Weather", DEFAULT_TEST_AMOUNT);
        investmentPlannerPage.validateGeneratedOrderTable(DEFAULT_TEST_AMOUNT);

        markPassed("IP_004", "All Weather generated order category and allocation validated");
        safeCleanup();
    }

    @Test(
            priority = 5,
            alwaysRun = true,
            description = "Generate Aggressive portfolio and verify generated category"
    )
    public void IP_005_GenerateAggressiveOrderAndValidateCategory() {
        startExtentCase(
                "IP_005",
                "Generate Aggressive order and validate category",
                "Select Aggressive, generate the order and fail if Your Orders shows a different portfolio category; validate dynamic order data without treating entered amount as a strict maximum"
        );

        ReportLogger.step("Starting test case: IP_005 - Generate Aggressive order and validate category");

        investmentPlannerPage.generateBuiltInPortfolioOrder("Aggressive", DEFAULT_TEST_AMOUNT);
        investmentPlannerPage.validateGeneratedOrderTable(DEFAULT_TEST_AMOUNT);

        markPassed("IP_005", "Aggressive generated order category and allocation validated");
        safeCleanup();
    }

    @Test(
            priority = 6,
            alwaysRun = true,
            description = "Generate order from Recommendations and validate generated order"
    )
    public void IP_006_GenerateOrderFromRecommendations() {
        String recommendationName = System.getProperty("ip.recommendationName", "All");
        String expectedPortfolio = System.getProperty("ip.recommendationExpectedPortfolio", "");

        startExtentCase(
                "IP_006",
                "Generate order from Recommendations",
                "Open Choose a list, select Recommendation All by exact accessibility ID, generate the order and validate runtime order data. Portfolio Preview is intentionally not part of IP_006."
        );

        ReportLogger.step("Starting test case: IP_006 - Generate order from Recommendations");

        investmentPlannerPage.openInvestmentPlannerFromHub();
        investmentPlannerPage.openOwnListSheetAndValidate();
        investmentPlannerPage.selectRecommendation(recommendationName);
        investmentPlannerPage.enterInvestmentAmount(DEFAULT_TEST_AMOUNT);
        investmentPlannerPage.generateOrderAndWaitForYourOrders();
        investmentPlannerPage.validateGeneratedPortfolioIfConfigured(expectedPortfolio);
        investmentPlannerPage.validateGeneratedOrderTable(DEFAULT_TEST_AMOUNT);

        markPassed(
                "IP_006",
                "Recommendation order validated successfully: " + recommendationName
        );
        safeCleanup();
    }

    @Test(
            priority = 7,
            alwaysRun = true,
            description = "Generate order from Watchlist and validate dynamic order"
    )
    public void IP_007_GenerateOrderFromWatchlist() {
        String preferredWatchlist = System.getProperty("ip.watchlistName", "");
        String expectedPortfolio = System.getProperty("ip.watchlistExpectedPortfolio", "");

        startExtentCase(
                "IP_007",
                "Generate order from Watchlist",
                "Select configured watchlist by exact accessibility ID or first available watchlist dynamically, generate order, validate runtime order data and optionally validate expected portfolio category"
        );

        ReportLogger.step("Starting test case: IP_007 - Generate order from Watchlist");

        investmentPlannerPage.openInvestmentPlannerFromHub();
        investmentPlannerPage.openOwnListSheetAndValidate();
        String selectedWatchlist = investmentPlannerPage.selectWatchlist(preferredWatchlist);
        investmentPlannerPage.enterInvestmentAmount(DEFAULT_TEST_AMOUNT);
        investmentPlannerPage.generateOrderAndWaitForYourOrders();
        investmentPlannerPage.validateGeneratedPortfolioIfConfigured(expectedPortfolio);
        investmentPlannerPage.validateGeneratedOrderTable(DEFAULT_TEST_AMOUNT);

        ReportLogger.info("Watchlist used in IP_007: " + selectedWatchlist);
        markPassed("IP_007", "Watchlist order validated successfully: " + selectedWatchlist);
        safeCleanup();
    }

    @Test(
            priority = 8,
            alwaysRun = true,
            description = "Validate Portfolio Preview Holdings, Sector and Market Cap views"
    )
    public void IP_008_ValidatePortfolioPreviewTabs() {
        startExtentCase(
                "IP_008",
                "Validate Portfolio Preview tabs",
                "Generate Long Term order, open the clickable Portfolio preview link and verify Holdings, Sector and Market Cap render meaningful percentage allocation data"
        );

        ReportLogger.step("Starting test case: IP_008 - Validate Portfolio Preview tabs");

        investmentPlannerPage.generateBuiltInPortfolioOrder("Long Term", DEFAULT_TEST_AMOUNT);
        investmentPlannerPage.openPortfolioPreviewFromYourOrders();
        investmentPlannerPage.validatePortfolioPreviewAllTabs();

        markPassed("IP_008", "Portfolio Preview Holdings, Sector and Market Cap validated");
        safeCleanup();
    }

    @Test(
            priority = 9,
            alwaysRun = true,
            description = "Verify Mail my order success confirmation"
    )
    public void IP_009_VerifyMailMyOrderSuccess() {
        startExtentCase(
                "IP_009",
                "Verify Mail my order success",
                "Generate Long Term order, tap exact Mail my order control and validate Order mailed successfully banner"
        );

        ReportLogger.step("Starting test case: IP_009 - Verify Mail my order success");

        investmentPlannerPage.generateBuiltInPortfolioOrder("Long Term", DEFAULT_TEST_AMOUNT);

        // IP_009 validates Mail my order only. Full allocation-table validation is
        // already covered by IP_003/IP_004/IP_005 and must not block this testcase.
        investmentPlannerPage.mailMyOrderAndValidateSuccess();

        markPassed("IP_009", "Mail my order success confirmation validated");
        safeCleanup();
    }

    // =========================================================
    // TEST HELPERS
    // =========================================================

    private void safeCleanup() {
        if (investmentPlannerPage == null) {
            return;
        }

        try {
            investmentPlannerPage.recoverToInvestmentPlannerSafely();
        } catch (AssertionError e) {
            ReportLogger.debug(
                    "Investment Planner cleanup assertion ignored so original testcase result is preserved: "
                            + e.getMessage()
            );
        } catch (Exception e) {
            ReportLogger.debug(
                    "Investment Planner cleanup error ignored so original testcase result is preserved: "
                            + e.getMessage()
            );
        }
    }

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: VRSA Investment Planner<br>"
                + "Case ID: " + caseId + "<br>"
                + "Page Build: " + InvestmentPlannerPage.BUILD_VERSION + "<br>"
                + "Validation: " + validation + "<br>"
                + "Test Amount: ₹" + DEFAULT_TEST_AMOUNT);

        ExtentTestManager.setTest(test);
    }

    private void markPassed(String caseId, String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + caseId + " - " + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + caseId + " - " + message);
    }
}