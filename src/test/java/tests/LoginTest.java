package tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import base.RequiresPinScreen;
import pages.DashboardPage;
import pages.PinPage;
import utils.ConfigReader;




/**
 * Tests for the PIN-based login flow.
 *
 * The app launches directly to the PIN screen on every session. PIN is entered by
 * tapping individual digit buttons on a custom numeric keypad; the screen auto-submits
 * once all 4 digits have been entered.
 *
 * PIN screen UI (verified against live XML dump):
 *   Normal state  : greeting "Hi, wamiq azeem" + heading "Enter your Advisor PIN" + keypad (0-9) + backspace
 *   Wrong PIN state: adds "Incorrect pin." error + "Change pin" link
 */
@RequiresPinScreen
public class LoginTest extends BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(LoginTest.class);
    private static final String WRONG_PIN = "0000";

    // ================================================================
    // POSITIVE TEST CASES
    // ================================================================

    // ----------------------------------------------------------------
    // TC_LOGIN_001 — PIN screen is displayed on app launch
    // ----------------------------------------------------------------

    @Test(
            priority = 1,
            description = "Verify that the PIN entry screen is displayed when the app launches"
    )
    public void verifyPinScreenIsDisplayedOnLaunch() {
        logger.info("[TC_LOGIN_001] Initialising PinPage");
        getExtentTest().info("Verifying PIN screen is displayed on app launch");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen heading 'Enter your Advisor PIN' should be visible on launch");
        logger.info("[TC_LOGIN_001] PIN screen heading is displayed");

        getExtentTest().pass("PIN screen loaded successfully on app launch");
        logger.info("[TC_LOGIN_001] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_002 — PIN screen shows the personalised greeting
    // ----------------------------------------------------------------

    @Test(
            priority = 2,
            description = "Verify that the PIN screen shows the personalised greeting 'Hi, wamiq azeem'"
    )
    public void verifyPinScreenShowsUserGreeting() {
        logger.info("[TC_LOGIN_002] Initialising PinPage");
        getExtentTest().info("Verifying personalised greeting on PIN screen");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before checking the greeting");
        Assert.assertTrue(pinPage.isGreetingDisplayed(),
                "Greeting 'Hi, wamiq azeem' should be visible on the PIN screen");
        logger.info("[TC_LOGIN_002] User greeting is displayed");

        getExtentTest().pass("Personalised greeting is present on the PIN screen");
        logger.info("[TC_LOGIN_002] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_003 — All numeric keypad digits are present
    // ----------------------------------------------------------------

    @Test(
            priority = 3,
            description = "Verify that all 10 digit keys (0–9) are visible on the custom keypad"
    )
    public void verifyAllKeypadDigitsArePresent() {
        logger.info("[TC_LOGIN_003] Initialising PinPage");
        getExtentTest().info("Verifying all keypad digit buttons are present");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before verifying the keypad");

        String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        for (String digit : digits) {
            Assert.assertTrue(pinPage.isDigitDisplayed(digit),
                    "Keypad digit '" + digit + "' should be visible");
            logger.info("[TC_LOGIN_003] Digit '{}' is present", digit);
        }

        getExtentTest().pass("All 10 digit keys (0–9) are present on the keypad");
        logger.info("[TC_LOGIN_003] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_004 — Backspace key is present on the keypad
    // ----------------------------------------------------------------

    @Test(
            priority = 4,
            description = "Verify that the backspace key is visible on the PIN keypad"
    )
    public void verifyBackspaceKeyIsPresent() {
        logger.info("[TC_LOGIN_004] Initialising PinPage");
        getExtentTest().info("Verifying backspace key is present");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before verifying backspace");
        Assert.assertTrue(pinPage.isBackspaceDisplayed(),
                "Backspace key should be visible on the PIN keypad");
        logger.info("[TC_LOGIN_004] Backspace key is visible");

        getExtentTest().pass("Backspace key is present on the keypad");
        logger.info("[TC_LOGIN_004] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_005 — Successful login with correct PIN reaches Dashboard
    // ----------------------------------------------------------------

    @Test(
            priority = 5,
            description = "Verify that entering the correct PIN (1454) navigates to the Dashboard"
    )
    public void verifySuccessfulLoginWithCorrectPin() {
        ConfigReader config = ConfigReader.getInstance();

        logger.info("[TC_LOGIN_005] Initialising PinPage");
        getExtentTest().info("Entering correct PIN via keypad");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before entering PIN");

        pinPage.enterPin(config.getLoginPin());
        logger.info("[TC_LOGIN_005] Correct PIN entered digit by digit");

        getExtentTest().info("Verifying Dashboard is displayed");
        DashboardPage dashboardPage = new DashboardPage(getDriver());

        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
                "Dashboard should be displayed after entering the correct PIN");
        logger.info("[TC_LOGIN_005] Dashboard is displayed — login successful");

        getExtentTest().pass("Successful login — Dashboard loaded after correct PIN entry");
        logger.info("[TC_LOGIN_005] PASSED");
    }

    // ================================================================
    // NEGATIVE TEST CASES
    // ================================================================

    // ----------------------------------------------------------------
    // TC_LOGIN_006 — Wrong PIN shows "Incorrect pin." error message
    // ----------------------------------------------------------------

    @Test(
            priority = 6,
            description = "Verify that entering a wrong PIN shows the 'Incorrect pin.' error message"
    )
    public void verifyWrongPinShowsErrorMessage() {
        logger.info("[TC_LOGIN_006] Initialising PinPage");
        getExtentTest().info("Entering wrong PIN: " + WRONG_PIN);
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before entering wrong PIN");

        pinPage.enterPin(WRONG_PIN);
        logger.info("[TC_LOGIN_006] Wrong PIN entered: {}", WRONG_PIN);

        Assert.assertTrue(pinPage.isIncorrectPinErrorDisplayed(),
                "'Incorrect pin.' error message should appear after entering a wrong PIN");
        logger.info("[TC_LOGIN_006] 'Incorrect pin.' error is displayed");

        getExtentTest().pass("'Incorrect pin.' error message displayed after wrong PIN entry");
        logger.info("[TC_LOGIN_006] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_007 — Wrong PIN keeps the user on the PIN screen
    // ----------------------------------------------------------------

    @Test(
            priority = 7,
            description = "Verify that entering a wrong PIN does not navigate away from the PIN screen"
    )
    public void verifyWrongPinKeepsUserOnPinScreen() {
        logger.info("[TC_LOGIN_007] Initialising PinPage");
        getExtentTest().info("Entering wrong PIN and verifying user stays on PIN screen");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before entering wrong PIN");

        pinPage.enterPin(WRONG_PIN);
        logger.info("[TC_LOGIN_007] Wrong PIN entered: {}", WRONG_PIN);

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen should still be displayed after a wrong PIN — no navigation should occur");
        logger.info("[TC_LOGIN_007] User remains on PIN screen after wrong PIN");

        DashboardPage dashboardPage = new DashboardPage(getDriver());
        Assert.assertTrue(dashboardPage.isDashboardNotDisplayed(),
                "Dashboard should NOT be displayed after entering a wrong PIN");
        logger.info("[TC_LOGIN_007] Dashboard is not displayed — correct behaviour");

        getExtentTest().pass("User remains on PIN screen after wrong PIN — Dashboard not reached");
        logger.info("[TC_LOGIN_007] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_008 — Wrong PIN shows the "Change pin" link
    // ----------------------------------------------------------------

    @Test(
            priority = 8,
            description = "Verify that the 'Change pin' link is displayed after entering a wrong PIN"
    )
    public void verifyChangePinLinkAppearsAfterWrongPin() {
        logger.info("[TC_LOGIN_008] Initialising PinPage");
        getExtentTest().info("Entering wrong PIN and verifying 'Change pin' link");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before entering wrong PIN");

        pinPage.enterPin(WRONG_PIN);
        logger.info("[TC_LOGIN_008] Wrong PIN entered: {}", WRONG_PIN);

        Assert.assertTrue(pinPage.isChangePinLinkDisplayed(),
                "'Change pin' link should be visible after an incorrect PIN attempt");
        logger.info("[TC_LOGIN_008] 'Change pin' link is displayed");

        getExtentTest().pass("'Change pin' link is present after wrong PIN entry");
        logger.info("[TC_LOGIN_008] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_009 — Wrong PIN shows both error and "Change pin" together
    // ----------------------------------------------------------------

    @Test(
            priority = 9,
            description = "Verify that both 'Incorrect pin.' error and 'Change pin' link appear together after a wrong PIN"
    )
    public void verifyWrongPinShowsErrorAndChangePinTogether() {
        logger.info("[TC_LOGIN_009] Initialising PinPage");
        getExtentTest().info("Entering wrong PIN and verifying both error and Change pin link");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before entering wrong PIN");

        pinPage.enterPin(WRONG_PIN);
        logger.info("[TC_LOGIN_009] Wrong PIN entered: {}", WRONG_PIN);

        Assert.assertTrue(pinPage.isIncorrectPinErrorDisplayed(),
                "'Incorrect pin.' error should be visible");
        logger.info("[TC_LOGIN_009] 'Incorrect pin.' error is displayed");

        Assert.assertTrue(pinPage.isChangePinLinkDisplayed(),
                "'Change pin' link should be visible alongside the error");
        logger.info("[TC_LOGIN_009] 'Change pin' link is displayed alongside error");

        getExtentTest().pass("Both 'Incorrect pin.' error and 'Change pin' link are displayed together");
        logger.info("[TC_LOGIN_009] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_010 — Recovery: correct PIN after wrong PIN reaches Dashboard
    // ----------------------------------------------------------------

    @Test(
            priority = 10,
            description = "Verify that entering the correct PIN after a wrong attempt still reaches the Dashboard"
    )
    public void verifyCorrectPinAfterWrongPinReachesDashboard() {
        ConfigReader config = ConfigReader.getInstance();

        logger.info("[TC_LOGIN_010] Initialising PinPage");
        getExtentTest().info("Step 1: Entering wrong PIN");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before entering wrong PIN");

        pinPage.enterPin(WRONG_PIN);
        logger.info("[TC_LOGIN_010] Wrong PIN entered: {}", WRONG_PIN);

        Assert.assertTrue(pinPage.isIncorrectPinErrorDisplayed(),
                "'Incorrect pin.' error should appear after wrong PIN");
        logger.info("[TC_LOGIN_010] Error message confirmed — clearing filled dots before re-entry");

        getExtentTest().info("Step 2: Clearing PIN field (4 backspaces) then entering correct PIN");
        pinPage.clearPinField(WRONG_PIN.length());
        logger.info("[TC_LOGIN_010] PIN field cleared via backspace x{}", WRONG_PIN.length());

        pinPage.enterPin(config.getLoginPin());
        logger.info("[TC_LOGIN_010] Correct PIN entered digit by digit");

        getExtentTest().info("Step 3: Verifying Dashboard is displayed");
        DashboardPage dashboardPage = new DashboardPage(getDriver());

        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
                "Dashboard should be displayed after entering the correct PIN following a wrong attempt");
        logger.info("[TC_LOGIN_010] Dashboard is displayed — recovery login successful");

        getExtentTest().pass("Dashboard reached after entering correct PIN following a wrong attempt");
        logger.info("[TC_LOGIN_010] PASSED");
    }

    // ================================================================
    // EDGE CASE TEST CASES
    // ================================================================

    // ----------------------------------------------------------------
    // TC_LOGIN_011 — Backspace removes a digit and PIN screen remains
    // ----------------------------------------------------------------

    @Test(
            priority = 11,
            description = "Verify that tapping backspace after entering a digit keeps the user on the PIN screen"
    )
    public void verifyBackspaceKeepsUserOnPinScreen() {
        logger.info("[TC_LOGIN_011] Initialising PinPage");
        getExtentTest().info("Tapping a digit then backspace — verifying PIN screen remains");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before testing backspace");

        pinPage.tapDigit("1");
        logger.info("[TC_LOGIN_011] Tapped digit '1'");

        pinPage.tapBackspace();
        logger.info("[TC_LOGIN_011] Tapped backspace");

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen should still be displayed after tapping a digit and then backspace");
        logger.info("[TC_LOGIN_011] PIN screen remains after backspace");

        getExtentTest().pass("PIN screen remains after digit entry followed by backspace");
        logger.info("[TC_LOGIN_011] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_012 — Partial PIN (3 digits) does not auto-submit
    // ----------------------------------------------------------------

    @Test(
            priority = 12,
            description = "Verify that entering only 3 digits does not auto-submit and the PIN screen remains"
    )
    public void verifyPartialPinDoesNotAutoSubmit() {
        logger.info("[TC_LOGIN_012] Initialising PinPage");
        getExtentTest().info("Entering 3 digits — verifying no auto-submit");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before testing partial entry");

        // Enter only 3 of the 4 required digits
        pinPage.tapDigit("1").tapDigit("4").tapDigit("5");
        logger.info("[TC_LOGIN_012] Entered 3 digits (partial PIN)");

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen should remain after entering only 3 digits — auto-submit requires 4");
        logger.info("[TC_LOGIN_012] PIN screen still displayed — no premature submission");

        DashboardPage dashboardPage = new DashboardPage(getDriver());
        Assert.assertTrue(dashboardPage.isDashboardNotDisplayed(),
                "Dashboard should NOT appear after entering only 3 digits");
        logger.info("[TC_LOGIN_012] Dashboard not shown — correct behaviour");

        getExtentTest().pass("Partial PIN (3 digits) does not trigger auto-submit");
        logger.info("[TC_LOGIN_012] PASSED");
    }

    // ----------------------------------------------------------------
    // TC_LOGIN_013 — "Change pin" link navigates away from the PIN screen
    // ----------------------------------------------------------------

    @Test(
            priority = 13,
            description = "Verify that tapping 'Change pin' navigates away from the PIN screen"
    )
    public void verifyChangePinLinkNavigatesAwayFromPinScreen() {
        logger.info("[TC_LOGIN_013] Initialising PinPage");
        getExtentTest().info("Entering wrong PIN then tapping 'Change pin'");
        PinPage pinPage = new PinPage(getDriver());

        Assert.assertTrue(pinPage.isPinScreenDisplayed(),
                "PIN screen must be visible before testing Change pin");

        pinPage.enterPin(WRONG_PIN);
        logger.info("[TC_LOGIN_013] Wrong PIN entered to reveal 'Change pin' link");

        Assert.assertTrue(pinPage.isChangePinLinkDisplayed(),
                "'Change pin' link must be visible before tapping it");

        pinPage.tapChangePin();
        logger.info("[TC_LOGIN_013] Tapped 'Change pin' link");

        Assert.assertFalse(pinPage.isPinScreenDisplayed(),
                "PIN screen should no longer be displayed after tapping 'Change pin'");
        logger.info("[TC_LOGIN_013] PIN screen is gone — navigation occurred");

        getExtentTest().pass("'Change pin' link navigated away from the PIN screen");
        logger.info("[TC_LOGIN_013] PASSED");
    }
}
