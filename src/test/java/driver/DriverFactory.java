package driver;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConfigReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe Appium driver factory for parallel test execution.
 *
 * Design principles:
 *   - ThreadLocal<AndroidDriver> — each thread owns its driver; no shared mutable state.
 *   - NOT synchronized — ThreadLocal access is inherently per-thread; synchronizing would
 *     serialize driver creation across threads without any safety benefit.
 *   - AtomicInteger port counter — sequential port allocation prevents the collision risk
 *     of random port selection when two threads start simultaneously (was: Random.nextInt).
 *   - isDriverAlive() probes the Appium server — a null SessionId check can return true
 *     for a session that crashed silently. getCurrentPackage() is lightweight and confirms
 *     the server can actually reach the device.
 */
public final class DriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);

    private static final int SYSTEM_PORT_RANGE = 100;

    // Sequential counter — thread 0 gets base, thread 1 gets base+1, etc.
    // Wraps after SYSTEM_PORT_RANGE so long suites don't exhaust the range.
    private static final AtomicInteger portCounter = new AtomicInteger(0);

    // One driver per thread — ThreadLocal provides isolation without locking
    private static final ThreadLocal<AndroidDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverFactory() {}

    // ----------------------------------------------------------------
    // Driver lifecycle
    // ----------------------------------------------------------------

    /**
     * Initializes AndroidDriver for the calling thread.
     * No-ops if a healthy session already exists (idempotent — safe to call from @BeforeClass
     * even if a prior class's @BeforeClass already initialized the driver).
     */
    public static void initDriver() {
        if (isDriverAlive()) {
            return;
        }
        // Remove any stale reference before creating a fresh session
        quitDriver();

        ConfigReader config = ConfigReader.getInstance();
        try {
            UiAutomator2Options options = buildCapabilities(config);
            URL appiumUrl = new URL(config.getAppiumURL());

            AndroidDriver driver = new AndroidDriver(appiumUrl, options);
            // Disable implicit wait — framework uses explicit waits exclusively
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driverThreadLocal.set(driver);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL: " + config.getAppiumURL(), e);
        } catch (Exception e) {
            throw new RuntimeException("Could not start AndroidDriver session", e);
        }
    }

    /**
     * Returns the AndroidDriver for the calling thread.
     * Throws IllegalStateException if initDriver() has not been called on this thread.
     */
    public static AndroidDriver getDriver() {
        AndroidDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "Driver not initialized for thread: " + Thread.currentThread().getName()
                    + ". Call DriverFactory.initDriver() first.");
        }
        return driver;
    }

    /**
     * Quits the driver for the calling thread and removes the ThreadLocal reference.
     * Safe to call even if the driver is already null or dead.
     */
    public static void quitDriver() {
        AndroidDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.warn("[DriverFactory] Quit failed: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    // ----------------------------------------------------------------
    // Driver health
    // ----------------------------------------------------------------

    /**
     * Returns true if the driver session is alive and the Appium server is responsive.
     *
     * Uses getCurrentPackage() as a lightweight health probe — it is far cheaper than
     * getPageSource() but still verifies the Appium server can reach the device.
     * Checking only SessionId != null is insufficient: a crashed session retains its ID.
     */
    public static boolean isDriverAlive() {
        AndroidDriver driver = driverThreadLocal.get();
        if (driver == null) return false;
        try {
            SessionId sessionId = driver.getSessionId();
            if (sessionId == null) return false;
            driver.getCurrentPackage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Quits the current session and starts a fresh one on the same thread. */
    public static void restartDriver() {
        quitDriver();
        initDriver();
    }

    // ----------------------------------------------------------------
    // Capability builder
    // ----------------------------------------------------------------

    private static UiAutomator2Options buildCapabilities(ConfigReader config) {
        UiAutomator2Options options = new UiAutomator2Options();

        options.setPlatformName(config.getPlatformName());
        options.setAutomationName(config.getAutomationName());
        options.setDeviceName(config.getDeviceName());
        options.setUdid(config.getUdid());
        options.setPlatformVersion(config.getPlatformVersion());
        options.setAppPackage(config.getAppPackage());
        options.setAppActivity(config.getAppActivity());
        options.setNoReset(config.isNoReset());
        options.setAutoGrantPermissions(config.isAutoGrantPermissions());
        options.setNewCommandTimeout(Duration.ofSeconds(config.getNewCommandTimeout()));

        // Reduce animation jitter during test execution
        options.setDisableWindowAnimation(true);
        options.setIgnoreHiddenApiPolicyError(true);

        int systemPort = allocateSystemPort(config);
        options.setSystemPort(systemPort);

        if (config.isEmulator()) {
            options.setIsHeadless(false);
            options.setAvd(config.getDeviceName());
        }
        return options;
    }

    // ----------------------------------------------------------------
    // Port allocation — sequential, not random, to avoid collisions
    // ----------------------------------------------------------------

    private static int allocateSystemPort(ConfigReader config) {
        // Each call increments the counter so concurrent threads get distinct ports.
        // Modulo wraps back to base after SYSTEM_PORT_RANGE ports are exhausted.
        return config.getSystemPortBase() + (portCounter.getAndIncrement() % SYSTEM_PORT_RANGE);
    }
}
