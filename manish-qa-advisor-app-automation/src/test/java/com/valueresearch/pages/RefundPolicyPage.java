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

public class RefundPolicyPage {

    private static final String REFUND_POLICY_TILE_EXACT_DESC = "View our refund policy";
    private static final String REFUND_POLICY_TITLE = "Refund and Cancellation Policy";
    private static final String REFUND_EMAIL = "advisor@valueresearch.in";
    private static final String LEGACY_REFUND_EMAIL = "advisory@valueresearch.in";

    private final AndroidDriver driver;
    private String advisorAppPackage = "";

    public RefundPolicyPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForRefundPolicy() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForRefundPolicy() {
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

    public void openHubFromBottomNavigationForRefundPolicy() {
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

    public void scrollToRefundPolicyInHubForRefundPolicy() {
        ReportLogger.step("Scrolling Hub page to Refund Policy option");

        for (int attempt = 0; attempt <= 12; attempt++) {
            if (isVisible(refundPolicyExactLocator())
                    || isVisible(refundPolicyDescriptionContainsLocator())
                    || isVisible(refundPolicyLowerDescriptionContainsLocator())
                    || isVisible(refundPolicyTextContainsLocator())
                    || isVisibleByAnyText("Refund Policy")) {
                ReportLogger.pass("Refund Policy option is visible in Hub");
                return;
            }

            if (attempt > 0 && isVisibleByAnyText("More")) {
                ReportLogger.debug("More section is visible. Performing small swipe for Refund Policy.");
                smallSwipeUp();
            } else {
                swipeUp();
            }

            sleep(900);
        }

        throw new AssertionError("Refund Policy option not visible inside Hub after scrolling"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void tapRefundPolicyForRefundPolicy() {
        ReportLogger.step("Tapping Refund Policy option");

        if (tapIfVisible(refundPolicyExactLocator(), "Refund Policy using exact accessibilityId: " + REFUND_POLICY_TILE_EXACT_DESC)) {
            sleep(2500);
            ReportLogger.pass("Tapped Refund Policy option using exact accessibilityId: " + REFUND_POLICY_TILE_EXACT_DESC);
            return;
        }

        if (tapIfVisible(refundPolicyDescriptionContainsLocator(), "Refund Policy using descriptionContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Refund Policy option using descriptionContains");
            return;
        }

        if (tapIfVisible(refundPolicyLowerDescriptionContainsLocator(), "Refund Policy using View our refund descriptionContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Refund Policy option using View our refund descriptionContains");
            return;
        }

        if (tapIfVisible(refundPolicyTextContainsLocator(), "Refund Policy using textContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped Refund Policy option using textContains");
            return;
        }

        WebElement refundPolicyElement = findVisibleTextElement("Refund Policy");

        if (refundPolicyElement != null) {
            tapElementCenter(refundPolicyElement);
            sleep(2500);
            ReportLogger.pass("Tapped Refund Policy option using visible text fallback");
            return;
        }

        if (tapAnyVisibleText("Refund Policy")) {
            sleep(2500);
            ReportLogger.pass("Tapped Refund Policy option by fallback");
            return;
        }

        throw new AssertionError("Unable to tap Refund Policy"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void waitForRefundPolicyPageForRefundPolicy() {
        ReportLogger.step("Waiting for Refund Policy page to load");

        for (int i = 1; i <= 25; i++) {
            if (isRefundPolicyPageVisible()) {
                ReportLogger.pass("Refund Policy page loaded");
                return;
            }

            sleep(800);
        }

        throw new AssertionError("Refund Policy page did not load"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void validateRefundPolicyPageContentForRefundPolicy() {
        ReportLogger.step("Validating Refund Policy page title and policy content");

        waitForRefundPolicyPageForRefundPolicy();

        List<String> values = collectVisibleStrings();
        String source = getVisibleTextBlob();

        boolean hasTitle = containsAny(values, REFUND_POLICY_TITLE)
                || containsIgnoreCase(source, REFUND_POLICY_TITLE);

        boolean hasOperator = containsAny(values, "Independent Advisors Private Limited", "Operator")
                || containsIgnoreCase(source, "Independent Advisors Private Limited")
                || containsIgnoreCase(source, "Operator");

        boolean hasRefundPolicyMarker = containsAny(values,
                "refund policy",
                "refund",
                "one quarter",
                "quarter")
                || containsIgnoreCase(source, "refund policy")
                || containsIgnoreCase(source, "one quarter");

        String visibleRefundEmail = resolveVisibleRefundEmail(values, source);
        boolean hasEmail = !visibleRefundEmail.isEmpty();

        boolean hasCancellationNotice = containsAny(values,
                "one month's notice",
                "one month",
                "cancellation")
                || containsIgnoreCase(source, "one month's notice")
                || containsIgnoreCase(source, "cancellation");

        boolean hasBankOrSubscriptionMarker = containsAny(values,
                "bank account",
                "subscription",
                "processed")
                || containsIgnoreCase(source, "bank account")
                || containsIgnoreCase(source, "subscription")
                || containsIgnoreCase(source, "processed");

        if (!hasTitle) {
            throw new AssertionError("Refund Policy title is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Refund Policy page title is visible");

        if (!hasOperator) {
            throw new AssertionError("Refund Policy operator/company text is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Refund Policy operator/company text is visible");

        if (!hasRefundPolicyMarker) {
            throw new AssertionError("Refund Policy core refund text is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Refund Policy core refund text is visible");

        if (!hasEmail) {
            throw new AssertionError("Refund Policy email link/text is not visible. Expected one of: "
                    + REFUND_EMAIL + " / " + LEGACY_REFUND_EMAIL
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Refund Policy email link/text is visible: " + visibleRefundEmail);

        if (!hasCancellationNotice) {
            throw new AssertionError("Refund Policy cancellation notice text is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Refund Policy cancellation notice text is visible");

        if (!hasBankOrSubscriptionMarker) {
            throw new AssertionError("Refund Policy bank/subscription processing text is not visible"
                    + " | visibleValues=" + values);
        }
        ReportLogger.pass("Refund Policy bank/subscription processing text is visible");

        ReportLogger.pass("Refund Policy page content validated successfully");
    }

    public void validateRefundPolicyEmailLinkForRefundPolicy() {
        ReportLogger.step("Validating Refund Policy email link tap response");

        waitForRefundPolicyPageForRefundPolicy();

        List<String> valuesBeforeTap = collectVisibleStrings();
        String sourceBeforeTap = getVisibleTextBlob();
        String refundEmailToTap = resolveVisibleRefundEmail(valuesBeforeTap, sourceBeforeTap);

        if (refundEmailToTap.isEmpty()) {
            throw new AssertionError("Refund Policy email link/text is not visible before tap. Expected one of: "
                    + REFUND_EMAIL + " / " + LEGACY_REFUND_EMAIL
                    + " | visibleValues=" + valuesBeforeTap);
        }

        String beforePackage = getCurrentPackageSafely();
        String beforeSource = getSafePageSource();

        boolean tapped = tapRefundEmailIfVisible(refundEmailToTap);

        if (!tapped) {
            throw new AssertionError("Unable to tap Refund Policy email link: " + refundEmailToTap
                    + " | visibleValues=" + collectVisibleStrings());
        }

        for (int attempt = 1; attempt <= 18; attempt++) {
            String currentPackage = getCurrentPackageSafely();
            String currentSource = getSafePageSource();
            List<String> values = collectVisibleStrings();

            boolean packageChanged = !beforePackage.isEmpty()
                    && !currentPackage.isEmpty()
                    && !currentPackage.equals(beforePackage);

            boolean mailComposeVisible = containsAny(values,
                    "To",
                    "Subject",
                    "Compose",
                    "Gmail",
                    "Mail",
                    refundEmailToTap,
                    REFUND_EMAIL,
                    LEGACY_REFUND_EMAIL)
                    || containsIgnoreCase(currentSource, refundEmailToTap)
                    || containsIgnoreCase(currentSource, REFUND_EMAIL)
                    || containsIgnoreCase(currentSource, LEGACY_REFUND_EMAIL)
                    || containsIgnoreCase(currentSource, "Subject")
                    || containsIgnoreCase(currentSource, "Compose");

            boolean sourceChanged = beforeSource != null
                    && currentSource != null
                    && !beforeSource.equals(currentSource);

            if (packageChanged || mailComposeVisible || sourceChanged) {
                ReportLogger.pass("Refund Policy email link tap response detected"
                        + " | packageChanged=" + packageChanged
                        + " | mailComposeVisible=" + mailComposeVisible
                        + " | sourceChanged=" + sourceChanged
                        + " | currentPackage=" + currentPackage);
                returnBackToRefundPolicyPageFromExternalIfNeeded();
                return;
            }

            sleep(700);
        }

        throw new AssertionError("Refund Policy email link did not trigger any visible response"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void returnBackToHubSafelyForRefundPolicy() {
        ReportLogger.step("Returning back to Hub after Refund Policy validation");

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (isLikelyOnHubPage() && isVisibleByAnyText("Hub")) {
                ReportLogger.pass("Already back on Hub page");
                return;
            }

            if (isRefundPolicyPageVisible()) {
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

        ReportLogger.debug("Could not confirm Hub return after Refund Policy flow"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifyRefundPolicyFromHub() {
        ReportLogger.step("Verifying Refund Policy module from Hub");

        captureAdvisorAppPackageForRefundPolicy();
        ensureAdvisorAppLoggedInForRefundPolicy();
        openHubFromBottomNavigationForRefundPolicy();
        scrollToRefundPolicyInHubForRefundPolicy();
        tapRefundPolicyForRefundPolicy();
        waitForRefundPolicyPageForRefundPolicy();
        validateRefundPolicyPageContentForRefundPolicy();
        validateRefundPolicyEmailLinkForRefundPolicy();

        ReportLogger.pass("Refund Policy module validated successfully");
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
    // HUB / REFUND POLICY HELPERS
    // =========================================================

    private boolean isLikelyOnHubPage() {
        List<String> values = collectVisibleStrings();

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.equals("More")
                    || clean.contains("Refund Policy")
                    || clean.contains("ODR Portal")
                    || clean.contains("Audit Status")
                    || clean.contains("Investor Complaint")
                    || clean.contains("Important Disclosures")
                    || clean.contains("Calculators")
                    || clean.contains("Tools")
                    || clean.contains("Knowledge")) {
                return true;
            }
        }

        return false;
    }

    private boolean isRefundPolicyPageVisible() {
        List<String> values = collectVisibleStrings();
        String source = getVisibleTextBlob();

        boolean titleVisible = containsAny(values, REFUND_POLICY_TITLE)
                || containsIgnoreCase(source, REFUND_POLICY_TITLE);

        boolean emailVisible = !resolveVisibleRefundEmail(values, source).isEmpty();

        boolean policyTextVisible = containsAny(values,
                "Independent Advisors Private Limited",
                "refund policy",
                "one quarter",
                "cancellation")
                || containsIgnoreCase(source, "Independent Advisors Private Limited")
                || containsIgnoreCase(source, "refund policy")
                || containsIgnoreCase(source, "one quarter")
                || containsIgnoreCase(source, "cancellation");

        return titleVisible || (emailVisible && policyTextVisible);
    }

    private String resolveVisibleRefundEmail(List<String> values, String source) {
        if (containsAny(values, REFUND_EMAIL) || containsIgnoreCase(source, REFUND_EMAIL)) {
            return REFUND_EMAIL;
        }

        if (containsAny(values, LEGACY_REFUND_EMAIL) || containsIgnoreCase(source, LEGACY_REFUND_EMAIL)) {
            return LEGACY_REFUND_EMAIL;
        }

        return "";
    }

    private boolean tapRefundEmailIfVisible(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        return tapIfVisible(AppiumBy.accessibilityId(email), "Refund Policy email link by accessibilityId: " + email)
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + email + "\")"), "Refund Policy email link by descriptionContains: " + email)
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + email + "\")"), "Refund Policy email link by textContains: " + email)
                || tapAnyVisibleText(email);
    }

    private void returnBackToRefundPolicyPageFromExternalIfNeeded() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isRefundPolicyPageVisible()) {
                ReportLogger.pass("Returned/confirmed Refund Policy page after email link check");
                return;
            }

            pressBackSilently();
            sleep(1300);

            if (isRefundPolicyPageVisible()) {
                ReportLogger.pass("Returned to Refund Policy page after back attempt " + attempt);
                return;
            }
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(1800);

                if (isRefundPolicyPageVisible()) {
                    ReportLogger.pass("Refund Policy page restored using app activate fallback");
                    return;
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp after email link failed: " + cleanError(e.getMessage()));
        }

        ReportLogger.debug("Could not confirm Refund Policy page after email link check"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    private boolean tapCloseOrBackButtonIfVisible() {
        return tapIfVisible(AppiumBy.accessibilityId("Back"), "Refund Policy back button")
                || tapIfVisible(AppiumBy.accessibilityId("Close"), "Refund Policy close button")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Back\")"), "Refund Policy back by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Close\")"), "Refund Policy close by description")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Back\")"), "Refund Policy back by text")
                || tapIfVisible(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Close\")"), "Refund Policy close by text");
    }

    // =========================================================
    // ELEMENT LOCATORS
    // =========================================================

    private By refundPolicyExactLocator() {
        return AppiumBy.accessibilityId(REFUND_POLICY_TILE_EXACT_DESC);
    }

    private By refundPolicyDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"refund policy\")");
    }

    private By refundPolicyLowerDescriptionContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"View our refund\")");
    }

    private By refundPolicyTextContainsLocator() {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Refund Policy\")");
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
    // TEXT / SOURCE HELPERS
    // =========================================================

    private String getVisibleTextBlob() {
        List<String> values = collectVisibleStrings();
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                builder.append(' ').append(value);
            }
        }

        return builder.toString();
    }

    private String getSafePageSource() {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            return "";
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