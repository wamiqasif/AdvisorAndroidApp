package com.valueresearch.tests;

import com.aventstack.extentreports.ExtentTest;
import com.valueresearch.base.BaseTest;
import com.valueresearch.pages.AppSettingsPage;
import com.valueresearch.utils.ExtentManager;
import com.valueresearch.utils.ExtentTestManager;
import com.valueresearch.utils.ReportLogger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AppSettingsTest extends BaseTest {

    private AppSettingsPage appSettingsPage;

    @BeforeMethod(alwaysRun = true)
    public void refreshAppSettingsPageAfterDriverHealthCheck() {
        appSettingsPage = new AppSettingsPage(driver);
    }

    @Test(priority = 1)
    public void AS_001_VerifyCompleteAppSettingsFlow() {
        createExtentTest(
                "AS_001",
                "Verify complete App Settings flow",
                "Open Hub, open App Settings, validate App tab, Check for Updates, Change PIN same-PIN validation, Storage Settings, and Portfolio Settings save"
        );

        ReportLogger.step("Starting test case: AS_001 - Verify complete App Settings flow");

        AppSettingsPage page = getAppSettingsPage();

        ReportLogger.step("APP SETTINGS STEP 01 - Capture Advisor app package");
        page.captureAdvisorAppPackageForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 01 PASSED - Advisor app package captured");

        ReportLogger.step("APP SETTINGS STEP 02 - Check Advisor app login/session");
        page.ensureAdvisorAppLoggedInForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 02 PASSED - Advisor app login/session confirmed");

        ReportLogger.step("APP SETTINGS STEP 03 - Open Hub from dashboard/home");
        page.openHubFromDashboardForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 03 PASSED - Hub opened");

        ReportLogger.step("APP SETTINGS STEP 04 - Open App Settings from Hub");
        page.openAppSettingsFromHubForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 04 PASSED - App Settings opened");

        ReportLogger.step("APP SETTINGS STEP 05 - Validate App Settings screen structure");
        page.validateAppSettingsScreenStructureForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 05 PASSED - App Settings structure validated");

        ReportLogger.step("APP SETTINGS STEP 06 - Validate Check for Updates flow");
        page.validateCheckForUpdatesFlowForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 06 PASSED - Check for Updates flow validated");

        ReportLogger.step("APP SETTINGS STEP 07 - Validate Change PIN same existing PIN error");
        page.validateChangePinSameExistingPinErrorForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 07 PASSED - Change PIN negative validation passed");

        ReportLogger.step("APP SETTINGS STEP 08 - Validate Storage Settings screen");
        page.validateStorageSettingsScreenForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 08 PASSED - Storage Settings validated");

        ReportLogger.step("APP SETTINGS STEP 09 - Validate Portfolio Settings and Save Changes");
        page.validatePortfolioSettingsAndSaveForAppSettings();
        ReportLogger.pass("APP SETTINGS STEP 09 PASSED - Portfolio Settings validated and saved");

        markPassed("AS_001 - Complete App Settings flow validated successfully");
    }

    @Test(priority = 2)
    public void AS_002_VerifyAppSettingsScreenStructure() {
        createExtentTest(
                "AS_002",
                "Verify App Settings screen structure",
                "Open/restore App Settings and validate Settings title, Portfolio/App tabs, App options, and version/build text"
        );

        ReportLogger.step("Starting test case: AS_002 - Verify App Settings screen structure");

        AppSettingsPage page = getAppSettingsPage();
        prepareAppSettingsScreen(page, "AS_002");

        markPassed("AS_002 - App Settings screen structure validated successfully");
    }

    @Test(priority = 3)
    public void AS_003_VerifyCheckForUpdatesFlow() {
        createExtentTest(
                "AS_003",
                "Verify Check for Updates flow",
                "Open/restore App Settings, tap Check for Updates, validate Google Play Advisor app page, and return to Advisor app"
        );

        ReportLogger.step("Starting test case: AS_003 - Verify Check for Updates flow");

        AppSettingsPage page = getAppSettingsPage();
        prepareAppSettingsScreen(page, "AS_003");

        ReportLogger.step("AS_003 STEP 05 - Validate Check for Updates flow");
        page.validateCheckForUpdatesFlowForAppSettings();
        ReportLogger.pass("AS_003 STEP 05 PASSED - Check for Updates flow validated");

        markPassed("AS_003 - Check for Updates flow validated successfully");
    }

    @Test(priority = 4)
    public void AS_004_VerifyChangePinSameExistingPinValidation() {
        createExtentTest(
                "AS_004",
                "Verify Change PIN same existing PIN validation",
                "Open/restore App Settings, open Change PIN, enter current PIN as new PIN, and validate the same-existing-PIN error"
        );

        ReportLogger.step("Starting test case: AS_004 - Verify Change PIN same existing PIN validation");

        AppSettingsPage page = getAppSettingsPage();
        prepareAppSettingsScreen(page, "AS_004");

        ReportLogger.step("AS_004 STEP 05 - Validate Change PIN same existing PIN error");
        page.validateChangePinSameExistingPinErrorForAppSettings();
        ReportLogger.pass("AS_004 STEP 05 PASSED - Change PIN same existing PIN validation passed");

        markPassed("AS_004 - Change PIN negative validation completed successfully");
    }

    @Test(priority = 5)
    public void AS_005_VerifyStorageSettingsScreen() {
        createExtentTest(
                "AS_005",
                "Verify Storage Settings screen",
                "Open/restore App Settings, open Storage Settings, and validate Storage, Free Space, and Clear cache"
        );

        ReportLogger.step("Starting test case: AS_005 - Verify Storage Settings screen");

        AppSettingsPage page = getAppSettingsPage();
        prepareAppSettingsScreen(page, "AS_005");

        ReportLogger.step("AS_005 STEP 05 - Validate Storage Settings screen");
        page.validateStorageSettingsScreenForAppSettings();
        ReportLogger.pass("AS_005 STEP 05 PASSED - Storage Settings screen validated");

        markPassed("AS_005 - Storage Settings screen validated successfully");
    }

    @Test(priority = 6)
    public void AS_006_VerifyPortfolioSettingsAndSave() {
        createExtentTest(
                "AS_006",
                "Verify Portfolio Settings and Save Changes",
                "Open/restore App Settings, switch to Portfolio tab, validate all portfolio settings fields, tap Save Changes, and validate update message"
        );

        ReportLogger.step("Starting test case: AS_006 - Verify Portfolio Settings and Save Changes");

        AppSettingsPage page = getAppSettingsPage();
        prepareAppSettingsScreen(page, "AS_006");

        ReportLogger.step("AS_006 STEP 05 - Validate Portfolio Settings and Save Changes");
        page.validatePortfolioSettingsAndSaveForAppSettings();
        ReportLogger.pass("AS_006 STEP 05 PASSED - Portfolio Settings and Save Changes validated");

        markPassed("AS_006 - Portfolio Settings and Save Changes validated successfully");
    }

    @Test(priority = 7)
    public void AS_007_VerifyActualChangePinWithEmailOtp() {
        createExtentTest(
                "AS_007",
                "Verify actual Change PIN with email OTP",
                "Change Advisor PIN from current PIN to 1976 using email OTP"
        );

        ReportLogger.step("Starting test case: AS_007 - Verify actual Change PIN with email OTP");

        AppSettingsPage page = getAppSettingsPage();

        prepareAppSettingsScreen(page, "AS_007");

        ReportLogger.step("AS_007 STEP 05 - Change PIN using email OTP");
        page.validateActualChangePinAndRestoreForAppSettings();
        ReportLogger.pass("AS_007 STEP 05 PASSED - Actual Change PIN completed using email OTP");

        markPassed("AS_007 - Actual Change PIN with email OTP validated successfully");
    }
    @AfterMethod(alwaysRun = true)
    public void resetAppSettingsScreenAfterEachTest() {
        if (appSettingsPage == null) {
            return;
        }

        try {
            appSettingsPage.resetAppSettingsToAppTabForNextTest();
            ReportLogger.pass("APP SETTINGS RESET COMPLETED - Ready for next test without returning to Hub/dashboard");
        } catch (Exception e) {
            ReportLogger.debug("APP SETTINGS RESET skipped/failed: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanupAppSettingsSuiteOnce() {
        if (appSettingsPage == null) {
            return;
        }

        ReportLogger.step("APP SETTINGS FINAL CLEANUP - Return back to Advisor App once after suite");

        try {
            appSettingsPage.returnBackToAdvisorAppSafely();
            ReportLogger.pass("APP SETTINGS FINAL CLEANUP COMPLETED - Return flow executed once");
        } catch (Exception e) {
            ReportLogger.debug("APP SETTINGS FINAL CLEANUP failed: " + e.getMessage());
        }
    }

    private AppSettingsPage getAppSettingsPage() {
        if (appSettingsPage == null) {
            appSettingsPage = new AppSettingsPage(driver);
        }

        return appSettingsPage;
    }

    private void prepareAppSettingsScreen(AppSettingsPage page, String caseId) {
        ReportLogger.step(caseId + " STEP 01 - Capture Advisor app package");
        page.captureAdvisorAppPackageForAppSettings();
        ReportLogger.pass(caseId + " STEP 01 PASSED - Advisor app package captured");

        ReportLogger.step(caseId + " STEP 02 - Check Advisor app login/session");
        page.ensureAdvisorAppLoggedInForAppSettings();
        ReportLogger.pass(caseId + " STEP 02 PASSED - Advisor app login/session confirmed");

        ReportLogger.step(caseId + " STEP 03 - Ensure App Settings screen is ready");
        page.ensureAppSettingsScreenReadyForAppSettings();
        ReportLogger.pass(caseId + " STEP 03 PASSED - App Settings screen is ready");

        ReportLogger.step(caseId + " STEP 04 - Validate App Settings screen structure");
        page.validateAppSettingsScreenStructureForAppSettings();
        ReportLogger.pass(caseId + " STEP 04 PASSED - App Settings screen validated");
    }

    private void createExtentTest(String caseId, String title, String validation) {
        ExtentTest test = ExtentManager.getExtentReports().createTest(
                caseId + " - " + title
        );

        test.info("Module: App Settings<br>"
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