package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;

import com.valueresearch.base.BaseTest;

import com.valueresearch.pages.StockDetailsPage;

import com.valueresearch.utils.AuthHelper;

import com.valueresearch.utils.AdvisorAccountSession;

import com.valueresearch.utils.ScreenshotUtils;

import io.appium.java_client.android.AndroidDriver;

import org.testng.ITestResult;

import org.testng.annotations.AfterMethod;

import org.testng.annotations.BeforeClass;

import org.testng.annotations.BeforeMethod;

import org.testng.annotations.Optional;

import org.testng.annotations.Parameters;

import java.lang.reflect.Method;

import java.util.Locale;

import com.valueresearch.utils.ExtentManager;

import com.valueresearch.utils.ExtentTestManager;

import com.valueresearch.utils.ReportLogger;

import org.testng.annotations.Test;

public class StockDetailsTest extends BaseTest {
    private boolean stockDetailsOpened;

    private AdvisorAccountSession accountSession;
    private AuthHelper.LoginMethod loginMethod;
    private String currentCaseId;
    private ExtentTest currentReport;

    @BeforeClass(alwaysRun = true)
    public void resetStockDetailsSession() {
        stockDetailsOpened = false;
        accountSession = null;
        currentCaseId = null;
        currentReport = null;
    }

    @BeforeMethod(alwaysRun = true)
   @Parameters("loginMethod")
   public void prepareStockDetailsTest(Method method, @Optional("PASSWORD") String selectedMethod) {
       currentCaseId = null;
       currentReport = null;
       loginMethod = null;
       String caseId = method.getName().substring(0, 6);
       try {
           // The child setup must not hide a failed BaseTest @BeforeClass/@BeforeMethod.
           if (driver == null || driver.getSessionId() == null) {
               throw new IllegalStateException(
                   "AndroidDriver is unavailable before Stock Details account setup. "
                   + "Check the earlier BaseTest/DriverManager configuration failure. "
                   + "The selected ADB udid must match 'adb devices'."
               );
           }
           createExtentTest(caseId, method.getAnnotation(Test.class).description(),
                   "Verify the configured active account; cancel biometrics before PIN entry");
           if (selectedMethod == null || selectedMethod.trim().isEmpty()) {
               throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
           }
           try {
               loginMethod = AuthHelper.LoginMethod.valueOf(
                       selectedMethod.trim().toUpperCase(Locale.ROOT));
           } catch (IllegalArgumentException invalidMethod) {
               throw new IllegalArgumentException(
                       "Suite loginMethod must be OTP or PASSWORD; received " + selectedMethod,
                       invalidMethod);
           }
           prepareStockDetailsAccount();
           StockDetailsPage page = createPage(driver);
           // Later cases can run without SD_001; preserve an already-open Stock Details screen.
           if (!method.getName().equals("SD_001_OpenStockDetailsFromSearch")
                   && !page.isOnStockDetailsPage()) {
               page.openStockDetailsFromSearch();
               stockDetailsOpened = true;
           }
       } catch (RuntimeException | AssertionError | LinkageError setupFailure) {
           stockDetailsOpened = false;
           if (accountSession != null) accountSession.invalidate();
           ReportLogger.fail("Stock Details setup failed for " + caseId + ": "
                   + setupFailure.getClass().getSimpleName()
                   + " | " + setupFailure.getMessage());
           // Print the real class/line/cause instead of repeating only 'NullPointerException'.
           setupFailure.printStackTrace(System.err);
           if (driver != null && driver.getSessionId() != null) {
               try {
                   String screenshot = ScreenshotUtils.captureScreenshot(
                           driver, caseId + "_authentication_setup");
                   if (screenshot != null && currentReport != null) {
                       currentReport.addScreenCaptureFromPath(
                               screenshot, "Stock Details preparation failure");
                   }
               } catch (RuntimeException screenshotFailure) {
                   setupFailure.addSuppressed(screenshotFailure);
               }
           } else {
               System.err.println("[SETUP] Screenshot skipped: AndroidDriver session is unavailable.");
           }
           throw setupFailure;
       }
   }
   /** Console evidence must include the cause, not just a screenshot notification. */

