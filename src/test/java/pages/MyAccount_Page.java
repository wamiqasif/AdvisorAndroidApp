package pages;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Advisor "Account Details" (My Account) screen.
 *
 * Locator strategy notes (from the captured UiAutomator2 hierarchy):
 *  - Field LABELS ("Email Address", "Name", "Phone", "Date of Birth") are exposed
 *    as content-desc, so accessibilityId is stable for them.
 *  - Field VALUES are exposed as the `text` attribute on android.view.View (NOT
 *    content-desc), so value locators use text / textMatches and getText().
 *  - Value patterns use [0-9] rather than the \d shorthand: Appium's UiSelector
 *    string parser does not apply Java escape processing, so a backslash-d reaches
 *    UiAutomator literally and never matches (same convention as InvestorAccountPage).
 *  - The top-right header icon has no content-desc, so a positional ImageView is used.
 *
 * IMPORTANT: Logout is intentionally never tapped here. Tapping it destroys the PIN
 * setup and forces a full OTP re-registration, breaking all automation (project rule).
 * Only its visibility is asserted.
 */
public class MyAccount_Page extends BasePage {

    // ============================================================
    // ENTRY POINT / NAVIGATION
    // ============================================================

    /** Menu/profile entry that opens this screen. Same label as the on-screen heading. */
    private final By accountDetailsEntry = AppiumBy.accessibilityId("Account Details");

    private final By backButton = AppiumBy.accessibilityId("Go back");

    // ============================================================
    // SCREEN ANCHORS
    // ============================================================

    private final By accountDetailsHeading = AppiumBy.accessibilityId("Account Details");
    private final By editAccountDetailsButton = AppiumBy.accessibilityId("Edit Account Details");

    /** Top-right header icon: no content-desc, positional (first ImageView in the tree). */
    private final By headerActionIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(0)");

    // ============================================================
    // FIELD LABELS (content-desc)
    // ============================================================

    private final By emailAddressLabel = AppiumBy.accessibilityId("Email Address");
    private final By nameLabel = AppiumBy.accessibilityId("Name");
    private final By phoneLabel = AppiumBy.accessibilityId("Phone");
    private final By dateOfBirthLabel = AppiumBy.accessibilityId("Date of Birth");

    // ============================================================
    // FIELD VALUES (exposed as `text`, not content-desc)
    // ============================================================

    /** Email always contains "@". */
    private final By emailValue = AppiumBy
            .androidUIAutomator("new UiSelector().textContains(\"@\")");

    /** Full name: two or more whitespace-separated words of letters. */
    private final By nameValue = AppiumBy
            .androidUIAutomator("new UiSelector().textMatches(\"[A-Za-z]+( [A-Za-z]+)+\")");

    /** Date of birth in dd-MMM-yyyy form, e.g. "18-Jun-1993". */
    private final By dateOfBirthValue = AppiumBy
            .androidUIAutomator("new UiSelector().textMatches(\"[0-9]{2}-[A-Za-z]{3}-[0-9]{4}\")");

    /** Calendar icon next to DOB: positional (second ImageView). */
    private final By dobCalendarIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(1)");

    // ============================================================
    // LOGOUT (visibility only - NEVER tapped, see class javadoc)
    // ============================================================

    private final By logoutButton = AppiumBy.accessibilityId("Logout");

    // ============================================================
    // EDIT SCREEN ANCHORS (post-navigation, after Edit Account Details)
    // ============================================================

    private final By editFormInput = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");
    private final By saveButton = AppiumBy.accessibilityId("Save");
    private final By updateButton = AppiumBy.accessibilityId("Update");

    // ============================================================

