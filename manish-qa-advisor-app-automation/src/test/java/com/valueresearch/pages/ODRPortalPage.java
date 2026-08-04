package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.AppiumBy;
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

public class ODRPortalPage {

    // ODR_FINAL_SPEED_PATCH_V5_ACTIVE

    // ODR_FAST_V3_SUPPORT_FALLBACK_ACTIVE

    // ODR_FAST_V2_REAL_HUB_FIX_ACTIVE

    private final AndroidDriver driver;
    private String advisorAppPackage = "";


    // ODR_FAST_V1_ACTIVE
    private static final boolean ODR_FAST_MODE = true;
    private static final long VISIBLE_STRINGS_CACHE_TTL_MS = 700L;
    private List<String> visibleStringsCache = null;
    private long visibleStringsCacheAtMs = 0L;


    public ODRPortalPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForODR() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForODR() {
        ReportLogger.step("Checking Advisor app login/session state");

        waitForAppToBeInteractive();

        if (isMainAppLoaded()) {
            ReportLogger.pass("Advisor app session is already active");
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




    public void openHubFromBottomNavigationForODR() {
        ReportLogger.step("Opening Hub from bottom navigation");

        /*
         * Do not spend 15-20 seconds proving current screen.
         * Hub tap is safe even when already selected.
         */
        forceTapHubBottomTabFastV5();

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isHubContentVisibleCheapV5() || isOdrPortalVisibleCheapV5()) {
                ReportLogger.pass("Hub page opened successfully after direct Hub tap");
                return;
            }

            sleep(300);
        }

        throw new AssertionError("Hub page did not open after direct Hub tap"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }





    public void scrollToODRPortalInHubForODR() {
        ReportLogger.step("Finding ODR Portal in Hub");

        /*
         * First try native UiScrollable. This is much faster than manual
         * repeated swipe + full-tree scan.
         */
        if (scrollDirectlyToOdrPortalFastV5()) {
            ReportLogger.pass("ODR Portal option is visible");
            return;
        }

        /*
         * Manual fallback with cheap ODR checks only.
         */
        for (int attempt = 1; attempt <= 12; attempt++) {
            if (isOdrPortalVisibleCheapV5()) {
                ReportLogger.pass("ODR Portal option is visible");
                return;
            }

            if (attempt <= 3) {
                smallSwipeUp();
            } else {
                swipeUp();
            }

            sleep(220);
        }

        /*
         * Some builds expose ODR under Support / Need more help.
         */
        if (openSupportAreaForOdrCheapV5()) {
            if (scrollDirectlyToOdrPortalFastV5()) {
                ReportLogger.pass("ODR Portal option is visible after Support fallback");
                return;
            }

            for (int attempt = 1; attempt <= 6; attempt++) {
                if (isOdrPortalVisibleCheapV5()) {
                    ReportLogger.pass("ODR Portal option is visible after Support fallback");
                    return;
                }

                smallSwipeUp();
                sleep(220);
            }
        }

        throw new AssertionError("ODR Portal option not visible after final fast Hub/Support scan"
                + " | visibleValues=" + collectVisibleStrings());
    }




    public void tapODRPortalForODR() {
        ReportLogger.step("Tapping ODR Portal option");

        if (tapIfVisible(odrPortalExactLocator(), "ODR Portal exact accessibilityId")) {
            sleep(450);
            ReportLogger.pass("Tapped ODR Portal option using exact accessibilityId");
            return;
        }

        if (tapIfVisible(odrPortalContainsLocator(), "ODR Portal descriptionContains")) {
            sleep(450);
            ReportLogger.pass("Tapped ODR Portal option using descriptionContains");
            return;
        }

        if (tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"ODR\")"), "ODR textContains")) {
            sleep(450);
            ReportLogger.pass("Tapped ODR Portal option using textContains");
            return;
        }

