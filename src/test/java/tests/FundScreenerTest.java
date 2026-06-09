package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;
import pages.FundScreener_Page;
import pages.FundScreener_Page.LinkResult;

/**
 * Fund Screener Test Suite.
 *
 * Journey for every test (set in recoverAppState):
 *   recover to dashboard -> tap Hub tab -> open Fund Screener section.
 *
 * Persistent-session mode (driver shared across methods, like AboutUsTest).
 *
 * PRD coverage (Premium presets: Top Rated Funds, Rs 100 SIP Funds, Funds for
 * first-time investors; plus the broader preset catalogue and Categories tab):
 *  - Structure / navigation of every preset card -> its filtered fund list, and
 *    the presence of the Filters + Custom-columns controls each preset defines,
 *    is covered here (explicit card tests + the recursive link crawl).
 *  - The PRD's per-preset FILTER VALUES, CUSTOM-COLUMN sets and the decimal-place
 *    display rules are data-level assertions that the current FundScreener_Page
 *    does not expose methods for, so they are not asserted here (see notes at the
 *    end of this file). They would require additional page locators/methods.
 */
public class FundScreenerTest extends BaseTest {

    private FundScreener_Page fundScreenerPage;
    private DashboardPage     dashboardPage;

    // ============================================================
    // CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        fundScreenerPage = new FundScreener_Page(getDriver());
        dashboardPage     = new DashboardPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        dashboardPage.tapHubTab();
        fundScreenerPage.openFundScreener();

        Assert.assertTrue(
                fundScreenerPage.isFundScreenerDisplayed(),
                "Fund Screener screen must load before " + method.getName());

