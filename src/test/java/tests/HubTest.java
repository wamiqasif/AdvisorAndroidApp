package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.FundHub_Page;
import pages.FundHub_Page.LinkResult;

/**
 * Hub Screen Test Suite — link navigation journey.
 *
 * <p>Persistent-session mode. Every test follows the same three-step journey:
 * <ol>
 *   <li><b>Start from Hub</b> — {@link #recoverAppState(Method)} guarantees the Hub
 *       screen is loaded before each test.</li>
 *   <li><b>Tap a link</b> — one specific Hub link per test.</li>
 *   <li><b>Validate the destination screen, then return to Hub</b> — each
 *       {@code tapXxxAndValidate()} method asserts a new screen opened and
 *       navigates back to Hub before returning.</li>
 * </ol>
 * </p>
 *
 * <p>Structure tests (TC_HUB_001 – TC_HUB_003) verify the Hub screen itself loads
 * correctly. Link-navigation tests (TC_HUB_004 – TC_HUB_021) each exercise one of
 * the 18 navigable links across all Hub sections. TC_HUB_022 is a comprehensive
 * bulk run that exercises all links in one pass.</p>
 */
public class HubTest extends BaseTest {

    private FundHub_Page hubPage;

    // ============================================================
    // CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        hubPage = new FundHub_Page(getDriver());
    }

    /**
     * Journey start: ensure the Hub screen is displayed before every test.
     *
     * <p>{@link FundHub_Page#openHubScreen()} presses back up to 4 times to
     * unwind any Flutter overlay before tapping the Hub tab, so it is safe to
     * call even when the bottom navigation bar is temporarily hidden (e.g.
     * during a Flutter animation right after PIN entry).</p>
     *
     * <p>The assertion accepts either the Hub content elements being visible
     * (full Hub render confirmed) OR the Hub tab being visible (navigation
     * possible — handles the case where content locators don't match the exact
     * content-desc strings the current app build exposes).</p>
     */
    @Override
    protected void recoverAppState(Method method) {
        waitForUiToSettle();
        hubPage.openHubScreen();
        boolean hubReady = hubPage.isHubScreenDisplayed() || hubPage.isHubTabVisible();
        Assert.assertTrue(hubReady,
                "Hub screen (content or tab) must be reachable before: " + method.getName());
        logger.info("Hub screen ready for: {}", method.getName());
    }

    // ============================================================
    // SECTION 1 — HUB SCREEN STRUCTURE
    // Journey: start on Hub, assert structure, stay on Hub
    // ============================================================

    @Test(description = "TC_HUB_001 — Hub screen loads with Mutual Funds section visible")
    public void tc_hub_001_verifyHubScreenLoads() {

        Assert.assertTrue(
                hubPage.isHubScreenDisplayed(),
                "Hub screen must be displayed");

        Assert.assertTrue(
                hubPage.isMutualFundsSectionDisplayed(),
                "'Mutual Funds' section label must be visible on Hub screen");

        logger.info("TC_HUB_001 — Hub screen load and Mutual Funds section verified");
    }

    @Test(description = "TC_HUB_002 — Bottom navigation is visible on Hub screen")
    public void tc_hub_002_verifyBottomNavigationVisible() {

        Assert.assertTrue(
                hubPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible on Hub screen");

        logger.info("TC_HUB_002 — Bottom navigation visibility verified");
    }

    @Test(description = "TC_HUB_003 — Hub scrolls to reveal More section")
    public void tc_hub_003_verifyMoreSectionVisible() {

        Assert.assertTrue(
                hubPage.isMoreSectionDisplayed(),
                "'More' section must be visible after scrolling Hub screen");

        logger.info("TC_HUB_003 — More section visibility verified");
    }

    // ============================================================
    // SECTION 2 — TRANSACTIONS SECTION
    // Journey: Hub → tap link → validate screen → back to Hub
    // ============================================================

    @Test(description = "TC_HUB_004 — Hub → View All Transactions → validate screen → back to Hub")
    public void tc_hub_004_verifyViewAllTransactionsNavigation() {

        Assert.assertTrue(
                hubPage.tapViewAllTransactionsAndValidate(),
                "'View All Transactions' must open a valid screen from Hub");

        logger.info("TC_HUB_004 — View All Transactions navigation journey verified");
    }

    // ============================================================
    // SECTION 3 — MUTUAL FUNDS SECTION
    // Journey: Hub → tap link → validate screen → back to Hub
    // ============================================================

    @Test(description = "TC_HUB_005 — Hub → Portfolio Planner → validate screen → back to Hub")
    public void tc_hub_005_verifyPortfolioPlannerNavigation() {

        Assert.assertTrue(
                hubPage.tapPortfolioPlannerAndValidate(),
                "'Portfolio Planner' must open a valid screen from Hub");

        logger.info("TC_HUB_005 — Portfolio Planner navigation journey verified");
    }

    @Test(description = "TC_HUB_006 — Hub → Analyst's Choice → validate screen → back to Hub")
    public void tc_hub_006_verifyAnalystsChoiceNavigation() {

        Assert.assertTrue(
                hubPage.tapAnalystsChoiceAndValidate(),
                "'Analyst's Choice' must open a valid screen from Hub");

        logger.info("TC_HUB_006 — Analyst's Choice navigation journey verified");
    }

    @Test(description = "TC_HUB_007 — Hub → Fund Screener → validate screen → back to Hub")
    public void tc_hub_007_verifyFundScreenerNavigation() {

        Assert.assertTrue(
                hubPage.tapFundScreenerAndValidate(),
                "'Fund Screener' must open a valid screen from Hub");

        logger.info("TC_HUB_007 — Fund Screener navigation journey verified");
    }

    @Test(description = "TC_HUB_008 — Hub → SIP Return Calculator → validate screen → back to Hub")
    public void tc_hub_008_verifySipReturnCalculatorNavigation() {

        Assert.assertTrue(
                hubPage.tapSipReturnCalculatorAndValidate(),
                "'SIP Return Calculator' must open a valid screen from Hub");

        logger.info("TC_HUB_008 — SIP Return Calculator navigation journey verified");
    }

    @Test(description = "TC_HUB_009 — Hub → Stories and Videos → validate screen → back to Hub")
    public void tc_hub_009_verifyStoriesAndVideosNavigation() {

        Assert.assertTrue(
                hubPage.tapStoriesAndVideosAndValidate(),
                "'Stories and Videos' must open a valid screen from Hub");

        logger.info("TC_HUB_009 — Stories and Videos navigation journey verified");
    }

    // ============================================================
    // SECTION 4 — STOCKS SECTION
    // Journey: Hub → tap link → validate screen → back to Hub
    // ============================================================

    @Test(description = "TC_HUB_010 — Hub → Market Monitor → validate screen → back to Hub")
    public void tc_hub_010_verifyMarketMonitorNavigation() {

        Assert.assertTrue(
                hubPage.tapMarketMonitorAndValidate(),
                "'Market Monitor' must open a valid screen from Hub");

        logger.info("TC_HUB_010 — Market Monitor navigation journey verified");
    }

    @Test(description = "TC_HUB_011 — Hub → Stock Advisor → validate screen → back to Hub")
    public void tc_hub_011_verifyStockAdvisorNavigation() {

        Assert.assertTrue(
                hubPage.tapStockAdvisorAndValidate(),
                "'Stock Advisor' must open a valid screen from Hub");

        logger.info("TC_HUB_011 — Stock Advisor navigation journey verified");
    }

    // ============================================================
    // SECTION 5 — MORE SECTION
    // Journey: Hub → scroll to More → tap link → validate screen → back to Hub
    // ============================================================

    @Test(description = "TC_HUB_012 — Hub → FAQ → validate screen → back to Hub")
    public void tc_hub_012_verifyFaqNavigation() {

        Assert.assertTrue(
                hubPage.tapFaqAndValidate(),
                "'FAQ' must open a valid screen from Hub");

        logger.info("TC_HUB_012 — FAQ navigation journey verified");
    }

    @Test(description = "TC_HUB_013 — Hub → About Value Research → validate screen → back to Hub")
    public void tc_hub_013_verifyAboutValueResearchNavigation() {

        Assert.assertTrue(
                hubPage.tapAboutValueResearchAndValidate(),
                "'About Value Research' must open a valid screen from Hub");

        logger.info("TC_HUB_013 — About Value Research navigation journey verified");
    }

    @Test(description = "TC_HUB_014 — Hub → Contact Us → validate screen → back to Hub")
    public void tc_hub_014_verifyContactUsNavigation() {

        Assert.assertTrue(
                hubPage.tapContactUsAndValidate(),
                "'Contact Us' must open a valid screen from Hub");

        logger.info("TC_HUB_014 — Contact Us navigation journey verified");
    }

    @Test(description = "TC_HUB_015 — Hub → Privacy Policy → validate screen → back to Hub")
    public void tc_hub_015_verifyPrivacyPolicyNavigation() {

        Assert.assertTrue(
                hubPage.tapPrivacyPolicyAndValidate(),
                "'Privacy Policy' must open a valid screen from Hub");

        logger.info("TC_HUB_015 — Privacy Policy navigation journey verified");
    }

    @Test(description = "TC_HUB_016 — Hub → User Agreement → validate screen → back to Hub")
    public void tc_hub_016_verifyUserAgreementNavigation() {

        Assert.assertTrue(
                hubPage.tapUserAgreementAndValidate(),
                "'User Agreement' must open a valid screen from Hub");

        logger.info("TC_HUB_016 — User Agreement navigation journey verified");
    }

    @Test(description = "TC_HUB_017 — Hub → Refund Policy → validate screen → back to Hub")
    public void tc_hub_017_verifyRefundPolicyNavigation() {

        Assert.assertTrue(
                hubPage.tapRefundPolicyAndValidate(),
                "'Refund Policy' must open a valid screen from Hub");

        logger.info("TC_HUB_017 — Refund Policy navigation journey verified");
    }

    @Test(description = "TC_HUB_018 — Hub → Investor Charter → validate screen → back to Hub")
    public void tc_hub_018_verifyInvestorCharterNavigation() {

        Assert.assertTrue(
                hubPage.tapInvestorCharterAndValidate(),
                "'Investor Charter' must open a valid screen from Hub");

        logger.info("TC_HUB_018 — Investor Charter navigation journey verified");
    }

    @Test(description = "TC_HUB_019 — Hub → Submit or Track Complaint → validate screen → back to Hub")
    public void tc_hub_019_verifyComplaintNavigation() {

        Assert.assertTrue(
                hubPage.tapComplaintAndValidate(),
                "'Submit or Track Complaint' must open a valid screen from Hub");

        logger.info("TC_HUB_019 — Complaint navigation journey verified");
    }

    @Test(description = "TC_HUB_020 — Hub → ODR Portal → validate screen → back to Hub")
    public void tc_hub_020_verifyOdrPortalNavigation() {

        Assert.assertTrue(
                hubPage.tapOdrPortalAndValidate(),
                "'ODR Portal' must open a valid screen from Hub");

        logger.info("TC_HUB_020 — ODR Portal navigation journey verified");
    }

    @Test(description = "TC_HUB_021 — Hub → Audit Status → validate screen → back to Hub")
    public void tc_hub_021_verifyAuditStatusNavigation() {

        Assert.assertTrue(
                hubPage.tapAuditStatusAndValidate(),
                "'Audit Status' must open a valid screen from Hub");

        logger.info("TC_HUB_021 — Audit Status navigation journey verified");
    }

    // ============================================================
    // SECTION 6 — COMPREHENSIVE BULK VALIDATION
    // Journey: Hub → each of the 18 links in order → validate → back to Hub → repeat
    // ============================================================

    @Test(description = "TC_HUB_022 — Hub → every link (one by one) → validate → back to Hub")
    public void tc_hub_022_verifyAllLinksOpenValidScreens() {

        List<LinkResult> results = hubPage.tapAllLinksAndValidateScreens();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one Hub link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "Hub link must open a valid screen: " + result.name);
        }

        logger.info("TC_HUB_022 — All Hub link journeys verified: {}", results);
    }
}
