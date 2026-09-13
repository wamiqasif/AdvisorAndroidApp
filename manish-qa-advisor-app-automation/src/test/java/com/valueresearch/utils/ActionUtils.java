package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Common actions that use the same WebElement returned by the explicit wait.
 * This avoids a second lookup between wait completion and the action, which
 * reduces race conditions on dynamic Flutter screens.
 */
public final class ActionUtils {

    private ActionUtils() {
    }

    public static void type(
            AndroidDriver driver,
            By locator,
            String text
    ) {
        WebElement element = WaitUtils.waitForElement(
                driver,
                locator
        );

        element.sendKeys(text);
    }

    public static void click(
            AndroidDriver driver,
            By locator
    ) {
        WebElement element = WaitUtils.waitForClickable(
                driver,
                locator
        );

        element.click();
    }

    public static void clearAndType(
            AndroidDriver driver,
            By locator,
            String text
    ) {
        WebElement element = WaitUtils.waitForElement(
                driver,
                locator
        );

        element.clear();
        element.sendKeys(text);
    }
}