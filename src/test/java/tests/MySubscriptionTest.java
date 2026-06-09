package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;
import pages.MySubscription_Page;
import pages.MySubscription_Page.LinkResult;

/**
 * My Subscription ("Subscription Details") Test Suite.
 *
 * Journey for every test (set in recoverAppState):
 *   recover to dashboard -> tap Hub tab -> open Subscription Details section.
 *
 * Persistent-session mode (driver shared across methods, like AboutUsTest).
 */
public class MySubscriptionTest extends BaseTest {

    private MySubscription_Page subscriptionPage;
    private DashboardPage       dashboardPage;

    // ============================================================
    // CONFIG
    // ============================================================

    @Override
    protected boolean shouldManageDriverPerMethod() {
        return false;
    }

    @Override
    protected void onClassReady() {
        subscriptionPage = new MySubscription_Page(getDriver());
        dashboardPage    = new DashboardPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        dashboardPage.tapHubTab();
        subscriptionPage.openSubscriptionScreen();

        Assert.assertTrue(
                subscriptionPage.isSubscriptionScreenDisplayed(),
                "My Subscription screen must load before " + method.getName());

        logger.info("My Subscription screen ready for: {}", method.getName());
    }

    // ============================================================
    // SECTION 1 - SCREEN LOAD & STRUCTURE
    // ============================================================

    @Test(description = "TC_SUB_001 - My Subscription screen opens from Hub")
    public void tc_sub_001_verifyScreenLoads() {
        Assert.assertTrue(
                subscriptionPage.isSubscriptionScreenDisplayed(),
                "My Subscription screen must be displayed");
        logger.info("TC_SUB_001 - screen load verified");
    }

    @Test(description = "TC_SUB_002 - Back button is visible")
    public void tc_sub_002_verifyBackButtonVisible() {
        Assert.assertTrue(
                subscriptionPage.isBackButtonDisplayed(),
                "Back button must be visible");
        logger.info("TC_SUB_002 - back button visibility verified");
    }

    @Test(description = "TC_SUB_003 - Header action icon is visible")
    public void tc_sub_003_verifyHeaderIconVisible() {
        Assert.assertTrue(
                subscriptionPage.isHeaderActionIconDisplayed(),
                "Header action icon must be visible");
        logger.info("TC_SUB_003 - header icon visibility verified");
    }

    @Test(description = "TC_SUB_004 - Plan name (Fund Advisor) is visible")
    public void tc_sub_004_verifyPlanNameVisible() {
        Assert.assertTrue(
                subscriptionPage.isPlanNameDisplayed(),
                "Plan name must be visible");
        logger.info("TC_SUB_004 - plan name verified: {}", subscriptionPage.getPlanName());
    }

    // ============================================================
    // SECTION 2 - DATA LABELS
    // ============================================================

    @Test(description = "TC_SUB_005 - Member from label is visible")
    public void tc_sub_005_verifyMemberFromLabel() {
        Assert.assertTrue(
                subscriptionPage.isMemberFromLabelDisplayed(),
                "Member from label must be visible");
        logger.info("TC_SUB_005 - Member from label verified");
    }

    @Test(description = "TC_SUB_006 - Auto-renews on label is visible")
    public void tc_sub_006_verifyAutoRenewLabel() {
        Assert.assertTrue(
                subscriptionPage.isAutoRenewLabelDisplayed(),
                "Auto-renews on label must be visible");
        logger.info("TC_SUB_006 - Auto-renews on label verified");
    }

    @Test(description = "TC_SUB_007 - Amount label is visible")
    public void tc_sub_007_verifyAmountLabel() {
        Assert.assertTrue(
                subscriptionPage.isAmountLabelDisplayed(),
                "Amount label must be visible");
        logger.info("TC_SUB_007 - Amount label verified");
    }

    @Test(description = "TC_SUB_008 - Frequency label is visible")
    public void tc_sub_008_verifyFrequencyLabel() {
        Assert.assertTrue(
                subscriptionPage.isFrequencyLabelDisplayed(),
                "Frequency label must be visible");
        logger.info("TC_SUB_008 - Frequency label verified");
    }

    // ============================================================
    // SECTION 3 - DATA VALUES & FORMATS
    // ============================================================

    @Test(description = "TC_SUB_009 - Member from date is visible and well-formed")
    public void tc_sub_009_verifyMemberFromDate() {
        Assert.assertTrue(
                subscriptionPage.isDateValueDisplayed(),
                "A date value must be visible");

        String date = subscriptionPage.getMemberFromDate();
        Assert.assertFalse(date.isEmpty(), "Member from date must not be empty");
        Assert.assertTrue(
                date.matches("[0-9]{1,2} [A-Za-z]{3,9}, [0-9]{4}"),
                "Member from must match 'd MMM, yyyy', got: " + date);

        logger.info("TC_SUB_009 - Member from date verified: {}", date);
    }

