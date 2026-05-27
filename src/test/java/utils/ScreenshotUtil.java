package utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots on test failure and saves them under reports/screenshots/.
 * Returns the saved file path so BaseTest can embed it in the Extent report.
 */
public class ScreenshotUtil {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOT_DIR = "reports/screenshots";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotUtil() {}

    /**
     * Captures the current screen and writes it to reports/screenshots/<testName>_<timestamp>.png.
     *
     * @param driver   the active AndroidDriver for this thread
     * @param testName used as the filename prefix (spaces replaced with underscores)
     * @return absolute path to the saved PNG, or null if the capture failed
     */
    public static String capture(AndroidDriver driver, String testName) {
        if (driver == null) {
            logger.warn("[ScreenshotUtil] Driver is null — screenshot skipped for: {}", testName);
            return null;
        }

        try {
            // Ensure the output directory exists
            Path dir = Paths.get(SCREENSHOT_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String safeName = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String fileName = safeName + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png";
            Path destination = dir.resolve(fileName);

            // Take screenshot via Selenium TakesScreenshot interface
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(srcFile.toPath(), destination);

            String absolutePath = destination.toAbsolutePath().toString();
            logger.info("[ScreenshotUtil] Screenshot saved: {}", absolutePath);
            return absolutePath;

        } catch (IOException e) {
            logger.error("[ScreenshotUtil] Failed to save screenshot for '{}': {}", testName, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("[ScreenshotUtil] Unexpected error capturing screenshot for '{}': {}", testName, e.getMessage());
            return null;
        }
    }
}
