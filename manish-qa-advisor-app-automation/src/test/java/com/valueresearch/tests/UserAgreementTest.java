package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.UserAgreementPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class UserAgreementTest extends BaseTest {

    @Test(priority = 1)
    public void UA_001_VerifyUserAgreementPdfFlowFromHub() {
        createExtentTest(
                "UA_001",
                "Verify User Agreement PDF flow from Hub",
                "Open Hub tab, locate More section, tap User Agreement, validate PDF download/open dialog, open PDF viewer, and return to Advisor App"
        );

        ReportLogger.step("Starting test case: UA_001 - Verify User Agreement PDF flow from Hub");

        UserAgreementPage userAgreementPage = new UserAgreementPage(driver);

        try {
            ReportLogger.step("UA STEP 01 - Capture Advisor app package");
            userAgreementPage.captureAdvisorAppPackageForUserAgreement();
            ReportLogger.pass("UA STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("UA STEP 02 - Check Advisor app login/session");
            userAgreementPage.ensureAdvisorAppLoggedInForUserAgreement();
            ReportLogger.pass("UA STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("UA STEP 03 - Open Hub from bottom navigation");
            userAgreementPage.openHubFromBottomNavigationForUserAgreement();
            ReportLogger.pass("UA STEP 03 PASSED - Hub page opened");

            ReportLogger.step("UA STEP 04 - Scroll Hub page to User Agreement option");
            userAgreementPage.scrollToUserAgreementInHubForUserAgreement();
            ReportLogger.pass("UA STEP 04 PASSED - User Agreement option is visible");

            ReportLogger.step("UA STEP 05 - Tap User Agreement option");
            userAgreementPage.tapUserAgreementForUserAgreement();
            ReportLogger.pass("UA STEP 05 PASSED - User Agreement option tapped");

            ReportLogger.step("UA STEP 06 - Validate PDF download/open dialog");
            userAgreementPage.validateUserAgreementDownloadDialogForUserAgreement();
            ReportLogger.pass("UA STEP 06 PASSED - User Agreement PDF download/open dialog validated");

            ReportLogger.step("UA STEP 07 - Tap Open on PDF dialog");
            userAgreementPage.tapOpenOnDownloadDialogForUserAgreement();
            ReportLogger.pass("UA STEP 07 PASSED - Open action completed");

            ReportLogger.step("UA STEP 08 - Validate User Agreement PDF viewer");
            userAgreementPage.validateUserAgreementPdfViewerForUserAgreement();
            ReportLogger.pass("UA STEP 08 PASSED - User Agreement PDF viewer validated");

            markPassed("UA_001 - User Agreement PDF flow validated successfully");

        } finally {
            ReportLogger.step("UA STEP 09 - Return back to Advisor App");
            userAgreementPage.returnBackToAdvisorAppSafely();
            ReportLogger.pass("UA STEP 09 COMPLETED - Return flow executed");
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: User Agreement<br>"
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