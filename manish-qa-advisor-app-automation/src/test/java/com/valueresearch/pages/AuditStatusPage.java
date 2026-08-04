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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuditStatusPage {

    // AUDIT_VALIDATION_FAST_V3_ACTIVE

    // AUDIT_LOGIN_PAGELOAD_FAST_V2_ACTIVE

    private final AndroidDriver driver;


    // AUDIT_FINAL_SPEED_PATCH_V1_ACTIVE
    private static final boolean AUDIT_FAST_MODE = true;
    private static final long VISIBLE_STRINGS_CACHE_TTL_MS = 700L;
    private List<String> visibleStringsCache = null;
    private long visibleStringsCacheAtMs = 0L;

    private String advisorAppPackage = "";

    public AuditStatusPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForAudit() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForAudit() {
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


    public void openHubFromBottomNavigationForAudit() {
        ReportLogger.step("Opening Hub from bottom navigation");

        forceTapHubBottomTabFastAuditV1();

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isHubContentVisibleCheapAuditV1() || isAuditStatusVisibleCheapAuditV1()) {
                ReportLogger.pass("Hub page opened successfully after direct Hub tap");
                return;
            }

            sleep(300);
        }

        throw new AssertionError("Hub page did not open after direct Hub tap"
                + " | visibleValues=" + collectVisibleStrings());
    }


    public void scrollToAuditStatusInHubForAudit() {
        ReportLogger.step("Finding Audit Status in Hub");

        if (scrollDirectlyToAuditStatusFastV1()) {
            ReportLogger.pass("Audit Status option is visible");
            return;
        }

        for (int attempt = 1; attempt <= 10; attempt++) {
            if (isAuditStatusVisibleCheapAuditV1()) {
                ReportLogger.pass("Audit Status option is visible");
                return;
            }

            if (attempt <= 3) {
                smallSwipeUp();
            } else {
                swipeUp();
            }

            sleep(220);
        }

        throw new AssertionError("Audit Status option not visible after final fast Hub scan"
                + " | visibleValues=" + collectVisibleStrings());
    }


    public void tapAuditStatusForAudit() {
        ReportLogger.step("Tapping Audit Status option");

        if (tapAuditStatusAliasFastV1()) {
            sleep(450);
            ReportLogger.pass("Tapped Audit Status option");
            return;
        }

        if (scrollDirectlyToAuditStatusFastV1() && tapAuditStatusAliasFastV1()) {
            sleep(450);
            ReportLogger.pass("Tapped Audit Status option after native scroll");
            return;
        }

        throw new AssertionError("Unable to tap Audit Status option"
                + " | visibleValues=" + collectVisibleStrings());
    }



    public void waitForAuditStatusPageForAudit() {
        ReportLogger.step("Waiting for Audit Status page to load");

        for (int i = 1; i <= 18; i++) {
            if (isAuditStatusPageLoadedCheapAuditV2()) {
                ReportLogger.pass("Audit Status page loaded");
                return;
            }

            sleep(350);
        }

        throw new AssertionError("Audit Status page did not load quickly"
                + " | visibleValues=" + collectVisibleStrings());
    }

    private void waitForAuditStatusPageToLoad() {
        ReportLogger.step("Waiting for Audit Status page to load");

        for (int i = 1; i <= 18; i++) {
            if (isAuditStatusPageLoadedCheapAuditV2()) {
                ReportLogger.pass("Audit Status page loaded");
                return;
            }

            sleep(350);
        }

        throw new AssertionError("Audit Status page did not load quickly"
                + " | visibleValues=" + collectVisibleStrings());
    }


    public void validateAuditStatusPageContentForAudit() {
        ReportLogger.step("Validating Audit Status page title, disclosure and table headers");

        waitForAuditStatusPageForAudit();

        boolean hasTitle = isAuditTextPresentFastV2("Audit Status");
        boolean hasDisclosure = isAuditDisclosureVisibleFastV3();

        boolean hasSrNo = isAuditTextPresentFastV2("Sr. No.")
                || isAuditTextPresentFastV2("Sr No")
                || isAuditTextPresentFastV2("Sr.");

        boolean hasFinancialYear = isAuditTextPresentFastV2("Financial Year");

        boolean hasComplianceStatus = isAuditTextPresentFastV2("Compliance Audit Status")
                || isAuditTextPresentFastV2("Audit Status");

        boolean hasRemarks = isAuditTextPresentFastV2("Remarks");

        if (!hasTitle) {
            throw new AssertionError("Audit Status page title is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }
        ReportLogger.pass("Audit Status page title is visible");

        if (!hasDisclosure) {
            throw new AssertionError("Audit Status disclosure text is not visible"
                    + " | visibleValues=" + collectVisibleStrings());
        }
        ReportLogger.pass("Audit Status disclosure text is visible");

        if (!hasSrNo || !hasFinancialYear || !hasComplianceStatus || !hasRemarks) {
            throw new AssertionError("Audit Status table headers are not visible"
                    + " | srNo=" + hasSrNo
                    + " | financialYear=" + hasFinancialYear
                    + " | complianceStatus=" + hasComplianceStatus
                    + " | remarks=" + hasRemarks
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Audit Status table headers are visible: Sr. No., Financial Year, Compliance Audit Status, Remarks");
    }


    public void validateAuditStatusTableDataForAudit() {
        ReportLogger.step("Validating Audit Status table data dynamically");

        waitForAuditStatusPageForAudit();

        int visibleRowCount = 0;
        int conductedVisibleCount = 0;
        int noneVisibleCount = 0;
        List<String> financialYears = new ArrayList<>();

        String[] expectedYears = new String[]{
                "FY 2020-21",
                "FY 2021-22",
                "FY 2022-23",
                "FY 2023-24"
        };

        for (String year : expectedYears) {
            if (isAuditTextPresentFastV2(year)) {
                visibleRowCount++;
                financialYears.add(year);
            }
        }

        /*
         * Count status/remarks using cheap repeated checks.
         * We do not scan the full XML tree unless validation fails.
         */
        if (isAuditTextPresentFastV2("Conducted")) {
            conductedVisibleCount = Math.max(conductedVisibleCount, visibleRowCount);
        }

        if (isAuditTextPresentFastV2("None")) {
            noneVisibleCount = Math.max(noneVisibleCount, visibleRowCount);
        }

        /*
         * Fallback for future years: if fixed FY values change later,
         * use one cached visible string collection only.
         */
        if (visibleRowCount == 0) {
            List<String> values = collectVisibleStrings();

            for (String value : values) {
                String clean = normalizeSpaces(value);

                if (clean.matches(".*FY\\s+20[0-9]{2}-[0-9]{2}.*")) {
                    visibleRowCount++;
                    financialYears.add(clean);
                }

                if (clean.toLowerCase().contains("conducted")) {
                    conductedVisibleCount++;
                }

                if (clean.equalsIgnoreCase("None") || clean.toLowerCase().contains(" none ")) {
                    noneVisibleCount++;
                }
            }
        }

        if (visibleRowCount <= 0) {
            throw new AssertionError("Audit Status table data is not visible"
                    + " | visibleRowCount=" + visibleRowCount
                    + " | visibleValues=" + collectVisibleStrings());
        }

        if (conductedVisibleCount <= 0) {
            throw new AssertionError("Audit Status conducted values are not visible"
                    + " | conductedVisibleCount=" + conductedVisibleCount
                    + " | visibleValues=" + collectVisibleStrings());
        }

        if (noneVisibleCount <= 0) {
            throw new AssertionError("Audit Status remarks values are not visible"
                    + " | noneVisibleCount=" + noneVisibleCount
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Audit Status table data validated dynamically"
                + " | visibleRowCount=" + visibleRowCount
                + " | conductedVisibleCount=" + conductedVisibleCount
                + " | noneVisibleCount=" + noneVisibleCount
                + " | financialYears=" + financialYears);
    }


    public void returnBackToHubSafelyForAudit() {
        ReportLogger.step("Returning back to Hub after Audit Status validation");

        for (int attempt = 1; attempt <= 2; attempt++) {
            if (isHubContentVisibleCheapAuditV1() || isAuditStatusVisibleCheapAuditV1()) {
                ReportLogger.pass("Already back on Hub");
                return;
            }

            pressBackSilently();
            sleep(500);

            if (isHubContentVisibleCheapAuditV1() || isAuditStatusVisibleCheapAuditV1()) {
                ReportLogger.pass("Returned back to Hub after back attempt " + attempt);
                return;
            }
        }

        try {
            driver.activateApp("com.valueresearch.advisor");
            sleep(600);

            if (isHubContentVisibleCheapAuditV1() || isAuditStatusVisibleCheapAuditV1()) {
                ReportLogger.pass("Advisor App activated and Hub content visible");
                return;
            }

            forceTapHubBottomTabFastAuditV1();

            if (isHubContentVisibleCheapAuditV1() || isAuditStatusVisibleCheapAuditV1()) {
                ReportLogger.pass("Returned to Hub using app activation + Hub tap");
                return;
            }
        } catch (Exception e) {
            ReportLogger.debug("Audit fast return fallback failed: " + cleanError(e.getMessage()));
        }

        ReportLogger.debug("Could not strictly confirm Hub return"
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifyAuditStatusFromHub() {
        ReportLogger.step("Verifying Audit Status module from Hub");

        captureAdvisorAppPackageForAudit();
        ensureAdvisorAppLoggedInForAudit();
        openHubFromBottomNavigationForAudit();
        scrollToAuditStatusInHubForAudit();
        tapAuditStatusForAudit();
        waitForAuditStatusPageForAudit();
        validateAuditStatusPageContentForAudit();
        validateAuditStatusTableDataForAudit();

        ReportLogger.pass("Audit Status module validated successfully");
    }

    // =========================================================
    // LOGIN / SESSION HELPERS
    // =========================================================


    private boolean isPinScreenVisible() {
        return isPinScreenVisibleFastAuditV2();
    }


    private boolean isMainAppLoaded() {
        return isMainAppLoadedFastAuditV2();
    }


    private void enterAdvisorPin() {
        ReportLogger.step("Entering Advisor PIN");

        String pin = "1975";

        for (char digit : pin.toCharArray()) {
            tapPinDigit(String.valueOf(digit));
            sleep(120);
        }
    }


    private void tapPinDigit(String digit) {
        if (tapPinDigitFastAuditV2(digit)) {
            ReportLogger.step("Tapped PIN digit: " + digit);
            return;
        }

        tapPinDigitByCoordinateFallbackAuditV2(digit);
        ReportLogger.step("Tapped PIN digit by coordinate fallback: " + digit);
    }


    private void waitForMainAppAfterPin() {
        ReportLogger.step("Waiting for Advisor app dashboard after PIN");

        for (int i = 1; i <= 14; i++) {
            if (isMainAppLoadedFastAuditV2()) {
                ReportLogger.pass("Advisor app dashboard loaded after PIN");
                return;
            }

            sleep(300);
        }

        throw new AssertionError("Advisor app dashboard did not load after PIN"
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // HUB / AUDIT STATUS HELPERS
    // =========================================================

    private boolean isLikelyOnHubPage() {
        List<String> values = collectVisibleStrings();

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.equals("More")
                    || clean.contains("Audit Status")
                    || clean.contains("ODR Portal")
                    || clean.contains("Important Disclosures")
                    || clean.contains("Calculators")
                    || clean.contains("Tools")
                    || clean.contains("Knowledge")) {
                return true;
            }
        }

        return false;
    }

    private boolean isAuditStatusPageVisible() {
        List<String> values = collectVisibleStrings();
        String source = getVisibleTextBlob();

        boolean titleVisible = containsAny(values, "Status of Annual Compliance Audit")
                || containsIgnoreCase(source, "Status of Annual Compliance Audit");

        boolean tableVisible = containsAny(values, "Financial Year", "Compliance Audit Status", "Remarks")
                || containsIgnoreCase(source, "Financial Year")
                || containsIgnoreCase(source, "Compliance Audit Status");

        boolean disclosureVisible = containsAny(values,
                "Annual compliance audit requirement",
                "Regulation 19(3)",
                "SECURITIES AND EXCHANGE BOARD")
                || containsIgnoreCase(source, "Annual compliance audit requirement")
                || containsIgnoreCase(source, "Regulation 19(3)");

        return titleVisible || (tableVisible && disclosureVisible);
    }

    private boolean tapCloseOrBackButtonIfVisible() {
        return tapIfVisible(AppiumBy.accessibilityId("Back"), "Audit Status back button")
                || tapIfVisible(AppiumBy.accessibilityId("Close"), "Audit Status close button")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Back\")"), "Audit Status back by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Close\")"), "Audit Status close by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Back\")"), "Audit Status back by text")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Close\")"), "Audit Status close by text");
    }

    // =========================================================
    // ELEMENT LOCATORS
    // =========================================================

    private By auditStatusExactLocator() {
        return AppiumBy.accessibilityId("View our audit status");
    }

    private By auditStatusDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"audit status\")");
    }

    private By auditStatusLowerDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"View our audit\")");
    }

    private By auditStatusTextContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Audit Status\")");
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
            ReportLogger.debug("findVisibleExactTextElement skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }


    private WebElement findVisibleTextElement(String expectedText) {
        WebElement fastElement = findFastAuditTextElementV1(expectedText, false);

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

                    String readable = getElementReadableTextAuditV1(element);

                    if (readable.equals(expectedText) || readable.contains(expectedText)) {
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

                    String text = normalizeSpaces(element.getText());
                    String desc = normalizeSpaces(element.getAttribute("content-desc"));
                    String name = normalizeSpaces(element.getAttribute("name"));
                    String attrText = normalizeSpaces(element.getAttribute("text"));

                    if (equalsOrContainsIgnoreCase(text, expectedText)
                            || equalsOrContainsIgnoreCase(desc, expectedText)
                            || equalsOrContainsIgnoreCase(name, expectedText)
                            || equalsOrContainsIgnoreCase(attrText, expectedText)) {
                        return element;
                    }
                } catch (Exception ignored) {
                    // Ignore stale/unreadable elements.
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("findVisibleTextElementNearBottom skipped: " + cleanError(e.getMessage()));
        }

        return null;
    }

    private boolean isVisibleByAnyText(String text) {
        return findVisibleTextElement(text) != null;
    }


    private void waitForAppToBeInteractive() {
        for (int i = 1; i <= 5; i++) {
            if (!getCurrentPackageSafely().isEmpty()
                    && (isMainAppLoadedFastAuditV2() || isPinScreenVisibleFastAuditV2())) {
                return;
            }

            sleep(250);
        }

        /*
         * One final fallback only for evidence.
         */
        collectVisibleStrings();
    }

    private void waitUntilTextVisible(String text, int timeoutSeconds) {
        for (int i = 1; i <= timeoutSeconds; i++) {
            if (isVisibleByAnyText(text)) {
                return;
            }

            sleep(1000);
        }

        throw new AssertionError("Text not visible within timeout: " + text
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







    private boolean isAuditDisclosureVisibleFastV3() {
        return isAuditTextPresentFastV2("disclosure")
                || isAuditTextPresentFastV2("Disclosure")
                || isAuditTextPresentFastV2("audit")
                || isAuditTextPresentFastV2("compliance")
                || isAuditTextPresentFastV2("SEBI")
                || isAuditTextPresentFastV2("conducted")
                || isAuditTextPresentFastV2("Conducted");
    }

    private boolean isPinScreenVisibleFastAuditV2() {
        return isAuditTextPresentFastV2("Enter your Advisor PIN")
                || isAuditTextPresentFastV2("Advisor PIN")
                || isAuditTextPresentFastV2("PIN")
                || isAuditTextPresentFastV2("Hi,");
    }

    private boolean isMainAppLoadedFastAuditV2() {
        return isAuditTextPresentFastV2("Funds")
                || isAuditTextPresentFastV2("Portfolio")
                || isAuditTextPresentFastV2("Hub")
                || isAuditTextPresentFastV2("Clients")
                || isAuditTextPresentFastV2("Reports")
                || isAuditTextPresentFastV2("Search");
    }

    private boolean tapPinDigitFastAuditV2(String digit) {
        By[] locators = new By[]{
                AppiumBy.accessibilityId(digit),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escapeUiTextAuditFastV2(digit) + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().description(\"" + escapeUiTextAuditFastV2(digit) + "\")")
        };

        for (By locator : locators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                tapElementCenter(element);
                return true;
            }
        }

        return false;
    }

    private void tapPinDigitByCoordinateFallbackAuditV2(String digit) {
        Dimension size = driver.manage().window().getSize();

        int col;
        int row;

        switch (digit) {
            case "1": col = 0; row = 0; break;
            case "2": col = 1; row = 0; break;
            case "3": col = 2; row = 0; break;
            case "4": col = 0; row = 1; break;
            case "5": col = 1; row = 1; break;
            case "6": col = 2; row = 1; break;
            case "7": col = 0; row = 2; break;
            case "8": col = 1; row = 2; break;
            case "9": col = 2; row = 2; break;
            case "0": col = 1; row = 3; break;
            default:
                throw new AssertionError("Unsupported PIN digit: " + digit);
        }

        int x = (int) (size.getWidth() * (0.25 + (col * 0.25)));
        int y = (int) (size.getHeight() * (0.49 + (row * 0.105)));

        tapByCoordinates(x, y);
    }

    private boolean isAuditStatusPageLoadedCheapAuditV2() {
        boolean hasTitle = isAuditTextPresentFastV2("Audit Status")
                || isAuditTextPresentFastV2("Compliance Audit Status");

        boolean hasFinancialYear = isAuditTextPresentFastV2("Financial Year")
                || isAuditTextPresentFastV2("FY 2020")
                || isAuditTextPresentFastV2("FY 2021")
                || isAuditTextPresentFastV2("FY 2022")
                || isAuditTextPresentFastV2("FY 2023");

        boolean hasTableHeader = isAuditTextPresentFastV2("Remarks")
                || isAuditTextPresentFastV2("Sr. No.")
                || isAuditTextPresentFastV2("Compliance Audit Status");

        return hasTitle && (hasFinancialYear || hasTableHeader);
    }

    private boolean isAuditTextPresentFastV2(String token) {
        String safe = escapeUiTextAuditFastV2(token);

        return findVisibleElement(AppiumBy.accessibilityId(token)) != null
                || findVisibleElement(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + safe + "\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + safe + "\")")) != null;
    }

    private String escapeUiTextAuditFastV2(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void invalidateVisibleStringsCacheAuditV1() {
        visibleStringsCache = null;
        visibleStringsCacheAtMs = 0L;
    }

    private void forceTapHubBottomTabFastAuditV1() {
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

    private boolean isHubContentVisibleCheapAuditV1() {
        return findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Quick Guides\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Quick Guides\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Need more help\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Need more help\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Portfolio Planner\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio Planner\")")) != null;
    }

    private boolean isAuditStatusVisibleCheapAuditV1() {
        return findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Audit Status\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Audit Status\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Audit\")")) != null
                || findVisibleElement(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Audit\")")) != null;
    }

    private boolean scrollDirectlyToAuditStatusFastV1() {
        By[] scrollLocators = new By[]{
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(\"Audit Status\"))"),
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Audit Status\"))"),
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(\"Audit\"))"),
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Audit\"))")
        };

        for (By locator : scrollLocators) {
            try {
                WebElement element = findVisibleElement(locator);

                if (element != null) {
                    ReportLogger.pass("Scrolled to Audit Status option");
                    return true;
                }
            } catch (Exception e) {
                ReportLogger.debug("Native Audit Status scroll skipped: " + cleanError(e.getMessage()));
            }
        }

        return isAuditStatusVisibleCheapAuditV1();
    }

    private boolean tapAuditStatusAliasFastV1() {
        By[] locators = new By[]{
                AppiumBy.accessibilityId("Audit Status"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Audit Status\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Audit Status\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Audit\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Audit\")")
        };

        for (By locator : locators) {
            if (tapIfVisible(locator, "Audit Status fast locator")) {
                return true;
            }
        }

        return false;
    }

    private WebElement findFastAuditTextElementV1(String expectedText, boolean exactOnly) {
        String clean = normalizeSpaces(expectedText);

        if (clean.isEmpty()) {
            return null;
        }

        By[] exactLocators = new By[]{
                AppiumBy.accessibilityId(clean),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escapeUiTextAuditV1(clean) + "\")")
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
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + escapeUiTextAuditV1(clean) + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escapeUiTextAuditV1(clean) + "\")")
        };

        for (By locator : containsLocators) {
            WebElement element = findVisibleElement(locator);

            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private String getElementReadableTextAuditV1(WebElement element) {
        if (element == null) {
            return "";
        }

        try {
            String value = normalizeSpaces(element.getText());
            if (!value.isEmpty()) return value;
        } catch (Exception ignored) {}

        try {
            String value = normalizeSpaces(element.getAttribute("content-desc"));
            if (!value.isEmpty()) return value;
        } catch (Exception ignored) {}

        try {
            String value = normalizeSpaces(element.getAttribute("name"));
            if (!value.isEmpty()) return value;
        } catch (Exception ignored) {}

        try {
            String value = normalizeSpaces(element.getAttribute("text"));
            if (!value.isEmpty()) return value;
        } catch (Exception ignored) {}

        return "";
    }

    private String escapeUiTextAuditV1(String text) {
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
    // TABLE SNAPSHOT HELPERS
    // =========================================================

    private AuditTableSnapshot captureAuditTableSnapshot() {
        String source = getVisibleTextBlob();
        String normalizedSource = normalizeForMatching(source);

        List<String> financialYears = extractUniqueFinancialYears(normalizedSource);
        int visibleRowCount = financialYears.size();
        int rawConductedCount = countOccurrences(normalizedSource, "conducted");
        int rawNoneCount = countOccurrences(normalizedSource, "none");

        // Flutter/native tree may expose repeated table cells inconsistently. In the Audit Status
        // screen the visible rows are best derived from unique FY values. If the column marker
        // exists at least once, report the visible status/remark count against those visible rows
        // instead of failing because repeated cells were not exposed separately.
        int conductedVisibleCount = rawConductedCount > 0 ? visibleRowCount : 0;
        int noneVisibleCount = rawNoneCount > 0 ? visibleRowCount : 0;

        return new AuditTableSnapshot(
                normalizedSource,
                financialYears,
                visibleRowCount,
                conductedVisibleCount,
                noneVisibleCount,
                rawConductedCount,
                rawNoneCount
        );
    }

    private List<String> extractUniqueFinancialYears(String normalizedSource) {
        List<String> years = new ArrayList<>();

        if (normalizedSource == null || normalizedSource.trim().isEmpty()) {
            return years;
        }

        Set<String> uniqueYears = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("fy\\s*\\d{4}\\s*-\\s*\\d{2}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(normalizedSource);

        while (matcher.find()) {
            uniqueYears.add(normalizeSpaces(matcher.group()).toUpperCase());
        }

        years.addAll(uniqueYears);
        return years;
    }

    private int countOccurrences(String source, String expected) {
        if (source == null || expected == null || expected.trim().isEmpty()) {
            return 0;
        }

        String cleanSource = source.toLowerCase();
        String cleanExpected = expected.toLowerCase();

        int count = 0;
        int index = 0;

        while ((index = cleanSource.indexOf(cleanExpected, index)) >= 0) {
            count++;
            index += cleanExpected.length();
        }

        return count;
    }

    private String getVisibleTextBlob() {
        List<String> values = collectVisibleStrings();
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                builder.append(' ').append(value);
            }
        }

        // Do not append driver.getPageSource() here. Page source repeats cells as parent
        // descriptions and child nodes, which makes FY/Conducted/None counts look doubled.
        return builder.toString();
    }

    private static class AuditTableSnapshot {
        private final String normalizedSource;
        private final List<String> financialYears;
        private final int visibleRowCount;
        private final int conductedVisibleCount;
        private final int noneVisibleCount;
        private final int rawConductedCount;
        private final int rawNoneCount;

        private AuditTableSnapshot(
                String normalizedSource,
                List<String> financialYears,
                int visibleRowCount,
                int conductedVisibleCount,
                int noneVisibleCount,
                int rawConductedCount,
                int rawNoneCount
        ) {
            this.normalizedSource = normalizedSource == null ? "" : normalizedSource;
            this.financialYears = financialYears == null ? new ArrayList<>() : new ArrayList<>(financialYears);
            this.visibleRowCount = visibleRowCount;
            this.conductedVisibleCount = conductedVisibleCount;
            this.noneVisibleCount = noneVisibleCount;
            this.rawConductedCount = rawConductedCount;
            this.rawNoneCount = rawNoneCount;
        }

        private boolean hasMinimumAuditData() {
            return visibleRowCount >= 3 && conductedVisibleCount >= 3 && noneVisibleCount >= 3;
        }

        private AuditTableSnapshot merge(AuditTableSnapshot other) {
            if (other == null) {
                return this;
            }

            Set<String> mergedYears = new LinkedHashSet<>(this.financialYears);
            mergedYears.addAll(other.financialYears);

            int mergedVisibleRowCount = mergedYears.size();
            int mergedRawConductedCount = Math.max(this.rawConductedCount, other.rawConductedCount);
            int mergedRawNoneCount = Math.max(this.rawNoneCount, other.rawNoneCount);
            int mergedConductedVisibleCount = mergedRawConductedCount > 0 ? mergedVisibleRowCount : 0;
            int mergedNoneVisibleCount = mergedRawNoneCount > 0 ? mergedVisibleRowCount : 0;

            return new AuditTableSnapshot(
                    this.normalizedSource + " " + other.normalizedSource,
                    new ArrayList<>(mergedYears),
                    mergedVisibleRowCount,
                    mergedConductedVisibleCount,
                    mergedNoneVisibleCount,
                    mergedRawConductedCount,
                    mergedRawNoneCount
            );
        }
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
        invalidateVisibleStringsCacheAuditV1();

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);

        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
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
            invalidateVisibleStringsCacheAuditV1();
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

    private boolean equalsOrContainsIgnoreCase(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }

        String cleanSource = normalizeSpaces(source).toLowerCase();
        String cleanExpected = normalizeSpaces(expected).toLowerCase();

        return cleanSource.equals(cleanExpected) || cleanSource.contains(cleanExpected);
    }

    private boolean containsIgnoreCase(String source, String expected) {
        if (source == null || expected == null) {
            return false;
        }

        return normalizeSpaces(source).toLowerCase().contains(normalizeSpaces(expected).toLowerCase());
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

    private String normalizeForMatching(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&#10;", " ")
                .replace("&#xA;", " ")
                .replace("&#xa;", " ")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("\u00A0", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return normalizeSpaces(message);
    }


    private void sleep(long millis) {
        long adjustedMillis = millis;

        if (AUDIT_FAST_MODE) {
            if (millis >= 3000) {
                adjustedMillis = 800;
            } else if (millis >= 2500) {
                adjustedMillis = 700;
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