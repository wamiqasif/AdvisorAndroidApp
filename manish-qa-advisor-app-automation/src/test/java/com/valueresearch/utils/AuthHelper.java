package com.valueresearch.utils;

import com.valueresearch.pages.LoginPage;
import com.valueresearch.pages.OtpPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthHelper {

    private final AndroidDriver driver;

    private final By nextButton = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Next\")"
    );

    private final By[] pinScreenLocators = new By[]{
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Enter your Advisor PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Enter PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Advisor PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Enter your Advisor PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Enter PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Advisor PIN\")")
    };

    private final By[] incorrectPinLocators = new By[]{
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Incorrect PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Invalid PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Incorrect PIN\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Invalid PIN\")")
    };

    private final By[] emailLoginLocators = new By[]{
            AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Email\")"),
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"email\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Email\")"),
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"email\")"),
            nextButton
    };

    private final By[] loggedInTabLocators = new By[]{
            AppiumBy.accessibilityId("Funds"),
            AppiumBy.accessibilityId("Stocks"),
            AppiumBy.accessibilityId("Portfolio"),
            AppiumBy.accessibilityId("Hub"),

            AppiumBy.androidUIAutomator("new UiSelector().description(\"Funds\")"),
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Stocks\")"),
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Portfolio\")"),
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Hub\")"),

            AppiumBy.androidUIAutomator("new UiSelector().text(\"Funds\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Stocks\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Portfolio\")"),
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Hub\")")
    };

    public AuthHelper(AndroidDriver driver) {
        this.driver = driver;
    }

    /*
     * Existing/default login method.
     * Keep all old modules using this method.
     * This waits only for normal logged-in bottom tabs.
     */
    public void ensureLoggedIn() {
        ensureLoggedIn(null);
    }

    /*
     * Onboarding-specific login method.
     * Use this only in OnboardingTest.java.
     *
     * Why needed:
     * After OTP, onboarding users may land directly on "Subscribe Now"
     * instead of the normal Funds/Stocks/Portfolio/Hub bottom tabs.
     */
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

        ensureLoggedIn(onboardingPostLoginMarkers);
    }

    /*
     * Shared login handler.
     * additionalPostLoginMarkers is null for normal modules.
     * For onboarding, it accepts Subscribe Now / onboarding screens after login.
     */
    private void ensureLoggedIn(By[] additionalPostLoginMarkers) {
        try {
            ReportLogger.step("Checking app login/session state");

            waitForAppToLoad();

            if (isLoggedInScreenVisible()) {
                ReportLogger.pass("User is already logged in. Bottom tabs are visible.");
                return;
            }

            if (isAdditionalPostLoginMarkerVisible(additionalPostLoginMarkers)) {
                ReportLogger.pass("User is already logged in. Onboarding screen is visible.");
                return;
            }

            if (isPinScreenVisible()) {
                ReportLogger.step("PIN screen detected. Entering PIN once.");
                enterPin();
                waitAfterPinLogin(additionalPostLoginMarkers);
                return;
            }

            if (isEmailLoginVisible()) {
                ReportLogger.step("Email login screen detected. Starting Email + OTP login.");
                loginWithEmailAndOtp(additionalPostLoginMarkers);
                return;
            }

            ReportLogger.fail("Unable to detect app state. Logged-in tabs, onboarding screen, PIN screen, and Email login screen were not found.");
            throw new RuntimeException("Unable to detect app state for login.");

        } catch (RuntimeException e) {
            ReportLogger.fail("Login/session check failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ReportLogger.fail("Login/session check failed unexpectedly: " + e.getMessage());
            throw new RuntimeException("Login/session check failed unexpectedly: " + e.getMessage(), e);
        }
    }

    private boolean isPinScreenVisible() {
        for (By locator : pinScreenLocators) {
            try {
                WebElement element = driver.findElement(locator);

                if (element != null && element.isDisplayed()) {
                    ReportLogger.step("PIN screen indicator found: " + safeElementText(element));
                    return true;
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }

    private boolean isIncorrectPinVisible() {
        for (By locator : incorrectPinLocators) {
            try {
                WebElement element = driver.findElement(locator);

                if (element != null && element.isDisplayed()) {
                    ReportLogger.fail("Incorrect PIN message visible: " + safeElementText(element));
                    return true;
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }

    private boolean isEmailLoginVisible() {
        for (By locator : emailLoginLocators) {
            try {
                WebElement element = driver.findElement(locator);

                if (element != null && element.isDisplayed()) {
                    ReportLogger.step("Email login screen indicator found: " + safeElementText(element));
                    return true;
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }

    private boolean isLoggedInScreenVisible() {
        for (By locator : loggedInTabLocators) {
            try {
                WebElement element = driver.findElement(locator);

                if (element != null && element.isDisplayed()) {
                    ReportLogger.step("Logged-in tab indicator found: " + safeElementText(element));
                    return true;
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }

    private boolean isAdditionalPostLoginMarkerVisible(By[] additionalPostLoginMarkers) {
        if (additionalPostLoginMarkers == null || additionalPostLoginMarkers.length == 0) {
            return false;
        }

        for (By locator : additionalPostLoginMarkers) {
            try {
                WebElement element = driver.findElement(locator);

                if (element != null && element.isDisplayed()) {
                    ReportLogger.step("Additional post-login indicator found: " + safeElementText(element));
                    return true;
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }

    private void enterPin() {
        try {
            String pin = getOptionalConfig("appPin");

            if (pin == null || pin.trim().isEmpty()) {
                pin = getOptionalConfig("pin");
            }

            if (pin == null || pin.trim().isEmpty()) {
                pin = getOptionalConfig("advisorPin");
            }

            if (pin == null || !pin.matches("\\d{4,6}")) {
                ReportLogger.fail("Invalid or missing PIN in config.properties. Add appPin=1234");
                throw new RuntimeException("Invalid or missing PIN in config.properties. Add appPin=1234");
            }

            ReportLogger.step("Entering app PIN using strict keypad locator");

            for (char digit : pin.toCharArray()) {
                tapPinDigit(String.valueOf(digit));
                sleep(700);
            }

            ReportLogger.pass("PIN entered successfully");

        } catch (RuntimeException e) {
            ReportLogger.fail("PIN login failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ReportLogger.fail("PIN login failed unexpectedly: " + e.getMessage());
            throw new RuntimeException("PIN login failed unexpectedly: " + e.getMessage(), e);
        }
    }

    private void tapPinDigit(String digit) {
        /*
         * First try exact visible keypad digit.
         * Do not use descriptionContains/textContains for digits because email/phone can contain numbers.
         */
        By[] exactDigitLocators = new By[]{
                AppiumBy.accessibilityId(digit),
                AppiumBy.androidUIAutomator("new UiSelector().description(\"" + digit + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + digit + "\")")
        };

        for (By locator : exactDigitLocators) {
            try {
                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    if (element != null && element.isDisplayed() && element.isEnabled()) {
                        element.click();
                        ReportLogger.step("Tapped PIN digit by locator: " + digit);
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        /*
         * Fallback: calibrated coordinates for visible keypad.
         */
        ReportLogger.step("Digit locator not found. Using coordinate fallback for: " + digit);
        tapPinDigitByCoordinates(digit.charAt(0));
    }

    private void tapPinDigitByCoordinates(char digit) {
        Dimension size = driver.manage().window().getSize();

        int width = size.getWidth();
        int height = size.getHeight();

        int leftX = (int) (width * 0.25);
        int centerX = (int) (width * 0.50);
        int rightX = (int) (width * 0.75);

        /*
         * Calibrated for your Pixel 9a emulator / Samsung real device PIN keypad.
         * Coordinate fallback is used only when exact digit locators fail.
         */
        int row1Y = (int) (height * 0.60); // 1 2 3
        int row2Y = (int) (height * 0.67); // 4 5 6
        int row3Y = (int) (height * 0.74); // 7 8 9
        int row4Y = (int) (height * 0.82); // 0

        int x;
        int y;

        switch (digit) {
            case '1':
                x = leftX;
                y = row1Y;
                break;
            case '2':
                x = centerX;
                y = row1Y;
                break;
            case '3':
                x = rightX;
                y = row1Y;
                break;
            case '4':
                x = leftX;
                y = row2Y;
                break;
            case '5':
                x = centerX;
                y = row2Y;
                break;
            case '6':
                x = rightX;
                y = row2Y;
                break;
            case '7':
                x = leftX;
                y = row3Y;
                break;
            case '8':
                x = centerX;
                y = row3Y;
                break;
            case '9':
                x = rightX;
                y = row3Y;
                break;
            case '0':
                x = centerX;
                y = row4Y;
                break;
            default:
                throw new RuntimeException("Invalid PIN digit: " + digit);
        }

        tapByCoordinates(x, y);
        ReportLogger.step("Tapped PIN digit by coordinates: " + digit);
    }

    private void tapByCoordinates(int x, int y) {
        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);

        driver.executeScript("mobile: clickGesture", params);
    }

    private void loginWithEmailAndOtp(By[] additionalPostLoginMarkers) {
        try {
            String testEmail = getOptionalConfig("testEmail");

            if (testEmail == null || testEmail.trim().isEmpty()) {
                testEmail = getOptionalConfig("email");
            }

            if (testEmail == null || testEmail.trim().isEmpty()) {
                testEmail = getOptionalConfig("otpEmail");
            }

            if (testEmail == null || testEmail.trim().isEmpty()) {
                ReportLogger.fail("Login email is missing in config.properties. Add testEmail=your_email or otpEmail=your_email");
                throw new RuntimeException("Login email is missing in config.properties. Add testEmail=your_email or otpEmail=your_email");
            }

            ReportLogger.step("Starting Email + OTP login flow for: " + maskEmail(testEmail));

            LoginPage loginPage = new LoginPage(driver);

            ReportLogger.step("Entering email");
            loginPage.enterEmail(testEmail);

            ReportLogger.step("Clicking Next after email entry");
            loginPage.clickNext();

            OtpPage otpPage = new OtpPage(driver);

            otpPage.waitForOtpScreen();

            String otp = getOtpAutomaticallyOrManually();

            otpPage.enterOtp(otp);
            otpPage.clickVerifyIfVisible();

            ReportLogger.step("Waiting briefly after OTP entry");
            sleep(5000);

            waitAfterOtpLogin(additionalPostLoginMarkers);

            ReportLogger.pass("Email + OTP login flow completed successfully");

        } catch (RuntimeException e) {
            ReportLogger.fail("Email + OTP login flow failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ReportLogger.fail("Email + OTP login flow failed unexpectedly: " + e.getMessage());
            throw new RuntimeException("Email + OTP login flow failed unexpectedly: " + e.getMessage(), e);
        }
    }

    private String getOtpAutomaticallyOrManually() {
        boolean autoFetchEnabled = isOtpAutoFetchEnabled();

        if (autoFetchEnabled) {
            ReportLogger.step("OTP auto-fetch is enabled. Fetching OTP from email.");

            String otp = OtpEmailReader.fetchLatestOtp();

            if (otp == null || !otp.matches("\\d{6}")) {
                ReportLogger.fail("Auto-fetched OTP is invalid.");
                throw new RuntimeException("Auto-fetched OTP is invalid.");
            }

            ReportLogger.pass("OTP auto-fetched successfully from email");
            return otp;
        }

        ReportLogger.step("OTP auto-fetch is disabled. Reading OTP from config.properties.");

        String otp = getOptionalConfig("manualOtp");

        if (otp == null || otp.trim().isEmpty()) {
            otp = getOptionalConfig("otp");
        }

        if (otp == null || !otp.matches("\\d{6}")) {
            ReportLogger.fail("Manual OTP is missing or invalid. Add manualOtp=123456 in config.properties.");
            throw new RuntimeException("Manual OTP is missing or invalid. Add manualOtp=123456 in config.properties.");
        }

        ReportLogger.pass("Manual OTP loaded successfully from config.properties");
        return otp;
    }

    private boolean isOtpAutoFetchEnabled() {
        String value = getOptionalConfig("otpAutoFetchEnabled");

        if (value == null || value.trim().isEmpty()) {
            value = getOptionalConfig("otpAutoFetch");
        }

        if (value == null || value.trim().isEmpty()) {
            value = getOptionalConfig("autoFetchOtp");
        }

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("y");
    }

    private void waitAfterPinLogin(By[] additionalPostLoginMarkers) {
        try {
            ReportLogger.step("Waiting for valid logged-in screen after PIN");

            long endTime = System.currentTimeMillis() + 45_000L;

            while (System.currentTimeMillis() < endTime) {
                if (isIncorrectPinVisible()) {
                    throw new RuntimeException("Incorrect Advisor PIN. Update appPin in config.properties.");
                }

                if (isLoggedInScreenVisible()) {
                    ReportLogger.pass("Logged-in bottom tabs loaded after PIN");
                    return;
                }

                if (isAdditionalPostLoginMarkerVisible(additionalPostLoginMarkers)) {
                    ReportLogger.pass("Onboarding post-login screen loaded after PIN");
                    return;
                }

                sleep(1000);
            }

            if (isPinScreenVisible()) {
                throw new RuntimeException("Still on PIN screen after entering PIN. Check appPin in config.properties.");
            }

            throw new RuntimeException("No valid logged-in screen was visible after PIN.");

        } catch (RuntimeException e) {
            ReportLogger.fail("Waiting after PIN failed: " + e.getMessage());
            throw e;
        }
    }

    private void waitAfterOtpLogin(By[] additionalPostLoginMarkers) {
        try {
            ReportLogger.step("Waiting for valid post-login screen after OTP");

            long endTime = System.currentTimeMillis() + 90_000L;
            boolean pinEnteredAfterOtp = false;

            while (System.currentTimeMillis() < endTime) {
                if (isLoggedInScreenVisible()) {
                    ReportLogger.pass("Logged-in bottom tabs loaded after OTP");
                    return;
                }

                if (isAdditionalPostLoginMarkerVisible(additionalPostLoginMarkers)) {
                    ReportLogger.pass("Onboarding post-login screen loaded after OTP");
                    return;
                }

                if (isIncorrectPinVisible()) {
                    throw new RuntimeException("Incorrect Advisor PIN. Update appPin in config.properties.");
                }

                if (!pinEnteredAfterOtp && isPinScreenVisible()) {
                    ReportLogger.step("PIN screen appeared after OTP. Entering PIN once.");
                    enterPin();
                    pinEnteredAfterOtp = true;
                    sleep(3000);
                    continue;
                }

                sleep(1500);
            }

            throw new RuntimeException("No valid post-login screen was visible after OTP.");

        } catch (RuntimeException e) {
            ReportLogger.fail("Waiting after OTP failed: " + e.getMessage());
            throw e;
        }
    }

    private void waitForAppToLoad() {
        ReportLogger.step("Waiting for app to load");
        sleep(5000);
    }

    private String getOptionalConfig(String key) {
        try {
            String value = ConfigReader.get(key);

            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            return value.trim();

        } catch (Exception ignored) {
            return null;
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "configured email";
        }

        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "**@" + domain;
        }

        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }

    private String safeElementText(WebElement element) {
        try {
            String text = element.getText();

            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        } catch (Exception ignored) {
            // Try content-desc
        }

        try {
            String contentDesc = element.getAttribute("content-desc");

            if (contentDesc != null && !contentDesc.trim().isEmpty()) {
                return contentDesc;
            }
        } catch (Exception ignored) {
            // Return fallback
        }

        return "visible element";
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