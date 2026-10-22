package com.valueresearch.utils;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
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
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Shared account verification/navigation. AuthHelper owns credentials, login, OTP and PIN. */
public class AdvisorAccountSession {
    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By SCROLL = AppiumBy.className("android.widget.ScrollView");
    private static final By ANY_BACK = AppiumBy.accessibilityId("Go back");
    private static final By BIOMETRIC_CANCEL = By.id("com.android.systemui:id/button_negative");
    private static final By ACCOUNT_ENTRY = AppiumBy.accessibilityId("View and edit your account details");
    private static final By ACCOUNT_TITLE = AppiumBy.accessibilityId("Account Details");
    private static final By EMAIL_LABEL = AppiumBy.accessibilityId("Email Address");
    private static final By EMAIL_VALUE = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").textMatches(\".+@.+\")");
    private static final By ACCOUNT_TITLE_TEXT = AppiumBy.androidUIAutomator(
            "new UiSelector().text(\"Account Details\")");
    private static final By EMAIL_LABEL_TEXT = AppiumBy.androidUIAutomator(
            "new UiSelector().text(\"Email Address\")");
    private static final By EMAIL_ANY_TEXT = AppiumBy.androidUIAutomator(
            "new UiSelector().textMatches(\".+@.+\")");
    private static final By EMAIL_DESCRIPTION = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\".+@.+\")");
    private static final By LOGOUT = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").description(\"Logout\")");
    private static final By LOGOUT_CONFIRMATION = AppiumBy.accessibilityId("Are you sure you want to logout?");
    private static final By CONFIRM_LOGOUT = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").description(\"Logout\")");
    private static final By HUB_SECTIONS = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\")"
                    + ".descriptionMatches(\"^(Profile|Investor Accounts|Mutual Funds|Stocks|More)$\")");
    private static final By HUB_TILES = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Open \")");

    private final AndroidDriver driver;
    private final AuthHelper auth;
    private AuthHelper.Account verifiedAccount;
    private String verifiedEmail;
    private String verifiedSessionId;
    private boolean timingsChanged;
    private Object previousSelectorTimeout;
    private Object previousIdleTimeout;
    private Duration previousImplicitWait;

    public AdvisorAccountSession(AndroidDriver driver) {
        this.driver = Objects.requireNonNull(driver,
                "AndroidDriver is null during Advisor account setup; check BaseTest/DriverManager initialization");
        if (driver.getSessionId() == null) {
            throw new IllegalStateException(
                    "AndroidDriver has no active Appium session during Advisor account setup");
        }
        this.auth = new AuthHelper(driver);
    }

    public boolean isForDriver(AndroidDriver candidate) { return driver == candidate; }

    /** Verify once per account/email/driver session; never log out a matching account. */
    public void prepare(AuthHelper.Account account, AuthHelper.LoginMethod method) {
        prepare(account, method, () -> false);
    }

    /** Keep a recognized feature screen only after verifying this account in this session.
     * Security prompts and logged-out screens always take priority over cached UI.
     * The original two-argument API still returns to Hub for existing callers.
     */
    public void prepare(AuthHelper.Account account, AuthHelper.LoginMethod method,
                        BooleanSupplier featureScreenVisible) {
        Objects.requireNonNull(featureScreenVisible, "Feature screen detection is required");
        Objects.requireNonNull(account, "Account is required");
        Objects.requireNonNull(method, "Login method is required");
        try {
            beginImmediateSelectors();
            if (!"NATIVE_APP".equals(driver.getContext())) driver.context("NATIVE_APP");
            String requiredEmail = AuthHelper.configuredEmail(account);
            if (requiredEmail == null || requiredEmail.trim().isEmpty()) {
                throw new IllegalStateException("No configured email for account " + account
                        + "; verify test account configuration");
            }
            boolean cached = account == verifiedAccount && requiredEmail.equalsIgnoreCase(verifiedEmail)
                    && sessionId().equals(verifiedSessionId);
            if (cached && !securityVisible() && !auth.isEmailLoginVisible()
                    && featureScreenVisible.getAsBoolean()) return;
            waitUntil(() -> securityVisible() || auth.isEmailLoginVisible()
                            || visible(HUB) || visible(ANY_BACK) || featureScreenVisible.getAsBoolean(), ExecutionProfile.longWait(),
                    "No known Advisor authentication/navigation screen appeared");
            // A Hub tab can remain exposed behind the Android biometric dialog.
            if (securityVisible()) auth.finishSecurityAuthentication(cached ? account : null);
            if (auth.isEmailLoginVisible()) {
                invalidate();
            } else {
                if (cached && featureScreenVisible.getAsBoolean()) return;
                returnToHub();
                if (cached) return;
                openAccountDetails();
                if (readAccountEmail().equalsIgnoreCase(requiredEmail)) {
                    backFromAccountDetails();
                    remember(account, requiredEmail);
                    ReportLogger.pass("Verified existing " + account + " account; reusing its session");
                    return;
                }
                // Validate before destroying a usable session; no OTP preflight for PASSWORD.
                AuthHelper.validateLoginConfiguration(account, method);
                ReportLogger.step("Switching to the configured " + account + " through Logout");
                logoutFromAccountDetails();
            }
            auth.loginFromLoginScreen(account, method);
            returnToHub();
            openAccountDetails();
            if (!readAccountEmail().equalsIgnoreCase(requiredEmail)) {
                throw new AssertionError("Logged-in email does not match the configured " + account);
            }
            backFromAccountDetails();
            remember(account, requiredEmail);
            ReportLogger.pass("Logged-in account email verified for " + account);
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            invalidate();
            throw failure;
        }
    }

    /** Page objects with common setup use this instead of starting another login. */
    public void assertPrepared() {
        if (verifiedAccount == null || !sessionId().equals(verifiedSessionId)
                || !AuthHelper.configuredEmail(verifiedAccount).equalsIgnoreCase(verifiedEmail)
                || securityVisible() || auth.isEmailLoginVisible()) {
            invalidate();
            throw new IllegalStateException("Prepare the required Advisor account before module navigation");
        }
    }

    public void invalidate() {
        verifiedAccount = null;
        verifiedEmail = null;
        verifiedSessionId = null;
    }

    private void remember(AuthHelper.Account account, String email) {
        verifiedAccount = account;
        verifiedEmail = email;
        verifiedSessionId = sessionId();
    }

    private String sessionId() {
        if (driver.getSessionId() == null) throw new IllegalStateException("AndroidDriver session is unavailable");
        return driver.getSessionId().toString();
    }

    private boolean securityVisible() { return visible(BIOMETRIC_CANCEL) || auth.isPinScreenVisible(); }

    private boolean hubVisible() {
        return visible(HUB) && visible(SCROLL) && !visible(ANY_BACK) && !securityVisible()
                && (visible(ACCOUNT_ENTRY) || visible(HUB_SECTIONS) || visible(HUB_TILES));
    }

    public void assertHubVisible() {
        waitUntil(this::hubVisible, ExecutionProfile.defaultWait(), "Expected the Hub screen");
    }

    private void returnToHub() {
        for (int attempt = 0; attempt < 5; attempt++) {
            if (hubVisible()) return;
            if (securityVisible() || auth.isEmailLoginVisible()) {
                throw new IllegalStateException("Hub recovery reached an authentication screen");
            }
            if (accountTitleVisible()) {
                backFromAccountDetails();
                return;
            }
            if (visible(ANY_BACK)) {
                tap(ANY_BACK);
                waitUntil(() -> hubVisible() || visible(HUB) || securityVisible()
                                || auth.isEmailLoginVisible(), ExecutionProfile.shortWait(),
                        "Back did not restore known app navigation");
            } else if (visible(HUB)) {
                tap(HUB);
                assertHubVisible();
                return;
            } else {
                driver.navigate().back();
                waitUntil(() -> hubVisible() || visible(HUB) || visible(ANY_BACK)
                                || auth.isEmailLoginVisible(), ExecutionProfile.shortWait(),
                        "Recovery did not restore known app navigation");
            }
        }
        assertHubVisible();
    }

    private void openAccountDetails() {
        if (accountDetailsReady()) return;
        assertHubVisible();
        Rectangle viewport = hubViewport();
        for (int swipe = 0; inViewport(ACCOUNT_ENTRY, viewport) == null; swipe++) {
            if (swipe >= 12) throw new AssertionError("Account Details entry was not found within 12 Hub swipes");
            swipeToAccountEntry(viewport);
            awaitHubSettled(viewport);
        }
        ReportLogger.step("Opening Account Details from the visible Hub account entry");
        tap(ACCOUNT_ENTRY);
        try {
            waitUntil(() -> accountTitleVisible() || !hubVisible(), ExecutionProfile.shortWait(),
                    "Account entry tap did not leave Hub");
        } catch (TimeoutException noNavigation) {
            // Retry only when the source page and the same fully visible control remain.
            // An elementId click can return successfully without activating a Flutter node.
            WebElement entry = hubVisible() ? inViewport(ACCOUNT_ENTRY, hubViewport()) : null;
            if (entry != null && entry.isEnabled() && !accountTitleVisible()) {
                Rectangle bounds = entry.getRect();
                ReportLogger.step("Hub is still visible; retrying Account Details at its current visible center");
                driver.executeScript("mobile: clickGesture", Map.of(
                        "x", bounds.getX() + bounds.getWidth() / 2,
                        "y", bounds.getY() + bounds.getHeight() / 2));
            }
        }
        try {
            waitUntil(this::accountDetailsReady, ExecutionProfile.defaultWait(),
                    "Account Details did not finish opening");
            ReportLogger.pass("Account Details screen recognized; verifying the account email");
        } catch (TimeoutException notReady) {
            boolean heading = accountTitleVisible();
            boolean emailLabel = visible(EMAIL_LABEL) || visible(EMAIL_LABEL_TEXT);
            String state = heading
                    ? "Account Details heading is visible, but its email label/value is not readable"
                    : hubVisible()
                        ? "Hub is still visible after the account-entry tap; Account Details navigation did not complete"
                        : "Hub is no longer recognized, but Account Details is not recognized either";
            throw new TimeoutException("Account Details did not finish opening | " + state
                    + " | accountHeadingVisible=" + heading + " | emailLabelVisible=" + emailLabel
                    + ". Capture the failure screenshot and Inspector XML to distinguish missing"
                    + " accessibility labels from an app loading problem.", notReady);
        }
    }

    private boolean accountTitleVisible() {
        return visible(ACCOUNT_TITLE) || visible(ACCOUNT_TITLE_TEXT);
    }

    private boolean accountDetailsReady() {
        // The purpose is to verify the actual account. A readable account email is
        // sufficient when Flutter does not expose the separate 'Email Address' label.
        return accountTitleVisible() && (visible(EMAIL_LABEL) || visible(EMAIL_LABEL_TEXT)
                || !readVisibleAccountEmail().isEmpty());
    }

    private String readAccountEmail() {
        return waitFor(ExecutionProfile.defaultWait()).withMessage("A unique account email was not exposed").until(d -> {
            String email = readVisibleAccountEmail();
            return email.isEmpty() ? null : email;
        });
    }

    private String readVisibleAccountEmail() {
        Set<String> emails = new LinkedHashSet<>();
        for (By locator : new By[]{EMAIL_VALUE, EMAIL_ANY_TEXT, EMAIL_DESCRIPTION}) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    if (!element.isDisplayed()) continue;
                    collectExactAccountEmails(element.getText(), emails);
                    collectExactAccountEmails(element.getAttribute("content-desc"), emails);
                } catch (StaleElementReferenceException replacedEmail) {
                    // Reacquire the field on the next explicit-wait poll.
                }
            }
        }
        if (emails.size() > 1) {
            throw new IllegalStateException("More than one account email was exposed; refusing to guess");
        }
        return emails.isEmpty() ? "" : emails.iterator().next();
    }

    private void collectExactAccountEmails(String raw, Set<String> emails) {
        if (raw == null) return;
        for (String line : raw.split("\\r?\\n")) {
            String value = line.trim();
            if (value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                emails.add(value.toLowerCase(Locale.ROOT));
            }
        }
    }

    private void logoutFromAccountDetails() {
        tap(LOGOUT);
        waitUntil(() -> visible(LOGOUT_CONFIRMATION) && visible(CONFIRM_LOGOUT),
                ExecutionProfile.defaultWait(), "Logout confirmation did not open");
        tap(CONFIRM_LOGOUT);
        waitUntil(() -> !visible(LOGOUT_CONFIRMATION) && auth.isEmailLoginVisible(),
                ExecutionProfile.longWait(), "Confirmed Logout did not reach email login");
    }

    private void backFromAccountDetails() {
        if (driver.isKeyboardShown()) driver.hideKeyboard();
        tap(ANY_BACK);
        waitUntil(() -> !accountTitleVisible() && hubVisible(), ExecutionProfile.defaultWait(),
                "Account Details Back did not return to Hub");
    }

    private Rectangle hubViewport() {
        Rectangle bounds = waitFor(ExecutionProfile.shortWait()).until(d -> firstVisible(SCROLL)).getRect();
        Dimension screen = driver.manage().window().getSize();
        Rectangle hub = waitFor(ExecutionProfile.shortWait()).until(d -> firstVisible(HUB)).getRect();
        int left = Math.max(0, bounds.getX()), top = Math.max(0, bounds.getY());
        int right = Math.min(screen.getWidth(), bounds.getX() + bounds.getWidth());
        int bottom = Math.min(hub.getY(), Math.min(screen.getHeight(), bounds.getY() + bounds.getHeight()));
        if (right - left < 80 || bottom - top < 160) throw new IllegalStateException("Hub has no usable scroll viewport");
        return new Rectangle(new Point(left, top), new Dimension(right - left, bottom - top));
    }

    private WebElement inViewport(By locator, Rectangle viewport) {
        for (WebElement element : driver.findElements(locator)) {
            Rectangle r = element.getRect();
            if (element.isDisplayed() && r.getWidth() > 0 && r.getHeight() > 0
                    && r.getX() >= viewport.getX() && r.getY() >= viewport.getY() + 8
                    && r.getX() + r.getWidth() <= viewport.getX() + viewport.getWidth()
                    && r.getY() + r.getHeight() <= viewport.getY() + viewport.getHeight() - 8) return element;
        }
        return null;
    }

    private void swipeToAccountEntry(Rectangle viewport) {
        boolean towardsTop = true;
        double fraction = 0.60;
        for (WebElement element : driver.findElements(ACCOUNT_ENTRY)) {
            Rectangle r = element.getRect();
            if (r.getWidth() <= 0 || r.getHeight() <= 0) continue;
            if (r.getHeight() > viewport.getHeight() - 16) {
                throw new IllegalStateException("Account Details entry cannot fit in the Hub viewport");
            }
            if (r.getY() + r.getHeight() > viewport.getY() + viewport.getHeight() - 8) towardsTop = false;
            if (r.getY() < viewport.getY() + viewport.getHeight() && r.getY() + r.getHeight() > viewport.getY()) {
                int clipped = towardsTop ? viewport.getY() + 8 - r.getY()
                        : r.getY() + r.getHeight() - (viewport.getY() + viewport.getHeight() - 8);
                if (clipped > 0) fraction = Math.min(fraction, Math.max(0.025, (clipped + 8.0) / viewport.getHeight()));
            }
            break;
        }
        int x = viewport.getX() + viewport.getWidth() / 2;
        int startY = viewport.getY() + (int) (viewport.getHeight() * (towardsTop ? 0.16 : 0.84));
        int endY = startY + (int) (viewport.getHeight() * (towardsTop ? fraction : -fraction));
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "advisor-account-hub");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(10)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(200), PointerInput.Origin.viewport(), x, endY));
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

    private void awaitHubSettled(Rectangle viewport) {
        final Map<?, ?>[] previous = {null};
        waitFor(ExecutionProfile.shortWait()).withMessage("Hub content did not settle after scrolling").until(d -> {
            Map<String, Rectangle> current = new LinkedHashMap<>();
            for (By locator : new By[]{ACCOUNT_ENTRY, HUB_SECTIONS, HUB_TILES}) {
                for (WebElement element : driver.findElements(locator)) {
                    if (element.isDisplayed()) current.put(element.getAttribute("content-desc"), element.getRect());
                }
                if (!current.isEmpty()) break;
            }
            if (current.isEmpty()) current.put("viewport", viewport);
            boolean stable = current.equals(previous[0]);
            previous[0] = current;
            return stable;
        });
    }

    private void tap(By locator) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement element = waitFor(ExecutionProfile.shortWait()).withMessage("Control is unavailable: " + locator)
                        .until(d -> {
                            WebElement candidate = firstVisible(locator);
                            return candidate != null && candidate.isEnabled() ? candidate : null;
                        });
                driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) element).getId()));
                return;
            } catch (StaleElementReferenceException stale) {
                if (attempt == 2) throw stale;
            }
        }
    }

    private WebElement firstVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) if (element.isDisplayed()) return element;
        return null;
    }

    private boolean visible(By locator) { return firstVisible(locator) != null; }

    private WebDriverWait waitFor(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.pollingEvery(ExecutionProfile.pollingInterval());
        wait.ignoring(NoSuchElementException.class).ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout, String message) {
        waitFor(timeout).withMessage(message).until(d -> condition.getAsBoolean());
    }

    private void beginImmediateSelectors() {
        if (timingsChanged) return;
        Map<String, Object> settings = Objects.requireNonNull(driver.getSettings(),
                "Appium returned null settings for the active session");
        previousSelectorTimeout = settings.get("waitForSelectorTimeout");
        previousIdleTimeout = settings.get("waitForIdleTimeout");
        if (!(previousSelectorTimeout instanceof Number)) throw new IllegalStateException("Cannot read Appium selector timeout");
        previousImplicitWait = driver.manage().timeouts().getImplicitWaitTimeout();
        timingsChanged = true;
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.setSetting("waitForSelectorTimeout", 0);
        if (previousIdleTimeout instanceof Number) driver.setSetting("waitForIdleTimeout", 0);
    }

    /** Restore shared settings after each test; keep only the verified-account identity. */
    public void restoreTimingSettings() {
        if (!timingsChanged) return;
        try {
            try {
                driver.setSetting("waitForSelectorTimeout", previousSelectorTimeout);
            } finally {
                try {
                    if (previousIdleTimeout instanceof Number) driver.setSetting("waitForIdleTimeout", previousIdleTimeout);
                } finally {
                    if (previousImplicitWait != null) {
                        driver.manage().timeouts().implicitlyWait(previousImplicitWait);
                    }
                }
            }
        } finally {
            timingsChanged = false;
        }
    }
}
