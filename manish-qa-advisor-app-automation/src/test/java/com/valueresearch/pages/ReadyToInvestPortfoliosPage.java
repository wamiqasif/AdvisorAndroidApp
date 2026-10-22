package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.ExecutionProfile;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legacy class name retained for existing RTI suite references.
 * Current product: Hub > Stocks > Stock Analyst's Choice.
 * Authentication and Hub navigation reuse the installed, working SAC page/AuthHelper.
 * Stock names, row values, top tabs and secondary filters remain runtime data.
 */
public class ReadyToInvestPortfoliosPage {
    public static final String BUILD_VERSION = "READY_TO_INVEST_SAC_V8_FILTER_SNAPSHOTS_20261022";

    private static final By TITLE = AppiumBy.accessibilityId("Analyst's Choice");
    private static final By STOCKS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Stocks\")");
    private static final By BACK = AppiumBy.accessibilityId("Go back");
    private static final By SCROLL = AppiumBy.className("android.widget.ScrollView");
    private static final By HORIZONTAL = AppiumBy.className("android.widget.HorizontalScrollView");
    private static final By VALUES = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").descriptionMatches(\"(?s).+\")");
    private static final By DATE_CELLS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").descriptionMatches(\"[0-9]{2}-[A-Za-z]{3}-[0-9]{2}\")");
    private static final By HEADERS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").descriptionMatches(\"(?s).+\")");
    private static final By TABS = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"^.+, tab [0-9]+ of [0-9]+$\")");
    private static final By NAVIGATION_BAR = By.id("android:id/navigationBarBackground");
    private static final By PLANNER_HEADLINE = AppiumBy.accessibilityId("Need a hand deciding?");
    private static final By PLANNER_SUBTITLE = AppiumBy.accessibilityId(
            "Our Investment Planner picks the stocks and split for you");
    private static final By PLANNER_CTA = AppiumBy.accessibilityId("Take me there");
    private static final By INVESTMENT_PLANNER = AppiumBy.accessibilityId("Investment Planner");
    private static final String LAST_COLUMN = "Vs BSE 500 TRI Return";
    private static final Pattern TAB = Pattern.compile("^(.+?),\\s*tab\\s+(\\d+)\\s+of\\s+(\\d+)$");
    private static final Pattern DATE = Pattern.compile("[0-9]{2}-[A-Za-z]{3}-[0-9]{2}");
    private static final Pattern NUMBER = Pattern.compile("[-+₹%0-9,. ]+");
    private static final String[] EMPTY = {"no data available", "no stocks available", "no recommendations",
            "no recommendation", "nothing to show", "underlying data is unavailable",
            "underlying data unavailable", "no result found", "no results found"};

    private final AndroidDriver driver;
    private final StockAnalystsChoiceChangebyProPage navigation;
    private boolean prepared;
    private final int maxHorizontal = positiveInt("stockAnalystsChoiceMaxHorizontalSwipes", 20);
    private final int maxVertical = positiveInt("stockAnalystsChoiceMaxListScrolls", 40);

    public ReadyToInvestPortfoliosPage(AndroidDriver driver) {
        this(driver, AuthHelper.configuredLoginMethod());
    }

    public ReadyToInvestPortfoliosPage(AndroidDriver driver, AuthHelper.LoginMethod method) {
        this.driver = Objects.requireNonNull(driver, "An active AndroidDriver is required");
        this.navigation = new StockAnalystsChoiceChangebyProPage(driver, method);
    }

    /** Called from common setup. A verified cache belongs to the current driver/session only. */
    public void prepareActiveAccount(boolean sameVerifiedSession) {
        prepared = false;
        if (!sameVerifiedSession || !navigation.reuseVerifiedAccountSession(
                StockAnalystsChoiceChangebyProPage.Persona.ACTIVE_SUBSCRIBER)) {
            navigation.ensureRequiredAccount(StockAnalystsChoiceChangebyProPage.Persona.ACTIVE_SUBSCRIBER);
        }
        navigation.assertHubVisible();
        prepared = true;
    }

    public void openFromHub() {
        if (!prepared) prepareActiveAccount(false); // Preserve standalone page-object callers.
        if (visible(TITLE) && visible(BACK)) return;
        navigation.openFromHub(); // Exact current tile: Open Stock Analyst's Choice screen.
        waitFor(ExecutionProfile.defaultWait()).withMessage("Analyst's Choice did not open")
                .until(d -> visible(TITLE) && visible(BACK) && complete(discoverTabsOnce()));
    }

    public void recoverToHubSafely() {
        navigation.returnToHub();
        navigation.assertHubVisible(); // Cleanup failure must remain visible to TestNG.
    }

    public void restoreTimingSettings() { navigation.restoreTimingSettings(); }

    public void validateLandingPageDynamically() {
        openFromHub();
        List<RuntimeTab> tabs = discoverTabs();
        RuntimeTab selected = selectedTab(tabs);
        TableState state = waitForTable(selected);
        ReportLogger.pass("Analyst's Choice landing | tabs=" + tabLabels(tabs)
                + " | filters=" + visibleFilters(selected).keySet() + " | " + state.describe());
    }

    public void validateEveryTopSectionAndSecondaryFilterDynamically() {
        openFromHub();
        for (RuntimeTab tab : discoverTabs()) {
            if (!selected(tab.locator())) tap(tab.locator());
            waitFor(ExecutionProfile.defaultWait()).withMessage("Requested top tab did not become selected: " + tab.label)
                    .until(d -> selected(tab.locator()));
            waitForFilterTable(tab);
            validateFilters(tab);
        }
    }

    /** Prove the named final column and return to the starting column on every runtime tab. */
    public void validateHorizontalTableScroll() {
        openFromHub();
        for (RuntimeTab tab : discoverTabs()) {
            TableState state = switchTab(tab);
            if (state.empty.length() > 0) {
                throw new AssertionError(tab.label + " is empty; horizontal movement cannot be verified");
            }
            Map<String, Rectangle> initial = settledHeaders(tab);
            String first = firstFullHeader(initial);
            if (first == null) {
                throw new AssertionError(tab.label + " has no visible starting table column");
            }
            if (fullColumn(initial.get(LAST_COLUMN))) {
                // A retained right-edge offset still needs a real round trip, not a zero-movement PASS.
                String dateColumn = tab.label.equalsIgnoreCase("Sell") ? "Exit Date" : "Reco Date";
                panTableTo(tab, dateColumn, false);
                panTableTo(tab, LAST_COLUMN, true);
                ReportLogger.pass(tab.label + " table moved from its retained right edge to '" + dateColumn + "' and back");
                continue;
            }
            panTableTo(tab, LAST_COLUMN, true);
            panTableTo(tab, first, false);
            ReportLogger.pass(tab.label + " table reached '" + LAST_COLUMN + "' and returned to '" + first + "'");
        }
    }

    /** Legacy method name retained; the current widget opens Investment Planner. */
    public void validateVerticalScrollAndPortfolioPlannerCta() {
        openFromHub();
        List<RuntimeTab> tabs = discoverTabs();
        RuntimeTab tab = tabs.get(tabs.size() - 1);
        switchTab(tab);
        ReportLogger.step("RTI_004: " + tab.label + " - scrolling to the Investment Planner widget");
        boolean found = false;
        for (int swipe = 0; swipe <= maxVertical; swipe++) {
            assertSelected(tab);
            if (!plannerWidgetBounds().isEmpty()) {
                ReportLogger.step("RTI_004: Widget is fully visible after " + swipe + " list scrolls");
                found = true;
                break;
            }
            if (swipe == maxVertical) break;
            Rectangle viewport = moduleViewport();
            int x = viewport.getX() + viewport.getWidth() / 2;
            int from = viewport.getY() + (int) (viewport.getHeight() * 0.82);
            int to = viewport.getY() + (int) (viewport.getHeight() * 0.22);
            performSwipe(x, from, x, to);
            settleRows(tab);
        }
        if (!found) throw new AssertionError("Current Investment Planner widget/CTA was not fully visible within "
                + maxVertical + " list scrolls");
        waitForPlannerCtaToSettle(tab);
        ReportLogger.step("RTI_004: Opening Investment Planner using Take me there");
        tap(PLANNER_CTA);
        waitFor(ExecutionProfile.longWait()).withMessage("Take me there did not open Investment Planner")
                .until(d -> visible(INVESTMENT_PLANNER) && !visible(TITLE));
        returnFromInvestmentPlanner(tab);
        ReportLogger.pass("Current widget copy and Investment Planner navigation verified");
    }

    private void returnFromInvestmentPlanner(RuntimeTab tab) {
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
            waitFor(ExecutionProfile.shortWait()).withMessage("Investment Planner keyboard did not close")
                    .until(d -> !driver.isKeyboardShown());
        }
        ReportLogger.step("RTI_004: Returning from Investment Planner to Analyst's Choice");
        boolean usedAppBack = visible(BACK);
        if (usedAppBack) tap(BACK); else driver.navigate().back();
        try {
            waitFor(ExecutionProfile.shortWait()).withMessage("Waiting for the destination after Investment Planner Back")
                    .until(d -> plannerReturnDestination() != null);
        } catch (TimeoutException firstBackWait) {
            // Use the working SAC fallback only while Planner is still open.
            // Never issue another Back on a partially restored module or on Hub.
            if (usedAppBack && visible(INVESTMENT_PLANNER) && !visible(TITLE)) {
                ReportLogger.step("RTI_004: Investment Planner is still open; using Android Back once");
                driver.navigate().back();
            }
            try {
                waitFor(ExecutionProfile.defaultWait()).withMessage(
                        "Investment Planner Back reached neither Analyst's Choice nor Hub")
                        .until(d -> plannerReturnDestination() != null);
            } catch (TimeoutException returnFailure) {
                returnFailure.addSuppressed(firstBackWait);
                throw returnFailure;
            }
        }
        if ("HUB".equals(plannerReturnDestination())) {
            ReportLogger.step("RTI_004: Planner returned to Hub; reopening Analyst's Choice");
            navigation.openFromHub();
        }
        waitFor(ExecutionProfile.defaultWait()).withMessage("Analyst's Choice did not finish restoring after Planner")
                .until(d -> "MODULE".equals(plannerReturnDestination()));
        if (!selected(tab.locator())) switchTab(tab);
        assertSelected(tab);
    }

    private String plannerReturnDestination() {
        if (visible(TITLE)) {
            return visible(BACK) && !visible(INVESTMENT_PLANNER) && complete(discoverTabsOnce()) ? "MODULE" : null;
        }
        if (visible(INVESTMENT_PLANNER)) return null;
        return navigation.isHubScreenVisible() ? "HUB" : null;
    }

    public void validateTopLevelSwitchingFast() {
        openFromHub();
        List<RuntimeTab> tabs = discoverTabs();
        for (RuntimeTab tab : tabs) switchTab(tab);
        switchTab(tabs.get(0));
        ReportLogger.pass("Every runtime top tab loaded; returned to " + tabs.get(0).label);
    }

    private List<RuntimeTab> discoverTabs() {
        return waitFor(ExecutionProfile.defaultWait()).withMessage("The runtime top-tab group is incomplete")
                .until(d -> {
                    List<RuntimeTab> tabs = discoverTabsOnce();
                    return complete(tabs) ? tabs : null;
                });
    }

    private List<RuntimeTab> discoverTabsOnce() {
        Map<Integer, Map<Integer, RuntimeTab>> groups = new LinkedHashMap<>();
        for (WebElement element : driver.findElements(TABS)) {
            if (!element.isDisplayed()) continue;
            String semantic = element.getAttribute("content-desc");
            Matcher match = TAB.matcher(normalize(semantic));
            if (!match.matches()) continue;
            int index = Integer.parseInt(match.group(2)), total = Integer.parseInt(match.group(3));
            if (index < 1 || total < 2 || index > total) continue;
            Rectangle r = element.getRect();
            Map<Integer, RuntimeTab> group = groups.computeIfAbsent(total, k -> new LinkedHashMap<>());
            RuntimeTab previous = group.get(index);
            if (previous == null || r.getY() < previous.bounds.getY()) {
                group.put(index, new RuntimeTab(index, total, match.group(1), semantic, r));
            }
        }
        List<RuntimeTab> best = List.of();
        for (Map<Integer, RuntimeTab> group : groups.values()) {
            List<RuntimeTab> current = new ArrayList<>(group.values());
            current.sort(Comparator.comparingInt(t -> t.index));
            if (complete(current) && (!complete(best) || current.get(0).bounds.getY() < best.get(0).bounds.getY())) best = current;
        }
        return best;
    }

    private boolean complete(List<RuntimeTab> tabs) {
        if (tabs.isEmpty() || tabs.size() != tabs.get(0).total) return false;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).index != i + 1) return false;
        return true;
    }

    private RuntimeTab selectedTab(List<RuntimeTab> tabs) {
        for (RuntimeTab tab : tabs) if (selected(tab.locator())) return tab;
        throw new AssertionError("No runtime recommendation tab is selected");
    }

    private TableState switchTab(RuntimeTab tab) {
        if (!selected(tab.locator())) tap(tab.locator());
        waitFor(ExecutionProfile.defaultWait()).withMessage("Requested top tab did not become selected: " + tab.label)
                .until(d -> selected(tab.locator()));
        return waitForTable(tab);
    }

    private void assertSelected(RuntimeTab tab) {
        if (!selected(tab.locator())) throw new AssertionError("Expected " + tab.label
                + " to remain selected; a gesture changed recommendation tabs instead of table content");
    }

    private Map<String, Filter> visibleFilters(RuntimeTab tab) {
        assertSelected(tab);
        Rectangle stocks = bounds(STOCKS);
        if (stocks == null) return Map.of();
        int top = 0;
        for (RuntimeTab live : discoverTabsOnce()) top = Math.max(top, live.bounds.getY() + live.bounds.getHeight());
        List<Filter> candidates = new ArrayList<>();
        for (WebElement element : driver.findElements(VALUES)) {
            if (!element.isDisplayed() || !element.isEnabled()) continue;
            Rectangle r = element.getRect();
            String semantic = element.getAttribute("content-desc"), label = normalize(semantic);
            int y = r.getY() + r.getHeight() / 2;
            if (y > top && y < stocks.getY() && r.getWidth() > 30 && r.getHeight() > 15
                    && r.getHeight() <= stocks.getHeight() && !label.isEmpty() && !TAB.matcher(label).matches()
                    && !label.equals("Customise") && !label.equals("?") && !label.equals("Analyst's Choice")) {
                candidates.add(new Filter(label, semantic, r));
            }
        }
        candidates.sort(Comparator.comparingInt(c -> c.bounds.getX()));
        Map<String, Filter> result = new LinkedHashMap<>();
        for (Filter filter : candidates) result.putIfAbsent(filter.label, filter);
        return result;
    }

    private void validateFilters(RuntimeTab tab) {
        Map<String, Filter> initial = readFilters(tab);
        if (initial.isEmpty()) {
            ReportLogger.pass(tab.label + " has no secondary filter rail; its live table/empty state was verified");
            return;
        }
        ReportLogger.step("RTI_002: " + tab.label + " - discovering the secondary filter rail");
        panFiltersToBoundary(tab, false);
        Set<String> checked = new LinkedHashSet<>();
        boolean reachedEnd = false;
        for (int swipe = 0; swipe <= maxHorizontal; swipe++) {
            Map<String, Filter> filters = readFilters(tab);
            for (Filter filter : filters.values()) {
                if (!fullyVisible(filter.bounds) || checked.contains(filter.label)) continue;
                ReportLogger.step("RTI_002: " + tab.label + " - validating filter '" + filter.label + "'");
                // Once another filter was confirmed, each newly discovered label
                // needs one tap. Avoid screenshotting a known inactive chip first.
                if (!checked.isEmpty() || !filterSelected(tab, filter)) tapFilter(tab, filter);
                waitFor(ExecutionProfile.shortWait()).withMessage("RTI_002: " + tab.label + " filter '"
                        + filter.label + "' has neither native selection nor a visible active fill")
                        .until(d -> filterSelected(tab, filter));
                waitForFilterTable(tab);
                checked.add(filter.label);
            }
            if (swipe == maxHorizontal) break;
            Map<String, Rectangle> before = filterPositions(tab);
            if (!swipeFilters(tab, true)) { reachedEnd = true; break; }
            if (settledFilterPositions(tab).equals(before)) { reachedEnd = true; break; }
        }
        if (!reachedEnd) throw new AssertionError(tab.label + " secondary filters did not reach their horizontal end");
        if (checked.isEmpty()) throw new AssertionError(tab.label + " exposed filter chips but none were validated");
        panFiltersToBoundary(tab, false);
        ReportLogger.pass(tab.label + " validated runtime secondary filters: " + checked);
    }

    private void panFiltersToBoundary(RuntimeTab tab, boolean later) {
        for (int i = 0; i < maxHorizontal; i++) {
            Map<String, Rectangle> before = filterPositions(tab);
            if (!swipeFilters(tab, later) || settledFilterPositions(tab).equals(before)) return;
        }
        throw new AssertionError(tab.label + " secondary filter rail did not reach its boundary");
    }

    private boolean swipeFilters(RuntimeTab tab, boolean later) {
        Map<String, Filter> filters = readFilters(tab);
        Rectangle viewport = screen();
        Filter anchor = visibleFilterAnchor(filters, viewport);
        if (anchor == null) throw new AssertionError("RTI_002: " + tab.label + " has no visible filter row to scroll");
        int y = anchor.bounds.getY() + anchor.bounds.getHeight() / 2;
        Rectangle rail = null;
        for (WebElement element : driver.findElements(HORIZONTAL)) {
            if (!element.isDisplayed()) continue;
            Rectangle r = element.getRect();
            int left = Math.max(viewport.getX(), r.getX());
            int right = Math.min(viewport.getX() + viewport.getWidth(), r.getX() + r.getWidth());
            int anchorX = anchor.bounds.getX() + anchor.bounds.getWidth() / 2;
            // Flutter can give this parent the height of the entire table. The
            // chip's Y, not the parent's centre, identifies the actual gesture lane.
            if (y >= r.getY() && y < r.getY() + r.getHeight() && anchorX >= left && anchorX < right
                    && right - left > 80 && (rail == null || right - left > rail.getWidth())) {
                rail = rect(left, anchor.bounds.getY(), right - left, anchor.bounds.getHeight());
            }
        }
        if (rail == null) {
            for (Filter filter : filters.values()) {
                if (!inside(filter.bounds, viewport)) throw new AssertionError("RTI_002: " + tab.label
                        + " clipped secondary filter has no native rail at its visible row: " + filter.label);
            }
            return false; // A static, fully visible chip row needs no panning.
        }
        int left = rail.getX() + (int) (rail.getWidth() * 0.15);
        int right = rail.getX() + rail.getWidth() - (int) (rail.getWidth() * 0.15);
        if (right - left < 60) throw new AssertionError("Secondary filter rail is too narrow to pan safely");
        performSwipe(later ? right : left, y, later ? left : right, y);
        assertSelected(tab);
        return true;
    }

    private Filter visibleFilterAnchor(Map<String, Filter> filters, Rectangle viewport) {
        Filter anchor = null;
        int middle = viewport.getX() + viewport.getWidth() / 2;
        for (Filter filter : filters.values()) {
            if (!inside(filter.bounds, viewport)) continue;
            if (anchor == null || Math.abs(filter.bounds.getX() + filter.bounds.getWidth() / 2 - middle)
                    < Math.abs(anchor.bounds.getX() + anchor.bounds.getWidth() / 2 - middle)) anchor = filter;
        }
        return anchor;
    }

    /** Resolve the exact semantic again, excluding matching text in the table/body. */
    private WebElement filterElement(RuntimeTab tab, Filter filter) {
        assertSelected(tab);
        Rectangle stocks = bounds(STOCKS), viewport = screen();
        if (stocks == null) return null;
        WebElement match = null;
        int top = tab.bounds.getY() + tab.bounds.getHeight();
        for (WebElement element : driver.findElements(filter.locator())) {
            if (!element.isDisplayed() || !element.isEnabled()) continue;
            Rectangle r = element.getRect();
            if (!inside(r, viewport) || r.getY() < top || r.getY() + r.getHeight() > stocks.getY()) continue;
            if (match != null) throw new AssertionError("RTI_002: " + tab.label
                    + " has more than one visible filter named '" + filter.label + "' above Stocks");
            match = element;
        }
        return match;
    }

    private boolean filterSelected(RuntimeTab tab, Filter filter) {
        WebElement element = filterElement(tab, filter);
        if (element == null) return false;
        if (element.isSelected()) return true;
        // Flutter exposes these chips as plain android.view.View controls. On
        // this build even the visibly active All chip reports selected=false.
        // Verify the actual filled pill rather than treating a successful tap
        // or an unchanged stock list as proof of selection.
        Rectangle before = element.getRect();
        Rectangle stocks = bounds(STOCKS);
        if (stocks == null) return false;
        int nativeWidth = driver.manage().window().getSize().getWidth();
        BufferedImage frame;
        try {
            frame = ImageIO.read(new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)));
        } catch (IOException invalidImage) {
            throw new IllegalStateException("RTI_002: Could not decode the filter-state screenshot", invalidImage);
        }
        if (frame == null) throw new IllegalStateException("RTI_002: Appium returned an unreadable filter-state screenshot");
        WebElement current = filterElement(tab, filter);
        if (current == null || !before.equals(current.getRect())) return false;
        return ChipAppearance.isFilled(frame, before.getX(), before.getY(), before.getWidth(), before.getHeight(),
                nativeWidth, tab.bounds.getY() + tab.bounds.getHeight(), stocks.getY());
    }

    /** Style fallback for the supplied filled-active / outlined-inactive chip UI. */
    private static final class ChipAppearance {
        private static final double[] SAMPLE_X = {0.20, 0.30, 0.70, 0.80};
        private static final double[] SAMPLE_Y = {0.25, 0.50, 0.75};

        static boolean isFilled(BufferedImage image, int x, int y, int width, int height,
                                int nativeWidth, int bandTop, int bandBottom) {
            if (nativeWidth <= 0 || width <= 0 || height <= 0 || y < bandTop || y + height > bandBottom) return false;
            // Native bounds and screenshots can use different densities. Use
            // uniform width scaling; stretching Y would misplace status-bar coordinates.
            double scale = image.getWidth() / (double) nativeWidth;
            if (x < 0 || (x + width) * scale > image.getWidth() || (y + height) * scale >= image.getHeight()) return false;
            double[] background = new double[6];
            int count = 0;
            int[] outsideY = {bandTop + (y - bandTop) / 2, y + height + (bandBottom - y - height) / 2};
            for (int outside : outsideY) {
                if (outside >= y && outside <= y + height) continue;
                if (outside * scale < 0 || outside * scale >= image.getHeight()) continue;
                for (double fraction : new double[]{0.20, 0.50, 0.80}) {
                    background[count++] = luminance(image.getRGB((int) ((x + width * fraction) * scale), (int) (outside * scale)));
                }
            }
            if (count == 0) return false;
            Arrays.sort(background, 0, count);
            double surrounding = background[count / 2];
            int contrasted = 0, upper = 0, lower = 0;
            for (int row = 0; row < SAMPLE_Y.length; row++) {
                for (double fraction : SAMPLE_X) {
                    double value = luminance(image.getRGB((int) ((x + width * fraction) * scale),
                            (int) ((y + height * SAMPLE_Y[row]) * scale)));
                    double contrast = (Math.max(value, surrounding) + 0.05) / (Math.min(value, surrounding) + 0.05);
                    if (contrast >= 2.0) {
                        contrasted++;
                        if (row == 0) upper++;
                        if (row == SAMPLE_Y.length - 1) lower++;
                    }
                }
            }
            // Text/outline pixels alone cannot satisfy this: the fill must also
            // cover the upper and lower chip interior. No fixed theme colour is used.
            return contrasted >= 8 && upper >= 2 && lower >= 2;
        }

        private static double luminance(int rgb) {
            return 0.2126 * linear((rgb >> 16) & 255) + 0.7152 * linear((rgb >> 8) & 255) + 0.0722 * linear(rgb & 255);
        }

        private static double linear(int channel) {
            double value = channel / 255.0;
            return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
        }
    }

    private void tapFilter(RuntimeTab tab, Filter filter) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Rectangle r = waitFor(ExecutionProfile.shortWait()).withMessage("RTI_002: " + tab.label
                        + " filter '" + filter.label + "' is not fully visible above Stocks").until(d -> {
                    WebElement element = filterElement(tab, filter);
                    return element == null ? null : element.getRect();
                });
                driver.executeScript("mobile: clickGesture", Map.of(
                        "x", r.getX() + r.getWidth() / 2, "y", r.getY() + r.getHeight() / 2));
                assertSelected(tab);
                return;
            } catch (StaleElementReferenceException replacedChip) {
                if (attempt == 2) throw replacedChip;
            }
        }
    }

    private Map<String, Rectangle> filterPositions(RuntimeTab tab) {
        Map<String, Rectangle> result = new LinkedHashMap<>();
        for (Filter filter : readFilters(tab).values()) result.put(filter.label, filter.bounds);
        return result;
    }

    private Map<String, Rectangle> settledFilterPositions(RuntimeTab tab) {
        // Discover once outside the short animation wait. A full table semantic
        // scan per poll can consume the entire wait on a physical device.
        Map<String, Filter> filters = readFilters(tab);
        Rectangle viewport = screen();
        Filter anchor = visibleFilterAnchor(filters, viewport);
        if (anchor == null) throw new AssertionError("RTI_002: " + tab.label
                + " has no fully visible secondary filter after a rail swipe");
        final Filter observed = anchor;
        final Rectangle[] previous = {anchor.bounds};
        waitFor(ExecutionProfile.shortWait()).withMessage("RTI_002: " + tab.label
                + " secondary filter '" + anchor.label + "' did not stop moving").until(d -> {
            assertSelected(tab);
            WebElement chip = filterElement(tab, observed);
            Rectangle current = chip == null ? null : chip.getRect();
            boolean stable = current != null && inside(current, viewport) && current.equals(previous[0]);
            previous[0] = current;
            return stable;
        });
        return filterPositions(tab); // Still compare the rail's before/after positions to prove its boundary.
    }

    /** One hierarchy request replaces hundreds of remote per-element reads in RTI_002. */
    private UiSnapshot filterSnapshot(RuntimeTab tab) {
        assertSelected(tab);
        String source = driver.getPageSource();
        if (source == null || source.trim().isEmpty()) throw new IllegalStateException("RTI_002: UiAutomator2 returned empty UI source");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void error(SAXParseException e) throws SAXParseException { throw e; }
                @Override public void fatalError(SAXParseException e) throws SAXParseException { throw e; }
            });
            NodeList elements = builder.parse(new InputSource(new StringReader(source))).getElementsByTagName("*");
            List<UiNode> nodes = new ArrayList<>();
            Pattern boundsPattern = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if ("false".equals(element.getAttribute("displayed"))) continue;
                Matcher match = boundsPattern.matcher(element.getAttribute("bounds"));
                if (!match.matches()) continue;
                int left = Integer.parseInt(match.group(1)), top = Integer.parseInt(match.group(2));
                int right = Integer.parseInt(match.group(3)), bottom = Integer.parseInt(match.group(4));
                if (right <= left || bottom <= top) continue;
                String type = element.getAttribute("class");
                nodes.add(new UiNode(type.isEmpty() ? element.getTagName() : type, element.getAttribute("content-desc"),
                        element.getAttribute("resource-id"), !"false".equals(element.getAttribute("enabled")),
                        rect(left, top, right - left, bottom - top)));
            }
            return new UiSnapshot(nodes, driver.manage().window().getSize());
        } catch (org.xml.sax.SAXException | javax.xml.parsers.ParserConfigurationException | IOException malformed) {
            throw new IllegalStateException("RTI_002: Could not read the native UI snapshot", malformed);
        }
    }

    private Map<String, Filter> readFilters(RuntimeTab tab) {
        UiSnapshot snapshot = filterSnapshot(tab);
        if (snapshot.stocks == null) throw new AssertionError("RTI_002: " + tab.label + " Stocks header is unavailable while reading filters");
        int top = tab.bounds.getY() + tab.bounds.getHeight();
        List<Filter> candidates = new ArrayList<>();
        for (UiNode node : snapshot.nodes) {
            Rectangle r = node.bounds;
            String label = normalize(node.semantic);
            int y = r.getY() + r.getHeight() / 2;
            if (node.type.equals("android.view.View") && node.enabled && y > top && y < snapshot.stocks.getY()
                    && r.getWidth() > 30 && r.getHeight() > 15 && r.getHeight() <= snapshot.stocks.getHeight()
                    && r.getX() < snapshot.viewport.getWidth() && r.getX() + r.getWidth() > 0
                    && !label.isEmpty() && !excluded(label)) candidates.add(new Filter(label, node.semantic, r));
        }
        candidates.sort(Comparator.comparingInt(f -> f.bounds.getX()));
        Map<String, Filter> result = new LinkedHashMap<>();
        for (Filter filter : candidates) result.putIfAbsent(filter.label, filter);
        return result;
    }

    private TableState waitForFilterTable(RuntimeTab tab) {
        final String[] previous = {null};
        return waitFor(ExecutionProfile.defaultWait()).withMessage("RTI_002: " + tab.label
                + " did not resolve to live rows or an explicit empty state").until(d -> {
            UiSnapshot snapshot = filterSnapshot(tab);
            if (snapshot.stocks == null || snapshot.content == null) { previous[0] = null; return null; }
            int top = snapshot.stocks.getY() + snapshot.stocks.getHeight();
            String empty = "";
            Set<String> names = new LinkedHashSet<>(), data = new LinkedHashSet<>();
            for (UiNode node : snapshot.nodes) {
                if (!node.type.equals("android.view.View") || !inside(node.bounds, snapshot.content)) continue;
                String value = normalize(node.semantic), lower = value.toLowerCase(Locale.ROOT);
                for (String marker : EMPTY) if (lower.contains(marker)) empty = value;
                if (node.bounds.getY() < top || excluded(value) || value.isEmpty()) continue;
                if (DATE.matcher(value).matches() || NUMBER.matcher(value).matches()) data.add(value);
                else if (!lower.startsWith("no ") && !lower.startsWith("underlying data") && !lower.equals("nothing to show")) names.add(value);
            }
            TableState current = new TableState(names, data, empty);
            if (!current.resolved()) { previous[0] = null; return null; }
            String key = current.stableKey();
            boolean stable = key.equals(previous[0]); previous[0] = key;
            return stable ? current : null;
        });
    }

    private static final class UiNode {
        final String type, semantic, resourceId;
        final boolean enabled;
        final Rectangle bounds;
        UiNode(String type, String semantic, String resourceId, boolean enabled, Rectangle bounds) {
            this.type = type; this.semantic = semantic; this.resourceId = resourceId; this.enabled = enabled; this.bounds = bounds;
        }
    }

    private final class UiSnapshot {
        final List<UiNode> nodes;
        final Rectangle viewport, stocks, content;
        UiSnapshot(List<UiNode> nodes, Dimension size) {
            this.nodes = nodes;
            int bottom = size.getHeight();
            Rectangle header = null, scroll = null;
            for (UiNode node : nodes) {
                if (node.resourceId.equals("android:id/navigationBarBackground") && node.bounds.getY() > 0) bottom = Math.min(bottom, node.bounds.getY());
                if (node.type.equals("android.widget.ImageView") && normalize(node.semantic).equals("Stocks")
                        && (header == null || node.bounds.getY() < header.getY())) header = node.bounds;
            }
            viewport = rect(0, 0, size.getWidth(), bottom);
            for (UiNode node : nodes) if (node.type.equals("android.widget.ScrollView")) {
                Rectangle r = node.bounds;
                int left = Math.max(0, r.getX()), right = Math.min(size.getWidth(), r.getX() + r.getWidth());
                int top = Math.max(0, r.getY()), end = Math.min(bottom, r.getY() + r.getHeight());
                if (right - left >= size.getWidth() * 0.8 && end - top > 160
                        && (scroll == null || end - top > scroll.getHeight())) scroll = rect(left, top, right - left, end - top);
            }
            stocks = header; content = scroll;
        }
    }

    private TableState waitForTable(RuntimeTab tab) {
        final String[] previous = {null};
        return waitFor(ExecutionProfile.defaultWait()).withMessage(tab.label
                + " did not resolve to live rows or an explicit empty state").until(d -> {
            assertSelected(tab);
            TableState current = readTable(tab);
            if (!current.resolved()) { previous[0] = null; return null; }
            String key = current.stableKey();
            boolean stable = key.equals(previous[0]); previous[0] = key;
            return stable ? current : null;
        });
    }

    private TableState readTable(RuntimeTab tab) {
        Rectangle stocks = bounds(STOCKS), viewport = moduleViewport();
        int top = stocks == null ? tab.bounds.getY() + tab.bounds.getHeight() : stocks.getY() + stocks.getHeight();
        String empty = "";
        Set<String> names = new LinkedHashSet<>(), data = new LinkedHashSet<>();
        for (WebElement element : driver.findElements(VALUES)) {
            if (!element.isDisplayed()) continue;
            Rectangle r = element.getRect();
            if (!inside(r, viewport)) continue;
            String value = normalize(element.getAttribute("content-desc"));
            String lower = value.toLowerCase(Locale.ROOT);
            for (String marker : EMPTY) if (lower.contains(marker)) empty = value;
            if (r.getY() < top || excluded(value) || value.isEmpty()) continue;
            if (DATE.matcher(value).matches() || NUMBER.matcher(value).matches()) data.add(value);
            else if (!lower.startsWith("no ") && !lower.startsWith("underlying data") && !lower.equals("nothing to show")) names.add(value);
        }
        return new TableState(names, data, empty);
    }

    private boolean excluded(String value) {
        return TAB.matcher(value).matches() || value.equals("Analyst's Choice") || value.equals("Customise")
                || value.equals("Stocks") || value.equals("#") || value.equals("?") || value.equals("Go back")
                || value.equals("Take me there") || value.equals("Need a hand deciding?")
                || value.equals("Our Investment Planner picks the stocks and split for you")
                || value.contains("Get Stock Advisor") || value.equals("Unlock now");
    }

    private void settleRows(RuntimeTab tab) {
        final Map<?, ?>[] previous = {null};
        waitFor(ExecutionProfile.shortWait()).withMessage(
                "RTI_004: No stable visible date-row position after scrolling " + tab.label).until(d -> {
            assertSelected(tab);
            if (!plannerWidgetBounds().isEmpty()) return true;
            Rectangle viewport = moduleViewport();
            // One fresh row is enough to observe the end of a scroll. Reading every
            // name/date/price on every poll can exhaust the short wait on a device.
            for (WebElement cell : driver.findElements(DATE_CELLS)) {
                if (!cell.isDisplayed()) continue;
                Rectangle r = cell.getRect();
                if (!inside(r, viewport)) continue;
                Map<String, Rectangle> current = Map.of(normalize(cell.getAttribute("content-desc")), r);
                boolean stable = current.equals(previous[0]); previous[0] = current;
                return stable;
            }
            // The viewport can contain only footer/empty-state content. Continue
            // the bounded search; success still requires all three widget controls.
            Map<String, Rectangle> current = Map.of("viewport", viewport);
            boolean stable = current.equals(previous[0]); previous[0] = current;
            return stable;
        });
    }

    private Map<String, Rectangle> plannerWidgetBounds() {
        try {
            Rectangle headline = bounds(PLANNER_HEADLINE);
            if (headline == null) return Map.of();
            Rectangle subtitle = bounds(PLANNER_SUBTITLE);
            if (subtitle == null) return Map.of();
            Rectangle cta = bounds(PLANNER_CTA);
            if (cta == null) return Map.of();
            Rectangle viewport = screen();
            if (!inside(headline, viewport) || !inside(subtitle, viewport) || !inside(cta, viewport)) return Map.of();
            return Map.of("headline", headline, "subtitle", subtitle, "cta", cta);
        } catch (StaleElementReferenceException replacedWidget) {
            return Map.of(); // Query fresh controls on the next bounded search/poll.
        }
    }

    /** Wait on the actual click target once the widget is visible. */
    private void waitForPlannerCtaToSettle(RuntimeTab tab) {
        final Rectangle[] previous = {null};
        waitFor(ExecutionProfile.shortWait()).withMessage("RTI_004: The visible planner CTA did not stop moving")
                .until(d -> {
                    assertSelected(tab);
                    Rectangle cta = plannerWidgetBounds().get("cta");
                    if (cta == null) { previous[0] = null; return false; }
                    boolean stable = cta.equals(previous[0]); previous[0] = cta;
                    return stable;
                });
    }

    private Map<String, Rectangle> settledHeaders(RuntimeTab tab) {
        final Map<?, ?>[] previous = {null};
        return waitFor(ExecutionProfile.defaultWait()).withMessage("Live table headings are unavailable").until(d -> {
            assertSelected(tab);
            Rectangle stocks = bounds(STOCKS);
            if (stocks == null) { previous[0] = null; return null; }
            Map<String, Rectangle> current = new LinkedHashMap<>();
            List<WebElement> elements = new ArrayList<>(driver.findElements(HEADERS));
            elements.sort(Comparator.comparingInt(e -> e.getRect().getX()));
            for (WebElement element : elements) {
                Rectangle r = element.getRect(); int y = r.getY() + r.getHeight() / 2;
                if (element.isDisplayed() && y >= stocks.getY() && y <= stocks.getY() + stocks.getHeight()
                        && r.getX() + r.getWidth() > stocks.getX() + stocks.getWidth()
                        && r.getX() < screen().getWidth()) current.put(normalize(element.getAttribute("content-desc")), r);
            }
            if (current.isEmpty()) { previous[0] = null; return null; }
            boolean stable = current.equals(previous[0]); previous[0] = current;
            return stable ? current : null;
        });
    }

    private String firstFullHeader(Map<String, Rectangle> headers) {
        for (Map.Entry<String, Rectangle> header : headers.entrySet()) if (fullColumn(header.getValue())) return header.getKey();
        return null;
    }

    private boolean fullColumn(Rectangle r) {
        Rectangle stocks = bounds(STOCKS);
        return r != null && stocks != null && fullyVisible(r) && r.getX() >= stocks.getX() + stocks.getWidth();
    }

    private void panTableTo(RuntimeTab tab, String target, boolean later) {
        Map<String, Rectangle> before = settledHeaders(tab);
        int stalled = 0;
        for (int swipe = 0; swipe < maxHorizontal; swipe++) {
            assertSelected(tab);
            Rectangle stocks = bounds(STOCKS), rail = tableRail(stocks);
            int left = rail.getX() + Math.max(24, (int) (rail.getWidth() * 0.12));
            int right = rail.getX() + rail.getWidth() - Math.max(24, (int) (rail.getWidth() * 0.12));
            int y = rail.getY() + Math.min((int) (stocks.getHeight() * (stalled > 0 ? 0.80 : 0.55)), rail.getHeight() / 4);
            if (right - left < 60 || y <= stocks.getY() + stocks.getHeight()) throw new AssertionError("No usable first-row horizontal data lane");
            performSwipe(later ? right : left, y, later ? left : right, y);
            Map<String, Rectangle> after = settledHeaders(tab);
            if (fullColumn(after.get(target))) return;
            stalled = after.equals(before) ? stalled + 1 : 0;
            if (stalled >= 2) throw new AssertionError(tab.label + " table did not move to column '" + target + "'");
            before = after;
        }
        throw new AssertionError(tab.label + " did not reach '" + target + "' within " + maxHorizontal + " horizontal swipes");
    }

    private Rectangle tableRail(Rectangle stocks) {
        if (stocks == null) throw new AssertionError("Stocks table header is unavailable");
        Rectangle screen = screen(), best = null;
        for (WebElement element : driver.findElements(HORIZONTAL)) {
            Rectangle r = element.getRect();
            int left = Math.max(stocks.getX() + stocks.getWidth(), r.getX());
            int right = Math.min(screen.getWidth(), r.getX() + r.getWidth());
            int top = Math.max(stocks.getY() + stocks.getHeight(), r.getY());
            int bottom = Math.min(screen.getHeight(), r.getY() + r.getHeight());
            if (element.isDisplayed() && right - left > 80 && bottom - top > screen.getHeight() * 0.15
                    && (best == null || bottom - top > best.getHeight())) best = rect(left, top, right - left, bottom - top);
        }
        if (best == null) throw new AssertionError("Native horizontal table scroll container is unavailable");
        return best;
    }

    private Rectangle moduleViewport() {
        Rectangle screen = screen(), best = null;
        for (WebElement element : driver.findElements(SCROLL)) {
            Rectangle r = element.getRect();
            int left = Math.max(0, r.getX()), right = Math.min(screen.getWidth(), r.getX() + r.getWidth());
            int top = Math.max(0, r.getY()), bottom = Math.min(screen.getHeight(), r.getY() + r.getHeight());
            if (element.isDisplayed() && right - left >= screen.getWidth() * 0.8 && bottom - top > 160
                    && (best == null || bottom - top > best.getHeight())) best = rect(left, top, right - left, bottom - top);
        }
        if (best == null) throw new AssertionError("Analyst's Choice has no usable content ScrollView");
        return best;
    }

    private void tap(By locator) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Rectangle r = waitFor(ExecutionProfile.shortWait()).withMessage("Control is unavailable: " + locator).until(d -> {
                    WebElement current = first(locator);
                    return current != null && current.isEnabled() && fullyVisible(current.getRect()) ? current.getRect() : null;
                });
                driver.executeScript("mobile: clickGesture", Map.of("x", r.getX() + r.getWidth() / 2, "y", r.getY() + r.getHeight() / 2));
                return;
            } catch (StaleElementReferenceException stale) {
                if (attempt == 2) throw stale;
            }
        }
    }

    private void performSwipe(int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "ready-invest-current-sac");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(100)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        try { driver.perform(List.of(swipe)); }
        catch (WebDriverException failure) {
            try { driver.resetInputState(); } catch (WebDriverException release) { failure.addSuppressed(release); }
            throw failure;
        }
    }

    private WebDriverWait waitFor(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.pollingEvery(ExecutionProfile.pollingInterval());
        wait.ignoring(NoSuchElementException.class).ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private WebElement first(By locator) {
        for (WebElement element : driver.findElements(locator)) if (element.isDisplayed()) return element;
        return null;
    }
    private Rectangle bounds(By locator) { WebElement e = first(locator); return e == null ? null : e.getRect(); }
    private boolean visible(By locator) { return first(locator) != null; }
    private boolean selected(By locator) { WebElement e = first(locator); return e != null && e.isSelected(); }
    private boolean full(By locator) { Rectangle r = bounds(locator); return r != null && fullyVisible(r); }
    private boolean fullyVisible(Rectangle r) { return inside(r, screen()); }
    private boolean inside(Rectangle r, Rectangle area) {
        return r.getWidth() > 0 && r.getHeight() > 0 && r.getX() >= area.getX() && r.getY() >= area.getY()
                && r.getX() + r.getWidth() <= area.getX() + area.getWidth() && r.getY() + r.getHeight() <= area.getY() + area.getHeight();
    }
    private Rectangle screen() {
        Dimension size = driver.manage().window().getSize(); int bottom = size.getHeight();
        for (WebElement bar : driver.findElements(NAVIGATION_BAR)) if (bar.getRect().getY() > 0) bottom = Math.min(bottom, bar.getRect().getY());
        return rect(0, 0, size.getWidth(), bottom);
    }
    private Rectangle rect(int x, int y, int width, int height) { return new Rectangle(new Point(x, y), new Dimension(width, height)); }
    private String normalize(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }
    private List<String> tabLabels(List<RuntimeTab> tabs) {
        List<String> result = new ArrayList<>(); for (RuntimeTab tab : tabs) result.add(tab.label); return result;
    }
    private int positiveInt(String key, int fallback) {
        String raw = ConfigReader.getOptional(key, String.valueOf(fallback));
        try { int value = Integer.parseInt(raw.trim()); if (value > 0) return value; }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException(key + " must be a positive integer", invalid); }
        throw new IllegalArgumentException(key + " must be a positive integer");
    }

    private static final class RuntimeTab {
        final int index, total; final String label, semantic; final Rectangle bounds;
        RuntimeTab(int index, int total, String label, String semantic, Rectangle bounds) {
            this.index = index; this.total = total; this.label = label; this.semantic = semantic; this.bounds = bounds;
        }
        By locator() { return AppiumBy.accessibilityId(semantic); }
    }
    private static final class Filter {
        final String label, semantic; final Rectangle bounds;
        Filter(String label, String semantic, Rectangle bounds) { this.label = label; this.semantic = semantic; this.bounds = bounds; }
        By locator() { return AppiumBy.accessibilityId(semantic); }
    }
    private static final class TableState {
        final Set<String> names, data; final String empty;
        TableState(Set<String> names, Set<String> data, String empty) { this.names = names; this.data = data; this.empty = empty; }
        boolean resolved() { return !empty.isEmpty() || !names.isEmpty() && !data.isEmpty(); }
        String stableKey() {
            List<String> dates = new ArrayList<>(); for (String value : data) if (DATE.matcher(value).matches()) dates.add(value);
            return empty.isEmpty() ? names + ":dates=" + dates : "EMPTY:" + empty;
        }
        String describe() { return empty.isEmpty() ? names.size() + " live stock semantics; " + data.size() + " date/numeric semantics" : "empty state: " + empty; }
    }
}
