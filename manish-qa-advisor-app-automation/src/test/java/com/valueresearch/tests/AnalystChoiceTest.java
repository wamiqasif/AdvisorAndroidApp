package com.valueresearch.tests;

import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.AnalystChoicePage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class AnalystChoiceTest extends BaseTest {

    private static boolean analystChoiceOpened = false;
    private static boolean aggressiveGrowthDetailOpened = false;
    private static boolean growthDetailOpened = false;
    private static boolean taxPlanningDetailOpened = false;
    private static boolean growthInternationalDetailOpened = false;
    private static boolean conservativeGrowthDetailOpened = false;
    private static boolean conservativeGrowthIncomeDetailOpened = false;
    private static boolean coreFixedIncomeDetailOpened = false;
    private static boolean capitalPreservationDetailOpened = false;

    @Test(priority = 1)
    public void AC_001_OpenAnalystChoiceFromHub() {
        createExtentTest("AC_001", "Open Analyst’s Choice from Hub", "Validate Analyst’s Choice listing page opens from Hub");

        ReportLogger.step("Starting test case: AC_001 - Open Analyst’s Choice from Hub");

        AnalystChoicePage page = new AnalystChoicePage(driver);

        ReportLogger.step("Checking Advisor login/session");
        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        page.openAnalystChoiceFromHub();
        analystChoiceOpened = true;

        page.verifyAnalystChoiceHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_001 - Analyst’s Choice opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_001");
    }

    @Test(priority = 2, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_002_VerifyAllRecommendationCards() {
        createExtentTest("AC_002", "Verify All Recommendation Cards", "Validate Analyst’s Choice cards");

        ReportLogger.step("Starting test case: AC_002 - Verify All Recommendation Cards");

        AnalystChoicePage page = getCurrentPage();
        page.recoverAnalystChoiceIfNeeded();
        page.verifyAllRecommendationCards();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_002 - All recommendation cards validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_002");
    }

    @Test(priority = 3, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_003_VerifyPortfolioPlannerCTA() {
        createExtentTest("AC_003", "Verify Portfolio Planner CTA", "Validate bottom Portfolio Planner CTA");

        ReportLogger.step("Starting test case: AC_003 - Verify Portfolio Planner CTA");

        AnalystChoicePage page = getCurrentPage();
        page.recoverAnalystChoiceIfNeeded();
        page.verifyPortfolioPlannerCta();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_003 - Portfolio Planner CTA validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_003");
    }

    @Test(priority = 4, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_004_OpenAggressiveGrowthDetail() {
        createExtentTest("AC_004", "Open Aggressive Growth Detail", "Open Aggressive Growth detail page");

        ReportLogger.step("Starting test case: AC_004 - Open Aggressive Growth Detail");

        AnalystChoicePage page = getCurrentPage();
        page.recoverAnalystChoiceIfNeeded();
        page.openAggressiveGrowthDetail();

        aggressiveGrowthDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_004 - Aggressive Growth detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_004");
    }

    @Test(priority = 5, dependsOnMethods = "AC_004_OpenAggressiveGrowthDetail")
    public void AC_005_VerifyAggressiveGrowthDetailHeader() {
        createExtentTest("AC_005", "Verify Aggressive Growth Detail Header", "Validate Aggressive Growth header");

        ReportLogger.step("Starting test case: AC_005 - Verify Aggressive Growth Detail Header");

        getAggressiveGrowthDetailPage().verifyAggressiveGrowthDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_005 - Aggressive Growth detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_005");
    }

    @Test(priority = 6, dependsOnMethods = "AC_004_OpenAggressiveGrowthDetail")
    public void AC_006_VerifyAggressiveGrowthInitialTable() {
        createExtentTest("AC_006", "Verify Aggressive Growth Initial Table", "Validate Aggressive Growth initial table");

        ReportLogger.step("Starting test case: AC_006 - Verify Aggressive Growth Initial Table");

        getAggressiveGrowthDetailPage().verifyAggressiveGrowthInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_006 - Aggressive Growth initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_006");
    }

    @Test(priority = 7, dependsOnMethods = "AC_004_OpenAggressiveGrowthDetail")
    public void AC_007_VerifyAggressiveGrowthCompleteTableCoverage() {
        createExtentTest("AC_007", "Verify Aggressive Growth Complete Table Coverage", "Validate Aggressive Growth table coverage");

        ReportLogger.step("Starting test case: AC_007 - Verify Aggressive Growth Complete Table Coverage");

        getAggressiveGrowthDetailPage().verifyAggressiveGrowthCompleteTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_007 - Aggressive Growth complete table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_007");
    }

    @Test(priority = 8, dependsOnMethods = "AC_004_OpenAggressiveGrowthDetail")
    public void AC_008_VerifyAggressiveGrowthHiddenTableColumnsCurrentState() {
        createExtentTest("AC_008", "Verify Aggressive Growth Hidden Table Columns Current State", "Validate Aggressive Growth hidden columns");

        ReportLogger.step("Starting test case: AC_008 - Verify Aggressive Growth Hidden Table Columns Current State");

        getAggressiveGrowthDetailPage().verifyAggressiveGrowthHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_008 - Aggressive Growth hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_008");
    }

    @Test(priority = 9, dependsOnMethods = "AC_004_OpenAggressiveGrowthDetail")
    public void AC_009_VerifyAggressiveGrowthCompareInScreenerButton() {
        createExtentTest("AC_009", "Verify Aggressive Growth Compare in Screener Button", "Validate Compare in screener button");

        ReportLogger.step("Starting test case: AC_009 - Verify Aggressive Growth Compare in Screener Button");

        getAggressiveGrowthDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_009 - Aggressive Growth Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_009");
    }

    @Test(priority = 10, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_010_OpenGrowthDetail() {
        createExtentTest("AC_010", "Open Growth Detail", "Open Growth detail page");

        ReportLogger.step("Starting test case: AC_010 - Open Growth Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openGrowthDetail();

        growthDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_010 - Growth detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_010");
    }

    @Test(priority = 11, dependsOnMethods = "AC_010_OpenGrowthDetail")
    public void AC_011_VerifyGrowthDetailHeader() {
        createExtentTest("AC_011", "Verify Growth Detail Header", "Validate Growth header");

        ReportLogger.step("Starting test case: AC_011 - Verify Growth Detail Header");

        getGrowthDetailPage().verifyGrowthDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_011 - Growth detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_011");
    }

    @Test(priority = 12, dependsOnMethods = "AC_010_OpenGrowthDetail")
    public void AC_012_VerifyGrowthInitialTable() {
        createExtentTest("AC_012", "Verify Growth Initial Table", "Validate Growth initial table");

        ReportLogger.step("Starting test case: AC_012 - Verify Growth Initial Table");

        getGrowthDetailPage().verifyGrowthInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_012 - Growth initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_012");
    }

    @Test(priority = 13, dependsOnMethods = "AC_010_OpenGrowthDetail")
    public void AC_013_VerifyGrowthCompleteTableCoverage() {
        createExtentTest("AC_013", "Verify Growth Complete Table Coverage", "Validate Growth table coverage");

        ReportLogger.step("Starting test case: AC_013 - Verify Growth Complete Table Coverage");

        getGrowthDetailPage().verifyGrowthCompleteTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_013 - Growth complete table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_013");
    }

    @Test(priority = 14, dependsOnMethods = "AC_010_OpenGrowthDetail")
    public void AC_014_VerifyGrowthHiddenTableColumnsCurrentState() {
        createExtentTest("AC_014", "Verify Growth Hidden Table Columns Current State", "Validate Growth hidden columns");

        ReportLogger.step("Starting test case: AC_014 - Verify Growth Hidden Table Columns Current State");

        getGrowthDetailPage().verifyGrowthHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_014 - Growth hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_014");
    }

    @Test(priority = 15, dependsOnMethods = "AC_010_OpenGrowthDetail")
    public void AC_015_VerifyGrowthCompareInScreenerButton() {
        createExtentTest("AC_015", "Verify Growth Compare in Screener Button", "Validate Growth Compare in screener button");

        ReportLogger.step("Starting test case: AC_015 - Verify Growth Compare in Screener Button");

        getGrowthDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_015 - Growth Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_015");
    }

    @Test(priority = 16, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_016_OpenTaxPlanningDetail() {
        createExtentTest("AC_016", "Open Tax Planning Detail", "Open Tax Planning detail page");

        ReportLogger.step("Starting test case: AC_016 - Open Tax Planning Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openTaxPlanningDetail();

        taxPlanningDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_016 - Tax Planning detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_016");
    }

    @Test(priority = 17, dependsOnMethods = "AC_016_OpenTaxPlanningDetail")
    public void AC_017_VerifyTaxPlanningDetailHeader() {
        createExtentTest("AC_017", "Verify Tax Planning Detail Header", "Validate Tax Planning header");

        ReportLogger.step("Starting test case: AC_017 - Verify Tax Planning Detail Header");

        getTaxPlanningDetailPage().verifyTaxPlanningDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_017 - Tax Planning detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_017");
    }

    @Test(priority = 18, dependsOnMethods = "AC_016_OpenTaxPlanningDetail")
    public void AC_018_VerifyTaxPlanningInitialTable() {
        createExtentTest("AC_018", "Verify Tax Planning Initial Table", "Validate Tax Planning initial table");

        ReportLogger.step("Starting test case: AC_018 - Verify Tax Planning Initial Table");

        getTaxPlanningDetailPage().verifyTaxPlanningInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_018 - Tax Planning initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_018");
    }

    @Test(priority = 19, dependsOnMethods = "AC_016_OpenTaxPlanningDetail")
    public void AC_019_VerifyTaxPlanningCompleteTableCoverage() {
        createExtentTest("AC_019", "Verify Tax Planning Complete Table Coverage", "Validate Tax Planning table coverage");

        ReportLogger.step("Starting test case: AC_019 - Verify Tax Planning Complete Table Coverage");

        getTaxPlanningDetailPage().verifyTaxPlanningCompleteTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_019 - Tax Planning complete table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_019");
    }

    @Test(priority = 20, dependsOnMethods = "AC_019_VerifyTaxPlanningCompleteTableCoverage")
    public void AC_020_VerifyTaxPlanningHiddenTableColumnsCurrentState() {
        createExtentTest("AC_020", "Verify Tax Planning Hidden Table Columns Current State", "Validate Tax Planning hidden columns");

        ReportLogger.step("Starting test case: AC_020 - Verify Tax Planning Hidden Table Columns Current State");

        getTaxPlanningDetailPage().verifyTaxPlanningHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_020 - Tax Planning hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_020");
    }

    @Test(priority = 21, dependsOnMethods = "AC_016_OpenTaxPlanningDetail")
    public void AC_021_VerifyTaxPlanningCompareInScreenerButton() {
        createExtentTest("AC_021", "Verify Tax Planning Compare in Screener Button", "Validate Tax Planning Compare in screener button");

        ReportLogger.step("Starting test case: AC_021 - Verify Tax Planning Compare in Screener Button");

        getTaxPlanningDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_021 - Tax Planning Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_021");
    }

    @Test(priority = 22, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_022_OpenGrowthInternationalDetail() {
        createExtentTest("AC_022", "Open Growth International Detail", "Open Growth - International detail page");

        ReportLogger.step("Starting test case: AC_022 - Open Growth International Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openGrowthInternationalDetail();

        growthInternationalDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_022 - Growth International detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_022");
    }

    @Test(priority = 23, dependsOnMethods = "AC_022_OpenGrowthInternationalDetail")
    public void AC_023_VerifyGrowthInternationalDetailHeader() {
        createExtentTest("AC_023", "Verify Growth International Detail Header", "Validate Growth International header");

        ReportLogger.step("Starting test case: AC_023 - Verify Growth International Detail Header");

        getGrowthInternationalDetailPage().verifyGrowthInternationalDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_023 - Growth International header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_023");
    }

    @Test(priority = 24, dependsOnMethods = "AC_022_OpenGrowthInternationalDetail")
    public void AC_024_VerifyGrowthInternationalEmptyState() {
        createExtentTest("AC_024", "Verify Growth International Empty State", "Validate Growth International no recommendation message");

        ReportLogger.step("Starting test case: AC_024 - Verify Growth International Empty State");

        getGrowthInternationalDetailPage().verifyGrowthInternationalEmptyState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_024 - Growth International empty state validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_024");
    }

    @Test(priority = 25, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_025_OpenConservativeGrowthDetail() {
        createExtentTest("AC_025", "Open Conservative Growth Detail", "Open Conservative Growth detail page");

        ReportLogger.step("Starting test case: AC_025 - Open Conservative Growth Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openConservativeGrowthDetail();

        conservativeGrowthDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_025 - Conservative Growth detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_025");
    }

    @Test(priority = 26, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_026_VerifyConservativeGrowthDetailHeader() {
        createExtentTest("AC_026", "Verify Conservative Growth Detail Header", "Validate Conservative Growth header");

        ReportLogger.step("Starting test case: AC_026 - Verify Conservative Growth Detail Header");

        getConservativeGrowthDetailPage().verifyConservativeGrowthDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_026 - Conservative Growth detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_026");
    }

    @Test(priority = 27, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_027_VerifyConservativeGrowthInitialTable() {
        createExtentTest("AC_027", "Verify Conservative Growth Initial Table", "Validate Conservative Growth initial table");

        ReportLogger.step("Starting test case: AC_027 - Verify Conservative Growth Initial Table");

        getConservativeGrowthDetailPage().verifyConservativeGrowthInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_027 - Conservative Growth initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_027");
    }

    @Test(priority = 28, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_028A_VerifyConservativeGrowthHybridFundsTableCoverage() {
        createExtentTest("AC_028A", "Verify Conservative Growth Hybrid Funds", "Validate Conservative Growth hybrid funds table coverage");

        ReportLogger.step("Starting test case: AC_028A - Verify Conservative Growth Hybrid Funds");

        getConservativeGrowthDetailPage().verifyConservativeGrowthHybridFundsTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_028A - Conservative Growth hybrid funds validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_028A");
    }

    @Test(priority = 29, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_028B_VerifyConservativeGrowthLargeCapFundsTableCoverage() {
        createExtentTest("AC_028B", "Verify Conservative Growth Large Cap Funds", "Validate Conservative Growth large cap funds table coverage");

        ReportLogger.step("Starting test case: AC_028B - Verify Conservative Growth Large Cap Funds");

        getConservativeGrowthDetailPage().verifyConservativeGrowthLargeCapFundsTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_028B - Conservative Growth large cap funds validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_028B");
    }

    @Test(priority = 30, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_028C_VerifyConservativeGrowthEtfIndexFundsTableCoverage() {
        createExtentTest("AC_028C", "Verify Conservative Growth ETF/Index Funds", "Validate Conservative Growth ETF/Index funds table coverage");

        ReportLogger.step("Starting test case: AC_028C - Verify Conservative Growth ETF/Index Funds");

        getConservativeGrowthDetailPage().verifyConservativeGrowthEtfIndexFundsTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_028C - Conservative Growth ETF/Index funds validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_028C");
    }

    @Test(priority = 31, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_029_VerifyConservativeGrowthHiddenTableColumnsCurrentState() {
        createExtentTest("AC_029", "Verify Conservative Growth Hidden Table Columns Current State", "Validate Conservative Growth hidden columns");

        ReportLogger.step("Starting test case: AC_029 - Verify Conservative Growth Hidden Table Columns Current State");

        getConservativeGrowthDetailPage().verifyConservativeGrowthHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_029 - Conservative Growth hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_029");
    }

    @Test(priority = 32, dependsOnMethods = "AC_025_OpenConservativeGrowthDetail")
    public void AC_030_VerifyConservativeGrowthCompareInScreenerButton() {
        createExtentTest("AC_030", "Verify Conservative Growth Compare in Screener Button", "Validate Conservative Growth Compare in screener button");

        ReportLogger.step("Starting test case: AC_030 - Verify Conservative Growth Compare in Screener Button");

        getConservativeGrowthDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_030 - Conservative Growth Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_030");
    }


    @Test(priority = 33, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_031_OpenConservativeGrowthIncomeDetail() {
        createExtentTest("AC_031", "Open Conservative Growth & Income Detail", "Open Conservative Growth & Income detail page");

        ReportLogger.step("Starting test case: AC_031 - Open Conservative Growth & Income Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openConservativeGrowthIncomeDetail();

        conservativeGrowthIncomeDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_031 - Conservative Growth & Income detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_031");
    }

    @Test(priority = 34, dependsOnMethods = "AC_031_OpenConservativeGrowthIncomeDetail")
    public void AC_032_VerifyConservativeGrowthIncomeDetailHeader() {
        createExtentTest("AC_032", "Verify Conservative Growth & Income Detail Header", "Validate Conservative Growth & Income header");

        ReportLogger.step("Starting test case: AC_032 - Verify Conservative Growth & Income Detail Header");

        getConservativeGrowthIncomeDetailPage().verifyConservativeGrowthIncomeDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_032 - Conservative Growth & Income detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_032");
    }

    @Test(priority = 35, dependsOnMethods = "AC_031_OpenConservativeGrowthIncomeDetail")
    public void AC_033_VerifyConservativeGrowthIncomeInitialTable() {
        createExtentTest("AC_033", "Verify Conservative Growth & Income Initial Table", "Validate Conservative Growth & Income initial table");

        ReportLogger.step("Starting test case: AC_033 - Verify Conservative Growth & Income Initial Table");

        getConservativeGrowthIncomeDetailPage().verifyConservativeGrowthIncomeInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_033 - Conservative Growth & Income initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_033");
    }

    @Test(priority = 36, dependsOnMethods = "AC_031_OpenConservativeGrowthIncomeDetail")
    public void AC_034_VerifyConservativeGrowthIncomeCompleteTableCoverage() {
        createExtentTest("AC_034", "Verify Conservative Growth & Income Complete Table Coverage", "Validate Conservative Growth & Income table coverage");

        ReportLogger.step("Starting test case: AC_034 - Verify Conservative Growth & Income Complete Table Coverage");

        getConservativeGrowthIncomeDetailPage().verifyConservativeGrowthIncomeCompleteTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_034 - Conservative Growth & Income complete table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_034");
    }

    @Test(priority = 37, dependsOnMethods = "AC_031_OpenConservativeGrowthIncomeDetail")
    public void AC_035_VerifyConservativeGrowthIncomeHiddenTableColumnsCurrentState() {
        createExtentTest("AC_035", "Verify Conservative Growth & Income Hidden Table Columns Current State", "Validate Conservative Growth & Income hidden columns");

        ReportLogger.step("Starting test case: AC_035 - Verify Conservative Growth & Income Hidden Table Columns Current State");

        getConservativeGrowthIncomeDetailPage().verifyConservativeGrowthIncomeHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_035 - Conservative Growth & Income hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_035");
    }

    @Test(priority = 38, dependsOnMethods = "AC_031_OpenConservativeGrowthIncomeDetail")
    public void AC_036_VerifyConservativeGrowthIncomeCompareInScreenerButton() {
        createExtentTest("AC_036", "Verify Conservative Growth & Income Compare in Screener Button", "Validate Conservative Growth & Income Compare in screener button");

        ReportLogger.step("Starting test case: AC_036 - Verify Conservative Growth & Income Compare in Screener Button");

        getConservativeGrowthIncomeDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_036 - Conservative Growth & Income Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_036");
    }

    @Test(priority = 39, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_037_OpenCoreFixedIncomeDetail() {
        createExtentTest("AC_037", "Open Core Fixed Income Detail", "Open Core Fixed Income detail page");

        ReportLogger.step("Starting test case: AC_037 - Open Core Fixed Income Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openCoreFixedIncomeDetail();

        coreFixedIncomeDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_037 - Core Fixed Income detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_037");
    }

    @Test(priority = 40, dependsOnMethods = "AC_037_OpenCoreFixedIncomeDetail")
    public void AC_038_VerifyCoreFixedIncomeDetailHeader() {
        createExtentTest("AC_038", "Verify Core Fixed Income Detail Header", "Validate Core Fixed Income header");

        ReportLogger.step("Starting test case: AC_038 - Verify Core Fixed Income Detail Header");

        getCoreFixedIncomeDetailPage().verifyCoreFixedIncomeDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_038 - Core Fixed Income detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_038");
    }

    @Test(priority = 41, dependsOnMethods = "AC_037_OpenCoreFixedIncomeDetail")
    public void AC_039_VerifyCoreFixedIncomeInitialTable() {
        createExtentTest("AC_039", "Verify Core Fixed Income Initial Table", "Validate Core Fixed Income initial table");

        ReportLogger.step("Starting test case: AC_039 - Verify Core Fixed Income Initial Table");

        getCoreFixedIncomeDetailPage().verifyCoreFixedIncomeInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_039 - Core Fixed Income initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_039");
    }

    @Test(priority = 42, dependsOnMethods = "AC_037_OpenCoreFixedIncomeDetail")
    public void AC_040_VerifyCoreFixedIncomeCompleteTableCoverage() {
        createExtentTest("AC_040", "Verify Core Fixed Income Complete Table Coverage", "Validate Core Fixed Income table coverage");

        ReportLogger.step("Starting test case: AC_040 - Verify Core Fixed Income Complete Table Coverage");

        getCoreFixedIncomeDetailPage().verifyCoreFixedIncomeCompleteTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_040 - Core Fixed Income complete table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_040");
    }

    @Test(priority = 43, dependsOnMethods = "AC_037_OpenCoreFixedIncomeDetail")
    public void AC_041_VerifyCoreFixedIncomeHiddenTableColumnsCurrentState() {
        createExtentTest("AC_041", "Verify Core Fixed Income Hidden Table Columns Current State", "Validate Core Fixed Income hidden columns");

        ReportLogger.step("Starting test case: AC_041 - Verify Core Fixed Income Hidden Table Columns Current State");

        getCoreFixedIncomeDetailPage().verifyCoreFixedIncomeHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_041 - Core Fixed Income hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_041");
    }

    @Test(priority = 44, dependsOnMethods = "AC_037_OpenCoreFixedIncomeDetail")
    public void AC_042_VerifyCoreFixedIncomeCompareInScreenerButton() {
        createExtentTest("AC_042", "Verify Core Fixed Income Compare in Screener Button", "Validate Core Fixed Income Compare in screener button");

        ReportLogger.step("Starting test case: AC_042 - Verify Core Fixed Income Compare in Screener Button");

        getCoreFixedIncomeDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_042 - Core Fixed Income Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_042");
    }


    @Test(priority = 45, dependsOnMethods = "AC_001_OpenAnalystChoiceFromHub")
    public void AC_043_OpenCapitalPreservationDetail() {
        createExtentTest("AC_043", "Open Capital Preservation Detail", "Open Capital Preservation detail page");

        ReportLogger.step("Starting test case: AC_043 - Open Capital Preservation Detail");

        AnalystChoicePage page = getCurrentPage();
        page.openCapitalPreservationDetail();

        capitalPreservationDetailOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_043 - Capital Preservation detail opened successfully</span>");
        ReportLogger.pass("Completed test case: AC_043");
    }

    @Test(priority = 46, dependsOnMethods = "AC_043_OpenCapitalPreservationDetail")
    public void AC_044_VerifyCapitalPreservationDetailHeader() {
        createExtentTest("AC_044", "Verify Capital Preservation Detail Header", "Validate Capital Preservation header");

        ReportLogger.step("Starting test case: AC_044 - Verify Capital Preservation Detail Header");

        getCapitalPreservationDetailPage().verifyCapitalPreservationDetailHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_044 - Capital Preservation detail header validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_044");
    }

    @Test(priority = 47, dependsOnMethods = "AC_043_OpenCapitalPreservationDetail")
    public void AC_045_VerifyCapitalPreservationInitialTable() {
        createExtentTest("AC_045", "Verify Capital Preservation Initial Table", "Validate Capital Preservation initial table");

        ReportLogger.step("Starting test case: AC_045 - Verify Capital Preservation Initial Table");

        getCapitalPreservationDetailPage().verifyCapitalPreservationInitialTableColumnsAndRows();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_045 - Capital Preservation initial table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_045");
    }

    @Test(priority = 48, dependsOnMethods = "AC_043_OpenCapitalPreservationDetail")
    public void AC_046_VerifyCapitalPreservationCompleteTableCoverage() {
        createExtentTest("AC_046", "Verify Capital Preservation Complete Table Coverage", "Validate Capital Preservation table coverage");

        ReportLogger.step("Starting test case: AC_046 - Verify Capital Preservation Complete Table Coverage");

        getCapitalPreservationDetailPage().verifyCapitalPreservationCompleteTableCoverage();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_046 - Capital Preservation complete table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_046");
    }

    @Test(priority = 49, dependsOnMethods = "AC_043_OpenCapitalPreservationDetail")
    public void AC_047_VerifyCapitalPreservationHiddenTableColumnsCurrentState() {
        createExtentTest("AC_047", "Verify Capital Preservation Hidden Table Columns Current State", "Validate Capital Preservation hidden columns");

        ReportLogger.step("Starting test case: AC_047 - Verify Capital Preservation Hidden Table Columns Current State");

        getCapitalPreservationDetailPage().verifyCapitalPreservationHiddenTableColumnsCurrentState();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_047 - Capital Preservation hidden table validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_047");
    }

    @Test(priority = 50, dependsOnMethods = "AC_043_OpenCapitalPreservationDetail")
    public void AC_048_VerifyCapitalPreservationCompareInScreenerButton() {
        createExtentTest("AC_048", "Verify Capital Preservation Compare in Screener Button", "Validate Capital Preservation Compare in screener button");

        ReportLogger.step("Starting test case: AC_048 - Verify Capital Preservation Compare in Screener Button");

        getCapitalPreservationDetailPage().verifyCompareInScreenerButton();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>AC_048 - Capital Preservation Compare in screener validated successfully</span>");
        ReportLogger.pass("Completed test case: AC_048");
    }

    private AnalystChoicePage getCurrentPage() {
        if (!analystChoiceOpened) {
            throw new IllegalStateException("Analyst’s Choice was not opened. AC_001 must run first.");
        }
        return new AnalystChoicePage(driver);
    }

    private AnalystChoicePage getAggressiveGrowthDetailPage() {
        if (!aggressiveGrowthDetailOpened) {
            throw new IllegalStateException("Aggressive Growth detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }

    private AnalystChoicePage getGrowthDetailPage() {
        if (!growthDetailOpened) {
            throw new IllegalStateException("Growth detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }

    private AnalystChoicePage getTaxPlanningDetailPage() {
        if (!taxPlanningDetailOpened) {
            throw new IllegalStateException("Tax Planning detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }

    private AnalystChoicePage getGrowthInternationalDetailPage() {
        if (!growthInternationalDetailOpened) {
            throw new IllegalStateException("Growth International detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }

    private AnalystChoicePage getConservativeGrowthDetailPage() {
        if (!conservativeGrowthDetailOpened) {
            throw new IllegalStateException("Conservative Growth detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }


    private AnalystChoicePage getConservativeGrowthIncomeDetailPage() {
        if (!conservativeGrowthIncomeDetailOpened) {
            throw new IllegalStateException("Conservative Growth & Income detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }

    private AnalystChoicePage getCoreFixedIncomeDetailPage() {
        if (!coreFixedIncomeDetailOpened) {
            throw new IllegalStateException("Core Fixed Income detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }


    private AnalystChoicePage getCapitalPreservationDetailPage() {
        if (!capitalPreservationDetailOpened) {
            throw new IllegalStateException("Capital Preservation detail was not opened.");
        }
        return new AnalystChoicePage(driver);
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTestManager.setTest(ExtentManager.getExtentReports().createTest(caseId + " - " + title));

        ExtentTestManager.getTest().log(
                Status.INFO,
                "<b>Module:</b> Analyst’s Choice<br>"
                        + "<b>Case ID:</b> " + caseId + "<br>"
                        + "<b>Scenario:</b> " + title + "<br>"
                        + "<b>Validation:</b> " + validation
        );
    }
}