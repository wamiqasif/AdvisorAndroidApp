package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.BottomNavBarPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class BottomNavBarTest extends BaseTest {

    @Test(priority = 1)
    public void BN_001_VerifyFundsTabOpens() {
        createExtentTest(
                "BN_001",
                "Verify Funds bottom tab opens",
                "Login/session check, tap Funds bottom tab, and validate Funds screen markers"
        );

        ReportLogger.step("Starting test case: BN_001 - Verify Funds bottom tab opens");
        prepareLoggedInState();

        getBottomNavBarPage().verifyFundsTabOpens();
        markPassed("BN_001 - Funds bottom tab validated successfully");
    }

    @Test(priority = 2)
    public void BN_002_VerifyStocksTabOpens() {
        createExtentTest(
                "BN_002",
                "Verify Stocks bottom tab opens",
                "Restore login/session if required, tap Stocks, and validate Stock Advisor markers"
        );

        ReportLogger.step("Starting test case: BN_002 - Verify Stocks bottom tab opens");
        prepareLoggedInState();

        getBottomNavBarPage().verifyStocksTabOpens();
        markPassed("BN_002 - Stocks bottom tab validated successfully");
    }

    @Test(priority = 3)
    public void BN_003_VerifyPortfolioTabOpens() {
        createExtentTest(
                "BN_003",
                "Verify Portfolio bottom tab opens",
                "Restore login/session if required, tap Portfolio, and validate portfolio markers"
        );

        ReportLogger.step("Starting test case: BN_003 - Verify Portfolio bottom tab opens");
        prepareLoggedInState();

        getBottomNavBarPage().verifyPortfolioTabOpens();
        markPassed("BN_003 - Portfolio bottom tab validated successfully");
    }

    @Test(priority = 4)
    public void BN_004_VerifyHubTabOpens() {
        createExtentTest(
                "BN_004",
                "Verify Hub bottom tab opens",
                "Restore login/session if required, tap Hub, and validate Hub/Profile markers"
        );

        ReportLogger.step("Starting test case: BN_004 - Verify Hub bottom tab opens");
        prepareLoggedInState();

        getBottomNavBarPage().verifyHubTabOpens();
        markPassed("BN_004 - Hub bottom tab validated successfully");
    }

    @Test(priority = 5)
    public void BN_005_VerifyAllBottomNavLabelsVisible() {
        createExtentTest(
                "BN_005",
                "Verify all bottom nav labels are visible",
                "Restore login/session and validate Funds, Stocks, Portfolio, and Hub labels"
        );

        ReportLogger.step("Starting test case: BN_005 - Verify all bottom nav labels are visible");
        prepareLoggedInState();

        getBottomNavBarPage().verifyAllBottomNavLabelsVisible();
        markPassed("BN_005 - All bottom nav labels validated successfully");
    }

    @Test(priority = 6)
    public void BN_006_VerifyActiveTabChangesAfterEachTap() {
        createExtentTest(
                "BN_006",
                "Verify active tab/content changes after each tap",
                "Restore login/session, tap all four tabs, and validate target screen markers"
        );

        ReportLogger.step("Starting test case: BN_006 - Verify active tab/content changes after each tap");
        prepareLoggedInState();

        getBottomNavBarPage().verifyActiveTabChangesAfterEachTap();
        markPassed("BN_006 - Active tab/content change validated successfully");
    }

    @Test(priority = 7)
    public void BN_007_VerifySelectedFundsTabRetapDoesNotCrash() {
        createExtentTest(
                "BN_007",
                "Negative: verify selected Funds tab re-tap does not crash",
                "Restore login/session, re-tap Funds, and validate the app remains responsive"
        );

        ReportLogger.step("Starting test case: BN_007 - Negative selected Funds tab re-tap validation");
        prepareLoggedInState();

        getBottomNavBarPage().verifySelectedFundsTabRetapDoesNotCrashOrNavigateUnexpectedly();
        markPassed("BN_007 - Selected Funds tab re-tap validation passed");
    }

    @Test(priority = 8)
    public void BN_008_VerifyRapidBottomTabSwitchingStability() {
        createExtentTest(
                "BN_008",
                "Verify rapid bottom tab switching stability",
                "Restore login/session, rapidly switch across all four tabs, and validate stability"
        );

        ReportLogger.step("Starting test case: BN_008 - Verify rapid bottom tab switching stability");
        prepareLoggedInState();

        getBottomNavBarPage().verifyRapidBottomTabSwitchingStability();
        markPassed("BN_008 - Rapid bottom tab switching stability validated successfully");
    }

    @Test(priority = 9)
    public void BN_009_VerifyNoCrashOrAnrAfterBottomNavSwitching() {
        createExtentTest(
                "BN_009",
                "Verify no crash/ANR after bottom nav switching",
                "Restore login/session, perform controlled switching, and validate no crash/ANR"
        );

        ReportLogger.step("Starting test case: BN_009 - Verify no crash/ANR after bottom nav switching");
        prepareLoggedInState();

        getBottomNavBarPage().verifyNoCrashOrAnrAfterBottomNavSwitching();
        markPassed("BN_009 - No crash/ANR after bottom nav switching validated successfully");
    }

    @Test(priority = 10)
    public void BN_010_VerifyReturnToFundsTab() {
        createExtentTest(
                "BN_010",
                "Verify final navigation returns to Funds tab",
                "Restore login/session, tap Funds, and validate Funds screen markers"
        );

        ReportLogger.step("Starting test case: BN_010 - Verify final navigation returns to Funds tab");
        prepareLoggedInState();

        getBottomNavBarPage().verifyReturnToFundsTab();
        markPassed("BN_010 - Final navigation to Funds tab validated successfully");
    }

    /**
     * This is intentionally called inside every test after the Extent test is
     * created. If BaseTest recovered the emulator/UiAutomator2 before the test,
     * the app may be on the PIN screen. AuthHelper restores the logged-in state
     * before any bottom-navigation action is attempted.
     */
    private void prepareLoggedInState() {
        if (driver == null) {
            throw new IllegalStateException(
                    "AndroidDriver is null before Bottom Navigation test preparation."
            );
        }

        ReportLogger.step("Checking/restoring Advisor app login/session state");
        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed for this test");
    }

    /**
     * Always creates the page object from the current driver. This is important
     * after automatic recovery because the old page object would still contain
     * the dead AndroidDriver session.
     */
    private BottomNavBarPage getBottomNavBarPage() {
        return new BottomNavBarPage(driver);
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );
        ExtentTestManager.setTest(test);

        ExtentTestManager.getTest().info(
                "Module: Bottom Nav Bar<br>"
                        + "Case ID: " + caseId + "<br>"
                        + "Validation: " + validation
        );
    }

    private void markPassed(String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>"
                        + message
                        + "</span>"
        );
        ReportLogger.pass("Completed test case: " + message);
    }
}