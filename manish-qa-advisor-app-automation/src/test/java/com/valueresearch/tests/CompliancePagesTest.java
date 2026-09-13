package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.CompliancePagesPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

/**
 * Compliance Pages VRSA coverage for Hub -> More.
 *
 * Each testcase is independent and uses the exact page locator before reporting the
 * module name. A failed page is recovered back to Hub so it cannot redirect the next
 * testcase into a different module.
 */
public class CompliancePagesTest extends BaseTest {

    @Test(priority = 1)
    public void CP_001_VerifyCompliancePagesInHubMore() {
        createExtentTest(
                "CP_001",
                "Verify Compliance Pages exact Hub locators",
                "Open Hub More and validate the exact accessibility IDs for FAQs, About Us, Privacy Policy, User Agreement, Refund Policy, Investor Charter, Investor Complaint, ODR Portal, and Audit Status"
        );

        CompliancePagesPage page = new CompliancePagesPage(driver);

        try {
            ReportLogger.step("Starting test case: CP_001 - Verify Compliance Pages exact Hub locators");
            page.verifyCompliancePageTilesVisible();
            markPassed("CP_001 - Compliance Pages exact Hub locators validated successfully");
        } finally {
            page.recoverToHubAfterComplianceCase();
        }
    }

    @Test(priority = 2)
    public void CP_002_VerifyFaqsVrsaPage() {
        runInternalPageCase(
                "CP_002",
                "Verify FAQs - Stock Advisor",
                "Tap exact FAQs Hub locator; if dual subscription selector appears choose exact Stock Advisor option, otherwise require direct VRSA WebView; verify VRSA WebView opens",
                CompliancePagesPage::verifyFaqsVrsaPage
        );
    }

    @Test(priority = 3)
    public void CP_003_VerifyAboutUsVrsaPage() {
        runInternalPageCase(
                "CP_003",
                "Verify About Us - Stock Advisor",
                "Tap exact About Us Hub locator; conditionally select Stock Advisor only when both service options exist; verify VRSA WebView opens",
                CompliancePagesPage::verifyAboutUsVrsaPage
        );
    }

    @Test(priority = 4)
    public void CP_004_VerifyPrivacyPolicyVrsaPage() {
        runInternalPageCase(
                "CP_004",
                "Verify Privacy Policy - Stock Advisor",
                "Tap exact Privacy Policy Hub locator; conditionally select Stock Advisor for dual subscription; verify VRSA WebView opens",
                CompliancePagesPage::verifyPrivacyPolicyVrsaPage
        );
    }

    @Test(priority = 5)
    public void CP_005_VerifyUserAgreementVrsa() {
        createExtentTest(
                "CP_005",
                "Verify User Agreement - Stock Advisor",
                "Tap exact User Agreement Hub locator; for dual subscription tap exact Stock Advisor Download row; for VRSA-only accept direct WebView or direct completed-PDF prompt; verify PDF/download/viewer state"
        );

        CompliancePagesPage page = new CompliancePagesPage(driver);

        try {
            ReportLogger.step("Starting test case: CP_005 - Verify User Agreement - Stock Advisor");
            page.verifyUserAgreementVrsaDownloadOpens();
            markPassed("CP_005 - Stock Advisor User Agreement validated successfully");
        } finally {
            page.recoverToHubAfterComplianceCase();
        }
    }

    @Test(priority = 6)
    public void CP_006_VerifyRefundPolicyVrsaPage() {
        runInternalPageCase(
                "CP_006",
                "Verify Refund Policy - Stock Advisor",
                "Tap exact Refund Policy Hub locator; conditionally select Stock Advisor for dual subscription; verify VRSA WebView opens",
                CompliancePagesPage::verifyRefundPolicyVrsaPage
        );
    }

    @Test(priority = 7)
    public void CP_007_VerifyInvestorCharterVrsaPage() {
        runInternalPageCase(
                "CP_007",
                "Verify Investor Charter - Stock Advisor",
                "Tap exact Investor Charter Hub locator; conditionally select Stock Advisor for dual subscription; verify VRSA WebView opens",
                CompliancePagesPage::verifyInvestorCharterVrsaPage
        );
    }

    @Test(priority = 8)
    public void CP_008_VerifyInvestorComplaintVrsaPage() {
        runInternalPageCase(
                "CP_008",
                "Verify Investor Complaint - Stock Advisor",
                "Tap exact Investor Complaint Hub locator; conditionally select Stock Advisor for dual subscription; verify VRSA WebView opens",
                CompliancePagesPage::verifyInvestorComplaintVrsaPage
        );
    }

    @Test(priority = 9)
    public void CP_009_VerifyOdrPortal() {
        createExtentTest(
                "CP_009",
                "Verify ODR Portal opens",
                "Tap exact ODR Portal Hub locator and verify navigation leaves Advisor app for the external ODR page"
        );

        CompliancePagesPage page = new CompliancePagesPage(driver);

        try {
            ReportLogger.step("Starting test case: CP_009 - Verify ODR Portal");
            page.verifyOdrPortalOpens();
            markPassed("CP_009 - ODR Portal opened successfully");
        } finally {
            page.recoverToHubAfterComplianceCase();
        }
    }

    @Test(priority = 10)
    public void CP_010_VerifyAuditStatusVrsaPage() {
        runInternalPageCase(
                "CP_010",
                "Verify Audit Status - Stock Advisor",
                "Tap exact Audit Status Hub locator; conditionally select Stock Advisor for dual subscription; verify VRSA WebView opens",
                CompliancePagesPage::verifyAuditStatusVrsaPage
        );
    }

    // =========================================================
    // TEST HELPERS
    // =========================================================

    private void runInternalPageCase(
            String caseId,
            String title,
            String validation,
            ComplianceAction action
    ) {
        createExtentTest(caseId, title, validation);

        CompliancePagesPage page = new CompliancePagesPage(driver);

        try {
            ReportLogger.step("Starting test case: " + caseId + " - " + title);
            action.run(page);
            markPassed(caseId + " - " + title + " validated successfully");
        } finally {
            page.recoverToHubAfterComplianceCase();
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Compliance Pages<br>"
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

    @FunctionalInterface
    private interface ComplianceAction {
        void run(CompliancePagesPage page);
    }
}