package com.valueresearch.pages;

import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.ExecutionProfile;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Product changes only. Shared AdvisorAccountSession/AuthHelper own login and PIN. */
public class CoverageChangesbyProPage {
    public static final String BUILD_VERSION = "COVERAGE_PRO_TC001_003_20261015";
    public static final String COVERAGE_PITCH = "See what our analysts say about every stock we cover";
    public static final String INSIGHTS_PITCH = "Read what our analysts are thinking";

    // Reused from CoverageInsightsPage and the supplied Inspector hierarchy.
    private static final By FUNDS = AppiumBy.accessibilityId("Funds");
    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By COVERAGE = AppiumBy.accessibilityId("Open coverage");
    private static final By INSIGHTS = AppiumBy.accessibilityId("Open insights");
    private static final By REPORTS = AppiumBy.accessibilityId("Open reports");
    private static final By STOCKS_HEADING = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").description(\"Stocks\")");
    private static final By HUB_TILES = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Open \")");
    private static final By HUB_SECTIONS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionMatches(\"^(Profile|Investor Accounts|Mutual Funds|Stocks|More)$\")");
    private static final By ALL_STORIES = AppiumBy.accessibilityId("All Stories");
    private static final By BACK = AppiumBy.accessibilityId("Go back");
    private static final By SCROLL = AppiumBy.className("android.widget.ScrollView");
    private static final By SEARCH = AppiumBy.className("android.widget.EditText");
    private static final By BUTTONS = AppiumBy.className("android.widget.Button");
    private static final By IMAGES = AppiumBy.className("android.widget.ImageView");
    private static final By VIEWS = AppiumBy.className("android.view.View");
    private static final By ALL_CATEGORY = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"(?i)^All(?:,\\\\s*tab\\\\s+\\\\d+\\\\s+of\\\\s+\\\\d+)?$\")");
    private static final By ANDROID_NAVIGATION_BAR = By.id("android:id/navigationBarBackground");

    private final AndroidDriver driver;
    private final AdvisorAccountSession session;
    // Same tuning keys and gesture speed as Stock Analyst's Choice TC_006.
    private final int dragMillis = positiveInt("stockAnalystsChoiceStockDragMillis", 500);
    private final int maxDrags = positiveInt("stockAnalystsChoiceMaxStockDrags", 3);
    private Rectangle feedBounds;
    private Rectangle moduleBack;
    private boolean moduleBackExposed;
    private String selectedCompany;
    private boolean drawerMayBeOpen;
    private double referenceDistance;

    public CoverageChangesbyProPage(AndroidDriver driver, AdvisorAccountSession session) {
        this.driver = Objects.requireNonNull(driver, "AndroidDriver is required");
        this.session = Objects.requireNonNull(session, "Prepare the account before module navigation");
        if (!session.isForDriver(driver)) throw new IllegalArgumentException("Account session belongs to another driver");
    }

    /** Called independently before every case; no assumption about the previous case's tab. */
    public void navigateFundsToHubStocks() {
        session.assertPrepared();
        returnToHub();
        ReportLogger.step("Navigate Funds -> Hub -> Stocks section");
        tap(FUNDS);
        until(() -> visible(FUNDS) && visible(HUB) && !isHubVisible(), ExecutionProfile.defaultWait(),
                "Funds tab did not open");
        tap(HUB);
        session.assertHubVisible();
        revealHubIcons();
    }

    // TC_001
    public void verifyCoverageInsightsReportsIcons() {
        session.assertPrepared();
        session.assertHubVisible();
        revealHubIcons();
        require(STOCKS_HEADING, ExecutionProfile.shortWait());
        for (By icon : new By[]{COVERAGE, INSIGHTS, REPORTS}) {
            require(icon, ExecutionProfile.shortWait());
        }
        ReportLogger.pass("Stocks section shows Coverage, Insights and Reports icons");
    }

    // TC_002: the company is a live Button below Coverage search, never a saved article/company name.
    public void verifyDynamicStockDrawerExpansionAndDismissal() {
        openHubSection(COVERAGE);
        selectCoverageAll();
        LiveCompany company = findSettledCompany();
        selectedCompany = company.name;
        feedBounds = feedContainer().getRect();
        WebElement back = first(BACK);
        moduleBackExposed = back != null;
        moduleBack = back == null ? moduleHeaderAnchor() : back.getRect();
        ReportLogger.step("Open the live company button: " + selectedCompany);
        drawerMayBeOpen = true;
        tapCompany(company.name);

        // Adapted from SAC TC_006: opaque ScrollView + changed geometry proves a drawer;
        // company/chart labels are not required because this APK omits their semantics.
        Panel initial = settledPanel(false, ExecutionProfile.longWait()).panel;
        int unit = headerUnit();
        if (initial.toolbar != null || initial.content.getY() <= bottom(moduleBack) + unit) {
            throw new AssertionError("Company tap must first open a partial stock-details drawer");
        }
        ReportLogger.pass("Stock-details drawer opened");
        Panel current = initial;
        referenceDistance = Math.abs(expansionStart(initial) - expansionEnd());
        boolean expanded = false;
        for (int attempt = 0; attempt < maxDrags; attempt++) {
            drag(centerX(current.content), expansionStart(current), expansionEnd(), true);
            current = settledPanel(false, ExecutionProfile.defaultWait()).panel;
            if (expanded(initial, current)) { expanded = true; break; }
        }
        if (!expanded) throw new AssertionError("Stock drawer did not expand to full screen after " + maxDrags + " swipes");
        ReportLogger.pass("Stock-details drawer expanded to full screen");

        for (int attempt = 0; attempt < maxDrags; attempt++) {
            Rectangle start = current.toolbar == null ? current.content : current.toolbar;
            int startY = current.toolbar == null ? expansionStart(current) : centerY(start);
            // Press and drag the actual toolbar, as in SAC TC_006; no Back in the assertion path.
            drag(centerX(start), startY, bottom(usableBounds()) - unit - 1, true);
            DrawerState outcome = settledPanel(true, ExecutionProfile.defaultWait());
            if (outcome.panel == null) {
                drawerMayBeOpen = false;
                ReportLogger.pass("Downward swipe dismissed the drawer and restored Coverage > All");
                returnToHub();
                return;
            }
            current = outcome.panel;
        }
        throw new AssertionError("Swiping the stock drawer down did not restore the Coverage feed");
    }

    // TC_003: each section is opened through its own exact Hub entry point.
    public void verifyLapsedCoverageAndInsightsCopy() {
        openHubSection(COVERAGE);
        verifyPitch(COVERAGE_PITCH);
        returnToHub();
        openHubSection(INSIGHTS);
        verifyPitch(INSIGHTS_PITCH);
    }

    private void verifyPitch(String expected) {
        require(ALL_STORIES, ExecutionProfile.defaultWait());
        WebElement pitch = require(AppiumBy.accessibilityId(expected), ExecutionProfile.longWait());
        if (!expected.equals(pitch.getAttribute("content-desc"))) {
            throw new AssertionError("Subscription pitch must match exactly: " + expected);
        }
        ReportLogger.pass("All Stories heading and exact pitch verified: " + expected);
    }

    private void openHubSection(By entry) {
        session.assertPrepared();
        returnToHub();
        revealHubIcons();
        tap(entry);
        // The supplied active-feed XML omits Go back and sometimes All Stories.
        until(() -> visible(HUB) && !visible(HUB_TILES)
                        && (visible(ALL_STORIES) || visible(SEARCH)), ExecutionProfile.defaultWait(),
                "Hub entry did not open Coverage/Insights");
    }

    private void revealHubIcons() {
        session.assertHubVisible();
        // Preserve the current Hub position. Search down, then recover up if below Stocks.
        for (boolean down : new boolean[]{true, false}) {
            for (int attempt = 0; attempt < 8; attempt++) {
                if (iconsVisible()) return;
                scrollContent(down, true);
                awaitContentSettled();
            }
        }
        if (!iconsVisible()) throw new AssertionError("Coverage, Insights and Reports icons were not all visible after bounded Hub scrolling");
    }

    private boolean iconsVisible() {
        return visible(STOCKS_HEADING) && fullyVisible(COVERAGE) && fullyVisible(INSIGHTS) && fullyVisible(REPORTS);
    }

    private void selectCoverageAll() {
        Rectangle search = require(SEARCH, ExecutionProfile.longWait()).getRect();
        WebElement all = null;
        for (WebElement candidate : driver.findElements(ALL_CATEGORY)) {
            if (candidate.isDisplayed() && bottom(candidate.getRect()) <= search.getY()) {
                all = candidate;
                break;
            }
        }
        if (all != null) tap(ALL_CATEGORY);
        else {
            // Supplied XML omits the visible All chip. Like CoverageInsightsPage's
            // unlabeled-tab fallback, anchor the tap to a live control, not device pixels.
            int x = search.getX() + Math.round(search.getWidth() * 0.09f);
            int y = search.getY() - search.getHeight();
            WebElement backButton = first(BACK);
            Rectangle back = backButton == null ? moduleHeaderAnchor() : backButton.getRect();
            if (y <= bottom(back) || y >= search.getY()) {
                throw new AssertionError("Cannot derive the unlabeled All chip from Coverage search bounds");
            }
            ReportLogger.debug("All chip has no accessibility node; using search-relative tap");
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
        }
        until(() -> visible(SEARCH) && !visible(AppiumBy.accessibilityId(COVERAGE_PITCH))
                        && feedHasArticles(), ExecutionProfile.longWait(),
                "Coverage > All did not load live articles for the active account");
        awaitContentSettled();
    }

    private Rectangle moduleHeaderAnchor() {
        Rectangle search = require(SEARCH, ExecutionProfile.shortWait()).getRect();
        Rectangle screen = usableBounds();
        Rectangle best = null;
        for (WebElement view : driver.findElements(VIEWS)) {
            Rectangle r = view.getRect();
            if (view.isDisplayed() && r.getWidth() >= screen.getWidth() * 4 / 5 && r.getY() > 0
                    && r.getHeight() > 0 && r.getHeight() <= screen.getHeight() / 5
                    && bottom(r) <= search.getY()
                    && (best == null || r.getY() < best.getY()
                    || (r.getY() == best.getY() && r.getHeight() < best.getHeight()))) best = r;
        }
        if (best != null) return best;
        // Opaque active-feed toolbar: use the live search-field height as the UI scale.
        // This anchor guides the drag only; full-screen success still requires the real
        // Go back toolbar and a settled panel, exactly as supplied in the drawer XML.
        int unit = search.getHeight();
        if (unit <= 0 || unit * 3 >= search.getY()) {
            throw new AssertionError("Cannot establish a safe module-header anchor");
        }
        return new Rectangle(new Point(0, unit), new Dimension(screen.getWidth(), unit));
    }

    private boolean feedHasArticles() {
        WebElement feed = feedContainer();
        for (WebElement image : feed.findElements(IMAGES)) {
            if (image.isDisplayed() && !label(image).isEmpty()) return true;
        }
        return liveCompany() != null;
    }

    private LiveCompany findSettledCompany() {
        for (int attempt = 0; attempt < 7; attempt++) {
            if (liveCompany() != null) {
                final LiveCompany[] previous = {null};
                return waitFor(ExecutionProfile.longWait()).withMessage("Live company button did not settle").until(d -> {
                    LiveCompany now = liveCompany();
                    boolean stable = now != null && previous[0] != null && now.name.equals(previous[0].name)
                            && now.bounds.equals(previous[0].bounds);
                    previous[0] = now;
                    return stable ? now : null;
                });
            }
            scrollContent(true, false);
            awaitContentSettled();
        }
        throw new AssertionError("Coverage > All has no visible company button after seven feed scans");
    }

    private LiveCompany liveCompany() {
        WebElement feed = feedContainer();
        Rectangle viewport = feed.getRect();
        WebElement search = first(SEARCH);
        int minimumY = search == null ? viewport.getY() : bottom(search.getRect());
        int navigationY = require(HUB, ExecutionProfile.shortWait()).getRect().getY();
        List<LiveCompany> companies = new ArrayList<>();
        for (WebElement button : feed.findElements(BUTTONS)) {
            String name = label(button);
            Rectangle r = button.getRect();
            if (button.isDisplayed() && button.isEnabled() && !name.isEmpty()
                    && r.getY() >= minimumY && inside(r, viewport) && bottom(r) < navigationY) {
                companies.add(new LiveCompany(name, r));
            }
        }
        companies.sort(Comparator.comparingInt(c -> c.bounds.getY()));
        return companies.isEmpty() ? null : companies.get(0);
    }

    private void tapCompany(String name) {
        waitFor(ExecutionProfile.shortWait()).withMessage("Live company changed before it could be tapped").until(d -> {
            for (WebElement button : feedContainer().findElements(BUTTONS)) {
                if (button.isDisplayed() && button.isEnabled() && name.equals(label(button))) {
                    try { click(button); return true; }
                    catch (StaleElementReferenceException | ElementClickInterceptedException retry) { return false; }
                }
            }
            return false;
        });
    }

    private WebElement feedContainer() {
        return waitFor(ExecutionProfile.shortWait()).withMessage("Visible module ScrollView is unavailable").until(d -> {
            WebElement best = null;
            for (WebElement element : driver.findElements(SCROLL)) {
                if (element.isDisplayed() && (best == null || element.getRect().getHeight() > best.getRect().getHeight())) best = element;
            }
            return best;
        });
    }

    /** Same container/toolbar strategy as SAC TC_006; never use an absolute View instance. */
    private Panel capturePanel() {
        if (feedBounds == null || moduleBack == null) return null;
        Rectangle screen = usableBounds();
        int tolerance = Math.max(3, screen.getHeight() / 200);
        List<WebElement> backs = driver.findElements(BACK);
        List<WebElement> views = driver.findElements(VIEWS);
        Panel best = null;
        for (WebElement container : driver.findElements(SCROLL)) {
            if (!container.isDisplayed() || !container.isEnabled()) continue;
            Rectangle content = container.getRect();
            if (!inside(content, screen) || content.getWidth() < screen.getWidth() * 4 / 5
                    || Math.abs(bottom(content) - bottom(screen)) > tolerance || content.equals(feedBounds)) continue;
            Rectangle toolbar = null;
            for (WebElement back : backs) {
                if (!back.isDisplayed()) continue;
                for (WebElement view : views) {
                    Rectangle r = view.getRect();
                    if (view.isDisplayed() && view.isEnabled() && inside(back.getRect(), r)
                            && r.getWidth() >= content.getWidth() * 4 / 5 && r.getY() > 0
                            && Math.abs(bottom(r) - content.getY()) <= tolerance
                            && (toolbar == null || r.getHeight() < toolbar.getHeight())) toolbar = r;
                }
            }
            // Coverage's feed ends above bottom navigation; its drawer covers navigation.
            // Unlike SAC's table, the partial drawer can therefore be taller than the feed.
            boolean partial = content.getY() > bottom(moduleBack) + headerUnit()
                    && (content.getY() - feedBounds.getY() > tolerance
                    || bottom(content) - bottom(feedBounds) > tolerance) && !visible(SEARCH);
            if ((toolbar != null || partial) && (best == null || content.getY() > best.content.getY())) {
                best = new Panel(content, toolbar);
            }
        }
        return best;
    }

    private DrawerState settledPanel(boolean allowClosed, Duration timeout) {
        final Panel[] previous = {null};
        return waitFor(timeout).withMessage("Stock drawer did not reach a settled state").until(d -> {
            Panel current = capturePanel();
            if (current == null && allowClosed && feedRestored()) return new DrawerState(null);
            boolean stable = current != null && current.sameGeometry(previous[0]);
            previous[0] = current;
            return stable ? new DrawerState(current) : null;
        });
    }

    private boolean expanded(Panel initial, Panel current) {
        return current.toolbar != null && initial.content.getY() - current.content.getY() >= headerUnit() * 2
                && current.content.getHeight() - initial.content.getHeight() >= headerUnit() * 2
                && current.content.getY() <= bottom(moduleBack) + headerUnit();
    }

    private boolean feedRestored() {
        if (capturePanel() != null || !visible(HUB)) return false;
        WebElement back = first(BACK);
        if (moduleBackExposed && (back == null || Math.abs(back.getRect().getY() - moduleBack.getY()) > 3)) return false;
        for (WebElement scroll : driver.findElements(SCROLL)) {
            if (scroll.isDisplayed() && scroll.getRect().equals(feedBounds)) {
                for (WebElement button : scroll.findElements(BUTTONS)) {
                    if (button.isDisplayed() && selectedCompany.equals(label(button))) return true;
                }
            }
        }
        return false;
    }

    /** Strict cleanup: Back may recover failures, but cannot satisfy TC_002's swipe assertion. */
    public void returnToHub() {
        if (drawerMayBeOpen) {
            for (int attempt = 0; attempt < 3 && capturePanel() != null; attempt++) {
                driver.navigate().back();
                settledPanel(true, ExecutionProfile.shortWait());
            }
            if (capturePanel() != null) throw new AssertionError("Stock drawer remained open during Hub recovery");
            drawerMayBeOpen = false;
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            if (isHubVisible()) { session.assertHubVisible(); return; }
            if (visible(BACK)) {
                tap(BACK);
                until(() -> isHubVisible() || (!visible(ALL_STORIES) && visible(HUB)), ExecutionProfile.shortWait(),
                        "Back did not restore Hub navigation");
            } else if (visible(HUB)) {
                tap(HUB);
                session.assertHubVisible();
                return;
            } else throw new IllegalStateException("No known module/Hub navigation is visible");
        }
        session.assertHubVisible();
    }

    private boolean isHubVisible() {
        return visible(HUB) && visible(SCROLL) && !visible(BACK) && !visible(ALL_STORIES)
                && (visible(HUB_TILES) || visible(HUB_SECTIONS));
    }

    private void scrollContent(boolean down, boolean hub) {
        Rectangle bounds = feedContainer().getRect();
        int top = bounds.getY();
        WebElement search = first(SEARCH);
        if (!hub && search != null) top = Math.max(top, bottom(search.getRect()));
        int lower = Math.min(bottom(bounds), require(HUB, ExecutionProfile.shortWait()).getRect().getY());
        int height = lower - top;
        if (height < 100) throw new IllegalStateException("No safe content viewport for scrolling");
        int highY = top + height / 5, lowY = lower - height / 6;
        drag(centerX(bounds), down ? lowY : highY, down ? highY : lowY, false);
    }

    private void awaitContentSettled() {
        final List<Map<String, Rectangle>> previous = new ArrayList<>();
        previous.add(null);
        waitFor(ExecutionProfile.shortWait()).withMessage("Content did not settle after navigation/scroll").until(d -> {
            Map<String, Rectangle> now = new LinkedHashMap<>();
            WebElement container = feedContainer();
            for (By locator : new By[]{BUTTONS, IMAGES}) {
                for (WebElement e : container.findElements(locator)) {
                    if (e.isDisplayed() && !label(e).isEmpty()) now.put(label(e), e.getRect());
                }
            }
            if (now.isEmpty()) now.put("viewport", container.getRect());
            boolean stable = now.equals(previous.get(0));
            previous.set(0, now);
            return stable;
        });
    }

    private int headerUnit() { return Math.max(2, moduleBack.getHeight()); }
    private int expansionStart(Panel panel) { return panel.content.getY() + Math.min(headerUnit(), panel.content.getHeight() / 10); }
    private int expansionEnd() { return bottom(moduleBack) + 1; }

    private void drag(int x, int startY, int endY, boolean calibrated) {
        Rectangle screen = usableBounds();
        x = Math.max(1, Math.min(screen.getWidth() - 2, x));
        startY = Math.max(1, Math.min(bottom(screen) - 2, startY));
        endY = Math.max(1, Math.min(bottom(screen) - 2, endY));
        if (startY == endY) throw new IllegalStateException("Gesture has no usable drag distance");
        long millis = calibrated ? Math.max(1, Math.round(dragMillis * Math.abs(endY - startY) / referenceDistance)) : 450;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "coverage-product-change");
        Sequence gesture = new Sequence(finger, 0);
        gesture.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        gesture.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        gesture.addAction(new Pause(finger, Duration.ofMillis(95)));
        gesture.addAction(finger.createPointerMove(Duration.ofMillis(millis), PointerInput.Origin.viewport(), x, endY));
        gesture.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        try { driver.perform(List.of(gesture)); }
        catch (WebDriverException failure) {
            try { driver.resetInputState(); } catch (WebDriverException release) { failure.addSuppressed(release); }
            throw failure;
        }
    }

    private Rectangle usableBounds() {
        Dimension screen = driver.manage().window().getSize();
        int lower = screen.getHeight();
        for (WebElement bar : driver.findElements(ANDROID_NAVIGATION_BAR)) {
            if (bar.getRect().getY() > 0) lower = Math.min(lower, bar.getRect().getY());
        }
        return new Rectangle(new Point(0, 0), new Dimension(screen.getWidth(), lower));
    }

    private boolean fullyVisible(By locator) {
        WebElement e = first(locator), hub = first(HUB);
        return e != null && hub != null && e.getRect().getWidth() > 0 && e.getRect().getY() > 0
                && bottom(e.getRect()) < hub.getRect().getY();
    }

    private void tap(By locator) {
        waitFor(ExecutionProfile.shortWait()).withMessage("Control could not be tapped: " + locator).until(d -> {
            WebElement e = first(locator);
            if (e == null || !e.isEnabled()) return false;
            try { click(e); return true; }
            catch (StaleElementReferenceException | ElementClickInterceptedException retry) { return false; }
        });
    }

    private void click(WebElement e) {
        driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) e).getId()));
    }

    private WebElement require(By locator, Duration timeout) {
        return waitFor(timeout).withMessage("Expected visible control: " + locator).until(d -> first(locator));
    }

    private WebElement first(By locator) {
        for (WebElement e : driver.findElements(locator)) if (e.isDisplayed()) return e;
        return null;
    }

    private boolean visible(By locator) { return first(locator) != null; }
    private String label(WebElement e) { String value = e.getAttribute("content-desc"); return value == null ? "" : value.trim(); }
    private static int bottom(Rectangle r) { return r.getY() + r.getHeight(); }
    private static int centerX(Rectangle r) { return r.getX() + r.getWidth() / 2; }
    private static int centerY(Rectangle r) { return r.getY() + r.getHeight() / 2; }
    private static boolean inside(Rectangle item, Rectangle outer) {
        return item.getWidth() > 0 && item.getHeight() > 0 && item.getX() >= outer.getX()
                && item.getY() >= outer.getY() && item.getX() + item.getWidth() <= outer.getX() + outer.getWidth()
                && bottom(item) <= bottom(outer);
    }

    private WebDriverWait waitFor(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.pollingEvery(ExecutionProfile.pollingInterval());
        wait.ignoring(NoSuchElementException.class).ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private void until(BooleanSupplier condition, Duration timeout, String message) {
        waitFor(timeout).withMessage(message).until(d -> condition.getAsBoolean());
    }

    private static int positiveInt(String key, int fallback) {
        int value = Integer.parseInt(ConfigReader.getOptional(key, String.valueOf(fallback)).trim());
        if (value <= 0) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }

    private static final class LiveCompany {
        private final String name;
        private final Rectangle bounds;
        private LiveCompany(String name, Rectangle bounds) { this.name = name; this.bounds = bounds; }
    }

    private static final class Panel {
        private final Rectangle content;
        private final Rectangle toolbar;
        private Panel(Rectangle content, Rectangle toolbar) { this.content = content; this.toolbar = toolbar; }
        private boolean sameGeometry(Panel other) {
            return other != null && content.equals(other.content) && Objects.equals(toolbar, other.toolbar);
        }
    }

    private static final class DrawerState {
        private final Panel panel;
        private DrawerState(Panel panel) { this.panel = panel; }
    }
}
