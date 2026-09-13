package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.SellPlannerPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * VRSA Sell Planner automation.
 *
 * The module uses a clean-state contract before every case. Sell Planner can
 * legitimately resolve either to a populated portfolio or to the empty/import
 * state. Data-dependent sell/preview scenarios are SKIPPED when configured holdings
 * are absent so missing QA data is not misreported as a product failure.
 * Successful cases clean up locally when possible; a retained holding selection is
 * reset through one controlled Back-to-Hub cycle.
 */
public class SellPlannerTest extends BaseTest {

    private static final String PRIMARY_STOCK =
            System.getProperty("sell.primaryStock", "Adani Power");

    private static final String SECONDARY_STOCK =
            System.getProperty("sell.secondaryStock", "Tata Power Company");

    private static final int PRIMARY_PARTIAL_QTY =
            Integer.parseInt(System.getProperty("sell.primaryPartialQty", "10"));

    private static final int SECONDARY_PARTIAL_QTY =
            Integer.parseInt(System.getProperty("sell.secondaryPartialQty", "5"));

    private SellPlannerPage sellPlannerPage;

    @BeforeMethod(alwaysRun = true)
    public void createSellPlannerPage() {
        sellPlannerPage = new SellPlannerPage(driver);
    }

    @Test(
            priority = 1,
            alwaysRun = true,
            description = "Open Sell Planner and validate populated or empty/import landing state"
    )
    public void SP_001_OpenSellPlannerAndValidateLandingPage() {
        startExtentCase(
                "SP_001",
                "Open Sell Planner and validate landing page",
                "Open exact Sell Planner Hub tile and validate common landing anchors plus either the populated holdings state or the legitimate empty/import-stocks state"
        );

        ReportLogger.step("Starting test case: SP_001 - Open Sell Planner and validate landing page");

        sellPlannerPage.openSellPlannerFromHub();
        sellPlannerPage.validateLandingPage();

        markPassed("SP_001", "Sell Planner landing page resolved to a valid clean state");
        safeCleanup();
    }

    @Test(
            priority = 2,
            alwaysRun = true,
            description = "Validate Sell Planner investor selector"
    )
    public void SP_002_ValidateInvestorSelector() {
        startExtentCase(
                "SP_002",
                "Validate investor selector",
                "Open the dynamic investor selector, verify Choose Investor appears and confirm the currently selected investor exists in the list without hardcoding PAN"
        );

        ReportLogger.step("Starting test case: SP_002 - Validate investor selector");

        sellPlannerPage.openSellPlannerFromHub();
        sellPlannerPage.validateInvestorSelector();

        markPassed("SP_002", "Sell Planner investor selector validated successfully");
        safeCleanup();
    }

    @Test(
            priority = 3,
            alwaysRun = true,
            description = "Validate Guidelines for selling stocks content"
    )
    public void SP_003_ValidateGuidelinesForSellingStocks() {
        startExtentCase(
                "SP_003",
                "Validate Guidelines for selling stocks",
                "Open the exact Guidelines ImageView and validate the recommendation/non-recommendation selling-priority content from exposed semantics"
        );

        ReportLogger.step("Starting test case: SP_003 - Validate Guidelines for selling stocks");

        sellPlannerPage.openSellPlannerFromHub();
        sellPlannerPage.validateGuidelinesSheet();

        markPassed("SP_003", "Guidelines for selling stocks validated successfully");
        safeCleanup();
    }

    @Test(
            priority = 4,
            alwaysRun = true,
            description = "Select one holding and validate selected count, worth and Mail CTA"
    )
    public void SP_004_SelectHoldingAndValidateSummary() {
        startExtentCase(
                "SP_004",
                "Select holding and validate summary",
                "Select the primary holding using runtime row geometry; validate selected count, total worth against row amount and Mail my order CTA without UiSelector instance locators"
        );

        ReportLogger.step("Starting test case: SP_004 - Select holding and validate summary");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_004");
        sellPlannerPage.validatePrimaryHoldingSelection(PRIMARY_STOCK);

        markPassed("SP_004", "Holding selection summary validated: " + PRIMARY_STOCK);
        safeCleanup();
    }

