package com.valueresearch.pages;

import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stocks tab -> Stocks home page (active VRSA subscriber).
 *
 * Every public flow starts from Hub (prepared by AdvisorAccountSession), opens the Stocks
 * bottom tab, works on the Stocks home feed and returns to Hub.
 *
 * Locator approach: the screen is Flutter, so visible text is exposed as content-desc.
 * Each page-source snapshot is parsed once and matched with case-insensitive patterns
 * (curly quotes normalised), which is faster than one findElements per label and survives
 * small casing changes. Icons without a content-desc are located relative to a labelled
 * anchor on the same screen, never by fixed screen coordinates.
 */
public class StockHomePage {

    public static final String BUILD_VERSION = "STOCK_HOME_V6_UPDATE_BUTTON_20261029";

    // ---------------------------------------------------------
    // Timing (explicit waits only, no Thread.sleep)
    // ---------------------------------------------------------
    private static final long SHORT_WAIT_MS = 12_000L;
    private static final long PAGE_READY_WAIT_MS = 25_000L;
    private static final long LEAVE_WAIT_MS = 8_000L;
    private static final long BACK_TRANSITION_WAIT_MS = 6_000L;
    private static final long CHANGE_WAIT_MS = 6_000L;
    private static final long POLL_MS = 350L;
    private static final long SETTLE_TIMEOUT_MS = 3_000L;
    private static final long SETTLE_POLL_MS = 300L;

    private static final int MAX_FEED_SWIPES = 25;
    private static final int MAX_BACK_STEPS = 5;
    private static final int MAX_SECTION_DRAGS = 6;

    // Safe tap band (fraction of screen height): below the app bar, above the bottom nav.
    private static final double TAP_BAND_TOP = 0.13;
    private static final double TAP_BAND_BOTTOM = 0.84;
    // A section heading is parked at this height before working inside the section.
    private static final double HEADING_PARK_Y = 0.22;

    // ---------------------------------------------------------
    // Confirmed locators (already used by working modules in this framework)
    // ---------------------------------------------------------
    private static final By HUB_TAB = AppiumBy.accessibilityId("Hub");
    private static final By FUNDS_TAB = AppiumBy.accessibilityId("Funds");
    private static final By STOCKS_TAB = AppiumBy.accessibilityId("Stocks");
    private static final By BACK_BUTTON = AppiumBy.accessibilityId("Go back");

    // ---------------------------------------------------------
    // Stocks home labels, taken from the test-case sheet.
    // NOT YET CONFIRMED against Appium Inspector XML - see the request list in the chat.
    // ---------------------------------------------------------
    private static final String TAB_SUFFIX = "(, tab [0-9]+ of [0-9]+)?";

    private static final Pattern PORTFOLIO_VALUE = ci("^portfolio value.*");
    private static final Pattern ONE_D_CHANGE_FIND = ci("1 ?d change");
    // XML: Button "My Investment - 5, Shows investor selection list"
    private static final Pattern PROFILE_BUTTON = ci("^.+, shows investor selection list$");
    private static final Pattern PROFILE_IS_INVESTMENT_5 = ci("^my investment - 5,.*");
    private static final Pattern CHOOSE_INVESTOR = ci("^choose investor$");
    private static final Pattern OLD_LABELS = ci("^old labels$");
    private static final Pattern INVESTMENT_5_OPTION = ci("^my investment - 5$");
    // XML: "1D Change:" label; amount and percent are one node: "\u20B9+6,297 \n0.83%"
    // Portfolio total (e.g. "\u20B97.68 L") is drawn but NOT exposed in the accessibility tree.
    private static final Pattern AMOUNT = ci("(\u20B9|\\brs\\.?)\\s?[+-]?\\s?[0-9][0-9,]*(\\.[0-9]+)?( ?(l|cr|k))?");
    private static final Pattern PERCENT = ci("[0-9]+(\\.[0-9]+)?\\s?%");

    private static final Pattern EYE_ICON = ci("^(eye|hide|show|unhide|hide values?|show values?|hide balance|show balance|visibility|toggle visibility)$");
    private static final Pattern NOTIFICATION_ICON = ci("^(notifications?|notification bell|bell)$");
    private static final Pattern SEARCH_ICON = ci("^(search|quick search|search for a stock.*)$");
    // XML: bell opens a screen titled "Alerts" (with "Filter By Date" and "Go back")
    private static final Pattern NOTIFICATION_SCREEN = ci("^(alerts|notifications?)$");

    // XML: card View "Outdated Stocks Portfolio\n...\nLast updated\n23 Sep, 2026" with a clickable child
    // View content-desc "Update Stocks Portfolio" (accessibility id).
    private static final Pattern UPDATE_STOCKS_CARD = ci(".*update.*stocks?.*portfolio.*");
    private static final Pattern UPDATE_STOCKS_BUTTON = ci("^update stocks portfolio$");
    private static final Pattern UPDATE_CARD_OR_BUTTON = ci("^(update stocks portfolio|outdated stocks portfolio.*|.*update.*stocks?.*portfolio.*)$");
    private static final Pattern UPDATE_TRANSACTIONS = ci("^update transactions?$");
    private static final Pattern STOCKS_ETFS_TAB = ci("^stocks ?& ?etfs?" + TAB_SUFFIX + "$");
    // XML: "Funds / SIFs, tab 1 of 7", "Stocks & ETFs, tab 2 of 7", "NPS, tab 3 of 7", ...
    private static final Pattern UPDATE_TRANSACTIONS_TABS = ci("^.+, tab [0-9]+ of [0-9]+$");

    private static final Pattern LATEST_RECOMMENDATIONS = ci("^latest recommendations?$");
    private static final Pattern VIEW_MORE = ci("^view more$");
    // XML: "Supriya Lifescience\nMoved from\nHOLD\nBUY"
    private static final Pattern RECOMMENDATION_CARD = ci("^.+ moved from (buy|hold|sell) (buy|hold|sell)$");
    private static final Pattern ANALYSTS_CHOICE = ci("^(stock )?analyst'?s choice$");

    private static final Pattern BIG_MOVES = ci("^big moves$");
    private static final Pattern BIG_MOVES_DEFINITION = ci("^stocks with the biggest price movement.*");
    private static final Pattern INFO_ICON = ci("^(i|info|information|definition|definitions)$");
    private static final Pattern PERIOD_CHIP = ci("^([0-9]+ ?[dwmy])" + TAB_SUFFIX + "$");
    private static final Pattern GAINERS = ci("^gainers$");
    private static final Pattern LOSERS = ci("^losers$");
    private static final Pattern TOP_GAINERS = ci("^top gainers$");
    private static final Pattern TOP_LOSERS = ci("^top losers$");
    private static final Pattern RETURNS_COLUMN = ci("^returns?$");

    private static final Pattern COVERAGE_ON_YOUR_STOCKS = ci("^coverage on your stocks$");
    private static final Pattern ALL_STORIES = ci("^all stories$");
    private static final Pattern COVERAGE_TAB = ci("^coverage" + TAB_SUFFIX + "$");
    private static final Pattern INSIGHTS_TAB = ci("^insights" + TAB_SUFFIX + "$");
    // XML: All Stories tabs are "Coverage, tab 1 of 3" / "Insights, tab 2 of 3" / "Reports, tab 3 of 3" but do not
    // expose which one is selected, so the active tab is confirmed by its category chips.
    private static final Pattern COVERAGE_TAB_CHIP = ci("^(sell )?recommendations?$");
    private static final Pattern INSIGHTS_TAB_CHIP = ci("^using value research stock advisor$");
    private static final Pattern ALL_STORIES_TABS = ci("^(coverage|insights|reports|videos?)" + TAB_SUFFIX + "$");

    // XML: "Ready-to-Invest Portfolios"
    private static final Pattern READY_TO_INVEST = ci("^ready.?to.?invest portfolios?$");
    private static final Pattern[] RTI_TABS = {
            ci("^long.?term growth" + TAB_SUFFIX + "$"),
            ci("^all.?weather" + TAB_SUFFIX + "$"),
            ci("^aggressive growth" + TAB_SUFFIX + "$")
    };
    private static final String[] RTI_TAB_NAMES = {"Long term Growth", "All weather", "Aggressive Growth"};
    private static final Pattern RTI_TAB_ANY = ci("^(long.?term growth|all.?weather|aggressive growth)" + TAB_SUFFIX + "$");
    private static final Pattern COMPANY_COLUMN = ci("^company$");
    private static final Pattern TREND_52W_COLUMN = ci("^52 ?w(eek)? trend$");
    private static final Pattern PRICE_COLUMN = ci("^price$");
    private static final Pattern PLAN_YOUR_INVESTMENT = ci("^plan your investments?$");
    private static final Pattern INVESTMENT_PLANNER = ci("^investment planner$");

    private static final Pattern COVERAGE_AND_INSIGHT = ci("^coverage (and|&) insights?$");
    private static final Pattern SHORT_TERM_CARD = ci("^looking for short.?term investments?.*");
    private static final Pattern NEED_CASH_CARD = ci("^need cash\\?.*");
    private static final Pattern SELL_PLANNER = ci("^sell planner$");

    private static final Pattern OUR_PERFORMANCE = ci("^our performance$");
    private static final Pattern SHOW_MORE = ci("^show more$");
    private static final Pattern SHOW_LESS = ci("^show less$");
    // Add the exact sentences here once you share them; each must be present (contains match).
    private static final List<String> OUR_PERFORMANCE_EXPECTED_TEXT = List.of(
            "Value Research Stock Advisor",
            "S&P BSE 500 TRI",
            "Disclaimer: The performance data presented herein are not verified by the Past Risk and Return Verification Agency (PaRRVA)",
            "Past performance is no guarantee of future results.",
            "is not a guarantee or assurance of future returns.");
    // XML: "+ 870.35 %" and "+ 78.71 %" (dynamic values, format only)
    private static final Pattern PERFORMANCE_PERCENT = ci("^[+-]? ?[0-9]+(\\.[0-9]+)? ?%$");
    private static final Pattern DISCLAIMER = ci("^disclaimer:.*");

    private static final Pattern BONUS_REPORTS = ci("^bonus reports?$");
    private static final int EXPECTED_BONUS_REPORTS = 3;

    private static final Pattern USERS_GUIDE = ci("^user'?s guide$");
    private static final int EXPECTED_USERS_GUIDE_ARTICLES = 4;

    private static final Pattern SCROLL_TOP_ICON = ci("^(scroll to top|back to top|go to top|top|arrow up(ward)?|up)$");

    // Expected top-to-bottom order of the feed (from the sheet); only used to pick the scroll direction.
    private static final List<Pattern> FEED_ORDER = List.of(
            PORTFOLIO_VALUE, UPDATE_STOCKS_CARD, LATEST_RECOMMENDATIONS, BIG_MOVES, COVERAGE_ON_YOUR_STOCKS,
            READY_TO_INVEST, COVERAGE_AND_INSIGHT, SHORT_TERM_CARD, NEED_CASH_CARD, OUR_PERFORMANCE,
            BONUS_REPORTS, USERS_GUIDE);

