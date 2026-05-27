package base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.ExtentManager;

/**
 * TestNG listener registered in testng.xml.
 * Ensures the Extent report is flushed even if the suite is aborted mid-run.
 */
public class TestListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        logger.info("[TestListener] Suite started: {}", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        // Secondary flush safety-net — primary flush is in BaseTest#afterSuite
        ExtentManager.flush();
        logger.info("[TestListener] Suite finished: {}", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        logger.info("[TestListener] Test starting: {}", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("[TestListener] Test passed: {}", result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logger.error("[TestListener] Test failed: {}", result.getName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("[TestListener] Test skipped: {}", result.getName());
    }

    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
}
