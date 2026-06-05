package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class CartingPage extends BasePage {

	// ============================================================
	// FUND SEARCH
	// ============================================================

	private final By searchEditText = AppiumBy
			.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

	private final By filterTabAll = AppiumBy.androidUIAutomator("new UiSelector().description(\"All\")");

	private final By filterTabFunds = AppiumBy.accessibilityId("Funds");

	private final By filterTabStocks = AppiumBy.accessibilityId("Stocks");

	// ============================================================
	// FUND DETAIL
	// ============================================================

	private final By investCta = AppiumBy.accessibilityId("Invest");

	// ============================================================
	// INVESTMENT TYPE DIALOG
	// ============================================================

	private final By oneTimeOption = AppiumBy.accessibilityId("One-time");

	private final By sipDialogOption = AppiumBy.accessibilityId("SIP");

	private final By investorLabel = AppiumBy.accessibilityId("Investor");

	private final By scrim = AppiumBy.accessibilityId("Scrim");

	// ============================================================
	// SIP FORM
	// ============================================================

	private final By sipAmountLabel = AppiumBy.accessibilityId("SIP amount");

	private final By sipDateRow = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"SIP Date\")");

	private final By investmentPeriodRow = AppiumBy.xpath("//android.widget.FrameLayout[@resource-id=\"android:id/content\"]/android.widget.FrameLayout/android.widget.FrameLayout/android.view.View/android.view.View/android.view.View/android.view.View/android.view.View/android.view.View/android.view.View[2]/android.view.View/android.widget.EditText[2]");

	private final By startSipCta = AppiumBy.accessibilityId("Start SIP");

	// ============================================================
	// ONE-TIME FORM
	// ============================================================

	private final By enterAmountLabel = AppiumBy.accessibilityId("Enter amount");

	private final By investNowFormCta = AppiumBy.accessibilityId("Invest Now");
	By Editcart = AppiumBy.accessibilityId("Edit");

	private final By soleEditText = AppiumBy
			.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

	// ============================================================
	// PAYMENT SCREEN
	// ============================================================

	private final By paymentMethodLabel = AppiumBy.accessibilityId("Choose your payment method:");

	private final By upiOption = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"UPI\")");

	private final By upiIdEditText = AppiumBy
			.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")" + ".hint(\"Enter UPI ID\")");

	private final By netbankingOption = AppiumBy.accessibilityId("Netbanking");

	private final By mandateOption = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Mandate\")");

	private final By payNowCta = AppiumBy.accessibilityId("Pay now");

	// ============================================================
	// CART REVIEW
	// ============================================================

	private final By cartTitle = AppiumBy.accessibilityId("Cart");

	private final By proceedToPayCta = AppiumBy.accessibilityId("Proceed to pay");

	// ============================================================
	// OTP AUTH
	// ============================================================

	private final By authorizeCta = AppiumBy.accessibilityId("Authorize");

	// ============================================================
	// LEAVE POPUP
	// ============================================================

	private final By leavePopupTitle = AppiumBy
			.androidUIAutomator("new UiSelector().descriptionContains(\"Leave Transaction\")");

	private final By leaveYesBtn = AppiumBy.accessibilityId("Yes");

	private final By leaveNoBtn = AppiumBy.accessibilityId("No");

	// ============================================================
	// NAVIGATION
	// ============================================================

	// "Exit" (top-right) is the cancel-transaction button on both the OTP screen
	// and the cart review screen. Tapping it shows the Leave Transaction popup.
	// The < back arrow navigates within the RN stack without showing the popup.
	private final By exitButton = AppiumBy.accessibilityId("Exit");
	private final By backArrowButton = AppiumBy
			.accessibilityId("Go back");

	// ============================================================
	// FINAL SCREENS
	// ============================================================

	private final By backToHomepageCta = AppiumBy.accessibilityId("Back to homepage");

	private final By cartSuccessIndicator = AppiumBy
			.androidUIAutomator("new UiSelector().descriptionContains(\"Cart Successful\")");

	private final By retryFailedTransactionsCta = AppiumBy
			.androidUIAutomator("new UiSelector().descriptionContains(\"Retry failed\")");

	// ============================================================

	public CartingPage(AndroidDriver driver) {
		super(driver);
	}

	// ============================================================
	// SCREEN DETECTION
	// ============================================================

	public boolean isFundSearchScreenDisplayed() {

		return isAnyDisplayed(filterTabAll, filterTabFunds) && isDisplayed(searchEditText);
	}

	public boolean isFundDetailScreenDisplayed() {

		return isDisplayed(investCta) && !isDisplayed(cartTitle);
	}

	public boolean isInvestmentTypeDialogDisplayed() {

		return isAnyDisplayed(oneTimeOption, sipDialogOption) && isDisplayed(investorLabel);
	}

	public boolean isSipOrderFormDisplayed() {

		return isDisplayed(sipAmountLabel) && isDisplayed(startSipCta);
	}

	public boolean isOneTimeOrderFormDisplayed() {

		return isDisplayed(enterAmountLabel) && isDisplayed(investNowFormCta);
	}

	public boolean isPaymentMethodScreenDisplayed() {

		return isDisplayed(paymentMethodLabel) && isAnyDisplayed(upiOption, netbankingOption, mandateOption);
	}

	public boolean isCartReviewScreenDisplayed() {
        waitForVisible(cartTitle);
		return isDisplayed(cartTitle) || isAnyDisplayed(proceedToPayCta, authorizeCta);
	}

	public boolean isEditCartDisplayed() {
		return isDisplayed(Editcart);
	}

	public boolean isOtpAuthScreenDisplayed() {
       waitForVisible(authorizeCta);
		return isDisplayed(authorizeCta); // && !isDisplayed(cartTitle);
	}

	public boolean isLeaveTransactionPopupDisplayed() {

		return isAnyDisplayed(leavePopupTitle, leaveYesBtn) && isDisplayed(leaveNoBtn);
	}

	public boolean isFinalScreenDisplayed() {

		return isAnyDisplayed(backToHomepageCta, cartSuccessIndicator, retryFailedTransactionsCta);
	}

	public boolean waitUntilTrue(
	        java.util.function.BooleanSupplier condition,
	        int seconds) {

	    long endTime =
	            System.currentTimeMillis()
	                    + (seconds * 1000L);

	    while (System.currentTimeMillis() < endTime) {

	        try {

	            if (condition.getAsBoolean()) {

	                return true;
	            }

	        } catch (Exception ignored) {
	        }

	        waitForUiToSettle(500);
	    }

	    return false;
	}
	// ============================================================
	// FUND SEARCH
	// ============================================================

	public CartingPage typeSearchQuery(String query) {

		safeClick(searchEditText);

		safeSendKeys(searchEditText, query);

		logger.info("Typed search query: {}", query);

		return this;
	}

	public CartingPage selectFilterTab(String tabLabel) {

		safeClick(AppiumBy.accessibilityId(tabLabel));

		logger.info("Selected filter tab: {}", tabLabel);

		return this;
	}

	public CartingPage selectFundFromResults(String partialName) {

		By fundRow =AppiumBy.androidUIAutomator("new UiSelector().description(\"Mutual Fund\").instance(0)");

		safeClick(fundRow);
		
		logger.info("Selected fund: {}", partialName);

		return this;
	}

	// ============================================================
	// FUND DETAIL
	// ============================================================

	public CartingPage tapInvestCta() {

		safeClick(investCta);

		logger.info("Tapped Invest CTA");

		return this;
	}

	// ============================================================
	// INVESTMENT TYPE
	// ============================================================

	public CartingPage selectInvestmentType(String type) {

		waitForVisible(investorLabel);

		By target = "SIP".equalsIgnoreCase(type) ? sipDialogOption : oneTimeOption;

		safeClick(target);

		logger.info("Selected investment type: {}", type);

		return this;
	}

	public CartingPage dismissInvestmentDialog() {

		safeClick(scrim);

		logger.info("Dismissed investment dialog");

		return this;
	}

	// ============================================================
	// SIP FORM
	// ============================================================

	public CartingPage enterSipAmount(String amount) {

		waitForVisible(sipAmountLabel);

		safeSendKeys(soleEditText, amount);

		logger.info("Entered SIP amount");

		return this;
	}

	public CartingPage tapStartSip() {

		safeClick(startSipCta);

		logger.info("Tapped Start SIP");

		return this;
	}

	// ============================================================
	// ONE-TIME FORM
	// ============================================================

	public CartingPage enterOneTimeAmount(String amount) {

		waitForVisible(enterAmountLabel);

		safeSendKeys(soleEditText, amount);

		logger.info("Entered one-time amount");

		return this;
	}

	public CartingPage tapInvestNowOnForm() {
		waitForVisible(investNowFormCta);
        // waitForClickable(investNowFormCta);
		safeClick(investNowFormCta);

		logger.info("Tapped Invest Now");

		return this;
	}

	// ============================================================
	// PAYMENT
	// ============================================================

	public CartingPage selectUpiPayment() {

		waitForVisible(paymentMethodLabel);
		logger.info("Selecting UPI payment");
		safeClick(upiOption);
		logger.info("UPI payment selected");

		return this;
	}

	public CartingPage enterUpiId(String upiId) {

		logger.info("Entering UPI ID");
		safeSendKeys(upiIdEditText, upiId);
		logger.info("UPI ID entered");

		return this;
	}

	public CartingPage selectNetbanking() {

		logger.info("Selecting Netbanking");
		safeClick(netbankingOption);
		logger.info("Netbanking selected");

		return this;
	}

	public CartingPage selectMandate() {

		logger.info("Selecting Mandate");
		safeClick(mandateOption);
		logger.info("Mandate selected");

		return this;
	}

	public CartingPage tapPayNow() {

		logger.info("Tapping Pay Now");
		safeClick(payNowCta);
		logger.info("Pay Now tapped");

		return this;
	}

	// ============================================================
	// CART REVIEW
	// ============================================================

	public CartingPage tapProceedToPay() {

		logger.info("Tapping Proceed to Pay");
		waitForClickable(proceedToPayCta);
		safeClick(proceedToPayCta);
		logger.info("Proceed to Pay tapped");

		return this;
	}

	// ============================================================
	// OTP AUTH
	// ============================================================

	public CartingPage tapAuthorize() {

		logger.info("Tapping Authorize");
		safeClick(authorizeCta);
		logger.info("Authorize tapped");

		return this;
	}
	// ============================================================
	// BOTTOM NAVIGATION
	// ============================================================

	private final By fundsBottomTab = AppiumBy.accessibilityId("Funds");

	private final By topRightIcon = AppiumBy
			.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(1)");

	// ============================================================
	// PAYMENT SCREEN HELPERS
	// ============================================================

	public boolean isUpiOptionDisplayed() {
        waitForVisible(upiOption);
		return isDisplayed(upiOption);
	}
	public boolean isUpiOptionDisplay() {
        
		return isDisplayed(upiOption);
	}

	public boolean isNetbankingOptionDisplayed() {
		waitForVisible(netbankingOption);
		return isDisplayed(netbankingOption);
	}

	public boolean isMandateOptionDisplayed() {

		return isDisplayed(mandateOption);
	}

	// ============================================================
	// CART SCREEN HELPERS
	// ============================================================

	public boolean isProceedToPayDisplayed() {

		return isDisplayed(proceedToPayCta);
	}

	public boolean isAuthorizeButtonEnabled() {

	    WebElement button =
	            waitForVisible(authorizeCta);

	    String clickable =
	            button.getAttribute(
	                    "clickable");

	    logger.info(
	            "Authorize clickable={}",
	            clickable);

	    return Boolean.parseBoolean(
	            clickable);
	}

	// ============================================================
	// LEAVE POPUP HELPERS
	// ============================================================

	public String getLeavePopupTitleText() {

		try {

			return waitForVisible(leavePopupTitle).getAttribute("content-desc");

		} catch (Exception e) {

			logger.warn("Unable to fetch Leave popup title");

			return "";
		}
	}

	// ============================================================
	// FUND TAB NAVIGATION
	// ============================================================

	public CartingPage openFundsTab() {

		logger.info("Opening Funds tab");

		safeClick(fundsBottomTab);

		waitForUiToSettle();

		return this;
	}

	public void tapTopRightIcon() {

		safeClick(topRightIcon);

		waitForUiToSettle();

		logger.info("Tapped top right icon");
	}

	// ============================================================
	// PAYMENT SCREEN VALIDATION
	// ============================================================

	public boolean verifyPaymentScreenLoaded() {
		waitForVisible(paymentMethodLabel);
		return isDisplayed(paymentMethodLabel);
	}

	// ============================================================
	// OTP VALIDATION
	// ============================================================

	public boolean verifyOtpAuthScreenLoaded() {
		waitForVisible(authorizeCta);
		return isDisplayed(authorizeCta);
	}

	// ============================================================
	// INVEST CTA VALIDATION
	// ============================================================

	public boolean isInvestCtaDisplayed() {
       waitForVisible(investCta);
		return isDisplayed(investCta);
	}

	// ============================================================
	// LEAVE POPUP
	// ============================================================

	public CartingPage tapLeaveYes() {

		logger.info("Tapping Leave popup — Yes");
		safeClick(leaveYesBtn);
		logger.info("Leave confirmed — exiting transaction");

		return this;
	}

	public CartingPage tapLeaveNo() {

		logger.info("Tapping Leave popup — No");
		safeClick(leaveNoBtn);
		logger.info("Leave dismissed — staying on screen");

		return this;
	}

	// ============================================================
	// FINAL SCREEN
	// ============================================================

	public CartingPage tapBackToHomepage() {

		logger.info("Tapping Back to Homepage");
		safeClick(backToHomepageCta);
		logger.info("Back to Homepage tapped");

		return this;
	}

	// ============================================================
	// NAVIGATION
	// ============================================================

	public void tapBack() {

		logger.info("Tapping back arrow");
		waitForVisible(backArrowButton).click();
		waitForUiToSettle();
		logger.info("Back arrow tapped");
	}

	public void tapExit() {

		logger.info("Tapping Exit button");
		waitForVisible(exitButton).click();
		waitForUiToSettle();
		logger.info("Exit button tapped");
	}
	// ============================================================
	// RECOVERY
	// ============================================================

	public void recoverToFundSearch() {

		final int maxAttempts = 8;

		for (int i = 0; i < maxAttempts; i++) {

			if (isFundSearchScreenDisplayed()) {

				logger.info("Recovered to Fund Search screen");

				return;
			}

			if (isLeaveTransactionPopupDisplayed()) {

				tapLeaveYes();

				continue;
			}

			tapBack();
		}

		throw new AssertionError("Failed to recover to Fund Search");
	}

	public void recoverToCartScreen() {

		final int maxAttempts = 2;

		for (int i = 0; i < maxAttempts; i++) {

			if (isCartReviewScreenDisplayed()) {

				logger.info("Recovered to Cart screen");

				return;
			}

			if (isLeaveTransactionPopupDisplayed()) {

				tapLeaveNo();

				continue;
			}

			tapBack();
		}

		throw new AssertionError("Failed to recover to Cart screen");
	}

	// ============================================================
	// VERIFICATION HELPERS
	// ============================================================

	public boolean verifyCartReviewScreenLoaded() {

		return isDisplayed(cartTitle);
	}

	public boolean verifyFundDetailLoaded() {
		waitForClickable(investCta);
		return isDisplayed(investCta);
	}

	public boolean verifySipFormLoaded() {

		return isDisplayed(sipAmountLabel);
	}

	public boolean verifyOneTimeFormLoaded() {

		return isDisplayed(enterAmountLabel);
	}

	public boolean verifySipDateRowDisplayed() {

		return isDisplayed(sipDateRow);
	}

	public boolean verifyInvestmentPeriodRowDisplayed() {

		return isDisplayed(investmentPeriodRow);
	}

}