    @AfterMethod(alwaysRun = true)
    public void reportStockDetailsFailure(ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE || result.getThrowable() == null) return;
        Throwable failure = result.getThrowable();
        String message = failure.getMessage();
        message = message == null ? "No exception message was supplied" : message.replaceAll("\\s+", " ");
        if (message.length() > 1800) message = message.substring(0, 1800);
        ReportLogger.fail(result.getName() + " | " + failure.getClass().getSimpleName() + ": " + message);
    }
    /** Central authentication also covers search recovery inside an existing case. */

    private void prepareStockDetailsAccount() {
       if (driver == null || driver.getSessionId() == null) {
           throw new IllegalStateException("Cannot prepare Advisor account without an active AndroidDriver session");
       }
        if (loginMethod == null) {
            throw new IllegalStateException("Common Stock Details authentication setup must run first");
        }
        if (accountSession == null || !accountSession.isForDriver(driver)) {
            accountSession = createAccountSession(driver);
            stockDetailsOpened = false;
        }
        Throwable preparationFailure = null;
        try {
            accountSession.prepare(AuthHelper.Account.ACTIVE_SUBSCRIBER, loginMethod,
                    () -> createPage(driver).isOnStockDetailsPage());
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            preparationFailure = failure;
            throw failure;
        } finally {
            try {
                accountSession.restoreTimingSettings();
            } catch (RuntimeException | AssertionError restoreFailure) {
                accountSession.invalidate();
                if (preparationFailure != null) preparationFailure.addSuppressed(restoreFailure);
                else throw restoreFailure;
            }
        }
    }

    protected AdvisorAccountSession createAccountSession(AndroidDriver currentDriver) {
        return new AdvisorAccountSession(currentDriver);
    }

    protected StockDetailsPage createPage(AndroidDriver currentDriver) {
        return new StockDetailsPage(currentDriver);
    }

    @Test(priority = 1, alwaysRun = true, description = "Open Stock Details from global search")
    public void SD_001_OpenStockDetailsFromSearch() {
        createExtentTest(
                "SD_001",
                "Open Stock Details from global search",
                "Open global search from home/dashboard, search ITC, select Stocks tab, and open ITC Stock Details page"
        );
        ReportLogger.step("Starting test case: SD_001 - Open Stock Details from global search");
        ReportLogger.pass("Configured active Advisor account/session confirmed");
        StockDetailsPage page = createPage(driver);
        page.openStockDetailsFromSearch();
        stockDetailsOpened = true;
        ExtentTestManager.getTest().pass("<span class='badge white-text green'>SD_001 - Stock Details opened successfully</span>");
        ReportLogger.pass("Completed test case: SD_001");
    }

    @Test(priority = 2, alwaysRun = true, description = "Verify Stock Details header")
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

    @Test(priority = 3, alwaysRun = true, description = "Verify Stock Rating and Stock Range")
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

    @Test(priority = 4, alwaysRun = true, description = "Verify Fundamentals")
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

    @Test(priority = 5, alwaysRun = true, description = "Verify 10 Years Aggregate")
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

    @Test(priority = 6, alwaysRun = true, description = "Verify Stock Performance")
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

    @Test(priority = 7, alwaysRun = true, description = "Verify Essential Checks")
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

    @Test(priority = 8, alwaysRun = true, description = "Verify Financials")
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

    @Test(priority = 9, alwaysRun = true, description = "Verify Key Ratios")
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

    @Test(priority = 10, alwaysRun = true, description = "Verify Key Ratios More Details")
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

    @Test(priority = 11, alwaysRun = true, description = "Verify Peers")
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

    @Test(priority = 12, alwaysRun = true, description = "Verify Peers More Details")
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

    @Test(priority = 13, alwaysRun = true, description = "Verify Shareholding")
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

    @Test(priority = 14, alwaysRun = true, description = "Verify Company Profile")
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

    @Test(priority = 15, alwaysRun = true, dependsOnMethods = "SD_014_VerifyCompanyProfile", description = "Verify News Section")
public void SD_015_VerifyNewsSection() {
    createExtentTest(
            "SD_015",
            "Verify News Section",
            "Validate that the News section is present on the Stock Details page"
    );
    ReportLogger.step("Starting test case: SD_015 - Verify News Section");
    StockDetailsPage page = createPage(driver);
    page.verifyNewsSectionPresent();
    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_015 - News section is present</span>"
    );
    ReportLogger.pass("Completed test case: SD_015");
}

    @Test(priority = 16, alwaysRun = true, dependsOnMethods = "SD_015_VerifyNewsSection", description = "Verify Analysis Section")
public void SD_016_VerifyAnalysisSection() {
    createExtentTest(
            "SD_016",
            "Verify Analysis Section",
            "Validate that the Analysis section is present on the Stock Details page"
    );
    ReportLogger.step("Starting test case: SD_016 - Verify Analysis Section");
    StockDetailsPage page = createPage(driver);
    page.verifyAnalysisSectionPresent();
    ExtentTestManager.getTest().pass(
            "<span class='badge white-text green'>SD_016 - Analysis section is present</span>"
    );
    ReportLogger.pass("Completed test case: SD_016");
}

    private StockDetailsPage getStockDetailsPageWithoutTopReset(String sectionName) {
    StockDetailsPage page = createPage(driver);
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
        prepareStockDetailsAccount();
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
        StockDetailsPage page = createPage(driver);
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
            prepareStockDetailsAccount();
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
            prepareStockDetailsAccount();
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
        if (!caseId.equals(currentCaseId) || currentReport == null) {
            currentReport = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
            currentCaseId = caseId;
        }
        currentReport.info("<b>Validation:</b> " + validation);
        ExtentTestManager.setTest(currentReport);
    }
}
