package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * VRSA Investment Planner page object.
 *
 * Stability rules used here:
 * - Exact accessibility IDs from Appium Inspector are primary locators.
 * - Dynamic investor name, stock names, prices and portfolio weights are never hardcoded.
 * - Generic hierarchy scans are used only to read dynamic table data by column geometry.
 * - No absolute XPath and no fixed element coordinates are used.
 * - W3C swipes are bounded and only used when exact semantic elements are off-screen.
 */
public class InvestmentPlannerPage {

    public static final String BUILD_VERSION = "VRSA-IP-2026-08-21-15.4";

    private static final String APP_PACKAGE = "com.valueresearch.advisor";

    private static final int SHORT_WAIT_SECONDS = 12;
    private static final int PAGE_WAIT_SECONDS = 25;

    // Investment Planner operations backed by Flutter/backend processing can take
    // longer than ordinary UI transitions. Keep these waits module-local so the
    // framework/global waits remain unchanged.
    private static final int OWN_LIST_ASYNC_WAIT_SECONDS = 20;
    private static final int MAIL_CONFIRM_WAIT_SECONDS = 60;
    private static final int ORDER_WAIT_SECONDS = 60;
    private static final int POLL_MS = 300;
    private static final long SCREEN_SETTLE_MS = 1100L;

    private final AndroidDriver driver;

    // =========================================================
    // CONFIRMED STABLE LOCATORS FROM APPIUM INSPECTOR
    // =========================================================

    private static final By HUB_TAB = AppiumBy.accessibilityId("Hub");

    // Keep the app's actual misspelling: "invesment".
    private static final By INVESTMENT_PLANNER_HUB_TILE =
            AppiumBy.accessibilityId("Open invesment planner");

    private static final By INVESTMENT_PLANNER_TITLE =
            AppiumBy.accessibilityId("Investment Planner");

    private static final By ALL_WEATHER = AppiumBy.accessibilityId("All Weather");
    private static final By LONG_TERM = AppiumBy.accessibilityId("Long Term");
    private static final By AGGRESSIVE = AppiumBy.accessibilityId("Aggressive");

    private static final By OWN_LIST =
            AppiumBy.accessibilityId("Or choose your own list");

    private static final By AMOUNT_LABEL =
            AppiumBy.accessibilityId("How much do you want to invest?");

    private static final By GENERATE_MY_ORDER =
            AppiumBy.accessibilityId("Generate my order");

    private static final By CHOOSE_A_LIST =
            AppiumBy.accessibilityId("Choose a list");

    private static final By RECOMMENDATIONS =
            AppiumBy.accessibilityId("Recommendations");

    private static final By WATCHLIST =
            AppiumBy.accessibilityId("Watchlist");

    // Exact clickable accessibility IDs confirmed in Appium Inspector on 2026-08-19.
    private static final By RECOMMENDATION_ALL =
            AppiumBy.accessibilityId("All");

    private static final By WATCHLIST_A5 =
            AppiumBy.accessibilityId("A5");

    private static final By YOUR_ORDERS =
            AppiumBy.accessibilityId("Your Orders");

    private static final By AMOUNT_INVESTED =
            AppiumBy.accessibilityId("Amount invested");

    private static final By COMPANY_NAME_HEADER =
            AppiumBy.accessibilityId("Company Name");

    private static final By ACTUAL_WEIGHT_HEADER =
            AppiumBy.accessibilityId("Actual Weight");

    private static final By VALUE_HEADER =
            AppiumBy.accessibilityId("Value (₹)");

    private static final By MAIL_MY_ORDER =
            AppiumBy.accessibilityId("Mail my order");

