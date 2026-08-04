package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BottomNavBarPage {

    private final AndroidDriver driver;

    private static final String TAB_FUNDS = "Funds";
    private static final String TAB_STOCKS = "Stocks";
    private static final String TAB_PORTFOLIO = "Portfolio";
    private static final String TAB_HUB = "Hub";

    private static final long NORMAL_TAB_SETTLE_MS = 750L;
    private static final long RETAP_SETTLE_MS = 900L;
    private static final long MARKER_RETRY_MS = 500L;

    private final By fundsTab = AppiumBy.accessibilityId(TAB_FUNDS);
    private final By stocksTab = AppiumBy.accessibilityId(TAB_STOCKS);
    private final By portfolioTab = AppiumBy.accessibilityId(TAB_PORTFOLIO);
    private final By hubTab = AppiumBy.accessibilityId(TAB_HUB);

    private final String[] fundsMarkers = new String[]{
            "Portfolio Value",
            "Outdated Portfolio",
            "Update Portfolio",
            "Rich Future Starts Here"
    };

    private final String[] stocksMarkers = new String[]{
            "Value Research Stock Advisor",
            "Stock Advisor",
            "Quick Search",
            "Search for a stock"
    };

    private final String[] portfolioMarkers = new String[]{
            "Summary",
            "Stocks & ETFs",
            "NPS",
            "Analysis",
            "Your Investments",
            "Portfolio Value"
    };

    private final String[] hubMarkers = new String[]{
            "Profile",
            "Account Details",
            "Subscription Details",
            "Portfolio Settings",
            "App Settings",
            "Investor Accounts",
            "Mutual Funds"
    };

    public BottomNavBarPage(AndroidDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("AndroidDriver cannot be null.");
        }
        this.driver = driver;
    }

    // =====================================================================
    // Public validations
    // =====================================================================

    public void verifyFundsTabOpens() {
        try {
            ReportLogger.step("Validating Funds bottom tab navigation");
            tapBottomTab(TAB_FUNDS);
            waitForFundsScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Funds tab opened and screen markers validated successfully");
        } catch (Exception e) {
            captureScreenshot("BN_001_Funds_Tab_Failure");
            throw new RuntimeException("Funds tab validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyStocksTabOpens() {
        try {
            ReportLogger.step("Validating Stocks bottom tab navigation");
            tapBottomTab(TAB_STOCKS);
            waitForStocksScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Stocks tab opened and screen markers validated successfully");
        } catch (Exception e) {
            captureScreenshot("BN_002_Stocks_Tab_Failure");
            throw new RuntimeException("Stocks tab validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyPortfolioTabOpens() {
        try {
            ReportLogger.step("Validating Portfolio bottom tab navigation");
            tapBottomTab(TAB_PORTFOLIO);
            waitForPortfolioScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Portfolio tab opened and screen markers validated successfully");
        } catch (Exception e) {
            captureScreenshot("BN_003_Portfolio_Tab_Failure");
            throw new RuntimeException("Portfolio tab validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyHubTabOpens() {
        try {
            ReportLogger.step("Validating Hub bottom tab navigation");
            tapBottomTab(TAB_HUB);
            waitForHubScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Hub tab opened and screen markers validated successfully");
        } catch (Exception e) {
            captureScreenshot("BN_004_Hub_Tab_Failure");
            throw new RuntimeException("Hub tab validation failed: " + cleanError(e.getMessage()), e);
        }
    }

    public void verifyAllBottomNavLabelsVisible() {
        try {
            ReportLogger.step("Validating all bottom navigation labels are visible/present");

            tapBottomTab(TAB_FUNDS);
            waitForFundsScreen();
            assertBottomNavVisible();

            ReportLogger.pass(
                    "All bottom navigation labels validated successfully: Funds, Stocks, Portfolio, Hub"
            );
        } catch (Exception e) {
            captureScreenshot("BN_005_Bottom_Nav_Labels_Failure");
            throw new RuntimeException(
                    "Bottom nav labels validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    public void verifyActiveTabChangesAfterEachTap() {
        try {
            ReportLogger.step("Validating screen/content changes after each bottom tab tap");

            tapBottomTab(TAB_FUNDS);
            waitForFundsScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Active tab validation checkpoint passed: Funds");

            tapBottomTab(TAB_STOCKS);
            waitForStocksScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Active tab changed successfully: Funds -> Stocks");

            tapBottomTab(TAB_PORTFOLIO);
            waitForPortfolioScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Active tab changed successfully: Stocks -> Portfolio");

            tapBottomTab(TAB_HUB);
            waitForHubScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Active tab changed successfully: Portfolio -> Hub");

            ReportLogger.pass("Bottom nav active-tab/content change validation completed successfully");
        } catch (Exception e) {
            captureScreenshot("BN_006_Active_Tab_Change_Failure");
            throw new RuntimeException(
                    "Active tab change validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    public void verifySelectedFundsTabRetapDoesNotCrashOrNavigateUnexpectedly() {
        try {
            ReportLogger.step(
                    "Negative validation: re-tapping selected Funds tab should not crash "
                            + "or navigate unexpectedly"
            );

            tapBottomTab(TAB_FUNDS);
            waitForFundsScreen();
            assertBottomNavVisible();

            tapBottomTab(TAB_FUNDS);
            sleep(RETAP_SETTLE_MS);

            assertDriverResponsive();
            assertNoCrashOrAnr();
            waitForFundsScreen();
            assertBottomNavVisible();

            ReportLogger.pass("Selected Funds tab re-tap kept user on Funds screen");
        } catch (Exception e) {
            captureScreenshot("BN_007_Selected_Funds_Retap_Failure");
            throw new RuntimeException(
                    "Selected Funds tab re-tap validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    public void verifyRapidBottomTabSwitchingStability() {
        try {
            ReportLogger.step("Stability validation: rapid switching across bottom nav tabs");

            for (int cycle = 1; cycle <= 3; cycle++) {
                ReportLogger.step("Bottom nav rapid switch cycle: " + cycle);

                tapBottomTab(TAB_FUNDS);
                waitForFundsScreen();

                tapBottomTab(TAB_STOCKS);
                waitForStocksScreen();

                tapBottomTab(TAB_PORTFOLIO);
                waitForPortfolioScreen();

                tapBottomTab(TAB_HUB);
                waitForHubScreen();
            }

            assertDriverResponsive();
            assertNoCrashOrAnr();
            assertBottomNavVisible();

            ReportLogger.pass("Rapid bottom nav switching stability validation passed");
        } catch (Exception e) {
            captureScreenshot("BN_008_Rapid_Bottom_Nav_Switch_Failure");
            throw new RuntimeException(
                    "Rapid bottom nav switching validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    public void verifyNoCrashOrAnrAfterBottomNavSwitching() {
        try {
            ReportLogger.step("Validating no crash/ANR after controlled bottom navigation switching");

            tapBottomTab(TAB_FUNDS);
            waitForFundsScreen();

            tapBottomTab(TAB_STOCKS);
            waitForStocksScreen();

            tapBottomTab(TAB_PORTFOLIO);
            waitForPortfolioScreen();

            tapBottomTab(TAB_HUB);
            waitForHubScreen();

            assertDriverResponsive();
            assertNoCrashOrAnr();
            assertBottomNavVisible();

            ReportLogger.pass("No crash/ANR marker found after bottom navigation switching");
        } catch (Exception e) {
            captureScreenshot("BN_009_No_Crash_Anr_Failure");
            throw new RuntimeException(
                    "No crash/ANR validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    public void verifyReturnToFundsTab() {
        try {
            ReportLogger.step("Validating final navigation returns to Funds tab");
            tapBottomTab(TAB_FUNDS);
            waitForFundsScreen();
            assertBottomNavVisible();
            ReportLogger.pass("Final navigation returned to Funds tab successfully");
        } catch (Exception e) {
            captureScreenshot("BN_010_Return_To_Funds_Failure");
            throw new RuntimeException(
                    "Return to Funds validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =====================================================================
    // Screen waits / screen detection
    // =====================================================================

    private void waitForFundsScreen() {
        waitForScreenMarkers("Funds", fundsMarkers, 1, 12);
    }

    private void waitForStocksScreen() {
        waitForScreenMarkers("Stocks", stocksMarkers, 1, 12);
    }

    private void waitForPortfolioScreen() {
        waitForScreenMarkers("Portfolio", portfolioMarkers, 2, 14);
    }

    private void waitForHubScreen() {
        waitForScreenMarkers("Hub", hubMarkers, 2, 14);
    }

    private void waitForScreenMarkers(
            String screenName,
            String[] markers,
            int minimumMatches,
            int timeoutSeconds
    ) {
        ReportLogger.step("Waiting for " + screenName + " screen markers to load");

        long deadline = System.currentTimeMillis() + Duration.ofSeconds(timeoutSeconds).toMillis();
        Set<String> matchedMarkers = new LinkedHashSet<>();

        while (System.currentTimeMillis() <= deadline) {
            assertDriverResponsive();
            matchedMarkers.clear();

            for (String marker : markers) {
                if (isUiTextPresent(marker)) {
                    matchedMarkers.add(marker);
                }
            }

            if (matchedMarkers.size() >= minimumMatches) {
                ReportLogger.pass(
                        screenName + " screen loaded | matchedMarkers=" + matchedMarkers.size()
                );

                for (String marker : matchedMarkers) {
                    ReportLogger.pass("Validated " + screenName + " marker: " + marker);
                }
                return;
            }

            sleep(MARKER_RETRY_MS);
        }

        throw new RuntimeException(
                screenName + " screen did not load. Expected at least "
                        + minimumMatches + " marker(s) from: "
                        + String.join(", ", markers)
        );
    }

    private boolean isAnyKnownAppScreenVisible() {
        return countPresentMarkers(fundsMarkers) >= 1
                || countPresentMarkers(stocksMarkers) >= 1
                || countPresentMarkers(portfolioMarkers) >= 2
                || countPresentMarkers(hubMarkers) >= 2;
    }

    private int countPresentMarkers(String[] markers) {
        int count = 0;

        for (String marker : markers) {
            if (isUiTextPresent(marker)) {
                count++;
            }
        }

        return count;
    }

    // =====================================================================
    // Bottom nav helpers
    // =====================================================================

    private void tapBottomTab(String tabName) {
        ReportLogger.step("Tapping bottom nav tab: " + tabName);
        assertDriverResponsive();

        By exactLocator = getExactTabLocator(tabName);
        By descContains = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\""
                        + escapeUiSelector(tabName)
                        + "\")"
        );
        By textContains = AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\""
                        + escapeUiSelector(tabName)
                        + "\")"
        );

        if (tapElementIfPresent(exactLocator, tabName + " bottom tab")
                || tapElementIfPresent(
                        descContains,
                        tabName + " bottom tab by description contains"
                )
                || tapElementIfPresent(
                        textContains,
                        tabName + " bottom tab by text contains"
                )) {
            sleep(NORMAL_TAB_SETTLE_MS);
            return;
        }

        /*
         * Coordinate fallback is allowed only when the application is already
         * confirmed to be on a known logged-in screen. This prevents accidental
         * taps on the PIN screen or another unexpected page after recovery.
         */
        if (countVisibleBottomTabs() == 0 && !isAnyKnownAppScreenVisible()) {
            throw new RuntimeException(
                    tabName + " bottom tab was not found and the app is not on a "
                            + "known logged-in screen. Coordinate fallback was blocked."
            );
        }

        ReportLogger.step(
                tabName + " bottom tab locator tap failed. Trying guarded coordinate fallback."
        );
        tapBottomTabCoordinateFallback(tabName);
        sleep(NORMAL_TAB_SETTLE_MS);
    }

    private By getExactTabLocator(String tabName) {
        switch (tabName) {
            case TAB_FUNDS:
                return fundsTab;
            case TAB_STOCKS:
                return stocksTab;
            case TAB_PORTFOLIO:
                return portfolioTab;
            case TAB_HUB:
                return hubTab;
            default:
                return AppiumBy.accessibilityId(tabName);
        }
    }

    private void tapBottomTabCoordinateFallback(String tabName) {
        assertDriverResponsive();

        Dimension size = driver.manage().window().getSize();
        int y = Math.min(size.height - 55, (int) (size.height * 0.945));
        int x;

        switch (tabName) {
            case TAB_FUNDS:
                x = (int) (size.width * 0.125);
                break;
            case TAB_STOCKS:
                x = (int) (size.width * 0.375);
                break;
            case TAB_PORTFOLIO:
                x = (int) (size.width * 0.625);
                break;
            case TAB_HUB:
                x = (int) (size.width * 0.875);
                break;
            default:
                x = size.width / 2;
        }

        tapAt(x, y, tabName + " bottom tab coordinate fallback");
    }

    private void assertBottomNavVisible() {
        boolean fundsVisible = isTabPresent(TAB_FUNDS);
        boolean stocksVisible = isTabPresent(TAB_STOCKS);
        boolean portfolioVisible = isTabPresent(TAB_PORTFOLIO);
        boolean hubVisible = isTabPresent(TAB_HUB);

        if (fundsVisible && stocksVisible && portfolioVisible && hubVisible) {
            ReportLogger.pass(
                    "Bottom nav labels are visible/present: Funds, Stocks, Portfolio, Hub"
            );
            return;
        }

        throw new RuntimeException(
                "Bottom nav labels not fully visible."
                        + " Funds=" + fundsVisible
                        + ", Stocks=" + stocksVisible
                        + ", Portfolio=" + portfolioVisible
                        + ", Hub=" + hubVisible
        );
    }

    private int countVisibleBottomTabs() {
        int count = 0;

        if (isTabPresent(TAB_FUNDS)) {
            count++;
        }
        if (isTabPresent(TAB_STOCKS)) {
            count++;
        }
        if (isTabPresent(TAB_PORTFOLIO)) {
            count++;
        }
        if (isTabPresent(TAB_HUB)) {
            count++;
        }

        return count;
    }

    private boolean isTabPresent(String tabName) {
        return hasUsableElement(getExactTabLocator(tabName))
                || hasUsableElement(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().descriptionContains(\""
                                        + escapeUiSelector(tabName)
                                        + "\")"
                        )
                )
                || hasUsableElement(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().textContains(\""
                                        + escapeUiSelector(tabName)
                                        + "\")"
                        )
                );
    }

    // =====================================================================
    // Generic helpers
    // =====================================================================

    private boolean isUiTextPresent(String expectedText) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            return false;
        }

        return hasUsableElement(AppiumBy.accessibilityId(expectedText))
                || hasUsableElement(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().descriptionContains(\""
                                        + escapeUiSelector(expectedText)
                                        + "\")"
                        )
                )
                || hasUsableElement(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().textContains(\""
                                        + escapeUiSelector(expectedText)
                                        + "\")"
                        )
                );
    }

    private boolean hasUsableElement(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements == null || elements.isEmpty()) {
                return false;
            }

            for (WebElement element : elements) {
                if (element == null) {
                    continue;
                }

                try {
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (Exception displayException) {
                    if (isDriverInfrastructureFailure(displayException)) {
                        throw displayException;
                    }

                    /*
                     * Flutter semantics do not always expose displayed=true.
                     * Presence in the native hierarchy is still acceptable.
                     */
                    return true;
                }
            }

            /*
             * Some Flutter semantic nodes are present but report displayed=false.
             * Treat native hierarchy presence as sufficient for tab/marker checks.
             */
            return true;

        } catch (Exception e) {
            if (isDriverInfrastructureFailure(e)) {
                throw new RuntimeException(
                        "AndroidDriver/UiAutomator2 became unavailable while checking locator "
                                + locator + ": " + cleanError(e.getMessage()),
                        e
                );
            }

            return false;
        }
    }

    private void assertNoCrashOrAnr() {
        assertDriverResponsive();

        boolean crashDialogVisible =
                isUiTextPresent("keeps stopping")
                        || isUiTextPresent("isn't responding")
                        || isUiTextPresent("App isn't responding")
                        || isUiTextPresent("Close app");

        if (crashDialogVisible) {
            captureScreenshot("BN_Crash_Or_ANR_Detected");
            throw new RuntimeException("Crash/ANR marker detected on screen.");
        }
    }

    private void assertDriverResponsive() {
        try {
            if (driver.getSessionId() == null) {
                throw new RuntimeException("AndroidDriver session ID is null.");
            }

            /*
             * getCurrentPackage is a lightweight UiAutomator2 command.
             * It confirms the session is responsive without downloading the
             * complete page hierarchy through getPageSource().
             */
            driver.getCurrentPackage();

        } catch (Exception e) {
            throw new RuntimeException(
                    "AndroidDriver/UiAutomator2 is not responsive: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private boolean isDriverResponsiveNoThrow() {
        try {
            return driver != null
                    && driver.getSessionId() != null
                    && driver.getCurrentPackage() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tapElementIfPresent(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements == null || elements.isEmpty()) {
                return false;
            }

            WebElement best = null;

            for (WebElement element : elements) {
                if (element == null) {
                    continue;
                }

                if (best == null) {
                    best = element;
                }

                try {
                    if (element.isDisplayed()) {
                        best = element;
                        break;
                    }
                } catch (Exception displayException) {
                    if (isDriverInfrastructureFailure(displayException)) {
                        throw displayException;
                    }
                }
            }

            if (best == null) {
                return false;
            }

            try {
                best.click();
                ReportLogger.step("Tapped: " + elementName);
                return true;
            } catch (Exception clickException) {
                if (isDriverInfrastructureFailure(clickException)) {
                    throw clickException;
                }

                ReportLogger.step(
                        "Normal click failed for " + elementName
                                + ". Trying element centre tap."
                );
                tapElementCenter(best, elementName);
                return true;
            }

        } catch (Exception e) {
            if (isDriverInfrastructureFailure(e)) {
                throw new RuntimeException(
                        "AndroidDriver/UiAutomator2 became unavailable while locating "
                                + elementName + ": " + cleanError(e.getMessage()),
                        e
                );
            }

            return false;
        }
    }

    private void tapElementCenter(WebElement element, String elementName) {
        Rectangle rect = element.getRect();
        int centreX = rect.getX() + (rect.getWidth() / 2);
        int centreY = rect.getY() + (rect.getHeight() / 2);
        tapAt(centreX, centreY, elementName + " centre");
    }

    private void tapAt(int x, int y, String elementName) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 1);

            tap.addAction(
                    finger.createPointerMove(
                            Duration.ZERO,
                            PointerInput.Origin.viewport(),
                            x,
                            y
                    )
            );
            tap.addAction(
                    finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            tap.addAction(
                    finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg())
            );

            driver.perform(Collections.singletonList(tap));

            ReportLogger.step(
                    "Tapped: " + elementName + " at x=" + x + ", y=" + y
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Coordinate tap failed for " + elementName + ": "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private boolean isDriverInfrastructureFailure(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            String message = current.getMessage();
            String lower = message == null ? "" : message.toLowerCase();

            if (lower.contains("instrumentation process is not running")
                    || lower.contains("uiautomator2 server")
                    || lower.contains("cannot be proxied")
                    || lower.contains("could not proxy")
                    || lower.contains("socket hang up")
                    || lower.contains("connection refused")
                    || lower.contains("device not found")
                    || lower.contains("no connected android device")
                    || lower.contains("device offline")
                    || lower.contains("invalid session id")
                    || lower.contains("session does not exist")
                    || lower.contains("session id is null")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private String escapeUiSelector(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void captureScreenshot(String name) {
        try {
            if (isDriverResponsiveNoThrow()) {
                ScreenshotUtils.captureScreenshot(driver, name);
            } else {
                ReportLogger.step(
                        "Screenshot skipped because AndroidDriver/UiAutomator2 is unavailable."
                );
            }
        } catch (Exception ignored) {
            // Screenshot failure must not hide the original validation error.
        }
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        return message
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bottom navigation wait was interrupted.", e);
        }
    }
}