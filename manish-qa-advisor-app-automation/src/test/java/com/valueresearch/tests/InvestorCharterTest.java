package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.InvestorCharterPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class InvestorCharterTest extends BaseTest {

    @Test(priority = 1)
    public void ICH_001_VerifyInvestorCharterFromHub() {
        createExtentTest(
                "ICH_001",
                "Verify Investor Charter module from Hub",
                "Open Hub tab, locate More section, tap Investor Charter, verify Investor Charter page, validate top content, scrollable charter content, open URL destinations, validate link pages, and return to Hub"
        );

        ReportLogger.step("Starting test case: ICH_001 - Verify Investor Charter module from Hub");

        InvestorCharterPage investorCharterPage = new InvestorCharterPage(driver);

        try {
            ReportLogger.step("INVESTOR CHARTER STEP 01 - Capture Advisor app package");
            investorCharterPage.captureAdvisorAppPackageForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 01 PASSED - Advisor app package captured");

            ReportLogger.step("INVESTOR CHARTER STEP 02 - Check Advisor app login/session");
            investorCharterPage.ensureAdvisorAppLoggedInForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 02 PASSED - Advisor app login/session confirmed");

            ReportLogger.step("INVESTOR CHARTER STEP 03 - Open Hub from bottom navigation");
            investorCharterPage.openHubFromBottomNavigationForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 03 PASSED - Hub page opened");

            ReportLogger.step("INVESTOR CHARTER STEP 04 - Scroll Hub page to Investor Charter option");
            investorCharterPage.scrollToInvestorCharterInHubForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 04 PASSED - Investor Charter option is visible");

            ReportLogger.step("INVESTOR CHARTER STEP 05 - Tap Investor Charter option");
            investorCharterPage.tapInvestorCharterForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 05 PASSED - Investor Charter option tapped");

            ReportLogger.step("INVESTOR CHARTER STEP 06 - Wait for Investor Charter page");
            investorCharterPage.waitForInvestorCharterPageForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 06 PASSED - Investor Charter page loaded");

            ReportLogger.step("INVESTOR CHARTER STEP 07 - Validate Investor Charter top content");
            investorCharterPage.validateInvestorCharterPageContentForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 07 PASSED - Investor Charter title and top content validated");

            ReportLogger.step("INVESTOR CHARTER STEP 08 - Validate Investor Charter scrollable content");
            investorCharterPage.validateInvestorCharterScrollableContentForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 08 PASSED - Investor Charter scrollable content validated");

            ReportLogger.step("INVESTOR CHARTER STEP 09 - Validate Investor Charter link markers");
            investorCharterPage.validateInvestorCharterLinksPresentForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 09 PASSED - Investor Charter link markers validated");

            ReportLogger.step("INVESTOR CHARTER STEP 10 - Open and validate Investor Charter URL destinations");
            investorCharterPage.openAndValidateInvestorCharterLinksForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 10 PASSED - Investor Charter URL destinations opened and validated");

            markPassed("ICH_001 - Investor Charter module validated successfully");

        } finally {
            ReportLogger.step("INVESTOR CHARTER STEP 11 - Return back to Hub");
            investorCharterPage.returnBackToHubSafelyForInvestorCharter();
            ReportLogger.pass("INVESTOR CHARTER STEP 11 COMPLETED - Return flow executed");
        }
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Investor Charter<br>"
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