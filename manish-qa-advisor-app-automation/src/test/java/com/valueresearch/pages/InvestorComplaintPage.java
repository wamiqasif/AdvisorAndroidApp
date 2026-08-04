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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvestorComplaintPage {

    private final AndroidDriver driver;
    private String advisorAppPackage = "";

    public InvestorComplaintPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForInvestorComplaint() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForInvestorComplaint() {
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

    public void openHubFromBottomNavigationForInvestorComplaint() {
        ReportLogger.step("Opening Hub from bottom navigation");

        waitForAppToBeInteractive();

        if (isVisibleByAnyText("Hub") && isLikelyOnHubPage()) {
            ReportLogger.pass("Hub page is already visible");
            return;
        }

        WebElement hubBottomTab = findVisibleTextElementNearBottom("Hub");

        if (hubBottomTab != null) {
            tapElementCenter(hubBottomTab);
            sleep(1800);
            ReportLogger.pass("Tapped Hub bottom navigation tab");
        } else if (tapAnyVisibleText("Hub")) {
            sleep(1800);
            ReportLogger.pass("Tapped Hub tab by visible text");
        } else {
            pressBackSilently();
            sleep(1000);

            hubBottomTab = findVisibleTextElementNearBottom("Hub");
            if (hubBottomTab != null) {
                tapElementCenter(hubBottomTab);
                sleep(1800);
                ReportLogger.pass("Tapped Hub bottom navigation tab after back recovery");
            } else {
                throw new AssertionError("Unable to find/tap Hub tab"
                        + " | visibleValues=" + collectVisibleStrings());
            }
        }

        waitUntilTextVisible("Hub", 10);
        ReportLogger.pass("Hub page opened successfully");
    }

    public void scrollToInvestorComplaintInHubForInvestorComplaint() {
        ReportLogger.step("Scrolling Hub page to Investor Complaint option");

        for (int attempt = 0; attempt <= 12; attempt++) {
            if (isVisible(investorComplaintExactLocator())
                    || isVisible(investorComplaintDescriptionContainsLocator())
                    || isVisible(investorComplaintLowerDescriptionContainsLocator())
                    || isVisible(investorComplaintTextContainsLocator())
                    || isVisibleByAnyText("Investor Complaint")) {
                ReportLogger.pass("Investor Complaint option is visible in Hub");
                return;
            }

            if (attempt > 0 && isVisibleByAnyText("More")) {
                ReportLogger.debug("More section is visible. Performing small swipe for Investor Complaint.");
                smallSwipeUp();
            } else {
                swipeUp();
            }

            sleep(900);
        }

        throw new AssertionError("Investor Complaint option not visible inside Hub after scrolling"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void tapInvestorComplaintForInvestorComplaint() {
        ReportLogger.step("Tapping Investor Complaint option");

        if (tapIfVisible(investorComplaintExactLocator(), "Investor Complaint using exact accessibilityId: Submit or track complaint")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Complaint option using exact accessibilityId: Submit or track complaint");
            return;
        }

        if (tapIfVisible(investorComplaintDescriptionContainsLocator(), "Investor Complaint using descriptionContains: track complaint")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Complaint option using descriptionContains: track complaint");
            return;
        }

        if (tapIfVisible(investorComplaintLowerDescriptionContainsLocator(), "Investor Complaint using descriptionContains: complaint")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Complaint option using descriptionContains: complaint");
            return;
        }

        if (tapIfVisible(investorComplaintTextContainsLocator(), "Investor Complaint using textContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Complaint option using textContains");
            return;
        }

        WebElement investorComplaintElement = findVisibleTextElement("Investor Complaint");

        if (investorComplaintElement != null) {
            tapElementCenter(investorComplaintElement);
            sleep(2500);
            ReportLogger.pass("Tapped Investor Complaint option using visible text fallback");
            return;
        }

        if (tapAnyVisibleText("Investor Complaint")) {
            sleep(2500);
            ReportLogger.pass("Tapped Investor Complaint option by fallback");
            return;
        }

        throw new AssertionError("Unable to tap Investor Complaint"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void waitForInvestorComplaintPageForInvestorComplaint() {
        ReportLogger.step("Waiting for Investor Complaint page to load");

        for (int i = 1; i <= 30; i++) {
            if (isInvestorComplaintPageVisible()) {
                ReportLogger.pass("Investor Complaint page loaded");
                return;
            }

            sleep(800);
        }

        throw new AssertionError("Investor Complaint page did not load"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void validateInvestorComplaintPageContentForInvestorComplaint() {
        ReportLogger.step("Validating Investor Complaint page title, summary table and trend table headers");

        waitForInvestorComplaintPageForInvestorComplaint();

        List<String> values = collectVisibleStrings();
        String source = getVisibleTextBlob();

        boolean hasSummaryTitle = containsAny(values,
                "Summary of investor complaints received",
                "Investor complaints received",
                "complaints received")
                || containsIgnoreCase(source, "Summary of investor complaints received")
                || containsIgnoreCase(source, "Investor complaints received");

        boolean hasMonthText = containsAny(values,
                "Data for the month ending",
                "month ending")
                || containsIgnoreCase(source, "Data for the month ending")
                || containsIgnoreCase(source, "month ending");

        boolean hasReceivedFrom = containsAny(values,
                "Received from",
                "Directly from Investors",
                "Other Sources")
                || containsIgnoreCase(source, "Received from")
                || containsIgnoreCase(source, "Directly from Investors")
                || containsIgnoreCase(source, "Other Sources");

        boolean hasPendingHeader = containsAny(values,
                "Pending",
                "end of last month",
                "last month")
                || containsIgnoreCase(source, "Pending")
                || containsIgnoreCase(source, "end of last month");

        boolean hasTrendTitle = containsAny(values,
                "Trend of monthly disposal of complaints",
                "monthly disposal of complaints")
                || containsIgnoreCase(source, "Trend of monthly disposal of complaints")
                || containsIgnoreCase(source, "monthly disposal of complaints");

        if (!hasSummaryTitle) {
            throw new AssertionError("Investor Complaint summary title is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Investor Complaint summary title is visible");

        if (!hasMonthText) {
            throw new AssertionError("Investor Complaint month-ending text is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Investor Complaint month-ending text is visible");

        if (!(hasReceivedFrom && hasPendingHeader)) {
            throw new AssertionError("Investor Complaint summary table header validation failed"
                    + " | hasReceivedFrom=" + hasReceivedFrom
                    + " | hasPendingHeader=" + hasPendingHeader
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Investor Complaint summary table headers are visible");

        if (!hasTrendTitle) {
            ReportLogger.debug("Trend table title not visible initially. Trying one small scroll.");
            smallSwipeUp();
            sleep(900);

            values = collectVisibleStrings();
            source = getVisibleTextBlob();
            hasTrendTitle = containsAny(values,
                    "Trend of monthly disposal of complaints",
                    "monthly disposal of complaints")
                    || containsIgnoreCase(source, "Trend of monthly disposal of complaints")
                    || containsIgnoreCase(source, "monthly disposal of complaints");
        }

        if (!hasTrendTitle) {
            throw new AssertionError("Investor Complaint trend table title is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Investor Complaint trend table title is visible");

        String normalizedSource = normalizeForMatching(source);
        List<String> trendPeriods = extractTrendYears(normalizedSource);

        boolean hasYearHeader = containsAny(values, "Year", "Month") || containsIgnoreCase(source, "Year");
        boolean hasTrendPeriodData = !trendPeriods.isEmpty();
        boolean hasReceivedHeader = containsAny(values, "Received") || containsIgnoreCase(source, "Received");
        boolean hasResolvedHeader = containsAny(values, "Resolved") || containsIgnoreCase(source, "Resolved");
        boolean hasPendingTrendHeader = containsAny(values, "Pending", "Carried forward")
                || containsIgnoreCase(source, "Pending")
                || containsIgnoreCase(source, "Carried forward");

        // On this Flutter screen, the trend table column header for Month/Year is not always
        // exposed separately in the native tree. The visible rows are exposed as values like
        // "May, 2026" and "April, 2026". Treat those month-year rows as valid trend table
        // evidence instead of failing on a missing hidden header.
        if (!((hasYearHeader || hasTrendPeriodData) && hasReceivedHeader && hasResolvedHeader && hasPendingTrendHeader)) {
            throw new AssertionError("Investor Complaint trend table header/data marker validation failed"
                    + " | hasYearHeader=" + hasYearHeader
                    + " | hasTrendPeriodData=" + hasTrendPeriodData
                    + " | trendPeriods=" + trendPeriods
                    + " | hasReceivedHeader=" + hasReceivedHeader
                    + " | hasResolvedHeader=" + hasResolvedHeader
                    + " | hasPendingTrendHeader=" + hasPendingTrendHeader
                    + " | visibleValues=" + values);
        }

        ReportLogger.pass("Investor Complaint trend table headers/data markers are visible"
                + " | hasYearHeader=" + hasYearHeader
                + " | trendPeriods=" + trendPeriods);
    }

    public void validateInvestorComplaintDataForInvestorComplaint() {
        ReportLogger.step("Validating Investor Complaint data dynamically");

        waitForInvestorComplaintPageForInvestorComplaint();

        InvestorComplaintSnapshot beforeScrollSnapshot = captureInvestorComplaintSnapshot();

        if (!beforeScrollSnapshot.hasMinimumComplaintData()) {
            ReportLogger.debug("Initial Investor Complaint snapshot was weak. Trying one small scroll.");
            smallSwipeUp();
            sleep(900);
        }

        InvestorComplaintSnapshot afterScrollSnapshot = captureInvestorComplaintSnapshot();
        InvestorComplaintSnapshot finalSnapshot = beforeScrollSnapshot.merge(afterScrollSnapshot);

        if (!finalSnapshot.hasMinimumComplaintData()) {
            throw new AssertionError("Investor Complaint data validation failed"
                    + " | summaryRowsFound=" + finalSnapshot.summaryRowsFound
                    + " | trendYearCount=" + finalSnapshot.trendYearCount
                    + " | numericValueCount=" + finalSnapshot.numericValueCount
                    + " | summaryRows=" + finalSnapshot.summaryRows
                    + " | trendYears=" + finalSnapshot.trendYears
                    + " | source=" + finalSnapshot.normalizedSource);
        }

        ReportLogger.pass("Investor Complaint data validated dynamically"
                + " | summaryRowsFound=" + finalSnapshot.summaryRowsFound
                + " | trendYearCount=" + finalSnapshot.trendYearCount
                + " | numericValueCount=" + finalSnapshot.numericValueCount
                + " | summaryRows=" + finalSnapshot.summaryRows
                + " | trendYears=" + finalSnapshot.trendYears);
    }

    public void returnBackToHubSafelyForInvestorComplaint() {
        ReportLogger.step("Returning back to Hub after Investor Complaint validation");

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                ReportLogger.pass("Already back on Hub page");
                return;
            }

            if (isInvestorComplaintPageVisible()) {
                if (tapCloseOrBackButtonIfVisible()) {
                    sleep(1500);
                } else {
                    pressBackSilently();
                    sleep(1500);
                }
            } else {
                pressBackSilently();
                sleep(1500);
            }

            if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                ReportLogger.pass("Returned to Hub after back attempt " + attempt);
                return;
            }
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(1800);

                if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                    ReportLogger.pass("Returned to Hub using app activate fallback");
                    return;
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp fallback failed: " + cleanError(e.getMessage()));
        }

        ReportLogger.debug("Could not confirm Hub return after Investor Complaint flow"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifyInvestorComplaintFromHub() {
        ReportLogger.step("Verifying Investor Complaint module from Hub");

        captureAdvisorAppPackageForInvestorComplaint();
        ensureAdvisorAppLoggedInForInvestorComplaint();
        openHubFromBottomNavigationForInvestorComplaint();
        scrollToInvestorComplaintInHubForInvestorComplaint();
        tapInvestorComplaintForInvestorComplaint();
        waitForInvestorComplaintPageForInvestorComplaint();
        validateInvestorComplaintPageContentForInvestorComplaint();
        validateInvestorComplaintDataForInvestorComplaint();

        ReportLogger.pass("Investor Complaint module validated successfully");
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
                "Search"
        );
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

        for (int i = 1; i <= 25; i++) {
            if (isMainAppLoaded()) {
                ReportLogger.pass("Advisor app dashboard loaded after PIN");
                return;
            }

            sleep(1000);
        }

        throw new AssertionError("Advisor app dashboard did not load after PIN"
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // HUB / INVESTOR COMPLAINT HELPERS
    // =========================================================

    private boolean isLikelyOnHubPage() {
        List<String> values = collectVisibleStrings();

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.equals("More")
                    || clean.contains("Investor Complaint")
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

    private boolean isInvestorComplaintPageVisible() {
        List<String> values = collectVisibleStrings();
        String source = getVisibleTextBlob();

        boolean summaryVisible = containsAny(values,
                "Summary of investor complaints received",
                "Investor complaints received",
                "complaints received")
                || containsIgnoreCase(source, "Summary of investor complaints received")
                || containsIgnoreCase(source, "Investor complaints received");

        boolean monthVisible = containsAny(values,
                "Data for the month ending",
                "month ending")
                || containsIgnoreCase(source, "Data for the month ending")
                || containsIgnoreCase(source, "month ending");

        boolean rowVisible = containsAny(values,
                "Directly from Investors",
                "Other Sources",
                "Received from")
                || containsIgnoreCase(source, "Directly from Investors")
                || containsIgnoreCase(source, "Other Sources")
                || containsIgnoreCase(source, "Received from");

        boolean trendVisible = containsAny(values,
                "Trend of monthly disposal of complaints",
                "monthly disposal of complaints")
                || containsIgnoreCase(source, "Trend of monthly disposal of complaints")
                || containsIgnoreCase(source, "monthly disposal of complaints");

        return summaryVisible || (monthVisible && rowVisible) || trendVisible;
    }

    private boolean tapCloseOrBackButtonIfVisible() {
        return tapIfVisible(AppiumBy.accessibilityId("Back"), "Investor Complaint back button")
                || tapIfVisible(AppiumBy.accessibilityId("Close"), "Investor Complaint close button")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Back\")"), "Investor Complaint back by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Close\")"), "Investor Complaint close by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Back\")"), "Investor Complaint back by text")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Close\")"), "Investor Complaint close by text");
    }

    // =========================================================
    // ELEMENT LOCATORS
    // =========================================================

    private By investorComplaintExactLocator() {
        return AppiumBy.accessibilityId("Submit or track complaint");
    }

    private By investorComplaintDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"track complaint\")");
    }

    private By investorComplaintLowerDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"complaint\")");
    }

    private By investorComplaintTextContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Investor Complaint\")");
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
            ReportLogger.debug("findVisibleTextElement skipped: " + cleanError(e.getMessage()));
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
        for (int i = 1; i <= 12; i++) {
            List<String> values = collectVisibleStrings();

            if (!values.isEmpty()) {
                return;
            }

            sleep(700);
        }
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

    // =========================================================
    // DYNAMIC DATA SNAPSHOT HELPERS
    // =========================================================

    private InvestorComplaintSnapshot captureInvestorComplaintSnapshot() {
        String source = getVisibleTextBlob();
        String normalizedSource = normalizeForMatching(source);

        List<String> summaryRows = extractSummaryRows(normalizedSource);
        List<String> trendYears = extractTrendYears(normalizedSource);
        int numericValueCount = countNumericTokens(normalizedSource);

        return new InvestorComplaintSnapshot(
                normalizedSource,
                summaryRows,
                summaryRows.size(),
                trendYears,
                trendYears.size(),
                numericValueCount
        );
    }

    private List<String> extractSummaryRows(String normalizedSource) {
        List<String> rows = new ArrayList<>();
        Set<String> uniqueRows = new LinkedHashSet<>();

        if (containsIgnoreCase(normalizedSource, "directly from investors")) {
            uniqueRows.add("Directly from Investors");
        }

        if (containsIgnoreCase(normalizedSource, "other sources")) {
            uniqueRows.add("Other Sources");
        }

        if (containsIgnoreCase(normalizedSource, "total")) {
            uniqueRows.add("Total");
        }

        rows.addAll(uniqueRows);
        return rows;
    }

    private List<String> extractTrendYears(String normalizedSource) {
        List<String> periods = new ArrayList<>();

        if (normalizedSource == null || normalizedSource.trim().isEmpty()) {
            return periods;
        }

        Set<String> uniquePeriods = new LinkedHashSet<>();

        Pattern monthYearPattern = Pattern.compile(
                "\\b(january|february|march|april|may|june|july|august|september|october|november|december)\\s*,?\\s*20\\d{2}\\b",
                Pattern.CASE_INSENSITIVE
        );
        Matcher monthYearMatcher = monthYearPattern.matcher(normalizedSource);

        while (monthYearMatcher.find()) {
            String period = normalizeMonthYearPeriod(monthYearMatcher.group());
            if (!period.trim().isEmpty()) {
                uniquePeriods.add(period);
            }
        }

        // Fallback for older layouts that expose only the year column instead of month-year rows.
        if (uniquePeriods.isEmpty()) {
            Pattern yearPattern = Pattern.compile("\\b20\\d{2}\\b", Pattern.CASE_INSENSITIVE);
            Matcher yearMatcher = yearPattern.matcher(normalizedSource);

            while (yearMatcher.find()) {
                String year = normalizeSpaces(yearMatcher.group());
                if (!year.trim().isEmpty()) {
                    uniquePeriods.add(year);
                }
            }
        }

        periods.addAll(uniquePeriods);
        return periods;
    }

    private String normalizeMonthYearPeriod(String rawPeriod) {
        String period = normalizeSpaces(rawPeriod);

        if (period.isEmpty()) {
            return "";
        }

        period = period.replaceAll("\\s*,\\s*", ", ");
        String[] parts = period.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            if (part.matches("20\\d{2}")) {
                builder.append(part);
            } else if (part.endsWith(",")) {
                String word = part.substring(0, part.length() - 1);
                builder.append(toTitleCase(word)).append(',');
            } else {
                builder.append(toTitleCase(part));
            }
        }

        return builder.toString();
    }

    private String toTitleCase(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        String lower = value.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    private int countNumericTokens(String source) {
        if (source == null || source.trim().isEmpty()) {
            return 0;
        }

        int count = 0;
        Matcher matcher = Pattern.compile("\\b\\d+\\b").matcher(source);

        while (matcher.find()) {
            count++;
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

        // Prefer visible accessibility strings only. Page source can duplicate parent and child
        // descriptions heavily on Flutter screens, so it is intentionally not appended here.
        return builder.toString();
    }

    private static class InvestorComplaintSnapshot {
        private final String normalizedSource;
        private final List<String> summaryRows;
        private final int summaryRowsFound;
        private final List<String> trendYears;
        private final int trendYearCount;
        private final int numericValueCount;

        private InvestorComplaintSnapshot(
                String normalizedSource,
                List<String> summaryRows,
                int summaryRowsFound,
                List<String> trendYears,
                int trendYearCount,
                int numericValueCount
        ) {
            this.normalizedSource = normalizedSource == null ? "" : normalizedSource;
            this.summaryRows = summaryRows == null ? new ArrayList<>() : new ArrayList<>(summaryRows);
            this.summaryRowsFound = summaryRowsFound;
            this.trendYears = trendYears == null ? new ArrayList<>() : new ArrayList<>(trendYears);
            this.trendYearCount = trendYearCount;
            this.numericValueCount = numericValueCount;
        }

        private boolean hasMinimumComplaintData() {
            return summaryRowsFound >= 2
                    && trendYearCount >= 2
                    && numericValueCount >= 8;
        }

        private InvestorComplaintSnapshot merge(InvestorComplaintSnapshot other) {
            if (other == null) {
                return this;
            }

            Set<String> mergedSummaryRows = new LinkedHashSet<>(this.summaryRows);
            mergedSummaryRows.addAll(other.summaryRows);

            Set<String> mergedTrendYears = new LinkedHashSet<>(this.trendYears);
            mergedTrendYears.addAll(other.trendYears);

            return new InvestorComplaintSnapshot(
                    this.normalizedSource + " " + other.normalizedSource,
                    new ArrayList<>(mergedSummaryRows),
                    mergedSummaryRows.size(),
                    new ArrayList<>(mergedTrendYears),
                    mergedTrendYears.size(),
                    Math.max(this.numericValueCount, other.numericValueCount)
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
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}