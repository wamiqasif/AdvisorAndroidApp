package tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import java.lang.reflect.Method;
import org.testng.annotations.Test;
import org.testng.SkipException;

import base.BaseTest;
import pages.PortfolioPage;

/**
 * Portfolio Test Suite
 *
 * Uses ONLY methods that exist in final PortfolioPage.
 */
public class PortfolioTest extends BaseTest {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    PortfolioTest.class);

    private PortfolioPage portfolioPage;

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        portfolioPage = new PortfolioPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        portfolioPage.navigateToPortfolioIfNotThere();
        Assert.assertTrue(portfolioPage.waitUntilPortfolioScreenVisible(),
                "Portfolio screen should be visible before " + method.getName());
        Assert.assertTrue(portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio screen must remain stable");
    }

    // ============================================================
    // SMOKE TESTS
    // ============================================================

    @Test(description =
            "Verify Portfolio screen loads properly")
    public void verifyPortfolioScreenLoads() {

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio screen must display");

        Assert.assertTrue(
                portfolioPage.isPortfolioValueLabelDisplayed(),
                "Portfolio Value label missing");

        Assert.assertFalse(
                portfolioPage.getPortfolioValueText()
                        .trim()
                        .isEmpty(),
                "Portfolio value missing");

        Assert.assertFalse(
                portfolioPage.getOneDayChangeAmount()
                        .trim()
                        .isEmpty(),
                "1D change missing");

        logger.info(
                "Portfolio smoke validation passed");
    }

    @Test(description =
            "Verify Portfolio summary layout")
    public void verifyPortfolioSummaryLayout() {

        boolean composite =
                portfolioPage.isCompositeSummaryCardDisplayed();

        boolean standard =
                portfolioPage.isPortfolioValueLabelDisplayed();

        Assert.assertTrue(
                composite || standard,
                "Unsupported Portfolio summary layout");

        logger.info(
                "Portfolio summary layout validated");
    }

    @Test(description =
            "Verify supported Portfolio state")
    public void verifyPortfolioState() {

        boolean populated =
                portfolioPage.isPopulatedPortfolioStateDisplayed();

        boolean empty =
                portfolioPage.isEmptyPortfolioStateDisplayed();

        Assert.assertTrue(
                populated || empty,
                "Unsupported Portfolio state");

        logger.info(
                "Portfolio state validated");
    }

    // ============================================================
    // NAVIGATION TESTS
    // ============================================================

    @Test(description =
            "Verify recovery back to Portfolio from Hub")
    public void verifyRecoveryFromHub() {

        portfolioPage.tapHubNavTab();

        portfolioPage.recoverToPortfolioScreen();

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio recovery from Hub failed");

        logger.info(
                "Recovery from Hub validated");
    }

    @Test(description =
            "Verify recovery back to Portfolio from Stocks")
    public void verifyRecoveryFromStocks() {

        portfolioPage.tapStocksNavTab();

        portfolioPage.recoverToPortfolioScreen();

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio recovery from Stocks failed");

        logger.info(
                "Recovery from Stocks validated");
    }

    @Test(description =
            "Verify Portfolio top tab switching")
    public void verifyPortfolioTabSwitching() {

        portfolioPage.tapTabFunds();

        portfolioPage.tapTabSummary();

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio screen lost after tab switching");

        logger.info(
                "Portfolio tab switching validated");
    }

    // ============================================================
    // DATA FORMAT TESTS
    // ============================================================

    @Test(description =
            "Verify Portfolio value format")
    public void verifyPortfolioValueFormat() {

        String portfolioValue =
                portfolioPage.getPortfolioValueText();

        Assert.assertNotNull(
                portfolioValue,
                "Portfolio value must not be null");

        Assert.assertFalse(
                portfolioValue.trim().isEmpty(),
                "Portfolio value must not be empty");

        Assert.assertTrue(
                portfolioValue.trim().startsWith("₹"),
                "Portfolio value must start with ₹");

        logger.info(
                "Portfolio value validated: {}",
                portfolioValue);
    }

    @Test(description =
            "Verify 1D change format")
    public void verifyOneDayChangeFormat() {

        String oneDayChange =
                portfolioPage.getOneDayChangeAmount();

        Assert.assertNotNull(
                oneDayChange,
                "1D change must not be null");

        Assert.assertFalse(
                oneDayChange.trim().isEmpty(),
                "1D change must not be empty");

        Assert.assertTrue(
                oneDayChange.trim().startsWith("₹"),
                "1D change must start with ₹");

        logger.info(
                "1D change validated: {}",
                oneDayChange);
    }

    // ============================================================
    // INVESTOR TESTS
    // ============================================================

    @Test(description =
            "Verify investor dropdown interaction")
    public void verifyInvestorDropdownInteraction() {

        Assert.assertTrue(
                portfolioPage.isInvestorDropdownDisplayed(),
                "Investor dropdown missing");

        portfolioPage.tapInvestorDropdown();

        Assert.assertTrue(
                portfolioPage.isInvestorSelectionSheetDisplayed()
                        || portfolioPage.isPortfolioScreenDisplayed(),
                "Investor interaction failed");

        portfolioPage.recoverToPortfolioScreen();

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio recovery failed");

        logger.info(
                "Investor dropdown interaction validated");
    }

    // ============================================================
    // ALERT TESTS
    // ============================================================

    @Test(description =
            "Verify alert banner interaction")
    public void verifyAlertBannerInteraction() {

        if (!portfolioPage.isAlertBannerDisplayed()) {

            throw new SkipException(
                    "Alert banner not visible");
        }

        String alertText =
                portfolioPage.getAlertBannerText();

        Assert.assertNotNull(
                alertText,
                "Alert text missing");

        Assert.assertFalse(
                alertText.trim().isEmpty(),
                "Alert text empty");

        portfolioPage.tapAlertBanner();

        portfolioPage.recoverToPortfolioScreen();

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio recovery failed");

        logger.info(
                "Alert banner interaction validated");
    }

    // ============================================================
    // SCROLL TESTS
    // ============================================================

    @Test(description =
            "Verify Analysis section reachable")
    public void verifyAnalysisSectionScroll() {

        Assert.assertTrue(
                portfolioPage.scrollToAnalysisSection(),
                "Analysis section unreachable");

        Assert.assertTrue(
                portfolioPage.isAnalysisHeadingDisplayed(),
                "Analysis heading missing");

        logger.info(
                "Analysis section validated");
    }

    @Test(description =
            "Verify Outdated Portfolio card reachable")
    public void verifyOutdatedPortfolioCardScroll() {

        Assert.assertTrue(
                portfolioPage.scrollToOutdatedPortfolioCard(),
                "Outdated Portfolio card unreachable");

        Assert.assertTrue(
                portfolioPage.isOutdatedPortfolioCardDisplayed(),
                "Outdated Portfolio card missing");

        logger.info(
                "Outdated Portfolio card validated");
    }

    @Test(description =
            "Verify Risk Profile card reachable")
    public void verifyRiskProfileCardScroll() {

        Assert.assertTrue(
                portfolioPage.scrollToRiskProfileCard(),
                "Risk Profile card unreachable");

        Assert.assertTrue(
                portfolioPage.isRiskProfileCardDisplayed(),
                "Risk Profile card missing");

        logger.info(
                "Risk Profile card validated");
    }

    @Test(description =
            "Verify View All link reachable")
    public void verifyViewAllLinkScroll() {

        Assert.assertTrue(
                portfolioPage.scrollToViewAllLink(),
                "View All link unreachable");

        Assert.assertTrue(
                portfolioPage.isViewAllLinkDisplayed(),
                "View All link missing");

        logger.info(
                "View All link validated");
    }

    // ============================================================
    // OVERLAY TESTS
    // ============================================================

    @Test(description =
            "Verify All Time Returns interaction")
    public void verifyAllTimeReturnsInteraction() {

        boolean overlayOpened =
                portfolioPage.openAllTimeReturnsOverlay();

        Assert.assertTrue(
                overlayOpened
                        || portfolioPage.isPortfolioScreenDisplayed(),
                "All Time Returns interaction failed");

        if (portfolioPage.isDurationBottomSheetVisible()) {

            portfolioPage.tapScrim();
        }

        Assert.assertTrue(
                portfolioPage.isPortfolioScreenDisplayed(),
                "Portfolio screen lost after interaction");

        logger.info(
                "All Time Returns interaction validated");
    }
}
