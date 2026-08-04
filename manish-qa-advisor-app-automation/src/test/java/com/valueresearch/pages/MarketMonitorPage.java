package com.valueresearch.pages;

import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast and stable page object for Stocks -> Market Monitor.
 *
 * Performance design:
 * - Uses exact accessibility ids for taps.
 * - Reads one Appium page source and validates all visible controls locally.
 * - Avoids repeated //* scans and repeated getAttribute calls.
 * - Accepts a structurally valid table immediately after an interaction instead
 *   of waiting for data to change when the selected option was already active.
 * - Uses short bounded retries only for real Flutter rebuild frames.
 *
 * Stability design:
 * - No hardcoded market values, index names, years, quarters, or months.
 * - Uses the parent android.widget.ScrollView for vertical scrolling.
 * - Uses the nested table area only for horizontal scrolling.
 */
public class MarketMonitorPage {

    public static final String BUILD_VERSION = "MM_FAST_STABLE_V6_20260728";

    private static final String STOCKS_TAB = "Stocks";
    private static final String MARKET_MONITOR = "Market Monitor";
    private static final String INDEX_HEADER = "Index";
    private static final String QUICK_SEARCH = "Quick Search";
    private static final String SEARCH_PLACEHOLDER = "Search for a stock";

    private static final String FILTER_ALL = "All";
    private static final String FILTER_INDIAN = "Indian";
    private static final String FILTER_GLOBAL = "Global";

    private static final String PERIOD_TRAILING = "Trailing";
    private static final String PERIOD_ANNUAL = "Annual";
    private static final String PERIOD_QUARTERLY = "Quarterly";
    private static final String PERIOD_MONTHLY = "Monthly";

    private static final String[] MARKET_FILTERS = {
            FILTER_ALL, FILTER_INDIAN, FILTER_GLOBAL
    };

    private static final String[] PERIOD_TABS = {
            PERIOD_TRAILING, PERIOD_ANNUAL, PERIOD_QUARTERLY, PERIOD_MONTHLY
    };

    private static final Pattern BOUNDS_PATTERN = Pattern.compile(
            "\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]"
    );

    private static final Pattern PURE_NUMBER = Pattern.compile(
            "^[+-]?(?:\\d{1,3}(?:,\\d{3})*|\\d+)(?:\\.\\d+)?%?$"
    );

    private static final Pattern NUMBER_TOKEN = Pattern.compile(
            "(?<![A-Za-z0-9])[-+]?(?:\\d{1,3}(?:,\\d{3})*|\\d+)(?:\\.\\d+)?%?(?![A-Za-z0-9-])"
    );

    private static final Pattern MONTH_YEAR_HEADER = Pattern.compile(
            "(?i)^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[-' ]?\\d{2,4}$"
    );

    private static final Pattern YEAR_HEADER = Pattern.compile("^(?:19|20)\\d{2}$");

    private static final long SOURCE_CACHE_TTL_MS = 850L;
    private static final int TABLE_READY_ATTEMPTS = 5;
    private static final int INTERACTION_SETTLE_MS = 280;

    private final AndroidDriver driver;

    private UiSnapshot uiSnapshotCache;
    private long uiSnapshotCacheAtMs;

    private boolean horizontalBaselineInitialised;
    private boolean tableHorizontallyShifted;

    public MarketMonitorPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // MM_001 - OPEN MARKET MONITOR
    // =========================================================

