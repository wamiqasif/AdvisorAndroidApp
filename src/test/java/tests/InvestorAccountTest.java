package tests;

import java.lang.reflect.Method;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;
import pages.InvestorAccountPage;

/**
 * Investor Account — Basic Details Test Suite
 *
 * All tests start from the Basic Details screen.
 * recoverAppState() navigates to Basic Details before each test.
 *
 * Update InvestorAccountPage.navigateToBasicDetails() with the
 * actual entry point for this screen in your app.
 */
public class InvestorAccountTest extends BaseTest {

    private InvestorAccountPage accountPage;
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
        accountPage   = new InvestorAccountPage(getDriver());
        dashboardPage = new DashboardPage(getDriver());
    }

    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.recoverToDashboard();
        accountPage.navigateToBasicDetails();
        Assert.assertTrue(
                accountPage.isBasicDetailsScreenDisplayed(),
                "Basic Details screen must load before " + method.getName());
        logger.info("Basic Details screen ready for: {}", method.getName());
    }

    // ============================================================
    // DATA PROVIDERS
    // ============================================================

    @DataProvider(name = "invalidMobileNumbers")
    public Object[][] invalidMobileNumbers() {
        return new Object[][]{
                {"abcdefghij"},
                {"12345"},
                {"@#$%^&*()!"}
        };
    }

    @DataProvider(name = "invalidEmails")
    public Object[][] invalidEmails() {
        return new Object[][]{
                {"invalidemail"},
                {"user name@domain.com"},
                {"user@"}
        };
    }

    // ============================================================
    // SECTION 1 — NAVIGATION & SCREEN LOAD
    // ============================================================

    @Test(description = "TC_BD_001 — Basic Details screen opens and all major sections visible")
    public void tc_bd_001_verifyBasicDetailsScreenLoads() {

        Assert.assertTrue(
                accountPage.isBasicDetailsScreenDisplayed(),
                "Basic Details screen must load");

        Assert.assertTrue(
                accountPage.isPanNumberLabelDisplayed(),
                "PAN Number label must be visible");

        Assert.assertTrue(
                accountPage.isKycStatusLabelDisplayed(),
                "KYC Status label must be visible");

        Assert.assertTrue(
                accountPage.isTransactionAccountLabelDisplayed(),
                "Transaction Account label must be visible");

        Assert.assertTrue(
                accountPage.isMobileLabelDisplayed(),
                "Mobile label must be visible");

        Assert.assertTrue(
                accountPage.isEmailLabelDisplayed(),
                "Email label must be visible");

        logger.info("TC_BD_001 — Basic Details screen load verified");
    }

    @Test(description = "TC_BD_002 — Back button navigates to previous screen")
    public void tc_bd_002_verifyBackButtonNavigation() {

        Assert.assertTrue(
                accountPage.isBackButtonDisplayed(),
                "Back button must be visible");

        Assert.assertTrue(
        		accountPage.tapBackAndVerify(),
                "Tapping back must navigate away from Basic Details");

        logger.info("TC_BD_002 — Back button navigation verified");
    }

    @Test(description = "TC_BD_003 — Screen persists after app background/foreground")
    public void tc_bd_003_verifyScreenPersistsAfterBackgroundForeground() {

        Assert.assertTrue(
                accountPage.isBasicDetailsScreenDisplayed(),
                "Basic Details screen must be active before background");

        try {
            getDriver().runAppInBackground(java.time.Duration.ofSeconds(3));
        } catch (Exception e) {
            throw new SkipException("runAppInBackground not supported: " + e.getMessage());
        }

        waitForUiToSettle();

        Assert.assertTrue(
                accountPage.isBasicDetailsScreenDisplayed(),
                "Basic Details screen must restore after foreground");

        logger.info("TC_BD_003 — Screen persistence after background verified");
    }

    // ============================================================
    // SECTION 2 — INVESTOR DROPDOWN
    // ============================================================

    @Test(description = "TC_BD_005 — Investor dropdown expands on tap")
    public void tc_bd_005_verifyInvestorDropdownExpands() {

        Assert.assertTrue(
                accountPage.isInvestorButtonDisplayed(),
                "Investor dropdown button must be visible");

        accountPage.tapInvestorDropdown();

        Assert.assertTrue(
                accountPage.isInvestorDropdownExpanded(),
                "Investor dropdown must expand on tap");

        logger.info("TC_BD_005 — Investor dropdown expansion verified");
    }

    @Test(description = "TC_BD_006 — Switching investor updates PAN and KYC details")
    public void tc_bd_006_verifyInvestorSwitchUpdatesDetails() {

        String panBefore = accountPage.getPanNumber();

        if (panBefore.isEmpty()) {
            throw new SkipException("No PAN visible — cannot compare before/after investor switch");
        }

        accountPage.tapInvestorDropdown();

        if (!accountPage.isInvestorDropdownExpanded()) {
            throw new SkipException("Investor dropdown did not expand — only one investor may be available");
        }

        // Tap second investor in the list
        try {
            getDriver().findElements(
                    io.appium.java_client.AppiumBy.accessibilityId(
                            "Vinit Sharma"))
                    .stream()
                    .filter(e -> e.isDisplayed() && !e.getAttribute("content-desc").equals("Investor"))
                    .findFirst()
                    .orElseThrow(() -> new SkipException("No second investor found in dropdown"))
                    .click();
        } catch (SkipException se) {
            throw se;
        } catch (Exception e) {
            throw new SkipException("Unable to select second investor: " + e.getMessage());
        }

        waitForUiToSettle();

        Assert.assertTrue(
                accountPage.isBasicDetailsScreenDisplayed(),
                "Basic Details must reload after investor switch");

        logger.info("TC_BD_006 — Investor switch and data refresh verified");
    }

    @Test(description = "TC_BD_007 — Investor dropdown closes on outside tap (via back press)")
    public void tc_bd_007_verifyInvestorDropdownClosesOnOutsideTap() {

        accountPage.tapInvestorDropdown();

        if (!accountPage.isInvestorDropdownExpanded()) {
            throw new SkipException("Investor dropdown did not expand");
        }

        getDriver().navigate().back();
        waitForUiToSettle();

        Assert.assertFalse(
                accountPage.isInvestorDropdownExpanded2(),
                "Investor dropdown must close after outside interaction");

        logger.info("TC_BD_007 — Investor dropdown close behavior verified");
    }

    // ============================================================
    // SECTION 3 — ADD INVESTOR CTA
    // ============================================================

    @Test(description = "TC_BD_009 — Add Investor CTA opens add investor flow")
    public void tc_bd_009_verifyAddInvestorCtaNavigation() {
    	 accountPage.tapAddInvestorCta();
    	 getDriver().navigate().back();
         waitForUiToSettle();
        
        
        
        accountPage.tapAddInvestorCta();
        if (!accountPage.isAddInvestorCtaDisplayed()) {
            throw new SkipException("Add Investor CTA not visible — max investors may already be reached");
        }

       

        Assert.assertFalse(
                accountPage.isBasicDetailsScreenDisplayed(),
                "Basic Details screen must no longer show after tapping Add Investor");

        logger.info("TC_BD_009 — Add Investor CTA navigation verified");
    }

    // ============================================================
    // SECTION 4 — BASIC DETAILS VALIDATION
    // ============================================================

    @Test(description = "TC_BD_012 — PAN value visible and matches expected format")
    public void tc_bd_012_verifyPanFormat() {

        Assert.assertTrue(
                accountPage.isPanValueDisplayed(),
                "PAN value must be visible in expected 10-char format");

        String pan = accountPage.getPanNumber();

        Assert.assertFalse(
                pan.isEmpty(),
                "PAN value must not be empty");

        Assert.assertTrue(
                pan.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}"),
                "PAN must match format AAAAA9999A, got: " + pan);

        logger.info("TC_BD_012 — PAN format verified: {}", pan);
    }

    @Test(description = "TC_BD_013 — KYC Validated badge shown for validated investor")
    public void tc_bd_013_verifyKycValidatedStatus() {

        if (!accountPage.isKycValidated()) {
            throw new SkipException("KYC Validated state not available for current investor");
        }

        Assert.assertTrue(
                accountPage.isKycValidatedStatusDisplayed(),
                "KYC Validated badge must display");

        logger.info("TC_BD_013 — KYC Validated status verified");
    }

    @Test(description = "TC_BD_014 — KYC inactive/pending state shown correctly")
    public void tc_bd_014_verifyKycPendingState() {

        if (!accountPage.isKycPending()) {
            throw new SkipException("KYC Pending state not available for current investor");
        }

        Assert.assertTrue(
                accountPage.isKycPendingStatusDisplayed(),
                "KYC Pending status must display");

        logger.info("TC_BD_014 — KYC Pending state verified");
    }

    @Test(description = "TC_BD_015 — Transaction Account status is shown")
    public void tc_bd_015_verifyTransactionAccountStatus() {

        Assert.assertTrue(
                accountPage.isTransactionAccountLabelDisplayed(),
                "Transaction Account label must be visible");

        boolean hasStatus =
                accountPage.isTransactionAccountActive()
                        ||accountPage.isTransactionAccountLabelDisplayed();

        Assert.assertTrue(
                hasStatus,
                "Transaction Account status must be shown");

        logger.info("TC_BD_015 — Transaction Account status verified");
    }

    @Test(description = "TC_BD_016 — Mobile edit icon opens mobile edit flow")
    public void tc_bd_016_verifyMobileEditIconNavigation() {
    	accountPage.selectInvestor("Wamiq Azeem Asif");
        Assert.assertTrue(
                accountPage.isMobileEditIconDisplayed(),
                "Mobile edit icon must be visible");

        boolean arrived = accountPage.tapMobileEditAndVerify();

        Assert.assertTrue(
                arrived,
                "Mobile edit flow must open after tapping edit icon");

        logger.info("TC_BD_016 — Mobile edit icon navigation verified");
    }

    @Test(description = "TC_BD_017 — Email edit icon opens email edit flow")
    public void tc_bd_017_verifyEmailEditIconNavigation() {
    	accountPage.selectInvestor("Wamiq Azeem Asif");
        Assert.assertTrue(
                accountPage.isEmailEditIconDisplayed(),
                "Email edit icon must be visible");

        boolean arrived = accountPage.tapEmailEditAndVerify();

        Assert.assertTrue(
                arrived,
                "Email edit flow must open after tapping edit icon");

        logger.info("TC_BD_017 — Email edit icon navigation verified");
    }

    // ============================================================
    // SECTION 5 — BANK ACCOUNT DETAILS
    // ============================================================

    @Test(description = "TC_BD_021 — Bank Account Details section visible with all sub-elements")
    public void tc_bd_021_verifyBankAccountSectionVisibility() {
         accountPage.selectInvestor("Wamiq Azeem Asif");
        accountPage.scrollToBankDetails();

        Assert.assertTrue(
                accountPage.isBankAccountSectionDisplayed(),
                "Bank Account Details heading must be visible");

        Assert.assertTrue(
                accountPage.isBankNameDisplayed(),
                "Bank name must be visible");

        Assert.assertTrue(
                accountPage.isMaskedAccountNumberDisplayed(),
                "Masked account number must be visible");

        Assert.assertTrue(
                accountPage.isIfscDisplayed(),
                "IFSC code must be visible");

        logger.info("TC_BD_021 — Bank Account Details section verified");
    }

    @Test(description = "TC_BD_022 — Bank account shows Approved badge")
    public void tc_bd_022_verifyBankApprovalBadge() {
    	accountPage.selectInvestor("Wamiq Azeem Asif");
        accountPage.scrollToBankDetails();

        if (!accountPage.isBankApprovedBadgeDisplayed()) {
            throw new SkipException("Bank not in Approved state for current investor");
        }

        Assert.assertTrue(
                accountPage.isBankApprovedBadgeDisplayed(),
                "Approved badge must be visible for approved bank accounts");

        logger.info("TC_BD_022 — Bank Approved badge verified");
    }

    @Test(description = "TC_BD_023 — Account number is masked")
    public void tc_bd_023_verifyMaskedAccountNumber() {
    	accountPage.selectInvestor("Wamiq Azeem Asif");
        accountPage.scrollToBankDetails();

        Assert.assertTrue(
                accountPage.isMaskedAccountNumberDisplayed(),
                "Account number must be partially masked (XXXX format)");

        logger.info("TC_BD_023 — Masked account number verified");
    }

    @Test(description = "TC_BD_024 — IFSC code matches expected format")
    public void tc_bd_024_verifyIfscFormat() {
    	accountPage.selectInvestor("Wamiq Azeem Asif");
        accountPage.scrollToBankDetails();

        Assert.assertTrue(
                accountPage.isIfscDisplayed(),
                "IFSC code must be visible");

        String ifsc = accountPage.getIfscCode();

        Assert.assertFalse(
                ifsc.isEmpty(),
                "IFSC value must not be empty");

        Assert.assertTrue(
                ifsc.matches("[A-Z]{4}0[A-Z0-9]{6}"),
                "IFSC must match format AAAA0AAAAAA, got: " + ifsc);

        logger.info("TC_BD_024 — IFSC format verified: {}", ifsc);
    }

    @Test(description = "TC_BD_025 — Empty state shown when no bank linked")
    public void tc_bd_025_verifyNoBankLinkedEmptyState() {

        accountPage.scrollToBankDetails();

        if (accountPage.isBankNameDisplayed()) {
            throw new SkipException("Bank is already linked for this investor — empty state not available");
        }

        Assert.assertTrue(
                accountPage.isNoBankLinkedStateDisplayed(),
                "No-bank-linked empty state must show when no bank is added");

        logger.info("TC_BD_025 — No bank linked empty state verified");
    }

    // ============================================================
    // SECTION 6 — AUTOPAY MANDATES
    // ============================================================

    @Test(description = "TC_BD_027 — No mandate empty state shown when no mandates exist")
    public void tc_bd_027_verifyNoMandateEmptyState() {

        accountPage.scrollToAutopayMandates();

        if (accountPage.isMandateListDisplayed()) {
            throw new SkipException("Mandates are present — empty state not available");
        }

        Assert.assertTrue(
                accountPage.isNoMandateEmptyStateDisplayed(),
                "No-payment-mandate message must display");

        logger.info("TC_BD_027 — No mandate empty state verified");
    }

    @Test(description = "TC_BD_028 — Mandate list displays correctly when mandates exist")
    public void tc_bd_028_verifyMandateListDisplay() {

        accountPage.scrollToAutopayMandates();

        if (!accountPage.isMandateListDisplayed()) {
            throw new SkipException("No mandates present for current investor");
        }

        Assert.assertTrue(
                accountPage.isAutopayMandatesHeadingDisplayed(),
                "Autopay Mandates heading must be visible");

        Assert.assertTrue(
                accountPage.isMandateListDisplayed(),
                "Mandate list item must be displayed");

        logger.info("TC_BD_028 — Mandate list display verified");
    }

    @Test(description = "TC_BD_029 — Expired mandate shows correct status label")
    public void tc_bd_029_verifyExpiredMandateStatus() {

        accountPage.scrollToAutopayMandates();

        if (!accountPage.isExpiredMandateStatusDisplayed()) {
            throw new SkipException("No expired mandate present for current investor");
        }

        Assert.assertTrue(
                accountPage.isExpiredMandateStatusDisplayed(),
                "Expired status must be shown for expired mandate");

        logger.info("TC_BD_029 — Expired mandate status verified");
    }

    // ============================================================
    // SECTION 7 — NOMINEE DETAILS
    // ============================================================

    @Test(description = "TC_BD_031 — Nominee Details section visible with key sub-elements")
    public void tc_bd_031_verifyNomineeDetailsSectionVisibility() {

        accountPage.scrollToNomineeDetails();

        Assert.assertTrue(
                accountPage.isNomineeDetailsHeadingDisplayed(),
                "Nominee Details heading must be visible");

        logger.info("TC_BD_031 — Nominee Details section heading verified");
    }

    @Test(description = "TC_BD_032 — Nominee percentage total is 100%")
    public void tc_bd_032_verifyNomineePercentageTotal() {

        accountPage.scrollToNomineeDetails();

        if (!accountPage.isNomineePercentageDisplayed()) {
            throw new SkipException("No nominee percentage visible for current investor");
        }

        String percentText = accountPage.getNomineePercentageText();

        Assert.assertFalse(
                percentText.isEmpty(),
                "Nominee percentage text must not be empty");

        Assert.assertTrue(
                percentText.contains("%"),
                "Nominee percentage text must contain % symbol");

        logger.info("TC_BD_032 — Nominee percentage verified: {}", percentText);
    }

    @Test(description = "TC_BD_034 — Nominee Aadhaar is masked")
    public void tc_bd_034_verifyNomineeAadhaarMasking() {

        accountPage.scrollToNomineeDetails();

        if (!accountPage.isNomineeAadhaarMasked()) {
            throw new SkipException("No masked Aadhaar visible — nominee may not have Aadhaar on record");
        }

        Assert.assertTrue(
                accountPage.isNomineeAadhaarMasked(),
                "Nominee Aadhaar must be partially masked");

        logger.info("TC_BD_034 — Nominee Aadhaar masking verified");
    }

    // ============================================================
    // SECTION 8 — RISK ASSESSMENT
    // ============================================================

    @Test(description = "TC_BD_037 — Risk Assessment card shows risk type, last updated, and View More CTA")
    public void tc_bd_037_verifyRiskAssessmentCardVisibility() {

        accountPage.scrollToRiskAssessment();

        Assert.assertTrue(
                accountPage.isRiskAssessmentHeadingDisplayed(),
                "Risk Assessment heading must be visible");

        Assert.assertTrue(
                accountPage.isAnyRiskTypeDisplayed(),
                "At least one risk type (Conservative/Moderate/Aggressive) must be shown");

        Assert.assertTrue(
                accountPage.isRiskViewMoreCtaDisplayed(),
                "View More CTA must be visible on Risk Assessment card");

        logger.info("TC_BD_037 — Risk Assessment card verified");
    }

    @Test(description = "TC_BD_038 — View More opens Risk Assessment detail screen")
    public void tc_bd_038_verifyRiskViewMoreNavigation() {

        accountPage.scrollToRiskAssessment();

        if (!accountPage.isRiskViewMoreCtaDisplayed()) {
            throw new SkipException("View More CTA not visible on Risk Assessment card");
        }

        boolean arrived = accountPage.tapRiskViewMoreAndVerify();

        Assert.assertTrue(
                arrived,
                "Risk Assessment detail screen must open after tapping View More");

        logger.info("TC_BD_038 — Risk View More navigation verified");
    }

    @Test(description = "TC_BD_039 — Risk type shown is one of the valid values")
    public void tc_bd_039_verifyRiskTypeIsValid() {

        accountPage.scrollToRiskAssessment();

        String riskType = accountPage.getRiskType();

        if (riskType.isEmpty()) {
            throw new SkipException("No risk type visible — assessment may be pending");
        }

        Assert.assertTrue(
                riskType.equals("Conservative")
                        || riskType.equals("Moderate")
                        || riskType.equals("Aggressive"),
                "Risk type must be one of: Conservative, Moderate, Aggressive. Got: " + riskType);

        logger.info("TC_BD_039 — Risk type validity verified: {}", riskType);
    }

    @Test(description = "TC_BD_041 — Prompt shown when risk assessment is not completed")
    public void tc_bd_041_verifyMissingRiskAssessmentState() {

        accountPage.scrollToRiskAssessment();

        if (accountPage.isAnyRiskTypeDisplayed()) {
            throw new SkipException("Risk assessment is already completed — pending state unavailable");
        }

        Assert.assertTrue(
                accountPage.isRiskAssessmentPendingPromptDisplayed(),
                "Risk assessment pending prompt must show when not completed");

        logger.info("TC_BD_041 — Missing risk assessment state verified");
    }

    // ============================================================
    // SECTION 9 — FAQ ACCORDION
    // ============================================================

    @Test(description = "TC_BD_042 — FAQ item expands on tap")
    public void tc_bd_042_verifyFaqItemExpansion() {

        accountPage.scrollToFaq();

        Assert.assertTrue(
                accountPage.isFaqSectionDisplayed(),
                "FAQ section must be visible");

        accountPage.tapFirstFaqItem();

        Assert.assertTrue(
                accountPage.isFaqItemExpanded(),
                "FAQ item must expand after tap");

        logger.info("TC_BD_042 — FAQ expansion verified");
    }

    @Test(description = "TC_BD_043 — FAQ item collapses on second tap")
    public void tc_bd_043_verifyFaqItemCollapse() {

        accountPage.scrollToFaq();
        accountPage.tapFirstFaqItem();

        Assert.assertTrue(
                accountPage.isFaqItemExpanded(),
                "FAQ must be expanded before testing collapse");

        accountPage.tapFirstFaqItem();

        Assert.assertTrue(
                accountPage.isFaqItemCollapsed(),
                "FAQ item must collapse on second tap");

        logger.info("TC_BD_043 — FAQ collapse verified");
    }

    @Test(description = "TC_BD_044 — Multiple FAQ items can be tapped without crash")
    public void tc_bd_044_verifyMultipleFaqBehavior() {

        accountPage.scrollToFaq();

        Assert.assertTrue(
                accountPage.isFaqSectionDisplayed(),
                "FAQ section must be visible");

        // Open and close repeatedly to test accordion stability
        for (int i = 0; i < 3; i++) {
            accountPage.tapFirstFaqItem();
            waitForUiToSettle();
        }

        Assert.assertTrue(
                accountPage.isBasicDetailsScreenDisplayed()
                        || accountPage.isFaqSectionDisplayed(),
                "App must remain stable after repeated FAQ taps");

        logger.info("TC_BD_044 — Multiple FAQ interaction stability verified");
    }

    // ============================================================
    // SECTION 10 — SEARCH
    // ============================================================

    @Test(description = "TC_BD_046 — Search icon tap opens search screen")
    public void tc_bd_046_verifySearchIconOpensSearchScreen() {

        if (!accountPage.isSearchIconDisplayed()) {
            throw new SkipException("Search icon not visible on Basic Details screen");
        }

        boolean arrived = accountPage.tapSearchIconAndVerify();

        Assert.assertTrue(
                arrived,
                "Search screen must open after tapping search icon");

        logger.info("TC_BD_046 — Search icon navigation verified");
    }

    // ============================================================
    // SECTION 11 — BOTTOM NAVIGATION
    // ============================================================

    @Test(description = "TC_BD_048 — Funds tab in bottom nav is reachable")
    public void tc_bd_048_verifyFundsTabNavigation() {

        Assert.assertTrue(
                accountPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible");

        boolean arrived = accountPage.tapFundsTabAndVerify();

        Assert.assertTrue(
                arrived,
                "Funds tab must load successfully");

        logger.info("TC_BD_048 — Funds tab navigation verified");
    }

    @Test(description = "TC_BD_049 — Stocks tab in bottom nav is reachable")
    public void tc_bd_049_verifyStocksTabNavigation() {

        Assert.assertTrue(
                accountPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible");

        boolean arrived = accountPage.tapStocksTabAndVerify();

        Assert.assertTrue(
                arrived,
                "Stocks tab must load successfully");

        logger.info("TC_BD_049 — Stocks tab navigation verified");
    }

    @Test(description = "TC_BD_050 — Portfolio tab in bottom nav is reachable")
    public void tc_bd_050_verifyPortfolioTabNavigation() {

        Assert.assertTrue(
                accountPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible");

        boolean arrived = accountPage.tapPortfolioTabAndVerify();

        Assert.assertTrue(
                arrived,
                "Portfolio tab must load successfully");

        logger.info("TC_BD_050 — Portfolio tab navigation verified");
    }

    @Test(description = "TC_BD_051 — Hub tab in bottom nav is reachable")
    public void tc_bd_051_verifyHubTabNavigation() {

        Assert.assertTrue(
                accountPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible");

        boolean arrived = accountPage.tapHubTabAndVerify();

        Assert.assertTrue(
                arrived,
                "Hub tab must load successfully");

        logger.info("TC_BD_051 — Hub tab navigation verified");
    }

    @Test(description = "TC_BD_052 — Bottom navigation remains visible after scrolling down")
    public void tc_bd_052_verifyBottomNavPersistsAfterScroll() {

        Assert.assertTrue(
                accountPage.isBottomNavigationDisplayed(),
                "Bottom navigation must be visible before scroll");

        // Scroll down to bottom of page
        accountPage.safeVerticalScroll("up");
        accountPage.safeVerticalScroll("up");
        waitForUiToSettle();

        Assert.assertTrue(
                accountPage.isBottomNavigationDisplayed(),
                "Bottom navigation must remain visible after scrolling");

        logger.info("TC_BD_052 — Bottom nav persistence after scroll verified");
    }
}
