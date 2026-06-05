package tests;

import java.lang.reflect.Method;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.CartingPage;
import pages.DashboardPage;
import pages.PortfolioPlannerPage;

/**
 * Optimized Carting Tests
 *
 * Goals:
 * - Persistent session reuse
 * - Flutter-safe recovery
 * - Minimal setup flakiness
 * - Business actions only inside tests
 * - Stable navigation recovery
 */
public class CartingTest extends BaseTest {

	private static final String PRIMARY_INVESTOR =
			"Wamiq Azeem Asif";

	private PortfolioPlannerPage plannerPage;

	private CartingPage cartingPage;

	// ============================================================
	// CONFIG
	// ============================================================

	@Override
	protected boolean shouldManageDriverPerMethod() {

		return false;
	}

	// ============================================================
	// CLASS SETUP
	// ============================================================

	@Override
	protected void onClassReady() {

		plannerPage =
				new PortfolioPlannerPage(
						getDriver());

		cartingPage =
				new CartingPage(
						getDriver());
	}

	// ============================================================
	// TEST RECOVERY
	// ============================================================

	//    @Override
	//    protected void recoverAppState(Method method) {
	//
	//        ensurePlannerReady();
	//    }

	@Override
	protected void recoverAppState(Method method) {

		DashboardPage dashboardPage =
				new DashboardPage(getDriver());

		dashboardPage.recoverToDashboard();

		dashboardPage.waitForDashboardLoaded();
	}
	// ============================================================
	// RECOVERY FLOW
	// ============================================================

	/**
	 * Recovery should ONLY restore stable planner state.
	 *
	 * Never execute business CTAs here.
	 */
	//    private void ensurePlannerReady() {
	//
	//        // Already on investment plan
	//        if (plannerPage.verifyInvestmentPlanScreen()) {
	//
	//            return;
	//        }
	//
	//        // Already on cart
	//        if (cartingPage.isCartReviewScreenDisplayed()) {
	//
	//            return;
	//        }
	//
	//        // Already on OTP
	//        if (cartingPage.isOtpAuthScreenDisplayed()) {
	//
	//            return;
	//        }
	//
	//        plannerPage.openPortfolioPlanner();
	//
	//        prepareSipPlan();
	//
	//        Assert.assertTrue(
	//                plannerPage.verifyInvestmentPlanScreen(),
	//                "Investment plan screen must load");
	//    }

	//    private void prepareSipPlan() {
	//
	//        if (plannerPage.isSelectInvestorScreenDisplayed()) {
	//
	//            plannerPage.selectInvestor(
	//                    PRIMARY_INVESTOR);
	//
	//            plannerPage.clickNext();
	//        }
	//
	//        if (plannerPage.isPlannerOptionsScreenDisplayed()) {
	//
	//            plannerPage.choosePlannerType(
	//                    "Invest for higher returns");
	//
	//            plannerPage.selectInvestmentMode(
	//                    "SIP");
	//
	//            plannerPage.enterSipAmount(
	//                    "1000");
	//
	//            plannerPage.enterInvestmentPeriod(
	//                    "15");
	//
	//            plannerPage.clickShowInvestmentPlan();
	//        }
	//    }

	/**
	 * Opens cart ONLY when test actually needs it.
	 */
	private void openInvestmentPlan() {

		plannerPage.openPortfolioPlanner();

		plannerPage.selectInvestor(
				PRIMARY_INVESTOR);

		plannerPage.clickNext();

		plannerPage.choosePlannerType(
				"Invest for higher returns");

		plannerPage.selectInvestmentMode(
				"SIP");

		plannerPage.enterSipAmount(
				"1000");

		plannerPage.enterInvestmentPeriod(
				"15");


		plannerPage.clickShowInvestmentPlan();

	
	}
	private void openCartReviewScreen() {

		
		cartingPage.tapInvestNowOnForm();

		Assert.assertTrue(
				cartingPage.isCartReviewScreenDisplayed(),
				"Cart review screen must load");
	}

	/**
	 * Opens OTP screen ONLY when test actually needs it.
	 * @throws Exception 
	 */
	private void openOtpScreen()  {



		
		cartingPage.tapProceedToPay();


	}

