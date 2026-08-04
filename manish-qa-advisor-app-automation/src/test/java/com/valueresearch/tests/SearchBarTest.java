package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.SearchBarPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class SearchBarTest extends BaseTest {

    @Test(priority = 1)
    public void SB_001_VerifySearchBarFundsAndStocksFlow() {
        createExtentTest(
                "SB_001",
                "Verify global search bar funds and stocks flow",
                "Open global search from dashboard, search HDFC, validate tabs, open a mutual fund result, return to search, open a stock result, and validate details pages"
        );

        ReportLogger.step("Starting test case: SB_001 - Verify global search bar funds and stocks flow");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            ReportLogger.step("SEARCH STEP 01 - Capture Advisor app package");
            searchBarPage.captureAdvisorAppPackageForSearch();
            ReportLogger.pass("SEARCH STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("SEARCH STEP 02 - Check Advisor app login/session");
            searchBarPage.ensureAdvisorAppLoggedInForSearch();
            ReportLogger.pass("SEARCH STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("SEARCH STEP 03 - Open global search from dashboard/home");
            searchBarPage.openGlobalSearchFromDashboardForSearch();
            ReportLogger.pass("SEARCH STEP 03 PASSED - Global search opened");

            ReportLogger.step("SEARCH STEP 04 - Validate search screen structure");
            searchBarPage.validateSearchScreenStructureForSearch();
            ReportLogger.pass("SEARCH STEP 04 PASSED - Search screen structure validated");

            ReportLogger.step("SEARCH STEP 05 - Enter HDFC search keyword");
            searchBarPage.enterSearchKeywordForSearch();
            ReportLogger.pass("SEARCH STEP 05 PASSED - Search keyword entered");

            ReportLogger.step("SEARCH STEP 06 - Validate search results loaded");
            searchBarPage.validateSearchResultsLoadedForSearch();
            ReportLogger.pass("SEARCH STEP 06 PASSED - Search results loaded");

            ReportLogger.step("SEARCH STEP 07 - Open Mutual Funds/SIPs tab");
            searchBarPage.openMutualFundsTabForSearch();
            ReportLogger.pass("SEARCH STEP 07 PASSED - Mutual Funds/SIPs tab opened");

            ReportLogger.step("SEARCH STEP 08 - Open fund result from search");
            searchBarPage.openFundResultForSearch();
            ReportLogger.pass("SEARCH STEP 08 PASSED - Fund result opened");

            ReportLogger.step("SEARCH STEP 09 - Validate fund details page");
            searchBarPage.validateFundDetailsOpenedForSearch();
            ReportLogger.pass("SEARCH STEP 09 PASSED - Fund details validated");

            ReportLogger.step("SEARCH STEP 10 - Return to search results after fund details");
            searchBarPage.returnBackToSearchResultsAfterFundForSearch();
            ReportLogger.pass("SEARCH STEP 10 PASSED - Returned to search results");

            ReportLogger.step("SEARCH STEP 11 - Open Stocks tab");
            searchBarPage.openStocksTabForSearch();
            ReportLogger.pass("SEARCH STEP 11 PASSED - Stocks tab opened");

            ReportLogger.step("SEARCH STEP 12 - Open stock result from search");
            searchBarPage.openStockResultForSearch();
            ReportLogger.pass("SEARCH STEP 12 PASSED - Stock result opened");

            ReportLogger.step("SEARCH STEP 13 - Validate stock details page");
            searchBarPage.validateStockDetailsOpenedForSearch();
            ReportLogger.pass("SEARCH STEP 13 PASSED - Stock details validated");

            markPassed("SB_001 - Global search bar funds and stocks flow validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_001");
        }
    }

    @Test(priority = 2)
    public void SB_002_VerifySearchScreenStructure() {
        createExtentTest(
                "SB_002",
                "Verify search screen structure",
                "Open global search and validate search input, All tab, Mutual Funds/SIPs tab, and Stocks tab"
        );

        ReportLogger.step("Starting test case: SB_002 - Verify search screen structure");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareSearchScreen(searchBarPage, "SB_002");

            markPassed("SB_002 - Search screen structure validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_002");
        }
    }

    @Test(priority = 3)
    public void SB_003_VerifyHDFCSearchResultsLoadedOnAllTab() {
        createExtentTest(
                "SB_003",
                "Verify HDFC search results load on All tab",
                "Open search, enter HDFC keyword, and validate that HDFC related search results are loaded"
        );

        ReportLogger.step("Starting test case: SB_003 - Verify HDFC search results loaded on All tab");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareHdfcSearchResults(searchBarPage, "SB_003");

            markPassed("SB_003 - HDFC search results loaded successfully on All tab");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_003");
        }
    }

    @Test(priority = 4)
    public void SB_004_VerifyMutualFundsTabResultsLoaded() {
        createExtentTest(
                "SB_004",
                "Verify Mutual Funds/SIPs tab results",
                "Open search, enter HDFC keyword, switch to Mutual Funds/SIPs tab, and validate fund results are visible"
        );

        ReportLogger.step("Starting test case: SB_004 - Verify Mutual Funds/SIPs tab results");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareHdfcSearchResults(searchBarPage, "SB_004");

            ReportLogger.step("SB_004 STEP 05 - Open Mutual Funds/SIPs tab");
            searchBarPage.openMutualFundsTabForSearch();
            ReportLogger.pass("SB_004 STEP 05 PASSED - Mutual Funds/SIPs tab opened with HDFC results");

            markPassed("SB_004 - Mutual Funds/SIPs search results validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_004");
        }
    }

    @Test(priority = 5)
    public void SB_005_VerifyFundResultDetailsAndBackNavigation() {
        createExtentTest(
                "SB_005",
                "Verify fund result details and back navigation",
                "Open HDFC fund result from Mutual Funds/SIPs tab, validate fund details, and verify user returns to search results after back"
        );

        ReportLogger.step("Starting test case: SB_005 - Verify fund result details and back navigation");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareHdfcSearchResults(searchBarPage, "SB_005");

            ReportLogger.step("SB_005 STEP 05 - Open Mutual Funds/SIPs tab");
            searchBarPage.openMutualFundsTabForSearch();
            ReportLogger.pass("SB_005 STEP 05 PASSED - Mutual Funds/SIPs tab opened");

            ReportLogger.step("SB_005 STEP 06 - Open fund result");
            searchBarPage.openFundResultForSearch();
            ReportLogger.pass("SB_005 STEP 06 PASSED - Fund result opened");

            ReportLogger.step("SB_005 STEP 07 - Validate fund details");
            searchBarPage.validateFundDetailsOpenedForSearch();
            ReportLogger.pass("SB_005 STEP 07 PASSED - Fund details validated");

            ReportLogger.step("SB_005 STEP 08 - Return back to search results after fund details");
            searchBarPage.returnBackToSearchResultsAfterFundForSearch();
            ReportLogger.pass("SB_005 STEP 08 PASSED - Back navigation from fund details returned to search results");

            markPassed("SB_005 - Fund details and back navigation validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_005");
        }
    }

    @Test(priority = 6)
    public void SB_006_VerifyStocksTabResultsLoaded() {
        createExtentTest(
                "SB_006",
                "Verify Stocks tab results",
                "Open search, enter HDFC keyword, switch to Stocks tab, and validate first HDFC stock row is visible using exact locator"
        );

        ReportLogger.step("Starting test case: SB_006 - Verify Stocks tab results");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareHdfcSearchResults(searchBarPage, "SB_006");

            ReportLogger.step("SB_006 STEP 05 - Open Stocks tab");
            searchBarPage.openStocksTabForSearch();
            ReportLogger.pass("SB_006 STEP 05 PASSED - Stocks tab opened with first HDFC stock row visible");

            markPassed("SB_006 - Stocks tab search results validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_006");
        }
    }

    @Test(priority = 7)
    public void SB_007_VerifyStockResultDetails() {
        createExtentTest(
                "SB_007",
                "Verify stock result details",
                "Open HDFC Bank result from Stocks tab and validate stock details page"
        );

        ReportLogger.step("Starting test case: SB_007 - Verify stock result details");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareHdfcSearchResults(searchBarPage, "SB_007");

            ReportLogger.step("SB_007 STEP 05 - Open Stocks tab");
            searchBarPage.openStocksTabForSearch();
            ReportLogger.pass("SB_007 STEP 05 PASSED - Stocks tab opened");

            ReportLogger.step("SB_007 STEP 06 - Open stock result");
            searchBarPage.openStockResultForSearch();
            ReportLogger.pass("SB_007 STEP 06 PASSED - Stock result opened");

            ReportLogger.step("SB_007 STEP 07 - Validate stock details");
            searchBarPage.validateStockDetailsOpenedForSearch();
            ReportLogger.pass("SB_007 STEP 07 PASSED - Stock details validated");

            markPassed("SB_007 - Stock details validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_007");
        }
    }

    @Test(priority = 8)
    public void SB_008_VerifyFundToStockNavigationRegression() {
        createExtentTest(
                "SB_008",
                "Verify Fund to Stock navigation regression",
                "Open a fund result, return to search results, switch to Stocks tab, open HDFC Bank, and validate stock details without wrong tap or invalid company id"
        );

        ReportLogger.step("Starting test case: SB_008 - Verify Fund to Stock navigation regression");

        SearchBarPage searchBarPage = new SearchBarPage(driver);

        try {
            prepareHdfcSearchResults(searchBarPage, "SB_008");

            ReportLogger.step("SB_008 STEP 05 - Open Mutual Funds/SIPs tab");
            searchBarPage.openMutualFundsTabForSearch();
            ReportLogger.pass("SB_008 STEP 05 PASSED - Mutual Funds/SIPs tab opened");

            ReportLogger.step("SB_008 STEP 06 - Open fund result");
            searchBarPage.openFundResultForSearch();
            ReportLogger.pass("SB_008 STEP 06 PASSED - Fund result opened");

            ReportLogger.step("SB_008 STEP 07 - Validate fund details");
            searchBarPage.validateFundDetailsOpenedForSearch();
            ReportLogger.pass("SB_008 STEP 07 PASSED - Fund details validated");

            ReportLogger.step("SB_008 STEP 08 - Return to search results after fund details");
            searchBarPage.returnBackToSearchResultsAfterFundForSearch();
            ReportLogger.pass("SB_008 STEP 08 PASSED - Returned to search results");

            ReportLogger.step("SB_008 STEP 09 - Open Stocks tab after returning from fund details");
            searchBarPage.openStocksTabForSearch();
            ReportLogger.pass("SB_008 STEP 09 PASSED - Stocks tab opened after fund back navigation");

            ReportLogger.step("SB_008 STEP 10 - Open HDFC Bank stock result");
            searchBarPage.openStockResultForSearch();
            ReportLogger.pass("SB_008 STEP 10 PASSED - HDFC Bank stock result opened");

            ReportLogger.step("SB_008 STEP 11 - Validate stock details");
            searchBarPage.validateStockDetailsOpenedForSearch();
            ReportLogger.pass("SB_008 STEP 11 PASSED - Stock details validated after fund to stock navigation");

            markPassed("SB_008 - Fund to Stock navigation regression validated successfully");

        } finally {
            cleanupSearchFlow(searchBarPage, "SB_008");
        }
    }

    private void prepareSearchScreen(SearchBarPage searchBarPage, String caseId) {
        ReportLogger.step(caseId + " STEP 01 - Capture Advisor app package");
        searchBarPage.captureAdvisorAppPackageForSearch();
        ReportLogger.pass(caseId + " STEP 01 PASSED - Advisor app package captured");

        ReportLogger.step(caseId + " STEP 02 - Check Advisor app login/session");
        searchBarPage.ensureAdvisorAppLoggedInForSearch();
        ReportLogger.pass(caseId + " STEP 02 PASSED - Advisor app login/session confirmed");

        ReportLogger.step(caseId + " STEP 03 - Open global search from dashboard/home");
        searchBarPage.openGlobalSearchFromDashboardForSearch();
        ReportLogger.pass(caseId + " STEP 03 PASSED - Global search opened");

        ReportLogger.step(caseId + " STEP 04 - Validate search screen structure");
        searchBarPage.validateSearchScreenStructureForSearch();
        ReportLogger.pass(caseId + " STEP 04 PASSED - Search screen structure validated");
    }

    private void prepareHdfcSearchResults(SearchBarPage searchBarPage, String caseId) {
        prepareSearchScreen(searchBarPage, caseId);

        ReportLogger.step(caseId + " STEP 05 - Enter HDFC search keyword");
        searchBarPage.enterSearchKeywordForSearch();
        ReportLogger.pass(caseId + " STEP 05 PASSED - HDFC search keyword entered");

        ReportLogger.step(caseId + " STEP 06 - Validate HDFC search results loaded");
        searchBarPage.validateSearchResultsLoadedForSearch();
        ReportLogger.pass(caseId + " STEP 06 PASSED - HDFC search results loaded");
    }

    private void cleanupSearchFlow(SearchBarPage searchBarPage, String caseId) {
        ReportLogger.step(caseId + " CLEANUP - Return back to Advisor App");

        try {
            searchBarPage.returnBackToAdvisorAppSafely();
            ReportLogger.pass(caseId + " CLEANUP COMPLETED - Return flow executed");
        } catch (Exception e) {
            ReportLogger.debug(caseId + " cleanup failed: " + e.getMessage());
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Search Bar<br>"
                + "Case ID: " + caseId + "<br>"
                + "Validation: " + validation);

        ExtentTestManager.setTest(test);
    }

    private void markPassed(String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + message);
    }
}