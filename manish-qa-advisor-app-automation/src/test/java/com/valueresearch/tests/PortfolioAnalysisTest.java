package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.PortfolioAnalysisPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class PortfolioAnalysisTest extends BaseTest {

    private static boolean portfolioAnalysisOpened = false;

    @Test(priority = 1)
    public void PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub() {
        createExtentTest("PA_SUM_001", "Open Portfolio Analysis Summary from Hub", "Validate Portfolio Analysis Summary page opens from Hub");
        ReportLogger.step("Starting test case: PA_SUM_001 - Open Portfolio Analysis Summary from Hub");
        PortfolioAnalysisPage page = new PortfolioAnalysisPage(driver);
        ReportLogger.step("Checking Advisor login/session");
        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");
        page.openPortfolioAnalysisFromHub();
        portfolioAnalysisOpened = true;
        page.verifySummaryHeaderAndTabs();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_001 - Portfolio Analysis Summary opened successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_001");
    }

    @Test(priority = 2, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_002_ChangeInvestorToManishKhatri() {
        createExtentTest("PA_SUM_002", "Change Investor to Manish Khatri", "Change investor from Summary page using same dropdown logic as Funds and Stocks");
        ReportLogger.step("Starting test case: PA_SUM_002 - Change Investor to Manish Khatri");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.changeInvestorToManishKhatriFromSummaryPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_002 - Investor changed to Manish Khatri successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_002");
    }

    @Test(priority = 3, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_003_VerifyOverallPortfolioPerformanceCard() {
        createExtentTest("PA_SUM_003", "Verify Overall Portfolio Performance Card", "Validate You vs Market and benchmark dropdown entry");
        ReportLogger.step("Starting test case: PA_SUM_003 - Verify Overall Portfolio Performance Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyOverallPortfolioPerformanceCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_003 - Overall Portfolio Performance validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_003");
    }

    @Test(priority = 4, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_004_VerifyBenchmarkDropdownOptions() {
        createExtentTest("PA_SUM_004", "Verify Benchmark Dropdown Options", "Validate benchmark dropdown options on Summary page");
        ReportLogger.step("Starting test case: PA_SUM_004 - Verify Benchmark Dropdown Options");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyBenchmarkDropdownOptions();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_004 - Benchmark dropdown options validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_004");
    }

    @Test(priority = 5, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_005_VerifyPortfolioProfileCard() {
        createExtentTest("PA_SUM_005", "Verify Portfolio Profile Card", "Validate profile suitability statement");
        ReportLogger.step("Starting test case: PA_SUM_005 - Verify Portfolio Profile Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyPortfolioProfileCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_005 - Portfolio Profile card validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_005");
    }

    @Test(priority = 6, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_006_VerifyPortfolioProfileMoreDetailPage() {
        createExtentTest("PA_SUM_006", "Verify Portfolio Profile More Detail Page", "Validate Portfolio Profile detail page after tapping More");
        ReportLogger.step("Starting test case: PA_SUM_006 - Verify Portfolio Profile More Detail Page");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyPortfolioProfileMoreDetailPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_006 - Portfolio Profile More detail validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_006");
    }

    @Test(priority = 7, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_007_VerifyRetirementProjectionCard() {
        createExtentTest("PA_SUM_007", "Verify Retirement Projection Card", "Validate corpus, monthly investment and retirement year controls");
        ReportLogger.step("Starting test case: PA_SUM_007 - Verify Retirement Projection Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyRetirementProjectionCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_007 - Retirement Projection card validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_007");
    }

    @Test(priority = 8, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_008_VerifyWithdrawalIncomeCard() {
        createExtentTest("PA_SUM_008", "Verify Withdrawal Income Card", "Validate monthly income support and withdrawal rate section");
        ReportLogger.step("Starting test case: PA_SUM_008 - Verify Withdrawal Income Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyWithdrawalIncomeCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_008 - Withdrawal Income card validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_008");
    }

    @Test(priority = 9, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_009_VerifyRiskProfileCard() {
        createExtentTest("PA_SUM_009", "Verify Risk Profile Card", "Validate risk profile details and Update Assessment CTA");
        ReportLogger.step("Starting test case: PA_SUM_009 - Verify Risk Profile Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyRiskProfileCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_009 - Risk Profile card validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_009");
    }

    @Test(priority = 10, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_010_VerifyAssetMixCard() {
        createExtentTest("PA_SUM_010", "Verify Asset Mix Card", "Validate Asset Mix total value and asset rows");
        ReportLogger.step("Starting test case: PA_SUM_010 - Verify Asset Mix Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyAssetMixCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_010 - Asset Mix card validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_010");
    }

    @Test(priority = 11, dependsOnMethods = "PA_SUM_001_OpenPortfolioAnalysisSummaryFromHub")
    public void PA_SUM_011_VerifyAssetMixCompositionSections() {
        createExtentTest("PA_SUM_011", "Verify Asset Mix Composition Sections", "Validate Equity Composition and Debt Composition sections");
        ReportLogger.step("Starting test case: PA_SUM_011 - Verify Asset Mix Composition Sections");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.verifyAssetMixCompositionSections();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_SUM_011 - Asset Mix composition sections validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_SUM_011");
    }

    @Test(priority = 12, dependsOnMethods = "PA_SUM_011_VerifyAssetMixCompositionSections")
    public void PA_FUN_001_NavigateToFundsTab() {
        createExtentTest("PA_FUN_001", "Navigate to Funds Tab", "Navigate from Summary flow to Funds tab");
        ReportLogger.step("Starting test case: PA_FUN_001 - Navigate to Funds Tab");
        PortfolioAnalysisPage page = getCurrentPage();
        page.recoverPortfolioAnalysisIfNeeded();
        page.navigateToFundsTab();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_001 - Funds tab opened successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_001");
    }

    @Test(priority = 13, dependsOnMethods = "PA_FUN_001_NavigateToFundsTab")
    public void PA_FUN_002_ChangeInvestorToLalitKumarKhatri() {
        createExtentTest("PA_FUN_002", "Change Investor to Lalit Kumar Khatri", "Change investor from Funds page using investor dropdown");
        ReportLogger.step("Starting test case: PA_FUN_002 - Change Investor to Lalit Kumar Khatri");
        PortfolioAnalysisPage page = getCurrentPage();
        page.changeInvestorToLalitKumarKhatriFromFundsPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_002 - Investor changed to Lalit Kumar Khatri successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_002");
    }

    @Test(priority = 14, dependsOnMethods = "PA_FUN_002_ChangeInvestorToLalitKumarKhatri")
    public void PA_FUN_003_VerifyFundPortfolioPerformanceCard() {
        createExtentTest("PA_FUN_003", "Verify Fund Portfolio Performance Card", "Validate You vs Market and return comparison on Funds tab");
        ReportLogger.step("Starting test case: PA_FUN_003 - Verify Fund Portfolio Performance Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyFundPortfolioPerformanceCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_003 - Fund Portfolio Performance validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_003");
    }

    @Test(priority = 15, dependsOnMethods = "PA_FUN_002_ChangeInvestorToLalitKumarKhatri")
    public void PA_FUN_004_VerifyFundActionCards() {
        createExtentTest("PA_FUN_004", "Verify Fund Action Cards", "Validate get-rid-of and sell-off fund cards");
        ReportLogger.step("Starting test case: PA_FUN_004 - Verify Fund Action Cards");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyFundActionCards();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_004 - Fund action cards validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_004");
    }

    @Test(priority = 16, dependsOnMethods = "PA_FUN_002_ChangeInvestorToLalitKumarKhatri")
    public void PA_FUN_005_VerifyPortfolioInsightsSection() {
        createExtentTest("PA_FUN_005", "Verify Funds Portfolio Insights Section", "Validate regular-plan and IDCW insights on Funds tab");
        ReportLogger.step("Starting test case: PA_FUN_005 - Verify Funds Portfolio Insights Section");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyFundsPortfolioInsightsSection();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_005 - Funds Portfolio Insights validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_005");
    }

    @Test(priority = 17, dependsOnMethods = "PA_FUN_002_ChangeInvestorToLalitKumarKhatri")
    public void PA_FUN_006_VerifyLiquiditySection() {
        createExtentTest("PA_FUN_006", "Verify Funds Liquidity Section", "Validate liquidity, redeemable funds and loan CTA");
        ReportLogger.step("Starting test case: PA_FUN_006 - Verify Funds Liquidity Section");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyFundsLiquiditySection();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_006 - Funds Liquidity section validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_006");
    }

    @Test(priority = 18, dependsOnMethods = "PA_FUN_002_ChangeInvestorToLalitKumarKhatri")
    public void PA_FUN_007_VerifyLiquidityMoreDetailPage() {
        createExtentTest("PA_FUN_007", "Verify Funds Liquidity More Detail Page", "Validate Funds Liquidity More page sections, fund rows and Sell CTA");
        ReportLogger.step("Starting test case: PA_FUN_007 - Verify Funds Liquidity More Detail Page");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyFundsLiquidityMoreDetailPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_FUN_007 - Funds Liquidity More detail page validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_FUN_007");
    }

@Test(priority = 19, dependsOnMethods = "PA_FUN_007_VerifyLiquidityMoreDetailPage")
    public void PA_STK_001_NavigateToStocksTab() {
        createExtentTest("PA_STK_001", "Prepare Stocks Flow from Funds", "After Funds flow, scroll Funds to top, select Vinit Sharma, then navigate to Stocks tab");
        ReportLogger.step("Starting test case: PA_STK_001 - Prepare Stocks Flow from Funds");
        PortfolioAnalysisPage page = getCurrentPage();
        page.prepareStocksFlowFromFundsPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_STK_001 - Vinit selected from Funds and Stocks tab opened successfully</span>");
        ReportLogger.pass("Completed test case: PA_STK_001");
    }





@Test(priority = 20, dependsOnMethods = "PA_STK_001_NavigateToStocksTab")
    public void PA_STK_002_ChangeInvestorToVinitSharma() {
        createExtentTest("PA_STK_002", "Verify Investor Vinit Sharma", "Verify Vinit Sharma investor is selected on Stocks page");
        ReportLogger.step("Starting test case: PA_STK_002 - Verify Investor Vinit Sharma");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyVinitInvestorOnStocksPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_STK_002 - Vinit Sharma investor verified successfully</span>");
        ReportLogger.pass("Completed test case: PA_STK_002");
    }





    @Test(priority = 21, dependsOnMethods = "PA_STK_002_ChangeInvestorToVinitSharma")
    public void PA_STK_003_VerifyStockPortfolioPerformanceCard() {
        createExtentTest("PA_STK_003", "Verify Stock Portfolio Performance Card", "Validate You vs Market and return comparison on Stocks tab");
        ReportLogger.step("Starting test case: PA_STK_003 - Verify Stock Portfolio Performance Card");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyStockPortfolioPerformanceCard();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_STK_003 - Stock Portfolio Performance validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_STK_003");
    }

    @Test(priority = 22, dependsOnMethods = "PA_STK_002_ChangeInvestorToVinitSharma")
    public void PA_STK_004_VerifyPortfolioInsightsSection() {
        createExtentTest("PA_STK_004", "Verify Stocks Portfolio Insights Section", "Validate dividend received and reinvest insight on Stocks tab");
        ReportLogger.step("Starting test case: PA_STK_004 - Verify Stocks Portfolio Insights Section");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyStocksPortfolioInsightsSection();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_STK_004 - Stocks Portfolio Insights validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_STK_004");
    }

    @Test(priority = 23, dependsOnMethods = "PA_STK_002_ChangeInvestorToVinitSharma")
    public void PA_STK_005_VerifyLiquiditySection() {
        createExtentTest("PA_STK_005", "Verify Stocks Liquidity Section", "Validate sellable stocks and Need Cash CTA");
        ReportLogger.step("Starting test case: PA_STK_005 - Verify Stocks Liquidity Section");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyStocksLiquiditySection();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_STK_005 - Stocks Liquidity section validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_STK_005");
    }

    @Test(priority = 24, dependsOnMethods = "PA_STK_002_ChangeInvestorToVinitSharma")
    public void PA_STK_006_VerifyLiquidityMoreDetailPage() {
        createExtentTest("PA_STK_006", "Verify Stocks Liquidity More Detail Page", "Validate Stocks Liquidity More page and stock rows");
        ReportLogger.step("Starting test case: PA_STK_006 - Verify Stocks Liquidity More Detail Page");
        PortfolioAnalysisPage page = getCurrentPage();
        page.verifyStocksLiquidityMoreDetailPage();
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>PA_STK_006 - Stocks Liquidity More detail page validated successfully</span>");
        ReportLogger.pass("Completed test case: PA_STK_006");
    }

    private PortfolioAnalysisPage getCurrentPage() {
        if (!portfolioAnalysisOpened) {
            throw new IllegalStateException("Portfolio Analysis was not opened. PA_SUM_001 must run before dependent test cases.");
        }
        return new PortfolioAnalysisPage(driver);
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTestManager.setTest(ExtentManager.getExtentReports().createTest(caseId + " - " + title));
        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> Portfolio Analysis<br>"
                        + "<b>Case ID:</b> " + caseId + "<br>"
                        + "<b>Scenario:</b> " + title + "<br>"
                        + "<b>Validation:</b> " + validation
        );
    }
}