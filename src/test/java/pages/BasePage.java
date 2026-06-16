package pages;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import utils.ConfigReader;

public abstract class BasePage {

    private static final int DEFAULT_POLLING_MS = 250;
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AndroidDriver driver;
    protected final FluentWait<AndroidDriver> wait;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = buildWait(Duration.ofSeconds(
                ConfigReader.getInstance().getExplicitWaitSeconds()));
    }

    protected FluentWait<AndroidDriver> buildWait(Duration timeout) {
        return new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(DEFAULT_POLLING_MS))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    protected FluentWait<AndroidDriver> shortWait(int seconds) {
        return buildWait(Duration.ofSeconds(seconds));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForVisible(By locator, int seconds) {
        return shortWait(seconds).until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(
                ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> waitForAllVisible(By locator) {
        return wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected void safeClick(By locator) {
        try {
            waitForClickable(locator).click();
        } catch (Exception firstFailure) {
            waitForVisible(locator).click();
        }
    }
    

    protected void safeSendKeys(By locator, String value) {
        WebElement element = waitForVisible(locator);
        element.click();
        element.clear();
        element.sendKeys(value);
        hideKeyboardIfVisible();
    }

    

    protected boolean waitUntilTrue(
            BooleanSupplier condition,
            int seconds) {

        long endTime =
                System.currentTimeMillis()
                        + (seconds * 1000L);

        while (System.currentTimeMillis() < endTime) {

            try {

                if (condition.getAsBoolean()) {

                    return true;
                }

            } catch (Exception ignored) {
            }

            try {

                Thread.sleep(250);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                return false;
            }
        }

        return false;
    }
    
    
    
    protected String safeGetText(By locator) {
        return waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            return !elements.isEmpty() && elements.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isDisplayed(By locator, int seconds) {
        try {
            shortWait(seconds).until(driver -> isDisplayed(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isAnyDisplayed(By... locators) {

        for (By locator : locators) {

            if (!driver.findElements(locator).isEmpty()) {

                return true;
            }
        }

        return false;
    }

    protected int countDisplayed(By... locators) {
        int visibleCount = 0;
        for (By locator : locators) {
            if (isDisplayed(locator)) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    protected void hideKeyboardIfVisible() {

        try {

            if (driver.isKeyboardShown()) {

                logger.info(
                        "Keyboard visible - skipping hideKeyboard()");
            }

        } catch (Exception ignored) {
        }
    }
    public void waitForUiToSettle() {

        try {

            Thread.sleep(1500);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }
    public void waitForUiToSettle(int time) {

        try {

            Thread.sleep(time);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    protected void waitForFlutterToSettle() {

        try {

            Thread.sleep(8000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    protected boolean waitUntilDisplayed(
            By locator,
            int seconds) {

        try {

            waitForUiToSettle();

            new WebDriverWait(
                    driver,
                    Duration.ofSeconds(seconds))
                    .until(d ->

                            isDisplayed(locator));

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public void safeVerticalScroll(String direction) {
        Dimension size = driver.manage().window().getSize();
        int startX = size.width / 2;
        int startY;
        int endY;

        if ("up".equalsIgnoreCase(direction)) {
            startY = (int) (size.height * 0.80);
            endY = (int) (size.height * 0.30);
        } else {
            startY = (int) (size.height * 0.30);
            endY = (int) (size.height * 0.80);
        }

        PointerInput finger = new PointerInput(
                PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                startX,
                startY));
        swipe.addAction(finger.createPointerDown(
                PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(500),
                PointerInput.Origin.viewport(),
                startX,
                endY));
        swipe.addAction(finger.createPointerUp(
                PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }

    protected void scrollToText(String text) {
        driver.findElement(
                AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))"
                                + ".scrollIntoView(new UiSelector().textContains(\""
                                + text
                                + "\"))"));
    }

    protected void waitForPage(By locator) {
        try {
            waitForVisible(locator);
        } catch (TimeoutException e) {
            throw new AssertionError("Page failed to load: " + locator, e);
        }
    }

}
