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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VRSA Hub -> Stocks -> Coverage and insights.
 *
 * Stability design:
 * - Uses the exact Hub tile accessibility id captured from Appium Inspector.
 * - Uses live Flutter semantics for category chips: "<label>, tab X of Y".
 * - Never hardcodes Coverage/Insights category names or article titles.
 * - Discovers the category count and labels at runtime, then validates every tab.
 * - Uses bounded W3C swipes with progress detection for horizontal and vertical lists.
 * - Treats loading skeletons as transient and waits for live content/empty-state semantics.
 * - Avoids absolute XPath, UiSelector instance locators and fixed screen coordinates except
 *   for ratio-based swipe fallbacks when a scroll container is not exposed.
 */
public class CoverageInsightsPage {

    public static final String BUILD_VERSION = "COVERAGE_INSIGHTS_DYNAMIC_V4_20260904";

    private static final long SHORT_WAIT_MS = 12_000L;
    private static final long PAGE_READY_WAIT_MS = 25_000L;
    private static final long CONTENT_WAIT_MS = 35_000L;
    private static final long POLL_MS = 350L;

    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By COVERAGE_INSIGHTS_TILE =
            AppiumBy.accessibilityId("Open coverage");
    private static final By ALL_STORIES = AppiumBy.accessibilityId("All Stories");

    // Flutter exposes these top tabs differently across builds. On the current build
    // the visible label may be represented as e.g. "Coverage, tab 1 of 3" instead
    // of a standalone accessibility id "Coverage". Keep only the stable business
    // labels here and resolve the actual live semantic element at runtime.
    private static final String TOP_COVERAGE = "Coverage";
    private static final String TOP_INSIGHTS = "Insights";
    private static final String TOP_REPORTS = "Reports";

    // Current Flutter builds normally expose top tabs as semantic values such as
    // "Coverage, tab 1 of 3". If a future build stops exposing those semantics, the
    // fallback is anchored to the live All Stories container, never absolute pixels.
    private static final double TOP_TAB_Y_RATIO_IN_ALL_STORIES = 0.45;
    private static final double TOP_TAB_FIRST_X_RATIO = 0.13;
    private static final double TOP_TAB_X_STEP_RATIO = 0.26;

    // Appium Inspector (04-Sep-2026) proves the Coverage search field is exposed
    // as android.widget.EditText with no usable text/content-desc. Locate it by
    // class plus live geometry relative to the category rail, never by placeholder.
    private static final By EDIT_TEXT = AppiumBy.className("android.widget.EditText");
    private static final By HORIZONTAL_SCROLL_VIEW = AppiumBy.className("android.widget.HorizontalScrollView");
    private static final By VIEW = AppiumBy.className("android.view.View");
    private static final By IMAGE_VIEW = AppiumBy.className("android.widget.ImageView");
    private static final By BUTTON = AppiumBy.className("android.widget.Button");

