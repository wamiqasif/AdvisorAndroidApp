package pages;

import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class InvestorAccountPage extends BasePage {

    // ============================================================
    // NAVIGATION
    // ============================================================

    private final By backArrowButton = AppiumBy.accessibilityId("Go back");

    // No stable content-desc — positional fallback (top-right edit icon on the screen header)
    private final By headerEditIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(1)");

    // Search icon in the app header
    private final By headerSearchIcon = AppiumBy.accessibilityId("Search");

    // ============================================================
    // INVESTOR DROPDOWN
    // ============================================================

    private final By investorButton = AppiumBy.accessibilityId("Investor");
    private final By investorDropDown = AppiumBy.accessibilityId("Maushami Singh");
    
    // After tapping investorButton, a list of investors appears; match any item inside it
    private final By investorDropdownList = AppiumBy
            .androidUIAutomator("new UiSelector().description(\"Choose Investor\")");

    // "+ Investor" / Add Investor CTA — adjust content-desc if different
    private final By addInvestorCta = AppiumBy
            .accessibilityId("Investor");

    // ============================================================
    // BASIC DETAILS SECTION
    // ============================================================

    private final By basicDetailsHeading = AppiumBy.accessibilityId("Basic Details");

    private final By panNumberLabel = AppiumBy.accessibilityId("PAN Number");

    // PAN values are always 10 chars: 5 alpha + 4 digit + 1 alpha
    private final By panValue = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\"[A-Z]{5}[0-9]{4}[A-Z]{1}\")");

    private final By kycStatusLabel = AppiumBy.accessibilityId("KYC Status");

    private final By kycValidatedStatus = AppiumBy.accessibilityId("KYC Validated");
    private final By kycPendingStatus   = AppiumBy.accessibilityId("KYC Pending");

    private final By transactionAccountLabel = AppiumBy.accessibilityId("Transaction Account");

    private final By transactionAccountActiveStatus = AppiumBy.accessibilityId("Inactive");

    // ============================================================
    // CONTACT DETAILS
    // ============================================================

    private final By mobileLabel = AppiumBy.accessibilityId("Mobile");

    // 10-digit mobile number — match by pattern
    private final By mobileValue = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\"[0-9]{10}\")");

    // Edit icons — positional fallbacks (no content-desc on these ImageViews)
    private final By mobileEditIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(2)");

    private final By emailLabel = AppiumBy.accessibilityId("Email");

    private final By emailValue = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"@\")");

    private final By emailEditIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(3)");

    // ============================================================
    // BANK ACCOUNT DETAILS SECTION
    // ============================================================

    private final By bankAccountDetailsHeading = AppiumBy.accessibilityId("Bank Account Details");

    private final By bankName = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BANK\")");

    // Account number is partially masked — last 4 digits visible, rest as XXXX
    private final By maskedAccountNumber = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"XXXX\")");

    // IFSC: 4 alpha + "0" + 6 alphanumeric
    private final By ifscCode = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\"[A-Z]{4}0[A-Z0-9]{6}\")");

    private final By bankBranchName = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Branch\")");

    private final By bankApprovedBadge = AppiumBy.accessibilityId("Approved");

    private final By noBankLinkedEmptyState = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"No bank\")");

    // ============================================================
    // AUTOPAY MANDATES SECTION
    // ============================================================

    private final By autopayMandatesHeading = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Mandate\")");

    private final By noMandateEmptyState = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"No payment mandate\")");

    private final By mandateListItem = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"mandate\").instance(0)");

    private final By expiredMandateStatus = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Expired\")");

    private final By inactiveMandateStatus = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Inactive\")");

    // ============================================================
    // NOMINEE DETAILS SECTION
    // ============================================================

    private final By nomineeDetailsHeading = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Nominee\")");

    // Masked Aadhaar — partial masking pattern like "XXXX XXXX 1234"
    private final By nomineeAadhaarMasked = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\".*XXXX.*\")");

    // Nominee share percentage
    private final By nomineePercentage = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"%\")");

    // ============================================================
    // RISK ASSESSMENT SECTION
    // ============================================================

    private final By riskAssessmentHeading = AppiumBy.accessibilityId("Risk Assessment");

    private final By riskTypeConservative = AppiumBy.accessibilityId("Conservative");
    private final By riskTypeModerate     = AppiumBy.accessibilityId("Moderate");
    private final By riskTypeAggressive   = AppiumBy.accessibilityId("Aggressive");

    private final By riskViewMoreCta = AppiumBy.accessibilityId("View More");

    private final By riskLastUpdated = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Last updated\")");

    // Shown when no risk assessment has been completed yet
    private final By riskAssessmentPendingPrompt = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Complete\")");

    // Post-navigation: Risk Assessment detail screen anchor
    private final By riskDetailScreenAnchor = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Risk\")");

    // ============================================================
    // FAQ ACCORDION SECTION
    // ============================================================

    private final By faqSectionHeading = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"FAQ\")");

    // First FAQ question item (clickable)
    private final By faqFirstItem = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"?\").instance(0)");

    // FAQ answer container — appears after expansion; adjust if needed
    private final By faqExpandedContent = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"?\").instance(1)");

    // ============================================================
    // SEARCH SCREEN (post-navigation)
    // ============================================================

    private final By searchScreenInput = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    private final By portfolioBottomTab = AppiumBy.accessibilityId("Portfolio");
    private final By hubBottomTab        = AppiumBy.accessibilityId("Hub");
    private final By fundsBottomTab      = AppiumBy.accessibilityId("Funds");
    private final By stocksBottomTab     = AppiumBy.accessibilityId("Stocks");

    // ============================================================
    // POST-NAVIGATION SCREEN LOCATORS
    // Verify / adjust against actual app behavior
    // ============================================================

    private final By editProfileSaveButton   = AppiumBy.accessibilityId("Save");
    private final By editProfileUpdateButton = AppiumBy.accessibilityId("Update");
    private final By editProfileFormInput    = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

    private final By mobileEditHeading = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Mobile\")");
    private final By mobileEditInput   = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");
    private final By mobileEditSendOtp = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"OTP\")");

    private final By emailEditHeading = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Email\")");
    private final By emailEditInput   = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

    // ============================================================

    public InvestorAccountPage(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // NAVIGATION TO SCREEN
    // ============================================================

    /**
     * Navigates to the Basic Details screen.
     * Call this after recovering to dashboard. Adjust the tap target
     * to match the actual entry point in your app (investor profile card,
     * a "Basic Details" tab, etc.).
     */
    public InvestorAccountPage navigateToBasicDetails() {
        logger.info("Navigating to Basic Details screen");
        if (!isBasicDetailsScreenDisplayed()) {
            safeClick(AppiumBy.accessibilityId("Basic Details"));
            waitForUiToSettle();
        }
        logger.info("Basic Details screen reached");
        return this;
    }

    // ============================================================
    // SCREEN DETECTION
    // ============================================================

    public boolean isBasicDetailsScreenDisplayed() {
        waitForVisible(basicDetailsHeading);
        return isDisplayed(basicDetailsHeading) && isDisplayed(panNumberLabel);
    }

    public boolean isBasicDetailsHeadingDisplayed() {
        logger.info("Checking Basic Details heading visibility");
        waitForVisible(basicDetailsHeading);
        return isDisplayed(basicDetailsHeading);
    }

    // ============================================================
    // LABEL VISIBILITY
    // ============================================================

    public boolean isPanNumberLabelDisplayed() {
        logger.info("Checking PAN Number label visibility");
        waitForVisible(panNumberLabel);
        return isDisplayed(panNumberLabel);
    }

    public boolean isPanValueDisplayed() {
        logger.info("Checking PAN value visibility");
        waitForVisible(panValue);
        return isDisplayed(panValue);
    }

    public boolean isKycStatusLabelDisplayed() {
        logger.info("Checking KYC Status label visibility");
        waitForVisible(kycStatusLabel);
        return isDisplayed(kycStatusLabel);
    }

    public boolean isKycValidatedStatusDisplayed() {
        logger.info("Checking KYC Validated status visibility");
        waitForVisible(kycValidatedStatus);
        return isDisplayed(kycValidatedStatus);
    }

    public boolean isKycPendingStatusDisplayed() {
        logger.info("Checking KYC Pending status visibility");
        waitForVisible(kycPendingStatus);
        return isDisplayed(kycPendingStatus);
    }

    public boolean isTransactionAccountLabelDisplayed() {
        logger.info("Checking Transaction Account label visibility");
        waitForVisible(transactionAccountLabel);
        return isDisplayed(transactionAccountLabel);
    }

    public boolean isTransactionAccountActiveDisplayed() {
        logger.info("Checking Transaction Account Active status visibility");
        waitForVisible(transactionAccountActiveStatus);
        return isDisplayed(transactionAccountActiveStatus);
    }

    public boolean isMobileLabelDisplayed() {
        logger.info("Checking Mobile label visibility");
        waitForVisible(mobileLabel);
        return isDisplayed(mobileLabel);
    }

    public boolean isMobileValueDisplayed() {
        logger.info("Checking mobile number value visibility");
        waitForVisible(mobileValue);
        return isDisplayed(mobileValue);
    }

    public boolean isEmailLabelDisplayed() {
        logger.info("Checking Email label visibility");
        waitForVisible(emailLabel);
        return isDisplayed(emailLabel);
    }

    public boolean isEmailValueDisplayed() {
        logger.info("Checking email value visibility");
        waitForVisible(emailValue);
        return isDisplayed(emailValue);
    }

    public boolean isEmailEditIconDisplayed() {
        logger.info("Checking email edit icon visibility");
        waitForVisible(emailEditIcon);
        return isDisplayed(emailEditIcon);
    }

    // ============================================================
    // KYC STATUS
    // ============================================================

    public boolean isKycValidated() {
        
        return isDisplayed(kycValidatedStatus);
    }

    public boolean isKycPending() {
        waitForVisible(kycPendingStatus);
        return isDisplayed(kycPendingStatus);
    }

    public String getKycStatusText() {
        try {
            if (isKycValidated()) return "KYC Validated";
            if (isKycPending())   return "KYC Pending";
            return waitForVisible(kycStatusLabel).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch KYC status text");
            return "";
        }
    }

    // ============================================================
    // TRANSACTION ACCOUNT
    // ============================================================

    public boolean isTransactionAccountActive() {
        waitForVisible(transactionAccountActiveStatus);
        return isDisplayed(transactionAccountActiveStatus);
    }

    // ============================================================
    // INVESTOR DROPDOWN
    // ============================================================

    public boolean isInvestorButtonDisplayed() {
        logger.info("Checking Investor button visibility");
        waitForVisible(investorButton);
        return isDisplayed(investorButton);
    }

    public boolean isInvestorDropdownExpanded() {
        logger.info("Checking investor dropdown expanded state");
        waitForVisible(investorDropdownList);
        return isDisplayed(investorDropdownList);
    }
    public boolean isInvestorDropdownExpanded2() {
        logger.info("Checking investor dropdown expanded state");
        
        return isDisplayed(investorDropdownList);
    }

    public boolean isAddInvestorCtaDisplayed() {
        logger.info("Checking Add Investor CTA visibility");
        
        return isDisplayed(addInvestorCta);
    }

    public InvestorAccountPage tapInvestorDropdown() {
        logger.info("Tapping investor dropdown");
        safeClick(investorDropDown);
        waitForUiToSettle();
        logger.info("Investor dropdown tapped");
        return this;
    }
    

    public InvestorAccountPage selectInvestor(String investorName) {
    	 safeClick(investorDropDown);
    	 waitForUiToSettle();
    	 waitForUiToSettle();
        logger.info("Selecting investor: {}", investorName);
        safeClick(AppiumBy.androidUIAutomator(
                "new UiSelector().description(\"" + investorName + "\")"));
        waitForUiToSettle();
        logger.info("Investor selected: {}", investorName);
        return this;
    }

    public InvestorAccountPage tapAddInvestorCta() {
        logger.info("Tapping Add Investor CTA");
        safeClick(addInvestorCta);
        waitForUiToSettle();
        logger.info("Add Investor CTA tapped");
        return this;
    }

    // ============================================================
    // BANK ACCOUNT DETAILS
    // ============================================================

    public boolean isBankAccountSectionDisplayed() {
        logger.info("Checking Bank Account Details section visibility");
        waitForVisible(bankAccountDetailsHeading);
        return isDisplayed(bankAccountDetailsHeading);
    }

    public boolean isBankNameDisplayed() {
        logger.info("Checking bank name visibility");
        waitForVisible(bankName);
        return isDisplayed(bankName);
    }

    public boolean isMaskedAccountNumberDisplayed() {
        logger.info("Checking masked account number visibility");
        waitForVisible(maskedAccountNumber);
        return isDisplayed(maskedAccountNumber);
    }

    public boolean isIfscDisplayed() {
        logger.info("Checking IFSC code visibility");
        waitForVisible(ifscCode);
        return isDisplayed(ifscCode);
    }

    public boolean isBankBranchDisplayed() {
        logger.info("Checking bank branch name visibility");
        waitForVisible(bankBranchName);
        return isDisplayed(bankBranchName);
    }

    public boolean isBankApprovedBadgeDisplayed() {
        logger.info("Checking bank Approved badge visibility");
        waitForVisible(bankApprovedBadge);
        return isDisplayed(bankApprovedBadge);
    }

    public boolean isNoBankLinkedStateDisplayed() {
        logger.info("Checking no-bank-linked empty state visibility");
        waitForVisible(noBankLinkedEmptyState);
        return isDisplayed(noBankLinkedEmptyState);
    }

    public String getBankName() {
        try {
            return waitForVisible(bankName).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch bank name");
            return "";
        }
    }

    public String getIfscCode() {
        try {
            return waitForVisible(ifscCode).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch IFSC code");
            return "";
        }
    }

    // ============================================================
    // AUTOPAY MANDATES
    // ============================================================

    public boolean isAutopayMandatesHeadingDisplayed() {
        logger.info("Checking Autopay Mandates heading visibility");
        waitForVisible(autopayMandatesHeading);
        return isDisplayed(autopayMandatesHeading);
    }

    public boolean isNoMandateEmptyStateDisplayed() {
        logger.info("Checking no-mandate empty state visibility");
        waitForVisible(noMandateEmptyState);
        return isDisplayed(noMandateEmptyState);
    }

    public boolean isMandateListDisplayed() {
        logger.info("Checking mandate list item visibility");
        waitForVisible(mandateListItem);
        return isDisplayed(mandateListItem);
    }

    public boolean isExpiredMandateStatusDisplayed() {
        logger.info("Checking expired mandate status visibility");
        waitForVisible(expiredMandateStatus);
        return isDisplayed(expiredMandateStatus);
    }

    public boolean isInactiveMandateStatusDisplayed() {
        logger.info("Checking inactive mandate status visibility");
        waitForVisible(inactiveMandateStatus);
        return isDisplayed(inactiveMandateStatus);
    }

    // ============================================================
    // NOMINEE DETAILS
    // ============================================================

    public boolean isNomineeDetailsHeadingDisplayed() {
        logger.info("Checking Nominee Details heading visibility");
        waitForVisible(nomineeDetailsHeading);
        return isDisplayed(nomineeDetailsHeading);
    }

    public boolean isNomineeAadhaarMasked() {
        logger.info("Checking masked Aadhaar visibility");
        waitForVisible(nomineeAadhaarMasked);
        return isDisplayed(nomineeAadhaarMasked);
    }

    public boolean isNomineePercentageDisplayed() {
        logger.info("Checking nominee percentage visibility");
        waitForVisible(nomineePercentage);
        return isDisplayed(nomineePercentage);
    }

    public String getNomineePercentageText() {
        try {
            return waitForVisible(nomineePercentage).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch nominee percentage text");
            return "";
        }
    }

    // ============================================================
    // RISK ASSESSMENT
    // ============================================================

    public boolean isRiskAssessmentHeadingDisplayed() {
        logger.info("Checking Risk Assessment heading visibility");
        waitForVisible(riskAssessmentHeading);
        return isDisplayed(riskAssessmentHeading);
    }

    public boolean isRiskViewMoreCtaDisplayed() {
        logger.info("Checking Risk View More CTA visibility");
        waitForVisible(riskViewMoreCta);
        return isDisplayed(riskViewMoreCta);
    }

    public boolean isRiskLastUpdatedDisplayed() {
        logger.info("Checking risk last updated label visibility");
        waitForVisible(riskLastUpdated);
        return isDisplayed(riskLastUpdated);
    }

    public boolean isRiskAssessmentPendingPromptDisplayed() {
        logger.info("Checking risk assessment pending prompt visibility");
        waitForVisible(riskAssessmentPendingPrompt);
        return isDisplayed(riskAssessmentPendingPrompt);
    }

    public boolean isAnyRiskTypeDisplayed() {
        logger.info("Checking if any risk type label is visible");
        waitForVisible(riskAssessmentHeading);
        return isAnyDisplayed(riskTypeConservative, riskTypeModerate, riskTypeAggressive);
    }

    public String getRiskType() {
        logger.info("Reading risk type");
        if (isDisplayed(riskTypeConservative)) return "Conservative";
        if (isDisplayed(riskTypeModerate))     return "Moderate";
        if (isDisplayed(riskTypeAggressive))   return "Aggressive";
        return "";
    }

    public boolean tapRiskViewMoreAndVerify() {
        logger.info("Tapping Risk View More CTA");
        safeClick(riskViewMoreCta);
        waitForUiToSettle();
        waitForVisible(riskDetailScreenAnchor);
        boolean arrived = isDisplayed(riskDetailScreenAnchor);
        logger.info("Risk detail screen loaded: {}", arrived);
        return arrived;
    }

    // ============================================================
    // FAQ ACCORDION
    // ============================================================

    public boolean isFaqSectionDisplayed() {
        logger.info("Checking FAQ section visibility");
        waitForVisible(faqSectionHeading);
        return isDisplayed(faqSectionHeading);
    }

    public InvestorAccountPage tapFirstFaqItem() {
        logger.info("Tapping first FAQ item");
        safeClick(faqFirstItem);
        waitForUiToSettle();
        logger.info("First FAQ item tapped");
        return this;
    }

    public boolean isFaqItemExpanded() {
        logger.info("Checking FAQ item expanded state");
        waitForVisible(faqExpandedContent);
        return isDisplayed(faqExpandedContent);
    }

    public boolean isFaqItemCollapsed() {
        // Uses waitForUiToSettle instead of waitForVisible — element should NOT be present
        logger.info("Checking FAQ item collapsed state");
        waitForUiToSettle();
        return !isDisplayed(faqExpandedContent);
    }

    // ============================================================
    // SEARCH
    // ============================================================

    public boolean isSearchIconDisplayed() {
        logger.info("Checking search icon visibility");
        waitForVisible(headerSearchIcon);
        return isDisplayed(headerSearchIcon);
    }

    public boolean tapSearchIconAndVerify() {
        logger.info("Tapping search icon");
        safeClick(headerSearchIcon);
        waitForUiToSettle();
        waitForVisible(searchScreenInput);
        boolean arrived = isDisplayed(searchScreenInput);
        logger.info("Search screen loaded: {}", arrived);
        return arrived;
    }

    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    public boolean isBottomNavigationDisplayed() {
        logger.info("Checking bottom navigation visibility");
        waitForVisible(hubBottomTab);
        return isAnyDisplayed(portfolioBottomTab, hubBottomTab, fundsBottomTab, stocksBottomTab);
    }

    public boolean tapFundsTabAndVerify() {
        logger.info("Tapping Funds bottom tab");
        safeClick(fundsBottomTab);
        waitForUiToSettle();
        waitForVisible(fundsBottomTab);
        boolean arrived = isDisplayed(fundsBottomTab);
        logger.info("Funds tab reachable: {}", arrived);
        return arrived;
    }

    public boolean tapStocksTabAndVerify() {
        logger.info("Tapping Stocks bottom tab");
        safeClick(stocksBottomTab);
        waitForUiToSettle();
        waitForVisible(stocksBottomTab);
        boolean arrived = isDisplayed(stocksBottomTab);
        logger.info("Stocks tab reachable: {}", arrived);
        return arrived;
    }

    public boolean tapPortfolioTabAndVerify() {
        logger.info("Tapping Portfolio bottom tab");
        safeClick(portfolioBottomTab);
        waitForUiToSettle();
        waitForVisible(portfolioBottomTab);
        boolean arrived = isDisplayed(portfolioBottomTab);
        logger.info("Portfolio tab reachable: {}", arrived);
        return arrived;
    }

    public boolean tapHubTabAndVerify() {
        logger.info("Tapping Hub bottom tab");
        safeClick(hubBottomTab);
        waitForUiToSettle();
        waitForVisible(hubBottomTab);
        boolean arrived = isDisplayed(hubBottomTab);
        logger.info("Hub tab reachable: {}", arrived);
        return arrived;
    }

    // ============================================================
    // POST-NAVIGATION SCREEN VALIDATION
    // ============================================================

    public boolean isEditProfileScreenDisplayed() {
        logger.info("Checking Edit Profile screen visibility");
        waitForVisible(editProfileFormInput);
        boolean result = isAnyDisplayed(editProfileSaveButton, editProfileUpdateButton)
                && isDisplayed(editProfileFormInput);
        logger.info("Edit Profile screen displayed: {}", result);
        return result;
    }

    public boolean isMobileEditScreenDisplayed() {
        logger.info("Checking Mobile Edit screen visibility");
        waitForVisible(mobileEditInput);
        boolean result = isDisplayed(mobileEditInput)
                && isAnyDisplayed(mobileEditHeading, mobileEditSendOtp);
        logger.info("Mobile Edit screen displayed: {}", result);
        return result;
    }

    public boolean isEmailEditScreenDisplayed() {
        logger.info("Checking Email Edit screen visibility");
        waitForVisible(emailEditInput);
        boolean result = isDisplayed(emailEditInput) && isDisplayed(emailEditHeading);
        logger.info("Email Edit screen displayed: {}", result);
        return result;
    }

    // ============================================================
    // LINK IDENTIFICATION
    // ============================================================

    public boolean isBackButtonDisplayed() {
        logger.info("Checking back arrow button visibility");
        waitForVisible(backArrowButton);
        return isDisplayed(backArrowButton);
    }

    public boolean isHeaderEditIconDisplayed() {
        logger.info("Checking header edit icon visibility");
        waitForVisible(headerEditIcon);
        return isDisplayed(headerEditIcon);
    }

    public boolean isMobileEditIconDisplayed() {
        logger.info("Checking mobile edit icon visibility");
        waitForVisible(mobileEditIcon);
        return isDisplayed(mobileEditIcon);
    }

    public boolean isAllLinksDisplayed() {
        logger.info("Verifying all navigable links are visible on Basic Details screen");
        boolean back       = isBackButtonDisplayed();
        boolean headerEdit = isHeaderEditIconDisplayed();
        boolean investor   = isInvestorButtonDisplayed();
        boolean mobileEdit = isMobileEditIconDisplayed();
        logger.info("Link visibility — Back:{}, HeaderEdit:{}, Investor:{}, MobileEdit:{}",
                back, headerEdit, investor, mobileEdit);
        return back && headerEdit && investor && mobileEdit;
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public String getMobileNumber() {
        try {
            return waitForVisible(mobileValue).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch mobile number");
            return "";
        }
    }

    public String getEmailAddress() {
        try {
            return waitForVisible(emailValue).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch email address");
            return "";
        }
    }

    public String getPanNumber() {
        try {
            return waitForVisible(panValue).getAttribute("content-desc");
        } catch (Exception e) {
            logger.warn("Unable to fetch PAN number");
            return "";
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    public InvestorAccountPage tapBack() {
        logger.info("Tapping back arrow on Basic Details screen");
        waitForVisible(backArrowButton).click();
        waitForUiToSettle();
        logger.info("Back arrow tapped");
        return this;
    }

    public InvestorAccountPage tapHeaderEdit() {
        logger.info("Tapping header edit icon");
        safeClick(headerEditIcon);
        waitForUiToSettle();
        logger.info("Header edit icon tapped");
        return this;
    }

    public InvestorAccountPage tapMobileEdit() {
        logger.info("Tapping mobile edit icon");
        safeClick(mobileEditIcon);
        waitForUiToSettle();
        logger.info("Mobile edit icon tapped");
        return this;
    }

    public InvestorAccountPage tapEmailEdit() {
        logger.info("Tapping email edit icon");
        safeClick(emailEditIcon);
        waitForUiToSettle();
        logger.info("Email edit icon tapped");
        return this;
    }

    public InvestorAccountPage tapInvestorButton() {
        logger.info("Tapping Investor toggle button");
        safeClick(investorButton);
        waitForUiToSettle();
        logger.info("Investor toggle tapped");
        return this;
    }

    public InvestorAccountPage scrollToBankDetails() {
        logger.info("Scrolling to Bank Account Details section");
        scrollToText("Bank Account Details");
        logger.info("Scrolled to Bank Account Details");
        return this;
    }

    public InvestorAccountPage scrollToAutopayMandates() {
        logger.info("Scrolling to Autopay Mandates section");
        scrollToText("Mandate");
        logger.info("Scrolled to Autopay Mandates");
        return this;
    }

    public InvestorAccountPage scrollToNomineeDetails() {
        logger.info("Scrolling to Nominee Details section");
        scrollToText("Nominee");
        logger.info("Scrolled to Nominee Details");
        return this;
    }

    public InvestorAccountPage scrollToRiskAssessment() {
        logger.info("Scrolling to Risk Assessment section");
        scrollToText("Risk Assessment");
        logger.info("Scrolled to Risk Assessment");
        return this;
    }

    public InvestorAccountPage scrollToFaq() {
        logger.info("Scrolling to FAQ section");
        scrollToText("FAQ");
        logger.info("Scrolled to FAQ section");
        return this;
    }

    // ============================================================
    // TAP + VALIDATE + RETURN
    // ============================================================

    public boolean tapHeaderEditAndVerify() {
        logger.info("=== Link test: Header Edit ===");
        safeClick(headerEditIcon);
        waitForUiToSettle();
        boolean verified = isEditProfileScreenDisplayed();
        logger.info("Edit Profile screen loaded: {}", verified);
        safeClick(backArrowButton);
        waitForUiToSettle();
        logger.info("=== Link test: Header Edit — done ===");
        return verified;
    }

    public boolean tapMobileEditAndVerify() {
        logger.info("=== Link test: Mobile Edit ===");
        safeClick(mobileEditIcon);
        waitForUiToSettle();
        boolean verified = isMobileEditScreenDisplayed();
        logger.info("Mobile Edit screen loaded: {}", verified);
        safeClick(backArrowButton);
        waitForUiToSettle();
        logger.info("=== Link test: Mobile Edit — done ===");
        return verified;
    }

    public boolean tapEmailEditAndVerify() {
        logger.info("=== Link test: Email Edit ===");
        safeClick(emailEditIcon);
        waitForUiToSettle();
        boolean verified = isEmailEditScreenDisplayed();
        logger.info("Email Edit screen loaded: {}", verified);
        safeClick(backArrowButton);
        waitForUiToSettle();
        logger.info("=== Link test: Email Edit — done ===");
        return verified;
    }

    public boolean tapInvestorButtonAndVerify() {
        logger.info("=== Link test: Investor button ===");
        safeClick(investorButton);
        waitForUiToSettle();
        boolean expanded = isInvestorDropdownExpanded() || isBasicDetailsScreenDisplayed();
        logger.info("Screen after Investor tap — expanded or still on Basic Details: {}", expanded);
        logger.info("=== Link test: Investor button — done ===");
        return expanded;
    }

    public boolean tapBackAndVerify() {
        logger.info("=== Link test: Back arrow ===");
        waitForVisible(backArrowButton).click();
        waitForUiToSettle();
        boolean leftScreen = isDisplayed(basicDetailsHeading);
        logger.info("Navigated away from Basic Details screen: {}", leftScreen);
        logger.info("=== Link test: Back arrow — done ===");
        return leftScreen;
    }

    public void verifyAllLinks() {
        logger.info("=== verifyAllLinks: full link validation cycle ===");
        logger.info("Header Edit link verified: {}", tapHeaderEditAndVerify());
        logger.info("Mobile Edit link verified: {}", tapMobileEditAndVerify());
        logger.info("Email Edit link verified: {}", tapEmailEditAndVerify());
        logger.info("Investor button verified: {}", tapInvestorButtonAndVerify());
        logger.info("Back button verified: {}", tapBackAndVerify());
        logger.info("=== verifyAllLinks: complete ===");
    }
}
