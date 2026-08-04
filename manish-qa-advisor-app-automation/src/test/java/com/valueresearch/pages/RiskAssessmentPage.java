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
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RiskAssessmentPage {

    private final AndroidDriver driver;

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By riskAssessmentMenu = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"risk assessment results\")"
    );

    private final By beginButton = AppiumBy.accessibilityId("Begin");
    private final By continueButton = AppiumBy.accessibilityId("Continue");
    private final By seeMyProfileButton = AppiumBy.accessibilityId("See my profile");
    private final By continueToAdviceButton = AppiumBy.accessibilityId("Continue to my advice");
    private final By updateAssessmentButton = AppiumBy.accessibilityId("Update Assessment");
    private final By exitButton = AppiumBy.accessibilityId("Exit");

    private final By exitAssessmentButton = AppiumBy.accessibilityId("Exit the assessment");
    private final By changeAnswerButton = AppiumBy.accessibilityId("Change an answer");

    private final By ageInput = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(0)"
    );

    private final By confirmationCheckbox = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"I confirm the information I provided is accurate\")"
    );

    public RiskAssessmentPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public void openRiskAssessmentFromHub() {
        try {
            ReportLogger.step("Opening Risk Assessment from Hub");

            WaitUtils.waitForElement(driver, hubTab, 60);
            tapByLocator(hubTab, "Hub tab");
            sleep(1200);

            scrollToRiskAssessmentIfNeeded();

            WaitUtils.waitForElement(driver, riskAssessmentMenu, 60);
            tapByLocator(riskAssessmentMenu, "Risk Assessment menu");
            sleep(1800);

            handleRiskAssessmentLandingScreen();

            ReportLogger.pass("Risk Assessment opened successfully");

        } catch (Exception e) {
            captureFailureScreenshot("RiskAssessment_Open_Failed");
            ReportLogger.fail("Failed to open Risk Assessment: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to open Risk Assessment: " + cleanError(e.getMessage()), e);
        }
    }

    public void continueRiskAssessmentFromCurrentScreen() {
        try {
            ReportLogger.step("Continuing Risk Assessment from current screen");

            if (isElementVisible(updateAssessmentButton)) {
                ReportLogger.pass("Risk Assessment Update Assessment button visible on current screen");
                return;
            }

            if (isElementVisible(beginButton)) {
                ReportLogger.pass("Risk Assessment Begin button visible on current screen");
                return;
            }

            By viewMore = AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"View more\")"
            );

            if (isElementVisible(viewMore)) {
                tapByLocator(viewMore, "Risk Assessment View more");
                sleep(1200);

                if (isElementVisible(updateAssessmentButton)) {
                    ReportLogger.pass("Risk Assessment detail opened from current screen");
                    return;
                }
            }

            if (pageContains("Risk Assessment") || pageContains("Risk Type")) {
                ReportLogger.step("Risk Assessment page detected. Trying direct Update Assessment visibility.");
                scrollToUpdateAssessmentIfNeeded();

                if (isElementVisible(updateAssessmentButton)) {
                    ReportLogger.pass("Update Assessment visible after scroll");
                    return;
                }
            }

            ReportLogger.step("Current screen is not Risk Assessment detail. Opening from Hub.");
            openRiskAssessmentFromHub();

        } catch (Exception e) {
            captureFailureScreenshot("RiskAssessment_CurrentScreen_Recovery_Failed");
            ReportLogger.fail("Failed to continue Risk Assessment from current screen: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to continue Risk Assessment from current screen: " + cleanError(e.getMessage()), e);
        }
    }

    public void completeRiskAssessmentFlow(
            String age,
            String annualIncomeRange,
            String savingsRange,
            String loanRange,
            String essentialExpenseRange,
            String goal,
            String investmentPreference,
            String marketCrashReaction,
            String expectedRiskProfile
    ) {
        try {
            ReportLogger.step("Starting Risk Assessment input flow");

            startOrUpdateAssessment();

            enterAge(age);

            selectSingleChoice("Annual income before tax", annualIncomeRange);
            selectSingleChoice("Savings worth", savingsRange);
            selectSingleChoice("Loans and credit card dues", loanRange);
            selectSingleChoice("Essential expenses share", essentialExpenseRange);

            selectGoal(goal);

            selectSingleChoice("Investment preference", investmentPreference);
            selectFinalQuestionOption("Market crash reaction", marketCrashReaction);

            validateRiskProfileResult(expectedRiskProfile);

            continueToAdviceAndExit();

            ReportLogger.pass("Risk Assessment flow completed successfully");

        } catch (AssertionError e) {
            /*
             * Functional PRD mismatch:
             * testcase must fail, but suite should continue.
             * Do not press BACK. Complete the result flow and Exit normally.
             */
            captureFailureScreenshot("RiskAssessment_PRD_Mismatch");
            cleanupResultScreenAfterValidationFailure();
            throw e;

        } catch (Exception e) {
            /*
             * Technical failure:
             * Try clean result/advice/detail cleanup without blind BACK.
             */
            captureFailureScreenshot("RiskAssessment_Flow_Failed");
            cleanupResultScreenAfterValidationFailure();

            ReportLogger.fail("Risk Assessment flow failed: " + cleanError(e.getMessage()));
            throw new RuntimeException("Risk Assessment flow failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void handleRiskAssessmentLandingScreen() {
        try {
            if (isElementVisible(beginButton)) {
                ReportLogger.pass("Risk Profile getting started screen detected");
                return;
            }

            if (isElementVisible(updateAssessmentButton)) {
                ReportLogger.pass("Existing Risk Assessment profile screen detected");
                return;
            }

            By viewMore = AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"View more\")"
            );

            if (isElementVisible(viewMore)) {
                tapByLocator(viewMore, "Risk Assessment View more");
                sleep(1200);
            }

            if (isElementVisible(updateAssessmentButton)) {
                ReportLogger.pass("Risk Assessment detail screen detected");
                return;
            }

            if (pageContains("Risk Type") || pageContains("Moderate") || pageContains("Risk Assessment")) {
                ReportLogger.pass("Risk Assessment summary detected");
                return;
            }

            ReportLogger.step("Risk Assessment landing screen handled");

        } catch (Exception e) {
            ReportLogger.step("Risk Assessment landing screen handling skipped safely: " + cleanError(e.getMessage()));
        }
    }

    private void startOrUpdateAssessment() {
        try {
            if (isElementVisible(updateAssessmentButton)) {
                tapByLocator(updateAssessmentButton, "Update Assessment");
                sleep(1800);
                ReportLogger.pass("Risk Assessment Update Assessment clicked");

                clickBeginAfterUpdateIfNeeded();
                return;
            }

            if (isElementVisible(beginButton)) {
                tapByLocator(beginButton, "Begin");
                sleep(1500);
                ReportLogger.pass("Risk Assessment started using Begin");
                return;
            }

            By viewMore = AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"View more\")"
            );

            if (isElementVisible(viewMore)) {
                tapByLocator(viewMore, "Risk Assessment View more");
                sleep(1200);

                if (isElementVisible(updateAssessmentButton)) {
                    tapByLocator(updateAssessmentButton, "Update Assessment");
                    sleep(1800);
                    ReportLogger.pass("Risk Assessment Update Assessment clicked after View more");

                    clickBeginAfterUpdateIfNeeded();
                    return;
                }
            }

            scrollToUpdateAssessmentIfNeeded();

            if (isElementVisible(updateAssessmentButton)) {
                tapByLocator(updateAssessmentButton, "Update Assessment");
                sleep(1800);
                ReportLogger.pass("Risk Assessment Update Assessment clicked after scroll");

                clickBeginAfterUpdateIfNeeded();
                return;
            }

            if (isElementVisible(ageInput) || isElementVisible(continueButton)) {
                ReportLogger.step("Risk Assessment questionnaire already started");
                return;
            }

            throw new RuntimeException("Begin/Update Assessment button not found");

        } catch (Exception e) {
            ReportLogger.fail("Failed to start/update Risk Assessment: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to start/update Risk Assessment: " + cleanError(e.getMessage()), e);
        }
    }

    private void clickBeginAfterUpdateIfNeeded() {
        if (isElementVisible(beginButton)) {
            tapByLocator(beginButton, "Begin after Update Assessment");
            sleep(1500);
            ReportLogger.pass("Risk Assessment started using Begin after Update Assessment");
            return;
        }

        if (isElementVisible(ageInput) || isElementVisible(continueButton)) {
            ReportLogger.pass("Risk Assessment questionnaire opened after Update Assessment");
            return;
        }

        throw new RuntimeException("Update Assessment clicked, but Begin/Age screen did not appear");
    }

    private void enterAge(String age) {
        try {
            ReportLogger.step("Step 1: Entering age: " + age);

            WaitUtils.waitForElement(driver, ageInput, 60);

            WebElement input = driver.findElement(ageInput);

            tapElementCenter(input);
            sleep(300);

            clearInput(input);

            input.sendKeys(age);

            closeKeyboard();

            ReportLogger.step("Age entered: " + age);

            tapByLocator(continueButton, "Continue after Age");
            sleep(1200);

            ReportLogger.pass("Step 1 completed: Age");

        } catch (Exception e) {
            ReportLogger.fail("Failed to enter age: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to enter age: " + cleanError(e.getMessage()), e);
        }
    }

    private void selectSingleChoice(String questionName, String optionText) {
        try {
            ReportLogger.step("Selecting " + questionName + ": " + optionText);

            By optionLocator = AppiumBy.accessibilityId(optionText);

            waitForPresent(optionLocator, 30);
            tapByLocator(optionLocator, optionText);

            sleep(700);

            tapByLocator(continueButton, "Continue after " + questionName);

            sleep(1200);

            ReportLogger.pass("Selected " + questionName + ": " + optionText);

        } catch (Exception e) {
            ReportLogger.fail("Failed to select " + questionName + ": " + optionText);
            throw new RuntimeException(
                    "Failed to select " + questionName + ": " + optionText + " | " + cleanError(e.getMessage()),
                    e
            );
        }
    }

    private void selectGoal(String goal) {
        try {
            String appGoalText = normalizeGoalForApp(goal);

            ReportLogger.step("Selecting goal"
                    + " | PRD/Test Data: " + goal
                    + " | App Locator Text: " + appGoalText
            );

            By goalLocator = AppiumBy.accessibilityId(appGoalText);

            waitForPresent(goalLocator, 20);
            tapByLocator(goalLocator, appGoalText);

            sleep(700);

            tapByLocator(continueButton, "Continue after Goal");

            sleep(1200);

            ReportLogger.pass("Goal selected: " + appGoalText);

        } catch (Exception e) {
            ReportLogger.fail("Failed to select goal: " + goal);
            throw new RuntimeException("Failed to select goal: " + goal + " | " + cleanError(e.getMessage()), e);
        }
    }

    private String normalizeGoalForApp(String goal) {
        if (goal == null) {
            return "";
        }

        String cleanGoal = goal.trim();

        if ("General long-term wealth".equalsIgnoreCase(cleanGoal)) {
            return "Long-term wealth";
        }

        if ("Children's education or marriage".equalsIgnoreCase(cleanGoal)) {
            return "Children";
        }

        if ("A major life event".equalsIgnoreCase(cleanGoal)) {
            return "Major life event";
        }

        return cleanGoal;
    }

    private void selectFinalQuestionOption(String questionName, String optionText) {
        try {
            ReportLogger.step("Selecting final question option: " + optionText);

            By optionLocator = AppiumBy.accessibilityId(optionText);

            waitForPresent(optionLocator, 30);
            tapByLocator(optionLocator, optionText);

            sleep(700);

            tapByLocator(seeMyProfileButton, "See my profile");

            sleep(2500);

            ReportLogger.pass("Selected " + questionName + ": " + optionText);

        } catch (Exception e) {
            ReportLogger.fail("Failed to select final option: " + optionText);
            throw new RuntimeException("Failed to select final option: " + optionText + " | " + cleanError(e.getMessage()), e);
        }
    }

    private void validateRiskProfileResult(String expectedRiskProfile) {
        try {
            String article = getRiskProfileArticle(expectedRiskProfile);
            String expectedText = "You are " + article + " " + expectedRiskProfile + " investor.";

            ReportLogger.step("Validating Risk Profile result: " + expectedText);

            By expectedResultLocator = AppiumBy.accessibilityId(expectedText);

            waitForVisible(expectedResultLocator, 25);

            WebElement result = driver.findElement(expectedResultLocator);

            if (!result.isDisplayed()) {
                throw new AssertionError("Risk Profile result is not visible: " + expectedText);
            }

            ReportLogger.pass("Risk Profile result validated: " + expectedText);

        } catch (Exception firstError) {
            String actualProfile = getVisibleRiskProfileFromPageSource();

            if (actualProfile != null && !actualProfile.trim().isEmpty()) {
                ReportLogger.step("Actual Risk Profile detected from screen: " + actualProfile);

                if (actualProfile.equalsIgnoreCase(expectedRiskProfile)) {
                    ReportLogger.pass("Risk Profile result validated from page source: " + actualProfile);
                    return;
                }

                ReportLogger.fail(
                        "Risk Profile mismatch. Expected: "
                                + expectedRiskProfile
                                + " | Actual: "
                                + actualProfile
                );

                throw new AssertionError(
                        "Risk Profile mismatch. Expected: "
                                + expectedRiskProfile
                                + " | Actual: "
                                + actualProfile
                );
            }

            ReportLogger.fail("Risk Profile result validation failed: " + cleanError(firstError.getMessage()));
            throw new RuntimeException("Risk Profile result validation failed: " + cleanError(firstError.getMessage()), firstError);
        }
    }

    private String getRiskProfileArticle(String profile) {
        if (profile == null) {
            return "a";
        }

        String clean = profile.trim().toLowerCase();

        if (clean.startsWith("a") || clean.startsWith("e") || clean.startsWith("i")
                || clean.startsWith("o") || clean.startsWith("u")) {
            return "an";
        }

        return "a";
    }

    private String getVisibleRiskProfileFromPageSource() {
        try {
            String source = driver.getPageSource();

            if (source == null) {
                return "";
            }

            if (source.contains("You are an Aggressive investor")
                    || source.contains("Risk Type\nAggressive")
                    || source.contains("Risk Type Aggressive")
                    || source.contains("Aggressive")) {
                return "Aggressive";
            }

            if (source.contains("You are a Conservative investor")
                    || source.contains("Risk Type\nConservative")
                    || source.contains("Risk Type Conservative")
                    || source.contains("Conservative")) {
                return "Conservative";
            }

            if (source.contains("You are a Moderate investor")
                    || source.contains("Risk Type\nModerate")
                    || source.contains("Risk Type Moderate")
                    || source.contains("Moderate")) {
                return "Moderate";
            }

            return "";

        } catch (Exception e) {
            return "";
        }
    }

    private void continueToAdviceAndExit() {
        try {
            ReportLogger.step("Continuing from Risk Profile result to advice/invest screen");

            scrollDownToConfirmationIfNeeded();

            if (isElementVisible(confirmationCheckbox)) {
                tapByLocator(confirmationCheckbox, "Risk Profile confirmation checkbox");
                sleep(800);
                ReportLogger.pass("Risk Profile confirmation selected");
            } else {
                ReportLogger.step("Risk Profile confirmation checkbox not visible. Skipping checkbox.");
            }

            if (isElementVisible(continueToAdviceButton)) {
                tapByLocator(continueToAdviceButton, "Continue to my advice");
                sleep(2500);
                ReportLogger.pass("Continue to my advice clicked");
            } else {
                ReportLogger.step("Continue to my advice button not visible. Trying to continue safely.");
            }

            exitAdviceScreenIfVisible();

        } catch (Exception e) {
            captureFailureScreenshot("RiskAssessment_Continue_Exit_Failed");
            ReportLogger.fail("Failed to continue to advice and exit: " + cleanError(e.getMessage()));
            throw new RuntimeException("Failed to continue to advice and exit: " + cleanError(e.getMessage()), e);
        }
    }

    private void cleanupResultScreenAfterValidationFailure() {
        try {
            ReportLogger.step("Cleaning up result screen after failed validation without pressing BACK");

            String visibleProfile = getVisibleRiskProfileFromPageSource();

            if (visibleProfile != null && !visibleProfile.trim().isEmpty()) {
                ReportLogger.step("Result screen detected during cleanup. Actual profile: " + visibleProfile);

                scrollDownToConfirmationIfNeeded();

                if (isElementVisible(confirmationCheckbox)) {
                    tapByLocator(confirmationCheckbox, "Risk Profile confirmation checkbox after failure");
                    sleep(800);
                    ReportLogger.step("Risk Profile confirmation selected during failure cleanup");
                } else {
                    ReportLogger.step("Confirmation checkbox not visible during failure cleanup.");
                }

                if (isElementVisible(continueToAdviceButton)) {
                    tapByLocator(continueToAdviceButton, "Continue to my advice after failure");
                    sleep(2500);
                    ReportLogger.step("Continue to my advice clicked during failure cleanup");
                } else {
                    ReportLogger.step("Continue to my advice not visible during failure cleanup.");
                }

                exitAdviceScreenIfVisible();
                return;
            }

            if (isElementVisible(exitButton)) {
                tapByLocator(exitButton, "Exit after failure");
                sleep(1800);
                waitForRiskAssessmentDetailAfterExit();
                return;
            }

            if (isElementVisible(updateAssessmentButton)) {
                ReportLogger.pass("Already on Risk Assessment detail screen after failure");
                return;
            }

            if (isElementVisible(exitAssessmentButton)) {
                tapByLocator(exitAssessmentButton, "Exit the assessment after failure");
                sleep(1800);
                waitForRiskAssessmentDetailAfterExit();
                return;
            }

            ReportLogger.step("Failure cleanup skipped because result/advice/detail screen was not detected.");

        } catch (Exception cleanupError) {
            ReportLogger.step(
                    "Failure cleanup could not complete, but testcase will remain failed: "
                            + cleanError(cleanupError.getMessage())
            );
        }
    }

    private void exitAdviceScreenIfVisible() {
        try {
            ReportLogger.step("Checking Portfolio Planner / advice screen for Exit");

            for (int i = 0; i < 10; i++) {
                if (isElementVisible(exitButton)) {
                    tapByLocator(exitButton, "Exit");
                    sleep(1800);
                    ReportLogger.pass("Exited advice / Portfolio Planner screen successfully");

                    waitForRiskAssessmentDetailAfterExit();

                    return;
                }

                sleep(700);
            }

            ReportLogger.step("Exit button not visible after advice screen. Trying Android BACK once.");

            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            sleep(1200);

            if (isElementVisible(exitAssessmentButton)) {
                tapByLocator(exitAssessmentButton, "Exit the assessment");
                sleep(1800);
            }

            waitForRiskAssessmentDetailAfterExit();

        } catch (Exception e) {
            ReportLogger.step("Advice screen exit skipped safely: " + cleanError(e.getMessage()));
        }
    }

    private void waitForRiskAssessmentDetailAfterExit() {
        try {
            ReportLogger.step("Waiting for Risk Assessment detail screen after Exit");

            for (int i = 0; i < 8; i++) {
                if (isElementVisible(updateAssessmentButton)) {
                    ReportLogger.pass("Risk Assessment detail screen ready for next test case");
                    return;
                }

                By viewMore = AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"View more\")"
                );

                if (isElementVisible(viewMore)) {
                    tapByLocator(viewMore, "Risk Assessment View more after Exit");
                    sleep(1000);

                    if (isElementVisible(updateAssessmentButton)) {
                        ReportLogger.pass("Risk Assessment detail screen ready after View more");
                        return;
                    }
                }

                if (isElementVisible(hubTab)) {
                    tapByLocator(hubTab, "Hub tab after Exit");
                    sleep(1000);

                    if (isElementVisible(updateAssessmentButton)) {
                        ReportLogger.pass("Risk Assessment detail screen ready after Hub");
                        return;
                    }
                }

                sleep(700);
            }

            ReportLogger.step("Risk Assessment detail screen not confirmed after Exit. Next test will recover from current screen if needed.");

        } catch (Exception e) {
            ReportLogger.step("Post-exit Risk Assessment detail wait skipped safely: " + cleanError(e.getMessage()));
        }
    }

    private void scrollToRiskAssessmentIfNeeded() {
        if (isElementVisible(riskAssessmentMenu)) {
            ReportLogger.step("Risk Assessment menu already visible");
            return;
        }

        for (int i = 0; i < 5; i++) {
            swipeUpW3C();
            sleep(700);

            if (isElementVisible(riskAssessmentMenu)) {
                ReportLogger.step("Risk Assessment menu visible after scroll attempt: " + (i + 1));
                return;
            }
        }

        ReportLogger.step("Risk Assessment menu not visible after scroll attempts. Continuing with direct wait.");
    }

    private void scrollToUpdateAssessmentIfNeeded() {
        if (isElementVisible(updateAssessmentButton)) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            swipeUpW3C();
            sleep(700);

            if (isElementVisible(updateAssessmentButton)) {
                return;
            }
        }
    }

    private void scrollDownToConfirmationIfNeeded() {
        if (isElementVisible(confirmationCheckbox) || isElementVisible(continueToAdviceButton)) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            swipeUpW3C();
            sleep(700);

            if (isElementVisible(confirmationCheckbox) || isElementVisible(continueToAdviceButton)) {
                return;
            }
        }
    }

    private void waitForVisible(By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private WebElement waitForPresent(By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private void tapByLocator(By locator, String elementName) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ReportLogger.debug("Trying to tap " + elementName + " | attempt: " + attempt);

                /*
                 * Important:
                 * Do NOT call element.isDisplayed() here.
                 * UiAutomator2 crashed earlier during isElementDisplayed.
                 * We use presence + rectangle coordinate tap instead.
                 */
                WebElement element = waitForPresent(locator, 15);

                Rectangle rect = element.getRect();

                int x = rect.getX() + (rect.getWidth() / 2);
                int y = rect.getY() + (rect.getHeight() / 2);

                tapByCoordinates(x, y);

                ReportLogger.step(elementName + " clicked");
                return;

            } catch (Exception e) {
                lastError = new RuntimeException(
                        "Failed to tap " + elementName + " on attempt " + attempt + ": " + cleanError(e.getMessage()),
                        e
                );

                ReportLogger.debug(lastError.getMessage());

                if (isUiAutomatorCrashed(e)) {
                    throw new RuntimeException(
                            "UiAutomator2/Appium connection crashed while tapping "
                                    + elementName
                                    + ". Restart Appium/emulator session before continuing. Root error: "
                                    + cleanError(e.getMessage()),
                            e
                    );
                }

                sleep(800);
            }
        }

        throw lastError;
    }

    private void tapByCoordinates(int x, int y) {
        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);

        driver.executeScript("mobile: clickGesture", params);

        sleep(300);
    }

    private void tapElementCenter(WebElement element) {
        try {
            Rectangle rect = element.getRect();

            int x = rect.getX() + (rect.getWidth() / 2);
            int y = rect.getY() + (rect.getHeight() / 2);

            tapByCoordinates(x, y);

        } catch (Exception e) {
            try {
                element.click();
                sleep(300);
            } catch (Exception clickError) {
                throw new RuntimeException("Failed to tap element center: " + cleanError(clickError.getMessage()), clickError);
            }
        }
    }

    private void clearInput(WebElement input) {
        try {
            input.clear();
            sleep(200);
        } catch (Exception e) {
            ReportLogger.debug("Normal clear failed. Trying DEL fallback: " + cleanError(e.getMessage()));

            try {
                input.click();

                for (int i = 0; i < 20; i++) {
                    driver.pressKey(new KeyEvent(AndroidKey.DEL));
                    sleep(20);
                }
            } catch (Exception deleteError) {
                ReportLogger.debug("DEL clear fallback skipped: " + cleanError(deleteError.getMessage()));
            }
        }
    }

    private void closeKeyboard() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            sleep(300);
        } catch (Exception e) {
            ReportLogger.debug("Keyboard close skipped: " + cleanError(e.getMessage()));
        }
    }

    private boolean isElementVisible(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            return element != null && element.isDisplayed();
        } catch (NoSuchElementException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean pageContains(String text) {
        try {
            String source = driver.getPageSource();
            return source != null && source.contains(text);
        } catch (Exception e) {
            return false;
        }
    }

    private void swipeUpW3C() {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 540, 1600));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(650), PointerInput.Origin.viewport(), 540, 650));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

        } catch (Exception e) {
            throw new RuntimeException("W3C swipe up failed: " + cleanError(e.getMessage()), e);
        }
    }

    private void captureFailureScreenshot(String name) {
        try {
            if (isDriverSessionBroken()) {
                ReportLogger.step("Skipping screenshot because Appium/UiAutomator2 session is broken: " + name);
                return;
            }

            String screenshotPath = ScreenshotUtils.captureScreenshot(driver, name);

            if (screenshotPath != null && ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().addScreenCaptureFromPath(screenshotPath);
                ReportLogger.step("Failure screenshot captured: " + name);
            }
        } catch (Exception e) {
            ReportLogger.step("Could not capture failure screenshot: " + cleanError(e.getMessage()));
        }
    }

    private boolean isDriverSessionBroken() {
        try {
            return driver == null || driver.getSessionId() == null;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isUiAutomatorCrashed(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            return false;
        }

        return message.contains("socket hang up")
                || message.contains("instrumentation process is not running")
                || message.contains("UiAutomator2 server")
                || message.contains("cannot be proxied");
    }

    private String cleanError(String message) {
        if (message == null) {
            return "";
        }

        int buildInfoIndex = message.indexOf("Build info:");
        if (buildInfoIndex > 0) {
            return message.substring(0, buildInfoIndex).trim();
        }

        int driverInfoIndex = message.indexOf("Driver info:");
        if (driverInfoIndex > 0) {
            return message.substring(0, driverInfoIndex).trim();
        }

        int capabilitiesIndex = message.indexOf("Capabilities");
        if (capabilitiesIndex > 0) {
            return message.substring(0, capabilitiesIndex).trim();
        }

        return message.trim();
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