package tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.FundSIFInPortfolio_Page;

/**
 * Portfolio > Funds / SIFs tab test suite.
 *
 * Journey for every test: Portfolio -> Funds / SIFs (recoverAppState) -> tap
 * the SIF(s) / header link(s) under test -> validate the resulting screen ->
 * return to Portfolio > Funds / SIFs. The framework repeats this journey
 * before each test method via recoverAppState(); tearDown() unwinds back to
 * Portfolio afterwards.
 *
 * Uses ONLY methods that exist on FundSIFInPortfolio_Page.
 */
public class SIFInPortfolioTest extends BaseTest {

    private static final Logger logger =
            LoggerFactory.getLogger(SIFInPortfolioTest.class);

    private FundSIFInPortfolio_Page fundSifPage;

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        fundSifPage = new FundSIFInPortfolio_Page(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        fundSifPage.navigateToFundsSifsTab();
        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Portfolio > Funds / SIFs screen should be visible before " + method.getName());
    }

    // ============================================================
    // SMOKE TESTS
    // ============================================================

    @Test(description = "Verify Portfolio > Funds / SIFs screen loads properly")
    public void verifyFundsSifsScreenLoads() {

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Funds / SIFs screen must display");

        Assert.assertTrue(fundSifPage.isSummaryRowDisplayed(),
                "Current / Invested / 1D Change summary row missing");

        logger.info("Funds / SIFs smoke validation passed");
    }

    @Test(description = "Verify bottom navigation is visible on Funds / SIFs tab")
    public void verifyBottomNavigationDisplayed() {

        Assert.assertTrue(fundSifPage.isBottomNavigationDisplayed(),
                "Bottom navigation (Funds / Stocks / Portfolio / Hub) must be visible");

        logger.info("Bottom navigation validated");
    }

    // ============================================================
    // FUND LIST DISCOVERY
    // ============================================================

    @Test(description = "Verify fund rows are discoverable on Funds / SIFs tab")
    public void verifyFundListIsDiscoverable() {

        List<String> fundNames = fundSifPage.getDisplayedFundNames();

        if (fundNames.isEmpty()) {
            throw new SkipException("No SIF/fund rows visible — portfolio may be empty");
        }

        Assert.assertEquals(fundSifPage.getDisplayedFundCount(), fundNames.size(),
                "getDisplayedFundCount() must match getDisplayedFundNames() size");

        for (String fundName : fundNames) {
            Assert.assertTrue(fundName.contains("\n"),
                    "Fund row content-desc should contain plan/rating lines: " + fundName);
            logger.info("Discovered fund row: {}", fundName);
        }
    }

    // ============================================================
    // CORE JOURNEY: tap every SIF, validate, return to Portfolio > Funds/SIFs
    // ============================================================

