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
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Module 1 testing, TC_001–TC_005. Locators come from the supplied Inspector captures/XML. */
public class Batch1ChangesbyProPage {

    public enum Persona {
        ACTIVE_SUBSCRIBER, LAPSED_USER, NON_SUBSCRIBER;

        public String email() {
            return AuthHelper.configuredEmail(AuthHelper.Account.valueOf(name()));
        }
    }

    // This order is the order required by item 1 in Module 1 testing.
    public enum StockFeature {
        INVESTMENT_PLANNER("Investment Planner", "Open invesment planner", "Investment Planner"),
        SELL_PLANNER("Sell Planner", "Open sell planner", "Sell Planner"),
        STOCK_SCREENER("Stock Screener", "Open stock screener", "Stock Screener"),
        COVERAGE("Coverage", "Open coverage", "All Stories"),
        INSIGHTS("Insights", "Open insights", "All Stories"),
        REPORTS("Reports", "Open reports", "All Stories"),
        MARKET_MONITOR("Market Monitor", "Open Market Monitor screen", "Market Monitor"),
        THE_TEAM("The Team", "Open about team", "MEET THE TEAM"),
        RECOMMENDATIONS("Stock Analyst's Choice", "Open Stock Analyst's Choice screen", "Analyst's Choice");

        private final String label;
        private final String entryDescription;
        private final String heading;

        StockFeature(String label, String entryDescription, String heading) {
            this.label = label;
            this.entryDescription = entryDescription;
            this.heading = heading;
        }

        public String label() {
            return label;
        }

