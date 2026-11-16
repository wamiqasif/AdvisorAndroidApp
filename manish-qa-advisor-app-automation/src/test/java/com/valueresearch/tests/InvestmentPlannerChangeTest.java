package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.InvestmentPlannerChangePage;
import com.valueresearch.utils.AdvisorAccountSession;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * VRSA Investment Planner 2.0 - Your Orders -> Customize -> Adjust Weightage (weight locks).
 *
 * Account: testEmailRegression / testEmailPassRegression (AuthHelper.Account.REGRESSION),
 * logged in with the suite loginMethod (default PASSWORD) plus the configured appPin.
 *
 * Run time: the order is generated once per source and reused. Every edit-only case ends with
 * Cancel (which restores the order exactly) and stays on Your Orders; the next case reuses that
 * order after checking it is the same source and opens with no locks, otherwise it generates a
 * fresh one. Cases that change the order (group "modifies-order": IPC_010 Confirm, IPC_011 Equal
 * Weightage) and failed cases return to Hub, so the next case starts from a new order.
 *
 * The order source comes from the suite parameter "orderSource" (one <test> block per source in
 * investment-planner-change.xml): PRESET:All Weather | PRESET:Long Term | PRESET:Aggressive |
 * RECOMMENDATION:All | WATCHLIST:Watchlist 1. Without the parameter, -Dipc.source is used
 * (default PRESET:All Weather). Amount: -Dipc.amount (default 50000).
 * A case that needs more stocks than the order has is reported as SKIPPED, not FAILED.
 *
 * Retries (module-local; the shared listener's retry only covers @Test infrastructure errors):
 * - Account preparation (password login + app PIN) gets up to 3 attempts; between attempts the
 *   app is closed and relaunched so the PIN screen starts clean.
 * - Every case is re-run once from a fresh order when it fails, and a pass on the second attempt is
 *   logged as [FLAKY] with the first error. A real defect fails twice and reports both errors.
 *   Deterministic product checks (ProductBehaviourError, e.g. IPC_012) and SKIPs are not retried.
 */
public class InvestmentPlannerChangeTest extends BaseTest {

    private static final AuthHelper.Account MODULE_ACCOUNT = AuthHelper.Account.REGRESSION;
    private static final int PREPARE_ATTEMPTS = 3;
    private static final int CASE_ATTEMPTS = 2;

    /** Cases that Confirm a change; the order cannot be reused after them. */
    static final String MODIFIES_ORDER = "modifies-order";
    private static final long AMOUNT = Long.parseLong(System.getProperty("ipc.amount", "50000"));

    private InvestmentPlannerChangePage.OrderSource source;

    private InvestmentPlannerChangePage changePage;
    private AdvisorAccountSession accountSession;
    private boolean accountPrepared;
    /** True when the previous case left this source's unchanged order open on Your Orders. */
    private boolean orderReusable;
    private AuthHelper.LoginMethod selectedMethod;
    private String currentCaseId;
    private ExtentTest currentReport;

    @BeforeClass(alwaysRun = true)
    @Parameters("orderSource")
    public void resetAccountSession(@Optional("") String orderSource) {
        accountSession = null;
        orderReusable = false;
        String raw = orderSource == null || orderSource.trim().isEmpty()
                ? System.getProperty("ipc.source", System.getProperty("ipc.portfolio", "PRESET:All Weather"))
                : orderSource;
        source = InvestmentPlannerChangePage.OrderSource.parse(raw);
        ReportLogger.info("Investment Planner change suite order source: " + source.label());
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void prepareAccount(Method method, @Optional("PASSWORD") String loginMethod) {
        changePage = null;
        accountPrepared = false;
        currentCaseId = null;
        currentReport = null;
        Test annotation = method.getAnnotation(Test.class);
        String[] parts = method.getName().split("_");
        String caseId = parts.length >= 2 ? parts[0] + "_" + parts[1] : method.getName();
        startExtentCase(caseId, annotation.description(),
                "Regression account verification, password login and app PIN before feature checks");
        try {
            try {
                selectedMethod = AuthHelper.LoginMethod.valueOf(loginMethod.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Suite loginMethod must be OTP or PASSWORD");
            }
            Throwable firstFailure = null;
            for (int attempt = 1; attempt <= PREPARE_ATTEMPTS; attempt++) {
                try {
                    prepareOnce();
                    if (attempt > 1) {
                        ReportLogger.info("[FLAKY] App/account preparation succeeded on attempt " + attempt
                                + " after: " + describe(firstFailure));
                    }
                    break;
                } catch (RuntimeException | AssertionError failure) {
                    if (firstFailure == null) firstFailure = failure;
                    else if (failure != firstFailure) firstFailure.addSuppressed(failure);
                    if (accountSession != null) accountSession.invalidate();
                    orderReusable = false;
                    if (attempt == PREPARE_ATTEMPTS) {
                        if (firstFailure instanceof RuntimeException) throw (RuntimeException) firstFailure;
                        throw (AssertionError) firstFailure;
                    }
                    ReportLogger.info("App/account preparation attempt " + attempt + " of " + PREPARE_ATTEMPTS
                            + " failed (" + describe(failure) + "); relaunching the app and trying again");
                    relaunchApp();
                }
            }
            accountPrepared = true;
        } catch (RuntimeException | AssertionError | LinkageError failure) {
            if (accountSession != null) accountSession.invalidate();
            ReportLogger.fail("App/account preparation failed after " + PREPARE_ATTEMPTS + " attempts: "
                    + failure.getClass().getSimpleName() + ". See the TestNG configuration failure trace.");
            attachFailureScreenshot();
            throw failure;
        } finally {
            restoreSessionTimings();
        }
    }

    /** One preparation attempt: verified account, PIN/biometric, then Hub or the reusable order. */
    private void prepareOnce() {
        if (accountSession == null || !accountSession.isForDriver(driver)) {
            accountSession = createAccountSession(driver);
            orderReusable = false; // new driver session: the app was relaunched
        }
        InvestmentPlannerChangePage page = createPage(driver, accountSession, selectedMethod);
        // Stay on Your Orders when the verified account's previous case left the order there;
        // PIN/biometric and logged-out screens still take priority inside prepare().
        BooleanSupplier stayOnOrder = () -> orderReusable && page.isYourOrdersVisible();
        try {
            accountSession.prepare(MODULE_ACCOUNT, selectedMethod, stayOnOrder);
        } catch (IllegalStateException lateSecurityPrompt) {
            // After a driver recovery the PIN/biometric screen can appear late; prepare once more.
            if (!String.valueOf(lateSecurityPrompt.getMessage()).contains("authentication screen")) {
                throw lateSecurityPrompt;
            }
            ReportLogger.info("Security screen appeared late after app relaunch; preparing the account once more");
            accountSession.invalidate();
            orderReusable = false;
            accountSession.prepare(MODULE_ACCOUNT, selectedMethod);
        } finally {
            restoreSessionTimings();
        }
        if (!stayOnOrder.getAsBoolean()) {
            orderReusable = false;
            accountSession.assertHubVisible();
        }
        changePage = page;
    }

    /** Closes and reopens the Advisor app so the next attempt starts from a clean PIN screen. */
    private void relaunchApp() {
        String appPackage = resolveAppPackage();
        try {
            driver.terminateApp(appPackage);
        } catch (RuntimeException ignored) {
            // The app may already be closed; activateApp below starts it either way.
        }
        try {
            driver.activateApp(appPackage);
        } catch (RuntimeException relaunchFailure) {
            ReportLogger.info("App relaunch failed (" + describe(relaunchFailure) + "); the next attempt will recover");
        }
    }

    private String resolveAppPackage() {
        Object capability = driver.getCapabilities().getCapability("appium:appPackage");
        if (!(capability instanceof String) || ((String) capability).trim().isEmpty()) {
            capability = driver.getCapabilities().getCapability("appPackage");
        }
        if (capability instanceof String && !((String) capability).trim().isEmpty()) return ((String) capability).trim();
        return com.valueresearch.utils.ConfigReader.getOptional("appPackage", "com.valueresearch.advisor").trim();
    }

    private void restoreSessionTimings() {
        if (accountSession == null) return;
        try {
            accountSession.restoreTimingSettings();
        } catch (RuntimeException | AssertionError restoreFailure) {
            ReportLogger.debug("Timing restore failed: " + restoreFailure.getClass().getSimpleName());
        }
    }

    private static String describe(Throwable failure) {
        if (failure == null) return "";
        String message = String.valueOf(failure.getMessage()).replaceAll("\\s+", " ").trim();
        if (message.length() > 220) message = message.substring(0, 220) + "...";
        return failure.getClass().getSimpleName() + ": " + message;
    }

    protected AdvisorAccountSession createAccountSession(AndroidDriver currentDriver) {
        return new AdvisorAccountSession(currentDriver);
    }

    protected InvestmentPlannerChangePage createPage(AndroidDriver currentDriver, AdvisorAccountSession session,
                                                     AuthHelper.LoginMethod method) {
        return new InvestmentPlannerChangePage(currentDriver, session, MODULE_ACCOUNT, method);
    }

    /** Common start: Your Orders for this source (reused when the previous case left it unchanged). */
    private void openYourOrders() {
        changePage.openYourOrdersFor(source, AMOUNT, orderReusable);
    }

    /**
     * Runs one case: its start (Your Orders, or Adjust Weightage on a clean order) and its checks.
     * A failure is retried once from a fresh order on a fresh page object; a pass on the retry is
     * logged as [FLAKY]. Not retried: SKIPs (too few stocks) and deterministic product checks
     * (ProductBehaviourError), which would give the same answer again.
     */
    private void runCase(String caseId, boolean editMode, Consumer<InvestmentPlannerChangePage> scenario) {
        Throwable firstFailure = null;
        for (int attempt = 1; attempt <= CASE_ATTEMPTS; attempt++) {
            try {
                if (editMode) openAdjustWeightage();
                else openYourOrders();
                InvestmentPlannerChangePage page = changePage;
                runScenario(() -> scenario.accept(page));
                if (firstFailure != null) {
                    ReportLogger.info("[FLAKY] " + caseId + " passed on the retry. First attempt failed: " + describe(firstFailure));
                }
                return;
            } catch (SkipException | InvestmentPlannerChangePage.ProductBehaviourError notRetried) {
                if (firstFailure != null) notRetried.addSuppressed(firstFailure);
                throw notRetried;
            } catch (RuntimeException | AssertionError failure) {
                if (attempt == CASE_ATTEMPTS) {
                    if (firstFailure != null) failure.addSuppressed(firstFailure);
                    ReportLogger.info(caseId + " failed again on the retry; first attempt: " + describe(firstFailure));
                    throw failure;
                }
                firstFailure = failure;
                ReportLogger.info(caseId + " attempt 1 failed (" + describe(failure) + "); retrying once from a fresh order");
                attachFailureScreenshot();
                prepareRetry(failure);
            }
        }
    }

    /** Leaves the failed state, re-checks the account/PIN and starts the retry from a fresh order. */
    private void prepareRetry(Throwable cause) {
        orderReusable = false;
        try {
            if (changePage != null) changePage.navigateBackToHub();
        } catch (RuntimeException | AssertionError navigationFailure) {
            ReportLogger.info("Could not navigate back to Hub (" + describe(navigationFailure) + "); relaunching the app");
            relaunchApp();
        } finally {
            if (changePage != null) changePage.restoreReadSettings();
        }
        try {
            if (accountSession != null) accountSession.invalidate();
            prepareOnce();
        } catch (RuntimeException | AssertionError prepareFailure) {
            prepareFailure.addSuppressed(cause);
            throw prepareFailure;
        } finally {
            restoreSessionTimings();
        }
    }

    /** Runs a scenario; an order with too few stocks for it is a SKIP, not a FAIL. */
    private void runScenario(Runnable scenario) {
        try {
            scenario.run();
        } catch (InvestmentPlannerChangePage.NotEnoughStocksException tooFewStocks) {
            ReportLogger.info("Skipped for " + source.label() + ": " + tooFewStocks.getMessage());
            throw new SkipException(source.label() + ": " + tooFewStocks.getMessage());
        }
    }

    /** Common start for edit-mode cases: Your Orders -> Customize -> Adjust Weightage, with no locks. */
    private void openAdjustWeightage() {
        changePage.startEditCase(source, AMOUNT, orderReusable);
    }

    @Test(priority = 1, description = "Customize shows 4 actions; Adjust Weightage shows lock bar, padlocks and steppers")
    public void IPC_001_OpenAdjustWeightageAndVerifyEditMode() {
        logValidation("IPC_001",
                "Generate the " + source.label() + " order for ₹" + AMOUNT + ", open Customize (Add Company, Adjust Shares, "
                        + "Adjust Weightage, Equal Weightage), open Adjust Weightage: lock bar reads 'Changing a weight "
                        + "locks that stock', every row has padlock / – / weight / +, Cancel and Confirm replace Mail my "
                        + "order, weights are equal and total 100");

        runCase("IPC_001", true, page -> page.verifyEditModeLayout());

        markPassed("IPC_001", "Adjust Weightage edit mode verified");
    }

    @Test(priority = 2, description = "Padlock locks/unlocks a stock without changing weights")
    public void IPC_002_PadlockTogglesLockWithoutChangingWeight() {
        logValidation("IPC_002",
                "Tap a padlock: no weight changes and the bar reads '1 of m stocks locked' with Unlock all; "
                        + "tap again: unlocked and the bar returns to 'Changing a weight locks that stock'");

        runCase("IPC_002", true, page -> page.verifyPadlockTogglesLockWithoutChangingWeight());

        markPassed("IPC_002", "Padlock lock/unlock verified");
    }

    @Test(priority = 3, description = "+ / – share the change equally over unlocked stocks and lock the changed stock")
    public void IPC_003_StepperRedistributesAndAutoLocks() {
        logValidation("IPC_003",
                "+ on stock A: A rises, the change is shared equally by all other unlocked stocks, total stays 100, "
                        + "A is locked (1 of m); – on stock B: A stays fixed, B falls, change shared by the rest (2 of m)");

        runCase("IPC_003", true, page -> page.verifyStepperRedistributesAndAutoLocks());

        markPassed("IPC_003", "Stepper redistribution and auto-lock verified");
    }

    @Test(priority = 4, description = "Two and three locked stocks keep their weights while another stock changes")
    public void IPC_004_LockedStocksHoldWithTwoAndThreeLocks() {
        logValidation("IPC_004",
                "Lock 2 stocks with padlocks and change a third: the 2 locked weights do not move (3 of m); "
                        + "change a fourth with 3 locked: the 3 locked weights do not move (4 of m)");

        runCase("IPC_004", true, page -> page.verifyLockedStocksHoldWithTwoAndThreeLocks());

        markPassed("IPC_004", "Locked stocks held their weights with 2 and 3 locks");
    }

    @Test(priority = 5, description = "Unlock all clears every lock without changing weights")
    public void IPC_005_UnlockAllReleasesEveryLock() {
        logValidation("IPC_005",
                "Lock 3 stocks, tap Unlock all: bar returns to 'Changing a weight locks that stock', weights unchanged; "
                        + "next change is shared by all other stocks including the previously locked ones");

        runCase("IPC_005", true, page -> page.verifyUnlockAllReleasesEveryLock());

        markPassed("IPC_005", "Unlock all verified");
    }

    @Test(priority = 6, description = "Two unlocked stocks: raise one until the other is 0%, then the 0% messages appear")
    public void IPC_006_NoRoomWhenOtherUnlockedStockIsAtZero() {
        logValidation("IPC_006",
                "Lock all but 2 stocks, raise one until the other reaches 0% (locked weights fixed, total 100); "
                        + "next + shows 'No room to rebalance since the other unlocked stocks are at 0%.'; "
                        + "– on the 0% stock shows 'This stock is already at 0%.'; weights unchanged by refused taps");

        runCase("IPC_006", true, page -> page.verifyNoRoomWhenOtherUnlockedStockIsAtZero());

        markPassed("IPC_006", "Other-unlocked-at-0% and already-at-0% messages verified");
    }

    @Test(priority = 7, description = "Only one unlocked stock: + / – show 'every other stock is already set'")
    public void IPC_007_NoRoomWhenEveryOtherStockIsLocked() {
        logValidation("IPC_007",
                "Lock every stock except one; + and – on it show 'No room to rebalance since every other stock is "
                        + "already set.', its weight does not change and it is not locked");

        runCase("IPC_007", true, page -> page.verifyNoRoomWhenEveryOtherStockIsLocked());

        markPassed("IPC_007", "Every-other-stock-set message verified with one unlocked stock");
    }

    @Test(priority = 8, description = "All stocks locked: changing any stock shows 'every other stock is already set'")
    public void IPC_008_NoRoomWhenAllStocksAreLocked() {
        logValidation("IPC_008",
                "Lock every stock (m of m); + on one and – on another show 'No room to rebalance since every other "
                        + "stock is already set.' and no weight changes");

        runCase("IPC_008", true, page -> page.verifyNoRoomWhenAllStocksAreLocked());

        markPassed("IPC_008", "Every-other-stock-set message verified with all stocks locked");
    }

    @Test(priority = 9, description = "Cancel discards weight changes and locks")
    public void IPC_009_CancelDiscardsChanges() {
        logValidation("IPC_009",
                "Change a weight and lock a stock, tap Cancel: 'Changes discarded', Mail my order returns, Your Orders "
                        + "shows the original tentative weights and reopened Adjust Weightage has no locks");

        runCase("IPC_009", true, page -> page.verifyCancelDiscardsChanges());

        markPassed("IPC_009", "Cancel restored the original weights and locks");
    }

    @Test(priority = 10, groups = MODIFIES_ORDER, description = "Confirm applies adjusted weights and recalculates the order")
    public void IPC_010_ConfirmAppliesWeightsAndRecalculatesOrder() {
        logValidation("IPC_010",
                "Read the generated order, adjust weights, tap Confirm: 'Changes applied', Your Orders shows the "
                        + "adjusted tentative weights, value = shares × price, actual weight = value / amount, Σ value = "
                        + "Amount invested, and Amount invested changed (lock persistence is IPC_012)");

        runCase("IPC_010", false, page -> page.verifyConfirmAppliesWeightsAndRecalculatesOrder());

        markPassed("IPC_010", "Confirmed weights applied and order recalculated");
    }

    @Test(priority = 11, groups = MODIFIES_ORDER, description = "Equal Weightage resets weights and clears locks")
    public void IPC_011_EqualWeightageResetsWeightsAndLocks() {
        logValidation("IPC_011",
                "Adjust and lock, Confirm, then Customize -> Equal Weightage: 'Reset to equal weightage', all tentative "
                        + "weights equal and total 100, order consistent, and no locks remain");

        runCase("IPC_011", true, page -> page.verifyEqualWeightageResetsWeightsAndLocks());

        markPassed("IPC_011", "Equal Weightage reset weights and cleared locks");
    }

    @Test(priority = 12, groups = MODIFIES_ORDER,
            description = "After Confirm, adjusted weights stay; reopened Adjust Weightage starts with no locks")
    public void IPC_012_WeightsKeptLocksResetAfterConfirm() {
        logValidation("IPC_012",
                "Raise one stock (auto-lock) and lock another with its padlock ('2 of m'), tap Confirm, reopen "
                        + "Adjust Weightage: the adjusted weights are unchanged, no stock is locked, and the next change "
                        + "is shared by all other stocks including the two that were locked");

        runCase("IPC_012", true, page -> page.verifyWeightsKeptAndLocksResetAfterConfirm());

        markPassed("IPC_012", "Adjusted weights kept after Confirm; locks reset on reopening");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupCase(ITestResult result) {
        Throwable original = result.getThrowable();
        boolean failed = result.getStatus() == ITestResult.FAILURE;
        if (accountPrepared && failed) attachFailureScreenshot();
        boolean modifiesOrder = Arrays.asList(result.getMethod().getGroups()).contains(MODIFIES_ORDER);
        Throwable cleanupFailure = null;
        boolean keepOrder = false;
        try {
            if (accountPrepared && changePage != null) {
                if (!failed && !modifiesOrder) {
                    // Cancel restores the order exactly, so the next case can reuse it.
                    changePage.leaveEditModeKeepingOrder();
                    keepOrder = true;
                } else {
                    changePage.navigateBackToHub();
                }
            }
        } catch (RuntimeException | AssertionError failure) {
            // Navigation-only failure: keep the verified account; the next prepare() recovers to Hub.
            cleanupFailure = failure;
        } finally {
            orderReusable = keepOrder;
            if (changePage != null) changePage.restoreReadSettings();
            if (accountSession != null) {
                try {
                    accountSession.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                    accountSession.invalidate();
                }
            }
            accountPrepared = false;
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else {
                result.setThrowable(cleanupFailure);
                result.setStatus(ITestResult.FAILURE);
                attachFailureScreenshot();
            }
            ReportLogger.fail("Investment Planner change cleanup failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void attachFailureScreenshot() {
        try {
            String tag = source == null ? "" : "_" + source.label().replaceAll("[^A-Za-z0-9]+", "_");
            String path = ScreenshotUtils.captureScreenshot(driver, currentCaseId + tag + "_before_Hub_reset");
            if (path != null && currentReport != null) {
                currentReport.addScreenCaptureFromPath(path, "Failed state before Hub reset");
            }
        } catch (Exception unavailable) {
            ReportLogger.debug("Failure screenshot unavailable: " + unavailable.getClass().getSimpleName());
        }
    }

    private void startExtentCase(String caseId, String title, String validation) {
        String sourceLabel = source == null ? "" : " [" + source.label() + "]";
        ExtentTest test = ExtentManager.getExtentReports().createTest(caseId + sourceLabel + " - " + title);
        currentCaseId = caseId;
        currentReport = test;
        test.info("Module: VRSA Investment Planner - Adjust Weightage<br>"
                + "Case ID: " + caseId + "<br>"
                + "Page Build: " + InvestmentPlannerChangePage.BUILD_VERSION + "<br>"
                + "Account: " + MODULE_ACCOUNT + " (testEmailRegression)<br>"
                + "Order source: " + (source == null ? "?" : source.label()) + " | ₹" + AMOUNT + "<br>"
                + "Validation: " + validation);
        ExtentTestManager.setTest(test);
    }

    private void logValidation(String caseId, String validation) {
        if (currentReport == null || !caseId.equals(currentCaseId)) {
            startExtentCase(caseId, caseId, validation);
            return;
        }
        currentReport.info("Validation: " + validation);
        ExtentTestManager.setTest(currentReport);
    }

    private void markPassed(String caseId, String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + caseId + " - " + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + caseId + " - " + message);
    }
}