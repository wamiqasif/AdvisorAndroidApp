package com.valueresearch.pages;

import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.ExecutionProfile;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * Recommendations product changes: TC_001-TC_013.
 * Locators: the supplied 13-Oct-2026 Inspector captures.
 * Authentication/Hub gestures follow the supplied updated Batch 1 flow.
 * Existing page objects, tests and shared utilities are left untouched.
 */
public class StockAnalystsChoiceChangebyProPage {

    public static final String BUILD_VERSION = "STOCK_ANALYSTS_CHOICE_PRO_V26_LAPSED_DATA_PANE_20261015";

    public enum Persona {
        ACTIVE_SUBSCRIBER, LAPSED_USER;
        public String email() { return AuthHelper.configuredEmail(AuthHelper.Account.valueOf(name())); }
    }

    private static final By FUNDS = AppiumBy.accessibilityId("Funds");
    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By SCROLL = AppiumBy.className("android.widget.ScrollView");
    private static final By HUB_SECTIONS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionMatches(\"^(Profile|Investor Accounts|Mutual Funds|Stocks|More)$\")");
    private static final By ENTRY = AppiumBy.accessibilityId("Open Stock Analyst's Choice screen");
    private static final By TITLE = AppiumBy.accessibilityId("Analyst's Choice");
    private static final By BACK = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.Button\").description(\"Go back\")");
    private static final By ANY_BACK = AppiumBy.accessibilityId("Go back");
    private static final By QUESTION = AppiumBy.accessibilityId("?");
    private static final By BUY_TAB = AppiumBy.accessibilityId("Buy, tab 1 of 3");
    private static final By HOLD_TAB = AppiumBy.accessibilityId("Hold, tab 2 of 3");
    private static final By SELL_TAB = AppiumBy.accessibilityId("Sell, tab 3 of 3");
    private static final By CUSTOMISE = AppiumBy.accessibilityId("Customise");
    private static final By SCREENER_TITLE = AppiumBy.accessibilityId("Stock Screener");
    private static final By PLANNER_TITLE = AppiumBy.accessibilityId("Investment Planner");
    private static final By RECO_DATE_HEADER = AppiumBy.accessibilityId("Reco\nDate");
    private static final By EXIT_DATE_HEADER = AppiumBy.accessibilityId("Exit\nDate");
    // Flutter may flatten the newline or expose another widget class on Hold/Sell.
    private static final By RECO_DATE_HEADER_FALLBACK = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"^Reco\\\\s+Date$\")");
    private static final By EXIT_DATE_HEADER_FALLBACK = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"^Exit\\\\s+Date$\")");
    private static final By DATE_CELLS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionMatches(\"[0-9]{2}-[A-Za-z]{3}-[0-9]{2}\")");
    private static final By TABLE_HEADER_CELLS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").descriptionMatches(\"(?s).+\")");
    private static final By TABLE_HORIZONTAL_SCROLL = AppiumBy.className("android.widget.HorizontalScrollView");
    private static final String LAST_COLUMN_COPY = "Vs BSE 500 TRI Return";
    private static final DateTimeFormatter RECOMMENDATION_DATE_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("dd-MMM-")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final By[] RECOMMENDATION_TABS = {BUY_TAB, HOLD_TAB, SELL_TAB};
    private static final String[] RECOMMENDATION_TAB_LABELS = {"Buy", "Hold", "Sell"};
    private static final String WIDGET_HEADING_COPY = "Need a hand deciding?";
    private static final String WIDGET_SUBTITLE_COPY = "Our Investment Planner picks the stocks and split for you";
    private static final By WIDGET_HEADING = AppiumBy.accessibilityId(WIDGET_HEADING_COPY);
    private static final By WIDGET_SUBTITLE = AppiumBy.accessibilityId(WIDGET_SUBTITLE_COPY);
    private static final By WIDGET_CTA = AppiumBy.accessibilityId("Take me there");
    private static final String[] OLD_WIDGET_COPY = {
            "Confused? Use our Portfolio Planner",
            "Get ready-to-go portfolios customised to your investing needs"
    };
    private static final By ROW_NUMBER_COLUMN = AppiumBy.accessibilityId("#");
    private static final By ADD_WATCHLIST = AppiumBy.accessibilityId("Add to watchlist");
    private static final By ADD_EXISTING_WATCHLIST = AppiumBy.accessibilityId("Add to Existing Watchlist");
    private static final By CREATE_WATCHLIST = AppiumBy.accessibilityId("Or Create a new Watchlist");
    private static final By WATCHLIST_NAME_LABEL = AppiumBy.accessibilityId("Enter watchlist name");
    private static final By WATCHLIST_SELECT = AppiumBy.accessibilityId("Select");
    private static final By WATCHLIST_CLOSE = AppiumBy.accessibilityId("Close");
    private static final By WATCHLIST_NOTICE = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").descriptionStartsWith(\"Please note\")");
    private static final AtomicLong WATCHLIST_NUMBER = new AtomicLong();
    private static final long WATCHLIST_EPOCH_SECONDS = 1_577_836_800L; // 01-Jan-2020 UTC
    private static final By ALL_CATEGORY = AppiumBy.accessibilityId("All");
    private static final By ALL_WEATHER_CATEGORY = AppiumBy.accessibilityId("All-weather");
    private static final By STOCKS_COLUMN = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Stocks\")");
    private static final By STOCK_NAME_VIEWS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").descriptionMatches(\"(?s).*[A-Za-z].*\")");
    private static final By STOCK_CHART = AppiumBy.accessibilityId("Stock Chart");
    private static final By STOCK_PANEL_VIEWS = AppiumBy.className("android.view.View");
    private static final By ANDROID_NAVIGATION_BAR = By.id("android:id/navigationBarBackground");
    // These ImageView controls are the help accordion, not the underlying table tabs.
    private static final By BUY_HELP = helpHeader("Buy");
    private static final By HOLD_HELP = helpHeader("Hold");
    private static final By SELL_HELP = helpHeader("Sell");
    private static final By ALL_WEATHER = AppiumBy.accessibilityId("All-Weather");
    private static final By AGGRESSIVE = AppiumBy.accessibilityId("Aggressive");
    private static final By LONG_TERM = AppiumBy.accessibilityId("Long term");
    private static final By[] BUY_DESCRIPTION_MARKERS = {
            AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"Suited for investors seeking stable performance\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"Geared towards investors willing to take on higher levels of risk\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"Aimed at investors seeking substantial capital appreciation\")")
    };
    private static final By EXPENSIVE = AppiumBy.accessibilityId("Expensive valuation");
    private static final By FUNDAMENTALS = AppiumBy.accessibilityId("Hold for fundamentals");
    private static final By DIVIDEND_DESCRIPTION = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"(?is).*\\\\bdividends?\\\\b.*\")");
    private static final By DIVIDEND_TEXT = AppiumBy.androidUIAutomator(
            "new UiSelector().textMatches(\"(?is).*\\\\bdividends?\\\\b.*\")");

    private static final String EXPENSIVE_COPY = "Stocks trading at unsustainable valuations, "
            + "but we stay bullish on growth. Hold if you own them; wait for a Buy call to add more.";
    private static final String FUNDAMENTALS_COPY = "Companies with fundamental concerns about their "
            + "business outlook. Hold if you already own them, and wait for a Buy call before adding more.";

    private static final By BIOMETRIC_CANCEL = By.id("com.android.systemui:id/button_negative");
    private static final By ACCOUNT_ENTRY = AppiumBy.accessibilityId("View and edit your account details");
    private static final By ACCOUNT_TITLE = AppiumBy.accessibilityId("Account Details");
    private static final By EMAIL_LABEL = AppiumBy.accessibilityId("Email Address");
    private static final By EMAIL_VALUE = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").textMatches(\".+@.+\")");
    private static final By LOGOUT = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Logout\")");
    private static final By LOGOUT_CONFIRMATION = AppiumBy.accessibilityId("Are you sure you want to logout?");
    private static final By CONFIRM_LOGOUT = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").description(\"Logout\")");
    private static final By INPUT = AppiumBy.className("android.widget.EditText");
    private static final String WATCHLIST_HELPER_COPY = "Watchlist 1";
    private static final String[] LAPSED_PITCH_COPY = {
            "Curious which stocks we consider most promising?\n"
                    + "Get Stock Advisor to see our handpicked list of top Buy recommendations, carefully selected for strong potential returns.\nUnlock now",
            "Wondering which past commendations you should not add more to?\n"
                    + "Get Stock Advisor to understand why these positions are being held.\nUnlock now",
            "Wondering which stocks to avoid investing in?\n"
                    + "Get Stock Advisor to learn which ones we recommend staying away from.\nUnlock now"
    };

    private final AndroidDriver driver;
    private final AuthHelper auth;
    private final AuthHelper.LoginMethod loginMethod;
    private final int maxScrolls = positiveInt("stockAnalystsChoiceMaxScrolls", 12);
    private final int maxClickAttempts = positiveInt("stockAnalystsChoiceClickAttempts", 3);
    private final int swipeMillis = positiveInt("stockAnalystsChoiceSwipeDurationMillis", 200);
    private final int stockDragMillis = positiveInt("stockAnalystsChoiceStockDragMillis", 500);
    private final boolean stockGestureDebug = Boolean.parseBoolean(configured(
            "STOCK_ANALYSTS_CHOICE_GESTURE_DEBUG", "stockAnalystsChoiceGestureDebug"));
    private final int maxStockDrags = positiveInt("stockAnalystsChoiceMaxStockDrags", 3);
    private final int maxListScrolls = positiveInt("stockAnalystsChoiceMaxListScrolls", 40);
    private final int maxHorizontalSwipes = positiveInt("stockAnalystsChoiceMaxHorizontalSwipes", 20);
    private boolean watchlistDrawerWasOpened;
    private String selectedStockName;
    private String selectedStockHeader;
    private Rectangle stockTableViewport;
    private Rectangle stockModuleTitle;
    private double stockDragReferenceDistance;
    private Object previousSelectorTimeout;
    private Object previousIdleTimeout;
    private Duration previousImplicitWait;
    private boolean timingSettingsChanged;

    public StockAnalystsChoiceChangebyProPage(AndroidDriver driver) {
        this(driver, null); // Existing callers keep global configuration selection.
    }

    public StockAnalystsChoiceChangebyProPage(AndroidDriver driver, AuthHelper.LoginMethod loginMethod) {
        if (driver == null) {
            throw new IllegalArgumentException("An active AndroidDriver is required");
        }
        this.driver = driver;
        this.auth = new AuthHelper(driver);
        this.loginMethod = loginMethod;
    }

    /** Verify the configured active/lapsed account, switching only when it differs. */
    public void ensureRequiredAccount(Persona persona) {
        beginImmediateSelectors();
        nativeContext();
        String requiredEmail = persona.email(); // Validate before changing a usable session.
        waitUntil(() -> visible(BIOMETRIC_CANCEL) || pinScreenVisible() || emailLoginVisible()
                        || appTabsVisible() || visible(ANY_BACK),
                ExecutionProfile.longWait(), "No known Advisor authentication/navigation screen appeared");
        if (visible(BIOMETRIC_CANCEL) || pinScreenVisible()) {
            finishSecurityAuthentication(null);
        }
        if (!emailLoginVisible()) {
            returnToHub();
            openAccountDetails();
            if (readAccountEmail().equalsIgnoreCase(requiredEmail)) {
                ReportLogger.pass("Verified existing " + persona + " account; reusing its session");
                backFromAccountDetails();
                return;
            }
            AuthHelper.validateLoginConfiguration(AuthHelper.Account.valueOf(persona.name()), selectedLoginMethod());
            ReportLogger.step("Switching to the configured " + persona + " through Logout");
            logoutFromAccountDetails();
        }
        loginAs(persona);
        openAccountDetails();
        if (!readAccountEmail().equalsIgnoreCase(requiredEmail)) {
            throw new AssertionError("Logged-in email does not match the configured " + persona);
        }
        ReportLogger.pass("Logged-in account email verified for " + persona);
        backFromAccountDetails();
    }

    /**
     * Reuse only after the test class verified this persona in the same driver
     * session. This checks navigation/unlock state without opening Account Details.
     * A logged-out screen invalidates that verification and requires fresh login.
     */
    public boolean reuseVerifiedAccountSession(Persona persona) {
        beginImmediateSelectors();
        nativeContext();
        if (!visible(BIOMETRIC_CANCEL) && !pinScreenVisible() && isHubScreenVisible()) {
            return true;
        }
        waitUntil(() -> visible(BIOMETRIC_CANCEL) || pinScreenVisible() || emailLoginVisible()
                        || appTabsVisible() || visible(ANY_BACK),
                ExecutionProfile.longWait(), "Verified account session did not reach a known navigation/unlock screen");
        if (emailLoginVisible()) {
            return false;
        }
        if (visible(BIOMETRIC_CANCEL) || pinScreenVisible()) {
            finishSecurityAuthentication(persona);
        }
        if (emailLoginVisible()) {
            return false;
        }
        returnToHub();
        assertHubVisible();
        return true;
    }

    /** Open only the captured Stocks entry. Never reuse the old ready-to-invest tile. */
    public void openFromHub() {
        assertHubVisible();
        scrollToHubElement(ENTRY, "down");
        ReportLogger.step("Opening Stock Analyst's Choice under Hub > Stocks");
        clickHubElement(ENTRY);
        waitForModuleReady();
    }

    public void verifyHeading() {
        waitForModuleReady();
        assertSemantic(TITLE, "Analyst's Choice");
        ReportLogger.pass("Page heading is Analyst's Choice");
    }

    /** TC_002 deliberately has no Android Back or Hub-tab fallback. */
    public void verifyTopBackButtonAndReturnToHub() {
        waitForModuleReady();
        WebElement back = require(BACK, ExecutionProfile.defaultWait());
        Rectangle backRect = back.getRect();
        Rectangle titleRect = require(TITLE, ExecutionProfile.shortWait()).getRect();
        if (!fullyOnScreen(backRect) || backRect.getY() + backRect.getHeight() > titleRect.getY()) {
            throw new AssertionError("Go back must be fully visible above the Analyst's Choice heading");
        }
        tapBack(BACK);
        waitUntil(() -> isHubScreenVisible() && !visible(TITLE) && !helpDrawerVisible(),
                ExecutionProfile.defaultWait(), "The top Go back button did not return to Hub");
        ReportLogger.pass("Top Go back button is present and returned to Hub");
    }

    /** TC_004: prove Buy is loaded and completely exposed before asserting absence. */
    public void verifyBuyHasNoDividendDefinition() {
        openKnowMore();
        expandBuy();
        waitForStableBuyContent();
        Rectangle buy = require(BUY_HELP, ExecutionProfile.shortWait()).getRect();
        Rectangle hold = require(HOLD_HELP, ExecutionProfile.shortWait()).getRect();
        for (By locator : new By[]{DIVIDEND_DESCRIPTION, DIVIDEND_TEXT}) {
            for (WebElement candidate : driver.findElements(locator)) {
                Rectangle r = candidate.getRect();
                if (candidate.isDisplayed() && r.getY() >= buy.getY() + buy.getHeight()
                        && r.getY() < hold.getY()) {
                    throw new AssertionError("Dividend definition is still present in the expanded Buy section");
                }
            }
        }
        ReportLogger.pass("Expanded Buy definitions are loaded; Dividend definition is absent");
    }

    /** TC_005 independently exercises Buy -> collapse Buy -> expand Hold. */
    public void verifyNewHoldDefinitions() {
        openKnowMore();
        expandBuy();
        collapseBuy();
        if (!visible(EXPENSIVE)) {
            click(HOLD_HELP);
        }
        waitUntil(() -> helpDrawerVisible() && !visible(ALL_WEATHER)
                        && visible(EXPENSIVE) && visible(FUNDAMENTALS)
                        && visible(AppiumBy.accessibilityId(EXPENSIVE_COPY))
                        && visible(AppiumBy.accessibilityId(FUNDAMENTALS_COPY)),
                ExecutionProfile.defaultWait(), "The two new Hold definitions did not finish loading");
        assertSemantic(EXPENSIVE, "Expensive valuation");
        assertSemantic(AppiumBy.accessibilityId(EXPENSIVE_COPY), EXPENSIVE_COPY);
        assertSemantic(FUNDAMENTALS, "Hold for fundamentals");
        assertSemantic(AppiumBy.accessibilityId(FUNDAMENTALS_COPY), FUNDAMENTALS_COPY);
        ReportLogger.pass("Both new Hold headings and their full definition copy match the product changes");
    }

    /** TC_006: select a live Buy/All stock; expand the drawer; drag its top down to dismiss. */
    public void verifyStockDrawerExpansionAndDismissal() {
        waitForModuleReady();
        tapNativeControl(BUY_TAB);
        // All-weather is a Buy category in the supplied hierarchy. Wait for the
        // Buy controls to resolve before selecting All or searching its rows.
        waitUntil(() -> visible(BUY_TAB) && visible(ALL_CATEGORY) && visible(ALL_WEATHER_CATEGORY),
                ExecutionProfile.defaultWait(), "Buy tab/category controls did not finish loading");
        tapNativeControl(ALL_CATEGORY);
        waitUntil(() -> visible(STOCKS_COLUMN) && visible(ALL_WEATHER_CATEGORY),
                ExecutionProfile.defaultWait(), "Buy > All did not expose the Stocks table");

        Rectangle moduleBack = require(BACK, ExecutionProfile.shortWait()).getRect();
        Rectangle moduleTitle = require(TITLE, ExecutionProfile.shortWait()).getRect();
        Rectangle buyTab = require(BUY_TAB, ExecutionProfile.shortWait()).getRect();
        final List<RuntimeStock> previousStock = new ArrayList<>();
        previousStock.add(null);
        RuntimeStock stock = waitFor(ExecutionProfile.longWait()).withMessage(
                "TC_006 needs at least one settled, fully visible live stock in Buy > All").until(d -> {
            RuntimeStock current = findLiveStock();
            RuntimeStock previous = previousStock.get(0);
            previousStock.set(0, current);
            return current != null && previous != null && current.name.equals(previous.name)
                    && current.bounds.equals(previous.bounds) ? current : null;
        });
        selectedStockName = stock.name;
        selectedStockHeader = null;
        stockModuleTitle = moduleTitle;
        stockTableViewport = stockTableViewport(stock.bounds);
        ReportLogger.step("TC_006: Opening " + selectedStockName + " from Buy > All");
        tapNativeControl(AppiumBy.accessibilityId(selectedStockName));

        ReportLogger.step("TC_006: Waiting for the stock details drawer to open");
        StockPanelSnapshot initial = waitForSettledStockPanel(
                ExecutionProfile.longWait(), "Stock details drawer did not finish opening");
        selectedStockHeader = initial.company;
        if (initial.toolbar != null || initial.content.getY() <= buyTab.getY() + buyTab.getHeight()) {
            throw new AssertionError("Stock tap must first open a partial drawer beneath the Analyst's Choice tabs");
        }
        ReportLogger.pass("TC_006: Stock details drawer is open");

        // Preserve the configured first-upward-drag duration. Every subsequent
        // drag uses the same pixels-per-millisecond speed, in either direction.
        StockDrag firstUp = expansionDrag(initial, moduleBack, moduleTitle, usableScreenBounds());
        stockDragReferenceDistance = stockDragDistance(firstUp);
        StockPanelSnapshot full = null;
        StockPanelSnapshot current = initial;
        for (int attempt = 1; attempt <= maxStockDrags; attempt++) {
            ReportLogger.step(attempt == 1
                    ? "TC_006: Swiping up to open stock details in full screen"
                    : "TC_006: Drawer is still partly expanded; swiping up again (attempt "
                    + attempt + " of " + maxStockDrags + ")");
            performStockDrag(expansionDrag(current, moduleBack, moduleTitle, usableScreenBounds()));
            // Wait for any settled panel, then inspect the outcome. A stable
            // partial expansion can be retried immediately; it is never a pass.
            current = waitForSettledStockPanel(ExecutionProfile.defaultWait(),
                    "Stock details did not finish moving after the upward swipe");
            if (expandedToFullScreen(initial, current, moduleBack, moduleTitle)) {
                full = current;
                break;
            }
        }
        if (full == null) {
            throw new AssertionError("Stock details did not open in full screen after " + maxStockDrags + " upward swipes");
        }
        ReportLogger.pass("TC_006: Stock details are open in full screen");

        // Press the top toolbar/handle then drag down. A partial collapse alone
        // is not a pass: keep polling for the complete panel dismissal.
        boolean dismissed = false;
        for (int attempt = 1; attempt <= maxStockDrags; attempt++) {
            current = requireStockPanel("Stock panel disappeared before its dismissal gesture");
            boolean isFull = expandedToFullScreen(initial, current, moduleBack, moduleTitle);
            ReportLogger.step(attempt == 1
                    ? "TC_006: Swiping down from the top bar to close stock details"
                    : "TC_006: Stock details are still open; swiping down again (attempt "
                    + attempt + " of " + maxStockDrags + ")");
            performStockDrag(dismissalDrag(current, isFull, moduleTitle, usableScreenBounds()));
            try {
                waitUntil(this::buyAllRestoredAfterStockDismissal, ExecutionProfile.shortWait(),
                        "Stock details panel did not close back to Buy > All");
                dismissed = true;
                break;
            } catch (TimeoutException notDismissed) {
                if (!stockDetailsPresent() || attempt == maxStockDrags) {
                    throw new AssertionError("Dragging the stock panel top down did not restore Buy > All", notDismissed);
                }
            }
        }
        if (!dismissed) {
            throw new AssertionError("Stock panel dismissal was not observed");
        }
        ReportLogger.pass("TC_006: Stock details are closed; returned to Buy > All");
    }

    /** TC_007: each recommendation tab exposes the updated footer with no selection. */
    public void verifyEndOfListWidgetOnAllTabs() {
        ensureNoStockSelection();
        By[] tabs = {BUY_TAB, HOLD_TAB, SELL_TAB};
        String[] labels = {"Buy", "Hold", "Sell"};
        for (int i = 0; i < tabs.length; i++) {
            switchRecommendationTab(tabs[i]);
            ReportLogger.step("TC_007: Scrolling to the end of the " + labels[i] + " list");
            scrollRecommendationList(true);
            verifyEndOfListWidget();
            ReportLogger.pass("TC_007: " + labels[i] + " shows the updated widget; old copy is absent");
        }
    }

    /** TC_008: tools return to this module; one checkbox selection creates one new watchlist. */
    public void verifyNavigationAndCreateWatchlist() {
        ensureNoStockSelection();
        switchRecommendationTab(BUY_TAB);
        ReportLogger.step("TC_008: Opening Stock Screener using Customise");
        tapNativeControl(CUSTOMISE);
        verifyToolAndReturn(SCREENER_TITLE, "Stock Screener");

        ReportLogger.step("TC_008: Scrolling to the end of the Buy list");
        scrollRecommendationList(true);
        ReportLogger.step("TC_008: Opening Investment Planner using Take me there");
        tapNativeControl(WIDGET_CTA);
        verifyToolAndReturn(PLANNER_TITLE, "Investment Planner");

        ReportLogger.step("TC_008: Returning to the top of the Buy list");
        scrollRecommendationList(false);
        RuntimeStock stock = selectStockAndOpenWatchlist("TC_008");

        String watchlistName = nextWatchlistName();
        ReportLogger.step("TC_008: Opening the new watchlist name field");
        tapNativeControl(CREATE_WATCHLIST);
        waitUntil(() -> visible(WATCHLIST_NAME_LABEL) && visible(WATCHLIST_SELECT)
                        && watchlistNameInput() != null, ExecutionProfile.defaultWait(),
                "Create new Watchlist did not open the name field and clear the existing selection");
        ReportLogger.step("TC_008: Entering watchlist name: " + watchlistName);
        typeWatchlistName(watchlistName);
        ReportLogger.pass("TC_008: Watchlist name entered and verified: " + watchlistName);
        waitUntil(() -> drawerStockVisible(stock.name), ExecutionProfile.defaultWait(),
                "The selected stock disappeared before watchlist submission");
        ReportLogger.step("TC_008: Adding " + stock.name + " to " + watchlistName);
        tapWatchlistSubmitButton();
        ReportLogger.step("TC_008: Add to watchlist tapped; waiting for the success popup");
        // The success notice can be brief: poll it immediately after submission.
        waitFor(ExecutionProfile.longWait()).withMessage(
                "No success popup confirmed the new watchlist " + watchlistName).until(d -> {
            for (WebElement candidate : driver.findElements(WATCHLIST_NOTICE)) {
                if (candidate.isDisplayed()) {
                    String text = normalize(candidate.getAttribute("content-desc"));
                    if (!watchlistSuccessMatches(text, watchlistName)) {
                        throw new AssertionError("Unexpected watchlist popup: " + text);
                    }
                    return text;
                }
            }
            return null;
        });
        waitUntil(() -> !watchlistDrawerVisible(), ExecutionProfile.defaultWait(),
                "The watchlist drawer remained open after the success popup");
        watchlistDrawerWasOpened = false;
        ReportLogger.pass("TC_008: " + stock.name + " added to " + watchlistName + "; success popup verified");
    }

    private RuntimeStock selectStockAndOpenWatchlist(String caseId) {
        if (visible(ADD_WATCHLIST)) {
            throw new AssertionError(caseId + " must start checkbox selection with no stocks already selected");
        }
        RuntimeStock stock = waitFor(ExecutionProfile.defaultWait())
                .withMessage(caseId + " needs one fully visible Buy stock to add to a watchlist")
                .until(d -> findLiveStock());
        Rectangle viewport = recommendationViewport();
        Rectangle numberColumn = require(ROW_NUMBER_COLUMN, ExecutionProfile.shortWait()).getRect();
        Point checkbox = checkboxPoint(numberColumn, stock.bounds, viewport);
        ReportLogger.step(caseId + ": Selecting the checkbox for " + stock.name);
        driver.executeScript("mobile: clickGesture", Map.of("x", checkbox.getX(), "y", checkbox.getY()));
        waitUntil(() -> visible(ADD_WATCHLIST), ExecutionProfile.defaultWait(),
                "Selecting the stock checkbox did not show Add to watchlist");
        ReportLogger.pass(caseId + ": Stock checkbox selected; Add to watchlist is visible");
        ReportLogger.step(caseId + ": Opening the Add to watchlist drawer");
        watchlistDrawerWasOpened = true;
        tapNativeControl(ADD_WATCHLIST);
        waitUntil(this::watchlistDrawerVisible, ExecutionProfile.defaultWait(),
                "Add to watchlist did not open the watchlist drawer");
        waitUntil(() -> drawerStockVisible(stock.name), ExecutionProfile.defaultWait(),
                "The selected stock is not listed in the watchlist drawer");
        ReportLogger.pass(caseId + ": Watchlist drawer is open with " + stock.name + " selected");

        return stock;
    }

    /** TC_009: validate both chronological orders across each live list, including tied dates. */
    public void verifyDateSortingOnAllTabs() {
        ensureNoStockSelection();
        List<Throwable> failures = new ArrayList<>();
        Set<String> completed = new HashSet<>();
        for (int tab = 0; tab < RECOMMENDATION_TABS.length; tab++) {
            String label = RECOMMENDATION_TAB_LABELS[tab];
            try {
                ReportLogger.step("TC_009: " + label + " - preparing date sorting");
                By dateHeader = prepareRecommendationTable(RECOMMENDATION_TABS[tab]);
                for (int click = 0; click < 2; click++) {
                    int expectedDirection = click == 0 ? 1 : -1;
                    String order = expectedDirection > 0 ? "ascending" : "descending";
                    Rectangle column = tableControlBounds(dateHeader);
                    if (!fullyOnScreen(column)) throw new AssertionError(label + " date header is clipped; sort was not clicked");
                    ReportLogger.step("TC_009: " + label + " - clicking "
                            + (tab == 2 ? "Exit Date" : "Reco Date") + " for " + order + " sorting");
                    driver.executeScript("mobile: clickGesture", Map.of(
                            "x", column.getX() + column.getWidth() / 2,
                            "y", column.getY() + column.getHeight() / 2));
                    DateWindow sorted = settledDateWindow(column, expectedDirection, ExecutionProfile.defaultWait());
                    DateWindow bottom = verifyDateOrderToBoundary(column, sorted, expectedDirection, true, label, sorted.dates);
                    verifyDateOrderToBoundary(column, bottom, expectedDirection, false, label, sorted.dates);
                    completed.add(label + ":" + expectedDirection);
                    ReportLogger.pass("TC_009: " + label + " - " + order
                            + " date order verified to the bottom and back to the top");
                }
            } catch (AssertionError | TimeoutException failure) {
                failures.add(new AssertionError(label + " sorting failed: " + failure.getMessage(), failure));
                ReportLogger.fail("TC_009: " + label + " sorting failed: " + failure.getMessage());
            }
        }
        if (!failures.isEmpty() || completed.size() != RECOMMENDATION_TABS.length * 2) {
            AssertionError failure = new AssertionError("TC_009 requires ascending and descending sorting on Buy, Hold and Sell; "
                    + "verified " + completed.size() + " of 6 directions. "
                    + (failures.isEmpty() ? "Sorting checks were skipped." : failures.get(0).getMessage()));
            for (Throwable cause : failures) failure.addSuppressed(cause);
            throw failure;
        }
    }

    /** TC_010: pan the table from its first date column to the right-hand boundary. */
    public void verifyLastColumnHeadingOnAllTabs() {
        ensureNoStockSelection();
        for (int tab = 0; tab < RECOMMENDATION_TABS.length; tab++) {
            String label = RECOMMENDATION_TAB_LABELS[tab];
            By dateHeader = prepareRecommendationTable(RECOMMENDATION_TABS[tab]);
            Rectangle firstColumn = tableControlBounds(dateHeader);
            ReportLogger.step("TC_010: " + label + " - scrolling horizontally from "
                    + (tab == 2 ? "Exit Date" : "Reco Date") + " to the last column");
            Map<String, Rectangle> before = settledTableHeaders();
            boolean reachedEnd = false;
            for (int swipe = 0; swipe < maxHorizontalSwipes; swipe++) {
                swipeRecommendationTable(true, swipe == 0 ? firstColumn : null, false);
                Map<String, Rectangle> after = settledTableHeaders();
                if (after.equals(before)) {
                    swipeRecommendationTable(true, null, true);
                    after = settledTableHeaders();
                    if (after.equals(before)) {
                        reachedEnd = true;
                        break;
                    }
                }
                before = after;
            }
            if (!reachedEnd) {
                throw new AssertionError(label + " table did not reach its horizontal end within "
                        + maxHorizontalSwipes + " bounded swipes");
            }
            List<HeaderCell> headers = visibleTableHeaders();
            HeaderCell last = headers.get(headers.size() - 1);
            if (!LAST_COLUMN_COPY.equals(last.label) || !fullyOnScreen(last.bounds)) {
                throw new AssertionError(label + " last column must be exactly '" + LAST_COLUMN_COPY
                        + "'; actual rightmost header: '" + last.label + "'");
            }
            ReportLogger.pass("TC_010: " + label + " - last column is exactly '" + LAST_COLUMN_COPY + "'");
        }
    }

    /** TC_011 reads the empty field's hint; it never types or creates a watchlist. */
    public void verifyWatchlistNameHelperText() {
        ensureNoStockSelection();
        switchRecommendationTab(BUY_TAB);
        waitForLoadedRecommendationTable();
        scrollRecommendationTableToTop();
        selectStockAndOpenWatchlist("TC_011");
        tapNativeControl(CREATE_WATCHLIST);
        waitFor(ExecutionProfile.defaultWait()).withMessage("TC_011: New watchlist name field did not open")
                .until(d -> watchlistNameInput());
        // Flutter can suppress placeholder semantics while the field is focused.
        // Closing the keyboard also lets the drawer/name field reach its final layout.
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
            waitUntil(() -> !driver.isKeyboardShown(), ExecutionProfile.shortWait(),
                    "TC_011: Keyboard did not close before reading the helper text");
        }
        String actual = waitForWatchlistHelperText();
        if (!WATCHLIST_HELPER_COPY.equals(firstWatchlistHelper(actual))) {
            throw new AssertionError("TC_011: Name helper's first example must be '" + WATCHLIST_HELPER_COPY
                    + "'; actual: '" + actual + "'");
        }
        ReportLogger.pass("TC_011: Name helper includes 'Watchlist 1'");
        // runCase's existing finally closes this drawer/keyboard and returns to Hub.
    }

    private String waitForWatchlistHelperText() {
        String[] previous = {null};
        String[] diagnostic = {"Name field is not visible"};
        try {
            return waitFor(ExecutionProfile.shortWait()).withMessage(
                    "TC_011: Waiting for exposed watchlist helper text").until(d -> {
                WebElement input = watchlistNameInput();
                if (input == null) {
                    previous[0] = null;
                    return null;
                }
                String hint = exposedText(input.getAttribute("hint"));
                String text = exposedText(input.getText());
                String description = exposedText(input.getAttribute("content-desc"));
                // Native hint is sufficient. Some UiAutomator2/Flutter combinations
                // reject showing-hint even though the server lists it as supported.
                boolean showingHint = hint.isEmpty() && optionalShowingHint(input);
                diagnostic[0] = "hint='" + hint + "', text='" + text + "', content-desc='"
                        + description + "', showing-hint=" + showingHint;
                String helper = hint;
                if (helper.isEmpty() && showingHint) helper = text;
                if (helper.isEmpty() && text.isEmpty()) {
                    // Do not accept an actual entered/default value as helper text.
                    // Flutter may expose the placeholder as a child/sibling semantic
                    // view inside the field instead of Android's hint attribute.
                    Set<String> candidates = new LinkedHashSet<>();
                    if (!description.isEmpty() && !"Enter watchlist name".equals(description)) candidates.add(description);
                    Rectangle bounds = input.getRect();
                    for (By locator : new By[]{STOCK_PANEL_VIEWS, AppiumBy.className("android.widget.TextView")}) {
                        for (WebElement candidate : driver.findElements(locator)) {
                            if (!candidate.isDisplayed() || !inside(candidate.getRect(), bounds, 0)) continue;
                            String value = exposedText(candidate.getAttribute("content-desc"));
                            if (value.isEmpty()) value = exposedText(candidate.getText());
                            if (!value.isEmpty() && !"Enter watchlist name".equals(value)) candidates.add(value);
                        }
                    }
                    if (candidates.size() > 1) {
                        throw new AssertionError("TC_011: Multiple different texts overlap the name field: " + candidates);
                    }
                    if (!candidates.isEmpty()) helper = candidates.iterator().next();
                }
                if (helper.isEmpty()) {
                    previous[0] = null;
                    return null;
                }
                String firstExample = firstWatchlistHelper(helper);
                boolean stable = firstExample.equals(previous[0]);
                previous[0] = firstExample;
                return stable ? helper : null;
            });
        } catch (TimeoutException unavailable) {
            throw new AssertionError("TC_011: Helper text is not exposed by the current Flutter accessibility tree. "
                    + diagnostic[0] + ". Provide the failure trace and the name field's full Inspector attributes/XML. "
                    + "Expected helper: 'Watchlist 1'. No name was typed or submitted.", unavailable);
        }
    }

    private static String exposedText(String value) {
        // Some Appium versions stringify an absent native attribute as "null".
        // Preserve all real whitespace/case so the exact-copy assertion stays strict.
        return value == null || "null".equals(value) ? "" : value;
    }

    private boolean optionalShowingHint(WebElement input) {
        for (String attribute : new String[]{"showing-hint", "showingHintText"}) {
            try {
                String value = exposedText(input.getAttribute(attribute));
                if (!value.isEmpty()) return Boolean.parseBoolean(value);
            } catch (UnsupportedCommandException unsupportedOptionalAttribute) {
                // Optional metadata must not prevent reading an exposed helper.
                // Other WebDriver/transport errors still propagate unchanged.
            }
        }
        return false;
    }

    private static String firstWatchlistHelper(String helper) {
        // Ignore later comma-separated examples; "Watchlist 10" is a different name.
        return helper.split(",", 2)[0].trim();
    }

    /** TC_012: account selection is common setup, outside these feature assertions. */
    public void verifyLapsedSubscriptionPitchOnAllTabs() {
        ensureNoStockSelection();
        int completed = 0;
        for (int tab = 0; tab < RECOMMENDATION_TABS.length; tab++) {
            String label = RECOMMENDATION_TAB_LABELS[tab];
            switchRecommendationTab(RECOMMENDATION_TABS[tab]);
            waitForLapsedPitch(tab);
            completed++;
            ReportLogger.pass("TC_012: " + label + " subscription pitch is present");
        }
        if (completed != 3) throw new AssertionError("TC_012 requires the subscription pitch on Buy, Hold and Sell");
    }

    /**
     * TC_013: activate the first visible date, then pan the table's data scroll
     * container. Header-band swipes can reach Flutter's outer Buy/Hold/Sell pager.
     * Verify actual endpoint labels; layout reflow is not a test failure.
     */
    public void verifyLapsedHorizontalScrollingOnAllTabs() {
        ensureNoStockSelection();
        int completed = 0;
        for (int tab = 0; tab < RECOMMENDATION_TABS.length; tab++) {
            String label = RECOMMENDATION_TAB_LABELS[tab];
            switchRecommendationTab(RECOMMENDATION_TABS[tab]);
            waitForLapsedPitch(tab);
            activateLapsedTableByDate(tab);
            ReportLogger.step("TC_013: " + label + " - swiping left inside the table data pane to " + LAST_COLUMN_COPY);
            panLapsedTableToColumn(true, tab);
            ReportLogger.pass("TC_013: " + label + " - final column is '" + LAST_COLUMN_COPY + "'");
            panLapsedTableToColumn(false, tab);
            completed++;
            ReportLogger.pass("TC_013: " + label + " table scrolled to its horizontal end and back");
        }
        if (completed != 3) throw new AssertionError("TC_013 requires verified horizontal movement on Buy, Hold and Sell");
    }

    private By lapsedPitch(int tab) { return AppiumBy.accessibilityId(LAPSED_PITCH_COPY[tab]); }

    private Rectangle waitForLapsedPitch(int tab) {
        return waitFor(ExecutionProfile.defaultWait()).withMessage("TC_012/TC_013: "
                + RECOMMENDATION_TAB_LABELS[tab] + " subscription pitch and Unlock now are missing or incorrect")
                .until(d -> {
                    WebElement pitch = firstVisible(lapsedPitch(tab));
                    if (pitch == null || !normalize(LAPSED_PITCH_COPY[tab]).equals(
                            normalize(pitch.getAttribute("content-desc")))) return null;
                    Rectangle r = pitch.getRect();
                    return r.getWidth() > 0 && r.getHeight() > 0 && visible(STOCKS_COLUMN) ? r : null;
                });
    }

    /** Prefer the first data-row date highlighted in Inspector; never hardcode its value. */
    private void activateLapsedTableByDate(int tab) {
        By dateHeader = tab == 2 ? EXIT_DATE_HEADER : RECO_DATE_HEADER;
        String label = tab == 2 ? "Exit Date" : "Reco Date";
        ReportLogger.step("TC_013: " + RECOMMENDATION_TAB_LABELS[tab] + " - activating horizontal scrolling with two taps in the "
                + label + " column");
        for (int tap = 1; tap <= 2; tap++) {
            Rectangle target = settledLapsedDateTarget(dateHeader, tab, label);
            // A successful tap is submitted once; the second requested tap always
            // starts from a new lookup rather than a cached element or old bounds.
            driver.executeScript("mobile: clickGesture", Map.of(
                    "x", target.getX() + target.getWidth() / 2,
                    "y", target.getY() + target.getHeight() / 2));
            settledLapsedDateTarget(dateHeader, tab, label);
        }
    }

    private Rectangle firstLapsedDateCell(Rectangle column) {
        Rectangle first = null;
        int headerBottom = column.getY() + column.getHeight();
        for (WebElement element : driver.findElements(DATE_CELLS)) {
            Rectangle r = element.getRect();
            int centreX = r.getX() + r.getWidth() / 2;
            if (element.isDisplayed() && fullyOnScreen(r)
                    && centreX >= column.getX() && centreX < column.getX() + column.getWidth()
                    && r.getY() >= headerBottom && r.getY() < headerBottom + column.getHeight() * 2
                    && (first == null || r.getY() < first.getY())) {
                first = r;
            }
        }
        return first;
    }

    private Rectangle settledLapsedDateTarget(By dateHeader, int tab, String label) {
        Rectangle[] previous = {null};
        return waitFor(ExecutionProfile.shortWait()).withMessage("TC_013: "
                + RECOMMENDATION_TAB_LABELS[tab] + " top " + label + " heading is unavailable. "
                + "Provide its current lapsed-user XML/locator if it differs from Reco Date / Exit Date.")
                .until(d -> {
                    if (!moduleReady() || !recommendationTabSelected(RECOMMENDATION_TABS[tab])) {
                        previous[0] = null;
                        return null;
                    }
                    Rectangle bounds = visibleControlBounds(dateHeader);
                    if (bounds == null || !fullyOnScreen(bounds)) {
                        previous[0] = null;
                        return null;
                    }
                    // Some Flutter snapshots expose only the heading until activated.
                    // Re-query both it and its first data date inside stale-aware polling.
                    Rectangle date = firstLapsedDateCell(bounds);
                    if (date != null) bounds = date;
                    boolean stable = bounds.equals(previous[0]);
                    previous[0] = bounds;
                    return stable ? bounds : null;
                });
    }

    /** Use the first data row, below the headings and above the bottom pitch overlay. */
    private void swipeLapsedTableData(boolean later, boolean retry, int tab) {
        assertLapsedTab(tab);
        Rectangle rail = tableHorizontalViewport();
        Rectangle stocks = tableControlBounds(STOCKS_COLUMN);
        int left = rail.getX() + Math.max(24, (int) (rail.getWidth() * 0.12));
        int right = rail.getX() + rail.getWidth() - Math.max(24, (int) (rail.getWidth() * 0.12));
        int rowInset = Math.min((int) (stocks.getHeight() * (retry ? 0.80 : 0.55)), rail.getHeight() / 4);
        int y = rail.getY() + rowInset;
        if (!fullyOnScreen(stocks) || right - left < 60 || rowInset < 20
                || y <= stocks.getY() + stocks.getHeight()) {
            throw new AssertionError("TC_013: No usable horizontal table data area below the column headings");
        }
        int start = later ? right : left, end = later ? left : right;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "stock-analysts-choice-lapsed-data");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), start, y));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(100)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(swipeMillis), PointerInput.Origin.viewport(), end, y));
        swipe.addAction(new Pause(finger, Duration.ofMillis(40)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        try {
            driver.perform(List.of(swipe));
        } catch (WebDriverException failure) {
            try {
                driver.resetInputState();
            } catch (WebDriverException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }

    private void assertLapsedTab(int tab) {
        if (recommendationTabSelected(RECOMMENDATION_TABS[tab])) return;
        String actual = "no selected tab";
        for (int i = 0; i < RECOMMENDATION_TABS.length; i++) {
            if (recommendationTabSelected(RECOMMENDATION_TABS[i])) actual = RECOMMENDATION_TAB_LABELS[i];
        }
        throw new AssertionError("TC_013: Expected " + RECOMMENDATION_TAB_LABELS[tab]
                + " to remain selected, but found " + actual + "; the gesture changed tabs instead of scrolling the table");
    }

    private Map<String, Rectangle> settledLapsedColumns(int tab) {
        assertLapsedTab(tab);
        Map<String, Rectangle> headers = settledTableHeaders();
        assertLapsedTab(tab);
        return headers;
    }

    private boolean lapsedColumnVisible(Map<String, Rectangle> headers, String label) {
        Rectangle bounds = headers.get(label);
        Rectangle stocks = visibleControlBounds(STOCKS_COLUMN);
        return bounds != null && stocks != null && fullyOnScreen(bounds)
                && bounds.getX() >= stocks.getX() + stocks.getWidth();
    }

    private void panLapsedTableToColumn(boolean later, int tab) {
        String label = RECOMMENDATION_TAB_LABELS[tab];
        String target = later ? LAST_COLUMN_COPY : tab == 2 ? "Exit Date" : "Reco Date";
        Map<String, Rectangle> before = settledLapsedColumns(tab);
        if (later && lapsedColumnVisible(before, target)) {
            throw new AssertionError("TC_013: " + label + " already shows its last column; horizontal movement was not demonstrated");
        }
        int stalled = 0;
        for (int swipe = 0; swipe < maxHorizontalSwipes; swipe++) {
            swipeLapsedTableData(later, stalled > 0, tab);
            Map<String, Rectangle> after = settledLapsedColumns(tab);
            if (lapsedColumnVisible(after, target)) return;
            stalled = after.equals(before) ? stalled + 1 : 0;
            if (stalled >= 2) {
                throw new AssertionError("TC_013: " + label + " table did not move horizontally to "
                        + (later ? "final column '" : "first column '") + target + "'; visible headings: " + after.keySet());
            }
            before = after;
        }
        throw new AssertionError("TC_013: " + label + " did not reach column '" + target + "' within "
                + maxHorizontalSwipes + " swipes");
    }

    private By prepareRecommendationTable(By tab) {
        switchRecommendationTab(tab);
        waitForLoadedRecommendationTable();
        scrollRecommendationTableToTop();
        By dateHeader = SELL_TAB.equals(tab) ? EXIT_DATE_HEADER : RECO_DATE_HEADER;
        showFirstDateColumn(dateHeader);
        String expected = SELL_TAB.equals(tab) ? "Exit Date" : "Reco Date";
        waitUntil(() -> visibleControlBounds(dateHeader) != null,
                ExecutionProfile.defaultWait(), "Loaded date column must be exactly '" + expected + "'");
        return dateHeader;
    }

    /** A visible module shell can still contain skeleton rows after a tab/category reload. */
    private void waitForLoadedRecommendationTable() {
        List<Map<String, Rectangle>> previous = new ArrayList<>();
        previous.add(null);
        waitFor(ExecutionProfile.defaultWait()).withMessage(
                "Recommendation table did not replace its loading placeholders with live stock rows").until(d -> {
            try {
                RuntimeStock row = findLiveStock();
                Rectangle stocks = visibleControlBounds(STOCKS_COLUMN);
                if (row == null || stocks == null) {
                    previous.set(0, null);
                    return false;
                }
                Map<String, Rectangle> current = Map.of("Stocks", stocks, "row:" + row.name, row.bounds);
                boolean settled = current.equals(previous.get(0));
                previous.set(0, current);
                return settled;
            } catch (StaleElementReferenceException replacedDuringRead) {
                previous.set(0, null); // Discard the incomplete sample; query the next render again.
                return false;
            }
        });
    }

    private Rectangle visibleControlBounds(By locator) {
        WebElement element = RECO_DATE_HEADER.equals(locator) || EXIT_DATE_HEADER.equals(locator)
                ? liveDateHeader(locator) : firstVisible(locator);
        if (element == null) {
            return null;
        }
        Rectangle bounds = element.getRect();
        return bounds.getWidth() > 0 && bounds.getHeight() > 0 ? bounds : null;
    }

    /** Match the exact date label in the current Stocks header band, including Flutter View headers. */
    private WebElement liveDateHeader(By locator) {
        Rectangle stocks = visibleControlBounds(STOCKS_COLUMN);
        if (stocks == null) return null;
        boolean sell = EXIT_DATE_HEADER.equals(locator);
        String expected = sell ? "Exit Date" : "Reco Date";
        for (By candidateLocator : new By[]{locator, sell ? EXIT_DATE_HEADER_FALLBACK : RECO_DATE_HEADER_FALLBACK}) {
            WebElement match = null;
            for (WebElement element : driver.findElements(candidateLocator)) {
                Rectangle bounds = element.getRect();
                int centreY = bounds.getY() + bounds.getHeight() / 2;
                if (element.isDisplayed() && element.isEnabled()
                        && expected.equals(normalize(element.getAttribute("content-desc")))
                        && centreY >= stocks.getY() && centreY <= stocks.getY() + stocks.getHeight()
                        && bounds.getX() + bounds.getWidth() > stocks.getX() + stocks.getWidth()) {
                    if (match != null) throw new AssertionError("Multiple live '" + expected + "' table headers are visible");
                    match = element;
                }
            }
            if (match != null) return match;
        }
        return null;
    }

    /** Return immutable bounds, with lookup AND getRect inside the stale-aware wait. */
    private Rectangle tableControlBounds(By locator) {
        return waitFor(ExecutionProfile.defaultWait()).withMessage(
                "Loaded table control did not provide fresh bounds: " + locator)
                .until(d -> visibleControlBounds(locator));
    }

    private Rectangle recommendationTableViewport() {
        return waitFor(ExecutionProfile.defaultWait()).withMessage(
                "Loaded recommendation table did not provide a fresh scrolling viewport")
                .until(d -> recommendationViewport());
    }

    /** Stop at the exposed table header; never prove the top by pulling an unchanged screen twice. */
    private void scrollRecommendationTableToTop() {
        Rectangle viewport = recommendationTableViewport();
        for (int swipe = 0; swipe < maxListScrolls; swipe++) {
            Rectangle header = tableControlBounds(STOCKS_COLUMN);
            if (inside(header, viewport, 0)) return;
            swipeHub(viewport, "up", false, STOCKS_COLUMN);
            settledRecommendationPosition(null, viewport, true, ExecutionProfile.defaultWait());
        }
        throw new AssertionError("Recommendation table did not reach the top within the configured scroll limit");
    }

    private void showFirstDateColumn(By dateHeader) {
        Rectangle stocks = tableControlBounds(STOCKS_COLUMN);
        Map<String, Rectangle> before = null;
        for (int swipe = 0; swipe <= maxHorizontalSwipes; swipe++) {
            Rectangle header = waitFor(ExecutionProfile.defaultWait())
                    .until(d -> Optional.ofNullable(visibleControlBounds(dateHeader))).orElse(null);
            if (header != null && header.getX() >= stocks.getX() + stocks.getWidth()
                    && fullyOnScreen(header)) {
                return;
            }
            if (swipe == maxHorizontalSwipes) {
                break;
            }
            if (before == null) before = settledTableHeaders();
            swipeRecommendationTable(false, null, false);
            Map<String, Rectangle> after = settledTableHeaders();
            if (after.equals(before)) {
                swipeRecommendationTable(false, null, true);
                after = settledTableHeaders();
                if (after.equals(before)) {
                    break;
                }
            }
            before = after;
        }
        throw new AssertionError("The first date column could not be exposed: " + dateHeader);
    }

    private DateWindow settledDateWindow(Rectangle column, int expectedDirection, Duration timeout) {
        DateWindow[] previous = {null};
        return waitFor(timeout).withMessage(expectedDirection == 0
                ? "Live date rows did not finish loading/scrolling"
                : "Date rows did not reach the expected " + (expectedDirection > 0 ? "ascending" : "descending")
                        + " order after the date-header click").until(d -> {
            DateWindow current;
            try {
                current = readDateWindow(column);
            } catch (StaleElementReferenceException replacedDuringRead) {
                previous[0] = null;
                return null;
            }
            if (current == null || expectedDirection != 0 && current.dates.size() < 2) {
                previous[0] = null;
                return null;
            }
            if (expectedDirection != 0) {
                try {
                    int direction = chronologicalDirection(current.dates);
                    if (direction != 0 && direction != expectedDirection) {
                        previous[0] = null;
                        return null;
                    }
                } catch (AssertionError stillUpdating) {
                    previous[0] = null;
                    return null;
                }
            }
            boolean settled = previous[0] != null && current.positions.equals(previous[0].positions);
            previous[0] = current;
            return settled ? current : null;
        });
    }

    private DateWindow readDateWindow(Rectangle column) {
        Rectangle viewport = recommendationViewport();
        int dataTop = dateTableDataTop(viewport);
        List<DateCell> cells = new ArrayList<>();
        for (WebElement element : driver.findElements(DATE_CELLS)) {
            Rectangle r = element.getRect();
            int centre = r.getX() + r.getWidth() / 2;
            if (centre >= column.getX() && centre < column.getX() + column.getWidth()
                    && r.getY() >= dataTop
                    && inside(r, viewport, 0) && element.isDisplayed()) {
                String text = normalize(element.getAttribute("content-desc"));
                cells.add(new DateCell(LocalDate.parse(text, RECOMMENDATION_DATE_FORMAT), r));
            }
        }
        cells.sort(Comparator.comparingInt(c -> c.bounds.getY()));
        List<LocalDate> dates = new ArrayList<>();
        Map<String, Rectangle> positions = new LinkedHashMap<>();
        for (DateCell cell : cells) {
            positions.put(dates.size() + ":" + cell.date, cell.bounds);
            dates.add(cell.date);
        }
        Rectangle screen = usableScreenBounds();
        Rectangle footer = visibleControlBounds(WIDGET_CTA);
        boolean footerVisible = footer != null && inside(footer, screen, 0);
        if (footerVisible) positions.put("footer", footer);
        // The captured header/title collapse while scrolling. A visible title plus
        // the original leading dates identifies the return to the first rows.
        Rectangle title = visibleControlBounds(TITLE);
        boolean topVisible = title != null && inside(title, screen, 0);
        if (topVisible) positions.put("top", title);
        return dates.isEmpty() && !footerVisible ? null : new DateWindow(dates, positions, footerVisible, topVisible);
    }

    private int dateTableDataTop(Rectangle viewport) {
        Rectangle header = visibleControlBounds(STOCKS_COLUMN);
        return header == null ? viewport.getY() : Math.max(viewport.getY(), header.getY() + header.getHeight());
    }

    private static boolean dateBoundaryReached(DateWindow window, boolean toBottom, List<LocalDate> firstDates) {
        if (toBottom) return window.footerVisible;
        int count = Math.min(firstDates.size(), window.dates.size());
        return window.topVisible && count > 0 && window.dates.subList(0, count).equals(firstDates.subList(0, count));
    }

    /** Check only chronological date order while scrolling down or back up. */
    private DateWindow verifyDateOrderToBoundary(Rectangle column, DateWindow window, int expectedDirection,
                                                boolean toBottom, String tab, List<LocalDate> firstDates) {
        List<LocalDate> dates = new ArrayList<>(window.dates);
        for (int swipe = 0; swipe <= maxListScrolls; swipe++) {
            assertDateOrder(dates, expectedDirection, tab);
            if (dateBoundaryReached(window, toBottom, firstDates)) {
                if (chronologicalDirection(dates) == 0) {
                    throw new AssertionError(tab + " needs at least two distinct dates to verify sorting");
                }
                return window;
            }
            if (swipe == maxListScrolls) break;
            Rectangle viewport = recommendationTableViewport();
            int top = dateTableDataTop(viewport);
            // Flutter's nested horizontal data pane can report canScrollMore=false
            // while the outer list is still moving. Observe the screen markers instead.
            driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", viewport.getX(), "top", top, "width", viewport.getWidth(),
                    "height", viewport.getY() + viewport.getHeight() - top,
                    "direction", toBottom ? "down" : "up", "percent", 0.6));
            DateWindow next = settledDateWindow(column, 0, ExecutionProfile.defaultWait());
            if (next.positions.equals(window.positions) && !dateBoundaryReached(next, toBottom, firstDates)) {
                swipeHub(viewport, toBottom ? "down" : "up", true, toBottom ? WIDGET_CTA : TITLE);
                next = settledDateWindow(column, 0, ExecutionProfile.defaultWait());
            }
            if (toBottom) {
                appendDateWindow(dates, next.dates);
            } else {
                List<LocalDate> earlier = new ArrayList<>(next.dates);
                appendDateWindow(earlier, dates);
                dates = earlier;
            }
            window = next;
        }
        throw new AssertionError(tab + " list did not reach the " + (toBottom ? "bottom" : "top")
                + " within " + maxListScrolls + " scrolls");
    }

    private static void assertDateOrder(List<LocalDate> dates, int expectedDirection, String tab) {
        int actual = chronologicalDirection(dates);
        if (actual != 0 && actual != expectedDirection) {
            throw new AssertionError(tab + " dates must be "
                    + (expectedDirection > 0 ? "ascending (oldest first)" : "descending (newest first)"));
        }
    }

    /** Remove the date overlap between successive viewports; retain tied dates within a viewport. */
    private static void appendDateWindow(List<LocalDate> collected, List<LocalDate> next) {
        int overlap = Math.min(collected.size(), next.size());
        while (overlap > 0 && !collected.subList(collected.size() - overlap, collected.size())
                .equals(next.subList(0, overlap))) {
            overlap--;
        }
        collected.addAll(next.subList(overlap, next.size()));
    }

    private static int chronologicalDirection(List<LocalDate> dates) {
        int direction = 0;
        for (int row = 1; row < dates.size(); row++) {
            int comparison = Integer.signum(dates.get(row).compareTo(dates.get(row - 1)));
            if (comparison != 0) {
                if (direction != 0 && comparison != direction) {
                    throw new AssertionError("Dates are not chronologically sorted: " + dates.get(row - 1)
                            + " followed by " + dates.get(row));
                }
                direction = comparison;
            }
        }
        return direction;
    }

    private List<HeaderCell> visibleTableHeaders() {
        return waitFor(ExecutionProfile.defaultWait()).withMessage("Live table column headers are unavailable")
                .until(d -> {
                    List<HeaderCell> headers = readTableHeaders();
                    return headers.isEmpty() ? null : headers;
                });
    }

    private List<HeaderCell> readTableHeaders() {
        Rectangle stocks = visibleControlBounds(STOCKS_COLUMN);
        if (stocks == null) {
            return List.of();
        }
        Rectangle screen = usableScreenBounds();
        List<HeaderCell> headers = new ArrayList<>();
        for (WebElement element : driver.findElements(TABLE_HEADER_CELLS)) {
            Rectangle r = element.getRect();
            int centreY = r.getY() + r.getHeight() / 2;
            if (centreY >= stocks.getY() && centreY <= stocks.getY() + stocks.getHeight()
                    && r.getX() + r.getWidth() > stocks.getX() + stocks.getWidth()
                    && r.getX() < screen.getX() + screen.getWidth() && r.getWidth() > 0
                    && element.isDisplayed()) {
                headers.add(new HeaderCell(normalize(element.getAttribute("content-desc")), r));
            }
        }
        headers.sort(Comparator.comparingInt(h -> h.bounds.getX()));
        return headers;
    }

    private Map<String, Rectangle> settledTableHeaders() {
        List<Map<String, Rectangle>> previous = new ArrayList<>();
        previous.add(null);
        return waitFor(ExecutionProfile.defaultWait()).withMessage("Table column headers did not settle").until(d -> {
            Map<String, Rectangle> current = new LinkedHashMap<>();
            try {
                for (HeaderCell header : readTableHeaders()) {
                    current.put(header.label, header.bounds);
                }
            } catch (StaleElementReferenceException replacedDuringRead) {
                previous.set(0, null);
                return null;
            }
            if (current.isEmpty()) {
                previous.set(0, null);
                return null;
            }
            boolean settled = current.equals(previous.get(0));
            previous.set(0, current);
            return settled ? current : null;
        });
    }

    /** Choose the table's large data rail rather than Buy's short category-chip rail. */
    private Rectangle tableHorizontalViewport() {
        return waitFor(ExecutionProfile.defaultWait()).withMessage(
                "The live horizontal table scroll container is not available")
                .until(d -> readTableHorizontalViewport());
    }

    private Rectangle readTableHorizontalViewport() {
        Rectangle stocks = visibleControlBounds(STOCKS_COLUMN);
        if (stocks == null) {
            return null;
        }
        Rectangle screen = usableScreenBounds();
        Rectangle best = null;
        for (WebElement element : driver.findElements(TABLE_HORIZONTAL_SCROLL)) {
            Rectangle r = element.getRect();
            int left = Math.max(stocks.getX() + stocks.getWidth(), Math.max(screen.getX(), r.getX()));
            int right = Math.min(screen.getX() + screen.getWidth(), r.getX() + r.getWidth());
            int top = Math.max(stocks.getY() + stocks.getHeight(), r.getY());
            int bottom = Math.min(screen.getY() + screen.getHeight(), r.getY() + r.getHeight());
            if (element.isDisplayed() && right - left >= 80 && bottom - top >= screen.getHeight() * 0.15) {
                Rectangle candidate = new Rectangle(new Point(left, top), new Dimension(right - left, bottom - top));
                if (best == null || candidate.getHeight() > best.getHeight()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private void swipeRecommendationTable(boolean later, Rectangle firstColumn, boolean retry) {
        Rectangle rail = tableHorizontalViewport();
        int left = rail.getX() + Math.max(18, (int) (rail.getWidth() * 0.18));
        int right = rail.getX() + Math.min(rail.getWidth() - 18, (int) (rail.getWidth() * 0.84));
        int start = later ? right : left;
        if (later && firstColumn != null) {
            start = Math.min(right, Math.max(left + 60, firstColumn.getX() + firstColumn.getWidth() / 2));
        }
        int end = later ? left : right;
        int y = rail.getY() + (int) (rail.getHeight() * (retry ? 0.65 : 0.35));
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "stock-analysts-choice-table");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), start, y));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(10)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(swipeMillis), PointerInput.Origin.viewport(), end, y));
        swipe.addAction(new Pause(finger, Duration.ofMillis(40)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        try {
            driver.perform(List.of(swipe));
        } catch (WebDriverException failure) {
            try {
                driver.resetInputState();
            } catch (WebDriverException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }

    private static final class DateCell {
        private final LocalDate date;
        private final Rectangle bounds;
        private DateCell(LocalDate date, Rectangle bounds) { this.date = date; this.bounds = bounds; }
    }

    private static final class DateWindow {
        private final List<LocalDate> dates;
        private final Map<String, Rectangle> positions;
        private final boolean footerVisible;
        private final boolean topVisible;
        private DateWindow(List<LocalDate> dates, Map<String, Rectangle> positions, boolean footerVisible, boolean topVisible) {
            this.dates = dates;
            this.positions = positions;
            this.footerVisible = footerVisible;
            this.topVisible = topVisible;
        }
    }

    private static final class HeaderCell {
        private final String label;
        private final Rectangle bounds;
        private HeaderCell(String label, Rectangle bounds) { this.label = label; this.bounds = bounds; }
    }

    private void ensureNoStockSelection() {
        waitForModuleReady();
        if (visible(ADD_WATCHLIST)) {
            // A previous/manual selection must not hide the footer in this case.
            // Re-enter once through Hub, then explicitly verify a clean module.
            returnToHub();
            openFromHub();
            waitUntil(() -> !visible(ADD_WATCHLIST), ExecutionProfile.defaultWait(),
                    "Recommendations retained selected stocks after reopening; cannot validate the unselected footer");
        }
    }

    private void switchRecommendationTab(By tab) {
        waitForModuleReady();
        if (!recommendationTabSelected(tab)) tapNativeControl(tab);
        waitUntil(() -> recommendationTabSelected(tab) && visible(SCROLL), ExecutionProfile.defaultWait(),
                "The requested recommendation tab did not load its list");
        if (BUY_TAB.equals(tab) && visible(ALL_CATEGORY) && !recommendationTabSelected(ALL_CATEGORY)) {
            tapNativeControl(ALL_CATEGORY);
        }
        if (visible(ADD_WATCHLIST)) {
            throw new AssertionError("Stock selection must be empty while checking recommendation tabs");
        }
    }

    private boolean recommendationTabSelected(By tab) {
        try {
            WebElement element = firstVisible(tab);
            return element != null && element.isSelected();
        } catch (StaleElementReferenceException replaced) {
            return false;
        }
    }

    private void verifyToolAndReturn(By heading, String label) {
        waitUntil(() -> visible(heading) && visible(BACK) && !visible(TITLE),
                ExecutionProfile.longWait(), label + " did not open from Analyst's Choice");
        assertSemantic(heading, label);
        ReportLogger.pass("TC_008: " + label + " heading verified");
        if (driver.isKeyboardShown()) {
            ReportLogger.step("TC_008: Closing the keyboard before returning from " + label);
            driver.hideKeyboard();
            waitUntil(() -> !driver.isKeyboardShown(), ExecutionProfile.shortWait(),
                    "Keyboard did not close before returning from " + label);
        }
        ReportLogger.step("TC_008: Using Back to return from " + label + " to Analyst's Choice");
        tapBack(BACK);
        try {
            waitUntil(() -> moduleReady() && !visible(heading), ExecutionProfile.shortWait(),
                    "Waiting for Analyst's Choice after " + label + " Back");
        } catch (TimeoutException firstBackWait) {
            // Follow Batch 1's unchanged-screen fallback and Planner's Android Back flow.
            // A returning module can expose its heading before its list is ready.
            // In that state, finish waiting without issuing another Back.
            if (visible(heading) && visible(BACK) && !visible(TITLE)) {
                ReportLogger.step("TC_008: " + label + " is still open; using Android Back once");
                driver.navigate().back();
            } else {
                ReportLogger.step(visible(TITLE)
                        ? "TC_008: Waiting for Analyst's Choice to finish loading"
                        : "TC_008: Waiting for Analyst's Choice to appear after Back");
            }
            try {
                waitUntil(() -> moduleReady() && !visible(heading), ExecutionProfile.defaultWait(),
                        label + " Back did not return to Analyst's Choice; watchlist steps were not started");
            } catch (TimeoutException returnFailure) {
                returnFailure.addSuppressed(firstBackWait);
                throw returnFailure;
            }
        }
        ReportLogger.pass("TC_008: Returned from " + label + " to Analyst's Choice");
    }

    private Rectangle recommendationViewport() {
        Rectangle screen = usableScreenBounds();
        Rectangle best = null;
        for (WebElement scroll : driver.findElements(SCROLL)) {
            Rectangle r = scroll.getRect();
            if (scroll.isDisplayed() && scroll.isEnabled() && r.getWidth() >= screen.getWidth() * 4 / 5) {
                int top = Math.max(screen.getY(), r.getY());
                int bottom = Math.min(screen.getY() + screen.getHeight(), r.getY() + r.getHeight());
                int left = Math.max(screen.getX(), r.getX());
                int right = Math.min(screen.getX() + screen.getWidth(), r.getX() + r.getWidth());
                if (right <= left || bottom <= top) {
                    continue;
                }
                Rectangle clipped = new Rectangle(new Point(left, top), new Dimension(right - left, bottom - top));
                if (clipped.getHeight() > 160 && (best == null || clipped.getHeight() > best.getHeight())) {
                    best = clipped;
                }
            }
        }
        if (best == null) {
            throw new IllegalStateException("Analyst's Choice has no usable list scrolling area");
        }
        return best;
    }

    private boolean widgetReady(Rectangle viewport) {
        return hubElement(WIDGET_HEADING, viewport) != null && hubElement(WIDGET_SUBTITLE, viewport) != null
                && hubElement(WIDGET_CTA, viewport) != null;
    }

    private boolean listTopReady(Rectangle viewport) {
        return hubElement(ALL_CATEGORY, viewport) != null && hubElement(ALL_WEATHER_CATEGORY, viewport) != null
                && hubElement(STOCKS_COLUMN, viewport) != null;
    }

    /** Uses the existing fast W3C swipe and settled-anchor pattern, inside this list only. */
    private void scrollRecommendationList(boolean toBottom) {
        waitForModuleReady();
        Rectangle viewport = recommendationViewport();
        Map<String, Rectangle> before = settledRecommendationPosition(null, viewport, toBottom,
                ExecutionProfile.defaultWait());
        for (int swipe = 0; swipe < maxListScrolls; swipe++) {
            if (before.containsKey("goal")) {
                return;
            }
            if (visible(ADD_WATCHLIST)) {
                throw new AssertionError("A stock was selected while scrolling the unselected list");
            }
            By target = toBottom ? WIDGET_CTA : ALL_CATEGORY;
            String direction = toBottom ? "down" : "up";
            swipeHub(viewport, direction, false, target);
            Map<String, Rectangle> after = settledRecommendationPosition(before, viewport, toBottom,
                    ExecutionProfile.defaultWait());
            if (!after.containsKey("goal") && after.equals(before)) {
                swipeHub(viewport, direction, true, target);
                after = settledRecommendationPosition(before, viewport, toBottom, ExecutionProfile.defaultWait());
                if (!after.containsKey("goal") && after.equals(before)) {
                    throw new AssertionError("The list stopped moving before reaching " + (toBottom ? "the updated footer" : "the top"));
                }
            }
            before = after;
        }
        if (!before.containsKey("goal")) {
            throw new AssertionError("Could not reach " + (toBottom ? "the updated footer" : "the top")
                    + " within " + maxListScrolls + " list swipes");
        }
    }

    private Map<String, Rectangle> settledRecommendationPosition(Map<String, Rectangle> reference,
                                                                 Rectangle viewport, boolean bottom, Duration timeout) {
        final List<Map<String, Rectangle>> previous = new ArrayList<>();
        previous.add(null);
        return waitFor(timeout).withMessage("Recommendation list has not finished loading/moving").until(d -> {
            boolean goal = bottom ? hubElement(WIDGET_CTA, viewport) != null : listTopReady(viewport);
            Map<String, Rectangle> current = goal
                    ? Map.of("goal", require(bottom ? WIDGET_CTA : ALL_CATEGORY, ExecutionProfile.shortWait()).getRect())
                    : recommendationAnchor(viewport, previous.get(0) == null ? reference : previous.get(0));
            if (current == null) {
                previous.set(0, null);
                return null;
            }
            boolean settled = current.equals(previous.get(0));
            previous.set(0, current);
            return settled ? current : null;
        });
    }

    private Map<String, Rectangle> recommendationAnchor(Rectangle viewport, Map<String, Rectangle> reference) {
        if (reference != null && !reference.containsKey("goal")) {
            String label = reference.keySet().iterator().next();
            WebElement previous = hubElement(AppiumBy.accessibilityId(label), viewport);
            if (previous != null) {
                return Map.of(label, previous.getRect());
            }
        }
        WebElement header = firstVisible(STOCKS_COLUMN);
        Rectangle column = header == null ? null : header.getRect();
        for (WebElement candidate : driver.findElements(STOCK_NAME_VIEWS)) {
            String label = normalize(candidate.getAttribute("content-desc"));
            Rectangle r = candidate.getRect();
            if (candidate.isDisplayed() && inside(r, viewport, 0)
                    && (label.equals(WIDGET_HEADING_COPY) || label.equals(WIDGET_SUBTITLE_COPY)
                    || label.equals("Take me there") || (column != null && stockNameCell(label, r, column, viewport)))) {
                return Map.of(label, r);
            }
        }
        return null;
    }

    private void verifyEndOfListWidget() {
        Rectangle viewport = recommendationViewport();
        for (String oldCopy : OLD_WIDGET_COPY) {
            By description = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + oldCopy + "\")");
            By text = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + oldCopy + "\")");
            if (visible(description) || visible(text)) {
                throw new AssertionError("Old end-of-list widget copy remains displayed: " + oldCopy);
            }
        }
        waitUntil(() -> widgetReady(viewport) && !visible(ADD_WATCHLIST), ExecutionProfile.defaultWait(),
                "The complete updated footer is not visible with stock selection empty");
        assertSemantic(WIDGET_HEADING, WIDGET_HEADING_COPY);
        assertSemantic(WIDGET_SUBTITLE, WIDGET_SUBTITLE_COPY);
        assertSemantic(WIDGET_CTA, "Take me there");
    }

    private static Point checkboxPoint(Rectangle numberColumn, Rectangle row, Rectangle viewport) {
        int x = numberColumn.getX() + numberColumn.getWidth() / 2;
        int y = row.getY() + row.getHeight() / 2;
        if (!inside(numberColumn, viewport, 0) || !inside(row, viewport, 0)
                || x >= row.getX() || y <= numberColumn.getY() + numberColumn.getHeight()) {
            throw new IllegalStateException("Could not safely align the stock checkbox with the # column and live row");
        }
        return new Point(x, y);
    }

    private boolean watchlistDrawerVisible() {
        return visible(ADD_EXISTING_WATCHLIST) && visible(CREATE_WATCHLIST) && visible(WATCHLIST_CLOSE);
    }

    private boolean drawerStockVisible(String name) {
        if (!watchlistDrawerVisible()) {
            return false;
        }
        int top = require(CREATE_WATCHLIST, ExecutionProfile.shortWait()).getRect().getY();
        int bottom = require(WATCHLIST_CLOSE, ExecutionProfile.shortWait()).getRect().getY();
        for (WebElement stock : driver.findElements(AppiumBy.accessibilityId(name))) {
            Rectangle r = stock.getRect();
            if (stock.isDisplayed() && fullyOnScreen(r) && r.getY() > top && r.getY() + r.getHeight() <= bottom) {
                return true;
            }
        }
        return false;
    }

    private WebElement watchlistNameInput() {
        if (!visible(WATCHLIST_NAME_LABEL)) {
            return null;
        }
        WebElement result = null;
        for (WebElement input : driver.findElements(INPUT)) {
            if (input.isDisplayed() && input.isEnabled()) {
                if (result != null) {
                    throw new IllegalStateException("Multiple watchlist inputs are visible; refusing to guess");
                }
                result = input;
            }
        }
        return result;
    }

    private void typeWatchlistName(String name) {
        for (int attempt = 1; attempt <= maxClickAttempts; attempt++) {
            try {
                WebElement input = waitFor(ExecutionProfile.defaultWait()).until(d -> watchlistNameInput());
                input.click();
                input.clear();
                input.sendKeys(name);
                try {
                    waitUntil(() -> {
                        WebElement current = watchlistNameInput();
                        return current != null && name.equals(current.getText().trim());
                    }, ExecutionProfile.shortWait(), "The watchlist name field did not retain the generated name");
                } catch (TimeoutException failure) {
                    WebElement current = watchlistNameInput();
                    String actual = current == null ? "<name field not visible>" : current.getText().trim();
                    throw new AssertionError("Watchlist name verification failed. Expected: '" + name
                            + "' (" + name.length() + " characters); field contains: '" + actual
                            + "'. Add to watchlist was not tapped.", failure);
                }
                return;
            } catch (StaleElementReferenceException | NoSuchElementException failure) {
                if (attempt == maxClickAttempts) {
                    throw failure;
                }
            }
        }
    }

    private WebElement watchlistSubmitButton() {
        WebElement close = firstVisible(WATCHLIST_CLOSE);
        if (close == null || !watchlistDrawerVisible()) {
            return null;
        }
        Rectangle footer = close.getRect();
        int footerCentre = footer.getY() + footer.getHeight() / 2;
        for (WebElement candidate : driver.findElements(ADD_WATCHLIST)) {
            Rectangle r = candidate.getRect();
            if (candidate.isDisplayed() && candidate.isEnabled() && fullyOnScreen(r)
                    && r.getX() + r.getWidth() <= footer.getX()
                    && Math.abs(r.getY() + r.getHeight() / 2 - footerCentre) <= Math.max(r.getHeight(), footer.getHeight()) / 4) {
                return candidate; // Same footer row as Close, excluding the duplicate drawer heading.
            }
        }
        return null;
    }

    private void tapWatchlistSubmitButton() {
        // The keyboard can resize the drawer. Reacquire the visible button beside
        // Close and tap it directly, leaving the keyboard open during submission.
        Rectangle[] previous = {null};
        WebElement button = waitFor(ExecutionProfile.defaultWait())
                .withMessage("The bottom Add to watchlist button beside Close did not become ready")
                .until(d -> {
                    WebElement current = watchlistSubmitButton();
                    if (current == null) {
                        previous[0] = null;
                        return null;
                    }
                    Rectangle bounds = current.getRect();
                    boolean settled = bounds.equals(previous[0]);
                    previous[0] = bounds;
                    return settled ? current : null;
                });
        // Do not retry submission: a delayed response must not create duplicate watchlists.
        driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) button).getId()));
    }

    private static String nextWatchlistName() {
        // Seconds since 2020 give a compact numeric suffix (20 characters in this build).
        // The atomic counter keeps consecutive names distinct within this test run.
        long number = WATCHLIST_NUMBER.updateAndGet(previous -> Math.max(previous + 1,
                System.currentTimeMillis() / 1000L - WATCHLIST_EPOCH_SECONDS));
        return "Watchlist[" + number + "]";
    }

    private static boolean watchlistSuccessMatches(String notice, String name) {
        return normalize(notice).equalsIgnoreCase("Please note Added to '" + name + "' watchlist");
    }

    private void closeWatchlistForCleanup() {
        if (watchlistDrawerWasOpened && watchlistDrawerVisible()) {
            if (driver.isKeyboardShown()) {
                driver.hideKeyboard();
            }
            tapNativeControl(WATCHLIST_CLOSE);
            waitUntil(() -> !watchlistDrawerVisible(), ExecutionProfile.shortWait(),
                    "The watchlist drawer did not close during Hub cleanup");
        }
        watchlistDrawerWasOpened = false;
    }

    private RuntimeStock findLiveStock() {
        WebElement stocks = firstVisible(STOCKS_COLUMN);
        if (stocks == null || stockDetailsPresent()) {
            return null;
        }
        Rectangle column = stocks.getRect();
        Rectangle viewport = usableScreenBounds();
        List<RuntimeStock> candidates = new ArrayList<>();
        // Stock names can be in the frozen column outside the horizontal rails.
        // Find native description-bearing cells, then restrict them to the live
        // Stocks column and visible table area. This excludes chips/date columns
        // without assuming that stock rows are HorizontalScrollView descendants.
        for (WebElement row : driver.findElements(STOCK_NAME_VIEWS)) {
            String name = normalize(row.getAttribute("content-desc"));
            Rectangle rect = row.getRect();
            if (row.isDisplayed() && row.isEnabled() && stockNameCell(name, rect, column, viewport)) {
                candidates.add(new RuntimeStock(name, rect));
            }
        }
        candidates.sort(Comparator.comparingInt((RuntimeStock s) -> s.bounds.getY())
                .thenComparingInt(s -> s.bounds.getX()));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean stockNameCell(String name, Rectangle rect, Rectangle column, Rectangle viewport) {
        int centerX = rect.getX() + rect.getWidth() / 2;
        return !name.isEmpty() && name.matches("(?s).*[A-Za-z].*") && inside(rect, viewport, 0)
                && rect.getY() >= column.getY() + column.getHeight()
                && centerX >= column.getX() && centerX <= column.getX() + column.getWidth()
                && rect.getHeight() <= Math.max(1, column.getHeight()) * 2;
    }

    private Rectangle stockTableViewport(Rectangle row) {
        Rectangle best = null;
        Rectangle header = require(STOCKS_COLUMN, ExecutionProfile.shortWait()).getRect();
        for (WebElement scroll : driver.findElements(SCROLL)) {
            Rectangle r = scroll.getRect();
            if (scroll.isDisplayed() && inside(row, r, 0) && inside(header, r, 0)
                    && (best == null || r.getY() > best.getY())) {
                best = r;
            }
        }
        if (best == null) {
            throw new IllegalStateException("Could not capture the live stock-table ScrollView before the stock tap");
        }
        return best;
    }

    private StockPanelSnapshot captureStockPanel() {
        if (selectedStockName == null || stockTableViewport == null || stockModuleTitle == null) {
            return null;
        }
        Rectangle viewport = usableScreenBounds();
        // The latest Inspector XML exposes only ScrollView/View containers in
        // this panel. Native company/chart labels are optional corroboration.
        // The panel itself must differ from the pre-tap table bounds and settle.
        List<WebElement> headings = driver.findElements(STOCK_NAME_VIEWS);
        List<WebElement> charts = driver.findElements(STOCK_CHART);
        List<WebElement> containers = driver.findElements(SCROLL);
        List<WebElement> backs = driver.findElements(BACK);
        List<WebElement> views = backs.isEmpty() ? List.of() : driver.findElements(STOCK_PANEL_VIEWS);
        StockPanelSnapshot best = null;
        for (WebElement content : containers) {
            if (!content.isDisplayed() || !content.isEnabled()) {
                continue;
            }
            Rectangle contentRect = content.getRect();
            int edgeTolerance = Math.max(2, viewport.getHeight() / 200);
            if (!inside(contentRect, viewport, 0)
                    || contentRect.getWidth() < viewport.getWidth() * 4 / 5
                    || Math.abs(contentRect.getY() + contentRect.getHeight()
                    - (viewport.getY() + viewport.getHeight())) > edgeTolerance) {
                continue;
            }
            Rectangle toolbar = stockToolbarBounds(contentRect, backs, views, edgeTolerance);
            int minimumShift = Math.max(2, stockModuleTitle.getHeight() / 2);
            boolean partial = contentRect.getY() - stockTableViewport.getY() >= minimumShift
                    && stockTableViewport.getHeight() - contentRect.getHeight() >= minimumShift;
            if (toolbar == null && !partial) {
                continue; // An unchanged table ScrollView is not an opened drawer.
            }

            String company = null;
            Rectangle headingRect = null;
            Rectangle chartRect = null;
            for (WebElement heading : headings) {
                String label = normalize(heading.getAttribute("content-desc"));
                if (!stockHeadingMatches(label)) {
                    continue;
                }
                Rectangle r = heading.getRect();
                if (heading.isDisplayed() && inside(r, contentRect, 0)
                        && r.getY() - contentRect.getY() <= Math.max(1, stockModuleTitle.getHeight()) * 2
                        && (headingRect == null || r.getY() < headingRect.getY())) {
                    company = label;
                    headingRect = r;
                }
            }
            if (company != null && selectedStockHeader != null && !sameCompany(selectedStockHeader, company)) {
                continue;
            }
            for (WebElement chart : charts) {
                Rectangle r = chart.getRect();
                if (chart.isDisplayed() && inside(r, contentRect, 0)) {
                    chartRect = r;
                    break;
                }
            }
            if (best == null || contentRect.getY() > best.content.getY()) {
                best = new StockPanelSnapshot(company, contentRect, headingRect, chartRect, toolbar);
            }
        }
        return best;
    }

    private static Rectangle stockToolbarBounds(Rectangle content, List<WebElement> backs,
                                                List<WebElement> views, int tolerance) {
        Rectangle best = null;
        for (WebElement back : backs) {
            if (!back.isDisplayed()) {
                continue;
            }
            Rectangle backRect = back.getRect();
            for (WebElement view : views) {
                Rectangle r = view.getRect();
                if (inside(backRect, r, 0)
                        && r.getWidth() >= content.getWidth() * 4 / 5 && r.getY() > 0
                        && Math.abs(r.getY() + r.getHeight() - content.getY()) <= tolerance
                        && (best == null || r.getHeight() < best.getHeight())
                        && view.isDisplayed() && view.isEnabled()) {
                    best = r; // The selected full-width View containing Go back.
                }
            }
        }
        return best;
    }

    private boolean stockHeadingMatches(String company) {
        String heading = normalize(company).toLowerCase(Locale.ROOT);
        String selected = normalize(selectedStockName).toLowerCase(Locale.ROOT);
        return heading.equals(selected) || heading.startsWith(selected + " ");
    }

    private StockPanelSnapshot waitForSettledStockPanel(Duration timeout, String message) {
        final List<StockPanelSnapshot> previous = new ArrayList<>();
        previous.add(null);
        return waitFor(timeout).withMessage(message).until(d -> {
            StockPanelSnapshot current = captureStockPanel();
            if (current == null) {
                previous.set(0, null);
                return null;
            }
            boolean stable = current.sameGeometry(previous.get(0));
            previous.set(0, current);
            return stable ? current : null;
        });
    }

    private StockPanelSnapshot requireStockPanel(String message) {
        return waitFor(ExecutionProfile.shortWait()).withMessage(message).until(d -> captureStockPanel());
    }

    private static boolean expandedToFullScreen(StockPanelSnapshot initial, StockPanelSnapshot current,
                                               Rectangle moduleBack, Rectangle moduleTitle) {
        int minimumMovement = Math.max(1, moduleTitle.getHeight()) * 2;
        return samePanelCompany(initial.company, current.company) && current.toolbar != null
                && initial.content.getY() - current.content.getY() >= minimumMovement
                && current.content.getHeight() - initial.content.getHeight() >= minimumMovement
                && current.content.getY() <= moduleBack.getY() + moduleBack.getHeight() + moduleTitle.getHeight()
                && (current.heading == null
                || current.heading.getY() <= moduleTitle.getY() + moduleTitle.getHeight());
    }

    private boolean buyAllRestoredAfterStockDismissal() {
        return !stockDetailsPresent() && moduleReady() && visible(BUY_TAB) && visible(ALL_CATEGORY)
                && visible(ALL_WEATHER_CATEGORY) && visible(STOCKS_COLUMN)
                && visible(AppiumBy.accessibilityId(selectedStockName));
    }

    private boolean stockDetailsPresent() {
        // Presence rather than viewport visibility prevents a scrolled-off chart
        // from being mistaken for a dismissed drawer.
        // Do not use the company name alone here: the restored table may expose
        // exactly the same name as the stock-details heading.
        return !driver.findElements(STOCK_CHART).isEmpty()
                || (stockTableViewport != null && captureStockPanel() != null);
    }

    private Rectangle usableScreenBounds() {
        Dimension size = driver.manage().window().getSize();
        int bottom = size.getHeight();
        for (WebElement navigation : driver.findElements(ANDROID_NAVIGATION_BAR)) {
            Rectangle r = navigation.getRect();
            if (r.getY() > 0 && r.getHeight() > 0) {
                bottom = Math.min(bottom, r.getY());
            }
        }
        return new Rectangle(new Point(0, 0), new Dimension(size.getWidth(), bottom));
    }

    private static StockDrag expansionDrag(StockPanelSnapshot current, Rectangle moduleBack,
                                          Rectangle moduleTitle, Rectangle viewport) {
        int x = current.content.getX() + current.content.getWidth() / 2;
        // Screenshot 1 selects the ScrollView body, not the band above it.
        // Start inside its upper portion so the sheet receives the upward drag.
        int startY = current.content.getY() + stockBodyInset(current.content, moduleTitle);
        int endY = moduleBack.getY() + moduleBack.getHeight() + 1;
        return boundedStockDrag(x, startY, endY, viewport);
    }

    private static StockDrag dismissalDrag(StockPanelSnapshot current, boolean fullScreen,
                                          Rectangle moduleTitle, Rectangle viewport) {
        if (fullScreen && current.toolbar == null) {
            throw new IllegalStateException("Full-screen dismissal requires the live stock toolbar bounds");
        }
        // Screenshot 2 selects the whole toolbar View: press its actual centre.
        int x = fullScreen ? current.toolbar.getX() + current.toolbar.getWidth() / 2
                : current.content.getX() + current.content.getWidth() / 2;
        int startY = fullScreen ? current.toolbar.getY() + current.toolbar.getHeight() / 2
                : current.content.getY() + stockBodyInset(current.content, moduleTitle);
        int endY = viewport.getY() + viewport.getHeight() - Math.max(1, moduleTitle.getHeight()) - 1;
        return boundedStockDrag(x, startY, endY, viewport);
    }

    private static int stockBodyInset(Rectangle content, Rectangle moduleTitle) {
        return Math.max(1, Math.min(moduleTitle.getHeight(), content.getHeight() / 10));
    }

    private static StockDrag boundedStockDrag(int x, int startY, int endY, Rectangle viewport) {
        int safeX = Math.max(viewport.getX() + 1, Math.min(viewport.getX() + viewport.getWidth() - 2, x));
        int safeStart = Math.max(viewport.getY() + 1, Math.min(viewport.getY() + viewport.getHeight() - 2, startY));
        int safeEnd = Math.max(viewport.getY() + 1, Math.min(viewport.getY() + viewport.getHeight() - 2, endY));
        if (safeStart == safeEnd) {
            throw new IllegalStateException("Stock panel has no usable drag distance");
        }
        return new StockDrag(new Point(safeX, safeStart), new Point(safeX, safeEnd));
    }

    private void performStockDrag(StockDrag drag) {
        long durationMillis = stockDragDurationMillis(drag, stockDragReferenceDistance, stockDragMillis);
        if (stockGestureDebug) {
            ReportLogger.debug("TC_006 gesture: (" + drag.start.getX() + "," + drag.start.getY()
                    + ") -> (" + drag.end.getX() + "," + drag.end.getY() + "), " + durationMillis + " ms");
        }
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "stock-analysts-choice-drawer");
        Sequence gesture = new Sequence(finger, 0);
        gesture.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), drag.start.getX(), drag.start.getY()));
        gesture.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        gesture.addAction(new Pause(finger, Duration.ofMillis(95))); // Press the live top band before dragging.
        gesture.addAction(finger.createPointerMove(Duration.ofMillis(durationMillis), PointerInput.Origin.viewport(), drag.end.getX(), drag.end.getY()));
        gesture.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        try {
            driver.perform(List.of(gesture));
        } catch (WebDriverException failure) {
            try {
                driver.resetInputState();
            } catch (WebDriverException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }

    private static double stockDragDistance(StockDrag drag) {
        return Math.hypot(drag.end.getX() - drag.start.getX(), drag.end.getY() - drag.start.getY());
    }

    private static long stockDragDurationMillis(StockDrag drag, double referenceDistance, int referenceMillis) {
        if (referenceDistance <= 0 || referenceMillis <= 0) {
            throw new IllegalStateException("Stock drag speed must be calibrated before swiping");
        }
        return Math.max(1L, Math.round(referenceMillis * stockDragDistance(drag) / referenceDistance));
    }

    private void closeStockDetailsForCleanup() {
        for (int attempt = 0; attempt < 3 && stockDetailsPresent(); attempt++) {
            driver.navigate().back();
            try {
                waitUntil(() -> !stockDetailsPresent(), ExecutionProfile.shortWait(), "Waiting for stock panel cleanup");
            } catch (TimeoutException stillOpen) {
                if (attempt == 2) {
                    throw new AssertionError("Could not close the stock panel during Hub cleanup", stillOpen);
                }
            }
        }
    }

    private static boolean sameCompany(String first, String second) {
        return normalize(first).equalsIgnoreCase(normalize(second));
    }

    private static boolean samePanelCompany(String first, String second) {
        return first == null || second == null || sameCompany(first, second);
    }

    private static final class RuntimeStock {
        private final String name;
        private final Rectangle bounds;
        private RuntimeStock(String name, Rectangle bounds) {
            this.name = name;
            this.bounds = bounds;
        }
    }

    private static final class StockPanelSnapshot {
        private final String company;
        private final Rectangle content;
        private final Rectangle heading;
        private final Rectangle chart;
        private final Rectangle toolbar;
        private StockPanelSnapshot(String company, Rectangle content, Rectangle heading, Rectangle chart, Rectangle toolbar) {
            this.company = company;
            this.content = content;
            this.heading = heading;
            this.chart = chart;
            this.toolbar = toolbar;
        }
        private boolean sameGeometry(StockPanelSnapshot other) {
            return other != null && Objects.equals(company, other.company) && content.equals(other.content)
                    && Objects.equals(heading, other.heading) && Objects.equals(chart, other.chart)
                    && Objects.equals(toolbar, other.toolbar);
        }
    }

    private static final class StockDrag {
        private final Point start;
        private final Point end;
        private StockDrag(Point start, Point end) {
            this.start = start;
            this.end = end;
        }
    }

    public boolean isHubScreenVisible() {
        if (!visible(HUB) || !visible(SCROLL) || visible(ANY_BACK)) {
            return false;
        }
        if (visible(ENTRY) || visible(ACCOUNT_ENTRY)) {
            return true;
        }
        return visible(HUB_SECTIONS);
    }

    public void assertHubVisible() {
        waitUntil(this::isHubScreenVisible, ExecutionProfile.defaultWait(), "Expected the Hub screen");
    }

    public boolean canReturnToHub() {
        return isHubScreenVisible() || visible(ACCOUNT_TITLE) || visible(TITLE) || helpDrawerPresent()
                || stockDetailsPresent() || appTabsVisible();
    }

    /** Bounded cleanup. A TC_002 Back failure remains a failure even if recovery succeeds. */
    public void returnToHub() {
        nativeContext();
        if (isHubScreenVisible()) {
            return;
        }
        if (watchlistDrawerWasOpened) {
            closeWatchlistForCleanup();
        }
        if (stockDetailsPresent()) {
            closeStockDetailsForCleanup();
        }
        if (helpDrawerPresent()) {
            driver.navigate().back();
            waitUntil(() -> !helpDrawerPresent() && (moduleReady() || isHubScreenVisible()),
                    ExecutionProfile.shortWait(), "Know More did not close after Android Back");
        }
        if (isHubScreenVisible()) {
            return;
        }
        if (visible(ACCOUNT_TITLE)) {
            backFromAccountDetails();
            return;
        }
        if (visible(TITLE) && visible(BACK)) {
            tapBack(BACK);
            if (awaitHub(ExecutionProfile.shortWait())) {
                return;
            }
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            if (isHubScreenVisible()) {
                return;
            }
            if (emailLoginVisible() || pinScreenVisible() || visible(BIOMETRIC_CANCEL)) {
                throw new IllegalStateException("Hub recovery reached an authentication screen");
            }
            if (appTabsVisible()) {
                click(HUB);
                assertHubVisible();
                return;
            }
            driver.navigate().back();
            if (awaitHub(ExecutionProfile.shortWait())) {
                return;
            }
        }
        throw new AssertionError("Could not restore Hub within five recovery actions");
    }

    /** Restore shared driver settings before this case finishes, including on failure. */
    public void restoreTimingSettings() {
        if (!timingSettingsChanged) {
            return;
        }
        try {
            driver.setSetting("waitForSelectorTimeout", previousSelectorTimeout);
        } finally {
            try {
                if (previousIdleTimeout instanceof Number) driver.setSetting("waitForIdleTimeout", previousIdleTimeout);
            } finally {
                driver.manage().timeouts().implicitlyWait(previousImplicitWait);
            }
        }
        timingSettingsChanged = false;
    }

    private void beginImmediateSelectors() {
        if (timingSettingsChanged) {
            return;
        }
        Map<String, Object> settings = driver.getSettings();
        previousSelectorTimeout = settings.get("waitForSelectorTimeout");
        previousIdleTimeout = settings.get("waitForIdleTimeout");
        if (!(previousSelectorTimeout instanceof Number)) {
            throw new IllegalStateException("Cannot read the Appium waitForSelectorTimeout setting");
        }
        previousImplicitWait = driver.manage().timeouts().getImplicitWaitTimeout();
        timingSettingsChanged = true;
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.setSetting("waitForSelectorTimeout", 0);
        // Explicit readiness/geometry polls own synchronization during this module,
        // including shared login. Avoid a native idle delay on every remote lookup.
        if (previousIdleTimeout instanceof Number) driver.setSetting("waitForIdleTimeout", 0);
    }

    private boolean moduleReady() {
        return visible(TITLE) && visible(BACK) && visible(QUESTION) && visible(SCROLL)
                && !helpDrawerPresent() && !stockDetailsPresent();
    }

    private void waitForModuleReady() {
        waitUntil(this::moduleReady, ExecutionProfile.defaultWait(),
                "Analyst's Choice heading, Back, help control and content did not become ready");
    }

    private boolean helpDrawerVisible() {
        return visible(BUY_HELP) && visible(HOLD_HELP) && visible(SELL_HELP);
    }

    private boolean helpDrawerPresent() {
        // During an expansion or on a smaller viewport, one row may be clipped.
        // Recognize the modal before attempting the underlying page Back control.
        return visible(BUY_HELP) || visible(HOLD_HELP) || visible(SELL_HELP);
    }

    private void openKnowMore() {
        waitForModuleReady();
        click(QUESTION);
        waitUntil(this::helpDrawerVisible, ExecutionProfile.defaultWait(),
                "Know More did not expose the Buy, Hold and Sell accordion controls");
    }

    private void expandBuy() {
        waitUntil(this::helpDrawerVisible, ExecutionProfile.defaultWait(), "Know More drawer is not ready");
        if (visible(EXPENSIVE)) {
            click(HOLD_HELP);
            waitUntil(() -> !visible(EXPENSIVE), ExecutionProfile.shortWait(), "Hold did not collapse");
        }
        if (!visible(ALL_WEATHER)) {
            click(BUY_HELP);
        }
        waitUntil(() -> visible(ALL_WEATHER) && visible(AGGRESSIVE) && visible(LONG_TERM),
                ExecutionProfile.defaultWait(), "Buy definitions did not finish expanding");
    }

    private void collapseBuy() {
        if (visible(ALL_WEATHER)) {
            click(BUY_HELP);
        }
        waitUntil(() -> helpDrawerVisible() && !visible(ALL_WEATHER)
                        && !visible(AGGRESSIVE) && !visible(LONG_TERM),
                ExecutionProfile.shortWait(), "Buy did not close before opening Hold");
        ReportLogger.step("Buy dropdown is closed; opening Hold next");
    }

    private void waitForStableBuyContent() {
        final List<List<Rectangle>> previous = new ArrayList<>();
        previous.add(null);
        waitFor(ExecutionProfile.defaultWait()).withMessage(
                "Buy content must settle and fit above the Hold row before checking Dividend absence").until(d -> {
            WebElement buy = firstVisible(BUY_HELP);
            WebElement hold = firstVisible(HOLD_HELP);
            if (buy == null || hold == null || !fullyOnScreen(hold.getRect())) {
                return false;
            }
            List<Rectangle> current = new ArrayList<>();
            current.add(buy.getRect());
            for (By label : new By[]{ALL_WEATHER, AGGRESSIVE, LONG_TERM}) {
                WebElement element = firstVisible(label);
                if (element == null) {
                    return false;
                }
                Rectangle r = element.getRect();
                if (!fullyOnScreen(r) || r.getY() < buy.getRect().getY() + buy.getRect().getHeight()
                        || r.getY() + r.getHeight() > hold.getRect().getY()) {
                    return false;
                }
                current.add(r);
            }
            for (By paragraph : BUY_DESCRIPTION_MARKERS) {
                WebElement element = firstVisible(paragraph);
                if (element == null) {
                    return false;
                }
                Rectangle r = element.getRect();
                if (!fullyOnScreen(r) || r.getY() < buy.getRect().getY() + buy.getRect().getHeight()
                        || r.getY() + r.getHeight() > hold.getRect().getY()) {
                    return false;
                }
                current.add(r);
            }
            current.add(hold.getRect());
            boolean stable = current.equals(previous.get(0));
            previous.set(0, current);
            return stable;
        });
    }

    // Authentication: verify the real email, reuse matching sessions, cancel biometrics first.
    private void loginAs(Persona persona) {
        auth.loginFromLoginScreen(AuthHelper.Account.valueOf(persona.name()), selectedLoginMethod());
        waitUntil(this::appTabsVisible, ExecutionProfile.longWait(), "Logged-in bottom tabs did not appear");
        click(HUB);
        assertHubVisible();
        ReportLogger.pass("Login completed; explicitly switched from Funds to Hub");
    }

    private void finishSecurityAuthentication(Persona persona) {
        auth.finishSecurityAuthentication(persona == null ? null : AuthHelper.Account.valueOf(persona.name()));
    }

    private AuthHelper.LoginMethod selectedLoginMethod() {
        return loginMethod == null ? AuthHelper.configuredLoginMethod() : loginMethod;
    }

    private boolean pinScreenVisible() { return auth.isPinScreenVisible(); }

    private boolean appTabsVisible() {
        return visible(FUNDS) && visible(HUB) && !visible(BIOMETRIC_CANCEL) && !pinScreenVisible();
    }

    private boolean emailLoginVisible() { return auth.isEmailLoginVisible(); }

    private void openAccountDetails() {
        assertHubVisible();
        scrollToHubElement(ACCOUNT_ENTRY, "up");
        clickHubElement(ACCOUNT_ENTRY);
        waitUntil(() -> visible(ACCOUNT_TITLE) && visible(EMAIL_LABEL), ExecutionProfile.defaultWait(),
                "Account Details did not finish opening");
    }

    private String readAccountEmail() {
        return waitFor(ExecutionProfile.defaultWait()).withMessage("A unique account email was not exposed").until(d -> {
            Set<String> emails = new LinkedHashSet<>();
            for (WebElement element : driver.findElements(EMAIL_VALUE)) {
                String value = element.getText().trim();
                if (element.isDisplayed() && value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                    emails.add(value.toLowerCase(Locale.ROOT));
                }
            }
            if (emails.size() > 1) {
                throw new IllegalStateException("More than one account email was exposed; refusing to guess");
            }
            return emails.isEmpty() ? null : emails.iterator().next();
        });
    }

    private void logoutFromAccountDetails() {
        if (!visible(ACCOUNT_TITLE)) {
            throw new IllegalStateException("Logout requires Account Details");
        }
        click(LOGOUT);
        waitUntil(() -> visible(LOGOUT_CONFIRMATION) && visible(CONFIRM_LOGOUT), ExecutionProfile.defaultWait(),
                "Logout confirmation did not open");
        click(CONFIRM_LOGOUT);
        waitUntil(() -> !visible(LOGOUT_CONFIRMATION) && emailLoginVisible(), ExecutionProfile.longWait(),
                "Confirmed Logout did not reach email login");
        ReportLogger.pass("Logout confirmed; email login is visible");
    }

    private void backFromAccountDetails() {
        if (!visible(ACCOUNT_TITLE)) {
            throw new IllegalStateException("Expected Account Details before Back");
        }
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
        }
        tapBack(ANY_BACK);
        waitUntil(() -> !visible(ACCOUNT_TITLE) && isHubScreenVisible(), ExecutionProfile.defaultWait(),
                "Account Details Back did not return to Hub");
    }

    // Hub scrolling: bounded W3C gestures, live viewport, two settled position samples.
    private void scrollToHubElement(By target, String defaultDirection) {
        assertHubVisible();
        Rectangle viewport = hubViewport();
        if (hubElement(target, viewport) != null) {
            return;
        }
        Map<String, Rectangle> before = stableHubSnapshot(null, target, viewport);
        for (int attempt = 0; attempt < maxScrolls; attempt++) {
            String direction = hubDirection(target, viewport, defaultDirection);
            swipeHub(viewport, direction, false, target);
            Map<String, Rectangle> after = stableHubSnapshot(before, target, viewport);
            if (after.containsKey("target")) {
                return;
            }
            if (after.equals(before)) {
                swipeHub(viewport, direction, true, target);
                after = stableHubSnapshot(before, target, viewport);
                if (after.containsKey("target")) {
                    return;
                }
                if (after.equals(before)) {
                    throw new IllegalStateException("Hub did not move after two bounded " + direction + " swipes");
                }
            }
            before = after;
            defaultDirection = direction;
        }
        throw new IllegalStateException("Hub target was not fully visible within " + maxScrolls + " swipes: " + target);
    }

    private Rectangle hubViewport() {
        Rectangle bounds = require(SCROLL, ExecutionProfile.shortWait()).getRect();
        Dimension screen = driver.manage().window().getSize();
        Rectangle hub = require(HUB, ExecutionProfile.shortWait()).getRect();
        int left = Math.max(0, bounds.getX());
        int top = Math.max(0, bounds.getY());
        int right = Math.min(screen.getWidth(), bounds.getX() + bounds.getWidth());
        int bottom = Math.min(hub.getY(), Math.min(screen.getHeight(), bounds.getY() + bounds.getHeight()));
        if (right - left < 80 || bottom - top < 160) {
            throw new IllegalStateException("Hub has no usable scroll viewport");
        }
        return new Rectangle(new Point(left, top), new Dimension(right - left, bottom - top));
    }

    private WebElement hubElement(By locator, Rectangle viewport) {
        for (WebElement element : driver.findElements(locator)) {
            if (element.isDisplayed() && inside(element.getRect(), viewport, 8)) {
                return element;
            }
        }
        return null;
    }

    private String hubDirection(By target, Rectangle viewport, String fallback) {
        for (WebElement element : driver.findElements(target)) {
            Rectangle r = element.getRect();
            if (r.getWidth() <= 0 || r.getHeight() <= 0) {
                continue;
            }
            if (r.getHeight() > viewport.getHeight() - 16) {
                throw new IllegalStateException("Hub target cannot fit in the scroll viewport");
            }
            if (r.getY() < viewport.getY() + 8) {
                return "up";
            }
            if (r.getY() + r.getHeight() > viewport.getY() + viewport.getHeight() - 8) {
                return "down";
            }
        }
        return fallback;
    }

    private Map<String, Rectangle> stableHubSnapshot(Map<String, Rectangle> reference, By target,
                                                    Rectangle viewport) {
        final List<Map<String, Rectangle>> previous = new ArrayList<>();
        previous.add(null);
        return waitFor(ExecutionProfile.shortWait()).withMessage("Hub content did not settle after navigation/swipe").until(d -> {
            Map<String, Rectangle> current;
            WebElement found = hubElement(target, viewport);
            if (found != null) {
                current = Map.of("target", found.getRect());
            } else {
                current = hubAnchor(viewport, previous.get(0) == null ? reference : previous.get(0));
            }
            if (current == null) {
                previous.set(0, null);
                return null;
            }
            boolean settled = current.equals(previous.get(0));
            previous.set(0, current);
            return settled ? current : null;
        });
    }

    private Map<String, Rectangle> hubAnchor(Rectangle viewport, Map<String, Rectangle> previous) {
        if (previous != null && !previous.containsKey("target")) {
            String description = previous.keySet().iterator().next();
            WebElement anchor = firstVisible(AppiumBy.accessibilityId(description));
            if (anchor != null && inside(anchor.getRect(), viewport, 0)) {
                return Map.of(description, anchor.getRect());
            }
        }
        for (By locator : new By[]{AppiumBy.className("android.widget.Button"), HUB_SECTIONS}) {
            for (WebElement element : driver.findElements(locator)) {
                String description = element.getAttribute("content-desc");
                if (element.isDisplayed() && inside(element.getRect(), viewport, 0)
                        && description != null && !description.trim().isEmpty()) {
                    return Map.of(description, element.getRect());
                }
            }
        }
        return null;
    }

    private void swipeHub(Rectangle viewport, String direction, boolean retry, By target) {
        double fraction = retry ? 0.65 : 0.60;
        for (WebElement element : driver.findElements(target)) {
            Rectangle r = element.getRect();
            if (r.getY() < viewport.getY() + viewport.getHeight()
                    && r.getY() + r.getHeight() > viewport.getY()) {
                int clipped = "down".equals(direction)
                        ? r.getY() + r.getHeight() - (viewport.getY() + viewport.getHeight() - 8)
                        : viewport.getY() + 8 - r.getY();
                if (clipped > 0) {
                    fraction = Math.min(fraction, Math.max(0.025, (clipped + 8.0) / viewport.getHeight()));
                }
            }
        }
        int x = viewport.getX() + (int) (viewport.getWidth() * (retry ? 0.40 : 0.50));
        int startY = viewport.getY() + (int) (viewport.getHeight() * ("down".equals(direction) ? 0.84 : 0.16));
        int endY = startY + (int) (viewport.getHeight() * ("down".equals(direction) ? -fraction : fraction));
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "stock-analysts-choice-hub");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(10)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(swipeMillis), PointerInput.Origin.viewport(), x, endY));
        swipe.addAction(new Pause(finger, Duration.ofMillis(40)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        try {
            driver.perform(List.of(swipe));
        } catch (WebDriverException failure) {
            try {
                driver.resetInputState();
            } catch (WebDriverException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }

    private void clickHubElement(By locator) {
        Rectangle viewport = hubViewport();
        clickLocated(locator, () -> hubElement(locator, viewport), ExecutionProfile.shortWait());
    }

    private void click(By locator) {
        clickLocated(locator, () -> firstVisible(locator), ExecutionProfile.defaultWait());
    }

    private void tapBack(By locator) {
        tapNativeControl(locator);
    }

    private void tapNativeControl(By locator) {
        // The captured Flutter Go back may expose clickable=false. Use a real
        // gesture on fresh element bounds. The same approach is used for tabs/cells.
        for (int attempt = 1; attempt <= maxClickAttempts; attempt++) {
            try {
                WebElement back = waitFor(ExecutionProfile.shortWait()).until(d -> {
                    WebElement candidate = firstVisible(locator);
                    return candidate != null && candidate.isEnabled() && fullyOnScreen(candidate.getRect())
                            ? candidate : null;
                });
                driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) back).getId()));
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == maxClickAttempts) {
                    throw e;
                }
            }
        }
    }

    private void clickLocated(By locator, java.util.function.Supplier<WebElement> supplier, Duration timeout) {
        for (int attempt = 1; attempt <= maxClickAttempts; attempt++) {
            try {
                WebElement element = waitFor(timeout).withMessage("Control did not become visible/enabled: " + locator)
                        .until(d -> {
                            WebElement candidate = supplier.get();
                            return candidate != null && candidate.isEnabled() ? candidate : null;
                        });
                try {
                    element.click();
                } catch (ElementNotInteractableException e) {
                    element = waitFor(timeout).until(d -> {
                        WebElement candidate = supplier.get();
                        return candidate != null && candidate.isEnabled() ? candidate : null;
                    });
                    driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) element).getId()));
                }
                return;
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                if (attempt == maxClickAttempts) {
                    throw e;
                }
            }
        }
    }

    private WebElement firstVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            if (element.isDisplayed()) {
                return element;
            }
        }
        return null;
    }

    private boolean visible(By locator) {
        try {
            return firstVisible(locator) != null;
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean clickable(By locator) {
        WebElement element = firstVisible(locator);
        return element != null && element.isEnabled();
    }

    private WebElement require(By locator, Duration timeout) {
        return waitFor(timeout).withMessage("Expected visible control: " + locator).until(d -> firstVisible(locator));
    }

    private void assertSemantic(By locator, String expected) {
        WebElement element = require(locator, ExecutionProfile.defaultWait());
        String actual = element.getAttribute("content-desc");
        if (actual == null || actual.trim().isEmpty()) {
            actual = element.getText();
        }
        if (!normalize(expected).equals(normalize(actual))) {
            throw new AssertionError("Expected '" + expected + "'; actual '" + normalize(actual) + "'");
        }
    }

    private boolean fullyOnScreen(Rectangle rect) {
        return inside(rect, new Rectangle(new Point(0, 0), driver.manage().window().getSize()), 0);
    }

    private static boolean inside(Rectangle rect, Rectangle bounds, int padding) {
        return rect.getWidth() > 0 && rect.getHeight() > 0
                && rect.getX() >= bounds.getX() && rect.getX() + rect.getWidth() <= bounds.getX() + bounds.getWidth()
                && rect.getY() >= bounds.getY() + padding
                && rect.getY() + rect.getHeight() <= bounds.getY() + bounds.getHeight() - padding;
    }

    private boolean awaitHub(Duration timeout) {
        try {
            waitUntil(this::isHubScreenVisible, timeout, "Waiting for Hub");
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void nativeContext() {
        if (!"NATIVE_APP".equals(driver.getContext())) {
            driver.context("NATIVE_APP");
        }
    }

    private WebDriverWait waitFor(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout, ExecutionProfile.pollingInterval());
        wait.ignoring(NoSuchElementException.class);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout, String message) {
        waitFor(timeout).withMessage(message).until(d -> condition.getAsBoolean());
    }

    private static By helpHeader(String label) {
        return AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\")"
                + ".description(\"" + label + "\")");
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static String configured(String environment, String... keys) {
        String value = System.getenv(environment);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        for (String key : keys) {
            value = ConfigReader.getOptional(key, "");
            if (!value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static int positiveInt(String key, int fallback) {
        int value = Integer.parseInt(ConfigReader.getOptional(key, String.valueOf(fallback)).trim());
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }

}
