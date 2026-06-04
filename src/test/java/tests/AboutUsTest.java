package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.AboutUs_Page;
import pages.AboutUs_Page.LinkResult;
import pages.DashboardPage;

public class AboutUsTest extends BaseTest {

    private AboutUs_Page aboutUsPage;
    private DashboardPage dashboardPage;

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        aboutUsPage = new AboutUs_Page(getDriver());
        dashboardPage = new DashboardPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        dashboardPage.tapHubTab();
        aboutUsPage.openAboutUsScreen();

        Assert.assertTrue(
                aboutUsPage.isAboutUsScreenDisplayed(),
                "About Us screen must load before " + method.getName());

        logger.info("About Us screen ready for: {}", method.getName());
    }

    @Test(description = "TC_ABOUT_001 - About Us screen opens from Hub")
    public void tc_about_001_verifyAboutUsScreenLoads() {

        Assert.assertTrue(
                aboutUsPage.isAboutUsScreenDisplayed(),
                "About Us screen must be displayed");

        logger.info("TC_ABOUT_001 - About Us screen load verified");
    }

    @Test(description = "TC_ABOUT_002 - Back button is visible on About Us screen")
    public void tc_about_002_verifyBackButtonVisible() {

        Assert.assertTrue(
                aboutUsPage.isBackButtonDisplayed(),
                "Back button must be visible on About Us screen");

        logger.info("TC_ABOUT_002 - Back button visibility verified");
    }

    @Test(description = "TC_ABOUT_003 - Independent Advisors section is visible")
    public void tc_about_003_verifyIndependentAdvisorsSectionVisible() {

        Assert.assertTrue(
                aboutUsPage.isIndependentAdvisorsSectionDisplayed(),
                "Independent Advisors section must be visible");

        logger.info("TC_ABOUT_003 - Independent Advisors section verified");
    }

    @Test(description = "TC_ABOUT_004 - Reliable source section is visible")
    public void tc_about_004_verifyReliableSourceSectionVisible() {

        Assert.assertTrue(
                aboutUsPage.isReliableSourceSectionDisplayed(),
                "Reliable source section must be visible");

        logger.info("TC_ABOUT_004 - Reliable source section verified");
    }

    @Test(description = "TC_ABOUT_005 - Branded products section is visible")
    public void tc_about_005_verifyBrandedProductsSectionVisible() {

        Assert.assertTrue(
                aboutUsPage.isBrandedProductsSectionDisplayed(),
                "Branded products section must be visible");

        logger.info("TC_ABOUT_005 - Branded products section verified");
    }

    @Test(description = "TC_ABOUT_006 - Stock Advisor link is visible")
    public void tc_about_006_verifyStockAdvisorLinkVisible() {

        Assert.assertTrue(
                aboutUsPage.isStockAdvisorLinkDisplayed(),
                "Value Research Stock Advisor link must be visible");

        logger.info("TC_ABOUT_006 - Stock Advisor link visibility verified");
    }

    @Test(description = "TC_ABOUT_007 - Fund Advisor link is visible")
    public void tc_about_007_verifyFundAdvisorLinkVisible() {

        Assert.assertTrue(
                aboutUsPage.isFundAdvisorLinkDisplayed(),
                "Value Research Fund Advisor link must be visible");

        logger.info("TC_ABOUT_007 - Fund Advisor link visibility verified");
    }

    @Test(description = "TC_ABOUT_008 - Independent Advisors link is visible")
    public void tc_about_008_verifyIndependentAdvisorsLinkVisible() {

        Assert.assertTrue(
                aboutUsPage.isIndependentAdvisorsLinkDisplayed(),
                "Independent Advisors link must be visible");

        logger.info("TC_ABOUT_008 - Independent Advisors link visibility verified");
    }

    @Test(description = "TC_ABOUT_009 - Mutual Fund Insight link is visible")
    public void tc_about_009_verifyMutualFundInsightLinkVisible() {

        Assert.assertTrue(
                aboutUsPage.isMutualFundInsightLinkDisplayed(),
                "Mutual Fund Insight link must be visible");

        logger.info("TC_ABOUT_009 - Mutual Fund Insight link visibility verified");
    }

    @Test(description = "TC_ABOUT_010 - Wealth Insight link is visible")
    public void tc_about_010_verifyWealthInsightLinkVisible() {

        Assert.assertTrue(
                aboutUsPage.isWealthInsightLinkDisplayed(),
                "Wealth Insight link must be visible");

        logger.info("TC_ABOUT_010 - Wealth Insight link visibility verified");
    }

    @Test(description = "TC_ABOUT_011 - Analytics Pro link is visible")
    public void tc_about_011_verifyAnalyticsProLinkVisible() {

        Assert.assertTrue(
                aboutUsPage.isAnalyticsProLinkDisplayed(),
                "Value Research Analytics Pro link must be visible");

        logger.info("TC_ABOUT_011 - Analytics Pro link visibility verified");
    }

    @Test(description = "TC_ABOUT_012 - Stock Advisor link opens a valid screen")
    public void tc_about_012_verifyStockAdvisorLinkNavigation() {

        Assert.assertTrue(
                aboutUsPage.tapStockAdvisorAndValidateScreen(),
                "Stock Advisor link must open a valid screen");

        logger.info("TC_ABOUT_012 - Stock Advisor link navigation verified");
    }

    @Test(description = "TC_ABOUT_013 - Fund Advisor link opens a valid screen")
    public void tc_about_013_verifyFundAdvisorLinkNavigation() {

        Assert.assertTrue(
                aboutUsPage.tapFundAdvisorAndValidateScreen(),
                "Fund Advisor link must open a valid screen");

        logger.info("TC_ABOUT_013 - Fund Advisor link navigation verified");
    }

    @Test(description = "TC_ABOUT_014 - Independent Advisors link opens a valid screen")
    public void tc_about_014_verifyIndependentAdvisorsLinkNavigation() {

        Assert.assertTrue(
                aboutUsPage.tapIndependentAdvisorsAndValidateScreen(),
                "Independent Advisors link must open a valid screen");

        logger.info("TC_ABOUT_014 - Independent Advisors link navigation verified");
    }

    @Test(description = "TC_ABOUT_015 - Mutual Fund Insight link opens a valid screen")
    public void tc_about_015_verifyMutualFundInsightLinkNavigation() {

        Assert.assertTrue(
                aboutUsPage.tapMutualFundInsightAndValidateScreen(),
                "Mutual Fund Insight link must open a valid screen");

        logger.info("TC_ABOUT_015 - Mutual Fund Insight link navigation verified");
    }

    @Test(description = "TC_ABOUT_016 - Wealth Insight link opens a valid screen")
    public void tc_about_016_verifyWealthInsightLinkNavigation() {

        Assert.assertTrue(
                aboutUsPage.tapWealthInsightAndValidateScreen(),
                "Wealth Insight link must open a valid screen");

        logger.info("TC_ABOUT_016 - Wealth Insight link navigation verified");
    }

    @Test(description = "TC_ABOUT_017 - Analytics Pro link opens a valid screen")
    public void tc_about_017_verifyAnalyticsProLinkNavigation() {

        Assert.assertTrue(
                aboutUsPage.tapAnalyticsProAndValidateScreen(),
                "Analytics Pro link must open a valid screen");

        logger.info("TC_ABOUT_017 - Analytics Pro link navigation verified");
    }

    @Test(description = "TC_ABOUT_018 - Every About Us link opens a valid screen")
    public void tc_about_018_verifyAllLinksOpenValidScreens() {

        List<LinkResult> results = aboutUsPage.tapAllLinksAndValidateScreens();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one About Us link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "About Us link must open a valid screen: " + result.name);
        }

        logger.info("TC_ABOUT_018 - All About Us link navigation verified: {}", results);
    }

    @Test(description = "TC_ABOUT_019 - Back button navigates away from About Us screen")
    public void tc_about_019_verifyBackButtonNavigation() {

        Assert.assertTrue(
                aboutUsPage.isBackButtonDisplayed(),
                "Back button must be visible before tapping it");

        aboutUsPage.tapBack();

        Assert.assertFalse(
                aboutUsPage.isAboutUsScreenDisplayed(),
                "About Us screen must no longer be displayed after tapping back");

        logger.info("TC_ABOUT_019 - Back button navigation verified");
    }
}