    public MyAccount_Page(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /**
     * Opens the Account Details screen. If already on it, returns immediately.
     * Otherwise taps the "Account Details" entry (scrolling to it if needed).
     */
    public MyAccount_Page openAccountDetailsScreen() {
        logger.info("Opening Account Details screen");

        if (isAccountDetailsScreenDisplayed()) {
            logger.info("Account Details screen already open");
            return this;
        }

        if (!isDisplayed(accountDetailsEntry)) {
            scrollDownUntilVisible(accountDetailsEntry, 8);
        }
        safeClick(accountDetailsEntry);
        waitForAccountDetailsScreen();
        logger.info("Account Details screen opened");
        return this;
    }

    private void waitForAccountDetailsScreen() {
        try {
            wait.until(driver -> isAccountDetailsScreenDisplayed());
        } catch (TimeoutException e) {
            throw new AssertionError("Account Details screen failed to load", e);
        }
    }

    // ============================================================
    // SCREEN DETECTION
    // ============================================================

    public boolean isAccountDetailsScreenDisplayed() {
        return isDisplayed(accountDetailsHeading) && isDisplayed(editAccountDetailsButton);
    }

    // ============================================================
    // LABEL VISIBILITY
    // ============================================================

    public boolean isEmailAddressLabelDisplayed() {
        logger.info("Checking Email Address label visibility");
        waitForVisible(emailAddressLabel);
        return isDisplayed(emailAddressLabel);
    }

    public boolean isNameLabelDisplayed() {
        logger.info("Checking Name label visibility");
        waitForVisible(nameLabel);
        return isDisplayed(nameLabel);
    }

    public boolean isPhoneLabelDisplayed() {
        logger.info("Checking Phone label visibility");
        waitForVisible(phoneLabel);
        return isDisplayed(phoneLabel);
    }

    public boolean isDateOfBirthLabelDisplayed() {
        logger.info("Checking Date of Birth label visibility");
        waitForVisible(dateOfBirthLabel);
        return isDisplayed(dateOfBirthLabel);
    }

    // ============================================================
    // VALUE VISIBILITY
    // ============================================================

    public boolean isEmailValueDisplayed() {
        logger.info("Checking email value visibility");
        waitForVisible(emailValue);
        return isDisplayed(emailValue);
    }

    public boolean isNameValueDisplayed() {
        logger.info("Checking name value visibility");
        waitForVisible(nameValue);
        return isDisplayed(nameValue);
    }

    public boolean isDateOfBirthValueDisplayed() {
        logger.info("Checking date of birth value visibility");
        waitForVisible(dateOfBirthValue);
        return isDisplayed(dateOfBirthValue);
    }

    public boolean isPhoneFieldDisplayed() {
        logger.info("Checking phone field/label visibility");
        return isDisplayed(phoneLabel);
    }

    public boolean isDobCalendarIconDisplayed() {
        return isDisplayed(dobCalendarIcon);
    }

    // ============================================================
    // VALUE GETTERS  (values live in `text`, so read getText())
    // ============================================================

    public String getEmailAddress() {
        try {
            return waitForVisible(emailValue).getText();
        } catch (Exception e) {
            logger.warn("Unable to fetch email address");
            return "";
        }
    }

    public String getName() {
        try {
            return waitForVisible(nameValue).getText();
        } catch (Exception e) {
            logger.warn("Unable to fetch name");
            return "";
        }
    }

    public String getDateOfBirth() {
        try {
            return waitForVisible(dateOfBirthValue).getText();
        } catch (Exception e) {
            logger.warn("Unable to fetch date of birth");
            return "";
        }
    }

    // ============================================================
    // LINK VISIBILITY
    // ============================================================

    public boolean isBackButtonDisplayed() {
        return isDisplayed(backButton);
    }

    public boolean isEditAccountDetailsDisplayed() {
        waitForVisible(editAccountDetailsButton);
        return isDisplayed(editAccountDetailsButton);
    }

    public boolean isHeaderActionIconDisplayed() {
        return isDisplayed(headerActionIcon);
    }

    public boolean isLogoutDisplayed() {
        logger.info("Checking Logout button visibility (visibility only - never tapped)");
        waitForVisible(logoutButton);
        return isDisplayed(logoutButton);
    }

    /** All non-destructive navigable links on this screen are present. */
    public boolean isAllLinksDisplayed() {
        boolean back = isBackButtonDisplayed();
        boolean edit = isEditAccountDetailsDisplayed();
        boolean header = isHeaderActionIconDisplayed();
        logger.info("Link visibility - Back:{}, EditAccountDetails:{}, HeaderIcon:{}", back, edit, header);
        return back && edit && header;
    }

    // ============================================================
    // TAP + VALIDATE OPENED SCREEN + RETURN
    // Tap each (safe) link and validate the screen that opens.
    // Logout is excluded - see class javadoc.
    // ============================================================

    /**
     * Taps "Edit Account Details", validates that an editable form opens
     * (EditText field and/or a Save/Update action), then returns to this screen.
     */
    public boolean tapEditAccountDetailsAndVerify() {
        logger.info("=== Link test: Edit Account Details ===");
        safeClick(editAccountDetailsButton);
        waitForUiToSettle();

        boolean editScreen = isDisplayed(editFormInput, 8)
                || isAnyDisplayed(saveButton, updateButton)
                || leftAccountDetailsScreen(2);

        logger.info("Edit Account Details screen opened: {}", editScreen);
        returnToAccountDetailsScreen();
        logger.info("=== Link test: Edit Account Details - done ===");
        return editScreen;
    }

    /**
     * Taps the top-right header icon, validates that a new screen opens
     * (the Account Details anchor disappears), then returns.
     */
    public boolean tapHeaderActionIconAndVerify() {
        logger.info("=== Link test: Header action icon ===");
        if (!isHeaderActionIconDisplayed()) {
            logger.warn("Header action icon not visible - skipping");
            return false;
        }
        safeClick(headerActionIcon);
        waitForUiToSettle();

        boolean opened = leftAccountDetailsScreen(8);
        logger.info("Header action icon opened a new screen: {}", opened);
        returnToAccountDetailsScreen();
        logger.info("=== Link test: Header action icon - done ===");
        return opened;
    }

    /**
     * Taps "Go back" and verifies navigation away from the Account Details screen.
     */
    public boolean tapBackAndVerify() {
        logger.info("=== Link test: Back ===");
        safeClick(backButton);
        waitForUiToSettle();
        boolean leftScreen = !isAccountDetailsScreenDisplayed();
        logger.info("Navigated away from Account Details: {}", leftScreen);
        logger.info("=== Link test: Back - done ===");
        return leftScreen;
    }

    /**
     * Validates every NON-destructive link on the screen and reports per-link results.
     * Logout is deliberately not tapped (would force OTP re-registration); its
     * presence is asserted separately.
     */
    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("Validating all Account Details links (Logout excluded by design)");
        List<LinkResult> results = new ArrayList<>();

        results.add(new LinkResult("Edit Account Details", tapEditAccountDetailsAndVerify()));
        results.add(new LinkResult("Header action icon", tapHeaderActionIconAndVerify()));
        results.add(new LinkResult("Logout (presence only)", isLogoutDisplayed()));

        logger.info("Account Details link validation complete: {}", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                return false;
            }
        }
        return true;
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    public MyAccount_Page tapBack() {
        logger.info("Tapping back on Account Details screen");
        safeClick(backButton);
        waitForUiToSettle();
        return this;
    }

