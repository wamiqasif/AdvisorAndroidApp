package com.valueresearch.pages;

import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
 * VRSA Investment Planner 2.0 - Your Orders -> Customize -> Adjust Weightage (PRD v1.1 weight locks).
 *
 * Flow per case: Hub -> Investment Planner -> order source (preset portfolio, Recommendation list
 * or saved Watchlist) + amount -> Generate my order (reuses InvestmentPlannerPage) -> Your Orders
 * -> Customize -> Adjust Weightage. See {@link OrderSource}.
 *
 * Locators (Appium Inspector, 30-Oct-2026):
 * - accessibility ids: Customize, Add Company, Adjust Shares, Adjust Weightage, Equal Weightage,
 *   "Changing a weight locks that stock", "n of m stocks locked", Unlock all, Cancel, Confirm,
 *   Company Name, Tentative Weight, Current Price, No of Shares, Actual Weight, Value (₹).
 * - Weight stepper row (inside the HorizontalScrollView): ImageView (padlock, no label),
 *   "–" (EN DASH U+2013, not a hyphen), weight value ("10.00" / "0"), "+".
 * - Rebalance messages: "Please note\nNo room to rebalance since ...".
 *
 * The padlock exposes no locked/unlocked attribute, so lock state is verified through the
 * lock bar count and through behaviour (a locked weight never moves).
 *
 * Every screen read parses one page-source snapshot; every tap uses the live centre of the
 * node in that snapshot (no stored coordinates). Waits are explicit WebDriverWait polls.
 * Stock names, prices, weights and step size are read at runtime, never hardcoded.
 */
public class InvestmentPlannerChangePage {

    public static final String BUILD_VERSION = "VRSA-IP-CHANGE-2026-11-02-3.4";

    // ---------------------------------------------------------
    // Timing (explicit waits only)
    // ---------------------------------------------------------
    private static final long ACTION_WAIT_MS = 8_000L;
    private static final long PAGE_WAIT_MS = 25_000L;
    private static final long MESSAGE_WAIT_MS = 8_000L;
    private static final long POPUP_AUTO_CLOSE_MS = 4_000L;
    private static final long POLL_MS = 250L;
    private static final long SETTLE_TIMEOUT_MS = 3_000L;
    private static final long SETTLE_POLL_MS = 250L;

    // Recommendation "All" has ~64 rows (36 ticked + unticked); ~5 rows per screen in edit mode.
    private static final int MAX_VERTICAL_DRAGS = 30;
    private static final int MAX_HORIZONTAL_DRAGS = 6;
    private static final int MAX_STEP_TAPS = 60;
    private static final int MAX_REALLOCATION_TAPS = 20;

    // ---------------------------------------------------------
    // Numeric tolerances (values are displayed with 2 decimals)
    // ---------------------------------------------------------
    private static final double SAME_EPS = 0.005;
    private static final double SHARE_EPS = 0.05;
    private static final double TOTAL_EPS = 0.10;
    private static final double EQUAL_EPS = 0.02;

    // ---------------------------------------------------------
    // Locators (content-desc patterns)
    // ---------------------------------------------------------
    private static final Pattern YOUR_ORDERS = ci("^your orders$");
    private static final Pattern AMOUNT_INVESTED = ci("^amount invested$");
    private static final Pattern CUSTOMIZE = ci("^customi[sz]e$");
    private static final Pattern ADD_COMPANY = ci("^add company$");
    private static final Pattern ADJUST_SHARES = ci("^adjust shares$");
    private static final Pattern ADJUST_WEIGHTAGE = ci("^adjust weightage$");
    private static final Pattern EQUAL_WEIGHTAGE = ci("^equal weightage$");

    private static final Pattern LOCK_HINT = ci("^changing a weight locks that stock$");
    private static final Pattern LOCK_COUNT = ci("^([0-9]+) of ([0-9]+) stocks? locked$");
    private static final Pattern UNLOCK_ALL = ci("^unlock all$");

    private static final Pattern MINUS = Pattern.compile("^[\u2013\u2212\u2014-]$");
    private static final Pattern PLUS = Pattern.compile("^\\+$");
    private static final Pattern WEIGHT_VALUE = Pattern.compile("^[0-9]+(\\.[0-9]+)?%?$");
    private static final Pattern NUMERIC_CELL = Pattern.compile("^\u20B9?\\s?-?[0-9][0-9,]*(\\.[0-9]+)?%?$");

    private static final Pattern CANCEL = ci("^cancel$");
    private static final Pattern CONFIRM = ci("^confirm$");
    private static final Pattern MAIL_MY_ORDER = ci("^mail my order$");

    private static final Pattern COMPANY_HEADER = ci("^company( name)?$");
    private static final Pattern TENTATIVE_HEADER = ci("^tentative (weight|wt\\.?)$");
    private static final Pattern PRICE_HEADER = ci("^(current )?price$");
    private static final Pattern SHARES_HEADER = ci("^(no\\.? of shares|shares)$");
    private static final Pattern ACTUAL_HEADER = ci("^actual (weight|wt\\.?)$");
    private static final Pattern VALUE_HEADER = ci("^value( \\(\u20B9\\))?$");

    private static final Pattern PLEASE_NOTE = ci("^please note.*");
    private static final Pattern GO_BACK = ci("^go back$");
    private static final Pattern HUB_TAB = ci("^hub$");
    private static final int MAX_BACK_STEPS = 5;
    private static final long BACK_STEP_WAIT_MS = 8_000L;

    // Messages (lower-case "contains" match on content-desc or text)
    public static final String MSG_EVERY_OTHER_SET = "no room to rebalance since every other stock is already set";
    public static final String MSG_OTHERS_AT_ZERO = "no room to rebalance since the other unlocked stocks are at 0%";
    // From the PRD; not yet captured in Appium Inspector.
    public static final String MSG_ALREADY_ZERO = "this stock is already at 0%";
    public static final String MSG_CHANGES_APPLIED = "changes applied";
    public static final String MSG_CHANGES_DISCARDED = "changes discarded";
    public static final String MSG_RESET_EQUAL = "reset to equal weightage";
    private static final String[] REBALANCE_MESSAGES = {MSG_EVERY_OTHER_SET, MSG_OTHERS_AT_ZERO, MSG_ALREADY_ZERO};