        private By entry() {
            if (this == INVESTMENT_PLANNER) {
                // The supplied QA hierarchy spells this "invesment". Accept that exact
                // description and the corrected spelling without changing the heading assertion.
                return AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.Button\")"
                        + ".descriptionMatches(\"^Open (invesment|investment) planner$\")");
            }
            return AppiumBy.accessibilityId(entryDescription);
        }
    }

    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By FUNDS = AppiumBy.accessibilityId("Funds");
    private static final By BIOMETRIC_CANCEL = By.id("com.android.systemui:id/button_negative");
    private static final By ACCOUNT_DETAILS = AppiumBy.accessibilityId("View and edit your account details");
    private static final By ACCOUNT_HEADING = AppiumBy.accessibilityId("Account Details");
    private static final By EMAIL_LABEL = AppiumBy.accessibilityId("Email Address");
    private static final By EMAIL_VALUE = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").textMatches(\".+@.+\")");
    // Both the account row and confirmation use "Logout"; scope each to its captured class.
    private static final By LOGOUT = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Logout\")");
    private static final By LOGOUT_CONFIRMATION_HEADING = AppiumBy.accessibilityId(
            "Are you sure you want to logout?");
    private static final By LOGOUT_CONFIRM = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").description(\"Logout\")");
    private static final By BACK = AppiumBy.accessibilityId("Go back");
    private static final By HUB_SCROLL = AppiumBy.className("android.widget.ScrollView");
    private static final By HUB_SECTION_HEADINGS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionMatches(\"^(Profile|Investor Accounts|Mutual Funds|Stocks|More)$\")");
    private static final By HUB_STOCK_BUTTONS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.Button\").descriptionMatches(\"^(Open "
                    + "(invesment|investment) planner|Open sell planner|Open stock screener|Open coverage|"
                    + "Open insights|Open reports|Open Market Monitor screen|Open about team|"
                    + "Open Stock Analyst's Choice screen)$\")");
    private static final By HUB_MORE_BUTTONS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.Button\").descriptionMatches(\"^(View frequently "
                    + "asked questions|Learn about Value Research|Get in touch with us|Read our privacy policy|"
                    + "View and download your user agreement|View our refund policy|View the investor charter|"
                    + "Submit or track complaint|Open the ODR portal for dispute resolution|View our audit status|"
                    + "Open watchlist)$\")");
    private static final By WATCHLIST_ENTRY = AppiumBy.accessibilityId("Open watchlist");
    private static final By KNOWN_FEATURE_HEADINGS = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"Investment Planner|Sell Planner|Stock Screener|"
                    + "All Stories|Market Monitor|MEET THE TEAM|Analyst's Choice\")");
    private static final By WEBVIEW = AppiumBy.className("android.webkit.WebView");
    private final AndroidDriver driver;
    private final AuthHelper auth;
    private final AuthHelper.LoginMethod loginMethod;
    private final int maxScrolls = positiveInt("batch1MaxScrolls", 12);
    private final int maxClickAttempts = positiveInt("batch1ClickAttempts", 3);
    private final int swipeDurationMillis = positiveInt("batch1SwipeDurationMillis", 200);
    private final Set<StockFeature> observedStockEntries = new LinkedHashSet<>();
    private final int maxRecoveryBacks = positiveInt("batch1MaxRecoveryBacks", 5);

    public Batch1ChangesbyProPage(AndroidDriver driver) {
        this(driver, null); // Existing callers retain global login-method selection.
    }

    public Batch1ChangesbyProPage(AndroidDriver driver, AuthHelper.LoginMethod loginMethod) {
        if (driver == null) {
            throw new IllegalArgumentException("An active AndroidDriver is required");
        }
        this.driver = driver;
        this.auth = new AuthHelper(driver);
        this.loginMethod = loginMethod;
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    }

    /** Optional explicit fresh-login API. This suite reuses matching sessions via ensureRequiredAccount. */
    public void loginFreshAs(Persona persona) {
        AuthHelper.validateLoginConfiguration(AuthHelper.Account.valueOf(persona.name()), selectedLoginMethod());
        prepareInitialAuthenticationState();
        if (!isEmailLoginScreen()) {
            ReportLogger.step("Logging out the existing session before the fresh " + persona + " login");
            logoutCurrentAccount();
        }
        loginAs(persona);
        verifyRequiredAccount(persona);
    }

    /** Reuse only the verified target account; already logged-out startup goes directly to login. */
    public void ensureRequiredAccount(Persona persona) {
        String requiredEmail = persona.email(); // Validate the target before changing a usable session.
        prepareInitialAuthenticationState();
        if (!isEmailLoginScreen()) {
            recoverToHub();
            openAccountDetails();
            String currentEmail = readCurrentAccountEmail();
            if (currentEmail.equalsIgnoreCase(requiredEmail)) {
                ReportLogger.pass("Verified the existing " + persona + " session; skipping Logout and login");
                backToHub();
                return;
            }
            // Validate only the selected method before logging out a usable session.
            AuthHelper.validateLoginConfiguration(AuthHelper.Account.valueOf(persona.name()), selectedLoginMethod());
            ReportLogger.step("Switching account to " + persona + " through explicit Logout");
            logoutFromAccountDetails();
        }
        loginAs(persona);
        verifyRequiredAccount(persona);
    }

    private void prepareInitialAuthenticationState() {
        nativeContext();
        waitUntil(() -> visible(BIOMETRIC_CANCEL) || pinScreenVisible() || isEmailLoginScreen()
                        || appTabsVisible() || visible(BACK),
                ExecutionProfile.longWait(), "No known Advisor login/navigation screen appeared");
        if (visible(BIOMETRIC_CANCEL) || pinScreenVisible()) {
            // Unlock a protected session to verify its email before deciding whether to reuse it.
            auth.finishSecurityAuthentication(null); // The existing account has not yet been verified.
        }
    }

    private void verifyRequiredAccount(Persona persona) {
        assertHubVisible();
        openAccountDetails();
        String actualEmail = readCurrentAccountEmail();
        if (!actualEmail.equalsIgnoreCase(persona.email())) {
            throw new AssertionError("The logged-in account does not match the configured " + persona);
        }
        ReportLogger.pass("Account email verified for " + persona);
        backToHub();
    }

    public void logoutCurrentAccount() {
        recoverToHub();
        openAccountDetails();
        readCurrentAccountEmail();
        logoutFromAccountDetails();
    }

    private void loginAs(Persona persona) {
        // Shared SAC flow owns password/OTP submission, biometric cancellation, then PIN entry.
        auth.loginFromLoginScreen(AuthHelper.Account.valueOf(persona.name()), selectedLoginMethod());
        switchToHubAfterLogin();
    }

    private AuthHelper.LoginMethod selectedLoginMethod() {
        return loginMethod == null ? AuthHelper.configuredLoginMethod() : loginMethod;
    }

    private boolean appTabsVisible() {
        return visible(FUNDS) && visible(HUB) && !visible(BIOMETRIC_CANCEL) && !pinScreenVisible();
    }

    private void switchToHubAfterLogin() {
        waitUntil(this::appTabsVisible, ExecutionProfile.longWait(), "Logged-in bottom tabs did not appear");
        click(HUB);
        assertHubVisible();
        ReportLogger.pass("Explicitly switched to Hub after login");
    }

    private void openAccountDetails() {
        assertHubVisible();
        scrollToHubElement(ACCOUNT_DETAILS, "up");
        clickFullyVisibleHubElement(ACCOUNT_DETAILS);
        waitUntil(() -> visible(ACCOUNT_HEADING) && visible(EMAIL_LABEL),
                ExecutionProfile.defaultWait(), "Account Details did not open");
    }

    public String readCurrentAccountEmail() {
        waitUntil(() -> visible(ACCOUNT_HEADING) && visible(EMAIL_LABEL),
                ExecutionProfile.defaultWait(), "Account email is not on the current screen");
        return waitFor(ExecutionProfile.defaultWait()).until(d -> {
            List<String> emails = new ArrayList<>();
            for (WebElement element : driver.findElements(EMAIL_VALUE)) {
                if (element.isDisplayed()) {
                    String text = element.getText().trim();
                    if (text.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                        emails.add(text);
                    }
                }
            }
            if (emails.size() > 1) {
                throw new IllegalStateException("More than one account email is visible; refusing to guess");
            }
            return emails.isEmpty() ? null : emails.get(0);
        });
    }

    private void logoutFromAccountDetails() {
        if (!visible(ACCOUNT_HEADING)) {
            throw new IllegalStateException("Logout requires the Account Details screen");
        }
        click(LOGOUT);
        waitUntil(() -> visible(LOGOUT_CONFIRMATION_HEADING) && visible(LOGOUT_CONFIRM),
                ExecutionProfile.defaultWait(), "Logout confirmation popup did not appear");
        ReportLogger.step("Confirming Logout on the account confirmation popup");
        click(LOGOUT_CONFIRM);
        waitUntil(() -> !visible(LOGOUT_CONFIRMATION_HEADING) && isEmailLoginScreen(),
                ExecutionProfile.longWait(), "Confirmed Logout did not close the popup and reach email login");
        ReportLogger.pass("Logout completed and email login is visible");
    }

    public boolean isHubScreenVisible() {
        if (!visible(HUB) || !visible(HUB_SCROLL)) {
            return false;
        }
        for (WebElement heading : driver.findElements(HUB_SECTION_HEADINGS)) {
            if (heading.isDisplayed()) {
                return true;
            }
        }
        // The section title can be clipped while the Stocks buttons are still on Hub.
        for (WebElement entry : driver.findElements(HUB_STOCK_BUTTONS)) {
            if (entry.isDisplayed()) {
                return true;
            }
        }
        for (WebElement entry : driver.findElements(HUB_MORE_BUTTONS)) {
            if (entry.isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    public void assertHubVisible() {
        waitUntil(this::isHubScreenVisible, ExecutionProfile.defaultWait(), "Expected the Hub screen");
    }

    /** The revised TC_003/TC_004 scope is verified account access to the inner app on Hub. */
    public void assertInnerAppAccessible() {
        nativeContext();
        String appPackage = ConfigReader.get("appPackage").trim();
        waitUntil(() -> appPackage.equals(driver.getCurrentPackage())
                        && appTabsVisible() && !visible(BACK) && isHubScreenVisible(),
                ExecutionProfile.defaultWait(), "The verified account did not reach the inner Advisor app on Hub");
    }

    /** Navigate directly to Watchlist under More; do not repeat a complete Stocks scroll pass. */
    public void openWatchlistAndVerifyInAppWebView() {
        scrollToHubElement(WATCHLIST_ENTRY, "down");
        ReportLogger.step("TC_002: Opening Watchlist under More");
        clickFullyVisibleHubElement(WATCHLIST_ENTRY);
        String appPackage = ConfigReader.get("appPackage").trim();
        waitUntil(() -> {
            String currentPackage = driver.getCurrentPackage();
            if (currentPackage != null && !currentPackage.isEmpty() && !appPackage.equals(currentPackage)) {
                throw new AssertionError("Watchlist opened outside Advisor. Expected package: " + appPackage
                        + "; actual package: " + currentPackage);
            }
            // The supplied XML exposes a WebView container, not its list contents.
            return appPackage.equals(currentPackage) && visible(WEBVIEW)
                    && visible(BACK) && !isHubScreenVisible();
        }, ExecutionProfile.longWait(), "Watchlist did not open an in-app WebView with Go back");
        ReportLogger.pass("Watchlist opened in a WebView inside Advisor");
    }

    /** Back must complete; tapping Hub is reserved for recovery after a recorded failure. */
    public void returnToHubAfterWatchlist() {
        backToHub();
        String appPackage = ConfigReader.get("appPackage").trim();
        waitUntil(() -> appPackage.equals(driver.getCurrentPackage())
                        && !visible(WEBVIEW) && !visible(BACK) && isHubScreenVisible(),
                ExecutionProfile.shortWait(), "Watchlist Back did not close the WebView and return to Advisor Hub");
    }

    /** TC_005 checks the supplied native WebView container; no WEBVIEW attachment is needed. */
    public void openTeamAndVerifyInAppWebView() {
        scrollToHubElement(StockFeature.THE_TEAM.entry(), "down");
        ReportLogger.step("TC_005: Opening About the Team under Stocks");
        clickFullyVisibleHubElement(StockFeature.THE_TEAM.entry());
        String appPackage = ConfigReader.get("appPackage").trim();
        waitUntil(() -> {
            String currentPackage = driver.getCurrentPackage();
            if (currentPackage != null && !currentPackage.isEmpty() && !appPackage.equals(currentPackage)) {
                throw new AssertionError("About the Team opened outside Advisor. Expected package: " + appPackage
                        + "; actual package: " + currentPackage);
            }
            return appPackage.equals(currentPackage) && visible(WEBVIEW)
                    && visible(BACK) && !isHubScreenVisible();
        }, ExecutionProfile.defaultWait(), "About the Team did not open an in-app WebView with Go back");
    }

    public void returnToHubAfterTeam() {
        backToHub();
        String appPackage = ConfigReader.get("appPackage").trim();
        waitUntil(() -> appPackage.equals(driver.getCurrentPackage())
                        && !visible(WEBVIEW) && !visible(BACK) && isHubScreenVisible(),
                ExecutionProfile.shortWait(), "Team Back did not close the WebView and return to Advisor Hub");
    }

    /** Recovery may tap Hub; normal feature/Watchlist returns MUST succeed through Back. */
    public void recoverToHub() {
        nativeContext();
        if (isHubScreenVisible()) {
            return;
        }
        if (visible(HUB)) {
            click(HUB);
            waitUntil(this::isHubScreenVisible, ExecutionProfile.defaultWait(), "Hub tab did not open");
            return;
        }
        for (int attempt = 0; attempt < maxRecoveryBacks; attempt++) {
            if (isEmailLoginScreen()) {
                throw new IllegalStateException("Recovery reached a logged-out screen");
            }
            performBack();
            if (awaitHub(ExecutionProfile.shortWait())) {
                return;
            }
            if (visible(HUB)) {
                click(HUB);
                waitUntil(this::isHubScreenVisible, ExecutionProfile.defaultWait(), "Hub recovery failed");
                return;
            }
        }
        throw new AssertionError("Could not restore Hub within the configured recovery limit");
    }

    public void resetToHubTop() {
        recoverToHub();
        scrollToHubElement(ACCOUNT_DETAILS, "up");
        assertHubVisible();
    }

    /** Stop at the first fully visible Stocks entry; never scan all of Stocks during setup. */
    public void scrollToStocksSection() {
        observedStockEntries.clear();
        scrollToHubElement(StockFeature.INVESTMENT_PLANNER.entry(), "down");
        observeVisibleStockEntries();
        ReportLogger.step("Stocks ready");
    }

    /** Check the order collected during navigation, without performing a separate scroll pass. */
    public void verifyStocksEntryOrder() {
        if (observedStockEntries.size() != StockFeature.values().length) {
            throw new AssertionError("Could not observe all nine Stocks entries during navigation. Found: "
                    + observedStockEntries);
        }
        if (!new ArrayList<>(observedStockEntries).equals(Arrays.asList(StockFeature.values()))) {
            throw new AssertionError("Stocks entry order differs from Module 1 testing: " + observedStockEntries);
        }
        ReportLogger.pass("All nine Stocks entries were observed in the required order");
    }

    private void observeVisibleStockEntries() {
        if (observedStockEntries.size() == StockFeature.values().length) {
            return;
        }
        List<StockFeature> visibleFeatures = new ArrayList<>();
        Map<StockFeature, Rectangle> positions = new java.util.EnumMap<>(StockFeature.class);
        Rectangle viewport = currentHubViewport();
        for (WebElement entry : driver.findElements(HUB_STOCK_BUTTONS)) {
            Rectangle rect = entry.getRect();
            if (centreInside(rect, viewport) && entry.isDisplayed()) {
                StockFeature feature = stockFeatureForDescription(entry.getAttribute("content-desc"));
                if (feature == null) {
                    continue;
                }
                visibleFeatures.add(feature);
                positions.put(feature, rect);
            }
        }
        visibleFeatures.sort(Comparator.comparingInt((StockFeature f) -> positions.get(f).getY())
                .thenComparingInt(f -> positions.get(f).getX()));
        observedStockEntries.addAll(visibleFeatures);
    }

    /** Leave the destination open so the test can log/screenshot a mismatch before navigating Back. */
    public void openFeatureAndVerifyHeading(StockFeature feature) {
        // Back normally preserves Stocks. Scroll only when this specific entry needs it.
        scrollToHubElement(feature.entry(), "down");
        // Order was already captured for this entry; avoid rereading every visible icon after Back.
        if (!observedStockEntries.contains(feature)) {
            observeVisibleStockEntries();
        }
        ReportLogger.step("TC_001: Opening " + feature.label);
        clickFullyVisibleHubElement(feature.entry());
        try {
            if (feature == StockFeature.THE_TEAM) {
                verifyTeamHeading();
            } else {
                waitUntil(() -> visible(AppiumBy.accessibilityId(feature.heading)),
                        ExecutionProfile.defaultWait(), "Expected heading did not appear");
            }
        } catch (AssertionError | RuntimeException failure) {
            throw new AssertionError(feature.label + " heading verification failed. Expected: '" + feature.heading
                    + "'; observed native headings: " + observedNativeFeatureHeadings()
                    + ". Details: " + ReportLogger.summary(failure), failure);
        }
    }

    /** Used in the test's finally block after both heading success and soft failure. */
    public void returnToHubAfterFeature(StockFeature feature) {
        // backToHub is a no-op if an entry failed before leaving Hub.
        backToHub();
    }

    /** Retained for callers that use the earlier combined page method. */
    public void verifyFeatureAndReturn(StockFeature feature) {
        try {
            openFeatureAndVerifyHeading(feature);
        } finally {
            returnToHubAfterFeature(feature);
        }
    }

    private String observedNativeFeatureHeadings() {
        try {
            nativeContext();
            Set<String> headings = new LinkedHashSet<>();
            for (WebElement element : driver.findElements(KNOWN_FEATURE_HEADINGS)) {
                if (element.isDisplayed()) {
                    String description = element.getAttribute("content-desc");
                    if (description != null && !description.trim().isEmpty()) {
                        headings.add(description.trim());
                    }
                }
            }
            return headings.isEmpty() ? "<no supplied heading locator was visible>" : headings.toString();
        } catch (WebDriverException e) {
            return "<unavailable: " + e.getClass().getSimpleName() + ">";
        }
    }

    private void verifyTeamHeading() {
        waitUntil(() -> visible(WEBVIEW), ExecutionProfile.longWait(), "The Team WebView did not open");
        By nativeHeading = AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i)MEET THE TEAM\")");
        String appPackage = ConfigReader.get("appPackage");
        String lastContextError = "No matching WEBVIEW context was exposed";
        long deadline = System.nanoTime() + ExecutionProfile.longWait().toNanos();
        try {
            // Context discovery has its own bounded polling; no screenshots/OCR or invented CSS classes.
            String context = waitFor(ExecutionProfile.longWait()).until(d -> {
                if (visible(nativeHeading)) {
                    return "NATIVE_APP";
                }
                for (String handle : driver.getContextHandles()) {
                    if (handle.startsWith("WEBVIEW") && handle.contains(appPackage)) {
                        return handle;
                    }
                }
                return null;
            });
            if ("NATIVE_APP".equals(context)) {
                return;
            }
            driver.context(context);
            Duration remaining = Duration.ofNanos(Math.max(1L, deadline - System.nanoTime()));
            waitUntil(() -> Boolean.TRUE.equals(driver.executeScript(
                    "return Array.from(document.querySelectorAll('h1,h2,h3,h4,h5,h6,[role=heading]'))"
                    + ".some(e => e.getClientRects().length > 0 && "
                    + "e.textContent.replace(/\\s+/g,' ').trim().toUpperCase() === arguments[0]);",
                    StockFeature.THE_TEAM.heading)), remaining, "MEET THE TEAM was not visible in the WebView DOM");
        } catch (TimeoutException e) {
            String observed = "<no visible native heading or usable WEBVIEW context>";
            if (!"NATIVE_APP".equals(driver.getContext())) {
                try {
                    observed = String.valueOf(driver.executeScript(
                            "return Array.from(document.querySelectorAll('h1,h2,h3,h4,h5,h6,[role=heading]'))"
                                    + ".filter(e => e.getClientRects().length > 0)"
                                    + ".map(e => e.textContent.replace(/\\s+/g,' ').trim());"));
                } catch (WebDriverException unavailable) {
                    observed = "<web headings unavailable: " + unavailable.getClass().getSimpleName() + ">";
                }
            }
            throw new AssertionError("Expected The Team heading: MEET THE TEAM; observed web headings: " + observed, e);
        } catch (WebDriverException e) {
            lastContextError = e.getClass().getSimpleName();
            throw new AssertionError("Cannot verify MEET THE TEAM. The QA app must expose a debuggable WEBVIEW "
                    + "and Appium needs a compatible Chromedriver. Failure: " + lastContextError, e);
        } finally {
            nativeContext();
        }
    }

    private void backToHub() {
        nativeContext();
        if (!visible(BACK) && isHubScreenVisible()) {
            return;
        }
        // A planner input can leave the IME open; dismiss it before the navigation action.
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
            waitUntil(() -> !driver.isKeyboardShown(), ExecutionProfile.shortWait(),
                    "Keyboard did not close before Back");
        }
        String source = currentBackSource();
        boolean usedControl = performBack();
        if (awaitHub(ExecutionProfile.shortWait())) {
            return;
        }
        // Recheck before every additional action: never send Back after reaching Hub.
        if (!visible(BACK) && isHubScreenVisible()) {
            return;
        }
        if (usedControl && !source.isEmpty() && source.equals(currentBackSource())) {
            ReportLogger.debug("Go back did not leave " + source + "; trying Android Back once");
            driver.navigate().back();
            waitUntil(() -> !visible(BACK) && isHubScreenVisible(), ExecutionProfile.shortWait(),
                    "Back did not return to Hub");
            return;
        }
        throw new IllegalStateException("Back did not return to Hub; current screen: " + currentBackSource());
    }

    private String currentBackSource() {
        String headings = observedNativeFeatureHeadings();
        if (!headings.startsWith("<")) {
            return headings;
        }
        if (visible(ACCOUNT_HEADING)) {
            return "Account Details";
        }
        return visible(WEBVIEW) ? "In-app WebView" : "";
    }

    /** Returns whether the supplied UI Back control was used, rather than Android Back. */
    private boolean performBack() {
        if (!visible(BACK)) {
            driver.navigate().back();
            return false;
        }
        for (int attempt = 1; attempt <= maxClickAttempts; attempt++) {
            try {
                WebElement back = waitFor(ExecutionProfile.shortWait()).until(d -> {
                    for (WebElement candidate : driver.findElements(BACK)) {
                        Rectangle rect = candidate.getRect();
                        // Flutter's semantic button may expose clickable=false. Visible, enabled
                        // bounds plus a real native tap are more useful than that metadata flag.
                        if (candidate.isDisplayed() && candidate.isEnabled()
                                && rect.getWidth() > 0 && rect.getHeight() > 0) {
                            return candidate;
                        }
                    }
                    return null;
                });
                driver.executeScript("mobile: clickGesture", Map.of(
                        "elementId", ((RemoteWebElement) back).getId()));
                return true;
            } catch (StaleElementReferenceException e) {
                if (attempt == maxClickAttempts) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Could not tap the Go back control");
    }

    private boolean awaitHub(Duration timeout) {
        try {
            waitUntil(() -> !visible(BACK) && isHubScreenVisible(), timeout, "Waiting for Hub recovery");
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void scrollToHubElement(By locator, String direction) {
        nativeContext();
        withImmediateHubSelectors(() -> scrollToHubElementWithImmediateSelectors(locator, direction));
    }

    /** Explicit polling owns Hub timing; off-screen UiSelector misses must not add an inner wait. */
    private void withImmediateHubSelectors(Runnable action) {
        String setting = "waitForSelectorTimeout";
        Object previous = driver.getSettings().get(setting);
        if (!(previous instanceof Number)) {
            throw new IllegalStateException("Could not read the current Appium " + setting + " setting");
        }
        if (((Number) previous).longValue() == 0L) {
            action.run();
            return;
        }
        driver.setSetting(setting, 0);
        Throwable originalFailure = null;
        try {
            action.run();
        } catch (RuntimeException | Error failure) {
            originalFailure = failure;
            throw failure;
        } finally {
            try {
                driver.setSetting(setting, previous);
            } catch (WebDriverException restoreFailure) {
                if (originalFailure == null) {
                    throw restoreFailure;
                }
                originalFailure.addSuppressed(restoreFailure);
            }
        }
    }

    private void scrollToHubElementWithImmediateSelectors(By locator, String direction) {
        assertHubVisible();
        Rectangle viewport = currentHubViewport();
        if (hubViewportElement(locator, viewport) != null) {
            return;
        }
        // Readiness is checked once; each settled snapshot becomes the next swipe's reference.
        Map<String, Rectangle> before = waitForHubPositionSnapshot(null, locator, viewport,
                ExecutionProfile.defaultWait(), "Hub content was not ready for scrolling");
        if (before.containsKey("target")) {
            return;
        }
        String fallbackDirection = direction;
        for (int attempt = 0; attempt < maxScrolls; attempt++) {
            String nextDirection = hubScrollDirection(locator, fallbackDirection, viewport);
            Map<String, Rectangle> after = scrollHub(nextDirection, locator, viewport, before);
            if (after.containsKey("target")) {
                return;
            }
            if (after.equals(before)) {
                throw new IllegalStateException("Hub did not move after two " + nextDirection
                        + " swipes. Target: " + locator + "; anchor: " + formatHubSnapshot(after));
            }
            before = after;
            fallbackDirection = nextDirection;
        }
        throw new IllegalStateException("Hub item was not fully visible after " + maxScrolls
                + " bounded swipes: " + locator);
    }

    private Rectangle currentHubViewport() {
        WebElement container = waitFor(ExecutionProfile.shortWait()).until(d -> {
            List<WebElement> matches = driver.findElements(HUB_SCROLL);
            return matches.size() == 1 && matches.get(0).isDisplayed() && matches.get(0).isEnabled()
                    ? matches.get(0) : null;
        });
        Rectangle bounds = container.getRect();
        Dimension screen = driver.manage().window().getSize();
        int left = Math.max(0, bounds.getX());
        int top = Math.max(0, bounds.getY());
        int right = Math.min(screen.getWidth(), bounds.getX() + bounds.getWidth());
        int bottom = Math.min(screen.getHeight(), bounds.getY() + bounds.getHeight());
        for (WebElement tab : driver.findElements(HUB)) {
            Rectangle tabBounds = tab.getRect();
            if (tab.isDisplayed() && tabBounds.getY() > top) {
                bottom = Math.min(bottom, tabBounds.getY());
            }
        }
        if (right - left < 80 || bottom - top < 160) {
            throw new IllegalStateException("Hub has no usable on-screen swipe area");
        }
        return new Rectangle(new Point(left, top), new Dimension(right - left, bottom - top));
    }

    private String hubScrollDirection(By locator, String fallbackDirection, Rectangle viewport) {
        for (WebElement target : driver.findElements(locator)) {
            Rectangle rect = target.getRect();
            if (rect.getHeight() <= 0 || rect.getWidth() <= 0) {
                continue;
            }
            int padding = hubEdgePadding(viewport);
            if (rect.getHeight() > viewport.getHeight() - 2 * padding || rect.getWidth() > viewport.getWidth()) {
                throw new IllegalStateException("Hub target cannot fit fully inside the usable viewport: " + locator);
            }
            if (rect.getX() < viewport.getX()
                    || rect.getX() + rect.getWidth() > viewport.getX() + viewport.getWidth()) {
                throw new IllegalStateException("Hub target is clipped horizontally; vertical swipes cannot expose it: " + locator);
            }
            if (rect.getY() < viewport.getY() + padding) {
                return "up";
            }
            if (rect.getY() + rect.getHeight() > viewport.getY() + viewport.getHeight() - padding) {
                return "down";
            }
        }
        for (StockFeature target : StockFeature.values()) {
            if (!locator.equals(target.entry())) {
                continue;
            }
            // One native lookup replaces nine independent locator/ScrollView checks.
            for (WebElement entry : driver.findElements(HUB_STOCK_BUTTONS)) {
                if (centreInside(entry.getRect(), viewport) && entry.isDisplayed()) {
                    StockFeature candidate = stockFeatureForDescription(entry.getAttribute("content-desc"));
                    if (candidate != null && candidate.ordinal() > target.ordinal()) {
                        return "up";
                    }
                }
            }
            for (WebElement entry : driver.findElements(HUB_MORE_BUTTONS)) {
                if (centreInside(entry.getRect(), viewport) && entry.isDisplayed()) {
                    return "up";
                }
            }
            break;
        }
        return fallbackDirection;
    }

    private StockFeature stockFeatureForDescription(String description) {
        for (StockFeature feature : StockFeature.values()) {
            if (feature.entryDescription.equals(description)
                    || (feature == StockFeature.INVESTMENT_PLANNER
                    && "Open investment planner".equals(description))) {
                return feature;
            }
        }
        return null;
    }

    private Map<String, Rectangle> scrollHub(String direction, By target, Rectangle viewport,
                                              Map<String, Rectangle> before) {
        performHubTouchSwipe(viewport, direction, hubSwipeFraction(target, viewport, direction, 0.60), false);
        Map<String, Rectangle> after = waitForHubPositionSnapshot(before, target, viewport,
                ExecutionProfile.shortWait(), "Hub did not settle after the swipe");
        if (!after.equals(before) || after.containsKey("target")) {
            return after;
        }
        ReportLogger.debug("Hub swipe had no observed movement; retrying once in another lane");
        performHubTouchSwipe(viewport, direction, hubSwipeFraction(target, viewport, direction, 0.65), true);
        return waitForHubPositionSnapshot(before, target, viewport,
                ExecutionProfile.shortWait(), "Hub did not settle after the retry swipe");
    }

    private double hubSwipeFraction(By target, Rectangle viewport, String direction, double usualFraction) {
        int padding = hubEdgePadding(viewport);
        for (WebElement element : driver.findElements(target)) {
            Rectangle r = element.getRect();
            if (r.getHeight() <= 0 || r.getWidth() <= 0
                    || r.getY() >= viewport.getY() + viewport.getHeight()
                    || r.getY() + r.getHeight() <= viewport.getY()) {
                continue;
            }
            int clipped = "down".equals(direction)
                    ? r.getY() + r.getHeight() - (viewport.getY() + viewport.getHeight() - padding)
                    : viewport.getY() + padding - r.getY();
            if (clipped > 0) {
                // Reveal just the clipped edge, with a small buffer; avoid another full section swipe.
                return Math.min(usualFraction, Math.max(0.025,
                        (clipped + padding) / (double) viewport.getHeight()));
            }
        }
        return usualFraction;
    }

    private Map<String, Rectangle> waitForHubPositionSnapshot(Map<String, Rectangle> reference, By target,
                                                             Rectangle viewport, Duration timeout,
                                                             String message) {
        List<Map<String, Rectangle>> last = new ArrayList<>();
        last.add(null);
        return waitFor(timeout).withMessage(message).until(d -> {
            WebElement entry = target == null ? null : hubViewportElement(target, viewport);
            Map<String, Rectangle> current = entry == null
                    ? visibleHubAnchorPosition(viewport, last.get(0) == null ? reference : last.get(0))
                    : Map.of("target", entry.getRect());
            if (current == null || current.isEmpty()) {
                last.set(0, null);
                return null;
            }
            boolean stable = current.equals(last.get(0));
            last.set(0, current);
            return stable ? current : null; // Two stable samples, rather than three per swipe.
        });
    }

    private void performHubTouchSwipe(Rectangle viewport, String direction, double fraction, boolean retry) {
        if (!"down".equals(direction) && !"up".equals(direction)) {
            throw new IllegalArgumentException("Unsupported Hub scroll direction: " + direction);
        }
        int x = viewport.getX() + (int) (viewport.getWidth() * (retry ? 0.40 : 0.50));
        int startY = viewport.getY() + (int) (viewport.getHeight() * ("down".equals(direction) ? 0.84 : 0.16));
        int endY = startY + (int) (viewport.getHeight() * ("down".equals(direction) ? -fraction : fraction));
        ReportLogger.debug("Hub swipe " + direction + ": (" + x + "," + startY + ") -> ("
                + x + "," + endY + "), move=" + swipeDurationMillis + " ms");
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "batch1-hub-finger");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(10)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(swipeDurationMillis),
                PointerInput.Origin.viewport(), x, endY));
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

    private Map<String, Rectangle> visibleHubAnchorPosition(Rectangle viewport, Map<String, Rectangle> reference) {
        try {
            if (reference != null && !reference.isEmpty() && !reference.containsKey("target")) {
                String description = reference.keySet().iterator().next();
                for (WebElement anchor : driver.findElements(AppiumBy.accessibilityId(description))) {
                    Rectangle rect = anchor.getRect();
                    if (centreInside(rect, viewport) && anchor.isDisplayed()) {
                        return Map.of(description, rect);
                    }
                }
            }
            for (WebElement button : driver.findElements(AppiumBy.className("android.widget.Button"))) {
                Rectangle rect = button.getRect();
                if (centreInside(rect, viewport) && button.isDisplayed()) {
                    String description = button.getAttribute("content-desc");
                    if (description != null && !description.isEmpty()) {
                        return Map.of(description, rect);
                    }
                }
            }
            for (WebElement heading : driver.findElements(HUB_SECTION_HEADINGS)) {
                Rectangle rect = heading.getRect();
                if (centreInside(rect, viewport) && heading.isDisplayed()) {
                    String description = heading.getAttribute("content-desc");
                    if (description != null && !description.isEmpty()) {
                        return Map.of(description, rect);
                    }
                }
            }
            return null;
        } catch (StaleElementReferenceException e) {
            return null;
        }
    }

    private String formatHubSnapshot(Map<String, Rectangle> snapshot) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Rectangle> entry : snapshot.entrySet()) {
            Rectangle r = entry.getValue();
            result.append(entry.getKey()).append(" [").append(r.getX()).append(',').append(r.getY())
                    .append(',').append(r.getWidth()).append(',').append(r.getHeight()).append("] ");
        }
        return result.toString().trim();
    }

    private boolean centreInside(Rectangle rect, Rectangle viewport) {
        int centreX = rect.getX() + rect.getWidth() / 2;
        int centreY = rect.getY() + rect.getHeight() / 2;
        return rect.getHeight() > 0 && rect.getWidth() > 0
                && centreX > viewport.getX() && centreX < viewport.getX() + viewport.getWidth()
                && centreY > viewport.getY() && centreY < viewport.getY() + viewport.getHeight();
    }

    private int hubEdgePadding(Rectangle viewport) {
        return Math.max(8, Math.min(24, viewport.getHeight() / 100));
    }

    private boolean fullyInsideHubViewport(Rectangle rect, Rectangle viewport) {
        int padding = hubEdgePadding(viewport);
        return rect.getWidth() > 0 && rect.getHeight() > 0
                && rect.getX() >= viewport.getX()
                && rect.getX() + rect.getWidth() <= viewport.getX() + viewport.getWidth()
                && rect.getY() >= viewport.getY() + padding
                && rect.getY() + rect.getHeight() <= viewport.getY() + viewport.getHeight() - padding;
    }

    private WebElement hubViewportElement(By locator) {
        try {
            return hubViewportElement(locator, currentHubViewport());
        } catch (StaleElementReferenceException e) {
            return null;
        }
    }

    private WebElement hubViewportElement(By locator, Rectangle viewport) {
        try {
            for (WebElement element : driver.findElements(locator)) {
                Rectangle rect = element.getRect();
                if (fullyInsideHubViewport(rect, viewport) && element.isDisplayed()) {
                    return element;
                }
            }
            return null;
        } catch (StaleElementReferenceException e) {
            return null;
        }
    }

    /** Reacquire and recheck the entire target immediately before every Hub item click/retry. */
    private void clickFullyVisibleHubElement(By locator) {
        Rectangle viewport = currentHubViewport();
        for (int attempt = 1; attempt <= maxClickAttempts; attempt++) {
            try {
                WebElement element = waitFor(ExecutionProfile.shortWait())
                        .withMessage("Hub target must be fully visible and enabled before clicking: " + locator)
                        .until(d -> {
                            WebElement candidate = hubViewportElement(locator, viewport);
                            return candidate != null && candidate.isEnabled() ? candidate : null;
                        });
                try {
                    element.click();
                } catch (ElementNotInteractableException failure) {
                    element = waitFor(ExecutionProfile.shortWait()).until(d -> {
                        WebElement candidate = hubViewportElement(locator, viewport);
                        return candidate != null && candidate.isEnabled() ? candidate : null;
                    });
                    driver.executeScript("mobile: clickGesture", Map.of(
                            "elementId", ((RemoteWebElement) element).getId()));
                }
                return;
            } catch (StaleElementReferenceException | NoSuchElementException failure) {
                if (attempt == maxClickAttempts) {
                    throw failure;
                }
            }
        }
    }

    private void click(By locator) {
        click(locator, ExecutionProfile.shortWait());
    }

    private void click(By locator, Duration timeout) {
        for (int attempt = 1; attempt <= maxClickAttempts; attempt++) {
            try {
                WebElement element = waitFor(timeout).until(
                        ExpectedConditions.elementToBeClickable(locator));
                try {
                    element.click();
                } catch (ElementNotInteractableException e) {
                    // Use the fresh element's id, not screen-size-dependent coordinates.
                    element = waitFor(timeout).until(
                            ExpectedConditions.elementToBeClickable(locator));
                    driver.executeScript("mobile: clickGesture", Map.of(
                            "elementId", ((RemoteWebElement) element).getId()));
                }
                return;
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                if (attempt == maxClickAttempts) {
                    throw e;
                }
            }
        }
    }

    private boolean visible(By locator) {
        try {
            for (WebElement element : driver.findElements(locator)) {
                if (element.isDisplayed()) {
                    return true;
                }
            }
            return false;
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean isEmailLoginScreen() {
        return auth.isEmailLoginVisible();
    }

    private boolean pinScreenVisible() {
        return auth.isPinScreenVisible();
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

    private static int positiveInt(String key, int defaultValue) {
        int value = Integer.parseInt(ConfigReader.getOptional(key, String.valueOf(defaultValue)));
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }
}
