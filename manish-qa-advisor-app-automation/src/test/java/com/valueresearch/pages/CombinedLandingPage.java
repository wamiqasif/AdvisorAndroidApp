package com.valueresearch.pages;

import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.ExecutionProfile;
import com.valueresearch.utils.ReportLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Subscription landing and post-login routing for free, active, and lapsed accounts. */
public class CombinedLandingPage {

    private static final By SCROLL = AppiumBy.className("android.widget.ScrollView");
    private static final By ADVICE = AppiumBy.accessibilityId("Advice");
    private static final By FIRST = AppiumBy.accessibilityId("first.");
    private static final By BUY_HOLD_SELL = AppiumBy.accessibilityId("What to buy, hold or sell.");
    private static final By FAMILY = AppiumBy.accessibilityId("For your whole family, in one place.");
    // Match the stable service name; validate each line of the composite description separately.
    private static final By FUND_CARD = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"FUND ADVISOR\")");
    private static final By STOCK_CARD = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"STOCK ADVISOR\")");
    private static final By START_WITH_ONE = AppiumBy.accessibilityId("Start with one. Add the other any time.");
    private static final By PROVENANCE = AppiumBy.accessibilityId(
            "From Value Research, publisher of India’s first mutual fund ratings in 1993.");
    private static final By REGISTRATION = AppiumBy.accessibilityId(
            "SEBI-registered investment adviser · INA100008443");
    // Logout icon in the landing header. It has no content-desc or resource-id, so this relies on it being the
    // 2nd ImageView in the tree (after the Value Research logo). Replace with an accessibility id once dev adds one.
    private static final By LANDING_LOGOUT = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").instance(1)");

    // Native Fund Advisor plan screen.
    private static final By FUND_SERVICE = AppiumBy.accessibilityId("FUND ADVISOR");
    private static final By FUND_HEADING = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Buy the right funds.\")");
    private static final By FUND_SUMMARY = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"A researched portfolio matched to your goals.\")");
    private static final By MONTHLY_PLAN = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"MONTHLY\")");
    private static final By ANNUAL_PLAN = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"ANNUAL\")");
    private static final By GET_STARTED = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Get started\")");
    private static final By SUBSCRIPTION_BACK = AppiumBy.accessibilityId("Go back");
    private static final By SUBSCRIPTION_PROGRESS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").descriptionMatches(\"^[0-9]+[ ]*/[ ]*[0-9]+$\")");

    // Stock Advisor paywall (Flutter). It has no in-app back button; Android Back returns to the landing.
    private static final By STOCK_PAYWALL_BADGE = AppiumBy.accessibilityId("SEBI-REGISTERED INVESTMENT ADVISOR");
    private static final By STOCK_PAYWALL_HEADLINE = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"No tips.\")");
    private static final By STOCK_PAYWALL_SUMMARY = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Three portfolios built on\")");
    private static final By STOCK_SUBSCRIBE = AppiumBy.accessibilityId("Subscribe");

    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By PORTFOLIO = AppiumBy.accessibilityId("Portfolio");
    private static final By FUNDS = AppiumBy.accessibilityId("Funds");
    private static final By STOCKS = AppiumBy.accessibilityId("Stocks");
    private static final By PORTFOLIO_TAB_ROW = AppiumBy.className("android.widget.HorizontalScrollView");
    private static final By SUMMARY_TAB = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Summary, tab \")");
    private static final By FUNDS_SIFS_TAB = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Funds / SIFs, tab \")");
    private static final By STOCKS_ETFS_TAB = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Stocks & ETFs, tab \")");
    private static final By INVESTOR_SELECTION = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Shows investor selection list\")");
    private static final By UPDATE_SUMMARY = AppiumBy.accessibilityId("Update Summary Holdings");
    private static final By SETUP_PORTFOLIO = AppiumBy.accessibilityId("Start setting up your portfolio");
    private static final By BIOMETRIC_CANCEL = By.id("com.android.systemui:id/button_negative");
    private static final By[] COMMON_MARKERS = {ADVICE, FUND_CARD, STOCK_CARD};
    private static final By[] EXISTING_SESSION_MARKERS =
            {ADVICE, FUND_CARD, STOCK_CARD, FUND_SERVICE, STOCK_PAYWALL_HEADLINE};
    private final AndroidDriver driver;
    private final AuthHelper auth;
    private String preparedSessionId;
    private Object previousSelectorTimeout;
    private boolean selectorTimeoutChanged;
    private AdvisorAccountSession accountSession;
    private AuthHelper.Account subscriberAccount;
    private boolean subscriberAccountVerified;

    public CombinedLandingPage(AndroidDriver driver) {
        this.driver = Objects.requireNonNull(driver, "An active AndroidDriver is required");
        this.auth = new AuthHelper(driver);
    }

    public boolean isForDriver(AndroidDriver currentDriver) {
        return driver == currentDriver;
    }

    /** Reuse the recognized common page; authenticate only if the app is actually logged out. */
    public void ensureNonSubscriberLanding(AuthHelper.LoginMethod method) {
        useImmediateSelectors();
        preparedSessionId = null;
        subscriberAccount = null;
        subscriberAccountVerified = false;
        nativeContext();
        for (int attempt = 0; attempt < maxRecoveryBacks(); attempt++) {
            String state = waitFor(ExecutionProfile.longWait()).withMessage(
                    "No recognized Advisor screen appeared during combined landing preparation")
                    .until(d -> currentScreen());
            ReportLogger.step("Preparing combined landing from screen: " + state);
            if ("SECURITY".equals(state)) {
                auth.finishSecurityAuthentication(null, EXISTING_SESSION_MARKERS);
            } else if ("LOGIN".equals(state)) {
                loginAsNonSubscriber(method);
                return;
            } else if ("COMMON".equals(state)) {
                verifyCombinedLandingDisplayed();
                preparedSessionId = driver.getSessionId().toString();
                // This screen exposes subscription state, not the preserved account's email.
                ReportLogger.pass("Combined landing already open; reusing the existing session without logout or scrolling");
                return;
            } else if ("TABS".equals(state)) {
                // A subscriber session is a different starting state. Validate credentials before switching it.
                AuthHelper.validateLoginConfiguration(AuthHelper.Account.NON_SUBSCRIBER, method);
                new Batch1ChangesbyProPage(driver, method).logoutCurrentAccount();
            } else if ("FUND".equals(state)) {
                returnFromFundAdvisor();
            } else if ("STOCK".equals(state)) {
                returnFromStockAdvisor();
            }
        }
        throw new IllegalStateException("Could not prepare the combined landing within the configured recovery limit");
    }

    /** Recover the starting screen without logging in as the free account before subscriber scenarios. */
    public void prepareSubscriberRouting() {
        useImmediateSelectors();
        preparedSessionId = null;
        subscriberAccount = null;
        subscriberAccountVerified = false;
        nativeContext();
        for (int attempt = 0; attempt < maxRecoveryBacks(); attempt++) {
            String state = waitFor(ExecutionProfile.longWait()).withMessage(
                    "No recognized screen appeared before subscriber routing verification")
                    .until(d -> currentScreen());
            if ("SECURITY".equals(state)) {
                auth.finishSecurityAuthentication(null, EXISTING_SESSION_MARKERS);
            } else if ("FUND".equals(state)) {
                returnFromFundAdvisor();
            } else if ("STOCK".equals(state)) {
                returnFromStockAdvisor();
            } else if ("TABS".equals(state)) {
                sharedAccountSession().returnToHub();
                sharedAccountSession().assertHubVisible();
                return;
            } else {
                // LOGIN and COMMON are valid, independent starting states for a fresh subscriber login.
                return;
            }
        }
        throw new IllegalStateException("Could not recover a starting screen for subscriber routing");
    }

    public void verifySubscriberDefaultPortfolio(AuthHelper.Account account, AuthHelper.LoginMethod method) {
        subscriberAccount = Objects.requireNonNull(account, "A subscriber account is required");
        subscriberAccountVerified = false;
        AuthHelper.validateLoginConfiguration(account, method);
        AdvisorAccountSession session = sharedAccountSession();
        session.invalidate();
        nativeContext();
        for (int attempt = 0; attempt < maxRecoveryBacks(); attempt++) {
            String state = waitFor(ExecutionProfile.longWait()).withMessage(
                    "No recognized screen appeared before fresh subscriber login").until(d -> currentScreen());
            if ("SECURITY".equals(state)) {
                auth.finishSecurityAuthentication(null, EXISTING_SESSION_MARKERS);
            } else if ("LOGIN".equals(state)) {
                ReportLogger.step("Fresh login to verify default Portfolio routing for " + account);
                session.loginAndVerifyLanding(account, method, this::verifyDefaultPortfolioLanding);
                subscriberAccountVerified = true;
                return;
            } else if ("TABS".equals(state)) {
                session.logoutCurrentAccount();
            } else if ("COMMON".equals(state)) {
                // Log out directly from the landing header; no subscription page is opened.
                session.logoutFromScreen(LANDING_LOGOUT);
            } else if ("FUND".equals(state)) {
                returnFromFundAdvisor();   // next iteration sees COMMON and logs out
            } else if ("STOCK".equals(state)) {
                returnFromStockAdvisor();  // next iteration sees COMMON and logs out
            }
        }
        throw new IllegalStateException("Could not perform a fresh login for " + account);
    }

    private void verifyDefaultPortfolioLanding() {
        // Never tap Portfolio here: these markers must appear as the result of login itself.
        waitUntil(this::isPortfolioLandingDisplayed, ExecutionProfile.longWait(),
                "Login did not land on the Portfolio summary by default; no Portfolio tab was tapped");
        ReportLogger.pass("Default Portfolio landing verified from summary tabs, portfolio controls and bottom navigation");
    }

    private boolean isPortfolioLandingDisplayed() {
        return visible(PORTFOLIO) && visible(FUNDS) && visible(STOCKS)
                && visible(PORTFOLIO_TAB_ROW) && visible(SUMMARY_TAB)
                && visible(FUNDS_SIFS_TAB) && visible(STOCKS_ETFS_TAB)
                && (visible(UPDATE_SUMMARY) || visible(INVESTOR_SELECTION) || visible(SETUP_PORTFOLIO));
    }

    public boolean hasSubscriberRoutingStarted() {
        return subscriberAccount != null;
    }

    public void finishSubscriberOnHub() {
        if (subscriberAccount == null) return;
        AdvisorAccountSession session = sharedAccountSession();
        if (!subscriberAccountVerified) {
            // On a routing assertion failure, still verify identity and restore Hub after the screenshot.
            session.verifyCurrentAccount(subscriberAccount);
            subscriberAccountVerified = true;
        } else {
            session.assertPrepared();
        }
        session.returnToHub();
        session.assertHubVisible();
        ReportLogger.pass("Subscriber routing case finished on Hub for " + subscriberAccount);
    }

    private AdvisorAccountSession sharedAccountSession() {
        if (accountSession == null) accountSession = new AdvisorAccountSession(driver);
        return accountSession;
    }

    public void loginAsNonSubscriber(AuthHelper.LoginMethod method) {
        auth.loginFromLoginScreen(AuthHelper.Account.NON_SUBSCRIBER, method, COMMON_MARKERS);
        verifyCombinedLandingDisplayed();
        preparedSessionId = driver.getSessionId().toString();
        ReportLogger.pass("Configured non-subscriber authenticated and reached the combined landing page");
    }

    public boolean hasPreparedLandingSession() {
        return driver.getSessionId() != null
                && driver.getSessionId().toString().equals(preparedSessionId);
    }

    public boolean isCombinedLandingDisplayed() {
        return visible(SCROLL) && !visible(SUBSCRIPTION_BACK)
                && ((visible(ADVICE) && visible(FIRST))
                || (visible(FUND_CARD) && visible(STOCK_CARD))
                || (visible(STOCK_CARD) && visible(START_WITH_ONE))
                || (visible(PROVENANCE) && visible(REGISTRATION)));
    }

    public void verifyCombinedLandingDisplayed() {
        nativeContext();
        waitUntil(this::isCombinedLandingDisplayed, ExecutionProfile.longWait(),
                "Expected the combined Fund Advisor / Stock Advisor landing after non-subscriber login");
        for (String tab : List.of("Funds", "Stocks", "Portfolio", "Hub")) {
            if (visible(AppiumBy.accessibilityId(tab))) {
                throw new AssertionError("A never-subscribed account must not have the " + tab + " tab");
            }
        }
    }

    public void verifyLandingHeader() {
        verifyCombinedLandingDisplayed();
        scrollToTop();
        verifyCopy(ADVICE, "Advice");
        verifyCopy(FIRST, "first.");
        verifyCopy(BUY_HOLD_SELL, "What to buy, hold or sell.");
        verifyCopy(FAMILY, "For your whole family, in one place.");
        ReportLogger.pass("Combined landing headline and introductory content verified");
    }

    public void verifyAllLandingContent() {
        verifyLandingHeader();
        verifyCopy(FUND_CARD, "FUND ADVISOR", "Fewer funds.", "Better ones.",
                "A portfolio built for your goals, and a clear call on every fund you hold. Commission-free direct plans.");
        verifyCopy(STOCK_CARD, "STOCK ADVISOR", "Stocks worth owning.", "And why.",
                "Our analysts’ picks, the full reasoning behind each, and updates when our view changes.");
        verifyCopy(START_WITH_ONE, "Start with one. Add the other any time.");
        verifyCopy(PROVENANCE, "From Value Research, publisher of India’s first mutual fund ratings in 1993.");
        // The compliance footer is the bottom-content assertion; do not add an extra boundary gesture.
        verifyCopy(REGISTRATION, "SEBI-registered investment adviser · INA100008443");
        ReportLogger.pass("All supplied landing copy, both service cards and the bottom compliance footer verified");
    }

    public void openFundAdvisor() {
        openService(FUND_CARD, this::isFundSubscriptionDisplayed, "Fund Advisor subscription page");
    }

    public boolean isFundSubscriptionDisplayed() {
        return visible(FUND_SERVICE) && visible(SUBSCRIPTION_BACK)
                && (visible(SUBSCRIPTION_PROGRESS) || visible(GET_STARTED));
    }

    public void verifyFundAdvisorSubscription() {
        waitUntil(this::isFundSubscriptionDisplayed, ExecutionProfile.shortWait(),
                "Expected the native Fund Advisor subscription screen");
        verifyVisibleCopy(FUND_SERVICE, "FUND ADVISOR");
        verifyVisibleCopy(FUND_HEADING, "Buy the right funds. Sharpen the ones you already own.");
        verifyVisibleCopy(FUND_SUMMARY,
                "A researched portfolio matched to your goals. A clear Buy, Sell, or Hold on every fund in your portfolio.");
        verifyPlan(MONTHLY_PLAN, "MONTHLY", "/month · GST included", "Auto-debited monthly via your mandate");
        verifyPlan(ANNUAL_PLAN, "ANNUAL", "/year · GST included");
        verifyGetStarted();
        ReportLogger.pass("Native Fund Advisor subscription content and monthly/annual plans verified");
    }

    public void openStockAdvisor() {
        openService(STOCK_CARD, this::isStockSubscriptionDisplayed, "Stock Advisor subscription page");
    }

    public boolean isStockSubscriptionDisplayed() {
        // The headline + Subscribe pair is unique to this paywall; ADVICE guards against the landing page.
        return visible(STOCK_PAYWALL_HEADLINE) && visible(STOCK_SUBSCRIBE) && !visible(ADVICE);
    }

    public void verifyStockAdvisorSubscription() {
        nativeContext();
        waitUntil(this::isStockSubscriptionDisplayed, ExecutionProfile.longWait(),
                "Expected the Stock Advisor subscription page");
        verifyVisibleCopy(STOCK_PAYWALL_BADGE, "SEBI-REGISTERED INVESTMENT ADVISOR");
        verifyVisibleCopy(STOCK_PAYWALL_HEADLINE, "No tips. No targets. No predictions. Just research.");
        verifyVisibleCopy(STOCK_PAYWALL_SUMMARY,
                "Three portfolios built on 30 years of studying businesses — not stock prices. "
                        + "Every recommendation comes with the reasoning. Every sell comes with an explanation.");
        verifyVisibleCopy(STOCK_SUBSCRIBE, "Subscribe");
        ReportLogger.pass("Stock Advisor subscription page content verified");
    }

    public void returnFromFundAdvisor() {
        nativeContext();
        if (isCombinedLandingDisplayed()) return;
        int attempts = positiveSetting("combinedLandingBackAttempts", 2);
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                WebElement back = waitFor(ExecutionProfile.shortWait()).withMessage(
                        "Fund Advisor must expose its Go back button on the subscription screen").until(d -> {
                    WebElement current = ExpectedConditions.visibilityOfElementLocated(SUBSCRIPTION_BACK).apply(d);
                    return current != null && current.isEnabled() && isFundSubscriptionDisplayed() ? current : null;
                });
                ReportLogger.step("Tapping the Fund Advisor Go back button");
                back.click();
                waitUntil(this::isCombinedLandingDisplayed, ExecutionProfile.shortWait(),
                        "Fund Advisor Go back did not return to the combined landing page");
                ReportLogger.pass("Fund Advisor Go back returned to the combined landing page");
                return;
            } catch (StaleElementReferenceException | TimeoutException failure) {
                // A late transition is already successful. Retry only if the source button is still present.
                if (isCombinedLandingDisplayed()) return;
                if (attempt == attempts - 1 || !isFundSubscriptionDisplayed()) throw failure;
            }
        }
    }

    public void returnFromStockAdvisor() {
        nativeContext();
        if (isCombinedLandingDisplayed()) return;
        int attempts = positiveSetting("combinedLandingBackAttempts", 2);
        for (int attempt = 0; attempt < attempts; attempt++) {
            // Press Back only while the paywall is confirmed; an extra Back on the landing would exit the app.
            waitUntil(this::isStockSubscriptionDisplayed, ExecutionProfile.shortWait(),
                    "Stock Advisor subscription page must be visible before pressing Back");
            ReportLogger.step("Pressing Android Back on the Stock Advisor subscription page");
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            try {
                waitUntil(this::isCombinedLandingDisplayed, ExecutionProfile.shortWait(),
                        "Android Back did not return to the combined landing page");
                ReportLogger.pass("Android Back returned from Stock Advisor to the combined landing page");
                return;
            } catch (TimeoutException failure) {
                if (isCombinedLandingDisplayed()) return;
                if (attempt == attempts - 1) throw failure;
            }
        }
    }

    public void recoverToCombinedLanding() {
        nativeContext();
        for (int attempt = 0; attempt < maxRecoveryBacks(); attempt++) {
            if (isCombinedLandingDisplayed()) return;
            if (isFundSubscriptionDisplayed()) {
                returnFromFundAdvisor();
                return;
            }
            if (isStockSubscriptionDisplayed()) {
                returnFromStockAdvisor();
                return;
            }
            if (securityVisible() || auth.isEmailLoginVisible() || visible(HUB)) {
                throw new IllegalStateException("Landing recovery reached an unprepared authentication/account state");
            }
            // Wait for a known screen instead of pressing Back on an unknown transient screen.
            waitUntil(() -> currentScreen() != null, ExecutionProfile.longWait(),
                    "Could not identify a screen during combined landing recovery");
        }
        throw new IllegalStateException("Combined landing recovery exceeded the configured limit");
    }

    public void restoreTimingSettings() {
        try {
            if (accountSession != null) accountSession.restoreTimingSettings();
        } finally {
            if (selectorTimeoutChanged) {
                try {
                    driver.setSetting("waitForSelectorTimeout", previousSelectorTimeout);
                } finally {
                    selectorTimeoutChanged = false;
                }
            }
        }
    }

    private void useImmediateSelectors() {
        if (selectorTimeoutChanged) return;
        previousSelectorTimeout = driver.getSettings().get("waitForSelectorTimeout");
        if (!(previousSelectorTimeout instanceof Number)) {
            throw new IllegalStateException("Cannot read the active Appium selector timeout");
        }
        selectorTimeoutChanged = true;
        // Screen recognition checks several alternative markers; explicit waits own the polling.
        driver.setSetting("waitForSelectorTimeout", 0);
    }

    private void openService(By card, BooleanSupplier destination, String description) {
        verifyCombinedLandingDisplayed();
        // A card can already be visible after footer verification. Reposition only when needed.
        if (visibleTarget(card, true) == null) scrollToTop();
        scrollIntoView(card, true);
        ReportLogger.step("Opening " + description + " using the visible service card");
        for (int attempt = 0; attempt < 3; attempt++) {
            if (destination.getAsBoolean()) return;
            verifyCombinedLandingDisplayed();
            tap(card);
            try {
                waitUntil(destination, ExecutionProfile.longWait(), description + " did not open");
                return;
            } catch (TimeoutException failure) {
                if (attempt == 2 || !isCombinedLandingDisplayed()) throw failure;
            }
        }
    }

    private String currentScreen() {
        if (securityVisible()) return "SECURITY";
        if (auth.isEmailLoginVisible()) return "LOGIN";
        if (isCombinedLandingDisplayed()) return "COMMON";
        if (isFundSubscriptionDisplayed()) return "FUND";
        if (isStockSubscriptionDisplayed()) return "STOCK";
        return visible(HUB) || (visible(PORTFOLIO) && visible(FUNDS) && visible(STOCKS)) ? "TABS" : null;
    }

    private boolean securityVisible() {
        return visible(BIOMETRIC_CANCEL) || auth.isPinScreenVisible();
    }

    private void verifyCopy(By locator, String... expectedLines) {
        verifyCopy(locator, true, expectedLines);
    }

    private void verifyVisibleCopy(By locator, String... expectedLines) {
        verifyCopy(locator, false, expectedLines);
    }

    private void verifyCopy(By locator, boolean allowScrolling, String... expectedLines) {
        String expected = normalize(String.join(" ", expectedLines));
        ReportLogger.step("Verifying content: " + expected);
        if (allowScrolling) scrollIntoView(locator);
        String actual = readVisibleCopy(locator);
        if (!expected.equals(actual)) {
            throw new AssertionError("Content mismatch for " + locator
                    + " | expected: " + expected + " | actual: " + actual);
        }
        ReportLogger.pass("Verified content: " + expected);
    }

    private String readVisibleCopy(By locator) {
        return waitFor(ExecutionProfile.shortWait()).withMessage(
                "Content could not be read from " + locator).until(d -> {
                    WebElement current = ExpectedConditions.visibilityOfElementLocated(locator).apply(d);
                    if (current == null) return null;
                    String description = normalize(current.getAttribute("content-desc"));
                    String copy = description.isEmpty() ? normalize(current.getText()) : description;
                    return copy.isEmpty() ? null : copy;
                });
    }

    private void verifyPlan(By locator, String name, String... expectedTerms) {
        String copy = readVisibleCopy(locator);
        if (!copy.startsWith(name + " ") || !copy.matches(".*₹[0-9][0-9,]*(?:\\.[0-9]{1,2})?.*")) {
            throw new AssertionError("Expected the " + name + " plan with a displayed price; actual: " + copy);
        }
        for (String term : expectedTerms) {
            if (!copy.contains(normalize(term))) {
                throw new AssertionError(name + " plan is missing expected text: " + term + " | actual: " + copy);
            }
        }
        ReportLogger.pass("Verified " + name + " plan, billing terms, and displayed price format");
    }

    private void verifyGetStarted() {
        String copy = readVisibleCopy(GET_STARTED);
        // Prices vary by product and offer. Verify the CTA and currency format without fixing its amount.
        if (!copy.matches("Get started · ₹[0-9][0-9,]*(?:\\.[0-9]{1,2})?")) {
            throw new AssertionError("Expected Get started with a displayed subscription price; actual: " + copy);
        }
        ReportLogger.pass("Verified Get started subscription CTA");
    }

    private WebElement scrollIntoView(By locator) {
        return scrollIntoView(locator, false);
    }

    private WebElement scrollIntoView(By locator, boolean requireTapPoint) {
        for (int swipe = 0; swipe < maxScrolls(); swipe++) {
            WebElement element = visibleTarget(locator, requireTapPoint);
            if (element != null) return element;
            boolean more = scroll("down");
            try {
                // Wait only for the requested copy/card, rather than compare the entire Flutter hierarchy.
                return waitFor(ExecutionProfile.shortWait()).withMessage("Expected visible content: " + locator)
                        .until(d -> visibleTarget(locator, requireTapPoint));
            } catch (TimeoutException missingTarget) {
                if (!more || swipe == maxScrolls() - 1) throw missingTarget;
            }
        }
        return waitFor(ExecutionProfile.shortWait()).withMessage("Expected visible landing content: " + locator)
                .until(d -> visibleTarget(locator, requireTapPoint));
    }

    private WebElement visibleTarget(By locator, boolean requireTapPoint) {
        WebElement element = ExpectedConditions.visibilityOfElementLocated(locator).apply(driver);
        return element != null && (!requireTapPoint || (element.isEnabled() && tapPointInViewport(element.getRect())))
                ? element : null;
    }

    private boolean tapPointInViewport(Rectangle bounds) {
        WebElement content = firstVisible(SCROLL);
        if (content == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return false;
        Rectangle viewport = content.getRect();
        Dimension screen = driver.manage().window().getSize();
        int x = bounds.getX() + bounds.getWidth() / 2;
        int y = bounds.getY() + bounds.getHeight() / 2;
        return x > Math.max(0, viewport.getX()) && y > Math.max(0, viewport.getY())
                && x < Math.min(screen.getWidth(), viewport.getX() + viewport.getWidth())
                && y < Math.min(screen.getHeight(), viewport.getY() + viewport.getHeight());
    }

    private void scrollToTop() {
        // An upward gesture at the top can trigger pull-to-refresh on this Flutter screen.
        // Stop as soon as the headline is in the viewport, without probing the top boundary.
        for (int swipe = 0; swipe < maxScrolls(); swipe++) {
            if (landingHeaderVisible()) return;
            boolean more = scroll("up");
            try {
                waitUntil(this::landingHeaderVisible, ExecutionProfile.shortWait(),
                        "Combined landing headline is not yet visible");
                return;
            } catch (TimeoutException missingHeader) {
                if (!more || swipe == maxScrolls() - 1) throw missingHeader;
            }
        }
        waitUntil(this::landingHeaderVisible, ExecutionProfile.shortWait(),
                "Combined landing headline did not become visible after scrolling toward the top");
    }

    private boolean landingHeaderVisible() {
        return fullyOnScreen(ADVICE) && fullyOnScreen(FIRST);
    }

    private boolean fullyOnScreen(By locator) {
        WebElement element = firstVisible(locator);
        if (element == null) return false;
        Rectangle bounds = element.getRect();
        Dimension screen = driver.manage().window().getSize();
        return bounds.getWidth() > 0 && bounds.getHeight() > 0
                && bounds.getX() >= 0 && bounds.getY() >= 0
                && bounds.getX() + bounds.getWidth() <= screen.getWidth()
                && bounds.getY() + bounds.getHeight() <= screen.getHeight();
    }

    private boolean scroll(String direction) {
        WebElement container = waitFor(ExecutionProfile.defaultWait())
                .until(ExpectedConditions.visibilityOfElementLocated(SCROLL));
        Rectangle bounds = container.getRect();
        Object more = driver.executeScript("mobile: scrollGesture", Map.of(
                "left", bounds.getX() + bounds.getWidth() / 10,
                "top", bounds.getY() + bounds.getHeight() / 10,
                "width", bounds.getWidth() * 8 / 10,
                "height", bounds.getHeight() * 8 / 10,
                "direction", direction, "percent", 0.65));
        // A missing result must not be treated as proof that the page reached its boundary.
        if (!(more instanceof Boolean)) throw new IllegalStateException("Appium did not return scroll availability");
        return (Boolean) more;
    }

    private void tap(By locator) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                final Rectangle[] previous = {null};
                WebElement element = waitFor(ExecutionProfile.defaultWait())
                        .withMessage("Expected a visible, enabled service card: " + locator)
                        .until(d -> {
                            WebElement current = firstVisible(locator);
                            if (current == null || !current.isEnabled()) {
                                previous[0] = null;
                                return null;
                            }
                            Rectangle bounds = current.getRect();
                            boolean stable = tapPointInViewport(bounds) && bounds.equals(previous[0]);
                            previous[0] = bounds;
                            return stable ? current : null;
                        });
                driver.executeScript("mobile: clickGesture", Map.of(
                        "elementId", ((RemoteWebElement) element).getId()));
                return;
            } catch (StaleElementReferenceException failure) {
                if (attempt == 2) throw failure;
            }
        }
    }

    private WebElement firstVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) return element;
            } catch (StaleElementReferenceException replacedElement) {
                // Resolve a fresh element on the next explicit-wait poll.
            }
        }
        return null;
    }

    private boolean visible(By locator) {
        return firstVisible(locator) != null;
    }

    private void nativeContext() {
        if (!"NATIVE_APP".equals(driver.getContext())) driver.context("NATIVE_APP");
    }

    private WebDriverWait waitFor(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout, ExecutionProfile.pollingInterval());
        wait.ignoring(NoSuchElementException.class).ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout, String message) {
        waitFor(timeout).withMessage(message).until(d -> condition.getAsBoolean());
    }

    private int maxScrolls() {
        return positiveSetting("combinedLandingMaxScrolls", 12);
    }

    private int maxRecoveryBacks() {
        return positiveSetting("combinedLandingMaxRecoveryBacks", 5);
    }

    private int positiveSetting(String key, int defaultValue) {
        int value = Integer.parseInt(ConfigReader.getOptional(key, String.valueOf(defaultValue)));
        if (value <= 0) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\\r\\n", " ").replace("\\n", " ")
                .replace("\\r", " ").replace("\\t", " ").replace('’', '\'').replace('‘', '\'')
                .replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }
}