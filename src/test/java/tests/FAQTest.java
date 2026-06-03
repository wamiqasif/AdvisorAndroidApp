package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;
import pages.FAQPage;
import pages.FAQPage.LinkResult;

/**
 * FAQ Screen Test Suite.
 *
 * <p>Persistent-session mode. Every test journey is the same and deliberately
 * simple: start on the Hub → tap the "FAQs" section → assert on the FAQ screen.
 * {@link #recoverAppState(Method)} performs that navigation before each test, so
 * each {@code @Test} method begins on the FAQ screen.</p>
 *
 * <p>The FAQ screen's only navigable links are the three category links
 * (Research and Advice, Buying and Selling Mutual Funds, Fund Advisor Account).
 * The link-coverage tests tap every one of them and validate that the
 * corresponding questions sub-screen opens, then return to the FAQ list.</p>
 */
public class FAQTest extends BaseTest {

    private FAQPage faqPage;
    private DashboardPage dashboardPage;

    // ============================================================
    // CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        faqPage = new FAQPage(getDriver());
        dashboardPage = new DashboardPage(getDriver());
    }

    /**
     * Common journey for every test: ensure we are on the Hub, then open the
     * FAQ section. Leaves the app on the FAQ screen.
     */
    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        dashboardPage.tapHubTab();
        faqPage.openFaqScreen();
        Assert.assertTrue(
                faqPage.isFaqScreenDisplayed(),
                "FAQ screen must load before " + method.getName());
        logger.info("FAQ screen ready for: {}", method.getName());
    }

    // ============================================================
    // SECTION 1 — SCREEN LOAD & STRUCTURE
    // ============================================================

    @Test(description = "TC_FAQ_001 — FAQ screen opens from Hub with heading visible")
    public void tc_faq_001_verifyFaqScreenLoads() {

        Assert.assertTrue(
                faqPage.isFaqScreenDisplayed(),
                "FAQ screen must load");

        Assert.assertTrue(
                faqPage.isFaqHeadingDisplayed(),
                "'Frequently Asked Questions' heading must be visible");

        logger.info("TC_FAQ_001 — FAQ screen load verified");
    }

    @Test(description = "TC_FAQ_002 — Back button is present on the FAQ screen")
    public void tc_faq_002_verifyBackButtonPresent() {

        Assert.assertTrue(
                faqPage.isBackButtonDisplayed(),
                "Back button must be visible on the FAQ screen");

        logger.info("TC_FAQ_002 — Back button presence verified");
    }

    @Test(description = "TC_FAQ_003 — Header action icon is present on the FAQ screen")
    public void tc_faq_003_verifyHeaderActionIconPresent() {

        Assert.assertTrue(
                faqPage.isHeaderActionIconDisplayed(),
                "Header action icon must be visible on the FAQ screen");

        logger.info("TC_FAQ_003 — Header action icon presence verified");
    }

    @Test(description = "TC_FAQ_004 — Bottom navigation is visible on the FAQ screen")
    public void tc_faq_004_verifyBottomNavigationPresent() {

        Assert.assertTrue(
                faqPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible on the FAQ screen");

        logger.info("TC_FAQ_004 — Bottom navigation presence verified");
    }

    // ============================================================
    // SECTION 2 — CATEGORY LINK VISIBILITY
    // ============================================================

    @Test(description = "TC_FAQ_005 — All three FAQ category links are visible")
    public void tc_faq_005_verifyAllCategoryLinksVisible() {

        Assert.assertTrue(
                faqPage.areAllCategoryLinksDisplayed(),
                "All FAQ category links must be visible");

        Assert.assertEquals(
                faqPage.getVisibleCategoryCount(),
                3,
                "Exactly three FAQ category links are expected");

        logger.info("TC_FAQ_005 — All category links visibility verified");
    }

    @Test(description = "TC_FAQ_006 — 'Research and Advice' link is visible")
    public void tc_faq_006_verifyResearchAndAdviceLinkVisible() {

        Assert.assertTrue(
                faqPage.isResearchAndAdviceLinkDisplayed(),
                "'Research and Advice' link must be visible");

        logger.info("TC_FAQ_006 — 'Research and Advice' link visibility verified");
    }

    @Test(description = "TC_FAQ_007 — 'Buying and Selling Mutual Funds' link is visible")
    public void tc_faq_007_verifyBuyingSellingFundsLinkVisible() {

        Assert.assertTrue(
                faqPage.isBuyingSellingFundsLinkDisplayed(),
                "'Buying and Selling Mutual Funds' link must be visible");

        logger.info("TC_FAQ_007 — 'Buying and Selling Mutual Funds' link visibility verified");
    }

    @Test(description = "TC_FAQ_008 — 'Fund Advisor Account' link is visible")
    public void tc_faq_008_verifyFundAdvisorAccountLinkVisible() {

        Assert.assertTrue(
                faqPage.isFundAdvisorAccountLinkDisplayed(),
                "'Fund Advisor Account' link must be visible");

        logger.info("TC_FAQ_008 — 'Fund Advisor Account' link visibility verified");
    }

    // ============================================================
    // SECTION 3 — LINK NAVIGATION (tap every link + validate sub-screen)
    // ============================================================

    @Test(description = "TC_FAQ_009 — Tapping 'Research and Advice' expands its questions")
    public void tc_faq_009_verifyResearchAndAdviceLinkNavigation() {

        boolean opened = faqPage.tapCategoryAndVerify("Research and Advice");

        Assert.assertTrue(
                opened,
                "'Research and Advice' questions must expand after tapping the link");

        logger.info("TC_FAQ_009 — 'Research and Advice' link expansion verified");
    }

    @Test(description = "TC_FAQ_010 — Tapping 'Buying and Selling Mutual Funds' expands its questions")
    public void tc_faq_010_verifyBuyingSellingFundsLinkNavigation() {

        boolean opened = faqPage.tapCategoryAndVerify("Buying and Selling Mutual Funds");

        Assert.assertTrue(
                opened,
                "'Buying and Selling Mutual Funds' questions must expand after tapping the link");

        logger.info("TC_FAQ_010 — 'Buying and Selling Mutual Funds' link expansion verified");
    }

    @Test(description = "TC_FAQ_011 — Tapping 'Fund Advisor Account' expands its questions")
    public void tc_faq_011_verifyFundAdvisorAccountLinkNavigation() {

        boolean opened = faqPage.tapCategoryAndVerify("Fund Advisor Account");

        Assert.assertTrue(
                opened,
                "'Fund Advisor Account' questions must expand after tapping the link");

        logger.info("TC_FAQ_011 — 'Fund Advisor Account' link expansion verified");
    }

    @Test(description = "TC_FAQ_012 — Every FAQ category link opens a valid sub-screen")
    public void tc_faq_012_verifyAllLinksOpenSubScreens() {

        List<LinkResult> results = faqPage.tapAllCategoriesAndVerify();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one FAQ category link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "FAQ category link must open a valid sub-screen: " + result.name);
        }

        logger.info("TC_FAQ_012 — All link sub-screen navigation verified: {}", results);
    }

    // ============================================================
    // SECTION 4 — ACCORDION BEHAVIOR & STABILITY
    // ============================================================

    @Test(description = "TC_FAQ_013 — Expanding a category reveals its questions inline (no navigation away)")
    public void tc_faq_013_verifyCategoryExpandsInline() {

        boolean expanded = faqPage.tapCategoryAndVerify("Research and Advice");

        Assert.assertTrue(
                expanded,
                "First category must expand its questions inline");

        Assert.assertTrue(
                faqPage.areQuestionsDisplayed(),
                "Question rows must be visible after expanding a category");

        logger.info("TC_FAQ_013 — Inline category expansion verified");
    }

    @Test(description = "TC_FAQ_014 — Back button navigates away from the FAQ screen")
    public void tc_faq_014_verifyBackButtonNavigation() {

        Assert.assertTrue(
                faqPage.isBackButtonDisplayed(),
                "Back button must be visible before navigating back");

        faqPage.tapBack();

        Assert.assertFalse(
                faqPage.isFaqScreenDisplayed(),
                "FAQ screen must no longer be shown after tapping back");

        logger.info("TC_FAQ_014 — Back button navigation verified");
    }
}
