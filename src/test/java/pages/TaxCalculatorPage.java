package pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class TaxCalculatorPage {
    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By portfolioPlanner = AppiumBy.accessibilityId("Portfolio Planner");
    private final By investorManish = AppiumBy.accessibilityId("Manish Khatri");
    private final By nextButton = AppiumBy.accessibilityId("Next");
    private final By helpMeCalculateThis = AppiumBy.accessibilityId("Help me calculate this");

    private final By continueButton = By.id("Continue");
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

    public TaxCalculatorPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }
}
