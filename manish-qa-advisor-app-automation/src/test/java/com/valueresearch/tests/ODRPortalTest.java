package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.ODRPortalPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class ODRPortalTest extends BaseTest {

    @Test(priority = 1)
    public void ODR_001_VerifyODRPortalRedirectionFromHub() {
        createExtentTest(
                "ODR_001",
                "Verify ODR Portal redirection from Hub",
                "Open Hub tab, locate More section, tap ODR Portal, verify external smartodr.in Investor Login page opens, and return to Advisor App"
        );

        ReportLogger.step("Starting test case: ODR_001 - Verify ODR Portal redirection from Hub");

        ODRPortalPage odrPortalPage = new ODRPortalPage(driver);

        try {
            ReportLogger.step("ODR STEP 01 - Capture Advisor app package");
            odrPortalPage.captureAdvisorAppPackageForODR();
            ReportLogger.pass("ODR STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("ODR STEP 02 - Check Advisor app login/session");
            odrPortalPage.ensureAdvisorAppLoggedInForODR();
            ReportLogger.pass("ODR STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("ODR STEP 03 - Open Hub from bottom navigation");
            odrPortalPage.openHubFromBottomNavigationForODR();
            ReportLogger.pass("ODR STEP 03 PASSED - Hub page opened");

            ReportLogger.step("ODR STEP 04 - Scroll Hub page to ODR Portal option");
            odrPortalPage.scrollToODRPortalInHubForODR();
            ReportLogger.pass("ODR STEP 04 PASSED - ODR Portal option is visible");

            ReportLogger.step("ODR STEP 05 - Tap ODR Portal option");
            odrPortalPage.tapODRPortalForODR();
            ReportLogger.pass("ODR STEP 05 PASSED - ODR Portal option tapped");

            ReportLogger.step("ODR STEP 06 - Validate external browser/custom tab opened");
            odrPortalPage.waitForODRExternalPageForODR();
            ReportLogger.pass("ODR STEP 06 PASSED - External ODR/browser page opened");

            ReportLogger.step("ODR STEP 07 - Validate smartodr Investor Login page content");
            odrPortalPage.validateODRLoginPageContentForODR();
            ReportLogger.pass("ODR STEP 07 PASSED - ODR Investor Login page validated");

            markPassed("ODR_001 - ODR Portal redirection validated successfully");

        } finally {
            ReportLogger.step("ODR STEP 08 - Return back to Advisor App");
            odrPortalPage.returnBackToAdvisorAppSafely();
            ReportLogger.pass("ODR STEP 08 COMPLETED - Return flow executed");
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: ODR Portal<br>"
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