    // Section headings in feed order; used to find where one section ends.
    private static final List<Pattern> SECTION_HEADINGS = List.of(
            LATEST_RECOMMENDATIONS, BIG_MOVES, COVERAGE_ON_YOUR_STOCKS, READY_TO_INVEST,
            COVERAGE_AND_INSIGHT, OUR_PERFORMANCE, BONUS_REPORTS, USERS_GUIDE);

    // Labels that only exist on Stocks home (used to tell it apart from Funds).
    private static final Pattern STOCKS_ONLY_MARKER = ci("^(value research stock advisor|stock advisor|quick search|search for a stock"
            + "|latest recommendations?|big moves|top gainers|top losers|coverage on your stocks|ready.?to.?invest portfolios?"
            + "|outdated stocks portfolio|coverage (and|&) insights?|our performance|bonus reports?|user'?s guide)([ :].*)?$");
    private static final Pattern STOCKS_HOME_MARKER = ci("^(portfolio value|1 ?d change|view more|show more|show less"
            + "|plan your investments?|looking for short.?term.*|need cash\\?.*)([ :].*)?$");
    private static final Pattern FUNDS_MARKER = ci("^(rich future starts here|outdated portfolio|update portfolio)( .*)?$");

    // Stock details headings (from StockDetailsPage.isStockDetailsSnapshot).
    private static final Set<String> STOCK_DETAIL_MARKERS = Set.of("stock rating", "stock range", "fundamentals",
            "10 years aggregate", "stock performance", "essential checks", "financials", "key ratios",
            "peers", "shareholding", "company profile");

