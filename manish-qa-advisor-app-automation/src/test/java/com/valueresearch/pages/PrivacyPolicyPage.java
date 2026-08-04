package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivacyPolicyPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private String advisorAppPackage;

    private boolean advisorUrlFound;
    private boolean googlePolicyFound;
    private boolean termsConditionsFound;
    private boolean grievanceEmailFound;

    private static final String CHROME_PACKAGE = "com.android.chrome";

    private static final By HUB_TAB_TEXT =
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Hub\")");

    private static final By HUB_TAB_DESC =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Hub\")");

    private static final By PRIVACY_POLICY_TILE_EXACT =
            AppiumBy.accessibilityId("Read our privacy policy");

    private static final By PRIVACY_POLICY_TILE_DESC =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"privacy policy\")");

    private static final By PRIVACY_POLICY_TILE_TEXT =
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Privacy Policy\")");

    private static final By PRIVACY_POLICY_WEBVIEW =
            AppiumBy.className("android.webkit.WebView");

    private static final By ADVISOR_URL_TEXT =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"advisor.valueresearchonline.com\")"
            );

    private static final By ADVISOR_URL_DESC =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"advisor.valueresearchonline.com\")"
            );

    /*
     * Important: Appium Inspector shows actual clickable WebView link containers as
     * android.view.View nodes with content-desc. The child android.widget.TextView
     * nodes are clickable=false. These description locators are therefore preferred
     * over text locators for actual tapping.
     */
    private static final By GOOGLE_API_POLICY_DESC =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Google API Services User Data Policy\")"
            );

    private static final By GOOGLE_API_POLICY_TEXT =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"Google API Services User Data Policy\")"
            );

    private static final By TERMS_AND_CONDITIONS_DESC =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Terms and Conditions\")"
            );

    private static final By TERMS_AND_CONDITIONS_TEXT =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"Terms and Conditions\")"
            );

    private static final By GRIEVANCE_EMAIL_DESC =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"grievanceofficer@valueresearch.in\")"
            );

    private static final By GRIEVANCE_EMAIL_TEXT =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"grievanceofficer@valueresearch.in\")"
            );

    public PrivacyPolicyPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    // =========================================================
    // METHODS USED BY PrivacyPolicyTest.java
    // =========================================================

    public void captureAdvisorAppPackageForPrivacyPolicy() {
        advisorAppPackage = safeCurrentPackage();

        if (advisorAppPackage == null || advisorAppPackage.trim().isEmpty()) {
            throw new RuntimeException("Unable to capture Advisor app package");
        }

        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForPrivacyPolicy() {
        ReportLogger.step("Checking Advisor app login/session state");

        AuthHelper authHelper = new AuthHelper(driver);
        authHelper.ensureLoggedIn();

        ReportLogger.pass("Advisor app login/session confirmed");
    }

    public void openHubFromBottomNavigationForPrivacyPolicy() {
        ReportLogger.step("Opening Hub from bottom navigation");

        if (tapIfVisible(HUB_TAB_TEXT, "Hub bottom navigation tab", 5)) {
            sleep(1500);
            ReportLogger.pass("Hub page opened successfully");
            return;
        }

        if (tapIfVisible(HUB_TAB_DESC, "Hub bottom navigation tab by description", 6)) {
            sleep(1500);
            ReportLogger.pass("Hub page opened successfully");
            return;
        }

        throw new RuntimeException("Hub bottom navigation tab not found");
    }

    public void scrollToPrivacyPolicyInHubForPrivacyPolicy() {
        ReportLogger.step("Scrolling Hub page to Privacy Policy option");

        if (findPrivacyPolicyTile() != null) {
            ReportLogger.pass("Privacy Policy option is visible in Hub");
            return;
        }

        for (int attempt = 1; attempt <= 14; attempt++) {
            scrollDownInCurrentView();
            sleep(700);

            if (findPrivacyPolicyTile() != null) {
                ReportLogger.pass("Privacy Policy option is visible in Hub");
                return;
            }
        }

        throw new RuntimeException("Privacy Policy option not found in Hub");
    }

    public void tapPrivacyPolicyForPrivacyPolicy() {
        ReportLogger.step("Tapping Privacy Policy option");

        WebElement tile = findPrivacyPolicyTile();

        if (tile == null) {
            scrollToPrivacyPolicyInHubForPrivacyPolicy();
            tile = findPrivacyPolicyTile();
        }

        if (tile == null) {
            throw new RuntimeException("Privacy Policy tile was not visible before tap");
        }

        safeClick(tile);
        sleep(1500);

        ReportLogger.pass("Tapped Privacy Policy option using stable locator: Read our privacy policy");
    }

    public void waitForPrivacyPolicyPageForPrivacyPolicy() {
        waitForPrivacyPolicyPage();
    }

    public void validatePrivacyPolicyPageContentForPrivacyPolicy() {
        ReportLogger.step("Confirming that the Privacy Policy page is open");

        waitForPrivacyPolicyPage();

        ReportLogger.pass("Privacy Policy page is open successfully");
    }

    public void validatePrivacyPolicyScrollableContentForPrivacyPolicy() {
        ReportLogger.step("Scrollable-content validation skipped as per requirement; confirming page remains open");

        waitForPrivacyPolicyPage();

        ReportLogger.pass("Privacy Policy page remains open; deep content validation is not required");
    }

    public void validatePrivacyPolicyLinksPresentForPrivacyPolicy() {
        ReportLogger.step("Link-marker validation skipped as per requirement; confirming page remains open");

        waitForPrivacyPolicyPage();

        ReportLogger.pass("Privacy Policy page remains open; link-marker validation is not required");
    }

    public void openAndValidatePrivacyPolicyLinksForPrivacyPolicy() {
        ReportLogger.step("External-link opening skipped as per requirement; confirming page remains open");

        waitForPrivacyPolicyPage();

        ReportLogger.pass("Privacy Policy page remains open; external links were intentionally not opened");
    }

    public void returnBackToHubSafelyForPrivacyPolicy() {
        ReportLogger.step("Returning back to Hub after Privacy Policy validation");

        activateAdvisorAppIfNeeded();

        for (int attempt = 1; attempt <= 7; attempt++) {
            if (isHubPageVisible()) {
                ReportLogger.pass("Returned to Hub after back attempt " + (attempt - 1));
                return;
            }

            driver.navigate().back();
            sleep(1200);
        }

        if (!isHubPageVisible()) {
            openHubFromBottomNavigationForPrivacyPolicy();
        }

        if (!isHubPageVisible()) {
            throw new RuntimeException("Unable to return back to Hub from Privacy Policy module");
        }

        ReportLogger.pass("Returned to Hub safely");
    }

    // =========================================================
    // OPTIONAL COMPATIBILITY METHODS
    // =========================================================

    public void openPrivacyPolicyFromHub() {
        openHubFromBottomNavigationForPrivacyPolicy();
        scrollToPrivacyPolicyInHubForPrivacyPolicy();
        tapPrivacyPolicyForPrivacyPolicy();
        waitForPrivacyPolicyPageForPrivacyPolicy();
    }

    public void verifyPrivacyPolicyPageOpened() {
        waitForPrivacyPolicyPageForPrivacyPolicy();
        validatePrivacyPolicyPageContentForPrivacyPolicy();
    }

    public void verifyPrivacyPolicyLinks() {
        validatePrivacyPolicyLinksPresentForPrivacyPolicy();
        openAndValidatePrivacyPolicyLinksForPrivacyPolicy();
    }

    public void validatePrivacyPolicyLinks() {
        verifyPrivacyPolicyLinks();
    }

    public void verifyInternalLinks() {
        verifyPrivacyPolicyLinks();
    }

    public void verifyLinks() {
        verifyPrivacyPolicyLinks();
    }

    // =========================================================
    // LINK OPEN FLOWS - ACTUAL PAGE LINK TAPS
    // =========================================================

    private void openGoogleApiPolicyLinkForPrivacyPolicy() {
        ReportLogger.step("Opening Google API Services User Data Policy using actual Privacy Policy page link");

        restorePrivacyPolicyPageAtTopForNextLink("Google API Services User Data Policy");

        WebElement link = scrollUntilAnyElementVisible(
                new By[]{GOOGLE_API_POLICY_DESC, GOOGLE_API_POLICY_TEXT},
                18,
                "Google API Services User Data Policy"
        );

        tapActualVisibleLinkAndValidate(
                link,
                "Google API Services User Data Policy",
                LinkType.WEB,
                new String[]{"Google", "API", "User Data", "Limited Use"}
        );
    }

    private void openTermsAndConditionsLinkForPrivacyPolicy() {
        ReportLogger.step("Opening Terms and Conditions using actual Privacy Policy page link");

        restorePrivacyPolicyPageAtTopForNextLink("Terms and Conditions");

        WebElement link = scrollUntilAnyElementVisible(
                new By[]{TERMS_AND_CONDITIONS_DESC, TERMS_AND_CONDITIONS_TEXT},
                24,
                "Terms and Conditions"
        );

        tapActualVisibleLinkAndValidate(
                link,
                "Terms and Conditions",
                LinkType.WEB,
                new String[]{"Terms", "Conditions", "User Agreement"}
        );
    }

    private void openGrievanceEmailLinkForPrivacyPolicy() {
        ReportLogger.step("Opening grievance officer email using actual Privacy Policy page link");

        restorePrivacyPolicyPageAtTopForNextLink("Grievance Officer email");

        WebElement link = scrollUntilAnyElementVisible(
                new By[]{GRIEVANCE_EMAIL_DESC, GRIEVANCE_EMAIL_TEXT},
                30,
                "grievanceofficer@valueresearch.in"
        );

        tapActualVisibleLinkAndValidate(
                link,
                "Grievance Officer email",
                LinkType.EMAIL,
                new String[]{"Gmail", "Email", "Compose", "Complete action", "Choose"}
        );
    }

    private void tapActualVisibleLinkAndValidate(WebElement linkElement,
                                                 String linkName,
                                                 LinkType linkType,
                                                 String[] destinationMarkers) {
        if (linkElement == null) {
            throw new RuntimeException("Actual visible Privacy Policy link element not found before tap: " + linkName);
        }

        String beforePackage = safeCurrentPackage();
        String beforeSource = safePageSource();

        ReportLogger.pass("Actual Privacy Policy page link is visible before tap: " + linkName);

        boolean opened = false;
        Exception lastTapException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                tapElementBySafePoint(linkElement, attempt, linkName);
                ReportLogger.step("Tapped actual Privacy Policy page link: " + linkName + " | tapAttempt=" + attempt);

                opened = waitForActualLinkOpenResult(beforePackage, beforeSource, linkName, linkType, destinationMarkers);

                if (opened) {
                    ReportLogger.pass("Actual Privacy Policy page-link tap opened destination: " + linkName);
                    break;
                }
            } catch (Exception e) {
                lastTapException = e;
                ReportLogger.step("Actual Privacy Policy page-link tap attempt failed: " + linkName
                        + " | tapAttempt=" + attempt
                        + " | error=" + e.getMessage());
            }

            if (!isPrivacyPolicyPageVisible()) {
                break;
            }

            sleep(900);
        }

        if (!opened) {
            String message = "Actual visible Privacy Policy page link did not open destination: " + linkName
                    + " | beforePackage=" + beforePackage
                    + " | currentPackage=" + safeCurrentPackage();

            if (lastTapException != null) {
                throw new RuntimeException(message, lastTapException);
            }

            throw new RuntimeException(message);
        }

        returnBackToPrivacyPolicyPage(linkName);
    }

    private boolean waitForActualLinkOpenResult(String beforePackage,
                                                String beforeSource,
                                                String linkName,
                                                LinkType linkType,
                                                String[] destinationMarkers) {
        ReportLogger.step("Waiting after actual Privacy Policy page-link tap: " + linkName
                + " | beforePackage=" + beforePackage);

        long endTime = System.currentTimeMillis() + 22000;

        while (System.currentTimeMillis() < endTime) {
            handleChooserOrBrowserPopupIfVisible();

            String currentPackage = safeCurrentPackage();
            String currentSource = safePageSource();

            if (currentPackage != null
                    && beforePackage != null
                    && !currentPackage.equals(beforePackage)) {
                ReportLogger.pass("External app/browser opened after actual page-link tap: " + linkName
                        + " | currentPackage=" + currentPackage);
                return true;
            }

            if (linkType == LinkType.EMAIL && isEmailOpenResultVisible(currentSource)) {
                ReportLogger.pass("Email chooser/client opened after actual page-link tap: " + linkName);
                return true;
            }

            if (containsAny(currentSource, destinationMarkers)) {
                ReportLogger.pass("Destination marker visible after actual page-link tap: " + linkName);
                return true;
            }

            if (currentSource != null
                    && beforeSource != null
                    && !currentSource.equals(beforeSource)
                    && !isTextSelectionToolbarVisible(currentSource)
                    && containsAny(currentSource, destinationMarkers)) {
                ReportLogger.pass("Destination source changed after actual page-link tap: " + linkName);
                return true;
            }

            sleep(650);
        }

        return false;
    }

    private void returnBackToPrivacyPolicyPage(String linkName) {
        ReportLogger.step("Returning to Privacy Policy page after actual link tap: " + linkName);

        for (int attempt = 1; attempt <= 6; attempt++) {
            activateAdvisorAppIfNeeded();

            if (isPrivacyPolicyPageVisible()) {
                ReportLogger.pass("Privacy Policy page restored after link: " + linkName);
                return;
            }

            driver.navigate().back();
            sleep(1400);
        }

        activateAdvisorAppIfNeeded();

        if (isPrivacyPolicyPageVisible()) {
            ReportLogger.pass("Privacy Policy page restored using app activate fallback after link: " + linkName);
            return;
        }

        if (isHubPageVisible()) {
            scrollToPrivacyPolicyInHubForPrivacyPolicy();
            tapPrivacyPolicyForPrivacyPolicy();
            waitForPrivacyPolicyPage();
            ReportLogger.pass("Privacy Policy page reopened from Hub after link: " + linkName);
            return;
        }

        throw new RuntimeException("Unable to restore Privacy Policy page after link: " + linkName);
    }

    private void restorePrivacyPolicyPageAtTopForNextLink(String nextLinkName) {
        ReportLogger.step("Preparing Privacy Policy page for actual link search: " + nextLinkName);

        activateAdvisorAppIfNeeded();

        if (!isPrivacyPolicyPageVisible()) {
            if (isHubPageVisible()) {
                scrollToPrivacyPolicyInHubForPrivacyPolicy();
                tapPrivacyPolicyForPrivacyPolicy();
            }
            waitForPrivacyPolicyPage();
        }

        scrollToTopOfPrivacyPolicy();
    }

    // =========================================================
    // PAGE WAIT / STATE HELPERS
    // =========================================================

    private void waitForPrivacyPolicyPage() {
        ReportLogger.step("Waiting for Privacy Policy page to load");

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(PRIVACY_POLICY_WEBVIEW));
            ReportLogger.pass("Privacy Policy page loaded");
        } catch (Exception e) {
            throw new RuntimeException("Privacy Policy WebView did not open", e);
        }
    }

    private boolean isPrivacyPolicyPageVisible() {
        String source = safePageSource();

        return firstVisibleElement(PRIVACY_POLICY_WEBVIEW) != null
                && containsAny(source,
                "Privacy Policy",
                "Value Research Fund Advisor Privacy Policy",
                "POLICY & PURPOSE",
                "Personal Data",
                "GOOGLE USER DATA",
                "GRIEVANCE REDRESSAL");
    }

    private boolean isHubPageVisible() {
        return firstVisibleElement(PRIVACY_POLICY_TILE_EXACT) != null
                || firstVisibleElement(PRIVACY_POLICY_TILE_DESC) != null
                || isVisibleByTextContains("More")
                || isVisibleByDescriptionContains("More")
                || isVisibleByTextContains("Investor Accounts")
                || isVisibleByDescriptionContains("Investor Accounts");
    }

    private void activateAdvisorAppIfNeeded() {
        String currentPackage = safeCurrentPackage();

        if (advisorAppPackage != null
                && !advisorAppPackage.trim().isEmpty()
                && currentPackage != null
                && !currentPackage.equals(advisorAppPackage)) {
            try {
                driver.activateApp(advisorAppPackage);
                sleep(1500);
            } catch (Exception e) {
                ReportLogger.step("Unable to activate Advisor app directly: " + e.getMessage());
            }
        }
    }

    private boolean isEmailOpenResultVisible(String source) {
        return containsAny(source,
                "Gmail",
                "Email",
                "Compose",
                "Complete action",
                "Choose",
                "Open with",
                "grievanceofficer@valueresearch.in");
    }

    private boolean isTextSelectionToolbarVisible(String source) {
        return containsAny(source, "Copy", "Share", "Select all", "Web search");
    }

    private void handleChooserOrBrowserPopupIfVisible() {
        By[] popupButtons = new By[]{
                textExact("Open"),
                textExact("Continue"),
                textExact("Just once"),
                textExact("Always"),
                textExact("Chrome"),
                textExact("Gmail"),
                textExact("Email"),
                textContains("Open"),
                textContains("Continue"),
                descriptionContains("Open"),
                descriptionContains("Continue")
        };

        for (By popupButton : popupButtons) {
            WebElement button = firstVisibleElement(popupButton);
            if (button != null) {
                try {
                    safeClick(button);
                    ReportLogger.step("Tapped browser/mail chooser popup button");
                    sleep(1500);
                    return;
                } catch (Exception ignored) {
                    // Try the next possible popup button.
                }
            }
        }
    }

    // =========================================================
    // LINK MARKER COLLECTION
    // =========================================================

    private void resetPrivacyLinkFlags() {
        advisorUrlFound = false;
        googlePolicyFound = false;
        termsConditionsFound = false;
        grievanceEmailFound = false;
    }

    private void collectVisiblePrivacyPolicyLinkMarkers() {
        String source = safePageSource();

        advisorUrlFound = advisorUrlFound
                || firstVisibleElement(ADVISOR_URL_DESC) != null
                || firstVisibleElement(ADVISOR_URL_TEXT) != null
                || containsAny(source, "advisor.valueresearchonline.com");

        googlePolicyFound = googlePolicyFound
                || firstVisibleElement(GOOGLE_API_POLICY_DESC) != null
                || firstVisibleElement(GOOGLE_API_POLICY_TEXT) != null
                || containsAny(source, "Google API Services User Data Policy", "Limited Use requirements");

        termsConditionsFound = termsConditionsFound
                || firstVisibleElement(TERMS_AND_CONDITIONS_DESC) != null
                || firstVisibleElement(TERMS_AND_CONDITIONS_TEXT) != null
                || containsAny(source, "Terms and Conditions");

        grievanceEmailFound = grievanceEmailFound
                || firstVisibleElement(GRIEVANCE_EMAIL_DESC) != null
                || firstVisibleElement(GRIEVANCE_EMAIL_TEXT) != null
                || containsAny(source, "grievanceofficer@valueresearch.in");
    }

    // =========================================================
    // SCROLL HELPERS
    // =========================================================

    private WebElement scrollUntilAnyElementVisible(By[] locators, int maxScrolls, String label) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            WebElement element = firstVisibleElement(locators);

            if (element != null) {
                ReportLogger.pass("Actual Privacy Policy link/container found: " + label + " | scrollAttempt=" + attempt);
                return element;
            }

            ReportLogger.step("Actual Privacy Policy link/container not visible yet. Scrolling. Target="
                    + label + " | attempt=" + (attempt + 1));

            scrollDownInCurrentView();
            sleep(650);
        }

        throw new RuntimeException("Actual Privacy Policy link/container not found after scrolling: " + label);
    }

    private boolean scrollUntilAnyVisible(By[] locators, int maxScrolls) {
        for (int attempt = 0; attempt <= maxScrolls; attempt++) {
            if (firstVisibleElement(locators) != null) {
                return true;
            }

            scrollDownInCurrentView();
            sleep(600);
        }

        return false;
    }

    private void scrollToTopOfPrivacyPolicy() {
        ReportLogger.step("Scrolling Privacy Policy page to top");

        for (int attempt = 1; attempt <= 12; attempt++) {
            if (isPrivacyTopVisible()) {
                ReportLogger.pass("Privacy Policy top area is visible");
                return;
            }

            scrollUpInCurrentView();
            sleep(450);
        }

        ReportLogger.step("Privacy Policy top marker not confirmed after scroll-up attempts. Continuing safely.");
    }

    private boolean isPrivacyTopVisible() {
        String source = safePageSource();
        return containsAny(source,
                "Value Research Fund Advisor Privacy Policy",
                "POLICY & PURPOSE",
                "This privacy policy");
    }

    private void scrollDownInCurrentView() {
        performScrollGesture("down", 0.68);
    }

    private void scrollUpInCurrentView() {
        performScrollGesture("up", 0.68);
    }

    private void performScrollGesture(String direction, double percent) {
        Dimension size = driver.manage().window().getSize();

        Map<String, Object> args = new HashMap<>();
        args.put("left", (int) (size.width * 0.06));
        args.put("top", (int) (size.height * 0.25));
        args.put("width", (int) (size.width * 0.88));
        args.put("height", (int) (size.height * 0.62));
        args.put("direction", direction);
        args.put("percent", percent);

        try {
            ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
        } catch (Exception e) {
            ReportLogger.step("scrollGesture " + direction + " failed: " + e.getMessage());
            fallbackSwipe(direction);
        }
    }

    private void fallbackSwipe(String direction) {
        Dimension size = driver.manage().window().getSize();
        int x = size.width / 2;
        int startY;
        int endY;

        if ("down".equalsIgnoreCase(direction)) {
            startY = (int) (size.height * 0.76);
            endY = (int) (size.height * 0.30);
        } else {
            startY = (int) (size.height * 0.30);
            endY = (int) (size.height * 0.76);
        }

        Map<String, Object> args = new HashMap<>();
        args.put("left", x - 5);
        args.put("top", Math.min(startY, endY));
        args.put("width", 10);
        args.put("height", Math.abs(startY - endY));
        args.put("direction", direction);
        args.put("percent", 0.80);

        try {
            ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
        } catch (Exception ignored) {
            // Last fallback is intentionally silent to avoid noisy logs.
        }
    }

    // =========================================================
    // CLICK HELPERS
    // =========================================================

    private boolean tapIfVisible(By locator, String label, int timeoutSeconds) {
        try {
            WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement element = customWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            safeClick(element);
            ReportLogger.pass("Tapped: " + label);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void safeClick(WebElement element) {
        try {
            element.click();
        } catch (Exception normalClickFailed) {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("elementId", ((RemoteWebElement) element).getId());
                ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            } catch (Exception elementClickFailed) {
                tapElementBySafePoint(element, 1, "safeClick fallback");
            }
        }
    }

    private void tapElementBySafePoint(WebElement element, int attempt, String label) {
        Rectangle rect = element.getRect();

        int x;
        int y;

        if (attempt == 1) {
            x = rect.getX() + (rect.getWidth() / 2);
            y = rect.getY() + (rect.getHeight() / 2);
        } else if (attempt == 2) {
            x = rect.getX() + Math.max(8, (int) (rect.getWidth() * 0.28));
            y = rect.getY() + (rect.getHeight() / 2);
        } else {
            x = rect.getX() + Math.min(rect.getWidth() - 8, (int) (rect.getWidth() * 0.72));
            y = rect.getY() + (rect.getHeight() / 2);
        }

        ReportLogger.step("Tapping actual Privacy Policy link/container by coordinate"
                + " | label=" + label
                + " | attempt=" + attempt
                + " | x=" + x
                + " | y=" + y);

        Map<String, Object> args = new HashMap<>();
        args.put("x", x);
        args.put("y", y);
        ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
        sleep(1100);
    }

    // =========================================================
    // ELEMENT HELPERS
    // =========================================================

    private WebElement findPrivacyPolicyTile() {
        WebElement exact = firstVisibleElement(PRIVACY_POLICY_TILE_EXACT);
        if (exact != null) {
            return exact;
        }

        WebElement desc = firstVisibleElement(PRIVACY_POLICY_TILE_DESC);
        if (desc != null) {
            return desc;
        }

        return firstVisibleElement(PRIVACY_POLICY_TILE_TEXT);
    }

    private WebElement firstVisibleElement(By[] locators) {
        for (By locator : locators) {
            WebElement element = firstVisibleElement(locator);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private WebElement firstVisibleElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                if (element != null && element.isDisplayed()) {
                    return element;
                }
            }
        } catch (Exception ignored) {
            // Return null below.
        }

        return null;
    }

    private boolean isVisibleByTextContains(String text) {
        return firstVisibleElement(textContains(text)) != null;
    }

    private boolean isVisibleByDescriptionContains(String text) {
        return firstVisibleElement(descriptionContains(text)) != null;
    }

    private boolean isTextPresentInPageSource(String text) {
        return containsAny(safePageSource(), text);
    }

    private boolean isAdvisorUrlTextPresentWithFallback() {
        return firstVisibleElement(ADVISOR_URL_DESC) != null
                || firstVisibleElement(ADVISOR_URL_TEXT) != null
                || containsAny(safePageSource(), "advisor.valueresearchonline.com");
    }

    private By textExact(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"" + escapeUiSelectorText(text) + "\")"
        );
    }

    private By textContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + escapeUiSelectorText(text) + "\")"
        );
    }

    private By descriptionContains(String text) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escapeUiSelectorText(text) + "\")"
        );
    }

    private boolean containsAny(String source, String... markers) {
        if (source == null) {
            return false;
        }

        for (String marker : markers) {
            if (marker != null && source.contains(marker)) {
                return true;
            }
        }

        return false;
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception e) {
            return "";
        }
    }

    private String safePageSource() {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    private String escapeUiSelectorText(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private enum LinkType {
        WEB,
        EMAIL
    }
}