    public void openMarketMonitorFromStocks() {
        try {
            ReportLogger.step("Opening Stocks module and Market Monitor | build=" + BUILD_VERSION);

            UiSnapshot current = getUiSnapshot(false);
            if (isMarketMonitorVisible(current)) {
                ReportLogger.pass("Market Monitor controls and table are already visible");
                return;
            }

            // After the vertical-scroll test the Market Monitor table can remain
            // visible while the filter row and heading are above the viewport.
            // Restore that existing page state instead of tapping the already
            // selected Stocks tab, which does not reset Flutter scroll position.
            if (isMarketMonitorTableContextVisible(current)
                    && restoreMarketMonitorControlArea()) {
                ReportLogger.pass("Existing Market Monitor table state restored to the control area");
                return;
            }

            recoverToAdvisorMainNavigationIfNeeded();
            tapStocksBottomTab();
            waitForStocksLandingPage();
            bringMarketMonitorIntoView();
            waitForMarketMonitor();

            horizontalBaselineInitialised = false;
            tableHorizontallyShifted = false;
            ReportLogger.pass("Stocks Market Monitor opened successfully");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Failed to open Stocks Market Monitor: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void tapStocksBottomTab() {
        ReportLogger.step("Tapping Stocks bottom navigation tab");

        WebElement stocks = findExactAccessibilityElement(STOCKS_TAB);
        if (stocks != null && isInBottomNavigationBand(safeRect(stocks))) {
            tapElementCenter(stocks);
            sleep(320);
            ReportLogger.pass("Stocks bottom tab tapped using accessibility locator");
            return;
        }

        NodeInfo sourceNode = findExactNode(getUiSnapshot(false), STOCKS_TAB);
        if (sourceNode != null && isInBottomNavigationBand(sourceNode.rect)) {
            tapRectCenter(sourceNode.rect);
            sleep(320);
            ReportLogger.pass("Stocks bottom tab tapped using page-source bounds");
            return;
        }

        Dimension size = driver.manage().window().getSize();
        int x = (int) (size.getWidth() * 0.375);
        int y = (int) (size.getHeight() * 0.955);
        tapByCoordinates(x, y);
        sleep(420);

        if (!isStocksLandingPageVisible()) {
            throw new AssertionError("Stocks bottom tab could not be opened"
                    + " | visibleValues=" + collectVisibleStrings());
        }

        ReportLogger.pass("Stocks bottom tab tapped using screen-ratio fallback");
    }

    private void waitForStocksLandingPage() {
        for (int attempt = 1; attempt <= 7; attempt++) {
            if (isStocksLandingPageVisible()) {
                ReportLogger.pass("Stocks landing page is ready");
                return;
            }
            sleep(300);
            invalidateUiSnapshot();
        }

        throw new AssertionError("Stocks landing page did not load"
                + " | visibleValues=" + collectVisibleStrings());
    }

    private boolean isStocksLandingPageVisible() {
        UiSnapshot snapshot = getUiSnapshot(false);
        return isMarketMonitorVisible(snapshot)
                || isMarketMonitorTableContextVisible(snapshot)
                || snapshot.contains(QUICK_SEARCH)
                || snapshot.contains(SEARCH_PLACEHOLDER)
                || snapshot.contains("Stock Advisor")
                || snapshot.contains(MARKET_MONITOR);
    }

    private void bringMarketMonitorIntoView() {
        ReportLogger.step("Locating complete Market Monitor interaction area | build=" + BUILD_VERSION);

        UiSnapshot current = getUiSnapshot(false);
        if (isMarketMonitorVisible(current)) {
            ReportLogger.pass("Market Monitor interaction area is already visible");
            return;
        }

        // The table may already be open but vertically scrolled below the filter
        // row. In that state the correct recovery direction is upward, not the
        // normal navigation scroll towards the section.
        if (isMarketMonitorTableContextVisible(current)
                && restoreMarketMonitorControlArea()) {
            ReportLogger.pass("Market Monitor interaction area restored from table-only viewport");
            return;
        }

        // Fast primary strategy: bounded central swipes. This avoids the long
        // internal UiScrollable timeout observed on the Flutter page.
        for (int attempt = 1; attempt <= 6; attempt++) {
            ReportLogger.step("Scrolling Stocks page towards Market Monitor | attempt=" + attempt);
            scrollStocksPageDownOneStep();
            sleep(260);

            if (isMarketMonitorVisible()) {
                ReportLogger.pass("Market Monitor interaction area found after scroll attempt " + attempt);
                return;
            }
        }

        // Last-resort semantic scroll. Run only once after the fast gestures.
        if (tryScrollIntoView(FILTER_ALL) || tryScrollIntoView(PERIOD_TRAILING)) {
            sleep(300);
            if (isMarketMonitorVisible()) {
                ReportLogger.pass("Market Monitor interaction area found using UiScrollable fallback");
                return;
            }
        }

        throw new AssertionError("Unable to bring complete Market Monitor interaction area into view"
                + " | build=" + BUILD_VERSION
                + " | visibleValues=" + collectVisibleStrings());
    }

    private boolean tryScrollIntoView(String targetText) {
        String escaped = escapeUiAutomatorText(targetText);
        String selector = "new UiScrollable(new UiSelector().scrollable(true))"
                + ".setAsVerticalList().scrollIntoView(new UiSelector().description(\""
                + escaped + "\"))";

        try {
            driver.findElement(AppiumBy.androidUIAutomator(selector));
            invalidateUiSnapshot();
            ReportLogger.debug("UiScrollable resolved Market Monitor target: " + targetText);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void scrollStocksPageDownOneStep() {
        Dimension size = driver.manage().window().getSize();

        try {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("left", Math.max(8, (int) (size.getWidth() * 0.08)));
            args.put("top", Math.max(100, (int) (size.getHeight() * 0.16)));
            args.put("width", Math.max(100, (int) (size.getWidth() * 0.84)));
            args.put("height", Math.max(260, (int) (size.getHeight() * 0.66)));
            args.put("direction", "down");
            args.put("percent", 0.72);

            driver.executeScript("mobile: scrollGesture", args);
            invalidateUiSnapshot();
            return;
        } catch (Exception gestureError) {
            ReportLogger.debug("Central scrollGesture failed; using W3C fallback: "
                    + cleanError(gestureError.getMessage()));
        }

        performSwipe(0.52, 0.80, 0.52, 0.32, 420);
    }

    private void waitForMarketMonitor() {
        AssertionError lastFailure = null;

        for (int attempt = 1; attempt <= 6; attempt++) {
            if (isMarketMonitorVisible()) {
                try {
                    waitForTableReady("Market Monitor initial table");
                    ReportLogger.pass("Market Monitor controls and table are ready | build=" + BUILD_VERSION);
                    return;
                } catch (AssertionError e) {
                    lastFailure = e;
                }
            }

            sleep(260);
            invalidateUiSnapshot();
        }

        throw new AssertionError("Market Monitor did not become ready"
                + " | build=" + BUILD_VERSION
                + (lastFailure == null ? "" : " | lastTableFailure=" + lastFailure.getMessage())
                + " | visibleValues=" + collectVisibleStrings());
    }

    // =========================================================
    // MM_002 - OVERVIEW
    // =========================================================

    public void verifyMarketMonitorOverview() {
        try {
            ReportLogger.step("Validating Market Monitor overview and controls");
            ensureMarketMonitorVisible();
            resetTableToTopAndLeft();

            UiSnapshot snapshot = getUiSnapshot(true);
            assertTextVisible(snapshot, MARKET_MONITOR, "Market Monitor heading");
            assertTextVisible(snapshot, QUICK_SEARCH, "Quick Search heading");

            if (snapshot.contains(SEARCH_PLACEHOLDER)) {
                logValidatedText("Quick Search placeholder", SEARCH_PLACEHOLDER);
            } else {
                ReportLogger.debug("Search placeholder is not separately exposed by Flutter semantics");
            }

            for (String filter : MARKET_FILTERS) {
                assertControlVisible(snapshot, filter, "Market filter");
            }
            for (String period : PERIOD_TABS) {
                assertControlVisible(snapshot, period, "Period tab");
            }

            TableSnapshot table = captureTableSnapshot(snapshot);
            validateTableSnapshot(table, "Market Monitor overview table");
            logTableSnapshot("Market Monitor overview", table);

            ReportLogger.pass("Market Monitor overview and controls validated successfully");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Market Monitor overview validation failed: " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =========================================================
    // MM_003..MM_005 - FILTERS
    // =========================================================

    public void verifyAllMarketFilter() {
        verifyMarketFilter(FILTER_ALL);
    }

    public void verifyIndianMarketFilter() {
        verifyMarketFilter(FILTER_INDIAN);
    }

    public void verifyGlobalMarketFilter() {
        verifyMarketFilter(FILTER_GLOBAL);
    }

    private void verifyMarketFilter(String filterName) {
        try {
            ReportLogger.step("Validating Market Monitor filter: " + filterName);
            ensureMarketMonitorVisible();
            resetTableToTopAndLeft();

            TableSnapshot before = captureTableSnapshot(getUiSnapshot(true));
            tapMarketMonitorControl(filterName, ControlType.MARKET_FILTER);
            TableSnapshot after = waitForTableAfterInteraction(
                    before == null ? "" : before.signature,
                    "Market filter " + filterName
            );

            logSelectionAttributes(filterName);
            logTableSnapshot(filterName + " filter", after);

            if (before != null && !before.signature.equals(after.signature)) {
                ReportLogger.pass(filterName + " filter refreshed the visible table data");
            } else {
                ReportLogger.debug(filterName
                        + " filter produced the same visible signature. "
                        + "This is valid when that filter was already active or visible rows overlap.");
            }

            ReportLogger.pass("Market filter validated successfully: " + filterName);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Market filter validation failed for " + filterName + ": "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =========================================================
    // MM_006..MM_009 - PERIODS
    // =========================================================

    public void verifyTrailingPeriod() {
        verifyPeriodTab(PERIOD_TRAILING);
    }

    public void verifyAnnualPeriod() {
        verifyPeriodTab(PERIOD_ANNUAL);
    }

    public void verifyQuarterlyPeriod() {
        verifyPeriodTab(PERIOD_QUARTERLY);
    }

    public void verifyMonthlyPeriod() {
        verifyPeriodTab(PERIOD_MONTHLY);
    }

    private void verifyPeriodTab(String periodName) {
        try {
            ReportLogger.step("Validating Market Monitor period tab: " + periodName);
            ensureMarketMonitorVisible();
            resetTableToTopAndLeft();

            TableSnapshot before = captureTableSnapshot(getUiSnapshot(true));
            tapMarketMonitorControl(periodName, ControlType.PERIOD_TAB);
            TableSnapshot after = waitForTableAfterInteraction(
                    before == null ? "" : before.signature,
                    "Period tab " + periodName
            );

            validatePeriodHeaderShape(periodName, after);
            logSelectionAttributes(periodName);
            logTableSnapshot(periodName + " period", after);

            if (before != null && !before.signature.equals(after.signature)) {
                ReportLogger.pass(periodName + " period refreshed table headers/data");
            } else {
                ReportLogger.debug(periodName
                        + " period produced the same visible signature. "
                        + "This is valid when that period was already selected.");
            }

            ReportLogger.pass("Period tab validated successfully: " + periodName);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Period tab validation failed for " + periodName + ": "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void validatePeriodHeaderShape(String periodName, TableSnapshot snapshot) {
        if (PERIOD_TRAILING.equals(periodName)) {
            boolean trailingShape = false;
            for (String header : snapshot.columnHeaders) {
                String lower = header.toLowerCase(Locale.ENGLISH);
                if (lower.contains("day")
                        || lower.contains("week")
                        || lower.contains("month")
                        || lower.contains("year")
                        || lower.equals("ytd")) {
                    trailingShape = true;
                    break;
                }
            }

            if (!trailingShape) {
                throw new AssertionError("Trailing period headers do not contain a trailing interval"
                        + " | headers=" + snapshot.columnHeaders);
            }
            return;
        }

        boolean dated = false;
        for (String header : snapshot.columnHeaders) {
            for (String part : splitSemanticValue(header)) {
                if (MONTH_YEAR_HEADER.matcher(part).matches()
                        || YEAR_HEADER.matcher(part).matches()) {
                    dated = true;
                    break;
                }
            }
            if (dated) {
                break;
            }
        }

        if (dated) {
            logValidatedText(periodName + " dated headers", snapshot.columnHeaders.toString());
        } else {
            ReportLogger.debug(periodName
                    + " headers are dynamic and do not match known date formats: "
                    + snapshot.columnHeaders);
        }
    }

    // =========================================================
    // MM_010 - VERTICAL SCROLL
    // =========================================================

    public void verifyMarketMonitorVerticalScroll() {
        try {
            ReportLogger.step("Validating vertical scrolling in Market Monitor table");
            ensureMarketMonitorVisible();
            resetTableToTopAndLeft();

            tapMarketMonitorControl(FILTER_ALL, ControlType.MARKET_FILTER);
            tapMarketMonitorControl(PERIOD_TRAILING, ControlType.PERIOD_TAB);
            waitForTableReady("Vertical scroll initial table");

            VerticalViewport before = captureVerticalViewport(getUiSnapshot(true));
            if (before.rowLabels.size() < 2) {
                throw new AssertionError("Insufficient visible rows before vertical scroll"
                        + " | viewport=" + before
                        + " | values=" + collectVisibleStrings());
            }

            VerticalViewport after = tryVerticalDirection(before, ScrollDirection.DOWN);
            if (!hasVerticalViewportMoved(before, after)) {
                ReportLogger.debug("Downward content scroll did not move; trying reverse direction because the page may already be at an edge");
                after = tryVerticalDirection(before, ScrollDirection.UP);
            }

            if (!hasVerticalViewportMoved(before, after)) {
                throw new AssertionError("Market Monitor parent ScrollView did not move in either direction"
                        + " | before=" + before
                        + " | after=" + after
                        + " | note=gesture was sent to android.widget.ScrollView and then to the fixed Index column fallback");
            }

            logValidatedText("Rows before vertical scroll", before.rowLabels.toString());
            logValidatedText("Rows after vertical scroll", after.rowLabels.toString());
            logValidatedText("Vertical row geometry before", before.geometrySignature);
            logValidatedText("Vertical row geometry after", after.geometrySignature);

            // Leave a deterministic page state for the next independent test.
            // Failure to restore here is non-fatal because ensureMarketMonitorVisible()
            // contains the same recovery path, but normally this completes in one
            // upward parent-scroll gesture.
            if (!restoreMarketMonitorControlArea()) {
                ReportLogger.debug("Market Monitor control area was not restored after vertical validation; next test will retry recovery");
            }

            ReportLogger.pass("Market Monitor vertical table scrolling validated successfully");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Market Monitor vertical scroll validation failed: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private VerticalViewport tryVerticalDirection(
            VerticalViewport baseline,
            ScrollDirection direction
    ) {
        VerticalViewport latest = baseline;

        for (int attempt = 1; attempt <= 3; attempt++) {
            ReportLogger.step("Scrolling Market Monitor rows vertically"
                    + " | direction=" + direction.label
                    + " | attempt=" + attempt);

            scrollParentMarketPage(direction);
            sleep(320);
            latest = captureVerticalViewport(getUiSnapshot(true));

            ReportLogger.debug("Vertical viewport comparison"
                    + " | direction=" + direction.label
                    + " | attempt=" + attempt
                    + " | before=" + baseline.geometrySignature
                    + " | after=" + latest.geometrySignature);

            if (hasVerticalViewportMoved(baseline, latest)) {
                return latest;
            }
        }

        return latest;
    }

    private VerticalViewport captureVerticalViewport(UiSnapshot snapshot) {
        Dimension screen = driver.manage().window().getSize();
        NodeInfo indexHeader = findIndexHeaderNode(snapshot.nodes);
        int topLimit = indexHeader == null
                ? (int) (screen.getHeight() * 0.35)
                : indexHeader.rect.getY() + indexHeader.rect.getHeight();
        int bottomLimit = findTableBottomY(snapshot.nodes, screen);

        Map<String, Integer> rowPositions = new LinkedHashMap<>();

        for (NodeInfo node : snapshot.nodes) {
            int centerX = centerX(node.rect);
            int centerY = centerY(node.rect);
            String text = normalizeSpaces(node.text);

            if (centerY <= topLimit || centerY >= bottomLimit) {
                continue;
            }
            if (centerX > (int) (screen.getWidth() * 0.47)) {
                continue;
            }
            if (!isPotentialIndexRowLabel(text)) {
                continue;
            }

            String label = cleanRowLabel(text);
            if (!label.isEmpty()) {
                rowPositions.putIfAbsent(label, centerY);
            }
        }

        List<String> rowLabels = limitList(new ArrayList<>(rowPositions.keySet()), 16);
        List<String> geometry = new ArrayList<>();

        for (String label : rowLabels) {
            Integer y = rowPositions.get(label);
            if (y != null) {
                geometry.add(label + "@" + ((y / 8) * 8));
            }
        }

        return new VerticalViewport(
                rowLabels,
                String.join("|", rowLabels),
                String.join("|", geometry)
        );
    }

    private boolean hasVerticalViewportMoved(VerticalViewport before, VerticalViewport after) {
        if (before == null || after == null || after.rowLabels.isEmpty()) {
            return false;
        }
        return !before.rowSignature.equals(after.rowSignature)
                || !before.geometrySignature.equals(after.geometrySignature);
    }

    private void scrollParentMarketPage(ScrollDirection direction) {
        WebElement scrollView = findLargestVisibleScrollView();
        boolean executed = false;

        if (scrollView instanceof RemoteWebElement) {
            try {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("elementId", ((RemoteWebElement) scrollView).getId());
                args.put("direction", direction.appiumDirection);
                args.put("percent", 0.74);

                Object result = driver.executeScript("mobile: scrollGesture", args);
                executed = !(result instanceof Boolean) || ((Boolean) result);
                invalidateUiSnapshot();

                ReportLogger.debug("Parent ScrollView scrollGesture executed"
                        + " | direction=" + direction.label
                        + " | result=" + result);
            } catch (Exception e) {
                ReportLogger.debug("Parent ScrollView scrollGesture failed: "
                        + cleanError(e.getMessage()));
            }
        }

        if (executed) {
            return;
        }

        // Fixed left Index column fallback. This stays outside the nested
        // HorizontalScrollView used by the return-value columns.
        if (direction == ScrollDirection.DOWN) {
            performSwipe(0.18, 0.86, 0.18, 0.31, 520);
        } else {
            performSwipe(0.18, 0.31, 0.18, 0.86, 520);
        }
    }

    private WebElement findLargestVisibleScrollView() {
        WebElement largest = null;
        long largestArea = -1L;

        try {
            List<WebElement> scrollViews = driver.findElements(
                    AppiumBy.className("android.widget.ScrollView")
            );

            for (WebElement element : scrollViews) {
                Rectangle rect = safeRect(element);
                if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                    continue;
                }

                long area = (long) rect.getWidth() * rect.getHeight();
                if (area > largestArea) {
                    largest = element;
                    largestArea = area;
                }
            }
        } catch (Exception ignored) {
            // W3C fallback will be used.
        }

        return largest;
    }

    // =========================================================
    // MM_011 - HORIZONTAL SCROLL
    // =========================================================

    public void verifyMarketMonitorHorizontalScroll() {
        try {
            ReportLogger.step("Validating horizontal scrolling in Market Monitor table");
            ensureMarketMonitorVisible();
            resetTableToTopAndLeft();

            tapMarketMonitorControl(PERIOD_TRAILING, ControlType.PERIOD_TAB);
            TableSnapshot before = waitForTableReady("Horizontal scroll initial table");

            boolean changed = false;
            TableSnapshot after = before;

            for (int attempt = 1; attempt <= 3; attempt++) {
                swipeTableHorizontallyLeft(before.headerCenterY);
                sleep(320);
                after = waitForTableReady("Horizontal scroll table attempt " + attempt);

                if (!before.headerSignature.equals(after.headerSignature)
                        || !before.valueSignature.equals(after.valueSignature)) {
                    changed = true;
                    break;
                }
            }

            if (!changed) {
                throw new AssertionError("Market Monitor horizontal table content did not change after repeated swipes"
                        + " | beforeHeaders=" + before.columnHeaders
                        + " | afterHeaders=" + after.columnHeaders);
            }

            logValidatedText("Headers before horizontal scroll", before.columnHeaders.toString());
            logValidatedText("Headers after horizontal scroll", after.columnHeaders.toString());

            swipeTableHorizontallyRight(after.headerCenterY);
            sleep(250);
            ReportLogger.pass("Market Monitor horizontal table scrolling validated successfully");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Market Monitor horizontal scroll validation failed: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =========================================================
    // MM_012 - FILTER/PERIOD MATRIX
    // =========================================================

    public void verifyAllMarketAndPeriodCombinations() {
        try {
            ReportLogger.step("Validating all Market Monitor filter and period combinations");
            ensureMarketMonitorVisible();
            resetTableToTopAndLeft();

            for (String filter : MARKET_FILTERS) {
                tapMarketMonitorControl(filter, ControlType.MARKET_FILTER);

                for (String period : PERIOD_TABS) {
                    tapMarketMonitorControl(period, ControlType.PERIOD_TAB);
                    TableSnapshot snapshot = waitForTableReady(
                            "Matrix combination " + filter + " / " + period
                    );
                    validatePeriodHeaderShape(period, snapshot);
                    logValidatedText(
                            "Validated Market Monitor combination",
                            filter + " / " + period
                                    + " | rows=" + snapshot.rowLabels.size()
                                    + " | numericCells=" + snapshot.numericValues.size()
                    );
                }
            }

            tapMarketMonitorControl(FILTER_ALL, ControlType.MARKET_FILTER);
            tapMarketMonitorControl(PERIOD_TRAILING, ControlType.PERIOD_TAB);
            resetTableToTopAndLeft();

            ReportLogger.pass("All Market Monitor filter/period combinations validated successfully");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(
                    "Market Monitor combination validation failed: "
                            + cleanError(e.getMessage()),
                    e
            );
        }
    }

    // =========================================================
    // CONTROL / TABLE HELPERS
    // =========================================================

    private void ensureMarketMonitorVisible() {
        UiSnapshot current = getUiSnapshot(false);
        if (isMarketMonitorVisible(current)) {
            return;
        }

        if (isMarketMonitorTableContextVisible(current)
                && restoreMarketMonitorControlArea()) {
            return;
        }

        openMarketMonitorFromStocks();

        if (!isMarketMonitorVisible()) {
            throw new AssertionError("Market Monitor is not visible after recovery"
                    + " | visibleValues=" + collectVisibleStrings());
        }
    }

    private boolean isMarketMonitorVisible() {
        return isMarketMonitorVisible(getUiSnapshot(false));
    }

    private boolean isMarketMonitorVisible(UiSnapshot snapshot) {
        int filters = countExactLabels(snapshot, MARKET_FILTERS);
        int periods = countExactLabels(snapshot, PERIOD_TABS);
        return filters >= 2 && periods >= 2 && findIndexHeaderNode(snapshot.nodes) != null;
    }

    /**
     * Detects the same Market Monitor screen when the page has been vertically
     * scrolled and the heading/filter row is above the viewport. This state is
     * still valid and must be restored in-place; tapping Stocks again does not
     * reset the Flutter ScrollView position.
     */
    private boolean isMarketMonitorTableContextVisible(UiSnapshot snapshot) {
        if (snapshot == null || findIndexHeaderNode(snapshot.nodes) == null) {
            return false;
        }

        int periods = countExactLabels(snapshot, PERIOD_TABS);
        boolean stocksNavigationVisible = snapshot.hasExact(STOCKS_TAB)
                || snapshot.hasExact("Funds")
                || snapshot.hasExact("Portfolio")
                || snapshot.hasExact("Hub");

        return periods >= 2 && stocksNavigationVisible;
    }

    /**
     * Scrolls the parent Market Monitor ScrollView upward until the market
     * filters, period tabs and Index header are visible together.
     */
    private boolean restoreMarketMonitorControlArea() {
        ReportLogger.step("Restoring Market Monitor controls after vertical table scroll");

        for (int attempt = 1; attempt <= 5; attempt++) {
            UiSnapshot snapshot = getUiSnapshot(true);
            if (isMarketMonitorVisible(snapshot)) {
                ReportLogger.pass("Market Monitor control area restored");
                return true;
            }

            if (!isMarketMonitorTableContextVisible(snapshot)) {
                ReportLogger.debug("Table-only Market Monitor context is no longer visible during restore"
                        + " | attempt=" + attempt);
                return false;
            }

            ReportLogger.step("Scrolling parent Market Monitor viewport upward"
                    + " | attempt=" + attempt);
            scrollParentMarketPage(ScrollDirection.UP);
            sleep(280);
        }

        UiSnapshot finalSnapshot = getUiSnapshot(true);
        boolean restored = isMarketMonitorVisible(finalSnapshot);
        if (restored) {
            ReportLogger.pass("Market Monitor control area restored");
        } else {
            ReportLogger.debug("Unable to restore complete Market Monitor control area"
                    + " | visibleValues=" + finalSnapshot.values);
        }
        return restored;
    }

    private int countExactLabels(UiSnapshot snapshot, String[] labels) {
        int count = 0;
        for (String label : labels) {
            if (snapshot.hasExact(label)) {
                count++;
            }
        }
        return count;
    }

    private void tapMarketMonitorControl(String label, ControlType type) {
        ReportLogger.step("Tapping " + type.displayName + ": " + label);

        WebElement element = findExactAccessibilityElement(label);
        if (element != null) {
            tapElementCenter(element);
        } else {
            NodeInfo node = findExactNode(getUiSnapshot(true), label);
            if (node == null) {
                throw new AssertionError(type.displayName + " not found: " + label
                        + " | visibleValues=" + collectVisibleStrings());
            }
            tapRectCenter(node.rect);
        }

        if (type == ControlType.MARKET_FILTER || type == ControlType.PERIOD_TAB) {
            tableHorizontallyShifted = false;
        }

        sleep(INTERACTION_SETTLE_MS);
        ReportLogger.pass("Tapped " + type.displayName + ": " + label);
    }

    private void assertTextVisible(UiSnapshot snapshot, String expected, String label) {
        if (!snapshot.contains(expected)) {
            throw new AssertionError(label + " not visible. Expected: " + expected
                    + " | visibleValues=" + snapshot.values);
        }
        logValidatedText(label, expected);
    }

    private void assertControlVisible(
            UiSnapshot snapshot,
            String label,
            String controlType
    ) {
        if (!snapshot.hasExact(label)) {
            throw new AssertionError(controlType + " not visible: " + label
                    + " | visibleValues=" + snapshot.values);
        }
        logValidatedText(controlType, label);
    }

    private TableSnapshot waitForTableReady(String context) {
        AssertionError lastFailure = null;

        for (int attempt = 1; attempt <= TABLE_READY_ATTEMPTS; attempt++) {
            try {
                UiSnapshot ui = getUiSnapshot(true);
                TableSnapshot table = captureTableSnapshot(ui);
                validateTableSnapshot(table, context);
                return table;
            } catch (AssertionError e) {
                lastFailure = e;
                sleep(220);
            }
        }

        throw new AssertionError(context + " did not become valid"
                + (lastFailure == null ? "" : " | lastFailure=" + lastFailure.getMessage()));
    }

    private TableSnapshot waitForTableAfterInteraction(
            String previousSignature,
            String context
    ) {
        // Structural validity is the requirement. Do not wait for all retries just
        // because the selected option was already active and the signature stayed
        // unchanged. This removes the former 60-70 second delay per test.
        TableSnapshot snapshot = waitForTableReady(context);

        if (previousSignature != null
                && !previousSignature.isEmpty()
                && previousSignature.equals(snapshot.signature)) {
            ReportLogger.debug(context
                    + " returned a valid unchanged signature; accepting immediately");
        }

        return snapshot;
    }

    private TableSnapshot captureTableSnapshot(UiSnapshot ui) {
        Dimension screen = driver.manage().window().getSize();
        NodeInfo indexHeader = findIndexHeaderNode(ui.nodes);

        if (indexHeader == null) {
            throw new AssertionError("Market Monitor table 'Index' header is not visible"
                    + " | values=" + ui.values);
        }

        int headerCenterY = centerY(indexHeader.rect);
        int headerTolerance = Math.max(55, indexHeader.rect.getHeight() * 2);
        int rowStartY = indexHeader.rect.getY() + Math.max(38, indexHeader.rect.getHeight());
        int tableBottomY = findTableBottomY(ui.nodes, screen);

        LinkedHashSet<String> columnHeaders = new LinkedHashSet<>();
        LinkedHashSet<String> rowLabels = new LinkedHashSet<>();
        LinkedHashSet<String> numericValues = new LinkedHashSet<>();

        for (NodeInfo node : ui.nodes) {
            Rectangle rect = node.rect;
            int x = centerX(rect);
            int y = centerY(rect);
            String text = normalizeSpaces(node.text);

            if (text.isEmpty()) {
                continue;
            }

            if (Math.abs(y - headerCenterY) <= headerTolerance
                    && x > centerX(indexHeader.rect)
                    && !isMarketPageControlText(text)
                    && !text.equalsIgnoreCase(INDEX_HEADER)) {
                columnHeaders.add(text);
            }

            if (y <= rowStartY || y >= tableBottomY) {
                continue;
            }

            for (String number : extractNumericTokens(text)) {
                numericValues.add(number);
            }

            boolean leftColumn = x <= (int) (screen.getWidth() * 0.47)
                    || rect.getX() <= (int) (screen.getWidth() * 0.12);

            if (leftColumn && isPotentialIndexRowLabel(text)) {
                rowLabels.add(cleanRowLabel(text));
            } else if (containsLetters(text)
                    && !extractNumericTokens(text).isEmpty()
                    && !isMarketPageControlText(text)) {
                rowLabels.add(cleanRowLabel(text));
            }
        }

        LinkedHashSet<String> normalizedHeaders = new LinkedHashSet<>();
        for (String header : columnHeaders) {
            for (String part : splitSemanticValue(header)) {
                if (!part.equalsIgnoreCase(INDEX_HEADER)
                        && !isMarketPageControlText(part)
                        && !PURE_NUMBER.matcher(part).matches()) {
                    normalizedHeaders.add(part);
                }
            }
        }

        // Flutter sometimes exposes row semantic nodes more reliably than the
        // visual header cells. Derive date/interval evidence from visible nodes as
        // a safe fallback without hardcoding actual dates.
        if (normalizedHeaders.size() < 2) {
            for (NodeInfo node : ui.nodes) {
                if (centerY(node.rect) <= rowStartY || centerY(node.rect) >= tableBottomY) {
                    continue;
                }
                for (String part : splitSemanticValue(node.text)) {
                    String lower = part.toLowerCase(Locale.ENGLISH);
                    if (MONTH_YEAR_HEADER.matcher(part).matches()
                            || YEAR_HEADER.matcher(part).matches()
                            || lower.equals("ytd")
                            || lower.contains("day")
                            || lower.contains("week")
                            || lower.contains("month")
                            || lower.contains("year")) {
                        normalizedHeaders.add(part);
                    }
                }
            }
        }

        List<String> headers = limitList(new ArrayList<>(normalizedHeaders), 16);
        List<String> rows = limitList(new ArrayList<>(rowLabels), 14);
        List<String> values = limitList(new ArrayList<>(numericValues), 28);

        String headerSignature = String.join("|", headers);
        String rowSignature = String.join("|", rows);
        String valueSignature = String.join("|", values);

        return new TableSnapshot(
                headers,
                rows,
                values,
                headerCenterY,
                headerSignature,
                rowSignature,
                valueSignature,
                headerSignature + "::" + rowSignature + "::" + valueSignature
        );
    }

    private void validateTableSnapshot(TableSnapshot snapshot, String context) {
        if (snapshot == null) {
            throw new AssertionError(context + ": table snapshot is null");
        }
        if (snapshot.columnHeaders.size() < 2) {
            throw new AssertionError(context + ": fewer than two table headers found"
                    + " | snapshot=" + snapshot);
        }
        if (snapshot.rowLabels.isEmpty()) {
            throw new AssertionError(context + ": no visible index row label found"
                    + " | snapshot=" + snapshot);
        }
        if (snapshot.numericValues.size() < 2) {
            throw new AssertionError(context + ": insufficient numeric return data found"
                    + " | snapshot=" + snapshot);
        }
    }

    private NodeInfo findIndexHeaderNode(List<NodeInfo> nodes) {
        NodeInfo fallback = null;

        for (NodeInfo node : nodes) {
            String lower = normalizeSpaces(node.text).toLowerCase(Locale.ENGLISH);
            if (lower.equals("index")) {
                return node;
            }
            if (fallback == null
                    && (lower.startsWith("index ")
                    || lower.startsWith("index,")
                    || lower.startsWith("index ▾")
                    || lower.startsWith("index ▼"))) {
                fallback = node;
            }
        }

        return fallback;
    }

    private int findTableBottomY(List<NodeInfo> nodes, Dimension screen) {
        int bottom = (int) (screen.getHeight() * 0.925);

        for (NodeInfo node : nodes) {
            String text = normalizeSpaces(node.text);
            if (text.equalsIgnoreCase("Funds")
                    || text.equalsIgnoreCase("Stocks")
                    || text.equalsIgnoreCase("Portfolio")
                    || text.equalsIgnoreCase("Hub")) {
                int candidate = node.rect.getY();
                if (candidate > (int) (screen.getHeight() * 0.70)) {
                    bottom = Math.min(bottom, candidate);
                }
            }
        }

        return bottom;
    }

    private boolean isPotentialIndexRowLabel(String value) {
        String clean = normalizeSpaces(value);
        String lower = clean.toLowerCase(Locale.ENGLISH);

        if (clean.length() < 3
                || PURE_NUMBER.matcher(clean).matches()
                || MONTH_YEAR_HEADER.matcher(clean).matches()
                || YEAR_HEADER.matcher(clean).matches()
                || isMarketPageControlText(clean)
                || !containsLetters(clean)) {
            return false;
        }

        return !lower.equals("index")
                && !lower.equals("return")
                && !lower.equals("returns");
    }

    private String cleanRowLabel(String value) {
        String clean = normalizeSpaces(value);
        String withoutTrailingCells = clean.replaceFirst(
                "(?:\\s+[-+]?\\d+(?:,\\d{3})*(?:\\.\\d+)?%?){2,}$",
                ""
        ).trim();
        return withoutTrailingCells.isEmpty() ? clean : withoutTrailingCells;
    }

    private List<String> extractNumericTokens(String value) {
        String clean = normalizeSpaces(value);
        List<String> result = new ArrayList<>();

        if (MONTH_YEAR_HEADER.matcher(clean).matches() || YEAR_HEADER.matcher(clean).matches()) {
            return result;
        }

        Matcher matcher = NUMBER_TOKEN.matcher(clean);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private List<String> splitSemanticValue(String value) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        String prepared = value == null
                ? ""
                : value.replace("\\n", "\n").replace("\r", "\n");

        for (String part : prepared.split("\\n+|\\|")) {
            String clean = normalizeSpaces(part);
            if (!clean.isEmpty()) {
                parts.add(clean);
            }
        }

        if (parts.isEmpty() && value != null && !normalizeSpaces(value).isEmpty()) {
            parts.add(normalizeSpaces(value));
        }
        return new ArrayList<>(parts);
    }

    private boolean isMarketPageControlText(String value) {
        String lower = normalizeSpaces(value).toLowerCase(Locale.ENGLISH);

        return lower.isEmpty()
                || lower.equals(MARKET_MONITOR.toLowerCase(Locale.ENGLISH))
                || lower.equals(QUICK_SEARCH.toLowerCase(Locale.ENGLISH))
                || lower.contains("search for a stock")
                || lower.contains("search any stock")
                || lower.contains("comprehensive research")
                || lower.contains("stock advisor")
                || lower.equals(FILTER_ALL.toLowerCase(Locale.ENGLISH))
                || lower.equals(FILTER_INDIAN.toLowerCase(Locale.ENGLISH))
                || lower.equals(FILTER_GLOBAL.toLowerCase(Locale.ENGLISH))
                || lower.equals(PERIOD_TRAILING.toLowerCase(Locale.ENGLISH))
                || lower.equals(PERIOD_ANNUAL.toLowerCase(Locale.ENGLISH))
                || lower.equals(PERIOD_QUARTERLY.toLowerCase(Locale.ENGLISH))
                || lower.equals(PERIOD_MONTHLY.toLowerCase(Locale.ENGLISH))
                || lower.equals("funds")
                || lower.equals("stocks")
                || lower.equals("portfolio")
                || lower.equals("hub")
                || lower.equals("value research stock advisor")
                || lower.equals("open stock advisor details");
    }

    private void logTableSnapshot(String label, TableSnapshot snapshot) {
        logValidatedText(label + " headers", snapshot.columnHeaders.toString());
        logValidatedText(label + " visible rows", snapshot.rowLabels.toString());
        logValidatedText(label + " numeric values", snapshot.numericValues.toString());
    }

    private void resetTableToTopAndLeft() {
        if (!isMarketMonitorVisible()) {
            openMarketMonitorFromStocks();
        }

        if (!horizontalBaselineInitialised || tableHorizontallyShifted) {
            int headerY = resolveIndexHeaderCenterY();
            swipeTableHorizontallyRight(headerY);
            sleep(220);
            swipeTableHorizontallyRight(headerY);
            sleep(220);
            horizontalBaselineInitialised = true;
            tableHorizontallyShifted = false;
        }
    }

    private int resolveIndexHeaderCenterY() {
        NodeInfo index = findIndexHeaderNode(getUiSnapshot(false).nodes);
        if (index != null) {
            return centerY(index.rect);
        }
        return (int) (driver.manage().window().getSize().getHeight() * 0.69);
    }

    // =========================================================
    // NAVIGATION / ELEMENT HELPERS
    // =========================================================

    private void recoverToAdvisorMainNavigationIfNeeded() {
        if (bottomNavigationVisible()) {
            return;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            pressBackSilently();
            sleep(300);
            if (bottomNavigationVisible()) {
                return;
            }
        }
    }

    private boolean bottomNavigationVisible() {
        UiSnapshot snapshot = getUiSnapshot(false);
        return snapshot.hasExact("Funds")
                || snapshot.hasExact(STOCKS_TAB)
                || snapshot.hasExact("Portfolio")
                || snapshot.hasExact("Hub");
    }

    private WebElement findExactAccessibilityElement(String label) {
        try {
            WebElement element = driver.findElement(AppiumBy.accessibilityId(label));
            Rectangle rect = safeRect(element);
            return rect != null && rect.getWidth() > 0 && rect.getHeight() > 0
                    ? element
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Rectangle safeRect(WebElement element) {
        if (element == null) {
            return null;
        }
        try {
            return element.getRect();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isInBottomNavigationBand(Rectangle rect) {
        if (rect == null) {
            return false;
        }
        Dimension size = driver.manage().window().getSize();
        return centerY(rect) >= (int) (size.getHeight() * 0.80);
    }

    private String escapeUiAutomatorText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private NodeInfo findExactNode(UiSnapshot snapshot, String label) {
        String expected = normalizeSpaces(label).toLowerCase(Locale.ENGLISH);
        for (NodeInfo node : snapshot.nodes) {
            if (normalizeSpaces(node.text).toLowerCase(Locale.ENGLISH).equals(expected)) {
                return node;
            }
        }
        return null;
    }

    private void logSelectionAttributes(String label) {
        NodeInfo node = findExactNode(getUiSnapshot(false), label);
        if (node == null) {
            return;
        }

        ReportLogger.debug("Control state after tap"
                + " | label=" + label
                + " | selected=" + node.selected
                + " | checked=" + node.checked
                + " | clickable=" + node.clickable);
    }

    // =========================================================
    // FAST PAGE-SOURCE SNAPSHOT
    // =========================================================

    private UiSnapshot getUiSnapshot(boolean forceRefresh) {
        long now = System.currentTimeMillis();

        if (!forceRefresh
                && uiSnapshotCache != null
                && now - uiSnapshotCacheAtMs <= SOURCE_CACHE_TTL_MS) {
            return uiSnapshotCache;
        }

        UiSnapshot parsed = parsePageSource(driver.getPageSource());
        uiSnapshotCache = parsed;
        uiSnapshotCacheAtMs = System.currentTimeMillis();
        return parsed;
    }

    private UiSnapshot parsePageSource(String xml) {
        List<NodeInfo> nodes = new ArrayList<>();
        LinkedHashSet<String> values = new LinkedHashSet<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
                // Some JDK XML implementations may not expose this feature.
            }

            Document document = factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml == null ? "" : xml))
            );
            NodeList all = document.getElementsByTagName("*");

            for (int i = 0; i < all.getLength(); i++) {
                if (!(all.item(i) instanceof Element)) {
                    continue;
                }

                Element element = (Element) all.item(i);
                Rectangle rect = parseBounds(element.getAttribute("bounds"));
                if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                    continue;
                }

                String displayed = element.getAttribute("displayed");
                if ("false".equalsIgnoreCase(displayed)) {
                    continue;
                }

                LinkedHashSet<String> readable = new LinkedHashSet<>();
                addReadableValues(readable, element.getAttribute("content-desc"));
                addReadableValues(readable, element.getAttribute("text"));
                addReadableValues(readable, element.getAttribute("name"));

                for (String value : readable) {
                    values.add(value);
                    nodes.add(new NodeInfo(
                            value,
                            rect,
                            element.getAttribute("class"),
                            element.getAttribute("clickable"),
                            element.getAttribute("selected"),
                            element.getAttribute("checked")
                    ));
                }
            }
        } catch (Exception e) {
            throw new AssertionError("Unable to parse Appium page source: "
                    + cleanError(e.getMessage()), e);
        }

        return new UiSnapshot(nodes, new ArrayList<>(values));
    }

    private void addReadableValues(Set<String> target, String raw) {
        if (raw == null) {
            return;
        }

        String whole = normalizeSpaces(raw);
        if (!whole.isEmpty() && !whole.equalsIgnoreCase("null")) {
            target.add(whole);
        }

        String prepared = raw.replace("\\n", "\n").replace("\r", "\n");
        for (String part : prepared.split("\\n+|\\|")) {
            String clean = normalizeSpaces(part);
            if (!clean.isEmpty() && !clean.equalsIgnoreCase("null")) {
                target.add(clean);
            }
        }
    }

    private Rectangle parseBounds(String bounds) {
        Matcher matcher = BOUNDS_PATTERN.matcher(bounds == null ? "" : bounds);
        if (!matcher.matches()) {
            return null;
        }

        int x1 = Integer.parseInt(matcher.group(1));
        int y1 = Integer.parseInt(matcher.group(2));
        int x2 = Integer.parseInt(matcher.group(3));
        int y2 = Integer.parseInt(matcher.group(4));
        return new Rectangle(
                new Point(x1, y1),
                new Dimension(Math.max(0, x2 - x1), Math.max(0, y2 - y1))
        );
    }

    private List<String> collectVisibleStrings() {
        return new ArrayList<>(getUiSnapshot(false).values);
    }

    private void invalidateUiSnapshot() {
        uiSnapshotCache = null;
        uiSnapshotCacheAtMs = 0L;
    }

    // =========================================================
    // GESTURES
    // =========================================================

    private void tapElementCenter(WebElement element) {
        Rectangle rect = safeRect(element);
        if (rect == null) {
            throw new AssertionError("Unable to read element bounds for tap");
        }
        tapRectCenter(rect);
    }

    private void tapRectCenter(Rectangle rect) {
        tapByCoordinates(centerX(rect), centerY(rect));
    }

    private void tapByCoordinates(int x, int y) {
        invalidateUiSnapshot();

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

    private void swipeTableHorizontallyLeft(int headerCenterY) {
        Dimension size = driver.manage().window().getSize();
        double y = clampRatio((double) (headerCenterY + 95) / size.getHeight(), 0.64, 0.82);
        performSwipe(0.87, y, 0.38, y, 420);
        tableHorizontallyShifted = true;
    }

    private void swipeTableHorizontallyRight(int headerCenterY) {
        Dimension size = driver.manage().window().getSize();
        double y = clampRatio((double) (headerCenterY + 95) / size.getHeight(), 0.64, 0.82);
        performSwipe(0.35, y, 0.88, y, 360);
        tableHorizontallyShifted = false;
    }

    private void performSwipe(
            double startXRatio,
            double startYRatio,
            double endXRatio,
            double endYRatio,
            int durationMillis
    ) {
        invalidateUiSnapshot();

        Dimension size = driver.manage().window().getSize();
        int startX = (int) (size.getWidth() * startXRatio);
        int startY = (int) (size.getHeight() * startYRatio);
        int endX = (int) (size.getWidth() * endXRatio);
        int endY = (int) (size.getHeight() * endYRatio);

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
            invalidateUiSnapshot();
            driver.navigate().back();
        } catch (Exception ignored) {
            // Recovery back must not hide the original failure.
        }
    }

    // =========================================================
    // VALUE / REPORT HELPERS
    // =========================================================

    private int centerX(Rectangle rect) {
        return rect.getX() + rect.getWidth() / 2;
    }

    private int centerY(Rectangle rect) {
        return rect.getY() + rect.getHeight() / 2;
    }

    private double clampRatio(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean containsLetters(String value) {
        return value != null && value.matches(".*[A-Za-z].*");
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private <T> List<T> limitList(List<T> input, int maxSize) {
        if (input.size() <= maxSize) {
            return input;
        }
        return new ArrayList<>(input.subList(0, maxSize));
    }

    private void logValidatedText(String label, String value) {
        String safeLabel = normalizeSpaces(label);
        String safeValue = normalizeSpaces(value);

        if (safeValue.isEmpty()) {
            return;
        }

        ReportLogger.pass("Validated text/value - " + safeLabel + ": " + safeValue);

        try {
            if (ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().pass(
                        "<b>Validated text/value:</b> " + safeLabel + " = " + safeValue
                );
            }
        } catch (Exception ignored) {
            // Reporting must never affect app validation.
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }
        return message.replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private enum ControlType {
        MARKET_FILTER("market filter"),
        PERIOD_TAB("period tab");

        private final String displayName;

        ControlType(String displayName) {
            this.displayName = displayName;
        }
    }

    private enum ScrollDirection {
        DOWN("down", "down"),
        UP("up", "up");

        private final String label;
        private final String appiumDirection;

        ScrollDirection(String label, String appiumDirection) {
            this.label = label;
            this.appiumDirection = appiumDirection;
        }
    }

    private static final class NodeInfo {
        private final String text;
        private final Rectangle rect;
        private final String className;
        private final String clickable;
        private final String selected;
        private final String checked;

        private NodeInfo(
                String text,
                Rectangle rect,
                String className,
                String clickable,
                String selected,
                String checked
        ) {
            this.text = text;
            this.rect = rect;
            this.className = className;
            this.clickable = clickable;
            this.selected = selected;
            this.checked = checked;
        }
    }

    private static final class UiSnapshot {
        private final List<NodeInfo> nodes;
        private final List<String> values;
        private final Set<String> exactLower;

        private UiSnapshot(List<NodeInfo> nodes, List<String> values) {
            this.nodes = nodes;
            this.values = values;
            this.exactLower = new LinkedHashSet<>();
            for (String value : values) {
                exactLower.add(value.toLowerCase(Locale.ENGLISH));
            }
        }

        private boolean hasExact(String expected) {
            return expected != null
                    && exactLower.contains(expected.trim().toLowerCase(Locale.ENGLISH));
        }

        private boolean contains(String expected) {
            if (expected == null || expected.trim().isEmpty()) {
                return false;
            }
            String target = expected.trim().toLowerCase(Locale.ENGLISH);
            for (String value : values) {
                String lower = value.toLowerCase(Locale.ENGLISH);
                if (lower.equals(target) || lower.contains(target)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class VerticalViewport {
        private final List<String> rowLabels;
        private final String rowSignature;
        private final String geometrySignature;

        private VerticalViewport(
                List<String> rowLabels,
                String rowSignature,
                String geometrySignature
        ) {
            this.rowLabels = rowLabels;
            this.rowSignature = rowSignature;
            this.geometrySignature = geometrySignature;
        }

        @Override
        public String toString() {
            return "VerticalViewport{"
                    + "rowLabels=" + rowLabels
                    + ", geometrySignature='" + geometrySignature + '\''
                    + '}';
        }
    }

    private static final class TableSnapshot {
        private final List<String> columnHeaders;
        private final List<String> rowLabels;
        private final List<String> numericValues;
        private final int headerCenterY;
        private final String headerSignature;
        private final String rowSignature;
        private final String valueSignature;
        private final String signature;

        private TableSnapshot(
                List<String> columnHeaders,
                List<String> rowLabels,
                List<String> numericValues,
                int headerCenterY,
                String headerSignature,
                String rowSignature,
                String valueSignature,
                String signature
        ) {
            this.columnHeaders = columnHeaders;
            this.rowLabels = rowLabels;
            this.numericValues = numericValues;
            this.headerCenterY = headerCenterY;
            this.headerSignature = headerSignature;
            this.rowSignature = rowSignature;
            this.valueSignature = valueSignature;
            this.signature = signature;
        }

        @Override
        public String toString() {
            return "TableSnapshot{"
                    + "columnHeaders=" + columnHeaders
                    + ", rowLabels=" + rowLabels
                    + ", numericValues=" + numericValues
                    + '}';
        }
    }
}