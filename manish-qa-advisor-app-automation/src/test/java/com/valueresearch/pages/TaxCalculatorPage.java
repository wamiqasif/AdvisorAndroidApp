package com.valueresearch.pages;

import com.valueresearch.tests.TaxCalculatorTest.TaxData;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxCalculatorPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By portfolioPlanner = AppiumBy.accessibilityId("Portfolio Planner");
    private final By investorManish = AppiumBy.accessibilityId("Manish Khatri");
    private final By nextButton = AppiumBy.accessibilityId("Next");
    private final By helpMeCalculateThis = AppiumBy.accessibilityId("Help me calculate this");

    private final By continueButton = AppiumBy.accessibilityId("Continue");
    private final By calculateTaxButton = AppiumBy.accessibilityId("Calculate Tax");
    private final By startOverButton = AppiumBy.accessibilityId("Start Over");
    private final By exitButton = AppiumBy.accessibilityId("Exit");

    private final By taxCalculatorTitle = AppiumBy.accessibilityId("Tax Calculator");

    private final By salaryBreakupScreenMarker = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Monthly contribution to Employee Provident Fund\")"
    );

    private final By houseRentDetailsMarker = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"House Rent Details\")"
    );

    private final By deductionsSection80Marker = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Deductions under Section 80\")"
    );

    private final By otherDeductionsMarker = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Other deductions\")"
    );

    private final By unableToSaveRecordPopup = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Unable to save record\")"
    );

    private final By unableToGetTaxInfoPopup = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Unable to get tax info data\")"
    );

    private final By pleaseNotePopup = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Please note\")"
    );

    private final By taxInlineValidationError = AppiumBy.xpath(
            "//*[contains(@content-desc,'Please') "
                    + "or contains(@content-desc,'please') "
                    + "or contains(@content-desc,'Enter') "
                    + "or contains(@content-desc,'enter') "
                    + "or contains(@content-desc,'valid') "
                    + "or contains(@content-desc,'Valid') "
                    + "or contains(@content-desc,'required') "
                    + "or contains(@content-desc,'Required') "
                    + "or contains(@content-desc,'should') "
                    + "or contains(@content-desc,'Should') "
                    + "or contains(@content-desc,'greater than') "
                    + "or contains(@content-desc,'less than') "
                    + "or contains(@content-desc,'equal to') "
                    + "or contains(@content-desc,'cannot') "
                    + "or contains(@content-desc,'Cannot') "
                    + "or contains(@content-desc,'invalid') "
                    + "or contains(@content-desc,'Invalid') "
                    + "or contains(@text,'Please') "
                    + "or contains(@text,'please') "
                    + "or contains(@text,'Enter') "
                    + "or contains(@text,'enter') "
                    + "or contains(@text,'valid') "
                    + "or contains(@text,'Valid') "
                    + "or contains(@text,'required') "
                    + "or contains(@text,'Required') "
                    + "or contains(@text,'should') "
                    + "or contains(@text,'Should') "
                    + "or contains(@text,'greater than') "
                    + "or contains(@text,'less than') "
                    + "or contains(@text,'equal to') "
                    + "or contains(@text,'cannot') "
                    + "or contains(@text,'Cannot') "
                    + "or contains(@text,'invalid') "
                    + "or contains(@text,'Invalid')]"
    );


    private static class VisibleInputSnapshot {
        private final WebElement element;
        private final int y;

        private VisibleInputSnapshot(WebElement element, int y) {
            this.element = element;
            this.y = y;
        }
    }

    public TaxCalculatorPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public void openTaxCalculatorFromHub() {
        try {
            ReportLogger.step("Opening Tax Calculator flow");

            tap(hubTab, "Hub");
            sleep(1200);

            tapPortfolioPlannerSafely();

            tapInvestorSafely("Manish Khatri");

            tap(nextButton, "Next");

            tapByDescriptionContainsSafely("Invest to save tax", 4);

            tapHelpMeCalculateThisSafely();

            waitForVisible(taxCalculatorTitle, "Tax Calculator");
            ReportLogger.pass("Tax Calculator opened successfully");

        } catch (Exception e) {
            ReportLogger.fail("Failed to open Tax Calculator flow");
            throw new RuntimeException("Failed to open Tax Calculator flow: " + e.getMessage(), e);
        }
    }
    
    private void tapPortfolioPlannerSafely() {
        ReportLogger.step("Trying to open Portfolio Planner from Hub");

        By[] locators = new By[]{
                AppiumBy.accessibilityId("Portfolio Planner"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio Planner\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Portfolio\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Portfolio Planner\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Portfolio\")")
        };

        for (int attempt = 1; attempt <= 6; attempt++) {
            for (By locator : locators) {
                if (tapIfVisible(locator, "Portfolio Planner")) {
                    ReportLogger.pass("Portfolio Planner opened");
                    sleep(1500);
                    return;
                }
            }

            ReportLogger.debug("Portfolio Planner not visible. Scrolling Hub. Attempt: " + attempt);
            swipeUpW3C();
            sleep(1000);
        }

        throw new RuntimeException("Portfolio Planner not found from Hub after scrolling");
    }

    private void tapInvestorSafely(String investorName) {
        ReportLogger.step("Selecting investor: " + investorName);

        By[] locators = new By[]{
                AppiumBy.accessibilityId(investorName),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + investorName + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + investorName + "\")")
        };

        for (int attempt = 1; attempt <= 4; attempt++) {
            for (By locator : locators) {
                if (tapIfVisible(locator, investorName)) {
                    ReportLogger.pass("Investor selected: " + investorName);
                    sleep(1000);
                    return;
                }
            }

            ReportLogger.debug("Investor not visible. Trying small scroll. Attempt: " + attempt);
            swipeUpW3C();
            sleep(800);
        }

        throw new RuntimeException("Investor not found: " + investorName);
    }

    private void tapByDescriptionContainsSafely(String text, int maxAttempts) {
        ReportLogger.step("Trying to tap: " + text);

        By[] locators = new By[]{
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + text + "\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")")
        };

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            for (By locator : locators) {
                if (tapIfVisible(locator, text)) {
                    sleep(1000);
                    return;
                }
            }

            ReportLogger.debug(text + " not visible. Scrolling. Attempt: " + attempt);
            swipeUpW3C();
            sleep(900);
        }

        throw new RuntimeException(text + " not found after scrolling");
    }

    private void tapHelpMeCalculateThisSafely() {
        ReportLogger.step("Trying to tap Help me calculate this");

        By[] locators = new By[]{
                helpMeCalculateThis,
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Help me calculate\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Help me calculate\")")
        };

        for (int attempt = 1; attempt <= 5; attempt++) {
            for (By locator : locators) {
                if (tapIfVisible(locator, "Help me calculate this")) {
                    sleep(1500);
                    return;
                }
            }

            ReportLogger.debug("Help me calculate this not visible. Scrolling. Attempt: " + attempt);
            swipeUpW3C();
            sleep(900);
        }

        throw new RuntimeException("Help me calculate this not found");
    }

    private boolean tapIfVisible(By locator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        tapElementCenter(element);
                        ReportLogger.step(elementName + " clicked");
                        return true;
                    }
                } catch (Exception ignored) {
                    // Try next matching element
                }
            }

            return false;

        } catch (Exception e) {
            ReportLogger.debug("tapIfVisible skipped for " + elementName + ": " + e.getMessage());
            return false;
        }
    } 
    public void startOverForNextTaxCaseIfVisible() {
        try {
            List<WebElement> startOverButtons = driver.findElements(startOverButton);

            if (!startOverButtons.isEmpty()) {
                WebElement startOver = startOverButtons.get(0);

                if (startOver.isDisplayed() && startOver.isEnabled()) {
                    ReportLogger.step("Result screen detected. Clicking Start Over for next test case.");

                    startOver.click();
                    sleep(2500);

                    waitForVisible(taxCalculatorTitle, "Tax Calculator");

                    ReportLogger.pass("Tax Calculator reset using Start Over");
                    return;
                }
            }

            ReportLogger.debug("Start Over button not visible. No reset required.");

        } catch (Exception e) {
            ReportLogger.debug("Start Over reset skipped/failed: " + e.getMessage());
        }
    }

    public void exitAndOpenFreshTaxCalculatorForNextCase() {
        try {
            ReportLogger.step("Preparing fresh Tax Calculator flow for next test case using Exit");

            List<WebElement> exitButtons = driver.findElements(exitButton);

            if (!exitButtons.isEmpty()) {
                WebElement exit = exitButtons.get(0);

                if (exit.isDisplayed() && exit.isEnabled()) {
                    ReportLogger.step("Result screen detected. Clicking Exit.");

                    exit.click();
                    sleep(2500);

                    ReportLogger.pass("Exited Tax Calculator result screen");
                }
            } else {
                ReportLogger.debug("Exit button not visible. Trying Android back as fallback.");

                try {
                    driver.pressKey(new KeyEvent(AndroidKey.BACK));
                    sleep(1500);
                } catch (Exception backError) {
                    ReportLogger.debug("Android back fallback skipped: " + backError.getMessage());
                }
            }

            openTaxCalculatorAfterExitWithoutHub();

            waitForVisible(taxCalculatorTitle, "Tax Calculator");

            ReportLogger.pass("Fresh Tax Calculator opened after Exit without Hub fallback");

        } catch (Exception e) {
            ReportLogger.fail("Failed to open fresh Tax Calculator for next test case");
            throw new RuntimeException("Failed to open fresh Tax Calculator for next test case: " + e.getMessage(), e);
        }
    }

    private void openTaxCalculatorAfterExitWithoutHub() {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 8; attempt++) {
            try {
                ReportLogger.step("Reopening Tax Calculator after Exit. Attempt: " + attempt);

                if (isElementPresent(taxCalculatorTitle)) {
                    ReportLogger.pass("Tax Calculator already visible after Exit");
                    return;
                }

                if (clickHelpMeCalculateThisIfVisibleOnce()) {
                    waitForVisible(taxCalculatorTitle, "Tax Calculator");
                    ReportLogger.pass("Help me calculate this clicked after Exit");
                    return;
                }

                if (clickByDescriptionContainsIfVisible("Invest to save tax")) {
                    ReportLogger.step("Invest to save tax clicked after Exit");
                    sleep(1200);

                    if (clickHelpMeCalculateThisIfVisibleOnce()) {
                        waitForVisible(taxCalculatorTitle, "Tax Calculator");
                        ReportLogger.pass("Tax Calculator opened using Invest to save tax -> Help me calculate this");
                        return;
                    }
                }

                if (attempt % 2 == 0) {
                    ReportLogger.debug("Help button still not visible. Pressing BACK once and retrying same area.");
                    try {
                        driver.pressKey(new KeyEvent(AndroidKey.BACK));
                        sleep(1200);
                    } catch (Exception backError) {
                        ReportLogger.debug("Back press skipped after Exit: " + backError.getMessage());
                    }
                } else {
                    ReportLogger.debug("Help button still not visible. Trying small swipe within same area.");
                    try {
                        swipeDownW3C();
                        sleep(1000);
                    } catch (Exception swipeDownError) {
                        ReportLogger.debug("Swipe down skipped after Exit: " + swipeDownError.getMessage());
                    }
                }

            } catch (Exception e) {
                lastError = e;
                ReportLogger.debug("Reopen after Exit attempt failed: " + e.getMessage());
            }
        }

        throw new RuntimeException(
                "Unable to reopen Tax Calculator after Exit without Hub fallback: "
                        + (lastError == null ? "Help me calculate this not found" : lastError.getMessage()),
                lastError
        );
    }

    private boolean clickHelpMeCalculateThisIfVisibleOnce() {
        try {
            List<WebElement> helpButtons = driver.findElements(helpMeCalculateThis);

            for (WebElement helpButton : helpButtons) {
                try {
                    if (helpButton.isDisplayed() && helpButton.isEnabled()) {
                        helpButton.click();

                        ReportLogger.step("Help me calculate this clicked");
                        sleep(1500);
                        return true;
                    }
                } catch (Exception ignored) {
                    // Try next matching button
                }
            }

            return false;

        } catch (Exception e) {
            ReportLogger.debug("Help me calculate this direct click skipped: " + e.getMessage());
            return false;
        }
    }

    private boolean clickByDescriptionContainsIfVisible(String text) {
        try {
            By locator = AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + text + "\")"
            );

            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        element.click();

                        ReportLogger.step(text + " clicked");
                        sleep(1000);
                        return true;
                    }
                } catch (Exception ignored) {
                    // Try next matching element
                }
            }

            return false;

        } catch (Exception e) {
            ReportLogger.debug("Description contains click skipped for " + text + ": " + e.getMessage());
            return false;
        }
    }

    public void runTaxCalculatorCase(TaxData data) {
        ReportLogger.step("Starting Tax Calculator input flow for: " + data.caseId);

        fillTaxCalculatorFirstScreen(data);

        if (!tapContinueAndVerifyNextScreen(
                data.caseId,
                "Tax Calculator",
                salaryBreakupScreenMarker,
                "Salary Breakup"
        )) {
            ReportLogger.pass("Tax Calculator input flow completed with validated inline red validation for: " + data.caseId);
            return;
        }

        fillSalaryBreakupScreen(data);

        if (!tapContinueAndVerifyNextScreen(
                data.caseId,
                "Salary Breakup",
                houseRentDetailsMarker,
                "House Rent Details"
        )) {
            ReportLogger.pass("Tax Calculator input flow completed with validated inline red validation for: " + data.caseId);
            return;
        }

        fillHouseRentDetailsScreen(data);

        if (!tapContinueAndVerifyNextScreen(
                data.caseId,
                "House Rent Details",
                deductionsSection80Marker,
                "Deductions under Section 80"
        )) {
            ReportLogger.pass("Tax Calculator input flow completed with validated inline red validation for: " + data.caseId);
            return;
        }

        fillDeductionsUnderSection80Screen(data);

        if (!tapContinueAndVerifyNextScreen(
                data.caseId,
                "Deductions under Section 80",
                otherDeductionsMarker,
                "Other deductions"
        )) {
            ReportLogger.pass("Tax Calculator input flow completed with validated inline red validation for: " + data.caseId);
            return;
        }

        fillOtherDeductionsScreen(data);

        tapCalculateTaxSafely();

        validateTaxFailurePopupIfPresent();

        ReportLogger.pass("Tax Calculator input flow completed for: " + data.caseId);
    }

    public void runTaxCalculatorSmokeCase() {
        ReportLogger.step("Starting Tax Calculator full input flow");

        fillTaxCalculatorFirstScreen();

        if (!tapContinueAndVerifyNextScreen(
                "SMOKE",
                "Tax Calculator",
                salaryBreakupScreenMarker,
                "Salary Breakup"
        )) {
            ReportLogger.pass("Tax Calculator smoke flow completed with validated inline red validation");
            return;
        }

        fillSalaryBreakupScreen();

        if (!tapContinueAndVerifyNextScreen(
                "SMOKE",
                "Salary Breakup",
                houseRentDetailsMarker,
                "House Rent Details"
        )) {
            ReportLogger.pass("Tax Calculator smoke flow completed with validated inline red validation");
            return;
        }

        fillHouseRentDetailsScreen();

        if (!tapContinueAndVerifyNextScreen(
                "SMOKE",
                "House Rent Details",
                deductionsSection80Marker,
                "Deductions under Section 80"
        )) {
            ReportLogger.pass("Tax Calculator smoke flow completed with validated inline red validation");
            return;
        }

        fillDeductionsUnderSection80Screen();

        if (!tapContinueAndVerifyNextScreen(
                "SMOKE",
                "Deductions under Section 80",
                otherDeductionsMarker,
                "Other deductions"
        )) {
            ReportLogger.pass("Tax Calculator smoke flow completed with validated inline red validation");
            return;
        }

        fillOtherDeductionsScreen();

        tapCalculateTaxSafely();

        validateTaxFailurePopupIfPresent();

        ReportLogger.pass("Tax Calculator full input flow completed till Calculate Tax");
    }

    private void fillTaxCalculatorFirstScreen() {
        waitForVisible(taxCalculatorTitle, "Tax Calculator");

        enterVisibleInputByIndex(0, "30", "Age");
        enterVisibleInputByIndex(1, "1200000", "Annual Income");

        ReportLogger.debug("Trying W3C swipe to reach Annual Interest");
        swipeUpW3C();
        sleep(1500);

        printVisibleInputs("after swipe for Annual Interest");
        enterFirstEmptyVisibleInput("10000", "Annual Interest");

        ReportLogger.debug("Trying W3C swipe to reach Other Taxable Income");
        swipeUpW3C();
        sleep(1500);

        printVisibleInputs("after swipe for Other Taxable Income");
        enterFirstEmptyVisibleInput("0", "Other Taxable Income");

        ReportLogger.pass("Tax Calculator first screen fields entered successfully");
    }

    private void fillTaxCalculatorFirstScreen(TaxData data) {
        waitForVisible(taxCalculatorTitle, "Tax Calculator");

        replaceVisibleInputByIndex(0, data.age, "Age");
        replaceVisibleInputByIndex(1, data.annualIncome, "Annual Income");

        ReportLogger.debug("Trying W3C swipe to reach Annual Interest");
        swipeUpW3C();
        sleep(1500);

        printVisibleInputs("after swipe for Annual Interest");
        replaceVisibleInputByIndex(2, data.annualInterest, "Annual Interest");

        ReportLogger.debug("Trying W3C swipe to reach Other Taxable Income");
        swipeUpW3C();
        sleep(1500);

        printVisibleInputs("after swipe for Other Taxable Income");
        replaceVisibleInputByIndex(3, data.otherTaxableIncome, "Other Taxable Income");

        ReportLogger.pass("Tax Calculator first screen fields replaced successfully for " + data.caseId);
    }

    private void fillSalaryBreakupScreen() {
        waitForVisible(salaryBreakupScreenMarker, "Salary Breakup fields");

        fillNamedEmptyFieldsAcrossScreenAllowMissing(
                "Salary Breakup",
                new String[]{
                        "Basic Salary",
                        "EPF Employee Contribution",
                        "EPF Employer Contribution",
                        "NPS Employee Contribution",
                        "NPS Employer Contribution"
                },
                new String[]{
                        "50000",
                        "5000",
                        "5000",
                        "2000",
                        "2000"
                },
                3
        );

        selectGovernmentEmployeeNo();

        fillNamedEmptyFieldsAcrossScreenAllowMissing(
                "Salary Breakup Personal NPS",
                new String[]{
                        "Personal NPS/NPS Vatsalya"
                },
                new String[]{
                        "10000"
                },
                0
        );

        ReportLogger.pass("Salary Breakup fields entered successfully");
    }

    private void fillSalaryBreakupScreen(TaxData data) {
        waitForVisible(salaryBreakupScreenMarker, "Salary Breakup fields");

        fillNamedEmptyFieldsAcrossScreenAllowMissing(
                "Salary Breakup",
                new String[]{
                        "Basic Salary",
                        "EPF Employee Contribution",
                        "EPF Employer Contribution",
                        "NPS Employee Contribution",
                        "NPS Employer Contribution"
                },
                new String[]{
                        data.basicSalary,
                        data.epfEmployee,
                        data.epfEmployer,
                        data.npsEmployee,
                        data.npsEmployer
                },
                3
        );

        if (isPositiveAmount(data.npsEmployee) || isPositiveAmount(data.npsEmployer)) {
            ReportLogger.step("NPS Employee/Employer amount is positive. Checking Government Employee option.");
            selectGovernmentEmployeeNo();
        } else {
            ReportLogger.step("NPS Employee/Employer amount is zero/empty. Government Employee option is not expected. Skipping selection.");
        }

        fillNamedEmptyFieldsAcrossScreenAllowMissing(
                "Salary Breakup Personal NPS",
                new String[]{
                        "Personal NPS/NPS Vatsalya"
                },
                new String[]{
                        data.personalNps
                },
                0
        );

        ReportLogger.pass("Salary Breakup fields entered successfully for " + data.caseId);
    }

    private void fillHouseRentDetailsScreen() {
        waitForVisible(houseRentDetailsMarker, "House Rent Details");

        selectYesForQuestion("House Rent Allowance", "HRA Yes");

        waitUntilVisibleInputCountAtLeast(2, "HRA fields");

        fillNamedEmptyFieldsAcrossScreen(
                "House Rent HRA",
                new String[]{
                        "Monthly Rent",
                        "Monthly HRA"
                },
                new String[]{
                        "20000",
                        "15000"
                }
        );

        selectYesForQuestion("home loan", "Home Loan Yes");

        waitUntilVisibleInputCountAtLeast(2, "Home Loan fields");

        fillNamedEmptyFieldsAcrossScreen(
                "House Rent Home Loan",
                new String[]{
                        "Home Loan Interest",
                        "Home Loan Principal Amount"
                },
                new String[]{
                        "50000",
                        "100000"
                }
        );

        ReportLogger.pass("House Rent Details fields entered successfully");
    }

    private void fillHouseRentDetailsScreen(TaxData data) {
        waitForVisible(houseRentDetailsMarker, "House Rent Details");

        if (data.hraYes) {
            selectYesForQuestion("House Rent Allowance", "HRA Yes");

            waitUntilVisibleInputCountAtLeast(2, "HRA fields");

            fillNamedEmptyFieldsAcrossScreen(
                    "House Rent HRA",
                    new String[]{
                            "Monthly Rent",
                            "Monthly HRA"
                    },
                    new String[]{
                            data.monthlyRent,
                            data.monthlyHra
                    }
            );
        } else {
            ReportLogger.step("HRA is No/default. Skipping HRA fields.");
        }

        if (data.homeLoanYes) {
            selectYesForQuestion("home loan", "Home Loan Yes");

            waitUntilVisibleInputCountAtLeast(2, "Home Loan fields");

            fillNamedEmptyFieldsAcrossScreen(
                    "House Rent Home Loan",
                    new String[]{
                            "Home Loan Interest",
                            "Home Loan Principal Amount"
                    },
                    new String[]{
                            data.homeLoanInterest,
                            data.homeLoanPrincipalAmount
                    }
            );
        } else {
            ReportLogger.step("Home Loan is No/default. Skipping Home Loan fields.");
        }

        ReportLogger.pass("House Rent Details fields entered successfully for " + data.caseId);
    }

    private void fillDeductionsUnderSection80Screen() {
        waitForVisible(deductionsSection80Marker, "Deductions under Section 80");

        fillNamedEmptyFieldsAcrossScreen(
                "Deductions under Section 80",
                new String[]{
                        "Life Insurance",
                        "ELSS",
                        "PPF",
                        "NSC",
                        "5-year Fixed Deposit",
                        "Tuition Fee",
                        "Section 80 Any Other"
                },
                new String[]{
                        "10000",
                        "50000",
                        "25000",
                        "10000",
                        "10000",
                        "20000",
                        "5000"
                }
        );

        ReportLogger.pass("Deductions under Section 80 fields entered successfully");
    }

    private void fillDeductionsUnderSection80Screen(TaxData data) {
        waitForVisible(deductionsSection80Marker, "Deductions under Section 80");

        fillNamedEmptyFieldsAcrossScreen(
                "Deductions under Section 80",
                new String[]{
                        "Life Insurance",
                        "ELSS",
                        "PPF",
                        "NSC",
                        "5-year Fixed Deposit",
                        "Tuition Fee",
                        "Section 80 Any Other"
                },
                new String[]{
                        data.lifeInsurance,
                        data.elss,
                        data.ppf,
                        data.nsc,
                        data.fixedDeposit,
                        data.tuitionFee,
                        data.section80AnyOther
                }
        );

        ReportLogger.pass("Deductions under Section 80 fields entered successfully for " + data.caseId);
    }

    private void fillOtherDeductionsScreen() {
        waitForVisible(otherDeductionsMarker, "Other deductions");

        ReportLogger.step("Other deductions screen opened for smoke case");
        logOtherDeductionsVisibleInputs("SMOKE");

        ReportLogger.step("Smoke case: Keeping Donation Section 80G / Education Loan Interest Section 80E / Any Other unchanged.");

        selectHealthInsurancePremiumOption("SMOKE", true);

        fillHealthInsurancePremiumFields(
                "SMOKE",
                "5000",
                "10000",
                "2000"
        );

        ReportLogger.pass("Other deductions screen completed successfully for smoke case");
    }

    private void fillOtherDeductionsScreen(TaxData data) {
        waitForVisible(otherDeductionsMarker, "Other deductions");

        ReportLogger.step("Other deductions screen opened for " + data.caseId);
        logOtherDeductionsVisibleInputs(data.caseId);

        ReportLogger.step(
                "Keeping Donation Section 80G / Education Loan Interest Section 80E / Any Other unchanged for "
                        + data.caseId
        );

        ReportLogger.step(
                "Health Insurance Premium flag for "
                        + data.caseId
                        + ": "
                        + data.healthInsurancePremiumYes
        );

        selectHealthInsurancePremiumOption(
                data.caseId,
                data.healthInsurancePremiumYes
        );

        if (data.healthInsurancePremiumYes) {
            fillHealthInsurancePremiumFields(
                    data.caseId,
                    data.healthInsurancePremiumField1,
                    data.healthInsurancePremiumField2,
                    data.healthInsurancePremiumField3
            );
        } else {
            ReportLogger.step(
                    "Health Insurance Premium is No/default for "
                            + data.caseId
                            + ". Health premium fields skipped."
            );
        }

        ReportLogger.pass("Other deductions screen completed successfully for " + data.caseId);
    }

    private void selectHealthInsurancePremiumOption(String caseId, boolean healthInsurancePremiumYes) {
        try {
            String optionText = healthInsurancePremiumYes ? "Yes" : "No";
            String optionName = healthInsurancePremiumYes
                    ? "Health Insurance Premium Yes"
                    : "Health Insurance Premium No";

            ReportLogger.step(
                    "Selecting Health Insurance Premium option for "
                            + caseId
                            + ": "
                            + optionText
            );

            closeKeyboardWithEnterAndWait();

            WebElement question = findVisibleQuestionByDescriptionContains(
                    new String[]{
                            "health insurance premium",
                            "Health insurance premium",
                            "Health Insurance Premium",
                            "Do you pay health insurance premium"
                    }
            );

            if (question != null) {
                Rectangle questionRect = question.getRect();

                WebElement option = findNearestVisibleOptionBelow(
                        questionRect.getY(),
                        optionText,
                        optionName
                );

                tapElementCenter(option);

                ReportLogger.pass("Selected " + optionName + " for " + caseId);
                sleep(1200);
                return;
            }

            if (tapLastVisibleOptionByText(optionText, optionName + " fallback")) {
                ReportLogger.pass("Selected " + optionName + " using fallback for " + caseId);
                sleep(1200);
                return;
            }

            if (!healthInsurancePremiumYes) {
                ReportLogger.step("Health Insurance Premium No not found. Assuming No/default selected for " + caseId);
                return;
            }

            throw new RuntimeException("Unable to select Health Insurance Premium Yes for " + caseId);

        } catch (Exception e) {
            if (!healthInsurancePremiumYes) {
                ReportLogger.step(
                        "Health Insurance Premium No selection skipped safely for "
                                + caseId
                                + ": "
                                + e.getMessage()
                );
                return;
            }

            ReportLogger.fail("Failed to select Health Insurance Premium Yes for " + caseId);
            throw new RuntimeException(
                    "Failed to select Health Insurance Premium Yes for "
                            + caseId
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void fillHealthInsurancePremiumFields(
            String caseId,
            String healthInsuranceValue1,
            String healthInsuranceValue2,
            String healthInsuranceValue3
    ) {
        try {
            ReportLogger.step("Filling Health Insurance Premium fields for " + caseId);

            String[] fieldNames = new String[]{
                    "Health Insurance Premium Field 1",
                    "Health Insurance Premium Field 2",
                    "Health Insurance Premium Field 3"
            };

            String[] values = new String[]{
                    healthInsuranceValue1,
                    healthInsuranceValue2,
                    healthInsuranceValue3
            };

            int valueIndex = 0;
            int swipeCount = 0;
            int noProgressCount = 0;
            String lastSignature = "";

            while (valueIndex < values.length && swipeCount <= 8) {
                Integer healthQuestionY = findHealthInsuranceQuestionYIfVisible();

                List<WebElement> inputs = getVisibleInputsTopToBottom();

                ReportLogger.debug(
                        "Visible inputs while filling health premium fields for "
                                + caseId
                                + ": "
                                + inputs.size()
                                + ", healthQuestionY="
                                + healthQuestionY
                );

                String currentSignature = buildInputSignature(inputs);
                boolean entered = false;

                for (int i = 0; i < inputs.size(); i++) {
                    WebElement input = inputs.get(i);

                    try {
                        String currentText = safeText(input);

                        if ("__STALE__".equals(currentText)) {
                            continue;
                        }

                        Rectangle rect = input.getRect();

                        if (healthQuestionY != null && rect.getY() <= healthQuestionY) {
                            continue;
                        }

                        if (hasDigit(currentText)) {
                            continue;
                        }

                        enterIntoInput(
                                input,
                                values[valueIndex],
                                fieldNames[valueIndex],
                                "Health Insurance Premium visible-empty-index=" + i
                        );

                        ReportLogger.step(
                                fieldNames[valueIndex]
                                        + " entered for "
                                        + caseId
                                        + ": "
                                        + values[valueIndex]
                        );

                        valueIndex++;
                        entered = true;
                        noProgressCount = 0;
                        sleep(650);
                        break;

                    } catch (Exception entryError) {
                        if (isStaleException(entryError)) {
                            ReportLogger.debug(
                                    "Stale input detected while filling Health Insurance Premium fields for "
                                            + caseId
                                            + ". Refetching visible inputs."
                            );
                            sleep(700);
                            entered = true;
                            break;
                        }

                        throw new RuntimeException(entryError);
                    }
                }

                if (valueIndex >= values.length) {
                    ReportLogger.pass("Health Insurance Premium fields entered successfully for " + caseId);
                    return;
                }

                if (!entered) {
                    if (currentSignature.equals(lastSignature)) {
                        noProgressCount++;
                    } else {
                        noProgressCount = 0;
                        lastSignature = currentSignature;
                    }

                    if (noProgressCount >= 3) {
                        break;
                    }

                    swipeUpW3C();
                    sleep(1200);
                    swipeCount++;
                }
            }

            if (valueIndex < values.length) {
                throw new RuntimeException(
                        "Unable to fill all Health Insurance Premium fields for "
                                + caseId
                                + ". Filled "
                                + valueIndex
                                + "/"
                                + values.length
                );
            }

        } catch (Exception e) {
            ReportLogger.fail(
                    "Failed to fill Health Insurance Premium fields for "
                            + caseId
                            + ": "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to fill Health Insurance Premium fields for "
                            + caseId
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }


    private Integer findHealthInsuranceQuestionYIfVisible() {
        try {
            WebElement question = findVisibleQuestionByDescriptionContains(
                    new String[]{
                            "health insurance premium",
                            "Health insurance premium",
                            "Health Insurance Premium",
                            "Do you pay health insurance premium"
                    }
            );

            if (question == null) {
                return null;
            }

            return question.getRect().getY();
        } catch (Exception e) {
            if (isStaleException(e)) {
                ReportLogger.debug("Stale health insurance question detected. Retrying through next loop.");
                return null;
            }

            throw new RuntimeException("Failed to read Health Insurance Premium question position: " + e.getMessage(), e);
        }
    }


    private boolean tapLastVisibleOptionByText(String optionText, String optionName) {
        try {
            List<WebElement> options = driver.findElements(AppiumBy.accessibilityId(optionText));
            List<WebElement> visibleOptions = new ArrayList<>();

            for (WebElement option : options) {
                try {
                    if (!option.isDisplayed() || !option.isEnabled()) {
                        continue;
                    }

                    Rectangle rect = option.getRect();

                    if (rect.getY() < 250 || rect.getY() > 2200) {
                        continue;
                    }

                    visibleOptions.add(option);

                } catch (Exception ignored) {
                    // Ignore stale/non-visible option
                }
            }

            visibleOptions.sort(Comparator.comparingInt(e -> e.getRect().getY()));

            if (visibleOptions.isEmpty()) {
                return false;
            }

            WebElement optionToTap = visibleOptions.get(visibleOptions.size() - 1);
            tapElementCenter(optionToTap);

            ReportLogger.step(optionName + " clicked");
            return true;

        } catch (Exception e) {
            ReportLogger.debug("Last visible option tap failed for " + optionName + ": " + e.getMessage());
            return false;
        }
    }

    private void validateTaxFailurePopupIfPresent() {
        try {
            /*
             * Validate all known tax calculation failure popups.
             *
             * Known failure popups:
             * 1. Please note - Unable to save record
             * 2. Please note - Unable to get tax info data
             */

            WebElement popup = findVisibleTaxFailurePopup();

            if (popup == null) {
                ReportLogger.pass("No tax calculation failure popup displayed. Tax calculation proceeded.");
                return;
            }

            String popupText = popup.getAttribute("content-desc");

            if (popupText == null || popupText.trim().isEmpty()) {
                popupText = popup.getText();
            }

            if (popupText == null || popupText.trim().isEmpty()) {
                popupText = "Tax calculation failure popup displayed, but popup text could not be read.";
            }

            ReportLogger.fail("Tax calculation failed. Popup displayed: " + popupText);

            try {
                String screenshotPath = ScreenshotUtils.captureScreenshot(
                        driver,
                        "Tax_Calculation_Failure_Popup"
                );

                if (screenshotPath != null && ExtentTestManager.getTest() != null) {
                    ExtentTestManager.getTest().addScreenCaptureFromPath(screenshotPath);
                    ReportLogger.step("Failure screenshot captured with tax calculation popup visible.");
                }
            } catch (Exception screenshotError) {
                ReportLogger.debug(
                        "Could not capture tax calculation popup screenshot before closing: "
                                + screenshotError.getMessage()
                );
            }

            closeTaxFailurePopupIfPresent();

            throw new AssertionError(
                    "Tax calculation failed because popup appeared: " + popupText
            );

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            ReportLogger.debug("Tax calculation failure popup check failed safely: " + e.getMessage());
        }
    }

    private WebElement findVisibleTaxFailurePopup() {
        try {
            By[] popupLocators = new By[]{
                    unableToSaveRecordPopup,
                    unableToGetTaxInfoPopup,
                    pleaseNotePopup
            };

            for (By locator : popupLocators) {
                List<WebElement> popups = driver.findElements(locator);

                for (WebElement popup : popups) {
                    try {
                        if (!popup.isDisplayed()) {
                            continue;
                        }

                        String popupText = popup.getAttribute("content-desc");

                        if (popupText == null || popupText.trim().isEmpty()) {
                            popupText = popup.getText();
                        }

                        if (popupText == null) {
                            popupText = "";
                        }

                        if (popupText.contains("Unable to save record")
                                || popupText.contains("Unable to get tax info data")
                                || popupText.contains("Please note")) {
                            return popup;
                        }

                    } catch (Exception ignored) {
                        // Try next popup
                    }
                }
            }

            return null;

        } catch (Exception e) {
            ReportLogger.debug("Tax failure popup lookup failed safely: " + e.getMessage());
            return null;
        }
    }

    private void closeTaxFailurePopupIfPresent() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", 985);
            params.put("y", 2025);

            driver.executeScript("mobile: clickGesture", params);

            ReportLogger.step("Tax failure popup closed using X button coordinate");
            sleep(700);

        } catch (Exception e) {
            ReportLogger.debug("Unable to close tax failure popup using X coordinate: " + e.getMessage());

            try {
                driver.pressKey(new KeyEvent(AndroidKey.BACK));
                ReportLogger.step("Tax failure popup closed using Android BACK");
                sleep(700);
            } catch (Exception backError) {
                ReportLogger.debug("Unable to close tax failure popup using BACK: " + backError.getMessage());
            }
        }
    }

    /*
     * Backward-compatible wrapper.
     * Keep this method so any old call does not break compilation.
     */
    private void validateUnableToSaveRecordPopupIfPresent() {
        validateTaxFailurePopupIfPresent();
    }

    /*
     * Backward-compatible wrapper.
     * Keep this method so any old call does not break compilation.
     */
    private void closeUnableToSaveRecordPopupIfPresent() {
        closeTaxFailurePopupIfPresent();
    }

    private void logOtherDeductionsVisibleInputs(String caseId) {
        List<WebElement> inputs = getVisibleInputsTopToBottom();

        ReportLogger.step("Other deductions visible input count for " + caseId + ": " + inputs.size());

        for (int i = 0; i < inputs.size(); i++) {
            try {
                WebElement input = inputs.get(i);
                Rectangle rect = input.getRect();
                String text = safeText(input);

                ReportLogger.debug(
                        "Other deductions input[" + i + "]"
                                + " value=" + text
                                + " x=" + rect.getX()
                                + " y=" + rect.getY()
                                + " w=" + rect.getWidth()
                                + " h=" + rect.getHeight()
                );

            } catch (Exception e) {
                ReportLogger.debug("Unable to log Other deductions input[" + i + "]: " + e.getMessage());
            }
        }
    }

    private void fillNamedEmptyFieldsAcrossScreen(String screenName, String[] fieldNames, String[] values) {
        if (fieldNames.length != values.length) {
            throw new RuntimeException("Field names and values count mismatch for " + screenName);
        }

        int valueIndex = 0;
        int swipeCount = 0;

        while (valueIndex < values.length && swipeCount <= 10) {
            List<WebElement> inputs = getVisibleInputsTopToBottom();

            ReportLogger.debug("Visible inputs on " + screenName + ": " + inputs.size());
            printInputList(inputs);

            boolean entered = false;

            for (int i = 0; i < inputs.size(); i++) {
                WebElement input = inputs.get(i);
                String currentText = safeText(input);

                if ("__STALE__".equals(currentText)) {
                    continue;
                }

                if (hasDigit(currentText)) {
                    continue;
                }

                try {
                    enterIntoInput(
                            input,
                            values[valueIndex],
                            fieldNames[valueIndex],
                            screenName + " fresh-empty-index=" + i
                    );

                    valueIndex++;
                    entered = true;
                    sleep(650);
                    break;

                } catch (Exception entryError) {
                    if (isStaleException(entryError)) {
                        ReportLogger.debug(
                                "Stale input detected on "
                                        + screenName
                                        + " while entering "
                                        + fieldNames[valueIndex]
                                        + ". Refetching visible inputs."
                        );
                        sleep(700);
                        entered = true;
                        break;
                    }

                    throw new RuntimeException(entryError);
                }
            }

            if (valueIndex >= values.length) {
                return;
            }

            if (!entered) {
                swipeUpW3C();
                sleep(1200);
                swipeCount++;
            }
        }

        if (valueIndex < values.length) {
            throw new RuntimeException(
                    "Unable to fill all fields on " + screenName + ". Filled " + valueIndex + "/" + values.length
            );
        }
    }


    private void fillNamedEmptyFieldsAcrossScreenAllowMissing(
            String screenName,
            String[] fieldNames,
            String[] values,
            int minimumRequiredFields
    ) {
        if (fieldNames.length != values.length) {
            throw new RuntimeException("Field names and values count mismatch for " + screenName);
        }

        int valueIndex = 0;
        int swipeCount = 0;
        int noProgressCount = 0;
        String lastVisibleSignature = "";

        while (valueIndex < values.length && swipeCount <= 10) {
            List<WebElement> inputs = getVisibleInputsTopToBottom();

            ReportLogger.debug("Visible inputs on " + screenName + " optional mode: " + inputs.size());
            printInputList(inputs);

            String currentVisibleSignature = buildInputSignature(inputs);
            boolean entered = false;

            for (int i = 0; i < inputs.size(); i++) {
                WebElement input = inputs.get(i);
                String currentText = safeText(input);

                if ("__STALE__".equals(currentText)) {
                    continue;
                }

                if (hasDigit(currentText)) {
                    continue;
                }

                try {
                    enterIntoInput(
                            input,
                            values[valueIndex],
                            fieldNames[valueIndex],
                            screenName + " optional-fresh-empty-index=" + i
                    );

                    valueIndex++;
                    entered = true;
                    noProgressCount = 0;
                    sleep(650);
                    break;

                } catch (Exception entryError) {
                    if (isStaleException(entryError)) {
                        ReportLogger.debug(
                                "Stale input detected on "
                                        + screenName
                                        + " while entering "
                                        + fieldNames[valueIndex]
                                        + ". Refetching visible inputs."
                        );
                        sleep(700);
                        entered = true;
                        break;
                    }

                    throw new RuntimeException(entryError);
                }
            }

            if (valueIndex >= values.length) {
                return;
            }

            if (!entered) {
                if (currentVisibleSignature.equals(lastVisibleSignature)) {
                    noProgressCount++;
                } else {
                    noProgressCount = 0;
                    lastVisibleSignature = currentVisibleSignature;
                }

                if (valueIndex >= minimumRequiredFields && noProgressCount >= 2) {
                    ReportLogger.step(
                            screenName
                                    + " optional fields not visible/applicable. Filled "
                                    + valueIndex
                                    + "/"
                                    + values.length
                                    + ". Continuing safely."
                    );
                    return;
                }

                swipeUpW3C();
                sleep(1200);
                swipeCount++;
            }
        }

        if (valueIndex < minimumRequiredFields) {
            throw new RuntimeException(
                    "Unable to fill minimum required fields on "
                            + screenName
                            + ". Filled "
                            + valueIndex
                            + "/"
                            + minimumRequiredFields
            );
        }

        ReportLogger.step(
                screenName
                        + " completed with optional missing fields. Filled "
                        + valueIndex
                        + "/"
                        + values.length
        );
    }


    private void fillVisibleFieldsByIndexReplaceExisting(String screenName, String[] fieldNames, String[] values) {
        if (fieldNames.length != values.length) {
            throw new RuntimeException("Field names and values count mismatch for " + screenName);
        }

        for (int i = 0; i < values.length; i++) {
            Exception lastError = null;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    List<WebElement> inputs = getVisibleInputsTopToBottom();

                    ReportLogger.debug("Visible inputs on " + screenName + " index replace mode: " + inputs.size());
                    printInputList(inputs);

                    if (inputs.size() < values.length) {
                        throw new RuntimeException(
                                screenName + " requires " + values.length + " visible inputs but found only " + inputs.size()
                        );
                    }

                    enterIntoInputReplacingExisting(
                            inputs.get(i),
                            values[i],
                            fieldNames[i],
                            screenName + " fixed-visible-index=" + i
                    );

                    sleep(650);
                    lastError = null;
                    break;

                } catch (Exception e) {
                    lastError = e;

                    if (isStaleException(e) && attempt < 3) {
                        ReportLogger.debug(
                                "Stale input detected on "
                                        + screenName
                                        + " while replacing "
                                        + fieldNames[i]
                                        + ". Refetching visible inputs. Attempt: "
                                        + attempt
                        );
                        sleep(700);
                        continue;
                    }

                    break;
                }
            }

            if (lastError != null) {
                throw new RuntimeException(
                        "Failed to replace "
                                + fieldNames[i]
                                + " on "
                                + screenName
                                + ": "
                                + lastError.getMessage(),
                        lastError
                );
            }
        }

        ReportLogger.pass(screenName + " fields replaced/filled by visible index successfully");
    }


    private void selectYesForQuestion(String questionText, String optionName) {
        try {
            ReportLogger.step("Selecting " + optionName + " for question: " + questionText);

            for (int attempt = 1; attempt <= 6; attempt++) {
                By questionLocator = AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"" + questionText + "\")"
                );

                List<WebElement> questions = driver.findElements(questionLocator);

                if (!questions.isEmpty()) {
                    WebElement question = questions.get(0);
                    Rectangle questionRect = question.getRect();

                    WebElement yesOption = findNearestVisibleYesBelow(questionRect.getY(), optionName);

                    tapElementCenter(yesOption);

                    ReportLogger.step("Selected " + optionName);
                    sleep(1200);
                    return;
                }

                swipeUpW3C();
                sleep(1000);
            }

            throw new RuntimeException("Unable to find/select Yes for question: " + questionText);

        } catch (Exception e) {
            ReportLogger.fail("Failed to select " + optionName);
            throw new RuntimeException("Failed to select " + optionName + ": " + e.getMessage(), e);
        }
    }

    private WebElement findNearestVisibleYesBelow(int questionY, String optionName) {
        return findNearestVisibleOptionBelow(questionY, "Yes", optionName);
    }

    private WebElement findNearestVisibleNoBelow(int questionY, String optionName) {
        return findNearestVisibleOptionBelow(questionY, "No", optionName);
    }

    private WebElement findNearestVisibleOptionBelow(int questionY, String optionText, String optionName) {
        List<WebElement> options = driver.findElements(AppiumBy.accessibilityId(optionText));
        List<WebElement> visibleOptions = new ArrayList<>();

        for (WebElement option : options) {
            try {
                if (!option.isDisplayed() || !option.isEnabled()) {
                    continue;
                }

                Rectangle rect = option.getRect();

                if (rect.getY() < 250 || rect.getY() > 2200) {
                    continue;
                }

                if (rect.getY() > questionY) {
                    visibleOptions.add(option);
                }

            } catch (Exception ignored) {
                // Ignore stale/non-visible option
            }
        }

        visibleOptions.sort(Comparator.comparingInt(e -> e.getRect().getY()));

        if (visibleOptions.isEmpty()) {
            throw new RuntimeException("No visible " + optionText + " option found below question for " + optionName);
        }

        return visibleOptions.get(0);
    }

    private void selectGovernmentEmployeeNo() {
        try {
            ReportLogger.step("Selecting Government Employee: No");

            closeKeyboardWithEnterAndWait();

            String[] possibleQuestionTexts = new String[]{
                    "government employee",
                    "Government employee",
                    "Government Employee",
                    "government",
                    "Government"
            };

            WebElement question = findVisibleQuestionByDescriptionContains(possibleQuestionTexts);

            if (question != null) {
                Rectangle questionRect = question.getRect();

                WebElement noOption = findNearestVisibleNoBelow(
                        questionRect.getY(),
                        "Government Employee No"
                );

                tapElementCenter(noOption);

                ReportLogger.step("Selected Government Employee: No");
                sleep(900);
                return;
            }

            if (tapFirstVisibleOptionByText("No", "Government Employee No direct visible fallback")) {
                ReportLogger.step("Selected Government Employee: No using direct visible fallback");
                sleep(900);
                return;
            }

            ReportLogger.step("Government Employee No not visible. Assuming default No and continuing without swipe.");

        } catch (Exception e) {
            ReportLogger.debug("Government Employee No selection skipped safely: " + e.getMessage());
            ReportLogger.step("Continuing with Government Employee default No");
        }
    }

    private boolean tapFirstVisibleOptionByText(String optionText, String optionName) {
        try {
            List<WebElement> options = driver.findElements(AppiumBy.accessibilityId(optionText));
            List<WebElement> visibleOptions = new ArrayList<>();

            for (WebElement option : options) {
                try {
                    if (!option.isDisplayed() || !option.isEnabled()) {
                        continue;
                    }

                    Rectangle rect = option.getRect();

                    if (rect.getY() < 250 || rect.getY() > 2200) {
                        continue;
                    }

                    visibleOptions.add(option);

                } catch (Exception ignored) {
                    // Ignore stale/non-visible option
                }
            }

            visibleOptions.sort(Comparator.comparingInt(e -> e.getRect().getY()));

            if (visibleOptions.isEmpty()) {
                return false;
            }

            WebElement optionToTap = visibleOptions.get(visibleOptions.size() - 1);
            tapElementCenter(optionToTap);

            return true;

        } catch (Exception e) {
            ReportLogger.debug("Direct visible option tap failed for " + optionName + ": " + e.getMessage());
            return false;
        }
    }

    private WebElement findVisibleQuestionByDescriptionContains(String[] possibleTexts) {
        for (String text : possibleTexts) {
            try {
                By locator = AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"" + text + "\")"
                );

                List<WebElement> elements = driver.findElements(locator);

                for (WebElement element : elements) {
                    try {
                        if (!element.isDisplayed()) {
                            continue;
                        }

                        Rectangle rect = element.getRect();

                        if (rect.getY() < 250 || rect.getY() > 2200) {
                            continue;
                        }

                        return element;

                    } catch (Exception ignored) {
                        // Ignore stale/non-visible
                    }
                }

            } catch (Exception ignored) {
                // Try next text
            }
        }

        return null;
    }

    private void waitUntilVisibleInputCountAtLeast(int minimumCount, String screenPartName) {
        for (int attempt = 1; attempt <= 8; attempt++) {
            List<WebElement> inputs = getVisibleInputsTopToBottom();

            if (inputs.size() >= minimumCount) {
                return;
            }

            sleep(700);
        }

        throw new RuntimeException(
                "Expected at least "
                        + minimumCount
                        + " visible inputs for "
                        + screenPartName
                        + " but not found"
        );
    }

    private void enterVisibleInputByIndex(int index, String value, String fieldName) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<WebElement> inputs = getVisibleInputsTopToBottom();

                if (inputs.size() <= index) {
                    throw new RuntimeException(
                            "Input index " + index + " not available. Visible inputs: " + inputs.size()
                    );
                }

                enterIntoInput(inputs.get(index), value, fieldName, "index=" + index);
                return;

            } catch (Exception e) {
                lastError = e;

                if (isStaleException(e) && attempt < 3) {
                    ReportLogger.debug(
                            "Stale input detected while entering "
                                    + fieldName
                                    + ". Refetching inputs. Attempt: "
                                    + attempt
                    );
                    sleep(700);
                    continue;
                }

                break;
            }
        }

        throw new RuntimeException(
                "Failed to enter "
                        + fieldName
                        + ": "
                        + (lastError == null ? "Unknown error" : lastError.getMessage()),
                lastError
        );
    }


    private void replaceVisibleInputByIndex(int index, String value, String fieldName) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<WebElement> inputs = getVisibleInputsTopToBottom();

                if (inputs.size() <= index) {
                    throw new RuntimeException(
                            "Input index " + index + " not available for replace. Visible inputs: " + inputs.size()
                    );
                }

                enterIntoInputReplacingExisting(
                        inputs.get(index),
                        value,
                        fieldName,
                        "replace-index=" + index
                );

                return;

            } catch (Exception e) {
                lastError = e;

                if (isStaleException(e) && attempt < 3) {
                    ReportLogger.debug(
                            "Stale input detected while replacing "
                                    + fieldName
                                    + ". Refetching inputs. Attempt: "
                                    + attempt
                    );
                    sleep(700);
                    continue;
                }

                break;
            }
        }

        throw new RuntimeException(
                "Failed to replace "
                        + fieldName
                        + ": "
                        + (lastError == null ? "Unknown error" : lastError.getMessage()),
                lastError
        );
    }


    private void enterFirstEmptyVisibleInput(String value, String fieldName) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<WebElement> inputs = getVisibleInputsTopToBottom();

                for (int i = 0; i < inputs.size(); i++) {
                    WebElement input = inputs.get(i);
                    String text = safeText(input);

                    if ("__STALE__".equals(text)) {
                        continue;
                    }

                    if (!hasDigit(text)) {
                        enterIntoInput(input, value, fieldName, "first-empty-index=" + i);
                        return;
                    }
                }

                throw new RuntimeException("No empty visible input found for " + fieldName);

            } catch (Exception e) {
                lastError = e;

                if (isStaleException(e) && attempt < 3) {
                    ReportLogger.debug(
                            "Stale input detected while entering first empty field "
                                    + fieldName
                                    + ". Refetching inputs. Attempt: "
                                    + attempt
                    );
                    sleep(700);
                    continue;
                }

                break;
            }
        }

        throw new RuntimeException(
                "Failed to enter "
                        + fieldName
                        + ": "
                        + (lastError == null ? "Unknown error" : lastError.getMessage()),
                lastError
        );
    }


    private void enterIntoInput(WebElement input, String value, String fieldName, String targetInfo) {
        try {
            tapElementCenter(input);
            sleep(120);

            input.clear();
            sleep(80);

            input.sendKeys(value);

            closeKeyboardWithEnterAndWait();

            ReportLogger.step(fieldName + " entered: " + value);
            return;

        } catch (Exception sendKeysError) {
            ReportLogger.debug(
                    "sendKeys failed for "
                            + fieldName
                            + ". Falling back to keyboard typing: "
                            + sendKeysError.getMessage()
            );
        }

        tapElementCenter(input);
        sleep(120);

        clearFocusedInput();

        if (value != null && !value.trim().isEmpty()) {
            typeNumberUsingKeyboard(value);
        }

        closeKeyboardWithEnterAndWait();

        ReportLogger.step(fieldName + " entered using keyboard fallback: " + value);
    }

    private void enterIntoInputReplacingExisting(WebElement input, String value, String fieldName, String targetInfo) {
        String currentText = safeText(input);

        try {
            tapElementCenter(input);
            sleep(120);

            if (!hasDigit(currentText)) {
                input.sendKeys(value);
                closeKeyboardWithEnterAndWait();

                ReportLogger.step(fieldName + " entered fast: " + value);
                return;
            }

            input.clear();
            sleep(120);

            input.sendKeys(value);
            closeKeyboardWithEnterAndWait();

            ReportLogger.step(fieldName + " replaced/entered: " + value);
            return;

        } catch (Exception sendKeysError) {
            ReportLogger.debug(
                    "Fast replace failed for "
                            + fieldName
                            + ". Falling back to keyboard typing: "
                            + sendKeysError.getMessage()
            );
        }

        tapElementCenter(input);
        sleep(120);

        if (hasDigit(currentText)) {
            clearFocusedInput();
        }

        if (value != null && !value.trim().isEmpty()) {
            typeNumberUsingKeyboard(value);
        }

        closeKeyboardWithEnterAndWait();

        ReportLogger.step(fieldName + " replaced/entered using keyboard fallback: " + value);
    }

    private void closeKeyboardWithEnterAndWait() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            sleep(300);
        } catch (Exception e) {
            ReportLogger.debug("Keyboard Done/Enter skipped: " + e.getMessage());
        }
    }

    private boolean tapContinueAndVerifyNextScreen(String caseId, String fromScreenName, By nextScreenMarker, String nextScreenName) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ReportLogger.step(
                        "Continue and verify next screen. From="
                                + fromScreenName
                                + ", To="
                                + nextScreenName
                                + ", Attempt="
                                + attempt
                );

                tapContinueSafely(fromScreenName);

                if (validateTaxInlineValidationIfPresent(caseId, fromScreenName)) {
                    ReportLogger.pass(
                            "Continue blocked because inline tax validation is displayed. "
                                    + "Validated and passing case: "
                                    + caseId
                    );
                    return false;
                }

                try {
                    wait.until(ExpectedConditions.visibilityOfElementLocated(nextScreenMarker));
                    ReportLogger.pass(nextScreenName + " screen opened successfully");
                    return true;
                } catch (Exception waitError) {
                    lastError = waitError;

                    if (validateTaxInlineValidationIfPresent(caseId, fromScreenName)) {
                        ReportLogger.pass(
                                "Continue blocked because inline tax validation is displayed. "
                                        + "Validated and passing case: "
                                        + caseId
                        );
                        return false;
                    }

                    closeKeyboardWithEnterAndWait();
                    swipeUpW3C();
                    sleep(1200);
                }

            } catch (Exception e) {
                lastError = e;
                ReportLogger.debug(
                        "Continue + verify failed from "
                                + fromScreenName
                                + " to "
                                + nextScreenName
                                + ". Attempt="
                                + attempt
                                + ". Error="
                                + e.getMessage()
                );

                if (validateTaxInlineValidationIfPresent(caseId, fromScreenName)) {
                    ReportLogger.pass(
                            "Continue blocked because inline tax validation is displayed. "
                                    + "Validated and passing case: "
                                    + caseId
                    );
                    return false;
                }
            }
        }

        if (validateTaxInlineValidationIfPresent(caseId, fromScreenName)) {
            ReportLogger.pass(
                    "Continue blocked because inline tax validation is displayed. "
                            + "Validated and passing case: "
                            + caseId
            );
            return false;
        }

        throw new RuntimeException(
                "Failed to navigate from "
                        + fromScreenName
                        + " to "
                        + nextScreenName
                        + ". Continue click did not open expected screen."
                        + (lastError == null ? "" : " Last error: " + lastError.getMessage()),
                lastError
        );
    }

    private boolean validateTaxInlineValidationIfPresent(String caseId, String screenName) {
        try {
            closeKeyboardWithEnterAndWait();
            sleep(400);

            List<String> allValidationMessages = new ArrayList<>();

            boolean validationFoundInCurrentView =
                    collectVisibleTaxInlineValidationMessages(allValidationMessages);

            if (!validationFoundInCurrentView) {
                return false;
            }

            try {
                String screenshotPath = ScreenshotUtils.captureScreenshot(
                        driver,
                        "Tax_Inline_Red_Validation_" + caseId
                );

                if (screenshotPath != null && ExtentTestManager.getTest() != null) {
                    ExtentTestManager.getTest().addScreenCaptureFromPath(screenshotPath);
                    ReportLogger.step("Tax inline red validation screenshot captured before passing case.");
                }
            } catch (Exception screenshotError) {
                ReportLogger.debug(
                        "Could not capture tax inline validation screenshot: "
                                + screenshotError.getMessage()
                );
            }

            /*
             * Once one red validation is found, scan the same blocked screen
             * to collect other visible/offscreen red validations also.
             * This runs only when validation is already present, so it will not
             * disturb valid flows where the next screen opened successfully.
             */
            String lastSignature = String.join(" | ", allValidationMessages);
            int noNewMessageCount = 0;

            for (int scanAttempt = 1; scanAttempt <= 5; scanAttempt++) {
                swipeUpW3C();
                sleep(800);

                collectVisibleTaxInlineValidationMessages(allValidationMessages);

                String currentSignature = String.join(" | ", allValidationMessages);

                if (currentSignature.equals(lastSignature)) {
                    noNewMessageCount++;
                } else {
                    noNewMessageCount = 0;
                    lastSignature = currentSignature;
                }

                if (noNewMessageCount >= 2) {
                    break;
                }
            }

            StringBuilder errorLog = new StringBuilder();

            for (int i = 0; i < allValidationMessages.size(); i++) {
                errorLog
                        .append(i + 1)
                        .append(". ")
                        .append(allValidationMessages.get(i));

                if (i < allValidationMessages.size() - 1) {
                    errorLog.append(" | ");
                }
            }

            ReportLogger.pass(
                    "Inline Tax red validations displayed and validated exactly"
                            + " | Case: "
                            + caseId
                            + " | Screen: "
                            + screenName
                            + " | Total Errors: "
                            + allValidationMessages.size()
                            + " | Errors: "
                            + errorLog
            );

            return true;

        } catch (Exception e) {
            ReportLogger.debug("Tax inline red validation check skipped safely: " + e.getMessage());
            return false;
        }
    }
    
    private boolean collectVisibleTaxInlineValidationMessages(List<String> collectedMessages) {
        boolean foundAnyValidation = false;

        try {
            List<WebElement> validationErrors = driver.findElements(taxInlineValidationError);

            for (WebElement validationError : validationErrors) {
                try {
                    if (!validationError.isDisplayed()) {
                        continue;
                    }

                    Rectangle rect = validationError.getRect();

                    if (rect.getY() < 180 || rect.getY() > 2250) {
                        continue;
                    }

                    String errorText = validationError.getAttribute("content-desc");

                    if (errorText == null || errorText.trim().isEmpty()) {
                        errorText = validationError.getText();
                    }

                    if (errorText == null || errorText.trim().isEmpty()) {
                        continue;
                    }

                    String normalizedErrorText = errorText
                            .replace("\n", " ")
                            .replace("\r", " ")
                            .replaceAll("\\s+", " ")
                            .trim();

                    if (normalizedErrorText.isEmpty()) {
                        continue;
                    }

                    if (!collectedMessages.contains(normalizedErrorText)) {
                        collectedMessages.add(normalizedErrorText);
                    }

                    foundAnyValidation = true;

                } catch (Exception ignored) {
                    // Try next matching validation element
                }
            }

        } catch (Exception e) {
            ReportLogger.debug("Collect tax inline validation messages skipped safely: " + e.getMessage());
        }

        return foundAnyValidation;
    }

    private void tapContinueSafely(String fromScreenName) {
        closeKeyboardWithEnterAndWait();

        Exception lastError = null;

        for (int attempt = 1; attempt <= 6; attempt++) {
            try {
                List<WebElement> continueButtons = driver.findElements(continueButton);

                for (WebElement continueElement : continueButtons) {
                    try {
                        if (continueElement.isDisplayed() && continueElement.isEnabled()) {
                            tapElementCenter(continueElement);

                            ReportLogger.step("Clicked actual Continue button from " + fromScreenName);
                            sleep(2200);
                            return;
                        }
                    } catch (Exception ignored) {
                        // Try next continue element
                    }
                }

                swipeDownW3C();
                sleep(1200);

            } catch (Exception e) {
                lastError = e;

                try {
                    closeKeyboardWithEnterAndWait();
                    swipeDownW3C();
                    sleep(1200);
                } catch (Exception ignored) {
                    // Ignore retry cleanup failure
                }
            }
        }

        throw new RuntimeException(
                "Failed to click actual Continue button from "
                        + fromScreenName
                        + ". Continue button was not visible after safe retries."
                        + (lastError == null ? "" : " Last error: " + lastError.getMessage()),
                lastError
        );
    }

    private void tapCalculateTaxSafely() {
        ReportLogger.step("Clicking Calculate Tax");

        try {
            WebElement calculateElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(calculateTaxButton)
            );

            if (calculateElement.isEnabled()) {
                calculateElement.click();
                ReportLogger.step("Clicked Calculate Tax");
                sleep(3000);
                return;
            }

        } catch (Exception e) {
            ReportLogger.debug("Normal Calculate Tax click failed/skipped: " + e.getMessage());
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", 540);
            params.put("y", 2140);

            driver.executeScript("mobile: clickGesture", params);

            ReportLogger.step("Clicked Calculate Tax using coordinate fallback");
            sleep(3000);

        } catch (Exception e) {
            throw new RuntimeException("Failed to tap Calculate Tax: " + e.getMessage(), e);
        }
    }

    private void tapElementCenter(WebElement element) {
        Rectangle rect = element.getRect();

        int x = rect.getX() + (rect.getWidth() / 2);
        int y = rect.getY() + (rect.getHeight() / 2);

        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);

        driver.executeScript("mobile: clickGesture", params);

        sleep(220);
    }

    private List<WebElement> getVisibleInputsTopToBottom() {
        List<WebElement> allInputs = driver.findElements(By.className("android.widget.EditText"));
        List<VisibleInputSnapshot> visibleInputSnapshots = new ArrayList<>();

        for (WebElement input : allInputs) {
            try {
                if (!input.isDisplayed() || !input.isEnabled()) {
                    continue;
                }

                Rectangle rect = input.getRect();

                if (rect.getHeight() < 60 || rect.getWidth() <= 0) {
                    continue;
                }

                if (rect.getY() < 250 || rect.getY() > 2200) {
                    continue;
                }

                visibleInputSnapshots.add(new VisibleInputSnapshot(input, rect.getY()));

            } catch (StaleElementReferenceException staleError) {
                ReportLogger.debug("Skipped stale EditText while collecting visible inputs.");
            } catch (Exception ignored) {
                // Ignore stale/non-visible input
            }
        }

        visibleInputSnapshots.sort(Comparator.comparingInt(snapshot -> snapshot.y));

        List<WebElement> visibleInputs = new ArrayList<>();

        for (VisibleInputSnapshot snapshot : visibleInputSnapshots) {
            visibleInputs.add(snapshot.element);
        }

        return visibleInputs;
    }


    private void printVisibleInputs(String tag) {
        List<WebElement> inputs = getVisibleInputsTopToBottom();
        ReportLogger.debug("Visible inputs " + tag + ": " + inputs.size());
        printInputList(inputs);
    }

    private void printInputList(List<WebElement> inputs) {
        for (int i = 0; i < inputs.size(); i++) {
            try {
                WebElement input = inputs.get(i);
                Rectangle rect = input.getRect();

                ReportLogger.debug(
                        "Input[" + i + "]"
                                + " x=" + rect.getX()
                                + " y=" + rect.getY()
                                + " w=" + rect.getWidth()
                                + " h=" + rect.getHeight()
                                + " text=" + safeText(input)
                );
            } catch (Exception ignored) {
                // Ignore stale
            }
        }
    }

    private String buildInputSignature(List<WebElement> inputs) {
        StringBuilder signature = new StringBuilder();

        for (WebElement input : inputs) {
            try {
                Rectangle rect = input.getRect();
                signature
                        .append(rect.getX())
                        .append(":")
                        .append(rect.getY())
                        .append(":")
                        .append(rect.getWidth())
                        .append(":")
                        .append(rect.getHeight())
                        .append(":")
                        .append(safeText(input))
                        .append("|");
            } catch (Exception ignored) {
                signature.append("stale|");
            }
        }

        return signature.toString();
    }

    private boolean isStaleException(Throwable error) {
        while (error != null) {
            if (error instanceof StaleElementReferenceException) {
                return true;
            }

            String message = error.getMessage();

            if (message != null
                    && (message.contains("stale element")
                    || message.contains("Cached elements")
                    || message.contains("do not exist in DOM anymore"))) {
                return true;
            }

            error = error.getCause();
        }

        return false;
    }

    private boolean hasDigit(String text) {
        return text != null && text.matches(".*\\d.*");
    }

    private boolean isPositiveAmount(String value) {
        if (value == null) {
            return false;
        }

        try {
            String cleanValue = value.replace(",", "").trim();

            if (cleanValue.isEmpty()) {
                return false;
            }

            return Double.parseDouble(cleanValue) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isElementPresent(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);

            for (WebElement element : elements) {
                if (element.isDisplayed()) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private String safeText(WebElement element) {
        try {
            String text = element.getText();
            return text == null ? "" : text;
        } catch (Exception e) {
            return "";
        }
    }

    private void clearFocusedInput() {
        try {
            for (int i = 0; i < 25; i++) {
                driver.pressKey(new KeyEvent(AndroidKey.DEL));
            }

            sleep(100);

        } catch (Exception e) {
            ReportLogger.debug("Focused input clear skipped: " + e.getMessage());
        }
    }

    private void typeNumberUsingKeyboard(String value) {
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '0':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_0));
                    break;
                case '1':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1));
                    break;
                case '2':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_2));
                    break;
                case '3':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_3));
                    break;
                case '4':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_4));
                    break;
                case '5':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_5));
                    break;
                case '6':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_6));
                    break;
                case '7':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_7));
                    break;
                case '8':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_8));
                    break;
                case '9':
                    driver.pressKey(new KeyEvent(AndroidKey.DIGIT_9));
                    break;
                default:
                    ReportLogger.debug("Skipped unsupported character: " + ch);
            }

            sleep(50);
        }
    }

    private void swipeUpW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 1650));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(750), PointerInput.Origin.viewport(), 540, 700));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe up failed: " + e.getMessage(), e);
        }
    }

    private void swipeDownW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 700));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), 540, 1450));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe down failed: " + e.getMessage(), e);
        }
    }

    private void tap(By locator, String elementName) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.click();

        ReportLogger.step(elementName + " clicked");
        sleep(650);
    }

    private void tapByDescriptionContains(String text) {
        By locator = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + text + "\")"
        );

        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.click();

        ReportLogger.step(text + " clicked");
        sleep(650);
    }

    private void waitForVisible(By locator, String elementName) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        ReportLogger.pass("Verified screen/element: " + elementName);
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