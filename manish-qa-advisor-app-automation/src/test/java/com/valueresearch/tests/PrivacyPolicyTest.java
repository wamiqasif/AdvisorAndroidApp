package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.PrivacyPolicyPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class PrivacyPolicyTest extends BaseTest {

    @Test(priority = 1)
    public void PP_001_VerifyPrivacyPolicyFromHub() {
        createExtentTest(
                "PP_001",
                "Verify Privacy Policy module from Hub",
                "Open Hub tab, locate More section, tap Privacy Policy, verify Privacy Policy page, validate content, scroll through the policy, tap actual page links, validate opened destinations, and return to Hub"
        );

        ReportLogger.step("Starting test case: PP_001 - Verify Privacy Policy module from Hub");

        PrivacyPolicyPage privacyPolicyPage = new PrivacyPolicyPage(driver);

        try {
            ReportLogger.step("PRIVACY POLICY STEP 01 - Capture Advisor app package");
            privacyPolicyPage.captureAdvisorAppPackageForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("PRIVACY POLICY STEP 02 - Check Advisor app login/session");
            privacyPolicyPage.ensureAdvisorAppLoggedInForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("PRIVACY POLICY STEP 03 - Open Hub from bottom navigation");
            privacyPolicyPage.openHubFromBottomNavigationForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 03 PASSED - Hub page opened");

            ReportLogger.step("PRIVACY POLICY STEP 04 - Scroll Hub page to Privacy Policy option");
            privacyPolicyPage.scrollToPrivacyPolicyInHubForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 04 PASSED - Privacy Policy option is visible");

            ReportLogger.step("PRIVACY POLICY STEP 05 - Tap Privacy Policy option");
            privacyPolicyPage.tapPrivacyPolicyForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 05 PASSED - Privacy Policy option tapped");

            ReportLogger.step("PRIVACY POLICY STEP 06 - Wait for Privacy Policy page");
            privacyPolicyPage.waitForPrivacyPolicyPageForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 06 PASSED - Privacy Policy page loaded");

            ReportLogger.step("PRIVACY POLICY STEP 07 - Validate Privacy Policy top content");
            privacyPolicyPage.validatePrivacyPolicyPageContentForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 07 PASSED - Privacy Policy title and top content validated");

            ReportLogger.step("PRIVACY POLICY STEP 08 - Validate Privacy Policy scrollable content");
            privacyPolicyPage.validatePrivacyPolicyScrollableContentForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 08 PASSED - Privacy Policy scrollable content validated");

            ReportLogger.step("PRIVACY POLICY STEP 09 - Validate Privacy Policy link markers");
            privacyPolicyPage.validatePrivacyPolicyLinksPresentForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 09 PASSED - Privacy Policy link markers validated");

            ReportLogger.step("PRIVACY POLICY STEP 10 - Tap actual Privacy Policy page links and validate destinations");
            privacyPolicyPage.openAndValidatePrivacyPolicyLinksForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 10 PASSED - Actual Privacy Policy links tapped and validated");

            markPassed("PP_001 - Privacy Policy module validated successfully");

        } finally {
            ReportLogger.step("PRIVACY POLICY STEP 11 - Return back to Hub");
            privacyPolicyPage.returnBackToHubSafelyForPrivacyPolicy();
            ReportLogger.pass("PRIVACY POLICY STEP 11 COMPLETED - Return flow executed");
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Privacy Policy<br>"
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
}