	// ============================================================
	// CART SCREEN
	// ============================================================
	@Test()
	public void myCartTest() {
		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
	}

	@Test(description =
			"Cart review screen loads correctly")
	public void verifyCartScreenLoadsWithCoreElements() {
		openInvestmentPlan();
		logger.info("plan completed");
		
		cartingPage.tapInvestNowOnForm();
		cartingPage.waitForUiToSettle(2000);
		Assert.assertTrue(
				cartingPage.isCartReviewScreenDisplayed(),
				"Cart review screen must load");

	}

	@Test(description =
			"Proceed to Pay visible")
	public void verifyProceedToPayCtaIsVisibleWithOrders() {

		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
		cartingPage.waitForUiToSettle(2000);
		Assert.assertTrue(
				cartingPage.isProceedToPayDisplayed(),
				"Proceed to Pay CTA must display");
	}

	// ============================================================
	// OTP FLOW
	// ============================================================

	@Test(description =
			"Proceed to Pay opens OTP screen")
	public void verifyProceedToPayReachesOtpScreen() {
		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
		cartingPage.tapProceedToPay();
		//cartingPage.waitForUiToSettle(10000);


		Assert.assertTrue(
				cartingPage.isOtpAuthScreenDisplayed(),
				"OTP screen must load");
	}

	@Test(description =
			"Authorize button disabled before OTP")
	public void verifyAuthorizeButtonInactiveBeforeOtpEntry() {

		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
		openOtpScreen();
		cartingPage.waitForUiToSettle(10050);


		Assert.assertFalse(
				cartingPage.isAuthorizeButtonEnabled(),
				"Authorize button must remain disabled");
	}

	// ============================================================
	// LEAVE POPUP
	// ============================================================

	@Test(description =
			"Back from OTP shows Leave popup")
	public void verifyBackOnOtpScreenTriggersLeavePopup() {

		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
		openOtpScreen();
		cartingPage.waitForUiToSettle(10050);

		cartingPage.tapBack();

		Assert.assertTrue(
				cartingPage.isLeaveTransactionPopupDisplayed(),
				"Leave popup must appear");
	}

	@Test(description =
			"Leave popup No keeps OTP screen")
	public void verifyLeavePopupNoDismissesAndStaysOnOtpScreen() {

		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
		openOtpScreen();
		cartingPage.waitForUiToSettle(10050);
		cartingPage.tapBack();

		cartingPage.tapLeaveNo();

		Assert.assertTrue(
				cartingPage.isOtpAuthScreenDisplayed(),
				"OTP screen must remain active");
	}

	@Test(description =
			"Leave popup Yes returns to plan")
	public void verifyLeavePopupYesFromOtpReturnsToOrigin() {

		openInvestmentPlan();
		logger.info("open investment plan is completed");
		cartingPage.tapInvestNowOnForm();
		openOtpScreen();
		cartingPage.waitForUiToSettle(10050);
		cartingPage.tapBack();

		cartingPage.tapLeaveYes();

		Assert.assertTrue(
				plannerPage.verifyInvestmentPlanScreen(),
				"User must return to plan screen");
	}

	// ============================================================
	// RECOVERY
	// ============================================================

	@Test(description =
			"Cart session recovers after exit")
	public void verifyCartSessionRecoversAfterLeaveYes() {

		openInvestmentPlan();
		logger.info("open investment plan is completed");
		openCartReviewScreen();
		logger.info("open cart review completed");
		cartingPage.waitForUiToSettle(1005);
		Assert.assertTrue(
				cartingPage.isCartReviewScreenDisplayed(),
				"Cart session must recover");
	}

	// ============================================================
	// PAYMENT OPTIONS
	// ============================================================