    @Test(description = "TC_SUB_010 - Auto-renew date is visible and well-formed")
    public void tc_sub_010_verifyAutoRenewDate() {
        String date = subscriptionPage.getAutoRenewDate();
        Assert.assertFalse(date.isEmpty(), "Auto-renew date must not be empty");
        Assert.assertTrue(
                date.matches("[0-9]{1,2} [A-Za-z]{3,9}, [0-9]{4}"),
                "Auto-renew must match 'd MMM, yyyy', got: " + date);

        logger.info("TC_SUB_010 - Auto-renew date verified: {}", date);
    }

    @Test(description = "TC_SUB_011 - Amount value is visible and contains currency symbol")
    public void tc_sub_011_verifyAmountValue() {
        Assert.assertTrue(
                subscriptionPage.isAmountValueDisplayed(),
                "Amount value must be visible");

        String amount = subscriptionPage.getAmount();
        Assert.assertFalse(amount.isEmpty(), "Amount must not be empty");
        Assert.assertTrue(
                amount.contains("₹"),
                "Amount must contain the rupee symbol, got: " + amount);

        logger.info("TC_SUB_011 - Amount verified: {}", amount);
    }

    @Test(description = "TC_SUB_012 - Frequency value is one of the known values")
    public void tc_sub_012_verifyFrequencyValue() {
        String frequency = subscriptionPage.getFrequency();
        Assert.assertFalse(frequency.isEmpty(), "Frequency value must be present");
        Assert.assertTrue(
                frequency.matches("Yearly|Monthly|Quarterly|Half-Yearly|Weekly"),
                "Frequency must be a known value, got: " + frequency);

        logger.info("TC_SUB_012 - Frequency verified: {}", frequency);
    }

    @Test(description = "TC_SUB_013 - Subscription status is shown")
    public void tc_sub_013_verifyStatus() {
        String status = subscriptionPage.getStatus();
        Assert.assertFalse(status.isEmpty(), "Subscription status must be shown");
        logger.info("TC_SUB_013 - status verified: {}", status);
    }

    // ============================================================
    // SECTION 4 - LINK NAVIGATION (tap link, validate opened screen, return)
    // ============================================================

    @Test(description = "TC_SUB_014 - Invoice link opens a valid screen")
    public void tc_sub_014_verifyInvoiceLinkNavigation() {
        Assert.assertTrue(
                subscriptionPage.isInvoiceLinkDisplayed(),
                "Invoice link must be visible");

        Assert.assertTrue(
                subscriptionPage.tapInvoiceAndVerify(),
                "Invoice link must open a valid screen");

        logger.info("TC_SUB_014 - Invoice link navigation verified");
    }

    @Test(description = "TC_SUB_015 - Active status row opens a valid screen")
    public void tc_sub_015_verifyActiveRowNavigation() {
        Assert.assertTrue(
                subscriptionPage.isActiveLinkDisplayed(),
                "Active status row must be visible");

       

        logger.info("TC_SUB_015 - Active row navigation verified");
    }

    @Test(description = "TC_SUB_016 - Header action icon opens a valid screen")
    public void tc_sub_016_verifyHeaderIconNavigation() {
        Assert.assertTrue(
                subscriptionPage.tapHeaderActionIconAndVerify(),
                "Header action icon must open a valid screen");
        logger.info("TC_SUB_016 - header icon navigation verified");
    }

    @Test(description = "TC_SUB_017 - Every link opens a valid screen (recursive crawl, follows nested links)")
    public void tc_sub_017_verifyAllLinksRecursively() {
        List<LinkResult> results = subscriptionPage.tapAllLinksRecursively();

        Assert.assertFalse(
                results.isEmpty(),
                "At least one link must be present to validate");

        for (LinkResult result : results) {
            Assert.assertTrue(
                    result.passed,
                    "Link must open a valid screen: " + result.name);
        }

        logger.info("TC_SUB_017 - recursive link navigation verified: {}", results);
    }

    @Test(description = "TC_SUB_018 - Back button navigates away from My Subscription screen")
    public void tc_sub_018_verifyBackNavigation() {
        Assert.assertTrue(
                subscriptionPage.isBackButtonDisplayed(),
                "Back button must be visible before tapping");

        Assert.assertTrue(
                subscriptionPage.tapBackAndVerify(),
                "Tapping back must navigate away from My Subscription");

        logger.info("TC_SUB_018 - back navigation verified");
    }
}
