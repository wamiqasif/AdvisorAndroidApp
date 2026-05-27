package pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import utils.ConfigReader;

/**
 * Optimized Portfolio Planner Page
 *
 * Goals:
 * - Fast execution
 * - Flutter-safe
 * - Lightweight recovery
 * - No over-engineering
 * - Stable persistent session
 */
public class PortfolioPlannerPage extends BasePage {

    // ============================================================
    // NAVIGATION
    // ============================================================

    private final By hubBottomTab =
            AppiumBy.accessibilityId("Hub");

    private final By portfolioPlannerEntry =
            AppiumBy.accessibilityId("Portfolio Planner");

    private final By exitButton =
            AppiumBy.accessibilityId("Exit");

    // ============================================================
    // LANDING SCREEN
    // ============================================================

    private final By selectInvestorTitle =
            AppiumBy.accessibilityId("Select Investor");

    private final By nextButton =
            AppiumBy.accessibilityId("Next");

    private final By portfolioPlannerTitle =
            AppiumBy.accessibilityId("Portfolio Planner");

    // ============================================================
    // PLANNER TYPES
    // ============================================================

    private final By higherReturnsOption =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".descriptionStartsWith("
                            + "\"Invest for higher returns\")");

    private final By monthlyIncomeOption =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".descriptionStartsWith("
                            + "\"Invest for monthly income\")");

    private final By saveTaxOption =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".descriptionStartsWith("
                            + "\"Invest to save tax\")");

    // ============================================================
    // FORM
    // ============================================================

    private final By amountInputField =
            AppiumBy.className(
                    "android.widget.EditText");

    private final By showInvestmentPlanButton =
            AppiumBy.accessibilityId(
                    "Show investment plan");

    // ============================================================
    // PLAN SCREEN
    // ============================================================

    private final By investmentPlanTitle =
            AppiumBy.accessibilityId(
                    "Your investment plan");

    private final By editInvestmentPlanTitle =
            AppiumBy.accessibilityId(
                    "Edit investment plan");

    private final By breakdownButton =
            AppiumBy.accessibilityId("Breakdown");

    private final By editButton =
            AppiumBy.accessibilityId("Edit");

    private final By investNowButton =
            AppiumBy.accessibilityId("Invest Now");

    private final By addFundButton =
            AppiumBy.accessibilityId("Add Fund");

    private final By resetButton =
            AppiumBy.accessibilityId("Reset");

    private final By subscribeButton =
            AppiumBy.accessibilityId("Subscribe");

    // ============================================================
    // VALIDATION
    // ============================================================

    private final By riskProfileLabel =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".descriptionContains("
                            + "\"Risk Profile\")");

    private final By completeKycNowButton =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".descriptionContains("
                            + "\"Complete KYC now\")");

    private final By riskAssessmentCta =
            AppiumBy.androidUIAutomator(
                    "new UiSelector()"
                            + ".descriptionContains("
                            + "\"Complete risk assessment\")");

    // ============================================================

    public PortfolioPlannerPage(AndroidDriver driver) {

        super(driver);
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    public void navigateToHubAndOpenPlanner() {

        if (isPlannerLandingScreenDisplayed()
                || verifyInvestmentPlanScreen()) {

            return;
        }

        navigateBackSafely();

        openPortfolioPlanner();
    }

    public void openPortfolioPlanner() {

        logger.info(
                "Opening Portfolio Planner");

        if (verifyInvestmentPlanScreen()) {

            return;
        }

        if (isDisplayed(hubBottomTab, 2)) {

            safeClick(hubBottomTab);

            waitForUiToSettle();
            waitForUiToSettle();
        }

        scrollToPortfolioPlannerIfNeeded();

        safeClick(portfolioPlannerEntry);

        waitForUiToSettle();

        logger.info(
                "Portfolio Planner opened");
    }

    // ============================================================
    // INVESTOR
    // ============================================================

    public void selectInvestor(String investorName) {

        By investor =
                AppiumBy.accessibilityId(investorName);

        scrollToElement(investor, 3);

        safeClick(investor);

        logger.info(
                "Selected investor '{}'",
                investorName);
    }

    public void clickNext() {

        safeClick(nextButton);

        waitForUiToSettle();

        logger.info("Tapped Next");
    }

    // ============================================================
    // PLANNER TYPE
    // ============================================================

    public void choosePlannerType(String plannerType) {

        By plannerCard =
                plannerTypeCard(plannerType);

        scrollToElement(plannerCard, 2);

        safeClick(plannerCard);

        waitForPlannerForm();

        logger.info(
                "Selected planner type '{}'",
                plannerType);
    }

    public void selectInvestmentMode(String mode) {

        By modeLocator =
                AppiumBy.androidUIAutomator(
                        "new UiSelector()"
                                + ".descriptionContains(\""
                                + mode
                                + "\")");

        scrollToElement(modeLocator, 2);

        safeClick(modeLocator);

        waitForUiToSettle();

        logger.info(
                "Selected investment mode '{}'",
                mode);
    }

    // ============================================================
    // FORM
    // ============================================================

    public void enterSipAmount(String amount) {

        WebElement field =
                waitForVisible(amountInputField);

        field.click();

        field.clear();

        field.sendKeys(amount);

        logger.info(
                "Entered SIP amount '{}'",
                amount);
    }

    public void enterOneTimeAmount(String amount) {

        enterSipAmount(amount);
    }

    public void enterInvestmentPeriod(String period) {

        hideKeyboardIfVisible();

        List<WebElement> fields =
                driver.findElements(
                        AppiumBy.className(
                                "android.widget.EditText"));

        if (fields.size() < 2) {

            throw new RuntimeException(
                    "Investment period field not found");
        }

        WebElement field =
                fields.get(fields.size() - 1);

        field.click();

        field.clear();

        field.sendKeys(period);

        hideKeyboardIfVisible();

        logger.info(
                "Entered investment period '{}'",
                period);
    }

    public void clickShowInvestmentPlan() {

        hideKeyboardIfVisible();

        scrollToElement(
                showInvestmentPlanButton,
                2);

        safeClick(showInvestmentPlanButton);

        waitForUiToSettle();

        logger.info(
                "Tapped Show investment plan");
    }

    public void chooseMonthsOrYears(String durationType) {

        safeClick(
                AppiumBy.accessibilityId(durationType));

        logger.info(
                "Selected duration type '{}'",
                durationType);
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    public boolean verifyRiskProfile() {

        return isDisplayed(riskProfileLabel);
    }

    public boolean verifyKycPendingScreen() {

        return isDisplayed(completeKycNowButton);
    }

    public boolean verifyRiskAssessmentPending() {

        return isDisplayed(riskAssessmentCta);
    }

    public boolean verifyInvestmentPlanScreen() {

        waitForUiToSettle();

        return isAnyDisplayed(
                investmentPlanTitle,
                editButton,
                investNowButton);
    }

    public boolean isBlurredInvestmentPlanDisplayed() {

        return isAnyDisplayed(
                investmentPlanTitle,
                subscribeButton);
    }

    public boolean verifyEditInvestmentPlan() {

        return isAnyDisplayed(
                editInvestmentPlanTitle,
                addFundButton,
                resetButton);
    }

    // ============================================================
    // PLAN ACTIONS
    // ============================================================

    public void openBreakdown() {

        safeClick(breakdownButton);
    }

    public void openEditInvestmentPlan() {

        safeClick(editButton);
    }

    public void clickInvestNow() {

        safeClick(investNowButton);
    }

    public void addFund() {

        safeClick(addFundButton);
    }

    public void resetInvestmentPlan() {

        safeClick(resetButton);
    }

    // ============================================================
    // SCREEN STATE
    // ============================================================

    public boolean isSelectInvestorScreenDisplayed() {

        return isAnyDisplayed(
                selectInvestorTitle,
                nextButton);
    }

    public boolean isPlannerOptionsScreenDisplayed() {

        return isAnyDisplayed(
                portfolioPlannerTitle,
                higherReturnsOption,
                monthlyIncomeOption,
                saveTaxOption);
    }

    private boolean isPlannerLandingScreenDisplayed() {

        return isSelectInvestorScreenDisplayed()
                || isPlannerOptionsScreenDisplayed()
                || verifyKycPendingScreen()
                || verifyRiskAssessmentPending();
    }

    public boolean isBreakdownButtonDisplayed() {

        return isDisplayed(breakdownButton);
    }

    public boolean isEditButtonDisplayed() {

        return isDisplayed(editButton);
    }

    public boolean isInvestNowEnabled() {

        return isCtaEnabled("Invest Now")
                || isCtaEnabled("Proceed to pay");
    }

    public boolean isSubscribeCtaDisplayed() {

        return isDisplayed(subscribeButton);
    }

    public boolean isSubscriberFlow() {

        return isSelectInvestorScreenDisplayed();
    }

    public boolean isFreeUserFlow() {

        return isPlannerOptionsScreenDisplayed()
                && !isSelectInvestorScreenDisplayed();
    }

    public boolean verifyValidationMessage(String message) {

        return isDisplayed(
                AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\""
                                + message
                                + "\")"),
                2);
    }

    public boolean verifyNoAppCrash() {

        try {

            return ConfigReader.getInstance()
                    .getAppPackage()
                    .equals(driver.getCurrentPackage());

        } catch (Exception e) {

            return false;
        }
    }

    // ============================================================
    // WAITS
    // ============================================================

    public void waitForPlannerLandingScreen() {

        wait.until(driver ->
                isSelectInvestorScreenDisplayed()
                        || isPlannerOptionsScreenDisplayed()
                        || verifyKycPendingScreen()
                        || verifyRiskAssessmentPending());
    }

    public void waitForPlannerForm() {

        new WebDriverWait(
                driver,
                Duration.ofSeconds(5))
                .until(d ->
                        d.findElements(
                                AppiumBy.className(
                                        "android.widget.EditText"))
                                .size() >= 2);
    }

    // ============================================================
    // SCROLLING
    // ============================================================

    private void scrollToElement(
            By locator,
            int maxSwipes) {

        for (int i = 0; i < maxSwipes; i++) {

            if (isDisplayed(locator, 1)) {

                return;
            }

            safeVerticalScroll("down");

            waitForUiToSettle();
        }

        throw new RuntimeException(
                "Element not found: "
                        + locator);
    }

    private void scrollToPortfolioPlannerIfNeeded() {

        if (isDisplayed(portfolioPlannerEntry, 2)) {

            return;
        }

        waitForUiToSettle();

        for (int i = 0; i < 1; i++) {

            safeVerticalScroll("up");

            waitForUiToSettle();

            if (isDisplayed(portfolioPlannerEntry, 2)) {

                return;
            }
        }

        throw new RuntimeException(
                "Portfolio Planner entry not visible");
    }

    // ============================================================
    // RECOVERY
    // ============================================================

    public void navigateBackSafely() {

        try {

            if (isDisplayed(exitButton, 2)) {

                safeClick(exitButton);

                waitForUiToSettle();
            }

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // CTA HELPERS
    // ============================================================

    public boolean isCtaVisible(String ctaText) {

        return isDisplayed(
                AppiumBy.accessibilityId(ctaText))
                || isDisplayed(
                AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\""
                                + ctaText
                                + "\")"));
    }

    public boolean isCtaEnabled(String ctaText) {

        try {

            By locator =
                    ctaLocator(ctaText);

            return waitForVisible(locator, 2)
                    .isEnabled();

        } catch (Exception e) {

            return false;
        }
    }

    public boolean tapCta(String ctaText) {

        try {

            safeClick(ctaLocator(ctaText));

            return true;

        } catch (Exception e) {

            logger.warn(
                    "Unable to tap CTA '{}': {}",
                    ctaText,
                    e.getMessage());

            return false;
        }
    }

    public List<String> getInvestmentPlanCtaIds() {

        List<String> knownCtas =
                Arrays.asList(
                        "Breakdown",
                        "Edit",
                        "Invest Now",
                        "Proceed to pay",
                        "Add Fund",
                        "Reset",
                        "Subscribe");

        List<String> visibleCtas =
                new ArrayList<>();

        for (String cta : knownCtas) {

            if (isCtaVisible(cta)) {

                visibleCtas.add(cta);
            }
        }

        return visibleCtas;
    }

    // ============================================================
    // INVESTOR DISCOVERY
    // ============================================================

    public List<String> discoverInvestorNames() {

        Set<String> names =
                new LinkedHashSet<>();

        for (int i = 0; i < 4; i++) {

            List<WebElement> elements =
                    driver.findElements(
                            AppiumBy.androidUIAutomator(
                                    "new UiSelector()"
                                            + ".className(\"android.view.View\")"
                                            + ".clickable(true)"));

            for (WebElement el : elements) {

                try {

                    String desc =
                            el.getAttribute(
                                    "content-desc");

                    if (desc == null
                            || desc.isBlank()) {

                        continue;
                    }

                    String name =
                            desc.split("\\n")[0]
                                    .trim();

                    if (!name.isBlank()
                            && !name.equalsIgnoreCase("Next")
                            && !name.equalsIgnoreCase("Update")) {

                        names.add(name);
                    }

                } catch (Exception ignored) {
                }
            }

            safeVerticalScroll("down");
        }

        return new ArrayList<>(names);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private By plannerTypeCard(String plannerType) {

        return AppiumBy.androidUIAutomator(
                "new UiSelector()"
                        + ".descriptionStartsWith(\""
                        + plannerType
                        + "\")");
    }

    private By ctaLocator(String ctaText) {

        if (isDisplayed(
                AppiumBy.accessibilityId(ctaText))) {

            return AppiumBy.accessibilityId(ctaText);
        }

        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\""
                        + ctaText
                        + "\")");
    }
}