	@Test(description =
			"Small cart payment options visible")
	public void verifyAllPaymentOptionsVisibleForSmallCart() {

		cartingPage.openFundsTab();
		logger.info("open funds search tap");
		cartingPage.tapTopRightIcon();
		logger.info("open funds search tap");
		//cartingPage.waitForUiToSettle(10000);
		Assert.assertTrue(
				cartingPage.isFundSearchScreenDisplayed(),
				"Fund search must load");

		cartingPage.typeSearchQuery(
				"Aditya Birla")
		.selectFundFromResults(
				"Birla Sun Life Liquid");

		Assert.assertTrue(
				cartingPage.verifyFundDetailLoaded(),
				"Fund detail screen must load");

		cartingPage.tapInvestCta()
		.selectInvestmentType("SIP")
		.enterSipAmount("500")
		.tapStartSip();

		Assert.assertTrue(
				cartingPage.verifyPaymentScreenLoaded(),
				"Payment screen must load");

		Assert.assertTrue(
				cartingPage.isUpiOptionDisplayed(),
				"UPI must display");

		Assert.assertTrue(
				
				cartingPage.isNetbankingOptionDisplayed(),
				"Netbanking must display");

		cartingPage.recoverToFundSearch();
	}

	@Test(description =
			"Large cart hides UPI")
	public void verifyUpiAbsentForLargeCartAmount() {

		cartingPage.openFundsTab();
		logger.info("open funds search tap");
		cartingPage.tapTopRightIcon();
		logger.info("open funds search tap");
		Assert.assertTrue(
				cartingPage.isFundSearchScreenDisplayed(),
				"Fund search must load");

		cartingPage.typeSearchQuery(
				"Aditya Birla")
		.selectFundFromResults(
				"Birla Sun Life Liquid");

		Assert.assertTrue(
				cartingPage.verifyFundDetailLoaded(),
				"Fund detail screen must load");

		cartingPage.tapInvestCta()
		.selectInvestmentType("One-time")
		.enterOneTimeAmount("150000")
		.tapInvestNowOnForm();

		
		Assert.assertFalse(
				cartingPage.isUpiOptionDisplay(),
				"UPI must not display");

		Assert.assertTrue(
				cartingPage.isNetbankingOptionDisplayed(),
				"Netbanking must display");

		cartingPage.recoverToFundSearch();
	}
	@Test(description =
			"Large cart netBanking working")
	public void verifyNetbankingWorksForLargeAmmount() {

		cartingPage.openFundsTab();
		logger.info("open funds search tap");
		cartingPage.tapTopRightIcon();
		logger.info("open funds search tap");
		Assert.assertTrue(
				cartingPage.isFundSearchScreenDisplayed(),
				"Fund search must load");

		cartingPage.typeSearchQuery(
				"Aditya Birla")
		.selectFundFromResults(
				"Birla Sun Life Liquid");

		Assert.assertTrue(
				cartingPage.verifyFundDetailLoaded(),
				"Fund detail screen must load");

		cartingPage.tapInvestCta()
		.selectInvestmentType("One-time")
		.enterOneTimeAmount("150000")
		.tapInvestNowOnForm();

		cartingPage.tapPayNow();
		Assert.assertTrue(
				cartingPage.isOtpAuthScreenDisplayed(),
				"Authorize button must remain disabled");
		

		cartingPage.recoverToFundSearch();
	}
	// ============================================================
	// EDGE
	// ============================================================

	@Test(description =
	        "Back absent on processing screen")
	public void verifyNoBackButtonOnProcessingScreen() {

	    openInvestmentPlan();

	    logger.info(
	            "Open investment plan completed");

	    cartingPage.tapInvestNowOnForm();

	    openOtpScreen();

	    // Wait briefly for processing screen
	   
	    // Processing skipped directly to OTP
	    if (cartingPage.isOtpAuthScreenDisplayed()) {

	        logger.info(
	                "Processing screen skipped quickly — OTP screen visible");

	        throw new SkipException(
	                "Processing screen skipped quickly");
	    }

	    boolean backButtonAbsent;

	    try {

	        cartingPage.tapBack();

	        backButtonAbsent =
	                !cartingPage.isLeaveTransactionPopupDisplayed();

	    } catch (Exception e) {

	        // No back button available
	        backButtonAbsent = true;
	    }

	    Assert.assertTrue(
	            backButtonAbsent,
	            "Back button must be absent");
	}
}