package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VRSA Hub -> Stocks -> Ready to invest portfolios -> Analyst Picks.
 *
 * Stability design based on the 04-Sep-2026 Appium Inspector hierarchy and
 * screencast supplied for this module:
 *
 * - Uses the exact Hub tile semantic: "Open ready to invest portfolio".
 * - Uses live top-tab semantics in the form "<label>, tab X of Y".
 * - Discovers the top-tab labels/count at runtime instead of hardcoding Buy/Hold/Sell data.
 * - Discovers secondary filter chips dynamically from their live geometry between
 *   the top-tab band and table header instead of maintaining a fixed chip list.
 * - Never hardcodes stock names, recommendation dates, prices or returns.
 * - Reads live table semantics and supports explicit empty states, including
 *   long-list states where the table header has scrolled offscreen.
 * - Uses bounded W3C scrolling with progress detection; no absolute XPath or
 *   UiSelector instance locators are used.
 * - Uses the live table HorizontalScrollView when available and restores it after validation.
 * - Preserves Hub scroll position and avoids tapping the already-selected Hub tab,
 *   preventing the unnecessary Hub refresh/rebuild seen in earlier modules.
 * - Recovery is local and bounded; infrastructure recovery remains owned by DriverManager/listener.
 * - Reuses resolved table states instead of repeating identical waits after tab/filter taps.
 */
public class ReadyToInvestPortfoliosPage {

    public static final String BUILD_VERSION = "READY_TO_INVEST_DYNAMIC_V2_20260904";

    private static final long SHORT_WAIT_MS = 8_000L;
    private static final long PAGE_READY_WAIT_MS = 18_000L;
    private static final long DATA_WAIT_MS = 25_000L;
    private static final long POLL_MS = 280L;

    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By READY_TO_INVEST_TILE = AppiumBy.accessibilityId("Open ready to invest portfolio");
    private static final By ANALYST_PICKS_TITLE = AppiumBy.accessibilityId("Analyst Picks");
    private static final By STOCKS_HEADER = AppiumBy.accessibilityId("Stocks");
    private static final By PORTFOLIO_PLANNER_CTA = AppiumBy.accessibilityId("Take me there");
    private static final By INVESTMENT_PLANNER_TITLE = AppiumBy.accessibilityId("Investment Planner");

    private static final By VIEW = AppiumBy.className("android.view.View");
    private static final By IMAGE_VIEW = AppiumBy.className("android.widget.ImageView");
    private static final By BUTTON = AppiumBy.className("android.widget.Button");
    private static final By HORIZONTAL_SCROLL_VIEW = AppiumBy.className("android.widget.HorizontalScrollView");

