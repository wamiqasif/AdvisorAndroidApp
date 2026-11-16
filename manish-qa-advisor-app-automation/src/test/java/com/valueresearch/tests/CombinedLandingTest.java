package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.CombinedLandingPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import com.valueresearch.utils.ScreenshotUtils;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public class CombinedLandingTest extends BaseTest {

    private CombinedLandingPage landingPage;
    private ExtentTest currentReport;
    private AuthHelper.LoginMethod loginMethod;
    private boolean subscriberRoutingCase;

    @BeforeClass(alwaysRun = true)
    public void resetLandingSession() {
        landingPage = null;
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("loginMethod")
    public void prepareCombinedLanding(Method method, @Optional("PASSWORD") String selectedMethod) {
        currentReport = ExtentManager.getExtentReports().createTest(
                method.getName() + " - " + method.getAnnotation(Test.class).description());
        ExtentTestManager.setTest(currentReport);
        subscriberRoutingCase = Arrays.asList(method.getAnnotation(Test.class).groups()).contains("subscriber-routing");
        try {
            loginMethod = AuthHelper.LoginMethod.valueOf(
                    selectedMethod.trim().toUpperCase(Locale.ROOT));
            if (landingPage == null || !landingPage.isForDriver(driver)) {
                landingPage = new CombinedLandingPage(driver);
            }
            if (subscriberRoutingCase) landingPage.prepareSubscriberRouting();
            else landingPage.ensureNonSubscriberLanding(loginMethod);
        } catch (RuntimeException | AssertionError failure) {
            attachFailureScreenshot(method.getName() + "_setup");
            throw failure;
        }
    }

    // TC_001: Verify the landing content immediately; setup logs in only when the login screen is visible.
    @Test(priority = 1, description = "Verify combined landing content after login or existing session reuse")
    public void TC_001_VerifyNonSubscriberLoginAndCombinedLanding() {
        landingPage.verifyLandingHeader();
    }

    // TC_002: Check all supplied static copy, including both composite service cards and the bottom footer.
    @Test(priority = 2, description = "Verify all combined landing content including the bottom compliance copy")
    public void TC_002_VerifyAllCombinedLandingContent() {
        landingPage.verifyAllLandingContent();
    }

    // TC_003: Verify the native Fund plan screen and return through its Go back button.
    @Test(priority = 3, description = "Verify native Fund Advisor subscription content and Go back navigation")
    public void TC_003_VerifyFundAdvisorSubscriptionAndBack() {
        verifySubscriptionAndReturn("TC_003", landingPage::openFundAdvisor,
                landingPage::verifyFundAdvisorSubscription, landingPage::returnFromFundAdvisor);
    }

    // TC_004: The supplied Stock plan screen is native and exposes the same Go back control.
    @Test(priority = 4, description = "Verify native Stock Advisor subscription content and Go back navigation")
    public void TC_004_VerifyStockAdvisorSubscriptionAndBack() {
        verifySubscriptionAndReturn("TC_004", landingPage::openStockAdvisor,
                landingPage::verifyStockAdvisorSubscription, landingPage::returnFromStockAdvisor);
    }

    @DataProvider(name = "activeSubscriberAccounts", parallel = false)
    public Object[][] activeSubscriberAccounts() {
        return new Object[][]{
                {AuthHelper.Account.ACTIVE_SUBSCRIBER, "ActiveSubscriberEmail (active subscriber to either service)"},
                {AuthHelper.Account.DEFAULT, "testEmail (active subscriber to both services)"}
        };
    }

    // TC_005: Fresh login for each active account; assert default Portfolio before identity verification and Hub.
    @Test(priority = 5, groups = "subscriber-routing", dataProvider = "activeSubscriberAccounts",
            description = "Verify active subscribers land on Portfolio by default and finish on Hub")
    public void TC_005_VerifyActiveSubscriberDefaultPortfolioAndHub(AuthHelper.Account account, String profile) {
        currentReport.getModel().setName("TC_005 - " + profile + " - default Portfolio, then Hub");
        currentReport.info("Configured account profile: " + profile);
        verifySubscriberRoutingCase("TC_005", account);
    }

    // TC_006: A configured lapsed account must also land on Portfolio after a fresh login.
    @Test(priority = 6, groups = "subscriber-routing",
            description = "Verify the lapsed subscriber lands on Portfolio by default and finishes on Hub")
    public void TC_006_VerifyLapsedSubscriberDefaultPortfolioAndHub() {
        currentReport.info("Configured account profile: LapsedUserEmail");
        verifySubscriberRoutingCase("TC_006", AuthHelper.Account.LAPSED_USER);
    }

    private void verifySubscriberRoutingCase(String caseId, AuthHelper.Account account) {
        Throwable validationFailure = null;
        try {
            landingPage.verifySubscriberDefaultPortfolio(account, loginMethod);
        } catch (RuntimeException | AssertionError failure) {
            validationFailure = failure;
            attachFailureScreenshot(caseId + "_" + account.name() + "_before_Hub");
            throw failure;
        } finally {
            try {
                landingPage.finishSubscriberOnHub();
            } catch (RuntimeException | AssertionError hubFailure) {
                if (validationFailure == null) throw hubFailure;
                validationFailure.addSuppressed(hubFailure);
            }
        }
    }

    private void verifySubscriptionAndReturn(String caseId, Runnable openSubscription,
                                            Runnable verifySubscription, Runnable returnToLanding) {
        Throwable validationFailure = null;
        try {
            openSubscription.run();
            verifySubscription.run();
        } catch (RuntimeException | AssertionError failure) {
            validationFailure = failure;
            attachFailureScreenshot(caseId + "_before_Go_back");
            throw failure;
        } finally {
            try {
                returnToLanding.run();
            } catch (RuntimeException | AssertionError backFailure) {
                if (validationFailure == null) throw backFailure;
                validationFailure.addSuppressed(backFailure);
            }
        }
    }

    @AfterMethod(alwaysRun = true)
    public void restoreExpectedDestination(ITestResult result) {
        Throwable original = result.getThrowable();
        if (original != null) attachFailureScreenshot(result.getName() + "_before_recovery");
        Throwable cleanupFailure = null;
        try {
            if (landingPage != null) {
                if (subscriberRoutingCase && landingPage.hasSubscriberRoutingStarted()) {
                    landingPage.finishSubscriberOnHub();
                } else if (!subscriberRoutingCase && landingPage.hasPreparedLandingSession()) {
                    landingPage.recoverToCombinedLanding();
                }
            }
        } catch (RuntimeException | AssertionError failure) {
            cleanupFailure = failure;
        } finally {
            if (landingPage != null) {
                try {
                    landingPage.restoreTimingSettings();
                } catch (RuntimeException | AssertionError failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                }
            }
        }
        if (cleanupFailure != null) {
            if (original != null) original.addSuppressed(cleanupFailure);
            else {
                result.setThrowable(cleanupFailure);
                result.setStatus(ITestResult.FAILURE);
                attachFailureScreenshot(result.getName() + "_cleanup");
            }
            ReportLogger.fail("Case destination recovery failed: " + cleanupFailure.getClass().getSimpleName());
        }
    }

    private void attachFailureScreenshot(String name) {
        String path = ScreenshotUtils.captureScreenshot(driver, name);
        if (path != null && currentReport != null) {
            try {
                currentReport.addScreenCaptureFromPath(path, "State before case recovery");
            } catch (Exception failure) {
                ReportLogger.debug("Unable to attach combined landing screenshot: " + failure.getClass().getSimpleName());
            }
        }
    }
}
