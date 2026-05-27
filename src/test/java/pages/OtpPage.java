package pages;

import org.openqa.selenium.By;

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
        safeSendKeys(otpInputField, otp);
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
}
