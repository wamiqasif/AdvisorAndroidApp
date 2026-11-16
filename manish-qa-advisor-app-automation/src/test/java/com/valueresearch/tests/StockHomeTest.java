package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.StockHomePage;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Stocks home page (Stocks bottom tab) - active VRSA subscriber.
 * Account: testEmail / testEmailPass from config.properties (AuthHelper.Account.DEFAULT).
 * Every case starts on Hub, opens the Stocks tab and finishes on Hub.
 * Lapsed-user cases can be added later with groups = "lapsed-user".
 */
public class StockHomeTest extends BaseTest {

    private StockHomePage stockHomePage;
    private AdvisorAccountSession accountSession;
    private boolean accountPrepared;
    private String currentCaseId;
    private ExtentTest currentReport;

    @BeforeClass(alwaysRun = true)
    public void resetAccountSession() {
        accountSession = null;
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void createStockHomePage(Method method, @Optional("PASSWORD") String loginMethod) {
        stockHomePage = null;
        accountPrepared = false;
        currentCaseId = null;
        currentReport = null;
        Test annotation = method.getAnnotation(Test.class);
        String caseId = method.getName().substring(0, 6);
        startExtentCase(caseId, annotation.description(),
                "Shared account verification, configured login and biometric/PIN authentication before feature checks");
        try {
            AuthHelper.LoginMethod selectedMethod;
            try {
                selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
            }
            if (accountSession == null || !accountSession.isForDriver(driver)) {
                accountSession = createAccountSession(driver);
            }
            try {
                accountSession.prepare(requiredAccountFor(method), selectedMethod);
            } catch (IllegalStateException lateSecurityPrompt) {
                // After a driver recovery the PIN/biometric screen can appear late; prepare once more.
                if (!String.valueOf(lateSecurityPrompt.getMessage()).contains("authentication screen")) {
                    throw lateSecurityPrompt;
                }
                ReportLogger.info("Security screen appeared late after app relaunch; preparing the account once more");
                accountSession.invalidate();
                accountSession.prepare(requiredAccountFor(method), selectedMethod);
            }
            accountSession.assertHubVisible();
            stockHomePage = createPage(driver, accountSession);
            accountPrepared = true;
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            if (accountSession != null) accountSession.invalidate();
            ReportLogger.fail("App/account preparation failed: " + failure.getClass().getSimpleName()
                    + ". See the TestNG configuration failure trace.");
            attachFailureScreenshot();
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
    }

    /** Active VRSA cases use testEmail (DEFAULT); future lapsed cases use LapsedUserEmail. */
    private AuthHelper.Account requiredAccountFor(Method method) {
        for (String group : method.getAnnotation(Test.class).groups()) {
            if ("lapsed-user".equals(group)) return AuthHelper.Account.LAPSED_USER;
        }
        return AuthHelper.Account.DEFAULT;
    }

    protected AdvisorAccountSession createAccountSession(AndroidDriver currentDriver) {
        return new AdvisorAccountSession(currentDriver);
    }

    protected StockHomePage createPage(AndroidDriver currentDriver, AdvisorAccountSession session) {
        return new StockHomePage(currentDriver, session);
    }

    @Test(priority = 1, description = "Funds tab is the landing tab; switching to Stocks opens Stocks home")
    public void SH_001_FundsTabThenOpenStocksHome() {
        logValidation("SH_001",
                "Open the Funds tab (default landing tab), verify the Funds screen, switch to the Stocks tab and verify Stocks home opens, return to Hub");

        stockHomePage.verifyFundsTabThenOpenStocksHome();
        stockHomePage.navigateBackToHub();

        markPassed("SH_001", "Funds tab verified and Stocks home opened from it, returned to Hub");
    }

    @Test(priority = 2, description = "Verify Portfolio Value and 1D change with rupee amounts and percentage")
    public void SH_002_VerifyPortfolioValueAndOneDayChange() {
        logValidation("SH_002",
                "Select My Investment - 5 under Old Labels if another profile is active, verify the Portfolio Value heading and amount, the 1D change label with a rupee amount and a percentage (values are not hardcoded), return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyPortfolioValueAndOneDayChange();
        stockHomePage.navigateBackToHub();

        markPassed("SH_002", "Portfolio Value and 1D change amount/percentage verified, returned to Hub");
    }

    @Test(priority = 3, description = "Eye button hides and shows the portfolio numbers")
    public void SH_003_VerifyEyeButtonHidesAndShowsValues() {
        logValidation("SH_003",
                "Tap the eye button: Portfolio Value, 1D change amount and percentage are hidden; tap again: all three are shown, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyEyeButtonHidesAndShowsValues();
        stockHomePage.navigateBackToHub();

        markPassed("SH_003", "Eye button hid and showed the three numbers, returned to Hub");
    }

    @Test(priority = 4, description = "Notification bell opens the notification panel and Search opens the search bar")
    public void SH_004_VerifyNotificationPanelAndSearch() {
        logValidation("SH_004",
                "Open the Notification panel from the bell icon and go back; open Search from the search icon and go back to Stocks home, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyNotificationPanelAndSearch();
        stockHomePage.navigateBackToHub();

        markPassed("SH_004", "Notification panel and Search opened and closed with Back, returned to Hub");
    }

    @Test(priority = 5, description = "Update stocks portfolio card opens Update Transactions on the Stocks & ETFs tab (if shown)")
    public void SH_005_VerifyUpdateStocksPortfolioCard() {
        logValidation("SH_005",
                "If the 'Update stocks portfolio' card is shown, open it and verify Update Transactions on the Stocks & ETFs tab, go back; if absent the portfolio is up to date and the case passes, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyUpdateStocksPortfolioCardIfPresent();
        stockHomePage.navigateBackToHub();

        markPassed("SH_005", "Update stocks portfolio card verified (or not shown), returned to Hub");
    }

    @Test(priority = 6, description = "Latest Recommendations shows 3 recommendations and View more opens Analyst's Choice")
    public void SH_006_VerifyLatestRecommendations() {
        logValidation("SH_006",
                "Scroll to Latest Recommendations, verify 3 recommendations and View more, open View more (Analyst's Choice) and go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyLatestRecommendations();
        stockHomePage.navigateBackToHub();

        markPassed("SH_006", "Latest Recommendations verified and Analyst's Choice opened, returned to Hub");
    }

    @Test(priority = 7, description = "Big Moves: info drawer, period tabs, Gainers/Losers toggle and company details")
    public void SH_007_VerifyBigMoves() {
        logValidation("SH_007",
                "Scroll to Big Moves, open and close the 'i' definition drawer, open every period tab 1W-5Y for Gainers and for Losers, verify Top Gainers/Top Losers heading and Returns column, open a (dynamic) company's Stock Details and go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyBigMoves();
        stockHomePage.navigateBackToHub();

        markPassed("SH_007", "Big Moves drawer, tabs, toggle and company details verified, returned to Hub");
    }

    @Test(priority = 8, description = "Coverage on your stocks View more opens All Stories on the Coverage tab")
    public void SH_008_VerifyCoverageOnYourStocks() {
        logValidation("SH_008",
                "Scroll to Coverage on your stocks, verify the heading, open View more and verify All Stories on the Coverage tab, go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyCoverageOnYourStocks();
        stockHomePage.navigateBackToHub();

        markPassed("SH_008", "Coverage on your stocks View more verified, returned to Hub");
    }

    @Test(priority = 9, description = "Ready to invest portfolios tabs, columns, Plan your Investment and company details")
    public void SH_009_VerifyReadyToInvestPortfolios() {
        logValidation("SH_009",
                "Scroll to Ready to invest portfolios, open Long term Growth / All weather / Aggressive Growth and verify Company, 52W Trend and Price columns, open Plan your Investment (Investment Planner) and go back, open a (dynamic) company's Stock Details and go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyReadyToInvestPortfolios();
        stockHomePage.navigateBackToHub();

        markPassed("SH_009", "Ready to invest portfolios verified, returned to Hub");
    }

    @Test(priority = 10, description = "Coverage and Insight View more opens All Stories on the Coverage tab")
    public void SH_010_VerifyCoverageAndInsights() {
        logValidation("SH_010",
                "Scroll to Coverage and Insight, verify the heading, open View more and verify All Stories on the Coverage tab, go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyCoverageAndInsights();
        stockHomePage.navigateBackToHub();

        markPassed("SH_010", "Coverage and Insight View more verified, returned to Hub");
    }

    @Test(priority = 11, description = "Short term investments card opens an article and Need cash card opens Sell Planner")
    public void SH_011_VerifyShortTermAndNeedCashCards() {
        logValidation("SH_011",
                "Open 'Looking for short term investments' (article) and go back; open 'Need cash? Decide which stock to sell' (Sell Planner) and go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyShortTermAndNeedCashCards();
        stockHomePage.navigateBackToHub();

        markPassed("SH_011", "Short term article and Sell Planner opened, returned to Hub");
    }

    @Test(priority = 12, description = "Our performance heading, text, Show more and Show less")
    public void SH_012_VerifyOurPerformance() {
        logValidation("SH_012",
                "Scroll to Our performance, verify heading and text content, Show more expands the disclaimer and Show less collapses it, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyOurPerformance();
        stockHomePage.navigateBackToHub();

        markPassed("SH_012", "Our performance verified with Show more / Show less, returned to Hub");
    }

    @Test(priority = 13, description = "Open every Bonus report and go back")
    public void SH_013_VerifyBonusReports() {
        logValidation("SH_013",
                "Scroll to Bonus reports, open each of the 3 reports and go back to Stocks home after each, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyBonusReports();
        stockHomePage.navigateBackToHub();

        markPassed("SH_013", "All bonus reports opened, returned to Hub");
    }

    @Test(priority = 14, description = "User's Guide articles and View more opens All Stories on the Insights tab")
    public void SH_014_VerifyUsersGuide() {
        logValidation("SH_014",
                "Scroll to User's Guide, scroll the (dynamic) articles horizontally and open each one and go back, open View more and verify All Stories on the Insights tab, go back, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyUsersGuide();
        stockHomePage.navigateBackToHub();

        markPassed("SH_014", "User's Guide articles and Insights View more verified, returned to Hub");
    }

    @Test(priority = 15, description = "Upward arrow takes the user to the top of Stocks home")
    public void SH_015_VerifyScrollToTopArrow() {
        logValidation("SH_015",
                "Scroll to the bottom of Stocks home, tap the upward arrow and verify the top of the page (Portfolio Value) is shown, return to Hub");

        stockHomePage.openStocksHome();
        stockHomePage.verifyScrollToTopArrow();
        stockHomePage.navigateBackToHub();

        markPassed("SH_015", "Upward arrow returned to the top of Stocks home, returned to Hub");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupStockHomeCase(ITestResult result) {
        Throwable original = result.getThrowable();
        if (accountPrepared && original != null) attachFailureScreenshot();
        Throwable cleanupFailure = null;
        try {
            if (accountPrepared && stockHomePage != null) {
                stockHomePage.navigateBackToHub();
                accountSession.assertHubVisible();
            }
        } catch (RuntimeException | AssertionError failure) {
            // Navigation-only failure: keep the verified account; the next prepare() recovers to Hub.
            cleanupFailure = failure;
        } finally {
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                    accountSession.invalidate();
                }
            }
            accountPrepared = false;
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else {
                result.setThrowable(cleanupFailure);
                result.setStatus(ITestResult.FAILURE);
                attachFailureScreenshot();
            }
            ReportLogger.fail("Stocks home cleanup failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void attachFailureScreenshot() {
        try {
            String path = ScreenshotUtils.captureScreenshot(driver, currentCaseId + "_before_Hub_reset");
            if (path != null && currentReport != null) {
                currentReport.addScreenCaptureFromPath(path, "Failed state before Hub reset");
            }
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }

    /** Creates the Extent node for a case (called once per case from @BeforeMethod). */
    private void startExtentCase(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + " - " + title);
        currentCaseId = caseId;
        currentReport = test;
        test.info(
                "Module: Stocks Home<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Page Build: " + StockHomePage.BUILD_VERSION + "<br>"
                        + "Validation: " + validation
        );
        ExtentTestManager.setTest(test);
    }

    /** Adds the case-specific validation text to the node created in @BeforeMethod. */
    private void logValidation(String caseId, String validation) {
        if (currentReport == null || !caseId.equals(currentCaseId)) {
            startExtentCase(caseId, caseId, validation);
            return;
        }
        currentReport.info("Validation: " + validation);
        ExtentTestManager.setTest(currentReport);
    }

    private void markPassed(String caseId, String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + caseId + " - " + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + caseId + " - " + message);
    }
}