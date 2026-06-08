package tests;

import java.lang.reflect.Method;
import java.util.List;


import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;
import pages.Home_Page;
import pages.Home_Page.LinkResult;

public class HomePageTest extends BaseTest {

    private Home_Page homePage;
    private DashboardPage dashboardPage;

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        homePage = new Home_Page(getDriver());
        dashboardPage = new DashboardPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        dashboardPage.tapFundsTab();
        homePage.openFundsScreen();

        Assert.assertTrue(
                homePage.isFundsScreenDisplayed(),
                "Funds screen must load before " + method.getName());

        logger.info("Funds screen ready for: {}", method.getName());
    }

    @Test(description = "TC_HOME_001 - Funds screen opens from Funds tab")
    public void tc_home_001_verifyFundsScreenLoads() {

        Assert.assertTrue(
                homePage.isFundsScreenDisplayed(),
                "Funds screen must be displayed");

        logger.info("TC_HOME_001 - Funds screen load verified");
    }

    @Test(description = "TC_HOME_002 - Bottom navigation is visible on Funds screen")
    public void tc_home_002_verifyBottomNavigationVisible() {

        Assert.assertTrue(
                homePage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible on Funds screen");

        logger.info("TC_HOME_002 - Bottom navigation visibility verified");
    }

    @Test(description = "TC_HOME_003 - Rich Future section is visible")
    public void tc_home_003_verifyRichFutureSectionVisible() {

        Assert.assertTrue(
                homePage.isRichFutureSectionDisplayed(),
                "Rich Future section must be visible");

        logger.info("TC_HOME_003 - Rich Future section visibility verified");
    }

    @Test(description = "TC_HOME_004 - Navigate To section is visible")
    public void tc_home_004_verifyNavigateToSectionVisible() {

        Assert.assertTrue(
                homePage.isNavigateToSectionDisplayed(),
                "Navigate To section must be visible");

        logger.info("TC_HOME_004 - Navigate To section visibility verified");
    }

    @Test(description = "TC_HOME_005 - Quick Guides section is visible")
    public void tc_home_005_verifyQuickGuidesSectionVisible() {

        Assert.assertTrue(
                homePage.isQuickGuidesSectionDisplayed(),
                "Quick Guides section must be visible");

        logger.info("TC_HOME_005 - Quick Guides section visibility verified");
    }

    @Test(description = "TC_HOME_006 - Analyst Choice section is visible")
    public void tc_home_006_verifyAnalystChoiceSectionVisible() {

        Assert.assertTrue(
                homePage.isAnalystChoiceSectionDisplayed(),
                "Analyst Choice section must be visible");

        logger.info("TC_HOME_006 - Analyst Choice section visibility verified");
    }

    @Test(description = "TC_HOME_007 - Fund Advisor Note section is visible")
    public void tc_home_007_verifyFundAdvisorNoteSectionVisible() {

        Assert.assertTrue(
                homePage.isFundAdvisorNoteSectionDisplayed(),
                "Fund Advisor Note section must be visible");

        logger.info("TC_HOME_007 - Fund Advisor Note section visibility verified");
    }

    @Test(description = "TC_HOME_008 - Retirement card is visible")
    public void tc_home_008_verifyRetirementCardVisible() {

        Assert.assertTrue(
                homePage.isRetirementCardDisplayed(),
                "Retirement card must be visible");

        logger.info("TC_HOME_008 - Retirement card visibility verified");
    }

    @Test(description = "TC_HOME_009 - Regular Income card is visible")
    public void tc_home_009_verifyRegularIncomeCardVisible() {

        Assert.assertTrue(
                homePage.isRegularIncomeCardDisplayed(),
                "Regular Income card must be visible");

        logger.info("TC_HOME_009 - Regular Income card visibility verified");
    }

    @Test(description = "TC_HOME_010 - No Goal Start Investing card is visible")
    public void tc_home_010_verifyNoGoalStartInvestingCardVisible() {

        Assert.assertTrue(
                homePage.isNoGoalStartInvestingCardDisplayed(),
                "No Goal Start Investing card must be visible");

        logger.info("TC_HOME_010 - No Goal Start Investing card visibility verified");
    }

    @Test(description = "TC_HOME_011 - MF Screener link is visible")
    public void tc_home_011_verifyMfScreenerLinkVisible() {

        Assert.assertTrue(
                homePage.isMfScreenerLinkDisplayed(),
                "MF Screener link must be visible");

        logger.info("TC_HOME_011 - MF Screener link visibility verified");
    }

    @Test(description = "TC_HOME_012 - SIP Calculator link is visible")
    public void tc_home_012_verifySipCalculatorLinkVisible() {

        Assert.assertTrue(
                homePage.isSipCalculatorLinkDisplayed(),
                "SIP Calculator link must be visible");

        logger.info("TC_HOME_012 - SIP Calculator link visibility verified");
    }

    @Test(description = "TC_HOME_013 - Fund Search link is visible")
    public void tc_home_013_verifyFundSearchLinkVisible() {

        Assert.assertTrue(
                homePage.isFundSearchLinkDisplayed(),
                "Fund Search link must be visible");

        logger.info("TC_HOME_013 - Fund Search link visibility verified");
    }

    @Test(description = "TC_HOME_014 - More Quick Guides link is visible")
    public void tc_home_014_verifyMoreQuickGuidesLinkVisible() {

        Assert.assertTrue(
                homePage.isMoreQuickGuidesLinkDisplayed(),
                "More Quick Guides link must be visible");

        logger.info("TC_HOME_014 - More Quick Guides link visibility verified");
    }

    @Test(description = "TC_HOME_015 - Build Investment Plan guide is visible")
    public void tc_home_015_verifyBuildInvestmentPlanGuideVisible() {

        Assert.assertTrue(
                homePage.isBuildInvestmentPlanGuideDisplayed(),
                "Build Investment Plan guide must be visible");

        logger.info("TC_HOME_015 - Build Investment Plan guide visibility verified");
    }

    @Test(description = "TC_HOME_016 - Upgrade Portfolio guide is visible")
    public void tc_home_016_verifyUpgradePortfolioGuideVisible() {

        Assert.assertTrue(
                homePage.isUpgradePortfolioGuideDisplayed(),
                "Upgrade Portfolio guide must be visible");

        logger.info("TC_HOME_016 - Upgrade Portfolio guide visibility verified");
    }

    @Test(description = "TC_HOME_017 - Analyst Picked Funds guide is visible")
    public void tc_home_017_verifyAnalystPickedFundsGuideVisible() {

        Assert.assertTrue(
                homePage.isAnalystPickedFundsGuideDisplayed(),
                "Analyst Picked Funds guide must be visible");

        logger.info("TC_HOME_017 - Analyst Picked Funds guide visibility verified");
    }

    @Test(description = "TC_HOME_018 - Personalised Support card is visible")
    public void tc_home_018_verifyPersonalisedSupportCardVisible() {

        Assert.assertTrue(
                homePage.isPersonalisedSupportCardDisplayed(),
                "Personalised Support card must be visible");

        logger.info("TC_HOME_018 - Personalised Support card visibility verified");
    }

    @Test(description = "TC_HOME_019 - Aggressive Growth card is visible")
    public void tc_home_019_verifyAggressiveGrowthCardVisible() {

        Assert.assertTrue(
                homePage.isAggressiveGrowthCardDisplayed(),
                "Aggressive Growth card must be visible");

        logger.info("TC_HOME_019 - Aggressive Growth card visibility verified");
    }

    @Test(description = "TC_HOME_020 - Retirement card opens a valid screen")
    public void tc_home_020_verifyRetirementCardNavigation() {

        Assert.assertTrue(
                homePage.tapRetirementCardAndValidateScreen(),
                "Retirement card must open a valid screen");

        logger.info("TC_HOME_020 - Retirement card navigation verified");
    }

    @Test(description = "TC_HOME_021 - Regular Income card opens a valid screen")
    public void tc_home_021_verifyRegularIncomeCardNavigation() {

        Assert.assertTrue(
                homePage.tapRegularIncomeCardAndValidateScreen(),
                "Regular Income card must open a valid screen");

        logger.info("TC_HOME_021 - Regular Income card navigation verified");
    }

    @Test(description = "TC_HOME_022 - No Goal Start Investing card opens a valid screen")
    public void tc_home_022_verifyNoGoalStartInvestingNavigation() {

        Assert.assertTrue(
                homePage.tapNoGoalStartInvestingAndValidateScreen(),
                "No Goal Start Investing card must open a valid screen");

        logger.info("TC_HOME_022 - No Goal Start Investing navigation verified");
    }

    @Test(description = "TC_HOME_023 - MF Screener link opens a valid screen")
    public void tc_home_023_verifyMfScreenerNavigation() {

        Assert.assertTrue(
                homePage.tapMfScreenerAndValidateScreen(),
                "MF Screener link must open a valid screen");

        logger.info("TC_HOME_023 - MF Screener navigation verified");
    }

    @Test(description = "TC_HOME_024 - SIP Calculator link opens a valid screen")
    public void tc_home_024_verifySipCalculatorNavigation() {

        Assert.assertTrue(
                homePage.tapSipCalculatorAndValidateScreen(),
                "SIP Calculator link must open a valid screen");

        logger.info("TC_HOME_024 - SIP Calculator navigation verified");
    }

    @Test(description = "TC_HOME_025 - Fund Search link opens a valid screen")
    public void tc_home_025_verifyFundSearchNavigation() {

        Assert.assertTrue(
                homePage.tapFundSearchAndValidateScreen(),
                "Fund Search link must open a valid screen");

        logger.info("TC_HOME_025 - Fund Search navigation verified");
    }

    @Test(description = "TC_HOME_026 - More Quick Guides link opens a valid screen")
    public void tc_home_026_verifyMoreQuickGuidesNavigation() {

        Assert.assertTrue(
                homePage.tapMoreQuickGuidesAndValidateScreen(),
                "More Quick Guides link must open a valid screen");

        logger.info("TC_HOME_026 - More Quick Guides navigation verified");
    }

    @Test(description = "TC_HOME_027 - Build Investment Plan guide opens a valid screen")
    public void tc_home_027_verifyBuildInvestmentPlanNavigation() {

        Assert.assertTrue(
                homePage.tapBuildInvestmentPlanAndValidateScreen(),
                "Build Investment Plan guide must open a valid screen");

        logger.info("TC_HOME_027 - Build Investment Plan guide navigation verified");
    }

    @Test(description = "TC_HOME_028 - Upgrade Portfolio guide opens a valid screen")
    public void tc_home_028_verifyUpgradePortfolioNavigation() {

        Assert.assertTrue(
                homePage.tapUpgradePortfolioAndValidateScreen(),
                "Upgrade Portfolio guide must open a valid screen");

        logger.info("TC_HOME_028 - Upgrade Portfolio guide navigation verified");
    }

    @Test(description = "TC_HOME_029 - Analyst Picked Funds guide opens a valid screen")
    public void tc_home_029_verifyAnalystPickedFundsNavigation() {

        Assert.assertTrue(
                homePage.tapAnalystPickedFundsAndValidateScreen(),
                "Analyst Picked Funds guide must open a valid screen");

        logger.info("TC_HOME_029 - Analyst Picked Funds guide navigation verified");
    }

    @Test(description = "TC_HOME_030 - Personalised Support card opens a valid screen")
    public void tc_home_030_verifyPersonalisedSupportNavigation() {

        Assert.assertTrue(
                homePage.tapPersonalisedSupportAndValidateScreen(),
                "Personalised Support card must open a valid screen");

        logger.info("TC_HOME_030 - Personalised Support card navigation verified");
    }

    @Test(description = "TC_HOME_031 - Aggressive Growth card opens a valid screen")
    public void tc_home_031_verifyAggressiveGrowthNavigation() {

        Assert.assertTrue(
                homePage.tapAggressiveGrowthAndValidateScreen(),
                "Aggressive Growth card must open a valid screen");

        logger.info("TC_HOME_031 - Aggressive Growth card navigation verified");
    }

    @Test(description = "TC_HOME_032 - Every Funds screen link opens a valid screen")
    public void tc_home_032_verifyAllLinksOpenValidScreens() {

        List<LinkResult> results = homePage.tapAllLinksAndValidateScreens();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one Funds screen link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "Funds screen link must open a valid screen: " + result.name);
        }

        logger.info("TC_HOME_032 - All Funds screen link navigation verified: {}", results);
    }
  
}
