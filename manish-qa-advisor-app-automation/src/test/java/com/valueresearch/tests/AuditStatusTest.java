package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.AuditStatusPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class AuditStatusTest extends BaseTest {

    @Test(priority = 1)
    public void AS_001_VerifyAuditStatusFromHub() {
        createExtentTest(
                "AS_001",
                "Verify Audit Status module from Hub",
                "Open Hub tab, locate More section, tap Audit Status, verify Annual Compliance Audit page, validate disclosure and table data, and return to Hub"
        );

        ReportLogger.step("Starting test case: AS_001 - Verify Audit Status module from Hub");

        AuditStatusPage auditStatusPage = new AuditStatusPage(driver);

        try {
            ReportLogger.step("AUDIT STEP 01 - Capture Advisor app package");
            auditStatusPage.captureAdvisorAppPackageForAudit();
            ReportLogger.pass("AUDIT STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("AUDIT STEP 02 - Check Advisor app login/session");
            auditStatusPage.ensureAdvisorAppLoggedInForAudit();
            ReportLogger.pass("AUDIT STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("AUDIT STEP 03 - Open Hub from bottom navigation");
            auditStatusPage.openHubFromBottomNavigationForAudit();
            ReportLogger.pass("AUDIT STEP 03 PASSED - Hub page opened");

            ReportLogger.step("AUDIT STEP 04 - Scroll Hub page to Audit Status option");
            auditStatusPage.scrollToAuditStatusInHubForAudit();
            ReportLogger.pass("AUDIT STEP 04 PASSED - Audit Status option is visible");

            ReportLogger.step("AUDIT STEP 05 - Tap Audit Status option");
            auditStatusPage.tapAuditStatusForAudit();
            ReportLogger.pass("AUDIT STEP 05 PASSED - Audit Status option tapped");

            ReportLogger.step("AUDIT STEP 06 - Wait for Audit Status page");
            auditStatusPage.waitForAuditStatusPageForAudit();
            ReportLogger.pass("AUDIT STEP 06 PASSED - Audit Status page loaded");

            ReportLogger.step("AUDIT STEP 07 - Validate Audit Status page content");
            auditStatusPage.validateAuditStatusPageContentForAudit();
            ReportLogger.pass("AUDIT STEP 07 PASSED - Audit Status title, disclosure and headers validated");

            ReportLogger.step("AUDIT STEP 08 - Validate Audit Status table data");
            auditStatusPage.validateAuditStatusTableDataForAudit();
            ReportLogger.pass("AUDIT STEP 08 PASSED - Audit Status table data validated");

            markPassed("AS_001 - Audit Status module validated successfully");

        } finally {
            ReportLogger.step("AUDIT STEP 09 - Return back to Hub");
            auditStatusPage.returnBackToHubSafelyForAudit();
            ReportLogger.pass("AUDIT STEP 09 COMPLETED - Return flow executed");
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Audit Status<br>"
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