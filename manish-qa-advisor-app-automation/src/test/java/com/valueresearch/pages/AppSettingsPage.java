package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.OtpEmailReader;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppSettingsPage {

    private final AndroidDriver driver;

    private String advisorAppPackage = "";

    // =========================================================
    // CONFIGURATION
    // =========================================================

    private static final String ADVISOR_APP_PACKAGE =
            "com.valueresearch.advisor";

    private static final String PLAY_STORE_PACKAGE =
            "com.android.vending";

    /*
     * Reuse the framework-wide PIN configuration.
     * AuthHelper also reads appPin from config.properties / environment.
     */
    private static final String ADVISOR_PIN =
            ConfigReader.getOptional("appPin", "1975");

    private static final String TEMPORARY_PIN =
            resolveSetting(
                    "advisor.temporary.pin",
                    "ADVISOR_TEMPORARY_PIN",
                    "1976"
            );

    // =========================================================
    // STABLE LOCATORS
    // =========================================================

    /*
     * Confirmed from Appium Inspector:
     *
     * accessibility id = Hub
     */
    private static final By HUB_TAB =
            AppiumBy.accessibilityId("Hub");

    private static final By HUB_TAB_DESCRIPTION =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"Hub\")"
            );

    /*
     * Confirmed from Appium Inspector:
     *
     * class        = android.widget.Button
     * content-desc = Manage your app settings
     *
     * This is the PRIMARY locator for App Settings.
     */
    private static final By APP_SETTINGS_TILE =
            AppiumBy.accessibilityId("Manage your app settings");

    private static final By APP_SETTINGS_TILE_DESCRIPTION =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"Manage your app settings\")"
            );

    /*
     * Safe fallback in case wording is slightly expanded in future.
     * Still semantic - no absolute XPath / coordinates.
     */
    private static final By APP_SETTINGS_TILE_DESCRIPTION_CONTAINS =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"app settings\")"
            );

    private static final By PORTFOLIO_SETTINGS_TILE =
            AppiumBy.accessibilityId("Manage your portfolio settings");

    private static final By FUNDS_TAB =
            AppiumBy.accessibilityId("Funds");

    private static final By STOCKS_TAB =
            AppiumBy.accessibilityId("Stocks");

    private static final By PORTFOLIO_TAB =
            AppiumBy.accessibilityId("Portfolio");

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AppSettingsPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForAppSettings() {

        String currentPackage = getCurrentPackageSafely();

        /*
         * Never accidentally remember Play Store or another external
         * application as the Advisor package.
         */
        if (ADVISOR_APP_PACKAGE.equals(currentPackage)) {
            advisorAppPackage = currentPackage;
        } else {
            advisorAppPackage = ADVISOR_APP_PACKAGE;
        }

        ReportLogger.pass(
                "Advisor app package captured for App Settings: "
                        + advisorAppPackage
        );
    }

    public void ensureAdvisorAppLoggedInForAppSettings() {

        ReportLogger.step(
                "Checking Advisor app login/session state for App Settings"
        );

        activateAdvisorAppIfNeeded();

        /*
         * If App Settings/Hub is already open, the user is authenticated and
         * there is no reason to force navigation back to the dashboard.
         */
        if (isAppSettingsScreenVisible() || isHubScreenVisible()) {

            ReportLogger.pass(
                    "Advisor app session is already active for App Settings"
            );

            return;
        }

        /*
         * IMPORTANT:
         *
         * Do not treat the first FrameLayout as proof that the Flutter screen
         * is ready. On the QA emulator the Advisor activity can remain on a
         * blank/white frame for several seconds before PIN/dashboard semantics
         * are exposed.
         *
         * The project already has AuthHelper for this exact problem. It waits
         * up to the configured long wait (60s in QA), detects PIN/email-login/
         * logged-in states, and uses config.properties appPin.
         */
        new AuthHelper(driver).ensureLoggedIn();

        /*
         * AuthHelper returns only after a valid logged-in state is detected.
         * Keep this page-specific assertion so failures remain easy to diagnose.
         */
        if (!isMainAppLoaded() && !isHubScreenVisible() && !isAppSettingsScreenVisible()) {

            throw new AssertionError(
                    "Advisor login helper completed but App Settings module "
                            + "could not confirm an authenticated Advisor state"
                            + " | currentPackage="
                            + getCurrentPackageSafely()
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        ReportLogger.pass(
                "Advisor app login/session confirmed for App Settings"
        );
    }

    public void openHubFromDashboardForAppSettings() {

        ReportLogger.step(
                "Opening Hub tab from dashboard/home"
        );

        activateAdvisorAppIfNeeded();

        waitForAppToBeInteractive();

        if (isHubScreenVisible()) {

            ReportLogger.pass(
                    "Hub screen is already visible"
            );

            return;
        }

        if (isAppSettingsScreenVisible()) {

            ReportLogger.pass(
                    "App Settings screen is already open, Hub navigation skipped"
            );

            return;
        }

        returnToDashboardIfNeeded();

        if (!tapHubTabSafely()) {

            throw new AssertionError(
                    "Unable to open Hub tab"
                            + " | currentPackage="
                            + getCurrentPackageSafely()
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForHubScreenReady();

        ReportLogger.pass(
                "Hub tab opened successfully"
        );
    }

    public void openAppSettingsFromHubForAppSettings() {

        ReportLogger.step(
                "Opening App Settings from Hub"
        );

        if (isAppSettingsScreenVisible()) {

            ReportLogger.pass(
                    "App Settings screen is already visible"
            );

            return;
        }

        if (!isHubScreenVisible()) {
            openHubFromDashboardForAppSettings();
        }

        if (!tapAppSettingsTileSafely()) {

            throw new AssertionError(
                    "Unable to tap App Settings from Hub"
                            + " | expectedAccessibilityId="
                            + "Manage your app settings"
                            + " | currentPackage="
                            + getCurrentPackageSafely()
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForAppSettingsScreenReady();

        ReportLogger.pass(
                "App Settings screen opened from Hub"
        );
    }

    public void validateAppSettingsScreenStructureForAppSettings() {

        ReportLogger.step(
                "Validating App Settings screen structure"
        );

        waitForAppSettingsScreenReady();

        assertAnyTextVisible("Settings");
        assertAnyTextVisible("Portfolio");
        assertAnyTextVisible("App");
        assertAnyTextVisible("Check for Updates");
        assertAnyTextVisible("Change PIN");
        assertAnyTextVisible("Storage Settings");

        if (!isAnyTextVisibleFast("Version", "Build")) {

            throw new AssertionError(
                    "Version/build text is not visible on App Settings screen"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        ReportLogger.pass(
                "App Settings structure validated: "
                        + "Settings + Portfolio/App tabs + "
                        + "App options + version/build"
        );
    }

    public void ensureAppSettingsScreenReadyForAppSettings() {

        ReportLogger.step(
                "Ensuring App Settings screen is ready "
                        + "without reopening Hub unnecessarily"
        );

        activateAdvisorAppIfNeeded();

        waitForAppToBeInteractive();

        if (isChangePinScreenVisible()
                || isStorageScreenVisible()) {

            returnToAppSettingsFromSubPage();
        }

        if (isAppSettingsScreenVisible()) {

            ensureAppSettingsScreenOnAppTab();

            ReportLogger.pass(
                    "App Settings screen is already ready"
            );

            return;
        }

        if (isHubScreenVisible()) {

            openAppSettingsFromHubForAppSettings();

            ensureAppSettingsScreenOnAppTab();

            ReportLogger.pass(
                    "App Settings screen opened from existing Hub screen"
            );

            return;
        }

        if (!isMainAppLoaded()) {
            ensureAdvisorAppLoggedInForAppSettings();
        }

        openHubFromDashboardForAppSettings();

        openAppSettingsFromHubForAppSettings();

        ensureAppSettingsScreenOnAppTab();

        ReportLogger.pass(
                "App Settings screen is ready"
        );
    }

    public void resetAppSettingsToAppTabForNextTest() {

        ReportLogger.step(
                "Resetting App Settings screen to App tab for next test"
        );

        activateAdvisorAppIfNeeded();

        /*
         * Cleanup must NEVER become the reason the following tests are
         * configuration failures.
         */

        if (isChangePinScreenVisible()
                || isStorageScreenVisible()) {

            returnToAppSettingsFromSubPage();
            return;
        }

        if (isAppSettingsScreenVisible()) {

            ensureAppSettingsScreenOnAppTab();
            return;
        }

        /*
         * IMPORTANT:
         *
         * Do not reopen App Settings from @AfterMethod.
         * If the actual test failed while on Hub/dashboard,
         * the next test's preparation method will recover from there.
         */
        if (isHubScreenVisible()) {

            ReportLogger.debug(
                    "Reset skipped because Hub is visible. "
                            + "Next test will reopen App Settings."
            );

            return;
        }

        if (isMainAppLoaded()) {

            ReportLogger.debug(
                    "Reset skipped because Advisor main screen is visible. "
                            + "Next test will reopen App Settings."
            );
        }
    }

    public void validateCheckForUpdatesFlowForAppSettings() {

        ReportLogger.step(
                "Validating Check for Updates flow"
        );

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Check for Updates")) {

            throw new AssertionError(
                    "Unable to tap Check for Updates"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        if (!waitForPlayStoreAdvisorPageVisible(15)) {

            throw new AssertionError(
                    "Google Play Advisor app page did not open "
                            + "after Check for Updates"
                            + " | currentPackage="
                            + getCurrentPackageSafely()
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        ReportLogger.pass(
                "Check for Updates opened Google Play page "
                        + "for Value Research Advisor"
        );

        returnToAdvisorAppFromExternalApp();

        waitForAppSettingsScreenReady();

        ReportLogger.pass(
                "Returned from Google Play to App Settings screen"
        );
    }

    public void validateChangePinSameExistingPinErrorForAppSettings() {

        ReportLogger.step(
                "Validating Change PIN negative flow "
                        + "with same existing PIN using two-screen flow"
        );

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Change PIN")) {

            throw new AssertionError(
                    "Unable to tap Change PIN"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForAnyTextVisible(
                Arrays.asList(
                        "Change your pin",
                        "Enter your pin"
                ),
                10
        );

        ReportLogger.pass(
                "Change PIN screen opened"
        );

        ReportLogger.step(
                "Entering existing PIN as new PIN"
        );

        enterPinByVisibleKeypad(ADVISOR_PIN);

        waitForReEnterPinScreenForAppSettings(
                "same existing PIN negative validation"
        );

        ReportLogger.step(
                "Re-entering existing PIN as confirm PIN"
        );

        enterPinByVisibleKeypad(ADVISOR_PIN);

        waitForAnyTextVisible(
                Arrays.asList(
                        "New PIN cannot be the same as existing PIN",
                        "same as existing PIN",
                        "cannot be the same"
                ),
                10
        );

        ReportLogger.pass(
                "Change PIN negative validation passed: "
                        + "same existing PIN is blocked"
        );

        pressBackSilently();

        sleep(1200);

        ensureAppSettingsScreenReadyForAppSettings();
    }

    public void validateActualChangePinAndRestoreForAppSettings() {

        ReportLogger.step(
                "Validating actual Change PIN flow with one email OTP"
        );

        performActualChangePinFlowForAppSettings(
                TEMPORARY_PIN,
                "change current PIN to temporary PIN using one email OTP"
        );

        ReportLogger.pass(
                "Actual Change PIN completed successfully."
        );
    }

    public void validateStorageSettingsScreenForAppSettings() {

        ReportLogger.step(
                "Validating Storage Settings screen"
        );

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Storage Settings")) {

            throw new AssertionError(
                    "Unable to tap Storage Settings"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForStorageScreenReady();

        assertAnyTextVisible("Storage");
        assertAnyTextVisible("Free Space");
        assertAnyTextVisible("Clear cache");

        if (!isAnyTextVisibleFast(
                "free up storage",
                "cache",
                "downloads won't be removed",
                "MB"
        )) {

            throw new AssertionError(
                    "Storage details text is not visible"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        ReportLogger.pass(
                "Storage Settings screen validated: "
                        + "Storage + Free Space + Clear cache"
        );

        returnToAppSettingsFromSubPage();
    }

    public void validatePortfolioSettingsAndSaveForAppSettings() {

        ReportLogger.step(
                "Validating Portfolio Settings tab and Save Changes flow"
        );

        waitForAppSettingsScreenReady();

        if (!tapAnyVisibleText("Portfolio")) {

            throw new AssertionError(
                    "Unable to tap Portfolio tab"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
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
        assertAnyTextVisible(
                "Include fully sold investment in returns"
        );
        assertAnyTextVisible("Yes");
        assertAnyTextVisible("No");
        assertAnyTextVisible(
                "Hide fully sold investments from view"
        );
        assertAnyTextVisible("Save Changes");

        ReportLogger.pass(
                "Portfolio Settings structure validated"
        );

        if (!tapAnyVisibleText("Save Changes")) {

            throw new AssertionError(
                    "Unable to tap Save Changes on Portfolio Settings"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForAnyTextVisible(
                Arrays.asList(
                        "Please note",
                        "Portfolio Setting Updated",
                        "Portfolio Settings Updated",
                        "Updated"
                ),
                12
        );

        ReportLogger.pass(
                "Portfolio Settings Save Changes flow "
                        + "validated successfully"
        );
    }

    public void returnBackToAdvisorAppSafely() {

        ReportLogger.step(
                "Returning back to Advisor App "
                        + "after App Settings validation"
        );

        activateAdvisorAppIfNeeded();

        for (int attempt = 1;
             attempt <= 7;
             attempt++) {

            if (isMainAppLoaded()
                    && !isAppSettingsScreenVisible()
                    && !isChangePinScreenVisible()
                    && !isStorageScreenVisible()) {

                ReportLogger.pass(
                        "Advisor App dashboard/home is visible"
                );

                return;
            }

            if (isHubScreenVisible()
                    && !isAppSettingsScreenVisible()) {

                ReportLogger.pass(
                        "Advisor App Hub screen is visible after cleanup"
                );

                return;
            }

            pressBackSilently();

            sleep(700);
        }

        if (isMainAppLoaded()
                || isHubScreenVisible()) {

            ReportLogger.pass(
                    "Advisor App is active after cleanup"
            );

            return;
        }

        ReportLogger.debug(
                "Could not fully confirm dashboard return "
                        + "after App Settings flow"
                        + " | currentPackage="
                        + getCurrentPackageSafely()
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    public void verifyAppSettingsCompleteFlow() {

        ReportLogger.step(
                "Verifying complete App Settings flow"
        );

        captureAdvisorAppPackageForAppSettings();

        ensureAdvisorAppLoggedInForAppSettings();

        openHubFromDashboardForAppSettings();

        openAppSettingsFromHubForAppSettings();

        validateAppSettingsScreenStructureForAppSettings();

        validateCheckForUpdatesFlowForAppSettings();

        validateChangePinSameExistingPinErrorForAppSettings();

        validateStorageSettingsScreenForAppSettings();

        validatePortfolioSettingsAndSaveForAppSettings();

        ReportLogger.pass(
                "Complete App Settings flow validated successfully"
        );
    }

    // =========================================================
    // SCREEN READINESS HELPERS
    // =========================================================

    private void waitForHubScreenReady() {

        long endTime =
                System.currentTimeMillis() + 12000L;

        while (System.currentTimeMillis() < endTime) {

            if (isHubScreenVisible()) {
                return;
            }

            sleep(350);
        }

        throw new AssertionError(
                "Hub screen did not become ready"
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void waitForAppSettingsScreenReady() {

        long endTime =
                System.currentTimeMillis() + 10000L;

        while (System.currentTimeMillis() < endTime) {

            if (isAppSettingsScreenVisible()) {
                return;
            }

            sleep(350);
        }

        throw new AssertionError(
                "App Settings screen is not ready"
                        + " | currentPackage="
                        + getCurrentPackageSafely()
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void waitForStorageScreenReady() {

        waitForAnyTextVisible(
                Arrays.asList(
                        "Storage",
                        "Free Space",
                        "Clear cache"
                ),
                10
        );

        if (!isStorageScreenVisible()) {

            throw new AssertionError(
                    "Storage screen is not ready"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }
    }

    private void waitForPortfolioSettingsReady() {

        waitForAnyTextVisible(
                Arrays.asList(
                        "Currency",
                        "INR",
                        "Unit of value",
                        "Save Changes"
                ),
                15
        );

        if (!isPortfolioSettingsVisible()) {

            throw new AssertionError(
                    "Portfolio Settings tab is not ready"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }
    }

    private boolean isHubScreenVisible() {

        /*
         * Strongest Hub signal:
         * the Inspector-confirmed App Settings profile tile.
         */
        if (isLocatorVisible(APP_SETTINGS_TILE)) {
            return true;
        }

        /*
         * Secondary Hub profile tile.
         */
        if (isLocatorVisible(PORTFOLIO_SETTINGS_TILE)) {
            return true;
        }

        /*
         * Fallback for a Hub screen that has been scrolled.
         */
        return isLocatorVisible(HUB_TAB)
                && isAnyTextVisibleFast(
                "Profile",
                "Investor Accounts",
                "Mutual Funds",
                "Account Details",
                "Subscription Details",
                "App Settings"
        );
    }

    private boolean isAppSettingsScreenVisible() {

        boolean settingsTitle =
                isVisibleByAnyText("Settings");

        boolean settingsTab =
                isVisibleByAnyText("Portfolio")
                        || isVisibleByAnyText("App");

        boolean settingsContent =
                isAnyTextVisibleFast(
                        "Check for Updates",
                        "Change PIN",
                        "Storage Settings",
                        "Currency",
                        "Unit of value",
                        "Save Changes"
                );

        return settingsTitle
                && settingsTab
                && settingsContent;
    }

    private boolean isChangePinScreenVisible() {

        return isVisibleByAnyText(
                "Change your pin"
        )
                && isAnyTextVisibleFast(
                "Enter your pin",
                "Re-Enter your pin",
                "Re-enter your pin",
                "Re Enter your pin"
        );
    }

    private boolean isStorageScreenVisible() {

        return isVisibleByAnyText("Storage")
                && isAnyTextVisibleFast(
                "Free Space",
                "Clear cache"
        );
    }

    private boolean isPortfolioSettingsVisible() {

        return isVisibleByAnyText("Settings")
                && isVisibleByAnyText("Currency")
                && isVisibleByAnyText("Unit of value")
                && isVisibleByAnyText("Save Changes");
    }

    private boolean waitForPlayStoreAdvisorPageVisible(
            int timeoutSeconds
    ) {

        long endTime =
                System.currentTimeMillis()
                        + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endTime) {

            String currentPackage =
                    getCurrentPackageSafely();

            boolean playStorePackage =
                    PLAY_STORE_PACKAGE.equals(currentPackage)
                            || currentPackage
                            .toLowerCase()
                            .contains("vending");

            boolean advisorPage =
                    isAnyTextVisibleFast(
                            "Value Research Advisor",
                            "Independent Advisors Private Limited"
                    );

            boolean playStoreAction =
                    isAnyTextVisibleFast(
                            "Open",
                            "Uninstall",
                            "What's new",
                            "Google Play"
                    );

            if (advisorPage
                    && (playStorePackage
                    || playStoreAction)) {

                return true;
            }

            sleep(500);
        }

        return false;
    }

    private void ensureAppSettingsScreenOnAppTab() {

        waitForAppSettingsScreenReady();

        if (isAnyTextVisibleFast(
                "Check for Updates",
                "Change PIN",
                "Storage Settings"
        )) {
            return;
        }

        if (!tapAnyVisibleText("App")) {

            throw new AssertionError(
                    "Unable to switch to App tab "
                            + "on Settings screen"
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForAnyTextVisible(
                Arrays.asList(
                        "Check for Updates",
                        "Change PIN",
                        "Storage Settings"
                ),
                8
        );
    }

    // =========================================================
    // NAVIGATION HELPERS
    // =========================================================

    private void returnToAdvisorAppFromExternalApp() {

        ReportLogger.step(
                "Returning from external app to Advisor App"
        );

        activateAdvisorAppIfNeeded();

        for (int attempt = 1;
             attempt <= 6;
             attempt++) {

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

            sleep(700);

            activateAdvisorAppIfNeeded();
        }

        throw new AssertionError(
                "Unable to return to Advisor App Settings "
                        + "after external app"
                        + " | currentPackage="
                        + getCurrentPackageSafely()
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void returnToAppSettingsFromSubPage() {

        for (int attempt = 1;
             attempt <= 5;
             attempt++) {

            if (isAppSettingsScreenVisible()) {

                ensureAppSettingsScreenOnAppTab();
                return;
            }

            pressBackSilently();

            sleep(650);
        }

        if (isHubScreenVisible()) {

            openAppSettingsFromHubForAppSettings();

            ensureAppSettingsScreenOnAppTab();

            return;
        }

        throw new AssertionError(
                "Unable to return to App Settings screen "
                        + "from sub-page"
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void returnToDashboardIfNeeded() {

        if (isMainAppLoaded()
                && !isAppSettingsScreenVisible()
                && !isChangePinScreenVisible()
                && !isStorageScreenVisible()) {

            return;
        }

        for (int attempt = 1;
             attempt <= 6;
             attempt++) {

            if (isMainAppLoaded()
                    && !isAppSettingsScreenVisible()
                    && !isChangePinScreenVisible()
                    && !isStorageScreenVisible()) {

                return;
            }

            pressBackSilently();

            sleep(700);
        }

        if (!isMainAppLoaded()) {

            activateAdvisorAppIfNeeded();

            sleep(900);
        }
    }

    private boolean tapHubTabSafely() {

        /*
         * PRIMARY - confirmed from Appium Inspector.
         */
        WebElement hub =
                waitForElementFast(
                        HUB_TAB,
                        4
                );

        if (hub != null) {

            ReportLogger.step(
                    "Tapping Hub tab using accessibility id"
            );

            tapElementCenter(hub);

            if (waitForHubVisibleSilently(8)) {
                return true;
            }
        }

        /*
         * Semantic fallback.
         */
        hub = waitForElementFast(
                HUB_TAB_DESCRIPTION,
                2
        );

        if (hub != null) {

            ReportLogger.step(
                    "Tapping Hub tab using content-desc fallback"
            );

            tapElementCenter(hub);

            if (waitForHubVisibleSilently(8)) {
                return true;
            }
        }

        /*
         * Last semantic fallback.
         * No coordinate fallback here.
         */
        WebElement hubText =
                findVisibleExactTextElement("Hub");

        if (hubText != null) {

            ReportLogger.step(
                    "Tapping Hub tab using visible text fallback"
            );

            tapElementCenter(hubText);

            return waitForHubVisibleSilently(8);
        }

        return false;
    }

    private boolean tapAppSettingsTileSafely() {

        /*
         * PRIMARY locator confirmed by Appium Inspector.
         */
        WebElement element =
                waitForElementFast(
                        APP_SETTINGS_TILE,
                        5
                );

        if (element != null) {

            ReportLogger.step(
                    "Tapping App Settings using accessibility id: "
                            + "Manage your app settings"
            );

            tapElementCenter(element);

            return true;
        }

        /*
         * Exact content-desc fallback.
         */
        element =
                waitForElementFast(
                        APP_SETTINGS_TILE_DESCRIPTION,
                        2
                );

        if (element != null) {

            ReportLogger.step(
                    "Tapping App Settings using exact "
                            + "content-desc fallback"
            );

            tapElementCenter(element);

            return true;
        }

        /*
         * Bounded semantic fallback.
         */
        element =
                waitForElementFast(
                        APP_SETTINGS_TILE_DESCRIPTION_CONTAINS,
                        2
                );

        if (element != null) {

            ReportLogger.step(
                    "Tapping App Settings using "
                            + "descriptionContains fallback"
            );

            tapElementCenter(element);

            return true;
        }

        /*
         * Optional final text fallback.
         *
         * The current app should not reach this because the Inspector
         * confirms "Manage your app settings" as the accessibility id.
         */
        element =
                findVisibleTextElement("App Settings");

        if (element != null) {

            ReportLogger.step(
                    "Tapping App Settings using visible text fallback"
            );

            tapElementCenter(element);

            return true;
        }

        return false;
    }

    private boolean waitForHubVisibleSilently(
            int timeoutSeconds
    ) {

        long endTime =
                System.currentTimeMillis()
                        + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endTime) {

            if (isHubScreenVisible()) {
                return true;
            }

            sleep(350);
        }

        return false;
    }

    private void activateAdvisorAppIfNeeded() {

        try {

            String packageToActivate =
                    advisorAppPackage;

            if (packageToActivate == null
                    || packageToActivate.trim().isEmpty()) {

                packageToActivate =
                        ADVISOR_APP_PACKAGE;
            }

            String currentPackage =
                    getCurrentPackageSafely();

            if (!packageToActivate.equals(currentPackage)) {

                driver.activateApp(packageToActivate);

                sleep(1000);
            }

        } catch (Exception e) {

            ReportLogger.debug(
                    "activateApp skipped/failed: "
                            + cleanError(e.getMessage())
            );
        }
    }

    // =========================================================
    // LOGIN / SESSION HELPERS
    // =========================================================

    private boolean isPinScreenVisible() {

        return isAnyTextVisibleFast(
                "Enter your Advisor PIN",
                "Advisor PIN"
        )
                || (
                isAnyTextVisibleFast("Hi,", "Hi")
                        && !isMainAppLoaded()
        );
    }

    private boolean isMainAppLoaded() {

        /*
         * Fast bottom-nav detection.
         *
         * These accessibility IDs are considerably cheaper than repeatedly
         * scanning every node from //*.
         */
        boolean hubAvailable =
                isLocatorVisible(HUB_TAB);

        boolean anotherMainTabAvailable =
                isLocatorVisible(FUNDS_TAB)
                        || isLocatorVisible(STOCKS_TAB)
                        || isLocatorVisible(PORTFOLIO_TAB);

        if (hubAvailable
                && anotherMainTabAvailable) {

            return true;
        }

        /*
         * Safe fallback for transient dashboard states.
         */
        return isAnyTextVisibleFast(
                "Portfolio Value",
                "Rich Future Starts Here",
                "Search"
        );
    }

    private void enterAdvisorPin() {

        enterAdvisorPinValue(
                ADVISOR_PIN
        );
    }

    private void enterAdvisorPinValue(
            String pin
    ) {

        if (pin == null
                || !pin.matches("\\d{4}")) {

            throw new AssertionError(
                    "Configured Advisor PIN must contain exactly 4 digits"
            );
        }

        for (char digit : pin.toCharArray()) {

            tapPinDigit(
                    String.valueOf(digit)
            );

            sleep(300);
        }
    }

    private void tapPinDigit(
            String digit
    ) {

        WebElement digitElement =
                findVisibleKeypadDigitElement(digit);

        if (digitElement != null) {

            tapElementCenter(digitElement);

            /*
             * Never print the actual PIN digit in logs.
             */
            ReportLogger.step(
                    "Tapped Advisor PIN keypad digit"
            );

            return;
        }

        /*
         * Coordinate usage is only the last fallback for the numeric keypad.
         */
        ReportLogger.debug(
                "PIN digit semantic locator unavailable. "
                        + "Using keypad coordinate fallback."
        );

        tapPinDigitByCoordinate(digit);

        ReportLogger.step(
                "Tapped Advisor PIN keypad digit using fallback"
        );
    }

    private void waitForMainAppAfterPin() {

        ReportLogger.step(
                "Waiting for Advisor app dashboard after PIN"
        );

        long endTime =
                System.currentTimeMillis() + 30000L;

        while (System.currentTimeMillis() < endTime) {

            if (isMainAppLoaded()) {

                ReportLogger.pass(
                        "Advisor app dashboard loaded after PIN"
                );

                return;
            }

            sleep(500);
        }

        throw new AssertionError(
                "Advisor app dashboard did not load after PIN"
                        + " | currentPackage="
                        + getCurrentPackageSafely()
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void enterPinByVisibleKeypad(
            String pin
    ) {

        if (pin == null
                || !pin.matches("\\d{4}")) {

            throw new AssertionError(
                    "Change PIN value must contain exactly 4 digits"
            );
        }

        for (char digit : pin.toCharArray()) {

            tapChangePinKeypadDigit(
                    String.valueOf(digit)
            );

            sleep(300);
        }
    }

    private void tapChangePinKeypadDigit(
            String digit
    ) {

        WebElement digitElement =
                findVisibleKeypadDigitElement(digit);

        if (digitElement != null) {

            tapElementCenter(digitElement);

            ReportLogger.step(
                    "Tapped Change PIN keypad digit"
            );

            return;
        }

        ReportLogger.debug(
                "Change PIN keypad semantic locator failed. "
                        + "Using coordinate fallback."
        );

        tapPinDigitByCoordinate(digit);

        ReportLogger.step(
                "Tapped Change PIN keypad digit using fallback"
        );
    }

    private WebElement findVisibleKeypadDigitElement(
            String digit
    ) {

        Dimension screen =
                driver.manage()
                        .window()
                        .getSize();

        List<WebElement> candidates =
                new ArrayList<>();

        String escapedDigit =
                escapeUiSelector(digit);

        By[] locators = new By[]{
                AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\""
                                + escapedDigit
                                + "\")"
                ),
                AppiumBy.androidUIAutomator(
                        "new UiSelector().description(\""
                                + escapedDigit
                                + "\")"
                ),
                AppiumBy.accessibilityId(digit)
        };

        for (By locator : locators) {

            try {

                List<WebElement> elements =
                        driver.findElements(locator);

                for (WebElement element : elements) {

                    try {

                        if (!isElementUsable(
                                element,
                                screen
                        )) {
                            continue;
                        }

                        Rectangle rect =
                                element.getRect();

                        int centerY =
                                rect.getY()
                                        + rect.getHeight() / 2;

                        /*
                         * Numeric keypad belongs in the lower half of the screen.
                         * This prevents matching unrelated numbers.
                         */
                        if (centerY
                                < (int) (
                                screen.getHeight()
                                        * 0.50
                        )) {
                            continue;
                        }

                        candidates.add(element);

                    } catch (Exception ignored) {
                        // Ignore stale candidate.
                    }
                }

            } catch (Exception ignored) {
                // Try next semantic locator.
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        /*
         * Prefer the smallest matching element.
         * Usually this is the actual keypad number rather than a container.
         */
        candidates.sort(
                (left, right) -> {

                    try {

                        Rectangle leftRect =
                                left.getRect();

                        Rectangle rightRect =
                                right.getRect();

                        int leftArea =
                                leftRect.getWidth()
                                        * leftRect.getHeight();

                        int rightArea =
                                rightRect.getWidth()
                                        * rightRect.getHeight();

                        return Integer.compare(
                                leftArea,
                                rightArea
                        );

                    } catch (Exception ignored) {
                        return 0;
                    }
                }
        );

        return candidates.get(0);
    }

    private void performActualChangePinFlowForAppSettings(
            String newPin,
            String flowLabel
    ) {

        ReportLogger.step(
                "Starting actual Change PIN two-screen OTP flow: "
                        + flowLabel
        );

        ensureAppSettingsScreenOnAppTab();

        if (!tapAnyVisibleText("Change PIN")) {

            throw new AssertionError(
                    "Unable to tap Change PIN"
                            + " | flow="
                            + flowLabel
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        waitForAnyTextVisible(
                Arrays.asList(
                        "Change your pin",
                        "Enter your pin"
                ),
                10
        );

        ReportLogger.pass(
                "Change PIN screen opened: "
                        + flowLabel
        );

        ReportLogger.step(
                "Entering new PIN on first screen: "
                        + flowLabel
        );

        enterPinByVisibleKeypad(newPin);

        waitForReEnterPinScreenForAppSettings(
                flowLabel
        );

        ReportLogger.step(
                "Re-entering new PIN on confirm screen: "
                        + flowLabel
        );

        enterPinByVisibleKeypad(newPin);

        waitForChangePinOtpSheetForAppSettings(
                flowLabel
        );

        /*
         * Small bounded wait for the newly-generated OTP email to arrive.
         */
        sleep(1500);

        String otp =
                OtpEmailReader.fetchLatestOtp();

        handleChangePinOtpUsingOtpPage(
                otp,
                flowLabel
        );

        waitAfterOtpVerifyForChangePin(
                flowLabel
        );

        ReportLogger.pass(
                "Actual Change PIN two-screen OTP flow completed: "
                        + flowLabel
        );
    }

    private void handleChangePinOtpUsingOtpPage(
            String otp,
            String flowLabel
    ) {

        if (otp == null
                || !otp.matches("\\d{6}")) {

            throw new AssertionError(
                    "Valid 6 digit OTP is required "
                            + "for Change PIN flow"
                            + " | flow="
                            + flowLabel
            );
        }

        ReportLogger.step(
                "Handling Change PIN OTP "
                        + "using existing OtpPage: "
                        + flowLabel
        );

        OtpPage otpPage =
                new OtpPage(driver);

        otpPage.waitForOtpScreen();

        otpPage.enterOtp(otp);

        otpPage.clickVerifyIfVisible();

        ReportLogger.pass(
                "Change PIN OTP submitted "
                        + "using existing OtpPage: "
                        + flowLabel
        );
    }

    private void waitForChangePinOtpSheetForAppSettings(
            String flowLabel
    ) {

        long endTime =
                System.currentTimeMillis() + 20000L;

        while (System.currentTimeMillis() < endTime) {

            if (isAnyTextVisibleFast(
                    "OTP sent successfully",
                    "Enter OTP",
                    "Verify OTP",
                    "Resend OTP",
                    "Change pin"
            )) {

                ReportLogger.pass(
                        "Change PIN OTP sheet is visible: "
                                + flowLabel
                );

                return;
            }

            if (isAnyTextVisibleFast(
                    "Pin Not Matched",
                    "PIN Not Matched",
                    "Pin Not Matched !!!"
            )) {

                throw new AssertionError(
                        "PIN mismatch appeared before OTP screen"
                                + " | flow="
                                + flowLabel
                                + " | visibleValues="
                                + collectVisibleStrings()
                );
            }

            if (isAnyTextVisibleFast(
                    "New PIN cannot be the same as existing PIN",
                    "same as existing PIN",
                    "cannot be the same"
            )) {

                throw new AssertionError(
                        "Same existing PIN validation "
                                + "appeared unexpectedly"
                                + " | flow="
                                + flowLabel
                                + " | visibleValues="
                                + collectVisibleStrings()
                );
            }

            sleep(400);
        }

        throw new AssertionError(
                "OTP sheet did not appear after Change PIN"
                        + " | flow="
                        + flowLabel
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void waitAfterOtpVerifyForChangePin(
            String flowLabel
    ) {

        long endTime =
                System.currentTimeMillis() + 25000L;

        while (System.currentTimeMillis() < endTime) {

            if (isAnyTextVisibleFast(
                    "PIN changed successfully",
                    "Pin changed successfully",
                    "OTP verified successfully"
            )) {

                ReportLogger.pass(
                        "Change PIN success message visible after OTP: "
                                + flowLabel
                );

                sleep(1000);

                ensureAppSettingsScreenReadyForAppSettings();

                return;
            }

            if (isAppSettingsScreenVisible()) {

                ensureAppSettingsScreenOnAppTab();

                ReportLogger.pass(
                        "Returned to App Settings after OTP verification: "
                                + flowLabel
                );

                return;
            }

            if (isAnyTextVisibleFast(
                    "Invalid OTP",
                    "Incorrect OTP",
                    "OTP expired",
                    "Please enter valid OTP"
            )) {

                throw new AssertionError(
                        "OTP verification failed"
                                + " | flow="
                                + flowLabel
                                + " | visibleValues="
                                + collectVisibleStrings()
                );
            }

            sleep(500);
        }

        throw new AssertionError(
                "Change PIN did not complete "
                        + "after OTP verification"
                        + " | flow="
                        + flowLabel
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void waitForReEnterPinScreenForAppSettings(
            String flowLabel
    ) {

        long endTime =
                System.currentTimeMillis() + 12000L;

        while (System.currentTimeMillis() < endTime) {

            if (isAnyTextVisibleFast(
                    "Re-Enter your pin",
                    "Re-enter your pin",
                    "Re Enter your pin"
            )) {

                ReportLogger.pass(
                        "Re-Enter PIN screen visible: "
                                + flowLabel
                );

                return;
            }

            if (isAnyTextVisibleFast(
                    "Pin Not Matched",
                    "PIN Not Matched",
                    "Pin Not Matched !!!"
            )) {

                throw new AssertionError(
                        "PIN mismatch appeared before "
                                + "confirm screen was ready"
                                + " | flow="
                                + flowLabel
                                + " | visibleValues="
                                + collectVisibleStrings()
                );
            }

            if (isAnyTextVisibleFast(
                    "New PIN cannot be the same as existing PIN",
                    "same as existing PIN",
                    "cannot be the same"
            )) {

                throw new AssertionError(
                        "New PIN same-as-existing validation appeared"
                                + " | flow="
                                + flowLabel
                                + " | visibleValues="
                                + collectVisibleStrings()
                );
            }

            sleep(400);
        }

        throw new AssertionError(
                "Re-Enter PIN screen did not appear"
                        + " | flow="
                        + flowLabel
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    private void tapPinDigitByCoordinate(
            String digit
    ) {

        Dimension size =
                driver.manage()
                        .window()
                        .getSize();

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
                throw new IllegalArgumentException(
                        "Unsupported PIN digit"
                );
        }

        tapByCoordinates(
                (int) (
                        size.getWidth()
                                * xRatio
                ),
                (int) (
                        size.getHeight()
                                * yRatio
                )
        );
    }

    // =========================================================
    // ELEMENT HELPERS
    // =========================================================

    private void assertAnyTextVisible(
            String text
    ) {

        if (!isVisibleByAnyText(text)) {

            throw new AssertionError(
                    "Expected text is not visible: "
                            + text
                            + " | visibleValues="
                            + collectVisibleStrings()
            );
        }

        ReportLogger.pass(
                "Visible text validated: "
                        + text
        );
    }

    private boolean tapAnyVisibleText(
            String text
    ) {

        WebElement element =
                findVisibleTextElement(text);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);

        sleep(400);

        return true;
    }

    private WebElement findVisibleExactTextElement(
            String expectedText
    ) {

        if (expectedText == null
                || expectedText.trim().isEmpty()) {

            return null;
        }

        String escaped =
                escapeUiSelector(expectedText);

        /*
         * Text is intentionally checked before content-desc.
         *
         * Example:
         * Portfolio Settings page may have a "Portfolio" text tab
         * while the bottom navigation also has content-desc="Portfolio".
         */
        By[] locators = new By[]{
                AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\""
                                + escaped
                                + "\")"
                ),

                AppiumBy.androidUIAutomator(
                        "new UiSelector().description(\""
                                + escaped
                                + "\")"
                ),

                AppiumBy.accessibilityId(
                        expectedText
                )
        };

        for (By locator : locators) {

            WebElement element =
                    firstUsableElement(locator);

            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private WebElement findVisibleTextElement(
            String expectedText
    ) {

        WebElement exact =
                findVisibleExactTextElement(
                        expectedText
                );

        if (exact != null) {
            return exact;
        }

        if (expectedText == null
                || expectedText.trim().isEmpty()) {

            return null;
        }

        String escaped =
                escapeUiSelector(expectedText);

        By[] containsLocators = new By[]{
                AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\""
                                + escaped
                                + "\")"
                ),

                AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\""
                                + escaped
                                + "\")"
                )
        };

        for (By locator : containsLocators) {

            WebElement element =
                    firstUsableElement(locator);

            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private boolean isVisibleByAnyText(
            String text
    ) {

        return findVisibleTextElement(text)
                != null;
    }

    private boolean isAnyTextVisibleFast(
            String... possibleTexts
    ) {

        if (possibleTexts == null) {
            return false;
        }

        for (String text : possibleTexts) {

            if (text == null
                    || text.trim().isEmpty()) {
                continue;
            }

            if (isVisibleByAnyText(text)) {
                return true;
            }
        }

        return false;
    }

    private boolean isLocatorVisible(
            By locator
    ) {

        return firstUsableElement(locator)
                != null;
    }

    private WebElement firstUsableElement(
            By locator
    ) {

        try {

            List<WebElement> elements =
                    driver.findElements(locator);

            for (WebElement element : elements) {

                if (isElementUsable(element)) {
                    return element;
                }
            }

        } catch (Exception ignored) {
            // Caller decides fallback strategy.
        }

        return null;
    }

    private WebElement waitForElementFast(
            By locator,
            int timeoutSeconds
    ) {

        long endTime =
                System.currentTimeMillis()
                        + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endTime) {

            WebElement element =
                    firstUsableElement(locator);

            if (element != null) {
                return element;
            }

            sleep(200);
        }

        return null;
    }

    private void waitForAppToBeInteractive() {

        long endTime =
                System.currentTimeMillis() + 10000L;

        while (System.currentTimeMillis() < endTime) {

            try {

                List<WebElement> roots =
                        driver.findElements(
                                By.className(
                                        "android.widget.FrameLayout"
                                )
                        );

                if (roots != null
                        && !roots.isEmpty()) {

                    return;
                }

            } catch (Exception ignored) {
                // Retry.
            }

            sleep(350);
        }
    }

    private void waitForAnyTextVisible(
            List<String> possibleTexts,
            int timeoutSeconds
    ) {

        long endTime =
                System.currentTimeMillis()
                        + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < endTime) {

            for (String text : possibleTexts) {

                if (isVisibleByAnyText(text)) {
                    return;
                }
            }

            sleep(300);
        }

        throw new AssertionError(
                "None of the expected texts visible within timeout"
                        + " | expected="
                        + possibleTexts
                        + " | visibleValues="
                        + collectVisibleStrings()
        );
    }

    /*
     * IMPORTANT:
     *
     * This is now diagnostic-only.
     *
     * Do not use collectVisibleStrings() in normal polling loops.
     * Full //* hierarchy scans are expensive with Appium/UiAutomator2.
     */
    private List<String> collectVisibleStrings() {

        List<String> values =
                new ArrayList<>();

        try {

            Dimension screen =
                    driver.manage()
                            .window()
                            .getSize();

            List<WebElement> elements =
                    driver.findElements(
                            By.xpath("//*")
                    );

            for (WebElement element : elements) {

                try {

                    if (!isElementUsable(
                            element,
                            screen
                    )) {
                        continue;
                    }

                    addUniqueValue(
                            values,
                            element.getText()
                    );

                    addUniqueValue(
                            values,
                            element.getAttribute(
                                    "content-desc"
                            )
                    );

                    addUniqueValue(
                            values,
                            element.getAttribute(
                                    "text"
                            )
                    );

                } catch (Exception ignored) {
                    // Ignore stale/unreadable element.
                }
            }

        } catch (Exception e) {

            ReportLogger.debug(
                    "collectVisibleStrings skipped: "
                            + cleanError(
                            e.getMessage()
                    )
            );
        }

        return values;
    }

    private void addUniqueValue(
            List<String> values,
            String rawValue
    ) {

        if (rawValue == null) {
            return;
        }

        String clean =
                normalizeSpaces(rawValue);

        if (clean.isEmpty()) {
            return;
        }

        if (!values.contains(clean)) {
            values.add(clean);
        }

        String[] parts =
                rawValue.split("\\n");

        for (String part : parts) {

            String cleanPart =
                    normalizeSpaces(part);

            if (!cleanPart.isEmpty()
                    && !values.contains(cleanPart)) {

                values.add(cleanPart);
            }
        }
    }

    private boolean isElementUsable(
            WebElement element
    ) {

        try {

            Dimension size =
                    driver.manage()
                            .window()
                            .getSize();

            return isElementUsable(
                    element,
                    size
            );

        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isElementUsable(
            WebElement element,
            Dimension screenSize
    ) {

        try {

            if (element == null
                    || !element.isDisplayed()) {

                return false;
            }

            Rectangle rect =
                    element.getRect();

            if (rect.getWidth() <= 0
                    || rect.getHeight() <= 0) {

                return false;
            }

            int centerX =
                    rect.getX()
                            + rect.getWidth() / 2;

            int centerY =
                    rect.getY()
                            + rect.getHeight() / 2;

            return centerX >= 0
                    && centerX <= screenSize.getWidth()
                    && centerY >= 0
                    && centerY <= screenSize.getHeight();

        } catch (Exception ignored) {
            return false;
        }
    }

    // =========================================================
    // GESTURE HELPERS
    // =========================================================

    private void tapElementCenter(
            WebElement element
    ) {

        Rectangle rect =
                element.getRect();

        Dimension size =
                driver.manage()
                        .window()
                        .getSize();

        int x =
                rect.getX()
                        + rect.getWidth() / 2;

        int y =
                rect.getY()
                        + rect.getHeight() / 2;

        x = clamp(
                x,
                1,
                size.getWidth() - 2
        );

        y = clamp(
                y,
                1,
                size.getHeight() - 2
        );

        tapByCoordinates(
                x,
                y
        );
    }

    private void tapByCoordinates(
            int x,
            int y
    ) {

        Dimension size =
                driver.manage()
                        .window()
                        .getSize();

        x = clamp(
                x,
                1,
                size.getWidth() - 2
        );

        y = clamp(
                y,
                1,
                size.getHeight() - 2
        );

        PointerInput finger =
                new PointerInput(
                        PointerInput.Kind.TOUCH,
                        "finger"
                );

        Sequence tap =
                new Sequence(
                        finger,
                        1
                );

        tap.addAction(
                finger.createPointerMove(
                        Duration.ZERO,
                        PointerInput.Origin.viewport(),
                        x,
                        y
                )
        );

        tap.addAction(
                finger.createPointerDown(
                        PointerInput.MouseButton.LEFT.asArg()
                )
        );

        tap.addAction(
                finger.createPointerUp(
                        PointerInput.MouseButton.LEFT.asArg()
                )
        );

        driver.perform(
                Collections.singletonList(tap)
        );
    }

    private void pressBackSilently() {

        try {

            driver.navigate().back();

        } catch (Exception e) {

            ReportLogger.debug(
                    "Back press failed: "
                            + cleanError(
                            e.getMessage()
                    )
            );
        }
    }

    // =========================================================
    // COMMON HELPERS
    // =========================================================

    private String getCurrentPackageSafely() {

        try {
            return driver.getCurrentPackage();

        } catch (Exception ignored) {
            return "";
        }
    }

    private int clamp(
            int value,
            int min,
            int max
    ) {

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private String normalizeSpaces(
            String value
    ) {

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

    private String cleanError(
            String message
    ) {

        if (message == null) {
            return "";
        }

        return normalizeSpaces(message);
    }

    private String escapeUiSelector(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void sleep(
            long millis
    ) {

        try {

            Thread.sleep(millis);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Sleep interrupted",
                    e
            );
        }
    }

    private static String resolveSetting(
            String systemProperty,
            String environmentVariable,
            String defaultValue
    ) {

        String propertyValue =
                System.getProperty(
                        systemProperty
                );

        if (propertyValue != null
                && !propertyValue.trim().isEmpty()) {

            return propertyValue.trim();
        }

        String environmentValue =
                System.getenv(
                        environmentVariable
                );

        if (environmentValue != null
                && !environmentValue.trim().isEmpty()) {

            return environmentValue.trim();
        }

        return defaultValue;
    }
}