    // Hub exposes many navigation tiles with content-desc beginning with "Open ".
    // This is used only as a current-screen signature so we do not tap Hub again
    // when cleanup already returned us there.
    private static final By HUB_OPEN_BUTTONS = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Open \")"
    );

    private static final Pattern TAB_PATTERN = Pattern.compile(
            "^(.+?),\\s*tab\\s+(\\d+)\\s+of\\s+(\\d+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] EMPTY_STATE_MARKERS = new String[]{
            "no stories available",
            "no result found",
            "no results found",
            "underlying data is unavailable",
            "underlying data unavailable",
            "no data available",
            "nothing to show"
    };

    private final AndroidDriver driver;

    public CoverageInsightsPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // =========================================================
    // Public navigation / recovery
    // =========================================================

    public void openFromHub() {
        ReportLogger.step("[" + BUILD_VERSION + "] Opening Coverage and insights from Hub");
        assertDriverAlive("before Coverage and insights navigation");

        if (isAllStoriesOpen()) {
            ReportLogger.pass("Coverage and insights is already open");
            return;
        }

        // AuthHelper is still the single source of truth for login/PIN recovery.
        new AuthHelper(driver).ensureLoggedIn();

        // Cleanup normally returns to Hub. Do not tap the already-selected Hub tab
        // again because Flutter rebuilds/refreshes the Hub unnecessarily.
        if (isHubScreenReady()) {
            ReportLogger.debug("Hub is already active - skipping redundant Hub tab tap/refresh");
        } else {
            WebElement hub = bottomMostVisible(HUB);
            if (hub == null) {
                throw new AssertionError("Hub bottom-navigation tab is not visible");
            }
            safeClick(hub, "Hub bottom-navigation tab");
            if (!waitForHubScreenReady(SHORT_WAIT_MS)) {
                throw new AssertionError(
                        "Hub tab was tapped but Hub navigation tiles did not become visible. visible="
                                + collectVisibleValues()
                );
            }
        }

        WebElement tile = findHubTileWithBoundedScroll();
        if (tile == null) {
            throw new AssertionError(
                    "Coverage and insights Hub tile was not found after bounded scrolling. visible="
                            + collectVisibleValues()
            );
        }

        safeClick(tile, "Coverage and insights Hub tile");
        waitForAllStoriesReady();
        ReportLogger.pass("Coverage and insights page opened successfully");
    }

    public void recoverToHubSafely() {
        try {
            if (isHubScreenReady()) {
                ReportLogger.debug("Coverage/Insights cleanup is already at Hub - no action required");
                return;
            }

            // Preferred cleanup: this module was opened from Hub, so one controlled
            // Android Back returns to the existing Hub instance without re-selecting
            // the Hub bottom tab and forcing an unnecessary Hub refresh/rebuild.
            if (isAllStoriesOpen()) {
                driver.navigate().back();
                if (waitForHubScreenReady(8_000L)) {
                    ReportLogger.debug("Coverage/Insights cleanup returned to Hub with one controlled Back");
                    return;
                }
            }

            // Fallback only when Back did not establish Hub. Never tap Hub if Hub is
            // already proven active.
            if (!isHubScreenReady()) {
                WebElement hub = bottomMostVisible(HUB);
                if (hub != null) {
                    safeClick(hub, "Hub bottom-navigation fallback during cleanup");
                    waitForHubScreenReady(8_000L);
                }
            }
        } catch (Exception e) {
            ReportLogger.debug("Coverage/Insights cleanup ignored: " + clean(e.getMessage()));
        } catch (AssertionError e) {
            ReportLogger.debug("Coverage/Insights cleanup assertion ignored: " + clean(e.getMessage()));
        }
    }

    // =========================================================
    // Landing / top-level sections
    // =========================================================

    public void validateLandingPage() {
        openFromHub();

        requireVisible(ALL_STORIES, "All Stories header container");
        WebElement search = waitForCoverageSearchField(PAGE_READY_WAIT_MS);
        validateCoverageSearchElement(search, "Coverage landing search control");

        List<RuntimeTab> visibleTabs = waitForRuntimeCategoryTabs(PAGE_READY_WAIT_MS);
        if (visibleTabs.isEmpty()) {
            throw new AssertionError("Coverage landing category rail is not available");
        }

        ReportLogger.pass(
                "All Stories landing is ready with Coverage search and live category rail; "
                        + "top-level tabs will use semantic locator when available or All Stories-relative gesture fallback"
        );
    }

    public void validateCoverageSearchIsAvailable() {
        openFromHub();
        openTopSection(TOP_COVERAGE);

        WebElement search = waitForCoverageSearchField(SHORT_WAIT_MS);
        validateCoverageSearchElement(search, "Coverage company-search control");
        Rectangle rect = safeRect(search);

        ReportLogger.pass(
                "Coverage search control detected as live android.widget.EditText"
                        + (rect == null ? "" : " | bounds=" + rect)
        );
    }

    public void validateTopLevelSwitching() {
        openFromHub();

        openTopSection(TOP_COVERAGE);
        waitForSectionResolved("Coverage");

        openTopSection(TOP_INSIGHTS);
        waitForSectionResolved("Insights");

        openTopSection(TOP_REPORTS);
        waitForSectionResolved("Reports");

        openTopSection(TOP_COVERAGE);
        waitForSectionResolved("Coverage");

        ReportLogger.pass("Coverage -> Insights -> Reports -> Coverage switching is stable");
    }

    // =========================================================
    // Dynamic category validation
    // =========================================================

    public void validateEveryCoverageCategoryDynamically() {
        openFromHub();
        openTopSection(TOP_COVERAGE);
        validateEveryRuntimeCategory("Coverage");
    }

    public void validateEveryInsightsCategoryDynamically() {
        openFromHub();
        openTopSection(TOP_INSIGHTS);
        validateEveryRuntimeCategory("Insights");
    }

    public void validateReportsFeedAndStableScroll() {
        openFromHub();
        openTopSection(TOP_REPORTS);

        ContentState state = waitForSectionResolved("Reports");
        logContentState("Reports", state);

        if (state.hasLiveContent()) {
            validateVerticalScrollProgress("Reports");
        } else {
            ReportLogger.pass("Reports resolved to an explicit empty state; vertical scroll is not applicable");
        }
    }

    private void validateEveryRuntimeCategory(String sectionName) {
        ReportLogger.step("Discovering " + sectionName + " category tabs from live semantics");

        Map<Integer, RuntimeTab> tabs = discoverAllRuntimeTabs();
        if (tabs.isEmpty()) {
            throw new AssertionError(
                    sectionName + " exposes no '<label>, tab X of Y' category semantics. visible="
                            + collectVisibleValues()
            );
        }

        int expectedTotal = tabs.values().iterator().next().total;
        if (tabs.size() != expectedTotal) {
            throw new AssertionError(
                    sectionName + " category discovery incomplete. discovered=" + tabs.size()
                            + ", semanticsTotal=" + expectedTotal + ", tabs=" + tabs.values()
            );
        }

        ReportLogger.pass(
                sectionName + " runtime category discovery complete: " + expectedTotal + " tabs -> "
                        + tabs.values()
        );

        for (int index = 1; index <= expectedTotal; index++) {
            RuntimeTab tab = tabs.get(index);
            if (tab == null) {
                throw new AssertionError(sectionName + " category index " + index + " was not discovered");
            }

            ReportLogger.step(
                    "Validating " + sectionName + " runtime tab " + index + "/" + expectedTotal
                            + ": " + tab.label
            );

            revealAndTapRuntimeTab(index, expectedTotal);
            ContentState state = waitForSectionResolved(sectionName + " / " + tab.label);
            logContentState(sectionName + " / " + tab.label, state);

            if (state.hasLiveContent()) {
                validateVerticalScrollProgress(sectionName + " / " + tab.label);
            }
        }

        ReportLogger.pass(sectionName + " - every runtime-discovered category validated");
    }

    /**
     * Discovers all tab chips by their live accessibility descriptions. The method first
     * returns the rail to its beginning, then advances until every index reported by the
     * semantics has been collected. No category names/counts are hardcoded.
     */
    private Map<Integer, RuntimeTab> discoverAllRuntimeTabs() {
        resetCategoryRailToStart();

        Map<Integer, RuntimeTab> discovered = new LinkedHashMap<>();
        int expectedTotal = -1;
        int noProgress = 0;
        int lastSize = -1;

        for (int attempt = 0; attempt < 20; attempt++) {
            List<RuntimeTab> visibleTabs = visibleRuntimeTabs();
            for (RuntimeTab tab : visibleTabs) {
                discovered.put(tab.index, tab);
                if (expectedTotal < 0) {
                    expectedTotal = tab.total;
                } else if (tab.total != expectedTotal) {
                    throw new AssertionError(
                            "Inconsistent runtime tab totals exposed by app: expected " + expectedTotal
                                    + " but saw " + tab.total + " on " + tab
                    );
                }
            }

            if (expectedTotal > 0 && discovered.size() >= expectedTotal) {
                break;
            }

            if (discovered.size() == lastSize) {
                noProgress++;
            } else {
                noProgress = 0;
                lastSize = discovered.size();
            }

            if (noProgress >= 3) {
                break;
            }

            swipeCategoryRail(true);
            sleep(300L);
        }

        return discovered;
    }

    private void revealAndTapRuntimeTab(int targetIndex, int total) {
        RuntimeTab visible = findVisibleRuntimeTab(targetIndex, total);
        if (visible != null && visible.element != null) {
            safeClick(visible.element, "runtime category tab " + targetIndex + "/" + total + " - " + visible.label);
            sleep(250L);
            return;
        }

        // Determine direction from currently visible semantic indices.
        for (int attempt = 0; attempt < 16; attempt++) {
            List<RuntimeTab> current = visibleRuntimeTabs();
            int min = current.stream().mapToInt(t -> t.index).min().orElse(targetIndex);
            int max = current.stream().mapToInt(t -> t.index).max().orElse(targetIndex);

            if (targetIndex < min) {
                swipeCategoryRail(false);
            } else if (targetIndex > max) {
                swipeCategoryRail(true);
            } else {
                // Target should be between visible indices but may be clipped. Nudge toward it.
                swipeCategoryRail(targetIndex >= ((min + max) / 2), true);
            }

            sleep(250L);
            visible = findVisibleRuntimeTab(targetIndex, total);
            if (visible != null && visible.element != null) {
                safeClick(
                        visible.element,
                        "runtime category tab " + targetIndex + "/" + total + " - " + visible.label
                );
                sleep(250L);
                return;
            }
        }

        throw new AssertionError("Unable to reveal runtime category tab " + targetIndex + " of " + total);
    }

    private RuntimeTab findVisibleRuntimeTab(int targetIndex, int total) {
        for (RuntimeTab tab : visibleRuntimeTabs()) {
            if (tab.index == targetIndex && tab.total == total) {
                return tab;
            }
        }
        return null;
    }

    private List<RuntimeTab> visibleRuntimeTabs() {
        List<RuntimeTab> result = new ArrayList<>();
        Rectangle rail = findCategoryRailRect();
        Dimension size = driver.manage().window().getSize();
        int topSectionBottom = rail == null ? topSectionBottomY() : -1;

        for (WebElement element : safeFindElements(VIEW)) {
            if (!isDisplayed(element)) {
                continue;
            }

            String desc = safeAttribute(element, "content-desc");
            if (desc.isEmpty()) {
                continue;
            }

            Matcher matcher = TAB_PATTERN.matcher(desc.trim());
            if (!matcher.matches()) {
                continue;
            }

            String label = matcher.group(1).trim();

            int index = parseInt(matcher.group(2), -1);
            int total = parseInt(matcher.group(3), -1);
            if (index <= 0 || total <= 0 || index > total) {
                continue;
            }

            Rectangle rect = safeRect(element);
            if (rect == null) {
                continue;
            }
            int cy = rect.getY() + (rect.getHeight() / 2);

            if (rail != null) {
                int tolerance = Math.max(12, rail.getHeight() / 2);
                int railTop = rail.getY() - tolerance;
                int railBottom = rail.getY() + rail.getHeight() + tolerance;
                if (cy < railTop || cy > railBottom) {
                    continue;
                }
            } else {
                // Future-build fallback if Flutter stops exposing HorizontalScrollView.
                // Only use geometry when the top-level row itself is semantically
                // discoverable; otherwise we cannot safely distinguish category tabs
                // from the top TabBar and deliberately return no category candidates.
                if (topSectionBottom <= 0
                        || cy <= topSectionBottom
                        || cy >= (int) (size.getHeight() * 0.52)) {
                    continue;
                }
            }

            result.add(new RuntimeTab(label, index, total, element));
        }

        result.sort(Comparator.comparingInt(tab -> tab.index));
        return result;
    }

    // =========================================================
    // Content resolution + scroll validation
    // =========================================================

    private ContentState waitForSectionResolved(String context) {
        long endAt = System.currentTimeMillis() + CONTENT_WAIT_MS;
        ContentState last = null;
        int stableCount = 0;

        while (System.currentTimeMillis() < endAt) {
            assertDriverAlive("while waiting for " + context + " content");
            ContentState current = captureContentState();

            if (current.isResolved()) {
                if (last != null && current.fingerprint.equals(last.fingerprint)) {
                    stableCount++;
                } else {
                    stableCount = 0;
                }

                // Require two matching polls so we do not validate a half-rendered feed.
                if (stableCount >= 1 || current.explicitEmptyState) {
                    return current;
                }
            }

            last = current;
            sleep(POLL_MS);
        }

        throw new AssertionError(
                context + " did not resolve from loading skeletons within " + (CONTENT_WAIT_MS / 1000)
                        + " seconds. visible=" + collectVisibleValues()
        );
    }

    private ContentState captureContentState() {
        Dimension size = driver.manage().window().getSize();
        int minY = (int) (size.getHeight() * 0.30);
        int maxY = (int) (size.getHeight() * 0.89);

        Set<String> values = new LinkedHashSet<>();
        boolean explicitEmpty = false;

        // Flutter article/report cards are exposed as a mix of android.view.View,
        // android.widget.ImageView and occasionally Button semantics. Scan only these
        // targeted classes instead of the full XML tree.
        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }

            Rectangle rect = safeRect(element);
            if (rect == null) {
                continue;
            }

            int cy = rect.getY() + (rect.getHeight() / 2);
            if (cy < minY || cy > maxY) {
                continue;
            }

            String value = semanticValue(element);
            if (value.isEmpty() || isKnownChromeValue(value) || TAB_PATTERN.matcher(value).matches()) {
                continue;
            }

            String lower = value.toLowerCase(Locale.ENGLISH);
            if (containsAny(lower, EMPTY_STATE_MARKERS)) {
                explicitEmpty = true;
            }

            // Live article/report semantics normally expose meaningful text. Tiny one-character
            // semantics are usually decoration and are deliberately ignored.
            if (value.length() >= 3) {
                values.add(normalizeWhitespace(value));
            }
        }

        String fingerprint = String.join(" || ", values);
        return new ContentState(values, explicitEmpty, fingerprint);
    }

    private void validateVerticalScrollProgress(String context) {
        ContentState before = captureContentState();
        if (!before.hasLiveContent()) {
            return;
        }

        ReportLogger.step("Checking bounded vertical-scroll stability for " + context);

        boolean changed = false;
        for (int attempt = 1; attempt <= 3; attempt++) {
            swipeVertical(true);
            sleep(450L);
            ContentState after = captureContentState();

            if (after.hasLiveContent() && !after.fingerprint.equals(before.fingerprint)) {
                changed = true;
                ReportLogger.pass(context + " content advanced after vertical swipe attempt " + attempt);
                break;
            }
        }

        if (!changed) {
            // A short feed may legitimately fit on one screen. This is not a locator failure.
            ReportLogger.pass(
                    context + " content remained stable after bounded swipes; feed is likely at a boundary/short list"
            );
        }

        restoreFeedTowardTop();
    }

    private void restoreFeedTowardTop() {
        // Do not stop merely because a sticky category rail is visible. The previous
        // implementation could leave the next category at an old pagination depth.
        // Instead, keep scrolling toward the top until a swipe no longer changes the
        // semantic feed fingerprint. The loop is bounded for short feeds/Reports.
        String previous = captureContentState().fingerprint;
        int stableAtTop = 0;

        for (int attempt = 0; attempt < 7; attempt++) {
            swipeVertical(false);
            sleep(260L);

            String current = captureContentState().fingerprint;
            if (current.equals(previous)) {
                stableAtTop++;
            } else {
                stableAtTop = 0;
            }

            if (stableAtTop >= 1) {
                return;
            }
            previous = current;
        }
    }

    private void logContentState(String context, ContentState state) {
        if (state.explicitEmptyState) {
            ReportLogger.pass(context + " resolved to explicit empty-state semantics: " + state.values);
        } else {
            ReportLogger.pass(
                    context + " resolved with " + state.values.size()
                            + " live semantic content values; sample=" + firstValues(state.values, 3)
            );
        }
    }

    // =========================================================
    // Horizontal and vertical W3C scrolling
    // =========================================================

    private void resetCategoryRailToStart() {
        for (int attempt = 0; attempt < 10; attempt++) {
            List<RuntimeTab> tabs = visibleRuntimeTabs();
            if (tabs.stream().anyMatch(tab -> tab.index == 1)) {
                return;
            }
            swipeCategoryRail(false);
            sleep(220L);
        }
    }

    private void swipeCategoryRail(boolean toLaterTabs) {
        swipeCategoryRail(toLaterTabs, false);
    }

    private void swipeCategoryRail(boolean toLaterTabs, boolean smallNudge) {
        Rectangle rail = findCategoryRailRect();
        Dimension size = driver.manage().window().getSize();

        int y;
        int startX;
        int endX;

        if (rail != null) {
            y = rail.getY() + (rail.getHeight() / 2);
            int left = rail.getX();
            int right = rail.getX() + rail.getWidth();
            double from = smallNudge ? 0.68 : 0.82;
            double to = smallNudge ? 0.38 : 0.20;
            startX = left + (int) (rail.getWidth() * from);
            endX = left + (int) (rail.getWidth() * to);
            if (!toLaterTabs) {
                int temp = startX;
                startX = endX;
                endX = temp;
            }
        } else {
            // Ratio-based fallback derived from the actual viewport. Prefer the live
            // category-chip Y position if semantics are available; this avoids assuming
            // a fixed screen density/header height.
            List<RuntimeTab> visibleTabs = visibleRuntimeTabs();
            if (!visibleTabs.isEmpty()) {
                List<Integer> centers = new ArrayList<>();
                for (RuntimeTab tab : visibleTabs) {
                    if (tab.element != null) {
                        centers.add(centerY(tab.element));
                    }
                }
                Collections.sort(centers);
                y = centers.isEmpty()
                        ? (int) (size.getHeight() * 0.31)
                        : centers.get(centers.size() / 2);
            } else {
                y = (int) (size.getHeight() * 0.31);
            }
            startX = (int) (size.getWidth() * (smallNudge ? 0.68 : 0.82));
            endX = (int) (size.getWidth() * (smallNudge ? 0.38 : 0.20));
            if (!toLaterTabs) {
                int temp = startX;
                startX = endX;
                endX = temp;
            }
        }

        performSwipe(startX, y, endX, y, 360L);
    }

    private Rectangle findCategoryRailRect() {
        Dimension size = driver.manage().window().getSize();
        int maxY = (int) (size.getHeight() * 0.45);

        return safeFindElements(HORIZONTAL_SCROLL_VIEW).stream()
                .filter(this::isDisplayed)
                .map(this::safeRect)
                .filter(r -> r != null)
                .filter(r -> r.getY() > 0 && r.getY() < maxY)
                .filter(r -> r.getWidth() > (size.getWidth() * 0.50))
                .max(Comparator.comparingInt(Rectangle::getWidth))
                .orElse(null);
    }

    /**
     * @param downFeed true = finger moves up, revealing later/lower content.
     */
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
    // Hub tile / section helpers
    // =========================================================

    private WebElement findHubTileWithBoundedScroll() {
        WebElement direct = firstDisplayed(COVERAGE_INSIGHTS_TILE);
        if (direct != null) {
            ReportLogger.debug("Coverage and insights Hub tile is already visible - no Hub scroll required");
            return direct;
        }

        // Search from the current Hub position first. This preserves the scroll
        // position restored by Android Back and avoids always resetting Hub to top.
        String previous = hubOpenTileFingerprint();
        int noProgress = 0;

        for (int i = 0; i < 7; i++) {
            swipeVertical(true);
            sleep(260L);

            direct = firstDisplayed(COVERAGE_INSIGHTS_TILE);
            if (direct != null) {
                ReportLogger.step("Coverage and insights Hub tile found after downward Hub scroll attempt " + (i + 1));
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

        // If Hub was already below the Stocks row, recover upward in a bounded way.
        previous = hubOpenTileFingerprint();
        noProgress = 0;
        for (int i = 0; i < 6; i++) {
            swipeVertical(false);
            sleep(240L);

            direct = firstDisplayed(COVERAGE_INSIGHTS_TILE);
            if (direct != null) {
                ReportLogger.step("Coverage and insights Hub tile found after upward Hub recovery attempt " + (i + 1));
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

    private void openTopSection(String name) {
        WebElement allStories = waitForVisible(ALL_STORIES, PAGE_READY_WAIT_MS);
        boolean searchWasVisible = isCoverageSearchVisible();
        String categoryFingerprintBefore = runtimeCategoryFingerprint();

        // Flutter can briefly rebuild the semantics tree immediately after changing
        // top-level tabs. Do not fall back to coordinates just because one lookup
        // happened during that short rebuild window. Retry semantic discovery first.
        WebElement semanticTab = findTopSectionElementWithRetry(name, 5_000L);
        if (semanticTab != null) {
            String semantic = semanticValue(semanticTab);
            safeClick(semanticTab, name + " top tab semantic element");
            ReportLogger.debug(
                    "Tapped " + name + " using live semantic: " + semantic
            );
        } else {
            // Last-resort fallback only. Normal current builds expose '<label>, tab X of Y'.
            tapTopSectionRelativeToAllStories(allStories, name);
            ReportLogger.debug(
                    "Tapped " + name + " using All Stories-relative geometry only after bounded semantic retries returned no valid top-tab candidate"
            );
        }

        waitForTopSectionState(
                name,
                PAGE_READY_WAIT_MS,
                searchWasVisible,
                categoryFingerprintBefore
        );
        ReportLogger.step(name + " top section selected and section signature confirmed");
    }

    private void waitForAllStoriesReady() {
        WebElement allStories = waitForVisible(ALL_STORIES, PAGE_READY_WAIT_MS);

        // The current build's top-level labels are visually rendered but not guaranteed
        // to be independent accessibility nodes. Do not fail page opening because of
        // missing semantics. Instead verify that the header container plus one valid
        // section body (category rail/search/live content) has appeared.
        long endAt = System.currentTimeMillis() + PAGE_READY_WAIT_MS;
        while (System.currentTimeMillis() < endAt) {
            if (isCoverageSearchVisible()
                    || findCategoryRailRect() != null
                    || captureContentState().isResolved()) {
                logTopSectionLocatorAvailability(allStories);
                return;
            }
            sleep(POLL_MS);
        }

        throw new AssertionError(
                "All Stories header appeared but module body did not become ready. visible="
                        + collectVisibleValues()
        );
    }

    private boolean isAllStoriesOpen() {
        return isVisible(ALL_STORIES);
    }

    private void logTopSectionLocatorAvailability(WebElement allStories) {
        List<String> resolved = new ArrayList<>();
        for (String name : new String[]{TOP_COVERAGE, TOP_INSIGHTS, TOP_REPORTS}) {
            WebElement element = findTopSectionElement(name);
            if (element != null) {
                resolved.add(name + "=" + semanticValue(element));
            }
        }

        if (resolved.size() == 3) {
            ReportLogger.info("Top-level tabs expose live semantics: " + resolved);
        } else {
            Rectangle rect = safeRect(allStories);
            ReportLogger.info(
                    "Top-level tabs are not independently exposed by UiAutomator2 on this build; "
                            + "using All Stories-relative tap fallback. headerRect=" + rect
                            + " | semanticTabsFound=" + resolved
            );
        }
    }

    private void tapTopSectionRelativeToAllStories(WebElement allStories, String name) {
        Rectangle rect = safeRect(allStories);
        if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
            throw new AssertionError(
                    "Cannot derive top-tab gesture target because All Stories bounds are unavailable"
            );
        }

        int position;
        if (TOP_COVERAGE.equalsIgnoreCase(name)) {
            position = 0;
        } else if (TOP_INSIGHTS.equalsIgnoreCase(name)) {
            position = 1;
        } else if (TOP_REPORTS.equalsIgnoreCase(name)) {
            position = 2;
        } else {
            throw new IllegalArgumentException("Unsupported top section: " + name);
        }

        double xRatio = TOP_TAB_FIRST_X_RATIO + (TOP_TAB_X_STEP_RATIO * position);
        int x = rect.getX() + (int) Math.round(rect.getWidth() * xRatio);

        // When a category rail is present, its Y coordinate gives an additional live
        // anchor. Tap just above it in the top-tab band. When Reports is already open
        // there is no rail, so fall back to the All Stories-relative Y ratio.
        Rectangle rail = findCategoryRailRect();
        int y;
        if (rail != null && rail.getY() > rect.getY()) {
            int derived = rail.getY() - Math.max(36, (int) Math.round(rail.getHeight() * 1.20));
            int min = rect.getY() + (int) Math.round(rect.getHeight() * 0.30);
            int max = rect.getY() + (int) Math.round(rect.getHeight() * 0.62);
            y = Math.max(min, Math.min(max, derived));
        } else {
            y = rect.getY() + (int) Math.round(rect.getHeight() * TOP_TAB_Y_RATIO_IN_ALL_STORIES);
        }

        performTap(x, y);
        sleep(450L);
    }

    private void waitForTopSectionState(
            String name,
            long timeoutMs,
            boolean searchWasVisible,
            String categoryFingerprintBefore
    ) {
        long endAt = System.currentTimeMillis() + timeoutMs;
        int consecutiveMatches = 0;

        while (System.currentTimeMillis() < endAt) {
            boolean searchVisible = isCoverageSearchVisible();
            boolean categoryRailVisible = findCategoryRailRect() != null;
            String categoryFingerprintNow = runtimeCategoryFingerprint();
            boolean matches = false;

            if (TOP_COVERAGE.equalsIgnoreCase(name)) {
                matches = searchVisible && categoryRailVisible;
            } else if (TOP_INSIGHTS.equalsIgnoreCase(name)) {
                boolean categoryChangedFromCoverage = !searchWasVisible
                        || categoryFingerprintBefore.isEmpty()
                        || !categoryFingerprintBefore.equals(categoryFingerprintNow);

                matches = !searchVisible
                        && categoryRailVisible
                        && !categoryFingerprintNow.isEmpty()
                        && categoryChangedFromCoverage;
            } else if (TOP_REPORTS.equalsIgnoreCase(name)) {
                matches = !searchVisible && !categoryRailVisible;
            }

            if (matches) {
                consecutiveMatches++;
                if (consecutiveMatches >= 2) {
                    return;
                }
            } else {
                consecutiveMatches = 0;
            }

            sleep(POLL_MS);
        }

        throw new AssertionError(
                "Top-level section did not reach expected UI signature: " + name
                        + " | searchVisible=" + isCoverageSearchVisible()
                        + " | categoryRailVisible=" + (findCategoryRailRect() != null)
                        + " | categoryFingerprintBefore=" + categoryFingerprintBefore
                        + " | categoryFingerprintNow=" + runtimeCategoryFingerprint()
                        + " | visible=" + collectVisibleValues()
        );
    }

    private String runtimeCategoryFingerprint() {
        List<RuntimeTab> tabs = visibleRuntimeTabs();
        if (tabs.isEmpty()) {
            return "";
        }

        List<String> values = new ArrayList<>();
        for (RuntimeTab tab : tabs) {
            values.add(tab.index + "/" + tab.total + ":" + normalizeWhitespace(tab.label));
        }
        return String.join("|", values);
    }

    private List<RuntimeTab> waitForRuntimeCategoryTabs(long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endAt) {
            List<RuntimeTab> tabs = visibleRuntimeTabs();
            if (!tabs.isEmpty()) {
                return tabs;
            }
            sleep(POLL_MS);
        }
        return Collections.emptyList();
    }

    /**
     * Resolves a top-level section without depending on the current body layout.
     *
     * Important Flutter detail:
     * - Top tabs are exposed as values such as 'Reports, tab 3 of 3'.
     * - Coverage also has a runtime category named Reports, e.g. 'Reports, tab 11 of 11'.
     *
     * Therefore the resolver must identify the semantic tab itself, not infer it from
     * the presence/absence of the category rail. This is especially important while
     * Reports is open because Reports has no category rail.
     */
    private WebElement findTopSectionElement(String name) {
        List<WebElement> candidates = safeFindElements(
                AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"" + name + "\")"
                )
        );

        List<TopTabCandidate> semanticTabs = new ArrayList<>();

        for (WebElement element : candidates) {
            if (!isDisplayed(element)) {
                continue;
            }

            String value = normalizeWhitespace(semanticValue(element));
            Matcher matcher = TAB_PATTERN.matcher(value);
            if (!matcher.matches()) {
                continue;
            }

            String label = normalizeWhitespace(matcher.group(1));
            if (!label.equalsIgnoreCase(name)) {
                continue;
            }

            try {
                int index = Integer.parseInt(matcher.group(2));
                int total = Integer.parseInt(matcher.group(3));
                if (index <= 0 || total <= 0 || index > total) {
                    continue;
                }

                semanticTabs.add(new TopTabCandidate(element, index, total));
            } catch (NumberFormatException ignored) {
                // Ignore malformed transient Flutter semantics and keep searching.
            }
        }

        if (!semanticTabs.isEmpty()) {
            // The top-level rail has fewer tabs than inner category rails. Choosing
            // the smallest valid total cleanly distinguishes:
            //   Reports, tab 3 of 3   -> top-level Reports
            //   Reports, tab 11 of 11 -> Coverage category Reports
            // without hardcoding category names/counts or relying on Y coordinates.
            semanticTabs.sort(Comparator
                    .comparingInt((TopTabCandidate candidate) -> candidate.total)
                    .thenComparingInt(candidate -> centerY(candidate.element)));
            return semanticTabs.get(0).element;
        }

        // Older builds may expose a standalone accessibility label. Accept it only
        // when it is in the top-tab/header band so a similarly named feed item cannot win.
        WebElement exact = firstDisplayed(AppiumBy.accessibilityId(name));
        if (exact != null && isAboveCategoryRail(exact)) {
            return exact;
        }

        return null;
    }

    private WebElement findTopSectionElementWithRetry(String name, long timeoutMs) {
        long endAt = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        WebElement element;

        do {
            element = findTopSectionElement(name);
            if (element != null) {
                return element;
            }
            sleep(POLL_MS);
        } while (System.currentTimeMillis() < endAt);

        return null;
    }

    private static final class TopTabCandidate {
        final WebElement element;
        final int index;
        final int total;

        TopTabCandidate(WebElement element, int index, int total) {
            this.element = element;
            this.index = index;
            this.total = total;
        }
    }


    private boolean isAboveCategoryRail(WebElement element) {
        Rectangle rect = safeRect(element);
        if (rect == null) {
            return false;
        }

        int cy = rect.getY() + (rect.getHeight() / 2);
        Rectangle rail = findCategoryRailRect();
        if (rail != null) {
            return cy < rail.getY();
        }

        WebElement allStories = firstDisplayed(ALL_STORIES);
        Rectangle header = safeRect(allStories);
        if (header == null) {
            return true;
        }

        int maxTopTabY = header.getY() + (int) Math.round(header.getHeight() * 0.70);
        return cy >= header.getY() && cy <= maxTopTabY;
    }

    private int topSectionBottomY() {
        int max = -1;
        String[] names = new String[]{TOP_COVERAGE, TOP_INSIGHTS, TOP_REPORTS};
        for (String name : names) {
            WebElement element = findTopSectionElement(name);
            Rectangle rect = safeRect(element);
            if (rect != null) {
                max = Math.max(max, rect.getY() + rect.getHeight());
            }
        }
        return max;
    }

    private boolean isTopSectionLabel(String label) {
        if (label == null) {
            return false;
        }
        String normalized = normalizeWhitespace(label);
        return normalized.equalsIgnoreCase(TOP_COVERAGE)
                || normalized.equalsIgnoreCase(TOP_INSIGHTS)
                || normalized.equalsIgnoreCase(TOP_REPORTS);
    }

    private boolean isTopSectionSemantic(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeWhitespace(value);
        if (isTopSectionLabel(normalized)) {
            return true;
        }
        Matcher matcher = TAB_PATTERN.matcher(normalized);
        return matcher.matches() && isTopSectionLabel(matcher.group(1).trim());
    }

    // =========================================================
    // Coverage search / Hub-state / semantic-content helpers
    // =========================================================

    private WebElement waitForCoverageSearchField(long timeoutMs) {
        long endAt = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endAt) {
            WebElement field = findCoverageSearchField();
            if (field != null) {
                return field;
            }
            sleep(POLL_MS);
        }

        throw new AssertionError(
                "Timed out waiting for Coverage search android.widget.EditText. "
                        + "visibleEditTexts=" + visibleEditTextDiagnostics()
        );
    }

    private boolean isCoverageSearchVisible() {
        return findCoverageSearchField() != null;
    }

    private WebElement findCoverageSearchField() {
        Dimension size = driver.manage().window().getSize();
        Rectangle rail = findCategoryRailRect();
        List<WebElement> candidates = new ArrayList<>();

        for (WebElement element : safeFindElements(EDIT_TEXT)) {
            if (!isDisplayed(element)) {
                continue;
            }

            Rectangle rect = safeRect(element);
            if (rect == null || rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                continue;
            }

            // The search control is a wide field in the upper half of All Stories.
            if (rect.getWidth() < (int) (size.getWidth() * 0.55)
                    || rect.getY() > (int) (size.getHeight() * 0.55)) {
                continue;
            }

            if (rail != null) {
                int railBottom = rail.getY() + rail.getHeight();
                int allowedGap = Math.max(
                        rail.getHeight() * 4,
                        (int) (size.getHeight() * 0.12)
                );

                // Inspector evidence: search field sits directly below the category rail.
                if (rect.getY() < railBottom - Math.max(8, rail.getHeight() / 3)
                        || rect.getY() > railBottom + allowedGap) {
                    continue;
                }
            }

            candidates.add(element);
        }

        return candidates.stream()
                .min(Comparator.comparingInt(this::centerY))
                .orElse(null);
    }

    private void validateCoverageSearchElement(WebElement search, String label) {
        if (search == null || !isDisplayed(search)) {
            throw new AssertionError(label + " is not displayed");
        }

        try {
            if (!search.isEnabled()) {
                throw new AssertionError(label + " is displayed but disabled");
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(label + " enabled-state could not be read: " + clean(e.getMessage()), e);
        }

        Rectangle rect = safeRect(search);
        Dimension size = driver.manage().window().getSize();
        if (rect == null || rect.getWidth() < (int) (size.getWidth() * 0.55)) {
            throw new AssertionError(label + " has invalid/unexpected bounds: " + rect);
        }
    }

    private String visibleEditTextDiagnostics() {
        List<String> values = new ArrayList<>();
        for (WebElement element : safeFindElements(EDIT_TEXT)) {
            if (!isDisplayed(element)) {
                continue;
            }
            Rectangle rect = safeRect(element);
            values.add("{bounds=" + rect + ", enabled=" + safeEnabled(element) + "}");
        }
        return values.toString();
    }

    private boolean safeEnabled(WebElement element) {
        try {
            return element != null && element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHubScreenReady() {
        if (isAllStoriesOpen()) {
            return false;
        }

        if (firstDisplayed(COVERAGE_INSIGHTS_TILE) != null) {
            return true;
        }

        int visibleOpenTiles = 0;
        for (WebElement element : safeFindElements(HUB_OPEN_BUTTONS)) {
            if (isDisplayed(element)) {
                visibleOpenTiles++;
                if (visibleOpenTiles >= 2) {
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
            assertDriverAlive("while waiting for Hub screen");
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
            String value = semanticValue(element);
            if (!value.isEmpty()) {
                values.add(normalizeWhitespace(value));
            }
        }
        return String.join("|", values);
    }

    private List<WebElement> contentSemanticElements() {
        List<WebElement> elements = new ArrayList<>();
        elements.addAll(safeFindElements(VIEW));
        elements.addAll(safeFindElements(IMAGE_VIEW));
        elements.addAll(safeFindElements(BUTTON));
        return elements;
    }

    // =========================================================
    // Generic safe helpers
    // =========================================================

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

    private void safeClick(WebElement element, String label) {
        try {
            element.click();
        } catch (Exception first) {
            Rectangle rect = safeRect(element);
            if (rect == null) {
                throw new AssertionError("Unable to click " + label + ": " + clean(first.getMessage()), first);
            }
            int x = rect.getX() + (rect.getWidth() / 2);
            int y = rect.getY() + (rect.getHeight() / 2);
            performTap(x, y);
        }
        ReportLogger.debug("Tapped " + label);
    }

    private void performTap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "tapFinger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(70L)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    private Rectangle safeRect(WebElement element) {
        try {
            return element.getRect();
        } catch (Exception e) {
            return null;
        }
    }

    private int centerY(WebElement element) {
        Rectangle rect = safeRect(element);
        return rect == null ? -1 : rect.getY() + (rect.getHeight() / 2);
    }

    private String semanticValue(WebElement element) {
        String desc = safeAttribute(element, "content-desc");
        if (!desc.isEmpty()) {
            return desc.trim();
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

    private boolean isKnownChromeValue(String value) {
        String normalized = normalizeWhitespace(value).toLowerCase(Locale.ENGLISH);
        return normalized.equals("all stories")
                || isTopSectionSemantic(value)
                || normalized.equals("funds")
                || normalized.equals("stocks")
                || normalized.equals("portfolio")
                || normalized.equals("hub")
                || normalized.startsWith("coverage search");
    }

    private String collectVisibleValues() {
        Set<String> values = new LinkedHashSet<>();
        for (WebElement element : contentSemanticElements()) {
            if (!isDisplayed(element)) {
                continue;
            }
            String value = semanticValue(element);
            if (!value.isEmpty()) {
                values.add(normalizeWhitespace(value));
            }
            if (values.size() >= 30) {
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

    private boolean containsAny(String value, String[] tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
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
    // Internal immutable state objects
    // =========================================================

    private static final class RuntimeTab {
        private final String label;
        private final int index;
        private final int total;
        private final WebElement element;

        private RuntimeTab(String label, int index, int total, WebElement element) {
            this.label = label;
            this.index = index;
            this.total = total;
            this.element = element;
        }

        @Override
        public String toString() {
            return index + "/" + total + " " + label;
        }
    }

    private static final class ContentState {
        private final Set<String> values;
        private final boolean explicitEmptyState;
        private final String fingerprint;

        private ContentState(Set<String> values, boolean explicitEmptyState, String fingerprint) {
            this.values = values;
            this.explicitEmptyState = explicitEmptyState;
            this.fingerprint = fingerprint == null ? "" : fingerprint;
        }

        private boolean isResolved() {
            return explicitEmptyState || hasLiveContent();
        }

        private boolean hasLiveContent() {
            return !explicitEmptyState && values != null && !values.isEmpty();
        }
    }
}