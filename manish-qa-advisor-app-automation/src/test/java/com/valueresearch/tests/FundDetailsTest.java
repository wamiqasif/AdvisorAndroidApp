package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.FundDetailsPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
public class FundDetailsTest extends BaseTest {

    private static boolean fundDetailsOpened = false;

    @Test(priority = 1, alwaysRun = true)
    public void FD_001_OpenFundDetailsFromSearch() {
        createExtentTest(
                "FD_001",
                "Open Fund Details from search",
                "Open Funds tab, search HDFC Flexi Cap Fund, and open Fund Details page"
        );

        ReportLogger.step(
                "Starting test case: FD_001 - Open Fund Details from search"
        );

        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        FundDetailsPage page = new FundDetailsPage(driver);

        page.openFundDetailsFromSearch();

        /*
         * Set this immediately after successful navigation.
         * Header validation must not control navigation state.
         */
        fundDetailsOpened = true;

        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" +
                        "FD_001 - Fund Details opened successfully" +
                        "</span>"
        );

        ReportLogger.pass("Completed test case: FD_001");
    }
    @BeforeClass(alwaysRun = true)
    public void resetFundDetailsState() {
        fundDetailsOpened = false;
    }
    @Test(priority = 2, alwaysRun = true)
    public void FD_002_VerifyFundHeader() {
        createExtentTest(
                "FD_002",
                "Verify Fund Details header",
                "Validate fund name, category, NAV, opinion, and visible header values"
        );

        ReportLogger.step("Starting test case: FD_002 - Verify Fund Details header");

        FundDetailsPage page = getFundDetailsPage();
        page.recoverFundDetailsIfNeeded();
        page.verifyFundHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_002 - Fund header validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_002");
    }

    @Test(priority = 3, alwaysRun = true)
    public void FD_003_VerifyInvestmentDetails() {
        createExtentTest(
                "FD_003",
                "Verify Investment Details",
                "Validate Your Investments card and Investment Details child page"
        );

        ReportLogger.step("Starting test case: FD_003 - Verify Investment Details");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyInvestmentDetailsCardAndPage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_003 - Investment Details validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_003");
    }

    @Test(priority = 4, alwaysRun = true)
    public void FD_004_VerifyPortfolioOverlap() {
        createExtentTest(
                "FD_004",
                "Verify Portfolio Overlap",
                "Validate portfolio overlap card and Total Overlap child page"
        );

        ReportLogger.step("Starting test case: FD_004 - Verify Portfolio Overlap");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyPortfolioOverlapCardAndPage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_004 - Portfolio Overlap validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_004");
    }

    @Test(priority = 5, alwaysRun = true)
    public void FD_005_VerifyReturns() {
        createExtentTest(
                "FD_005",
                "Verify Returns section",
                "Validate Returns section and Returns More page"
        );

        ReportLogger.step("Starting test case: FD_005 - Verify Returns section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyReturnsSectionAndMorePage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_005 - Returns section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_005");
    }

    @Test(priority = 6, alwaysRun = true)
    public void FD_006_VerifyRisk() {
        createExtentTest(
                "FD_006",
                "Verify Risk section",
                "Validate Risk section and Risk More page"
        );

        ReportLogger.step("Starting test case: FD_006 - Verify Risk section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyRiskSectionAndMorePage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_006 - Risk section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_006");
    }

    @Test(priority = 7, alwaysRun = true)
    public void FD_007_VerifyWhoShouldInvest() {
        createExtentTest(
                "FD_007",
                "Verify Who should invest section",
                "Validate suitability text and SIP note"
        );

        ReportLogger.step("Starting test case: FD_007 - Verify Who should invest section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyWhoShouldInvestSection();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_007 - Who should invest section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_007");
    }

    @Test(priority = 8, alwaysRun = true)
    public void FD_008_VerifyWhereDoesItInvest() {
        createExtentTest(
                "FD_008",
                "Verify Where does it invest section",
                "Validate Asset Class, Market Cap, Holding, and Holdings More flow"
        );

        ReportLogger.step("Starting test case: FD_008 - Verify Where does it invest section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyWhereDoesItInvestSectionAndHoldingsPage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_008 - Where does it invest section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_008");
    }

    @Test(priority = 9, alwaysRun = true)
    public void FD_009_VerifyFundManager() {
        createExtentTest(
                "FD_009",
                "Verify Fund Manager section",
                "Validate manager section and visible manager details"
        );

        ReportLogger.step("Starting test case: FD_009 - Verify Fund Manager section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyFundManagerSection();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_009 - Fund Manager section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_009");
    }

    @Test(priority = 10, alwaysRun = true)
    public void FD_010_VerifyMoreDetails() {
        createExtentTest(
                "FD_010",
                "Verify More Details section",
                "Validate More Details section and child page"
        );

        ReportLogger.step("Starting test case: FD_010 - Verify More Details section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyMoreDetailsSectionAndChildPage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_010 - More Details section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_010");
    }

    @Test(priority = 11, alwaysRun = true)
    public void FD_011_VerifyNews() {
        createExtentTest(
                "FD_011",
                "Verify News section",
                "Validate News section and News listing page"
        );

        ReportLogger.step("Starting test case: FD_011 - Verify News section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyNewsSectionAndNewsPage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_011 - News section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_011");
    }
    
    @Test(priority = 12, alwaysRun = true)
    public void FD_012_VerifyConsistencyScore() {
        createExtentTest(
                "FD_012",
                "Verify Consistency Score card",
                "Validate paid-user Consistency Score card after Returns section"
        );

        ReportLogger.step("Starting test case: FD_012 - Verify Consistency Score card");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyConsistencyScoreCardForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_012 - Consistency Score validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_012");
    }

    @Test(priority = 13, alwaysRun = true)
    public void FD_013_VerifyWorstOneYearReturnsFootnote() {
        createExtentTest(
                "FD_013",
                "Verify Worst 1 year returns footnote",
                "Validate Worst 1 year returns section and last 10 years footnote for paid user"
        );

        ReportLogger.step("Starting test case: FD_013 - Verify Worst 1 year returns footnote");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyWorstOneYearReturnsFootnoteForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_013 - Worst 1 year returns footnote validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_013");
    }
    
    @Test(priority = 14, alwaysRun = true)
    public void FD_014_VerifyTransactionButtonsVisible() {
        createExtentTest(
                "FD_014",
                "Verify transaction buttons",
                "Validate Redeem and Invest buttons are visible on Fund Details page for paid user"
        );

        ReportLogger.step("Starting test case: FD_014 - Verify transaction buttons");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyTransactionButtonsVisibleForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_014 - Transaction buttons validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_014");
    }
    
    @Test(priority = 15, alwaysRun = true)
    public void FD_015_VerifyFundStyleDescription() {
        createExtentTest(
                "FD_015",
                "Verify Fund Style description",
                "Validate Fund Style label and description on Where does it invest More page for paid user"
        );

        ReportLogger.step("Starting test case: FD_015 - Verify Fund Style description");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyFundStyleDescriptionForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_015 - Fund Style description validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_015");
    }
    
    @Test(priority = 16, alwaysRun = true)
    public void FD_016_VerifyOverlapWithBenchmark() {
        createExtentTest(
                "FD_016",
                "Verify Overlap with benchmark",
                "Validate Overlap with benchmark section on Where does it invest More page for paid user"
        );

        ReportLogger.step("Starting test case: FD_016 - Verify Overlap with benchmark");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyOverlapWithBenchmarkForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_016 - Overlap with benchmark validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_016");
    }
    
    @Test(priority = 17, alwaysRun = true)
    public void FD_017_VerifyConcentrationSection() {
        createExtentTest(
                "FD_017",
                "Verify Concentration section",
                "Validate Concentration section on Where does it invest More page for paid user"
        );

        ReportLogger.step("Starting test case: FD_017 - Verify Concentration section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyConcentrationSectionForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_017 - Concentration section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_017");
    }
    
    @Test(priority = 18, alwaysRun = true)
    public void FD_018_VerifySectorAllocationSection() {
        createExtentTest(
                "FD_018",
                "Verify Sector Allocation section",
                "Validate Sector Allocation section on Where does it invest More page for paid user"
        );

        ReportLogger.step("Starting test case: FD_018 - Verify Sector Allocation section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifySectorAllocationSectionForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_018 - Sector Allocation section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_018");
    }

    @Test(priority = 19, alwaysRun = true)
    public void FD_019_VerifyTopEquityHoldingsSection() {
        createExtentTest(
                "FD_019",
                "Verify Top Equity Holdings section",
                "Validate Top Equity Holdings section on Where does it invest More page for paid user"
        );

        ReportLogger.step("Starting test case: FD_019 - Verify Top Equity Holdings section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyTopEquityHoldingsSectionForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_019 - Top Equity Holdings section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_019");
    }

    @Test(priority = 20, alwaysRun = true)
    public void FD_020_VerifyKeyLegendSection() {
        createExtentTest(
                "FD_020",
                "Verify Key legend section",
                "Validate holding movement Key legend on Where does it invest More page for paid user"
        );

        ReportLogger.step("Starting test case: FD_020 - Verify Key legend section");

        FundDetailsPage page = getFundDetailsPage();
        page.verifyKeyLegendSectionForPaidUser();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_020 - Key legend section validated successfully</span>");
        ReportLogger.pass("Completed test case: FD_020");
    }
    
    @Test(priority = 21, alwaysRun = true)
    public void FD_021_VerifyRedeemInvestButtonsFromPortfolioFunds() {
        createExtentTest(
                "FD_021",
                "Verify Redeem and Invest More buttons from Portfolio funds",
                "Validate Redeem and Invest More buttons for funds present in different investors' Portfolio Funds list"
        );

        ReportLogger.step("Starting test case: FD_021 - Verify Redeem and Invest More buttons from Portfolio funds");

        /*
         * Required for standalone FD_021 run.
         * Do not call getFundDetailsPage() here because it opens HDFC fund from search.
         * Only confirm login/session, then start Portfolio flow directly.
         */
        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        FundDetailsPage page = new FundDetailsPage(driver);
        page.verifyRedeemInvestButtonsFromPortfolioInvestorFunds();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_021 - Redeem and Invest More buttons validated from Portfolio funds</span>");
        ReportLogger.pass("Completed test case: FD_021");
    }
    

    @Test(priority = 22, alwaysRun = true)
    public void FD_022_VerifyPremiumCoverageForWhiteOakMidCapFund() {
        createExtentTest(
                "FD_022",
                "Verify Premium Coverage for WhiteOak Capital Mid Cap Fund",
                "Search WhiteOak Capital Mid Cap Fund and validate Premium Coverage section for paid user"
        );

        ReportLogger.step("Starting test case: FD_022 - Verify Premium Coverage for WhiteOak Capital Mid Cap Fund");

        /*
         * Standalone Premium Coverage test.
         * Do not use getFundDetailsPage() because it opens the default HDFC fund.
         * This test must search the known eligible WhiteOak fund directly.
         */
        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        FundDetailsPage page = new FundDetailsPage(driver);
        page.verifyPremiumCoverageForWhiteOakMidCapFund();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>FD_022 - Premium Coverage validated for WhiteOak Capital Mid Cap Fund</span>");
        ReportLogger.pass("Completed test case: FD_022");
    }

    private FundDetailsPage getFundDetailsPage() {
        FundDetailsPage page = new FundDetailsPage(driver);

        if (!fundDetailsOpened) {
            ReportLogger.step("Fund Details page was not opened by FD_001. Opening it now.");
            new AuthHelper(driver).ensureLoggedIn();
            ReportLogger.pass("Advisor login/session confirmed");
            page.openFundDetailsFromSearch();
            fundDetailsOpened = true;
            return page;
        }

        try {
            page.recoverFundDetailsIfNeeded();
        } catch (Exception recoveryError) {
            ReportLogger.debug("Fund Details recovery failed. Reopening from search. Reason: " + recoveryError.getMessage());
            new AuthHelper(driver).ensureLoggedIn();
            page.openFundDetailsFromSearch();
            fundDetailsOpened = true;
        }

        return page;
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("<b>Validation:</b> " + validation);
        ExtentTestManager.setTest(test);
    }
}