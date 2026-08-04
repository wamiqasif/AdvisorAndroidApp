package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.ContactUsPage;
import com.valueresearch.utils.AuthHelper;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.Test;

public class ContactUsTest extends BaseTest {

    private static boolean contactUsOpened = false;

    @Test(priority = 1)
    public void CU_001_OpenContactUsFromHub() {
        createExtentTest(
                "CU_001",
                "Open Contact Us from Hub",
                "Open Hub tab, locate More section, tap Contact Us, and verify Contact us page opens"
        );

        ReportLogger.step("Starting test case: CU_001 - Open Contact Us from Hub");

        new AuthHelper(driver).ensureLoggedIn();
        ReportLogger.pass("Advisor login/session confirmed");

        ContactUsPage page = new ContactUsPage(driver);
        page.openContactUsFromHub();
        contactUsOpened = true;

        ExtentTestManager.getTest().pass(
                "<span class='badge white-text green'>CU_001 - Contact Us opened successfully</span>"
        );
        ReportLogger.pass("Completed test case: CU_001");
    }

    @Test(priority = 2, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_002_VerifyTitleAndMessageBox() {
        createExtentTest(
                "CU_002",
                "Verify Contact Us title and message box",
                "Validate Contact us title, Your Message label, placeholder note, and message input box"
        );

        ReportLogger.step("Starting test case: CU_002 - Verify title and message box");
        getContactUsPage().verifyTitleAndMessageBox();
        markPassed("CU_002 - Title and message box validated successfully");
    }

    @Test(priority = 3, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_003_VerifyChooseFileAndEscalationMatrixStaticText() {
        createExtentTest(
                "CU_003",
                "Verify Choose File and Escalation Matrix static text",
                "Validate Choose File attachment text and Escalation Matrix link text on Contact us page"
        );

        ReportLogger.step("Starting test case: CU_003 - Verify Choose File and Escalation Matrix static text");
        getContactUsPage().verifyChooseFileAndEscalationMatrix();
        markPassed("CU_003 - Choose File and Escalation Matrix static text validated successfully");
    }

    @Test(priority = 4, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_004_VerifyPhoneAndWorkingHours() {
        createExtentTest(
                "CU_004",
                "Verify phone and working hours",
                "Validate Phone label, contact number, and Monday-Friday working hours"
        );

        ReportLogger.step("Starting test case: CU_004 - Verify phone and working hours");
        getContactUsPage().verifyPhoneAndWorkingHours();
        markPassed("CU_004 - Phone and working hours validated successfully");
    }

    @Test(priority = 5, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_005_VerifyPostalAddress() {
        createExtentTest(
                "CU_005",
                "Verify postal address",
                "Validate Postal Address label and Independent Advisors Private Limited office address"
        );

        ReportLogger.step("Starting test case: CU_005 - Verify postal address");
        getContactUsPage().verifyPostalAddress();
        markPassed("CU_005 - Postal address validated successfully");
    }

    @Test(priority = 6, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_006_VerifyGrievanceAndScoresInfo() {
        createExtentTest(
                "CU_006",
                "Verify grievance email and SCORES link",
                "Validate grievance officer email, grievance text, SEBI SCORES URL, and SEBI offices text"
        );

        ReportLogger.step("Starting test case: CU_006 - Verify grievance email and SCORES link");
        getContactUsPage().verifyGrievanceAndScoresInfo();
        markPassed("CU_006 - Grievance email and SCORES link validated successfully");
    }

    @Test(priority = 7, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_007_VerifyAndroidAndIosAppLinksStaticText() {
        createExtentTest(
                "CU_007",
                "Verify Android App and iOS App links static text",
                "Validate Link for SCORES, Android App, and iOS App link labels"
        );

        ReportLogger.step("Starting test case: CU_007 - Verify Android and iOS App links static text");
        getContactUsPage().verifyAppLinks();
        markPassed("CU_007 - Android and iOS App static link text validated successfully");
    }

    @Test(priority = 8, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_008_VerifySubmitButtonStaticText() {
        createExtentTest(
                "CU_008",
                "Verify Submit button static text",
                "Validate Submit button is present and its text is Submit"
        );

        ReportLogger.step("Starting test case: CU_008 - Verify Submit button static text");
        getContactUsPage().verifySubmitButton();
        markPassed("CU_008 - Submit button text validated successfully");
    }

    @Test(priority = 9, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_009_VerifyContactUsScreenStability() {
        createExtentTest(
                "CU_009",
                "Verify Contact Us screen stability",
                "Validate Contact us screen does not show crash or ANR markers"
        );

        ReportLogger.step("Starting test case: CU_009 - Verify Contact Us screen stability");
        getContactUsPage().verifyContactUsScreenStability();
        markPassed("CU_009 - Contact Us screen stability validated successfully");
    }

    @Test(priority = 10, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_010_VerifyPositiveMessageTyping() {
        createExtentTest(
                "CU_010",
                "Verify positive message typing",
                "Validate user can type a standard valid message in Contact us message input box"
        );

        ReportLogger.step("Starting test case: CU_010 - Verify positive message typing");
        getContactUsPage().verifyMessageTyping();
        markPassed("CU_010 - Positive message typing validated successfully");
    }

    @Test(priority = 11, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_011_VerifySpecialCharacterMessageTyping() {
        createExtentTest(
                "CU_011",
                "Verify special character message typing",
                "Validate message input accepts safe special characters and numbers"
        );

        ReportLogger.step("Starting test case: CU_011 - Verify special character message typing");
        getContactUsPage().verifySpecialCharacterMessageTyping();
        markPassed("CU_011 - Special character message typing validated successfully");
    }

    @Test(priority = 12, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_012_VerifyInputCanBeCleared() {
        createExtentTest(
                "CU_012",
                "Verify message input can be cleared",
                "Validate text entered in message input can be removed before submission"
        );

        ReportLogger.step("Starting test case: CU_012 - Verify input can be cleared");
        getContactUsPage().verifyInputCanBeCleared();
        markPassed("CU_012 - Message input clear validation passed");
    }

    @Test(priority = 13, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_013_VerifyEmptySubmitNegativeValidation() {
        createExtentTest(
                "CU_013",
                "Verify empty submit negative validation",
                "Tap Submit with empty message and validate the request is not submitted"
        );

        ReportLogger.step("Starting test case: CU_013 - Verify empty submit negative validation");
        getContactUsPage().verifyEmptySubmitValidation();
        markPassed("CU_013 - Empty submit negative validation passed");
    }

    @Test(priority = 14, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_014_VerifyWhitespaceSubmitNegativeValidation() {
        createExtentTest(
                "CU_014",
                "Verify whitespace submit negative validation",
                "Tap Submit with whitespace-only message and validate the request is not submitted"
        );

        ReportLogger.step("Starting test case: CU_014 - Verify whitespace submit negative validation");
        getContactUsPage().verifyWhitespaceSubmitValidation();
        markPassed("CU_014 - Whitespace submit negative validation passed");
    }

    @Test(priority = 15, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_015_VerifySubmitButtonStateForEmptyMessage() {
        createExtentTest(
                "CU_015",
                "Verify Submit button state for empty message",
                "Log and validate Submit button state attributes when message input is empty"
        );

        ReportLogger.step("Starting test case: CU_015 - Verify Submit button state for empty message");
        getContactUsPage().verifySubmitButtonStateForEmptyMessage();
        markPassed("CU_015 - Submit button state logged successfully");
    }

    @Test(priority = 16, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_016_VerifyChooseFileFunctionalTap() {
        createExtentTest(
                "CU_016",
                "Verify Choose File functional tap",
                "Tap Choose File and validate file picker, chooser, permission dialog, or external response opens, then return"
        );

        ReportLogger.step("Starting test case: CU_016 - Verify Choose File functional tap");
        getContactUsPage().verifyChooseFileFunctionalTap();
        markPassed("CU_016 - Choose File functional tap validated successfully");
    }

    @Test(priority = 17, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_017_VerifyEscalationMatrixFunctionalTap() {
        createExtentTest(
                "CU_017",
                "Verify Escalation Matrix functional tap",
                "Tap Escalation Matrix and validate navigation/chooser response, then return"
        );

        ReportLogger.step("Starting test case: CU_017 - Verify Escalation Matrix functional tap");
        getContactUsPage().verifyEscalationMatrixFunctionalTap();
        markPassed("CU_017 - Escalation Matrix functional tap validated successfully");
    }

    @Test(priority = 18, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_018_VerifyPhoneFunctionalTap() {
        createExtentTest(
                "CU_018",
                "Verify phone functional tap",
                "Tap phone number and validate dialer/chooser response, then return"
        );

        ReportLogger.step("Starting test case: CU_018 - Verify phone functional tap");
        getContactUsPage().verifyPhoneFunctionalTap();
        markPassed("CU_018 - Phone functional tap validated successfully");
    }

    @Test(priority = 19, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_019_VerifyGrievanceEmailFunctionalTap() {
        createExtentTest(
                "CU_019",
                "Verify grievance email functional tap",
                "Tap grievance email and validate email app/chooser response, then return"
        );

        ReportLogger.step("Starting test case: CU_019 - Verify grievance email functional tap");
        getContactUsPage().verifyGrievanceEmailFunctionalTap();
        markPassed("CU_019 - Grievance email functional tap validated successfully");
    }

    @Test(priority = 20, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_020_VerifyScoresLinkFunctionalTap() {
        createExtentTest(
                "CU_020",
                "Verify SEBI SCORES functional tap",
                "Tap SEBI SCORES link and validate browser/chooser response, then return"
        );

        ReportLogger.step("Starting test case: CU_020 - Verify SEBI SCORES functional tap");
        getContactUsPage().verifyScoresLinkFunctionalTap();
        markPassed("CU_020 - SEBI SCORES functional tap validated successfully");
    }

    @Test(priority = 21, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_021_VerifyAndroidAppFunctionalTap() {
        createExtentTest(
                "CU_021",
                "Verify Android App functional tap",
                "Tap Android App link under SCORES and validate external response, then return"
        );

        ReportLogger.step("Starting test case: CU_021 - Verify Android App functional tap");
        getContactUsPage().verifyAndroidAppFunctionalTap();
        markPassed("CU_021 - Android App functional tap validated successfully");
    }

    @Test(priority = 22, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_022_VerifyIosAppFunctionalTap() {
        createExtentTest(
                "CU_022",
                "Verify iOS App functional tap",
                "Tap iOS App link under SCORES and validate external/browser response, then return"
        );

        ReportLogger.step("Starting test case: CU_022 - Verify iOS App functional tap");
        getContactUsPage().verifyIosAppFunctionalTap();
        markPassed("CU_022 - iOS App functional tap validated successfully");
    }

    @Test(priority = 23, dependsOnMethods = "CU_001_OpenContactUsFromHub")
    public void CU_023_VerifyBackNavigationToHub() {
        createExtentTest(
                "CU_023",
                "Verify back navigation to Hub",
                "Validate back navigation from Contact us page returns user to Hub area"
        );

        ReportLogger.step("Starting test case: CU_023 - Verify back navigation to Hub");
        getContactUsPage().verifyBackNavigationToHub();
        markPassed("CU_023 - Back navigation to Hub validated successfully");
    }

    private ContactUsPage getContactUsPage() {
        ContactUsPage page = new ContactUsPage(driver);

        if (!contactUsOpened || !page.isContactUsPageVisible()) {
            ReportLogger.step("Contact us page is not active. Recovering before validation.");
            page.recoverContactUsIfNeeded();
            contactUsOpened = true;
        }

        return page;
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: Contact Us<br>"
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