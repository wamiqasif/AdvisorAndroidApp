package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.InvestorComplaintPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class InvestorComplaintTest extends BaseTest {

    @Test(priority = 1)
    public void IC_001_VerifyInvestorComplaintFromHub() {
        createExtentTest(
                "IC_001",
                "Verify Investor Complaint module from Hub",
                "Open Hub tab, locate More section, tap Investor Complaint, verify complaint summary page, validate summary and trend table data, and return to Hub"
        );

        ReportLogger.step("Starting test case: IC_001 - Verify Investor Complaint module from Hub");

        InvestorComplaintPage investorComplaintPage = new InvestorComplaintPage(driver);

        try {
            ReportLogger.step("INVESTOR COMPLAINT STEP 01 - Capture Advisor app package");
            investorComplaintPage.captureAdvisorAppPackageForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("INVESTOR COMPLAINT STEP 02 - Check Advisor app login/session");
            investorComplaintPage.ensureAdvisorAppLoggedInForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("INVESTOR COMPLAINT STEP 03 - Open Hub from bottom navigation");
            investorComplaintPage.openHubFromBottomNavigationForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 03 PASSED - Hub page opened");

            ReportLogger.step("INVESTOR COMPLAINT STEP 04 - Scroll Hub page to Investor Complaint option");
            investorComplaintPage.scrollToInvestorComplaintInHubForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 04 PASSED - Investor Complaint option is visible");

            ReportLogger.step("INVESTOR COMPLAINT STEP 05 - Tap Investor Complaint option");
            investorComplaintPage.tapInvestorComplaintForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 05 PASSED - Investor Complaint option tapped");

            ReportLogger.step("INVESTOR COMPLAINT STEP 06 - Wait for Investor Complaint page");
            investorComplaintPage.waitForInvestorComplaintPageForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 06 PASSED - Investor Complaint page loaded");

            ReportLogger.step("INVESTOR COMPLAINT STEP 07 - Validate Investor Complaint page content");
            investorComplaintPage.validateInvestorComplaintPageContentForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 07 PASSED - Investor Complaint title, summary and headers validated");

            ReportLogger.step("INVESTOR COMPLAINT STEP 08 - Validate Investor Complaint data");
            investorComplaintPage.validateInvestorComplaintDataForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 08 PASSED - Investor Complaint data validated");

            markPassed("IC_001 - Investor Complaint module validated successfully");

        } finally {
            ReportLogger.step("INVESTOR COMPLAINT STEP 09 - Return back to Hub");
            investorComplaintPage.returnBackToHubSafelyForInvestorComplaint();
            ReportLogger.pass("INVESTOR COMPLAINT STEP 09 COMPLETED - Return flow executed");
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Investor Complaint<br>"
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