    @Test(
            priority = 5,
            alwaysRun = true,
            description = "Set primary holding sell quantity to 1 and validate stepper/summary"
    )
    public void SP_005_PrimarySellOneShare() {
        startExtentCase(
                "SP_005",
                PRIMARY_STOCK + " sell 1",
                "Reveal Shares using bounded W3C horizontal drag, locate the row-specific minus/quantity/plus controls by runtime Y geometry, set sell quantity to 1 and validate selected worth"
        );

        ReportLogger.step("Starting test case: SP_005 - " + PRIMARY_STOCK + " sell 1");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_005");
        sellPlannerPage.validateSingleSellQuantity(PRIMARY_STOCK, 1);

        markPassed("SP_005", PRIMARY_STOCK + " sell quantity 1 validated");
        safeCleanup();
    }

    @Test(
            priority = 6,
            alwaysRun = true,
            description = "Validate partial-sale Holdings preview for the primary stock"
    )
    public void SP_006_PrimaryPartialSalePortfolioPreview() {
        startExtentCase(
                "SP_006",
                PRIMARY_STOCK + " partial-sale Holdings preview",
                "Set runtime sell quantity to configured partial quantity, calculate expected remaining portfolio weights from live holding weights/shares and strictly validate Portfolio Preview Holdings; duplicate/100%-0% corruption must fail as a product defect"
        );

        ReportLogger.step(
                "Starting test case: SP_006 - " + PRIMARY_STOCK
                        + " sell " + PRIMARY_PARTIAL_QTY + " Holdings preview"
        );

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_006");
        sellPlannerPage.validateSingleHoldingPreview(PRIMARY_STOCK, PRIMARY_PARTIAL_QTY);

        markPassed(
                "SP_006",
                PRIMARY_STOCK + " partial-sale Portfolio Preview calculations validated"
        );
        safeCleanup();
    }

    @Test(
            priority = 7,
            alwaysRun = true,
            description = "Validate full sale makes the primary holding net weight 0%"
    )
    public void SP_007_PrimaryFullSalePortfolioPreview() {
        startExtentCase(
                "SP_007",
                PRIMARY_STOCK + " full-sale Holdings preview",
                "Read total shares dynamically, sell the full holding and validate Portfolio Preview Holdings uses 0% net weight for the sold stock while correctly reweighting remaining holdings"
        );

        ReportLogger.step("Starting test case: SP_007 - " + PRIMARY_STOCK + " full sale");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_007");
        sellPlannerPage.validateFullSalePreview(PRIMARY_STOCK);

        markPassed("SP_007", PRIMARY_STOCK + " full-sale Portfolio Preview validated");
        safeCleanup();
    }

    @Test(
            priority = 8,
            alwaysRun = true,
            description = "Validate two partial sales in Holdings preview"
    )
    public void SP_008_PrimaryPartialSecondaryPartialHoldingsPreview() {
        startExtentCase(
                "SP_008",
                "Primary partial + secondary partial Holdings preview",
                "Apply partial sale quantities to both configured holdings and validate every Holdings Current wt./Net wt. dynamically after the combined sale"
        );

        ReportLogger.step(
                "Starting test case: SP_008 - " + PRIMARY_STOCK + " sell " + PRIMARY_PARTIAL_QTY
                        + " + " + SECONDARY_STOCK + " sell " + SECONDARY_PARTIAL_QTY
        );

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_008");
        sellPlannerPage.validateDualHoldingPreview(
                PRIMARY_STOCK,
                PRIMARY_PARTIAL_QTY,
                SECONDARY_STOCK,
                SECONDARY_PARTIAL_QTY
        );

        markPassed("SP_008", "Dual partial-sale Holdings preview validated");
        safeCleanup();
    }

    @Test(
            priority = 9,
            alwaysRun = true,
            description = "Validate primary full + secondary partial same-sector aggregation"
    )
    public void SP_009_PrimaryFullSecondaryPartialSectorPreview() {
        startExtentCase(
                "SP_009",
                "Primary full + secondary partial Sector preview",
                "Sell the primary holding fully and secondary holding partially, then validate the runtime same-sector Current wt./Net wt. aggregation independently of Holdings preview"
        );

        ReportLogger.step("Starting test case: SP_009 - full + partial Sector preview");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_009");
        sellPlannerPage.validateFullPrimaryPartialSecondarySectorPreview(
                PRIMARY_STOCK,
                SECONDARY_STOCK,
                SECONDARY_PARTIAL_QTY
        );

        markPassed("SP_009", "Full + partial Sector preview aggregation validated");
        safeCleanup();
    }

