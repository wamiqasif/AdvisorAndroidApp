package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;
import pages.MyAccount_Page;
import pages.MyAccount_Page.LinkResult;

/**
 * My Account ("Account Details") Test Suite.
 *
 * Journey for every test (set in recoverAppState):
 *   recover to dashboard -> tap Hub tab -> open Account Details section.
 *
 * Persistent-session mode (driver shared across methods, like AboutUsTest).
 *
 * NOTE: Logout is intentionally never tapped - doing so destroys the PIN setup
 * and forces OTP re-registration (project rule). Its visibility is asserted,
 * but no test taps it.
 */
public class MyAccountTest extends BaseTest {

    private MyAccount_Page myAccountPage;
    private DashboardPage  dashboardPage;

    // ============================================================
    // CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        myAccountPage = new MyAccount_Page(getDriver());
        dashboardPage = new DashboardPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        dashboardPage.tapHubTab();
        myAccountPage.openAccountDetailsScreen();

        Assert.assertTrue(
                myAccountPage.isAccountDetailsScreenDisplayed(),
                "Account Details screen must load before " + method.getName());

        logger.info("Account Details screen ready for: {}", method.getName());
    }

    // ============================================================
    // SECTION 1 - SCREEN LOAD & STRUCTURE
    // ============================================================

    @Test(description = "TC_MYACC_001 - Account Details screen opens from Hub")
    public void tc_myacc_001_verifyScreenLoads() {
        Assert.assertTrue(
                myAccountPage.isAccountDetailsScreenDisplayed(),
                "Account Details screen must be displayed");
        logger.info("TC_MYACC_001 - screen load verified");
    }

    @Test(description = "TC_MYACC_002 - Back button is visible")
    public void tc_myacc_002_verifyBackButtonVisible() {
        Assert.assertTrue(
                myAccountPage.isBackButtonDisplayed(),
                "Back button must be visible");
        logger.info("TC_MYACC_002 - back button visibility verified");
    }

    @Test(description = "TC_MYACC_003 - Edit Account Details button is visible")
    public void tc_myacc_003_verifyEditButtonVisible() {
        Assert.assertTrue(
                myAccountPage.isEditAccountDetailsDisplayed(),
                "Edit Account Details button must be visible");
        logger.info("TC_MYACC_003 - edit button visibility verified");
    }

    @Test(description = "TC_MYACC_004 - Header action icon is visible")
    public void tc_myacc_004_verifyHeaderIconVisible() {
        Assert.assertTrue(
                myAccountPage.isHeaderActionIconDisplayed(),
                "Header action icon must be visible");
        logger.info("TC_MYACC_004 - header icon visibility verified");
    }

    // ============================================================
    // SECTION 2 - FIELD LABELS
    // ============================================================

    @Test(description = "TC_MYACC_005 - Email Address label is visible")
    public void tc_myacc_005_verifyEmailLabel() {
        Assert.assertTrue(
                myAccountPage.isEmailAddressLabelDisplayed(),
                "Email Address label must be visible");
        logger.info("TC_MYACC_005 - email label verified");
    }

    @Test(description = "TC_MYACC_006 - Name label is visible")
    public void tc_myacc_006_verifyNameLabel() {
        Assert.assertTrue(
                myAccountPage.isNameLabelDisplayed(),
                "Name label must be visible");
        logger.info("TC_MYACC_006 - name label verified");
    }

    @Test(description = "TC_MYACC_007 - Phone label/field is visible")
    public void tc_myacc_007_verifyPhoneLabel() {
        Assert.assertTrue(
                myAccountPage.isPhoneLabelDisplayed(),
                "Phone label must be visible");
        Assert.assertTrue(
                myAccountPage.isPhoneFieldDisplayed(),
                "Phone field must be present");
        logger.info("TC_MYACC_007 - phone label/field verified");
    }

    @Test(description = "TC_MYACC_008 - Date of Birth label is visible")
    public void tc_myacc_008_verifyDobLabel() {
        Assert.assertTrue(
                myAccountPage.isDateOfBirthLabelDisplayed(),
                "Date of Birth label must be visible");
        logger.info("TC_MYACC_008 - DOB label verified");
    }

    // ============================================================
    // SECTION 3 - FIELD VALUES & FORMATS
    // ============================================================

    @Test(description = "TC_MYACC_009 - Email value is visible and well-formed")
    public void tc_myacc_009_verifyEmailValue() {
        Assert.assertTrue(
                myAccountPage.isEmailValueDisplayed(),
                "Email value must be visible");

        String email = myAccountPage.getEmailAddress();
        Assert.assertFalse(email.isEmpty(), "Email value must not be empty");
        Assert.assertTrue(
                email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                "Email must be well-formed, got: " + email);

        logger.info("TC_MYACC_009 - email value verified: {}", email);
    }

    @Test(description = "TC_MYACC_010 - Name value is visible and non-empty")
    public void tc_myacc_010_verifyNameValue() {
        Assert.assertTrue(
                myAccountPage.isNameValueDisplayed(),
                "Name value must be visible");

        String name = myAccountPage.getName();
        Assert.assertFalse(name.isEmpty(), "Name value must not be empty");

        logger.info("TC_MYACC_010 - name value verified: {}", name);
    }

    @Test(description = "TC_MYACC_011 - Date of Birth value is visible and well-formed (dd-MMM-yyyy)")
    public void tc_myacc_011_verifyDobValue() {
        Assert.assertTrue(
                myAccountPage.isDateOfBirthValueDisplayed(),
                "Date of Birth value must be visible");

        String dob = myAccountPage.getDateOfBirth();
        Assert.assertFalse(dob.isEmpty(), "DOB value must not be empty");
        Assert.assertTrue(
                dob.matches("\\d{2}-[A-Za-z]{3}-\\d{4}"),
                "DOB must match dd-MMM-yyyy, got: " + dob);

        logger.info("TC_MYACC_011 - DOB value verified: {}", dob);
    }

    // ============================================================
    // SECTION 4 - LOGOUT (presence only, never tapped)
    // ============================================================

    @Test(description = "TC_MYACC_012 - Logout button is present (not tapped - destroys PIN setup)")
    public void tc_myacc_012_verifyLogoutPresent() {
        Assert.assertTrue(
                myAccountPage.isLogoutDisplayed(),
                "Logout button must be present on the Account Details screen");
        logger.info("TC_MYACC_012 - logout presence verified (intentionally not tapped)");
    }

    // ============================================================
    // SECTION 5 - LINK NAVIGATION (tap link, validate opened screen, return)
    // ============================================================

    @Test(description = "TC_MYACC_013 - Edit Account Details link opens an editable screen")
    public void tc_myacc_013_verifyEditLinkNavigation() {
        Assert.assertTrue(
                myAccountPage.tapEditAccountDetailsAndVerify(),
                "Edit Account Details must open an editable screen");
        logger.info("TC_MYACC_013 - edit link navigation verified");
    }

    @Test(description = "TC_MYACC_014 - Header action icon opens a valid screen")
    public void tc_myacc_014_verifyHeaderIconNavigation() {
        Assert.assertTrue(
                myAccountPage.tapHeaderActionIconAndVerify(),
                "Header action icon must open a valid screen");
        logger.info("TC_MYACC_014 - header icon navigation verified");
    }

    @Test(description = "TC_MYACC_015 - Every (non-destructive) link opens a valid screen")
    public void tc_myacc_015_verifyAllLinksOpenValidScreens() {
        List<LinkResult> results = myAccountPage.tapAllLinksAndValidateScreens();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "Link must open/validate successfully: " + result.name);
        }

        logger.info("TC_MYACC_015 - all link navigation verified: {}", results);
    }

    @Test(description = "TC_MYACC_016 - Back button navigates away from Account Details screen")
    public void tc_myacc_016_verifyBackNavigation() {
        Assert.assertTrue(
                myAccountPage.isBackButtonDisplayed(),
                "Back button must be visible before tapping");

        Assert.assertTrue(
                myAccountPage.tapBackAndVerify(),
                "Tapping back must navigate away from Account Details");

        logger.info("TC_MYACC_016 - back navigation verified");
    }
}