        if (scrollDirectlyToOdrPortalFastV5()) {
            if (tapIfVisible(odrPortalExactLocator(), "ODR Portal exact accessibilityId after scroll")
                    || tapIfVisible(odrPortalContainsLocator(), "ODR Portal descriptionContains after scroll")
                    || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"ODR\")"), "ODR textContains after scroll")) {
                sleep(450);
                ReportLogger.pass("Tapped ODR Portal option after native scroll");
                return;
            }
        }

        throw new AssertionError("Unable to tap ODR Portal"
                + " | visibleValues=" + collectVisibleStrings());
    }



    public void waitForODRExternalPageForODR() {
        ReportLogger.step("Waiting for browser/custom tab or ODR login page");

        String initialPackage = advisorAppPackage;

        for (int i = 1; i <= 12; i++) {
            String currentPackage = getCurrentPackageSafely();

            boolean packageChanged = !initialPackage.isEmpty()
                    && !currentPackage.isEmpty()
                    && !currentPackage.equals(initialPackage);

            if (packageChanged) {
                ReportLogger.pass("External ODR/browser page detected by package change"
                        + " | currentPackage=" + currentPackage);

                /*
                 * Do not run expensive browser popup scan here.
                 * Real-device log showed Chrome opened instantly, then popup check wasted ~89 sec.
                 */
                handleBrowserFirstRunPopupsIfAny();
                return;
            }

            if (i % 3 == 0 && isOdrLoginPageVisibleCheapV5()) {
                ReportLogger.pass("External ODR/browser page detected by ODR login text"
                        + " | currentPackage=" + currentPackage);
                handleBrowserFirstRunPopupsIfAny();
                return;
            }

            sleep(300);
        }

        throw new AssertionError("External ODR Portal page/browser did not appear"
                + " | package=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }



    public void validateODRLoginPageContentForODR() {
        ReportLogger.step("Validating ODR Investor Login page content");

        boolean pageReady = false;

        for (int attempt = 1; attempt <= 18; attempt++) {
            if (isOdrLoginPageVisibleCheapV5()) {
                pageReady = true;
                break;
            }

            sleep(400);
        }

        if (!pageReady) {
            throw new AssertionError("ODR login page did not become visible quickly"
                    + " | currentPackage=" + getCurrentPackageSafely()
                    + " | visibleValues=" + collectVisibleStrings());
        }

        boolean hasSmartOdrOrInvestorLogin = isChromeTextPresentFastV5("smartodr")
                || isChromeTextPresentFastV5("Investor Login");

        boolean hasEmail = isChromeTextPresentFastV5("Email");
        boolean hasPassword = isChromeTextPresentFastV5("Password");
        boolean hasLogin = isChromeTextPresentFastV5("Login");

        if (!hasSmartOdrOrInvestorLogin) {
            throw new AssertionError("ODR page URL/header validation failed"
                    + " | currentPackage=" + getCurrentPackageSafely()
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("ODR page identity validated using fast locator");

        if (!hasEmail) {
            throw new AssertionError("ODR Email field is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }
        ReportLogger.pass("ODR Email field is visible");

        if (!hasPassword) {
            throw new AssertionError("ODR Password field is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }
        ReportLogger.pass("ODR Password field is visible");

        if (!hasLogin) {
            throw new AssertionError("ODR Login button/text is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }
        ReportLogger.pass("ODR Login button/text is visible");

        if (isChromeTextPresentFastV5("Forgot Password") || isChromeTextPresentFastV5("Forgot password")) {
            ReportLogger.pass("ODR Forgot Password link is visible");
        } else {
            ReportLogger.debug("ODR Forgot Password link not exposed. Soft check skipped.");
        }

        if (isChromeTextPresentFastV5("reCAPTCHA")
                || isChromeTextPresentFastV5("captcha")
                || isChromeTextPresentFastV5("not a robot")) {
            ReportLogger.pass("ODR captcha area is visible");
        } else {
            ReportLogger.debug("ODR captcha area not exposed. Soft check skipped.");
        }

        ReportLogger.pass("ODR Investor Login page content validated successfully");
    }



    public void returnBackToAdvisorAppSafely() {
        ReportLogger.step("Returning back to Advisor App after ODR validation");

        if (advisorAppPackage == null || advisorAppPackage.trim().isEmpty()) {
            advisorAppPackage = "com.valueresearch.advisor";
        }

        if (isBackOnAdvisorApp()) {
            ReportLogger.pass("Already back on Advisor App");
            return;
        }

        try {
            driver.activateApp(advisorAppPackage);
            sleep(600);

            if (isBackOnAdvisorApp()) {
                ReportLogger.pass("Advisor App activated using package fallback");
                return;
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp fallback failed: " + cleanError(e.getMessage()));
        }

        pressBackSilently();
        sleep(500);

        if (isBackOnAdvisorApp()) {
            ReportLogger.pass("Returned to Advisor App using one back press");
            return;
        }

        ReportLogger.debug("Could not confirm Advisor App return"
                + " | currentPackage=" + getCurrentPackageSafely());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifyODRPortalRedirectionFromHub() {
        ReportLogger.step("Verifying ODR Portal redirection from Hub");

        captureAdvisorAppPackageForODR();
        ensureAdvisorAppLoggedInForODR();
        openHubFromBottomNavigationForODR();
        scrollToODRPortalInHubForODR();
        tapODRPortalForODR();
        waitForODRExternalPageForODR();
        validateODRLoginPageContentForODR();

        ReportLogger.pass("ODR Portal redirection validated successfully");
    }

    // =========================================================
    // LOGIN / SESSION HELPERS
    // =========================================================


    private boolean isPinScreenVisible() {
        return isTextVisibleFast("Enter your Advisor PIN")
                || isTextVisibleFast("Advisor PIN")
                || isTextVisibleFast("PIN")
                || isTextVisibleFast("Hi,");
    }


    private boolean isMainAppLoaded() {
        return isTextVisibleFast("Funds")
                || isTextVisibleFast("Portfolio")
                || isTextVisibleFast("Hub")
                || isTextVisibleFast("Clients")
                || isTextVisibleFast("Reports")
                || isTextVisibleFast("Search");
    }

    private void enterAdvisorPin() {
        String pin = "1975";

        for (char digit : pin.toCharArray()) {
            tapPinDigit(String.valueOf(digit));
            sleep(450);
        }
    }

    private void tapPinDigit(String digit) {
        WebElement digitElement = findVisibleExactTextElement(digit);

        if (digitElement != null) {
            tapElementCenter(digitElement);
            ReportLogger.step("Tapped PIN digit: " + digit);
            return;
        }

        digitElement = findVisibleTextElement(digit);

        if (digitElement != null) {
            tapElementCenter(digitElement);
            ReportLogger.step("Tapped PIN digit by fallback: " + digit);
            return;
        }

        throw new AssertionError("Unable to tap PIN digit: " + digit
                + " | visibleValues=" + collectVisibleStrings());
    }


    private void waitForMainAppAfterPin() {
        ReportLogger.step("Waiting for Advisor app dashboard after PIN");

        for (int i = 1; i <= 12; i++) {
            if (isMainAppLoaded()) {
                ReportLogger.pass("Advisor app dashboard loaded after PIN");
                return;
            }

            sleep(500);
        }

        throw new AssertionError("Advisor app dashboard did not load after PIN"
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // HUB HELPERS
    // =========================================================



    private boolean isLikelyOnHubPage() {
        return isStrictHubContentVisibleForODR();
    }



    private void handleBrowserFirstRunPopupsIfAny() {
        ReportLogger.step("Checking browser popup if any");

        /*
         * Final fast behaviour:
         * Do not scan the whole Chrome tree for popups.
         * On this real device, no popup is present and the previous scan wasted ~89 sec.
         * If a real popup appears on a fresh Chrome install, login validation will fail clearly.
         */
        String currentPackage = getCurrentPackageSafely();

        if (currentPackage != null && currentPackage.toLowerCase().contains("chrome")) {
            ReportLogger.debug("Chrome is active. No first-run popup action required.");
            return;
        }

        String[] possibleButtons = new String[]{
                "Accept & continue",
                "Accept and continue",
                "Continue",
                "No thanks",
                "Got it",
                "Skip"
        };

        for (String button : possibleButtons) {
            if (tapIfVisible(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escapeUiTextOdrFinalV5(button) + "\")"),
                    "Browser popup textContains=" + button)) {
                sleep(400);
                return;
            }
        }

        ReportLogger.debug("No browser first-run popup visible.");
    }

    private boolean isBackOnAdvisorApp() {
        String currentPackage = getCurrentPackageSafely();

        return advisorAppPackage != null
                && !advisorAppPackage.trim().isEmpty()
                && currentPackage.equals(advisorAppPackage);
    }

    // =========================================================
    // ELEMENT HELPERS
    // =========================================================



    private void forceTapHubBottomTabForODR() {
        ReportLogger.step("Opening Hub tab");

        if (tapIfVisible(AppiumBy.accessibilityId("Hub"), "Hub bottom tab accessibilityId")) {
            sleep(700);
            return;
        }

        if (tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\\\"Hub\\\")"), "Hub bottom tab descriptionContains")) {
            sleep(700);
            return;
        }

        WebElement hubBottomTab = findVisibleTextElementNearBottom("Hub");
        if (hubBottomTab != null) {
            tapElementCenter(hubBottomTab);
            sleep(700);
            ReportLogger.pass("Tapped Hub bottom tab using bottom-band text");
            return;
        }

        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.50);
        int y = (int) (size.getHeight() * 0.955);

        ReportLogger.step("Tapping Hub bottom tab by coordinate fallback | x=" + x + " | y=" + y);
        tapByCoordinates(x, y);
        sleep(800);
    }

    private boolean isOdrPortalVisibleFastForODR() {
        return isVisible(odrPortalExactLocator())
                || isVisible(odrPortalContainsLocator())
                || isTextVisibleFast("ODR Portal")
                || isTextVisibleFast("ODR portal")
                || isTextVisibleFast("Open the ODR portal");
    }

    private boolean isStrictHubContentVisibleForODR() {
        if (isOdrPortalVisibleFastForODR()) {
            return true;
        }

        return isTextVisibleFast("Stories & Videos")
                || isTextVisibleFast("Portfolio Planner")
                || isTextVisibleFast("Analyst's Choice")
                || isTextVisibleFast("Fund Screener")
                || isTextVisibleFast("SIP Calculator")
                || isTextVisibleFast("Calculators")
                || isTextVisibleFast("Knowledge")
                || isTextVisibleFast("Tools")
                || isTextVisibleFast("Manage your SIPs")
                || isTextVisibleFast("Manage SIP");
    }



    private boolean isOdrPortalVisibleFastForODRV3() {
        String[] aliases = new String[]{
                "ODR Portal",
                "ODR portal",
                "Open the ODR portal",
                "Open the ODR portal for dispute resolution",
                "Online Dispute Resolution",
                "Online dispute resolution",
                "Dispute Resolution",
                "dispute resolution",
                "ODR",
                "smartodr"
        };

        for (String alias : aliases) {
            if (findVisibleElement(AppiumBy.accessibilityId(alias)) != null) {
                return true;
            }

            if (findVisibleElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + escapeUiTextOdrV3(alias) + "\")")) != null) {
                return true;
            }

            if (findVisibleElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escapeUiTextOdrV3(alias) + "\")")) != null) {
                return true;
            }
        }

        WebElement fallback = findVisibleTextElement("ODR");
        return fallback != null;
    }

    private boolean tapOdrPortalAliasForODRV3() {
        String[] aliases = new String[]{
                "Open the ODR portal for dispute resolution",
                "ODR Portal",
                "ODR portal",
                "Open the ODR portal",
                "Online Dispute Resolution",
                "Online dispute resolution",
                "Dispute Resolution",
                "dispute resolution",
                "ODR"
        };

        for (String alias : aliases) {
            if (tapIfVisible(AppiumBy.accessibilityId(alias), "ODR alias accessibilityId=" + alias)) {
                return true;
            }

            if (tapIfVisible(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + escapeUiTextOdrV3(alias) + "\")"),
                    "ODR alias descriptionContains=" + alias)) {
                return true;
            }

            if (tapIfVisible(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escapeUiTextOdrV3(alias) + "\")"),
                    "ODR alias textContains=" + alias)) {
                return true;
            }
        }

        WebElement element = findVisibleTextElement("ODR");
        if (element != null) {
            tapElementCenter(element);
            return true;
        }

        return false;
    }

    private boolean openSupportAreaForOdrIfVisibleV3() {
        ReportLogger.step("ODR not visible on Hub. Trying Support/Need more help fallback.");

        String[] supportAliases = new String[]{
                "Need more help?",
                "Need more help",
                "We're here with personalised support",
                "We're here with personalized support",
                "personalised support",
                "personalized support",
                "Contact Us",
                "Contact us",
                "Support",
                "Help"
        };

        for (String alias : supportAliases) {
            WebElement element = findVisibleTextElement(alias);

            if (element != null) {
                ReportLogger.step("Tapping Support/Need more help fallback: " + alias);
                tapElementCenter(element);
                sleep(900);
                return true;
            }
        }

        return false;
    }

    private String escapeUiTextOdrV3(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }



    private void forceTapHubBottomTabFastV5() {
        ReportLogger.step("Opening Hub tab");

        if (tapIfVisible(AppiumBy.accessibilityId("Hub"), "Hub bottom tab accessibilityId")) {
            sleep(350);
            return;
        }

        if (tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Hub\")"), "Hub bottom tab descriptionContains")) {
            sleep(350);
            return;
        }

        Dimension size = driver.manage().window().getSize();

        int x = (int) (size.getWidth() * 0.50);
        int y = (int) (size.getHeight() * 0.955);

        ReportLogger.step("Tapping Hub bottom tab by coordinate fallback | x=" + x + " | y=" + y);
        tapByCoordinates(x, y);
        sleep(450);
    }

    private boolean isHubContentVisibleCheapV5() {
        return findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Quick Guides\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Quick Guides\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Need more help\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Need more help\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Portfolio Planner\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio Planner\")")) != null;
    }

    private boolean isOdrPortalVisibleCheapV5() {
        return findVisibleElement(odrPortalExactLocator()) != null
                || findVisibleElement(odrPortalContainsLocator()) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"ODR\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"ODR\")")) != null;
    }

    private boolean scrollDirectlyToOdrPortalFastV5() {
        By[] scrollLocators = new By[]{
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(\"ODR\"))"),
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"ODR\"))"),
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(\"dispute\"))"),
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"dispute\"))")
        };

        for (By locator : scrollLocators) {
            try {
                WebElement element = findVisibleElement(locator);

                if (element != null) {
                    ReportLogger.pass("Scrolled to ODR Portal option");
                    return true;
                }
            } catch (Exception e) {
                ReportLogger.debug("Native ODR scroll attempt skipped: " + cleanError(e.getMessage()));
            }
        }

        return isOdrPortalVisibleCheapV5();
    }

    private boolean openSupportAreaForOdrCheapV5() {
        ReportLogger.step("Trying Support/Need more help fallback for ODR");

        By[] supportLocators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Need more help\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Need more help\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"support\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"support\")")
        };

        for (By locator : supportLocators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                tapElementCenter(element);
                sleep(500);
                ReportLogger.pass("Opened Support/Need more help fallback");
                return true;
            }
        }

        return false;
    }

    private boolean isOdrLoginPageVisibleCheapV5() {
        return isChromeTextPresentFastV5("Investor Login")
                || isChromeTextPresentFastV5("smartodr")
                || isChromeTextPresentFastV5("Email");
    }

    private boolean isChromeTextPresentFastV5(String token) {
        String safe = escapeUiTextOdrFinalV5(token);

        return findVisibleElement(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + safe + "\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + safe + "\")")) != null;
    }

    private String escapeUiTextOdrFinalV5(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private By odrPortalExactLocator() {
        return AppiumBy.accessibilityId("Open the ODR portal for dispute resolution");
    }

    private By odrPortalContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"ODR portal\")");
    }
    private WebElement findVisibleElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element != null && element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean isVisible(By locator) {
        return findVisibleElement(locator) != null;
    }

    private boolean tapIfVisible(By locator, String label) {
        WebElement element = findVisibleElement(locator);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);
        ReportLogger.pass("Tapped: " + label);
        return true;
    }

    private boolean tapAnyVisibleText(String text) {
        WebElement element = findVisibleTextElement(text);

        if (element == null) {
            return false;
        }

        tapElementCenter(element);
        return true;
    }


    private WebElement findVisibleExactTextElement(String expectedText) {
        WebElement fastElement = findFastTextElement(expectedText, true);

        if (fastElement != null) {
            return fastElement;
        }

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
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
            ReportLogger.debug("findVisibleExactTextElement fallback skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }


    private WebElement findVisibleTextElement(String expectedText) {
        WebElement fastElement = findFastTextElement(expectedText, false);

        if (fastElement != null) {
            return fastElement;
        }

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
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
            ReportLogger.debug("findVisibleTextElement fallback skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }


    private WebElement findVisibleTextElementNearBottom(String expectedText) {
        WebElement fastElement = findFastTextElement(expectedText, false);

        if (fastElement != null) {
            try {
                Dimension size = driver.manage().window().getSize();
                Rectangle rect = fastElement.getRect();
                int centerY = rect.getY() + rect.getHeight() / 2;

                if (centerY >= (int) (size.getHeight() * 0.60)) {
                    return fastElement;
                }
            } catch (Exception ignored) {
                return fastElement;
            }
        }

        try {
            Dimension size = driver.manage().window().getSize();
            int bottomMinY = (int) (size.getHeight() * 0.70);

            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = element.getRect();
                    int centerY = rect.getY() + rect.getHeight() / 2;

                    if (centerY < bottomMinY) {
                        continue;
                    }

                    String readable = normalizeSpaces(getElementReadableText(element));

                    if (readable.equals(expectedText) || readable.contains(expectedText)) {
                        return element;
                    }

                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleTextElementNearBottom fallback skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }


    private boolean isVisibleByAnyText(String text) {
        return isTextVisibleFast(text) || findVisibleTextElement(text) != null;
    }


    private void waitForAppToBeInteractive() {
        for (int i = 1; i <= 5; i++) {
            if (!getCurrentPackageSafely().isEmpty()
                    && (isMainAppLoaded() || isPinScreenVisible() || isTextVisibleFast("Hub"))) {
                return;
            }

            if (i == 5 && !collectVisibleStrings().isEmpty()) {
                return;
            }

            sleep(300);
        }
    }


    private void waitUntilTextVisible(String text, int timeoutSeconds) {
        int attempts = Math.max(1, timeoutSeconds * 2);

        for (int i = 1; i <= attempts; i++) {
            if (isTextVisibleFast(text)) {
                return;
            }

            sleep(500);
        }

        throw new AssertionError("Text not visible within timeout: " + text
                + " | visibleValues=" + collectVisibleStrings());
    }


    private void waitForAnyTextVisible(List<String> possibleTexts, int timeoutSeconds) {
        int attempts = Math.max(1, timeoutSeconds * 2);

        for (int i = 1; i <= attempts; i++) {
            for (String text : possibleTexts) {
                if (isTextVisibleFast(text)) {
                    return;
                }
            }

            if (i % 4 == 0) {
                List<String> values = collectVisibleStrings();

                for (String text : possibleTexts) {
                    if (containsAny(values, text)) {
                        return;
                    }
                }
            }

            sleep(500);
        }

        throw new AssertionError("None of the expected texts visible within timeout"
                + " | expected=" + possibleTexts
                + " | visibleValues=" + collectVisibleStrings());
    }


    private List<String> collectVisibleStrings() {
        long now = System.currentTimeMillis();

        if (visibleStringsCache != null && (now - visibleStringsCacheAtMs) <= VISIBLE_STRINGS_CACHE_TTL_MS) {
            return new ArrayList<>(visibleStringsCache);
        }

        List<String> values = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(By.xpath("//*"));

            for (WebElement element : elements) {
                try {
                    if (element == null || !element.isDisplayed()) {
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

        visibleStringsCache = new ArrayList<>(values);
        visibleStringsCacheAtMs = System.currentTimeMillis();

        return values;
    }



    private void invalidateVisibleStringsCache() {
        visibleStringsCache = null;
        visibleStringsCacheAtMs = 0L;
    }

    private boolean isTextVisibleFast(String expectedText) {
        return findFastTextElement(expectedText, false) != null;
    }

    private boolean isOdrBrowserOrLoginTextVisibleFast() {
        return isTextVisibleFast("smartodr")
                || isTextVisibleFast("Investor Login")
                || isTextVisibleFast("Email")
                || isTextVisibleFast("Password")
                || isTextVisibleFast("Login")
                || isTextVisibleFast("Chrome")
                || isTextVisibleFast("Search or type web address")
                || isTextVisibleFast("Accept")
                || isTextVisibleFast("Continue");
    }

    private WebElement findFastTextElement(String expectedText, boolean exactOnly) {
        String clean = normalizeSpaces(expectedText);

        if (clean.isEmpty()) {
            return null;
        }

        By[] exactLocators = new By[]{
                AppiumBy.accessibilityId(clean),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escapeUiAutomatorText(clean) + "\")")
        };

        for (By locator : exactLocators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                return element;
            }
        }

        if (exactOnly) {
            return null;
        }

        By[] containsLocators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + escapeUiAutomatorText(clean) + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escapeUiAutomatorText(clean) + "\")")
        };

        for (By locator : containsLocators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private String getElementReadableText(WebElement element) {
        if (element == null) {
            return "";
        }

        try {
            String value = normalizeSpaces(element.getText());
            if (!value.isEmpty()) {
                return value;
            }
        } catch (Exception ignored) {}

        try {
            String value = normalizeSpaces(element.getAttribute("content-desc"));
            if (!value.isEmpty()) {
                return value;
            }
        } catch (Exception ignored) {}

        try {
            String value = normalizeSpaces(element.getAttribute("name"));
            if (!value.isEmpty()) {
                return value;
            }
        } catch (Exception ignored) {}

        try {
            String value = normalizeSpaces(element.getAttribute("text"));
            if (!value.isEmpty()) {
                return value;
            }
        } catch (Exception ignored) {}

        return "";
    }

    private String escapeUiAutomatorText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
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

    // =========================================================
    // GESTURE HELPERS
    // =========================================================

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();

        int x = rect.getX() + rect.getWidth() / 2;
        int y = rect.getY() + rect.getHeight() / 2;

        tapByCoordinates(x, y);
    }


    private void tapByCoordinates(int x, int y) {
        invalidateVisibleStringsCache();

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

    private void swipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.78);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.30);

        swipeByCoordinates(startX, startY, endX, endY, 650);
    }

    private void smallSwipeUp() {
        Dimension size = driver.manage().window().getSize();

        int startX = (int) (size.getWidth() * 0.50);
        int startY = (int) (size.getHeight() * 0.66);
        int endX = (int) (size.getWidth() * 0.50);
        int endY = (int) (size.getHeight() * 0.45);

        swipeByCoordinates(startX, startY, endX, endY, 450);
    }


    private void swipeByCoordinates(int startX, int startY, int endX, int endY, long durationMillis) {
        invalidateVisibleStringsCache();

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                startX,
                startY
        ));

        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(durationMillis),
                PointerInput.Origin.viewport(),
                endX,
                endY
        ));

        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }


    private void pressBackSilently() {
        try {
            invalidateVisibleStringsCache();
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
        long adjustedMillis = millis;

        if (ODR_FAST_MODE) {
            if (millis >= 2500) {
                adjustedMillis = 750;
            } else if (millis >= 1800) {
                adjustedMillis = 600;
            } else if (millis >= 1500) {
                adjustedMillis = 550;
            } else if (millis >= 1000) {
                adjustedMillis = 420;
            } else if (millis >= 700) {
                adjustedMillis = 300;
            } else if (millis >= 500) {
                adjustedMillis = 250;
            }
        }

        try {
            Thread.sleep(adjustedMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}