        logger.info("Fund Screener ready for: {}", method.getName());
    }

    // ============================================================
    // SECTION 1 - SCREEN LOAD & TABS
    // ============================================================

    @Test(description = "TC_FS_001 - Fund Screener opens from Hub")
    public void tc_fs_001_verifyScreenLoads() {
        Assert.assertTrue(
                fundScreenerPage.isFundScreenerDisplayed(),
                "Fund Screener screen must be displayed");
        logger.info("TC_FS_001 - screen load verified");
    }

    @Test(description = "TC_FS_002 - Premium tab is the default selected tab")
    public void tc_fs_002_verifyPremiumTabDefaultSelected() {
        Assert.assertTrue(
                fundScreenerPage.isPremiumTabDisplayed(),
                "Premium tab must be visible");
        Assert.assertTrue(
                fundScreenerPage.isPremiumTabSelected(),
                "Premium tab must be selected by default");
        logger.info("TC_FS_002 - Premium default selection verified");
    }

    @Test(description = "TC_FS_003 - Categories tab is visible")
    public void tc_fs_003_verifyCategoriesTabVisible() {
        Assert.assertTrue(
                fundScreenerPage.isCategoriesTabDisplayed(),
                "Categories tab must be visible");
        logger.info("TC_FS_003 - Categories tab visibility verified");
    }

    @Test(description = "TC_FS_004 - Saved tab is visible")
    public void tc_fs_004_verifySavedTabVisible() {
        Assert.assertTrue(
                fundScreenerPage.isSavedTabDisplayed(),
                "Saved tab must be visible");
        logger.info("TC_FS_004 - Saved tab visibility verified");
    }

    @Test(description = "TC_FS_005 - Back button is visible")
    public void tc_fs_005_verifyBackButtonVisible() {
        Assert.assertTrue(
                fundScreenerPage.isBackButtonDisplayed(),
                "Back button must be visible");
        logger.info("TC_FS_005 - back button visibility verified");
    }

    // ============================================================
    // SECTION 2 - PREMIUM PRESET CARDS (PRD: Premium presets)
    // ============================================================

    @Test(description = "TC_FS_006 - Top Rated Funds card is visible (PRD: Top Rated Funds preset)")
    public void tc_fs_006_verifyTopRatedCardVisible() {
        Assert.assertTrue(
                fundScreenerPage.isTopRatedFundsCardDisplayed(),
                "Top Rated Funds card must be visible");
        logger.info("TC_FS_006 - Top Rated Funds card verified");
    }

    @Test(description = "TC_FS_007 - Rs 100 SIP Funds card is visible (PRD: Rs 100 SIP Funds preset)")
    public void tc_fs_007_verifyRs100SipCardVisible() {
        Assert.assertTrue(
                fundScreenerPage.isSip100CardDisplayed(),
                "Rs 100 SIP Funds card must be visible");
        logger.info("TC_FS_007 - Rs 100 SIP Funds card verified");
    }

    @Test(description = "TC_FS_008 - Funds for first-time investor card is visible (PRD: first-time investors preset)")
    public void tc_fs_008_verifyFirstTimeInvestorCardVisible() {
        Assert.assertTrue(
                fundScreenerPage.isFirstTimeInvestorCardDisplayed(),
                "Funds for first-time investor card must be visible");
        logger.info("TC_FS_008 - first-time investor card verified");
    }

    @Test(description = "TC_FS_009 - Top Rated Funds opens a filtered fund list")
    public void tc_fs_009_verifyTopRatedOpensFundList() {
        Assert.assertTrue(
                fundScreenerPage.tapTopRatedFundsAndVerify(),
                "Top Rated Funds must open a fund list");
        logger.info("TC_FS_009 - Top Rated Funds navigation verified");
    }

    @Test(description = "TC_FS_010 - Rs 100 SIP Funds opens a filtered fund list")
    public void tc_fs_010_verifyRs100SipOpensFundList() {
        Assert.assertTrue(
                fundScreenerPage.tapSip100FundsAndVerify(),
                "Rs 100 SIP Funds must open a fund list");
        logger.info("TC_FS_010 - Rs 100 SIP Funds navigation verified");
    }

    @Test(description = "TC_FS_011 - Funds for first-time investor opens a filtered fund list")
    public void tc_fs_011_verifyFirstTimeInvestorOpensFundList() {
        Assert.assertTrue(
                fundScreenerPage.tapFirstTimeInvestorAndVerify(),
                "Funds for first-time investor must open a fund list");
        logger.info("TC_FS_011 - first-time investor navigation verified");
    }

    // ============================================================
    // SECTION 3 - CATEGORIES TAB
    // ============================================================

    @Test(description = "TC_FS_012 - Switching to Categories tab shows category content")
    public void tc_fs_012_verifySwitchToCategoriesTab() {
        fundScreenerPage.tapCategoriesTab();

        Assert.assertTrue(
                fundScreenerPage.isCategoriesTabSelected(),
                "Categories tab must be selected after tap");
        Assert.assertTrue(
                fundScreenerPage.isCategoryChipsDisplayed()
                        || fundScreenerPage.isAnyCategoryCardDisplayed(),
                "Categories tab must show chips or category cards");

        logger.info("TC_FS_012 - Categories tab switch verified");
    }

    @Test(description = "TC_FS_013 - Category filter chips are visible on the Categories tab")
    public void tc_fs_013_verifyCategoryChipsVisible() {
        fundScreenerPage.tapCategoriesTab();

        Assert.assertTrue(
                fundScreenerPage.isCategoryChipsDisplayed(),
                "Category chips (Popular/Equity/Debt/Hybrid) must be visible");

        logger.info("TC_FS_013 - category chips verified");
    }

    @Test(description = "TC_FS_014 - Category cards are visible on the Categories tab")
    public void tc_fs_014_verifyCategoryCardsVisible() {
        fundScreenerPage.tapCategoriesTab();

        Assert.assertTrue(
                fundScreenerPage.isAnyCategoryCardDisplayed(),
                "At least one category card ('<n> Funds') must be visible");

        logger.info("TC_FS_014 - category cards verified");
    }

    @Test(description = "TC_FS_015 - Saved tab is reachable")
    public void tc_fs_015_verifySwitchToSavedTab() {
        fundScreenerPage.tapSavedTab();

        Assert.assertTrue(
                fundScreenerPage.isFundScreenerDisplayed(),
                "Fund Screener must remain displayed on the Saved tab");

        logger.info("TC_FS_015 - Saved tab switch verified");
    }

    // ============================================================
    // SECTION 4 - RECURSIVE LINK NAVIGATION (requirement #4)
    // Covers every reachable preset card -> fund list -> nested links
    // (fund rows, Filters, Columns), validating each opens a screen.
    // ============================================================

    @Test(description = "TC_FS_016 - Every link opens a valid screen (recursive crawl, follows nested links)")
    public void tc_fs_016_verifyAllLinksRecursively() {
        List<LinkResult> results = fundScreenerPage.tapAllLinksRecursively();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "Link must open a valid screen: " + result.name);
        }

        logger.info("TC_FS_016 - recursive link navigation verified: {}", results);
    }

    @Test(description = "TC_FS_017 - Back button navigates away from Fund Screener")
    public void tc_fs_017_verifyBackNavigation() {
        Assert.assertTrue(
                fundScreenerPage.isBackButtonDisplayed(),
                "Back button must be visible before tapping");

        Assert.assertTrue(
                fundScreenerPage.tapBackAndVerify(),
                "Tapping back must navigate away from Fund Screener");

        logger.info("TC_FS_017 - back navigation verified");
    }

    // ============================================================
    // SECTION 5 - CUSTOMISED FILTER FLOW
    // open Fund Screener -> Top Rated Funds -> Filters -> add filter -> Apply
    // ============================================================

    @Test(description = "TC_FS_018 - Filters panel opens from a fund list with its controls")
    public void tc_fs_018_verifyFiltersPanelOpens() {
        Assert.assertTrue(
                fundScreenerPage.openTopRatedFundList(),
                "Top Rated Funds list must open");

        Assert.assertTrue(
                fundScreenerPage.openFiltersPanel(),
                "Filters panel must open after tapping Filters");

        Assert.assertTrue(
                fundScreenerPage.isFiltersPanelDisplayed(),
                "Filters panel must show 'Add Filter' and 'Apply Filters'");

        logger.info("TC_FS_018 - Filters panel open verified");
    }

    @Test(description = "TC_FS_019 - Customise a filter on Top Rated Funds and Apply returns a filtered list")
    public void tc_fs_019_verifyCustomiseAndApplyFilters() {
        boolean appliedFilteredList =
                fundScreenerPage.openTopRatedThenCustomiseAndApply("Rating");
        
        fundScreenerPage.tapOnAddFilter();

        Assert.assertTrue(
                appliedFilteredList,
                "After customising and applying filters, a filtered fund list must be shown");

        logger.info("TC_FS_019 - customise + apply filters verified");
    }

    // ============================================================
    // PRD COVERAGE NOTES (not asserted - need FundScreener_Page extensions)
    // ============================================================
    //
    // The PRD specifies, per preset, exact Filter selections and Custom-column
    // sets, plus app-wide decimal-place display rules. Asserting these needs page
    // methods that do not exist yet, e.g.:
    //   - openPresetByName(String) / openFundListFor(preset) that opens AND stays
    //     on the fund list (today's tap...AndVerify methods open then navigate back)
    //   - getAppliedFilters() / getCustomColumns() to read the Filters and Columns
    //     panels for value-level assertions
    //   - getCell(fund, column) + a decimal-format matcher for the decimal rules
    // Adding those (page-only, no framework change) would let this suite assert the
    // remaining PRD presets (Popular, Stable Growth, Steady Income, Funds on Fire,
    // Efficient index, Most rewarding SIPs, Consistent Outperformers, Safety First)
    // and their filter/column/decimal specifications.
}
