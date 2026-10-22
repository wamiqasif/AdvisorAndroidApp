package com.valueresearch.utils;

import com.valueresearch.pages.LoginPage;
import com.valueresearch.pages.OtpPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.jsoup.Jsoup;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared Advisor authentication. Credentials, OTP, password fields and PIN handling live here. */
public class AuthHelper {
    public enum LoginMethod { OTP, PASSWORD }
    public enum Account {
        DEFAULT("", ""), REGRESSION("Regression", "REGRESSION"),
        ACTIVE_SUBSCRIBER("ActiveSubscriber", "ACTIVE_SUBSCRIBER"),
        LAPSED_USER("LapsedUser", "LAPSED_USER"), NON_SUBSCRIBER("NonSubscriber", "NON_SUBSCRIBER");
        private final String suffix;
        private final String environmentSuffix;
        Account(String suffix, String environmentSuffix) { this.suffix = suffix; this.environmentSuffix = environmentSuffix; }
    }

    private final AndroidDriver driver;
    private long lastInfrastructureHealthCheckAtMs;
    private static final By LOGIN_TITLE = AppiumBy.accessibilityId("Log in");
    private static final By NEXT = AppiumBy.accessibilityId("Next");
    private static final By CONTINUE_WITH_PASSWORD = AppiumBy.accessibilityId("Continue with Password");
    private static final By OTP_TITLE = AppiumBy.accessibilityId("Enter OTP");
    private static final By VERIFY_OTP = AppiumBy.accessibilityId("Verify OTP");
    private static final By INPUT = AppiumBy.className("android.widget.EditText");
    private static final By FUNDS = AppiumBy.accessibilityId("Funds");
    private static final By HUB = AppiumBy.accessibilityId("Hub");
    private static final By BIOMETRIC_CANCEL = By.id("com.android.systemui:id/button_negative");
    private static final By USE_BIOMETRICS = AppiumBy.accessibilityId("Use biometrics");
    private static final By PIN_DELETE = AppiumBy.accessibilityId("Delete last digit");
    private static final By[] TABS = {FUNDS, AppiumBy.accessibilityId("Stocks"), AppiumBy.accessibilityId("Portfolio"), HUB};
    private static final By[] PIN_MARKERS = {
        AppiumBy.accessibilityId("Enter your Advisor PIN"), AppiumBy.accessibilityId("Enter PIN"), AppiumBy.accessibilityId("Advisor PIN"),
        AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Enter your Advisor PIN\")"),
        AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Enter PIN\")"),
        AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Advisor PIN\")")
    };
    private static final By PIN_ERROR = AppiumBy.androidUIAutomator("new UiSelector().descriptionMatches(\"(?i).*(incorrect|invalid).*PIN.*\")");
    private static final By PIN_ERROR_TEXT = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i).*(incorrect|invalid).*PIN.*\")");

    public AuthHelper(AndroidDriver driver) {
        if (driver == null) throw new IllegalArgumentException("An active AndroidDriver is required");
        this.driver = driver;
    }

    /** Existing entry point: keep the current session, otherwise use testEmail/testEmailPass. */
    public void ensureLoggedIn() { ensureLoggedIn(Account.DEFAULT, null); }
    public void ensureLoggedInForOnboarding() {
        ReportLogger.step("Checking Advisor app login/session state before onboarding");

        By[] onboardingPostLoginMarkers = new By[]{
                AppiumBy.accessibilityId("Subscribe Now"),
                AppiumBy.accessibilityId("Starts at just"),
                AppiumBy.accessibilityId("Continue to Payment"),
                AppiumBy.accessibilityId("Verify PAN and KYC"),

                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Subscribe Now\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Starts at just\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"right partner\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Continue to Payment\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Verify PAN and KYC\")")
        };

        ensureLoggedIn(Account.DEFAULT, onboardingPostLoginMarkers);
    }


    private void ensureLoggedIn(Account account, By[] additional) {
        nativeContext();
        String state = waitFor(ExecutionProfile.longWait()).withMessage("No known Advisor login/session screen appeared").until(d -> {
            assertInfrastructureHealthy("while detecting login/session state", 1500L);
            if (visible(BIOMETRIC_CANCEL) || isPinScreenVisible()) return "SECURITY";
            if (postLoginVisible(additional)) return "AUTHENTICATED";
            return isEmailLoginVisible() ? "LOGIN" : null;
        });
        if (state.equals("LOGIN")) loginFromLoginScreen(account, configuredLoginMethod(), additional);
        else if (state.equals("SECURITY")) finishSecurityAuthentication(null, additional);
        else ReportLogger.pass("Existing authenticated session reused");
    }

    /** Caller owns account verification/logout. This method never switches an authenticated account. */
    public void loginFromLoginScreen(Account account) { loginFromLoginScreen(account, configuredLoginMethod(), null); }

    /** Explicit suite selection overrides global/default configuration for this login only. */
    public void loginFromLoginScreen(Account account, LoginMethod method) { loginFromLoginScreen(account, method, null); }

    private void loginFromLoginScreen(Account account, LoginMethod method, By[] additional) {
        Objects.requireNonNull(account, "A target account is required for login");
        Objects.requireNonNull(method, "An explicit login method is required");
        nativeContext();
        validateLoginConfiguration(account, method);
        waitUntil(this::isEmailLoginVisible, ExecutionProfile.longWait(), "Email login did not appear");
        ReportLogger.step("Logging in as " + account + " using " + method);
        if (method == LoginMethod.PASSWORD) loginWithPassword(account, additional);
        else loginWithOtp(account, additional);
        finishSecurityAuthentication(account, additional);
        ReportLogger.pass("Configured login and security authentication completed");
    }

    /** Global selection takes precedence. Missing selection preserves OTP behavior. */
    public static LoginMethod configuredLoginMethod() {
        String value = configured("LOGIN_METHOD", "loginMethod", "batch1LoginMethod");
        if (value.isEmpty()) value = configured("STOCK_ANALYSTS_CHOICE_LOGIN_METHOD", "stockAnalystsChoiceLoginMethod");
        if (value.isEmpty()) return LoginMethod.OTP;
        try { return LoginMethod.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("loginMethod must be OTP or PASSWORD"); }
    }

    public static String configuredEmail(Account account) {
        Objects.requireNonNull(account, "Account is required");
        String value;
        switch (account) {
            case ACTIVE_SUBSCRIBER: value = accountValue(account, false, "ActiveSubscriberEmail"); break;
            case LAPSED_USER: value = accountValue(account, false, "LapsedUserEmail"); break;
            case NON_SUBSCRIBER: value = accountValue(account, false, "NonSubscriberEmail"); break;
            case REGRESSION: value = configured("TEST_EMAIL_REGRESSION", "testEmailRegression"); break;
            default: value = configured("TEST_EMAIL", "testEmail", "email", "otpEmail", "OtpEmail");
        }
        value = value.trim();
        if (!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Configure a valid login email for " + account);
        return value;
    }

    private static String configuredPassword(Account account) {
        String value;
        String key;
        switch (account) {
            case ACTIVE_SUBSCRIBER: key = "ActiveUserPass"; value = accountValue(account, true, key); break;
            case LAPSED_USER: key = "LapsedUserPass"; value = accountValue(account, true, key); break;
            case NON_SUBSCRIBER: key = "NonSubscriberPass"; value = accountValue(account, true, key); break;
            case REGRESSION: key = "testEmailPassRegression"; value = secret(new String[]{"TEST_EMAIL_PASS_REGRESSION"}, key); break;
            default: key = "testEmailPass"; value = secret(new String[]{"TEST_EMAIL_PASS"}, key);
        }
        if (value.isEmpty()) throw new IllegalArgumentException("Configure " + key + " for " + account);
        return value;
    }

    private static String accountValue(Account account, boolean password, String primaryKey) {
        String tail = password ? "Password" : "Email";
        String[] environment = {account.environmentSuffix + (password ? "_PASSWORD" : "_EMAIL"),
            primaryKey.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT),
            "STOCK_ANALYSTS_CHOICE_" + account.environmentSuffix + (password ? "_PASSWORD" : "_EMAIL"),
            "BATCH1_" + account.environmentSuffix + (password ? "_PASSWORD" : "_EMAIL")};
        return secret(environment, primaryKey, "batch1" + account.suffix + tail, "stockAnalystsChoice" + account.suffix + tail);
    }

    private static String secret(String[] environment, String... keys) {
        for (String key : environment) {
            String value = System.getenv(key);
            if (value != null && !value.isEmpty()) return value;
        }
        for (String key : keys) {
            String value = ConfigReader.getOptional(key, "");
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    /** Call before Logout. Matching authenticated sessions need no password/mailbox validation. */
    public static void validateLoginConfiguration(Account account) {
        validateLoginConfiguration(account, configuredLoginMethod());
    }

    public static void validateLoginConfiguration(Account account, LoginMethod method) {
        Objects.requireNonNull(method, "An explicit login method is required");
        configuredEmail(account);
        if (method == LoginMethod.PASSWORD) configuredPassword(account);
        else if (otpAutoFetchEnabled(account)) OtpRequest.validateConfiguration();
        else manualOtp();
    }

    private static boolean otpAutoFetchEnabled(Account account) {
        String value = configured("OTP_AUTO_FETCH_ENABLED", "otpAutoFetchEnabled", "otpAutoFetch", "autoFetchOtp");
        if (value.isEmpty()) return account != Account.DEFAULT && account != Account.REGRESSION;
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y");
    }

    private static String manualOtp() {
        String value = configured("MANUAL_OTP", "manualOtp", "otp");
        if (!value.matches("[0-9]{6}")) throw new IllegalArgumentException("Configure a six-digit manualOtp or enable OTP auto-fetch");
        return value;
    }

    private void loginWithOtp(Account account, By[] additional) {
        String email = configuredEmail(account);
        enterLoginEmail(email);
        if (otpAutoFetchEnabled(account)) {
            ReportLogger.step("Preparing the fresh OTP mailbox checkpoint before Next");
            try (OtpRequest request = OtpRequest.checkpoint()) {
                clickNextAndWait(additional);
                if (otpEntryVisible()) enterAndSubmitOtp(account, email, request.awaitOtp(email), additional);
            }
        } else {
            String code = manualOtp();
            clickNextAndWait(additional);
            if (otpEntryVisible()) enterAndSubmitOtp(account, email, code, additional);
        }
    }

    private void enterAndSubmitOtp(Account account, String email, String code, By[] additional) {
        if (account != Account.DEFAULT && account != Account.REGRESSION)
            waitUntil(() -> visible(AppiumBy.accessibilityId(email)), ExecutionProfile.defaultWait(), "OTP destination does not match the configured account");
        OtpPage page = new OtpPage(driver);
        page.waitForOtpScreen();
        page.enterOtp(code);
        submitOtpAndWait(additional);
    }

    private void loginWithPassword(Account account, By[] additional) {
        waitUntil(() -> isEmailLoginVisible() && visible(CONTINUE_WITH_PASSWORD) && loginInput(true) != null,
            ExecutionProfile.defaultWait(), "This APK did not expose the email/password form and Continue with Password CTA");
        String email = configuredEmail(account);
        String password = configuredPassword(account);
        enterLoginEmail(email);
        enterLoginCredential(true, password);
        submitPasswordForm(email, password);
        waitUntil(() -> {
            if (otpEntryVisible()) throw new IllegalStateException("PASSWORD login opened an OTP sheet; no automatic OTP fallback is allowed");
            return visible(BIOMETRIC_CANCEL) || isPinScreenVisible() || postLoginVisible(additional);
        }, ExecutionProfile.longWait(), "Continue with Password did not reach security/app navigation; verify the configured credentials");
    }

    private void clickNextAndWait(By[] additional) {
        waitUntil(this::isEmailLoginVisible, ExecutionProfile.defaultWait(), "Next requires the email login screen");
        ReportLogger.step("Waiting for and clicking Next on email login");
        tapNativeControl(NEXT);
        waitUntil(() -> otpEntryVisible() || visible(BIOMETRIC_CANCEL) || isPinScreenVisible() || postLoginVisible(additional),
            ExecutionProfile.longWait(), "Next did not reach OTP, security or app navigation");
    }

    private void submitOtpAndWait(By[] additional) {
        if (otpEntryVisible()) tapNativeControl(VERIFY_OTP);
        waitUntil(() -> !visible(OTP_TITLE) && (visible(BIOMETRIC_CANCEL) || isPinScreenVisible() || postLoginVisible(additional)),
            ExecutionProfile.longWait(), "OTP verification did not reach security/app navigation");
        ReportLogger.pass("OTP authentication completed");
    }

    public void finishSecurityAuthentication(Account account) { finishSecurityAuthentication(account, null); }

    private void finishSecurityAuthentication(Account account, By[] additional) {
        boolean[] pinEntered = {false};
        waitUntil(() -> {
            assertInfrastructureHealthy("during security authentication", 1500L);
            rejectIncorrectPin();
            if (visible(BIOMETRIC_CANCEL)) {
                dismissBiometricPrompt(additional);
                return false;
            }
            if (postLoginVisible(additional)) return true;
            if (!pinEntered[0] && pinKeypadVisible()) {
                String pin = configuredPin(account);
                ReportLogger.step("Entering the configured Advisor security PIN");
                for (char digit : pin.toCharArray()) {
                    WebElement key = waitFor(ExecutionProfile.shortWait()).until(d -> {
                        rejectIncorrectPin();
                        if (visible(BIOMETRIC_CANCEL)) {
                            tapNativeControl(BIOMETRIC_CANCEL);
                            return null;
                        }
                        WebElement candidate = pinKeypadVisible() ? firstVisible(AppiumBy.accessibilityId(String.valueOf(digit))) : null;
                        return candidate != null && candidate.isEnabled() ? candidate : null;
                    });
                    key.click(); // Submit once; never blindly re-enter an accepted PIN digit.
                }
                pinEntered[0] = true;
            }
            return false;
        }, ExecutionProfile.longWait(), "Security authentication did not reach valid app navigation");
    }

    private void dismissBiometricPrompt(By[] additional) {
        String action = waitFor(ExecutionProfile.defaultWait()).withMessage(
                "Biometric prompt did not expose Cancel or transition to the Advisor PIN keypad").until(d -> {
            rejectIncorrectPin();
            WebElement cancel = firstVisible(BIOMETRIC_CANCEL);
            if (cancel == null) {
                // The system can remove the prompt between detecting it and
                // resolving its button, especially after a fresh driver session.
                return pinKeypadVisible() || postLoginVisible(additional) ? "ALREADY_CLOSED" : null;
            }
            if (!cancel.isEnabled() || !fullyOnScreen(cancel.getRect())) return null;
            driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) cancel).getId()));
            return "CANCELLED";
        });
        waitUntil(() -> !visible(BIOMETRIC_CANCEL), ExecutionProfile.defaultWait(), "Biometric prompt did not close");
        ReportLogger.step(action.equals("CANCELLED") ? "Cancelled the biometric prompt before PIN entry"
                : "Biometric prompt already closed; continuing Advisor PIN authentication");
    }

    private static String configuredPin(Account account) {
        String value = account == null
            ? configured("STOCK_ANALYSTS_CHOICE_EXISTING_SESSION_PIN", "stockAnalystsChoiceExistingSessionPin", "batch1ExistingSessionPin")
            : configured("STOCK_ANALYSTS_CHOICE_" + account.environmentSuffix + "_PIN", "stockAnalystsChoice" + account.suffix + "Pin", "batch1" + account.suffix + "Pin");
        if (value.isEmpty() && account == null) value = configured("BATCH1_EXISTING_SESSION_PIN");
        if (value.isEmpty() && account != null) value = configured("BATCH1_" + account.environmentSuffix + "_PIN");
        if (value.isEmpty()) value = configured("APP_PIN", "appPin", "pin", "advisorPin");
        if (!value.matches("[0-9]{4,6}")) throw new IllegalArgumentException("Configure a valid Advisor appPin or account PIN");
        return value;
    }

    private void rejectIncorrectPin() {
        if (visible(PIN_ERROR) || visible(PIN_ERROR_TEXT)) throw new IllegalStateException("Advisor rejected the configured PIN");
    }

    private boolean pinKeypadVisible() {
        return (visible(USE_BIOMETRICS) || visible(PIN_DELETE)) && visible(AppiumBy.accessibilityId("1")) && visible(AppiumBy.accessibilityId("0"));
    }

    public boolean isPinScreenVisible() {
        for (By marker : PIN_MARKERS) if (visible(marker)) return true;
        return pinKeypadVisible();
    }

    public boolean isEmailLoginVisible() {
        return visible(LOGIN_TITLE) && (visible(NEXT) || visible(CONTINUE_WITH_PASSWORD)) && visible(INPUT)
            && !visible(OTP_TITLE) && !visible(HUB);
    }

    private boolean emailLoginVisible() { return isEmailLoginVisible(); }

    private boolean otpEntryVisible() { return visible(OTP_TITLE) && visible(VERIFY_OTP) && visible(INPUT); }

    private boolean postLoginVisible(By[] additional) {
        if (visible(BIOMETRIC_CANCEL) || isPinScreenVisible() || visible(OTP_TITLE) || isEmailLoginVisible()) return false;
        for (By tab : TABS) if (visible(tab)) return true;
        if (additional != null) for (By marker : additional) if (visible(marker)) return true;
        return false;
    }
    private void enterLoginEmail(String email) {
        if (visible(CONTINUE_WITH_PASSWORD)) {
            // The shared legacy method presses ENTER to dismiss the keyboard.
            // On this two-field form, explicitly hide it to avoid submitting early.
            enterLoginCredential(false, email);
        } else {
            new LoginPage(driver).enterEmail(email);
        }
        waitUntil(() -> emailLoginVisible() && uniqueInputMatches(email), ExecutionProfile.defaultWait(),
                "Login email field does not match the configured account");
    }

    private void enterLoginCredential(boolean password, String value) {
        String label = password ? "password" : "email";
        ReportLogger.step("Entering the configured login " + label);
        int attempts = positiveInt("authClickAttempts", 3);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                focusLoginInput(password).clear();
                // Flutter can replace a field after clear or after changing focus.
                focusLoginInput(password).sendKeys(value);
                waitUntil(() -> credentialRetained(password, value), ExecutionProfile.shortWait(),
                        "Login " + label + " field did not retain the configured value");
                hideLoginKeyboard();
                waitUntil(() -> credentialRetained(password, value), ExecutionProfile.shortWait(),
                        "Login " + label + " field became empty or changed after closing the keyboard");
                ReportLogger.pass("Configured login " + label + " entry verified");
                return;
            } catch (TimeoutException | StaleElementReferenceException | ElementNotInteractableException failure) {
                if (attempt == attempts) {
                    // Do not attach sendKeys command details, which can contain the secret.
                    throw new IllegalStateException("Could not focus and verify the login " + label
                            + " field after " + attempts + " attempts; login was not submitted");
                }
            }
        }
    }

    private WebElement focusLoginInput(boolean password) {
        return waitFor(ExecutionProfile.shortWait()).withMessage("Login input did not acquire focus").until(d -> {
            if (!isEmailLoginVisible()) return null;
            WebElement input = loginInput(password);
            if (input == null || !fullyOnScreen(input.getRect())) return null;
            if (!"true".equalsIgnoreCase(input.getAttribute("focused"))) {
                tapCenter(input.getRect());
                input = loginInput(password);
            }
            return input != null && "true".equalsIgnoreCase(input.getAttribute("focused")) ? input : null;
        });
    }

    private void hideLoginKeyboard() {
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
            waitUntil(() -> !driver.isKeyboardShown(), ExecutionProfile.shortWait(),
                    "Login keyboard did not close after credential entry");
        }
    }

    private boolean credentialRetained(boolean password, String expected) {
        WebElement input = loginInput(password);
        if (input == null) return false;
        String actual = input.getText();
        if (actual == null || actual.isEmpty()) return false;
        if (!password) return expected.equalsIgnoreCase(actual.trim());
        if (expected.equals(actual)) return true;
        // Secure fields expose bullets instead of plaintext. Require the full length;
        // placeholders and an empty field are never accepted as successful entry.
        return actual.matches("[\\u2022\\u25cf\\u2219*]+")
                && actual.codePointCount(0, actual.length()) == expected.codePointCount(0, expected.length());
    }

    private void submitPasswordForm(String email, String password) {
        hideLoginKeyboard();
        List<Rectangle> previous = new ArrayList<>();
        Rectangle bounds = waitFor(ExecutionProfile.defaultWait()).withMessage(
                "Password form was not ready: both credentials and the exact Continue with Password CTA are required")
                .until(d -> {
                    if (!isEmailLoginVisible() || driver.isKeyboardShown()
                            || !credentialRetained(false, email) || !credentialRetained(true, password)) {
                        previous.clear();
                        return null;
                    }
                    WebElement submit = null;
                    for (WebElement candidate : driver.findElements(CONTINUE_WITH_PASSWORD)) {
                        if (!candidate.isDisplayed()) continue;
                        if (submit != null) throw new IllegalStateException("Multiple Continue with Password controls are visible");
                        submit = candidate;
                    }
                    if (submit == null || !submit.isEnabled()
                            || !"Continue with Password".equals(submit.getAttribute("content-desc"))) return null;
                    Rectangle button = submit.getRect();
                    Rectangle input = loginInput(true).getRect();
                    WebElement next = firstVisible(NEXT);
                    Rectangle nextBounds = next == null ? null : next.getRect();
                    if (!fullyOnScreen(button) || input.getY() + input.getHeight() > button.getY()
                            || (nextBounds != null && button.getY() + button.getHeight() > nextBounds.getY())) {
                        previous.clear();
                        return null;
                    }
                    // Wait for the layout to settle after keyboard dismissal, then use
                    // the exact CTA's live center rather than a cached native element id.
                    List<Rectangle> current = Arrays.asList(loginInput(false).getRect(), input, button, nextBounds);
                    boolean stable = previous.equals(current);
                    previous.clear();
                    previous.addAll(current);
                    return stable ? button : null;
                });
        ReportLogger.step("Submitting verified credentials using Continue with Password");
        tapCenter(bounds); // Submit once; this path never invokes Next or an IME action.
    }

    private void tapCenter(Rectangle bounds) {
        driver.executeScript("mobile: clickGesture", Map.of(
                "x", bounds.getX() + bounds.getWidth() / 2,
                "y", bounds.getY() + bounds.getHeight() / 2));
    }

    private boolean uniqueInputMatches(String email) {
        WebElement input = loginInput(false);
        return input != null && email.equalsIgnoreCase(input.getText().trim());
    }

    /** Supplied XML has unlabeled fields: email above password, both above the password CTA. */
    private WebElement loginInput(boolean password) {
        List<WebElement> inputs = new ArrayList<>();
        for (WebElement input : driver.findElements(INPUT)) {
            if (input.isDisplayed() && input.isEnabled()) {
                inputs.add(input);
            }
        }
        if (!driver.findElements(CONTINUE_WITH_PASSWORD).isEmpty()) {
            if (inputs.size() < 2) {
                return null; // Wait for both supplied fields, rather than typing into a partial render.
            }
            if (inputs.size() != 2) {
                throw new IllegalStateException("Expected exactly two login fields above Continue with Password");
            }
            // Geometry, not UiSelector.instance or XPath position, identifies these unlabeled fields.
            inputs.sort(Comparator.comparingInt(input -> input.getRect().getY()));
            Rectangle emailBounds = inputs.get(0).getRect();
            Rectangle passwordBounds = inputs.get(1).getRect();
            if (emailBounds.getWidth() <= 0 || passwordBounds.getWidth() <= 0
                    || emailBounds.getY() + emailBounds.getHeight() > passwordBounds.getY()) {
                throw new IllegalStateException("Email and password field geometry is ambiguous");
            }
            WebElement submit = firstVisible(CONTINUE_WITH_PASSWORD);
            if (submit != null && !driver.isKeyboardShown()
                    && passwordBounds.getY() + passwordBounds.getHeight() > submit.getRect().getY()) {
                throw new IllegalStateException("The login fields are not above Continue with Password");
            }
            return inputs.get(password ? 1 : 0);
        }
        if (password) {
            return null;
        }
        if (inputs.size() > 1) {
            throw new IllegalStateException("Multiple visible login inputs; refusing to guess");
        }
        return inputs.size() == 1 ? inputs.get(0) : null;
    }

    private void tapNativeControl(By locator) {
        // The captured Flutter Go back may expose clickable=false. Use a real
        // gesture on fresh element bounds. The same approach is used for tabs/cells.
        for (int attempt = 1; attempt <= positiveInt("authClickAttempts", 3); attempt++) {
            try {
                WebElement back = waitFor(ExecutionProfile.shortWait()).until(d -> {
                    WebElement candidate = firstVisible(locator);
                    return candidate != null && candidate.isEnabled() && fullyOnScreen(candidate.getRect())
                            ? candidate : null;
                });
                driver.executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) back).getId()));
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == positiveInt("authClickAttempts", 3)) {
                    throw e;
                }
            }
        }
    }

    private void assertInfrastructureHealthy(String context, long minimumIntervalMs) {
        long now = System.currentTimeMillis();

        if (lastInfrastructureHealthCheckAtMs > 0L
                && (now - lastInfrastructureHealthCheckAtMs) < minimumIntervalMs) {
            return;
        }

        lastInfrastructureHealthCheckAtMs = now;

        if (!DriverManager.isDriverHealthy()) {
            throw new RuntimeException(
                    "Infrastructure failure: Android device/Appium/UiAutomator2 session became unhealthy "
                            + context
            );
        }
    }


    private WebElement firstVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) if (element.isDisplayed()) return element;
        return null;
    }
    private boolean visible(By locator) {
        try { return firstVisible(locator) != null; } catch (StaleElementReferenceException stale) { return false; }
    }
    private boolean fullyOnScreen(Rectangle r) {
        Dimension size = driver.manage().window().getSize();
        return r.getWidth() > 0 && r.getHeight() > 0 && r.getX() >= 0 && r.getY() >= 0
            && r.getX() + r.getWidth() <= size.getWidth() && r.getY() + r.getHeight() <= size.getHeight();
    }
    private void nativeContext() { if (!"NATIVE_APP".equals(driver.getContext())) driver.context("NATIVE_APP"); }
    private WebDriverWait waitFor(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout, ExecutionProfile.pollingInterval());
        wait.ignoring(NoSuchElementException.class); wait.ignoring(StaleElementReferenceException.class); return wait;
    }
    private void waitUntil(BooleanSupplier condition, Duration timeout, String message) {
        waitFor(timeout).withMessage(message).until(d -> condition.getAsBoolean());
    }
    private static String normalize(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }
    private static String configured(String environment, String... keys) {
        String value = System.getenv(environment);
        if (value != null && !value.trim().isEmpty()) return value.trim();
        for (String key : keys) {
            value = ConfigReader.getOptional(key, "");
            if (!value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
    private static int positiveInt(String key, int fallback) {
        String sharedKey = key.replace("stockAnalystsChoice", "auth");
        int value = Integer.parseInt(ConfigReader.getOptional(sharedKey,
            ConfigReader.getOptional(key.replace("stockAnalystsChoice", "batch1"), ConfigReader.getOptional(key, String.valueOf(fallback)))).trim());
        if (value <= 0) throw new IllegalArgumentException(sharedKey + " must be positive");
        return value;
    }
    private static final class OtpRequest implements AutoCloseable {
        private final Batch1OtpApi sharedReader;
        private final Object marker;
        private final FreshOtpMailbox legacyMailbox;

        private OtpRequest(Batch1OtpApi sharedReader, Object marker, FreshOtpMailbox legacyMailbox) {
            this.sharedReader = sharedReader;
            this.marker = marker;
            this.legacyMailbox = legacyMailbox;
        }

        private static void validateConfiguration() {
            Batch1OtpApi reader = Batch1OtpApi.discover(OtpEmailReader.class);
            if (reader != null) {
                reader.invoke(reader.validate, "Batch 1 OTP configuration validation");
            } else {
                FreshOtpMailbox.validateConfiguration();
            }
        }

        private static OtpRequest checkpoint() {
            Batch1OtpApi reader = Batch1OtpApi.discover(OtpEmailReader.class);
            if (reader != null) {
                ReportLogger.step("Using Batch 1 OtpEmailReader for the fresh login request");
                Object marker = reader.invoke(reader.capture, "Batch 1 fresh OTP checkpoint before Next");
                if (marker == null) {
                    throw new IllegalStateException("Batch 1 OTP checkpoint returned no request marker; Next was not clicked");
                }
                ReportLogger.pass("Batch 1 fresh OTP checkpoint captured before Next");
                return new OtpRequest(reader, marker, null);
            }
            ReportLogger.step("Legacy OtpEmailReader detected; using the compatible fresh-OTP mailbox checkpoint");
            return new OtpRequest(null, null, FreshOtpMailbox.checkpoint());
        }

        private String awaitOtp(String email) {
            if (legacyMailbox != null) {
                return legacyMailbox.awaitOtp(email);
            }
            Object result = sharedReader.invoke(sharedReader.fetch, "Batch 1 fresh OTP retrieval", marker, email);
            if (!(result instanceof String) || !((String) result).matches("[0-9]{6}")) {
                throw new IllegalStateException("Batch 1 OTP reader did not return a valid six-digit code");
            }
            return (String) result;
        }

        @Override
        public void close() {
            if (legacyMailbox != null) {
                legacyMailbox.close();
            }
        }
    }

    private static final class Batch1OtpApi {
        private final Method validate;
        private final Method capture;
        private final Method fetch;

        private Batch1OtpApi(Method validate, Method capture, Method fetch) {
            this.validate = validate;
            this.capture = capture;
            this.fetch = fetch;
        }

        private static Batch1OtpApi discover(Class<?> reader) {
            // An old reader has none of this API. A partially updated reader must
            // fail clearly rather than silently choose a different mailbox flow.
            boolean modern = false;
            for (Method method : reader.getMethods()) {
                String name = method.getName();
                modern |= name.equals("validateBatch1Configuration")
                        || name.equals("captureLoginRequestMarker") || name.equals("fetchLatestOtpAfter");
            }
            if (!modern) {
                return null;
            }
            try {
                Method validate = reader.getMethod("validateBatch1Configuration");
                Method capture = reader.getMethod("captureLoginRequestMarker");
                Method fetch = reader.getMethod("fetchLatestOtpAfter", capture.getReturnType(), String.class);
                return new Batch1OtpApi(validate, capture, fetch);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("OtpEmailReader has an incomplete Batch 1 fresh-login API", e);
            }
        }

        private Object invoke(Method method, String stage, Object... arguments) {
            try {
                return method.invoke(null, arguments);
            } catch (InvocationTargetException e) {
                // Preserve the underlying cause for the listener/report. Do not
                // print credentials or codes, reconnect with guessed settings,
                // or click Next after a failed checkpoint.
                throw new IllegalStateException(stage + " failed", e.getCause());
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                throw new IllegalStateException(stage + " could not call the shared OTP reader", e);
            }
        }
    }

    private static final class FreshOtpMailbox implements AutoCloseable {
        private final Store store;
        private final Folder inbox;
        private final UIDFolder uidFolder;
        private final long validity;
        private final long lastUid;

        private FreshOtpMailbox(Store store, Folder inbox) throws MessagingException {
            this.store = store;
            this.inbox = inbox;
            if (!(inbox instanceof UIDFolder)) {
                throw new IllegalStateException("OTP mailbox must support IMAP message UIDs");
            }
            uidFolder = (UIDFolder) inbox;
            validity = uidFolder.getUIDValidity();
            int count = inbox.getMessageCount();
            lastUid = count == 0 ? 0 : uidFolder.getUID(inbox.getMessage(count));
            if (lastUid < 0 || validity <= 0) {
                throw new IllegalStateException("Cannot establish a reliable fresh-OTP mailbox checkpoint");
            }
        }

        private static void validateConfiguration() {
            mailboxEmail();
            mailboxPassword();
            mailboxHost();
        }

        private static FreshOtpMailbox checkpoint() {
            Store openedStore = null;
            Folder openedInbox = null;
            try {
                String host = mailboxHost();
                Properties props = new Properties();
                props.setProperty("mail.imaps.ssl.enable", "true");
                props.setProperty("mail.imaps.ssl.checkserveridentity", "true");
                props.setProperty("mail.imaps.connectiontimeout", "10000");
                props.setProperty("mail.imaps.timeout", "10000");
                props.setProperty("mail.imaps.writetimeout", "10000");
                Session session = Session.getInstance(props);
                session.setDebug(false);
                openedStore = session.getStore("imaps");
                openedStore.connect(host, mailboxEmail(), mailboxPassword());
                openedInbox = openedStore.getFolder("INBOX");
                openedInbox.open(Folder.READ_ONLY);
                FreshOtpMailbox mailbox = new FreshOtpMailbox(openedStore, openedInbox);
                ReportLogger.step("Fresh OTP mailbox checkpoint captured before requesting Next");
                return mailbox;
            } catch (MessagingException | RuntimeException e) {
                closeResources(openedInbox, openedStore);
                throw new IllegalStateException("Could not establish the fresh OTP mailbox checkpoint", e);
            }
        }

        private String awaitOtp(String recipient) {
            return new FluentWait<>(this)
                    .withTimeout(Duration.ofSeconds(positiveInt("stockAnalystsChoiceOtpTimeoutSeconds", 90)))
                    .pollingEvery(Duration.ofMillis(positiveInt("stockAnalystsChoiceOtpPollingMillis", 2000)))
                    .withMessage("No fresh OTP for the configured account; check mailbox delivery and recipient headers")
                    .until(mailbox -> mailbox.readFreshOtp(recipient));
        }

        private String readFreshOtp(String recipient) {
            try {
                inbox.getMessageCount(); // Refresh the open IMAP folder; no stale cached latest-message selection.
                if (uidFolder.getUIDValidity() != validity) {
                    throw new IllegalStateException("OTP mailbox UID validity changed; refusing stale messages");
                }
                Message[] messages = uidFolder.getMessagesByUID(lastUid + 1, UIDFolder.MAXUID);
                String keyword = configured("OTP_EMAIL_SUBJECT_KEYWORD", "otpEmailSubjectKeyword", "OtpEmailSubjectKeyword");
                if (keyword.isEmpty()) {
                    keyword = "otp";
                }
                for (int index = messages.length - 1; index >= 0; index--) {
                    Message message = messages[index];
                    if (message == null || message.isExpunged() || uidFolder.getUID(message) <= lastUid
                            || !hasRecipient(message, recipient)) {
                        continue;
                    }
                    String subject = normalize(message.getSubject()).toLowerCase(Locale.ROOT);
                    if (!subject.contains(keyword.toLowerCase(Locale.ROOT)) && !subject.contains("otp")
                            && !subject.contains("one time password") && !subject.contains("verification code")) {
                        continue;
                    }
                    String body = text(message, 0);
                    Matcher labelled = Pattern.compile("(?i)(?:otp|verification code|one[- ]time(?: password| code)?)"
                            + "[^0-9]{0,100}([0-9]{6})(?![0-9])").matcher(body);
                    Set<String> codes = new LinkedHashSet<>();
                    while (labelled.find()) {
                        codes.add(labelled.group(1));
                    }
                    if (codes.isEmpty()) {
                        Matcher plain = Pattern.compile("(?<![0-9])[0-9]{6}(?![0-9])").matcher(body);
                        while (plain.find()) {
                            codes.add(plain.group());
                        }
                    }
                    if (codes.size() > 1) {
                        throw new IllegalStateException("Fresh OTP email contains ambiguous codes; refusing to guess");
                    }
                    if (codes.size() == 1) {
                        ReportLogger.pass("Fresh OTP received for the requested account");
                        return codes.iterator().next();
                    }
                }
                return null;
            } catch (Exception e) {
                throw new IllegalStateException("Fresh OTP mailbox processing failed", e);
            }
        }

        private static boolean hasRecipient(Message message, String recipient) throws MessagingException {
            Address[] addresses = message.getAllRecipients();
            if (addresses != null) {
                for (Address address : addresses) {
                    if (address instanceof InternetAddress
                            && recipient.equalsIgnoreCase(((InternetAddress) address).getAddress())) {
                        return true;
                    }
                }
            }
            Pattern exactAddress = Pattern.compile("(?i)(?<![a-z0-9.!#$%&'*+/=?^_`{|}~-])"
                    + Pattern.quote(recipient) + "(?![a-z0-9.-])");
            for (String header : new String[]{"X-Original-To", "Delivered-To", "Envelope-To"}) {
                String[] values = message.getHeader(header);
                if (values != null) {
                    for (String value : values) {
                        if (exactAddress.matcher(value).find()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private static String text(Part part, int depth) throws Exception {
            if (depth > 8 || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                return "";
            }
            if (part.isMimeType("text/plain")) {
                return String.valueOf(part.getContent());
            }
            if (part.isMimeType("text/html")) {
                return Jsoup.parse(String.valueOf(part.getContent())).text();
            }
            if (part.isMimeType("multipart/*")) {
                Multipart parts = (Multipart) part.getContent();
                StringBuilder result = new StringBuilder();
                for (int index = 0; index < parts.getCount(); index++) {
                    result.append(text(parts.getBodyPart(index), depth + 1)).append('\n');
                }
                return result.toString();
            }
            if (part.isMimeType("message/rfc822")) {
                return text((Part) part.getContent(), depth + 1);
            }
            return "";
        }

        private static String mailboxEmail() {
            String email = configured("OTP_EMAIL", "otpEmail", "OtpEmail");
            if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                throw new IllegalArgumentException("Configure OTP_EMAIL or otpEmail for account switching");
            }
            return email;
        }

        private static String mailboxPassword() {
            String password = configured("OTP_EMAIL_APP_PASSWORD", "otpEmailAppPassword", "OtpEmailAppPassword").replace(" ", "");
            if (password.isEmpty()) {
                throw new IllegalArgumentException("Configure OTP_EMAIL_APP_PASSWORD or otpEmailAppPassword");
            }
            return password;
        }

        private static String mailboxHost() {
            String override = configured("OTP_IMAP_HOST", "otpImapHost");
            if (!override.isEmpty()) {
                return override;
            }
            String email = mailboxEmail().toLowerCase(Locale.ROOT);
            if (email.endsWith("@gmail.com") || email.endsWith("@valueresearch.in")
                    || email.endsWith("@valueresearchonline.com")) {
                return "imap.gmail.com";
            }
            if (email.endsWith("@outlook.com") || email.endsWith("@hotmail.com") || email.endsWith("@live.com")) {
                return "outlook.office365.com";
            }
            throw new IllegalArgumentException("Configure OTP_IMAP_HOST or otpImapHost for this mailbox provider");
        }

        @Override
        public void close() {
            closeResources(inbox, store);
        }

        private static void closeResources(Folder folder, Store store) {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
            } catch (MessagingException e) {
                ReportLogger.debug("OTP mailbox close failed: " + e.getClass().getSimpleName());
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                ReportLogger.debug("OTP connection close failed: " + e.getClass().getSimpleName());
            }
        }
    }
}
