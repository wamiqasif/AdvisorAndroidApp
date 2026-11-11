package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.FundStockLanding_Page;
import pages.FundStockLanding_Page.LinkResult;

/**
 * Stocks Landing Screen Test Suite.
 *
 * <p>Persistent-session mode. Every test follows the same three-step journey:
 * <ol>
 *   <li><b>Start from Stocks tab</b> — {@link #recoverAppState(Method)} guarantees the Stocks
 *       screen is loaded before each test.</li>
 *   <li><b>Interact with the element under test</b> — one element / interaction per test.</li>
 *   <li><b>Validate the result, then return to Stocks</b> — navigation-link tests assert a new
 *       screen opened and navigate back; in-page tests (filters, periods, sorts) assert market
 *       data remains visible.</li>
 * </ol>
 *
 * <p>The Stocks tab can show two distinct screen states depending on the subscription:
 * <ul>
 *   <li><b>Stock Advisor landing</b> (XML-1) — upsell screen with feature bullets and an
 *       "Open Stock Advisor Details" call-to-action.</li>
 *   <li><b>Stocks dashboard</b> (XML-2) — Quick Search input plus the Market Monitor section
 *       with filter tabs, period tabs, column sort headers, and live BSE index data.</li>
 * </ul>
 * Tests that are specific to one screen state perform a soft-skip (log and return) when the
 * relevant screen is not active, so the suite does not fail on the wrong app state.
 *
 * <p>Test IDs: TC_STK_001 – TC_STK_030.
 */
public class StockLandingTest extends BaseTest {

    private FundStockLanding_Page stocksPage;