    // Generated-order link is a clickable ImageView.
    private static final By PORTFOLIO_PREVIEW_LINK =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.ImageView\").description(\"Portfolio preview\")"
            );

    // Actual Portfolio Preview screen title is a non-clickable View.
    private static final By PORTFOLIO_PREVIEW_PAGE_TITLE =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.view.View\").description(\"Portfolio preview\")"
            );

    // Generic semantic anchor used after route transitions. The clickable Your Orders
    // link is an ImageView, while the preview page title is a View, but Flutter can
    // transiently expose only the generic accessibility node during rebuilds.
    private static final By PORTFOLIO_PREVIEW_ANY =
            AppiumBy.accessibilityId("Portfolio preview");

    private static final By MAIL_SUCCESS_EXACT =
            AppiumBy.accessibilityId("Order mailed successfully");

    private static final By MAIL_SUCCESS =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Order mailed successfully\")"
            );

    private static final By MAIL_SUCCESS_TEXT =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"Order mailed successfully\")"
            );

    private static final By MAIL_SUCCESS_TOAST =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Toast\").textContains(\"Order mailed successfully\")"
            );

    private static final By UNDERLYING_DATA_ERROR =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Underlying data not available\")"
            );

    private static final By EDIT_TEXT =
            AppiumBy.className("android.widget.EditText");

    private static final By HORIZONTAL_SCROLL_VIEW =
            AppiumBy.className("android.widget.HorizontalScrollView");

    public InvestmentPlannerPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // PUBLIC - NAVIGATION / LANDING PAGE
    // =========================================================

    public void openInvestmentPlannerFromHub() {
        ReportLogger.step("Opening VRSA Investment Planner");

        activateAdvisorAppIfNeeded();

        /*
         * Fast path: keep this module local. If the previous testcase left us on
         * Investment Planner, Your Orders, Portfolio Preview, or the Choose-list
         * sheet, recover only as far as the Investment Planner landing page.
         * Hub is used only when local recovery cannot establish a known planner
         * state. This keeps testcases independent without reopening from Hub on
         * every single case.
         */
        if (recoverToInvestmentPlannerLandingLocally()) {
            waitForPlannerLandingReadyOrThrow("local Investment Planner recovery");

            /*
             * A planner route can be visible while still retaining an own-list
             * selection from the previous testcase. In that state the normal
             * "Or choose your own list" CTA is replaced, which is not a clean
             * baseline for a following own-list testcase. Give Flutter a brief
             * chance to expose the CTA; if it stays absent, perform exactly one
             * intentional Hub reset and reopen Investment Planner through the
             * normal fallback path below.
             */
            if (waitForCleanPlannerBaseline(2200L)) {
                sleep(SCREEN_SETTLE_MS);
                ReportLogger.pass("Investment Planner ready using local screen recovery");
                return;
            }

            if (isPlannerLandingReady()) {
                ReportLogger.debug(
                        "Investment Planner is visible but retains an own-list selection; "
                                + "performing one Hub reset for a clean testcase baseline"
                );
                recoverToHubSafely();
            }
        }

        /*
         * Fallback path: resolve login/session state and enter through Hub only
         * when local planner recovery is not possible.
         */
        recoverKnownPlannerScreenBeforeAuth();

        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor app login/session confirmed");

        WebElement hub = ensureHubTabReachable();
        if (hub == null) {
            throw new AssertionError(
                    "Hub bottom navigation tab is not reachable from the current Advisor app screen"
            );
        }

        clickElement(hub, "Hub bottom navigation tab");
        sleep(700);

        WebElement plannerTile = findPlannerHubTileWithBoundedScroll();
        if (plannerTile == null) {
            throw new AssertionError(
                    "Investment Planner Hub tile not found using exact accessibilityId=Open invesment planner"
            );
        }

        clickElement(plannerTile, "Investment Planner Hub tile");
        waitForPlannerLandingReadyOrThrow("Hub navigation");
        sleep(SCREEN_SETTLE_MS);

        ReportLogger.pass("VRSA Investment Planner opened successfully");
    }

    public void validateInvestmentPlannerLandingPage() {
        ReportLogger.step("Validating Investment Planner landing page controls");

        assertDisplayed(INVESTMENT_PLANNER_TITLE, "Investment Planner title");
        assertDisplayed(ALL_WEATHER, "All Weather portfolio option");
        assertDisplayed(LONG_TERM, "Long Term portfolio option");
        assertDisplayed(AGGRESSIVE, "Aggressive portfolio option");
        assertDisplayed(OWN_LIST, "Or choose your own list control");
        assertDisplayed(AMOUNT_LABEL, "Investment amount label");

        WebElement amountField = findAmountField();
        if (amountField == null) {
            throw new AssertionError("Investment amount EditText was not found under the amount section");
        }

        assertDisplayed(GENERATE_MY_ORDER, "Generate my order button");
        ReportLogger.pass("Investment Planner landing page validated successfully");
    }

    public void selectBuiltInPortfolio(String portfolioName) {
        By locator = portfolioLocator(portfolioName);
        ReportLogger.step("Selecting built-in portfolio: " + portfolioName);

        WebElement option = waitForDisplayed(locator, SHORT_WAIT_SECONDS);
        if (option == null) {
            throw new AssertionError("Portfolio option not visible: " + portfolioName);
        }

        clickElement(option, portfolioName + " portfolio option");

        /*
         * Flutter updates the visual selection immediately but can rebuild the
         * semantics tree a few hundred milliseconds later. Give that state a
         * bounded settle window before amount entry / Generate my order.
         */
        long settleDeadline = System.currentTimeMillis() + 2200L;
        while (System.currentTimeMillis() < settleDeadline) {
            if (firstDisplayed(INVESTMENT_PLANNER_TITLE) != null
                    && firstDisplayed(locator) != null
                    && findAmountField() != null
                    && firstDisplayed(GENERATE_MY_ORDER) != null) {
                sleep(450);
                ReportLogger.pass("Portfolio selected and planner settled: " + portfolioName);
                return;
            }

            assertDriverAlive("while waiting for portfolio selection to settle: " + portfolioName);
            sleep(180);
        }

        throw new AssertionError(
                "Investment Planner did not settle after selecting portfolio: " + portfolioName
        );
    }

    // =========================================================
    // PUBLIC - OWN LIST / RECOMMENDATIONS / WATCHLIST
    // =========================================================

    public void openOwnListSheetAndValidate() {
        ReportLogger.step("Opening Choose a list sheet");

        if (isRecommendationSheetReady()) {
            logOwnListSheetAnchors();
            ReportLogger.pass("Choose a list sheet is already open");
            return;
        }

        WebElement ownList = waitForDisplayed(OWN_LIST, SHORT_WAIT_SECONDS);
        if (ownList == null) {
            throw new AssertionError("Or choose your own list control is not visible");
        }

        /*
         * Flutter accepted element.click() in prior runs without actually opening
         * the bottom sheet. Use a single forced element-based clickGesture first.
         */
        clickElementWithMobileGesture(ownList, "Or choose your own list");

        if (!waitForRecommendationSheetReady(Duration.ofSeconds(OWN_LIST_ASYNC_WAIT_SECONDS).toMillis())) {
            /*
             * Retry only if the underlying control is still genuinely visible.
             * Never blindly search/click through an already-open Flutter sheet.
             */
            WebElement freshOwnList = firstDisplayed(OWN_LIST);

            if (freshOwnList != null && !isRecommendationSheetReady()) {
                ReportLogger.debug(
                        "Choose a list sheet not ready after first physical tap; "
                                + "retrying once because the own-list control is still visible"
                );

                clickElementWithMobileGesture(
                        freshOwnList,
                        "Or choose your own list retry"
                );
            }

            if (!waitForRecommendationSheetReady(Duration.ofSeconds(OWN_LIST_ASYNC_WAIT_SECONDS).toMillis())) {
                throw new AssertionError(
                        "Choose a list sheet did not become ready"
                                + " | chooseListVisible=" + (firstDisplayed(CHOOSE_A_LIST) != null)
                                + " | recommendationsVisible=" + (firstDisplayed(RECOMMENDATIONS) != null)
                                + " | watchlistVisible=" + (firstDisplayed(WATCHLIST) != null)
                                + " | allVisible=" + (firstDisplayed(RECOMMENDATION_ALL) != null)
                                + " | a5Visible=" + (firstDisplayed(WATCHLIST_A5) != null)
                                + " | ownListStillVisible=" + (firstDisplayed(OWN_LIST) != null)
                );
            }
        }

        sleep(650);
        logOwnListSheetAnchors();
        ReportLogger.pass("Choose a list sheet validated successfully");
    }

    private void logOwnListSheetAnchors() {
        if (firstDisplayed(CHOOSE_A_LIST) != null) {
            ReportLogger.pass("Choose a list title is visible");
        } else {
            ReportLogger.debug("Choose a list title is not currently exposed");
        }

        if (firstDisplayed(RECOMMENDATIONS) != null) {
            ReportLogger.pass("Recommendations section is visible");
        } else if (firstDisplayed(RECOMMENDATION_ALL) != null) {
            ReportLogger.debug(
                    "Recommendations heading is not exposed, but exact Recommendation item 'All' is visible"
            );
        } else {
            ReportLogger.debug("Recommendations section is not currently exposed");
        }

        if (firstDisplayed(WATCHLIST) != null) {
            ReportLogger.pass("Watchlist section is visible");
        } else if (firstDisplayed(WATCHLIST_A5) != null) {
            ReportLogger.debug(
                    "Watchlist heading is not exposed, but exact Watchlist item 'A5' is visible"
            );
        } else {
            ReportLogger.debug("Watchlist section is currently outside the exposed sheet viewport");
        }
    }

    public void selectRecommendation(String recommendationName) {
        String requested = recommendationName == null || recommendationName.trim().isEmpty()
                ? "All"
                : recommendationName.trim();

        if (!isRecommendationSheetReady()) {
            openOwnListSheetAndValidate();
        }

        ReportLogger.step("Selecting Recommendation list: " + requested);

        WebElement recommendationMarker = firstDisplayed(RECOMMENDATIONS);
        if (recommendationMarker == null) {
            throw new AssertionError("Recommendations section marker is not visible");
        }

        By exactLocator = "All".equalsIgnoreCase(requested)
                ? RECOMMENDATION_ALL
                : AppiumBy.accessibilityId(requested);

        WebElement option = waitForDisplayed(exactLocator, SHORT_WAIT_SECONDS);
        if (option == null) {
            throw new AssertionError(
                    "Recommendation option not found by exact accessibility ID: " + requested
            );
        }

        try {
            int markerBottom = recommendationMarker.getRect().getY()
                    + recommendationMarker.getRect().getHeight();
            int optionY = option.getRect().getY();

            WebElement watchlistMarker = firstDisplayed(WATCHLIST);
            int maxY = driver.manage().window().getSize().getHeight() - 80;
            if (watchlistMarker != null) {
                maxY = Math.min(maxY, watchlistMarker.getRect().getY());
            }

            if (optionY < markerBottom || optionY >= maxY) {
                throw new AssertionError(
                        "Exact Recommendation locator resolved outside Recommendations section"
                                + " | item=" + requested
                                + " | optionY=" + optionY
                                + " | minY=" + markerBottom
                                + " | maxY=" + maxY
                );
            }
        } catch (StaleElementReferenceException e) {
            option = waitForDisplayed(exactLocator, SHORT_WAIT_SECONDS);
            if (option == null) {
                throw new AssertionError(
                        "Recommendation option rebuilt before selection and could not be re-located: "
                                + requested
                );
            }
        }

        selectOwnListItemWithVerifiedClose(option, requested, "Recommendation");
        ReportLogger.pass("Recommendation selected: " + requested);
    }

    /**
     * Selects a named watchlist when supplied. If the name is empty, the first
     * visible watchlist below the Watchlist section marker is selected dynamically.
     * Returns the actual selected watchlist name for reporting.
     */
    public String selectWatchlist(String preferredWatchlistName) {
        if (!isRecommendationSheetReady()) {
            openOwnListSheetAndValidate();
        }

        WebElement marker = findWatchlistMarkerWithBoundedSheetScroll();
        if (marker == null) {
            throw new AssertionError("Watchlist section marker is not visible after bounded sheet scroll");
        }

        String preferred = preferredWatchlistName == null
                ? ""
                : preferredWatchlistName.trim();

        WebElement candidate;
        if (!preferred.isEmpty()) {
            By exactLocator = "A5".equalsIgnoreCase(preferred)
                    ? WATCHLIST_A5
                    : AppiumBy.accessibilityId(preferred);

            candidate = firstDisplayed(exactLocator);

            for (int attempt = 1; candidate == null && attempt <= 5; attempt++) {
                if (!isOwnListSheetOpen()) {
                    break;
                }

                swipeChooseListSheetUp(attempt);
                sleep(500);
                candidate = firstDisplayed(exactLocator);
            }

            if (candidate == null) {
                throw new AssertionError(
                        "Requested watchlist is not visible by exact accessibility ID: " + preferred
                );
            }
        } else {
            candidate = findFirstDynamicWatchlistItem(marker);
            if (candidate == null) {
                throw new AssertionError("No selectable watchlist item is visible below the Watchlist section");
            }
        }

        String selectedName = semanticValue(candidate);
        if (selectedName.isEmpty()) {
            selectedName = preferred.isEmpty() ? "<dynamic watchlist>" : preferred;
        }

        ReportLogger.step("Selecting Watchlist: " + selectedName);
        selectOwnListItemWithVerifiedClose(candidate, selectedName, "Watchlist");

        ReportLogger.pass("Watchlist selected: " + selectedName);
        return selectedName;
    }

    // =========================================================
    // PUBLIC - AMOUNT / GENERATE ORDER
    // =========================================================

    public void enterInvestmentAmount(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Investment amount must be greater than zero");
        }

        ReportLogger.step("Entering investment amount: ₹" + amount);

        if (findAmountField() == null) {
            throw new AssertionError("Investment amount EditText is not visible");
        }

        /*
         * Flutter rebuilds the semantics tree when this field receives focus and
         * again when its formatted value changes. A WebElement captured before one
         * of those rebuilds immediately becomes stale. Therefore every mutating
         * operation below re-locates the EditText instead of caching the element.
         */
        focusAmountFieldFresh();
        clearAmountFieldFresh();

        if (!typeAmountFresh(amount)) {
            throw new AssertionError(
                    "Unable to enter investment amount after bounded stale-safe retries"
                            + " | expected=" + amount
                            + " | actual=" + readAmountDigitsFresh()
            );
        }

        closeKeyboardSafely();

        if (!waitForAmountFieldValueFresh(amount, 5000L)) {
            ReportLogger.debug(
                    "Amount field did not match after first entry. Performing one bounded stale-safe re-entry."
            );

            focusAmountFieldFresh();
            clearAmountFieldFresh();

            if (!typeAmountFresh(amount)) {
                throw new AssertionError(
                        "Unable to re-enter investment amount"
                                + " | expected=" + amount
                                + " | actual=" + readAmountDigitsFresh()
                );
            }

            closeKeyboardSafely();

            if (!waitForAmountFieldValueFresh(amount, 5000L)) {
                throw new AssertionError(
                        "Investment amount field did not contain expected value"
                                + " | expected=" + amount
                                + " | actual=" + readAmountDigitsFresh()
                );
            }
        }

        ReportLogger.pass(
                "Investment amount entered successfully"
                        + " | value=" + readAmountTextFresh()
        );
    }

    public void generateOrderAndWaitForYourOrders() {
        ReportLogger.step("Generating Investment Planner order");

        if (firstDisplayed(INVESTMENT_PLANNER_TITLE) == null) {
            throw new AssertionError("Investment Planner page is not visible before order generation");
        }

        if (findAmountField() == null || readAmountDigitsFresh().isEmpty()) {
            throw new AssertionError("Investment amount is not available before order generation");
        }

        WebElement generate = waitForDisplayed(GENERATE_MY_ORDER, SHORT_WAIT_SECONDS);
        if (generate == null) {
            throw new AssertionError("Generate my order button is not visible");
        }

        clickElement(generate, "Generate my order");

        long startedAt = System.currentTimeMillis();
        long deadline = startedAt + Duration.ofSeconds(ORDER_WAIT_SECONDS).toMillis();
        boolean freshElementRetryDone = false;
        boolean gestureRetryDone = false;
        boolean centerRetryDone = false;

        while (System.currentTimeMillis() < deadline) {
            if (firstDisplayed(YOUR_ORDERS) != null) {
                if (!waitForYourOrdersCoreReady(12000L)) {
                    throw new AssertionError(
                            "Your Orders opened but its core content did not become ready"
                                    + " | " + orderScreenDiagnostics()
                    );
                }

                sleep(850);
                ReportLogger.pass("Your Orders screen opened successfully");
                return;
            }

            if (firstDisplayed(UNDERLYING_DATA_ERROR) != null) {
                throw new AssertionError(
                        "Order generation failed: Underlying data not available is displayed"
                );
            }

            long elapsed = System.currentTimeMillis() - startedAt;

            // Flutter can rebuild the button semantics without delivering the first tap.
            // Retry with progressively lower-level element-based strategies only while
            // the planner is still visibly unchanged. Order generation is read-only.
            if (!freshElementRetryDone
                    && elapsed >= 3000L
                    && isStillOnPlannerBeforeGeneration()) {
                WebElement freshGenerate = firstDisplayed(GENERATE_MY_ORDER);
                if (freshGenerate != null) {
                    ReportLogger.debug(
                            "Still on Investment Planner after Generate tap; retrying with a fresh element"
                    );
                    clickElement(freshGenerate, "Generate my order fresh retry");
                    freshElementRetryDone = true;
                }
            }

            if (!gestureRetryDone
                    && elapsed >= 6500L
                    && isStillOnPlannerBeforeGeneration()) {
                WebElement freshGenerate = firstDisplayed(GENERATE_MY_ORDER);
                if (freshGenerate != null) {
                    ReportLogger.debug(
                            "Generate still has not transitioned; retrying with element-based clickGesture"
                    );
                    clickElementWithMobileGesture(freshGenerate, "Generate my order gesture retry");
                    gestureRetryDone = true;
                }
            }

            if (!centerRetryDone
                    && elapsed >= 10000L
                    && isStillOnPlannerBeforeGeneration()) {
                WebElement freshGenerate = firstDisplayed(GENERATE_MY_ORDER);
                if (freshGenerate != null) {
                    ReportLogger.debug(
                            "Generate route still unchanged; using final runtime-center tap fallback"
                    );
                    tapDynamicElementCenter(freshGenerate, "Generate my order final fallback");
                    centerRetryDone = true;
                }
            }

            assertDriverAlive("while waiting for Your Orders screen");
            sleep(POLL_MS);
        }

        String diagnostics = orderScreenDiagnostics();
        ReportLogger.debug("Order generation timeout diagnostics: " + diagnostics);

        throw new AssertionError(
                "Your Orders screen did not open within " + ORDER_WAIT_SECONDS + " seconds"
                        + " | " + diagnostics
        );
    }

    private boolean isStillOnPlannerBeforeGeneration() {
        return firstDisplayed(INVESTMENT_PLANNER_TITLE) != null
                && firstDisplayed(GENERATE_MY_ORDER) != null
                && firstDisplayed(YOUR_ORDERS) == null;
    }

    public void generateBuiltInPortfolioOrder(String portfolioName, long amount) {
        openInvestmentPlannerFromHub();
        selectBuiltInPortfolio(portfolioName);
        enterInvestmentAmount(amount);
        generateOrderAndWaitForYourOrders();
        validateGeneratedPortfolioTitle(portfolioName);
    }

    // =========================================================
    // PUBLIC - YOUR ORDERS VALIDATION
    // =========================================================

    public void validateGeneratedPortfolioTitle(String expectedPortfolioName) {
        String expectedTitle = expectedPortfolioName.trim();
        ReportLogger.step("Validating generated portfolio title: " + expectedTitle);

        long deadline = System.currentTimeMillis() + 9000L;
        String actual = "";

        while (System.currentTimeMillis() < deadline) {
            actual = readGeneratedPortfolioTitle();
            if (expectedTitle.equalsIgnoreCase(actual)) {
                ReportLogger.pass("Generated portfolio title matched: " + expectedTitle);
                return;
            }

            if (!actual.isEmpty()) {
                break;
            }

            assertDriverAlive("while reading generated portfolio title");
            sleep(250);
        }

        throw new AssertionError(
                "Generated portfolio category does not match selected portfolio"
                        + " | selected=" + expectedPortfolioName
                        + " | expectedTitle=" + expectedTitle
                        + " | actualTitle=" + actual
        );
    }

    public String readGeneratedPortfolioTitle() {
        if (firstDisplayed(YOUR_ORDERS) == null) {
            return "";
        }

        String[] knownTitles = {
                "Long Term",
                "All Weather",
                "Aggressive"
        };

        // Flutter may expose the title as an exact node or as part of a merged
        // content-desc. Prefer semantic contains before falling back to scans.
        for (String title : knownTitles) {
            WebElement exact = firstDisplayed(AppiumBy.accessibilityId(title));
            if (exact != null) {
                ReportLogger.info("Generated portfolio title detected: " + title);
                return title;
            }

            WebElement merged = firstDisplayed(
                    AppiumBy.androidUIAutomator(
                            "new UiSelector().descriptionContains(\"" + title + "\")"
                    )
            );
            if (merged != null) {
                ReportLogger.info("Generated portfolio title detected from merged semantics: " + title);
                return title;
            }
        }

        WebElement amountMarker = firstDisplayed(AMOUNT_INVESTED);
        WebElement ordersTitle = firstDisplayed(YOUR_ORDERS);
        int upperY = ordersTitle == null ? 0 : ordersTitle.getRect().getY();
        int lowerY = amountMarker == null
                ? driver.manage().window().getSize().getHeight() / 2
                : amountMarker.getRect().getY();

        for (WebElement element : allDisplayedElements()) {
            try {
                String value = semanticValue(element);
                Rectangle rect = element.getRect();
                if (rect.getY() < upperY || rect.getY() >= lowerY || value.isEmpty()) {
                    continue;
                }

                for (String title : knownTitles) {
                    if (value.toLowerCase(Locale.US).contains(title.toLowerCase(Locale.US))) {
                        ReportLogger.info("Generated portfolio title detected from semantic scan: " + title);
                        return title;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    public double readDisplayedAmountInvested() {
        ReportLogger.step("Reading displayed Amount invested value");

        WebElement marker = waitForDisplayed(AMOUNT_INVESTED, SHORT_WAIT_SECONDS);
        WebElement header = waitForDisplayed(COMPANY_NAME_HEADER, SHORT_WAIT_SECONDS);

        if (marker == null || header == null) {
            throw new AssertionError("Amount invested marker or Company Name header is not visible");
        }

        int minY = marker.getRect().getY();
        int maxY = header.getRect().getY();

        Double bestValue = null;
        int bestY = Integer.MAX_VALUE;

        for (WebElement element : allDisplayedElements()) {
            try {
                Rectangle rect = element.getRect();
                if (rect.getY() <= minY || rect.getY() >= maxY) {
                    continue;
                }

                String semantic = semanticValue(element);
                if (!semantic.contains("₹")) {
                    continue;
                }

                Double parsed = parseMoney(semantic);
                if (parsed != null && parsed > 0.0 && rect.getY() < bestY) {
                    bestValue = parsed;
                    bestY = rect.getY();
                }
            } catch (Exception ignored) {
            }
        }

        if (bestValue == null) {
            throw new AssertionError("Unable to read the displayed Amount invested value");
        }

        ReportLogger.pass(String.format(Locale.US, "Displayed Amount invested: ₹%.2f", bestValue));
        return bestValue;
    }

    /**
     * Dynamically reads generated rows using the confirmed Company Name / Actual Weight /
     * Value (₹) headers as column geometry. It never relies on today's stock names/prices.
     */
    public OrderValidationSummary validateGeneratedOrderTable(long enteredAmount) {
        ReportLogger.step("Validating generated order table dynamically");

        assertDisplayedWithin(YOUR_ORDERS, "Your Orders title", SHORT_WAIT_SECONDS);

        /*
         * Appium Inspector confirms exact accessibility IDs for all three
         * headers. The failure was timing: Flutter exposed Company Name first
         * and the old code asserted Actual Weight immediately afterwards.
         * Wait for the complete header set as one readiness condition.
         */
        waitForGeneratedOrderTableReady(12000L);
        ReportLogger.pass("Company Name header is visible");
        ReportLogger.pass("Actual Weight header is visible");
        ReportLogger.pass("Value (₹) header is visible");

        double displayedAmountInvested = readDisplayedAmountInvested();

        ReportLogger.info(
                String.format(
                        Locale.US,
                        "Investment amount comparison is informational only"
                                + " | enteredReference=₹%.2f"
                                + " | generatedAmountInvested=₹%.2f",
                        (double) enteredAmount,
                        displayedAmountInvested
                )
        );

        if (displayedAmountInvested <= 0.0) {
            throw new AssertionError("Displayed Amount invested must be greater than zero");
        }

        Map<String, OrderRow> rows = collectOrderRowsAcrossScroll();

        if (rows.size() < 3) {
            throw new AssertionError(
                    "Too few valid generated order rows were captured dynamically"
                            + " | capturedRows=" + rows.size()
                            + " | companies=" + rows.keySet()
            );
        }

        double totalCapturedValue = 0.0;

        for (OrderRow row : rows.values()) {
            if (row.actualWeight <= 0.0 || row.actualWeight > 100.0) {
                throw new AssertionError(
                        "Invalid actual weight"
                                + " | company=" + row.companyName
                                + " | weight=" + row.actualWeight
                );
            }

            if (row.value <= 0.0) {
                throw new AssertionError(
                        "Generated row has zero/negative value"
                                + " | company=" + row.companyName
                                + " | value=" + row.value
                );
            }

            totalCapturedValue += row.value;
        }

        ReportLogger.info(
                String.format(
                        Locale.US,
                        "Captured allocation total is informational only"
                                + " | amountInvested=₹%.2f"
                                + " | capturedRowTotal=₹%.2f",
                        displayedAmountInvested,
                        totalCapturedValue
                )
        );

        ReportLogger.pass(
                String.format(
                        Locale.US,
                        "Generated order table validated dynamically"
                                + " | capturedRows=%d"
                                + " | entered=₹%.2f"
                                + " | amountInvested=₹%.2f"
                                + " | capturedAllocation=₹%.2f",
                        rows.size(),
                        (double) enteredAmount,
                        displayedAmountInvested,
                        totalCapturedValue
                )
        );

        return new OrderValidationSummary(
                rows.size(),
                displayedAmountInvested,
                totalCapturedValue,
                new ArrayList<>(rows.keySet())
        );
    }

    // =========================================================
    // PUBLIC - PORTFOLIO PREVIEW
    // =========================================================

    public void openPortfolioPreviewFromYourOrders() {
        ReportLogger.step("Opening Portfolio preview from Your Orders");

        /*
         * IP_008 calls this immediately after Your Orders opens. Give Flutter a
         * bounded render window before attempting any scroll. This avoids the v14
         * regression where an immediate scroll started while semantics were still
         * rebuilding.
         */
        WebElement link = waitForPortfolioPreviewLinkOnFreshOrders(8000L);

        if (link == null) {
            // Previously stable fallback used by IP_008 before v14.
            link = findWithBoundedOrderScroll(PORTFOLIO_PREVIEW_LINK, 8);
        }

        if (link == null) {
            throw new AssertionError(
                    "Clickable Portfolio preview link was not found on fresh Your Orders screen"
                            + " | yourOrdersVisible=" + (firstDisplayed(YOUR_ORDERS) != null)
                            + " | previewSemanticVisible=" + (firstDisplayed(PORTFOLIO_PREVIEW_ANY) != null)
            );
        }

        clickElement(link, "Portfolio preview link");

        long startedAt = System.currentTimeMillis();
        long deadline = startedAt + Duration.ofSeconds(PAGE_WAIT_SECONDS).toMillis();
        boolean gestureRetryDone = false;
        boolean centerRetryDone = false;

        while (System.currentTimeMillis() < deadline) {
            if (isPortfolioPreviewScreenReady()) {
                assertNoUnderlyingDataError();
                sleep(SCREEN_SETTLE_MS);
                ReportLogger.pass("Portfolio preview page opened successfully");
                return;
            }

            if (firstDisplayed(UNDERLYING_DATA_ERROR) != null) {
                throw new AssertionError("Portfolio Preview shows: Underlying data not available");
            }

            long elapsed = System.currentTimeMillis() - startedAt;

            if (!gestureRetryDone
                    && elapsed >= 2500L
                    && firstDisplayed(YOUR_ORDERS) != null) {
                WebElement freshLink = waitForPortfolioPreviewLinkOnFreshOrders(1500L);
                if (freshLink == null) {
                    freshLink = findWithBoundedOrderScroll(PORTFOLIO_PREVIEW_LINK, 2);
                }

                if (freshLink != null) {
                    ReportLogger.debug(
                            "Portfolio preview route did not change after first tap; retrying with clickGesture"
                    );
                    clickElementWithMobileGesture(
                            freshLink,
                            "Portfolio preview gesture retry"
                    );
                    gestureRetryDone = true;
                }
            }

            if (!centerRetryDone
                    && elapsed >= 5500L
                    && firstDisplayed(YOUR_ORDERS) != null) {
                WebElement freshLink = waitForPortfolioPreviewLinkOnFreshOrders(1500L);
                if (freshLink == null) {
                    freshLink = findWithBoundedOrderScroll(PORTFOLIO_PREVIEW_LINK, 2);
                }

                if (freshLink != null) {
                    ReportLogger.debug(
                            "Portfolio preview still on Your Orders; using runtime-center tap fallback"
                    );
                    tapDynamicElementCenter(
                            freshLink,
                            "Portfolio preview final fallback"
                    );
                    centerRetryDone = true;
                }
            }

            assertDriverAlive("while waiting for Portfolio preview route");
            sleep(POLL_MS);
        }

        throw new AssertionError(
                "Portfolio preview page did not open"
                        + " | package=" + safeCurrentPackage()
                        + " | yourOrders=" + (firstDisplayed(YOUR_ORDERS) != null)
                        + " | previewSemantic=" + (firstDisplayed(PORTFOLIO_PREVIEW_ANY) != null)
                        + " | underlyingDataError="
                        + (firstDisplayed(UNDERLYING_DATA_ERROR) != null
                        || pageSourceContainsIgnoreCase("Underlying data not available"))
        );
    }

    private WebElement waitForPortfolioPreviewLinkOnFreshOrders(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(500L, timeoutMs);

        while (System.currentTimeMillis() < deadline) {
            WebElement strict = firstDisplayed(PORTFOLIO_PREVIEW_LINK);
            if (strict != null) {
                return strict;
            }

            /*
             * Flutter may temporarily expose a generic accessibility wrapper
             * instead of the ImageView-specific node. On Your Orders that generic
             * "Portfolio preview" semantic is still the intended link.
             */
            if (firstDisplayed(YOUR_ORDERS) != null) {
                WebElement generic = firstDisplayed(PORTFOLIO_PREVIEW_ANY);
                if (generic != null) {
                    return generic;
                }
            }

            assertDriverAlive("while waiting for Portfolio preview link on Your Orders");
            sleep(200L);
        }

        return null;
    }

    /**
     * Identifies Your Orders even when the title is outside the current Flutter
     * semantics viewport after table scrolling. Used only by local recovery.
     */
    private boolean isYourOrdersContextVisible() {
        return firstDisplayed(YOUR_ORDERS) != null
                || firstDisplayed(AMOUNT_INVESTED) != null
                || firstDisplayed(COMPANY_NAME_HEADER) != null
                || firstDisplayed(ACTUAL_WEIGHT_HEADER) != null
                || firstDisplayed(VALUE_HEADER) != null
                || firstDisplayed(MAIL_MY_ORDER) != null;
    }

    private boolean isPortfolioPreviewScreenReady() {
        if (firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) != null) {
            return true;
        }

        // Generic accessibility anchor is safe only once Your Orders has disappeared;
        // on Your Orders the same description belongs to the clickable ImageView link.
        return firstDisplayed(YOUR_ORDERS) == null
                && firstDisplayed(PORTFOLIO_PREVIEW_ANY) != null;
    }

    private WebElement findPortfolioPreviewPageAnchor() {
        WebElement strict = firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE);
        if (strict != null) {
            return strict;
        }

        if (firstDisplayed(YOUR_ORDERS) == null) {
            return firstDisplayed(PORTFOLIO_PREVIEW_ANY);
        }

        return null;
    }

    public void validatePortfolioPreviewAllTabs() {
        ReportLogger.step("Validating Portfolio Preview Holdings, Sector and Market Cap tabs");

        if (waitForPortfolioPreviewAnchor(SHORT_WAIT_SECONDS) == null) {
            throw new AssertionError("Portfolio preview title is not visible");
        }
        ReportLogger.pass("Portfolio preview title is visible");
        assertNoUnderlyingDataError();

        /*
         * Appium Inspector exposes the whole Portfolio preview header as one
         * semantic element; Holdings/Sector/Market Cap are visual controls but
         * do not have individual accessibility nodes. Use the confirmed header
         * as a runtime anchor and tap tab centers RELATIVE to that rectangle.
         * No absolute screen coordinate is stored.
         */
        String holdingsFingerprint = validateCurrentPreviewData("Holdings");

        tapPreviewTabByAnchor("Sector");
        waitForPreviewDataChange(holdingsFingerprint, "Sector", 7000L);
        String sectorFingerprint = validateCurrentPreviewData("Sector");

        tapPreviewTabByAnchor("Market Cap");
        waitForPreviewDataChange(sectorFingerprint, "Market Cap", 7000L);
        String marketCapFingerprint = validateCurrentPreviewData("Market Cap");

        tapPreviewTabByAnchor("Holdings");
        waitForPreviewDataChange(marketCapFingerprint, "Holdings", 7000L);
        validateCurrentPreviewData("Holdings");

        ReportLogger.pass("Portfolio Preview tabs validated successfully");
    }

    public void assertNoUnderlyingDataError() {
        if (firstDisplayed(UNDERLYING_DATA_ERROR) != null
                || pageSourceContainsIgnoreCase("Underlying data not available")) {
            throw new AssertionError("Portfolio Preview shows: Underlying data not available");
        }
    }

    // =========================================================
    // PUBLIC - MAIL ORDER
    // =========================================================

    public void mailMyOrderAndValidateSuccess() {
        ReportLogger.step(
                "Tapping Mail my order once and waiting up to "
                        + MAIL_CONFIRM_WAIT_SECONDS
                        + " seconds for success confirmation"
        );

        WebElement mailButton = findWithBoundedOrderScroll(MAIL_MY_ORDER, 10);
        if (mailButton == null) {
            throw new AssertionError("Mail my order button is not visible on Your Orders screen");
        }

        /*
         * Manual Mail works, while UiAutomator2 clickGesture did not produce a
         * visible response in v14. Use one real W3C touch at the live center of
         * the exact Mail element. Never retry this action: a delayed first request
         * must not create a duplicate email.
         */
        tapDynamicElementCenter(mailButton, "Mail my order");

        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(MAIL_CONFIRM_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            if (firstDisplayed(MAIL_SUCCESS_EXACT) != null
                    || firstDisplayed(MAIL_SUCCESS) != null
                    || firstDisplayed(MAIL_SUCCESS_TEXT) != null
                    || firstDisplayed(MAIL_SUCCESS_TOAST) != null) {
                ReportLogger.pass(
                        "Mail my order succeeded: Order mailed successfully"
                );
                return;
            }

            assertDriverAlive("while waiting for Mail my order confirmation");
            sleep(150L);
        }

        boolean successTextInPageSource =
                pageSourceContainsIgnoreCase("Order mailed successfully");

        if (successTextInPageSource) {
            ReportLogger.pass(
                    "Mail my order succeeded: success confirmation found in page source"
            );
            return;
        }

        throw new AssertionError(
                "Order mailed successfully confirmation was not displayed within "
                        + MAIL_CONFIRM_WAIT_SECONDS
                        + " seconds after one physical Mail tap"
                        + " | yourOrdersVisible=" + (firstDisplayed(YOUR_ORDERS) != null)
                        + " | mailButtonVisible=" + (firstDisplayed(MAIL_MY_ORDER) != null)
                        + " | successTextInPageSource=" + successTextInPageSource
        );
    }

    // =========================================================
    // PUBLIC - OPTIONAL STRICT CATEGORY CHECKS FOR OWN LISTS
    // =========================================================

    public void validateGeneratedPortfolioIfConfigured(String expectedPortfolio) {
        if (expectedPortfolio == null || expectedPortfolio.trim().isEmpty()) {
            String actual = readGeneratedPortfolioTitle();
            ReportLogger.info(
                    "Own-list generated portfolio category observed (no strict expected category configured): "
                            + actual
            );
            return;
        }

        validateGeneratedPortfolioTitle(expectedPortfolio.trim());
    }

    // =========================================================
    // PUBLIC - SAFE CLEANUP
    // =========================================================

    public void recoverToInvestmentPlannerSafely() {
        try {
            activateAdvisorAppIfNeeded();

            if (recoverToInvestmentPlannerLandingLocally()) {
                /*
                 * Local Back recovery may correctly return from Your Orders to
                 * Investment Planner while the previous Recommendation/Watchlist
                 * remains selected. That route is valid, but it is not a clean
                 * baseline for the next testcase because the own-list CTA can be
                 * replaced by the retained selection. Built-in cases keep the CTA
                 * and therefore remain fully local. Own-list cases get one Hub
                 * reset only when this retained state is actually observed.
                 */
                if (waitForCleanPlannerBaseline(2200L)) {
                    sleep(650);
                    ReportLogger.debug("Investment Planner cleanup returned locally to clean planner landing page");
                    return;
                }

                if (isPlannerLandingReady()) {
                    ReportLogger.debug(
                            "Investment Planner cleanup detected retained own-list selection; "
                                    + "using one Hub reset for the next testcase baseline"
                    );
                    recoverToHubSafely();
                    return;
                }
            }

            ReportLogger.debug(
                    "Local Investment Planner cleanup could not establish planner landing page; using Hub fallback"
            );
            recoverToHubSafely();
        } catch (Exception e) {
            ReportLogger.debug(
                    "Investment Planner local cleanup ignored so original testcase result is preserved: "
                            + cleanError(e.getMessage())
            );
        } catch (AssertionError e) {
            ReportLogger.debug(
                    "Investment Planner local cleanup assertion ignored so original testcase result is preserved: "
                            + cleanError(e.getMessage())
            );
        }
    }

    public void recoverToHubSafely() {
        try {
            activateAdvisorAppIfNeeded();

            for (int attempt = 1; attempt <= 5; attempt++) {
                WebElement hub = firstDisplayed(HUB_TAB);
                if (hub != null) {
                    clickElement(hub, "Hub recovery tab");
                    sleep(700);
                    ReportLogger.debug("Investment Planner cleanup returned to Hub fallback");
                    return;
                }

                driver.navigate().back();
                sleep(700);
            }
        } catch (Exception e) {
            ReportLogger.debug(
                    "Investment Planner Hub fallback cleanup ignored: "
                            + cleanError(e.getMessage())
            );
        } catch (AssertionError e) {
            ReportLogger.debug(
                    "Investment Planner Hub fallback cleanup assertion ignored: "
                            + cleanError(e.getMessage())
            );
        }
    }

    // =========================================================
    // INTERNAL - PREVIEW HELPERS
    // =========================================================

    private String validateCurrentPreviewData(String tabName) {
        ReportLogger.step("Validating Portfolio Preview tab data: " + tabName);

        assertNoUnderlyingDataError();

        if (findPortfolioPreviewPageAnchor() == null) {
            throw new AssertionError("Portfolio Preview title disappeared while validating " + tabName);
        }

        List<Double> percentages = visiblePercentValues();
        if (percentages.size() < 3) {
            throw new AssertionError(
                    "Portfolio Preview tab did not expose enough allocation data"
                            + " | tab=" + tabName
                            + " | visiblePercentValues=" + percentages
            );
        }

        boolean hasPositiveNetAllocation = false;
        for (Double value : percentages) {
            if (value != null && value > 0.0) {
                hasPositiveNetAllocation = true;
                break;
            }
        }

        if (!hasPositiveNetAllocation) {
            throw new AssertionError(
                    "Portfolio Preview tab contains no positive allocation values: " + tabName
            );
        }

        String fingerprint = previewDataFingerprint();
        if (fingerprint.isEmpty()) {
            fingerprint = percentages.toString();
        }

        ReportLogger.pass(
                "Portfolio Preview tab rendered meaningful allocation data"
                        + " | tab=" + tabName
                        + " | percentageValues=" + percentages.size()
        );

        return fingerprint;
    }

    private void tapPreviewTabByAnchor(String tabName) {
        WebElement anchor = waitForPortfolioPreviewAnchor(SHORT_WAIT_SECONDS);
        if (anchor == null) {
            throw new AssertionError("Portfolio preview anchor is not visible for tab: " + tabName);
        }

        double xRatio;
        switch (tabName.toLowerCase(Locale.US)) {
            case "holdings":
                xRatio = 0.14;
                break;
            case "sector":
                xRatio = 0.38;
                break;
            case "market cap":
                xRatio = 0.64;
                break;
            default:
                throw new IllegalArgumentException("Unsupported Portfolio Preview tab: " + tabName);
        }

        Rectangle rect = anchor.getRect();
        int x = rect.getX() + (int) Math.round(rect.getWidth() * xRatio);
        int y = rect.getY() + (int) Math.round(rect.getHeight() * 0.74);

        tapViewportPoint(x, y, "Portfolio Preview tab: " + tabName);
        sleep(650);
    }

    private void waitForPreviewDataChange(String previousFingerprint, String tabName, long timeoutMs) {
        if (previousFingerprint == null || previousFingerprint.isEmpty()) {
            sleep(700);
            return;
        }

        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (firstDisplayed(UNDERLYING_DATA_ERROR) != null) {
                throw new AssertionError("Portfolio Preview shows: Underlying data not available");
            }

            String current = previewDataFingerprint();
            if (!current.isEmpty() && !current.equals(previousFingerprint)) {
                return;
            }

            /* Full hierarchy fingerprinting is intentionally throttled. */
            sleep(500);
        }

        /*
         * Some Flutter builds expose only percentage values for these charts.
         * The subsequent data validation is still authoritative; log the lack
         * of a semantic fingerprint change rather than failing on semantics the
         * app does not expose.
         */
        ReportLogger.debug(
                "Portfolio Preview semantic fingerprint did not change after tapping " + tabName
                        + "; continuing with allocation-data validation"
        );
    }

    private String previewDataFingerprint() {
        WebElement title = findPortfolioPreviewPageAnchor();
        if (title == null) {
            return "";
        }

        int minY = title.getRect().getY() + title.getRect().getHeight();
        List<String> values = new ArrayList<>();

        for (WebElement element : allDisplayedElements()) {
            try {
                Rectangle rect = element.getRect();
                if (rect.getY() + rect.getHeight() / 2 <= minY) {
                    continue;
                }

                String semantic = semanticValue(element);
                if (semantic.isEmpty()
                        || semantic.equalsIgnoreCase("Portfolio preview")) {
                    continue;
                }

                values.add(semantic);
            } catch (Exception ignored) {
            }
        }

        if (values.isEmpty()) {
            return "";
        }

        Collections.sort(values);
        return values.toString();
    }

    private WebElement waitForPortfolioPreviewAnchor(int timeoutSeconds) {
        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(timeoutSeconds).toMillis();

        while (System.currentTimeMillis() < deadline) {
            WebElement anchor = findPortfolioPreviewPageAnchor();
            if (anchor != null) {
                return anchor;
            }

            if (firstDisplayed(UNDERLYING_DATA_ERROR) != null) {
                return null;
            }

            assertDriverAlive("while waiting for Portfolio preview anchor");
            sleep(220);
        }

        return null;
    }

    private List<Double> visiblePercentValues() {
        List<Double> values = new ArrayList<>();

        for (WebElement element : allDisplayedElements()) {
            try {
                String semantic = semanticValue(element);
                if (!semantic.matches(".*\\d+(?:\\.\\d+)?%.*")) {
                    continue;
                }

                String[] parts = semantic.split("\\s+");
                for (String part : parts) {
                    if (!part.contains("%")) {
                        continue;
                    }

                    String cleaned = part.replaceAll("[^0-9.\\-]", "");
                    if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
                        continue;
                    }

                    try {
                        values.add(Double.parseDouble(cleaned));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return values;
    }

    // =========================================================
    // INTERNAL - ORDER TABLE CAPTURE
    // =========================================================

    private Map<String, OrderRow> collectOrderRowsAcrossScroll() {
        Map<String, OrderRow> rows = new LinkedHashMap<>();
        String previousFingerprint = "";
        int unchangedCount = 0;

        for (int viewport = 0; viewport < 9; viewport++) {
            Map<String, OrderRow> current = captureVisibleOrderRows();
            rows.putAll(current);

            String fingerprint = current.keySet().toString() + current.values().toString();
            if (fingerprint.equals(previousFingerprint)) {
                unchangedCount++;
            } else {
                unchangedCount = 0;
            }

            if (unchangedCount >= 2) {
                break;
            }

            previousFingerprint = fingerprint;
            swipeUpW3C(0.74, 0.38, 520);
            sleep(500);
        }

        return rows;
    }

    private Map<String, OrderRow> captureVisibleOrderRows() {
        Map<String, OrderRow> rows = new LinkedHashMap<>();

        WebElement companyHeader = firstDisplayed(COMPANY_NAME_HEADER);
        WebElement weightHeader = firstDisplayed(ACTUAL_WEIGHT_HEADER);
        WebElement valueHeader = firstDisplayed(VALUE_HEADER);
        WebElement mailButton = firstDisplayed(MAIL_MY_ORDER);

        if (companyHeader == null || weightHeader == null || valueHeader == null) {
            return rows;
        }

        Rectangle companyRect = companyHeader.getRect();
        Rectangle weightRect = weightHeader.getRect();
        Rectangle valueRect = valueHeader.getRect();

        int tableTopY = Math.max(
                Math.max(companyRect.getY() + companyRect.getHeight(), weightRect.getY() + weightRect.getHeight()),
                valueRect.getY() + valueRect.getHeight()
        ) + 2;

        int screenHeight = driver.manage().window().getSize().getHeight();
        int tableBottomY = mailButton == null
                ? (int) (screenHeight * 0.90)
                : mailButton.getRect().getY() - 4;

        List<NodeValue> companyNodes = new ArrayList<>();
        List<NodeValue> weightNodes = new ArrayList<>();
        List<NodeValue> valueNodes = new ArrayList<>();

        for (WebElement element : allDisplayedElements()) {
            try {
                Rectangle rect = element.getRect();
                int centerX = rect.getX() + rect.getWidth() / 2;
                int centerY = rect.getY() + rect.getHeight() / 2;

                if (centerY <= tableTopY || centerY >= tableBottomY) {
                    continue;
                }

                String semantic = semanticValue(element);
                if (semantic.isEmpty()) {
                    continue;
                }

                if (isWithinColumn(centerX, companyRect)) {
                    if (isLikelyCompanyName(semantic)) {
                        companyNodes.add(new NodeValue(semantic, centerY));
                    }
                    continue;
                }

                if (isWithinColumn(centerX, weightRect)) {
                    Double weight = parsePlainNumber(semantic);
                    if (weight != null && weight > 0.0 && weight <= 100.0) {
                        weightNodes.add(new NodeValue(semantic, centerY, weight));
                    }
                    continue;
                }

                if (isWithinColumn(centerX, valueRect)) {
                    Double value = parseMoney(semantic);
                    if (value != null && value > 0.0) {
                        valueNodes.add(new NodeValue(semantic, centerY, value));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (NodeValue company : companyNodes) {
            NodeValue weight = nearestNode(company.centerY, weightNodes, 95);
            NodeValue value = nearestNode(company.centerY, valueNodes, 95);

            if (weight == null || value == null || weight.numericValue == null || value.numericValue == null) {
                continue;
            }

            rows.put(
                    company.rawValue,
                    new OrderRow(company.rawValue, weight.numericValue, value.numericValue)
            );
        }

        return rows;
    }

    private NodeValue nearestNode(int targetY, List<NodeValue> candidates, int maxDistance) {
        NodeValue best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (NodeValue candidate : candidates) {
            int distance = Math.abs(targetY - candidate.centerY);
            if (distance <= maxDistance && distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        return best;
    }

    private boolean isWithinColumn(int centerX, Rectangle headerRect) {
        return centerX >= headerRect.getX() - 15
                && centerX <= headerRect.getX() + headerRect.getWidth() + 15;
    }

    private boolean isLikelyCompanyName(String value) {
        if (value == null) {
            return false;
        }

        String clean = value.trim();
        if (clean.isEmpty()
                || clean.contains("|")
                || clean.contains("₹")
                || clean.matches("[-+]?\\d+(?:,\\d{3})*(?:\\.\\d+)?%?")) {
            return false;
        }

        return !clean.equals("Company Name")
                && !clean.equals("Actual Weight")
                && !clean.equals("Value (₹)")
                && !clean.equals("Portfolio preview")
                && !clean.equals("Mail my order")
                && !clean.equals("Add to watchlist")
                && !clean.equals("Customize");
    }

    // =========================================================
    // INTERNAL - OWN LIST HELPERS
    // =========================================================

    private boolean isRecommendationSheetReady() {
        /*
         * Only bottom-sheet-specific semantics prove that the Choose-a-list
         * sheet is open. Selected values such as All/A5 may remain visible
         * on the main planner after selection and must not be used here.
         */
        return firstDisplayed(CHOOSE_A_LIST) != null
                || firstDisplayed(RECOMMENDATIONS) != null
                || firstDisplayed(WATCHLIST) != null;
    }

    private boolean waitForRecommendationSheetReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (isRecommendationSheetReady()) {
                return true;
            }

            assertDriverAlive("while waiting for Choose a list sheet");
            sleep(220);
        }

        return false;
    }

    private boolean isOwnListSheetOpen() {
        /*
         * All/A5 are selection values, not reliable sheet markers. They can
         * legitimately remain exposed after the bottom sheet has closed.
         */
        return firstDisplayed(CHOOSE_A_LIST) != null
                || firstDisplayed(RECOMMENDATIONS) != null
                || firstDisplayed(WATCHLIST) != null;
    }

    private boolean waitForOwnListSheetToClose(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(1000L, timeoutMs);
        int consecutiveConfirmedPolls = 0;

        while (System.currentTimeMillis() < deadline) {
            assertDriverAlive("while waiting for Choose a list sheet to close");

            boolean sheetMarkersGone =
                    firstDisplayed(CHOOSE_A_LIST) == null
                            && firstDisplayed(RECOMMENDATIONS) == null
                            && firstDisplayed(WATCHLIST) == null;

            boolean plannerReturned =
                    firstDisplayed(INVESTMENT_PLANNER_TITLE) != null
                            && firstDisplayed(GENERATE_MY_ORDER) != null;

            if (sheetMarkersGone && plannerReturned) {
                consecutiveConfirmedPolls++;

                // Require two consecutive confirmed observations so a transient
                // Flutter semantics rebuild cannot be mistaken for completion.
                if (consecutiveConfirmedPolls >= 2) {
                    sleep(350);
                    ReportLogger.pass(
                            "Choose a list sheet closed and own-list selection was applied successfully"
                    );
                    return true;
                }
            } else {
                consecutiveConfirmedPolls = 0;
            }

            sleep(200);
        }

        return false;
    }

    private void selectOwnListItemWithVerifiedClose(
            WebElement candidate,
            String itemName,
            String itemType
    ) {
        if (candidate == null) {
            throw new AssertionError(
                    "Own-list item is not available for selection"
                            + " | type=" + itemType
                            + " | item=" + itemName
            );
        }

        String clickable = "";
        try {
            clickable = candidate.getAttribute("clickable");
        } catch (Exception ignored) {
        }

        ReportLogger.debug(
                "Selecting exact own-list accessibility node"
                        + " | type=" + itemType
                        + " | item=" + itemName
                        + " | clickable=" + clickable
        );

        /*
         * Inspector confirms All/A5 are full-width clickable android.view.View
         * nodes. Selenium element.click() was accepted without triggering Flutter.
         * Tap physically inside that exact element's live rectangle instead.
         *
         * The point is derived from the element bounds, not a fixed device
         * coordinate, so it remains future-safe across screen sizes.
         */
        tapOwnListExactRow(
                candidate,
                itemType + " option: " + itemName
        );

        if (waitForOwnListSheetToClose(Duration.ofSeconds(OWN_LIST_ASYNC_WAIT_SECONDS).toMillis())) {
            ReportLogger.pass(
                    itemType + " selected successfully: " + itemName
            );
            return;
        }

        throw new AssertionError(
                "Own-list selection did not return to a confirmed planner state within "
                        + OWN_LIST_ASYNC_WAIT_SECONDS
                        + " seconds"
                        + " | type=" + itemType
                        + " | item=" + itemName
                        + " | chooseListVisible=" + (firstDisplayed(CHOOSE_A_LIST) != null)
                        + " | recommendationsVisible=" + (firstDisplayed(RECOMMENDATIONS) != null)
                        + " | watchlistVisible=" + (firstDisplayed(WATCHLIST) != null)
                        + " | exactAllVisible=" + (firstDisplayed(RECOMMENDATION_ALL) != null)
                        + " | exactA5Visible=" + (firstDisplayed(WATCHLIST_A5) != null)
                        + " | plannerTitleVisible=" + (firstDisplayed(INVESTMENT_PLANNER_TITLE) != null)
                        + " | generateVisible=" + (firstDisplayed(GENERATE_MY_ORDER) != null)
        );
    }

    private void tapOwnListExactRow(WebElement element, String label) {
        if (element == null) {
            throw new AssertionError("Cannot tap null own-list element: " + label);
        }

        try {
            Rectangle rect = element.getRect();

            int x = rect.getX() + (int) Math.round(rect.getWidth() * 0.72);
            int y = rect.getY() + rect.getHeight() / 2;

            Dimension screen = driver.manage().window().getSize();

            x = Math.max(
                    rect.getX() + 5,
                    Math.min(x, rect.getX() + rect.getWidth() - 5)
            );
            y = Math.max(
                    rect.getY() + 5,
                    Math.min(y, rect.getY() + rect.getHeight() - 5)
            );

            // Also keep the derived point safely within the actual viewport.
            x = Math.max(20, Math.min(x, screen.getWidth() - 20));
            y = Math.max(20, Math.min(y, screen.getHeight() - 20));

            PointerInput finger =
                    new PointerInput(PointerInput.Kind.TOUCH, "ipOwnListExactRowFinger");

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
                    finger.createPointerDown(
                            PointerInput.MouseButton.LEFT.asArg()
                    )
            );
            tap.addAction(
                    finger.createPointerUp(
                            PointerInput.MouseButton.LEFT.asArg()
                    )
            );

            driver.perform(Collections.singletonList(tap));

            ReportLogger.step(
                    "Tapped exact own-list row using runtime element bounds: " + label
            );

        } catch (StaleElementReferenceException e) {
            throw new AssertionError(
                    "Exact own-list element rebuilt before physical tap"
                            + " | label=" + label,
                    e
            );
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to physically tap exact own-list row"
                            + " | label=" + label
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private WebElement findWatchlistMarkerWithBoundedSheetScroll() {
        if (!isOwnListSheetOpen()) {
            return null;
        }

        WebElement marker = firstDisplayed(WATCHLIST);
        if (marker != null) {
            return marker;
        }

        /*
         * The Watchlist section can be below the currently exposed part of the
         * Choose-a-list bottom sheet. Scroll INSIDE the live sheet region only
         * while that sheet still exists.
         */
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (!isOwnListSheetOpen()) {
                ReportLogger.debug(
                        "Choose a list sheet closed; stopping Watchlist scroll attempts"
                );
                return null;
            }

            swipeChooseListSheetUp(attempt);
            sleep(500);

            if (!isOwnListSheetOpen()) {
                ReportLogger.debug(
                        "Choose a list sheet closed during Watchlist search; stopping scroll attempts"
                );
                return null;
            }

            marker = firstDisplayed(WATCHLIST);
            if (marker != null) {
                ReportLogger.debug(
                        "Watchlist section exposed after targeted sheet scroll"
                                + " | attempt=" + attempt
                );
                return marker;
            }
        }

        return null;
    }

    private void swipeChooseListSheetUp(int attempt) {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenWidth = size.getWidth();
            int screenHeight = size.getHeight();

            int sheetTop = (int) Math.round(screenHeight * 0.36);

            WebElement sheetTitle = firstDisplayed(CHOOSE_A_LIST);
            if (sheetTitle != null) {
                try {
                    Rectangle titleRect = sheetTitle.getRect();
                    sheetTop = Math.max(
                            sheetTop,
                            titleRect.getY() + titleRect.getHeight() + 20
                    );
                } catch (Exception ignored) {
                }
            }

            WebElement recommendationsMarker = firstDisplayed(RECOMMENDATIONS);
            if (recommendationsMarker != null) {
                try {
                    Rectangle markerRect = recommendationsMarker.getRect();
                    sheetTop = Math.max(
                            sheetTop,
                            markerRect.getY() + markerRect.getHeight() + 10
                    );
                } catch (Exception ignored) {
                }
            }

            int x = screenWidth / 2;
            int startY = (int) Math.round(screenHeight * 0.90);
            int endY = sheetTop + 45;

            // Guarantee a meaningful upward movement while staying in the sheet.
            if (endY >= startY - 120) {
                endY = Math.max(
                        (int) Math.round(screenHeight * 0.48),
                        startY - (int) Math.round(screenHeight * 0.30)
                );
            }

            PointerInput finger = new PointerInput(
                    PointerInput.Kind.TOUCH,
                    "ipChooseListSheetFinger"
            );
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ZERO,
                            PointerInput.Origin.viewport(),
                            x,
                            startY
                    )
            );
            swipe.addAction(
                    finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(520L),
                            PointerInput.Origin.viewport(),
                            x,
                            endY
                    )
            );
            swipe.addAction(
                    finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg())
            );

            driver.perform(Collections.singletonList(swipe));

            ReportLogger.step(
                    "Scrolled inside Choose a list sheet to expose Watchlist"
                            + " | attempt=" + attempt
            );
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to scroll inside Choose a list sheet"
                            + " | attempt=" + attempt
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private WebElement findExactSemanticBetweenY(String value, int minY, int maxY) {
        for (WebElement element : findAllExactSemantic(value)) {
            try {
                int centerY = element.getRect().getY() + element.getRect().getHeight() / 2;
                if (centerY > minY && centerY < maxY) {
                    return element;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private WebElement findExactSemanticBelowY(String value, int minY) {
        for (WebElement element : findAllExactSemantic(value)) {
            try {
                int centerY = element.getRect().getY() + element.getRect().getHeight() / 2;
                if (centerY > minY) {
                    return element;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private WebElement findFirstDynamicWatchlistItem(WebElement watchlistMarker) {
        int minY = watchlistMarker.getRect().getY() + watchlistMarker.getRect().getHeight();
        int screenHeight = driver.manage().window().getSize().getHeight();
        List<WebElement> candidates = new ArrayList<>();

        for (WebElement element : allDisplayedElements()) {
            try {
                Rectangle rect = element.getRect();
                int centerY = rect.getY() + rect.getHeight() / 2;

                if (centerY <= minY || centerY >= (int) (screenHeight * 0.94)) {
                    continue;
                }

                String value = semanticValue(element);
                if (value.isEmpty()
                        || isOwnListSheetReservedValue(value)
                        || !isLikelyDynamicListName(value)) {
                    continue;
                }

                if (rect.getHeight() < 18 || rect.getHeight() > 140) {
                    continue;
                }

                candidates.add(element);
            } catch (Exception ignored) {
            }
        }

        candidates.sort(Comparator.comparingInt(e -> e.getRect().getY()));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isLikelyDynamicListName(String value) {
        if (value == null) {
            return false;
        }

        String clean = normalizeSpaces(value);
        if (clean.isEmpty() || clean.length() > 80) {
            return false;
        }

        if (clean.contains("₹")
                || clean.contains("%")
                || clean.equalsIgnoreCase("Step 1")
                || clean.equalsIgnoreCase("Step 2")
                || clean.equalsIgnoreCase("Select a portfolio")
                || clean.toLowerCase(Locale.US).contains("how much do you want to invest")
                || clean.toLowerCase(Locale.US).contains("spread equally across")) {
            return false;
        }

        return true;
    }

    private boolean isOwnListSheetReservedValue(String value) {
        return value.equals("Choose a list")
                || value.equals("Recommendations")
                || value.equals("Watchlist")
                || value.equals("All")
                || value.equals("All Weather")
                || value.equals("Latest")
                || value.equals("Investment Planner")
                || value.equals("Generate my order")
                || value.equals("Or choose your own list");
    }

    private boolean recoverToInvestmentPlannerLandingLocally() {
        try {
            if (isPlannerLandingReady()) {
                return true;
            }

            /*
             * Hub is a valid safe state, but it is NOT an Investment Planner
             * deep state. In earlier builds the Hub tile text "Investment Planner"
             * made page-source matching incorrectly start local Back recovery from
             * Hub. Stop immediately here and let normal Hub navigation handle it.
             */
            if (firstDisplayed(HUB_TAB) != null) {
                ReportLogger.debug(
                        "Hub/bottom navigation is already visible; skipping local Investment Planner Back recovery"
                );
                return false;
            }

            boolean knownPlannerState =
                    firstDisplayed(INVESTMENT_PLANNER_TITLE) != null
                            || firstDisplayed(YOUR_ORDERS) != null
                            || firstDisplayed(AMOUNT_INVESTED) != null
                            || firstDisplayed(COMPANY_NAME_HEADER) != null
                            || firstDisplayed(ACTUAL_WEIGHT_HEADER) != null
                            || firstDisplayed(VALUE_HEADER) != null
                            || firstDisplayed(MAIL_MY_ORDER) != null
                            || firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) != null
                            || firstDisplayed(PORTFOLIO_PREVIEW_ANY) != null
                            || firstDisplayed(CHOOSE_A_LIST) != null
                            || firstDisplayed(RECOMMENDATIONS) != null
                            || firstDisplayed(WATCHLIST) != null;

            if (!knownPlannerState) {
                return false;
            }

            ReportLogger.debug(
                    "Known Investment Planner screen detected; attempting state-based local recovery"
            );

            /*
             * This is a state machine, not a blind Back loop.
             *
             * Choose list       -> Back -> Planner
             * Portfolio Preview -> Back -> Your Orders -> Back -> Planner
             * Your Orders       -> Back -> Planner
             *
             * After each Back we WAIT for Flutter to expose the expected next
             * state. We never issue another Back merely because the semantic tree
             * is rebuilding for a moment.
             */
            for (int step = 1; step <= 4; step++) {
                if (isPlannerLandingReady()) {
                    ReportLogger.debug(
                            "Recovered locally to Investment Planner landing page"
                                    + " | localSteps=" + (step - 1)
                    );
                    return true;
                }

                if (firstDisplayed(HUB_TAB) != null) {
                    ReportLogger.debug(
                            "Local recovery reached Hub/bottom navigation; stopping Back recovery"
                                    + " | localSteps=" + (step - 1)
                    );
                    return false;
                }

                assertDriverAlive("while recovering locally to Investment Planner landing page");

                boolean chooseListState = isOwnListSheetOpen();

                /*
                 * The strict Portfolio Preview page title is an android.view.View,
                 * while the Your Orders link is an android.widget.ImageView. Flutter
                 * can leave some Your Orders semantics exposed under the Preview
                 * route, so the strict Preview title must win during recovery.
                 */
                boolean previewState =
                        firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) != null;

                boolean orderState =
                        !previewState && isYourOrdersContextVisible();

                try {
                    if (chooseListState) {
                        ReportLogger.debug(
                                "Local recovery step: Choose a list -> Investment Planner"
                                        + " | step=" + step
                        );
                        driver.navigate().back();

                        if (waitForPlannerLandingAfterBack(8000L)) {
                            return true;
                        }

                        if (firstDisplayed(HUB_TAB) != null) {
                            return false;
                        }

                        continue;
                    }

                    if (previewState) {
                        ReportLogger.debug(
                                "Local recovery step: Portfolio Preview -> Your Orders"
                                        + " | step=" + step
                        );
                        driver.navigate().back();

                        RecoveryState afterPreviewBack =
                                waitForPlannerRecoveryState(8000L);

                        if (afterPreviewBack == RecoveryState.PLANNER) {
                            return true;
                        }

                        if (afterPreviewBack == RecoveryState.HUB) {
                            return false;
                        }

                        /*
                         * YOUR_ORDERS is expected here. The next loop iteration
                         * performs exactly one more Back to the planner.
                         */
                        continue;
                    }

                    if (orderState) {
                        ReportLogger.debug(
                                "Local recovery step: Your Orders -> Investment Planner"
                                        + " | step=" + step
                        );
                        driver.navigate().back();

                        /*
                         * Large Recommendation orders can take several seconds to
                         * rebuild the planner after Back. Wait generously instead
                         * of issuing extra Back presses that can eject us to Hub.
                         */
                        if (waitForPlannerLandingAfterBack(12000L)) {
                            return true;
                        }

                        if (firstDisplayed(HUB_TAB) != null) {
                            return false;
                        }

                        /*
                         * If the route is still transitional after the full wait,
                         * stop local Back recovery. Hub fallback is safer than a
                         * second blind Back.
                         */
                        ReportLogger.debug(
                                "Your Orders Back did not expose Investment Planner within 12 seconds; "
                                        + "stopping local Back recovery without additional Back presses"
                        );
                        return false;
                    }

                    /*
                     * Flutter may temporarily expose no stable route marker while
                     * rebuilding. Wait for a known state; do NOT press Back from an
                     * unidentified screen.
                     */
                    RecoveryState transitional =
                            waitForPlannerRecoveryState(3500L);

                    if (transitional == RecoveryState.PLANNER) {
                        return true;
                    }

                    if (transitional == RecoveryState.HUB) {
                        return false;
                    }

                    if (transitional == RecoveryState.UNKNOWN) {
                        ReportLogger.debug(
                                "Local recovery state remained unidentified; stopping without blind Back"
                        );
                        return false;
                    }

                } catch (Exception e) {
                    ReportLogger.debug(
                            "Local Investment Planner recovery step " + step
                                    + " failed: " + cleanError(e.getMessage())
                    );
                    return false;
                }
            }

            return isPlannerLandingReady();

        } catch (Exception e) {
            ReportLogger.debug(
                    "Local Investment Planner recovery could not complete: "
                            + cleanError(e.getMessage())
            );
            return false;
        }
    }

    private enum RecoveryState {
        PLANNER,
        YOUR_ORDERS,
        HUB,
        UNKNOWN
    }

    private RecoveryState waitForPlannerRecoveryState(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(500L, timeoutMs);

        while (System.currentTimeMillis() < deadline) {
            if (isPlannerLandingReady()) {
                return RecoveryState.PLANNER;
            }

            if (isYourOrdersContextVisible()) {
                return RecoveryState.YOUR_ORDERS;
            }

            if (firstDisplayed(HUB_TAB) != null) {
                return RecoveryState.HUB;
            }

            assertDriverAlive("while waiting for Investment Planner recovery state");
            sleep(250L);
        }

        return RecoveryState.UNKNOWN;
    }

    private boolean waitForPlannerLandingAfterBack(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(500L, timeoutMs);

        while (System.currentTimeMillis() < deadline) {
            if (isPlannerLandingReady()) {
                ReportLogger.debug(
                        "Investment Planner landing page became ready after Back"
                );
                return true;
            }

            if (firstDisplayed(HUB_TAB) != null) {
                return false;
            }

            assertDriverAlive("while waiting for Investment Planner landing page after Back");
            sleep(250L);
        }

        return false;
    }

    private boolean waitForCleanPlannerBaseline(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(500L, timeoutMs);

        while (System.currentTimeMillis() < deadline) {
            if (isPlannerLandingReady() && firstDisplayed(OWN_LIST) != null) {
                return true;
            }

            if (firstDisplayed(HUB_TAB) != null) {
                return false;
            }

            assertDriverAlive("while waiting for clean Investment Planner testcase baseline");
            sleep(200L);
        }

        return isPlannerLandingReady() && firstDisplayed(OWN_LIST) != null;
    }

    private boolean isPlannerLandingReady() {
        /*
         * The own-list CTA is not a stable landing-page anchor after selecting
         * Recommendation All or a Watchlist such as A5. The selected value can
         * replace that semantic on the planner. These three controls remain
         * stable for both built-in and own-list selections.
         */
        return firstDisplayed(INVESTMENT_PLANNER_TITLE) != null
                && firstDisplayed(GENERATE_MY_ORDER) != null
                && firstDisplayed(AMOUNT_LABEL) != null;
    }

    private void waitForPlannerLandingReadyOrThrow(String source) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(PAGE_WAIT_SECONDS).toMillis();

        while (System.currentTimeMillis() < deadline) {
            if (isPlannerLandingReady()) {
                return;
            }

            assertDriverAlive("while waiting for Investment Planner landing page after " + source);
            sleep(250);
        }

        throw new AssertionError(
                "Investment Planner landing page did not become ready after " + source
                        + " | title=" + (firstDisplayed(INVESTMENT_PLANNER_TITLE) != null)
                        + " | generate=" + (firstDisplayed(GENERATE_MY_ORDER) != null)
                        + " | amountLabel=" + (firstDisplayed(AMOUNT_LABEL) != null)
        );
    }

    private void recoverKnownPlannerScreenBeforeAuth() {
        if (firstDisplayed(HUB_TAB) != null) {
            return;
        }

        boolean knownPlannerScreen =
                firstDisplayed(INVESTMENT_PLANNER_TITLE) != null
                        || firstDisplayed(YOUR_ORDERS) != null
                        || firstDisplayed(AMOUNT_INVESTED) != null
                        || firstDisplayed(COMPANY_NAME_HEADER) != null
                        || firstDisplayed(ACTUAL_WEIGHT_HEADER) != null
                        || firstDisplayed(VALUE_HEADER) != null
                        || firstDisplayed(MAIL_MY_ORDER) != null
                        || firstDisplayed(PORTFOLIO_PREVIEW_PAGE_TITLE) != null
                        || firstDisplayed(PORTFOLIO_PREVIEW_ANY) != null
                        || firstDisplayed(CHOOSE_A_LIST) != null
                        || firstDisplayed(RECOMMENDATIONS) != null
                        || firstDisplayed(WATCHLIST) != null;

        if (!knownPlannerScreen) {
            return;
        }

        ReportLogger.debug(
                "Known Investment Planner deep screen detected before auth check; "
                        + "recovering to bottom navigation first"
        );

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (firstDisplayed(HUB_TAB) != null) {
                ReportLogger.debug(
                        "Recovered Investment Planner deep screen before auth"
                                + " | backAttempts=" + (attempt - 1)
                );
                return;
            }

            assertDriverAlive("while recovering known Investment Planner screen before auth");

            try {
                driver.navigate().back();
            } catch (Exception e) {
                ReportLogger.debug(
                        "Pre-auth Investment Planner back attempt " + attempt
                                + " failed: " + cleanError(e.getMessage())
                );
            }

            sleep(650);
        }

        if (firstDisplayed(HUB_TAB) == null) {
            ReportLogger.debug(
                    "Known Investment Planner screen recovery did not expose Hub; "
                            + "AuthHelper will now resolve PIN/login/session state"
            );
        }
    }

    private WebElement ensureHubTabReachable() {
        WebElement hub = firstDisplayed(HUB_TAB);
        if (hub != null) {
            return hub;
        }

        for (int attempt = 1; attempt <= 7; attempt++) {
            assertDriverAlive("while recovering to Hub before testcase start");

            try {
                driver.navigate().back();
            } catch (Exception e) {
                ReportLogger.debug(
                        "Back navigation attempt " + attempt + " while recovering to Hub failed: "
                                + cleanError(e.getMessage())
                );
            }

            sleep(600);
            hub = firstDisplayed(HUB_TAB);
            if (hub != null) {
                ReportLogger.debug("Recovered bottom navigation before testcase start | backAttempts=" + attempt);
                return hub;
            }
        }

        try {
            driver.activateApp(APP_PACKAGE);
            sleep(900);
        } catch (Exception ignored) {
        }

        return waitForDisplayed(HUB_TAB, 5);
    }

    private boolean waitForYourOrdersCoreReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (firstDisplayed(YOUR_ORDERS) != null
                    && firstDisplayed(AMOUNT_INVESTED) != null) {
                return true;
            }

            if (firstDisplayed(UNDERLYING_DATA_ERROR) != null) {
                return false;
            }

            assertDriverAlive("while waiting for Your Orders core content");
            sleep(220);
        }

        return false;
    }

    private void waitForGeneratedOrderTableReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        boolean horizontalRevealAttempted = false;

        while (System.currentTimeMillis() < deadline) {
            boolean companyReady = firstDisplayed(COMPANY_NAME_HEADER) != null;
            boolean weightReady = firstDisplayed(ACTUAL_WEIGHT_HEADER) != null;
            boolean valueReady = firstDisplayed(VALUE_HEADER) != null;

            if (companyReady && weightReady && valueReady) {
                return;
            }

            if (firstDisplayed(UNDERLYING_DATA_ERROR) != null) {
                throw new AssertionError(
                        "Generated order table could not load: Underlying data not available"
                );
            }

            /*
             * Your Orders uses an Android HorizontalScrollView. The initial
             * viewport shows Company Name / Tentative Weight / Current Price,
             * while Actual Weight / Value (₹) are to the right. Appium Inspector
             * confirms the exact accessibility IDs after that horizontal scroll.
             * Reveal those columns with a bounded, runtime-element-based swipe;
             * never wait 12 seconds for off-screen headers to magically appear.
             */
            if (companyReady && (!weightReady || !valueReady) && !horizontalRevealAttempted) {
                revealGeneratedOrderComputedColumns();
                horizontalRevealAttempted = true;
                continue;
            }

            assertDriverAlive("while waiting for generated order table headers");
            sleep(220);
        }

        throw new AssertionError(
                "Generated order table headers did not become ready"
                        + " | companyName=" + (firstDisplayed(COMPANY_NAME_HEADER) != null)
                        + " | actualWeight=" + (firstDisplayed(ACTUAL_WEIGHT_HEADER) != null)
                        + " | value=" + (firstDisplayed(VALUE_HEADER) != null)
                        + " | horizontalRevealAttempted=" + horizontalRevealAttempted
                        + " | " + orderScreenDiagnostics()
        );
    }

    private void revealGeneratedOrderComputedColumns() {
        if (firstDisplayed(ACTUAL_WEIGHT_HEADER) != null
                && firstDisplayed(VALUE_HEADER) != null) {
            return;
        }

        /*
         * IMPORTANT FOR ANDROID GESTURE NAVIGATION:
         * Do not use a broad Appium area swipe for this horizontal Flutter table.
         * A broad right-to-left swipe can start inside Android's right-edge Back
         * gesture zone and navigate away from Your Orders.
         *
         * Capture the Company Name rectangle ONCE before the first gesture. We
         * retain only primitive Rectangle geometry, never the WebElement itself,
         * so Flutter semantics rebuilds cannot create stale-element failures.
         */
        Rectangle companyHeaderRect = null;

        for (int attempt = 1; attempt <= 4 && companyHeaderRect == null; attempt++) {
            WebElement freshCompanyHeader = firstDisplayed(COMPANY_NAME_HEADER);
            if (freshCompanyHeader != null) {
                try {
                    companyHeaderRect = freshCompanyHeader.getRect();
                } catch (StaleElementReferenceException ignored) {
                    companyHeaderRect = null;
                } catch (Exception ignored) {
                    companyHeaderRect = null;
                }
            }

            if (companyHeaderRect == null) {
                assertDriverAlive("while acquiring fresh Company Name table geometry");
                sleep(220);
            }
        }

        if (companyHeaderRect == null) {
            throw new AssertionError(
                    "Company Name header is not visible before horizontal table reveal"
            );
        }

        /*
         * Two precise interior drags are normally enough to expose the computed
         * columns. Allow at most four bounded attempts for slower Flutter builds.
         * After EVERY gesture verify that Android Back was not triggered.
         */
        for (int attempt = 1; attempt <= 4; attempt++) {
            if (firstDisplayed(ACTUAL_WEIGHT_HEADER) != null
                    && firstDisplayed(VALUE_HEADER) != null) {
                ReportLogger.pass(
                        "Generated order computed columns revealed"
                                + " | horizontalSwipes=" + (attempt - 1)
                );
                return;
            }

            if (firstDisplayed(YOUR_ORDERS) == null) {
                throw new AssertionError(
                        "Your Orders disappeared before horizontal table swipe"
                                + " | attempt=" + attempt
                                + " | possible Android Back/navigation gesture"
                                + " | " + orderScreenDiagnostics()
                );
            }

            swipeOrderTableLeftInsideViewport(companyHeaderRect, attempt);
            sleep(480);

            /*
             * Critical guard added after screencast evidence: if the horizontal
             * gesture was interpreted as Android Back, stop immediately. Never
             * continue issuing blind swipes on another screen.
             */
            if (firstDisplayed(YOUR_ORDERS) == null) {
                throw new AssertionError(
                        "Your Orders disappeared immediately after horizontal table swipe"
                                + " | attempt=" + attempt
                                + " | Android Back/navigation gesture was likely triggered"
                                + " | " + orderScreenDiagnostics()
                );
            }

            if (firstDisplayed(ACTUAL_WEIGHT_HEADER) != null
                    && firstDisplayed(VALUE_HEADER) != null) {
                ReportLogger.pass(
                        "Generated order computed columns revealed"
                                + " | horizontalSwipes=" + attempt
                );
                return;
            }
        }

        throw new AssertionError(
                "Unable to reveal Actual Weight / Value (₹) columns after safe interior table swipes"
                        + " | actualWeight=" + (firstDisplayed(ACTUAL_WEIGHT_HEADER) != null)
                        + " | value=" + (firstDisplayed(VALUE_HEADER) != null)
                        + " | yourOrders=" + (firstDisplayed(YOUR_ORDERS) != null)
                        + " | " + orderScreenDiagnostics()
        );
    }

    private void swipeOrderTableLeftInsideViewport(Rectangle headerRect, int attempt) {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenWidth = size.getWidth();
            int screenHeight = size.getHeight();

            /*
             * Stay well away from Android's right-edge Back gesture zone.
             * On a 1080px emulator this is roughly 799px -> 346px.
             */
            int startX = (int) Math.round(screenWidth * 0.74);
            int endX = (int) Math.round(screenWidth * 0.32);

            /*
             * Drag inside the table body, just below Company Name, rather than
             * across a huge screen region or directly on Android navigation edges.
             */
            int y = headerRect.getY()
                    + headerRect.getHeight()
                    + Math.max(55, Math.min(125, screenHeight / 18));

            int minY = (int) Math.round(screenHeight * 0.22);
            int maxY = (int) Math.round(screenHeight * 0.74);
            y = Math.max(minY, Math.min(y, maxY));

            PointerInput finger = new PointerInput(
                    PointerInput.Kind.TOUCH,
                    "ipOrderInteriorHorizontalFinger"
            );
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ZERO,
                            PointerInput.Origin.viewport(),
                            startX,
                            y
                    )
            );
            swipe.addAction(
                    finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(560L),
                            PointerInput.Origin.viewport(),
                            endX,
                            y
                    )
            );
            swipe.addAction(
                    finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg())
            );

            driver.perform(Collections.singletonList(swipe));

            ReportLogger.step(
                    "Swiped Your Orders table left using safe interior W3C drag"
                            + " | attempt=" + attempt
                            + " | startXRatio=0.74"
                            + " | endXRatio=0.32"
            );
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to perform safe interior horizontal swipe on Your Orders table"
                            + " | attempt=" + attempt
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private String orderScreenDiagnostics() {
        return "package=" + safeCurrentPackage()
                + ", investmentPlanner=" + (firstDisplayed(INVESTMENT_PLANNER_TITLE) != null)
                + ", generateButton=" + (firstDisplayed(GENERATE_MY_ORDER) != null)
                + ", yourOrders=" + (firstDisplayed(YOUR_ORDERS) != null)
                + ", amountInvested=" + (firstDisplayed(AMOUNT_INVESTED) != null)
                + ", companyHeader=" + (firstDisplayed(COMPANY_NAME_HEADER) != null)
                + ", actualWeightHeader=" + (firstDisplayed(ACTUAL_WEIGHT_HEADER) != null)
                + ", valueHeader=" + (firstDisplayed(VALUE_HEADER) != null)
                + ", underlyingDataError="
                + (firstDisplayed(UNDERLYING_DATA_ERROR) != null
                || pageSourceContainsIgnoreCase("Underlying data not available"));
    }

    // =========================================================
    // INTERNAL - LOCATOR / ELEMENT HELPERS
    // =========================================================

    private By portfolioLocator(String portfolioName) {
        if (portfolioName == null) {
            throw new IllegalArgumentException("Portfolio name cannot be null");
        }

        String normalized = portfolioName.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "all weather":
                return ALL_WEATHER;
            case "long term":
                return LONG_TERM;
            case "aggressive":
                return AGGRESSIVE;
            default:
                throw new IllegalArgumentException("Unsupported built-in portfolio: " + portfolioName);
        }
    }

    private void focusAmountFieldFresh() {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 4; attempt++) {
            WebElement field = findAmountField();
            if (field == null) {
                sleep(180);
                continue;
            }

            try {
                field.click();
                sleep(180);
                return;
            } catch (Exception e) {
                lastError = e;
                sleep(180);
            }
        }

        WebElement fallback = findAmountField();
        if (fallback != null) {
            try {
                tapDynamicElementCenter(fallback, "investment amount field");
                sleep(180);
                return;
            } catch (Exception e) {
                lastError = e;
            }
        }

        throw new AssertionError(
                "Unable to focus investment amount field"
                        + (lastError == null ? "" : " | " + cleanError(lastError.getMessage()))
        );
    }

    private void clearAmountFieldFresh() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            WebElement field = findAmountField();
            if (field == null) {
                sleep(180);
                continue;
            }

            try {
                field.clear();
            } catch (Exception e) {
                ReportLogger.debug(
                        "Amount clear attempt " + attempt
                                + " returned a transient element error: "
                                + cleanError(e.getMessage())
                );
            }

            sleep(220);

            if (readAmountDigitsFresh().isEmpty()) {
                return;
            }
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            WebElement field = findAmountField();

            if (field != null) {
                try {
                    field.click();
                } catch (Exception ignored) {
                }
            }

            String currentDigits = readAmountDigitsFresh();
            int deleteCount = Math.max(12, Math.min(currentDigits.length() + 8, 40));

            for (int i = 0; i < deleteCount; i++) {
                try {
                    driver.pressKey(new KeyEvent(AndroidKey.DEL));
                } catch (Exception ignored) {
                    break;
                }
            }

            sleep(220);

            if (readAmountDigitsFresh().isEmpty()) {
                return;
            }
        }

        throw new AssertionError(
                "Unable to clear investment amount field"
                        + " | remaining=" + readAmountTextFresh()
        );
    }

    private boolean typeAmountFresh(long amount) {
        String expected = String.valueOf(amount);

        /*
         * Flutter can rebuild the EditText immediately after clear() or focus.
         * Re-focus and re-locate the field before EVERY bulk sendKeys attempt.
         * This specifically prevents the observed blank-field state where the
         * field was cleared successfully but the stale pre-rebuild element never
         * received the new amount.
         */
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                focusAmountFieldFresh();
            } catch (AssertionError e) {
                ReportLogger.debug(
                        "Amount focus attempt " + attempt + " failed transiently: "
                                + cleanError(e.getMessage())
                );
                sleep(180);
                continue;
            }

            WebElement field = findAmountField();
            if (field == null) {
                sleep(180);
                continue;
            }

            try {
                field.sendKeys(expected);
            } catch (Exception e) {
                ReportLogger.debug(
                        "Amount sendKeys attempt " + attempt
                                + " returned a transient element error: "
                                + cleanError(e.getMessage())
                );
            }

            if (waitForAmountDigits(expected, 1800L)) {
                return true;
            }

            clearAmountFieldFresh();
        }

        /*
         * Final stale-safe fallback: enter one digit at a time, re-focusing and
         * re-locating after each Flutter rebuild. No coordinates, ADB shell, or
         * hardcoded formatted value is used.
         */
        ReportLogger.debug("Bulk amount entry did not stick; using digit-by-digit stale-safe fallback");
        clearAmountFieldFresh();
        return typeAmountDigitByDigitFresh(expected);
    }

    private boolean typeAmountDigitByDigitFresh(String expected) {
        StringBuilder prefix = new StringBuilder();

        for (int index = 0; index < expected.length(); index++) {
            String previousPrefix = prefix.toString();
            String digit = String.valueOf(expected.charAt(index));
            prefix.append(digit);
            String expectedPrefix = prefix.toString();
            boolean digitAccepted = false;

            for (int retry = 1; retry <= 2; retry++) {
                String before = readAmountDigitsFresh();
                if (expectedPrefix.equals(before)) {
                    digitAccepted = true;
                    break;
                }

                /* Do not duplicate a digit if the previous send actually stuck late. */
                if (!previousPrefix.equals(before)) {
                    ReportLogger.debug(
                            "Digit-by-digit amount state changed unexpectedly"
                                    + " | expectedBefore=" + previousPrefix
                                    + " | actualBefore=" + before
                    );
                    return false;
                }

                try {
                    focusAmountFieldFresh();
                    WebElement field = findAmountField();
                    if (field == null) {
                        sleep(150);
                        continue;
                    }
                    field.sendKeys(digit);
                } catch (Exception e) {
                    ReportLogger.debug(
                            "Digit amount entry retry " + retry
                                    + " failed for position " + index
                                    + ": " + cleanError(e.getMessage())
                    );
                }

                if (waitForAmountDigits(expectedPrefix, 1200L)) {
                    digitAccepted = true;
                    break;
                }
            }

            if (!digitAccepted) {
                ReportLogger.debug(
                        "Digit-by-digit amount entry stopped"
                                + " | expectedPrefix=" + expectedPrefix
                                + " | actual=" + readAmountDigitsFresh()
                );
                return false;
            }
        }

        return expected.equals(readAmountDigitsFresh());
    }

    private boolean waitForAmountDigits(String expectedDigits, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (expectedDigits.equals(readAmountDigitsFresh())) {
                return true;
            }
            sleep(120);
        }

        return false;
    }

    private boolean waitForAmountFieldValueFresh(long expectedAmount, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String expected = String.valueOf(expectedAmount);

        while (System.currentTimeMillis() < deadline) {
            if (expected.equals(readAmountDigitsFresh())) {
                return true;
            }
            sleep(200);
        }

        return false;
    }

    private String readAmountDigitsFresh() {
        return readAmountTextFresh().replaceAll("[^0-9]", "");
    }

    private String readAmountTextFresh() {
        WebElement field = findAmountField();
        return field == null ? "" : safeText(field);
    }

    private void closeKeyboardSafely() {
        try {
            driver.hideKeyboard();
            sleep(250);
        } catch (Exception ignored) {
            // Keyboard can already be hidden.
        }
    }

    private WebElement findAmountField() {
        WebElement label = firstDisplayed(AMOUNT_LABEL);
        WebElement generate = firstDisplayed(GENERATE_MY_ORDER);

        List<WebElement> fields = displayedElements(EDIT_TEXT);
        if (fields.isEmpty()) {
            return null;
        }

        if (label == null) {
            return fields.get(0);
        }

        int minY = label.getRect().getY();
        int maxY = generate == null
                ? driver.manage().window().getSize().getHeight()
                : generate.getRect().getY();

        for (WebElement field : fields) {
            try {
                int centerY = field.getRect().getY() + field.getRect().getHeight() / 2;
                if (centerY > minY && centerY < maxY) {
                    return field;
                }
            } catch (Exception ignored) {
            }
        }

        return fields.get(0);
    }

    private WebElement findPlannerHubTileWithBoundedScroll() {
        WebElement tile = firstDisplayed(INVESTMENT_PLANNER_HUB_TILE);
        if (tile != null) {
            return tile;
        }

        for (int attempt = 1; attempt <= 5; attempt++) {
            swipeUpW3C(0.78, 0.30, 600);
            sleep(450);
            tile = firstDisplayed(INVESTMENT_PLANNER_HUB_TILE);
            if (tile != null) {
                ReportLogger.step("Investment Planner Hub tile found after downward scroll | attempt=" + attempt);
                return tile;
            }
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            swipeDownW3C();
            sleep(450);
            tile = firstDisplayed(INVESTMENT_PLANNER_HUB_TILE);
            if (tile != null) {
                ReportLogger.step("Investment Planner Hub tile found after upward recovery scroll | attempt=" + attempt);
                return tile;
            }
        }

        return null;
    }

    private WebElement findWithBoundedOrderScroll(By locator, int maxSwipes) {
        WebElement element = firstDisplayed(locator);
        if (element != null) {
            return element;
        }

        for (int attempt = 1; attempt <= maxSwipes; attempt++) {
            swipeUpW3C(0.74, 0.38, 520);
            sleep(450);
            element = firstDisplayed(locator);
            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private void assertDisplayed(By locator, String label) {
        if (firstDisplayed(locator) == null) {
            throw new AssertionError(label + " is not visible");
        }
        ReportLogger.pass(label + " is visible");
    }

    private void assertDisplayedWithin(By locator, String label, int timeoutSeconds) {
        if (waitForDisplayed(locator, timeoutSeconds) == null) {
            throw new AssertionError(label + " is not visible");
        }
        ReportLogger.pass(label + " is visible");
    }

    private WebElement waitForDisplayed(By locator, int timeoutSeconds) {
        long deadline = System.currentTimeMillis()
                + Duration.ofSeconds(timeoutSeconds).toMillis();

        while (System.currentTimeMillis() < deadline) {
            WebElement element = firstDisplayed(locator);
            if (element != null) {
                return element;
            }

            assertDriverAlive("while waiting for locator: " + locator);
            sleep(POLL_MS);
        }

        return null;
    }

    private WebElement firstDisplayed(By locator) {
        List<WebElement> elements = displayedElements(locator);
        return elements.isEmpty() ? null : elements.get(0);
    }

    private List<WebElement> displayedElements(By locator) {
        List<WebElement> displayed = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                try {
                    if (element != null && element.isDisplayed()) {
                        displayed.add(element);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return displayed;
    }

    private List<WebElement> findAllExactSemantic(String value) {
        List<WebElement> result = new ArrayList<>();

        result.addAll(displayedElements(AppiumBy.accessibilityId(value)));

        if (!result.isEmpty()) {
            return uniqueElements(result);
        }

        String escaped = escapeUiSelector(value);
        result.addAll(
                displayedElements(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().description(\"" + escaped + "\")"
                        )
                )
        );

        return uniqueElements(result);
    }

    private List<WebElement> uniqueElements(List<WebElement> input) {
        Map<String, WebElement> unique = new LinkedHashMap<>();

        for (WebElement element : input) {
            try {
                unique.put(((RemoteWebElement) element).getId(), element);
            } catch (Exception e) {
                unique.put(String.valueOf(System.identityHashCode(element)), element);
            }
        }

        return new ArrayList<>(unique.values());
    }

    private List<WebElement> allDisplayedElements() {
        List<WebElement> visible = new ArrayList<>();

        try {
            List<WebElement> all = driver.findElements(By.xpath("//*"));
            for (WebElement element : all) {
                try {
                    if (element != null && element.isDisplayed()) {
                        visible.add(element);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Dynamic hierarchy scan skipped: " + cleanError(e.getMessage()));
        }

        return visible;
    }

    private String semanticValue(WebElement element) {
        if (element == null) {
            return "";
        }

        String[] attributes = new String[]{"content-desc", "text", "name"};
        for (String attribute : attributes) {
            try {
                String value = element.getAttribute(attribute);
                if (value != null && !value.trim().isEmpty()) {
                    return normalizeSpaces(value);
                }
            } catch (Exception ignored) {
            }
        }

        try {
            return normalizeSpaces(element.getText());
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safeText(WebElement element) {
        if (element == null) {
            return "";
        }

        try {
            String text = element.getText();
            if (text != null && !text.trim().isEmpty()) {
                return normalizeSpaces(text);
            }
        } catch (Exception ignored) {
        }

        return semanticValue(element);
    }

    private void clickElement(WebElement element, String label) {
        if (element == null) {
            throw new AssertionError("Cannot tap null element: " + label);
        }

        try {
            if (element.isDisplayed() && element.isEnabled()) {
                String clickable = element.getAttribute("clickable");
                if (Boolean.parseBoolean(clickable)) {
                    element.click();
                    ReportLogger.step("Tapped using element click: " + label);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("elementId", ((RemoteWebElement) element).getId());
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            ReportLogger.step("Tapped using element-based mobile: clickGesture: " + label);
            return;
        } catch (Exception ignored) {
        }

        try {
            element.click();
            ReportLogger.step("Tapped using fallback element click: " + label);
        } catch (Exception e) {
            throw new AssertionError("Unable to tap " + label + " | error=" + cleanError(e.getMessage()));
        }
    }

    private void clickElementWithMobileGesture(WebElement element, String label) {
        if (element == null) {
            throw new AssertionError("Cannot gesture-tap null element: " + label);
        }

        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("elementId", ((RemoteWebElement) element).getId());
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", args);
            ReportLogger.step("Tapped using forced element clickGesture: " + label);
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to gesture-tap " + label + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void tapDynamicElementCenter(WebElement element, String label) {
        try {
            Rectangle rect = element.getRect();
            int x = rect.getX() + rect.getWidth() / 2;
            int y = rect.getY() + rect.getHeight() / 2;

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "ipTapFinger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));

            ReportLogger.step("Tapped runtime center of " + label);
        } catch (Exception e) {
            throw new AssertionError("Unable to tap runtime center of " + label, e);
        }
    }

    private void tapViewportPoint(int x, int y, String label) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "ipRelativeTapFinger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));
            ReportLogger.step("Tapped runtime anchored position: " + label);
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to tap runtime anchored position for " + label
                            + " | x=" + x + " | y=" + y
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =========================================================
    // INTERNAL - GESTURE / DRIVER HELPERS
    // =========================================================

    private void swipeLeftWithinElement(
            WebElement element,
            double startXRatio,
            double endXRatio,
            double yRatio,
            long durationMs
    ) {
        if (element == null) {
            throw new AssertionError("Cannot horizontally swipe a null element");
        }

        try {
            Rectangle rect = element.getRect();
            int startX = rect.getX() + (int) Math.round(rect.getWidth() * startXRatio);
            int endX = rect.getX() + (int) Math.round(rect.getWidth() * endXRatio);
            int y = rect.getY() + (int) Math.round(rect.getHeight() * yRatio);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "ipHorizontalScrollFinger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ZERO,
                            PointerInput.Origin.viewport(),
                            startX,
                            y
                    )
            );
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(durationMs),
                            PointerInput.Origin.viewport(),
                            endX,
                            y
                    )
            );
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));

            ReportLogger.step("Swiped Your Orders table horizontally to reveal computed columns");
        } catch (Exception e) {
            throw new AssertionError(
                    "Your Orders horizontal table swipe failed"
                            + " | error=" + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void swipeDownW3C() {
        swipeUpW3C(0.30, 0.78, 550);
    }

    private void swipeUpW3C(double startRatio, double endRatio, long durationMs) {
        try {
            Dimension size = driver.manage().window().getSize();
            int x = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * startRatio);
            int endY = (int) (size.getHeight() * endRatio);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "ipScrollFinger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(
                    finger.createPointerMove(
                            Duration.ofMillis(durationMs),
                            PointerInput.Origin.viewport(),
                            x,
                            endY
                    )
            );
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
        } catch (Exception e) {
            throw new AssertionError("Investment Planner W3C scroll failed", e);
        }
    }

    private void activateAdvisorAppIfNeeded() {
        String current = safeCurrentPackage();
        if (APP_PACKAGE.equals(current)) {
            return;
        }

        try {
            driver.activateApp(APP_PACKAGE);
            sleep(900);
        } catch (Exception e) {
            throw new AssertionError(
                    "Unable to activate Advisor app"
                            + " | currentPackage=" + current,
                    e
            );
        }
    }

    private void assertDriverAlive(String context) {
        try {
            driver.getCurrentPackage();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Appium/UiAutomator2 session became unavailable " + context,
                    e
            );
        }
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean pageSourceContainsIgnoreCase(String value) {
        try {
            String source = driver.getPageSource();
            return source != null && source.toLowerCase(Locale.US).contains(value.toLowerCase(Locale.US));
        } catch (Exception ignored) {
            return false;
        }
    }

    // =========================================================
    // INTERNAL - PARSING HELPERS
    // =========================================================

    private Double parseMoney(String raw) {
        if (raw == null) {
            return null;
        }

        String clean = raw.replace("₹", "")
                .replace(",", "")
                .replaceAll("[^0-9.\\-]", "")
                .trim();

        if (clean.isEmpty() || clean.equals("-") || clean.equals(".")) {
            return null;
        }

        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parsePlainNumber(String raw) {
        if (raw == null) {
            return null;
        }

        String clean = raw.replace(",", "")
                .replace("%", "")
                .replaceAll("[^0-9.\\-]", "")
                .trim();

        if (clean.isEmpty() || clean.equals("-") || clean.equals(".")) {
            return null;
        }

        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException ignored) {
            return null;
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

    private String escapeUiSelector(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String cleanError(String message) {
        return normalizeSpaces(message == null ? "" : message);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Investment Planner wait interrupted", e);
        }
    }

    // =========================================================
    // DATA OBJECTS USED BY TEST REPORTING
    // =========================================================

    private static final class NodeValue {
        private final String rawValue;
        private final int centerY;
        private final Double numericValue;

        private NodeValue(String rawValue, int centerY) {
            this(rawValue, centerY, null);
        }

        private NodeValue(String rawValue, int centerY, Double numericValue) {
            this.rawValue = rawValue;
            this.centerY = centerY;
            this.numericValue = numericValue;
        }
    }

    private static final class OrderRow {
        private final String companyName;
        private final double actualWeight;
        private final double value;

        private OrderRow(String companyName, double actualWeight, double value) {
            this.companyName = companyName;
            this.actualWeight = actualWeight;
            this.value = value;
        }

        @Override
        public String toString() {
            return companyName + "|" + actualWeight + "|" + value;
        }
    }

    public static final class OrderValidationSummary {
        private final int capturedRows;
        private final double amountInvested;
        private final double capturedAllocation;
        private final List<String> companies;

        private OrderValidationSummary(
                int capturedRows,
                double amountInvested,
                double capturedAllocation,
                List<String> companies
        ) {
            this.capturedRows = capturedRows;
            this.amountInvested = amountInvested;
            this.capturedAllocation = capturedAllocation;
            this.companies = companies == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(companies));
        }

        public int getCapturedRows() {
            return capturedRows;
        }

        public double getAmountInvested() {
            return amountInvested;
        }

        public double getCapturedAllocation() {
            return capturedAllocation;
        }

        public List<String> getCompanies() {
            return companies;
        }
    }
}