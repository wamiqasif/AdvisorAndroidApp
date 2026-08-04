package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import com.valueresearch.utils.OtpEmailReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppSettingsPage {

    private final AndroidDriver driver;
    private String advisorAppPackage = "";

    private static final String ADVISOR_PIN = "1975";
    private static final String ADVISOR_APP_PACKAGE = "com.valueresearch.advisor";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    public AppSettingsPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForAppSettings() {
        advisorAppPackage = getCurrentPackageSafely();

        if (advisorAppPackage == null || advisorAppPackage.trim().isEmpty()) {
            advisorAppPackage = ADVISOR_APP_PACKAGE;
        }

        ReportLogger.pass("Advisor app package captured for App Settings: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForAppSettings() {
        ReportLogger.step("Checking Advisor app login/session state for App Settings");

        activateAdvisorAppIfNeeded();
        waitForAppToBeInteractive();

        if (isAppSettingsScreenVisible() || isHubScreenVisible() || isMainAppLoaded()) {
            ReportLogger.pass("Advisor app session is already active for App Settings");
            return;
        }

        if (isPinScreenVisible()) {
            ReportLogger.step("PIN screen detected. Entering Advisor PIN");
            enterAdvisorPin();
            waitForMainAppAfterPin();
            ReportLogger.pass("Advisor app login/session confirmed after PIN");
            return;
        }

        throw new AssertionError("Unable to confirm Advisor app login/session state"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void openHubFromDashboardForAppSettings() {
        ReportLogger.step("Opening Hub tab from dashboard/home");

        activateAdvisorAppIfNeeded();
        waitForAppToBeInteractive();

        if (isHubScreenVisible()) {
            ReportLogger.pass("Hub screen is already visible");
            return;
        }

        if (isAppSettingsScreenVisible()) {
            ReportLogger.pass("App Settings screen is already open, Hub navigation skipped");
            return;
        }

        returnToDashboardIfNeeded();

        if (!tapHubTabSafely()) {
            throw new AssertionError("Unable to open Hub tab"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForHubScreenReady();
        ReportLogger.pass("Hub tab opened successfully");
    }

    public void openAppSettingsFromHubForAppSettings() {
        ReportLogger.step("Opening App Settings from Hub");

        if (isAppSettingsScreenVisible()) {
            ReportLogger.pass("App Settings screen is already visible");
            return;
        }

        if (!isHubScreenVisible()) {
            openHubFromDashboardForAppSettings();
        }

        if (!tapAnyVisibleText("App Settings")) {
            throw new AssertionError("Unable to tap App Settings from Hub"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForAppSettingsScreenReady();
        ReportLogger.pass("App Settings screen opened from Hub");
    }

    public void validateAppSettingsScreenStructureForAppSettings() {
        ReportLogger.step("Validating App Settings screen structure");

        waitForAppSettingsScreenReady();

        assertAnyTextVisible("Settings");
        assertAnyTextVisible("Portfolio");
        assertAnyTextVisible("App");
        assertAnyTextVisible("Check for Updates");
        assertAnyTextVisible("Change PIN");
        assertAnyTextVisible("Storage Settings");

        if (!containsAny(collectVisibleStrings(), "Version", "Build")) {
            throw new AssertionError("Version/build text is not visible on App Settings screen"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("App Settings structure validated: Settings + Portfolio/App tabs + App options + version/build");
    }

    public void ensureAppSettingsScreenReadyForAppSettings() {
        ReportLogger.step("Ensuring App Settings screen is ready without reopening Hub unnecessarily");

        activateAdvisorAppIfNeeded();
        waitForAppToBeInteractive();

        if (isChangePinScreenVisible() || isStorageScreenVisible()) {
            returnToAppSettingsFromSubPage();
        }

        if (isAppSettingsScreenVisible()) {
            ensureAppSettingsScreenOnAppTab();
            ReportLogger.pass("App Settings screen is already ready");
            return;
        }

        if (isHubScreenVisible()) {
            openAppSettingsFromHubForAppSettings();
            ensureAppSettingsScreenOnAppTab();
            ReportLogger.pass("App Settings screen opened from existing Hub screen");
            return;
        }

        if (!isMainAppLoaded()) {
            ensureAdvisorAppLoggedInForAppSettings();
        }

        openHubFromDashboardForAppSettings();
        openAppSettingsFromHubForAppSettings();
        ensureAppSettingsScreenOnAppTab();

        ReportLogger.pass("App Settings screen is ready");
    }

    public void resetAppSettingsToAppTabForNextTest() {
        ReportLogger.step("Resetting App Settings screen to App tab for next test");

        activateAdvisorAppIfNeeded();

        if (isChangePinScreenVisible() || isStorageScreenVisible()) {
            returnToAppSettingsFromSubPage();
            return;
        }

        if (isAppSettingsScreenVisible()) {
            ensureAppSettingsScreenOnAppTab();
            return;
        }

        if (isHubScreenVisible()) {
            openAppSettingsFromHubForAppSettings();
            ensureAppSettingsScreenOnAppTab();
        }
    }

    public void validateCheckForUpdatesFlowForAppSettings() {
        ReportLogger.step("Validating Check for Updates flow");

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Check for Updates")) {
            throw new AssertionError("Unable to tap Check for Updates"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        if (!waitForPlayStoreAdvisorPageVisible(15)) {
            throw new AssertionError("Google Play Advisor app page did not open after Check for Updates"
                    + " | currentPackage=" + getCurrentPackageSafely()
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Check for Updates opened Google Play page for Value Research Advisor");

        returnToAdvisorAppFromExternalApp();
        waitForAppSettingsScreenReady();

        ReportLogger.pass("Returned from Google Play to App Settings screen");
    }

    public void validateChangePinSameExistingPinErrorForAppSettings() {
        ReportLogger.step("Validating Change PIN negative flow with same existing PIN using two-screen flow");

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Change PIN")) {
            throw new AssertionError("Unable to tap Change PIN"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForAnyTextVisible(Arrays.asList("Change your pin", "Enter your pin"), 10);
        ReportLogger.pass("Change PIN screen opened");

        ReportLogger.step("Entering existing PIN as new PIN");
        enterPinByVisibleKeypad(ADVISOR_PIN);

        waitForReEnterPinScreenForAppSettings("same existing PIN negative validation");

        ReportLogger.step("Re-entering existing PIN as confirm PIN");
        enterPinByVisibleKeypad(ADVISOR_PIN);

        waitForAnyTextVisible(
                Arrays.asList(
                        "New PIN cannot be the same as existing PIN",
                        "same as existing PIN",
                        "cannot be the same"
                ),
                10
        );

        ReportLogger.pass("Change PIN negative validation passed: same existing PIN is blocked");

        pressBackSilently();
        sleep(1200);

        ensureAppSettingsScreenReadyForAppSettings();
    }
    
    public void validateActualChangePinAndRestoreForAppSettings() {
        ReportLogger.step("Validating actual Change PIN flow with one email OTP");

        String temporaryPin = "1976";

        performActualChangePinFlowForAppSettings(
                temporaryPin,
                "change current PIN to temporary PIN 1976 using one email OTP"
        );

        ReportLogger.pass("Actual Change PIN completed successfully. Current PIN is now 1976.");
    }
    public void validateStorageSettingsScreenForAppSettings() {
        ReportLogger.step("Validating Storage Settings screen");

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Storage Settings")) {
            throw new AssertionError("Unable to tap Storage Settings"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForStorageScreenReady();

        assertAnyTextVisible("Storage");
        assertAnyTextVisible("Free Space");
        assertAnyTextVisible("Clear cache");

        if (!containsAny(collectVisibleStrings(), "free up storage", "cache", "downloads won't be removed", "MB")) {
            throw new AssertionError("Storage details text is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Storage Settings screen validated: Storage + Free Space + Clear cache");

        returnToAppSettingsFromSubPage();
    }

    public void validatePortfolioSettingsAndSaveForAppSettings() {
        ReportLogger.step("Validating Portfolio Settings tab and Save Changes flow");

        waitForAppSettingsScreenReady();

        if (!tapAnyVisibleText("Portfolio")) {
            throw new AssertionError("Unable to tap Portfolio tab"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForPortfolioSettingsReady();

        assertAnyTextVisible("Currency");
        assertAnyTextVisible("INR");
        assertAnyTextVisible("Unit of value");
        assertAnyTextVisible("Lakh-Crore");
        assertAnyTextVisible("Million-Billion");
        assertAnyTextVisible("Stock Exchange Priority");
        assertAnyTextVisible("NSE / BSE");
        assertAnyTextVisible("BSE / NSE");
        assertAnyTextVisible("Include fully sold investment in returns");
        assertAnyTextVisible("Yes");
        assertAnyTextVisible("No");
        assertAnyTextVisible("Hide fully sold investments from view");
        assertAnyTextVisible("Save Changes");

        ReportLogger.pass("Portfolio Settings structure validated");

        if (!tapAnyVisibleText("Save Changes")) {
            throw new AssertionError("Unable to tap Save Changes on Portfolio Settings"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForAnyTextVisible(
                Arrays.asList("Please note", "Portfolio Setting Updated", "Portfolio Settings Updated", "Updated"),
                12
        );

        ReportLogger.pass("Portfolio Settings Save Changes flow validated successfully");
    }

    public void returnBackToAdvisorAppSafely() {
        ReportLogger.step("Returning back to Advisor App after App Settings validation");

        activateAdvisorAppIfNeeded();

        for (int attempt = 1; attempt <= 7; attempt++) {
            if (isMainAppLoaded() && !isAppSettingsScreenVisible() && !isChangePinScreenVisible() && !isStorageScreenVisible()) {
                ReportLogger.pass("Advisor App dashboard/home is visible");
                return;
            }

            if (isHubScreenVisible() && !isAppSettingsScreenVisible()) {
                ReportLogger.pass("Advisor App Hub screen is visible after cleanup");
                return;
            }

            pressBackSilently();
            sleep(900);
        }

        if (isMainAppLoaded() || isHubScreenVisible()) {
            ReportLogger.pass("Advisor App is active after cleanup");
            return;
        }

        ReportLogger.debug("Could not fully confirm dashboard return after App Settings flow"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void verifyAppSettingsCompleteFlow() {
        ReportLogger.step("Verifying complete App Settings flow");

        captureAdvisorAppPackageForAppSettings();
        ensureAdvisorAppLoggedInForAppSettings();
        openHubFromDashboardForAppSettings();
        openAppSettingsFromHubForAppSettings();
        validateAppSettingsScreenStructureForAppSettings();
        validateCheckForUpdatesFlowForAppSettings();
        validateChangePinSameExistingPinErrorForAppSettings();
        validateStorageSettingsScreenForAppSettings();
        validatePortfolioSettingsAndSaveForAppSettings();

        ReportLogger.pass("Complete App Settings flow validated successfully");
    }

    // =========================================================
    // SCREEN READINESS HELPERS
    // =========================================================

    private void waitForHubScreenReady() {
        waitForAnyTextVisible(Arrays.asList("Profile", "Investor Accounts", "App Settings", "Account Details"), 12);

        if (!isHubScreenVisible()) {
            throw new AssertionError("Hub screen did not become ready"
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private void waitForAppSettingsScreenReady() {
        for (int i = 1; i <= 16; i++) {
            if (isAppSettingsScreenVisible()) {
                return;
            }

            sleep(400);
        }

        throw new AssertionError("App Settings screen is not ready"
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void waitForChangePinScreenReady() {
        waitForAnyTextVisible(Arrays.asList("Change your pin", "Enter your pin"), 10);

        if (!isChangePinScreenVisible()) {
            throw new AssertionError("Change PIN screen is not ready"
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private void waitForStorageScreenReady() {
        waitForAnyTextVisible(Arrays.asList("Storage", "Free Space", "Clear cache"), 10);

        if (!isStorageScreenVisible()) {
            throw new AssertionError("Storage screen is not ready"
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private void waitForPortfolioSettingsReady() {
        waitForAnyTextVisible(Arrays.asList("Currency", "INR", "Unit of value", "Save Changes"), 15);

        if (!isPortfolioSettingsVisible()) {
            throw new AssertionError("Portfolio Settings tab is not ready"
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private boolean isHubScreenVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values, "Profile", "Investor Accounts", "Mutual Funds")
                && containsAny(values, "App Settings", "Account Details", "Portfolio Settings")
                && containsAny(values, "Funds", "Stocks", "Portfolio", "Hub");
    }

    private boolean isAppSettingsScreenVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values, "Settings")
                && containsAny(values, "Portfolio")
                && containsAny(values, "App")
                && containsAny(values, "Check for Updates", "Change PIN", "Storage Settings", "Currency", "Save Changes");
    }

    private boolean isChangePinScreenVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values, "Change your pin")
                && containsAny(values, "Enter your pin", "Re-Enter your pin");
    }

    private boolean isStorageScreenVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values, "Storage")
                && containsAny(values, "Free Space", "Clear cache");
    }

    private boolean isPortfolioSettingsVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values, "Settings")
                && containsAny(values, "Currency")
                && containsAny(values, "Unit of value")
                && containsAny(values, "Save Changes");
    }

    private boolean waitForPlayStoreAdvisorPageVisible(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            List<String> values = collectVisibleStrings();
            String currentPackage = getCurrentPackageSafely();

            boolean playStorePackage = currentPackage.toLowerCase().contains("vending")
                    || currentPackage.toLowerCase().contains("google");

            boolean advisorPage = containsAny(values, "Value Research Advisor", "Independent Advisors Private Limited")
                    && containsAny(values, "Open", "Uninstall", "What's new", "Google Play");

            if (playStorePackage && advisorPage) {
                return true;
            }

            if (advisorPage) {
                return true;
            }

            sleep(600);
        }

        return false;
    }

    private void ensureAppSettingsScreenOnAppTab() {
        waitForAppSettingsScreenReady();

        if (containsAny(collectVisibleStrings(), "Check for Updates", "Change PIN", "Storage Settings")) {
            return;
        }

        if (!tapAnyVisibleText("App")) {
            throw new AssertionError("Unable to switch to App tab on Settings screen"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForAnyTextVisible(Arrays.asList("Check for Updates", "Change PIN", "Storage Settings"), 8);
    }

    // =========================================================
    // NAVIGATION HELPERS
    // =========================================================

    private void returnToAdvisorAppFromExternalApp() {
        ReportLogger.step("Returning from external app to Advisor App");

        activateAdvisorAppIfNeeded();

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (isAppSettingsScreenVisible()) {
                return;
            }

            if (isHubScreenVisible()) {
                openAppSettingsFromHubForAppSettings();
                return;
            }

            if (isMainAppLoaded()) {
                openHubFromDashboardForAppSettings();
                openAppSettingsFromHubForAppSettings();
                return;
            }

            pressBackSilently();
            sleep(900);
            activateAdvisorAppIfNeeded();
        }

        throw new AssertionError("Unable to return to Advisor App Settings after external app"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void returnToAppSettingsFromSubPage() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isAppSettingsScreenVisible()) {
                ensureAppSettingsScreenOnAppTab();
                return;
            }

            pressBackSilently();
            sleep(800);
        }

        if (isHubScreenVisible()) {
            openAppSettingsFromHubForAppSettings();
            return;
        }

        throw new AssertionError("Unable to return to App Settings screen from sub-page"
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void returnToDashboardIfNeeded() {
        if (isMainAppLoaded() && !isAppSettingsScreenVisible() && !isChangePinScreenVisible() && !isStorageScreenVisible()) {
            return;
        }

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (isMainAppLoaded() && !isAppSettingsScreenVisible() && !isChangePinScreenVisible() && !isStorageScreenVisible()) {
                return;
            }

            pressBackSilently();
            sleep(900);
        }

        if (!isMainAppLoaded()) {
            activateAdvisorAppIfNeeded();
            sleep(1000);
        }
    }

    private boolean tapHubTabSafely() {
        By[] hubLocators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().description(\"Hub\")"),
                By.xpath("//*[@content-desc='Hub']")
        };

        for (By locator : hubLocators) {
            WebElement element = waitForElementFast(locator, 2);

            if (element != null) {
                ReportLogger.step("Tapping Hub tab by exact locator");
                tapElementCenter(element);

                if (waitForHubVisibleSilently(8)) {
                    return true;
                }
            }
        }

        WebElement hubText = findVisibleExactTextElement("Hub");

        if (hubText != null) {
            ReportLogger.step("Tapping Hub tab by visible text");
            tapElementCenter(hubText);

            if (waitForHubVisibleSilently(8)) {
                return true;
            }
        }

        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.88);
        int y = (int) (size.getHeight() * 0.94);

        ReportLogger.step("Tapping Hub tab by coordinate fallback | x=" + x + " | y=" + y);
        tapByCoordinates(x, y);

        return waitForHubVisibleSilently(8);
    }

    private boolean waitForHubVisibleSilently(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (isHubScreenVisible()) {
                return true;
            }

            sleep(500);
        }

        return false;
    }

    private void activateAdvisorAppIfNeeded() {
        try {
            String packageToActivate = advisorAppPackage;

            if (packageToActivate == null || packageToActivate.trim().isEmpty()) {
                packageToActivate = ADVISOR_APP_PACKAGE;
            }

            String currentPackage = getCurrentPackageSafely();

            if (!packageToActivate.equals(currentPackage)) {
                driver.activateApp(packageToActivate);
                sleep(1200);
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp skipped/failed: " + cleanError(e.getMessage()));
        }
    }

    // =========================================================
    // LOGIN / SESSION HELPERS
    // =========================================================

    private boolean isPinScreenVisible() {
        List<String> values = collectVisibleStrings();

        return containsAny(values,
                "Enter your Advisor PIN",
                "Advisor PIN",
                "PIN",
                "Hi,"
        );
    }

    private boolean isMainAppLoaded() {
        List<String> values = collectVisibleStrings();

        return containsAny(values,
                "Funds",
                "Portfolio",
                "Hub",
                "Clients",
                "Reports",
                "Search",
                "Portfolio Value",
                "Rich Future Starts Here",
                "Shourya Pratap Singh"
        );
    }

    private void enterAdvisorPin() {
        enterAdvisorPinValue(ADVISOR_PIN);
    }

    private void enterAdvisorPinValue(String pin) {
        for (char digit : pin.toCharArray()) {
            tapPinDigit(String.valueOf(digit));
            sleep(350);
        }
    }

    private void tapPinDigit(String digit) {
        WebElement digitElement = findVisibleExactTextElement(digit);

        if (digitElement != null) {
            tapElementCenter(digitElement);
            ReportLogger.step("Tapped PIN digit: " + digit);
            return;
        }

        tapPinDigitByCoordinate(digit);
        ReportLogger.step("Tapped PIN digit by coordinate fallback: " + digit);
    }

    private void waitForMainAppAfterPin() {
        ReportLogger.step("Waiting for Advisor app dashboard after PIN");

        for (int i = 1; i <= 25; i++) {
            if (isMainAppLoaded()) {
                ReportLogger.pass("Advisor app dashboard loaded after PIN");
                return;
            }

            sleep(800);
        }

        throw new AssertionError("Advisor app dashboard did not load after PIN"
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void enterPinByVisibleKeypad(String pin) {
        for (char digit : pin.toCharArray()) {
            tapChangePinKeypadDigit(String.valueOf(digit));
            sleep(350);
        }
    }

    private void tapChangePinKeypadDigit(String digit) {
        WebElement digitElement = findVisibleKeypadDigitElement(digit);

        if (digitElement != null) {
            tapElementCenter(digitElement);
            ReportLogger.step("Tapped Change PIN keypad digit: " + digit);
            return;
        }

        ReportLogger.debug("Visible keypad digit locator failed for " + digit + ". Using coordinate fallback.");
        tapPinDigitByCoordinate(digit);
        ReportLogger.step("Tapped Change PIN keypad digit by coordinate fallback: " + digit);
    }

    private WebElement findVisibleKeypadDigitElement(String digit) {
        Dimension size = driver.manage().window().getSize();
        List<WebElement> matchingElements = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerY < (int) (size.getHeight() * 0.52)) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (digit.equals(text) || digit.equals(desc) || digit.equals(name) || digit.equals(attrText)) {
                        matchingElements.add(element);
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable element.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleKeypadDigitElement skipped: " + cleanError(e.getMessage()));
        }

        if (matchingElements.isEmpty()) {
            return null;
        }

        matchingElements.sort((left, right) -> {
            Rectangle leftRect = left.getRect();
            Rectangle rightRect = right.getRect();
            int leftArea = leftRect.getWidth() * leftRect.getHeight();
            int rightArea = rightRect.getWidth() * rightRect.getHeight();

            int areaCompare = Integer.compare(leftArea, rightArea);
            if (areaCompare != 0) {
                return areaCompare;
            }

            return Integer.compare(rightRect.getY(), leftRect.getY());
        });

        return matchingElements.get(0);
    }

    private void waitForChangePinStepToAdvance(String completedStep) {
        sleep(700);

        if (!isChangePinScreenVisible()) {
            return;
        }

        List<String> values = collectVisibleStrings();
        ReportLogger.debug("Change PIN step completed: " + completedStep + " | visibleValues=" + values);
    }

    private void performActualChangePinFlowForAppSettings(String newPin, String flowLabel) {
        ReportLogger.step("Starting actual Change PIN two-screen OTP flow: " + flowLabel);

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Change PIN")) {
            throw new AssertionError("Unable to tap Change PIN"
                    + " | flow=" + flowLabel
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitForAnyTextVisible(Arrays.asList("Change your pin", "Enter your pin"), 10);
        ReportLogger.pass("Change PIN screen opened: " + flowLabel);

        ReportLogger.step("Entering new PIN on first screen: " + flowLabel);
        enterPinByVisibleKeypad(newPin);

        waitForReEnterPinScreenForAppSettings(flowLabel);

        ReportLogger.step("Re-entering new PIN on confirm screen: " + flowLabel);
        enterPinByVisibleKeypad(newPin);

        waitForChangePinOtpSheetForAppSettings(flowLabel);

        String otp = OtpEmailReader.fetchLatestOtp();

        handleChangePinOtpUsingOtpPage(otp, flowLabel);

        waitAfterOtpVerifyForChangePin(newPin, flowLabel);

        ReportLogger.pass("Actual Change PIN two-screen OTP flow completed: " + flowLabel);
    }
    
    private void handleChangePinOtpUsingOtpPage(String otp, String flowLabel) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new AssertionError("Valid 6 digit OTP is required for Change PIN flow"
                    + " | flow=" + flowLabel
                    + " | otp=" + otp);
        }

        ReportLogger.step("Handling Change PIN OTP using existing OtpPage: " + flowLabel);

        OtpPage otpPage = new OtpPage(driver);

        otpPage.waitForOtpScreen();
        otpPage.enterOtp(otp);
        otpPage.clickVerifyIfVisible();

        ReportLogger.pass("Change PIN OTP submitted using existing OtpPage: " + flowLabel);
    }
    
    private void waitForChangePinOtpSheetForAppSettings(String flowLabel) {
        long endTime = System.currentTimeMillis() + 20000L;

        while (System.currentTimeMillis() < endTime) {
            List<String> values = collectVisibleStrings();

            if (containsAny(values,
                    "OTP sent successfully",
                    "Enter OTP",
                    "Verify OTP",
                    "Resend OTP",
                    "Change pin")) {
                ReportLogger.pass("Change PIN OTP sheet is visible: " + flowLabel);
                return;
            }

            if (containsAny(values,
                    "Pin Not Matched",
                    "PIN Not Matched",
                    "Pin Not Matched !!!")) {
                throw new AssertionError("PIN mismatch appeared before OTP screen"
                        + " | flow=" + flowLabel
                        + " | visibleValues=" + values);
            }

            if (containsAny(values,
                    "New PIN cannot be the same as existing PIN",
                    "same as existing PIN",
                    "cannot be the same")) {
                throw new AssertionError("Same existing PIN validation appeared unexpectedly"
                        + " | flow=" + flowLabel
                        + " | visibleValues=" + values);
            }

            sleep(500);
        }

        throw new AssertionError("OTP sheet did not appear after Change PIN"
                + " | flow=" + flowLabel
                + " | visibleValues=" + collectVisibleStrings());
    }
    
    private void waitAfterOtpVerifyForChangePin(String activePinAfterChange, String flowLabel) {
        long endTime = System.currentTimeMillis() + 25000L;

        while (System.currentTimeMillis() < endTime) {
            List<String> values = collectVisibleStrings();

            if (containsAny(values,
                    "PIN changed successfully",
                    "Pin changed successfully",
                    "OTP verified successfully",
                    "successfully")) {
                ReportLogger.pass("Change PIN success message visible after OTP: " + flowLabel);
                sleep(1500);
                ensureAppSettingsScreenReadyForAppSettings();
                return;
            }

            if (isAppSettingsScreenVisible()) {
                ensureAppSettingsScreenOnAppTab();
                ReportLogger.pass("Returned to App Settings after OTP verification: " + flowLabel);
                return;
            }

            if (containsAny(values,
                    "Invalid OTP",
                    "Incorrect OTP",
                    "OTP expired",
                    "Please enter valid OTP")) {
                throw new AssertionError("OTP verification failed"
                        + " | flow=" + flowLabel
                        + " | visibleValues=" + values);
            }

            sleep(700);
        }

        throw new AssertionError("Change PIN did not complete after OTP verification"
                + " | flow=" + flowLabel
                + " | visibleValues=" + collectVisibleStrings());
    }
    
    private void waitForReEnterPinScreenForAppSettings(String flowLabel) {
        long endTime = System.currentTimeMillis() + 12000L;

        while (System.currentTimeMillis() < endTime) {
            List<String> values = collectVisibleStrings();

            if (containsAny(values, "Re-Enter your pin", "Re-enter your pin", "Re Enter your pin")) {
                ReportLogger.pass("Re-Enter PIN screen visible: " + flowLabel);
                return;
            }

            if (containsAny(values, "Pin Not Matched", "PIN Not Matched", "Pin Not Matched !!!")) {
                throw new AssertionError("PIN mismatch appeared before confirm screen was ready"
                        + " | flow=" + flowLabel
                        + " | visibleValues=" + values);
            }

            if (containsAny(values,
                    "New PIN cannot be the same as existing PIN",
                    "same as existing PIN",
                    "cannot be the same")) {
                throw new AssertionError("New PIN same-as-existing validation appeared"
                        + " | flow=" + flowLabel
                        + " | visibleValues=" + values);
            }

            sleep(500);
        }

        throw new AssertionError("Re-Enter PIN screen did not appear"
                + " | flow=" + flowLabel
                + " | visibleValues=" + collectVisibleStrings());
    }
    private void waitForActualChangePinSuccessOrSafeState(String activePinAfterChange, String flowLabel) {
        long endTime = System.currentTimeMillis() + 16000L;

        while (System.currentTimeMillis() < endTime) {
            List<String> values = collectVisibleStrings();

            if (containsAny(values,
                    "PIN changed successfully",
                    "Pin changed successfully",
                    "pin changed successfully",
                    "PIN updated successfully",
                    "Pin updated successfully",
                    "changed successfully",
                    "updated successfully")) {
                ReportLogger.pass("Actual Change PIN success message visible for flow: " + flowLabel);
                returnToAppSettingsFromAnyChangePinSuccessState(activePinAfterChange);
                return;
            }

            if (containsAny(values,
                    "Incorrect PIN",
                    "Wrong PIN",
                    "Invalid PIN",
                    "PIN does not match",
                    "Pins do not match",
                    "PINs do not match",
                    "cannot be the same",
                    "same as existing PIN")) {
                throw new AssertionError("Actual Change PIN flow showed error state"
                        + " | flow=" + flowLabel
                        + " | visibleValues=" + values);
            }

            if (isPinScreenVisible()) {
                ReportLogger.step("Advisor PIN screen appeared after actual PIN change. Entering active PIN to continue: " + flowLabel);
                enterAdvisorPinValue(activePinAfterChange);
                waitForMainAppAfterPin();
                ensureAppSettingsScreenReadyForAppSettings();
                return;
            }

            if (isAppSettingsScreenVisible()) {
                ensureAppSettingsScreenOnAppTab();
                ReportLogger.pass("Returned to App Settings after actual Change PIN flow: " + flowLabel);
                return;
            }

            if (!isChangePinScreenVisible() && (isMainAppLoaded() || isHubScreenVisible())) {
                ensureAppSettingsScreenReadyForAppSettings();
                ReportLogger.pass("Returned to Advisor app after actual Change PIN flow: " + flowLabel);
                return;
            }

            sleep(500);
        }

        throw new AssertionError("Actual Change PIN flow did not show success or safe return state"
                + " | flow=" + flowLabel
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void returnToAppSettingsFromAnyChangePinSuccessState(String activePinAfterChange) {
        if (isPinScreenVisible()) {
            enterAdvisorPinValue(activePinAfterChange);
            waitForMainAppAfterPin();
            ensureAppSettingsScreenReadyForAppSettings();
            return;
        }

        if (isAppSettingsScreenVisible()) {
            ensureAppSettingsScreenOnAppTab();
            return;
        }

        if (isChangePinScreenVisible()) {
            returnToAppSettingsFromSubPage();
            return;
        }

        ensureAppSettingsScreenReadyForAppSettings();
    }

    private void tapPinDigitByCoordinate(String digit) {
        Dimension size = driver.manage().window().getSize();

        double xRatio;
        double yRatio;

        switch (digit) {
            case "1":
                xRatio = 0.22;
                yRatio = 0.680;
                break;
            case "2":
                xRatio = 0.50;
                yRatio = 0.680;
                break;
            case "3":
                xRatio = 0.78;
                yRatio = 0.680;
                break;
            case "4":
                xRatio = 0.22;
                yRatio = 0.758;
                break;
            case "5":
                xRatio = 0.50;
                yRatio = 0.758;
                break;
            case "6":
                xRatio = 0.78;
                yRatio = 0.758;
                break;
            case "7":
                xRatio = 0.22;
                yRatio = 0.835;
                break;
            case "8":
                xRatio = 0.50;
                yRatio = 0.835;
                break;
            case "9":
                xRatio = 0.78;
                yRatio = 0.835;
                break;
            case "0":
                xRatio = 0.50;
                yRatio = 0.915;
                break;
            default:
                throw new IllegalArgumentException("Unsupported PIN digit: " + digit);
        }

        tapByCoordinates((int) (size.getWidth() * xRatio), (int) (size.getHeight() * yRatio));
    }

    // =========================================================
    // ELEMENT HELPERS
    // =========================================================

    private void assertAnyTextVisible(String text) {
        if (!isVisibleByAnyText(text)) {
            throw new AssertionError("Expected text is not visible: " + text
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Visible text validated: " + text);
    }

    private boolean tapAnyVisibleText(String text) {
        WebElement element = findVisibleTextElement(text);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);
        sleep(500);
        return true;
    }

    private WebElement findVisibleExactTextElement(String expectedText) {
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (expectedText.equals(text)
                            || expectedText.equals(desc)
                            || expectedText.equals(name)
                            || expectedText.equals(attrText)) {
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleExactTextElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private WebElement findVisibleTextElement(String expectedText) {
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (expectedText.equals(text)
                            || expectedText.equals(desc)
                            || expectedText.equals(name)
                            || expectedText.equals(attrText)
                            || text.contains(expectedText)
                            || desc.contains(expectedText)
                            || name.contains(expectedText)
                            || attrText.contains(expectedText)) {
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleTextElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean isVisibleByAnyText(String text) {
        return findVisibleTextElement(text) != null;
    }

    private WebElement waitForElementFast(By locator, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (isElementUsable(element)) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Retry until timeout.
            }

            sleep(250);
        }

        return null;
    }

    private void waitForAppToBeInteractive() {
        for (int i = 1; i <= 12; i++) {
            List<String> values = collectVisibleStrings();

            if (!values.isEmpty()) {
                return;
            }

            sleep(600);
        }
    }

    private void waitForAnyTextVisible(List<String> possibleTexts, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            List<String> values = collectVisibleStrings();

            for (String text : possibleTexts) {
                if (containsAny(values, text)) {
                    return;
                }
            }

            sleep(400);
        }

        throw new AssertionError("None of the expected texts visible within timeout"
                + " | expected=" + possibleTexts
                + " | visibleValues=" + collectVisibleStrings());
    }

    private List<String> collectVisibleStrings() {
        List<String> values = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (!isElementUsable(element)) {
                        continue;
                    }

                    addUniqueValue(values, element.getText());
                    addUniqueValue(values, element.getAttribute("content-desc"));
                    addUniqueValue(values, element.getAttribute("text"));
                    addUniqueValue(values, element.getAttribute("name"));

                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("collectVisibleStrings skipped: " + cleanError(e.getMessage()));
        }

        return values;
    }

    private void addUniqueValue(List<String> values, String rawValue) {
        if (rawValue == null) {
            return;
        }

        String clean = normalizeSpaces(rawValue);

        if (clean.isEmpty()) {
            return;
        }

        if (!values.contains(clean)) {
            values.add(clean);
        }

        String[] parts = rawValue.split("\\n");

        for (String part : parts) {
            String cleanPart = normalizeSpaces(part);

            if (!cleanPart.isEmpty() && !values.contains(cleanPart)) {
                values.add(cleanPart);
            }
        }
    }

    private boolean containsAny(List<String> values, String... expectedTexts) {
        for (String value : values) {
            String cleanValue = normalizeSpaces(value).toLowerCase();

            for (String expectedText : expectedTexts) {
                if (expectedText == null) {
                    continue;
                }

                String cleanExpected = normalizeSpaces(expectedText).toLowerCase();

                if (!cleanExpected.isEmpty() && cleanValue.contains(cleanExpected)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isElementUsable(WebElement element) {
        try {
            if (element == null || !element.isDisplayed()) {
                return false;
            }

            Rectangle rect = element.getRect();
            Dimension size = driver.manage().window().getSize();

            if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                return false;
            }

            int centerX = rect.getX() + rect.getWidth() / 2;
            int centerY = rect.getY() + rect.getHeight() / 2;

            return centerX >= 0
                    && centerX <= size.getWidth()
                    && centerY >= 0
                    && centerY <= size.getHeight();

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // GESTURE HELPERS
    // =========================================================

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();
        Dimension size = driver.manage().window().getSize();

        int x = rect.getX() + rect.getWidth() / 2;
        int y = rect.getY() + rect.getHeight() / 2;

        x = clamp(x, 1, size.getWidth() - 2);
        y = clamp(y, 1, size.getHeight() - 2);

        tapByCoordinates(x, y);
    }

    private void tapByCoordinates(int x, int y) {
        Dimension size = driver.manage().window().getSize();

        x = clamp(x, 1, size.getWidth() - 2);
        y = clamp(y, 1, size.getHeight() - 2);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);

        tap.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                x,
                y
        ));

        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(tap));
    }

    private void pressBackSilently() {
        try {
            driver.navigate().back();
        } catch (Exception e) {
            ReportLogger.debug("Back press failed: " + cleanError(e.getMessage()));
        }
    }

    // =========================================================
    // COMMON HELPERS
    // =========================================================

    private String getCurrentPackageSafely() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return normalizeSpaces(message);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}