    private static final Pattern PAGE_SOURCE_BOUNDS = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]");
    private static final Pattern HAS_LETTERS = Pattern.compile(".*[A-Za-z]{3,}.*");

    private final AndroidDriver driver;
    private final AdvisorAccountSession preparedSession;
    private String appPackage;
    private Dimension screen;

    public StockHomePage(AndroidDriver driver) {
        this(driver, null);
    }

    public StockHomePage(AndroidDriver driver, AdvisorAccountSession preparedSession) {
        this.driver = Objects.requireNonNull(driver, "AndroidDriver is required");
        if (preparedSession != null && !preparedSession.isForDriver(driver)) {
            throw new IllegalArgumentException("Prepared account session belongs to another driver");
        }
        this.preparedSession = preparedSession;
    }

    // =========================================================
    // Navigation: Hub -> Stocks home, Stocks home -> Hub
    // =========================================================

    /** Opens the Stocks bottom tab. Starts from Hub (or any screen with the bottom nav). */
    public void openStocksHome() {
        ReportLogger.step("[" + BUILD_VERSION + "] Opening Stocks home from the bottom navigation");
        requirePreparedSession();
        rememberAppPackage();
        if (isStocksHomeReady(snapshot())) {
            ReportLogger.pass("Stocks home is already open");
            return;
        }
        tapBottomTab(STOCKS_TAB, "Stocks");
        if (!waitUntil(() -> isStocksHomeReady(snapshot()), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Stocks home did not open after tapping the Stocks tab; visible=" + visibleLabels());
        }
        ReportLogger.pass("Stocks home page is open");
    }

    /** TC1: Funds is the default landing tab; switching to Stocks opens Stocks home. */
    public void verifyFundsTabThenOpenStocksHome() {
        requirePreparedSession();
        rememberAppPackage();
        ReportLogger.step("Opening the Funds tab (default landing tab) from the bottom navigation");
        tapBottomTab(FUNDS_TAB, "Funds");
        if (!waitUntil(() -> isFundsScreen(snapshot()), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Funds tab screen was not recognised; visible=" + visibleLabels());
        }
        ReportLogger.pass("Funds tab screen is visible");

        ReportLogger.step("Switching from Funds to the Stocks tab");
        tapBottomTab(STOCKS_TAB, "Stocks");
        if (!waitUntil(() -> {
            List<UiNode> nodes = snapshot();
            return isStocksHomeReady(nodes) && !isFundsScreen(nodes);
        }, PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Stocks home did not open after switching from Funds; visible=" + visibleLabels());
        }
        ReportLogger.pass("Stocks home page opened from the Funds tab");
    }

    /** Leaves any Stocks screen and finishes on Hub (shared AdvisorAccountSession recovery). */
    public void navigateBackToHub() {
        ReportLogger.step("Returning to Hub");
        reopenAppIfItWasLeft();
        if (preparedSession == null) {
            throw new IllegalStateException("Prepared AdvisorAccountSession is required to return to Hub");
        }
        preparedSession.returnToHub();
        preparedSession.assertHubVisible();
        ReportLogger.pass("Hub is visible");
    }

    // =========================================================
    // TC2: Portfolio value + 1D change
    // =========================================================

    public void verifyPortfolioValueAndOneDayChange() {
        scrollToTop();
        ensureInvestment5Selected();
        PortfolioBlock block = readPortfolioBlock();
        ReportLogger.pass("Investment 5 profile is selected");

        ReportLogger.step("Verifying Portfolio Value, 1D change amount and percentage");
        assertPortfolioNumbersShown(block);
        ReportLogger.pass("Portfolio Value '" + block.portfolioAmount + "', 1D change '" + block.dayAmount
                + "' and '" + block.dayPercent + "' are shown");
    }

    /** Selects "My Investment - 5" under Old Labels in Choose Investor when another profile is active. */
    private void ensureInvestment5Selected() {
        ReportLogger.step("Checking the selected profile is My Investment - 5");
        UiNode profile = find(snapshot(), PROFILE_BUTTON);
        if (profile == null) {
            throw new AssertionError("Profile selector ('..., Shows investor selection list') not visible; visible=" + visibleLabels());
        }
        if (PROFILE_IS_INVESTMENT_5.matcher(profile.label).matches()) {
            ReportLogger.pass("My Investment - 5 is already selected");
            return;
        }
        ReportLogger.step("Current profile is '" + profile.label + "'; selecting My Investment - 5 under Old Labels");
        tap(profile, "profile selector");
        if (!waitUntil(() -> any(snapshot(), CHOOSE_INVESTOR), SHORT_WAIT_MS)) {
            throw new AssertionError("Choose Investor sheet did not open; visible=" + visibleLabels());
        }
        UiNode option = find(settledSnapshot(), INVESTMENT_5_OPTION);
        if (option == null) {
            UiNode oldLabels = find(snapshot(), OLD_LABELS);
            if (oldLabels == null) throw new AssertionError("'Old Labels' not found in Choose Investor; visible=" + visibleLabels());
            tap(oldLabels, "Old Labels (expand)");
            waitUntil(() -> any(snapshot(), INVESTMENT_5_OPTION), CHANGE_WAIT_MS);
            option = find(snapshot(), INVESTMENT_5_OPTION);
        }
        for (int i = 0; option != null && option.cy() > screen().getHeight() * 0.92 && i < 3; i++) {
            dragFeedBy((int) (screen().getHeight() * 0.25));
            option = find(snapshot(), INVESTMENT_5_OPTION);
        }
        if (option == null) {
            throw new AssertionError("'My Investment - 5' not found under Old Labels; visible=" + visibleLabels());
        }
        tap(option, "My Investment - 5");
        if (!waitUntil(() -> {
            UiNode current = find(snapshot(), PROFILE_BUTTON);
            return current != null && PROFILE_IS_INVESTMENT_5.matcher(current.label).matches();
        }, PAGE_READY_WAIT_MS)) {
            throw new AssertionError("My Investment - 5 was tapped but the profile selector did not change; visible=" + visibleLabels());
        }
        ReportLogger.pass("My Investment - 5 selected");
    }

    // =========================================================
    // TC3: eye button hides and shows the three numbers
    // =========================================================

    public void verifyEyeButtonHidesAndShowsValues() {
        scrollToTop();
        ensureInvestment5Selected();
        PortfolioBlock shown = readPortfolioBlock();
        assertPortfolioNumbersShown(shown);

        UiNode eye = findEyeButton();
        ReportLogger.step("Tapping the eye button to hide the values");
        tap(eye, "eye button");
        if (!waitUntil(() -> { PortfolioBlock b = readPortfolioBlockOrNull(); return b != null && b.allHidden(); }, CHANGE_WAIT_MS)) {
            throw new AssertionError("Eye button did not hide Portfolio Value / 1D change numbers; block="
                    + readPortfolioBlock().text);
        }
        ReportLogger.pass("Portfolio Value, 1D change amount and percentage are hidden");

        ReportLogger.step("Tapping the eye button again to show the values");
        tap(findEyeButton(), "eye button");
        if (!waitUntil(() -> { PortfolioBlock b = readPortfolioBlockOrNull(); return b != null && b.allShown(); }, CHANGE_WAIT_MS)) {
            throw new AssertionError("Eye button did not show the numbers again; block=" + readPortfolioBlock().text);
        }
        ReportLogger.pass("Portfolio Value, 1D change amount and percentage are visible again");
    }

    // =========================================================
    // TC4: notification bell and search
    // =========================================================

    public void verifyNotificationPanelAndSearch() {
        scrollToTop();
        List<UiNode> icons = topBarIconCandidates();
        if (icons.isEmpty()) {
            throw new AssertionError("No top-bar icon found on Stocks home; visible=" + visibleLabels());
        }
        boolean notificationVerified = false;
        boolean searchVerified = false;
        for (UiNode candidate : icons) {
            if (notificationVerified && searchVerified) break;
            String name = candidate.label.isEmpty() ? "top-bar icon at x=" + candidate.cx() : "'" + candidate.label + "'";
            ReportLogger.step("Tapping " + name);
            tap(candidate, name);
            waitUntil(() -> !isStocksHomeReady(snapshot()), LEAVE_WAIT_MS);
            // Run 1: classifying right after leaving saw only "Go back" (Alerts had not rendered yet).
            waitUntil(() -> {
                List<UiNode> n = snapshot();
                return isSearchScreen(n) || any(n, NOTIFICATION_SCREEN) || isStocksHomeReady(n);
            }, SHORT_WAIT_MS);
            List<UiNode> opened = settledSnapshot();
            if (!searchVerified && isSearchScreen(opened)) {
                ReportLogger.pass("Search opened (input field, All / Stocks / Mutual Funds tabs)");
                searchVerified = true;
            } else if (!notificationVerified && any(opened, NOTIFICATION_SCREEN) && !isStocksHomeReady(opened)) {
                ReportLogger.pass("Notification panel opened");
                notificationVerified = true;
            } else if (isStocksHomeReady(opened)) {
                ReportLogger.debug(name + " did not leave Stocks home");
                continue;
            } else {
                ReportLogger.info(name + " opened another screen: " + abbreviate(labelsOf(opened), 8));
            }
            returnToStocksHome(name);
            scrollToTop();
        }
        if (!notificationVerified) throw new AssertionError("Notification bell / Notification panel was not found among top-bar icons");
        if (!searchVerified) throw new AssertionError("Search icon / search screen was not found among top-bar icons");
        ReportLogger.pass("Notification panel and Search both opened and returned to Stocks home with Back");
    }

    // =========================================================
    // TC5: Update stocks portfolio card (optional)
    // =========================================================

    public void verifyUpdateStocksPortfolioCardIfPresent() {
        scrollToTop();
        ReportLogger.step("Looking for the 'Update stocks portfolio' card near the top of Stocks home");
        // Run 1: searching for the unlabelled button first scrolled the card off screen and the case passed without
        // testing anything. Look for either the button label or the card text in one pass.
        // The card loads after Portfolio Value; give it a moment before treating it as absent.
        if (waitUntil(() -> any(snapshot(), UPDATE_CARD_OR_BUTTON), CHANGE_WAIT_MS)) {
            ReportLogger.debug("Outdated Stocks Portfolio card rendered on the first screen");
        }
        UiNode card = revealDownOnly(UPDATE_CARD_OR_BUTTON, 2);
        if (card != null && !UPDATE_STOCKS_BUTTON.matcher(card.label).matches()) {
            UiNode button = find(snapshot(), UPDATE_STOCKS_BUTTON);
            if (button != null) card = button;
        }
        if (card == null) {
            ReportLogger.pass("'Update stocks portfolio' card is not shown - portfolio is already updated, nothing to verify");
            return;
        }
        String cardLabel = card.label;
        UiNode target = UPDATE_STOCKS_BUTTON.matcher(card.label).matches() ? card : updateButtonInCard(card);
        ReportLogger.step(target == card ? "Tapping the 'Update Stocks Portfolio' button"
                : "Button label not exposed; tapping the button area at the bottom of the card");
        tapAndLeave(target, "'Update Stocks Portfolio' button");
        if (!waitUntil(() -> any(snapshot(), UPDATE_TRANSACTIONS), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Update Transactions heading not shown after tapping '" + cardLabel + "'; visible="
                    + visibleLabels());
        }
        List<UiNode> nodes = settledSnapshot();
        UiNode etfTab = find(nodes, STOCKS_ETFS_TAB);
        if (etfTab == null) {
            throw new AssertionError("'Stocks & ETFs' tab not shown under Update Transactions; visible=" + abbreviate(labelsOf(nodes), 20));
        }
        Boolean selected = selectedInGroup(nodes, STOCKS_ETFS_TAB, UPDATE_TRANSACTIONS_TABS);
        if (Boolean.FALSE.equals(selected)) {
            throw new AssertionError("Update Transactions opened, but the 'Stocks & ETFs' tab is not the selected tab");
        }
        ReportLogger.pass("Update Transactions opened on the Stocks & ETFs tab"
                + (selected == null ? " (tab selection state not exposed; tab presence verified)" : ""));
        returnToStocksHome("Update Transactions");
    }

    /**
     * XML: the card is one node "Outdated Stocks Portfolio\n...\nLast updated\n23 Sep, 2026"; the
     * "Update Stocks Portfolio" button is drawn below that text. Prefer a clickable full-width node
     * just under the card text; otherwise tap the bottom strip of the card.
     */
    private UiNode updateButtonInCard(UiNode card) {
        List<UiNode> nodes = snapshot();
        UiNode best = null;
        int limit = card.bottom + (int) (screen().getHeight() * 0.08);
        for (UiNode node : nodes) {
            if (node.clickable && node != card && node.width() > screen().getWidth() * 0.5
                    && node.height() < screen().getHeight() * 0.08 && node.top >= card.top && node.top <= limit
                    && (best == null || node.top > best.top)) best = node;
        }
        if (best != null) return best;
        int stripTop = card.bottom - Math.max(card.height() / 6, 40);
        return new UiNode("", "synthetic", card.left, stripTop, card.right, card.bottom, true, false, false);
    }

    // =========================================================
    // TC6: Latest Recommendations
    // =========================================================

    public void verifyLatestRecommendations() {
        ReportLogger.step("Scrolling to Latest Recommendations");
        revealHeadingNearTop(LATEST_RECOMMENDATIONS, "Latest Recommendations");
        SectionCards cards = collectSectionCards(LATEST_RECOMMENDATIONS, RECOMMENDATION_CARD);
        ReportLogger.info("Latest Recommendations cards: " + cards.labels);
        if (cards.labels.size() != 3) {
            throw new AssertionError("Expected 3 recommendations under Latest Recommendations but found "
                    + cards.labels.size() + ": " + cards.labels);
        }
        ReportLogger.pass("Latest Recommendations shows 3 recommendations");

        UiNode viewMore = viewMoreFor(LATEST_RECOMMENDATIONS, "Latest Recommendations");
        tapAndLeave(viewMore, "View more (Latest Recommendations)");
        if (!waitUntil(() -> any(snapshot(), ANALYSTS_CHOICE), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("View more did not open Analyst's Choice; visible=" + visibleLabels());
        }
        ReportLogger.pass("View more opened Analyst's Choice");
        returnToStocksHome("Analyst's Choice");
    }

    // =========================================================
    // TC7: Big Moves
    // =========================================================

    public void verifyBigMoves() {
        ReportLogger.step("Scrolling to Big Moves");
        UiNode heading = revealHeadingNearTop(BIG_MOVES, "Big Moves");

        // 'i' definition drawer
        UiNode info = findIconNextTo(heading, INFO_ICON, "Big Moves 'i' icon");
        Set<String> before = labelSet(snapshot());
        tap(info, "Big Moves 'i' icon");
        if (!waitUntil(() -> any(snapshot(), BIG_MOVES_DEFINITION) || isDrawerShown(before), CHANGE_WAIT_MS)) {
            throw new AssertionError("Big Moves 'i' did not open a definition drawer; visible=" + visibleLabels());
        }
        ReportLogger.pass("Definition drawer opened: " + abbreviate(newLabels(before), 3));
        ReportLogger.step("Closing the definition drawer with the device Back key");
        driver.navigate().back();
        if (!waitUntil(() -> { List<UiNode> n = snapshot(); return isStocksHomeReady(n) && any(n, BIG_MOVES); }, SHORT_WAIT_MS)) {
            throw new AssertionError("Definition drawer did not close back to Big Moves; visible=" + visibleLabels());
        }
        ReportLogger.pass("Definition drawer closed");

        // Gainers first, then Losers
        ensureBigMovesMode(true);
        verifyAllPeriodTabs("Top Gainers", TOP_GAINERS);
        ensureBigMovesMode(false);
        verifyAllPeriodTabs("Top Losers", TOP_LOSERS);
        ensureBigMovesMode(true);
        ReportLogger.pass("Gainers/Losers toggle switches the heading and the Returns column stays in both");

        // Open any (dynamic) company
        openFirstCompanyBelow(BIG_MOVES, RETURNS_COLUMN, "Big Moves");
    }

    private void ensureBigMovesMode(boolean gainers) {
        Pattern wanted = gainers ? TOP_GAINERS : TOP_LOSERS;
        String wantedName = gainers ? "Top Gainers" : "Top Losers";
        revealHeadingNearTop(BIG_MOVES, "Big Moves");
        if (findInSectionOrNull(BIG_MOVES, wanted) != null) {
            ReportLogger.debug(wantedName + " is already active");
            return;
        }
        ReportLogger.step("Toggling Big Moves to " + (gainers ? "Gainers" : "Losers"));
        // Run 1: the toggle sits on the "Big Moves" heading row (right side), not below the heading.
        UiNode toggle = inHeadingRow(BIG_MOVES, gainers ? GAINERS : LOSERS);
        if (toggle == null) toggle = findInSectionOrNull(BIG_MOVES, gainers ? GAINERS : LOSERS);
        if (toggle == null) {
            toggle = switchInSection(BIG_MOVES);
        }
        if (toggle == null) {
            throw new AssertionError("Gainers/Losers toggle not found in Big Moves; section="
                    + abbreviate(labelsOf(sectionNodes(snapshot(), BIG_MOVES)), 20));
        }
        tap(toggle, "Gainers/Losers toggle");
        if (!waitUntil(() -> findInSectionOrNull(BIG_MOVES, wanted) != null, CHANGE_WAIT_MS)) {
            throw new AssertionError("Heading did not change to " + wantedName + " after toggling; section="
                    + abbreviate(labelsOf(sectionNodes(snapshot(), BIG_MOVES)), 20));
        }
        ReportLogger.pass("Big Moves heading changed to " + wantedName);
    }

    private void verifyAllPeriodTabs(String modeName, Pattern modeHeading) {
        revealHeadingNearTop(BIG_MOVES, "Big Moves");
        Set<String> visited = new LinkedHashSet<>();
        int railSwipes = 0;
        while (true) {
            List<UiNode> chips = periodChips();
            UiNode next = null;
            for (UiNode chip : chips) {
                if (!visited.contains(chipKey(chip))) { next = chip; break; }
            }
            if (next == null) {
                if (visited.contains("5Y") || chips.isEmpty() || railSwipes >= 3) break;
                UiNode last = chips.get(chips.size() - 1);
                swipeHorizontal(last.cy(), true);
                waitForSettled();
                railSwipes++;
                continue;
            }
            String key = chipKey(next);
            ReportLogger.step(modeName + ": opening period tab " + key);
            tap(next, "period tab " + key);
            waitForSettled();
            if (findInSectionOrNull(BIG_MOVES, modeHeading) == null) {
                throw new AssertionError(modeName + " heading disappeared after selecting " + key);
            }
            if (findInSectionOrNull(BIG_MOVES, RETURNS_COLUMN) == null) {
                findInSection(BIG_MOVES, RETURNS_COLUMN, "Returns column (" + modeName + " / " + key + ")");
            }
            visited.add(key);
            revealHeadingNearTop(BIG_MOVES, "Big Moves");
        }
        if (!visited.contains("1W") || !visited.contains("5Y")) {
            throw new AssertionError(modeName + ": period tabs from 1W to 5Y were not all found; opened=" + visited);
        }
        ReportLogger.pass(modeName + ": opened period tabs " + visited + " with heading and Returns column intact");
    }

    private List<UiNode> periodChips() {
        List<UiNode> chips = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (UiNode node : sectionNodes(snapshot(), BIG_MOVES)) {
            if (PERIOD_CHIP.matcher(node.label).matches() && keys.add(chipKey(node))) chips.add(node);
        }
        chips.sort(Comparator.comparingInt(UiNode::cx));
        return chips;
    }

    private static String chipKey(UiNode chip) {
        Matcher m = PERIOD_CHIP.matcher(chip.label);
        return (m.matches() ? m.group(1) : chip.label).replace(" ", "").toUpperCase(Locale.ROOT);
    }

    // =========================================================
    // TC8 / TC10: View more -> All Stories (Coverage tab)
    // =========================================================

    public void verifyCoverageOnYourStocks() {
        verifyViewMoreOpensAllStories(COVERAGE_ON_YOUR_STOCKS, "Coverage on your stocks", COVERAGE_TAB, "Coverage");
    }

    public void verifyCoverageAndInsights() {
        verifyViewMoreOpensAllStories(COVERAGE_AND_INSIGHT, "Coverage and Insight", COVERAGE_TAB, "Coverage");
    }

    private void verifyViewMoreOpensAllStories(Pattern heading, String headingName, Pattern tab, String tabName) {
        ReportLogger.step("Scrolling to " + headingName);
        revealHeadingNearTop(heading, headingName);
        ReportLogger.pass(headingName + " heading is visible");
        UiNode viewMore = viewMoreFor(heading, headingName);
        tapAndLeave(viewMore, "View more (" + headingName + ")");
        verifyAllStoriesTab(tab, tabName);
        returnToStocksHome("All Stories");
    }

    private void verifyAllStoriesTab(Pattern tab, String tabName) {
        if (!waitUntil(() -> any(snapshot(), ALL_STORIES), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("All Stories did not open; visible=" + visibleLabels());
        }
        boolean coverageWanted = tab == COVERAGE_TAB;
        Pattern wantedChip = coverageWanted ? COVERAGE_TAB_CHIP : INSIGHTS_TAB_CHIP;
        Pattern otherChip = coverageWanted ? INSIGHTS_TAB_CHIP : COVERAGE_TAB_CHIP;
        // Chips render before the story list (the list may still show loading placeholders).
        waitUntil(() -> { List<UiNode> n = snapshot(); return any(n, wantedChip) || any(n, otherChip); }, SHORT_WAIT_MS);
        List<UiNode> nodes = settledSnapshot();
        if (find(nodes, tab) == null) {
            throw new AssertionError("All Stories opened but the " + tabName + " tab is not shown; visible="
                    + abbreviate(labelsOf(nodes), 20));
        }
        Boolean selected = selectedInGroup(nodes, tab, ALL_STORIES_TABS);
        if (Boolean.FALSE.equals(selected)) {
            throw new AssertionError("All Stories opened, but " + tabName + " is not the selected tab");
        }
        if (Boolean.TRUE.equals(selected)) {
            ReportLogger.pass("All Stories opened on the " + tabName + " tab (selected state)");
            return;
        }
        boolean wantedBody = any(nodes, wantedChip);
        boolean otherBody = any(nodes, otherChip);
        if (!wantedBody || otherBody) {
            throw new AssertionError("All Stories did not open on the " + tabName + " tab: "
                    + (coverageWanted ? "Recommendation / Sell Recommendation" : "'Using Value Research Stock Advisor'")
                    + " chip " + (wantedBody ? "shown" : "missing") + ", other tab's chips "
                    + (otherBody ? "shown" : "absent") + "; visible=" + abbreviate(labelsOf(nodes), 20));
        }
        ReportLogger.pass("All Stories opened on the " + tabName + " tab (verified by its category chips)");
    }

    // =========================================================
    // TC9: Ready to invest portfolios
    // =========================================================

    public void verifyReadyToInvestPortfolios() {
        ReportLogger.step("Scrolling to Ready to invest portfolios");
        revealHeadingNearTop(READY_TO_INVEST, "Ready to invest portfolios");
        ReportLogger.pass("Ready to invest portfolios heading is visible");

        for (int i = 0; i < RTI_TABS.length; i++) {
            revealHeadingNearTop(READY_TO_INVEST, "Ready to invest portfolios");
            UiNode tab = findInSection(READY_TO_INVEST, RTI_TABS[i], RTI_TAB_NAMES[i] + " tab");
            ReportLogger.step("Opening the " + RTI_TAB_NAMES[i] + " tab");
            tap(tab, RTI_TAB_NAMES[i] + " tab");
            waitForSettled();
            findInSection(READY_TO_INVEST, COMPANY_COLUMN, "Company column (" + RTI_TAB_NAMES[i] + ")");
            findInSection(READY_TO_INVEST, TREND_52W_COLUMN, "52W Trend column (" + RTI_TAB_NAMES[i] + ")");
            findInSection(READY_TO_INVEST, PRICE_COLUMN, "Price column (" + RTI_TAB_NAMES[i] + ")");
            ReportLogger.pass(RTI_TAB_NAMES[i] + ": columns Company, 52W Trend and Price are shown");
        }

        revealHeadingNearTop(READY_TO_INVEST, "Ready to invest portfolios");
        UiNode plan = findInSection(READY_TO_INVEST, PLAN_YOUR_INVESTMENT, "Plan your Investment button");
        ReportLogger.pass("Plan your Investment button is shown");
        tapAndLeave(plan, "Plan your Investment button");
        if (!waitUntil(() -> any(snapshot(), INVESTMENT_PLANNER), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Plan your Investment did not open Investment Planner; visible=" + visibleLabels());
        }
        ReportLogger.pass("Plan your Investment opened Investment Planner");
        returnToStocksHome("Investment Planner");

        openFirstCompanyBelow(READY_TO_INVEST, COMPANY_COLUMN, "Ready to invest portfolios");
    }

    // =========================================================
    // TC11: Short term investments article and Need cash -> Sell Planner
    // =========================================================

    public void verifyShortTermAndNeedCashCards() {
        ReportLogger.step("Scrolling to 'Looking for short term investments'");
        UiNode shortTerm = reveal(SHORT_TERM_CARD, "'Looking for short term investments' card");
        tapAndLeave(shortTerm, "'Looking for short term investments' card");
        List<UiNode> article = waitForDestinationContent();
        if (isStocksHomeReady(article)) {
            throw new AssertionError("Short term investments card did not open an article");
        }
        ReportLogger.pass("Article opened: " + articleTitle(article));
        returnToStocksHome("short term investments article");

        ReportLogger.step("Scrolling to 'Need cash? Decide which stock to sell'");
        UiNode needCash = reveal(NEED_CASH_CARD, "'Need cash?' card");
        tapAndLeave(needCash, "'Need cash?' card");
        if (!waitUntil(() -> any(snapshot(), SELL_PLANNER), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("'Need cash?' card did not open Sell Planner; visible=" + visibleLabels());
        }
        ReportLogger.pass("'Need cash?' card opened Sell Planner");
        returnToStocksHome("Sell Planner");
    }

    // =========================================================
    // TC12: Our performance + Show more / Show less
    // =========================================================

    public void verifyOurPerformance() {
        ReportLogger.step("Scrolling to Our performance");
        revealHeadingNearTop(OUR_PERFORMANCE, "Our performance");
        ReportLogger.pass("Our performance heading is visible");

        String collapsedText = sectionText(OUR_PERFORMANCE);
        List<UiNode> perfNodes = sectionNodes(snapshot(), OUR_PERFORMANCE);
        int percentCount = 0;
        for (UiNode node : perfNodes) if (PERFORMANCE_PERCENT.matcher(node.label).matches()) percentCount++;
        if (percentCount < 2) {
            throw new AssertionError("Expected 2 performance percentages (Stock Advisor and S&P BSE 500 TRI); section="
                    + abbreviate(labelsOf(perfNodes), 12));
        }
        if (collapsedText.length() < 20) {
            throw new AssertionError("Our performance has no readable text content; section=" + collapsedText);
        }
        for (String expected : OUR_PERFORMANCE_EXPECTED_TEXT) {
            if (!normalize(collapsedText).toLowerCase(Locale.ROOT).contains(normalize(expected).toLowerCase(Locale.ROOT))) {
                throw new AssertionError("Our performance text missing: '" + expected + "'");
            }
        }
        ReportLogger.info("Our performance text: " + abbreviateText(collapsedText, 300));

        UiNode showMore = findInSection(OUR_PERFORMANCE, SHOW_MORE, "Show more");
        UiNode collapsed = find(snapshot(), DISCLAIMER);
        tap(showMore, "Show more");
        UiNode showLess = waitForInSection(OUR_PERFORMANCE, SHOW_LESS, "Show less");
        UiNode expanded = find(snapshot(), DISCLAIMER);
        // The full disclaimer is in content-desc in both states, so the expansion is checked by its height.
        if (collapsed != null && expanded != null && expanded.height() <= collapsed.height()) {
            throw new AssertionError("Show more did not expand the disclaimer (height " + collapsed.height()
                    + " -> " + expanded.height() + ")");
        }
        ReportLogger.pass("Show more expanded the disclaimer");

        tap(showLess, "Show less");
        waitForInSection(OUR_PERFORMANCE, SHOW_MORE, "Show more");
        ReportLogger.pass("Show less collapsed the disclaimer");
    }

    // =========================================================
    // TC13: Bonus reports
    // =========================================================

    public void verifyBonusReports() {
        ReportLogger.step("Scrolling to Bonus reports");
        revealHeadingNearTop(BONUS_REPORTS, "Bonus reports");
        Set<String> opened = openEachCardInSection(BONUS_REPORTS, "Bonus report", EXPECTED_BONUS_REPORTS);
        if (opened.size() != EXPECTED_BONUS_REPORTS) {
            throw new AssertionError("Expected " + EXPECTED_BONUS_REPORTS + " bonus reports but opened "
                    + opened.size() + ": " + opened);
        }
        ReportLogger.pass("Opened all " + opened.size() + " bonus reports and returned each time: " + opened);
    }

    // =========================================================
    // TC14: User's Guide
    // =========================================================

    public void verifyUsersGuide() {
        ReportLogger.step("Scrolling to User's Guide");
        revealHeadingNearTop(USERS_GUIDE, "User's Guide");
        Set<String> opened = openEachCardInSection(USERS_GUIDE, "User's Guide article", 0);
        if (opened.isEmpty()) {
            throw new AssertionError("No User's Guide article could be opened");
        }
        if (opened.size() != EXPECTED_USERS_GUIDE_ARTICLES) {
            ReportLogger.info("User's Guide has " + opened.size() + " articles (sheet mentions "
                    + EXPECTED_USERS_GUIDE_ARTICLES + "; content is dynamic)");
        }
        ReportLogger.pass("Opened " + opened.size() + " User's Guide articles: " + opened);

        revealHeadingNearTop(USERS_GUIDE, "User's Guide");
        UiNode viewMore = viewMoreFor(USERS_GUIDE, "User's Guide");
        tapAndLeave(viewMore, "View more (User's Guide)");
        verifyAllStoriesTab(INSIGHTS_TAB, "Insights");
        returnToStocksHome("All Stories");
    }

    // =========================================================
    // TC15: scroll-to-top arrow
    // =========================================================

    public void verifyScrollToTopArrow() {
        ReportLogger.step("Scrolling to the bottom of Stocks home");
        scrollToBottom();
        UiNode arrow = findScrollTopArrow();
        tap(arrow, "scroll-to-top arrow");
        if (!waitUntil(() -> any(snapshot(), PORTFOLIO_VALUE), SHORT_WAIT_MS)) {
            throw new AssertionError("Upward arrow did not return to the top of Stocks home; visible=" + visibleLabels());
        }
        ReportLogger.pass("Upward arrow returned to the top of Stocks home (Portfolio Value visible)");
    }

    // =========================================================
    // Shared section flows
    // =========================================================

    /** Opens the first company row below a column header in a section, verifies Stock Details, returns. */
    private void openFirstCompanyBelow(Pattern heading, Pattern columnHeader, String sectionName) {
        revealHeadingNearTop(heading, sectionName);
        UiNode header = findInSection(heading, columnHeader, sectionName + " column header");
        UiNode company = null;
        for (int attempt = 0; attempt < 3 && company == null; attempt++) {
            List<UiNode> nodes = snapshot();
            UiNode currentHeader = find(nodes, columnHeader);
            int top = currentHeader != null ? currentHeader.bottom : header.bottom;
            int bottom = sectionBottom(nodes, top, heading);
            for (UiNode node : nodes) {
                if (node.cy() > top && node.cy() < bottom && isCompanyLabel(node.label)) {
                    if (company == null || node.top < company.top) company = node;
                }
            }
            if (company == null) dragFeedBy((int) (screen().getHeight() * 0.25));
        }
        if (company == null) {
            throw new AssertionError("No company row found in " + sectionName + "; section="
                    + abbreviate(labelsOf(sectionNodes(snapshot(), heading)), 20));
        }
        String companyName = company.label;
        ReportLogger.step("Opening company '" + companyName + "' from " + sectionName);
        tapAndLeave(company, "company '" + companyName + "'");
        if (!waitUntil(() -> isStockDetails(snapshot(), companyName), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Stock Details did not open for '" + companyName + "'; visible=" + visibleLabels());
        }
        ReportLogger.pass("Stock Details opened for '" + companyName + "'");
        returnToStocksHome("Stock Details");
    }

    /** Opens every card in a section (vertical or horizontal list). Returns the opened card labels. */
    private Set<String> openEachCardInSection(Pattern heading, String cardName, int stopAfter) {
        Set<String> opened = new LinkedHashSet<>();
        int horizontalSwipes = 0;
        int verticalDrags = 0;
        while (opened.size() < 12) {
            revealHeadingNearTop(heading, cardName + " section");
            List<UiNode> nodes = snapshot();
            List<UiNode> cards = cardsIn(sectionNodes(nodes, heading));
            UiNode next = null;
            for (UiNode card : cards) {
                if (!opened.contains(card.label) && card.left >= 0 && card.right <= screen().getWidth()) {
                    next = card;
                    break;
                }
            }
            if (next == null) {
                if (stopAfter > 0 && opened.size() >= stopAfter) break;
                if (!cards.isEmpty() && horizontalSwipes < 4 && isHorizontalRow(cards)) {
                    ReportLogger.step("Scrolling the " + cardName + " row horizontally");
                    String beforeSwipe = keysOf(cardsIn(sectionNodes(snapshot(), heading)));
                    swipeHorizontal(cards.get(0).cy(), true);
                    waitForSettled();
                    horizontalSwipes++;
                    if (keysOf(cardsIn(sectionNodes(snapshot(), heading))).equals(beforeSwipe)) horizontalSwipes = 4;
                    continue;
                }
                if (verticalDrags < 2 && sectionContinuesBelow(heading)) {
                    dragFeedBy((int) (screen().getHeight() * 0.25));
                    verticalDrags++;
                    UiNode extra = firstUnopenedCardOnScreen(heading, opened);
                    if (extra != null) {
                        openCardAndReturn(extra, cardName);
                        opened.add(extra.label);
                    }
                    continue;
                }
                break;
            }
            openCardAndReturn(next, cardName);
            opened.add(next.label);
        }
        return opened;
    }

    private UiNode firstUnopenedCardOnScreen(Pattern heading, Set<String> opened) {
        List<UiNode> nodes = snapshot();
        UiNode headingNode = find(nodes, heading);
        int top = headingNode != null ? headingNode.bottom : bandTop();
        int bottom = sectionBottom(nodes, top, heading);
        List<UiNode> band = new ArrayList<>();
        for (UiNode node : nodes) if (node.cy() > top && node.cy() < bottom) band.add(node);
        for (UiNode card : cardsIn(band)) if (!opened.contains(card.label)) return card;
        return null;
    }

    private void openCardAndReturn(UiNode card, String cardName) {
        String label = card.label;
        ReportLogger.step("Opening " + cardName + " '" + abbreviateText(label, 60) + "'");
        tapAndLeave(card, cardName + " '" + abbreviateText(label, 40) + "'");
        List<UiNode> opened = waitForDestinationContent();
        ReportLogger.pass(cardName + " opened: " + articleTitle(opened));
        returnToStocksHome(cardName);
    }

    private boolean sectionContinuesBelow(Pattern heading) {
        List<UiNode> nodes = snapshot();
        UiNode headingNode = find(nodes, heading);
        int top = headingNode != null ? headingNode.bottom : bandTop();
        return nextHeadingBelow(nodes, top, heading) == null;
    }

    // =========================================================
    // Stocks home state
    // =========================================================

    private boolean isStocksHomeStructural(List<UiNode> nodes) {
        return bottomTab(nodes, "Stocks") != null && find(nodes, ci("^go back$")) == null && !isSearchScreen(nodes);
    }

    private boolean isStocksHomeReady(List<UiNode> nodes) {
        if (!isStocksHomeStructural(nodes)) return false;
        UiNode stocksTab = bottomTab(nodes, "Stocks");
        UiNode fundsTab = bottomTab(nodes, "Funds");
        if (stocksTab != null && stocksTab.selected) return true;
        if (fundsTab != null && fundsTab.selected) return false;
        return any(nodes, STOCKS_ONLY_MARKER) || (any(nodes, STOCKS_HOME_MARKER) && !any(nodes, FUNDS_MARKER));
    }

    private boolean isFundsScreen(List<UiNode> nodes) {
        if (bottomTab(nodes, "Funds") == null || find(nodes, ci("^go back$")) != null) return false;
        UiNode fundsTab = bottomTab(nodes, "Funds");
        UiNode stocksTab = bottomTab(nodes, "Stocks");
        if (fundsTab.selected) return true;
        if (stocksTab != null && stocksTab.selected) return false;
        return !any(nodes, STOCKS_ONLY_MARKER) && (any(nodes, FUNDS_MARKER) || any(nodes, PORTFOLIO_VALUE));
    }

    private boolean isSearchScreen(List<UiNode> nodes) {
        boolean input = false;
        for (UiNode node : nodes) {
            if (node.cls.endsWith("EditText")) { input = true; break; }
        }
        return input && find(nodes, ci("^all" + TAB_SUFFIX + "$")) != null
                && find(nodes, ci("^mutual funds.*")) != null;
    }

    /** XML (search screen): EditText with an unlabelled back ImageView to its left on the same row. */
    private UiNode searchBackArrow(List<UiNode> nodes) {
        UiNode input = null;
        for (UiNode node : nodes) if (node.cls.endsWith("EditText")) { input = node; break; }
        if (input == null) return null;
        UiNode best = null;
        for (UiNode node : nodes) {
            if (node.cls.endsWith("ImageView") && node.label.isEmpty() && node.right <= input.left + 10
                    && Math.abs(node.cy() - input.cy()) <= input.height() && (best == null || node.left < best.left)) {
                best = node;
            }
        }
        return best;
    }

    private boolean isStockDetails(List<UiNode> nodes, String companyName) {
        if (isStocksHomeStructural(nodes)) return false;
        String company = normalize(companyName).toLowerCase(Locale.ROOT);
        String firstLine = company.split(" [0-9\u20B9+-]")[0].trim();
        for (UiNode node : nodes) {
            String label = node.label.toLowerCase(Locale.ROOT);
            if (STOCK_DETAIL_MARKERS.contains(label)) return true;
            if (!firstLine.isEmpty() && firstLine.length() >= 4 && label.startsWith(firstLine)
                    && node.cy() < screen().getHeight() * 0.35) return true;
        }
        return false;
    }

    private boolean isDrawerShown(Set<String> before) {
        List<UiNode> nodes = snapshot();
        for (String label : newLabels(before, nodes)) {
            if (label.length() >= 25) return true;
        }
        return false;
    }

    /** Back / device Back until Stocks home is visible again. */
    private void returnToStocksHome(String from) {
        ReportLogger.step("Returning to Stocks home from " + from);
        for (int step = 1; step <= MAX_BACK_STEPS; step++) {
            reopenAppIfItWasLeft();
            waitUntil(() -> {
                List<UiNode> n = snapshot();
                return isStocksHomeReady(n) || find(n, ci("^go back$")) != null || bottomTab(n, "Stocks") != null
                        || isSearchScreen(n);
            }, SHORT_WAIT_MS);
            List<UiNode> nodes = snapshot();
            if (isStocksHomeReady(nodes)) {
                ReportLogger.pass("Back on Stocks home");
                return;
            }
            UiNode searchBack = isSearchScreen(nodes) ? searchBackArrow(nodes) : null;
            WebElement back = searchBack == null ? firstDisplayed(BACK_BUTTON) : null;
            if (searchBack != null) {
                // Search has its own back arrow (unlabelled ImageView left of the input); the keyboard hides the nav bar.
                tap(searchBack, "Search back arrow");
            } else if (back != null) {
                safeClick(back, "Go back (step " + step + ")");
            } else if (isStocksHomeStructural(nodes)) {
                // Bottom nav visible, no back button, but not recognised as Stocks home: re-select Stocks.
                tapBottomTab(STOCKS_TAB, "Stocks");
            } else {
                ReportLogger.step("No Go back button on " + from + "; using the device Back key (step " + step + ")");
                driver.navigate().back();
            }
            waitUntil(() -> isStocksHomeReady(snapshot()), BACK_TRANSITION_WAIT_MS);
        }
        if (!isStocksHomeReady(snapshot())) {
            throw new AssertionError("Stocks home not reached after " + MAX_BACK_STEPS + " back steps from "
                    + from + "; visible=" + visibleLabels());
        }
    }

    private void tapAndLeave(UiNode node, String name) {
        tap(node, name);
        if (waitUntil(() -> !isStocksHomeReady(snapshot()), LEAVE_WAIT_MS)) return;
        ReportLogger.step(name + " tap did not navigate yet; tapping once more");
        UiNode again = find(snapshot(), Pattern.compile(Pattern.quote(node.label)));
        tap(again != null ? again : node, name + " (retry)");
        if (!waitUntil(() -> !isStocksHomeReady(snapshot()), LEAVE_WAIT_MS)) {
            throw new AssertionError(name + " did not open a new screen; visible=" + visibleLabels());
        }
    }

    // =========================================================
    // Feed scrolling and section helpers
    // =========================================================

    private void scrollToTop() {
        if (any(snapshot(), PORTFOLIO_VALUE)) return;
        ReportLogger.step("Scrolling Stocks home to the top");
        String previous = fingerprint(snapshot());
        for (int i = 0; i < MAX_FEED_SWIPES; i++) {
            swipeFeed(false);
            List<UiNode> nodes = settledSnapshot();
            if (any(nodes, PORTFOLIO_VALUE)) return;
            String current = fingerprint(nodes);
            if (current.equals(previous)) break;
            previous = current;
        }
        if (!any(snapshot(), PORTFOLIO_VALUE)) {
            throw new AssertionError("Portfolio Value not found at the top of Stocks home; visible=" + visibleLabels());
        }
    }

    private void scrollToBottom() {
        String previous = fingerprint(snapshot());
        int unchanged = 0;
        for (int i = 0; i < MAX_FEED_SWIPES && unchanged < 2; i++) {
            swipeFeed(true);
            String current = fingerprint(settledSnapshot());
            unchanged = current.equals(previous) ? unchanged + 1 : 0;
            previous = current;
        }
    }

    /** Finds a label anywhere on the feed: searches down first, then up. Returns it inside the tap band. */
    private UiNode reveal(Pattern pattern, String name) {
        UiNode node = inBandOrNudge(pattern);
        if (node != null) return node;
        for (boolean down : searchOrder(pattern)) {
            String previous = fingerprint(snapshot());
            int unchanged = 0;
            for (int i = 0; i < MAX_FEED_SWIPES && unchanged < 2; i++) {
                swipeFeed(down);
                List<UiNode> nodes = settledSnapshot();
                if (find(nodes, pattern) != null) {
                    node = inBandOrNudge(pattern);
                    if (node != null) return node;
                }
                String current = fingerprint(nodes);
                unchanged = current.equals(previous) ? unchanged + 1 : 0;
                previous = current;
            }
        }
        throw new AssertionError(name + " not found on Stocks home after scrolling both ways; visible=" + visibleLabels());
    }

    /** Scrolls up first when a section that comes after the target is already on screen. */
    private boolean[] searchOrder(Pattern target) {
        int targetIndex = FEED_ORDER.indexOf(target);
        if (targetIndex >= 0) {
            List<UiNode> nodes = snapshot();
            for (int i = targetIndex + 1; i < FEED_ORDER.size(); i++) {
                if (any(nodes, FEED_ORDER.get(i))) return new boolean[]{false, true};
            }
        }
        return new boolean[]{true, false};
    }

    /** Like reveal() but only scrolls down a limited number of times; returns null when absent. */
    private UiNode revealDownOnly(Pattern pattern, int maxSwipes) {
        UiNode node = inBandOrNudge(pattern);
        for (int i = 0; node == null && i < maxSwipes; i++) {
            swipeFeed(true);
            waitForSettled();
            node = inBandOrNudge(pattern);
        }
        return node;
    }

    private UiNode inBandOrNudge(Pattern pattern) {
        UiNode node = find(snapshot(), pattern);
        if (node == null) return null;
        if (node.cy() >= bandTop() && node.cy() <= bandBottom()) return node;
        int target = (int) (screen().getHeight() * 0.45);
        dragFeedBy(node.cy() - target);
        node = find(settledSnapshot(), pattern);
        if (node != null && node.cy() >= bandTop() && node.cy() <= bandBottom()) return node;
        return null;
    }

    /** Reveals a section heading and parks it near the top so the section body is on screen. */
    private UiNode revealHeadingNearTop(Pattern heading, String name) {
        UiNode node = reveal(heading, name);
        int park = (int) (screen().getHeight() * HEADING_PARK_Y);
        if (node.top > park + screen().getHeight() * 0.08) {
            dragFeedBy(node.top - park);
            UiNode parked = find(settledSnapshot(), heading);
            if (parked != null && parked.cy() >= bandTop()) return parked;
            return reveal(heading, name);
        }
        return node;
    }

    /** Finds a label that belongs to a section (between its heading and the next heading). Fails if absent. */
    private UiNode findInSection(Pattern heading, Pattern target, String name) {
        UiNode node = searchSection(heading, target);
        if (node == null) {
            throw new AssertionError(name + " not found in its section; section="
                    + abbreviate(labelsOf(sectionNodes(snapshot(), heading)), 25));
        }
        return node;
    }

    private UiNode findInSectionOrNull(Pattern heading, Pattern target) {
        List<UiNode> nodes = snapshot();
        UiNode headingNode = find(nodes, heading);
        if (headingNode == null) return null;
        return topmostInBand(nodes, target, headingNode.bottom, sectionBottom(nodes, headingNode.bottom, heading));
    }

    private UiNode waitForInSection(Pattern heading, Pattern target, String name) {
        final UiNode[] found = new UiNode[1];
        waitUntil(() -> (found[0] = searchSectionOnScreen(heading, target)) != null, CHANGE_WAIT_MS);
        return found[0] != null ? found[0] : findInSection(heading, target, name);
    }

    private UiNode searchSection(Pattern heading, Pattern target) {
        revealHeadingNearTop(heading, "section heading");
        String previous = "";
        for (int drag = 0; drag <= MAX_SECTION_DRAGS; drag++) {
            List<UiNode> nodes = settledSnapshot();
            UiNode found = searchSectionOnScreen(nodes, heading, target);
            if (found != null) return found;
            UiNode headingNode = find(nodes, heading);
            int top = headingNode != null ? headingNode.bottom : bandTop();
            UiNode next = nextHeadingBelow(nodes, top, heading);
            if (next != null && next.top < bandBottom()) return null; // section ends on screen
            String current = fingerprint(nodes);
            if (current.equals(previous)) return null; // end of feed
            previous = current;
            dragFeedBy((int) (screen().getHeight() * 0.30));
        }
        return null;
    }

    private UiNode searchSectionOnScreen(Pattern heading, Pattern target) {
        return searchSectionOnScreen(snapshot(), heading, target);
    }

    private UiNode searchSectionOnScreen(List<UiNode> nodes, Pattern heading, Pattern target) {
        UiNode headingNode = find(nodes, heading);
        int top = headingNode != null ? headingNode.bottom : bandTop();
        int bottom = sectionBottom(nodes, top, heading);
        return topmostInBand(nodes, target, top, bottom);
    }

    private UiNode topmostInBand(List<UiNode> nodes, Pattern target, int top, int bottom) {
        UiNode best = null;
        for (UiNode node : nodes) {
            if (target.matcher(node.label).matches() && node.cy() > top && node.cy() < bottom
                    && (best == null || node.top < best.top || (node.top == best.top && node.area() < best.area()))) {
                best = node;
            }
        }
        return best;
    }

    private int sectionBottom(List<UiNode> nodes, int top, Pattern heading) {
        UiNode next = nextHeadingBelow(nodes, top, heading);
        int limit = bandBottom();
        return next != null ? Math.min(next.top, limit) : limit;
    }

    private UiNode nextHeadingBelow(List<UiNode> nodes, int y, Pattern current) {
        UiNode best = null;
        for (Pattern other : SECTION_HEADINGS) {
            if (other == current) continue;
            for (UiNode node : nodes) {
                if (other.matcher(node.label).matches() && node.top >= y && (best == null || node.top < best.top)) {
                    best = node;
                }
            }
        }
        return best;
    }

    /** Nodes between a visible heading and the next heading / tap-band bottom. */
    private List<UiNode> sectionNodes(List<UiNode> nodes, Pattern heading) {
        UiNode headingNode = find(nodes, heading);
        List<UiNode> section = new ArrayList<>();
        if (headingNode == null) return section;
        int bottom = sectionBottom(nodes, headingNode.bottom, heading);
        for (UiNode node : nodes) {
            if (node.cy() > headingNode.bottom && node.cy() < bottom) section.add(node);
        }
        return section;
    }

    private String sectionText(Pattern heading) {
        StringBuilder sb = new StringBuilder();
        for (UiNode node : sectionNodes(settledSnapshot(), heading)) {
            if (!node.label.isEmpty() && !SHOW_MORE.matcher(node.label).matches()
                    && !SHOW_LESS.matcher(node.label).matches() && sb.indexOf(node.label) < 0) {
                sb.append(node.label).append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** Clickable, labelled cards in a section; nested children of another card are dropped. */
    private List<UiNode> cardsIn(List<UiNode> section) {
        List<UiNode> candidates = new ArrayList<>();
        for (UiNode node : section) {
            if (node.label.isEmpty() || !(node.clickable || node.cls.endsWith("ImageView"))) continue;
            if (VIEW_MORE.matcher(node.label).matches() || SHOW_MORE.matcher(node.label).matches()
                    || PERIOD_CHIP.matcher(node.label).matches()) continue;
            boolean isHeading = false;
            for (Pattern heading : SECTION_HEADINGS) {
                if (heading.matcher(node.label).matches()) { isHeading = true; break; }
            }
            if (!isHeading) candidates.add(node);
        }
        List<UiNode> cards = new ArrayList<>();
        Set<String> labels = new LinkedHashSet<>();
        for (UiNode node : candidates) {
            boolean nested = false;
            for (UiNode other : candidates) {
                if (other != node && other.contains(node) && other.area() > node.area()) { nested = true; break; }
            }
            if (!nested && labels.add(node.label)) cards.add(node);
        }
        cards.sort(Comparator.comparingInt((UiNode n) -> n.top).thenComparingInt(n -> n.left));
        return cards;
    }

    /** Collects every card label matching cardPattern between the heading and the next section heading. */
    private SectionCards collectSectionCards(Pattern heading, Pattern cardPattern) {
        SectionCards result = new SectionCards();
        String previous = "";
        for (int drag = 0; drag <= MAX_SECTION_DRAGS; drag++) {
            List<UiNode> nodes = settledSnapshot();
            UiNode headingNode = find(nodes, heading);
            int top = headingNode != null ? headingNode.bottom : bandTop();
            UiNode next = nextHeadingBelow(nodes, top, heading);
            int bottom = next != null ? next.top : screen().getHeight();
            for (UiNode node : nodes) {
                if (node.cy() > top && node.cy() < bottom && cardPattern.matcher(node.label).matches()) {
                    result.labels.add(node.label);
                }
            }
            if (next != null) return result;
            String current = fingerprint(nodes);
            if (current.equals(previous)) return result;
            previous = current;
            dragFeedBy((int) (screen().getHeight() * 0.25));
        }
        return result;
    }

    /** A label on the same row as a visible heading, to its right (toggles, links). */
    private UiNode inHeadingRow(Pattern heading, Pattern target) {
        List<UiNode> nodes = snapshot();
        UiNode anchor = find(nodes, heading);
        if (anchor == null) return null;
        UiNode best = null;
        for (UiNode node : nodes) {
            if (target.matcher(node.label).matches() && Math.abs(node.cy() - anchor.cy()) <= Math.max(anchor.height(), 60)
                    && node.cx() > anchor.cx() && (best == null || node.area() < best.area())) best = node;
        }
        return best;
    }

    /** XML: "View more" sits on the heading row (right side), not below the heading. */
    private UiNode viewMoreFor(Pattern heading, String headingName) {
        UiNode headingNode = revealHeadingNearTop(heading, headingName);
        List<UiNode> nodes = snapshot();
        UiNode current = find(nodes, heading);
        UiNode anchor = current != null ? current : headingNode;
        UiNode best = null;
        for (UiNode node : nodes) {
            if (VIEW_MORE.matcher(node.label).matches() && Math.abs(node.cy() - anchor.cy()) <= Math.max(anchor.height(), 60)
                    && node.cx() > anchor.cx() && (best == null || distance(node, anchor) < distance(best, anchor))) {
                best = node;
            }
        }
        if (best == null) best = searchSection(heading, VIEW_MORE);
        if (best == null) {
            throw new AssertionError("View more for " + headingName + " not found; visible=" + visibleLabels());
        }
        return best;
    }

    private List<UiNode> wideLabelledNodes(List<UiNode> band) {
        List<UiNode> wide = new ArrayList<>();
        for (UiNode node : band) {
            if (!node.label.isEmpty() && node.width() > screen().getWidth() * 0.6
                    && !VIEW_MORE.matcher(node.label).matches()) wide.add(node);
        }
        return wide;
    }

    private boolean isHorizontalRow(List<UiNode> cards) {
        if (cards.size() == 1) return cards.get(0).width() < screen().getWidth() * 0.85;
        UiNode first = cards.get(0);
        for (UiNode card : cards) {
            if (card != first && Math.abs(card.cy() - first.cy()) < first.height() / 2) return true;
        }
        return false;
    }

    private boolean isCompanyLabel(String label) {
        if (label.isEmpty() || !HAS_LETTERS.matcher(label).matches()) return false;
        for (Pattern skip : new Pattern[]{COMPANY_COLUMN, TREND_52W_COLUMN, PRICE_COLUMN, RETURNS_COLUMN,
                TOP_GAINERS, TOP_LOSERS, GAINERS, LOSERS, PERIOD_CHIP, VIEW_MORE, PLAN_YOUR_INVESTMENT,
                RTI_TAB_ANY, INFO_ICON}) {
            if (skip.matcher(label).matches()) return false;
        }
        for (Pattern heading : SECTION_HEADINGS) if (heading.matcher(label).matches()) return false;
        return true;
    }

    // =========================================================
    // Icon finders (icons may have no content-desc)
    // =========================================================

    private PortfolioBlock readPortfolioBlock() {
        List<UiNode> nodes = snapshot();
        UiNode heading = find(nodes, PORTFOLIO_VALUE);
        if (heading == null) {
            throw new AssertionError("Portfolio Value heading is not visible; visible=" + abbreviate(labelsOf(nodes), 20));
        }
        UiNode dayLabel = null;
        for (UiNode node : nodes) {
            if (ONE_D_CHANGE_FIND.matcher(node.label).find() && node.top >= heading.top
                    && (dayLabel == null || node.area() < dayLabel.area())) dayLabel = node;
        }
        int top = heading.top;
        int bottom = dayLabel != null ? dayLabel.bottom + heading.height() : heading.bottom + (int) (screen().getHeight() * 0.12);
        List<String> labels = new ArrayList<>();
        boolean valueElement = false;
        for (UiNode node : nodes) {
            boolean overlaps = node.bottom > top && node.top < bottom && node.height() < screen().getHeight() * 0.35;
            if (!overlaps) continue;
            if (!node.label.isEmpty() && !labels.contains(node.label)) labels.add(node.label);
            // Unlabelled element between the heading and "1D Change:" that holds the drawn amount.
            if (node.label.isEmpty() && dayLabel != null && node.top >= heading.bottom - 5 && node.bottom <= dayLabel.top + 5
                    && node.cx() < screen().getWidth() * 0.6 && !node.cls.endsWith("ImageView")) valueElement = true;
        }
        return new PortfolioBlock(labels, dayLabel != null, valueElement);
    }

    private PortfolioBlock readPortfolioBlockOrNull() {
        try {
            return readPortfolioBlock();
        } catch (AssertionError headingNotVisible) {
            return null;
        }
    }

    private void assertPortfolioNumbersShown(PortfolioBlock block) {
        if (!block.hasDayLabel) {
            throw new AssertionError("'1D Change:' label not shown under Portfolio Value; block=" + block.text);
        }
        if (block.portfolioAmount == null) {
            if (!block.valueElement) {
                throw new AssertionError("Portfolio Value amount not found under the heading; block=" + block.text);
            }
            ReportLogger.info("Portfolio Value amount is drawn but not exposed to accessibility (no content-desc/text); "
                    + "its element under the heading is verified. Ask dev to add a semantics label to assert the \u20B9 number.");
        }
        if (block.dayAmount == null) {
            throw new AssertionError("1D change amount with \u20B9 sign not shown; block=" + block.text);
        }
        if (block.dayPercent == null) {
            throw new AssertionError("1D change percentage not shown; block=" + block.text);
        }
    }

    private UiNode findEyeButton() {
        List<UiNode> nodes = snapshot();
        UiNode semantic = find(nodes, EYE_ICON);
        if (semantic != null) return semantic;
        UiNode heading = find(nodes, PORTFOLIO_VALUE);
        if (heading == null) throw new AssertionError("Portfolio Value heading not visible; cannot locate the eye button");
        int rowTop = heading.top - heading.height();
        int rowBottom = heading.bottom + (int) (screen().getHeight() * 0.05);
        UiNode best = null;
        for (UiNode node : nodes) {
            // XML: clickable ImageView at the right edge of the Portfolio Value row (no content-desc)
            if (node.clickable && node.label.isEmpty() && node.width() < screen().getWidth() * 0.2
                    && node.cx() > screen().getWidth() * 0.7 && node.cy() >= rowTop && node.cy() <= rowBottom) {
                if (best == null || distance(node, heading) < distance(best, heading)) best = node;
            }
        }
        if (best == null) {
            throw new AssertionError("Eye button not found next to Portfolio Value (no labelled or unlabelled icon). "
                    + "Share the Inspector XML of the portfolio card.");
        }
        return best;
    }

    /** Labelled icons first (Search / Notifications), then unlabelled icons in the top bar, right to left. */
    private List<UiNode> topBarIconCandidates() {
        List<UiNode> nodes = snapshot();
        List<UiNode> result = new ArrayList<>();
        for (UiNode node : nodes) {
            if (NOTIFICATION_ICON.matcher(node.label).matches() || SEARCH_ICON.matcher(node.label).matches()) result.add(node);
        }
        List<UiNode> unlabelled = new ArrayList<>();
        int barBottom = (int) (screen().getHeight() * 0.15);
        for (UiNode node : nodes) {
            if ((node.clickable || node.cls.endsWith("ImageView")) && node.label.isEmpty() && node.cy() < barBottom
                    && node.cx() > screen().getWidth() * 0.5 && node.width() < screen().getWidth() * 0.2) {
                unlabelled.add(node);
            }
        }
        unlabelled.sort(Comparator.comparingInt(UiNode::cx).reversed());
        result.addAll(unlabelled);
        return result;
    }

    private UiNode findIconNextTo(UiNode heading, Pattern semantic, String name) {
        List<UiNode> nodes = snapshot();
        UiNode current = find(nodes, Pattern.compile(Pattern.quote(heading.label)));
        UiNode anchor = current != null ? current : heading;
        UiNode best = null;
        for (UiNode node : nodes) {
            if (semantic.matcher(node.label).matches() && Math.abs(node.cy() - anchor.cy()) < anchor.height() * 2) return node;
        }
        for (UiNode node : nodes) {
            if ((node.clickable || node.cls.endsWith("ImageView")) && node.label.isEmpty()
                    && node.width() < screen().getWidth() * 0.2
                    && Math.abs(node.cy() - anchor.cy()) <= Math.max(anchor.height(), 40) && node.left >= anchor.right - 5) {
                if (best == null || distance(node, anchor) < distance(best, anchor)) best = node;
            }
        }
        if (best == null) {
            throw new AssertionError(name + " not found on the heading row; share the Inspector XML of this section");
        }
        return best;
    }

    private UiNode switchInSection(Pattern heading) {
        for (UiNode node : sectionNodes(snapshot(), heading)) {
            if (node.cls.endsWith("Switch") || node.checkable) return node;
        }
        return null;
    }

    private UiNode findScrollTopArrow() {
        List<UiNode> nodes = snapshot();
        UiNode semantic = find(nodes, SCROLL_TOP_ICON);
        if (semantic != null) return semantic;
        UiNode best = null;
        for (UiNode node : nodes) {
            if (node.clickable && node.label.isEmpty() && node.cx() > screen().getWidth() * 0.70
                    && node.cy() > screen().getHeight() * 0.45 && node.cy() < screen().getHeight() * 0.90
                    && node.width() < screen().getWidth() * 0.25) {
                if (best == null || node.top > best.top) best = node;
            }
        }
        if (best == null) {
            throw new AssertionError("Upward (scroll-to-top) arrow not found at the bottom of Stocks home; "
                    + "share the Inspector XML of that screen");
        }
        return best;
    }

    private static int distance(UiNode a, UiNode b) {
        return Math.abs(a.cx() - b.cx()) + Math.abs(a.cy() - b.cy());
    }

    private UiNode bottomTab(List<UiNode> nodes, String name) {
        UiNode best = null;
        for (UiNode node : nodes) {
            if (node.label.equalsIgnoreCase(name) && node.cy() > screen().getHeight() * 0.80
                    && (best == null || node.top > best.top)) best = node;
        }
        return best;
    }

    /** True/false when the group exposes a selected tab; null when selection state is not exposed. */
    private Boolean selectedInGroup(List<UiNode> nodes, Pattern wanted, Pattern group) {
        boolean anySelected = false;
        for (UiNode node : nodes) {
            if (node.selected && group.matcher(node.label).matches()) {
                anySelected = true;
                if (wanted.matcher(node.label).matches()) return Boolean.TRUE;
            }
        }
        return anySelected ? Boolean.FALSE : null;
    }

    // =========================================================
    // Snapshots, waits, gestures
    // =========================================================

    private List<UiNode> snapshot() {
        String source = driver.getPageSource();
        List<UiNode> nodes = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
            NodeList all = document.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Element element = (Element) all.item(i);
                if ("false".equals(element.getAttribute("displayed"))) continue;
                Matcher m = PAGE_SOURCE_BOUNDS.matcher(element.getAttribute("bounds"));
                if (!m.find()) continue;
                int left = Integer.parseInt(m.group(1));
                int top = Integer.parseInt(m.group(2));
                int right = Integer.parseInt(m.group(3));
                int bottom = Integer.parseInt(m.group(4));
                if (right <= left || bottom <= top) continue;
                String desc = normalize(element.getAttribute("content-desc"));
                String label = desc.isEmpty() ? normalize(element.getAttribute("text")) : desc;
                String cls = element.getAttribute("class");
                if (cls.isEmpty()) cls = element.getTagName();
                nodes.add(new UiNode(label, cls, left, top, right, bottom,
                        "true".equals(element.getAttribute("clickable")),
                        "true".equals(element.getAttribute("selected")),
                        "true".equals(element.getAttribute("checkable"))));
            }
        } catch (Exception parseFailure) {
            ReportLogger.debug("Page source parse failed: " + parseFailure.getClass().getSimpleName());
        }
        return nodes;
    }

    /** Waits until two consecutive snapshots are identical (scroll/animation finished). */
    private List<UiNode> settledSnapshot() {
        final List<List<UiNode>> last = new ArrayList<>(Collections.singletonList(snapshot()));
        final String[] lastSignature = {signature(last.get(0))};
        try {
            new WebDriverWait(driver, Duration.ofMillis(SETTLE_TIMEOUT_MS), Duration.ofMillis(SETTLE_POLL_MS))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        List<UiNode> nodes = snapshot();
                        String sig = signature(nodes);
                        boolean stable = sig.equals(lastSignature[0]);
                        lastSignature[0] = sig;
                        last.set(0, nodes);
                        return stable;
                    });
        } catch (TimeoutException stillMoving) {
            ReportLogger.debug("UI did not fully settle within " + SETTLE_TIMEOUT_MS + " ms; using latest snapshot");
        }
        return last.get(0);
    }

    private void waitForSettled() {
        settledSnapshot();
    }

    private boolean waitUntil(BooleanSupplier condition, long timeoutMs) {
        try {
            new WebDriverWait(driver, Duration.ofMillis(timeoutMs), Duration.ofMillis(POLL_MS))
                    .ignoring(StaleElementReferenceException.class)
                    .ignoring(NoSuchElementException.class)
                    .until(d -> condition.getAsBoolean());
            return true;
        } catch (TimeoutException timeout) {
            return false;
        }
    }

    private void tap(UiNode node, String name) {
        if (node == null) throw new AssertionError(name + " is not available to tap");
        ReportLogger.debug("Tapping " + name + " at its live centre (" + node.cx() + "," + node.cy() + ")");
        driver.executeScript("mobile: clickGesture", Map.of("x", node.cx(), "y", node.cy()));
    }

    private void tapBottomTab(By tab, String name) {
        WebElement element = bottomMostVisible(tab);
        if (element == null) {
            throw new AssertionError(name + " bottom-navigation tab is not visible; visible=" + visibleLabels());
        }
        safeClick(element, name + " bottom-navigation tab");
    }

    private void safeClick(WebElement element, String description) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) element).getId()));
                return;
            } catch (StaleElementReferenceException stale) {
                if (attempt == 2) throw stale;
            }
        }
        throw new AssertionError("Failed to click " + description);
    }

    private WebElement firstDisplayed(By locator) {
        try {
            for (WebElement element : driver.findElements(locator)) {
                if (element.isDisplayed()) return element;
            }
        } catch (StaleElementReferenceException | NoSuchElementException ignored) {
            // treated as not visible
        }
        return null;
    }

    private WebElement bottomMostVisible(By locator) {
        WebElement best = null;
        int bestY = -1;
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed() && element.getRect().getY() > bestY) {
                    bestY = element.getRect().getY();
                    best = element;
                }
            } catch (StaleElementReferenceException ignored) {
                // skip stale element
            }
        }
        return best;
    }

    private void swipeFeed(boolean down) {
        int x = screen().getWidth() / 2;
        int startY = (int) (screen().getHeight() * (down ? 0.75 : 0.35));
        int endY = (int) (screen().getHeight() * (down ? 0.35 : 0.75));
        performSwipe(x, startY, x, endY, 450L, 0L);
    }

    /** Slow drag with a hold before release (no fling) so the feed moves by about dy pixels. */
    private void dragFeedBy(int dy) {
        if (Math.abs(dy) < 20) return;
        int h = screen().getHeight();
        int x = (int) (screen().getWidth() * 0.15);
        int max = (int) (h * 0.62);
        int distance = Math.max(-max, Math.min(max, dy));
        int startY = distance > 0 ? (int) (h * 0.80) : (int) (h * 0.18);
        performSwipe(x, startY, x, startY - distance, 900L, 300L);
        waitForSettled();
    }

    private void swipeHorizontal(int y, boolean toLaterItems) {
        int w = screen().getWidth();
        int startX = (int) (w * (toLaterItems ? 0.85 : 0.15));
        int endX = (int) (w * (toLaterItems ? 0.25 : 0.75));
        performSwipe(startX, y, endX, y, 600L, 200L);
    }

    private void performSwipe(int startX, int startY, int endX, int endY, long durationMs, long holdMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(80L)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY));
        if (holdMs > 0) swipe.addAction(new Pause(finger, Duration.ofMillis(holdMs)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    private Dimension screen() {
        if (screen == null) screen = driver.manage().window().getSize();
        return screen;
    }

    private int bandTop() { return (int) (screen().getHeight() * TAP_BAND_TOP); }

    private int bandBottom() { return (int) (screen().getHeight() * TAP_BAND_BOTTOM); }

    private void requirePreparedSession() {
        if (driver.getSessionId() == null) throw new IllegalStateException("AndroidDriver session is unavailable");
        if (preparedSession == null) {
            throw new IllegalStateException("Prepared AdvisorAccountSession is required for Stocks home navigation");
        }
        preparedSession.assertPrepared();
    }

    private void rememberAppPackage() {
        if (appPackage == null) {
            try {
                appPackage = driver.getCurrentPackage();
            } catch (RuntimeException unavailable) {
                ReportLogger.debug("Current package unavailable: " + unavailable.getClass().getSimpleName());
            }
        }
    }

    /** An article can open in a browser; bring the Advisor app back to the foreground if so. */
    private void reopenAppIfItWasLeft() {
        if (appPackage == null) return;
        try {
            String current = driver.getCurrentPackage();
            if (current != null && !current.equals(appPackage)) {
                ReportLogger.step("Another app (" + current + ") is in front; returning to the Advisor app");
                driver.activateApp(appPackage);
                waitUntil(() -> appPackage.equals(driver.getCurrentPackage()), SHORT_WAIT_MS);
            }
        } catch (RuntimeException unavailable) {
            ReportLogger.debug("Package check failed: " + unavailable.getClass().getSimpleName());
        }
    }

    // =========================================================
    // Small helpers
    // =========================================================

    private static Pattern ci(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    /** Smallest node whose label fully matches (the label itself, not a parent that merges it). */
    private static UiNode find(List<UiNode> nodes, Pattern pattern) {
        UiNode best = null;
        for (UiNode node : nodes) {
            if (!node.label.isEmpty() && pattern.matcher(node.label).matches()
                    && (best == null || node.area() < best.area())) best = node;
        }
        return best;
    }

    private static boolean any(List<UiNode> nodes, Pattern pattern) {
        return find(nodes, pattern) != null;
    }

    private static List<String> labelsOf(List<UiNode> nodes) {
        List<String> labels = new ArrayList<>();
        for (UiNode node : nodes) if (!node.label.isEmpty() && !labels.contains(node.label)) labels.add(node.label);
        return labels;
    }

    private static Set<String> labelSet(List<UiNode> nodes) {
        return new LinkedHashSet<>(labelsOf(nodes));
    }

    private List<String> newLabels(Set<String> before) {
        return newLabels(before, snapshot());
    }

    private static List<String> newLabels(Set<String> before, List<UiNode> nodes) {
        List<String> added = new ArrayList<>();
        for (String label : labelsOf(nodes)) if (!before.contains(label)) added.add(label);
        return added;
    }

    /** Waits until the opened screen shows more than its back button (or a WebView / another app). */
    private List<UiNode> waitForDestinationContent() {
        waitUntil(() -> {
            List<UiNode> n = snapshot();
            for (UiNode node : n) {
                if (node.cls.contains("WebView")) return true;
                if (!node.label.isEmpty() && !"go back".equalsIgnoreCase(node.label)) return true;
            }
            return appPackage != null && !appPackage.equals(driver.getCurrentPackage());
        }, SHORT_WAIT_MS);
        List<UiNode> nodes = settledSnapshot();
        boolean webView = false;
        for (UiNode node : nodes) if (node.cls.contains("WebView")) { webView = true; break; }
        if (webView && articleTitle(nodes).equals("[]")) {
            ReportLogger.info("Article is shown in a WebView whose text is not exposed to the native tree");
        }
        return nodes;
    }

    private String articleTitle(List<UiNode> nodes) {
        List<UiNode> top = new ArrayList<>();
        for (UiNode node : nodes) {
            if (!node.label.isEmpty() && !"Go back".equalsIgnoreCase(node.label) && node.cy() < screen().getHeight() * 0.4) top.add(node);
        }
        top.sort(Comparator.comparingInt((UiNode n) -> n.top));
        return abbreviate(labelsOf(top), 3);
    }

    private static String fingerprint(List<UiNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (UiNode node : nodes) if (!node.label.isEmpty()) sb.append(node.label).append('@').append(node.top).append('|');
        return sb.toString();
    }

    private static String signature(List<UiNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (UiNode node : nodes) {
            sb.append(node.label).append('@').append(node.left).append(',').append(node.top).append('|');
        }
        return sb.toString();
    }

    private static String keysOf(List<UiNode> nodes) {
        return String.join("|", labelsOf(nodes));
    }

    private String visibleLabels() {
        try {
            return abbreviate(labelsOf(snapshot()), 30);
        } catch (RuntimeException unavailable) {
            return "<page source unavailable: " + unavailable.getClass().getSimpleName() + ">";
        }
    }

    private static String abbreviate(List<String> labels, int max) {
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        for (String label : labels) {
            if (count++ == max) { sb.append("... +").append(labels.size() - max).append(" more"); break; }
            if (count > 1) sb.append("; ");
            sb.append(abbreviateText(label, 60));
        }
        return sb.append(']').toString();
    }

    private static final Pattern SECRET_QUERY = Pattern.compile("(?i)([?&](token|auth|session|sid|key|otp|code)[^=&\\s]*=)[^&\\s]+");

    private static String abbreviateText(String text, int max) {
        // Run 1: a Chrome URL with an auth token reached the HTML report; never log secrets.
        String safe = SECRET_QUERY.matcher(text).replaceAll("$1***");
        return safe.length() <= max ? safe : safe.substring(0, max) + "...";
    }

    /** Straightens curly quotes and collapses whitespace (incl. new lines) so labels compare reliably. */
    private static String normalize(String value) {
        if (value == null) return "";
        return value.replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u201C', '"').replace('\u201D', '"')
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    // =========================================================
    // Data holders
    // =========================================================

    private static final class UiNode {
        final String label;
        final String cls;
        final int left;
        final int top;
        final int right;
        final int bottom;
        final boolean clickable;
        final boolean selected;
        final boolean checkable;

        UiNode(String label, String cls, int left, int top, int right, int bottom,
               boolean clickable, boolean selected, boolean checkable) {
            this.label = label;
            this.cls = cls;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.clickable = clickable;
            this.selected = selected;
            this.checkable = checkable;
        }

        int cx() { return (left + right) / 2; }
        int cy() { return (top + bottom) / 2; }
        int width() { return right - left; }
        int height() { return bottom - top; }
        long area() { return (long) width() * height(); }

        boolean contains(UiNode other) {
            return left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom;
        }
    }

    private static final class SectionCards {
        final Set<String> labels = new LinkedHashSet<>();
    }

    /**
     * Portfolio card numbers. XML: 1D amount and percent share one node ("\u20B9+6,297 0.83%");
     * the portfolio total is only used when the app exposes it as a separate amount node.
     */
    private static final class PortfolioBlock {
        final String text;
        final boolean hasDayLabel;
        final boolean valueElement;
        final String portfolioAmount;
        final String dayAmount;
        final String dayPercent;

        PortfolioBlock(List<String> labels, boolean hasDayLabel, boolean valueElement) {
            this.text = String.join(" | ", labels);
            this.hasDayLabel = hasDayLabel;
            this.valueElement = valueElement;
            String total = null;
            String day = null;
            String percent = null;
            for (String label : labels) {
                String amount = firstMatch(AMOUNT, label);
                String pct = firstMatch(PERCENT, label);
                if (pct != null && percent == null) percent = pct;
                if (amount == null) continue;
                boolean dayNode = pct != null || ONE_D_CHANGE_FIND.matcher(label).find() || amount.matches(".*[+-].*");
                if (dayNode && day == null) day = amount;
                else if (!dayNode && total == null) total = amount;
            }
            this.portfolioAmount = total;
            this.dayAmount = day;
            this.dayPercent = percent;
        }

        boolean allShown() {
            return hasDayLabel && dayAmount != null && dayPercent != null;
        }

        boolean allHidden() {
            return portfolioAmount == null && dayAmount == null && dayPercent == null;
        }

        private static String firstMatch(Pattern pattern, String value) {
            Matcher m = pattern.matcher(value);
            return m.find() ? m.group().trim() : null;
        }
    }
}