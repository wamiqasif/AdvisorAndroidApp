package com.valueresearch.utils;

import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    public static String captureScreenshot(AndroidDriver driver, String screenshotName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
                    .format(new Date());

            String screenshotDir = System.getProperty("user.dir")
                    + File.separator + "test-output"
                    + File.separator + "screenshots";

            new File(screenshotDir).mkdirs();

            String cleanName = screenshotName.replaceAll("[^a-zA-Z0-9_]", "_");

            String screenshotPath = screenshotDir
                    + File.separator
                    + cleanName + "_" + timestamp + ".png";

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destination = new File(screenshotPath);

            FileUtils.copyFile(source, destination);

            return screenshotPath;

        } catch (Exception e) {
            System.out.println("Screenshot capture failed: " + e.getMessage());
            return null;
        }
    }
}