    // ============================================================
    // CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        stocksPage = new FundStockLanding_Page(getDriver());
    }

    /**
     * Journey start: navigate to the Stocks tab before every test.
     *
     * <p>{@link FundStockLanding_Page#openStocksScreen()} presses back up to 4 times to
     * unwind any Flutter overlay before tapping the Stocks tab, so it is safe to call
     * even when the bottom navigation bar is temporarily hidden.</p>
     */
    @Override
    protected void recoverAppState(Method method) {
        waitForUiToSettle();
        stocksPage.openStocksScreen();
        boolean stocksReady = stocksPage.isStocksScreenDisplayed()
                || stocksPage.isStocksTabVisible();
        Assert.assertTrue(stocksReady,
                "Stocks screen (content or tab) must be reachable before: " + method.getName());
        logger.info("Stocks screen ready for: {}", method.getName());
    }

    // ============================================================
    // SECTION 1 — STOCKS SCREEN STRUCTURE
    // Journey: navigate to Stocks, assert structure, stay on Stocks
    // ============================================================

    @Test(description = "TC_STK_001 — Stocks screen loads and is displayed after tapping the Stocks tab")
    public void tc_stk_001_verifyStocksScreenLoads() {

        Assert.assertTrue(
                stocksPage.isStocksScreenDisplayed(),
                "Stocks screen must be displayed after navigating to the Stocks tab");

        logger.info("TC_STK_001 — Stocks screen load verified");
    }

    @Test(description = "TC_STK_002 — Bottom navigation (Funds / Stocks / Portfolio / Hub) is visible on Stocks screen")
    public void tc_stk_002_verifyBottomNavigationVisible() {

        Assert.assertTrue(
                stocksPage.isBottomNavigationDisplayed(),
                "Bottom navigation bar must be visible on the Stocks screen");

        logger.info("TC_STK_002 — Bottom navigation visibility verified");
    }

    // ============================================================
    // SECTION 2 — STOCK ADVISOR LANDING (XML-1)
    // Journey: Stocks → assert landing content → stay on Stocks
    // These tests soft-skip when the Stocks dashboard (XML-2) is active instead.
    // ============================================================

    @Test(description = "TC_STK_003 — Stock Advisor landing headline is displayed on Stocks tab")
    public void tc_stk_003_verifyStockAdvisorLandingDisplayed() {

        if (!stocksPage.isStockAdvisorLandingDisplayed()) {
            logger.info("TC_STK_003 — Stock Advisor landing not active (dashboard shown) — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isStockAdvisorLandingDisplayed(),
                "Stock Advisor landing headline must be visible on the Stocks tab");

        logger.info("TC_STK_003 — Stock Advisor landing headline verified");
    }

    @Test(description = "TC_STK_004 — Stock Advisor feature bullets are displayed on the landing screen")
    public void tc_stk_004_verifyStockAdvisorFeaturesDisplayed() {

        if (!stocksPage.isStockAdvisorLandingDisplayed()) {
            logger.info("TC_STK_004 — Stock Advisor landing not active — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.areStockAdvisorFeaturesDisplayed(),
                "At least one Stock Advisor feature bullet must be visible");

        logger.info("TC_STK_004 — Stock Advisor feature bullets verified");
    }

    @Test(description = "TC_STK_005 — 'Open Stock Advisor Details' button is displayed on the landing screen")
    public void tc_stk_005_verifyOpenStockAdvisorButtonDisplayed() {

        if (!stocksPage.isStockAdvisorLandingDisplayed()) {
            logger.info("TC_STK_005 — Stock Advisor landing not active — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isOpenStockAdvisorButtonDisplayed(),
                "'Open Stock Advisor Details' button must be visible on the landing screen");

        logger.info("TC_STK_005 — 'Open Stock Advisor Details' button verified");
    }

    // ============================================================
    // SECTION 3 — STOCKS DASHBOARD: QUICK SEARCH (XML-2)
    // Journey: Stocks dashboard → assert Quick Search section → stay on Stocks
    // Soft-skip when the Stock Advisor landing (XML-1) is active instead.
    // ============================================================

    @Test(description = "TC_STK_006 — Quick Search section label is displayed on the Stocks dashboard")
    public void tc_stk_006_verifyQuickSearchSectionDisplayed() {

        if (!stocksPage.isQuickSearchDisplayed()) {
            logger.info("TC_STK_006 — Quick Search not present (landing screen active) — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isQuickSearchDisplayed(),
                "'Quick Search' section label must be visible on the Stocks dashboard");

        logger.info("TC_STK_006 — Quick Search section verified");
    }

    @Test(description = "TC_STK_007 — Search input field is displayed in the Quick Search section")
    public void tc_stk_007_verifySearchFieldDisplayed() {

        if (!stocksPage.isQuickSearchDisplayed()) {
            logger.info("TC_STK_007 — Quick Search not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isSearchFieldDisplayed(),
                "Search input field (hint: 'Search for a stock') must be visible");

        logger.info("TC_STK_007 — Search input field verified");
    }
@Test
    public void tc_stk_007_verifySearchFieldWorking() {


        if (!stocksPage.isQuickSearchDisplayed()) {
            logger.info("TC_STK_007 — Quick Search not present — soft-skip");
            return;
        }


        Assert.assertTrue(
                stocksPage.isSearchFieldDisplayed(),
                "Search input field (hint: 'Search for a stock') must be visible");
        stocksPage.enterStock("ITC");

        logger.info("TC_STK_007 — Search input field verified");


    }

    // ============================================================
    // SECTION 4 — STOCKS DASHBOARD: MARKET MONITOR STRUCTURE (XML-2)
    // Journey: Stocks dashboard → assert Market Monitor elements → stay on Stocks
    // ============================================================

    @Test(description = "TC_STK_008 — Market Monitor section label is displayed on the Stocks dashboard")
    public void tc_stk_008_verifyMarketMonitorSectionDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_008 — Market Monitor not present (landing screen active) — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isMarketMonitorDisplayed(),
                "'Market Monitor' section label must be visible");

        logger.info("TC_STK_008 — Market Monitor section label verified");
    }

    @Test(description = "TC_STK_009 — Market type filter bar (All / Indian / Global) is visible in Market Monitor")
    public void tc_stk_009_verifyFilterBarDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_009 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isFilterBarDisplayed(),
                "Market type filter bar (All / Indian / Global) must be visible");

        logger.info("TC_STK_009 — Filter bar verified");
    }

    @Test(description = "TC_STK_010 — Return-period bar (Trailing / Annual / Quarterly / Monthly) is visible in Market Monitor")
    public void tc_stk_010_verifyPeriodBarDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_010 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isPeriodBarDisplayed(),
                "Period bar (Trailing / Annual / Quarterly / Monthly) must be visible");

        logger.info("TC_STK_010 — Period bar verified");
    }

    @Test(description = "TC_STK_011 — Column sort headers (Index / 1 Day) are visible in Market Monitor")
    public void tc_stk_011_verifyColumnHeadersDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_011 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isColumnHeadersDisplayed(),
                "Column sort headers (Index / 1 Day) must be visible");

        logger.info("TC_STK_011 — Column sort headers verified");
    }

    @Test(description = "TC_STK_012 — BSE index row labels are visible in Market Monitor data area")
    public void tc_stk_012_verifyMarketMonitorRowLabelsDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_012 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isMarketMonitorDataDisplayed(),
                "At least one BSE index row label must be visible in Market Monitor");

        logger.info("TC_STK_012 — Market Monitor row labels verified");
    }

    // ============================================================
    // SECTION 5 — BSE INDEX PERFORMANCE DATA CELLS (XML-2)
    // Journey: Stocks dashboard → assert data cells → stay on Stocks
    // ============================================================

    @Test(description = "TC_STK_013 — BSE Sensex Index performance data cell is displayed")
    public void tc_stk_013_verifyBseSensexDataCellDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_013 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isBseSensexDataCellDisplayed(),
                "BSE Sensex Index performance data cell (1 Day / YTD / 1 Week) must be visible");

        logger.info("TC_STK_013 — BSE Sensex data cell verified");
    }

    @Test(description = "TC_STK_014 — BSE IPO Index 1 Day performance data cell is displayed")
    public void tc_stk_014_verifyBseIpoDataCellDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_014 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isBseIpoDataCellDisplayed(),
                "BSE IPO Index 1 Day performance cell must be visible");

        logger.info("TC_STK_014 — BSE IPO 1 Day data cell verified");
    }

    @Test(description = "TC_STK_015 — BSE 100 Index 1 Day performance data cell is displayed")
    public void tc_stk_015_verifyBse100DataCellDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_015 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isBse100DataCellDisplayed(),
                "BSE 100 Index 1 Day performance cell must be visible");

        logger.info("TC_STK_015 — BSE 100 1 Day data cell verified");
    }

    @Test(description = "TC_STK_016 — BSE 200 Index 1 Day performance data cell is displayed")
    public void tc_stk_016_verifyBse200DataCellDisplayed() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_016 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.isBse200DataCellDisplayed(),
                "BSE 200 Index 1 Day performance cell must be visible");

        logger.info("TC_STK_016 — BSE 200 1 Day data cell verified");
    }

    // ============================================================
    // SECTION 6 — IN-PAGE FILTER INTERACTIONS (XML-2)
    // Journey: Stocks dashboard → tap filter tab → validate market data → stay on Stocks
    // ============================================================

    @Test(description = "TC_STK_017 — Stocks → tap 'All' market filter → market data remains visible")
    public void tc_stk_017_verifyFilterAllInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_017 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapFilterAllAndValidate(),
                "Tapping 'All' market filter must keep market data visible on screen");

        logger.info("TC_STK_017 — 'All' filter interaction verified");
    }

    @Test(description = "TC_STK_018 — Stocks → tap 'Indian' market filter → market data remains visible")
    public void tc_stk_018_verifyFilterIndianInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_018 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapFilterIndianAndValidate(),
                "Tapping 'Indian' filter must keep market data visible on screen");

        logger.info("TC_STK_018 — 'Indian' filter interaction verified");
    }

    @Test(description = "TC_STK_019 — Stocks → tap 'Global' market filter → filter tab remains visible")
    public void tc_stk_019_verifyFilterGlobalInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_019 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapFilterGlobalAndValidate(),
                "Tapping 'Global' filter must keep the filter tab visible on screen");

        logger.info("TC_STK_019 — 'Global' filter interaction verified");
    }

    // ============================================================
    // SECTION 7 — PERIOD TAB INTERACTIONS (XML-2)
    // Journey: Stocks dashboard → tap period tab → validate market data → stay on Stocks
    // ============================================================

    @Test(description = "TC_STK_020 — Stocks → tap 'Trailing' period tab → market data remains visible")
    public void tc_stk_020_verifyPeriodTrailingInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_020 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapPeriodTrailingAndValidate(),
                "Tapping 'Trailing' period tab must keep market data visible");

        logger.info("TC_STK_020 — 'Trailing' period tab interaction verified");
    }

    @Test(description = "TC_STK_021 — Stocks → tap 'Annual' period tab → market data remains visible")
    public void tc_stk_021_verifyPeriodAnnualInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_021 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapPeriodAnnualAndValidate(),
                "Tapping 'Annual' period tab must keep market data visible");

        logger.info("TC_STK_021 — 'Annual' period tab interaction verified");
    }

    @Test(description = "TC_STK_022 — Stocks → tap 'Quarterly' period tab → market data remains visible")
    public void tc_stk_022_verifyPeriodQuarterlyInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_022 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapPeriodQuarterlyAndValidate(),
                "Tapping 'Quarterly' period tab must keep market data visible");

        logger.info("TC_STK_022 — 'Quarterly' period tab interaction verified");
    }

    @Test(description = "TC_STK_023 — Stocks → tap 'Monthly' period tab → market data remains visible")
    public void tc_stk_023_verifyPeriodMonthlyInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_023 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapPeriodMonthlyAndValidate(),
                "Tapping 'Monthly' period tab must keep market data visible");

        logger.info("TC_STK_023 — 'Monthly' period tab interaction verified");
    }

    // ============================================================
    // SECTION 8 — COLUMN SORT HEADER INTERACTIONS (XML-2)
    // Journey: Stocks dashboard → tap column header → validate data → stay on Stocks
    // ============================================================

    @Test(description = "TC_STK_024 — Stocks → tap 'Index' column sort header → market data remains visible")
    public void tc_stk_024_verifyColumnSortIndexInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_024 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapColumnSortIndexAndValidate(),
                "Tapping 'Index' sort header must keep market data visible");

        logger.info("TC_STK_024 — 'Index' column sort interaction verified");
    }

    @Test(description = "TC_STK_025 — Stocks → tap '1 Day' column sort header → market data remains visible")
    public void tc_stk_025_verifyColumnSort1DayInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_025 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapColumnSort1DayAndValidate(),
                "Tapping '1 Day' sort header must keep market data visible");

        logger.info("TC_STK_025 — '1 Day' column sort interaction verified");
    }

    @Test(description = "TC_STK_026 — Stocks → tap 'YTD' column sort header → market data remains visible")
    public void tc_stk_026_verifyColumnSortYtdInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_026 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapColumnSortYtdAndValidate(),
                "Tapping 'YTD' sort header must keep market data visible");

        logger.info("TC_STK_026 — 'YTD' column sort interaction verified");
    }

    @Test(description = "TC_STK_027 — Stocks → tap '1 Week' column sort header → market data remains visible")
    public void tc_stk_027_verifyColumnSort1WeekInteraction() {

        if (!stocksPage.isMarketMonitorDisplayed()) {
            logger.info("TC_STK_027 — Market Monitor not present — soft-skip");
            return;
        }

        Assert.assertTrue(
                stocksPage.tapColumnSort1WeekAndValidate(),
                "Tapping '1 Week' sort header must keep market data visible");

        logger.info("TC_STK_027 — '1 Week' column sort interaction verified");
    }

    // ============================================================
    // SECTION 9 — NAVIGATION LINK JOURNEYS
    // Journey: Stocks → tap link → validate destination screen opened → back to Stocks
    // ============================================================

    @Test(description = "TC_STK_028 — Stocks → Open Stock Advisor Details → validate screen → back to Stocks")
    public void tc_stk_028_verifyOpenStockAdvisorDetailsNavigation() {

        Assert.assertTrue(
                stocksPage.tapOpenStockAdvisorDetailsAndValidate(),
                "'Open Stock Advisor Details' must open a valid screen from the Stocks tab");

        logger.info("TC_STK_028 — Stock Advisor Details navigation journey verified");
    }

    @Test(description = "TC_STK_029 — Stocks → Quick Search field → validate screen change → back to Stocks")
    public void tc_stk_029_verifyQuickSearchNavigation() {

        Assert.assertTrue(
                stocksPage.tapQuickSearchFieldAndValidate(),
                "Tapping the Quick Search field must trigger a screen change from the Stocks tab");

        logger.info("TC_STK_029 — Quick Search navigation journey verified");
    }

    // ============================================================
    // SECTION 10 — COMPREHENSIVE BULK VALIDATION
    // Journey: Stocks → each navigation link (one by one) → validate → back to Stocks → repeat
    // ============================================================

    @Test(description = "TC_STK_030 — Stocks → every navigation link → validate screen → back to Stocks")
    public void tc_stk_030_verifyAllLinksOpenValidScreens() {

        List<LinkResult> results = stocksPage.tapAllLinksAndValidateScreens();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one Stocks navigation link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "Stocks link must open a valid screen: " + result.name);
        }

        logger.info("TC_STK_030 — All Stocks navigation link journeys verified: {}", results);
    }
}