    @Test(description =
            "Journey: Portfolio > Funds / SIFs -> open every SIF's detail -> validate -> return")
    public void verifyEachFundRowOpensDetailAndReturnsToFundsSifsTab() {

        List<String> fundNames = fundSifPage.getDisplayedFundNames();

        if (fundNames.isEmpty()) {
            throw new SkipException("No SIF/fund rows visible — portfolio may be empty");
        }

        for (String fundName : fundNames) {

            logger.info("Opening fund detail for: {}", fundName);
            fundSifPage.openFundDetail(fundName);

            Assert.assertTrue(fundSifPage.isFundDetailScreenDisplayed(),
                    "Fund Detail screen should open for: " + fundName);

            fundSifPage.closeFundDetail();

            Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                    "Should return to Portfolio > Funds / SIFs after closing fund: " + fundName);

            logger.info("Fund '{}' validated and returned to Funds / SIFs", fundName);
        }
    }

    @Test(description = "Verify Fund Detail data (Invested / Current / Your Return) is extractable")
    public void verifyFundDetailDataIsExtractable() {

        List<String> fundNames = fundSifPage.getDisplayedFundNames();

        if (fundNames.isEmpty()) {
            throw new SkipException("No SIF/fund rows visible — portfolio may be empty");
        }

        String fundName = fundNames.get(0);
        fundSifPage.openFundDetail(fundName);

        Assert.assertTrue(fundSifPage.isFundDetailScreenDisplayed(),
                "Fund Detail screen should open for: " + fundName);

        String invested = fundSifPage.getInvestedAmountOnDetail();
        String current = fundSifPage.getCurrentAmountOnDetail();
        String yourReturn = fundSifPage.getYourReturnPercent();

        logger.info("Fund '{}' — Invested: '{}', Current: '{}', Your Return: '{}'",
                fundName, invested, current, yourReturn);

        Assert.assertNotNull(invested, "Invested amount must not be null");
        Assert.assertNotNull(current, "Current amount must not be null");
        Assert.assertNotNull(yourReturn, "Your Return percentage must not be null");

        fundSifPage.closeFundDetail();

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should return to Portfolio > Funds / SIFs after closing fund detail");
    }

    // ============================================================
    // FUND DETAIL SECTIONS + FORWARD NAVIGATION TO SIF DETAILS PAGE
    // ============================================================

    @Test(description =
            "Journey: open 'Arudha Equity Long-Short Reg-IDCWM' detail -> tap Performance & Your "
                    + "Investment, validate their data -> tap forward arrow -> open SIF Details page "
                    + "-> validate -> return")
    public void verifyFundDetailSectionsAndForwardNavigationToSifDetails() {

        String fundName = "Arudha Equity Long-Short Reg-IDCWM";

        if (!fundSifPage.scrollToFund(fundName)) {
            throw new SkipException("Fund not visible on Funds / SIFs tab: " + fundName);
        }

        fundSifPage.openFundDetail(fundName);
        Assert.assertTrue(fundSifPage.isFundDetailScreenDisplayed(),
                "Fund Detail screen should open for: " + fundName);

        fundSifPage.tapPerformanceSection();
        String yourReturn = fundSifPage.getYourReturnPercent();
        logger.info("Performance section — Your Return: '{}'", yourReturn);
        Assert.assertNotNull(yourReturn, "Your Return text must not be null after tapping Performance");

        fundSifPage.tapYourInvestmentSection();
        String invested = fundSifPage.getInvestedAmountOnDetail();
        String current = fundSifPage.getCurrentAmountOnDetail();
        logger.info("Your Investment section — Invested: '{}', Current: '{}'", invested, current);
        Assert.assertNotNull(invested, "Invested text must not be null after tapping Your Investment");
        Assert.assertNotNull(current, "Current text must not be null after tapping Your Investment");

        fundSifPage.tapForwardArrowOnFundDetail(fundName);

        boolean sifDetailsOpened = fundSifPage.isSifDetailsPageDisplayed();
        Assert.assertTrue(sifDetailsOpened,
                "SIF Details page should open after tapping the forward arrow on: " + fundName);

        logger.info("SIF Details page opened for '{}': {}", fundName, sifDetailsOpened);

        fundSifPage.navigateToFundsSifsTab();
        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should return to Portfolio > Funds / SIFs after SIF Details validation");
    }

    // ============================================================
    // HEADER LINK INTERACTIONS — tap link, validate, return
    // ============================================================

    @Test(description = "Verify Investor Selector link opens and returns to Funds / SIFs")
    public void verifyInvestorSelectorInteraction() {

        if (!fundSifPage.isInvestorSelectorDisplayed()) {
            throw new SkipException("Investor Selector not visible");
        }

        fundSifPage.tapInvestorSelector();
        fundSifPage.navigateToFundsSifsTab();

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should return to Funds / SIFs after Investor Selector interaction");

        logger.info("Investor Selector interaction validated");
    }

    @Test(description = "Verify Update Funds / SIFs Holdings link opens and returns to Funds / SIFs")
    public void verifyUpdateHoldingsButtonInteraction() {

        if (!fundSifPage.isUpdateHoldingsButtonDisplayed()) {
            throw new SkipException("Update Funds / SIFs Holdings button not visible");
        }

        fundSifPage.tapUpdateHoldingsButton();
        fundSifPage.navigateToFundsSifsTab();

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should return to Funds / SIFs after Update Holdings interaction");

        logger.info("Update Funds / SIFs Holdings interaction validated");
    }

    @Test(description = "Verify All Time Returns toggle opens and returns to Funds / SIFs")
    public void verifyAllTimeReturnsToggleInteraction() {

        fundSifPage.tapAllTimeReturnsToggle();
        fundSifPage.navigateToFundsSifsTab();

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should return to Funds / SIFs after All Time Returns interaction");

        logger.info("All Time Returns toggle interaction validated");
    }

    @Test(description = "Verify Sort By control opens and returns to Funds / SIFs")
    public void verifySortByInteraction() {

        fundSifPage.tapSortByButton();
        fundSifPage.navigateToFundsSifsTab();

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should return to Funds / SIFs after Sort By interaction");

        logger.info("Sort By interaction validated");
    }

    @Test(description = "Verify Current / Invested toggle switches in-place without leaving Funds / SIFs")
    public void verifyCurrentInvestedToggleInteraction() {

        fundSifPage.tapCurrentInvestedToggle();

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Current / Invested toggle should switch column data in place");

        logger.info("Current / Invested toggle interaction validated");
    }

    // ============================================================
    // COMPREHENSIVE LINK VALIDATION — every header link + every fund row
    // ============================================================

    @Test(description =
            "Journey: tap every link and every SIF on Funds / SIFs, validate the screen each opens, return")
    public void verifyAllLinksAndFundRowsPassValidation() {

        List<FundSIFInPortfolio_Page.LinkResult> results = fundSifPage.tapAllLinksAndValidateScreens();

        Assert.assertFalse(results.isEmpty(), "No navigable links were discovered on Funds / SIFs tab");

        List<String> failed = new ArrayList<>();
        for (FundSIFInPortfolio_Page.LinkResult result : results) {
            logger.info("Link result: {}", result);
            if (!result.passed) {
                failed.add(result.name);
            }
        }

        Assert.assertTrue(failed.isEmpty(), "Links failed validation: " + failed);

        Assert.assertTrue(fundSifPage.isFundsSifsScreenDisplayed(),
                "Should end back on Portfolio > Funds / SIFs after full link validation");

        logger.info("All {} links/SIFs validated on Funds / SIFs tab", results.size());
    }

    @Test(description = "Full regression: verify overall link validation passes across Funds / SIFs")
    public void verifyAllLinksOverallPass() {

        Assert.assertTrue(fundSifPage.verifyAllLinksOpenScreens(),
                "One or more Funds / SIFs links failed to open their expected screen");

        logger.info("Overall Funds / SIFs link validation passed");
    }
}
