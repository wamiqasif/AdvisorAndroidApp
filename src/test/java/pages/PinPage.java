package pages;

import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class PinPage extends BasePage {

    private final By pinHeadingLocator =
            AppiumBy.accessibilityId("Enter your Advisor PIN");

    private final By userGreetingLocator =
            AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Hi,\")");

    private final By incorrectPinLocator =
            AppiumBy.accessibilityId("Incorrect pin.");

    private final By changePinLocator =
            AppiumBy.accessibilityId("Change pin");

    // Fallback retained because the app does not expose a stable identifier here.
    private final By backspaceLocator =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".className(\"android.widget.ImageView\")"
                            + ".clickable(true)"
                            + ".instance(1)");

    public PinPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isPinScreenDisplayed() {
        return isDisplayed(pinHeadingLocator, 3);
    }

    public boolean isPinScreenDisplayedFast() {
        return isDisplayed(pinHeadingLocator);
    }

    public PinPage waitForPinScreenLoaded() {
        waitForVisible(pinHeadingLocator, 3);
        return this;
    }

    public boolean isGreetingDisplayed() {
        return isDisplayed(userGreetingLocator);
    }

    public boolean isIncorrectPinErrorDisplayed() {
        return isDisplayed(incorrectPinLocator);
    }

    public boolean isChangePinLinkDisplayed() {
        return isDisplayed(changePinLocator);
    }

    public boolean isBackspaceDisplayed() {
        return isDisplayed(backspaceLocator);
    }

    public boolean isDigitDisplayed(String digit) {
        return isDisplayed(pinDigitLocator(digit));
    }

    public PinPage tapDigit(String digit) {
        safeClick(pinDigitLocator(digit));
        return this;
    }

    public PinPage tapBackspace() {
        safeClick(backspaceLocator);
        return this;
    }

    /**
     * Enters full PIN sequentially.
     *
     * Auto-submit happens automatically
     * after last digit.
     */
    public PinPage enterPin(String pin) {
        logger.info("Entering PIN ({} digits)", pin.length());
        for (char digit : pin.toCharArray()) {
            tapDigit(String.valueOf(digit));
        }
        logger.info("PIN entry complete — waiting for auto-submit");
        return this;
    }

    /**
     * Clears entered PIN manually.
     */
    public PinPage clearPinField(int digitCount) {
        for (int i = 0; i < digitCount; i++) {
            tapBackspace();
        }
        return this;
    }

    /**
     * Taps Change PIN CTA.
     */
    public PinPage tapChangePin() {
        logger.info("Tapping Change PIN link");
        safeClick(changePinLocator);
        logger.info("Change PIN tapped");
        return this;
    }

    // ============================================================
    // NEGATIVE VALIDATION
    // ============================================================

    /**
     * Fast negative validation.
     */
    public boolean isPinScreenNotDisplayed() {
        return !isPinScreenDisplayedFast();
    }

    private By pinDigitLocator(String digit) {
        return AppiumBy.accessibilityId(digit);
    }
}
