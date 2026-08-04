package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.OnboardingPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

/**
 * Onboarding flow split into dependent TestNG test cases.
 *
 * Important:
 * - The test methods execute in one continuous Appium session.
 * - Each test case creates its own ExtentReports entry.
 * - If a prerequisite test fails, the remaining dependent tests are skipped.
 * - Do not quit/recreate the driver between these methods.
 */
public class OnboardingTest extends BaseTest {

    @Test(priority = 1)
    public void ONB_001_LoginAndValidateSubscriptionLanding() {
        createExtentTest(
                "ONB_001",
                "Login and validate subscription landing",
                "Validate Advisor login/session and confirm the Subscribe Now landing screen"
        );

        ReportLogger.step("Starting test case: ONB_001 - Login and validate subscription landing");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_001 STEP 01 - Check Advisor app login/session");
        onboardingPage.ensureAdvisorAppLoggedInForOnboarding();
        ReportLogger.pass("ONB_001 STEP 01 PASSED - Login/session confirmed");

        ReportLogger.step("ONB_001 STEP 02 - Validate subscription landing screen");
        onboardingPage.waitForSubscriptionLandingForOnboarding();
        ReportLogger.pass("ONB_001 STEP 02 PASSED - Subscription landing visible");

        markPassed("ONB_001 - Login and subscription landing validated successfully");
    }

    @Test(
            priority = 2,
            dependsOnMethods = "ONB_001_LoginAndValidateSubscriptionLanding"
    )
    public void ONB_002_OpenAndValidatePlanSelection() {
        createExtentTest(
                "ONB_002",
                "Open and validate plan selection",
                "Tap Subscribe Now and validate the Fund Advisor plan selection screen"
        );

        ReportLogger.step("Starting test case: ONB_002 - Open and validate plan selection");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_002 STEP 01 - Tap Subscribe Now");
        onboardingPage.tapSubscribeNowForOnboarding();
        ReportLogger.pass("ONB_002 STEP 01 PASSED - Subscribe Now tapped");

        ReportLogger.step("ONB_002 STEP 02 - Validate plan selection screen");
        onboardingPage.validatePlanSelectionForOnboarding();
        ReportLogger.pass("ONB_002 STEP 02 PASSED - Plan selection screen visible");

        markPassed("ONB_002 - Plan selection screen validated successfully");
    }

    @Test(
            priority = 3,
            dependsOnMethods = "ONB_002_OpenAndValidatePlanSelection"
    )
    public void ONB_003_CompleteIdentityDetails() {
        createExtentTest(
                "ONB_003",
                "Complete identity details",
                "Continue from plan selection and fill PAN, date of birth, mobile number and tax residency details"
        );

        ReportLogger.step("Starting test case: ONB_003 - Complete identity details");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_003 STEP 01 - Continue from plan selection");
        onboardingPage.continueFromPlanSelectionForOnboarding();
        ReportLogger.pass("ONB_003 STEP 01 PASSED - Continued from plan selection");

        ReportLogger.step("ONB_003 STEP 02 - Validate identity screen");
        onboardingPage.waitForIdentityScreenForOnboarding();
        ReportLogger.pass("ONB_003 STEP 02 PASSED - Identity screen visible");

        ReportLogger.step("ONB_003 STEP 03 - Fill identity details");
        onboardingPage.fillIdentityDetailsForOnboarding();
        ReportLogger.pass("ONB_003 STEP 03 PASSED - Identity details filled");

        markPassed("ONB_003 - Identity details completed successfully");
    }

    @Test(
            priority = 4,
            dependsOnMethods = "ONB_003_CompleteIdentityDetails"
    )
    public void ONB_004_VerifyPanKycAndOpenPaymentSetup() {
        createExtentTest(
                "ONB_004",
                "Verify PAN and KYC",
                "Select KYC consent, verify PAN/KYC and continue to recurring mandate payment setup"
        );

        ReportLogger.step("Starting test case: ONB_004 - Verify PAN and KYC");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_004 STEP 01 - Verify PAN and KYC");
        onboardingPage.verifyPanAndKycForOnboarding();
        ReportLogger.pass("ONB_004 STEP 01 PASSED - PAN and KYC verified");

        ReportLogger.step("ONB_004 STEP 02 - Continue to payment setup");
        onboardingPage.continueToPaymentAfterKycForOnboarding();
        ReportLogger.pass("ONB_004 STEP 02 PASSED - Continued to payment setup");

        ReportLogger.step("ONB_004 STEP 03 - Validate recurring mandate setup");
        onboardingPage.waitForMandateSetupForOnboarding();
        ReportLogger.pass("ONB_004 STEP 03 PASSED - Mandate setup visible");

        markPassed("ONB_004 - PAN/KYC verification and payment setup validated successfully");
    }

