package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.StockDetailsPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class StockDetailsTest extends BaseTest {

    private static boolean stockDetailsOpened = false;

    @Test(priority = 1, alwaysRun = true)
    public void SD_001_OpenStockDetailsFromSearch() {
        createExtentTest(
                "SD_001",
                "Open Stock Details from global search",
                "Open global search from home/dashboard, search ITC, select Stocks tab, and open ITC Stock Details page"
        );

        ReportLogger.step("Starting test case: SD_001 - Open Stock Details from global search");

        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        StockDetailsPage page = new StockDetailsPage(driver);
        page.openStockDetailsFromSearch();

        stockDetailsOpened = true;

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_001 - Stock Details opened successfully</span>");
        ReportLogger.pass("Completed test case: SD_001");
    }

    @Test(priority = 2, alwaysRun = true)
    public void SD_002_VerifyStockHeader() {
        createExtentTest(
                "SD_002",
                "Verify Stock Details header",
                "Validate ITC stock header, exchange chips, sector, industry, price chart options, and Stock Rating"
        );

        ReportLogger.step("Starting test case: SD_002 - Verify Stock Details header");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyStockHeader();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_002 - Stock header validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_002");
    }

    @Test(priority = 3, alwaysRun = true)
    public void SD_003_VerifyStockRatingAndRange() {
        createExtentTest(
                "SD_003",
                "Verify Stock Rating and Stock Range",
                "Validate Stock Rating scores and all 3 Stock Range carousel cards for ITC Stock Details"
        );

        ReportLogger.step("Starting test case: SD_003 - Verify Stock Rating and Stock Range");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyStockRatingAndRange();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_003 - Stock Rating and Range validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_003");
    }

    @Test(priority = 4, alwaysRun = true)
    public void SD_004_VerifyFundamentals() {
        createExtentTest(
                "SD_004",
                "Verify Fundamentals",
                "Validate Fundamentals section labels for ITC Stock Details"
        );

        ReportLogger.step("Starting test case: SD_004 - Verify Fundamentals");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyFundamentals();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_004 - Fundamentals validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_004");
    }
    
    @Test(priority = 5, alwaysRun = true)
    public void SD_005_VerifyTenYearsAggregate() {
        createExtentTest(
                "SD_005",
                "Verify 10 Years Aggregate",
                "Validate 10 Years Aggregate section labels for ITC Stock Details"
        );

        ReportLogger.step("Starting test case: SD_005 - Verify 10 Years Aggregate");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyTenYearsAggregate();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_005 - 10 Years Aggregate validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_005");
    }

    @Test(priority = 6, alwaysRun = true)
    public void SD_006_VerifyStockPerformance() {
        createExtentTest(
                "SD_006",
                "Verify Stock Performance",
                "Validate Stock Performance Trailing and Annual tables with live value capture"
        );

        ReportLogger.step("Starting test case: SD_006 - Verify Stock Performance");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyStockPerformance();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_006 - Stock Performance validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_006");
    }
    @Test(priority = 7, alwaysRun = true)
    public void SD_007_VerifyEssentialChecks() {
        createExtentTest(
                "SD_007",
                "Verify Essential Checks",
                "Validate Essential Checks carousel cards with live value capture"
        );

        ReportLogger.step("Starting test case: SD_007 - Verify Essential Checks");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyEssentialChecks();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_007 - Essential Checks validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_007");
    }
    
    @Test(priority = 8, alwaysRun = true)
    public void SD_008_VerifyFinancials() {
        createExtentTest(
                "SD_008",
                "Verify Financials",
                "Validate Financials tabs, legends, period labels, and annual/quarterly options"
        );

        ReportLogger.step("Starting test case: SD_008 - Verify Financials");

        StockDetailsPage page = getStockDetailsPage();
        page.recoverStockDetailsIfNeeded();
        page.verifyFinancials();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_008 - Financials validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_008");
    }
    
    @Test(priority = 9, alwaysRun = true)
    public void SD_009_VerifyKeyRatios() {
        createExtentTest(
                "SD_009",
                "Verify Key Ratios",
                "Validate Key Ratios tabs and carousel cards with live numeric value capture"
        );

        ReportLogger.step("Starting test case: SD_009 - Verify Key Ratios");

        StockDetailsPage page = getStockDetailsPageWithoutTopReset("Key Ratios");

        page.verifyKeyRatios();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_009 - Key Ratios validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_009");
    }
    
    @Test(priority = 10, alwaysRun = true)
    public void SD_010_VerifyKeyRatiosMoreDetails() {
        createExtentTest(
                "SD_010",
                "Verify Key Ratios More Details",
                "Validate all Key Ratios More tabs and all ratio blocks dynamically"
        );

        ReportLogger.step("Starting test case: SD_010 - Verify Key Ratios More Details");

        StockDetailsPage page = getStockDetailsPageWithoutTopReset("Key Ratios More Details");

        page.verifyKeyRatiosMoreDetails();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_010 - Key Ratios More Details validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_010");
    }
    
    @Test(priority = 11, alwaysRun = true)
    public void SD_011_VerifyPeers() {
        createExtentTest(
                "SD_011",
                "Verify Peers",
                "Validate Peers section headers when exposed and visible peer company names"
        );

        ReportLogger.step("Starting test case: SD_011 - Verify Peers");

        StockDetailsPage page = getStockDetailsPageWithoutTopReset("Peers");

        page.verifyPeers();

        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_011 - Peers validated successfully</span>");
        ReportLogger.pass("Completed test case: SD_011");
    }
    @Test(priority = 12, alwaysRun = true)
public void SD_012_VerifyPeersMoreDetails() {
    createExtentTest(
            "SD_012",
            "Verify Peers More Details",
            "Validate Peers More details page, all detail tabs and all peer companies"
    );

    ReportLogger.step("Starting test case: SD_012 - Verify Peers More Details");

    StockDetailsPage page = getStockDetailsPageWithoutTopReset("Peers More Details");

    page.verifyPeersMoreDetails();

    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_012 - Peers More Details validated successfully</span>"
    );

    ReportLogger.pass("Completed test case: SD_012");
}
    @Test(priority = 13, alwaysRun = true)
