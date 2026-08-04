package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class ActionUtils {

    public static void type(AndroidDriver driver, By locator, String text) {
        WaitUtils.waitForElement(driver, locator);
        driver.findElement(locator).sendKeys(text);
    }

    public static void click(AndroidDriver driver, By locator) {
        WaitUtils.waitForElement(driver, locator);
        driver.findElement(locator).click();
    }

    public static void clearAndType(AndroidDriver driver, By locator, String text) {
        WaitUtils.waitForElement(driver, locator);
        driver.findElement(locator).clear();
        driver.findElement(locator).sendKeys(text);
    }
}