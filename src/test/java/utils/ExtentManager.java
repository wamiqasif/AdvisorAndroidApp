package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Singleton ExtentReports manager.
 *
 * ExtentReports itself is shared (one report file).
 * ExtentTest instances are stored in a ThreadLocal so concurrent
 * test threads each log to their own node without cross-contamination.
 */
public class ExtentManager {

    private ExtentManager() {}
    
    static String timeStamp =
            new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());

//    String reportPath =
//            System.getProperty("user.dir")
//            + "/reports/ExtentReport_"
//            + timeStamp
//            + ".html";

    private static volatile ExtentReports extentReports;

    // One ExtentTest node per thread
    private static final ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

      public static String REPORT_PATH = System.getProperty("user.dir")
            + "/reports/ExtentReport_"
            + timeStamp
            + ".html";

    // ----------------------------------------------------------------
    // ExtentReports singleton
    // ----------------------------------------------------------------

    public static ExtentReports getInstance() {
        if (extentReports == null) {
            synchronized (ExtentManager.class) {
                if (extentReports == null) {
                    extentReports = createInstance();
                }
            }
        }
        return extentReports;
    }

    private static ExtentReports createInstance() {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(REPORT_PATH);

        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("ValueResearch Advisor - Test Report");
        sparkReporter.config().setReportName("Mobile Automation Suite");
        sparkReporter.config().setTimeStampFormat("dd-MMM-yyyy HH:mm:ss");
        sparkReporter.config().setEncoding("UTF-8");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);

        reports.setSystemInfo("Application", "ValueResearch Advisor");
        reports.setSystemInfo("Platform", "Android");
        reports.setSystemInfo("Automation Tool", "Appium + UiAutomator2");
        reports.setSystemInfo("Framework", "Java + TestNG");

        return reports;
    }

    // ----------------------------------------------------------------
    // Per-thread ExtentTest
    // ----------------------------------------------------------------

    /**
     * Creates a new test node in the report for the calling thread.
     * Call this in @BeforeMethod.
     */
    public static ExtentTest createTest(String testName, String description) {
        ExtentTest test = getInstance().createTest(testName, description);
        extentTestThreadLocal.set(test);
        return test;
    }

    public static ExtentTest createTest(String testName) {
        return createTest(testName, "");
    }

    /**
     * Returns the ExtentTest node for the calling thread.
     */
    public static ExtentTest getTest() {
        ExtentTest test = extentTestThreadLocal.get();
        if (test == null) {
            throw new IllegalStateException(
                    "ExtentTest not created for thread: " + Thread.currentThread().getName()
                            + ". Call ExtentManager.createTest() in @BeforeMethod.");
        }
        return test;
    }

    /**
     * Clears the ThreadLocal for the calling thread.
     * Call this at end of @AfterMethod after all logging is done.
     */
    public static void removeTest() {
        extentTestThreadLocal.remove();
    }

    /**
     * Flushes the report to disk.
     * Call this in @AfterSuite exactly once.
     */
    public static void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