public void SD_013_VerifyShareholding() {
    createExtentTest(
            "SD_013",
            "Verify Shareholding",
            "Validate Shareholding quarter chips, headers, labels and percentage values"
    );

    ReportLogger.step("Starting test case: SD_013 - Verify Shareholding");

    StockDetailsPage page = getStockDetailsPageWithoutTopReset("Shareholding");

    page.verifyShareholding();

    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_013 - Shareholding validated successfully</span>"
    );

    ReportLogger.pass("Completed test case: SD_013");
}
    @Test(priority = 14, alwaysRun = true)
public void SD_014_VerifyCompanyProfile() {
    createExtentTest(
            "SD_014",
            "Verify Company Profile",
            "Validate Company Profile section strict label-value mapping and Business text"
    );

    ReportLogger.step("Starting test case: SD_014 - Verify Company Profile");

    StockDetailsPage page = getStockDetailsPageWithoutTopReset("Company Profile");

    page.verifyCompanyProfile();

    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_014 - Company Profile validated successfully</span>"
    );

    ReportLogger.pass("Completed test case: SD_014");
}
    @Test(priority = 15, alwaysRun = true, dependsOnMethods = "SD_014_VerifyCompanyProfile")
public void SD_015_VerifyNewsSection() {
    createExtentTest(
            "SD_015",
            "Verify News Section",
            "Validate that the News section is present on the Stock Details page"
    );

    ReportLogger.step("Starting test case: SD_015 - Verify News Section");

    StockDetailsPage page = new StockDetailsPage(driver);

    page.verifyNewsSectionPresent();

    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_015 - News section is present</span>"
    );

    ReportLogger.pass("Completed test case: SD_015");
}

    @Test(priority = 16, alwaysRun = true, dependsOnMethods = "SD_015_VerifyNewsSection")
