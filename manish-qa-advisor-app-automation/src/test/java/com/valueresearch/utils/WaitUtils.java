package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Central explicit-wait layer for QA-safe synchronization.
 *
 * Important: implicit wait remains 0 in DriverManager. These waits poll for
 * the real UI condition and continue immediately when the condition is met.
 */
public final class WaitUtils {

    private WaitUtils() {
    }

    private static WebDriverWait createWait(
            AndroidDriver driver,
            Duration timeout
    ) {
        WebDriverWait wait = new WebDriverWait(
                driver,
                timeout,
                ExecutionProfile.pollingInterval()
        );

        wait.ignoring(NoSuchElementException.class);
        wait.ignoring(StaleElementReferenceException.class);

        return wait;
    }

    public static WebElement waitForElement(
            AndroidDriver driver,
            By locator
    ) {
        return createWait(
                driver,
                ExecutionProfile.defaultWait()
        ).until(
                ExpectedConditions.visibilityOfElementLocated(locator)
        );
    }

    public static WebElement waitForElement(
            AndroidDriver driver,
            By locator,
            int timeoutSeconds
    ) {
        return createWait(
                driver,
                Duration.ofSeconds(timeoutSeconds)
        ).until(
                ExpectedConditions.visibilityOfElementLocated(locator)
        );
    }

    public static WebElement waitForClickable(
            AndroidDriver driver,
            By locator
    ) {
        return createWait(
                driver,
                ExecutionProfile.defaultWait()
        ).until(
                ExpectedConditions.elementToBeClickable(locator)
        );
    }

    public static WebElement waitForClickable(
            AndroidDriver driver,
            By locator,
            int timeoutSeconds
    ) {
        return createWait(
                driver,
                Duration.ofSeconds(timeoutSeconds)
        ).until(
                ExpectedConditions.elementToBeClickable(locator)
        );
    }

    public static WebElement waitForPresent(
            AndroidDriver driver,
            By locator
    ) {
        return createWait(
                driver,
                ExecutionProfile.defaultWait()
        ).until(
                ExpectedConditions.presenceOfElementLocated(locator)
        );
    }

    public static WebElement waitForPresent(
            AndroidDriver driver,
            By locator,
            int timeoutSeconds
    ) {
        return createWait(
                driver,
                Duration.ofSeconds(timeoutSeconds)
        ).until(
                ExpectedConditions.presenceOfElementLocated(locator)
        );
    }

    public static boolean waitForInvisible(
            AndroidDriver driver,
            By locator
    ) {
        try {
            return createWait(
                    driver,
                    ExecutionProfile.longWait()
            ).until(
                    ExpectedConditions.invisibilityOfElementLocated(locator)
            );

        } catch (TimeoutException e) {
            return false;
        }
    }

    public static boolean waitForInvisible(
            AndroidDriver driver,
            By locator,
            int timeoutSeconds
    ) {
        try {
            return createWait(
                    driver,
                    Duration.ofSeconds(timeoutSeconds)
            ).until(
                    ExpectedConditions.invisibilityOfElementLocated(locator)
            );

        } catch (TimeoutException e) {
            return false;
        }
    }

    public static boolean isVisibleQuickly(
            AndroidDriver driver,
            By locator
    ) {
        try {
            createWait(
                    driver,
                    ExecutionProfile.shortWait()
            ).until(
                    ExpectedConditions.visibilityOfElementLocated(locator)
            );

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}