    @Test(
            priority = 5,
            dependsOnMethods = "ONB_004_VerifyPanKycAndOpenPaymentSetup"
    )
    public void ONB_005_SelectPaymentAndAuthoriseMandate() {
        createExtentTest(
                "ONB_005",
                "Select payment mode and authorise mandate",
                "Select the configured payment mode and tap Authorise mandate"
        );

        ReportLogger.step("Starting test case: ONB_005 - Select payment mode and authorise mandate");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_005 STEP 01 - Choose payment mode");
        onboardingPage.choosePaymentModeForOnboarding();
        ReportLogger.pass("ONB_005 STEP 01 PASSED - Payment mode selected");

        ReportLogger.step("ONB_005 STEP 02 - Tap Authorise mandate");
        onboardingPage.authoriseMandateForOnboarding();
        ReportLogger.pass("ONB_005 STEP 02 PASSED - Authorise mandate tapped");

        markPassed("ONB_005 - Payment mode and mandate authorisation validated successfully");
    }

    @Test(
            priority = 6,
            dependsOnMethods = "ONB_005_SelectPaymentAndAuthoriseMandate"
    )
    public void ONB_006_CompleteRazorpayMandateAndOpenKycOtp() {
        createExtentTest(
                "ONB_006",
                "Complete Razorpay mandate",
                "Complete the Razorpay mandate flow, validate Mandate authorised and open the KYC OTP screen"
        );

        ReportLogger.step("Starting test case: ONB_006 - Complete Razorpay mandate");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_006 STEP 01 - Complete Razorpay mandate flow");
        onboardingPage.completeRazorpayMandateForOnboarding();
        ReportLogger.pass("ONB_006 STEP 01 PASSED - Mandate authorised");

        ReportLogger.step("ONB_006 STEP 02 - Continue to Fetch KYC");
        onboardingPage.continueToFetchKycForOnboarding();
        ReportLogger.pass("ONB_006 STEP 02 PASSED - KYC OTP screen visible");

        markPassed("ONB_006 - Razorpay mandate and KYC OTP navigation validated successfully");
    }

    @Test(
            priority = 7,
            dependsOnMethods = "ONB_006_CompleteRazorpayMandateAndOpenKycOtp"
    )
    public void ONB_007_VerifyKycOtpAndCompleteContactDetails() {
        createExtentTest(
                "ONB_007",
                "Verify KYC OTP and contact details",
                "Fetch KYC OTP from the separate email inbox, verify it and continue from Contact Details"
        );

        ReportLogger.step("Starting test case: ONB_007 - Verify KYC OTP and complete Contact Details");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_007 STEP 01 - Fetch and verify KYC OTP");
        onboardingPage.completeKycOtpVerificationForOnboarding();
        ReportLogger.pass("ONB_007 STEP 01 PASSED - KYC OTP verified");

        ReportLogger.step("ONB_007 STEP 02 - Continue from Contact Details");
        onboardingPage.continueFromContactDetailsForOnboarding();
        ReportLogger.pass("ONB_007 STEP 02 PASSED - Contact Details completed");

        markPassed("ONB_007 - KYC OTP and Contact Details validated successfully");
    }

    @Test(
            priority = 8,
            dependsOnMethods = "ONB_007_VerifyKycOtpAndCompleteContactDetails"
    )
    public void ONB_008_OpenDigioAndSubmitESign() {
        createExtentTest(
                "ONB_008",
                "Open Digio and submit e-sign",
                "Continue to E-Sign, validate the Digio signing page and submit Sign Now"
        );

        ReportLogger.step("Starting test case: ONB_008 - Open Digio and submit e-sign");

        OnboardingPage onboardingPage = new OnboardingPage(driver);

        ReportLogger.step("ONB_008 STEP 01 - Continue to E-Sign");
        onboardingPage.continueToESignForOnboarding();
        ReportLogger.pass("ONB_008 STEP 01 PASSED - Digio signing page loaded");

        ReportLogger.step("ONB_008 STEP 02 - Complete Digio consent and Sign Now");
        onboardingPage.completeDigioESignForOnboarding();
        ReportLogger.pass("ONB_008 STEP 02 PASSED - Digio Sign Now submitted");

        markPassed("ONB_008 - Digio e-sign submission validated successfully");
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Onboarding<br>"
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