public void SD_016_VerifyAnalysisSection() {
    createExtentTest(
            "SD_016",
            "Verify Analysis Section",
            "Validate that the Analysis section is present on the Stock Details page"
    );

    ReportLogger.step("Starting test case: SD_016 - Verify Analysis Section");

    StockDetailsPage page = new StockDetailsPage(driver);

    page.verifyAnalysisSectionPresent();

    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_016 - Analysis section is present</span>"
    );

    ReportLogger.pass("Completed test case: SD_016");
}

    private StockDetailsPage getStockDetailsPageWithoutTopReset(String sectionName) {
    StockDetailsPage page = new StockDetailsPage(driver);

    /*
     * Bottom-section regression stability:
     * Do not reset Stock Details to top when we are genuinely still on Stock Details.
     */
    if (page.isOnStockDetailsPage()) {
        ReportLogger.pass("Stock Details page is already open for " + sectionName);
        stockDetailsOpened = true;
        return page;
    }

    /*
     * Critical guard:
     * If SD_012 cleanup accidentally lands on dashboard/home, do not press Back.
     * Reopen Stock Details directly. Pressing Back from dashboard makes the app
     * state worse and was the reason SD_013 started unstable.
     */
    if (page.isOnDashboardOrHome()) {
        return reopenStockDetailsForSection(page, sectionName, "Dashboard/Home detected before recovery");
    }

    try {
        page.recoverStockDetailsIfNeeded();
        stockDetailsOpened = true;
        return page;

    } catch (Exception recoveryError) {
        ReportLogger.debug(sectionName + " recovery failed. Reason: "
                + recoveryError.getMessage());
    }

    if (page.isOnDashboardOrHome()) {
        return reopenStockDetailsForSection(page, sectionName, "Dashboard/Home detected after recovery failure");
    }

    /*
     * We may still be inside a details page. Use a very limited back recovery,
     * and stop immediately if we reach either Stock Details or Dashboard/Home.
     */
    for (int attempt = 1; attempt <= 3; attempt++) {
        try {
            driver.navigate().back();
            sleepTest(1400);

            if (page.isOnStockDetailsPage()) {
                ReportLogger.pass("Returned to Stock Details page for " + sectionName
                        + " after back-navigation attempt " + attempt);
                stockDetailsOpened = true;
                return page;
            }

            if (page.isOnDashboardOrHome()) {
                return reopenStockDetailsForSection(
                        page,
                        sectionName,
                        "Dashboard/Home reached during back-navigation attempt " + attempt
                );
            }

        } catch (Exception backError) {
            ReportLogger.debug("Back-navigation recovery attempt failed for "
                    + sectionName
                    + " | attempt="
                    + attempt
                    + " | reason="
                    + backError.getMessage());
        }
    }

    return reopenStockDetailsForSection(page, sectionName, "Final fallback after limited recovery");
}

private StockDetailsPage reopenStockDetailsForSection(
        StockDetailsPage page,
        String sectionName,
        String reason
) {
    try {
        ReportLogger.debug(sectionName + " - " + reason + ". Reopening Stock Details from search.");
        new AuthHelper(driver).ensureLoggedIn();
        page.openStockDetailsFromSearch();
        stockDetailsOpened = true;
        return page;

    } catch (Exception finalRecoveryError) {
        throw new RuntimeException("Unable to prepare Stock Details page for "
                + sectionName
                + ". Current app state could not be recovered safely.",
                finalRecoveryError);
    }
}


    private StockDetailsPage getStockDetailsPage() {
        StockDetailsPage page = new StockDetailsPage(driver);

        if (page.isOnDashboardOrHome()) {
            return reopenStockDetailsForSection(page, "Stock Details", "Dashboard/Home detected in generic page recovery");
        }

        if (page.isOnStockDetailsPage()) {
            ReportLogger.pass("Stock Details page is already open");

            /*
             * Compile-safe fix:
             * Do not call resetStockDetailsToTopForRegression() because the current
             * StockDetailsPage.java in your project does not expose that method.
             * Each page validation method already performs its own section alignment.
             */
            stockDetailsOpened = true;
            return page;
        }

        if (!stockDetailsOpened) {
            ReportLogger.step("Stock Details page was not opened by SD_001. Opening it now.");
            new AuthHelper(driver).ensureLoggedIn();
            ReportLogger.pass("Advisor login/session confirmed");
            page.openStockDetailsFromSearch();
            stockDetailsOpened = true;
            return page;
        }

        try {
            page.recoverStockDetailsIfNeeded();
            stockDetailsOpened = true;
        } catch (Exception recoveryError) {
            ReportLogger.debug("Stock Details recovery failed. Reopening from search. Reason: " + recoveryError.getMessage());
            new AuthHelper(driver).ensureLoggedIn();
            page.openStockDetailsFromSearch();
            stockDetailsOpened = true;
        }

        return page;
    }

    private void sleepTest(long millis) {
    try {
        Thread.sleep(millis);
    } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting during Stock Details test recovery", interruptedException);
    }
}

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("<b>Validation:</b> " + validation);
        ExtentTestManager.setTest(test);
    }
}