    // ============================================================
    // INTERNAL HELPERS
    // ============================================================

    /**
     * Lightweight check that we navigated away from the Account Details screen,
     * i.e. its anchor (heading + edit button) is no longer present. Uses
     * findElements-backed isDisplayed rather than getPageSource (full Flutter
     * tree dumps are heavy and can destabilize the UiAutomator2 bridge).
     */
    private boolean leftAccountDetailsScreen(int seconds) {
        try {
            shortWait(seconds).until(driver -> !isAccountDetailsScreenDisplayed());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Best-effort return to the Account Details screen after visiting a link target. */
    private boolean returnToAccountDetailsScreen() {
        if (isAccountDetailsScreenDisplayed()) {
            return true;
        }
        try {
            driver.navigate().back();
            shortWait(8).until(driver -> isAccountDetailsScreenDisplayed());
            return true;
        } catch (Exception firstFailure) {
            logger.info("System back did not restore Account Details, trying header back button");
        }
        try {
            if (isDisplayed(backButton, 3)) {
                safeClick(backButton);
                shortWait(8).until(driver -> isAccountDetailsScreenDisplayed());
                return true;
            }
        } catch (Exception ignored) {
        }
        return isAccountDetailsScreenDisplayed();
    }

    private boolean scrollDownUntilVisible(By locator, int maxSwipes) {
        for (int swipe = 0; swipe <= maxSwipes; swipe++) {
            if (isDisplayed(locator)) {
                return true;
            }
            safeVerticalScroll("up");
            waitForUiToSettle();
        }
        return isDisplayed(locator);
    }

    // ============================================================

    public static class LinkResult {

        public final String name;
        public final boolean passed;

        public LinkResult(String name, boolean passed) {
            this.name = name;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return name + "=" + passed;
        }
    }
}
