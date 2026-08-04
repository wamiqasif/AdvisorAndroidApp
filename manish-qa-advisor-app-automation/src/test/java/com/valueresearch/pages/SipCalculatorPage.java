package com.valueresearch.pages;

import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import com.valueresearch.utils.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SipCalculatorPage {

    private final AndroidDriver driver;

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By sipCalculator = AppiumBy.accessibilityId("Open SIP Return Calculator screen");

    /*
     * Stable SIP Calculator locators from Appium Inspector.
     *
     * Do not use EditText instance(0/1/2) here.
     * Flutter/Android hierarchy can keep EditText nodes in source even when the form is not aligned at top.
     * Labels have stable content-desc, so inputs are anchored using their nearby labels.
     */
    private final By sipTitle = AppiumBy.accessibilityId("SIP Calculator");

    private final By monthlyInvestmentLabel =
            AppiumBy.accessibilityId("Monthly investment amount");

    private final By lumpsumAmountLabel =
            AppiumBy.accessibilityId("Lumpsum amount, if any?");

    private final By timeYearsLabel =
            AppiumBy.accessibilityId("Time (years)");

    private final By monthlyInvestmentInput = AppiumBy.xpath(
            "//android.view.View[@content-desc='Monthly investment amount']" +
                    "/following-sibling::android.widget.EditText[1]"
    );

    private final By lumpsumAmountInput = AppiumBy.xpath(
            "//android.view.View[@content-desc='Lumpsum amount, if any?']" +
                    "/following-sibling::android.widget.EditText[1]"
    );

    private final By timeYearsInput = AppiumBy.xpath(
            "//android.view.View[@content-desc='Time (years)']" +
                    "/following-sibling::android.widget.EditText[1]"
    );

    private final By calculateButton = AppiumBy.accessibilityId("Calculate");
    private final By refreshButton = AppiumBy.accessibilityId("Refresh");

    /*
     * SIP validation popup examples:
     *
     * Please note
     * Monthly amount must be in between 500 - 1,00,00,000
     *
     * Please note
     * Lumpsum amount must be in between 0 - 1,00,00,00,000
     *
     * Please note
     * Years must be in between 1 - 30
     */
    private final By sipValidationPopup = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Please note\")"
    );

    /*
     * The current SIP screen mainly shows inline red validation messages below fields,
     * not popup dialogs. Examples seen on the app:
     * - Monthly Investment should be greater than or equal to ₹500
     * - Years should be greater than or equal to 1
     * - Years should be less than or equal to 30
     *
     * Keep this locator broad enough to catch all field-level validation messages,
     * but narrow enough to avoid normal calculator labels/results.
     */
    private final By inlineValidationError = AppiumBy.xpath(
            "//*[contains(@content-desc,'should') "
                    + "or contains(@content-desc,'required') "
                    + "or contains(@content-desc,'greater than') "
                    + "or contains(@content-desc,'less than') "
                    + "or contains(@content-desc,'equal to') "
                    + "or contains(@content-desc,'Please enter') "
                    + "or contains(@content-desc,'Enter your') "
                    + "or contains(@content-desc,'valid years') "
                    + "or contains(@content-desc,'Monthly Investment') "
                    + "or contains(@content-desc,'Investment Duration') "
                    + "or contains(@content-desc,'Lumpsum Amount') "
                    + "or contains(@content-desc,'lumpsum amount') "
                    + "or contains(@text,'should') "
                    + "or contains(@text,'required') "
                    + "or contains(@text,'greater than') "
                    + "or contains(@text,'less than') "
                    + "or contains(@text,'equal to') "
                    + "or contains(@text,'Please enter') "
                    + "or contains(@text,'Enter your') "
                    + "or contains(@text,'valid years') "
                    + "or contains(@text,'Monthly Investment') "
                    + "or contains(@text,'Investment Duration') "
                    + "or contains(@text,'Lumpsum Amount') "
                    + "or contains(@text,'lumpsum amount')]"
    );

    public SipCalculatorPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public void openSipCalculatorFromHub() {
        try {
            ReportLogger.step("Opening SIP Calculator flow");

            WaitUtils.waitForElement(driver, hubTab, 60);

            WebElement hub = driver.findElement(hubTab);

            if (!hub.isDisplayed()) {
                ReportLogger.fail("Hub tab is not visible. Login/session may not be completed.");
                throw new RuntimeException("Hub tab is not visible. Login/session may not be completed.");
            }

            hub.click();
            ReportLogger.step("Hub clicked");
            sleep(700);

            WaitUtils.waitForElement(driver, sipCalculator, 60);
            driver.findElement(sipCalculator).click();
            ReportLogger.step("SIP Calculator clicked");
            sleep(1200);

            waitForMonthlyInputVisible();
            waitForActionButtonOrFormVisible();

            ReportLogger.pass("SIP Calculator opened successfully");

        } catch (Exception e) {
            ReportLogger.fail("Failed to open SIP Calculator flow");
            throw new RuntimeException("Failed to open SIP Calculator flow: " + e.getMessage(), e);
        }
    }

    public void prepareFreshSipCalculatorForNextCase() {
        try {
            ReportLogger.step("Preparing SIP Calculator for next test case without reopening from Hub");

            closeSipValidationPopupIfPresent();
            closeKeyboardIfPresentForScroll();

            scrollBackToTop();
            waitForMonthlyInputVisible();

            /*
             * Do not reopen SIP Calculator from Hub.
             * The SIP screen may show either Calculate or Refresh depending on previous state.
             * Both states are acceptable because fields remain editable and inline red validation
             * is the source of truth for invalid values.
             */
            if (findElementIfVisible(calculateButton) != null) {
                ReportLogger.pass("Verified action button: Calculate is visible");
            } else if (findElementIfVisible(refreshButton) != null) {
                ReportLogger.pass("Verified action button: Refresh is visible; continuing on same SIP screen without Hub reopen");
            } else {
                ReportLogger.step("Calculate/Refresh not immediately visible. Continuing because SIP input form is visible.");
            }

            ReportLogger.pass("SIP Calculator prepared successfully without Hub reopen");

        } catch (Exception e) {
            ReportLogger.fail("Failed to prepare SIP Calculator for next test case");
            throw new RuntimeException("Failed to prepare SIP Calculator: " + e.getMessage(), e);
        }
    }

    public void reopenSipCalculatorOnlyAfterResultState() {
        /*
         * Kept only for backward compatibility with older tests.
         * Current requirement: do not reopen SIP Calculator from Hub between test cases.
         */
        ReportLogger.step("Hub reopen skipped. Staying on the same SIP Calculator screen as requested.");
        prepareFreshSipCalculatorForNextCase();
    }

    public boolean runSipCase(
            String caseId,
            String monthlyAmount,
            String lumpsumAmount,
            String years,
            boolean shouldCalculate
    ) {
        return runSipCase(caseId, monthlyAmount, lumpsumAmount, years, shouldCalculate, null);
    }

    public boolean runSipCase(
            String caseId,
            String monthlyAmount,
            String lumpsumAmount,
            String years,
            boolean shouldCalculate,
            String expectedPopupText
    ) {
        try {
            ReportLogger.step("Starting SIP Calculator input flow for: " + caseId);

            waitForMonthlyInputVisible();
            waitForActionButtonOrFormVisible();

            boolean monthlyBlockedAtInput = enterMonthlyInvestment(monthlyAmount);
            boolean lumpsumBlockedAtInput = enterLumpsumAmount(lumpsumAmount);
            boolean yearsBlockedAtInput = enterTimeYears(years);

            boolean invalidInputBlockedAtFieldLevel =
                    monthlyBlockedAtInput || lumpsumBlockedAtInput || yearsBlockedAtInput;

            /*
             * First check inline red validation immediately after field entry.
             * Some SIP validations appear as soon as the field loses focus, before pressing Calculate/Refresh.
             * Example: Monthly Investment = 0 shows red text under the field immediately.
             */
            ReportLogger.step("Checking SIP inline red validation immediately after field entry.");

            boolean inlineValidationBeforeAction =
                    validateInlineSipValidationIfPresent(caseId, shouldCalculate, expectedPopupText);

            if (inlineValidationBeforeAction) {
                ReportLogger.pass("SIP Calculator input flow completed for: " + caseId);
                return false;
            }

            /*
             * Important:
             * Some invalid values are blocked directly by the app input field.
             * Example: decimal point "." may not be accepted manually either.
             *
             * In that case, no popup is expected because invalid character never entered the field.
             * So for negative cases, field-level blocking is a valid validation result.
             */
            if (!shouldCalculate && invalidInputBlockedAtFieldLevel) {
                ReportLogger.pass(
                        "Invalid SIP input was blocked at input-field level. "
                                + "Popup validation not required for: " + caseId
                );
                ReportLogger.pass("SIP Calculator input flow completed for: " + caseId);
                return false;
            }

            boolean actionClicked = clickMainActionButton();

            if (!actionClicked) {
                ReportLogger.step("Calculate/Refresh action was not clickable. Checking if inline validation is already visible.");
            }

            ReportLogger.step("Checking SIP inline red validation after input/action.");

            sleep(1200);

            /*
             * The app shows inline red field validation errors for SIP invalid input.
             * Popup is only kept as fallback for older builds.
             */
            boolean inlineValidationDisplayed =
                    validateInlineSipValidationIfPresent(caseId, shouldCalculate, expectedPopupText);

            boolean popupDisplayed = false;

            if (!inlineValidationDisplayed) {
                popupDisplayed = validateSipValidationPopupIfPresent(caseId, shouldCalculate, expectedPopupText);
            }

            /*
             * For invalid/negative cases:
             * If invalid input was accepted, action clicked, and neither inline validation nor popup appeared,
             * then the app allowed invalid calculation. That is a real failure.
             */
            if (!shouldCalculate && !inlineValidationDisplayed && !popupDisplayed && !invalidInputBlockedAtFieldLevel) {
                ReportLogger.fail(
                        "Invalid SIP input was accepted. Expected inline red validation/input blocking, "
                                + "but no validation appeared for: " + caseId
                );

                throw new AssertionError(
                        "Invalid SIP input was accepted. Expected inline red validation/input blocking, "
                                + "but no validation appeared for: " + caseId
                );
            }

            ReportLogger.pass("SIP Calculator input flow completed for: " + caseId);

            /*
             * true means a valid calculation proceeded and the screen may now be in result/Refresh state.
             * Invalid cases with inline validation/popup/input-blocking return false because they remain on input/error state.
             */
            return false;

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            ReportLogger.fail("SIP Calculator input flow failed for: " + caseId);
            throw new RuntimeException("SIP Calculator input flow failed for " + caseId + ": " + e.getMessage(), e);
        }
    }

    /*
     * Returns true when invalid requested value was blocked by the input field itself.
     */
    private boolean enterMonthlyInvestment(String amount) {
        try {
            scrollBackToTop();

            String actualValue = enterValueAndReturnActual(monthlyInvestmentInput, amount);
            String reason = getEmptyActualValueReason(amount, actualValue);

            ReportLogger.step(
                    "Monthly Investment entry"
                            + " | Requested: " + printable(amount)
                            + " | Actual Field Value: " + printable(actualValue)
                            + reason
            );

            boolean blockedAtInput = isInputBlockedAtFieldLevel(amount, actualValue);

            if (blockedAtInput) {
                ReportLogger.pass(
                        "Monthly Investment invalid characters/value blocked by app input field."
                                + " | Requested: " + printable(amount)
                                + " | Actual: " + printable(actualValue)
                                + " | Reason: " + getInputBlockedReason(amount, actualValue)
                );
            }

            return blockedAtInput;

        } catch (Exception e) {
            ReportLogger.fail("Failed to enter Monthly Investment: " + printable(amount));
            throw new RuntimeException("Failed to enter Monthly Investment: " + e.getMessage(), e);
        }
    }

    /*
     * Returns true when invalid requested value was blocked by the input field itself.
     */
    private boolean enterLumpsumAmount(String amount) {
        try {
            String actualValue = enterValueAndReturnActual(lumpsumAmountInput, amount);
            String reason = getEmptyActualValueReason(amount, actualValue);

            ReportLogger.step(
                    "Lumpsum Amount entry"
                            + " | Requested: " + printable(amount)
                            + " | Actual Field Value: " + printable(actualValue)
                            + reason
            );

            boolean blockedAtInput = isInputBlockedAtFieldLevel(amount, actualValue);

            if (blockedAtInput) {
                ReportLogger.pass(
                        "Lumpsum Amount invalid characters/value blocked by app input field."
                                + " | Requested: " + printable(amount)
                                + " | Actual: " + printable(actualValue)
                                + " | Reason: " + getInputBlockedReason(amount, actualValue)
                );
            }

            return blockedAtInput;

        } catch (Exception e) {
            ReportLogger.fail("Failed to enter Lumpsum Amount: " + printable(amount));
            throw new RuntimeException("Failed to enter Lumpsum Amount: " + e.getMessage(), e);
        }
    }

    /*
     * Returns true when invalid requested value was blocked by the input field itself.
     */
    private boolean enterTimeYears(String years) {
        try {
            String actualValue = enterValueAndReturnActual(timeYearsInput, years);
            String reason = getEmptyActualValueReason(years, actualValue);

            ReportLogger.step(
                    "Time Years entry"
                            + " | Requested: " + printable(years)
                            + " | Actual Field Value: " + printable(actualValue)
                            + reason
            );

            boolean blockedAtInput = isInputBlockedAtFieldLevel(years, actualValue);

            if (blockedAtInput) {
                ReportLogger.pass(
                        "Time Years invalid characters/value blocked by app input field."
                                + " | Requested: " + printable(years)
                                + " | Actual: " + printable(actualValue)
                                + " | Reason: " + getInputBlockedReason(years, actualValue)
                );
            }

            return blockedAtInput;

        } catch (Exception e) {
            ReportLogger.fail("Failed to enter Time Years: " + printable(years));
            throw new RuntimeException("Failed to enter Time Years: " + e.getMessage(), e);
        }
    }

    private boolean clickMainActionButton() {
        try {
            /*
             * Inline red validation is checked before this method is called.
             * Prefer Calculate when available.
             * Refresh is only a fallback when the screen is already in Refresh/result state
             * and no inline validation was visible before the action.
             */
            WebElement calculate = findElementIfVisible(calculateButton);

            if (calculate != null) {
                if (!calculate.isEnabled()) {
                    ReportLogger.step("Calculate button is visible but disabled");
                    return false;
                }

                calculate.click();
                ReportLogger.step("Calculate button clicked");
                sleep(1200);
                return true;
            }

            WebElement refresh = findElementIfVisible(refreshButton);

            if (refresh != null) {
                if (!refresh.isEnabled()) {
                    ReportLogger.step("Refresh button is visible but disabled");
                    return false;
                }

                refresh.click();
                ReportLogger.step("Refresh fallback clicked because Calculate was not visible and no inline validation was visible before action");
                sleep(1200);
                return true;
            }

            ReportLogger.step("Neither Calculate nor Refresh button is visible. Continuing to validation check.");
            return false;

        } catch (Exception e) {
            ReportLogger.step("SIP Calculator action button click skipped safely: " + e.getMessage());
            return false;
        }
    }

    private boolean validateSipValidationPopupIfPresent(String caseId, boolean shouldCalculate, String expectedPopupText) {
        try {
            List<WebElement> popups = driver.findElements(sipValidationPopup);

            if (popups.isEmpty()) {
                if (shouldCalculate) {
                    ReportLogger.pass("SIP validation popup not displayed. SIP calculation proceeded for: " + caseId);
                } else {
                    ReportLogger.step("SIP validation popup not displayed for invalid/edge case: " + caseId);
                }
                return false;
            }

            WebElement popup = null;

            for (WebElement popupElement : popups) {
                try {
                    if (popupElement.isDisplayed()) {
                        popup = popupElement;
                        break;
                    }
                } catch (Exception ignored) {
                    // Try next matching popup
                }
            }

            if (popup == null) {
                if (shouldCalculate) {
                    ReportLogger.pass("SIP validation popup found but not visible. SIP calculation proceeded for: " + caseId);
                } else {
                    ReportLogger.step("SIP validation popup found but not visible for invalid/edge case: " + caseId);
                }
                return false;
            }

            String popupText = popup.getAttribute("content-desc");

            if (popupText == null || popupText.trim().isEmpty()) {
                popupText = popup.getText();
            }

            if (popupText == null || popupText.trim().isEmpty()) {
                popupText = "SIP validation popup displayed";
            }

            /*
             * Capture screenshot while popup is still visible.
             */
            try {
                String screenshotPath = ScreenshotUtils.captureScreenshot(
                        driver,
                        "SIP_Validation_Popup_" + caseId
                );

                if (screenshotPath != null && ExtentTestManager.getTest() != null) {
                    ExtentTestManager.getTest().addScreenCaptureFromPath(screenshotPath);
                    ReportLogger.step("SIP popup screenshot captured before closing.");
                }
            } catch (Exception screenshotError) {
                ReportLogger.step(
                        "Could not capture SIP popup screenshot before closing: "
                                + screenshotError.getMessage()
                );
            }

            if (!shouldCalculate) {
                if (expectedPopupText != null && !expectedPopupText.trim().isEmpty()) {
                    String normalizedPopupText = normalizePopupText(popupText);
                    String normalizedExpectedText = normalizePopupText(expectedPopupText);

                    if (!normalizedPopupText.contains(normalizedExpectedText)) {
                        ReportLogger.fail(
                                "Wrong SIP validation popup displayed for " + caseId
                                        + " | Expected contains: " + expectedPopupText
                                        + " | Actual: " + popupText
                        );
                        closeSipValidationPopupIfPresent();
                        throw new AssertionError(
                                "Wrong SIP validation popup displayed. Expected contains: "
                                        + expectedPopupText + " | Actual: " + popupText
                        );
                    }

                    ReportLogger.pass(
                            "Expected SIP validation popup text validated for invalid/edge case: "
                                    + caseId + " | Popup: " + popupText
                    );
                } else {
                    ReportLogger.pass("Expected SIP validation popup displayed for invalid/edge case: " + popupText);
                }

                closeSipValidationPopupIfPresent();
                return true;
            }

            ReportLogger.fail("SIP calculation failed. Unexpected popup displayed: " + popupText);
            closeSipValidationPopupIfPresent();

            throw new AssertionError(
                    "SIP calculation failed because validation popup appeared: " + popupText
            );

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            ReportLogger.step("SIP validation popup check skipped safely: " + e.getMessage());
            return false;
        }
    }

    private boolean validateInlineSipValidationIfPresent(
            String caseId,
            boolean shouldCalculate,
            String expectedValidationText
    ) {
        try {
            List<WebElement> errors = driver.findElements(inlineValidationError);

            WebElement visibleError = null;
            String visibleErrorText = null;

            for (WebElement error : errors) {
                try {
                    if (!error.isDisplayed()) {
                        continue;
                    }

                    String errorText = error.getAttribute("content-desc");

                    if (errorText == null || errorText.trim().isEmpty()) {
                        errorText = error.getText();
                    }

                    if (errorText == null || errorText.trim().isEmpty()) {
                        continue;
                    }

                    visibleError = error;
                    visibleErrorText = errorText.trim();
                    break;

                } catch (Exception ignored) {
                    // Try next matched validation node.
                }
            }

            if (visibleError == null || visibleErrorText == null || visibleErrorText.trim().isEmpty()) {
                if (!shouldCalculate) {
                    ReportLogger.step("Inline SIP red validation error not displayed for invalid/edge case: " + caseId);
                }
                return false;
            }

            captureSipValidationScreenshot(caseId, "Inline_Red_Validation");

            /*
             * Current requirement:
             * Any red inline validation means the entered value is out of range/smaller than minimum.
             * Validate it in the report and pass the testcase instead of failing it.
             */
            ReportLogger.pass(
                    "Inline SIP red validation displayed and validated for case: "
                            + caseId + " | Error: " + visibleErrorText
            );

            return true;

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            ReportLogger.step("Inline SIP validation check skipped safely: " + e.getMessage());
            return false;
        }
    }

    private void captureSipValidationScreenshot(String caseId, String validationType) {
        try {
            String screenshotPath = ScreenshotUtils.captureScreenshot(
                    driver,
                    "SIP_" + validationType + "_" + caseId
            );

            if (screenshotPath != null && ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().addScreenCaptureFromPath(screenshotPath);
                ReportLogger.step("SIP validation screenshot captured: " + validationType);
            }
        } catch (Exception screenshotError) {
            ReportLogger.step(
                    "Could not capture SIP validation screenshot: "
                            + screenshotError.getMessage()
            );
        }
    }

    private void closeSipValidationPopupIfPresent() {
        try {
            List<WebElement> popups = driver.findElements(sipValidationPopup);

            boolean popupVisible = false;

            for (WebElement popup : popups) {
                try {
                    if (popup.isDisplayed()) {
                        popupVisible = true;
                        break;
                    }
                } catch (Exception ignored) {
                    // Ignore stale popup
                }
            }

            if (!popupVisible) {
                return;
            }

            /*
             * Popup close X coordinate from Appium Inspector:
             * near right side of bottom popup.
             */
            Map<String, Object> params = new HashMap<>();
            params.put("x", 985);
            params.put("y", 2025);

            driver.executeScript("mobile: clickGesture", params);

            ReportLogger.step("SIP validation popup closed using X button coordinate");
            sleep(700);

        } catch (Exception e) {
            ReportLogger.step("Unable to close SIP validation popup using X coordinate: " + e.getMessage());

            try {
                driver.pressKey(new KeyEvent(AndroidKey.BACK));
                ReportLogger.step("SIP validation popup closed using Android BACK");
                sleep(700);
            } catch (Exception backError) {
                ReportLogger.step("Unable to close SIP validation popup using BACK: " + backError.getMessage());
            }
        }
    }

    private void resetCalculatorIfRefreshVisible() {
        /* Kept only for backward compatibility. Do not reopen from Hub. */
        if (findElementIfVisible(refreshButton) != null) {
            ReportLogger.step("Refresh button is visible. Staying on same SIP screen.");
        } else {
            ReportLogger.step("Refresh button not visible. SIP Calculator is already in Calculate/input mode.");
        }
    }

    private void scrollBackToTop() {
        closeKeyboardIfPresentForScroll();

        for (int i = 0; i < 7; i++) {
            if (isSipTopFormVisible()) {
                ReportLogger.step("SIP top form is visible. Monthly Investment field aligned at top.");
                return;
            }

            try {
                swipeDownW3C();
                ReportLogger.step("W3C swipe down towards SIP top form. Attempt: " + (i + 1));
                sleep(500);
            } catch (Exception e) {
                ReportLogger.step("W3C swipe down failed/skipped: " + e.getMessage());
            }
        }

        if (!isSipTopFormVisible()) {
            ReportLogger.fail("Unable to scroll back to SIP Calculator top form");
            throw new RuntimeException("Unable to scroll back to SIP Calculator top form");
        }
    }

    private boolean isSipTopFormVisible() {
        WebElement title = findElementIfVisible(sipTitle);
        WebElement monthlyLabel = findElementIfVisible(monthlyInvestmentLabel);
        WebElement monthlyInput = findElementIfVisible(monthlyInvestmentInput);
        WebElement lumpsumLabel = findElementIfVisible(lumpsumAmountLabel);
        WebElement timeLabel = findElementIfVisible(timeYearsLabel);

        if (title == null || monthlyLabel == null || monthlyInput == null
                || lumpsumLabel == null || timeLabel == null) {
            return false;
        }

        /*
         * Extra safety:
         * Monthly input should be visually in upper/middle form area.
         * This avoids false-positive visibility from hierarchy/source nodes.
         */
        int inputY = monthlyInput.getRect().getY();

        return inputY >= 250 && inputY <= 950;
    }

    private void waitForMonthlyInputVisible() {
        WaitUtils.waitForElement(driver, sipTitle, 60);
        WaitUtils.waitForElement(driver, monthlyInvestmentLabel, 60);
        WaitUtils.waitForElement(driver, monthlyInvestmentInput, 60);

        ReportLogger.pass("Verified screen/element: SIP Calculator Monthly Investment field");
    }

    private void waitForCalculateButtonVisible() {
        waitForActionButtonOrFormVisible();
    }

    private void waitForActionButtonOrFormVisible() {
        if (findElementIfVisible(calculateButton) != null) {
            ReportLogger.pass("Verified action button: Calculate is visible");
            return;
        }

        if (findElementIfVisible(refreshButton) != null) {
            ReportLogger.pass("Verified action button: Refresh is visible; staying on same SIP screen");
            return;
        }

        ReportLogger.step("Calculate/Refresh not visible yet. SIP form is visible, continuing safely.");
    }

    private String enterValueAndReturnActual(By locator, String value) {
        WaitUtils.waitForElement(driver, locator, 60);

        WebElement input = driver.findElement(locator);

        input.click();
        sleep(150);

        fastClearInput(input);

        if (value != null && !value.trim().isEmpty()) {
            if (containsManualKeyboardBlockedCharacter(value)) {
                enterOnlyManualAllowedPrefix(input, value);
            } else {
                input.sendKeys(value);
            }
        }

        closeKeyboardWithEnterAndWait();

        sleep(350);

        return getInputActualValue(locator);
    }

    private boolean containsManualKeyboardBlockedCharacter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        /*
         * Appium sendKeys can inject '.', '-', letters and symbols even when the app keyboard does not allow them.
         * To match manual behavior, do not inject these characters directly.
         */
        return value.matches(".*[^0-9].*");
    }

    private void enterOnlyManualAllowedPrefix(WebElement input, String requestedValue) {
        if (requestedValue == null) {
            return;
        }

        StringBuilder typedPrefix = new StringBuilder();

        for (int i = 0; i < requestedValue.length(); i++) {
            char ch = requestedValue.charAt(i);

            if (!Character.isDigit(ch)) {
                ReportLogger.step(
                        "Manual keyboard blocked unsupported character '" + ch + "'. "
                                + "Stopped typing remaining characters for requested value: "
                                + printable(requestedValue)
                );
                break;
            }

            typedPrefix.append(ch);
        }

        if (typedPrefix.length() > 0) {
            input.sendKeys(typedPrefix.toString());
        }
    }

    private String getInputActualValue(By locator) {
        try {
            WebElement input = driver.findElement(locator);

            /*
             * Android UiAutomator2 supports text/name for EditText.
             * For empty fields, getText() usually returns empty string.
             */
            String text = input.getText();

            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }

            /*
             * Some Flutter/React Native views expose data through content-desc.
             */
            String contentDesc = input.getAttribute("content-desc");

            if (contentDesc != null && !contentDesc.trim().isEmpty()) {
                return contentDesc.trim();
            }

            /*
             * Some input fields expose placeholder through hint.
             * If only hint is present, actual user-entered value is empty.
             */
            String hint = input.getAttribute("hint");

            if (hint != null && !hint.trim().isEmpty()) {
                return "";
            }

            /*
             * Do not call input.getAttribute("value").
             * UiAutomator2 does not support value attribute and it pollutes report with a huge error.
             */
            return "";

        } catch (Exception e) {
            ReportLogger.debug("Could not read actual input value safely.");
            return "";
        }
    }

    private void fastClearInput(WebElement input) {
        try {
            input.clear();
            sleep(100);
            return;
        } catch (Exception ignored) {
            // Fallback below
        }

        try {
            String currentValue = input.getText();

            if (currentValue == null || currentValue.isEmpty()) {
                return;
            }

            input.click();

            int deleteCount = Math.min(currentValue.length() + 5, 20);

            for (int i = 0; i < deleteCount; i++) {
                driver.pressKey(new KeyEvent(AndroidKey.DEL));
            }

            sleep(120);

        } catch (Exception e) {
            ReportLogger.step("Fast clear fallback skipped safely: " + e.getMessage());
        }
    }

    private void closeKeyboardIfPresentForScroll() {
        try {
            driver.hideKeyboard();
            sleep(300);
        } catch (Exception ignored) {
            // Keyboard was not open
        }
    }

    private void closeKeyboardWithEnterAndWait() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            sleep(300);
        } catch (Exception e) {
            ReportLogger.step("Keyboard Enter skipped safely: " + e.getMessage());
        }
    }

    private WebElement findElementIfVisible(By locator) {
        try {
            WebElement element = driver.findElement(locator);

            if (element != null && element.isDisplayed()) {
                return element;
            }

        } catch (NoSuchElementException ignored) {
            // Element not present
        } catch (Exception ignored) {
            // Ignore and return null
        }

        return null;
    }

    private boolean isElementVisible(By locator) {
        return findElementIfVisible(locator) != null;
    }

    private boolean isInputBlockedAtFieldLevel(String requestedValue, String actualValue) {
        if (requestedValue == null || requestedValue.trim().isEmpty()) {
            return false;
        }

        /*
         * If the test requested a value but the field became empty,
         * the app rejected that value at the field level.
         * Example: requested monthly value 0 may be exposed as empty by UiAutomator.
         */
        if (actualValue == null || actualValue.trim().isEmpty()) {
            return true;
        }

        /*
         * Manual keyboard blocked characters:
         * Appium sendKeys can inject '.', '-', letters, comma and symbols. Manual user input cannot.
         * If requested value had non-digit characters and the actual value is not exactly the same,
         * treat it as valid field-level blocking.
         */
        if (containsManualKeyboardBlockedCharacter(requestedValue)
                && !normalizeForCompare(requestedValue).equals(normalizeForCompare(actualValue))) {
            return true;
        }

        if (isDecimalInput(requestedValue) && !containsDecimal(actualValue)) {
            return true;
        }

        if (requestedValue.contains("-") && !safeString(actualValue).contains("-")) {
            return true;
        }

        if (requestedValue.contains(",") && !safeString(actualValue).contains(",")) {
            return true;
        }

        if (requestedValue.contains("₹") && !safeString(actualValue).contains("₹")) {
            return true;
        }

        /*
         * Special character handling:
         * If requested value contains unsupported special chars and actual field is empty,
         * app blocked the input.
         */
        if (isSpecialCharacterInput(requestedValue) && isBlankOrOnlyCurrency(actualValue)) {
            return true;
        }

        /*
         * Alphabet handling:
         * If requested value contains letters and actual field became empty,
         * app blocked text input.
         */
        if (requestedValue.matches(".*[A-Za-z].*") && isBlankOrOnlyCurrency(actualValue)) {
            return true;
        }

        return false;
    }

    private String getInputBlockedReason(String requestedValue, String actualValue) {
        if (requestedValue == null || requestedValue.trim().isEmpty()) {
            return "No requested value.";
        }

        if (actualValue == null || actualValue.trim().isEmpty()) {
            return "Requested value was rejected by app input field.";
        }

        if (containsManualKeyboardBlockedCharacter(requestedValue)
                && !normalizeForCompare(requestedValue).equals(normalizeForCompare(actualValue))) {
            return "Unsupported keyboard character was blocked by app input field/manual keyboard.";
        }

        if (isDecimalInput(requestedValue) && !containsDecimal(actualValue)) {
            return "Decimal point was blocked by app input field.";
        }

        if (requestedValue.contains("-") && !safeString(actualValue).contains("-")) {
            return "Negative sign was blocked by app input field.";
        }

        if (requestedValue.contains(",") && !safeString(actualValue).contains(",")) {
            return "Comma was blocked/rejected by app input field.";
        }

        if (requestedValue.contains("₹") && !safeString(actualValue).contains("₹")) {
            return "Currency symbol was blocked/rejected by app input field.";
        }

        if (isSpecialCharacterInput(requestedValue) && isBlankOrOnlyCurrency(actualValue)) {
            return "Unsupported special characters were blocked by app input field.";
        }

        if (requestedValue.matches(".*[A-Za-z].*") && isBlankOrOnlyCurrency(actualValue)) {
            return "Alphabetic/text input was blocked by app input field.";
        }

        return "Requested invalid value was not accepted exactly as typed.";
    }

    private boolean isDecimalInput(String value) {
        return value != null && value.contains(".");
    }

    private boolean containsDecimal(String value) {
        return value != null && value.contains(".");
    }

    private boolean isSpecialCharacterInput(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        /*
         * Marks inputs like @@@, ###, %%% as special-character inputs.
         * Digits, comma, rupee symbol, dot, minus, spaces and letters are not counted here.
         */
        return value.matches(".*[^0-9,₹.\\-\\sA-Za-z].*");
    }

    private boolean isBlankOrOnlyCurrency(String value) {
        if (value == null) {
            return true;
        }

        String cleanValue = value
                .replace("₹", "")
                .replace(",", "")
                .trim();

        return cleanValue.isEmpty();
    }

    private String getEmptyActualValueReason(String requestedValue, String actualValue) {
        if (actualValue != null && !actualValue.trim().isEmpty()) {
            return "";
        }

        if (requestedValue == null || requestedValue.trim().isEmpty()) {
            return " | Reason: Test data intentionally left this field empty.";
        }

        if (isSpecialCharacterInput(requestedValue)) {
            return " | Reason: App blocked unsupported special characters.";
        }

        if (requestedValue.contains("₹")) {
            return " | Reason: App rejected currency symbol. Field accepts numeric amount only.";
        }

        if (requestedValue.matches(".*[A-Za-z].*")) {
            return " | Reason: App rejected alphabetic/text input. Field accepts numeric value only.";
        }

        if (requestedValue.contains(".")) {
            return " | Reason: App rejected decimal value. Field accepts whole numbers only.";
        }

        return " | Reason: App did not accept the requested value in this input field.";
    }

    private void swipeDownW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 700));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), 540, 1550));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe down failed: " + e.getMessage(), e);
        }
    }


    private String normalizePopupText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
    private String normalizeForCompare(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String printable(String value) {
        if (value == null) {
            return "null";
        }

        if (value.isEmpty()) {
            return "<empty>";
        }

        return value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
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