    private static final Pattern BOUNDS = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]");

    private final AndroidDriver driver;
    private final AdvisorAccountSession accountSession;
    private final InvestmentPlannerPage plannerPage;
    private Dimension screen;

    /** Table order of ticked (included) companies, top to bottom, refreshed by every full weight scan. */
    private final List<String> tableOrder = new ArrayList<>();

    /**
     * Unticked (excluded) rows of the generated order, e.g. Recommendation "All" lists ~64 stocks but
     * only ~36 are ticked. Learned once per case from the first fresh read (weight 0 / no stepper /
     * 0 shares) and kept out of every weight check. null = not learned yet.
     */
    private Set<String> excludedCompanies;
    private Integer includedCount;
    /** Whether the lock bar's "of m" counts unticked rows too (learned from the first count seen). */
    private Boolean barCountsExcluded;

    /*
     * Read caches. Weights can only change after a tap, so a full table scan is reused until the next
     * tap (tapVersion). The lock bar also holds tap coordinates (Unlock all), so it is reused only until
     * the next tap or scroll (viewVersion).
     */
    private long tapVersion;
    private long viewVersion;
    private Map<String, Double> cachedWeights;
    private long cachedWeightsVersion = -1;
    private LockBar cachedBar;
    private long cachedBarTapVersion = -1;
    private long cachedBarViewVersion = -1;

    /** UiAutomator2 idle wait saved while fast reads are on (Flutter screens rarely report idle). */
    private Object savedIdleTimeout;
    private boolean fastReads;

    public InvestmentPlannerChangePage(AndroidDriver driver, AdvisorAccountSession accountSession,
                                       AuthHelper.Account account, AuthHelper.LoginMethod loginMethod) {
        this.driver = Objects.requireNonNull(driver, "AndroidDriver is required");
        this.accountSession = Objects.requireNonNull(accountSession, "Prepared AdvisorAccountSession is required");
        if (!accountSession.isForDriver(driver)) {
            throw new IllegalArgumentException("Prepared account session belongs to another driver");
        }
        this.plannerPage = new InvestmentPlannerPage(driver, accountSession, account, loginMethod);
    }

    // =========================================================
    // ORDER SOURCE
    // =========================================================

    /**
     * Where the order comes from on the Investment Planner inputs screen:
     * PRESET (All Weather / Long Term / Aggressive), RECOMMENDATION (e.g. All, Latest) or
     * WATCHLIST (a saved watchlist such as Watchlist 1). Text form: "PRESET:Long Term",
     * "RECOMMENDATION:All", "WATCHLIST:Watchlist 1"; a bare name means PRESET.
     */
    public static final class OrderSource {
        public enum Type { PRESET, RECOMMENDATION, WATCHLIST }

        public final Type type;
        public final String name;

        public OrderSource(Type type, String name) {
            this.type = Objects.requireNonNull(type, "Order source type is required");
            if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Order source name is required");
            this.name = name.trim();
        }

        public static OrderSource parse(String raw) {
            if (raw == null || raw.trim().isEmpty()) throw new IllegalArgumentException("Order source is empty");
            String value = raw.trim();
            int colon = value.indexOf(':');
            if (colon < 0) return new OrderSource(Type.PRESET, value);
            String type = value.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            try {
                return new OrderSource(Type.valueOf(type), value.substring(colon + 1));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Order source must be PRESET:<name>, RECOMMENDATION:<name> or "
                        + "WATCHLIST:<name> but was '" + raw + "'");
            }
        }

        /** Short label for reports, e.g. "Long Term", "Recommendation All", "Watchlist Watchlist 1". */
        public String label() {
            switch (type) {
                case RECOMMENDATION: return "Recommendation " + name;
                case WATCHLIST: return "Watchlist " + name;
                default: return name;
            }
        }

        @Override
        public String toString() {
            return type + ":" + name;
        }
    }

    /**
     * A deterministic product-behaviour mismatch (e.g. PRD v1.1 lock persistence): the app answers the
     * same way every time, so the case is not retried.
     */
    public static final class ProductBehaviourError extends AssertionError {
        private static final long serialVersionUID = 1L;

        public ProductBehaviourError(String message) {
            super(message);
        }
    }

    /** Thrown when the generated order has too few stocks for a scenario (reported as SKIP, not FAIL). */
    public static final class NotEnoughStocksException extends RuntimeException {
        public NotEnoughStocksException(String message) {
            super(message);
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    /** Hub -> Investment Planner -> select preset -> amount -> Generate my order -> Your Orders. */
    public void generateOrderAndOpenYourOrders(String portfolioName, long amount) {
        generateOrderAndOpenYourOrders(new OrderSource(OrderSource.Type.PRESET, portfolioName), amount);
    }

    /** Hub -> Investment Planner -> preset / Recommendation / Watchlist -> amount -> Generate my order -> Your Orders. */
    public void generateOrderAndOpenYourOrders(OrderSource source, long amount) {
        ReportLogger.step("[" + BUILD_VERSION + "] Generating order from " + source.label() + " for ₹" + amount);
        switch (source.type) {
            case PRESET:
                // Includes the summary-card title check against the selected preset.
                try {
                    plannerPage.generateBuiltInPortfolioOrder(source.name, amount);
                } catch (AssertionError wrongPortfolio) {
                    if (!String.valueOf(wrongPortfolio.getMessage()).contains("does not match selected portfolio")) {
                        throw wrongPortfolio;
                    }
                    // Run evidence (2026-11-01): the first planner opening after app launch or after a
                    // Confirm kept Long Term, and reselecting on that same planner screen failed again,
                    // while every planner opened fresh from Hub a second time generated the selected
                    // portfolio. So go back to Hub and open the planner fresh.
                    ReportLogger.info("Order was generated for another portfolio (" + wrongPortfolio.getMessage()
                            + "); reopening Investment Planner from Hub and generating " + source.name + " once more");
                    stepBackUntilHubTabVisible();
                    try {
                        accountSession.returnToHub();
                    } finally {
                        accountSession.restoreTimingSettings();
                    }
                    plannerPage.generateBuiltInPortfolioOrder(source.name, amount);
                }
                break;
            case RECOMMENDATION:
                plannerPage.openInvestmentPlannerFromHub();
                plannerPage.openOwnListSheetAndValidate();
                plannerPage.selectRecommendation(source.name);
                plannerPage.enterInvestmentAmount(amount);
                plannerPage.generateOrderAndWaitForYourOrders();
                break;
            case WATCHLIST:
                plannerPage.openInvestmentPlannerFromHub();
                plannerPage.openOwnListSheetAndValidate();
                String selected = plannerPage.selectWatchlist(source.name);
                ReportLogger.info("Watchlist selected on the planner: " + selected);
                plannerPage.enterInvestmentAmount(amount);
                plannerPage.generateOrderAndWaitForYourOrders();
                break;
            default:
                throw new IllegalArgumentException("Unsupported order source " + source);
        }
        if (!waitUntil(() -> any(snapshot(), YOUR_ORDERS) || any(snapshot(), AMOUNT_INVESTED), PAGE_WAIT_MS)) {
            throw new AssertionError("Your Orders is not visible after Generate my order; visible=" + visibleLabels());
        }
        if (source.type != OrderSource.Type.PRESET) {
            // The summary card shows the list name, e.g. "All" or "Watchlist 1".
            boolean matched = waitUntil(() -> summaryCardShows(snapshot(), source.name), ACTION_WAIT_MS);
            if (!matched) {
                throw new AssertionError("Your Orders summary card should show '" + source.name + "' but shows '"
                        + summaryCardTitle(snapshot()) + "'");
            }
        }
        ReportLogger.pass("Your Orders opened for " + source.label() + " | summary card title: " + summaryCardTitle(settledSnapshot()));
    }

    /**
     * Labels in the Your Orders summary card area: below "Your Orders", above "Amount invested",
     * left-aligned with it. The planner screen underneath can still expose "Step 1" / "Select a
     * portfolio" at the same spot (run evidence 2026-11-01), so callers look for the expected name
     * among these instead of taking the first one.
     */
    private List<String> summaryCardLabels(List<UiNode> nodes) {
        List<String> labels = new ArrayList<>();
        UiNode heading = find(nodes, YOUR_ORDERS);
        UiNode amount = find(nodes, AMOUNT_INVESTED);
        if (heading == null || amount == null) return labels;
        for (UiNode n : nodes) {
            if (n.label.isEmpty() || n.cy() <= heading.cy() || n.cy() >= amount.cy()) continue;
            if (Math.abs(n.left - amount.left) > screen().getWidth() / 12) continue;
            labels.add(n.label);
        }
        return labels;
    }

    private boolean summaryCardShows(List<UiNode> nodes, String name) {
        for (String label : summaryCardLabels(nodes)) if (label.equalsIgnoreCase(name)) return true;
        return false;
    }

    private String summaryCardTitle(List<UiNode> nodes) {
        List<String> labels = summaryCardLabels(nodes);
        return labels.isEmpty() ? null : String.join(" / ", labels);
    }

    /**
     * Opens Your Orders for this source. When reuse is allowed and the previous case left this same
     * order on screen (it ends every edit-only case with Cancel, which restores the order exactly),
     * the order is reused instead of generated again. Returns true when the order was reused.
     */
    public boolean openYourOrdersFor(OrderSource source, long amount, boolean reuseIfOpen) {
        if (reuseIfOpen && reuseCurrentOrder(source)) return true;
        generateOrderAndOpenYourOrders(source, amount);
        enableFastReads();
        return false;
    }

    /**
     * Your Orders -> Customize -> Adjust Weightage on a clean order. A reused order must open with no
     * locks; otherwise the order is generated again so one case can never inherit another's state.
     */
    public void startEditCase(OrderSource source, long amount, boolean reuseIfOpen) {
        boolean reused = openYourOrdersFor(source, amount, reuseIfOpen);
        openCustomizeAndVerifyOptions();
        openAdjustWeightage();
        if (reused && !readLockBar().hint) {
            ReportLogger.info("Reused order still has locked stocks; generating a fresh order instead");
            cancelEditIfOpen();
            generateOrderAndOpenYourOrders(source, amount);
            enableFastReads();
            openCustomizeAndVerifyOptions();
            openAdjustWeightage();
        }
    }

    private boolean reuseCurrentOrder(OrderSource source) {
        if (!isYourOrdersVisible()) return false;
        cancelEditIfOpen();
        if (!waitUntil(() -> find(snapshot(), MAIL_MY_ORDER) != null, ACTION_WAIT_MS)) return false;
        enableFastReads();
        scrollToTop();
        List<UiNode> nodes = settledSnapshot();
        if (!summaryCardShows(nodes, source.name)) {
            ReportLogger.info("Open order card shows '" + summaryCardTitle(nodes) + "', not " + source.label()
                    + "; generating a new order");
            return false;
        }
        ReportLogger.pass("Reusing the generated " + source.label() + " order (the previous case restored it with Cancel)");
        return true;
    }

    /** Your Orders (normal or edit mode) is on screen. Used by the account check to stay on this screen. */
    public boolean isYourOrdersVisible() {
        List<UiNode> nodes = snapshot();
        return any(nodes, YOUR_ORDERS) || any(nodes, AMOUNT_INVESTED)
                || (find(nodes, CONFIRM) != null && find(nodes, CANCEL) != null)
                || (find(nodes, MAIL_MY_ORDER) != null && any(nodes, COMPANY_HEADER));
    }

    /** Ends an edit-only case on Your Orders: Cancel any open edit session so the order is unchanged. */
    public void leaveEditModeKeepingOrder() {
        clearRebalancePopup();
        cancelEditIfOpen();
        if (!waitUntil(() -> find(snapshot(), MAIL_MY_ORDER) != null && find(snapshot(), CONFIRM) == null, ACTION_WAIT_MS)) {
            throw new AssertionError("Your Orders did not return to normal mode after Cancel; visible=" + visibleLabels());
        }
        clearNotes();
        ReportLogger.step("Edit session cancelled; order kept on Your Orders for the next case");
    }

    /**
     * Turns off UiAutomator2's idle wait while this page reads the screen. Flutter keeps animating,
     * so each page-source read otherwise waits for the idle timeout. Restored by restoreReadSettings().
     */
    public void enableFastReads() {
        if (fastReads) return;
        try {
            Map<String, Object> settings = driver.getSettings();
            savedIdleTimeout = settings == null ? null : settings.get("waitForIdleTimeout");
            driver.setSetting("waitForIdleTimeout", 0);
            fastReads = true;
        } catch (RuntimeException unsupported) {
            ReportLogger.debug("Could not change waitForIdleTimeout: " + unsupported.getClass().getSimpleName());
        }
    }

    /** Restores UiAutomator2's idle wait. Safe to call more than once. */
    public void restoreReadSettings() {
        if (!fastReads) return;
        fastReads = false;
        try {
            if (savedIdleTimeout instanceof Number) driver.setSetting("waitForIdleTimeout", savedIdleTimeout);
        } catch (RuntimeException unavailable) {
            ReportLogger.debug("Could not restore waitForIdleTimeout: " + unavailable.getClass().getSimpleName());
        }
    }

    /** Leaves any Your Orders state (discarding an open edit session) and finishes on Hub. */
    public void navigateBackToHub() {
        ReportLogger.step("Returning to Hub");
        restoreReadSettings();
        List<UiNode> nodes = snapshot();
        UiNode cancel = find(nodes, CANCEL);
        if (cancel != null && find(nodes, CONFIRM) != null) {
            tap(cancel, "Cancel (discard open edit session before leaving)");
            waitUntil(() -> find(snapshot(), CONFIRM) == null, ACTION_WAIT_MS);
        }
        stepBackUntilHubTabVisible();
        try {
            accountSession.returnToHub();
            accountSession.assertHubVisible();
        } finally {
            accountSession.restoreTimingSettings();
        }
        ReportLogger.pass("Hub is visible");
    }

    /**
     * Your Orders is two screens deep (Hub -> Investment Planner -> Your Orders). The shared
     * AdvisorAccountSession.returnToHub() expects the Hub tab right after one "Go back" and times
     * out on the intermediate planner screen, so step back one screen at a time here, waiting for
     * each screen to change, until the bottom-navigation Hub tab is visible.
     */
    private void stepBackUntilHubTabVisible() {
        for (int step = 1; step <= MAX_BACK_STEPS; step++) {
            List<UiNode> nodes = settledSnapshot();
            if (hubTab(nodes) != null) return;
            String before = fingerprint(nodes);
            UiNode back = find(nodes, GO_BACK);
            if (back != null) {
                tap(back, "Go back (step " + step + ")");
            } else {
                ReportLogger.step("No Go back button; using the device Back key (step " + step + ")");
                driver.navigate().back();
            }
            boolean changed = waitUntil(() -> {
                List<UiNode> now = snapshot();
                return hubTab(now) != null || !fingerprint(now).equals(before);
            }, BACK_STEP_WAIT_MS);
            if (!changed) {
                ReportLogger.debug("Screen did not change after back step " + step + "; trying again");
            }
        }
        if (hubTab(snapshot()) == null) {
            ReportLogger.debug("Hub tab still not visible after " + MAX_BACK_STEPS
                    + " back steps; AdvisorAccountSession recovery will report the screen");
        }
    }

    /** The bottom-navigation Hub tab (lowest "Hub" node in the bottom 15% of the screen). */
    private UiNode hubTab(List<UiNode> nodes) {
        UiNode best = null;
        for (UiNode n : nodes) {
            if (HUB_TAB.matcher(n.label).matches() && n.cy() > screen().getHeight() * 0.85
                    && (best == null || n.top > best.top)) {
                best = n;
            }
        }
        return best;
    }

    // =========================================================
    // CUSTOMIZE SHEET / EDIT MODE
    // =========================================================

    /** Taps Customize on the Your Orders summary card and verifies the four sheet actions. */
    public void openCustomizeAndVerifyOptions() {
        ReportLogger.step("Opening Customize from the Your Orders summary card");
        clearNotes();
        scrollToTop();
        UiNode customize = find(snapshot(), CUSTOMIZE);
        if (customize == null) {
            throw new AssertionError("Customize link is not visible on Your Orders; visible=" + visibleLabels());
        }
        tap(customize, "Customize");
        if (!waitUntil(() -> any(snapshot(), ADJUST_WEIGHTAGE), ACTION_WAIT_MS)) {
            UiNode retry = find(snapshot(), CUSTOMIZE);
            if (retry != null) tap(retry, "Customize (retry)");
            if (!waitUntil(() -> any(snapshot(), ADJUST_WEIGHTAGE), ACTION_WAIT_MS)) {
                throw new AssertionError("Customize sheet did not open; visible=" + visibleLabels());
            }
        }
        List<UiNode> sheet = settledSnapshot();
        List<String> missing = new ArrayList<>();
        if (!any(sheet, ADD_COMPANY)) missing.add("Add Company");
        if (!any(sheet, ADJUST_SHARES)) missing.add("Adjust Shares");
        if (!any(sheet, ADJUST_WEIGHTAGE)) missing.add("Adjust Weightage");
        if (!any(sheet, EQUAL_WEIGHTAGE)) missing.add("Equal Weightage");
        if (!missing.isEmpty()) {
            throw new AssertionError("Customize sheet is missing " + missing + "; visible=" + abbreviate(labelsOf(sheet), 25));
        }
        ReportLogger.pass("Customize sheet shows Add Company, Adjust Shares, Adjust Weightage and Equal Weightage");
    }

    /** Customize -> Adjust Weightage; waits for the edit mode and brings the stepper column into view. */
    public void openAdjustWeightage() {
        tapSheetOption(ADJUST_WEIGHTAGE, "Adjust Weightage", this::isEditModeVisible);
        ensureStepperColumnVisible();
        ReportLogger.pass("Adjust Weightage edit mode is open");
    }

    /**
     * Taps a Customize-sheet option and waits for its result. Clears any "Please note" toast first
     * (it covers the sheet's lower rows) and retries when a tap is swallowed and the sheet stays open.
     */
    private void tapSheetOption(Pattern option, String name, BooleanSupplier done) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            clearNotes();
            List<UiNode> nodes = settledSnapshot();
            UiNode node = find(nodes, option);
            if (node == null) {
                if (done.getAsBoolean()) return;
                openCustomizeAndVerifyOptions();
                node = find(settledSnapshot(), option);
                if (node == null) throw new AssertionError(name + " option is not visible in the Customize sheet");
            }
            tap(node, name + (attempt > 1 ? " (attempt " + attempt + ")" : ""));
            if (waitUntil(done, ACTION_WAIT_MS)) return;
            ReportLogger.debug(name + " tap did not take effect; the Customize sheet is still open");
        }
        throw new AssertionError(name + " did not respond after 3 taps; visible=" + visibleLabels());
    }

    /** Lock bar hint, padlock/–/+ on every row, Cancel/Confirm replace Mail my order, equal weights. */
    public Map<String, Double> verifyEditModeLayout() {
        ReportLogger.step("Verifying the Adjust Weightage edit mode layout");
        LockBar bar = readLockBar();
        if (!bar.hint) {
            throw new AssertionError("Lock bar should read 'Changing a weight locks that stock' with no stock locked; bar=" + bar);
        }
        ReportLogger.pass("Lock bar reads 'Changing a weight locks that stock'");

        List<UiNode> nodes = settledSnapshot();
        if (find(nodes, CANCEL) == null || find(nodes, CONFIRM) == null) {
            throw new AssertionError("Cancel and Confirm must be shown in edit mode; visible=" + abbreviate(labelsOf(nodes), 25));
        }
        if (find(nodes, MAIL_MY_ORDER) != null) {
            throw new AssertionError("Mail my order must be replaced by Cancel/Confirm while editing");
        }
        ReportLogger.pass("Cancel and Confirm replace Mail my order");
        if (find(nodes, TENTATIVE_HEADER) == null) {
            ReportLogger.info("Tentative Weight header not exposed in this snapshot; stepper column verified by its cells");
        }
        String note = firstLabelContaining(nodes, "adjust weightage with");
        if (note != null) ReportLogger.info("Edit-mode note shown: " + note);

        Map<String, Double> weights = readAllWeights();
        assertTotal(weights, "fresh edit mode");
        assertEqualWeights(weights, "fresh " + weights.size() + "-stock order");
        ReportLogger.pass("Every row (" + weights.size() + ") shows a padlock, – / + stepper and weight; weights are equal "
                + "and total 100: " + weights);
        return weights;
    }

    // =========================================================
    // SCENARIOS
    // =========================================================

    /** Padlock tap locks without changing any weight; second tap unlocks. */
    public void verifyPadlockTogglesLockWithoutChangingWeight() {
        Map<String, Double> before = readAllWeights();
        String stock = tableOrder.get(0);

        tapPadlock(stock);
        Map<String, Double> afterLock = readAllWeights();
        assertWeightsUnchanged(before, afterLock, "padlock tap on " + stock);
        assertLockBar(1, before.size(), "after locking " + stock + " with its padlock");
        ReportLogger.pass("Padlock locked " + stock + " without changing any weight");

        tapPadlock(stock);
        Map<String, Double> afterUnlock = readAllWeights();
        assertWeightsUnchanged(before, afterUnlock, "second padlock tap on " + stock);
        assertLockHint("after unlocking " + stock + " with its padlock");
        ReportLogger.pass("Second padlock tap unlocked " + stock + " without changing any weight");
    }

    /** + spreads the change equally over unlocked stocks and locks the changed stock; – likewise. */
    public void verifyStepperRedistributesAndAutoLocks() {
        requireStocks(3);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        String a = tableOrder.get(0);
        String b = tableOrder.get(1);
        Set<String> locked = new LinkedHashSet<>();

        stepAndVerify(a, true, w0, locked);
        locked.add(a);
        assertLockBar(1, total, "after + on " + a);

        Map<String, Double> w1 = readAllWeights();
        stepAndVerify(b, false, w1, locked);
        locked.add(b);
        assertLockBar(2, total, "after – on " + b);
        ReportLogger.pass("+ / – share the change equally over unlocked stocks, keep locked weights and lock the changed stock");
    }

    /** Two, then three locked stocks keep their weights while another stock is changed. */
    public void verifyLockedStocksHoldWithTwoAndThreeLocks() {
        requireStocks(5);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        Set<String> locked = new LinkedHashSet<>();

        lockWithPadlocks(tableOrder.subList(0, 2), locked);
        assertLockBar(2, total, "after locking 2 stocks with padlocks");
        Map<String, Double> w1 = readAllWeights();
        assertWeightsUnchanged(w0, w1, "locking 2 stocks with padlocks");

        String third = tableOrder.get(2);
        stepAndVerify(third, true, w1, locked);
        locked.add(third);
        assertLockBar(3, total, "after + on " + third + " with 2 stocks locked");
        ReportLogger.pass("With 2 stocks locked their weights stayed fixed while " + third + " changed");

        Map<String, Double> w2 = readAllWeights();
        String fourth = tableOrder.get(3);
        stepAndVerify(fourth, false, w2, locked);
        locked.add(fourth);
        assertLockBar(4, total, "after – on " + fourth + " with 3 stocks locked");
        ReportLogger.pass("With 3 stocks locked their weights stayed fixed while " + fourth + " changed");
    }

    /** Unlock all clears every lock without changing weights; previously locked stocks move again. */
    public void verifyUnlockAllReleasesEveryLock() {
        requireStocks(5);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        Set<String> locked = new LinkedHashSet<>();
        lockWithPadlocks(tableOrder.subList(0, 2), locked);
        String third = tableOrder.get(2);
        stepAndVerify(third, true, readAllWeights(), locked);
        locked.add(third);
        assertLockBar(3, total, "before Unlock all");

        Map<String, Double> beforeUnlock = readAllWeights();
        LockBar bar = readLockBar();
        if (bar.unlockAll == null) throw new AssertionError("Unlock all is not shown while stocks are locked; bar=" + bar);
        tap(bar.unlockAll, "Unlock all");
        if (!waitUntil(() -> any(snapshot(), LOCK_HINT), ACTION_WAIT_MS)) {
            throw new AssertionError("Lock bar did not return to 'Changing a weight locks that stock' after Unlock all; visible="
                    + visibleLabels());
        }
        ReportLogger.pass("Unlock all reset the lock bar to 'Changing a weight locks that stock'");
        Map<String, Double> afterUnlock = readAllWeights();
        assertWeightsUnchanged(beforeUnlock, afterUnlock, "Unlock all");
        ReportLogger.pass("Unlock all did not change any weight");

        String fourth = tableOrder.get(3);
        stepAndVerify(fourth, true, afterUnlock, Collections.emptySet());
        assertLockBar(1, total, "after + on " + fourth + " following Unlock all");
        ReportLogger.pass("Previously locked stocks moved again after Unlock all (change shared by all " + (total - 1) + " others)");
    }

    /**
     * All but two stocks locked: + on one moves weight from the other until it reaches 0%,
     * the next + shows "No room ... other unlocked stocks are at 0%", and – on the 0% stock shows
     * "This stock is already at 0%".
     */
    public void verifyNoRoomWhenOtherUnlockedStockIsAtZero() {
        requireStocks(3);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        String gainer = tableOrder.get(0);
        String donor = tableOrder.get(1);
        Set<String> locked = new LinkedHashSet<>();
        lockWithPadlocks(tableOrder.subList(2, total), locked);
        assertLockBar(total - 2, total, "with only " + gainer + " and " + donor + " unlocked");
        Map<String, Double> w1 = readAllWeights();
        assertWeightsUnchanged(w0, w1, "locking " + (total - 2) + " stocks with padlocks");

        double donorStart = w1.get(donor);
        double gainerStart = w1.get(gainer);
        ReportLogger.step("Raising " + gainer + " until " + donor + " reaches 0% (start " + gainerStart + " / " + donorStart + ")");
        int maxTaps = MAX_STEP_TAPS;
        double donorNow = donorStart;
        for (int tapCount = 1; donorNow > SAME_EPS; tapCount++) {
            if (tapCount > maxTaps) {
                throw new AssertionError(donor + " did not reach 0% within " + maxTaps + " + taps on " + gainer
                        + "; donor weight=" + donorNow);
            }
            StepOutcome out = step(gainer, true);
            if (out.popup != null) {
                throw new AssertionError("Rebalance message shown while " + donor + " still had " + donorNow
                        + "% to give: '" + out.popup + "'");
            }
            if (Math.abs(out.after - out.before) < SAME_EPS) {
                throw new AssertionError("+ on " + gainer + " did not change its weight (tap " + tapCount + ")");
            }
            if (tapCount == 1) {
                double stepSize = out.after - out.before;
                maxTaps = Math.min(MAX_STEP_TAPS, (int) Math.ceil(donorStart / stepSize) + 2);
                ReportLogger.info("Observed step size " + round2(stepSize) + "; allowing up to " + maxTaps + " taps");
            }
            Double donorVisible = out.visible.get(donor);
            donorNow = donorVisible != null ? donorVisible : readAllWeights().get(donor);
            if (donorNow < -SAME_EPS) throw new AssertionError(donor + " went below 0%: " + donorNow);
        }
        Map<String, Double> w2 = readAllWeights();
        assertLockedUnchanged(w1, w2, locked, "raising " + gainer);
        assertClose(w2.get(donor), 0.0, SAME_EPS, donor + " weight");
        assertClose(w2.get(gainer), gainerStart + donorStart, TOTAL_EPS, gainer + " weight (took all of " + donor + ")");
        assertTotal(w2, "after " + donor + " reached 0%");
        ReportLogger.pass(donor + " reached 0% and " + gainer + " holds " + w2.get(gainer) + "%; locked weights unchanged");

        StepOutcome noRoom = step(gainer, true);
        assertPopup(noRoom, MSG_OTHERS_AT_ZERO, "+ on " + gainer + " with the only other unlocked stock at 0%");
        assertClose(noRoom.after, noRoom.before, SAME_EPS, gainer + " weight after the refused +");

        // Unlock the gainer so the 0% stock has an unlocked partner; otherwise
        // "every other stock is already set" correctly takes priority.
        tapPadlock(gainer);
        locked.remove(gainer);
        assertLockBar(total - 2, total, "after unlocking " + gainer + " with its padlock");
        StepOutcome atZero = step(donor, false);
        assertPopup(atZero, MSG_ALREADY_ZERO, "– on " + donor + " at 0% (with " + gainer + " unlocked)");
        assertClose(atZero.after, 0.0, SAME_EPS, donor + " weight after the refused –");

        Map<String, Double> w3 = readAllWeights();
        assertWeightsUnchanged(w2, w3, "refused + / – taps");
        ReportLogger.pass("Refused taps left every weight unchanged");
    }

    /** All stocks but one locked: + and – on the unlocked stock show "every other stock is already set". */
    public void verifyNoRoomWhenEveryOtherStockIsLocked() {
        requireStocks(2);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        String free = tableOrder.get(0);
        Set<String> locked = new LinkedHashSet<>();
        lockWithPadlocks(tableOrder.subList(1, total), locked);
        assertLockBar(total - 1, total, "with only " + free + " unlocked");

        StepOutcome up = step(free, true);
        assertPopup(up, MSG_EVERY_OTHER_SET, "+ on " + free + " with every other stock locked");
        assertClose(up.after, up.before, SAME_EPS, free + " weight after the refused +");
        StepOutcome down = step(free, false);
        assertPopup(down, MSG_EVERY_OTHER_SET, "– on " + free + " with every other stock locked");
        assertClose(down.after, down.before, SAME_EPS, free + " weight after the refused –");

        assertWeightsUnchanged(w0, readAllWeights(), "refused taps with " + (total - 1) + " stocks locked");
        assertLockBar(total - 1, total, "refused taps must not lock " + free);
        ReportLogger.pass("Refused taps did not change weights and did not lock " + free);
    }

    /** Every stock locked: changing any stock shows "every other stock is already set". */
    public void verifyNoRoomWhenAllStocksAreLocked() {
        requireStocks(2);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        Set<String> locked = new LinkedHashSet<>();
        lockWithPadlocks(new ArrayList<>(tableOrder), locked);
        assertLockBar(total, total, "with every stock locked");

        String first = tableOrder.get(0);
        String last = tableOrder.get(total - 1);
        StepOutcome up = step(first, true);
        assertPopup(up, MSG_EVERY_OTHER_SET, "+ on locked " + first + " with all stocks locked");
        StepOutcome down = step(last, false);
        assertPopup(down, MSG_EVERY_OTHER_SET, "– on locked " + last + " with all stocks locked");
        assertWeightsUnchanged(w0, readAllWeights(), "refused taps with all stocks locked");
        ReportLogger.pass("With all " + total + " stocks locked no weight can change");
    }

    /** Cancel restores the snapshot (weights and locks) and shows "Changes discarded". */
    public void verifyCancelDiscardsChanges() {
        requireStocks(3);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        String a = tableOrder.get(0);
        String b = tableOrder.get(1);
        stepAndVerify(a, true, w0, Collections.emptySet());
        tapPadlock(b);
        assertLockBar(2, total, "before Cancel");

        UiNode cancel = find(snapshot(), CANCEL);
        if (cancel == null) throw new AssertionError("Cancel is not visible in edit mode");
        tap(cancel, "Cancel");
        assertMessage(MSG_CHANGES_DISCARDED, "Cancel");
        waitForNormalMode("Cancel");
        ReportLogger.pass("Cancel closed edit mode and Mail my order is back");

        OrderTable table = readOrderTable();
        assertTentativeMatches(w0, table, "after Cancel");
        ReportLogger.pass("Tentative weights are back to the pre-edit values");

        openAdjustWeightage();
        assertLockHint("after Cancel (locks must be discarded with the snapshot)");
        assertWeightsUnchanged(w0, readAllWeights(), "reopening Adjust Weightage after Cancel");
        ReportLogger.pass("Reopened edit mode has no locks and the original weights");
        cancelEditIfOpen();
    }

    /** Confirm applies the weights to Your Orders, recalculates shares/values/amount and keeps locks. */
    public void verifyConfirmAppliesWeightsAndRecalculatesOrder() {
        ReportLogger.step("Reading the generated order before editing");
        OrderTable before = readOrderTable();
        learnExcludedFromOrder(before);
        assertOrderConsistent(before, "generated order");

        openAdjustWeightage();
        requireStocks(3);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        // Adjust the two cheapest stocks so the change is large enough to move whole shares.
        List<String> byPrice = new ArrayList<>(w0.keySet());
        byPrice.sort(Comparator.comparingDouble(c -> before.rows.get(c).price));
        String a = byPrice.get(0);
        String b = byPrice.get(1);
        if (!w0.containsKey(a) || !w0.containsKey(b)) {
            throw new AssertionError("Order rows and edit rows differ: " + before.rows.keySet() + " vs " + w0.keySet());
        }
        Set<String> locked = new LinkedHashSet<>();
        Map<String, Double> beforeFirst = w0;
        stepAndVerify(a, true, beforeFirst, locked);
        locked.add(a);
        Map<String, Double> current = readAllWeights();
        double stepSize = current.get(a) - beforeFirst.get(a);
        double rupeesPerStep = before.amountInvested * stepSize / 100.0;
        int neededTaps = (int) Math.max(1, Math.ceil(1.5 * before.rows.get(a).price / rupeesPerStep));
        int tapsForA = Math.min(MAX_REALLOCATION_TAPS, neededTaps);
        if (neededTaps > MAX_REALLOCATION_TAPS) {
            ReportLogger.info("Cheapest stock " + a + " needs about " + neededTaps + " taps to move a whole share;"
                    + " capped at " + MAX_REALLOCATION_TAPS + " (raise ipc.amount if shares do not change)");
        }
        ReportLogger.info("Step " + round2(stepSize) + "% ≈ ₹" + round2(rupeesPerStep) + "; " + a + " (₹"
                + round2(before.rows.get(a).price) + ") gets " + tapsForA + " + taps in total");
        // Intermediate taps only check that a's weight rises; the full table is checked after the last one.
        double expectedA = current.get(a);
        for (int t = 1; t < tapsForA; t++) {
            StepOutcome out = step(a, true);
            if (out.popup != null || out.after - out.before < SAME_EPS) {
                throw new AssertionError("+ on " + a + " (tap " + (t + 1) + ") did not raise its weight"
                        + (out.popup != null ? ": '" + out.popup + "'" : ""));
            }
            expectedA = out.after;
        }
        if (tapsForA > 1) {
            Map<String, Double> afterTaps = readAllWeights();
            assertClose(afterTaps.get(a), expectedA, SAME_EPS, a + " weight after " + tapsForA + " + taps");
            assertTotal(afterTaps, "after " + tapsForA + " + taps on " + a);
            current = afterTaps;
            ReportLogger.pass(a + " raised to " + round2(expectedA) + "% with " + tapsForA + " + taps; total still 100");
        }
        stepAndVerify(b, false, current, locked);
        locked.add(b);
        Map<String, Double> finalWeights = readAllWeights();
        assertLockBar(2, total, "before Confirm");
        ReportLogger.info("Adjusted weights before Confirm: " + finalWeights);

        UiNode confirm = find(snapshot(), CONFIRM);
        if (confirm == null) throw new AssertionError("Confirm is not visible in edit mode");
        tap(confirm, "Confirm");
        assertMessage(MSG_CHANGES_APPLIED, "Confirm");
        waitForNormalMode("Confirm");

        OrderTable after = readOrderTable();
        assertTentativeMatches(finalWeights, after, "after Confirm");
        ReportLogger.pass("Your Orders shows the adjusted tentative weights");
        assertOrderConsistent(after, "confirmed order");

        if (Math.abs(after.amountInvested - before.amountInvested) < 0.009) {
            throw new AssertionError("Amount invested did not change after Confirm: before=₹" + before.amountInvested
                    + " after=₹" + after.amountInvested);
        }
        List<String> sharesChanged = new ArrayList<>();
        for (String company : after.rows.keySet()) {
            OrderRow was = before.rows.get(company);
            OrderRow now = after.rows.get(company);
            if (was != null && was.shares != null && now.shares != null && Math.abs(was.shares - now.shares) > SAME_EPS) {
                sharesChanged.add(company + " " + fmt(was.shares) + "→" + fmt(now.shares));
            }
        }
        if (sharesChanged.isEmpty()) {
            throw new AssertionError("No share count changed after Confirm: the weight change on " + a + " / " + b
                    + " was smaller than one share at ₹" + round2(before.amountInvested) + " invested");
        }
        ReportLogger.pass("Amount invested changed ₹" + round2(before.amountInvested) + " → ₹" + round2(after.amountInvested)
                + "; shares changed: " + sharesChanged);

        ReportLogger.info("Lock persistence after Confirm is checked separately by IPC_012");
    }

    /**
     * PRD v1.1 "Locks stay in place after Confirm", as agreed for this suite: the weights set while
     * stocks were locked stay in place after Confirm, while the locks themselves belong to one Adjust
     * Weightage session, so reopening Adjust Weightage shows no locks. (Equal Weightage, ticking or
     * unticking a stock, or generating again would change the weights; none of those happen here.)
     */
    public void verifyWeightsKeptAndLocksResetAfterConfirm() {
        requireStocks(4);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        String a = tableOrder.get(0);
        String b = tableOrder.get(1);
        String c = tableOrder.get(2);
        Map<String, Double> adjusted = stepAndVerify(a, true, w0, Collections.emptySet());
        tapPadlock(b);
        assertLockBar(2, total, "before Confirm (" + a + " auto-locked, " + b + " locked with its padlock)");

        UiNode confirm = find(snapshot(), CONFIRM);
        if (confirm == null) throw new AssertionError("Confirm is not visible in edit mode");
        tap(confirm, "Confirm");
        assertMessage(MSG_CHANGES_APPLIED, "Confirm");
        waitForNormalMode("Confirm");

        openCustomizeAndVerifyOptions();
        openAdjustWeightage();
        Map<String, Double> reopened = readAllWeights();
        assertWeightsUnchanged(adjusted, reopened, "Confirm and reopening Adjust Weightage");
        ReportLogger.pass("Weights set before Confirm are unchanged after reopening (" + a + " = "
                + round2(reopened.get(a)) + ", " + b + " = " + round2(reopened.get(b)) + ")");

        LockBar bar = readLockBar();
        if (!bar.hint) {
            throw new ProductBehaviourError("Reopening Adjust Weightage after Confirm should start with no locks, but the"
                    + " lock bar shows " + bar);
        }
        ReportLogger.pass("Reopened Adjust Weightage starts with no locks ('Changing a weight locks that stock')");

        // Behaviour check: with no locks, a change to another stock is shared by every other stock,
        // including the two that were locked before Confirm.
        stepAndVerify(c, true, reopened, Collections.emptySet());
        ReportLogger.pass(a + " and " + b + " take part in the next change, so their earlier locks are really gone");
        cancelEditIfOpen();
    }

    /** Equal Weightage resets every included stock to equal weight and clears all locks. */
    public void verifyEqualWeightageResetsWeightsAndLocks() {
        requireStocks(3);
        Map<String, Double> w0 = readAllWeights();
        int total = w0.size();
        String a = tableOrder.get(0);
        stepAndVerify(a, true, w0, Collections.emptySet());
        tapPadlock(tableOrder.get(1));
        assertLockBar(2, total, "before Confirm");
        UiNode confirm = find(snapshot(), CONFIRM);
        if (confirm == null) throw new AssertionError("Confirm is not visible in edit mode");
        tap(confirm, "Confirm");
        assertMessage(MSG_CHANGES_APPLIED, "Confirm");
        waitForNormalMode("Confirm");

        openCustomizeAndVerifyOptions();
        final String[] resetMessage = new String[1];
        tapSheetOption(EQUAL_WEIGHTAGE, "Equal Weightage",
                () -> (resetMessage[0] = firstLabelContaining(snapshot(), MSG_RESET_EQUAL)) != null);
        ReportLogger.pass("Equal Weightage showed: " + resetMessage[0]);
        if (isEditModeVisible()) {
            ReportLogger.info("Equal Weightage left an edit session open; confirming it");
            UiNode confirmAgain = find(snapshot(), CONFIRM);
            if (confirmAgain != null) tap(confirmAgain, "Confirm (after Equal Weightage)");
        }
        waitForNormalMode("Equal Weightage");

        OrderTable table = readOrderTable();
        Map<String, Double> tentative = new LinkedHashMap<>();
        for (Map.Entry<String, OrderRow> row : table.rows.entrySet()) {
            if (excludedCompanies != null && excludedCompanies.contains(row.getKey())) continue;
            if (row.getValue().tentative == null) {
                throw new AssertionError("Tentative weight not read for " + row.getKey());
            }
            tentative.put(row.getKey(), row.getValue().tentative);
        }
        assertTotal(tentative, "after Equal Weightage");
        assertEqualWeights(tentative, "after Equal Weightage");
        assertOrderConsistent(table, "order after Equal Weightage");
        ReportLogger.pass("Equal Weightage reset all " + tentative.size() + " stocks to equal weight");

        openCustomizeAndVerifyOptions();
        openAdjustWeightage();
        assertLockHint("after Equal Weightage (locks must be cleared)");
        ReportLogger.pass("Equal Weightage cleared every lock");
        cancelEditIfOpen();
    }

    // =========================================================
    // STEP / LOCK ACTIONS
    // =========================================================

    private Map<String, Double> stepAndVerify(String company, boolean increase, Map<String, Double> before, Set<String> lockedBefore) {
        StepOutcome out = step(company, increase);
        if (out.popup != null) {
            throw new AssertionError((increase ? "+" : "–") + " on " + company + " was refused: '" + out.popup + "'");
        }
        Map<String, Double> after = readAllWeights();
        assertRedistribution(before, after, company, increase, lockedBefore);
        return after;
    }

    private StepOutcome step(String company, boolean increase) {
        String sign = increase ? "+" : "–";
        clearRebalancePopup();
        EditRow row = revealRow(company);
        final double before = row.weight;
        ReportLogger.step("Tapping " + sign + " on " + company + " (weight " + fmt(before) + ")");
        tap(increase ? row.plus : row.minus, sign + " on " + company);

        waitUntil(() -> {
            List<UiNode> nodes = snapshot();
            if (rebalanceMessage(nodes) != null) return true;
            EditRow current = visibleRows(nodes).get(company);
            return current != null && Math.abs(current.weight - before) > SAME_EPS;
        }, ACTION_WAIT_MS);

        List<UiNode> nodes = settledSnapshot();
        StepOutcome out = new StepOutcome();
        out.before = before;
        out.popup = rebalanceMessage(nodes);
        Map<String, EditRow> rows = visibleRows(nodes);
        for (EditRow r : rows.values()) out.visible.put(r.company, r.weight);
        EditRow current = rows.get(company);
        if (current == null) {
            // The "Please note" popup can cover the bottom rows; close it, then read the weight.
            if (out.popup != null) clearRebalancePopup();
            current = revealRow(company);
        }
        out.after = current.weight;
        ReportLogger.info(company + " " + fmt(before) + " → " + fmt(out.after)
                + (out.popup != null ? " | message: " + out.popup : ""));
        return out;
    }

    private void tapPadlock(String company) {
        clearRebalancePopup();
        EditRow row = revealRow(company);
        LockBar barBefore = lockBarIfVisible(snapshot());
        tap(row.lock, "padlock of " + company);
        List<UiNode> after = settledSnapshot();
        LockBar barAfter = lockBarIfVisible(after);
        if (barBefore != null && barAfter != null && barBefore.locked == barAfter.locked) {
            ReportLogger.debug("Lock bar did not change after the padlock tap on " + company + "; tapping once more");
            EditRow fresh = revealRow(company);
            tap(fresh.lock, "padlock of " + company + " (retry)");
            barAfter = lockBarIfVisible(settledSnapshot());
            if (barAfter != null && barAfter.locked == barBefore.locked) {
                throw new AssertionError("Padlock tap on " + company + " did not change the lock bar (" + barBefore + ")");
            }
        }
        ReportLogger.step("Toggled the padlock of " + company + (barAfter != null ? " | bar: " + barAfter : ""));
    }

    /**
     * Locks many stocks in one top-to-bottom pass: every target row visible on a screen is locked before
     * scrolling on (36 single reveal-and-tap cycles would take minutes on Recommendation "All").
     * The final lock-bar count, asserted by the caller, proves every tap registered.
     */
    private void lockWithPadlocks(Collection<String> companies, Set<String> locked) {
        List<String> pending = new ArrayList<>(companies);
        pending.removeAll(locked);
        if (pending.size() > 3) {
            clearRebalancePopup();
            scrollToTop();
            ensureStepperColumnVisible();
            for (int drag = 0; drag <= MAX_VERTICAL_DRAGS && !pending.isEmpty(); drag++) {
                List<UiNode> nodes = settledSnapshot();
                Map<String, EditRow> rows = tappableRows(nodes);
                boolean tapped = false;
                for (EditRow row : rows.values()) {
                    if (!pending.contains(row.company)) continue;
                    tap(row.lock, "padlock of " + row.company);
                    pending.remove(row.company);
                    locked.add(row.company);
                    tapped = true;
                    waitForSettled();
                }
                if (pending.isEmpty()) break;
                if (!tapped && !rows.isEmpty() && !dragTableBy(true)) break;
                if (tapped && !dragTableBy(true)) break;
            }
        }
        for (String company : new ArrayList<>(pending)) {
            tapPadlock(company);
            locked.add(company);
        }
        ReportLogger.pass("Locked " + companies.size() + " stocks with padlocks: " + companies);
    }

    private void cancelEditIfOpen() {
        UiNode cancel = find(snapshot(), CANCEL);
        if (cancel != null && isEditModeVisible()) {
            tap(cancel, "Cancel (leave edit mode)");
            waitUntil(() -> !isEditModeVisible(), ACTION_WAIT_MS);
        }
    }

    // =========================================================
    // WEIGHT TABLE (edit mode)
    // =========================================================

    /**
     * Reads every ticked row's tentative weight by scrolling the table top to bottom; refreshes
     * tableOrder. The first read of a case also learns which rows are unticked (no stepper, or weight 0
     * in the fresh order) and checks the row count against the lock bar.
     */
    public Map<String, Double> readAllWeights() {
        if (cachedWeights != null && cachedWeightsVersion == tapVersion) {
            tableOrder.clear();
            tableOrder.addAll(cachedWeights.keySet());
            return new LinkedHashMap<>(cachedWeights);
        }
        clearRebalancePopup();
        scrollToTop();
        ensureStepperColumnVisible();
        boolean learning = excludedCompanies == null;
        Map<String, Double> weights = new LinkedHashMap<>();
        Set<String> allCompanies = new LinkedHashSet<>();
        int noNewRows = 0;
        String previous = "";
        for (int drag = 0; drag <= MAX_VERTICAL_DRAGS; drag++) {
            List<UiNode> nodes = settledSnapshot();
            int sizeBefore = allCompanies.size() + weights.size();
            Band band = band(nodes);
            for (UiNode c : companyNodes(nodes, band, hsvLeftOf(nodes))) allCompanies.add(c.label);
            for (EditRow row : visibleRows(nodes).values()) {
                if (excludedCompanies == null || !excludedCompanies.contains(row.company)) weights.put(row.company, row.weight);
            }
            if (!learning && includedCount != null && weights.size() >= includedCount) break;
            String fingerprint = fingerprint(nodes);
            noNewRows = allCompanies.size() + weights.size() == sizeBefore ? noNewRows + 1 : 0;
            if (noNewRows >= 2 || fingerprint.equals(previous)) break;
            previous = fingerprint;
            if (!dragTableBy(true)) break;
        }
        if (weights.isEmpty()) {
            throw new AssertionError("No weight rows (padlock, –, weight, +) were found in edit mode; visible=" + visibleLabels());
        }
        if (learning) learnExcluded(weights, allCompanies);
        weights.keySet().removeAll(excludedCompanies);
        if (includedCount != null && weights.size() != includedCount) {
            throw new AssertionError("Read " + weights.size() + " ticked rows but the order has " + includedCount
                    + " ticked stocks: " + weights.keySet());
        }
        tableOrder.clear();
        tableOrder.addAll(weights.keySet());
        checkLockBarTotal(readLockBar(), weights.size());
        cachedWeights = new LinkedHashMap<>(weights);
        cachedWeightsVersion = tapVersion;
        return weights;
    }

    /** First fresh read: rows without a stepper, or at 0 while the rest share the weight, are unticked. */
    private void learnExcluded(Map<String, Double> weights, Set<String> allCompanies) {
        Set<String> excluded = new LinkedHashSet<>();
        for (String company : allCompanies) if (!weights.containsKey(company)) excluded.add(company);
        boolean anyPositive = false;
        for (double w : weights.values()) if (w > SAME_EPS) anyPositive = true;
        if (anyPositive) {
            for (Map.Entry<String, Double> e : weights.entrySet()) if (e.getValue() <= SAME_EPS) excluded.add(e.getKey());
        }
        excludedCompanies = excluded;
        int ticked = 0;
        for (String company : weights.keySet()) if (!excluded.contains(company)) ticked++;
        includedCount = ticked;
        if (!excluded.isEmpty()) {
            ReportLogger.info(includedCount + " ticked stocks are checked; " + excluded.size()
                    + " unticked stocks (weight 0) are left out of weight checks: " + excluded);
        }
    }

    /** Learns from the generated order table when it is read before edit mode (IPC_010). */
    private void learnExcludedFromOrder(OrderTable table) {
        if (excludedCompanies != null) return;
        Set<String> excluded = new LinkedHashSet<>();
        for (OrderRow row : table.rows.values()) {
            boolean zero = row.tentative != null && row.tentative <= SAME_EPS && (row.shares == null || row.shares <= SAME_EPS);
            if (zero) excluded.add(row.company);
        }
        excludedCompanies = excluded;
        includedCount = table.rows.size() - excluded.size();
        if (!excluded.isEmpty()) {
            ReportLogger.info(includedCount + " ticked stocks in the order; " + excluded.size() + " unticked stocks: " + excluded);
        }
    }

    /** The bar's "of m" is either the ticked count or all rows; learn which, then hold it to that. */
    private void checkLockBarTotal(LockBar bar, int ticked) {
        if (bar.total == null) return;
        int all = ticked + (excludedCompanies == null ? 0 : excludedCompanies.size());
        if (barCountsExcluded == null) {
            if (bar.total == ticked) barCountsExcluded = false;
            else if (bar.total == all) barCountsExcluded = true;
            else {
                throw new AssertionError("Lock bar reports " + bar.total + " stocks but " + ticked + " ticked rows were read"
                        + (all != ticked ? " (" + all + " rows including unticked)" : "") + ": " + tableOrder);
            }
            if (barCountsExcluded && all != ticked) {
                ReportLogger.info("Lock bar counts all " + all + " rows (including unticked) in 'of m'");
            }
            return;
        }
        int expected = barCountsExcluded ? all : ticked;
        if (bar.total != expected) {
            throw new AssertionError("Lock bar reports " + bar.total + " stocks but " + expected + " were expected");
        }
    }

    private int hsvLeftOf(List<UiNode> nodes) {
        UiNode hsv = horizontalScroll(nodes);
        return hsv != null ? hsv.left : (int) (screen().getWidth() * 0.45);
    }

    /** Brings a row fully inside the safe band (below the sticky header, above popups and Cancel/Confirm). */
    private EditRow revealRow(String company) {
        int targetIndex = tableOrder.indexOf(company);
        for (int attempt = 0; attempt < MAX_VERTICAL_DRAGS + 2; attempt++) {
            List<UiNode> nodes = settledSnapshot();
            Map<String, EditRow> rows = tappableRows(nodes);
            EditRow row = rows.get(company);
            if (row != null) return row;
            if (rows.isEmpty()) {
                ensureStepperColumnVisible();
                continue;
            }
            int minIndex = Integer.MAX_VALUE;
            int maxIndex = -1;
            for (String visible : rows.keySet()) {
                int i = tableOrder.indexOf(visible);
                if (i >= 0) {
                    minIndex = Math.min(minIndex, i);
                    maxIndex = Math.max(maxIndex, i);
                }
            }
            boolean towardsEnd = targetIndex < 0 || maxIndex < 0 || targetIndex > maxIndex;
            if (targetIndex >= 0 && minIndex != Integer.MAX_VALUE && targetIndex < minIndex) towardsEnd = false;
            if (!dragTableBy(towardsEnd) && rebalancePopupNode(snapshot()) != null) {
                // End of the table reached and the row is hidden behind the popup.
                clearRebalancePopup();
            }
        }
        throw new AssertionError("Row '" + company + "' could not be brought into view; visible=" + visibleLabels());
    }

    /** Rows whose padlock, –, weight, + and company name can be read. */
    private Map<String, EditRow> visibleRows(List<UiNode> nodes) {
        return visibleRows(nodes, false);
    }

    /** Rows whose padlock, – and + can be tapped (also clear of the floating button). */
    private Map<String, EditRow> tappableRows(List<UiNode> nodes) {
        return visibleRows(nodes, true);
    }

    private Map<String, EditRow> visibleRows(List<UiNode> nodes, boolean forTap) {
        Band band = band(nodes, forTap);
        UiNode hsv = horizontalScroll(nodes);
        int hsvLeft = hsv != null ? hsv.left : (int) (screen().getWidth() * 0.45);
        int hsvRight = hsv != null ? hsv.right : screen().getWidth();

        List<UiNode> minus = new ArrayList<>();
        for (UiNode n : nodes) {
            if (MINUS.matcher(n.label).matches() && band.contains(n) && n.left >= hsvLeft - 2 && n.right <= hsvRight + 2) {
                minus.add(n);
            }
        }
        minus.sort(Comparator.comparingInt(UiNode::cy));
        int half = halfRow(minus);
        List<UiNode> companies = companyNodes(nodes, band(nodes, false), hsvLeft);

        Map<String, EditRow> rows = new LinkedHashMap<>();
        for (UiNode m : minus) {
            UiNode plus = nearestInRow(nodes, m, half, n -> PLUS.matcher(n.label).matches() && n.cx() > m.cx());
            if (plus == null || !band.contains(plus) || plus.right > hsvRight + 2) continue;
            UiNode value = nearestInRow(nodes, m, half, n -> WEIGHT_VALUE.matcher(n.label).matches()
                    && n.cx() > m.cx() && n.cx() < plus.cx());
            UiNode lock = nearestInRow(nodes, m, half, n -> n.label.isEmpty() && n.cls.endsWith("ImageView")
                    && n.cx() < m.cx() && n.left >= hsvLeft - 2);
            UiNode company = null;
            for (UiNode c : companies) {
                if (Math.abs(c.cy() - m.cy()) <= half * 1.2
                        && (company == null || Math.abs(c.cy() - m.cy()) < Math.abs(company.cy() - m.cy()))) {
                    company = c;
                }
            }
            if (value == null || lock == null || company == null || !band.contains(lock)) continue;
            rows.put(company.label, new EditRow(company.label, parseNumber(value.label), lock, m, plus));
        }
        return rows;
    }

    private void ensureStepperColumnVisible() {
        for (int attempt = 0; attempt < 3; attempt++) {
            List<UiNode> nodes = settledSnapshot();
            if (!visibleRows(nodes).isEmpty()) return;
            UiNode hsv = horizontalScroll(nodes);
            if (hsv == null) return;
            ReportLogger.debug("Weight stepper column is not fully visible; scrolling the table to its first column");
            dragHorizontal(nodes, false);
        }
    }

    // =========================================================
    // LOCK BAR
    // =========================================================

    private LockBar readLockBar() {
        if (cachedBar != null && cachedBarTapVersion == tapVersion && cachedBarViewVersion == viewVersion) return cachedBar;
        LockBar bar = lockBarIfVisible(settledSnapshot());
        if (bar == null) {
            scrollToTop();
            bar = lockBarIfVisible(settledSnapshot());
        }
        if (bar == null) throw new AssertionError("Lock bar is not shown in Adjust Weightage; visible=" + visibleLabels());
        cachedBar = bar;
        cachedBarTapVersion = tapVersion;
        cachedBarViewVersion = viewVersion;
        return bar;
    }

    private LockBar lockBarIfVisible(List<UiNode> nodes) {
        UiNode hint = find(nodes, LOCK_HINT);
        if (hint != null) return new LockBar(true, 0, null, find(nodes, UNLOCK_ALL));
        UiNode count = find(nodes, LOCK_COUNT);
        if (count == null) return null;
        Matcher m = LOCK_COUNT.matcher(count.label);
        if (!m.matches()) return null;
        return new LockBar(false, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), find(nodes, UNLOCK_ALL));
    }

    private void assertLockBar(int expectedLocked, int expectedTotal, String context) {
        LockBar bar = readLockBar();
        if (!bar.hint && bar.total != null && barCountsExcluded == null) checkLockBarTotal(bar, expectedTotal);
        int unticked = excludedCompanies == null ? 0 : excludedCompanies.size();
        if (Boolean.TRUE.equals(barCountsExcluded)) expectedTotal += unticked;
        if (bar.hint || bar.locked != expectedLocked || bar.total == null || bar.total != expectedTotal) {
            throw new AssertionError("Lock bar " + context + ": expected '" + expectedLocked + " of " + expectedTotal
                    + " stocks locked' but found " + bar);
        }
        if (bar.unlockAll == null) throw new AssertionError("Unlock all is not shown " + context);
        ReportLogger.pass("Lock bar reads '" + expectedLocked + " of " + expectedTotal + " stocks locked' with Unlock all " + context);
    }

    private void assertLockHint(String context) {
        LockBar bar = readLockBar();
        if (!bar.hint) {
            throw new AssertionError("Lock bar " + context + ": expected 'Changing a weight locks that stock' but found " + bar);
        }
        ReportLogger.pass("Lock bar reads 'Changing a weight locks that stock' " + context);
    }

    // =========================================================
    // MESSAGES / POPUPS
    // =========================================================

    private String rebalanceMessage(List<UiNode> nodes) {
        for (UiNode n : nodes) {
            String all = (n.label + " " + n.text).toLowerCase(Locale.ROOT);
            for (String message : REBALANCE_MESSAGES) {
                if (all.contains(message)) return n.label.isEmpty() ? n.text : n.label;
            }
        }
        return null;
    }

    private UiNode rebalancePopupNode(List<UiNode> nodes) {
        for (UiNode n : nodes) {
            String all = (n.label + " " + n.text).toLowerCase(Locale.ROOT);
            for (String message : REBALANCE_MESSAGES) {
                if (all.contains(message)) return n;
            }
        }
        return null;
    }

    /** Waits for the "Please note" rebalance popup to close; uses its close (x) when it stays. */
    private void clearRebalancePopup() {
        clearOverlay(this::rebalancePopupNode, "rebalance popup", true);
    }

    /**
     * Clears any "Please note" toast (e.g. "Changes discarded", "Changes applied") before tapping
     * Customize or a Customize-sheet option: the toast sits exactly over the sheet's lower rows and
     * swallows the tap (run evidence 2026-11-01: Adjust Weightage tapped under "Changes discarded").
     */
    private void clearNotes() {
        clearOverlay(nodes -> find(nodes, PLEASE_NOTE), "'Please note' message", false);
    }

    private void clearOverlay(java.util.function.Function<List<UiNode>, UiNode> finder, String what, boolean mustClose) {
        if (finder.apply(snapshot()) == null) return;
        // Its text has already been read and checked, so close it at once instead of waiting it out.
        // The close (x) has no node of its own; tap its position relative to the live popup bounds.
        // The floating scroll-to-top button can sit on the same spot: then the first tap only scrolls
        // the page to the top (hiding that button) and the second tap reaches the (x).
        for (int attempt = 1; attempt <= 2; attempt++) {
            UiNode popup = finder.apply(snapshot());
            if (popup == null) return;
            int x = popup.right - (int) Math.round(popup.width() * 0.075);
            int y = popup.top + (int) Math.round(popup.height() * 0.24);
            ReportLogger.debug("Closing the " + what + " with its (x), attempt " + attempt);
            tapPoint(x, y, what + " close (x)");
            if (waitUntil(() -> finder.apply(snapshot()) == null, POPUP_AUTO_CLOSE_MS)) return;
        }
        UiNode still = finder.apply(snapshot());
        if (still != null) tap(still, what);
        if (!waitUntil(() -> finder.apply(snapshot()) == null, ACTION_WAIT_MS)) {
            if (mustClose) {
                throw new AssertionError("The " + what + " did not close, so a new popup cannot be verified reliably");
            }
            ReportLogger.debug("The " + what + " is still shown; continuing");
        }
    }

    private void assertPopup(StepOutcome out, String expected, String context) {
        if (out.popup == null) {
            throw new AssertionError("Expected message '" + expected + "' after " + context
                    + " but none was shown; weight " + fmt(out.before) + " → " + fmt(out.after));
        }
        if (!out.popup.toLowerCase(Locale.ROOT).contains(expected)) {
            throw new AssertionError("Wrong message after " + context + ": expected '" + expected
                    + "' but found '" + out.popup + "'");
        }
        ReportLogger.pass("Message shown after " + context + ": " + out.popup);
    }

    /** Polls quickly for a short-lived toast/snackbar (content-desc or text, contains match). */
    private void assertMessage(String expected, String action) {
        final String[] seen = new String[1];
        boolean found = waitUntil(() -> (seen[0] = firstLabelContaining(snapshot(), expected)) != null, MESSAGE_WAIT_MS);
        if (!found) {
            throw new AssertionError("'" + expected + "' message was not shown after " + action + "; visible=" + visibleLabels());
        }
        ReportLogger.pass(action + " showed: " + seen[0]);
    }

    // =========================================================
    // YOUR ORDERS TABLE (normal mode)
    // =========================================================

    private boolean isEditModeVisible() {
        List<UiNode> nodes = snapshot();
        return find(nodes, CONFIRM) != null && find(nodes, CANCEL) != null
                && (any(nodes, LOCK_HINT) || any(nodes, LOCK_COUNT) || any(nodes, MINUS));
    }

    private void waitForNormalMode(String action) {
        if (!waitUntil(() -> {
            List<UiNode> nodes = snapshot();
            return find(nodes, MAIL_MY_ORDER) != null && find(nodes, CONFIRM) == null;
        }, PAGE_WAIT_MS)) {
            throw new AssertionError("Your Orders did not return to normal mode (Mail my order) after " + action
                    + "; visible=" + visibleLabels());
        }
    }

    /** Reads Amount invested and every row's Tentative wt., Price, Shares, Actual wt. and Value. */
    public OrderTable readOrderTable() {
        scrollToTop();
        OrderTable table = new OrderTable();
        table.amountInvested = readAmountInvested(settledSnapshot());

        for (int i = 0; i < 3; i++) {
            List<UiNode> nodes = settledSnapshot();
            if (find(nodes, TENTATIVE_HEADER) != null) break;
            dragHorizontal(nodes, false);
        }
        for (int position = 0; position <= MAX_HORIZONTAL_DRAGS; position++) {
            readColumnsVertically(table);
            if (table.allColumnsRead()) break;
            scrollToTop();
            List<UiNode> nodes = settledSnapshot();
            String before = fingerprint(nodes);
            dragHorizontal(nodes, true);
            if (fingerprint(settledSnapshot()).equals(before)) break;
        }
        if (table.rows.isEmpty()) throw new AssertionError("No order rows were read on Your Orders");
        if (!table.allColumnsRead()) {
            throw new AssertionError("Could not read every order column " + table.missingColumns() + " for rows " + table.rows.keySet());
        }
        // Return to the first column so later steps start from the default view.
        for (int i = 0; i < 3; i++) dragHorizontal(settledSnapshot(), false);
        ReportLogger.info("Your Orders: amount invested ₹" + round2(table.amountInvested) + " | rows " + table.rows.values());
        return table;
    }

    private void readColumnsVertically(OrderTable table) {
        scrollToTop();
        List<UiNode> top = settledSnapshot();
        Map<String, UiNode> headers = visibleHeaders(top);
        if (headers.isEmpty()) throw new AssertionError("Order table headers are not visible; visible=" + abbreviate(labelsOf(top), 25));
        int noNewCells = 0;
        String previous = "";
        for (int drag = 0; drag <= MAX_VERTICAL_DRAGS; drag++) {
            List<UiNode> nodes = drag == 0 ? top : settledSnapshot();
            Map<String, UiNode> liveHeaders = visibleHeaders(nodes);
            Map<String, UiNode> columnHeaders = liveHeaders.isEmpty() ? headers : liveHeaders;
            int before = table.filledCells();
            Band band = band(nodes);
            UiNode hsv = horizontalScroll(nodes);
            int hsvLeft = hsv != null ? hsv.left : (int) (screen().getWidth() * 0.45);
            List<UiNode> companies = companyNodes(nodes, band, hsvLeft);
            int half = halfRow(companies);
            for (UiNode company : companies) {
                OrderRow row = table.rows.computeIfAbsent(company.label, OrderRow::new);
                for (UiNode cell : nodes) {
                    if (!NUMERIC_CELL.matcher(cell.label).matches() || cell.cx() < hsvLeft || !band.contains(cell)) continue;
                    if (Math.abs(cell.cy() - company.cy()) > half * 1.2) continue;
                    String column = nearestHeader(columnHeaders, cell);
                    if (column != null) row.set(column, parseNumber(cell.label));
                }
            }
            String fingerprint = fingerprint(nodes);
            // Rows are already known after the first column pass, so progress = newly read cells.
            noNewCells = table.filledCells() == before ? noNewCells + 1 : 0;
            if (noNewCells >= 2 || fingerprint.equals(previous)) break;
            previous = fingerprint;
            if (!dragTableBy(true)) break;
        }
    }

    private Map<String, UiNode> visibleHeaders(List<UiNode> nodes) {
        Map<String, UiNode> headers = new LinkedHashMap<>();
        putHeader(headers, nodes, TENTATIVE_HEADER, OrderRow.TENTATIVE);
        putHeader(headers, nodes, PRICE_HEADER, OrderRow.PRICE);
        putHeader(headers, nodes, SHARES_HEADER, OrderRow.SHARES);
        putHeader(headers, nodes, ACTUAL_HEADER, OrderRow.ACTUAL);
        putHeader(headers, nodes, VALUE_HEADER, OrderRow.VALUE);
        return headers;
    }

    private void putHeader(Map<String, UiNode> headers, List<UiNode> nodes, Pattern pattern, String key) {
        UiNode node = find(nodes, pattern);
        if (node != null && node.left >= 0 && node.right <= screen().getWidth()) headers.put(key, node);
    }

    private String nearestHeader(Map<String, UiNode> headers, UiNode cell) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, UiNode> header : headers.entrySet()) {
            int distance = Math.abs(header.getValue().cx() - cell.cx());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = header.getKey();
            }
        }
        return bestDistance <= screen().getWidth() * 0.15 ? best : null;
    }

    private double readAmountInvested(List<UiNode> nodes) {
        UiNode label = find(nodes, AMOUNT_INVESTED);
        if (label == null) throw new AssertionError("Amount invested is not visible; visible=" + abbreviate(labelsOf(nodes), 20));
        UiNode best = null;
        for (UiNode n : nodes) {
            if (n.label.contains("\u20B9") && n.cy() > label.cy() && n.cy() - label.cy() < screen().getHeight() * 0.12
                    && (best == null || n.cy() < best.cy())) {
                best = n;
            }
        }
        if (best == null) throw new AssertionError("Amount invested value (₹) not found under its label");
        return parseNumber(best.label);
    }

    // =========================================================
    // ASSERTIONS
    // =========================================================

    private void assertRedistribution(Map<String, Double> before, Map<String, Double> after, String target,
                                      boolean increase, Set<String> lockedBefore) {
        if (!before.keySet().equals(after.keySet())) {
            throw new AssertionError("Row set changed: before=" + before.keySet() + " after=" + after.keySet());
        }
        double delta = after.get(target) - before.get(target);
        if (increase ? delta <= SAME_EPS : delta >= -SAME_EPS) {
            throw new AssertionError((increase ? "+" : "–") + " on " + target + " did not " + (increase ? "raise" : "lower")
                    + " its weight: " + before.get(target) + " → " + after.get(target));
        }
        List<String> unlockedOthers = new ArrayList<>();
        for (String company : before.keySet()) {
            if (company.equals(target)) continue;
            if (lockedBefore.contains(company)) {
                assertClose(after.get(company), before.get(company), SAME_EPS, "locked " + company + " weight");
            } else {
                unlockedOthers.add(company);
            }
        }
        if (unlockedOthers.isEmpty()) throw new AssertionError("Weight changed although every other stock was locked");
        double expectedShare = -delta / unlockedOthers.size();
        for (String company : unlockedOthers) {
            double share = after.get(company) - before.get(company);
            assertClose(share, expectedShare, SHARE_EPS, "share of the change for " + company);
            if (after.get(company) < -SAME_EPS) throw new AssertionError(company + " went below 0%: " + after.get(company));
        }
        assertTotal(after, "after " + (increase ? "+" : "–") + " on " + target);
        ReportLogger.pass(target + " " + fmt(before.get(target)) + " → " + fmt(after.get(target)) + "; change "
                + round2(-delta) + " shared equally (" + round2(expectedShare) + " each) by " + unlockedOthers.size()
                + " unlocked stocks; " + lockedBefore.size() + " locked stocks unchanged");
    }

    private void assertLockedUnchanged(Map<String, Double> before, Map<String, Double> after, Set<String> locked, String context) {
        for (String company : locked) {
            assertClose(after.get(company), before.get(company), SAME_EPS, "locked " + company + " weight while " + context);
        }
    }

    private void assertWeightsUnchanged(Map<String, Double> before, Map<String, Double> after, String context) {
        if (!before.keySet().equals(after.keySet())) {
            throw new AssertionError("Row set changed during " + context + ": " + before.keySet() + " → " + after.keySet());
        }
        for (String company : before.keySet()) {
            assertClose(after.get(company), before.get(company), SAME_EPS, company + " weight after " + context);
        }
    }

    /** 2-decimal display: each weight can be off by up to 0.005, so the shown total drifts with the count. */
    private static double totalTolerance(int stocks) {
        return Math.max(TOTAL_EPS, 0.005 * stocks + 0.01);
    }

    private void assertTotal(Map<String, Double> weights, String context) {
        double sum = 0;
        for (double w : weights.values()) sum += w;
        if (Math.abs(sum - 100.0) > totalTolerance(weights.size())) {
            throw new AssertionError("Weights must total 100% " + context + " but total " + round2(sum) + ": " + weights);
        }
    }

    private void assertEqualWeights(Map<String, Double> weights, String context) {
        double expected = 100.0 / weights.size();
        // 2-decimal display: the rounding remainder (up to 0.005 per stock) may sit on one stock.
        double tolerance = Math.max(EQUAL_EPS, 0.005 * weights.size() + 0.01);
        boolean unequal = false;
        for (double w : weights.values()) if (Math.abs(w - expected) > tolerance) unequal = true;
        if (unequal) {
            throw new AssertionError("Weights are not equal (" + round2(expected) + " each) " + context + ": " + weights);
        }
    }

    private void assertTentativeMatches(Map<String, Double> expected, OrderTable table, String context) {
        if (!table.rows.keySet().containsAll(expected.keySet())) {
            Set<String> missing = new LinkedHashSet<>(expected.keySet());
            missing.removeAll(table.rows.keySet());
            throw new AssertionError("Order rows missing " + context + ": " + missing);
        }
        for (OrderRow row : table.rows.values()) {
            if (expected.containsKey(row.company)) continue;
            if (row.tentative != null && row.tentative > SAME_EPS) {
                throw new AssertionError(context + ": unticked " + row.company + " has tentative weight " + row.tentative);
            }
        }
        for (Map.Entry<String, Double> e : expected.entrySet()) {
            Double tentative = table.rows.get(e.getKey()).tentative;
            if (tentative == null) throw new AssertionError("Tentative weight not read for " + e.getKey() + " " + context);
            assertClose(tentative, e.getValue(), SAME_EPS, e.getKey() + " tentative weight " + context);
        }
    }

    /** Value = shares × price, Actual wt. = value / amount × 100, Σ value = Amount invested. */
    private void assertOrderConsistent(OrderTable table, String context) {
        double valueSum = 0;
        double actualSum = 0;
        for (OrderRow row : table.rows.values()) {
            double expectedValue = row.shares * row.price;
            if (Math.abs(row.value - expectedValue) > Math.max(0.05, expectedValue * 0.0005)) {
                throw new AssertionError(context + ": " + row.company + " value ₹" + row.value + " ≠ shares " + fmt(row.shares)
                        + " × price ₹" + row.price);
            }
            if (table.amountInvested > 0) {
                double expectedActual = row.value / table.amountInvested * 100.0;
                assertClose(row.actual, expectedActual, SHARE_EPS, context + ": " + row.company + " actual weight");
            }
            valueSum += row.value;
            actualSum += row.actual;
        }
        if (Math.abs(valueSum - table.amountInvested) > 1.0) {
            throw new AssertionError(context + ": Σ value ₹" + round2(valueSum) + " ≠ Amount invested ₹" + round2(table.amountInvested));
        }
        if (Math.abs(actualSum - 100.0) > Math.max(0.25, totalTolerance(table.rows.size()))) {
            throw new AssertionError(context + ": actual weights total " + round2(actualSum) + "% instead of 100%");
        }
        ReportLogger.pass(context + " is consistent: value = shares × price, actual weight = value / amount, Σ value = ₹"
                + round2(table.amountInvested));
    }

    private void requireStocks(int minimum) {
        if (tableOrder.isEmpty()) readAllWeights();
        if (tableOrder.size() < minimum) {
            throw new NotEnoughStocksException("This check needs at least " + minimum + " stocks but the order has "
                    + tableOrder.size() + ": " + tableOrder);
        }
    }

    private static void assertClose(double actual, double expected, double tolerance, String what) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(what + ": expected " + round2(expected) + " but found " + round2(actual)
                    + " (tolerance " + tolerance + ")");
        }
    }

    // =========================================================
    // GEOMETRY HELPERS
    // =========================================================

    /** Safe area for reading rows (the floating button does not hide nodes from the XML). */
    private Band band(List<UiNode> nodes) {
        return band(nodes, false);
    }

    /** Safe area for tapping rows: also stays clear of the floating scroll-to-top button. */
    private Band tapBand(List<UiNode> nodes) {
        return band(nodes, true);
    }

    private Band band(List<UiNode> nodes, boolean forTap) {
        int top = (int) (screen().getHeight() * 0.12);
        UiNode header = find(nodes, COMPANY_HEADER);
        if (header != null) top = Math.max(top, header.bottom + 8);
        UiNode tentative = find(nodes, TENTATIVE_HEADER);
        if (tentative != null) top = Math.max(top, tentative.bottom + 8);
        int bottom = (int) (screen().getHeight() * 0.90);
        for (UiNode n : nodes) {
            boolean overlay = CANCEL.matcher(n.label).matches() || CONFIRM.matcher(n.label).matches()
                    || MAIL_MY_ORDER.matcher(n.label).matches() || PLEASE_NOTE.matcher(n.label).matches();
            if (overlay && n.top > screen().getHeight() * 0.4) bottom = Math.min(bottom, n.top - 8);
            if (forTap && isFloatingButton(n)) bottom = Math.min(bottom, n.top - 8);
        }
        return new Band(top, bottom);
    }

    /**
     * The floating scroll-to-top button (unlabelled ImageView over the right edge of the table, shown
     * after scrolling). It covers the "+" column, so rows level with it are never tapped.
     * Row padlocks are ImageViews too, but they sit left of the middle of the screen.
     */
    private boolean isFloatingButton(UiNode n) {
        return n.label.isEmpty() && n.cls.endsWith("ImageView")
                && n.cx() > screen().getWidth() * 0.75 && n.cy() > screen().getHeight() * 0.5
                && n.width() < screen().getWidth() * 0.2 && n.height() < screen().getHeight() * 0.1;
    }

    /** Company name cells: left column, inside the band, with their "Cap | Sector" line just below. */
    private List<UiNode> companyNodes(List<UiNode> nodes, Band band, int hsvLeft) {
        List<UiNode> companies = new ArrayList<>();
        for (UiNode n : nodes) {
            if (isMessageText(n.label) || !hasSubLineBelow(nodes, n)) continue;
            if (n.label.isEmpty() || n.label.contains("|") || !band.contains(n)) continue;
            if (n.cx() >= hsvLeft || n.cx() < screen().getWidth() * 0.08) continue;
            if (NUMERIC_CELL.matcher(n.label).matches() || MINUS.matcher(n.label).matches() || PLUS.matcher(n.label).matches()) continue;
            if (COMPANY_HEADER.matcher(n.label).matches() || PLEASE_NOTE.matcher(n.label).matches()
                    || LOCK_HINT.matcher(n.label).matches() || LOCK_COUNT.matcher(n.label).matches()
                    || UNLOCK_ALL.matcher(n.label).matches() || n.label.length() > 80) continue;
            companies.add(n);
        }
        companies.sort(Comparator.comparingInt(UiNode::cy));
        return companies;
    }

    private boolean hasSubLineBelow(List<UiNode> nodes, UiNode name) {
        for (UiNode n : nodes) {
            if (n.label.contains("|") && Math.abs(n.left - name.left) < screen().getWidth() * 0.05
                    && n.top >= name.top && n.top - name.bottom < name.height() * 2 + 20) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMessageText(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        if (PLEASE_NOTE.matcher(label).matches()) return true;
        for (String message : new String[]{MSG_CHANGES_APPLIED, MSG_CHANGES_DISCARDED, MSG_RESET_EQUAL,
                MSG_EVERY_OTHER_SET, MSG_OTHERS_AT_ZERO, MSG_ALREADY_ZERO}) {
            if (lower.contains(message)) return true;
        }
        return false;
    }

    private int halfRow(List<UiNode> rowAnchors) {
        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < rowAnchors.size(); i++) {
            int gap = rowAnchors.get(i).cy() - rowAnchors.get(i - 1).cy();
            if (gap > 10) gaps.add(gap);
        }
        if (gaps.isEmpty()) return (int) (screen().getHeight() * 0.03);
        Collections.sort(gaps);
        return Math.max(20, gaps.get(gaps.size() / 2) / 2);
    }

    private UiNode nearestInRow(List<UiNode> nodes, UiNode anchor, int half, java.util.function.Predicate<UiNode> filter) {
        UiNode best = null;
        for (UiNode n : nodes) {
            if (!filter.test(n) || Math.abs(n.cy() - anchor.cy()) > half) continue;
            if (best == null || Math.abs(n.cx() - anchor.cx()) < Math.abs(best.cx() - anchor.cx())) best = n;
        }
        return best;
    }

    private UiNode horizontalScroll(List<UiNode> nodes) {
        UiNode best = null;
        for (UiNode n : nodes) {
            if (n.cls.endsWith("HorizontalScrollView") && (best == null || n.area() > best.area())) best = n;
        }
        return best;
    }

    // =========================================================
    // GESTURES
    // =========================================================

    private void scrollToTop() {
        String previous = fingerprint(snapshot());
        for (int i = 0; i < MAX_VERTICAL_DRAGS; i++) {
            List<UiNode> nodes = snapshot();
            UiNode amount = find(nodes, AMOUNT_INVESTED);
            if (amount != null && amount.top > screen().getHeight() * 0.10) return;
            if (!dragTableBy(false)) return;
            String current = fingerprint(snapshot());
            if (current.equals(previous)) return;
            previous = current;
        }
    }

    /**
     * Slow drag (no fling) in the Company column, never on a stepper. towardsEnd = reveal lower rows.
     *
     * The drag starts well inside the rows: the sticky Cancel/Confirm (or Mail my order) panel
     * is taller than its buttons, and a touch that starts on it does not scroll the list.
     * If the screen does not move, one retry is made from the screen centre.
     * Returns true when the visible content changed.
     */
    private boolean dragTableBy(boolean towardsEnd) {
        List<UiNode> nodes = snapshot();
        String before = fingerprint(nodes);
        Band band = band(nodes);
        int height = Math.max(300, band.bottom - band.top);
        int inner = band.top + (int) (height * 0.80);
        int outer = band.top + (int) (height * 0.15);
        int x = (int) (screen().getWidth() * 0.30);
        if (towardsEnd) performSwipe(x, inner, x, outer, 700L, 250L);
        else performSwipe(x, outer, x, inner, 700L, 250L);
        if (!fingerprint(settledSnapshot()).equals(before)) return true;

        int centerX = (int) (screen().getWidth() * 0.45);
        int high = (int) (screen().getHeight() * 0.40);
        int low = Math.min((int) (screen().getHeight() * 0.68), band.bottom - (int) (screen().getHeight() * 0.06));
        if (low - high < 200) return false;
        ReportLogger.debug("Table did not move; retrying the drag from the screen centre");
        if (towardsEnd) performSwipe(centerX, low, centerX, high, 700L, 250L);
        else performSwipe(centerX, high, centerX, low, 700L, 250L);
        return !fingerprint(settledSnapshot()).equals(before);
    }

    /** Horizontal drag inside the order table, away from the Android edge-back zones. */
    private void dragHorizontal(List<UiNode> nodes, boolean revealRightColumns) {
        Band band = band(nodes);
        UiNode hsv = horizontalScroll(nodes);
        int w = screen().getWidth();
        int left = Math.max((int) (w * 0.58), hsv != null ? hsv.left + 30 : 0);
        int right = (int) (w * 0.90);
        int y = (band.top + band.bottom) / 2;
        if (revealRightColumns) performSwipe(right, y, left, y, 600L, 200L);
        else performSwipe(left, y, right, y, 600L, 200L);
        waitForSettled();
    }

    private void performSwipe(int startX, int startY, int endX, int endY, long durationMs, long holdMs) {
        viewVersion++;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "ipChangeFinger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(80L)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY));
        if (holdMs > 0) swipe.addAction(new Pause(finger, Duration.ofMillis(holdMs)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    private void tap(UiNode node, String name) {
        if (node == null) throw new AssertionError(name + " is not available to tap");
        tapPoint(node.cx(), node.cy(), name);
    }

    private void tapPoint(int x, int y, String name) {
        tapVersion++;
        viewVersion++;
        ReportLogger.debug("Tapping " + name + " at its live position (" + x + "," + y + ")");
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    // =========================================================
    // SNAPSHOTS AND WAITS
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
                Matcher m = BOUNDS.matcher(element.getAttribute("bounds"));
                if (!m.find()) continue;
                int left = Integer.parseInt(m.group(1));
                int top = Integer.parseInt(m.group(2));
                int right = Integer.parseInt(m.group(3));
                int bottom = Integer.parseInt(m.group(4));
                if (right <= left || bottom <= top) continue;
                String desc = normalize(element.getAttribute("content-desc"));
                String text = normalize(element.getAttribute("text"));
                String cls = element.getAttribute("class");
                if (cls.isEmpty()) cls = element.getTagName();
                nodes.add(new UiNode(desc.isEmpty() ? text : desc, text, cls, left, top, right, bottom));
            }
        } catch (Exception parseFailure) {
            ReportLogger.debug("Page source parse failed: " + parseFailure.getClass().getSimpleName());
        }
        return nodes;
    }

    /** Waits until two consecutive snapshots are identical (scroll/animation/rebuild finished). */
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

    private Dimension screen() {
        if (screen == null) screen = driver.manage().window().getSize();
        return screen;
    }

    // =========================================================
    // SMALL HELPERS
    // =========================================================

    private static Pattern ci(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /** Smallest node whose label fully matches. */
    private static UiNode find(List<UiNode> nodes, Pattern pattern) {
        UiNode best = null;
        for (UiNode n : nodes) {
            if (!n.label.isEmpty() && pattern.matcher(n.label).matches() && (best == null || n.area() < best.area())) best = n;
        }
        return best;
    }

    private static boolean any(List<UiNode> nodes, Pattern pattern) {
        return find(nodes, pattern) != null;
    }

    private static String firstLabelContaining(List<UiNode> nodes, String lowerCaseNeedle) {
        for (UiNode n : nodes) {
            if (n.label.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle)) return n.label;
            if (n.text.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle)) return n.text;
        }
        return null;
    }

    static double parseNumber(String raw) {
        String clean = raw.replace("\u20B9", "").replace(",", "").replace("%", "").trim();
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException invalid) {
            throw new AssertionError("Not a number: '" + raw + "'");
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static List<String> labelsOf(List<UiNode> nodes) {
        List<String> labels = new ArrayList<>();
        for (UiNode n : nodes) if (!n.label.isEmpty() && !labels.contains(n.label)) labels.add(n.label);
        return labels;
    }

    private static String fingerprint(List<UiNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (UiNode n : nodes) if (!n.label.isEmpty()) sb.append(n.label).append('@').append(n.left).append(',').append(n.top).append('|');
        return sb.toString();
    }

    private static String signature(List<UiNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (UiNode n : nodes) sb.append(n.label).append('@').append(n.left).append(',').append(n.top).append('|');
        return sb.toString();
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
            if (count++ == max) {
                sb.append("... +").append(labels.size() - max).append(" more");
                break;
            }
            if (count > 1) sb.append("; ");
            sb.append(label.length() <= 60 ? label : label.substring(0, 60) + "...");
        }
        return sb.append(']').toString();
    }

    private static String round2(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String fmt(Double value) {
        return value == null ? "null" : round2(value);
    }

    // =========================================================
    // DATA HOLDERS
    // =========================================================

    private static final class UiNode {
        final String label;
        final String text;
        final String cls;
        final int left;
        final int top;
        final int right;
        final int bottom;

        UiNode(String label, String text, String cls, int left, int top, int right, int bottom) {
            this.label = label;
            this.text = text;
            this.cls = cls;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        int cx() { return (left + right) / 2; }
        int cy() { return (top + bottom) / 2; }
        int width() { return right - left; }
        int height() { return bottom - top; }
        long area() { return (long) width() * height(); }
    }

    private static final class Band {
        final int top;
        final int bottom;

        Band(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        boolean contains(UiNode n) {
            return n.cy() > top && n.cy() < bottom && n.top >= top - n.height() / 2 && n.bottom <= bottom + n.height() / 2;
        }
    }

    private static final class EditRow {
        final String company;
        final double weight;
        final UiNode lock;
        final UiNode minus;
        final UiNode plus;

        EditRow(String company, double weight, UiNode lock, UiNode minus, UiNode plus) {
            this.company = company;
            this.weight = weight;
            this.lock = lock;
            this.minus = minus;
            this.plus = plus;
        }
    }

    private static final class StepOutcome {
        double before;
        double after;
        String popup;
        final Map<String, Double> visible = new LinkedHashMap<>();
    }

    private static final class LockBar {
        final boolean hint;
        final int locked;
        final Integer total;
        final UiNode unlockAll;

        LockBar(boolean hint, int locked, Integer total, UiNode unlockAll) {
            this.hint = hint;
            this.locked = locked;
            this.total = total;
            this.unlockAll = unlockAll;
        }

        @Override
        public String toString() {
            return hint ? "'Changing a weight locks that stock'" : "'" + locked + " of " + total + " stocks locked'"
                    + (unlockAll != null ? " + Unlock all" : " (no Unlock all)");
        }
    }

    /** Your Orders data: Amount invested and one row per company. */
    public static final class OrderTable {
        double amountInvested;
        final Map<String, OrderRow> rows = new LinkedHashMap<>();

        boolean allColumnsRead() {
            return missingColumns().isEmpty();
        }

        int filledCells() {
            int filled = 0;
            for (OrderRow row : rows.values()) {
                if (row.tentative != null) filled++;
                if (row.price != null) filled++;
                if (row.shares != null) filled++;
                if (row.actual != null) filled++;
                if (row.value != null) filled++;
            }
            return filled + rows.size();
        }

        Set<String> missingColumns() {
            Set<String> missing = new LinkedHashSet<>();
            for (OrderRow row : rows.values()) {
                if (row.tentative == null) missing.add(OrderRow.TENTATIVE);
                if (row.price == null) missing.add(OrderRow.PRICE);
                if (row.shares == null) missing.add(OrderRow.SHARES);
                if (row.actual == null) missing.add(OrderRow.ACTUAL);
                if (row.value == null) missing.add(OrderRow.VALUE);
            }
            return missing;
        }
    }

    public static final class OrderRow {
        static final String TENTATIVE = "Tentative Weight";
        static final String PRICE = "Current Price";
        static final String SHARES = "No of Shares";
        static final String ACTUAL = "Actual Weight";
        static final String VALUE = "Value (₹)";

        final String company;
        Double tentative;
        Double price;
        Double shares;
        Double actual;
        Double value;

        OrderRow(String company) {
            this.company = company;
        }

        void set(String column, double number) {
            switch (column) {
                case TENTATIVE: tentative = number; break;
                case PRICE: price = number; break;
                case SHARES: shares = number; break;
                case ACTUAL: actual = number; break;
                case VALUE: value = number; break;
                default: break;
            }
        }

        @Override
        public String toString() {
            return company + "{wt " + fmt(tentative) + ", price " + fmt(price) + ", shares " + fmt(shares)
                    + ", actual " + fmt(actual) + ", value " + fmt(value) + "}";
        }
    }
}