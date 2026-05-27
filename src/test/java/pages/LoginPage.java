package pages;

import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class LoginPage extends BasePage {

    private final By loginLabel =
            AppiumBy.accessibilityId("Log in");

    private final By credentialField =
            AppiumBy.className("android.widget.EditText");

    private final By nextButtonLocator =
            AppiumBy.accessibilityId("Next");

    public LoginPage(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // LOGIN ACTIONS
    // ============================================================

    /**
     * Enters mobile/email credential safely.
     *
     * Uses direct locator interaction to avoid:
     * - stale proxy references
     * - RN re-render failures
     * - proxy caching issues
     */
    public LoginPage enterCredential(String credential) {
        safeSendKeys(credentialField, credential);
        return this;
    }

    /**
     * Taps Next CTA safely.
     */
    public LoginPage tapNext() {
        safeClick(nextButtonLocator);
        return this;
    }

    /**
     * Full login action.
     */
    public LoginPage loginWith(String credential) {
        enterCredential(credential);
        tapNext();
        return this;
    }

    // ============================================================
    // SCREEN VALIDATION
    // ============================================================

    /**
     * Confirms Login screen visibility.
     */
    public boolean isLoginScreenDisplayed() {
        return isDisplayed(loginLabel, 3);
    }

    /**
     * Validates Next button visibility.
     */
    public boolean isNextButtonDisplayed() {
        return isDisplayed(nextButtonLocator);
    }

    // ============================================================
    // EXTRA VALIDATION HELPERS
    // ============================================================

    /**
     * Confirms credential field visibility.
     */
    public boolean isCredentialFieldDisplayed() {
        return isDisplayed(credentialField);
    }

    /**
     * Waits until login page fully stabilizes.
     */
    public LoginPage waitForLoginPageLoaded() {
        waitForVisible(loginLabel, 3);
        waitForVisible(credentialField, 3);
        return this;
    }

    // ============================================================
    // NEGATIVE VALIDATION
    // ============================================================

    /**
     * Fast negative validation.
     */
    public boolean isLoginScreenNotDisplayed() {
        return !isLoginScreenDisplayed();
    }
}