    @Test(
            priority = 10,
            alwaysRun = true,
            description = "Validate full sale of both same-sector holdings drives sector to 0% when no other holding belongs to it"
    )
    public void SP_010_PrimaryFullSecondaryFullSectorPreview() {
        startExtentCase(
                "SP_010",
                "Primary full + secondary full Sector preview",
                "Select both holdings, derive total shares after selection, sell both fully and validate same-sector aggregation independently, including 0% net sector weight when those are the only holdings in that sector"
        );

        ReportLogger.step("Starting test case: SP_010 - full + full Sector preview");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_010");
        sellPlannerPage.validateFullPrimaryFullSecondarySectorPreview(PRIMARY_STOCK, SECONDARY_STOCK);

        markPassed("SP_010", "Full + full Sector preview aggregation validated");
        safeCleanup();
    }

    @Test(
            priority = 11,
            alwaysRun = true,
            description = "Validate Market Cap preview after two partial sales"
    )
    public void SP_011_DualPartialMarketCapPreview() {
        startExtentCase(
                "SP_011",
                "Dual partial-sale Market Cap preview",
                "Apply partial sell quantities to both configured holdings and validate the runtime market-cap bucket Current wt./Net wt. calculation using holding metadata"
        );

        ReportLogger.step("Starting test case: SP_011 - dual partial Market Cap preview");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_011");
        sellPlannerPage.validateDualPartialMarketCapPreview(
                PRIMARY_STOCK,
                PRIMARY_PARTIAL_QTY,
                SECONDARY_STOCK,
                SECONDARY_PARTIAL_QTY
        );

        markPassed("SP_011", "Dual partial-sale Market Cap preview validated");
        safeCleanup();
    }

    @Test(
            priority = 12,
            alwaysRun = true,
            description = "Verify Mail my order CTA becomes available after a valid sell selection without sending an email"
    )
    public void SP_012_ValidateMailMyOrderAvailability() {
        startExtentCase(
                "SP_012",
                "Validate Mail my order availability",
                "Create a valid primary-stock sell selection and verify Mail my order is visible and enabled; this test intentionally does not send mail"
        );

        ReportLogger.step("Starting test case: SP_012 - Validate Mail my order availability");

        sellPlannerPage.openSellPlannerFromHub();
        requireConfiguredHoldingsOrSkip("SP_012");
        sellPlannerPage.validateMailCtaAvailability(PRIMARY_STOCK, PRIMARY_PARTIAL_QTY);

        markPassed("SP_012", "Mail my order CTA availability validated");
        safeCleanup();
    }

    // =========================================================
    // TEST HELPERS
    // =========================================================

    private void requireConfiguredHoldingsOrSkip(String caseId) {
        List<String> missing = sellPlannerPage.missingConfiguredHoldings();
        if (missing.isEmpty()) {
            return;
        }

        String message = caseId
                + " skipped: Sell Planner test-data precondition is not met. "
                + "Missing configured holdings=" + missing
                + ". Current investor is in the empty/import-stocks state or the configured portfolio is incomplete. "
                + "Restore/import the QA portfolio, then rerun this scenario.";

        ReportLogger.info(message);
        safeCleanup();
        throw new SkipException(message);
    }

    private void safeCleanup() {
        if (sellPlannerPage == null) {
            return;
        }

        try {
            sellPlannerPage.recoverToSellPlannerSafely();
        } catch (AssertionError e) {
            ReportLogger.debug(
                    "Sell Planner cleanup assertion ignored so original testcase result is preserved: "
                            + e.getMessage()
            );
        } catch (Exception e) {
            ReportLogger.debug(
                    "Sell Planner cleanup error ignored so original testcase result is preserved: "
                            + e.getMessage()
            );
        }
    }

    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);

        test.info("Module: VRSA Sell Planner<br>"
                + "Case ID: " + caseId + "<br>"
                + "Page Build: " + SellPlannerPage.BUILD_VERSION + "<br>"
                + "Primary Stock: " + PRIMARY_STOCK + "<br>"
                + "Secondary Stock: " + SECONDARY_STOCK + "<br>"
                + "Primary Partial Qty: " + PRIMARY_PARTIAL_QTY + "<br>"
                + "Secondary Partial Qty: " + SECONDARY_PARTIAL_QTY + "<br>"
                + "Validation: " + validation);

        ExtentTestManager.setTest(test);
    }

    private void markPassed(String caseId, String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + caseId + " - " + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + caseId + " - " + message);
    }
}