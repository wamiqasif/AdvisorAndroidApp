package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

public class OtpPage {

    private final AndroidDriver driver;

    private final By otpField = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(0)"
    );

    private final By[] verifyButtonLocators = new By[]{
            AppiumBy.accessibilityId("Verify OTP"),
            AppiumBy.accessibilityId("Verify"),
            AppiumBy.accessibilityId("Continue"),
            AppiumBy.accessibilityId("Submit"),

            AppiumBy.androidUIAutomator("new UiSelector().description(\"Verify OTP\")"),
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Verify\")"),
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Continue\")"),
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Submit\")"),

            AppiumBy.androidUIAutomator("new UiSelector().text(\"Verify OTP\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Verify\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Continue\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Submit\")")
    };

    public OtpPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public void waitForOtpScreen() {
        try {
            ReportLogger.step("Waiting for OTP screen");

            WaitUtils.waitForElement(driver, otpField, 30);

            ReportLogger.pass("OTP screen loaded successfully");

        } catch (Exception e) {
            ReportLogger.fail("OTP screen did not load: " + e.getMessage());
            throw new RuntimeException("OTP screen did not load: " + e.getMessage(), e);
        }
    }

    public void enterOtp(String otp) {
        try {
            if (otp == null || !otp.matches("\\d{6}")) {
                ReportLogger.fail("Invalid OTP. Expected 6 digits.");
                throw new RuntimeException("Invalid OTP. Expected 6 digits, got: " + otp);
            }

            ReportLogger.step("Entering OTP");

            WaitUtils.waitForElement(driver, otpField, 30);

            WebElement otpInput = driver.findElement(otpField);
            otpInput.click();

            sleep(500);

            for (char digit : otp.toCharArray()) {
                driver.pressKey(new KeyEvent(getAndroidKey(digit)));
                sleep(250);
            }

            ReportLogger.pass("OTP entered successfully");

        } catch (RuntimeException e) {
            ReportLogger.fail("Failed to enter OTP: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ReportLogger.fail("Failed to enter OTP: " + e.getMessage());
            throw new RuntimeException("Failed to enter OTP: " + e.getMessage(), e);
        }
    }

    public void clickVerifyIfVisible() {
        try {
            ReportLogger.step("Checking Verify OTP button");

            sleep(1500);

            WebElement verifyButton = findVerifyButton();

            if (verifyButton == null) {
                ReportLogger.step("Verify OTP button not found. App may auto-verify OTP.");
                return;
            }

            if (verifyButton.isDisplayed() && verifyButton.isEnabled()) {
                verifyButton.click();
                ReportLogger.pass("Verify OTP button clicked");
                sleep(4000);
                return;
            }

            ReportLogger.step("Verify OTP button found but not clickable. Skipping click.");

        } catch (Exception e) {
            ReportLogger.step("Verify OTP click skipped: " + e.getMessage());
        }
    }

    public boolean isOtpScreenVisible() {
        try {
            WebElement element = driver.findElement(otpField);
            return element != null && element.isDisplayed();
        } catch (Exception ignored) {
            return false;
        }
    }

    private WebElement findVerifyButton() {
        for (By locator : verifyButtonLocators) {
            try {
                WebElement element = driver.findElement(locator);

                if (element != null && element.isDisplayed()) {
                    return element;
                }
            } catch (NoSuchElementException ignored) {
                // Try next locator
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return null;
    }

    private AndroidKey getAndroidKey(char digit) {
        switch (digit) {
            case '0':
                return AndroidKey.DIGIT_0;
            case '1':
                return AndroidKey.DIGIT_1;
            case '2':
                return AndroidKey.DIGIT_2;
            case '3':
                return AndroidKey.DIGIT_3;
            case '4':
                return AndroidKey.DIGIT_4;
            case '5':
                return AndroidKey.DIGIT_5;
            case '6':
                return AndroidKey.DIGIT_6;
            case '7':
                return AndroidKey.DIGIT_7;
            case '8':
                return AndroidKey.DIGIT_8;
            case '9':
                return AndroidKey.DIGIT_9;
            default:
                throw new RuntimeException("Invalid OTP digit: " + digit);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }
}