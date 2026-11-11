package pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * OTP Verification Bottom Sheet Page Object.
 *
 * Appears after:
 * credential entry -> Next
 *
 * React Native app:
 * - no stable resource IDs
 * - content-desc driven
 * - direct locator interactions preferred
 *
 * OTP can be fetched from:
 * - email
 * - SMS
 * - API
 * - mock service
 */
public class OtpPage extends BasePage {

    private final By otpHeading =
            AppiumBy.accessibilityId("Enter OTP");

    private final By otpInputField =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".className(\"android.widget.EditText\")"
                            + ".instance(0)");

    // Some OTP screens (e.g. the Finvu AA verification screen) split entry into
    // one single-digit EditText box per character instead of a single field —
    // this matches all of them so digits can be distributed one-per-box.
    private final By otpInputBoxes =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.EditText\")");

    private final By verifyOtpButtonLocator =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".description(\"Verify OTP\")");

    private final By changeEmailPhoneLocator =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".description(\"Change Email / Phone\")");

    private final By resendOtpLocator =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".description(\"Resend OTP\")");

    // Finvu AA "too many attempts" state confirmed via XML: the error banner
    // is a WebView-bridged node with resource-id="otp-error-desc" but NO
    // content-desc (only @text is populated), so accessibilityId() never
    // matches it — textContains() on the real message is required instead.
    private final By attemptsExceededFallback1 = AppiumBy.androidUIAutomator(
            "new UiSelector().textContains(\"Too many OTP requests\")");
    private final By attemptsExceededFallback2 = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Too many attempts\")");
    private final By attemptsExceededFallback3 = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"maximum\")");
    private final By attemptsExceededFallback4 = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"try again after\")");
    private final By invalidOtpFallback = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Invalid OTP\")");

    // ============================================================

    public OtpPage(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // SCREEN VALIDATION
    // ============================================================

    /**
     * Confirms OTP bottom sheet visibility.
     */
    public boolean isOtpScreenDisplayed() {
        return isDisplayed(otpHeading, 3);
    }

    /**
     * Waits until OTP screen fully stabilizes.
     */
    public OtpPage waitForOtpScreenLoaded() {
        waitForVisible(otpHeading, 3);
        waitForVisible(otpInputField, 3);
        return this;
    }

    // ============================================================
    // OTP ACTIONS
    // ============================================================

    /**
     * Enters OTP safely.
     */
    public OtpPage enterOtp(String otp) {
        List<WebElement> boxes = findElements(otpInputBoxes);

        if (boxes.size() > 1) {
            for (int i = 0; i < boxes.size() && i < otp.length(); i++) {
                WebElement box = boxes.get(i);
                box.click();
                box.clear();
                box.sendKeys(String.valueOf(otp.charAt(i)));
            }
            hideKeyboardIfVisible();
        } else {
            safeSendKeys(otpInputField, otp);
        }

        return this;
    }

    /**
     * Taps Verify OTP CTA safely.
     */
    public OtpPage tapVerifyOtp() {
        safeClick(verifyOtpButtonLocator);
        return this;
    }

    /**
     * Full OTP verification flow.
     */
    public OtpPage verifyOtp(String otp) {
        enterOtp(otp);
        tapVerifyOtp();
        return this;
    }

    // ============================================================
    // SECONDARY ACTIONS
    // ============================================================

    /**
     * Navigates back to credential screen.
     */
    public OtpPage tapChangeEmailPhone() {
        safeClick(changeEmailPhoneLocator);
        return this;
    }

    /**
     * Requests a new OTP.
     */
    public OtpPage tapResendOtp() {
        safeClick(resendOtpLocator);
        return this;
    }

    // ============================================================
    // VALIDATION HELPERS
    // ============================================================
    public boolean isVerifyButtonDisplayed() {

        return isDisplayed(verifyOtpButtonLocator);
    }

    public boolean isResendOtpDisplayed() {

        return isDisplayed(resendOtpLocator);
    }

    public boolean isChangeEmailPhoneDisplayed() {

        return isDisplayed(changeEmailPhoneLocator);
    }

    public boolean isOtpInputDisplayed() {

        return isDisplayed(otpInputField);
    }

    // ============================================================
    // NEGATIVE VALIDATION
    // ============================================================

    /**
     * Fast negative validation.
     */
    public boolean isOtpScreenNotDisplayed() {
        return !isOtpScreenDisplayed();
    }

    /**
     * Best-effort check for a "maximum attempts exceeded" state after
     * repeated wrong-OTP submissions. See field comments above — tighten
     * once the real screen's XML has been captured.
     */
    public boolean isOtpAttemptsExceededDisplayed() {
        return isAnyDisplayed(attemptsExceededFallback1, attemptsExceededFallback2,
                attemptsExceededFallback3, attemptsExceededFallback4);
    }

    public boolean isInvalidOtpMessageDisplayed() {
        return isDisplayed(invalidOtpFallback);
    }
}
