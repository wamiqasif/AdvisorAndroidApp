package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserAgreementPage {

    private final AndroidDriver driver;
    private String advisorAppPackage = "";

    public UserAgreementPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC STEP METHODS USED BY TEST CLASS
    // =========================================================

    public void captureAdvisorAppPackageForUserAgreement() {
        advisorAppPackage = getCurrentPackageSafely();
        ReportLogger.pass("Advisor app package captured: " + advisorAppPackage);
    }

    public void ensureAdvisorAppLoggedInForUserAgreement() {
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

    public void openHubFromBottomNavigationForUserAgreement() {
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
            throw new AssertionError("Unable to find/tap Hub tab"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        waitUntilTextVisible("Hub", 10);

        ReportLogger.pass("Hub page opened successfully");
    }

    public void scrollToUserAgreementInHubForUserAgreement() {
        ReportLogger.step("Scrolling Hub page to User Agreement option");

        for (int attempt = 0; attempt <= 12; attempt++) {
            if (isVisible(userAgreementExactLocator())
                    || isVisible(userAgreementContainsLocator())
                    || isVisibleByAnyText("User Agreement")) {
                ReportLogger.pass("User Agreement option is visible in Hub");
                return;
            }

            if (attempt > 0 && isVisibleByAnyText("More")) {
                ReportLogger.debug("More section is visible. Performing small swipe for User Agreement.");
                smallSwipeUp();
            } else {
                swipeUp();
            }

            sleep(900);
        }

        throw new AssertionError("User Agreement option not visible inside Hub after scrolling"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void tapUserAgreementForUserAgreement() {
        ReportLogger.step("Tapping User Agreement option");

        if (tapIfVisible(userAgreementExactLocator(), "User Agreement using accessibilityId")) {
            sleep(2500);
            ReportLogger.pass("Tapped User Agreement option using accessibilityId");
            return;
        }

        if (tapIfVisible(userAgreementContainsLocator(), "User Agreement using descriptionContains")) {
            sleep(2500);
            ReportLogger.pass("Tapped User Agreement option using descriptionContains");
            return;
        }

        WebElement userAgreementElement = findVisibleTextElement("User Agreement");

        if (userAgreementElement != null) {
            tapElementCenter(userAgreementElement);
            sleep(2500);
            ReportLogger.pass("Tapped User Agreement option using visible text fallback");
            return;
        }

        if (tapAnyVisibleText("User Agreement")) {
            sleep(2500);
            ReportLogger.pass("Tapped User Agreement option by fallback");
            return;
        }

        throw new AssertionError("Unable to tap User Agreement"
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void validateUserAgreementDownloadDialogForUserAgreement() {
        ReportLogger.step("Validating User Agreement PDF download/open dialog");

        for (int i = 1; i <= 35; i++) {
            List<String> values = collectVisibleStrings();

            if (isUserAgreementDownloadDialogVisible(values)) {
                ReportLogger.pass("User Agreement PDF download dialog detected"
                        + " | currentPackage=" + getCurrentPackageSafely()
                        + " | values=" + values);
                return;
            }

            /*
             * Some devices may directly open the PDF viewer without showing
             * the intermediate Open/Cancel dialog.
             */
            if (isPdfViewerVisible(values)) {
                ReportLogger.pass("User Agreement PDF viewer opened directly without download dialog"
                        + " | currentPackage=" + getCurrentPackageSafely()
                        + " | values=" + values);
                return;
            }

            sleep(1000);
        }

        throw new AssertionError("User Agreement PDF download/open dialog was not detected"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void tapOpenOnDownloadDialogForUserAgreement() {
        ReportLogger.step("Tapping Open on User Agreement PDF download dialog");

        for (int attempt = 1; attempt <= 5; attempt++) {
            List<String> valuesBeforeTap = collectVisibleStrings();

            /*
             * Critical fix:
             * Only skip tapping Open if real PDF viewer is visible.
             * Do NOT skip when the Open/Cancel download dialog is still present.
             */
            if (!isUserAgreementDownloadDialogVisible(valuesBeforeTap)
                    && isPdfViewerVisible(valuesBeforeTap)) {
                ReportLogger.pass("PDF viewer is already open. Open button tap not required.");
                return;
            }

            WebElement openButton = findVisibleExactTextElement("Open");

            if (openButton == null) {
                openButton = findVisibleTextElement("Open");
            }

            if (openButton != null) {
                tapElementCenter(openButton);
                sleep(3000);

                if (!isUserAgreementDownloadDialogVisible(collectVisibleStrings())) {
                    ReportLogger.pass("Tapped Open on PDF download dialog and dialog closed");
                    return;
                }

                ReportLogger.debug("Open button tapped but dialog is still visible. Retrying. Attempt: " + attempt);
                continue;
            }

            /*
             * Coordinate fallback for Android download dialog.
             * Used only when text-based Open locator is unavailable.
             */
            ReportLogger.step("Open button element not found. Trying coordinate fallback. Attempt: " + attempt);
            tapOpenButtonByDialogCoordinates();
            sleep(3000);

            if (!isUserAgreementDownloadDialogVisible(collectVisibleStrings())) {
                ReportLogger.pass("Tapped Open using coordinate fallback and dialog closed");
                return;
            }
        }

        throw new AssertionError("Unable to tap Open on User Agreement PDF dialog"
                + " | dialogStillVisible=" + isUserAgreementDownloadDialogVisible(collectVisibleStrings())
                + " | visibleValues=" + collectVisibleStrings());
    }

    public void validateUserAgreementPdfViewerForUserAgreement() {
        ReportLogger.step("Validating User Agreement PDF viewer/page");

        waitForPdfViewerForUserAgreement();

        List<String> values = collectVisibleStrings();

        if (isUserAgreementDownloadDialogVisible(values)) {
            throw new AssertionError("User Agreement PDF viewer validation failed because download dialog is still open"
                    + " | currentPackage=" + getCurrentPackageSafely()
                    + " | visibleValues=" + values);
        }

        boolean hasPdfFileName = containsAny(values, ".pdf", "pdf");
        boolean hasPdfViewerUi = containsAny(values,
                "Search",
                "More options",
                "Share",
                "Print",
                "Open with",
                "Drive",
                "Save to Drive",
                "PDF"
        );
        boolean packageChanged = isExternalPackageActive();

        /*
         * PDF content may not expose text to Appium.
         * Validate that the Open/Cancel dialog is gone and viewer/package/pdf markers exist.
         */
        if (!hasPdfFileName && !hasPdfViewerUi && !packageChanged) {
            throw new AssertionError("User Agreement PDF viewer did not open/read correctly"
                    + " | currentPackage=" + getCurrentPackageSafely()
                    + " | visibleValues=" + values);
        }

        ReportLogger.pass("User Agreement PDF viewer validated"
                + " | packageChanged=" + packageChanged
                + " | hasPdfFileName=" + hasPdfFileName
                + " | hasPdfViewerUi=" + hasPdfViewerUi
                + " | currentPackage=" + getCurrentPackageSafely());
    }

    public void returnBackToAdvisorAppSafely() {
        ReportLogger.step("Returning back to Advisor App after User Agreement validation");

        if (advisorAppPackage == null || advisorAppPackage.trim().isEmpty()) {
            advisorAppPackage = getCurrentPackageSafely();
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            if (isBackOnAdvisorApp()) {
                ReportLogger.pass("Already back on Advisor App");
                return;
            }

            pressBackSilently();
            sleep(1600);

            if (isBackOnAdvisorApp()) {
                ReportLogger.pass("Returned to Advisor App after back attempt " + attempt);
                return;
            }
        }

        try {
            if (advisorAppPackage != null && !advisorAppPackage.trim().isEmpty()) {
                driver.activateApp(advisorAppPackage);
                sleep(2500);

                if (isBackOnAdvisorApp()) {
                    ReportLogger.pass("Advisor App activated using package fallback");
                    return;
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("activateApp fallback failed: " + cleanError(e.getMessage()));
        }

        ReportLogger.debug("Could not confirm Advisor App return"
                + " | currentPackage=" + getCurrentPackageSafely());
    }

    // =========================================================
    // OPTIONAL SINGLE-FLOW METHOD
    // =========================================================

    public void verifyUserAgreementPdfFlowFromHub() {
        ReportLogger.step("Verifying User Agreement PDF flow from Hub");

        captureAdvisorAppPackageForUserAgreement();
        ensureAdvisorAppLoggedInForUserAgreement();
        openHubFromBottomNavigationForUserAgreement();
        scrollToUserAgreementInHubForUserAgreement();
        tapUserAgreementForUserAgreement();
        validateUserAgreementDownloadDialogForUserAgreement();
        tapOpenOnDownloadDialogForUserAgreement();
        validateUserAgreementPdfViewerForUserAgreement();

        ReportLogger.pass("User Agreement PDF flow validated successfully");
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
    // HUB / PDF HELPERS
    // =========================================================

    private boolean isLikelyOnHubPage() {
        List<String> values = collectVisibleStrings();

        for (String value : values) {
            String clean = normalizeSpaces(value);

            if (clean.equals("More")
                    || clean.contains("User Agreement")
                    || clean.contains("ODR Portal")
                    || clean.contains("Contact Us")
                    || clean.contains("Privacy Policy")
                    || clean.contains("Investor Charter")) {
                return true;
            }
        }

        return false;
    }

    private void waitForPdfViewerForUserAgreement() {
        ReportLogger.step("Waiting for User Agreement PDF viewer");

        for (int i = 1; i <= 40; i++) {
            List<String> values = collectVisibleStrings();

            if (isUserAgreementDownloadDialogVisible(values)) {
                ReportLogger.debug("PDF download dialog still visible. Waiting for viewer. Attempt: " + i);
                sleep(1000);
                continue;
            }

            if (isPdfViewerVisible(values)) {
                ReportLogger.pass("User Agreement PDF viewer is visible");
                return;
            }

            sleep(1000);
        }

        throw new AssertionError("User Agreement PDF viewer did not become visible"
                + " | currentPackage=" + getCurrentPackageSafely()
                + " | visibleValues=" + collectVisibleStrings());
    }

    private boolean isPdfViewerVisible() {
        return isPdfViewerVisible(collectVisibleStrings());
    }

    private boolean isPdfViewerVisible(List<String> values) {
        /*
         * Critical:
         * The Android download dialog also contains ".pdf", "Open", "Cancel".
         * That dialog is NOT the PDF viewer.
         */
        if (isUserAgreementDownloadDialogVisible(values)) {
            return false;
        }

        boolean hasPdfFileName = containsAny(values, ".pdf", "pdf");

        boolean hasViewerUi = containsAny(values,
                "Search",
                "More options",
                "Share",
                "Print",
                "Open with",
                "Drive",
                "Save to Drive",
                "PDF"
        );

        boolean packageChanged = isExternalPackageActive();

        return hasPdfFileName || hasViewerUi || packageChanged;
    }

    private boolean isUserAgreementDownloadDialogVisible() {
        return isUserAgreementDownloadDialogVisible(collectVisibleStrings());
    }

    private boolean isUserAgreementDownloadDialogVisible(List<String> values) {
        boolean hasPdf = containsAny(values, ".pdf", "pdf");
        boolean hasOpen = containsAny(values, "Open");
        boolean hasCancel = containsAny(values, "Cancel");
        boolean hasDownloadProgress = containsAny(values,
                "downloaded",
                "100.0%",
                "100%",
                "MB"
        );

        /*
         * Real download/open dialog condition:
         * PDF filename/progress + Open button + Cancel button/progress.
         */
        return hasPdf && hasOpen && (hasCancel || hasDownloadProgress);
    }

    private void tapOpenButtonByDialogCoordinates() {
        Dimension size = driver.manage().window().getSize();

        /*
         * Android download dialog Open button is usually bottom-right.
         * This is fallback only when text-based Open locator is unavailable.
         */
        int x = (int) (size.getWidth() * 0.86);
        int y = (int) (size.getHeight() * 0.62);

        tapByCoordinates(x, y);
    }

    private boolean isExternalPackageActive() {
        String currentPackage = getCurrentPackageSafely();

        return advisorAppPackage != null
                && !advisorAppPackage.trim().isEmpty()
                && currentPackage != null
                && !currentPackage.trim().isEmpty()
                && !currentPackage.equals(advisorAppPackage);
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

    private By userAgreementExactLocator() {
        return AppiumBy.accessibilityId("View and download your user agreement");
    }

    private By userAgreementContainsLocator() {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"user agreement\")"
        );
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