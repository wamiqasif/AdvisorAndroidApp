// Automation active for 20-Oct-2026 to 19-Nov-2026 only.
package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.valueresearch.base.BaseTest;
import com.valueresearch.listeners.ExtentTestListener;
import com.valueresearch.pages.MFIanniversaryvroPage;
import com.valueresearch.utils.ConfigReader;
import com.valueresearch.utils.DriverManager;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.Assert;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MFIanniversaryvroTest extends BaseTest {

    private MFIanniversaryvroPage page;
    private boolean paymentSubmissionAttempted;
    private boolean printPaymentSubmissionAttempted;
    private final Map<String, List<String>> retryHistory = new HashMap<>();

    @Override
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        // Connect in @BeforeMethod: a temporary class-start failure must not skip every testcase.
        boundedSetting("MFItestRetryCount", 1, 0, 2);
        boundedSetting("MFIrecoveryAttempts", 2, 1, 3);
    }

    @Override
    @BeforeMethod(alwaysRun = true)
    public void ensureHealthyDriverBeforeEveryTest() {
        recoverVroHome("Before-test recovery");
    }

    private void initializeVroModule() {
        driver = DriverManager.getDriver(
                "VROappPackage",
                "VROappActivity"
        );

        // Rebind the page if infrastructure recovery created a new driver.
        page = new MFIanniversaryvroPage(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void returnToVroHomeAfterEveryTest() {
        recoverVroHome("Post-test reset");
        ReportLogger.pass("Post-test reset completed: VRO Home page verified");
    }

    private void recoverVroHome(String stage) {
        int attempts = boundedSetting("MFIrecoveryAttempts", 2, 1, 3);
        Throwable firstFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                // DriverManager reuses healthy sessions and rebuilds dead UiAutomator2 sessions.
                initializeVroModule();
                page.resetToVroHome();
                return;
            } catch (RuntimeException | AssertionError failure) {
                // Missing/invalid configuration cannot be repaired by restarting Appium.
                if (failure instanceof IllegalArgumentException) throw (IllegalArgumentException) failure;
                if (firstFailure == null) firstFailure = failure;
                else if (firstFailure != failure) firstFailure.addSuppressed(failure);
                if (attempt == attempts) break;
                ReportLogger.info(stage + " failed; recreating the VRO session for recovery attempt "
                        + (attempt + 1) + " of " + attempts);
                DriverManager.quitDriver();
                driver = null;
                page = null;
            }
        }
        DriverManager.quitDriver();
        driver = null;
        page = null;
        // Keep an unrecoverable Home reset visible; suite configuration allows later tests to recover.
        throw new IllegalStateException(stage + " could not restore VRO Home after " + attempts
                + " recovery attempts", firstFailure);
    }

    @Test(
            priority = 2,
            retryAnalyzer = MfiRetryAnalyzer.class,
            description = "Verify Subscribe opens Store with Magazines and MFI card"
    )
    public void MFI_VRO_002_VerifySubscribeStoreNavigation() {
        createReport(
                "MFI_VRO_002",
                "Verify Subscribe navigation and magazine card"
        );

        // TC_002: Start independently from the verified VRO Home page.
        page.ensureHomePage();
        page.openStoreAndVerifyMagazineCard();

        ReportLogger.pass("MFI_VRO_002 completed");
    }

    @Test(
            priority = 3,
            retryAnalyzer = MfiRetryAnalyzer.class,
            description = "TC_003: Verify Anniversary Special and Subscribe Now on the MFI card"
    )
    public void MFI_VRO_003_VerifyAnniversaryMagazineCard() {
        createReport("MFI_VRO_003", "MFI magazine card verification");
        page.verifyMfiAnniversaryMagazineCard();
        ReportLogger.pass("MFI_VRO_003 completed");
    }

    @Test(
            priority = 4,
            retryAnalyzer = MfiRetryAnalyzer.class,
            description = "TC_004: Verify subscription plans and remaining landing-page content"
    )
    public void MFI_VRO_004_VerifySubscriptionPlansAndContent() {
        createReport("MFI_VRO_004", "MFI subscription plans and page content");
        page.verifySubscriptionPlansAndContent();
        ReportLogger.pass("MFI_VRO_004 completed");
    }

    @Test(
            priority = 5,
            retryAnalyzer = MfiRetryAnalyzer.class,
            description = "TC_005: Verify Invest in Clarity navigates to Checkout"
    )
    public void MFI_VRO_005_VerifyCheckoutNavigation() {
        createReport("MFI_VRO_005", "Invest in Clarity to Checkout navigation");
        page.verifyInvestInClarityCheckoutNavigation();
        ReportLogger.pass("MFI_VRO_005 completed");
    }

    @Test(
            priority = 6,
            retryAnalyzer = NoPaymentRetry.class,
            description = "TC_006: Purchase Digital 3 Years through PayU sandbox and return Home"
    )
    public void MFI_VRO_006_PurchaseThreeYearDigitalPlan() {
        createReport("MFI_VRO_006", "Digital 3 Years sandbox purchase and My Subscription navigation");
        // Also protects a direct class run if an external transformer overrides the suite policy.
        Assert.assertTrue(!paymentSubmissionAttempted,
                "Payment submission was already attempted in this run; inspect the original result before rerunning");
        page.purchaseThreeYearDigitalPlan(() -> paymentSubmissionAttempted = true);
        ReportLogger.pass("MFI_VRO_006 completed; VRO Home verified");
    }

    @Test(
            priority = 7,
            retryAnalyzer = NoPaymentRetry.class,
            description = "TC_007: Purchase Print + Digital 3 Years with the second billing address and return Home"
    )
    public void MFI_VRO_007_PurchaseThreeYearPrintDigitalPlan() {
        createReport("MFI_VRO_007", "Print + Digital 3 Years sandbox purchase and address verification");
        // Independent of TC_006's guard: each purchase can submit once in this run.
        Assert.assertTrue(!printPaymentSubmissionAttempted,
                "Print payment submission was already attempted in this run; inspect the original result before rerunning");
        page.purchaseThreeYearPrintDigitalPlan(() -> printPaymentSubmissionAttempted = true);
        ReportLogger.pass("MFI_VRO_007 completed; VRO Home verified");
    }

    @Test(
            priority = 8,
            retryAnalyzer = MfiRetryAnalyzer.class,
            description = "TC_008: Verify 2025/2024 archives and three visible magazine issues without dropdown scrolling"
    )
    public void MFI_VRO_008_VerifyProfileSubscriptionHistory() {
        createReport("MFI_VRO_008", "Subscription archives and magazine assets in the initial viewport; dropdown taps only");
        page.verifyProfileSubscriptionHistory();
        ReportLogger.pass("MFI_VRO_008 completed; VRO Home verified");
    }

    public static class NoPaymentRetry implements IRetryAnalyzer {
        @Override
        public boolean retry(ITestResult result) { return false; }
    }

    /** Retry UI/transport errors and assertions in non-payment tests; the final attempt stays strict. */
    public static class MfiRetryAnalyzer implements IRetryAnalyzer {
        private final Map<String, Integer> retries = new HashMap<>();

        @Override
        public synchronized boolean retry(ITestResult result) {
            if (result == null) return false;
            // Keep a reused ITestResult from retaining the previous attempt's scheduled flag.
            result.setAttribute("MFI_RETRY_SCHEDULED", false);
            if (result.getMethod() == null || !isRetryableFailure(result.getThrowable()))
                return false;
            String method = result.getMethod().getMethodName();
            // Defense against an external annotation transformer assigning this analyzer to a purchase test.
            if ("MFI_VRO_006_PurchaseThreeYearDigitalPlan".equals(method)
                    || "MFI_VRO_007_PurchaseThreeYearPrintDigitalPlan".equals(method)) return false;
            int maximum = boundedSetting("MFItestRetryCount", 1, 0, 2);
            int used = retries.getOrDefault(method, 0);
            if (used >= maximum) {
                ReportLogger.info("Retry limit reached for " + method + "; keeping the final failure");
                return false;
            }
            retries.put(method, used + 1);
            String reason = "Attempt " + (used + 1) + " failed with "
                    + result.getThrowable().getClass().getSimpleName();
            result.setAttribute("MFI_RETRY_SCHEDULED", true);
            result.setAttribute("MFI_RETRY_REASON", reason);
            if (result.getInstance() instanceof MFIanniversaryvroTest) {
                MFIanniversaryvroTest instance = (MFIanniversaryvroTest) result.getInstance();
                instance.retryHistory.computeIfAbsent(method, key -> new ArrayList<>()).add(reason);
            }
            ReportLogger.info("Failure in " + method + "; retry " + (used + 1) + " of " + maximum
                    + " will start from a verified VRO Home page");
            return true;
        }

        private static boolean isRetryableFailure(Throwable failure) {
            boolean assertion = false;
            for (Throwable current = failure; current != null; current = current.getCause()) {
                // A retry cannot correct malformed configuration or programmer arguments.
                if (current instanceof IllegalArgumentException) return false;
                if (current instanceof AssertionError) assertion = true;
            }
            return assertion || isTransientFailure(failure);
        }

        private static boolean isTransientFailure(Throwable failure) {
            for (Throwable current = failure; current != null; current = current.getCause()) {
                String type = current.getClass().getSimpleName();
                if (type.equals("TimeoutException") || type.equals("StaleElementReferenceException")
                        || type.equals("NoSuchElementException") || type.equals("NoSuchSessionException")
                        || type.equals("SessionNotCreatedException") || type.equals("UnreachableBrowserException")
                        || type.equals("SocketTimeoutException") || type.equals("ConnectException")) return true;
                // Assertions use the bounded assertion policy, rather than message token matching.
                if (current instanceof AssertionError) continue;
                String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase(Locale.ROOT);
                if (message.contains("instrumentation process is not running")
                        || message.contains("cannot be proxied to uiautomator2")
                        || message.contains("could not proxy command") || message.contains("socket hang up")
                        || message.contains("connection reset") || message.contains("connection refused")
                        || message.contains("broken pipe") || message.contains("device offline")
                        || message.contains("device is not connected")) return true;
            }
            return false;
        }
    }

    /** Applies the MFI policy only to this module; other modules keep their existing policy. */
    public static class MfiSuiteListener extends ExtentTestListener {
        @Override
        public void onTestSkipped(ITestResult result) {
            if (!discardScheduledMfiRetry(result)) super.onTestSkipped(result);
        }

        @Override
        public void onTestFailure(ITestResult result) {
            if (!discardScheduledMfiRetry(result)) super.onTestFailure(result);
        }

        @Override
        public void onTestFailedWithTimeout(ITestResult result) {
            if (!discardScheduledMfiRetry(result)) super.onTestFailedWithTimeout(result);
        }

        private boolean discardScheduledMfiRetry(ITestResult result) {
            if (result == null || !Boolean.TRUE.equals(result.getAttribute("MFI_RETRY_SCHEDULED")))
                return false;
            ExtentTest attempt = ExtentTestManager.getTest();
            try {
                // TestNG marks a retried attempt SKIP. The parent listener logs its throwable as FAIL.
                // Remove only the scheduled intermediate attempt; retain a final exhausted failure.
                if (attempt != null) ExtentManager.getExtentReports().removeTest(attempt);
            } catch (RuntimeException reportingFailure) {
                if (attempt != null) {
                    attempt.log(Status.SKIP, "Intermediate attempt scheduled for retry");
                    attempt.log(Status.INFO, "Unable to remove the intermediate retry entry: "
                            + reportingFailure.getClass().getSimpleName());
                }
            } finally {
                ExtentTestManager.unload();
            }
            return true;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public void transform(ITestAnnotation annotation, Class testClass,
                              Constructor testConstructor, Method testMethod) {
            if (annotation != null && testMethod != null
                    && ("MFI_VRO_006_PurchaseThreeYearDigitalPlan".equals(testMethod.getName())
                    || "MFI_VRO_007_PurchaseThreeYearPrintDigitalPlan".equals(testMethod.getName()))) {
                annotation.setRetryAnalyzer(NoPaymentRetry.class);
            } else if (annotation != null && testMethod != null
                    && MFIanniversaryvroTest.class.isAssignableFrom(testMethod.getDeclaringClass())) {
                annotation.setRetryAnalyzer(MfiRetryAnalyzer.class);
            } else {
                super.transform(annotation, testClass, testConstructor, testMethod);
            }
        }
    }

    private static int boundedSetting(String key, int fallback, int minimum, int maximum) {
        String value = ConfigReader.getOptional(key, String.valueOf(fallback));
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum)
                throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(key + " must be an integer", failure);
        }
    }

    private void createReport(String id, String description) {
        ExtentTestManager.unload();

        ExtentTestManager.setTest(
                ExtentManager.getExtentReports().createTest(
                        id + " - " + description,
                        description
                )
        );

        ReportLogger.step("Starting " + id);
        retryHistory.forEach((method, attempts) -> {
            if (method.startsWith(id + "_"))
                for (String attempt : attempts) ReportLogger.info("Retry history: " + attempt);
        });
        // Records the expected account; login is an existing prerequisite.
        ReportLogger.info("Expected VRO test account: " + ConfigReader.get("VRO_TEST_USER"));
    }

    // Driver cleanup is inherited from BaseTest.
}