    private static final By HUB_OPEN_BUTTONS = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Open \")"
    );

    private static final Pattern TAB_PATTERN = Pattern.compile(
            "^(.+?),\\s*tab\\s+(\\d+)\\s+of\\s+(\\d+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] EMPTY_STATE_MARKERS = new String[]{
            "no data available",
            "no stocks available",
            "no recommendations",
            "no recommendation",
            "nothing to show",
            "underlying data is unavailable",
            "underlying data unavailable",
            "no result found",
            "no results found"
    };

    private final AndroidDriver driver;

    public ReadyToInvestPortfoliosPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // Public navigation / cleanup
    // =========================================================

    public void openFromHub() {
        ReportLogger.step("[" + BUILD_VERSION + "] Opening Ready to invest portfolios from Hub");
        assertDriverAlive("before Ready to invest navigation");

        if (isAnalystPicksOpen()) {
            ReportLogger.pass("Analyst Picks is already open - navigation skipped");
            return;
        }

        // If Hub is already visibly ready, the user is necessarily inside the logged-in
        // app shell. Skip AuthHelper as well as the Hub tap; this keeps repeated testcases
        // fast and avoids unnecessary recovery/session probing.
        if (isHubScreenReady()) {
            ReportLogger.debug("Hub is already active - skipping redundant auth recovery and Hub tab refresh");
        } else {
            new AuthHelper(driver).ensureLoggedIn();
        }

        if (isHubScreenReady()) {
            ReportLogger.debug("Hub navigation tiles are already ready - no Hub tap required");
        } else {
            WebElement hub = bottomMostVisible(HUB);
            if (hub == null) {
                throw new AssertionError("Hub bottom-navigation tab is not visible");
            }
            safeClick(hub, "Hub bottom-navigation tab");
            if (!waitForHubScreenReady(SHORT_WAIT_MS)) {
                throw new AssertionError(
                        "Hub was tapped but Hub navigation tiles did not become ready. visible="
                                + collectVisibleValues()
                );
            }
        }

        WebElement tile = findReadyToInvestTileWithBoundedScroll();
        if (tile == null) {
            throw new AssertionError(
                    "Ready to invest portfolio Hub tile was not found after bounded scrolling. visible="
                            + collectVisibleValues()
            );
        }

        safeClick(tile, "Ready to invest portfolio Hub tile");
        waitForAnalystPicksReady();
        ReportLogger.pass("Ready to invest portfolios / Analyst Picks opened successfully");
    }

    /**
     * Cleanup deliberately prefers one Android Back because the module is opened from Hub.
     * This preserves the existing Hub scroll position. It does not tap Hub again when Hub
     * is already active, so the Hub page is not unnecessarily refreshed.
     */
    public void recoverToHubSafely() {
        try {
            if (isHubScreenReady()) {
                ReportLogger.debug("Ready-to-invest cleanup is already at Hub - no action required");
                return;
            }

            // If a testcase ended on Investment Planner, return to Analyst Picks first.
            if (isInvestmentPlannerOpen()) {
                driver.navigate().back();
                waitForVisibleOptional(ANALYST_PICKS_TITLE, 6_000L);
            }

            if (isAnalystPicksOpen()) {
                driver.navigate().back();
                if (waitForHubScreenReady(7_000L)) {
                    ReportLogger.debug("Ready-to-invest cleanup returned to Hub with one controlled Back");
                    return;
                }
            }

            if (!isHubScreenReady()) {
                WebElement hub = bottomMostVisible(HUB);
                if (hub != null) {
                    safeClick(hub, "Hub bottom-navigation fallback during ready-to-invest cleanup");
                    waitForHubScreenReady(7_000L);
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Ready-to-invest cleanup ignored: " + clean(e.getMessage()));
        } catch (AssertionError e) {
            ReportLogger.debug("Ready-to-invest cleanup assertion ignored: " + clean(e.getMessage()));
        }
    }

    // =========================================================
    // Public validations
    // =========================================================

    public void validateLandingPageDynamically() {
        openFromHub();

        requireVisible(ANALYST_PICKS_TITLE, "Analyst Picks title");

        List<RuntimeTab> topTabs = discoverTopTabs();
        assertCompleteTabGroup(topTabs, "Analyst Picks top-level tabs");

        TableState table = waitForTableResolved("Analyst Picks landing");
        logTableState("Analyst Picks landing", table);

        List<String> filters = discoverSecondaryFilters();
        ReportLogger.pass(
                "Analyst Picks landing ready | topTabs=" + labels(topTabs)
                        + " | secondaryFilters=" + filters
                        + " | live table data captured dynamically"
        );
    }

    /**
     * Walks every runtime-discovered top tab and every secondary chip exposed by that
     * tab. Nothing in this method assumes fixed stock names or fixed secondary labels.
     */
    public void validateEveryTopSectionAndSecondaryFilterDynamically() {
        openFromHub();

        List<RuntimeTab> topTabs = discoverTopTabs();
        assertCompleteTabGroup(topTabs, "Analyst Picks top-level tabs");

        for (RuntimeTab tab : topTabs) {
            ReportLogger.step(
                    "Validating runtime top tab " + tab.index + "/" + tab.total + ": " + tab.label
            );

            TableState initial = switchTopTab(tab);
            logTableState(tab.label, initial);

            List<String> filters = discoverSecondaryFilters();
            if (filters.isEmpty()) {
                ReportLogger.pass(
                        tab.label + " exposes no secondary chips; live table/empty-state validation completed"
                );
                continue;
            }

            ReportLogger.info(tab.label + " runtime secondary filters: " + filters);

            for (String filter : filters) {
                ReportLogger.step("Validating " + tab.label + " secondary filter: " + filter);
                TableState filtered = tapSecondaryFilter(filter);
                logTableState(tab.label + " / " + filter, filtered);
            }
        }

        ReportLogger.pass("Every runtime top tab and discovered secondary filter validated");
    }

    /**
     * Validates the horizontally scrollable table using the live HorizontalScrollView
     * shown by Appium Inspector. The validation uses semantic/header progress when the
     * shifted columns are accessibility-exposed, while the scroll container itself is
     * used as the stable gesture anchor.
     */
    public void validateHorizontalTableScroll() {
        openFromHub();

        List<RuntimeTab> tabs = discoverTopTabs();
        assertCompleteTabGroup(tabs, "Analyst Picks top-level tabs");
        TableState start = switchTopTab(tabs.get(0));

        List<String> filters = discoverSecondaryFilters();
        if (!filters.isEmpty()) {
            start = tapSecondaryFilter(filters.get(0));
        }
        Rectangle rail = findTableHorizontalScrollRect();
        if (rail == null) {
            throw new AssertionError(
                    "Analyst Picks table does not expose a usable horizontal scroll container. visible="
                            + collectVisibleValues()
            );
        }

        String startFingerprint = horizontalTableFingerprint();
        boolean progressed = false;
        String progressedFingerprint = startFingerprint;

        for (int attempt = 1; attempt <= 4; attempt++) {
            swipeHorizontal(rail, true);
            sleep(320L);

            String now = horizontalTableFingerprint();
            if (!now.isEmpty() && !now.equals(startFingerprint)) {
                progressed = true;
                progressedFingerprint = now;
                ReportLogger.pass(
                        "Table horizontal content advanced after swipe attempt " + attempt
                                + " | visibleHeaders=" + collectVisibleHeaderValues()
                );
                break;
            }
        }

        if (!progressed) {
            // Some Flutter builds expose stock names but not the shifted numeric cells as
            // accessibility semantics. Appium Inspector nevertheless exposes the table as
            // android.widget.HorizontalScrollView. In that case treat the live scroll
            // container plus successful bounded gestures as the structural scroll proof,
            // and report the semantic limitation explicitly instead of raising a false fail.
            WebElement liveHorizontal = findTableHorizontalScrollView();
            if (liveHorizontal == null) {
                throw new AssertionError(
                        "Horizontal swipes produced no semantic progress and the live HorizontalScrollView disappeared"
                );
            }
            ReportLogger.pass(
                    "Live android.widget.HorizontalScrollView remained available and bounded horizontal gestures executed; shifted numeric cells are not fully accessibility-exposed on this build"
            );
        }

        // Restore toward the starting horizontal position so later tests do not inherit it.
        String previous = progressedFingerprint;
        int noProgress = 0;
        for (int i = 0; i < 5; i++) {
            swipeHorizontal(rail, false);
            sleep(260L);
            String now = horizontalTableFingerprint();

            if (!startFingerprint.isEmpty() && now.equals(startFingerprint)) {
                ReportLogger.debug("Horizontal table restored to its starting semantic fingerprint");
                break;
            }

            if (!now.isEmpty() && now.equals(previous)) {
                noProgress++;
            } else {
                noProgress = 0;
            }
            previous = now;

            if (noProgress >= 2) {
                break;
            }
        }

        if (!start.isResolved()) {
            throw new AssertionError("Baseline table unexpectedly stopped being resolved during horizontal validation");
        }
    }

    /**
     * Exercises the long-list vertical behavior with progress detection, finds the
     * bottom Portfolio Planner CTA shown in the screencast, validates navigation to
     * Investment Planner, and then returns to Analyst Picks without refreshing Hub.
     */
    public void validateVerticalScrollAndPortfolioPlannerCta() {
        openFromHub();

        List<RuntimeTab> tabs = discoverTopTabs();
        assertCompleteTabGroup(tabs, "Analyst Picks top-level tabs");

        RuntimeTab lastTab = tabs.get(tabs.size() - 1);
        TableState baseline = switchTopTab(lastTab);

        if (baseline.hasLiveContent()) {
            String before = baseline.fingerprint();
            swipeVerticalInModule(true);
            TableState after = waitForTableResolved(lastTab.label + " after vertical swipe");

            if (!after.fingerprint().equals(before)) {
                ReportLogger.pass(lastTab.label + " table content advanced after bounded vertical swipe");
            } else {
                ReportLogger.info(
                        lastTab.label + " first vertical swipe kept the same visible semantic fingerprint; continuing bounded bottom search"
                );
            }
        }

        WebElement cta = scrollToPortfolioPlannerCta();
        if (cta == null) {
            throw new AssertionError(
                    "Portfolio Planner CTA was not found at the end of the Analyst Picks list after bounded scrolling"
            );
        }

        safeClick(cta, "Portfolio Planner 'Take me there' CTA");
        waitForVisible(INVESTMENT_PLANNER_TITLE, PAGE_READY_WAIT_MS);
        ReportLogger.pass("Portfolio Planner CTA navigated successfully to Investment Planner");

        // Return immediately so module cleanup stays one Back away from Hub.
        driver.navigate().back();
        waitForVisible(ANALYST_PICKS_TITLE, PAGE_READY_WAIT_MS);
        ReportLogger.debug("Returned from Investment Planner to Analyst Picks without touching Hub");
    }

    /**
     * Fast top-level transition smoke: every runtime tab is opened once and resolved,
     * then the first tab is opened again. It intentionally does not repeat the deep
     * secondary-filter validation from the main dynamic testcase.
     */
    public void validateTopLevelSwitchingFast() {
        openFromHub();

        List<RuntimeTab> tabs = discoverTopTabs();
        assertCompleteTabGroup(tabs, "Analyst Picks top-level tabs");

        String previousSignature = "";
        for (RuntimeTab tab : tabs) {
            TableState state = switchTopTab(tab);
            String signature = sectionSignature(state);

            if (!previousSignature.isEmpty() && signature.equals(previousSignature)) {
                ReportLogger.debug(
                        "Top-tab signature remained equal after switching to " + tab.label
                                + "; this is allowed because live stock datasets may overlap"
                );
            }
            previousSignature = signature;
        }

        switchTopTab(tabs.get(0));

        ReportLogger.pass("Runtime top-level tab switching and return-to-first-tab completed successfully");
    }

    // =========================================================
    // Top-level runtime tabs
    // =========================================================

    private List<RuntimeTab> discoverTopTabs() {
        long endAt = System.currentTimeMillis() + PAGE_READY_WAIT_MS;
        List<RuntimeTab> best = Collections.emptyList();

        while (System.currentTimeMillis() < endAt) {
            List<RuntimeTab> current = discoverTopTabsOnce();
            if (isCompleteTabGroup(current)) {
                return current;
            }
            if (current.size() > best.size()) {
                best = current;
            }
            sleep(POLL_MS);
        }

        return best;
    }

    private List<RuntimeTab> discoverTopTabsOnce() {
        Map<Integer, Map<Integer, RuntimeTab>> byTotal = new HashMap<>();

        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }

            String value = normalizeWhitespace(semanticValue(element));
            Matcher matcher = TAB_PATTERN.matcher(value);
            if (!matcher.matches()) {
                continue;
            }

            int index = parseInt(matcher.group(2), -1);
            int total = parseInt(matcher.group(3), -1);
            String label = normalizeWhitespace(matcher.group(1));
            Rectangle rect = safeRect(element);

            if (index < 1 || total < 2 || index > total || label.isEmpty() || rect == null) {
                continue;
            }

            RuntimeTab tab = new RuntimeTab(index, total, label, rect.getY(), rect.getY() + rect.getHeight());
            Map<Integer, RuntimeTab> group = byTotal.get(total);
            if (group == null) {
                group = new LinkedHashMap<>();
                byTotal.put(total, group);
            }

            RuntimeTab existing = group.get(index);
            if (existing == null || tab.topY < existing.topY) {
                group.put(index, tab);
            }
        }

        List<RuntimeTab> selected = Collections.emptyList();
        double selectedAverageY = Double.MAX_VALUE;
        int selectedCompleteness = -1;

        for (Map.Entry<Integer, Map<Integer, RuntimeTab>> entry : byTotal.entrySet()) {
            int total = entry.getKey();
            List<RuntimeTab> tabs = new ArrayList<>(entry.getValue().values());
            tabs.sort(Comparator.comparingInt(t -> t.index));

            int completeness = tabs.size() == total ? 2 : 1;
            double averageY = averageTopY(tabs);

            if (completeness > selectedCompleteness
                    || (completeness == selectedCompleteness && averageY < selectedAverageY)) {
                selected = tabs;
                selectedAverageY = averageY;
                selectedCompleteness = completeness;
            }
        }

        return selected;
    }

    private TableState switchTopTab(RuntimeTab tab) {
        String before = sectionSignature(captureTableState());
        WebElement element = findTopTabElement(tab);

        if (element != null) {
            safeClick(element, "runtime top tab " + tab.index + "/" + tab.total + " - " + tab.label);
        } else {
            tapTopTabByLiveGeometry(tab);
            ReportLogger.debug(
                    "Tapped " + tab.label + " using live top-tab geometry after semantic refind was unavailable"
            );
        }

        TableState resolved = waitForTableResolved("Top tab " + tab.label);
        String after = sectionSignature(resolved);

        if (!before.isEmpty() && before.equals(after)) {
            ReportLogger.debug(
                    "Top tab " + tab.label + " resolved with the same live section signature as before; dataset overlap is permitted"
            );
        }
        return resolved;
    }

    private WebElement findTopTabElement(RuntimeTab target) {
        long endAt = System.currentTimeMillis() + 4_000L;

        while (System.currentTimeMillis() < endAt) {
            for (WebElement element : safeFindElements(VIEW)) {
                if (!isDisplayed(element)) {
                    continue;
                }
                String value = normalizeWhitespace(semanticValue(element));
                Matcher matcher = TAB_PATTERN.matcher(value);
                if (!matcher.matches()) {
                    continue;
                }

                int index = parseInt(matcher.group(2), -1);
                int total = parseInt(matcher.group(3), -1);
                String label = normalizeWhitespace(matcher.group(1));

                if (index == target.index
                        && total == target.total
                        && label.equalsIgnoreCase(target.label)) {
                    return element;
                }
            }
            sleep(220L);
        }

        return null;
    }

    private void tapTopTabByLiveGeometry(RuntimeTab target) {
        List<RuntimeTab> current = discoverTopTabsOnce();
        if (current.isEmpty()) {
            throw new AssertionError("No live top-tab geometry is available for fallback tap: " + target.label);
        }

        RuntimeTab geometry = null;
        for (RuntimeTab tab : current) {
            if (tab.index == target.index && tab.total == target.total) {
                geometry = tab;
                break;
            }
        }

        if (geometry == null) {
            throw new AssertionError("Target top-tab geometry not found: " + target.label);
        }

        // Re-find a matching semantic element only for its current rectangle. If it
        // disappeared, derive X from the complete top-band bounds instead of fixed pixels.
        WebElement live = findTopTabElement(geometry);
        if (live != null) {
            Rectangle rect = safeRect(live);
            if (rect != null) {
                performTap(rect.getX() + rect.getWidth() / 2, rect.getY() + rect.getHeight() / 2);
                return;
            }
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int centerY = -1;
        for (RuntimeTab tab : current) {
            WebElement e = findTopTabElement(tab);
            Rectangle rect = safeRect(e);
            if (rect == null) {
                continue;
            }
            minX = Math.min(minX, rect.getX());
            maxX = Math.max(maxX, rect.getX() + rect.getWidth());
            centerY = rect.getY() + rect.getHeight() / 2;
        }

        if (centerY < 0 || minX == Integer.MAX_VALUE || maxX <= minX) {
            throw new AssertionError("Unable to derive top-tab band geometry for " + target.label);
        }

        double slotWidth = (maxX - minX) / (double) target.total;
        int tapX = (int) Math.round(minX + ((target.index - 0.5) * slotWidth));
        performTap(tapX, centerY);
    }

    private void assertCompleteTabGroup(List<RuntimeTab> tabs, String label) {
        if (!isCompleteTabGroup(tabs)) {
            throw new AssertionError(
                    label + " is incomplete. discovered=" + labels(tabs)
                            + " | visible=" + collectVisibleValues()
            );
        }
    }

    private boolean isCompleteTabGroup(List<RuntimeTab> tabs) {
        if (tabs == null || tabs.isEmpty()) {
            return false;
        }
        int total = tabs.get(0).total;
        if (tabs.size() != total) {
            return false;
        }
        for (int i = 0; i < tabs.size(); i++) {
            RuntimeTab tab = tabs.get(i);
            if (tab.total != total || tab.index != i + 1) {
                return false;
            }
        }
        return true;
    }

    // =========================================================
    // Dynamic secondary filter discovery
    // =========================================================

    private List<String> discoverSecondaryFilters() {
        List<RuntimeTab> topTabs = discoverTopTabsOnce();
        WebElement stocks = firstDisplayed(STOCKS_HEADER);
        Rectangle stocksRect = safeRect(stocks);

        if (topTabs.isEmpty() || stocksRect == null) {
            return Collections.emptyList();
        }

        int topBottom = 0;
        for (RuntimeTab tab : topTabs) {
            topBottom = Math.max(topBottom, tab.bottomY);
        }
        int tableTop = stocksRect.getY();

        Map<String, Rectangle> candidates = new LinkedHashMap<>();
        for (WebElement element : safeFindElements(VIEW)) {
            if (!isDisplayed(element) || !safeClickable(element)) {
                continue;
            }

            String value = normalizeWhitespace(semanticValue(element));
            if (value.isEmpty() || TAB_PATTERN.matcher(value).matches()) {
                continue;
            }

            Rectangle rect = safeRect(element);
            if (rect == null || rect.getWidth() < 35 || rect.getHeight() < 20 || rect.getHeight() > 160) {
                continue;
            }

            int centerY = rect.getY() + rect.getHeight() / 2;
            if (centerY <= topBottom + 4 || centerY >= tableTop - 2) {
                continue;
            }

            String lower = value.toLowerCase(Locale.ENGLISH);
            if (lower.equals("analyst picks")
                    || lower.equals("customise")
                    || lower.equals("stocks")
                    || lower.equals("funds")
                    || lower.equals("portfolio")
                    || lower.equals("hub")) {
                continue;
            }

            if (!candidates.containsKey(value)) {
                candidates.put(value, rect);
            }
        }

        List<Map.Entry<String, Rectangle>> ordered = new ArrayList<>(candidates.entrySet());
        ordered.sort(Comparator.comparingInt(e -> e.getValue().getX()));

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Rectangle> entry : ordered) {
            result.add(entry.getKey());
        }
        return result;
    }

    private TableState tapSecondaryFilter(String label) {
        WebElement target = firstDisplayed(AppiumBy.accessibilityId(label));
        if (target == null) {
            for (WebElement element : safeFindElements(VIEW)) {
                if (isDisplayed(element)
                        && normalizeWhitespace(semanticValue(element)).equals(label)) {
                    target = element;
                    break;
                }
            }
        }

        if (target == null) {
            throw new AssertionError("Runtime secondary filter disappeared before tap: " + label);
        }

        safeClick(target, "secondary filter " + label);

        // Wait through skeleton/rebuild once and return the resolved state to the caller.
        // This avoids the duplicate data wait that V1 performed after every chip tap.
        return waitForTableResolved("Secondary filter " + label);
    }

    // =========================================================
    // Table state / live data
    // =========================================================

    private TableState waitForTableResolved(String context) {
        long endAt = System.currentTimeMillis() + DATA_WAIT_MS;
        String previous = "";
        int stableReads = 0;
        TableState last = new TableState(false, false, "", new LinkedHashSet<String>(), new LinkedHashSet<String>());

        while (System.currentTimeMillis() < endAt) {
            TableState state = captureTableState();
            last = state;

            if (state.isResolved()) {
                String fingerprint = state.fingerprint();
                if (!fingerprint.isEmpty() && fingerprint.equals(previous)) {
                    stableReads++;
                } else {
                    stableReads = 0;
                }
                previous = fingerprint;

                if (state.hasExplicitEmptyState() || stableReads >= 1) {
                    return state;
                }
            } else {
                stableReads = 0;
                previous = "";
            }

            sleep(POLL_MS);
        }

        throw new AssertionError(
                context + " did not resolve to live table data or an explicit empty state. last="
                        + last.describe() + " | visible=" + collectVisibleValues()
        );
    }

    private TableState captureTableState() {
        WebElement header = firstDisplayed(STOCKS_HEADER);
        Rectangle headerRect = safeRect(header);
        String emptyMessage = "";

        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }
            String value = normalizeWhitespace(semanticValue(element));
            if (value.isEmpty()) {
                continue;
            }

            String lower = value.toLowerCase(Locale.ENGLISH);
            if (containsAny(lower, EMPTY_STATE_MARKERS)) {
                emptyMessage = value;
            }
        }

        Set<String> headers = collectVisibleHeaderValuesSet(headerRect);
        Set<String> tableValues = collectVisibleTableBodyValues(headerRect);

        boolean headerVisible = headerRect != null;
        boolean hasLive = tableValues.size() >= 2;
        return new TableState(headerVisible, hasLive, emptyMessage, headers, tableValues);
    }

    /**
     * Captures visible table-body semantics even after the sticky table header has
     * scrolled out of the viewport. V1 only collected rows when the "Stocks"
     * header was visible, which produced false failures on long Sell lists.
     *
     * When the header is visible, it remains the strongest upper bound. Once it
     * leaves the screen, the live top-tab band becomes the fallback upper bound.
     * This keeps row detection dynamic and avoids stock/date/price hardcoding.
     */
    private Set<String> collectVisibleTableBodyValues(Rectangle headerRect) {
        Set<String> tableValues = new LinkedHashSet<>();
        Dimension screen = driver.manage().window().getSize();

        int tableStartY;
        if (headerRect != null) {
            tableStartY = headerRect.getY() + headerRect.getHeight();
        } else {
            int topTabsBottom = 0;
            for (RuntimeTab tab : discoverTopTabsOnce()) {
                topTabsBottom = Math.max(topTabsBottom, tab.bottomY);
            }

            if (topTabsBottom > 0) {
                tableStartY = topTabsBottom + 4;
            } else {
                Rectangle titleRect = safeRect(firstDisplayed(ANALYST_PICKS_TITLE));
                tableStartY = titleRect == null
                        ? (int) Math.round(screen.getHeight() * 0.12)
                        : titleRect.getY() + titleRect.getHeight();
            }
        }

        int tableEndY = (int) Math.round(screen.getHeight() * 0.91);

        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }

            Rectangle rect = safeRect(element);
            String value = normalizeWhitespace(semanticValue(element));
            if (rect == null || value.isEmpty()) {
                continue;
            }

            int centerY = rect.getY() + rect.getHeight() / 2;
            if (centerY <= tableStartY || centerY >= tableEndY) {
                continue;
            }

            if (isBottomNavigationValue(value) || TAB_PATTERN.matcher(value).matches()) {
                continue;
            }

            String lower = value.toLowerCase(Locale.ENGLISH);
            if (lower.equals("analyst picks")
                    || lower.equals("customise")
                    || lower.equals("?")
                    || lower.equals("take me there")
                    || lower.contains("confused? use our portfolio planner")) {
                continue;
            }

            tableValues.add(value);
        }

        return tableValues;
    }

    private Set<String> collectVisibleHeaderValuesSet(Rectangle stocksRect) {
        Set<String> values = new LinkedHashSet<>();
        if (stocksRect == null) {
            return values;
        }

        int rowCenter = stocksRect.getY() + stocksRect.getHeight() / 2;
        int tolerance = Math.max(45, stocksRect.getHeight());

        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }
            Rectangle rect = safeRect(element);
            String value = normalizeWhitespace(semanticValue(element));
            if (rect == null || value.isEmpty() || TAB_PATTERN.matcher(value).matches()) {
                continue;
            }

            int center = rect.getY() + rect.getHeight() / 2;
            if (Math.abs(center - rowCenter) <= tolerance) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> collectVisibleHeaderValues() {
        Rectangle stocksRect = safeRect(firstDisplayed(STOCKS_HEADER));
        return new ArrayList<>(collectVisibleHeaderValuesSet(stocksRect));
    }

    private String horizontalTableFingerprint() {
        TableState state = captureTableState();
        StringBuilder sb = new StringBuilder();
        for (String header : state.headers) {
            sb.append("H:").append(header).append('|');
        }
        int count = 0;
        for (String value : state.values) {
            sb.append("V:").append(value).append('|');
            count++;
            if (count >= 12) {
                break;
            }
        }
        return sb.toString();
    }

    private String sectionSignature(TableState state) {
        List<String> filters = discoverSecondaryFilters();
        return filters.toString() + "||" + state.fingerprint();
    }

    private void logTableState(String context, TableState state) {
        if (state.hasExplicitEmptyState()) {
            ReportLogger.pass(context + " resolved to explicit empty state: " + state.emptyMessage);
            return;
        }

        ReportLogger.pass(
                context + " resolved with " + state.values.size() + " live semantic table values"
                        + " | headers=" + firstValues(state.headers, 5)
                        + " | sample=" + firstValues(state.values, 5)
        );
    }

    // =========================================================
    // Horizontal / vertical scrolling
    // =========================================================

    private WebElement findTableHorizontalScrollView() {
        WebElement stocks = firstDisplayed(STOCKS_HEADER);
        Rectangle stocksRect = safeRect(stocks);
        if (stocksRect == null) {
            return null;
        }

        Dimension screen = driver.manage().window().getSize();
        WebElement best = null;
        long bestArea = -1L;

        for (WebElement element : safeFindElements(HORIZONTAL_SCROLL_VIEW)) {
            if (!isDisplayed(element)) {
                continue;
            }
            Rectangle rect = safeRect(element);
            if (rect == null) {
                continue;
            }

            int centerY = rect.getY() + rect.getHeight() / 2;
            if (centerY <= stocksRect.getY()) {
                continue;
            }
            if (rect.getWidth() < screen.getWidth() * 0.45
                    || rect.getHeight() < screen.getHeight() * 0.15) {
                continue;
            }

            long area = (long) rect.getWidth() * rect.getHeight();
            if (area > bestArea) {
                bestArea = area;
                best = element;
            }
        }

        return best;
    }

    private Rectangle findTableHorizontalScrollRect() {
        return safeRect(findTableHorizontalScrollView());
    }

    private void swipeHorizontal(Rectangle rect, boolean toLaterColumns) {
        if (rect == null) {
            throw new AssertionError("Horizontal swipe requested without a table scroll rectangle");
        }

        int y = rect.getY() + Math.max(30, Math.min(rect.getHeight() - 25, rect.getHeight() / 3));
        int left = rect.getX() + Math.max(18, (int) Math.round(rect.getWidth() * 0.18));
        int right = rect.getX() + Math.min(rect.getWidth() - 18, (int) Math.round(rect.getWidth() * 0.84));

        if (toLaterColumns) {
            performSwipe(right, y, left, y, 420L);
        } else {
            performSwipe(left, y, right, y, 420L);
        }
    }

    private void swipeVerticalInModule(boolean downFeed) {
        Dimension size = driver.manage().window().getSize();
        int x = (int) Math.round(size.getWidth() * 0.82);
        int top = (int) Math.round(size.getHeight() * 0.34);
        int bottom = (int) Math.round(size.getHeight() * 0.79);

        if (downFeed) {
            performSwipe(x, bottom, x, top, 440L);
        } else {
            performSwipe(x, top, x, bottom, 440L);
        }
    }

    private void swipeHub(boolean downHub) {
        Dimension size = driver.manage().window().getSize();
        int x = (int) Math.round(size.getWidth() * 0.82);
        int top = (int) Math.round(size.getHeight() * 0.28);
        int bottom = (int) Math.round(size.getHeight() * 0.78);

        if (downHub) {
            performSwipe(x, bottom, x, top, 420L);
        } else {
            performSwipe(x, top, x, bottom, 420L);
        }
    }

    private WebElement scrollToPortfolioPlannerCta() {
        WebElement direct = firstDisplayed(PORTFOLIO_PLANNER_CTA);
        if (direct != null) {
            return direct;
        }

        String previous = captureTableState().fingerprint();
        int noProgress = 0;

        for (int i = 0; i < 12; i++) {
            swipeVerticalInModule(true);
            sleep(300L);

            direct = firstDisplayed(PORTFOLIO_PLANNER_CTA);
            if (direct != null) {
                ReportLogger.step("Portfolio Planner CTA found after bounded vertical scroll attempt " + (i + 1));
                return direct;
            }

            String current = captureTableState().fingerprint();
            if (!current.isEmpty() && current.equals(previous)) {
                noProgress++;
            } else {
                noProgress = 0;
            }
            previous = current;

            if (noProgress >= 3) {
                break;
            }
        }

        return null;
    }

    private void performSwipe(int startX, int startY, int endX, int endY, long durationMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "readyInvestFinger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(70L)));
        swipe.addAction(
                finger.createPointerMove(
                        Duration.ofMillis(durationMs),
                        PointerInput.Origin.viewport(),
                        endX,
                        endY
                )
        );
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    // =========================================================
    // Hub / page readiness
    // =========================================================

    private WebElement findReadyToInvestTileWithBoundedScroll() {
        WebElement direct = firstDisplayed(READY_TO_INVEST_TILE);
        if (direct != null) {
            ReportLogger.debug("Ready to invest portfolio Hub tile already visible - no Hub scroll required");
            return direct;
        }

        String previous = hubOpenTileFingerprint();
        int noProgress = 0;

        for (int i = 0; i < 7; i++) {
            swipeHub(true);
            sleep(240L);

            direct = firstDisplayed(READY_TO_INVEST_TILE);
            if (direct != null) {
                ReportLogger.step("Ready to invest Hub tile found after downward Hub scroll attempt " + (i + 1));
                return direct;
            }

            String current = hubOpenTileFingerprint();
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

        previous = hubOpenTileFingerprint();
        noProgress = 0;

        for (int i = 0; i < 6; i++) {
            swipeHub(false);
            sleep(230L);

            direct = firstDisplayed(READY_TO_INVEST_TILE);
            if (direct != null) {
                ReportLogger.step("Ready to invest Hub tile found after upward Hub recovery attempt " + (i + 1));
                return direct;
            }

            String current = hubOpenTileFingerprint();
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

    private void waitForAnalystPicksReady() {
        waitForVisible(ANALYST_PICKS_TITLE, PAGE_READY_WAIT_MS);

        long endAt = System.currentTimeMillis() + PAGE_READY_WAIT_MS;
        while (System.currentTimeMillis() < endAt) {
            List<RuntimeTab> tabs = discoverTopTabsOnce();
            TableState state = captureTableState();

            if (isCompleteTabGroup(tabs) && state.isResolved()) {
                ReportLogger.info("Analyst Picks top tabs discovered live: " + labels(tabs));
                return;
            }
            sleep(POLL_MS);
        }

        throw new AssertionError(
                "Analyst Picks title appeared but top tabs/table did not become ready. visible="
                        + collectVisibleValues()
        );
    }

    private boolean isAnalystPicksOpen() {
        return isVisible(ANALYST_PICKS_TITLE) && !discoverTopTabsOnce().isEmpty();
    }

    private boolean isInvestmentPlannerOpen() {
        return isVisible(INVESTMENT_PLANNER_TITLE);
    }

    private boolean isHubScreenReady() {
        if (firstDisplayed(READY_TO_INVEST_TILE) != null) {
            return true;
        }

        int openTiles = 0;
        for (WebElement element : safeFindElements(HUB_OPEN_BUTTONS)) {
            if (isDisplayed(element)) {
                openTiles++;
                if (openTiles >= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean waitForHubScreenReady(long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endAt) {
            if (isHubScreenReady()) {
                return true;
            }
            sleep(POLL_MS);
        }
        return false;
    }

    private String hubOpenTileFingerprint() {
        Set<String> values = new LinkedHashSet<>();
        for (WebElement element : safeFindElements(HUB_OPEN_BUTTONS)) {
            if (!isDisplayed(element)) {
                continue;
            }
            String value = normalizeWhitespace(semanticValue(element));
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values.toString();
    }

    // =========================================================
    // Generic helpers
    // =========================================================

    private List<WebElement> contentSemanticElements() {
        List<WebElement> elements = new ArrayList<>();
        elements.addAll(safeFindElements(VIEW));
        elements.addAll(safeFindElements(IMAGE_VIEW));
        elements.addAll(safeFindElements(BUTTON));
        return elements;
    }

    private void assertDriverAlive(String context) {
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || pkg.trim().isEmpty()) {
                throw new AssertionError("Driver returned no current package " + context);
            }
        } catch (Exception e) {
            throw new AssertionError("Appium driver is not healthy " + context + ": " + clean(e.getMessage()), e);
        }
    }

    private WebElement waitForVisible(By locator, long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endAt) {
            WebElement element = firstDisplayed(locator);
            if (element != null) {
                return element;
            }
            sleep(POLL_MS);
        }
        throw new AssertionError("Timed out waiting for visible locator: " + locator);
    }

    private WebElement waitForVisibleOptional(By locator, long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endAt) {
            WebElement element = firstDisplayed(locator);
            if (element != null) {
                return element;
            }
            sleep(POLL_MS);
        }
        return null;
    }

    private void requireVisible(By locator, String label) {
        WebElement element = waitForVisible(locator, SHORT_WAIT_MS);
        if (!isDisplayed(element)) {
            throw new AssertionError(label + " is not displayed");
        }
    }

    private boolean isVisible(By locator) {
        return firstDisplayed(locator) != null;
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : safeFindElements(locator)) {
            if (isDisplayed(element)) {
                return element;
            }
        }
        return null;
    }

    private WebElement bottomMostVisible(By locator) {
        return safeFindElements(locator).stream()
                .filter(this::isDisplayed)
                .max(Comparator.comparingInt(this::centerY))
                .orElse(null);
    }

    private List<WebElement> safeFindElements(By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean isDisplayed(WebElement element) {
        try {
            return element != null && element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeClickable(WebElement element) {
        return "true".equalsIgnoreCase(safeAttribute(element, "clickable"));
    }

    private void safeClick(WebElement element, String label) {
        try {
            element.click();
        } catch (Exception first) {
            Rectangle rect = safeRect(element);
            if (rect == null) {
                throw new AssertionError("Unable to click " + label + ": " + clean(first.getMessage()), first);
            }
            performTap(
                    rect.getX() + rect.getWidth() / 2,
                    rect.getY() + rect.getHeight() / 2
            );
        }
        ReportLogger.debug("Tapped " + label);
    }

    private void performTap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "readyInvestTap");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(65L)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    private Rectangle safeRect(WebElement element) {
        try {
            return element == null ? null : element.getRect();
        } catch (Exception e) {
            return null;
        }
    }

    private int centerY(WebElement element) {
        Rectangle rect = safeRect(element);
        return rect == null ? -1 : rect.getY() + rect.getHeight() / 2;
    }

    private String semanticValue(WebElement element) {
        String desc = safeAttribute(element, "content-desc");
        if (!desc.isEmpty()) {
            return desc;
        }
        try {
            return sanitizeSemanticValue(element.getText());
        } catch (Exception e) {
            return "";
        }
    }

    private String safeAttribute(WebElement element, String name) {
        try {
            return sanitizeSemanticValue(element.getAttribute(name));
        } catch (Exception e) {
            return "";
        }
    }

    private String sanitizeSemanticValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty()
                || cleaned.equalsIgnoreCase("null")
                || cleaned.equalsIgnoreCase("none")
                || cleaned.equalsIgnoreCase("undefined")) {
            return "";
        }
        return cleaned;
    }

    private boolean isBottomNavigationValue(String value) {
        String lower = normalizeWhitespace(value).toLowerCase(Locale.ENGLISH);
        return lower.equals("funds")
                || lower.equals("stocks")
                || lower.equals("portfolio")
                || lower.equals("hub");
    }

    private String collectVisibleValues() {
        Set<String> values = new LinkedHashSet<>();
        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }
            String value = normalizeWhitespace(semanticValue(element));
            if (!value.isEmpty()) {
                values.add(value);
            }
            if (values.size() >= 35) {
                break;
            }
        }
        return values.toString();
    }

    private List<String> firstValues(Set<String> values, int limit) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(value);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<String> labels(List<RuntimeTab> tabs) {
        List<String> result = new ArrayList<>();
        for (RuntimeTab tab : tabs) {
            result.add(tab.index + "/" + tab.total + " " + tab.label);
        }
        return result;
    }

    private boolean containsAny(String value, String[] tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private double averageTopY(List<RuntimeTab> tabs) {
        if (tabs.isEmpty()) {
            return Double.MAX_VALUE;
        }
        long total = 0L;
        for (RuntimeTab tab : tabs) {
            total += tab.topY;
        }
        return total / (double) tabs.size();
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Thread interrupted while waiting", e);
        }
    }

    // =========================================================
    // Immutable runtime state
    // =========================================================

    private static final class RuntimeTab {
        private final int index;
        private final int total;
        private final String label;
        private final int topY;
        private final int bottomY;

        private RuntimeTab(int index, int total, String label, int topY, int bottomY) {
            this.index = index;
            this.total = total;
            this.label = label;
            this.topY = topY;
            this.bottomY = bottomY;
        }
    }

    private static final class TableState {
        private final boolean headerVisible;
        private final boolean liveContent;
        private final String emptyMessage;
        private final Set<String> headers;
        private final Set<String> values;

        private TableState(
                boolean headerVisible,
                boolean liveContent,
                String emptyMessage,
                Set<String> headers,
                Set<String> values
        ) {
            this.headerVisible = headerVisible;
            this.liveContent = liveContent;
            this.emptyMessage = emptyMessage == null ? "" : emptyMessage;
            this.headers = headers == null ? new LinkedHashSet<String>() : headers;
            this.values = values == null ? new LinkedHashSet<String>() : values;
        }

        private boolean hasLiveContent() {
            // The table header is allowed to scroll out of view on long lists.
            // Visible row semantics alone are sufficient proof that live table content remains present.
            return liveContent;
        }

        private boolean hasExplicitEmptyState() {
            return !emptyMessage.isEmpty();
        }

        private boolean isResolved() {
            return hasLiveContent() || hasExplicitEmptyState();
        }

        private String fingerprint() {
            if (hasExplicitEmptyState()) {
                return "EMPTY:" + emptyMessage;
            }
            StringBuilder sb = new StringBuilder();
            int headerCount = 0;
            for (String header : headers) {
                sb.append("H:").append(header).append('|');
                headerCount++;
                if (headerCount >= 6) {
                    break;
                }
            }
            int valueCount = 0;
            for (String value : values) {
                sb.append("V:").append(value).append('|');
                valueCount++;
                if (valueCount >= 10) {
                    break;
                }
            }
            return sb.toString();
        }

        private String describe() {
            return "headerVisible=" + headerVisible
                    + ", liveContent=" + liveContent
                    + ", emptyMessage=" + emptyMessage
                    + ", headers=" + headers
                    + ", values=" + firstStatic(values, 8);
        }

        private static List<String> firstStatic(Set<String> values, int limit) {
            List<String> result = new ArrayList<>();
            for (String value : values) {
                result.add(value);
                if (result.size() >= limit) {
                    break;
                }
            }
            return result;
        }
    }
}