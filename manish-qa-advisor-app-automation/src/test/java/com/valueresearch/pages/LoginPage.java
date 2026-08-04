package com.valueresearch.pages;

import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginPage {

    private final AndroidDriver driver;

    private final By emailOrPhoneInput = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(0)"
    );

    private final By nextButton = AppiumBy.accessibilityId("Next");

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public void enterEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                ReportLogger.fail("Login email is empty");
                throw new RuntimeException("Login email is empty");
            }

            ReportLogger.step("Waiting for email/phone input field");

            WaitUtils.waitForElement(driver, emailOrPhoneInput, 60);

            WebElement input = driver.findElement(emailOrPhoneInput);

            tapElementCenter(input);
            sleep(500);

            clearInput(input);

            input.sendKeys(email.trim());

            sleep(700);

            String actualValue = readInputValue();

            ReportLogger.step(
                    "Email/Phone entered"
                            + " | Requested: " + maskEmail(email)
                            + " | Actual field value: " + maskEmail(actualValue)
            );

            if (actualValue == null || actualValue.trim().isEmpty()) {
                throw new RuntimeException("Email/phone field is still empty after sendKeys");
            }

            dismissKeyboardSafely();

            ReportLogger.pass("Email/Phone field filled successfully");

        } catch (Exception e) {
            ReportLogger.fail("Failed to enter email/phone: " + e.getMessage());
            throw new RuntimeException("Failed to enter email/phone: " + e.getMessage(), e);
        }
    }

    public void clickNext() {
        try {
            ReportLogger.step("Clicking Next on login screen");

            dismissKeyboardSafely();

            WaitUtils.waitForElement(driver, nextButton, 60);

            WebElement next = driver.findElement(nextButton);

            if (!next.isDisplayed() || !next.isEnabled()) {
                throw new RuntimeException("Next button is not clickable");
            }

            tapElementCenter(next);

            ReportLogger.step("Next clicked on login screen");

            sleep(1500);

        } catch (Exception e) {
            ReportLogger.fail("Failed to click Next on login screen: " + e.getMessage());
            throw new RuntimeException("Failed to click Next on login screen: " + e.getMessage(), e);
        }
    }

    private void tapElementCenter(WebElement element) {
        try {
            Rectangle rect = element.getRect();

            int x = rect.getX() + (rect.getWidth() / 2);
            int y = rect.getY() + (rect.getHeight() / 2);

            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);

            driver.executeScript("mobile: clickGesture", params);

            sleep(300);

        } catch (Exception e) {
            try {
                element.click();
                sleep(300);
            } catch (Exception clickError) {
                throw new RuntimeException("Failed to tap element center: " + clickError.getMessage(), clickError);
            }
        }
    }

    private void clearInput(WebElement input) {
        try {
            input.clear();
            sleep(300);
        } catch (Exception e) {
            ReportLogger.debug("Normal clear failed. Trying DEL fallback: " + e.getMessage());

            try {
                input.click();

                for (int i = 0; i < 40; i++) {
                    driver.pressKey(new KeyEvent(AndroidKey.DEL));
                    sleep(20);
                }
            } catch (Exception deleteError) {
                ReportLogger.debug("DEL clear fallback skipped: " + deleteError.getMessage());
            }
        }
    }

    private String readInputValue() {
        try {
            List<WebElement> inputs = driver.findElements(emailOrPhoneInput);

            if (inputs.isEmpty()) {
                return "";
            }

            WebElement input = inputs.get(0);

            String text = input.getText();

            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }

            String contentDesc = input.getAttribute("content-desc");

            if (contentDesc != null && !contentDesc.trim().isEmpty()) {
                return contentDesc.trim();
            }

            String value = input.getAttribute("value");

            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }

            return "";

        } catch (Exception e) {
            ReportLogger.debug("Could not read email/phone input value: " + e.getMessage());
            return "";
        }
    }

    private void dismissKeyboardSafely() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            sleep(400);
        } catch (Exception e) {
            ReportLogger.debug("Keyboard dismiss skipped: " + e.getMessage());
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "<empty>";
        }

        int atIndex = email.indexOf("@");

        if (atIndex <= 1) {
            return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        }

        String prefix = email.substring(0, 2);
        String domain = atIndex >= 0 ? email.substring(atIndex) : "";

        return prefix + "***" + domain;
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