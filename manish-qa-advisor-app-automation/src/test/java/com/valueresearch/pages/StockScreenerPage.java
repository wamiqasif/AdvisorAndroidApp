package com.valueresearch.pages;

import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockScreenerPage {

    public static final String BUILD_VERSION = "STOCK_SCREENER_V29_20261113";

    private static final long SHORT_WAIT_MS = 12_000L;
    private static final long PAGE_READY_WAIT_MS = 25_000L;
    private static final long POLL_MS = 350L;

    // After one "Go back" tap, how long to wait for Hub before tapping again.
    private static final long BACK_TRANSITION_WAIT_MS = 5_000L;
    private static final int MAX_BACK_TAPS = 4;

    // "UI settled" waits used after swipes instead of fixed sleeps.
    private static final long SETTLE_TIMEOUT_MS = 3_000L;
    private static final long SETTLE_POLL_MS = 250L;

    private static final long TAB_SELECT_WAIT_MS = 6_000L;
    private static final long TAB_RETAP_AFTER_MS = 2_500L;
    private static final long TAB_CONTENT_CHANGE_WAIT_MS = 4_000L;
    private static final int PREDEFINED_MAX_SWIPES = 30;

    // TC5/TC6: finding a predefined card and tapping it safely.
    private static final int PREDEFINED_CARD_MAX_SWIPES = 20;
    // A card is tapped only when its centre is above this fraction of the screen
    // height, so the tap never lands on a bottom overlay.
    private static final double CARD_TAP_BAND_BOTTOM = 0.80;

    // TC9-TC12: horizontal scrolling of the stock table header.
    private static final int TABLE_MAX_SWIPES_PER_DIRECTION = 15;
    private static final long TABLE_DRAG_MS = 600L;
    private static final long TABLE_DRAG_HOLD_MS = 250L;
    // Fraction of the scrollable header width moved per drag (the finger holds before lifting, so no fling).
    private static final double TABLE_DRAG_FRACTION = 0.85;
    private static final Pattern PAGE_SOURCE_BOUNDS = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]");

    // ---------------------------------------------------------
    // Hub / navigation
    // ---------------------------------------------------------
    private static final By HUB_TAB = AppiumBy.accessibilityId("Hub");
    private static final By HUB_SCROLL_VIEW = AppiumBy.className("android.widget.ScrollView");
    private static final By STOCK_SCREENER_TILE = AppiumBy.accessibilityId("Open stock screener");
    private static final By SCREENER_HEADING = AppiumBy.accessibilityId("Stock Screener");
    private static final By BACK_BUTTON = AppiumBy.accessibilityId("Go back");
    private static final By HUB_OPEN_BUTTONS = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Open \")");
    private static final By HUB_SECTIONS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionMatches(\"^(Profile|Investor Accounts|Mutual Funds|Stocks|More)$\")");
    private static final String HUB_TILE_PREFIX = "Open ";

    // Fallback for collecting labels when the page source cannot be parsed.
    // (?s) lets ".+" also match multi-line content-desc values.
    private static final By ANY_DESCRIBED_NODE = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"(?s).+\")");

    // ---------------------------------------------------------
    // Stock Screener landing
    // ---------------------------------------------------------
    private static final By PREDEFINED_TAB = AppiumBy.accessibilityId("Predefined, tab 1 of 2");
    private static final By SAVED_TAB = AppiumBy.accessibilityId("Saved, tab 2 of 2");
    private static final By CREATE_NEW_SCREEN_BUTTON = AppiumBy.accessibilityId("Create New Screen");

    // ---------------------------------------------------------
    // Attractive Bluechips (opened from Create New Screen)
    // ---------------------------------------------------------
    private static final By ATTRACTIVE_BLUECHIPS_HEADING = AppiumBy.accessibilityId("Attractive Bluechips");
    // Matches "8 Stocks Filtered", "1 Stock Filtered", "1,204 Stocks Filtered".
    // [0-9,] is used instead of \d so no extra regex escaping is needed inside UiSelector.
    private static final By STOCKS_FILTERED_LABEL = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"^[0-9,]+ Stocks? Filtered$\")");
    private static final By HORIZONTAL_SCROLL_VIEW = AppiumBy.className("android.widget.HorizontalScrollView");
    private static final By FILTERS_BUTTON = AppiumBy.accessibilityId("Filters");
    // Fixed first column of every detail-screen table; its row is the header row.
    private static final By SECURITIES_HEADER = AppiumBy.accessibilityId("Securities");

    // TC13-TC17: Filters sheet ("Add Filter" page opened from the Filters button).
    // "1 Filter Applied", "4 Filters Applied", "36 Filters Applied"
    private static final By FILTERS_APPLIED_LABEL = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"^[0-9]+ Filters? Applied$\")");
    private static final Pattern FILTERS_APPLIED_TEXT = Pattern.compile("^(\\d+) Filters? Applied$");
    // Multi-select filters appear as one node: "By Capitalisation\n1 Selected" (normalised to one line).
    private static final Pattern SELECTION_FILTER_TEXT = Pattern.compile("^(.+?) (\\d+) Selected$");
    private static final int FILTER_SHEET_MAX_SWIPES = 10;
    // Expected value for a filter box whose value comes from market data (not asserted).
    private static final String ANY = "*";

    // TC17-TC20 (lapsed user): subscription page, in-screen nudge and the screen-actions drawer.
    private static final By SUBSCRIPTION_HEADLINE = AppiumBy.accessibilityId(
            "No tips. No targets. No predictions. Just research.");
    private static final By SUBSCRIBE_BUTTON = AppiumBy.accessibilityId("Subscribe");
    // On the Saved tab the nudge is one node ("...Stock Advisor.\nUnlock full..."); on Custom View it is
    // two nodes, so "contains" matches both layouts.
    private static final By LIMITED_VERSION_NUDGE = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"You are viewing a limited version of Stock Advisor\")");
    private static final By UNLOCK_FULL_DATA_NUDGE = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Unlock full stock data, ratings, and filters trusted by long-term investors\")");
    private static final By SUBSCRIBE_NOW_BUTTON = AppiumBy.accessibilityId("Subscribe Now");
    // Drawer item is an ImageView; the "Name Your Screen" dialog's button is a View with the same label.
    private static final By DRAWER_SAVE_SCREEN = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Save Screen\")");
    private static final By DRAWER_EXPORT = AppiumBy.accessibilityId("Export");
    // The three-dots button exposes no content-desc; it sits at the right end of the heading row.
    private static final double THREE_DOTS_X_FRACTION = 0.92;

    // SS_013: By Sector picker ("Add Filter" page with Search filters / Apply Filters), closed with device Back.
    private static final By APPLY_FILTERS_BUTTON = AppiumBy.accessibilityId("Apply Filters");
    private static final int SECTOR_PICKER_MAX_SWIPES = 8;

    // SS_022: pull-to-refresh indicator on a screener detail screen ("75, Refresh" while pulling).
    private static final By REFRESH_INDICATOR = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Refresh\")");
    private static final long REFRESH_INDICATOR_WAIT_MS = 2_500L;

    // SS_023: building and saving a screen.
    private static final By ADD_FILTER_BUTTON = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Add Filter\")");
    private static final By OWNERSHIP_CATEGORY = AppiumBy.accessibilityId("Ownership");
    private static final By PROMOTERS_STAKE = AppiumBy.accessibilityId("Promoter's Stake");
    private static final By INSTITUTIONAL_STAKE = AppiumBy.accessibilityId("Institutional Stake");
    private static final By FILTER_DRAWER_APPLY = AppiumBy.accessibilityId("Apply");
    private static final By REVIEW_FILTERS_TITLE = AppiumBy.accessibilityId("Review Filters");
    private static final By UPDATE_SCREEN_BUTTON = AppiumBy.accessibilityId("Update Screen");
    private static final By NAME_YOUR_SCREEN_TITLE = AppiumBy.accessibilityId("Name Your Screen");
    private static final By SCREEN_NAME_INPUT = AppiumBy.className("android.widget.EditText");
    private static final By SAVE_SCREEN_DIALOG_BUTTON = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").description(\"Save Screen\")");
    private static final By NEW_RECORD_ADDED_POPUP = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"New Record Added\")");
    // A newly saved screen can take a while to reach the Saved tab: wait this long before
    // the first refresh, and this long after each refresh.
    private static final long SAVED_TAB_INITIAL_WAIT_MS = 30_000L;
    private static final long SAVED_TAB_AFTER_REFRESH_WAIT_MS = 10_000L;
    // Total time allowed for a new saved screen to show up, refreshes included.
    private static final long SAVED_TAB_MAX_WAIT_MS = 180_000L;
    private static final int SAVED_LIST_MAX_SWIPES = 10;
    // The app's generic error page ("... spilled everything. We're picking up the pieces ...").
    private static final By APP_ERROR_PAGE = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"picking up the pieces\")");
    private static final Set<String> BOTTOM_NAV_LABELS = Set.of("Portfolio", "Funds", "Stocks", "Hub");

    // SS_036: Review Filters "Reset all" (works only before Update Screen).
    private static final By RESET_ALL_BUTTON = AppiumBy.accessibilityId("Reset all");
    private static final By REVIEW_STOCK_RATING_ROW = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Stock Rating\")");

    // SS_038: Export dialog ("<file>.xlsx", "22.9 KB downloaded", Cancel, Open).
    private static final By EXPORT_FILE_NAME = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"(?i).+\\\\.xlsx?$\")");
    private static final By EXPORT_DOWNLOADED_LABEL = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"downloaded\")");
    private static final By EXPORT_CANCEL = AppiumBy.accessibilityId("Cancel");
    private static final By EXPORT_OPEN = AppiumBy.accessibilityId("Open");
    private static final DateTimeFormatter EXPORT_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    // SS_028 / SS_029: Custom View -> Columns -> Add Columns (closed with the device Back key).
    private static final By COLUMNS_BUTTON = AppiumBy.accessibilityId("Columns");
    private static final By ADD_COLUMNS_BUTTON = AppiumBy.accessibilityId("Add Columns");
    private static final By APPLY_COLUMNS_BUTTON = AppiumBy.accessibilityId("Apply Columns");
    private static final By UPDATE_COLUMNS_BUTTON = AppiumBy.accessibilityId("Update Columns");
    private static final int ADD_COLUMNS_MAX_SWIPES = 12;

    // SS_026: Stock Rating column values.
    private static final By STOCK_RATING_HEADER = AppiumBy.accessibilityId("Stock Rating");
    private static final int TABLE_ROWS_MAX_SWIPES = 25;

    private static final List<DetailTab> DETAIL_TABS = List.of(
            new DetailTab("Overview", 1, "Overview, tab 1 of 6"),
            new DetailTab("Returns", 2, "Returns, tab 2 of 6"),
            new DetailTab("Valuation", 3, "Valuation, tab 3 of 6"),
            new DetailTab("Per share data", 4, "Per share data, tab 4 of 6"),
            new DetailTab("Stock Rating", 5, "Stock Rating, tab 5 of 6"),
            new DetailTab("Custom View", 6, "Custom View, tab 6 of 6")
    );
    private static final Set<String> DETAIL_TAB_IDS = detailTabIds();
    private static final DetailTab OVERVIEW_TAB = DETAIL_TABS.get(0);
    private static final DetailTab STOCK_RATING_TAB = DETAIL_TABS.get(4);

    /**
     * SS_009-SS_012 / SS_025: exact columns of every tab, including the ₹ / % / CR units.
     * Matching ignores case, spaces and dots; a two-line header matches on its first line
     * ("Price (₹)\nDD-MMM HH:MM" -> "Price (₹)"). Extra columns are reported too, so a
     * removed column coming back (e.g. Neff Total Return on Valuation) fails the test.
     */
    private static final Map<DetailTab, List<String>> EXPECTED_COLUMNS = expectedColumns();

    /** SS_027: Custom View columns per predefined screen (confirmed against the VRO site, 3 Nov). */
    private static final Map<String, List<String>> CUSTOM_VIEW_COLUMNS = customViewColumns();

    /** SS_013: screens whose "N Filters Applied" count is asserted, and their By Sector picker selection. */
    private static final Map<String, Integer> EXPECTED_APPLIED_COUNT = Map.of("Top Rated Banks", 6);
    private static final Map<String, Map<String, Set<String>>> EXPECTED_SECTOR_SELECTION = Map.of(
            "Top Rated Banks", Map.of("Financial", Set.of(
                    "Banks - Diversified", "Corporate Banks", "Consumer Banks", "Small Finance Banks")));

    /**
     * TC13-TC16: default filters per predefined screen (sheet + app values).
     * range(name, from) = only the "from" box checked; range(name, from, to) = both boxes
     * ("" = empty box, ANY = market-data value); present(name) = presence only;
     * selection(name) = multi-select filter shown as "<name> N Selected" (N not asserted).
     */
    private static final Map<String, List<FilterSpec>> DEFAULT_FILTERS = defaultFilters();

    // ---------------------------------------------------------
    // TC3/TC5/TC6: Predefined list content.
    // Titles are matched exactly; descriptions by their opening words, so a
    // copy edit later in a long paragraph does not break the test.
    // The optional third argument is the tab a screen must open on
    // (default: Overview).
    // ---------------------------------------------------------
    // Seasonal is fixed in iteration 2 and included in every check again.
    private static final boolean SEASONAL_ENABLED = true;

    private static final List<PredefinedSection> PREDEFINED_SECTIONS = List.of(
            new PredefinedSection("Value Research Ratings", List.of(
                    card("Top Rated Stocks", "All stocks with 5-star Value Research Ratings", STOCK_RATING_TAB),
                    card("High Quality Small Caps", "Small-cap stocks rated high on quality score"),
                    card("High Growth Large Caps", "Large-cap stocks with a high growth score"),
                    card("Value Meets Momentum", "Top momentum stocks with a high valuation score"),
                    card("Top Rated Banks", "Banks that rate high on Value Research Stock Ratings", STOCK_RATING_TAB)
            )),
            new PredefinedSection("Momentum Picks", List.of(
                    card("Top Momentum Stocks", "All Stocks witnessing the highest upward momentum."),
                    card("High Momentum Large Caps", "Large-cap stocks with high momentum score."),
                    card("High Momentum Mid Caps", "Mid-cap stocks with high momentum score."),
                    card("High Momentum Small Caps", "Small-cap stocks with high momentum score.")
            )),
            new PredefinedSection("Investment Ideas", List.of(
                    card("Attractive Bluechips", "Blue-chip stocks the largest, most consistently profitable"),
                    card("Book Value Discount", "Book value is an accountant's measure of the worth"),
                    card("Growth at Reasonable PE", "Why choose between chocolate and vanilla"),
                    card("High Dividend Yield", "'Cows for their milk, hens for their eggs"),
                    card("Cheap Quality Stocks", "Safety first. This filters uses our essential-checks"),
                    card("Small Cap Growth", "'Small is beautiful' as the title of a popular economics work")
            )),
            new PredefinedSection("Value guru screens", List.of(
                    // Quotes per the VRO site (updated 5 Nov); matched on their opening words.
                    card("Ben Graham", "Chief losses to investors come from the purchase of"),
                    card("Joel Greenblatt", "The strategy of putting all your eggs in"),
                    card("John Neff", "Successful stocks don't tell you when to sell"),
                    card("Peter Lynch", "Go for a business that any idiot can run"),
                    card("Walter Schloss", "When a stock trades below its working capital")
            )),
            // Seasonal is excluded from every check until its app issues are fixed;
            // set the flag to true to bring it back into SS_003 and the section tests.
            new PredefinedSection("Seasonal", SEASONAL_ENABLED, List.of(
                    card("at 52 Week High", "Stocks that have touched their 52 week Highs"),
                    card("at 3 Year High", "Stocks within 5 percent of their 3 year high"),
                    card("at 5 Year High", "Stocks within 5 percent of their 5 year high"),
                    card("at All Time High", "Stocks within 5 percent of their all time high"),
                    card("at 52 Week Low", "Stocks within 5 percent of their 52 week low"),
                    card("at 3 Year Low", "Stocks within 5 percent of their 3 year low"),
                    card("at 5 Year Low", "Stocks within 5 percent of their 5 year low"),
                    card("at All Time Low", "Stocks within 5 percent of their all time low")
            ))
    );

    private final AndroidDriver driver;
    private final AdvisorAccountSession preparedSession;
    // Which row scrolls the stock table sideways; detected on the first table, then reused.
    private Boolean tableScrollsOnBody;

    public StockScreenerPage(AndroidDriver driver) {
        this(driver, null);
    }

    public StockScreenerPage(AndroidDriver driver, AdvisorAccountSession preparedSession) {
        this.driver = Objects.requireNonNull(driver, "AndroidDriver is required");
        if (preparedSession != null && !preparedSession.isForDriver(driver)) {
            throw new IllegalArgumentException("Prepared account session belongs to another driver");
        }
        this.preparedSession = preparedSession;
    }

    // =========================================================
    // TC1: Open Stock Screener / heading / back to Hub
    // =========================================================

    public void openFromHub() {
        ReportLogger.step("[" + BUILD_VERSION + "] Opening Stock Screener from Hub");
        String stage = "checking driver and prepared account";
        try {
            assertDriverAlive("before Stock Screener navigation");
            if (preparedSession != null) preparedSession.assertPrepared();

            stage = "checking current screen";
            if (isOnStockScreenerPage()) {
                ReportLogger.pass("Stock Screener is already open");
                return;
            }

            if (preparedSession == null) {
                throw new IllegalStateException("Prepared AdvisorAccountSession is required for Stock Screener navigation");
            }

            stage = "opening Hub";
            if (isHubScreenReady()) {
                ReportLogger.debug("Hub is already active - skipping redundant Hub tab tap");
            } else {
                WebElement hub = bottomMostVisible(HUB_TAB);
                if (hub == null) {
                    throw new AssertionError("Hub bottom-navigation tab is not visible");
                }
                safeClick(hub, "Hub bottom-navigation tab");
                if (!waitForHubContainerAfterNavigation(SHORT_WAIT_MS)) {
                    throw new AssertionError(
                            "Hub was tapped but its scroll container did not become visible. visible="
                                    + collectVisibleValues()
                    );
                }
            }

            stage = "scrolling Hub to locate Open stock screener";
            ReportLogger.step("Hub is ready for scrolling; locating Open stock screener tile");
            WebElement tile = findStockScreenerTileWithBoundedScroll();
            if (tile == null) {
                throw new AssertionError(
                        "Stock Screener Hub tile (accessibility id: Open stock screener) was not found after bounded scrolling. visible="
                                + collectVisibleValues()
                );
            }

            stage = "tapping Open stock screener";
            ReportLogger.step("Stock Screener Hub tile found; opening it");
            safeClick(tile, "Stock Screener Hub tile");

            stage = "waiting for Stock Screener heading";
            requireVisible(SCREENER_HEADING, "Stock Screener heading", PAGE_READY_WAIT_MS);
            ReportLogger.pass("Stock Screener page opened successfully");
        } catch (RuntimeException | AssertionError failure) {
            ReportLogger.fail("[" + BUILD_VERSION + "] Navigation failed while " + stage
                    + ": " + failure.getClass().getSimpleName() + " - " + clean(failure.getMessage()));
            throw failure;
        }
    }

    public void verifyHeading() {
        ReportLogger.step("Verifying Stock Screener heading");
        requireVisible(SCREENER_HEADING, "Stock Screener heading", PAGE_READY_WAIT_MS);
        ReportLogger.pass("Stock Screener heading is visible");

        // Log tabs as info only (not asserting)
        logTabsIfPresent();
    }

    /**
     * Taps "Go back" until Hub is visible (max 3 taps).
     * Uses the instant isHubScreenReady() check, so no time is lost when Hub is
     * not showing, and it can unwind 2-3 screens when @AfterMethod recovers
     * from a failure deep inside the module.
     */
    public void navigateBackToHub() {
        ReportLogger.step("Navigating back to Hub via Go back button");
        for (int tap = 1; tap <= MAX_BACK_TAPS; tap++) {
            // Returns as soon as Hub, a Back button or an overlay without one is on screen.
            waitUntil(() -> isHubScreenReady() || isVisible(BACK_BUTTON) || isOverlayWithoutBackButton(), SHORT_WAIT_MS);
            if (isHubScreenReady()) {
                ReportLogger.pass("Hub is visible");
                return;
            }
            if (isOverlayWithoutBackButton()) {
                // Subscription page, actions drawer or Filters sheet: only the device Back key closes them.
                ReportLogger.step("Closing overlay with the device Back key (step " + tap + ")");
                driver.navigate().back();
                waitForStableNodes(desc -> true);
                continue;
            }
            WebElement back = firstDisplayed(BACK_BUTTON);
            if (back == null) {
                throw new AssertionError("Go back button not found on current screen; visible="
                        + collectVisibleValues());
            }
            safeClick(back, "Go back button (tap " + tap + ")");
            if (waitForHubContainerAfterNavigation(BACK_TRANSITION_WAIT_MS)) {
                ReportLogger.pass("Returned to Hub");
                return;
            }
        }
        throw new AssertionError("Hub not reached after " + MAX_BACK_TAPS + " Go back taps; visible="
                + collectVisibleValues());
    }

    /**
     * The Stock Screener landing page (heading plus the Predefined tab). A customised or
     * refreshed screener detail screen is also headed "Stock Screener", so the heading
     * alone is not enough.
     */
    public boolean isOnStockScreenerPage() {
        return isVisible(SCREENER_HEADING) && isVisible(PREDEFINED_TAB);
    }

    private boolean isOverlayWithoutBackButton() {
        return isVisible(SUBSCRIBE_BUTTON) || isActionsDrawerOpen() || isVisible(FILTERS_APPLIED_LABEL)
                || isVisible(NAME_YOUR_SCREEN_TITLE) || isVisible(APPLY_COLUMNS_BUTTON) || isVisible(ADD_COLUMNS_BUTTON);
    }

    private boolean isActionsDrawerOpen() {
        return isVisible(DRAWER_SAVE_SCREEN) || isVisible(DRAWER_EXPORT);
    }

    // =========================================================
    // TC2: Predefined/Saved tabs and Create New Screen
    // =========================================================

    public void verifyPredefinedAndSavedTabs() {
        ReportLogger.step("Verifying Predefined and Saved tabs");
        requireVisible(PREDEFINED_TAB, "Predefined tab", PAGE_READY_WAIT_MS);
        requireVisible(SAVED_TAB, "Saved tab", PAGE_READY_WAIT_MS);
        ReportLogger.pass("Predefined and Saved tabs are visible");
    }

    public void clickCreateNewScreen() {
        ReportLogger.step("Clicking Create New Screen button under Predefined tab");
        WebElement createBtn = requireVisible(CREATE_NEW_SCREEN_BUTTON, "Create New Screen button", PAGE_READY_WAIT_MS);
        safeClick(createBtn, "Create New Screen button");
        ReportLogger.pass("Create New Screen button clicked");
    }

    public void verifyAttractiveBluechipsScreen() {
        ReportLogger.step("Verifying Attractive Bluechips screen elements");
        requireVisible(ATTRACTIVE_BLUECHIPS_HEADING, "Attractive Bluechips heading", PAGE_READY_WAIT_MS);
        verifyStocksFilteredCount(1);
        ReportLogger.pass("Attractive Bluechips screen verified with heading and filtered count");
    }

    /**
     * Verifies the "N Stocks Filtered" label without hardcoding N (it changes
     * with market data) and returns N.
     */
    public int verifyStocksFilteredCount(int minimumExpected) {
        WebElement label = requireVisible(STOCKS_FILTERED_LABEL, "'<N> Stocks Filtered' label", PAGE_READY_WAIT_MS);
        String text = safeAttribute(label, "content-desc");
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new AssertionError("Could not read the stock count from: '" + text + "'");
        }
        int count = Integer.parseInt(digits);
        if (count < minimumExpected) {
            throw new AssertionError("Expected at least " + minimumExpected + " filtered stock(s) but got: '" + text + "'");
        }
        ReportLogger.pass("Stocks filtered count verified: " + text);
        return count;
    }

    public void navigateBackFromAttractiveBluechips() {
        navigateBackToStockScreener("Attractive Bluechips");
    }

    /** Taps Go back once on a screener detail screen and waits for Stock Screener (or Hub). */
    private void navigateBackToStockScreener(String fromScreen) {
        ReportLogger.step("Navigating back from " + fromScreen + " to Stock Screener");
        WebElement back = requireVisible(BACK_BUTTON, "Go back button on " + fromScreen + " screen", SHORT_WAIT_MS);
        safeClick(back, "Go back button from " + fromScreen);

        waitUntil(() -> isOnStockScreenerPage() || isHubScreenReady(), PAGE_READY_WAIT_MS);
        if (isOnStockScreenerPage()) {
            // Let the route transition finish so the next tap is not swallowed.
            waitForStableNodes(desc -> true);
            ReportLogger.pass("Returned to Stock Screener page");
        } else if (isHubScreenReady()) {
            ReportLogger.info("Back from " + fromScreen + " returned directly to Hub");
        } else {
            throw new AssertionError("Back from " + fromScreen + " reached neither Stock Screener nor Hub; visible="
                    + collectVisibleValues());
        }
    }

    // =========================================================
    // TC4: Attractive Bluechips detail screen - open all tabs
    // =========================================================

    public void verifyAttractiveBluechipsTabsAndFilters() {
        ReportLogger.step("Opening every Attractive Bluechips tab and verifying the Filters button");

        // Verify the detail screen before touching its tab rail.
        requireVisible(ATTRACTIVE_BLUECHIPS_HEADING, "Attractive Bluechips heading", PAGE_READY_WAIT_MS);
        ReportLogger.pass("Attractive Bluechips heading verified");

        verifyStocksFilteredCount(1);

        boolean selectionSupported = isTabSelectionStateSupported();

        // Tabs 5 and 6 sit off-screen on a phone viewport, so every tab must be
        // brought into the rail viewport before it can be tapped.
        for (DetailTab tab : DETAIL_TABS) {
            openDetailTab(tab, selectionSupported);
        }

        // Return to the default tab so the Filters action is checked from a stable state.
        openDetailTab(DETAIL_TABS.get(0), selectionSupported);

        // Verify Filters button (3 dots) - just verify presence, don't click
        requireVisible(FILTERS_BUTTON, "Filters button (3 dots) on Attractive Bluechips screen", SHORT_WAIT_MS);
        ReportLogger.pass("Filters button (3 dots) verified present (not clicked)");

        ReportLogger.pass("All 6 Attractive Bluechips tabs opened and Filters button verified");
    }

    private void openDetailTab(DetailTab tab, boolean selectionSupported) {
        openDetailTab(tab, selectionSupported, true);
    }

    /**
     * needContentEvidence=false is used by the column checks: the expected columns
     * themselves prove which tab is open, so the up-to-4 s "content changed" wait is skipped.
     */
    private void openDetailTab(DetailTab tab, boolean selectionSupported, boolean needContentEvidence) {
        // The rail auto-scrolls after every tab change; tapping while it moves can miss.
        waitForStableNodes(DETAIL_TAB_IDS::contains);
        WebElement element = revealDetailTab(tab);

        if (selectionSupported) {
            safeClick(element, "Detail tab: " + tab.label);
            if (!waitUntil(() -> isTabSelected(firstDisplayed(tab.locator)), TAB_RETAP_AFTER_MS)) {
                ReportLogger.info("Tab '" + tab.label + "' not selected after the first tap; tapping it once more");
                waitForStableNodes(DETAIL_TAB_IDS::contains);
                assertStillOnDetailScreen("tapping the '" + tab.label + "' tab");
                safeClick(revealDetailTab(tab), "Detail tab (retry): " + tab.label);
            }
            waitForDetailTabSelected(tab);
            return;
        }

        if (!needContentEvidence) {
            safeClick(element, "Detail tab: " + tab.label);
            waitForStableNodes(DETAIL_TAB_IDS::contains);
            return;
        }
        // This build does not expose the selected state, so use the table content
        // as evidence instead of a fixed sleep: wait until it changes after the tap.
        Set<String> before = contentLabelsOutsideTabRail();
        safeClick(element, "Detail tab: " + tab.label);
        boolean changed = waitUntil(() -> !contentLabelsOutsideTabRail().equals(before), TAB_CONTENT_CHANGE_WAIT_MS);
        if (changed) {
            ReportLogger.pass("Tab opened: " + tab.label + " (screen content changed)");
        } else {
            ReportLogger.info("Tab tapped: " + tab.label + " (content unchanged - tab was probably already active;"
                    + " this build does not expose tab selected state)");
        }
    }

    private Set<String> contentLabelsOutsideTabRail() {
        Set<String> labels = new LinkedHashSet<>();
        for (UiNode node : displayedDescribedNodes(desc -> !DETAIL_TAB_IDS.contains(desc))) {
            labels.add(node.desc);
        }
        return labels;
    }

    private WebElement revealDetailTab(DetailTab tab) {
        WebElement element = firstDisplayed(tab.locator);
        if (element != null && isTabFullyExposed(element)) {
            return element;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            List<Integer> visibleIndexes = visibleDetailTabIndexes();
            if (visibleIndexes.isEmpty()) {
                // No tab rail at all: never swipe blindly on whatever screen this is.
                assertStillOnDetailScreen("looking for the '" + tab.label + "' tab");
            }
            boolean moveToLaterTabs;

            if (visibleIndexes.isEmpty()) {
                moveToLaterTabs = true;
            } else {
                int min = visibleIndexes.get(0);
                int max = visibleIndexes.get(visibleIndexes.size() - 1);
                if (tab.index < min) {
                    moveToLaterTabs = false;
                } else if (tab.index > max) {
                    moveToLaterTabs = true;
                } else {
                    moveToLaterTabs = tab.index >= ((min + max) / 2);
                }
            }

            ReportLogger.debug("Revealing tab '" + tab.label + "' with horizontal rail swipe toward "
                    + (moveToLaterTabs ? "later" : "earlier") + " tabs (attempt " + (attempt + 1) + ")");
            swipeDetailTabRail(moveToLaterTabs);
            waitForStableNodes(DETAIL_TAB_IDS::contains);

            element = firstDisplayed(tab.locator);
            if (element != null && isTabFullyExposed(element)) {
                return element;
            }
        }

        element = firstDisplayed(tab.locator);
        if (element != null) {
            ReportLogger.info("Tab '" + tab.label + "' is visible but still partially clipped after bounded scrolling");
            return element;
        }

        throw new AssertionError("Unable to reveal Attractive Bluechips tab '" + tab.label
                + "' after bounded horizontal scrolling; visible tabs=" + visibleDetailTabLabels());
    }

    /** Fails clearly when the app has left the screener's detail screen (no "N Stocks Filtered" label). */
    private void assertStillOnDetailScreen(String during) {
        if (!isVisible(STOCKS_FILTERED_LABEL)) {
            throw new AssertionError("The app left the screener screen while " + during + "; visible="
                    + collectVisibleValues());
        }
    }

    private List<Integer> visibleDetailTabIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (DetailTab tab : DETAIL_TABS) {
            if (firstDisplayed(tab.locator) != null) {
                indexes.add(tab.index);
            }
        }
        Collections.sort(indexes);
        return indexes;
    }

    private String visibleDetailTabLabels() {
        StringBuilder labels = new StringBuilder();
        for (DetailTab tab : DETAIL_TABS) {
            if (firstDisplayed(tab.locator) == null) {
                continue;
            }
            if (labels.length() > 0) {
                labels.append(", ");
            }
            labels.append(tab.label);
        }
        return labels.length() == 0 ? "none" : labels.toString();
    }

    private boolean isTabFullyExposed(WebElement tab) {
        Rectangle rail = findDetailTabRailRect();
        Rectangle rect = safeRect(tab);
        if (rail == null || rect == null || rect.getWidth() <= 0) {
            return true;
        }

        int visibleLeft = Math.max(rail.getX(), rect.getX());
        int visibleRight = Math.min(rail.getX() + rail.getWidth(), rect.getX() + rect.getWidth());
        int visibleWidth = Math.max(0, visibleRight - visibleLeft);

        // A tab clipped by a few pixels is still safely tappable at its center.
        return visibleWidth >= (rect.getWidth() - 16);
    }

    private Rectangle findDetailTabRailRect() {
        Dimension size = driver.manage().window().getSize();
        List<WebElement> rails = safeFindElements(HORIZONTAL_SCROLL_VIEW);

        // Prefer the HorizontalScrollView that geometrically contains a visible tab chip.
        Rectangle visibleTab = safeRect(firstVisibleDetailTab());
        if (visibleTab != null) {
            int centerY = visibleTab.getY() + (visibleTab.getHeight() / 2);
            Rectangle containingRail = null;
            for (WebElement rail : rails) {
                if (!isDisplayed(rail)) {
                    continue;
                }
                Rectangle rect = safeRect(rail);
                if (rect == null || rect.getWidth() < (size.getWidth() * 0.50)) {
                    continue;
                }
                if (centerY >= rect.getY() && centerY <= rect.getY() + rect.getHeight()
                        && (containingRail == null || rect.getHeight() < containingRail.getHeight())) {
                    containingRail = rect;
                }
            }
            if (containingRail != null) {
                return containingRail;
            }
        }

        // Fallback: the tab rail is the widest horizontal container near the
        // top of the Attractive Bluechips screen.
        Rectangle best = null;
        int maxY = (int) (size.getHeight() * 0.40);
        for (WebElement rail : rails) {
            if (!isDisplayed(rail)) {
                continue;
            }
            Rectangle rect = safeRect(rail);
            if (rect == null || rect.getWidth() < (size.getWidth() * 0.50)) {
                continue;
            }
            if (rect.getY() <= 0 || rect.getY() >= maxY) {
                continue;
            }
            if (best == null || rect.getWidth() > best.getWidth()) {
                best = rect;
            }
        }
        return best;
    }

    private WebElement firstVisibleDetailTab() {
        for (DetailTab tab : DETAIL_TABS) {
            WebElement element = firstDisplayed(tab.locator);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private void swipeDetailTabRail(boolean toLaterTabs) {
        Dimension size = driver.manage().window().getSize();
        Rectangle rail = findDetailTabRailRect();

        int y;
        int startX;
        int endX;

        if (rail != null) {
            y = rail.getY() + (rail.getHeight() / 2);
            startX = rail.getX() + (int) (rail.getWidth() * (toLaterTabs ? 0.80 : 0.20));
            endX = rail.getX() + (int) (rail.getWidth() * (toLaterTabs ? 0.20 : 0.80));
        } else {
            Rectangle visibleTab = safeRect(firstVisibleDetailTab());
            y = visibleTab == null
                    ? (int) (size.getHeight() * 0.24)
                    : visibleTab.getY() + (visibleTab.getHeight() / 2);
            startX = (int) (size.getWidth() * (toLaterTabs ? 0.80 : 0.20));
            endX = (int) (size.getWidth() * (toLaterTabs ? 0.20 : 0.80));
        }

        performSwipe(startX, y, endX, y, 420L);
    }

    private boolean isTabSelectionStateSupported() {
        // The initially active tab tells us whether this build exposes
        // Flutter tab selection semantics at all. Some Flutter builds report
        // selected=false for every chip even though the visual state is active.
        for (DetailTab tab : DETAIL_TABS) {
            if (isTabSelected(firstDisplayed(tab.locator))) {
                return true;
            }
        }
        return false;
    }

    private void waitForDetailTabSelected(DetailTab tab) {
        if (waitUntil(() -> isTabSelected(firstDisplayed(tab.locator)), TAB_SELECT_WAIT_MS)) {
            ReportLogger.pass("Tab opened and selected: " + tab.label);
            return;
        }
        throw new AssertionError("Tab '" + tab.label + "' was clicked but did not become the selected tab");
    }

    private boolean isTabSelected(WebElement element) {
        if (!isDisplayed(element)) {
            return false;
        }
        try {
            return element.isSelected() || "true".equalsIgnoreCase(safeAttribute(element, "selected"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void logTabsIfPresent() {
        try {
            WebElement predefined = findElementIfVisible(PREDEFINED_TAB);
            WebElement saved = findElementIfVisible(SAVED_TAB);
            if (predefined != null) ReportLogger.info("Predefined tab detected");
            if (saved != null) ReportLogger.info("Saved tab detected");
        } catch (Exception e) {
            ReportLogger.debug("Tab detection skipped: " + e.getMessage());
        }
    }

    // =========================================================
    // TC3: Verify all predefined screen headings by scrolling
    // =========================================================

    /**
     * One top-to-bottom pass over the Predefined list: every displayed label is
     * collected into a set until the list stops moving, then every expected
     * section heading, screen title and description is asserted. Order of cards
     * does not matter and every missing item is reported in one failure.
     */
    public void verifyAllPredefinedScreenHeadings() {
        ReportLogger.step("Verifying all predefined screen headings by scrolling");

        // Ensure we're on the Predefined tab (first tab)
        verifyPredefinedAndSavedTabs();

        Set<String> seen = collectLabelsTopToBottom();
        ReportLogger.info("Collected " + seen.size() + " labels from the Predefined list");

        List<String> allMissing = new ArrayList<>();
        for (PredefinedSection section : PREDEFINED_SECTIONS) {
            if (!section.enabled) {
                ReportLogger.info("Section skipped (disabled until app issues are fixed): " + section.heading);
                continue;
            }
            List<String> missing = new ArrayList<>();
            if (!containsExact(seen, section.heading)) {
                missing.add("section heading");
            }
            for (ScreenCard screen : section.screens) {
                if (!containsExact(seen, screen.title)) {
                    missing.add("title '" + screen.title + "'");
                }
                if (!containsPrefix(seen, screen.descriptionPrefix)) {
                    missing.add("description of '" + screen.title + "' (starts with: " + screen.descriptionPrefix + ")");
                }
            }
            if (missing.isEmpty()) {
                ReportLogger.pass("Section verified: " + section.heading + " (" + section.screens.size() + " screens)");
            } else {
                ReportLogger.fail("Section '" + section.heading + "' is missing: " + missing);
                for (String item : missing) {
                    allMissing.add(section.heading + " -> " + item);
                }
            }
        }

        if (!allMissing.isEmpty()) {
            throw new AssertionError("Predefined list is missing " + allMissing.size() + " item(s): " + allMissing
                    + " | labels collected while scrolling=" + abbreviateAll(seen));
        }
        ReportLogger.pass("All predefined screen headings and sections verified successfully");
    }

    private Set<String> collectLabelsTopToBottom() {
        Set<String> seen = new LinkedHashSet<>();
        String previousScreen = null;
        for (int swipe = 0; swipe <= PREDEFINED_MAX_SWIPES; swipe++) {
            List<UiNode> nodes = waitForStableNodes(desc -> true);
            for (UiNode node : nodes) {
                seen.add(node.desc);
            }
            String currentScreen = signature(nodes);
            if (currentScreen.equals(previousScreen)) {
                ReportLogger.step("Reached the end of the Predefined list after " + (swipe - 1) + " swipe(s)");
                return seen;
            }
            previousScreen = currentScreen;
            swipeVerticalOnView(true);
        }
        ReportLogger.info("Stopped collecting after " + PREDEFINED_MAX_SWIPES + " swipes (list kept moving)");
        return seen;
    }

    private static boolean containsExact(Set<String> seen, String expected) {
        return seen.contains(normalize(expected));
    }

    private static boolean containsPrefix(Set<String> seen, String expectedPrefix) {
        String prefix = normalize(expectedPrefix);
        for (String label : seen) {
            if (label.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String abbreviateAll(Set<String> labels) {
        List<String> shortLabels = new ArrayList<>();
        for (String label : labels) {
            shortLabels.add(label.length() > 50 ? label.substring(0, 50) + "..." : label);
        }
        return shortLabels.toString();
    }

    private void swipeVerticalOnView(boolean downFeed) {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * (downFeed ? 0.80 : 0.30));
        int endY = (int) (size.getHeight() * (downFeed ? 0.35 : 0.75));
        performSwipe(x, startY, x, endY, 500L);
    }

    // =========================================================
    // TC5/TC6: open every screen of a Predefined section
    // =========================================================

    /**
     * Opens every screen listed under the given Predefined section heading and,
     * for each one, verifies the heading and the tab the screen opens on. (The
     * "N Stocks Filtered" label is only waited for as proof the screen loaded.) Heading/tab mismatches are collected so one wrong
     * screen does not hide problems in the others; navigation failures stop the
     * test immediately.
     */
    public void verifyPredefinedSectionScreens(String sectionHeading) {
        PredefinedSection section = findSection(sectionHeading);
        ReportLogger.step("Opening every screen under '" + section.heading + "' (" + section.screens.size() + " screens)");
        verifyPredefinedAndSavedTabs();

        List<String> failures = new ArrayList<>();
        for (ScreenCard screen : section.screens) {
            if (!isOnStockScreenerPage()) {
                ReportLogger.info("Not on Stock Screener before opening '" + screen.title + "'; reopening it from Hub");
                openFromHub();
            }
            openPredefinedScreen(screen);
            try {
                verifyDetailHeading(screen.title);
                verifyOpeningTab(screen.title, screen.openingTab);
            } catch (AssertionError mismatch) {
                failures.add(screen.title + ": " + clean(mismatch.getMessage()));
                ReportLogger.fail("Screen '" + screen.title + "' check failed: " + clean(mismatch.getMessage()));
            }
            navigateBackToStockScreener(screen.title);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("'" + section.heading + "' screens failed (" + failures.size() + "/"
                    + section.screens.size() + "): " + failures);
        }
        ReportLogger.pass("All " + section.screens.size() + " '" + section.heading
                + "' screens opened with correct heading and opening tab");
    }

    private PredefinedSection findSection(String heading) {
        for (PredefinedSection section : PREDEFINED_SECTIONS) {
            if (section.heading.equalsIgnoreCase(heading.trim())) {
                if (!section.enabled) {
                    throw new IllegalStateException("Predefined section '" + section.heading
                            + "' is disabled until its app issues are fixed");
                }
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown Predefined section: " + heading);
    }

    private void openPredefinedScreen(ScreenCard screen) {
        ReportLogger.step("Opening predefined screen: " + screen.title);
        WebElement card = revealPredefinedCard(screen.title);
        safeClick(card, "Predefined screen card: " + screen.title);

        // The "N Stocks Filtered" label exists only on the opened screen, not on
        // the Predefined list, so it proves navigation happened.
        waitUntil(() -> isVisible(STOCKS_FILTERED_LABEL) || isVisible(APP_ERROR_PAGE), PAGE_READY_WAIT_MS);
        if (isVisible(APP_ERROR_PAGE)) {
            throw new AssertionError("App showed its error page (\"...We're picking up the pieces...\") when opening '"
                    + screen.title + "' - app/backend error, not a test step");
        }
        requireVisible(STOCKS_FILTERED_LABEL, "'<N> Stocks Filtered' label after opening '" + screen.title + "'",
                SHORT_WAIT_MS);
        // Let the open transition and the tab-rail auto-scroll finish.
        waitForStableNodes(desc -> true);
    }

    /**
     * Scrolls the Predefined list (down first, then up) until the card title is
     * inside the tappable band below the Predefined/Saved tabs.
     */
    private WebElement revealPredefinedCard(String title) {
        By locator = AppiumBy.accessibilityId(title);
        boolean scrollDown = true;
        int reversals = 0;
        String previousScreen = signature(displayedDescribedNodes(desc -> true));

        for (int attempt = 0; attempt < PREDEFINED_CARD_MAX_SWIPES; attempt++) {
            WebElement card = firstDisplayed(locator);
            Rectangle rect = safeRect(card);
            int[] band = cardTapBand();
            if (rect != null) {
                int centerY = rect.getY() + rect.getHeight() / 2;
                if (centerY > band[0] && centerY < band[1]) {
                    return card;
                }
                // Visible but under the header or near the bottom: move it toward the middle.
                scrollDown = centerY >= band[1];
            }

            swipeVerticalOnView(scrollDown);
            String currentScreen = signature(waitForStableNodes(desc -> true));
            if (currentScreen.equals(previousScreen) && rect != null) {
                // List cannot move any further (top/bottom edge): tap where the card is.
                WebElement atEdge = firstDisplayed(locator);
                if (atEdge != null) {
                    ReportLogger.debug("Card '" + title + "' is at the list edge; tapping it in place");
                    return atEdge;
                }
            }
            if (currentScreen.equals(previousScreen) && rect == null) {
                // Hit the end of the list without finding the card: try the other direction once.
                reversals++;
                if (reversals > 1) {
                    break;
                }
                scrollDown = !scrollDown;
            }
            previousScreen = currentScreen;
        }
        throw new AssertionError("Predefined card '" + title + "' was not found in a tappable position; visible="
                + collectVisibleValues());
    }

    /** [top, bottom] Y range in which a card centre can be tapped safely. */
    private int[] cardTapBand() {
        Dimension size = driver.manage().window().getSize();
        int top = (int) (size.getHeight() * 0.25);
        Rectangle tabs = safeRect(firstDisplayed(PREDEFINED_TAB));
        if (tabs != null) {
            top = tabs.getY() + tabs.getHeight() + 10;
        }
        return new int[]{top, (int) (size.getHeight() * CARD_TAP_BAND_BOTTOM)};
    }

    private void verifyDetailHeading(String expectedTitle) {
        Set<String> labels = new LinkedHashSet<>();
        for (UiNode node : displayedDescribedNodes(desc -> !DETAIL_TAB_IDS.contains(desc))) {
            labels.add(node.desc);
        }
        if (!labels.contains(normalize(expectedTitle))) {
            throw new AssertionError("Heading '" + expectedTitle + "' is not shown on the opened screen; first labels="
                    + abbreviateFirst(labels, 12));
        }
        ReportLogger.pass("Heading verified: " + expectedTitle);
    }

    /**
     * Verifies the tab a screen opens on. If the build exposes the tab
     * "selected" state this is an exact check. Otherwise it is inferred from the
     * tab rail: the app auto-scrolls the rail to a later opening tab, which pushes
     * Overview out of view (e.g. Top Rated Stocks shows tabs 4-6 only).
     */
    private void verifyOpeningTab(String screenTitle, DetailTab expected) {
        AtomicReference<OpeningTabState> lastState = new AtomicReference<>(readOpeningTabState());
        boolean matched = waitUntil(() -> {
            OpeningTabState state = readOpeningTabState();
            lastState.set(state);
            return state.opensOn(expected);
        }, TAB_SELECT_WAIT_MS);

        if (matched) {
            // Re-check once the rail is still, so a late auto-scroll cannot slip through.
            waitForStableNodes(DETAIL_TAB_IDS::contains);
            OpeningTabState settled = readOpeningTabState();
            lastState.set(settled);
            matched = settled.opensOn(expected);
        }

        OpeningTabState state = lastState.get();
        if (!matched) {
            throw new AssertionError("'" + screenTitle + "' should open on the '" + expected.label + "' tab but "
                    + state.describe());
        }
        ReportLogger.pass("'" + screenTitle + "' opened on the '" + expected.label + "' tab ("
                + (state.selectedId != null ? "selected state" : "inferred from tab-rail position") + ")");
    }

    private OpeningTabState readOpeningTabState() {
        List<String> visible = new ArrayList<>();
        String selected = null;
        for (UiNode node : displayedDescribedNodes(DETAIL_TAB_IDS::contains)) {
            visible.add(node.desc);
            if (node.selected) {
                selected = node.desc;
            }
        }
        return new OpeningTabState(visible, selected);
    }

    private static String abbreviateFirst(Set<String> labels, int max) {
        List<String> first = new ArrayList<>();
        for (String label : labels) {
            if (first.size() == max) {
                first.add("...");
                break;
            }
            first.add(label.length() > 50 ? label.substring(0, 50) + "..." : label);
        }
        return first.toString();
    }

    // =========================================================
    // SS_021 / SS_022: stock detail page and refresh (iteration 2)
    // =========================================================

    /**
     * SS_021: Top Rated Stocks -> Stock Rating tab -> tap the first stock row. The stock
     * detail page must show that stock's name (first line of the row's label) and the
     * screener table must be gone; Go back must return to Top Rated Stocks.
     */
    public void verifyStockDetailFromTopRatedStocks() {
        ScreenCard screen = selectScreens(findSection("Value Research Ratings"), "Top Rated Stocks").get(0);
        ReportLogger.step("Opening a stock's detail page from '" + screen.title + "' / Stock Rating");
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        openDetailTab(STOCK_RATING_TAB, isTabSelectionStateSupported(), false);

        WebElement securities = requireVisible(SECURITIES_HEADER, "'Securities' table header", PAGE_READY_WAIT_MS);
        Rectangle header = safeRect(securities);
        if (header == null) {
            throw new AssertionError("Could not read the position of the 'Securities' header");
        }
        String[] row = firstStockRow(header);
        String stockName = row[0];
        int x = Integer.parseInt(row[1]);
        int y = Integer.parseInt(row[2]);
        ReportLogger.step("Tapping stock row '" + stockName + "'");
        tapAt(x, y);

        // The table may show a shortened name ("3B Blackbio Dx") while the detail page shows the
        // full one ("3B BlackBio Dx Ltd"), so the detail page must show a label starting with it.
        String nameKey = stockName.toLowerCase(Locale.ROOT);
        if (!waitUntil(() -> !isVisible(STOCKS_FILTERED_LABEL) && !displayedDescribedNodes(
                desc -> desc.toLowerCase(Locale.ROOT).startsWith(nameKey)).isEmpty(), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Stock detail page for '" + stockName + "' did not open; visible="
                    + collectVisibleValues());
        }
        waitForStableNodes(desc -> true);
        ReportLogger.pass("Stock detail page opened for '" + stockName + "'");

        WebElement back = requireVisible(BACK_BUTTON, "Go back button on the stock detail page", SHORT_WAIT_MS);
        safeClick(back, "Go back from stock detail");
        requireVisible(STOCKS_FILTERED_LABEL, "'" + screen.title + "' after Go back from the stock detail page",
                PAGE_READY_WAIT_MS);
        verifyDetailHeading(screen.title);
        ReportLogger.pass("Returned to '" + screen.title + "' from the stock detail page");
        navigateBackToStockScreener(screen.title);
    }

    /**
     * [name, centreX, centreY] of the topmost stock row in the Securities column. Supports both
     * layouts: one node "Name\nSector\nIndustry", or three stacked nodes (name, sector,
     * industry) where the name is the topmost node of the row.
     */
    private String[] firstStockRow(Rectangle securitiesHeader) {
        Document document = parseSource(driver.getPageSource());
        int headerBottom = securitiesHeader.getY() + securitiesHeader.getHeight();
        int columnRight = securitiesHeader.getX() + securitiesHeader.getWidth();
        int bottomLimit = (int) (driver.manage().window().getSize().getHeight() * CARD_TAP_BAND_BOTTOM);
        String best = null;
        int bestTop = Integer.MAX_VALUE;
        int bestX = 0;
        int bestY = 0;
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String raw = element.getAttribute("content-desc");
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (raw == null || raw.trim().isEmpty() || box == null || "false".equals(element.getAttribute("displayed"))) {
                continue;
            }
            int centerX = (box[0] + box[2]) / 2;
            int centerY = (box[1] + box[3]) / 2;
            if (box[1] >= headerBottom && centerX < columnRight && centerY < bottomLimit && box[1] < bestTop) {
                String name = raw.split("\n", 2)[0].trim();
                if (!name.isEmpty()) {
                    best = name;
                    bestTop = box[1];
                    bestX = centerX;
                    bestY = centerY;
                }
            }
        }
        if (best == null) {
            throw new AssertionError("No stock row found under the 'Securities' header; visible=" + collectVisibleValues());
        }
        return new String[]{best, String.valueOf(bestX), String.valueOf(bestY)};
    }

    /**
     * SS_022: pull-to-refresh on Attractive Bluechips. Opened from its Predefined card the
     * heading must stay "Attractive Bluechips". Opened from Create New Screen it is a general
     * screener, so after refresh the heading is "Stock Screener" (confirmed by dev, 5 Nov).
     */
    public void verifyAttractiveBluechipsHeadingAfterRefresh() {
        ReportLogger.step("Attractive Bluechips heading after pull-to-refresh, from both entry points");
        verifyPredefinedAndSavedTabs();
        List<String> failures = new ArrayList<>();

        clickCreateNewScreen();
        requireVisible(STOCKS_FILTERED_LABEL, "Attractive Bluechips (from Create New Screen)", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        // Opened from Create New Screen it is a general screener: after refresh it is headed "Stock Screener".
        checkHeadingAfterRefresh("Create New Screen", "Stock Screener", failures);
        navigateBackToStockScreener("Attractive Bluechips");

        ScreenCard card = selectScreens(findSection("Investment Ideas"), "Attractive Bluechips").get(0);
        if (!isOnStockScreenerPage()) {
            openFromHub();
        }
        openPredefinedScreen(card);
        checkHeadingAfterRefresh("Predefined card", "Attractive Bluechips", failures);
        navigateBackToStockScreener("Attractive Bluechips");

        if (!failures.isEmpty()) {
            throw new AssertionError("Heading after refresh is wrong: " + failures);
        }
        ReportLogger.pass("Headings after refresh correct: 'Stock Screener' from Create New Screen, "
                + "'Attractive Bluechips' from the Predefined card");
    }

    private void checkHeadingAfterRefresh(String entry, String expectedHeading, List<String> failures) {
        verifyDetailHeading("Attractive Bluechips");
        pullToRefresh(entry);
        By expected = AppiumBy.accessibilityId(expectedHeading);
        String other = expectedHeading.equals("Stock Screener") ? "Attractive Bluechips" : "Stock Screener";
        By unexpected = AppiumBy.accessibilityId(other);
        boolean ok = waitUntil(() -> isVisible(expected) && isVisible(STOCKS_FILTERED_LABEL) && !isVisible(unexpected),
                PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        ok = ok && isVisible(expected) && !isVisible(unexpected);
        if (ok) {
            ReportLogger.pass("Heading after refresh is '" + expectedHeading + "' (opened from " + entry + ")");
        } else {
            String failure = "opened from " + entry + ": heading after refresh is not '" + expectedHeading + "'"
                    + (isVisible(unexpected) ? " ('" + other + "' shown)" : "");
            failures.add(failure);
            ReportLogger.fail(failure + "; visible=" + collectVisibleValues());
        }
    }

    /**
     * Pull-to-refresh: drags the stock list down from just below its header. The
     * refresh indicator ("NN, Refresh") must appear, otherwise the drag is repeated
     * once; a refresh that never starts fails the step (the check would prove nothing).
     */
    private void pullToRefresh(String entry) {
        Dimension size = driver.manage().window().getSize();
        Rectangle header = safeRect(firstDisplayed(SECURITIES_HEADER));
        int startY = header == null ? (int) (size.getHeight() * 0.40) : header.getY() + header.getHeight() + 40;
        int endY = (int) (size.getHeight() * 0.85);
        int x = size.getWidth() / 2;
        for (int attempt = 1; attempt <= 2; attempt++) {
            ReportLogger.step("Pull-to-refresh on Attractive Bluechips (opened from " + entry + "), attempt " + attempt);
            performSwipe(x, startY, x, endY, 700L);
            if (waitUntil(() -> isVisible(REFRESH_INDICATOR), REFRESH_INDICATOR_WAIT_MS)) {
                waitUntil(() -> !isVisible(REFRESH_INDICATOR), PAGE_READY_WAIT_MS);
                ReportLogger.pass("Refresh started and finished");
                return;
            }
        }
        throw new AssertionError("Pull-to-refresh did not start on Attractive Bluechips (opened from " + entry
                + "): no refresh indicator seen");
    }

    // =========================================================
    // SS_023 / SS_024: saved screens (active account)
    // =========================================================

    /**
     * SS_023: Top Rated Stocks -> Filters -> Add Filter -> Ownership -> Promoter's Stake
     * and Institutional Stake (default values) -> Apply Filters -> Update Screen. The
     * heading must become "Stock Screener" with fewer stocks; then three dots -> Save
     * Screen as "screen<N>" (N > 100), the "New Record Added" popup must show, and the
     * new screen must appear in the Saved tab (pull-to-refresh until it does).
     * There is no delete flow in the app, so every run leaves one saved screen.
     * Returns the saved screen name.
     */
    public String createAndVerifySavedScreen() {
        return createSavedScreen(false);
    }

    /** Stocks Filtered count of the last screen saved by createSavedScreen (SS_037 compares it). */
    private int lastSavedScreenCount = -1;

    /**
     * SS_023 flow; with withEpsColumn the EPS column is also added to Custom View before saving
     * (SS_037). Ends on Stock Screener with the screen listed in the Saved tab.
     */
    private String createSavedScreen(boolean withEpsColumn) {
        ScreenCard screen = selectScreens(findSection("Value Research Ratings"), "Top Rated Stocks").get(0);
        ReportLogger.step("Building a custom screen from '" + screen.title + "' and saving it"
                + (withEpsColumn ? " (with the EPS column added to Custom View)" : ""));
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        int countBefore = readStocksFilteredCount();

        // Filters sheet -> Add Filter picker
        safeClick(requireVisible(FILTERS_BUTTON, "Filters button", SHORT_WAIT_MS), "Filters button");
        requireVisible(FILTERS_APPLIED_LABEL, "Filters sheet", PAGE_READY_WAIT_MS);
        safeClick(requireVisible(ADD_FILTER_BUTTON, "'Add Filter' button in the Filters sheet", SHORT_WAIT_MS),
                "Add Filter button");
        requireVisible(APPLY_FILTERS_BUTTON, "Add Filter picker (Apply Filters button)", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);

        // Ownership -> Promoter's Stake and Institutional Stake with their default ranges
        safeClick(revealInPickerCategories(OWNERSHIP_CATEGORY, "Ownership"), "Ownership category");
        applyRangeFilterWithDefaults(PROMOTERS_STAKE, "Promoter's Stake");
        applyRangeFilterWithDefaults(INSTITUTIONAL_STAKE, "Institutional Stake");
        WebElement applied = requireVisible(FILTERS_APPLIED_LABEL, "'N Filters Applied' in the picker", SHORT_WAIT_MS);
        String appliedText = safeAttribute(applied, "content-desc");
        if (!appliedText.startsWith("3 ")) {
            throw new AssertionError("Expected '3 Filters Applied' after adding two Ownership filters, saw '"
                    + appliedText + "'");
        }
        safeClick(requireVisible(APPLY_FILTERS_BUTTON, "Apply Filters button", SHORT_WAIT_MS), "Apply Filters");

        // Review Filters -> Update Screen
        requireVisible(REVIEW_FILTERS_TITLE, "Review Filters screen", PAGE_READY_WAIT_MS);
        for (By row : List.of(PROMOTERS_STAKE, INSTITUTIONAL_STAKE, AppiumBy.accessibilityId("Stock Rating"))) {
            requireVisible(row, "filter row on Review Filters", SHORT_WAIT_MS);
        }
        ReportLogger.pass("Review Filters lists Promoter's Stake, Institutional Stake and Stock Rating");
        safeClick(requireVisible(UPDATE_SCREEN_BUTTON, "Update Screen button", SHORT_WAIT_MS), "Update Screen");

        // Updated screen: heading "Stock Screener", fewer stocks
        boolean updated = waitUntil(() -> isVisible(SCREENER_HEADING) && isVisible(STOCKS_FILTERED_LABEL)
                && !isVisible(REVIEW_FILTERS_TITLE), PAGE_READY_WAIT_MS);
        if (!updated) {
            throw new AssertionError("Screen did not update after Update Screen; visible=" + collectVisibleValues());
        }
        waitForStableNodes(desc -> true);
        int countAfter = readStocksFilteredCount();
        if (countAfter >= countBefore) {
            throw new AssertionError("Stocks Filtered did not drop after adding filters: " + countBefore + " -> " + countAfter);
        }
        ReportLogger.pass("Screen updated: heading 'Stock Screener', stocks " + countBefore + " -> " + countAfter);

        lastSavedScreenCount = countAfter;
        if (withEpsColumn) {
            addColumnInCustomView("Per share data", "EPS", "EPS (₹)");
        }

        // Three dots -> Save Screen -> name -> Save
        openScreenActionsDrawer("Stock Screener");
        safeClick(requireVisible(DRAWER_SAVE_SCREEN, "'Save Screen' in the actions drawer", SHORT_WAIT_MS),
                "Save Screen in the drawer");
        requireVisible(NAME_YOUR_SCREEN_TITLE, "'Name Your Screen' dialog", PAGE_READY_WAIT_MS);
        String name = "screen" + (101 + (System.currentTimeMillis() / 1000L) % 899_899L);
        WebElement input = requireVisible(SCREEN_NAME_INPUT, "screen name input", SHORT_WAIT_MS);
        safeClick(input, "screen name input");
        input.sendKeys(name);
        String typed = safeAttribute(requireVisible(SCREEN_NAME_INPUT, "screen name input", SHORT_WAIT_MS), "text");
        if (!typed.contains(name)) {
            throw new AssertionError("Screen name was not typed into the dialog (field shows '" + typed + "')");
        }
        // No keyboard hiding here: it can close the whole dialog. A first tap may only dismiss
        // the keyboard; the dialog's closing animation can also flicker, so the outcome is read
        // once the screen is still, and the popup is remembered as soon as it is seen.
        AtomicBoolean popupSeen = new AtomicBoolean(false);
        safeClick(requireVisible(SAVE_SCREEN_DIALOG_BUTTON, "'Save Screen' button in the dialog", SHORT_WAIT_MS),
                "Save Screen (dialog)");
        waitForSaveOutcome(popupSeen);
        if (!popupSeen.get() && isVisible(NAME_YOUR_SCREEN_TITLE)) {
            WebElement saveAgain = firstDisplayed(SAVE_SCREEN_DIALOG_BUTTON);
            if (saveAgain != null) {
                ReportLogger.info("Dialog still open after tapping Save Screen; tapping it once more");
                safeClick(saveAgain, "Save Screen (dialog, retry)");
                waitForSaveOutcome(popupSeen);
            }
        }
        if (!popupSeen.get()) {
            popupSeen.set(waitUntil(() -> isVisible(NEW_RECORD_ADDED_POPUP), SHORT_WAIT_MS));
        }
        if (!popupSeen.get()) {
            throw new AssertionError("'New Record Added' success popup did not appear after saving '" + name
                    + "'; dialog " + (isVisible(NAME_YOUR_SCREEN_TITLE) ? "still open" : "closed")
                    + "; visible=" + collectVisibleValues());
        }
        ReportLogger.pass("Screen saved as '" + name + "' (success popup shown)");
        // Go back only once the popup has gone, so the tap reaches the screen's back arrow.
        if (!waitUntil(() -> !isVisible(NEW_RECORD_ADDED_POPUP), PAGE_READY_WAIT_MS)) {
            ReportLogger.info("Success popup still shown after " + PAGE_READY_WAIT_MS / 1000 + " s; going back anyway");
        }
        waitForStableNodes(desc -> true);

        // Back to Stock Screener -> Saved tab, refresh until the screen is listed
        safeClick(requireVisible(BACK_BUTTON, "Go back on the saved screen", SHORT_WAIT_MS), "Go back");
        if (!waitUntil(() -> isVisible(PREDEFINED_TAB) && !isVisible(STOCKS_FILTERED_LABEL), PAGE_READY_WAIT_MS)) {
            throw new AssertionError("Go back did not return to Stock Screener; visible=" + collectVisibleValues());
        }
        safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
        // A new saved screen reaches the list late. First the Saved tab is refreshed in several
        // ways; if it is still missing, the app is restarted and the search is repeated.
        boolean listed = findSavedScreenWithRefreshes(name);
        if (!listed) {
            ReportLogger.info("'" + name + "' still not listed after " + SAVED_TAB_MAX_WAIT_MS / 1000
                    + " s of refreshes; restarting the app and searching again");
            restartAppAndOpenSavedTab();
            listed = findSavedScreenWithRefreshes(name);
            if (listed) {
                ReportLogger.info("'" + name + "' appeared after restarting the app");
            }
        }
        restorePredefinedTab();
        if (!listed) {
            throw new AssertionError("Saved screen '" + name + "' not listed in the Saved tab, neither within "
                    + SAVED_TAB_MAX_WAIT_MS / 1000 + " s of refreshes nor after an app restart and another "
                    + SAVED_TAB_MAX_WAIT_MS / 1000 + " s (list searched top to bottom after every refresh)");
        }
        ReportLogger.pass("Saved screen '" + name + "' is listed in the Saved tab");
        return name;
    }

    // =========================================================
    // SS_036-SS_038: customisation suite (active account)
    // =========================================================

    /**
     * SS_036: on Top Rated Stocks, add Promoter's Stake, Apply Filters, then tap "Reset all" on
     * Review Filters (before Update Screen). The sheet must be back to "1 Filter Applied" with
     * only Stock Rating, and the screen must be unchanged after leaving the sheet.
     */
    public void verifyResetAllRestoresDefaultFilters() {
        ScreenCard screen = selectScreens(findSection("Value Research Ratings"), "Top Rated Stocks").get(0);
        ReportLogger.step("Reset all on '" + screen.title + "' must restore its default filter");
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        int countBefore = readStocksFilteredCount();

        safeClick(requireVisible(FILTERS_BUTTON, "Filters button", SHORT_WAIT_MS), "Filters button");
        requireVisible(FILTERS_APPLIED_LABEL, "Filters sheet", PAGE_READY_WAIT_MS);
        safeClick(requireVisible(ADD_FILTER_BUTTON, "'Add Filter' button in the Filters sheet", SHORT_WAIT_MS),
                "Add Filter button");
        requireVisible(APPLY_FILTERS_BUTTON, "Add Filter picker (Apply Filters button)", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        safeClick(revealInPickerCategories(OWNERSHIP_CATEGORY, "Ownership"), "Ownership category");
        applyRangeFilterWithDefaults(PROMOTERS_STAKE, "Promoter's Stake");
        safeClick(requireVisible(APPLY_FILTERS_BUTTON, "Apply Filters button", SHORT_WAIT_MS), "Apply Filters");

        requireVisible(REVIEW_FILTERS_TITLE, "Review Filters screen", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        String before = appliedFiltersText();
        if (!before.startsWith("2 ") || !isVisible(PROMOTERS_STAKE)) {
            throw new AssertionError("Expected '2 Filters Applied' with Promoter's Stake before Reset all, saw '"
                    + before + "'; visible=" + collectVisibleValues());
        }
        ReportLogger.pass("Review Filters shows '" + before + "' with Promoter's Stake and Stock Rating");

        safeClick(requireVisible(RESET_ALL_BUTTON, "'Reset all' on Review Filters", SHORT_WAIT_MS), "Reset all");
        boolean reset = waitUntil(() -> appliedFiltersText().startsWith("1 ") && !isVisible(PROMOTERS_STAKE),
                SHORT_WAIT_MS);
        waitForStableNodes(desc -> true);
        List<String> problems = new ArrayList<>();
        String after = appliedFiltersText();
        if (!reset || !after.startsWith("1 ")) {
            problems.add("sheet shows '" + after + "' after Reset all, expected '1 Filter Applied'");
        }
        if (isVisible(PROMOTERS_STAKE)) {
            problems.add("Promoter's Stake still listed after Reset all");
        }
        if (!isVisible(REVIEW_STOCK_RATING_ROW)) {
            problems.add("default filter Stock Rating missing after Reset all");
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("Reset all did not restore the default filters: " + problems + "; visible="
                    + collectVisibleValues());
        }
        ReportLogger.pass("Reset all restored the default: '" + after + "' with only Stock Rating");

        // Leave without Update Screen: the screen itself must be unchanged.
        driver.navigate().back();
        waitUntil(() -> !isVisible(REVIEW_FILTERS_TITLE) && !isVisible(FILTERS_APPLIED_LABEL), SHORT_WAIT_MS);
        if (isVisible(FILTERS_APPLIED_LABEL)) {
            closeFiltersSheetWithDeviceBack(screen.title);
        }
        verifyDetailHeading(screen.title);
        int countAfter = readStocksFilteredCount();
        if (countAfter != countBefore) {
            throw new AssertionError("Stocks Filtered changed after Reset all without Update Screen: " + countBefore
                    + " -> " + countAfter);
        }
        ReportLogger.pass("'" + screen.title + "' unchanged (" + countAfter + " stocks)");
        navigateBackToStockScreener(screen.title);
    }

    private String appliedFiltersText() {
        WebElement label = firstDisplayed(FILTERS_APPLIED_LABEL);
        return label == null ? "" : safeAttribute(label, "content-desc");
    }

    /**
     * SS_037: build a screen (2 Ownership filters + EPS in Custom View), save it, reopen it from
     * the Saved tab, and check name, Overview tab, stock count, filters and the EPS column.
     */
    public String verifySavedScreenRestoresFiltersAndColumns() {
        String name = createSavedScreen(true);
        int savedCount = lastSavedScreenCount;
        openSavedScreen(name);

        List<String> problems = new ArrayList<>();
        if (!isVisible(AppiumBy.accessibilityId(name))) {
            problems.add("heading is not '" + name + "'");
        }
        int count = readStocksFilteredCount();
        if (count != savedCount) {
            problems.add("Stocks Filtered is " + count + ", saved with " + savedCount);
        }
        if (!isVisible(AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"Price (\")"))) {
            problems.add("did not open on the Overview tab (no 'Price (₹)' column)");
        }

        safeClick(requireVisible(FILTERS_BUTTON, "Filters button", SHORT_WAIT_MS), "Filters button");
        requireVisible(FILTERS_APPLIED_LABEL, "Filters sheet of the saved screen", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        String applied = appliedFiltersText();
        if (!applied.startsWith("3 ")) {
            problems.add("Filters sheet shows '" + applied + "', expected '3 Filters Applied'");
        }
        for (String filter : List.of("Promoter's Stake", "Institutional Stake", "Stock Rating")) {
            By row = AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"" + filter + "\")");
            if (!isVisible(row)) {
                problems.add("filter '" + filter + "' not restored");
            }
        }
        closeFiltersSheetWithDeviceBack(name);

        openDetailTab(DETAIL_TABS.get(5), isTabSelectionStateSupported(), false);
        List<String> eps = List.of("EPS (₹)");
        if (!missingColumns(collectTableHeaders(eps), eps).isEmpty()) {
            problems.add("Custom View has no 'EPS (₹)' column");
        }
        navigateBackToStockScreener(name);
        if (!problems.isEmpty()) {
            throw new AssertionError("Saved screen '" + name + "' not restored correctly: " + problems);
        }
        ReportLogger.pass("Saved screen '" + name + "' restored: heading, Overview tab, " + count
                + " stocks, 3 filters and the EPS column in Custom View");
        return name;
    }

    /** Custom View -> Columns -> Add Columns -> tick a column -> Apply Columns -> Update Columns. */
    private void addColumnInCustomView(String category, String column, String tableHeader) {
        ReportLogger.step("Adding column '" + column + "' (" + category + ") to Custom View");
        openDetailTab(DETAIL_TABS.get(5), isTabSelectionStateSupported(), false);
        safeClick(requireVisible(COLUMNS_BUTTON, "'Columns' button on Custom View", SHORT_WAIT_MS), "Columns button");
        requireVisible(ADD_COLUMNS_BUTTON, "Columns page (Add Columns button)", PAGE_READY_WAIT_MS);
        safeClick(requireVisible(ADD_COLUMNS_BUTTON, "Add Columns button", SHORT_WAIT_MS), "Add Columns");
        requireVisible(APPLY_COLUMNS_BUTTON, "Add Columns panel (Apply Columns button)", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);

        By columnRow = AppiumBy.accessibilityId(column);
        By categoryRow = AppiumBy.accessibilityId(category);
        Dimension size = driver.manage().window().getSize();
        boolean expanded = false;
        for (int swipe = 0; swipe < ADD_COLUMNS_MAX_SWIPES && !isVisible(columnRow); swipe++) {
            Rectangle header = safeRect(firstDisplayed(categoryRow));
            if (!expanded && header != null && header.getY() > size.getHeight() * 0.12
                    && header.getY() < size.getHeight() * 0.75) {
                tapAt((int) (size.getWidth() * 0.25), Math.min(header.getY() + 45, header.getY() + header.getHeight() / 2));
                expanded = true;
                waitForStableNodes(desc -> true);
                continue;
            }
            int x = (int) (size.getWidth() * 0.85);
            performSwipe(x, (int) (size.getHeight() * 0.70), x, (int) (size.getHeight() * 0.45), 500L);
            waitForStableNodes(desc -> true);
        }
        WebElement row = requireVisible(columnRow, "'" + column + "' in Add Columns > " + category, SHORT_WAIT_MS);
        String selectedBefore = columnsSelectedText();
        safeClick(row, column + " column");
        waitUntil(() -> !columnsSelectedText().equals(selectedBefore), SHORT_WAIT_MS);
        ReportLogger.info("Columns selected: '" + selectedBefore + "' -> '" + columnsSelectedText() + "'");
        safeClick(requireVisible(APPLY_COLUMNS_BUTTON, "Apply Columns", SHORT_WAIT_MS), "Apply Columns");
        requireVisible(ADD_COLUMNS_BUTTON, "Columns page after Apply Columns", PAGE_READY_WAIT_MS);
        requireVisible(columnRow, "'" + column + "' listed on the Columns page", SHORT_WAIT_MS);
        safeClick(requireVisible(UPDATE_COLUMNS_BUTTON, "Update Columns", SHORT_WAIT_MS), "Update Columns");
        requireVisible(STOCKS_FILTERED_LABEL, "screen after Update Columns", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        List<String> expected = List.of(tableHeader);
        if (!missingColumns(collectTableHeaders(expected), expected).isEmpty()) {
            throw new AssertionError("'" + tableHeader + "' not shown in Custom View after Update Columns");
        }
        ReportLogger.pass("'" + tableHeader + "' added to Custom View");
    }

    private String columnsSelectedText() {
        for (UiNode node : displayedDescribedNodes(desc -> desc.matches("^\\d+ Columns? Selected$"))) {
            return node.desc;
        }
        return "";
    }

    /** Saved tab -> scrolls to the screen and opens it. */
    private void openSavedScreen(String name) {
        safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
        By entry = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + name + "\")");
        Dimension size = driver.manage().window().getSize();
        for (int swipe = 0; swipe <= SAVED_LIST_MAX_SWIPES; swipe++) {
            waitForStableNodes(desc -> true);
            Rectangle rect = safeRect(firstDisplayed(entry));
            if (rect != null && rect.getY() + rect.getHeight() / 2 < size.getHeight() * CARD_TAP_BAND_BOTTOM) {
                ReportLogger.step("Opening saved screen '" + name + "'");
                tapAt(rect.getX() + rect.getWidth() / 2, rect.getY() + rect.getHeight() / 2);
                requireVisible(STOCKS_FILTERED_LABEL, "saved screen '" + name + "'", PAGE_READY_WAIT_MS);
                waitForStableNodes(desc -> true);
                return;
            }
            swipeVerticalOnView(true);
        }
        throw new AssertionError("Saved screen '" + name + "' not found to open in the Saved tab");
    }

    /**
     * SS_038: Top Rated Stocks -> three dots -> Export. The download dialog must show the file
     * name, "... downloaded", Cancel and Open; it is closed with Cancel (the file is not opened).
     * The file name must start with the screen name and today's date, as on the VRO site
     * ("top-rated-stocks-11-Nov-2026-0204.xls"); the extension is not compared.
     */
    public void verifyExportToExcel() {
        ScreenCard screen = selectScreens(findSection("Value Research Ratings"), "Top Rated Stocks").get(0);
        ReportLogger.step("Export to Excel from '" + screen.title + "'");
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        openScreenActionsDrawer(screen.title);
        safeClick(requireVisible(DRAWER_EXPORT, "'Export' in the actions drawer", SHORT_WAIT_MS), "Export");

        WebElement file = requireVisible(EXPORT_FILE_NAME, "export dialog (file name)", PAGE_READY_WAIT_MS);
        String fileName = safeAttribute(file, "content-desc").trim();
        List<String> problems = new ArrayList<>();
        WebElement downloaded = waitForVisible(EXPORT_DOWNLOADED_LABEL, SHORT_WAIT_MS);
        if (downloaded == null) {
            problems.add("no '... downloaded' size label");
        } else {
            ReportLogger.pass("Export dialog: '" + fileName + "', '" + safeAttribute(downloaded, "content-desc") + "'");
        }
        if (!isVisible(EXPORT_OPEN)) {
            problems.add("no 'Open' button");
        }
        safeClick(requireVisible(EXPORT_CANCEL, "'Cancel' in the export dialog", SHORT_WAIT_MS), "Cancel");
        if (!waitUntil(() -> !isVisible(EXPORT_FILE_NAME), SHORT_WAIT_MS)) {
            problems.add("dialog did not close with Cancel");
        }
        waitForStableNodes(desc -> true);

        String expectedPrefix = screen.title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                + "-" + LocalDate.now().format(EXPORT_DATE);
        String baseName = fileName.replaceAll("(?i)\\.xlsx?$", "");
        if (!baseName.toLowerCase(Locale.ROOT).startsWith(expectedPrefix.toLowerCase(Locale.ROOT))) {
            problems.add("file name '" + fileName + "' does not match the VRO format '" + expectedPrefix
                    + "-HHMM' (screen name + date; awaiting dev confirmation)");
        } else {
            ReportLogger.pass("File name '" + fileName + "' has the screen name and date");
        }
        navigateBackToStockScreener(screen.title);
        if (!problems.isEmpty()) {
            throw new AssertionError("Export to Excel on '" + screen.title + "': " + problems);
        }
        ReportLogger.pass("Export to Excel verified on '" + screen.title + "'");
    }

    /**
     * SS_024: scrolling the Predefined tab must not scroll the Saved tab. The Saved
     * list's first entry and its position are recorded, the Predefined list is
     * scrolled, and the Saved tab must show the same first entry at the same place.
     */
    public void verifySavedTabNotScrolledByPredefined() {
        ReportLogger.step("Saved tab must keep its own scroll position");
        verifyPredefinedAndSavedTabs();
        safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
        String[] before = firstSavedEntry();
        ReportLogger.info("Saved tab top entry before: '" + before[0] + "' at y=" + before[1]);

        safeClick(requireVisible(PREDEFINED_TAB, "Predefined tab", SHORT_WAIT_MS), "Predefined tab");
        requireVisible(CREATE_NEW_SCREEN_BUTTON, "Predefined list", PAGE_READY_WAIT_MS);
        swipeVerticalOnView(true);
        waitForStableNodes(desc -> true);
        swipeVerticalOnView(true);
        waitForStableNodes(desc -> true);

        safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
        String[] after = firstSavedEntry();
        restorePredefinedTab();
        boolean same = before[0].equals(after[0]) && Math.abs(Integer.parseInt(before[1]) - Integer.parseInt(after[1])) <= 10;
        if (!same) {
            throw new AssertionError("Saved tab was scrolled together with Predefined: top entry was '" + before[0]
                    + "' at y=" + before[1] + ", now '" + after[0] + "' at y=" + after[1]);
        }
        ReportLogger.pass("Saved tab kept its position ('" + after[0] + "' still on top) after scrolling Predefined");
    }

    /**
     * On the Saved tab: waits up to 30 s, then refreshes in rotation (full pull-to-refresh,
     * re-selecting the Saved tab, reopening Stock Screener from Hub) until the screen is found
     * or SAVED_TAB_MAX_WAIT_MS has passed. The whole list is searched after every refresh.
     */
    private boolean findSavedScreenWithRefreshes(String name) {
        ReportLogger.step("Waiting up to " + SAVED_TAB_INITIAL_WAIT_MS / 1000 + " s for '" + name
                + "' to reach the Saved tab before refreshing");
        long deadline = System.currentTimeMillis() + SAVED_TAB_MAX_WAIT_MS;
        boolean listed = waitUntil(() -> savedScreenOnScreen(name), SAVED_TAB_INITIAL_WAIT_MS)
                || savedScreenInList(name);
        for (int round = 1; !listed && System.currentTimeMillis() < deadline; round++) {
            String how;
            if (round % 3 == 0) {
                how = "reopening Stock Screener from Hub";
                navigateBackToHub();
                openFromHub();
                safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
            } else if (round % 3 == 2) {
                how = "re-selecting the Saved tab";
                safeClick(requireVisible(PREDEFINED_TAB, "Predefined tab", SHORT_WAIT_MS), "Predefined tab");
                waitForStableNodes(desc -> true);
                safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
            } else {
                how = "pull-to-refresh";
                pullDownList();
            }
            ReportLogger.step("'" + name + "' not listed yet; refresh " + round + " (" + how + ")");
            listed = waitUntil(() -> savedScreenOnScreen(name), SAVED_TAB_AFTER_REFRESH_WAIT_MS)
                    || savedScreenInList(name);
            if (listed) {
                ReportLogger.info("'" + name + "' appeared after refresh " + round + " (" + how + ")");
            }
        }
        return listed;
    }

    /**
     * Closes and relaunches the app, completes the PIN/biometric step if it appears (shared
     * AuthHelper, existing-session PIN), then opens Stock Screener -> Saved tab.
     */
    private void restartAppAndOpenSavedTab() {
        String appId = ConfigReader.getOptional("appPackage", "com.valueresearch.advisor.qa").trim();
        ReportLogger.step("Restarting the app (" + appId + ")");
        driver.executeScript("mobile: terminateApp", Map.of("appId", appId));
        driver.executeScript("mobile: activateApp", Map.of("appId", appId));
        new AuthHelper(driver).finishSecurityAuthentication(null);
        if (!waitForHubContainerAfterNavigation(PAGE_READY_WAIT_MS)) {
            WebElement hub = bottomMostVisible(HUB_TAB);
            if (hub != null) {
                safeClick(hub, "Hub bottom-navigation tab");
            }
        }
        openFromHub();
        safeClick(requireVisible(SAVED_TAB, "Saved tab", SHORT_WAIT_MS), "Saved tab");
        ReportLogger.pass("App restarted; Saved tab open again");
    }

    /** True when a displayed label contains the saved screen's name (case-insensitive, any extra lines). */
    private boolean savedScreenOnScreen(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return !displayedDescribedNodes(desc -> desc.toLowerCase(Locale.ROOT).contains(key)).isEmpty();
    }

    /**
     * Searches the whole Saved list: scrolls down until the list stops moving, then back
     * to the top so the next pull-to-refresh works.
     */
    private boolean savedScreenInList(String name) {
        if (savedScreenOnScreen(name)) {
            return true;
        }
        boolean found = false;
        String previous = signature(waitForStableNodes(desc -> true));
        for (int swipe = 0; swipe < SAVED_LIST_MAX_SWIPES && !found; swipe++) {
            swipeVerticalOnView(true);
            String current = signature(waitForStableNodes(desc -> true));
            found = savedScreenOnScreen(name);
            if (current.equals(previous)) {
                break;
            }
            previous = current;
        }
        if (found) {
            return true;
        }
        for (int swipe = 0; swipe < SAVED_LIST_MAX_SWIPES; swipe++) {
            swipeVerticalOnView(false);
            String current = signature(waitForStableNodes(desc -> true));
            if (current.equals(previous)) {
                break;
            }
            previous = current;
        }
        return false;
    }

    /** [name, top y] of the topmost saved-screen entry (ImageView labels between the tabs and the bottom bar). */
    private String[] firstSavedEntry() {
        waitForStableNodes(desc -> true);
        Rectangle tabs = safeRect(firstDisplayed(SAVED_TAB));
        int top = tabs == null ? 0 : tabs.getY() + tabs.getHeight();
        Document document = parseSource(driver.getPageSource());
        String best = null;
        int bestTop = Integer.MAX_VALUE;
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String desc = normalize(element.getAttribute("content-desc"));
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (desc.isEmpty() || box == null || !element.getTagName().endsWith("ImageView")
                    || BOTTOM_NAV_LABELS.contains(desc) || "false".equals(element.getAttribute("displayed"))) {
                continue;
            }
            if (box[1] >= top && box[1] < bestTop) {
                best = desc;
                bestTop = box[1];
            }
        }
        if (best == null) {
            throw new AssertionError("No saved screen found in the Saved tab; visible=" + collectVisibleValues());
        }
        return new String[]{best, String.valueOf(bestTop)};
    }

    private void restorePredefinedTab() {
        WebElement predefined = firstDisplayed(PREDEFINED_TAB);
        if (predefined != null) {
            safeClick(predefined, "Predefined tab");
            waitForStableNodes(desc -> true);
        }
    }

    private int readStocksFilteredCount() {
        WebElement label = requireVisible(STOCKS_FILTERED_LABEL, "'N Stocks Filtered' label", PAGE_READY_WAIT_MS);
        String digits = safeAttribute(label, "content-desc").replaceAll("[^0-9]", "");
        return digits.isEmpty() ? -1 : Integer.parseInt(digits);
    }

    /** Left-hand category list of the Add Filter picker: scrolls it until the category is in a tappable spot. */
    private WebElement revealInPickerCategories(By locator, String name) {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.13);
        for (int attempt = 0; attempt < 6; attempt++) {
            WebElement category = firstDisplayed(locator);
            Rectangle rect = safeRect(category);
            if (rect != null) {
                int centerY = rect.getY() + rect.getHeight() / 2;
                if (centerY > size.getHeight() * 0.15 && centerY < size.getHeight() * 0.80) {
                    return category;
                }
            }
            performSwipe(x, (int) (size.getHeight() * 0.70), x, (int) (size.getHeight() * 0.45), 500L);
            waitForStableNodes(desc -> true);
        }
        throw new AssertionError("'" + name + "' category not found in the Add Filter picker; visible=" + collectVisibleValues());
    }

    /** Taps a range filter in the picker and applies its drawer with the default values. */
    private void applyRangeFilterWithDefaults(By filter, String name) {
        safeClick(requireVisible(filter, "'" + name + "' in Ownership", SHORT_WAIT_MS), name);
        requireVisible(FILTER_DRAWER_APPLY, "'" + name + "' drawer (Apply button)", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        safeClick(requireVisible(FILTER_DRAWER_APPLY, "Apply in the '" + name + "' drawer", SHORT_WAIT_MS),
                "Apply (" + name + ")");
        if (!waitUntil(() -> !isVisible(FILTER_DRAWER_APPLY), SHORT_WAIT_MS)) {
            throw new AssertionError("'" + name + "' drawer did not close after Apply");
        }
        waitForStableNodes(desc -> true);
        ReportLogger.pass("'" + name + "' added with its default range");
    }

    /**
     * Waits until the success popup shows or the Name Your Screen dialog has closed and the
     * screen is still, recording whether the popup was seen at any point.
     */
    private void waitForSaveOutcome(AtomicBoolean popupSeen) {
        waitUntil(() -> {
            if (isVisible(NEW_RECORD_ADDED_POPUP)) {
                popupSeen.set(true);
                return true;
            }
            return !isVisible(NAME_YOUR_SCREEN_TITLE);
        }, PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        if (isVisible(NEW_RECORD_ADDED_POPUP)) {
            popupSeen.set(true);
        }
    }

    private void hideKeyboardQuietly() {
        try {
            driver.executeScript("mobile: hideKeyboard");
        } catch (RuntimeException ignored) {
            // Keyboard already hidden or command unsupported; the next lookup reports any real problem.
        }
    }

    /**
     * Full pull-to-refresh on the Saved list: the list only reloads completely when the finger
     * travels from just below the tabs almost to the bottom of the screen, slowly, and pauses
     * before lifting. The refresh indicator is then awaited until it has gone.
     */
    private void pullDownList() {
        Dimension size = driver.manage().window().getSize();
        Rectangle tabs = safeRect(firstDisplayed(SAVED_TAB));
        int startY = tabs == null ? (int) (size.getHeight() * 0.25) : tabs.getY() + tabs.getHeight() + 30;
        int endY = (int) (size.getHeight() * 0.92);
        int x = size.getWidth() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence pull = new Sequence(finger, 1);
        pull.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        pull.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        pull.addAction(new Pause(finger, Duration.ofMillis(120L)));
        pull.addAction(finger.createPointerMove(Duration.ofMillis(1_400L), PointerInput.Origin.viewport(), x, endY));
        pull.addAction(new Pause(finger, Duration.ofMillis(400L)));
        pull.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(pull));
        if (waitUntil(() -> isVisible(REFRESH_INDICATOR), REFRESH_INDICATOR_WAIT_MS)) {
            waitUntil(() -> !isVisible(REFRESH_INDICATOR), PAGE_READY_WAIT_MS);
        }
        waitForStableNodes(desc -> true);
    }

    // =========================================================
    // SS_026: "Unrated" in the Stock Rating column
    // =========================================================

    /**
     * Small Cap Growth -> Stock Rating tab: scrolls the table looking for "Unrated" in
     * the Stock Rating column. Fails if that column shows "0.00" (the old bug).
     * Returns false when no unrated stock exists today (data-dependent: the test skips).
     */
    public boolean verifyUnratedStockRating() {
        ScreenCard screen = selectScreens(findSection("Investment Ideas"), "Small Cap Growth").get(0);
        ReportLogger.step("Looking for 'Unrated' in the Stock Rating column of '" + screen.title + "'");
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        openDetailTab(STOCK_RATING_TAB, isTabSelectionStateSupported(), false);

        Rectangle column = safeRect(requireVisible(STOCK_RATING_HEADER, "'Stock Rating' column header", PAGE_READY_WAIT_MS));
        if (column == null) {
            throw new AssertionError("Could not read the position of the 'Stock Rating' column");
        }
        int left = column.getX();
        int right = column.getX() + column.getWidth();
        int headerBottom = column.getY() + column.getHeight();
        Dimension size = driver.manage().window().getSize();
        String previous = null;
        boolean found = false;
        for (int swipe = 0; swipe <= TABLE_ROWS_MAX_SWIPES; swipe++) {
            waitForStableNodes(desc -> true);
            String source = driver.getPageSource();
            List<String> values = columnValues(source, left, right, headerBottom);
            for (String value : values) {
                if (value.matches("^0(\\.0+)?$")) {
                    throw new AssertionError("Stock Rating column shows '" + value + "' instead of 'Unrated' on '"
                            + screen.title + "'");
                }
            }
            if (values.contains("Unrated")) {
                found = true;
                break;
            }
            String current = String.join("|", values);
            if (current.equals(previous)) {
                break;
            }
            previous = current;
            int x = (int) (size.getWidth() * 0.75);
            performSwipe(x, (int) (size.getHeight() * 0.80), x, (int) (size.getHeight() * 0.45), 500L);
        }
        navigateBackToStockScreener(screen.title);
        if (found) {
            ReportLogger.pass("'Unrated' shown in the Stock Rating column of '" + screen.title + "'");
        } else {
            ReportLogger.info("No unrated stock in '" + screen.title + "' today; nothing to verify");
        }
        return found;
    }

    /** Labels of displayed nodes whose centre lies inside a table column, below its header. */
    private List<String> columnValues(String source, int left, int right, int belowY) {
        Document document = parseSource(source);
        List<String> values = new ArrayList<>();
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String desc = normalize(element.getAttribute("content-desc"));
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (desc.isEmpty() || box == null || "false".equals(element.getAttribute("displayed"))) {
                continue;
            }
            int centerX = (box[0] + box[2]) / 2;
            if (centerX >= left && centerX <= right && box[1] >= belowY) {
                values.add(desc);
            }
        }
        return values;
    }

    private static Document parseSource(String source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
        } catch (Exception parseFailure) {
            throw new AssertionError("Could not parse the page source: " + parseFailure.getClass().getSimpleName());
        }
    }

    // =========================================================
    // SS_028 / SS_029: Add Columns panel (Custom View)
    // =========================================================

    /**
     * Opens Top Rated Stocks -> Custom View -> Columns -> Add Columns, reads every
     * category with its columns while scrolling, then leaves with the device Back key
     * (twice) without applying anything. Returns category -> columns shown.
     */
    public Map<String, List<String>> readAddColumnsPanel() {
        ScreenCard screen = selectScreens(findSection("Value Research Ratings"), "Top Rated Stocks").get(0);
        ReportLogger.step("Reading the Add Columns panel from '" + screen.title + "' / Custom View");
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        openDetailTab(DETAIL_TABS.get(5), isTabSelectionStateSupported(), false);

        safeClick(requireVisible(COLUMNS_BUTTON, "'Columns' button on Custom View", SHORT_WAIT_MS), "Columns button");
        requireVisible(ADD_COLUMNS_BUTTON, "Columns page (Add Columns button)", PAGE_READY_WAIT_MS);
        safeClick(requireVisible(ADD_COLUMNS_BUTTON, "Add Columns button", SHORT_WAIT_MS), "Add Columns");
        requireVisible(APPLY_COLUMNS_BUTTON, "Add Columns panel (Apply Columns button)", PAGE_READY_WAIT_MS);

        Map<String, List<String>> shown = new LinkedHashMap<>();
        Set<String> expandAttempts = new LinkedHashSet<>();
        String previous = null;
        Dimension size = driver.manage().window().getSize();
        for (int round = 0; round <= ADD_COLUMNS_MAX_SWIPES + ADD_COLUMNS_EXPECTED.size(); round++) {
            waitForStableNodes(desc -> true);
            int[] toExpand = readAddColumnsSnapshot(shown, expandAttempts, size);
            if (toExpand != null) {
                // Collapsed category on screen: tap its header row (top of the node, never the items).
                tapAt(toExpand[0], toExpand[1]);
                continue;
            }
            String signature = signature(displayedDescribedNodes(desc -> true));
            if (signature.equals(previous)) {
                break;
            }
            previous = signature;
            int x = (int) (size.getWidth() * 0.85);
            performSwipe(x, (int) (size.getHeight() * 0.75), x, (int) (size.getHeight() * 0.40), 500L);
        }

        // Leave without applying. One Back may close both the Add Columns panel and the
        // Columns page, so press Back only while a Columns page is still showing.
        for (int press = 1; press <= 2; press++) {
            driver.navigate().back();
            waitUntil(() -> !isVisible(APPLY_COLUMNS_BUTTON)
                    && (isVisible(STOCKS_FILTERED_LABEL) || isOnStockScreenerPage()), BACK_TRANSITION_WAIT_MS);
            waitForStableNodes(desc -> true);
            if (!isVisible(APPLY_COLUMNS_BUTTON) && !isVisible(ADD_COLUMNS_BUTTON)) {
                break;
            }
        }
        if (isVisible(STOCKS_FILTERED_LABEL)) {
            navigateBackToStockScreener(screen.title);
        } else if (!isOnStockScreenerPage()) {
            throw new AssertionError("Device Back did not leave the Columns pages; visible=" + collectVisibleValues());
        }
        ReportLogger.info("Add Columns panel shows: " + shown);
        return shown;
    }

    /**
     * One snapshot of the Add Columns panel. Categories are accordions: the category
     * node holds its columns as descendants once expanded. Columns are merged into
     * `shown`. Returns the tap point of a collapsed category header that is safely on
     * screen (each category is expanded at most once), or null.
     */
    private int[] readAddColumnsSnapshot(Map<String, List<String>> shown, Set<String> expandAttempts, Dimension size) {
        Document document = parseSource(driver.getPageSource());
        NodeList all = document.getElementsByTagName("*");
        int[] tapPoint = null;
        // Collapsed headers can be tapped anywhere above the bottom bar ("N Columns Selected" /
        // Apply Columns): the last category may sit low when the list cannot scroll any further.
        int bottomBarTop = (int) (size.getHeight() * 0.85);
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String desc = normalize(element.getAttribute("content-desc"));
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (box != null && (desc.matches("^\\d+ Columns? Selected$") || desc.equals("Apply Columns"))) {
                bottomBarTop = Math.min(bottomBarTop, box[1]);
            }
        }
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String desc = normalize(element.getAttribute("content-desc"));
            if (!ADD_COLUMNS_EXPECTED.containsKey(desc) || hasCategoryAncestor(element)) {
                continue;               // not a category, or a column named like one ("Stock Rating")
            }
            List<String> columns = shown.computeIfAbsent(desc, k -> new ArrayList<>());
            NodeList descendants = element.getElementsByTagName("*");
            int found = 0;
            for (int d = 0; d < descendants.getLength(); d++) {
                String column = normalize(((Element) descendants.item(d)).getAttribute("content-desc"));
                if (!column.isEmpty()) {
                    found++;
                    if (!columns.contains(column)) {
                        columns.add(column);
                    }
                }
            }
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (found == 0 && tapPoint == null && box != null && !expandAttempts.contains(desc)
                    && box[1] > size.getHeight() * 0.10 && box[1] + 90 < bottomBarTop) {
                expandAttempts.add(desc);
                tapPoint = new int[]{(int) (size.getWidth() * 0.25), Math.min(box[1] + 45, (box[1] + box[3]) / 2)};
                ReportLogger.step("Expanding Add Columns category '" + desc + "'");
            }
        }
        return tapPoint;
    }

    private static boolean hasCategoryAncestor(Element element) {
        for (Node parent = element.getParentNode(); parent instanceof Element; parent = parent.getParentNode()) {
            if (ADD_COLUMNS_EXPECTED.containsKey(normalize(((Element) parent).getAttribute("content-desc")))) {
                return true;
            }
        }
        return false;
    }

    /** SS_028: EPS must be offered under Per share data. */
    public void verifyAddColumnsOffersEps() {
        Map<String, List<String>> shown = readAddColumnsPanel();
        List<String> perShare = shown.getOrDefault("Per share data", Collections.emptyList());
        if (!perShare.contains("EPS")) {
            throw new AssertionError("EPS is not offered under 'Per share data' in Add Columns; shown=" + shown);
        }
        ReportLogger.pass("Add Columns offers EPS under 'Per share data'");
    }

    /** SS_029: every expected column must be offered under its category. */
    public void verifyAddColumnsListsAllColumns() {
        Map<String, List<String>> shown = readAddColumnsPanel();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, List<String>> category : ADD_COLUMNS_EXPECTED.entrySet()) {
            List<String> offered = shown.get(category.getKey());
            if (offered == null) {
                problems.add("category '" + category.getKey() + "' missing");
                continue;
            }
            List<String> missing = new ArrayList<>();
            for (String column : category.getValue()) {
                if (!offered.contains(column)) {
                    missing.add(column);
                }
            }
            if (!missing.isEmpty()) {
                problems.add(category.getKey() + " missing " + missing);
            }
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("Add Columns is missing columns: " + problems + " | shown=" + shown);
        }
        ReportLogger.pass("Add Columns offers every expected column in all " + ADD_COLUMNS_EXPECTED.size() + " categories");
    }

    // =========================================================
    // SS_030: lapsed user - Seasonal screens, only Custom View locked
    // =========================================================

    /**
     * After a tab switch the table is rebuilt, so the header can be missing for a moment.
     * Waits for a definite state - the table ("Securities" header) or the lock nudge - and
     * reads it once the screen is still: "open", "is locked (nudge shown)" or
     * "shows neither the table nor the nudge".
     */
    private String tabContentState() {
        waitUntil(() -> isVisible(SECURITIES_HEADER) || isVisible(LIMITED_VERSION_NUDGE), PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        if (isVisible(LIMITED_VERSION_NUDGE)) {
            return "is locked (nudge shown)";
        }
        return isVisible(SECURITIES_HEADER) ? "open" : "shows neither the table nor the nudge";
    }

    public void verifyLapsedSeasonalOnlyCustomViewLocked() {
        PredefinedSection section = findSection("Seasonal");
        ReportLogger.step("Non-subscriber (lapsed / VRFA-only): Seasonal screens must lock only the Custom View tab");
        verifyPredefinedAndSavedTabs();
        List<String> failures = new ArrayList<>();
        for (ScreenCard screen : section.screens) {
            if (!isOnStockScreenerPage()) {
                openFromHub();
            }
            openPredefinedScreen(screen);
            try {
                verifyDetailHeading(screen.title);
                boolean selectionSupported = isTabSelectionStateSupported();
                for (int t = 0; t < 5; t++) {
                    DetailTab tab = DETAIL_TABS.get(t);
                    openDetailTab(tab, selectionSupported, false);
                    String state = tabContentState();
                    if (!"open".equals(state)) {
                        failures.add(screen.title + " / " + tab.label + " " + state);
                        ReportLogger.fail("'" + screen.title + "' / " + tab.label + " " + state + " for the lapsed user");
                    } else {
                        ReportLogger.pass("'" + screen.title + "' / " + tab.label + " is open (table shown, no nudge)");
                    }
                }
                openDetailTab(DETAIL_TABS.get(5), selectionSupported, false);
                verifySubscriptionNudge(screen.title + " / Custom View");
                ReportLogger.pass("'" + screen.title + "': tabs 1-5 open, Custom View locked");
            } catch (AssertionError mismatch) {
                failures.add(screen.title + ": " + clean(mismatch.getMessage()));
                ReportLogger.fail("'" + screen.title + "' check failed: " + clean(mismatch.getMessage()));
            }
            navigateBackToStockScreener(screen.title);
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Seasonal lock wrong for the lapsed user: " + failures);
        }
        ReportLogger.pass("All Seasonal screens: only Custom View is locked for the lapsed user");
    }

    // =========================================================
    // TC17-TC20: lapsed user - subscription page and nudges
    // =========================================================

    /** TC17: Create New Screen must open the subscription page for a lapsed user. */
    public void verifyLapsedCreateNewScreenOpensSubscription() {
        ReportLogger.step("Non-subscriber (lapsed / VRFA-only): Create New Screen should open the subscription page");
        clickCreateNewScreen();
        verifySubscriptionPage("Create New Screen");
        closeSubscriptionPageWithDeviceBack("Create New Screen", this::isOnStockScreenerPage, "Stock Screener");
    }

    /** TC18: the Saved tab must show the subscription nudge for a lapsed user. */
    public void verifyLapsedSavedTabShowsNudge() {
        ReportLogger.step("Non-subscriber (lapsed / VRFA-only): Saved tab should show the subscription nudge");
        WebElement saved = requireVisible(SAVED_TAB, "Saved tab", PAGE_READY_WAIT_MS);
        safeClick(saved, "Saved tab");
        verifySubscriptionNudge("Saved tab");

        // Leave Stock Screener on its default tab for the next steps.
        WebElement predefined = requireVisible(PREDEFINED_TAB, "Predefined tab", SHORT_WAIT_MS);
        safeClick(predefined, "Predefined tab");
        requireVisible(CREATE_NEW_SCREEN_BUTTON, "Create New Screen button on the Predefined tab", PAGE_READY_WAIT_MS);
    }

    /**
     * TC19: on the first screen of every enabled Predefined section, the Custom
     * View tab must show the subscription nudge. Mismatches are collected; a
     * navigation failure stops the test.
     */
    public void verifyLapsedCustomViewNudgeOnFirstScreens() {
        ReportLogger.step("Non-subscriber (lapsed / VRFA-only): Custom View should show the subscription nudge on each section's first screen");
        verifyPredefinedAndSavedTabs();
        DetailTab customView = DETAIL_TABS.get(5);

        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (PredefinedSection section : PREDEFINED_SECTIONS) {
            if (!section.enabled) {
                ReportLogger.info("Section skipped (disabled until app issues are fixed): " + section.heading);
                continue;
            }
            ScreenCard screen = section.screens.get(0);
            if (!isOnStockScreenerPage()) {
                ReportLogger.info("Not on Stock Screener before opening '" + screen.title + "'; reopening it from Hub");
                openFromHub();
            }
            openPredefinedScreen(screen);
            checked++;
            try {
                verifyDetailHeading(screen.title);
                openDetailTab(customView, isTabSelectionStateSupported());
                verifySubscriptionNudge(screen.title + " / Custom View");
            } catch (AssertionError mismatch) {
                failures.add(screen.title + ": " + clean(mismatch.getMessage()));
                ReportLogger.fail("Custom View nudge check failed on '" + screen.title + "': " + clean(mismatch.getMessage()));
            }
            navigateBackToStockScreener(screen.title);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Custom View nudge missing on " + failures.size() + "/" + checked + " screen(s): " + failures);
        }
        ReportLogger.pass("Custom View subscription nudge verified on " + checked + " screens");
    }

    /**
     * TC20: on a predefined screen's Valuation tab, the three-dots drawer offers
     * Save Screen (lapsed users get no Export); it must open the subscription page.
     */
    public void verifyLapsedScreenActionsOpenSubscription(String sectionHeading, String screenTitle) {
        ScreenCard screen = selectScreens(findSection(sectionHeading), screenTitle).get(0);
        ReportLogger.step("Non-subscriber (lapsed / VRFA-only): Save Screen on '" + screen.title + "' should open the subscription page");
        verifyPredefinedAndSavedTabs();
        openPredefinedScreen(screen);
        verifyDetailHeading(screen.title);
        openDetailTab(DETAIL_TABS.get(2), isTabSelectionStateSupported());

        // For a lapsed user the drawer offers only Save Screen (no Export).
        openScreenActionsDrawer(screen.title);
        WebElement saveScreen = requireVisible(DRAWER_SAVE_SCREEN, "'Save Screen' in the actions drawer", SHORT_WAIT_MS);
        safeClick(saveScreen, "Save Screen in the actions drawer");
        verifySubscriptionPage("Save Screen");
        closeSubscriptionPageWithDeviceBack("Save Screen", this::isOnDetailScreenWithoutOverlay, "'" + screen.title + "'");
        navigateBackToStockScreener(screen.title);
    }

    private void verifySubscriptionPage(String openedFrom) {
        requireVisible(SUBSCRIPTION_HEADLINE, "Subscription page headline after '" + openedFrom + "'", PAGE_READY_WAIT_MS);
        requireVisible(SUBSCRIBE_BUTTON, "Subscribe button on the subscription page", SHORT_WAIT_MS);
        ReportLogger.pass("'" + openedFrom + "' opened the subscription page (headline and Subscribe button visible)");
    }

    private void verifySubscriptionNudge(String where) {
        requireVisible(LIMITED_VERSION_NUDGE, "'You are viewing a limited version of Stock Advisor.' nudge on " + where,
                PAGE_READY_WAIT_MS);
        requireVisible(UNLOCK_FULL_DATA_NUDGE, "'Unlock full stock data...' nudge text on " + where, SHORT_WAIT_MS);
        requireVisible(SUBSCRIBE_NOW_BUTTON, "'Subscribe Now' button on " + where, SHORT_WAIT_MS);
        ReportLogger.pass("Subscription nudge with 'Subscribe Now' shown on " + where);
    }

    /** The subscription page has no back button; the device Back key returns to the previous screen. */
    private void closeSubscriptionPageWithDeviceBack(String openedFrom, BooleanSupplier returned, String returnName) {
        for (int press = 1; press <= 2; press++) {
            driver.navigate().back();
            if (waitUntil(() -> !isVisible(SUBSCRIBE_BUTTON) && returned.getAsBoolean(), SHORT_WAIT_MS)) {
                waitForStableNodes(desc -> true);
                ReportLogger.pass("Device Back from the subscription page (opened via '" + openedFrom + "') returned to "
                        + returnName);
                return;
            }
            // Back may first close a drawer left behind the subscription page; press once more.
            if (!isVisible(SUBSCRIBE_BUTTON) && !isActionsDrawerOpen()) {
                break;
            }
        }
        throw new AssertionError("Device Back from the subscription page did not return to " + returnName + "; visible="
                + collectVisibleValues());
    }

    private boolean isOnDetailScreenWithoutOverlay() {
        return isVisible(STOCKS_FILTERED_LABEL) && !isActionsDrawerOpen() && !isVisible(SUBSCRIBE_BUTTON);
    }

    /** Taps the three dots at the right end of the heading row and waits for the Save Screen drawer. */
    private void openScreenActionsDrawer(String screenTitle) {
        WebElement heading = requireVisible(AppiumBy.accessibilityId(screenTitle), "Heading '" + screenTitle + "'",
                SHORT_WAIT_MS);
        Rectangle rect = safeRect(heading);
        if (rect == null) {
            throw new AssertionError("Could not read the position of the '" + screenTitle + "' heading");
        }
        int x = (int) (driver.manage().window().getSize().getWidth() * THREE_DOTS_X_FRACTION);
        int y = rect.getY() + rect.getHeight() / 2;
        ReportLogger.step("Tapping the three-dots button next to '" + screenTitle + "' at (" + x + ", " + y + ")");
        tapAt(x, y);
        if (!waitUntil(() -> isVisible(DRAWER_SAVE_SCREEN), SHORT_WAIT_MS)) {
            throw new AssertionError("Three-dots drawer with 'Save Screen' did not open on '" + screenTitle
                    + "'; visible=" + collectVisibleValues());
        }
        waitForStableNodes(desc -> true);
    }

    private void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(80L)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    // =========================================================
    // TC13-TC16: default filters of every screen in a section
    // =========================================================

    /**
     * Opens every screen of a Predefined section, opens its Filters sheet and
     * verifies the default filters exactly: every expected filter is present
     * with the expected "from" value, and no unexpected filter is applied.
     * The sheet is closed with the device Back key (it has no back button).
     */
    public void verifyDefaultFilters(String sectionHeading) {
        PredefinedSection section = findSection(sectionHeading);
        for (ScreenCard screen : section.screens) {
            if (!DEFAULT_FILTERS.containsKey(screen.title)) {
                throw new IllegalStateException("No default filters defined for '" + screen.title + "'");
            }
        }
        ReportLogger.step("Verifying default filters of " + section.screens.size() + " '" + section.heading + "' screens");
        verifyPredefinedAndSavedTabs();

        List<String> failures = new ArrayList<>();
        for (ScreenCard screen : section.screens) {
            if (!isOnStockScreenerPage()) {
                ReportLogger.info("Not on Stock Screener before opening '" + screen.title + "'; reopening it from Hub");
                openFromHub();
            }
            openPredefinedScreen(screen);
            try {
                verifyDetailHeading(screen.title);
                openFiltersSheet(screen.title);
                FilterSheetState state = readFilterSheet(DEFAULT_FILTERS.get(screen.title));
                List<String> problems = compareFilters(state, DEFAULT_FILTERS.get(screen.title));
                Integer expectedCount = EXPECTED_APPLIED_COUNT.get(screen.title);
                if (expectedCount != null) {
                    String shown = state.appliedLabel == null ? "" : state.appliedLabel.replaceAll("[^0-9]", "");
                    if (!shown.equals(String.valueOf(expectedCount))) {
                        problems.add("sheet shows '" + state.appliedLabel + "', expected " + expectedCount
                                + " filters applied");
                    }
                }
                Map<String, Set<String>> sectorSelection = EXPECTED_SECTOR_SELECTION.get(screen.title);
                if (sectorSelection != null) {
                    for (Map.Entry<String, Set<String>> group : sectorSelection.entrySet()) {
                        problems.addAll(verifySectorSelection(screen.title, group.getKey(), group.getValue()));
                    }
                }
                if (problems.isEmpty()) {
                    ReportLogger.pass("'" + screen.title + "' default filters correct: " + state.describe());
                } else {
                    failures.add(screen.title + ": " + problems);
                    ReportLogger.fail("'" + screen.title + "' default filters wrong: " + problems
                            + " | shown: " + state.describe());
                    if ((state.ranges.isEmpty() && state.selections.isEmpty())
                            || problems.toString().contains("did not expand")) {
                        // Nothing could be read at all: keep the raw page source for diagnosis.
                        dumpPageSource("filters-" + screen.title, state.lastPageSource);
                    }
                }
            } catch (AssertionError mismatch) {
                failures.add(screen.title + ": " + clean(mismatch.getMessage()));
                ReportLogger.fail("Screen '" + screen.title + "' check failed: " + clean(mismatch.getMessage()));
            }
            if (isVisible(FILTERS_APPLIED_LABEL)) {
                closeFiltersSheetWithDeviceBack(screen.title);
            }
            navigateBackToStockScreener(screen.title);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Default filters wrong on " + failures.size() + "/" + section.screens.size()
                    + " '" + section.heading + "' screen(s): " + failures);
        }
        ReportLogger.pass("Default filters verified on all " + section.screens.size() + " '" + section.heading + "' screens");
    }

    // ---------------------------------------------------------
    // SS_013: By Sector picker (tick state is drawn, not exposed)
    // ---------------------------------------------------------

    /**
     * From the Filters sheet: tap By Sector, expand the sector group (e.g. Financial),
     * read every sub-sector's tick box while scrolling the list, then leave with the
     * device Back key. Nothing inside the list is tapped, so no filter can change.
     * The app exposes no checked state: a ticked box is an ImageView, an empty box a
     * plain View (49x49 square left of the label).
     */
    private List<String> verifySectorSelection(String screenTitle, String group, Set<String> expectedChecked) {
        List<String> problems = new ArrayList<>();
        // Filters sheet -> Add Filter picker -> "By Sector" in the left category list.
        safeClick(requireVisible(ADD_FILTER_BUTTON, "'Add Filter' button in the Filters sheet of '" + screenTitle + "'",
                SHORT_WAIT_MS), "Add Filter button");
        requireVisible(APPLY_FILTERS_BUTTON, "Add Filter picker (Apply Filters button)", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
        safeClick(revealInPickerCategories(AppiumBy.accessibilityId("By Sector"), "By Sector"), "By Sector category");
        waitForStableNodes(desc -> true);

        SectorScan scan = new SectorScan();
        String source = driver.getPageSource();
        readSectorSnapshot(scan, group, source);
        if (!scan.groupSeen) {
            problems.add("sector group '" + group + "' not found in the By Sector picker");
            dumpPageSource("sector-" + screenTitle, source);
        } else {
            if (scan.rows.isEmpty()) {
                // Group collapsed: tap its row once to expand it.
                WebElement groupRow = requireVisible(AppiumBy.accessibilityId(group), "'" + group + "' sector row",
                        SHORT_WAIT_MS);
                safeClick(groupRow, group + " sector row (expand)");
                waitForStableNodes(desc -> true);
                source = driver.getPageSource();
                readSectorSnapshot(scan, group, source);
            }
            String previous = signature(displayedDescribedNodes(desc -> true));
            for (int swipe = 0; swipe < SECTOR_PICKER_MAX_SWIPES && !scan.groupEnded; swipe++) {
                swipeSectorList();
                String current = signature(waitForStableNodes(desc -> true));
                source = driver.getPageSource();
                readSectorSnapshot(scan, group, source);
                if (current.equals(previous)) {
                    break;
                }
                previous = current;
            }
            List<String> checked = new ArrayList<>();
            for (Map.Entry<String, Boolean> row : scan.rows.entrySet()) {
                if (row.getValue() == null) {
                    problems.add("could not read the tick box of '" + row.getKey() + "'");
                } else if (row.getValue()) {
                    checked.add(row.getKey());
                }
            }
            for (String name : expectedChecked) {
                if (!scan.rows.containsKey(name)) {
                    problems.add("'" + name + "' not listed under " + group);
                } else if (Boolean.FALSE.equals(scan.rows.get(name))) {
                    problems.add("'" + name + "' should be ticked under " + group);
                }
            }
            for (String name : checked) {
                if (!expectedChecked.contains(name)) {
                    problems.add("'" + name + "' is ticked under " + group + " but should not be");
                }
            }
            if (problems.isEmpty()) {
                ReportLogger.pass("'" + screenTitle + "' By Sector > " + group + ": only " + expectedChecked
                        + " ticked (" + scan.rows.size() + " sub-sectors read)");
            } else {
                dumpPageSource("sector-" + screenTitle, source);
            }
        }

        // Leave without applying anything. With nothing changed, Back from the picker returns
        // straight to the screen (Top Rated Banks), not to the Filters sheet; both are fine.
        driver.navigate().back();
        boolean left = waitUntil(() -> !isVisible(APPLY_FILTERS_BUTTON)
                && (isVisible(FILTERS_APPLIED_LABEL) || isVisible(STOCKS_FILTERED_LABEL)), SHORT_WAIT_MS);
        if (!left) {
            throw new AssertionError("Device Back from the By Sector picker returned to neither the Filters sheet nor the "
                    + "screen; visible=" + collectVisibleValues());
        }
        waitForStableNodes(desc -> true);
        ReportLogger.info("Left the By Sector picker without applying; now on "
                + (isVisible(FILTERS_APPLIED_LABEL) ? "the Filters sheet" : "'" + screenTitle + "'"));
        return problems;
    }

    /** Right-hand list of the picker, dragged on its right side, away from the tick boxes. */
    private void swipeSectorList() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.88);
        performSwipe(x, (int) (size.getHeight() * 0.72), x, (int) (size.getHeight() * 0.42), 500L);
    }

    /**
     * Sector rows (ImageView with a label, e.g. "Financial") and sub-sector rows
     * (View with a label) in the right-hand list, in document order. Sub-sectors
     * after the group row and before the next sector row belong to the group.
     */
    private void readSectorSnapshot(SectorScan scan, String group, String pageSource) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(pageSource)));
        } catch (Exception parseFailure) {
            throw new AssertionError("Could not parse the By Sector picker page source");
        }
        int listLeft = (int) (driver.manage().window().getSize().getWidth() * 0.38);
        NodeList all = document.getElementsByTagName("*");
        List<int[]> boxes = new ArrayList<>();
        List<Boolean> boxTicked = new ArrayList<>();
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (box == null || box[0] < listLeft || !normalize(element.getAttribute("content-desc")).isEmpty()) {
                continue;
            }
            int width = box[2] - box[0];
            int height = box[3] - box[1];
            if (width >= 30 && width <= 80 && height >= 30 && height <= 80) {
                boxes.add(box);
                boxTicked.add(element.getTagName().endsWith("ImageView"));
            }
        }
        boolean inGroup = scan.groupSeen && !scan.groupEnded;
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String desc = normalize(element.getAttribute("content-desc"));
            int[] label = parseBounds(element.getAttribute("bounds"));
            if (desc.isEmpty() || label == null || label[0] < listLeft) {
                continue;
            }
            if (FILTERS_APPLIED_TEXT.matcher(desc).matches() || desc.equals("Reset") || desc.equals("Apply Filters")) {
                continue;
            }
            boolean sectorRow = element.getTagName().endsWith("ImageView");
            if (sectorRow) {
                if (desc.equals(group)) {
                    scan.groupSeen = true;
                    inGroup = true;
                } else if (inGroup) {
                    scan.groupEnded = true;
                    inGroup = false;
                }
                continue;
            }
            if (!inGroup) {
                continue;
            }
            int labelCenterY = (label[1] + label[3]) / 2;
            Boolean ticked = null;
            for (int b = 0; b < boxes.size(); b++) {
                int[] box = boxes.get(b);
                int boxCenterY = (box[1] + box[3]) / 2;
                int boxCenterX = (box[0] + box[2]) / 2;
                // Box on the same row, left of the label's centre (the label node may or may not include the box).
                if (Math.abs(boxCenterY - labelCenterY) <= 40 && boxCenterX < (label[0] + label[2]) / 2
                        && box[0] >= label[0] - 200) {
                    ticked = boxTicked.get(b);
                    break;
                }
            }
            if (ticked != null || !scan.rows.containsKey(desc)) {
                scan.rows.put(desc, ticked);
            }
        }
    }

    /** Saves a page source under test-output/page-sources/ and logs the path. */
    private void dumpPageSource(String name, String source) {
        if (source == null) {
            return;
        }
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "test-output", "page-sources");
            Files.createDirectories(dir);
            Path file = dir.resolve(name.replaceAll("[^A-Za-z0-9_-]+", "_") + ".xml");
            Files.write(file, source.getBytes(StandardCharsets.UTF_8));
            ReportLogger.info("Page source saved for diagnosis: " + file);
        } catch (Exception writeFailure) {
            ReportLogger.debug("Could not save page source: " + writeFailure.getClass().getSimpleName());
        }
    }

    private void openFiltersSheet(String screenTitle) {
        WebElement filters = requireVisible(FILTERS_BUTTON, "Filters button on '" + screenTitle + "'", SHORT_WAIT_MS);
        safeClick(filters, "Filters button on " + screenTitle);
        requireVisible(FILTERS_APPLIED_LABEL, "'N Filters Applied' label of the Filters sheet", PAGE_READY_WAIT_MS);
        waitForStableNodes(desc -> true);
    }

    /** The Filters sheet has no back button, so the Android Back key closes it. */
    private void closeFiltersSheetWithDeviceBack(String screenTitle) {
        ReportLogger.step("Closing Filters sheet of '" + screenTitle + "' with the device Back key");
        driver.navigate().back();
        boolean closed = waitUntil(() -> !isVisible(FILTERS_APPLIED_LABEL) && isVisible(STOCKS_FILTERED_LABEL),
                SHORT_WAIT_MS);
        if (!closed) {
            throw new AssertionError("Filters sheet of '" + screenTitle + "' did not close with the device Back key; visible="
                    + collectVisibleValues());
        }
        waitForStableNodes(desc -> true);
    }

    /**
     * Reads the whole sheet top to bottom, merging what each scroll position shows.
     * Filter rows start collapsed (name only). Expected range filters that are
     * collapsed and fully on screen are tapped once to expand them, so their
     * from/to boxes can be read. Selection rows are never tapped (they may open a
     * picker); their presence is enough.
     */
    private FilterSheetState readFilterSheet(List<FilterSpec> expected) {
        Set<String> rangeNames = new LinkedHashSet<>();
        for (FilterSpec spec : expected) {
            if (!spec.selection) {
                rangeNames.add(filterKey(spec.name));
            }
        }
        FilterSheetState state = new FilterSheetState();
        String previousScreen = null;
        for (int swipe = 0; swipe <= FILTER_SHEET_MAX_SWIPES; swipe++) {
            String currentScreen = readAndExpand(state, rangeNames);
            if (currentScreen.equals(previousScreen)) {
                return state;
            }
            previousScreen = currentScreen;
            swipeFilterSheet();
        }
        ReportLogger.info("Stopped scrolling the Filters sheet after " + FILTER_SHEET_MAX_SWIPES + " swipes");
        return state;
    }

    /**
     * Reads the current position and expands collapsed expected range rows one at a
     * time (each expansion moves the rows below it, so positions are re-read after
     * every tap). Each row is tapped at most once. Returns the settled signature.
     */
    private String readAndExpand(FilterSheetState state, Set<String> rangeNames) {
        String currentScreen = signature(waitForStableNodes(desc -> true));
        state.lastPageSource = driver.getPageSource();
        mergeFilterSnapshot(state, state.lastPageSource);
        for (int round = 0; round <= rangeNames.size(); round++) {
            String nextName = null;
            int[] nextPoint = null;
            for (Map.Entry<String, int[]> row : state.collapsedOnScreen.entrySet()) {
                String key = filterKey(row.getKey());
                if (rangeNames.contains(key) && findIgnoreCase(state.ranges, row.getKey()) == null
                        && !state.expandAttempts.contains(key)) {
                    nextName = row.getKey();
                    nextPoint = row.getValue();
                    break;
                }
            }
            if (nextName == null || nextPoint == null) {
                break;
            }
            state.expandAttempts.add(filterKey(nextName));
            ReportLogger.step("Expanding collapsed filter row '" + nextName + "'");
            tapAt(nextPoint[0], nextPoint[1]);
            currentScreen = signature(waitForStableNodes(desc -> true));
            state.lastPageSource = driver.getPageSource();
            mergeFilterSnapshot(state, state.lastPageSource);
        }
        return currentScreen;
    }

    /**
     * Scrolls the sheet with a drag along its left margin, away from sliders,
     * input boxes and Low/Mid/High buttons, so no filter value can change.
     */
    private void swipeFilterSheet() {
        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.04);
        performSwipe(x, (int) (size.getHeight() * 0.75), x, (int) (size.getHeight() * 0.40), 500L);
    }

    /**
     * Range filter = a group whose first child is the label and whose next child
     * is the "from" EditText (Quality Score / 8 / to / ... / Low / Mid / High).
     * Selection filter = a single node "<name> N Selected".
     */
    private void mergeFilterSnapshot(FilterSheetState state, String pageSource) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(pageSource)));
        } catch (Exception parseFailure) {
            throw new AssertionError("Could not parse the Filters sheet page source: "
                    + parseFailure.getClass().getSimpleName());
        }
        NodeList all = document.getElementsByTagName("*");
        state.collapsedOnScreen.clear();
        // Visible list area: below the "N Filters Applied" header, above the Add Filter / Update Screen bar.
        int listTop = -1;
        int listBottom = Integer.MAX_VALUE;
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String desc = normalize(element.getAttribute("content-desc"));
            int[] box = parseBounds(element.getAttribute("bounds"));
            if (box == null) {
                continue;
            }
            if (FILTERS_APPLIED_TEXT.matcher(desc).matches() || desc.equals("Reset all")) {
                listTop = Math.max(listTop, box[3]);
            } else if (desc.equals("Update Screen")) {
                listBottom = Math.min(listBottom, box[1]);
            }
        }
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            // Some builds expose the line break as the two characters "\n" instead of a newline.
            String desc = normalize(element.getAttribute("content-desc").replace("\\n", " "));
            if (desc.isEmpty()) {
                continue;
            }
            Matcher applied = FILTERS_APPLIED_TEXT.matcher(desc);
            if (applied.matches()) {
                state.appliedLabel = desc;
                continue;
            }
            Matcher selection = SELECTION_FILTER_TEXT.matcher(desc);
            if (selection.matches()) {
                state.selections.put(selection.group(1), Integer.parseInt(selection.group(2)));
                continue;
            }
            if (!isRangeLabel(element, desc)) {
                if (isCollapsedFilterRow(element, desc)) {
                    state.collapsedNames.add(desc);
                    int[] box = parseBounds(element.getAttribute("bounds"));
                    if (box != null && box[1] > listTop && box[3] < listBottom
                            && !"false".equals(element.getAttribute("displayed"))) {
                        state.collapsedOnScreen.put(desc, new int[]{(box[0] + box[2]) / 2, (box[1] + box[3]) / 2});
                    }
                }
                continue;
            }
            // from = first EditText after the label, to = the next EditText (before the next label).
            String[] values = new String[2];
            int found = 0;
            for (Element sibling = nextElementSibling(element); sibling != null && found < 2;
                 sibling = nextElementSibling(sibling)) {
                if (sibling.getTagName().endsWith("EditText")) {
                    values[found++] = sibling.getAttribute("text").trim();
                } else if (isRangeLabel(sibling, normalize(sibling.getAttribute("content-desc")))) {
                    break;
                }
            }
            String[] known = state.ranges.get(desc);
            if (known == null) {
                state.ranges.put(desc, values);
            } else {
                // A later scroll position may show the box that was off-screen before.
                for (int v = 0; v < 2; v++) {
                    if ((known[v] == null || known[v].isEmpty()) && values[v] != null) {
                        known[v] = values[v];
                    }
                }
            }
        }
    }

    private static final Set<String> SHEET_CHROME = Set.of("Add Filter", "Reset all", "Update Screen",
            "to", "Low", "Mid", "High");

    /**
     * A collapsed row is a named node that is not sheet chrome, is not a slider
     * hint ("Min. 0.0279..."), does not itself contain from/to boxes, and whose
     * parent holds no EditText (an expanded group always has its from/to boxes).
     * Small Cap Growth's expanded Mcap group carries the slider hint
     * "Min. 0.027964264" as its own content-desc, with Mcap and the boxes inside.
     */
    private static boolean isCollapsedFilterRow(Element element, String desc) {
        if (SHEET_CHROME.contains(desc) || FILTERS_APPLIED_TEXT.matcher(desc).matches()
                || desc.startsWith("Min. ") || desc.startsWith("Max. ")) {
            return false;
        }
        NodeList descendants = element.getElementsByTagName("*");
        for (int i = 0; i < descendants.getLength(); i++) {
            if (((Element) descendants.item(i)).getTagName().endsWith("EditText")) {
                return false;
            }
        }
        Node parent = element.getParentNode();
        if (parent instanceof Element) {
            NodeList siblings = ((Element) parent).getChildNodes();
            for (int i = 0; i < siblings.getLength(); i++) {
                Node sibling = siblings.item(i);
                if (sibling instanceof Element && ((Element) sibling).getTagName().endsWith("EditText")) {
                    return false;
                }
            }
        }
        return true;
    }

    /** [left, top, right, bottom] from "[x1,y1][x2,y2]"; null if unreadable. */
    private static int[] parseBounds(String bounds) {
        Matcher m = bounds == null ? null : PAGE_SOURCE_BOUNDS.matcher(bounds);
        if (m == null || !m.find()) {
            return null;
        }
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))};
    }

    /**
     * A range-filter label is followed directly by its "from" EditText. This also
     * covers groups with an extra node in front (Small Cap Growth's
     * "Min. 0.0279..." before Mcap). "to" also precedes an EditText, so it is excluded.
     */
    private static boolean isRangeLabel(Element element, String desc) {
        if (desc.isEmpty() || desc.equals("to") || desc.equals("Low") || desc.equals("Mid") || desc.equals("High")) {
            return false;
        }
        Element next = nextElementSibling(element);
        return next != null && next.getTagName().endsWith("EditText");
    }

    private static Element nextElementSibling(Element element) {
        for (Node node = element.getNextSibling(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            }
        }
        return null;
    }

    private static List<String> compareFilters(FilterSheetState state, List<FilterSpec> expected) {
        List<String> problems = new ArrayList<>();
        Set<String> expectedNames = new LinkedHashSet<>();
        for (FilterSpec spec : expected) {
            expectedNames.add(filterKey(spec.name));
            boolean collapsed = containsIgnoreCase(state.collapsedNames, spec.name);
            if (spec.selection) {
                // Collapsed selection rows show only the name; presence is what is checked.
                if (findIgnoreCase(state.selections, spec.name) == null && !collapsed) {
                    problems.add("missing filter '" + spec.name + "'");
                }
                continue;
            }
            String[] shown = findIgnoreCase(state.ranges, spec.name);
            if (shown == null) {
                problems.add(collapsed
                        ? "filter '" + spec.name + "' did not expand after tapping, so its values could not be read"
                        : "missing filter '" + spec.name + "'");
                continue;
            }
            checkBound(problems, spec.name, "from", shown[0], spec.from);
            checkBound(problems, spec.name, "to", shown[1], spec.to);
        }
        for (String name : state.ranges.keySet()) {
            if (!expectedNames.contains(filterKey(name))) {
                problems.add("unexpected filter '" + name + "'");
            }
        }
        for (String name : state.selections.keySet()) {
            if (!expectedNames.contains(filterKey(name))) {
                problems.add("unexpected filter '" + name + "'");
            }
        }
        for (String name : state.collapsedNames) {
            if (!expectedNames.contains(filterKey(name)) && findIgnoreCase(state.ranges, name) == null
                    && findIgnoreCase(state.selections, name) == null) {
                problems.add("unexpected filter '" + name + "'");
            }
        }
        if (state.appliedLabel == null) {
            problems.add("'N Filters Applied' label not found");
        }
        return problems;
    }

    /** expected null = not checked, ANY = any value (data-driven), "" = box must be empty. */
    private static void checkBound(List<String> problems, String name, String bound, String shown, String expected) {
        if (expected == null || ANY.equals(expected)) {
            return;
        }
        if (shown == null) {
            problems.add("could not read the '" + bound + "' box of '" + name + "'");
        } else if (!sameNumber(shown, expected)) {
            problems.add("'" + name + "' " + bound + " is '" + (shown.isEmpty() ? "empty" : shown) + "', expected '"
                    + (expected.isEmpty() ? "empty" : expected) + "'");
        }
    }

    private static boolean containsIgnoreCase(Set<String> names, String name) {
        for (String candidate : names) {
            if (filterKey(candidate).equals(filterKey(name))) {
                return true;
            }
        }
        return false;
    }

    private static <V> V findIgnoreCase(Map<String, V> map, String name) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (filterKey(entry.getKey()).equals(filterKey(name))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String filterKey(String name) {
        return normalize(name).toLowerCase(Locale.ROOT);
    }

    /** "8" equals "8.0"; non-numeric values are compared as text. */
    private static boolean sameNumber(String shown, String expected) {
        try {
            return new BigDecimal(shown.replace(",", "")).compareTo(new BigDecimal(expected)) == 0;
        } catch (NumberFormatException notNumeric) {
            return shown.trim().equalsIgnoreCase(expected.trim());
        }
    }

    // =========================================================
    // TC9-TC12: verify table columns on every tab
    // =========================================================

    /**
     * Opens the given screens of a Predefined section (all of them when no
     * titles are passed) and, on every tab except Custom View, scrolls the
     * table header horizontally and verifies the expected columns. Missing
     * columns are collected per screen/tab and reported together at the end;
     * navigation failures stop the test immediately.
     */
    public void verifyPredefinedScreenColumns(String sectionHeading, String... onlyScreenTitles) {
        PredefinedSection section = findSection(sectionHeading);
        List<ScreenCard> screens = selectScreens(section, onlyScreenTitles);
        ReportLogger.step("Verifying table columns for " + screens.size() + " '" + section.heading + "' screen(s)");
        verifyPredefinedAndSavedTabs();

        List<String> failures = new ArrayList<>();
        for (ScreenCard screen : screens) {
            if (!isOnStockScreenerPage()) {
                ReportLogger.info("Not on Stock Screener before opening '" + screen.title + "'; reopening it from Hub");
                openFromHub();
            }
            openPredefinedScreen(screen);
            try {
                verifyDetailHeading(screen.title);
                boolean selectionSupported = isTabSelectionStateSupported();
                for (Map.Entry<DetailTab, List<String>> entry : EXPECTED_COLUMNS.entrySet()) {
                    DetailTab tab = entry.getKey();
                    openDetailTab(tab, selectionSupported, false);
                    checkColumns(screen.title, tab.label, entry.getValue(), failures);
                }
            } catch (AssertionError mismatch) {
                failures.add(screen.title + ": " + clean(mismatch.getMessage()));
                ReportLogger.fail("Screen '" + screen.title + "' check failed: " + clean(mismatch.getMessage()));
            }
            navigateBackToStockScreener(screen.title);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Column checks failed for '" + section.heading + "' (" + failures.size()
                    + " screen/tab combination(s)): " + failures);
        }
        ReportLogger.pass("All expected columns verified on " + screens.size() + " '" + section.heading + "' screen(s)");
    }

    /**
     * SS_027: on every screen of every enabled section, the Custom View tab must show
     * exactly that screen's columns (CUSTOM_VIEW_COLUMNS).
     */
    public void verifyCustomViewColumns() {
        ReportLogger.step("Verifying Custom View columns on every predefined screen");
        verifyPredefinedAndSavedTabs();
        DetailTab customView = DETAIL_TABS.get(5);
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (PredefinedSection section : PREDEFINED_SECTIONS) {
            if (!section.enabled) {
                continue;
            }
            for (ScreenCard screen : section.screens) {
                List<String> expected = CUSTOM_VIEW_COLUMNS.get(screen.title);
                if (expected == null) {
                    throw new IllegalStateException("No Custom View columns defined for '" + screen.title + "'");
                }
                if (!isOnStockScreenerPage()) {
                    ReportLogger.info("Not on Stock Screener before opening '" + screen.title + "'; reopening it from Hub");
                    openFromHub();
                }
                openPredefinedScreen(screen);
                checked++;
                try {
                    verifyDetailHeading(screen.title);
                    openDetailTab(customView, isTabSelectionStateSupported(), false);
                    checkColumns(screen.title, customView.label, expected, failures);
                } catch (AssertionError mismatch) {
                    failures.add(screen.title + ": " + clean(mismatch.getMessage()));
                    ReportLogger.fail("Screen '" + screen.title + "' check failed: " + clean(mismatch.getMessage()));
                }
                navigateBackToStockScreener(screen.title);
            }
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Custom View columns wrong on " + failures.size() + " screen(s) of " + checked
                    + ": " + failures);
        }
        ReportLogger.pass("Custom View columns verified on all " + checked + " predefined screens");
    }

    /** Exact column check of the current tab: reports missing and unexpected columns. */
    private void checkColumns(String screenTitle, String tabLabel, List<String> expected, List<String> failures) {
        Set<String> seen = collectTableHeaders(expected);
        List<String> missing = missingColumns(seen, expected);
        List<String> unexpected = unexpectedColumns(seen, expected);
        if (missing.isEmpty() && unexpected.isEmpty()) {
            ReportLogger.pass("'" + screenTitle + "' / " + tabLabel + ": all " + expected.size()
                    + " columns present, no extra columns");
            return;
        }
        String failure = screenTitle + " / " + tabLabel
                + (missing.isEmpty() ? "" : " missing " + missing)
                + (unexpected.isEmpty() ? "" : " unexpected " + unexpected);
        failures.add(failure);
        ReportLogger.fail("Columns check failed: " + failure);
    }

    private static List<String> unexpectedColumns(Set<String> seen, List<String> expected) {
        List<String> unexpected = new ArrayList<>();
        for (String label : seen) {
            boolean known = false;
            for (String column : expected) {
                if (columnMatches(label, column)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                unexpected.add(label);
            }
        }
        return unexpected;
    }

    private List<ScreenCard> selectScreens(PredefinedSection section, String... titles) {
        if (titles == null || titles.length == 0) {
            return section.screens;
        }
        List<ScreenCard> selected = new ArrayList<>();
        for (String title : titles) {
            ScreenCard match = null;
            for (ScreenCard screen : section.screens) {
                if (screen.title.equalsIgnoreCase(title.trim())) {
                    match = screen;
                }
            }
            if (match == null) {
                throw new IllegalArgumentException("'" + title + "' is not a screen of section '" + section.heading + "'");
            }
            selected.add(match);
        }
        return selected;
    }

    /**
     * Collects every header label of the current table. The table is scrolled right
     * to its end (needed to spot unexpected columns), and back left only if expected
     * columns are still missing (a table that did not start at its left edge). Which
     * row scrolls the table (body or header) is detected once and then reused.
     */
    private Set<String> collectTableHeaders(List<String> expected) {
        WebElement securities = requireVisible(SECURITIES_HEADER, "'Securities' table header", PAGE_READY_WAIT_MS);
        Rectangle header = safeRect(securities);
        if (header == null) {
            throw new AssertionError("Could not read the position of the 'Securities' header");
        }

        Set<String> seen = new LinkedHashSet<>();
        List<UiNode> startNodes = waitForStable(() -> headerRowNodes(header));
        addDescriptions(seen, startNodes);
        String startSignature = signature(startNodes);

        int bodyY = header.getY() + header.getHeight() + header.getHeight() / 2;
        int headerY = header.getY() + header.getHeight() / 2;
        Integer dragY = tableScrollsOnBody == null ? null : (tableScrollsOnBody ? bodyY : headerY);
        if (dragY == null) {
            // First table of this run: find which row scrolls it (table body first, then the header row).
            search:
            for (int candidateY : new int[]{bodyY, headerY}) {
                for (boolean towardLater : new boolean[]{true, false}) {
                    dragTableHorizontally(header, candidateY, towardLater);
                    List<UiNode> moved = waitForStable(() -> headerRowNodes(header));
                    addDescriptions(seen, moved);
                    if (!signature(moved).equals(startSignature)) {
                        dragY = candidateY;
                        tableScrollsOnBody = candidateY == bodyY;
                        break search;
                    }
                }
            }
            if (dragY == null) {
                ReportLogger.info("Table header did not scroll horizontally; checking only the visible columns");
                return seen;
            }
        }

        scrollTableToEnd(header, dragY, true, seen);
        if (!missingColumns(seen, expected).isEmpty()) {
            scrollTableToEnd(header, dragY, false, seen);
        }
        ReportLogger.debug("Table headers collected (" + seen.size() + "): " + seen);
        return seen;
    }

    /** Drags until the header row stops changing; returns true if it moved at all. */
    private boolean scrollTableToEnd(Rectangle header, int dragY, boolean towardLaterColumns, Set<String> seen) {
        String previous = signature(headerRowNodes(header));
        boolean moved = false;
        for (int swipe = 0; swipe < TABLE_MAX_SWIPES_PER_DIRECTION; swipe++) {
            dragTableHorizontally(header, dragY, towardLaterColumns);
            List<UiNode> nodes = waitForStable(() -> headerRowNodes(header));
            addDescriptions(seen, nodes);
            String current = signature(nodes);
            if (current.equals(previous)) {
                return moved;
            }
            moved = true;
            previous = current;
        }
        ReportLogger.info("Stopped horizontal table scrolling after " + TABLE_MAX_SWIPES_PER_DIRECTION + " swipes");
        return moved;
    }

    /** Header-row labels: displayed nodes whose vertical centre lies inside the 'Securities' header row. */
    private List<UiNode> headerRowNodes(Rectangle header) {
        int top = header.getY();
        int bottom = header.getY() + header.getHeight();
        List<UiNode> row = new ArrayList<>();
        for (UiNode node : displayedDescribedNodes(desc -> !DETAIL_TAB_IDS.contains(desc))) {
            if (node.centerY >= top && node.centerY <= bottom) {
                row.add(node);
            }
        }
        return row;
    }

    /**
     * Drags the table sideways right of the fixed 'Securities' column. The finger
     * holds still before lifting, so the table does not fling past columns.
     */
    private void dragTableHorizontally(Rectangle securitiesHeader, int y, boolean towardLaterColumns) {
        int screenWidth = driver.manage().window().getSize().getWidth();
        int scrollableLeft = securitiesHeader.getX() + securitiesHeader.getWidth();
        int right = screenWidth - 30;
        int distance = (int) ((right - scrollableLeft) * TABLE_DRAG_FRACTION);
        int startX = towardLaterColumns ? right : right - distance;
        int endX = towardLaterColumns ? right - distance : right;

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 1);
        drag.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
        drag.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        drag.addAction(new Pause(finger, Duration.ofMillis(80L)));
        drag.addAction(finger.createPointerMove(Duration.ofMillis(TABLE_DRAG_MS), PointerInput.Origin.viewport(), endX, y));
        drag.addAction(new Pause(finger, Duration.ofMillis(TABLE_DRAG_HOLD_MS)));
        drag.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(drag));
    }

    private static void addDescriptions(Set<String> seen, List<UiNode> nodes) {
        for (UiNode node : nodes) {
            seen.add(node.desc);
        }
    }

    private static List<String> missingColumns(Set<String> seen, List<String> expected) {
        List<String> missing = new ArrayList<>();
        for (String column : expected) {
            boolean found = false;
            for (String label : seen) {
                if (columnMatches(label, column)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(column);
            }
        }
        return missing;
    }

    /** Case-, space- and dot-insensitive; a multi-line header matches on its first line. */
    private static boolean columnMatches(String label, String expected) {
        if (columnKey(label).equals(columnKey(expected))) {
            return true;
        }
        return normalize(label).toLowerCase(Locale.ROOT)
                .startsWith(normalize(expected).toLowerCase(Locale.ROOT) + " ");
    }

    private static String columnKey(String value) {
        return normalize(value).toLowerCase(Locale.ROOT).replaceAll("[\\s.]", "");
    }

    // =========================================================
    // Hub scroll / tile discovery
    // =========================================================

    private WebElement findStockScreenerTileWithBoundedScroll() {
        WebElement direct = firstDisplayed(STOCK_SCREENER_TILE);
        if (direct != null) {
            ReportLogger.debug("Stock Screener Hub tile is already visible - no Hub scroll required");
            return direct;
        }

        String previous = hubOpenTileFingerprint();
        int noProgress = 0;

        // Scroll DOWN (toward bottom of Hub content)
        for (int i = 0; i < 7; i++) {
            swipeVertical(true);
            String current = descriptionsOf(waitForStableNodes(desc -> desc.startsWith(HUB_TILE_PREFIX)));

            direct = firstDisplayed(STOCK_SCREENER_TILE);
            if (direct != null) {
                ReportLogger.step("Stock Screener Hub tile found after downward Hub scroll attempt " + (i + 1));
                return direct;
            }

            if (!current.isEmpty() && current.equals(previous)) {
                noProgress++;
            } else {
                noProgress = 0;
            }
            previous = current;

            if (noProgress >= 2) {
                break;
            }
        }

        // Recovery: scroll UP if we went too far
        previous = hubOpenTileFingerprint();
        noProgress = 0;
        for (int i = 0; i < 6; i++) {
            swipeVertical(false);
            String current = descriptionsOf(waitForStableNodes(desc -> desc.startsWith(HUB_TILE_PREFIX)));

            direct = firstDisplayed(STOCK_SCREENER_TILE);
            if (direct != null) {
                ReportLogger.step("Stock Screener Hub tile found after upward Hub recovery attempt " + (i + 1));
                return direct;
            }

            if (!current.isEmpty() && current.equals(previous)) {
                noProgress++;
            } else {
                noProgress = 0;
            }
            previous = current;

            if (noProgress >= 2) {
                break;
            }
        }

        return null;
    }

    private String hubOpenTileFingerprint() {
        return descriptionsOf(displayedDescribedNodes(desc -> desc.startsWith(HUB_TILE_PREFIX)));
    }

    private boolean isHubScreenReady() {
        return isVisible(HUB_TAB)
                && isVisible(HUB_SCROLL_VIEW)
                && !isVisible(BACK_BUTTON)
                && (isVisible(HUB_OPEN_BUTTONS) || isVisible(HUB_SECTIONS));
    }

    private boolean waitForHubContainerAfterNavigation(long timeoutMs) {
        return waitUntil(this::isHubScreenReady, timeoutMs);
    }

    // =========================================================
    // Explicit-wait helpers (no Thread.sleep)
    // =========================================================

    /** Polls the condition with WebDriverWait; returns false on timeout instead of throwing. */
    private boolean waitUntil(BooleanSupplier condition, long timeoutMs) {
        try {
            new WebDriverWait(driver, Duration.ofMillis(timeoutMs), Duration.ofMillis(POLL_MS))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> condition.getAsBoolean());
            return true;
        } catch (TimeoutException timeout) {
            return false;
        }
    }

    private WebElement waitForVisible(By locator, long timeoutMs) {
        try {
            return new WebDriverWait(driver, Duration.ofMillis(timeoutMs), Duration.ofMillis(POLL_MS))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> firstDisplayed(locator));
        } catch (TimeoutException timeout) {
            return null;
        }
    }

    private WebElement requireVisible(By locator, String name, long timeoutMs) {
        WebElement element = waitForVisible(locator, timeoutMs);
        if (element == null) {
            throw new AssertionError(name + " is not visible; visible=" + collectVisibleValues());
        }
        return element;
    }

    /**
     * Waits until two consecutive snapshots of the matching labels and their
     * bounds are identical, i.e. scrolling/animation has stopped. Returns the
     * latest snapshot. Never fails: if the UI keeps moving it continues after
     * SETTLE_TIMEOUT_MS with what it has.
     */
    private List<UiNode> waitForStableNodes(Predicate<String> filter) {
        return waitForStable(() -> displayedDescribedNodes(filter));
    }

    /** Same settle logic for any snapshot of nodes (e.g. only the table header row). */
    private List<UiNode> waitForStable(Supplier<List<UiNode>> snapshot) {
        AtomicReference<String> lastSignature = new AtomicReference<>();
        AtomicReference<List<UiNode>> lastNodes = new AtomicReference<>(Collections.<UiNode>emptyList());
        try {
            new WebDriverWait(driver, Duration.ofMillis(SETTLE_TIMEOUT_MS), Duration.ofMillis(SETTLE_POLL_MS))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        List<UiNode> nodes = snapshot.get();
                        String signature = signature(nodes);
                        boolean stable = signature.equals(lastSignature.get());
                        lastSignature.set(signature);
                        lastNodes.set(nodes);
                        return stable;
                    });
        } catch (TimeoutException stillMoving) {
            ReportLogger.debug("UI did not fully settle within " + SETTLE_TIMEOUT_MS + " ms; using latest snapshot");
        }
        return lastNodes.get();
    }

    /**
     * Reads every displayed node that has a content-desc from one page-source
     * call (much faster than one findElements per locator). Falls back to
     * element lookups if the XML cannot be parsed.
     */
    private List<UiNode> displayedDescribedNodes(Predicate<String> filter) {
        // Session/driver failures propagate so the infrastructure retry can react.
        String source = driver.getPageSource();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
            NodeList all = document.getElementsByTagName("*");
            List<UiNode> nodes = new ArrayList<>();
            for (int i = 0; i < all.getLength(); i++) {
                Element element = (Element) all.item(i);
                String desc = normalize(element.getAttribute("content-desc"));
                // A missing "displayed" attribute is treated as displayed.
                if (desc.isEmpty() || "false".equals(element.getAttribute("displayed")) || !filter.test(desc)) {
                    continue;
                }
                String bounds = element.getAttribute("bounds");
                nodes.add(new UiNode(desc, bounds, "true".equals(element.getAttribute("selected")),
                        centerYOfBounds(bounds)));
            }
            return nodes;
        } catch (Exception parseFailure) {
            ReportLogger.debug("Page source parse failed (" + parseFailure.getClass().getSimpleName()
                    + "); falling back to element lookup");
            return displayedDescribedNodesFromElements(filter);
        }
    }

    private List<UiNode> displayedDescribedNodesFromElements(Predicate<String> filter) {
        List<UiNode> nodes = new ArrayList<>();
        for (WebElement element : safeFindElements(ANY_DESCRIBED_NODE)) {
            if (!isDisplayed(element)) {
                continue;
            }
            String desc = normalize(safeAttribute(element, "content-desc"));
            if (desc.isEmpty() || !filter.test(desc)) {
                continue;
            }
            Rectangle rect = safeRect(element);
            String bounds = rect == null ? "" : rect.getX() + "," + rect.getY() + "," + rect.getWidth() + "," + rect.getHeight();
            nodes.add(new UiNode(desc, bounds, "true".equalsIgnoreCase(safeAttribute(element, "selected")),
                    rect == null ? -1 : rect.getY() + rect.getHeight() / 2));
        }
        return nodes;
    }

    /** Centre Y of a page-source bounds value like "[0,823][487,980]"; -1 if unreadable. */
    private static int centerYOfBounds(String bounds) {
        Matcher m = bounds == null ? null : PAGE_SOURCE_BOUNDS.matcher(bounds);
        if (m == null || !m.find()) {
            return -1;
        }
        return (Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(4))) / 2;
    }

    private static String signature(List<UiNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (UiNode node : nodes) {
            sb.append(node.desc).append('@').append(node.bounds).append('|');
        }
        return sb.toString();
    }

    private static String descriptionsOf(List<UiNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (UiNode node : nodes) {
            sb.append(node.desc).append('|');
        }
        return sb.toString();
    }

    /** Straightens curly quotes and collapses whitespace so labels compare reliably. */
    private static String normalize(String value) {
        if (value == null) return "";
        return value.replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u201C', '"').replace('\u201D', '"')
                .replaceAll("\\s+", " ").trim();
    }

    // =========================================================
    // Shared helpers
    // =========================================================

    private void assertDriverAlive(String context) {
        if (driver == null || driver.getSessionId() == null) {
            throw new IllegalStateException("AndroidDriver session is unavailable " + context);
        }
    }

    private boolean isVisible(By locator) {
        return firstDisplayed(locator) != null;
    }

    private WebElement firstDisplayed(By locator) {
        try {
            for (WebElement element : driver.findElements(locator)) {
                if (element != null && element.isDisplayed()) {
                    return element;
                }
            }
        } catch (StaleElementReferenceException | NoSuchElementException ignored) {
        }
        return null;
    }

    private WebElement findElementIfVisible(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            if (element != null && element.isDisplayed()) {
                return element;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private WebElement bottomMostVisible(By locator) {
        WebElement best = null;
        int bestY = -1;
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element != null && element.isDisplayed()) {
                    int y = element.getRect().getY();
                    if (y > bestY) {
                        bestY = y;
                        best = element;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private void safeClick(WebElement element, String description) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (element != null && element.isEnabled()) {
                    driver.executeScript("mobile: clickGesture",
                            Map.of("elementId", ((RemoteWebElement) element).getId()));
                    return;
                }
            } catch (StaleElementReferenceException stale) {
                if (attempt == 2) throw stale;
            }
        }
        throw new AssertionError("Failed to click " + description + " after retries");
    }

    private void swipeVertical(boolean downFeed) {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * (downFeed ? 0.78 : 0.46));
        int endY = (int) (size.getHeight() * (downFeed ? 0.44 : 0.80));
        performSwipe(x, startY, x, endY, 480L);
    }

    private void performSwipe(int startX, int startY, int endX, int endY, long durationMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(80L)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    private String collectVisibleValues() {
        StringBuilder sb = new StringBuilder();
        for (WebElement element : safeFindElements(AppiumBy.className("android.view.View"))) {
            try {
                if (element.isDisplayed()) {
                    String desc = safeAttribute(element, "content-desc");
                    if (!desc.isEmpty()) {
                        sb.append(desc.length() > 60 ? desc.substring(0, 60) + "..." : desc).append("; ");
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }

    private List<WebElement> safeFindElements(By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String safeAttribute(WebElement element, String attribute) {
        try {
            String value = element.getAttribute(attribute);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private Rectangle safeRect(WebElement element) {
        try {
            return element == null ? null : element.getRect();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDisplayed(WebElement element) {
        try {
            return element != null && element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private String clean(String message) {
        if (message == null) return "";
        return message.replaceAll("\\s+", " ").trim();
    }

    // =========================================================
    // Small data holders (Java 11: no records)
    // =========================================================

    private static ScreenCard card(String title, String descriptionPrefix) {
        return new ScreenCard(title, descriptionPrefix, OVERVIEW_TAB);
    }

    private static ScreenCard card(String title, String descriptionPrefix, DetailTab openingTab) {
        return new ScreenCard(title, descriptionPrefix, openingTab);
    }

    private static Map<String, List<FilterSpec>> defaultFilters() {
        Map<String, List<FilterSpec>> filters = new LinkedHashMap<>();
        // TC13 - Value Research Ratings
        filters.put("Top Rated Stocks", List.of(selection("Stock Rating")));
        filters.put("High Quality Small Caps", List.of(range("Quality Score", "8"), range("Growth Score", "4"),
                range("Valuation Score", "4"), selection("By Capitalisation")));
        filters.put("High Growth Large Caps", List.of(range("Growth Score", "7"), selection("By Capitalisation")));
        filters.put("Value Meets Momentum", List.of(range("Quality Score", "4"), range("Valuation Score", "6"),
                range("Momentum Score", "7"), range("Mcap", "500")));
        filters.put("Top Rated Banks", List.of(selection("By Sector"), selection("Stock Rating")));
        // TC14 - Momentum Picks
        filters.put("Top Momentum Stocks", List.of(range("Quality Score", "5"), range("Momentum Score", "8"),
                range("Mcap", "500")));
        filters.put("High Momentum Large Caps", List.of(range("Momentum Score", "7"), selection("By Capitalisation")));
        filters.put("High Momentum Mid Caps", List.of(range("Quality Score", "4"), range("Momentum Score", "8"),
                selection("By Capitalisation")));
        // Values confirmed from the app (Inspector screenshot 27 Oct); the sheet lists no values.
        filters.put("High Momentum Small Caps", List.of(range("Quality Score", "6"), range("Momentum Score", "8"),
                range("Mcap", "500"), selection("By Capitalisation")));

        // TC15 - Investment Ideas (values from the app, Inspector 27 Oct; "" = empty box)
        filters.put("Attractive Bluechips", List.of(range("Mcap", ANY, ""), range("D/E", "0", "2"),
                range("Int Coverage", "2", ""), range("ROE 5Y Avg", "20", ""), range("5Y EPS Growth", "20", ""),
                range("PEG (5Y)", "0", "1.5"), selection("5Y consistent ROE >20%")));
        filters.put("Book Value Discount", List.of(range("Mcap", "500", ""), range("D/E", "0", "1.5"),
                range("ROE", "10"), range("5Y EPS Growth", "10", ""), range("P/B", "0.01", "1")));
        filters.put("Growth at Reasonable PE", List.of(range("Mcap", "500", ""), range("EPS YoY (Qtr)", "20", ""),
                range("1Y EPS Growth", "20", ""), range("5Y EPS Growth", "20", ""), range("P/E", "1", "15")));
        filters.put("High Dividend Yield", List.of(range("Mcap", "500", ""), range("Div Payout", "0", "40"),
                range("Div Yield", "3", ""), selection("5Y consistent dividend")));
        filters.put("Cheap Quality Stocks", List.of(range("Mcap", "500", ""), range("Altman Z-score", "2.99", ""),
                range("Piotroski F-score", "7", ""), range("Modified C-score", "", "4"),
                range("Earnings Yield", "5", ""), range("PEG (5Y)", "0", "1"), range("PE / Median PE", "", "1.5")));
        // Mcap upper bound (22849 on 27 Oct) is a market-data cut-off, so it is not asserted.
        filters.put("Small Cap Growth", List.of(range("Mcap", "0", ANY), range("D/E", "0", "1"),
                range("ROE 3Y Avg", "20", ""), range("3Y Revenue Growth", "20", ""), range("3Y EPS Growth", "20", ""),
                range("PEG (5Y)", "0", "1")));

        // TC16 - Value guru screens
        // Ben Graham's ROE shows the data range (-3416.3 to 1041.1813 on 27 Oct), so only presence is checked.
        filters.put("Ben Graham", List.of(range("Mcap", "500", ""), range("D/E", "0", "1"),
                range("Current Ratio", "2", ""), present("ROE"), range("ROE 5Y Avg", "12", ""),
                range("5Y EPS Growth", "10", ""), range("Earnings Yield", "10.5", ""),
                selection("Debt < 2x (Working Cap)")));
        filters.put("Joel Greenblatt", List.of(range("ROCE", "", ""), range("Earnings Yield", "", ""),
                range("Magic Formula", "", "31"), selection("5Y consistent ROCE >20%")));
        // "3Y Postive FCF" is spelt this way in the app.
        filters.put("John Neff", List.of(range("Mcap", "500", ""), range("ROE", "15", ""),
                range("5Y Revenue Growth", "7", "50"), range("5Y EPS Growth", "7", "50"), range("P/E", "", "15"),
                range("Neff Total Return", "1", ""), selection("3Y Postive FCF")));
        filters.put("Peter Lynch", List.of(range("Mcap", "500", ""), range("D/E", "0", "1"), range("ROE", "15", ""),
                range("5Y EPS Growth", "15", "30"), range("Institutional Stake", "", "30"), range("P/E", "", "15"),
                selection("Inventory change < 1.2x (Revenue growth)")));
        // "Excl. Sevice Comp." is spelt this way in the app.
        filters.put("Walter Schloss", List.of(range("Mcap", "500", ""), range("D/E", "0", "0.3"),
                range("ROCE 3Y Avg", "12", ""), range("Promoter's Stake", "25", ""), range("P/B", "", "1"),
                selection("10Y History Company"), selection("Excl. Sevice Comp.")));
        return Collections.unmodifiableMap(filters);
    }

    /** Range filter, only the "from" box checked. */
    private static FilterSpec range(String name, String from) {
        return new FilterSpec(name, from, null, false);
    }

    /** Range filter, both boxes checked ("" = empty box, ANY = data-driven value). */
    private static FilterSpec range(String name, String from, String to) {
        return new FilterSpec(name, from, to, false);
    }

    /** Range filter whose values are not confirmed yet: only its presence is checked. */
    private static FilterSpec present(String name) {
        return new FilterSpec(name, ANY, ANY, false);
    }

    private static FilterSpec selection(String name) {
        return new FilterSpec(name, null, null, true);
    }

    private static Map<DetailTab, List<String>> expectedColumns() {
        Map<DetailTab, List<String>> columns = new LinkedHashMap<>();
        columns.put(DETAIL_TABS.get(0), List.of("Securities", "Price (₹)", "1D Change (%)", "52W Low (₹)",
                "52W High (₹)", "3Y Low (₹)", "3Y High (₹)", "5Y Low (₹)", "5Y High (₹)", "All Time Low (₹)",
                "All Time High (₹)", "Mcap (CR)", "EV (CR)"));
        columns.put(DETAIL_TABS.get(1), List.of("Securities", "1W Return (%)", "1M Return (%)", "3M Return (%)",
                "1Y Return (%)", "3Y Return (%)", "5Y Return (%)", "10Y Return (%)"));
        // Neff Total Return, PE / Median PE and PB / Median PB were removed from Valuation (bug fix).
        columns.put(DETAIL_TABS.get(2), List.of("Securities", "P/E", "Median P/E", "P/B", "Median P/B",
                "Earnings Yield (%)", "PEG (5Y)", "Div. Yield (%)", "EV/EBITDA", "Price/Sales", "Price/CFO"));
        // EPS (₹) is back in Per share data (bug fix).
        columns.put(DETAIL_TABS.get(3), List.of("Securities", "EPS (₹)", "BVPS (₹)", "CFO Per Share (₹)",
                "FCF Per Share (₹)", "Div. Per Share (₹)"));
        columns.put(DETAIL_TABS.get(4), List.of("Securities", "Stock Rating", "Quality Score", "Growth Score",
                "Valuation Score", "Momentum Score", "Mcap Category"));
        // Custom View (tab 6) differs per screen: see CUSTOM_VIEW_COLUMNS (SS_027).
        return Collections.unmodifiableMap(columns);
    }

    /**
     * SS_029: columns offered by Custom View -> Columns -> Add Columns, per category
     * (labels without units, as the panel shows them: "PEG", not "PEG (5Y)").
     */
    static final Map<String, List<String>> ADD_COLUMNS_EXPECTED = addColumnsExpected();

    private static Map<String, List<String>> addColumnsExpected() {
        Map<String, List<String>> c = new LinkedHashMap<>();
        c.put("Overview", List.of("Price", "1D Change", "52W Low", "52W High", "3Y Low", "3Y High", "5Y Low", "5Y High",
                "All Time Low", "All Time High", "Mcap", "EV"));
        c.put("Return", List.of("1W Return", "1M Return", "3M Return", "1Y Return", "3Y Return", "5Y Return",
                "10Y Return"));
        c.put("Valuation", List.of("P/E", "Median P/E", "P/B", "Median P/B", "Earnings Yield", "PEG",
                "Div. Yield", "EV/EBITDA", "Price/Sales", "Price/CFO"));
        c.put("Per share data", List.of("EPS", "BVPS", "CFO Per Share", "FCF Per Share", "Div. Per Share"));
        c.put("Stock Rating", List.of("Stock Rating", "Quality Score", "Growth Score", "Valuation Score",
                "Momentum Score", "Mcap Category"));
        return Collections.unmodifiableMap(c);
    }

    /** Common Custom View columns, then each screen's own. */
    private static Map<String, List<String>> customViewColumns() {
        Map<String, List<String>> c = new LinkedHashMap<>();
        // Value Research Ratings
        c.put("Top Rated Stocks", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)", "PEG (5Y)",
                "Stock Rating"));
        c.put("High Quality Small Caps", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Quality Score", "Growth Score", "Valuation Score"));
        c.put("High Growth Large Caps", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Growth Score"));
        c.put("Value Meets Momentum", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Quality Score", "Valuation Score", "Momentum Score"));
        c.put("Top Rated Banks", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)", "PEG (5Y)",
                "Stock Rating"));
        // Momentum Picks
        c.put("Top Momentum Stocks", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Quality Score", "Momentum Score"));
        c.put("High Momentum Large Caps", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Momentum Score"));
        c.put("High Momentum Mid Caps", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Quality Score", "Momentum Score"));
        c.put("High Momentum Small Caps", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)", "Quality Score", "Momentum Score"));
        // Investment Ideas
        c.put("Attractive Bluechips", custom("5Y EPS Growth (%)", "D/E", "Int Coverage", "ROE 5Y Avg (%)",
                "PEG (5Y)"));
        c.put("Book Value Discount", custom("P/B", "ROE (%)", "D/E", "5Y EPS Growth (%)"));
        c.put("Growth at Reasonable PE", custom("5Y EPS Growth (%)", "1Y EPS Growth (%)", "EPS YoY (Qtr) (%)", "P/E"));
        c.put("High Dividend Yield", custom("Div Payout (%)", "Div. Yield (%)"));
        c.put("Cheap Quality Stocks", custom("Altman Z-score", "Piotroski F-score", "Modified C-score", "PEG (5Y)",
                "PE / Median PE", "Earnings Yield (%)"));
        c.put("Small Cap Growth", custom("3Y EPS Growth (%)", "3Y Revenue Growth (%)", "ROE 3Y Avg (%)", "D/E",
                "PEG (5Y)"));
        // Value guru screens
        c.put("Ben Graham", custom("D/E", "Current Ratio", "ROE (%)", "ROE 5Y Avg (%)", "5Y EPS Growth (%)",
                "Earnings Yield (%)"));
        c.put("Joel Greenblatt", custom("Earnings Yield (%)", "ROCE (%)", "Magic Formula"));
        c.put("John Neff", custom("ROE (%)", "5Y Revenue Growth (%)", "5Y EPS Growth (%)", "P/E",
                "Neff Total Return (%)"));
        c.put("Peter Lynch", custom("D/E", "ROE (%)", "5Y EPS Growth (%)", "Institutional Stake (%)", "P/E"));
        c.put("Walter Schloss", custom("D/E", "ROCE 3Y Avg (%)", "Promoter's Stake (%)", "P/B"));
        // Seasonal (52 Week High / Low show the common columns only)
        c.put("at 52 Week High", custom());
        c.put("at 3 Year High", custom("3Y High (₹)", "3Y Low (₹)"));
        c.put("at 5 Year High", custom("5Y High (₹)", "5Y Low (₹)"));
        c.put("at All Time High", custom("All Time High (₹)", "All Time Low (₹)"));
        c.put("at 52 Week Low", custom());
        c.put("at 3 Year Low", custom("3Y High (₹)", "3Y Low (₹)"));
        c.put("at 5 Year Low", custom("5Y High (₹)", "5Y Low (₹)"));
        c.put("at All Time Low", custom("All Time High (₹)", "All Time Low (₹)"));
        return Collections.unmodifiableMap(c);
    }

    private static List<String> custom(String... ownColumns) {
        List<String> columns = new ArrayList<>(List.of("Securities", "1D Change (%)", "52W Low (₹)", "52W High (₹)",
                "Mcap (CR)", "EV (CR)"));
        columns.addAll(List.of(ownColumns));
        return Collections.unmodifiableList(columns);
    }

    private static Set<String> detailTabIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (DetailTab tab : DETAIL_TABS) {
            ids.add(tab.accessibilityId);
        }
        return Collections.unmodifiableSet(ids);
    }

    private static final class DetailTab {
        private final String label;
        private final int index;
        private final String accessibilityId;
        private final By locator;

        private DetailTab(String label, int index, String accessibilityId) {
            this.label = label;
            this.index = index;
            this.accessibilityId = accessibilityId;
            this.locator = AppiumBy.accessibilityId(accessibilityId);
        }
    }

    private static final class PredefinedSection {
        private final String heading;
        private final boolean enabled;
        private final List<ScreenCard> screens;

        private PredefinedSection(String heading, List<ScreenCard> screens) {
            this(heading, true, screens);
        }

        private PredefinedSection(String heading, boolean enabled, List<ScreenCard> screens) {
            this.heading = heading;
            this.enabled = enabled;
            this.screens = screens;
        }
    }

    private static final class ScreenCard {
        private final String title;
        private final String descriptionPrefix;
        private final DetailTab openingTab;

        private ScreenCard(String title, String descriptionPrefix, DetailTab openingTab) {
            this.title = title;
            this.descriptionPrefix = descriptionPrefix;
            this.openingTab = openingTab;
        }
    }

    /** Which detail tabs are visible in the rail right after a screen opens. */
    private static final class OpeningTabState {
        private final List<String> visibleIds;
        private final String selectedId;

        private OpeningTabState(List<String> visibleIds, String selectedId) {
            this.visibleIds = visibleIds;
            this.selectedId = selectedId;
        }

        private boolean opensOn(DetailTab expected) {
            if (selectedId != null) {
                return selectedId.equals(expected.accessibilityId);
            }
            if (expected == OVERVIEW_TAB) {
                return visibleIds.contains(OVERVIEW_TAB.accessibilityId);
            }
            return visibleIds.contains(expected.accessibilityId)
                    && !visibleIds.contains(OVERVIEW_TAB.accessibilityId);
        }

        private String describe() {
            return "selected tab=" + (selectedId == null ? "not exposed" : selectedId)
                    + ", visible tabs=" + visibleIds;
        }
    }

    private static final class FilterSpec {
        private final String name;
        private final String from;
        private final String to;
        private final boolean selection;

        private FilterSpec(String name, String from, String to, boolean selection) {
            this.name = name;
            this.from = from;
            this.to = to;
            this.selection = selection;
        }
    }

    /** What the Filters sheet shows: range filters with their "from" value, selection filters with their count. */
    private static final class FilterSheetState {
        private final Map<String, String[]> ranges = new LinkedHashMap<>();
        private final Map<String, Integer> selections = new LinkedHashMap<>();
        private String appliedLabel;
        private String lastPageSource;
        // Names seen as collapsed rows anywhere, and the ones on screen now (centre point for tapping).
        private final Set<String> collapsedNames = new LinkedHashSet<>();
        private final Map<String, int[]> collapsedOnScreen = new LinkedHashMap<>();
        private final Set<String> expandAttempts = new LinkedHashSet<>();

        private String describe() {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, String[]> range : ranges.entrySet()) {
                parts.add(range.getKey() + " (" + shownValue(range.getValue()[0]) + " to "
                        + shownValue(range.getValue()[1]) + ")");
            }
            for (Map.Entry<String, Integer> selection : selections.entrySet()) {
                parts.add(selection.getKey() + " (" + selection.getValue() + " selected)");
            }
            for (String name : collapsedNames) {
                if (!containsKey(ranges, name) && !containsKey(selections, name)) {
                    parts.add(name + " (collapsed)");
                }
            }
            return (appliedLabel == null ? "no applied-count label" : appliedLabel) + ": " + parts;
        }

        private static boolean containsKey(Map<String, ?> map, String name) {
            for (String key : map.keySet()) {
                if (filterKey(key).equals(filterKey(name))) {
                    return true;
                }
            }
            return false;
        }

        private static String shownValue(String value) {
            return value == null ? "?" : value.isEmpty() ? "-" : value;
        }
    }

    /** Sub-sector tick states read from the By Sector picker (null = box not found). */
    private static final class SectorScan {
        private final Map<String, Boolean> rows = new LinkedHashMap<>();
        private boolean groupSeen;
        private boolean groupEnded;
    }

    private static final class UiNode {
        private final String desc;
        private final String bounds;
        private final boolean selected;
        private final int centerY;

        private UiNode(String desc, String bounds, boolean selected, int centerY) {
            this.desc = desc;
            this.bounds = bounds;
            this.selected = selected;
            this.centerY = centerY;
        }
    }
}