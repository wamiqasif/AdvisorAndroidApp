package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.RefundPolicyPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class RefundPolicyTest extends BaseTest {

    @Test(priority = 1)
    public void RP_001_VerifyRefundPolicyFromHub() {
        createExtentTest(
                "RP_001",
                "Verify Refund Policy module from Hub",
                "Open Hub tab, locate More section, tap Refund Policy, verify Refund and Cancellation Policy page, validate policy content and email link response, and return to Hub"
        );

        ReportLogger.step("Starting test case: RP_001 - Verify Refund Policy module from Hub");

        RefundPolicyPage refundPolicyPage = new RefundPolicyPage(driver);
        boolean hubWasOpened = false;

        try {
            ReportLogger.step("REFUND POLICY STEP 01 - Capture Advisor app package");
            refundPolicyPage.captureAdvisorAppPackageForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("REFUND POLICY STEP 02 - Check Advisor app login/session");
            refundPolicyPage.ensureAdvisorAppLoggedInForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("REFUND POLICY STEP 03 - Open Hub from bottom navigation");
            refundPolicyPage.openHubFromBottomNavigationForRefundPolicy();
            hubWasOpened = true;
            ReportLogger.pass("REFUND POLICY STEP 03 PASSED - Hub page opened");

            ReportLogger.step("REFUND POLICY STEP 04 - Scroll Hub page to Refund Policy option");
            refundPolicyPage.scrollToRefundPolicyInHubForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 04 PASSED - Refund Policy option is visible");

            ReportLogger.step("REFUND POLICY STEP 05 - Tap Refund Policy option");
            refundPolicyPage.tapRefundPolicyForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 05 PASSED - Refund Policy option tapped");

            ReportLogger.step("REFUND POLICY STEP 06 - Wait for Refund Policy page");
            refundPolicyPage.waitForRefundPolicyPageForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 06 PASSED - Refund Policy page loaded");

            ReportLogger.step("REFUND POLICY STEP 07 - Validate Refund Policy page content");
            refundPolicyPage.validateRefundPolicyPageContentForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 07 PASSED - Refund Policy page content validated");

            ReportLogger.step("REFUND POLICY STEP 08 - Validate Refund Policy email link response");
            refundPolicyPage.validateRefundPolicyEmailLinkForRefundPolicy();
            ReportLogger.pass("REFUND POLICY STEP 08 PASSED - Refund Policy email link response validated");

            markPassed("RP_001 - Refund Policy module validated successfully");

        } finally {
            if (hubWasOpened) {
                ReportLogger.step("REFUND POLICY STEP 09 - Return back to Hub");

                try {
                    refundPolicyPage.returnBackToHubSafelyForRefundPolicy();
                    ReportLogger.pass("REFUND POLICY STEP 09 COMPLETED - Return flow executed");
                } catch (AssertionError e) {
                    ReportLogger.debug("REFUND POLICY STEP 09 cleanup assertion skipped/failed: "
                            + safeMessage(e));
                } catch (Exception e) {
                    ReportLogger.debug("REFUND POLICY STEP 09 cleanup skipped/failed: "
                            + safeMessage(e));
                }
            } else {
                ReportLogger.debug("REFUND POLICY STEP 09 SKIPPED - Hub was never opened because test failed earlier");
            }
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Refund Policy<br>"
                + "Case ID: " + caseId + "<br>"
                + "Validation: " + validation);

        ExtentTestManager.setTest(test);
    }

    private void markPassed(String message) {
        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>" + message + "</span>"
        );
        ReportLogger.pass("Completed test case: " + message);
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        String message = throwable.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getSimpleName();
        }

        return message;
    }
}