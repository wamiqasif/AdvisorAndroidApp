package tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.FundFinvuAA_Page;
import pages.OtpPage;

/**
 * Portfolio > Stocks & ETFs tab — Account Aggregator (Finvu AA) stock-import
 * test suite.
 *
 * Journey for every test: Portfolio -> Stocks & ETFs (recoverAppState) -> tap
 * the link(s) / run the AA import flow under test -> validate the resulting
 * screen -> return to Portfolio > Stocks & ETFs. The framework repeats this
 * journey before each test method via recoverAppState().
 *
 * Uses ONLY methods that exist on FundFinvuAA_Page (plus the shared OtpPage
 * for the OTP bottom sheet, since the AA mobile-verification step reuses that
 * same component elsewhere in the app).
 */
public class FinvuAATest extends BaseTest {

	private static final Logger logger =
			LoggerFactory.getLogger(FinvuAATest.class);

	// Test data supplied for the Account Aggregator import journey
	private static final String TEST_PAN    = "HIDIB9764C";
	private static final String TEST_MOBILE = "7838197642";

	// Best-effort upper bound on wrong-OTP submissions before giving up on
	// reaching the "attempts exceeded" state — the app's real limit is
	// unconfirmed (no XML captured for that screen yet). Tighten once known.
	private static final int MAX_WRONG_OTP_ATTEMPTS = 6;

	private FundFinvuAA_Page finvuAAPage;
	private OtpPage otpPage;


	@Override
	protected boolean shouldManageDriverPerMethod() {
		return false;
	}

	@Override
	protected void onClassReady() {
		finvuAAPage = new FundFinvuAA_Page(getDriver());
		otpPage = new OtpPage(getDriver());
	}

	@Override
	protected void recoverAppState(Method method) {
		finvuAAPage.navigateToStocksEtfsTab();
		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Portfolio > Stocks & ETFs screen should be visible before " + method.getName());
	}

	// ============================================================
	// SMOKE TESTS
	// ============================================================

	@Test(description = "Verify Portfolio > Stocks & ETFs screen loads properly")
	public void verifyStocksEtfsScreenLoads() {

		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Stocks & ETFs screen must display");

		logger.info("Stocks & ETFs smoke validation passed");
	}

	@Test(description = "Verify bottom navigation is visible on Stocks & ETFs tab")
	public void verifyBottomNavigationDisplayed() {

		Assert.assertTrue(finvuAAPage.isBottomNavigationDisplayed(),
				"Bottom navigation (Funds / Stocks / Portfolio / Hub) must be visible");

		logger.info("Bottom navigation validated");
	}

	@Test(description = "Verify the Account Aggregator import card (PAN + Mobile fields) is displayed")
	public void verifyAaImportCardDisplayed() {

		Assert.assertTrue(finvuAAPage.isAaImportCardDisplayed(),
				"Account Aggregator import card must display");

		//finvuAAPage.tapAaSelectInvestorButton();
		finvuAAPage.tapAddInvestorAndValidate();
		Assert.assertTrue(finvuAAPage.isPanFieldDisplayed(), "PAN field must display");
		Assert.assertTrue(finvuAAPage.isMobileFieldDisplayed(), "Mobile field must display");

		logger.info("AA import card validated");
	}

	// ============================================================
	// ASSET-CLASS TAB SWITCHING (in-page, no screen change)
	// ============================================================

	@Test(description = "Verify switching to Funds / SIFs tab and back to Stocks & ETFs")
	public void verifyFundsSifsTabSwitch() {
		Assert.assertTrue(finvuAAPage.tapFundsSifsTabAndValidate(),
				"Funds / SIFs tab should open and return to Stocks & ETFs");
	}

	@Test(description = "Verify switching to NPS tab and back to Stocks & ETFs")
	public void verifyNpsTabSwitch() {
		Assert.assertTrue(finvuAAPage.tapNpsTabAndValidate(),
				"NPS tab should open and return to Stocks & ETFs");
	}

	@Test(description = "Verify switching to Bonds & FDs tab and back to Stocks & ETFs")
	public void verifyBondsFdsTabSwitch() {
		Assert.assertTrue(finvuAAPage.tapBondsFdsTabAndValidate(),
				"Bonds & FDs tab should open and return to Stocks & ETFs");
	}

	// ============================================================
	// HEADER LINK INTERACTIONS — tap link, validate, return
	// ============================================================

	@Test(description = "Verify Investor Switcher link opens and returns to Stocks & ETFs")
	public void verifyInvestorSwitcherInteraction() {

		Assert.assertTrue(finvuAAPage.isInvestorSwitcherDisplayed(), "Investor Switcher must be visible");

		finvuAAPage.tapInvestorSwitcher();
		finvuAAPage.navigateToStocksEtfsTab();

		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should return to Stocks & ETFs after Investor Switcher interaction");

		logger.info("Investor Switcher interaction validated");
	}

	@Test(description = "Verify Update Stocks & ETFs Holdings link opens and returns to Stocks & ETFs")
	public void verifyUpdateHoldingsButtonInteraction() {

		Assert.assertTrue(finvuAAPage.isUpdateHoldingsButtonDisplayed(),
				"Update Stocks & ETFs Holdings button must be visible");

		finvuAAPage.tapUpdateHoldingsButton();
		finvuAAPage.navigateToStocksEtfsTab();

		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should return to Stocks & ETFs after Update Holdings interaction");

		logger.info("Update Stocks & ETFs Holdings interaction validated");
	}

	@Test(description = "Verify 'Recommended' badge tap keeps the Stocks & ETFs tab in place")
	public void verifyRecommendedBadgeInteraction() {

		finvuAAPage.tapRecommendedBadge();

		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"'Recommended' badge should not navigate away from Stocks & ETFs");

		logger.info("Recommended badge interaction validated");
	}

	@Test(description = "Verify the AA card's own 'Select Investor' button opens and returns to Stocks & ETFs")
	public void verifyAaSelectInvestorButtonInteraction() {

		finvuAAPage.tapAddInvestorAndValidate();
		// finvuAAPage.navigateToStocksEtfsTab();

		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should return to Stocks & ETFs after AA Select Investor interaction");

		logger.info("AA Select Investor button interaction validated");
	}

	@Test(description = "Verify Add Investor link opens and returns to Stocks & ETFs")
	public void verifyAddInvestorLinkOpensAndReturns() {

		Assert.assertTrue(finvuAAPage.tapAddInvestorAndValidate(),
				"Add Investor link should open a screen and return to Stocks & ETFs");

		logger.info("Add Investor interaction validated");
	}

	// ============================================================
	// CORE JOURNEY 1: Portfolio > Stocks & ETFs -> select/add investor ->
	// enter PAN + Mobile -> Update -> OTP screen -> wrong OTP -> attempts
	// exceeded
	// ============================================================

	@Test(description =
			"Journey: Stocks & ETFs -> select investor -> Add Investor -> enter PAN + Mobile -> "
					+ "Update -> OTP screen opens -> repeated wrong OTP -> attempts exceeded screen")
	public void verifyAccountAggregatorImportWithWrongOtpReachesAttemptsExceeded()  {

		logger.info("=== AA import journey (wrong OTP) — PAN:{}, Mobile:{} ===", TEST_PAN, TEST_MOBILE);

		// Portfolio > Stocks & ETFs -> select investor -> Add Investor
		Assert.assertTrue(finvuAAPage.tapAddInvestorAndValidate(),
				"Add Investor step of the journey should open and return to Stocks & ETFs");
		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should be back on Stocks & ETFs before entering PAN/Mobile");

		// Enter PAN + Mobile, submit
		finvuAAPage.enterPan(TEST_PAN);
		finvuAAPage.enterMobile(TEST_MOBILE);
		finvuAAPage.tapUpdateButton();

		finvuAAPage.waitForUiToSettle();

		Assert.assertTrue(finvuAAPage.waitForFinvuWebViewDisplayed(15),
				"OTP screen should open after submitting PAN + Mobile via Update");
		logger.info("OTP screen confirmed open after Update tap");

		// Repeated wrong OTP until attempts-exceeded (or best-effort bound reached)
		boolean attemptsExceeded = false;
		for (int attempt = 1; attempt <= MAX_WRONG_OTP_ATTEMPTS && !attemptsExceeded; attempt++) {
			logger.info("Submitting wrong OTP — attempt {}/{}", attempt, MAX_WRONG_OTP_ATTEMPTS);
			finvuAAPage.waitForUiToSettle();
			//otpPage.verifyOtp("000050");
			otpPage.enterOtp("012345");
			finvuAAPage.waitForUiToSettle();
			attemptsExceeded = otpPage.isOtpAttemptsExceededDisplayed();
		}

		Assert.assertTrue(attemptsExceeded,
				"Attempts-exceeded screen should be shown after " + MAX_WRONG_OTP_ATTEMPTS
				+ " wrong OTP submissions (raise MAX_WRONG_OTP_ATTEMPTS if the app allows more)");

		logger.info("Attempts-exceeded screen confirmed after repeated wrong OTP");

		// Recover to a known state for subsequent tests
		//finvuAAPage.navigateToStocksEtfsTab();
	}

	// ============================================================
	// CORE JOURNEY 2: enter PAN + Mobile -> Update -> OTP screen -> read the
	// real OTP from the console (manual step) -> verify -> tap every link ->
	// validate -> back to Portfolio > Stocks & ETFs
	// ============================================================

	@Test(description =
			"Journey: enter PAN + Mobile -> Update -> OTP screen opens -> OTP entered via console -> "
					+ "verify -> tap every link on Stocks & ETFs, validate each screen -> back to Portfolio")
	public void verifyAccountAggregatorImportWithConsoleOtpThenValidatesAllLinks() {

		logger.info("=== AA import journey (console OTP) — PAN:{}, Mobile:{} ===", TEST_PAN, TEST_MOBILE);

		finvuAAPage.enterPan(TEST_PAN);
		finvuAAPage.enterMobile(TEST_MOBILE);
		finvuAAPage.tapUpdateButton();

		Assert.assertTrue(otpPage.isOtpScreenDisplayed(),
				"OTP screen should open after submitting PAN + Mobile via Update");
		logger.info("OTP screen confirmed open after Update tap");

		String otp = readOtpFromConsole();
		otpPage.verifyOtp(otp);
		finvuAAPage.waitForUiToSettle();

		Assert.assertFalse(otpPage.isOtpScreenDisplayed(),
				"OTP screen should close after entering the correct OTP");
		logger.info("OTP verified — OTP screen closed");

		// Land back on a known screen regardless of what the OTP success
		// destination is (Finvu WebView, success dialog, etc.)
		finvuAAPage.navigateToStocksEtfsTab();
		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should be back on Portfolio > Stocks & ETFs after OTP verification");

		// Tap every link, validate the screen each opens
		List<FundFinvuAA_Page.LinkResult> results = finvuAAPage.tapAllLinksAndValidateScreens();
		Assert.assertFalse(results.isEmpty(), "No navigable links were discovered on Stocks & ETFs tab");

		List<String> failed = new ArrayList<>();
		for (FundFinvuAA_Page.LinkResult result : results) {
			logger.info("Link result: {}", result);
			if (!result.passed) {
				failed.add(result.name);
			}
		}
		Assert.assertTrue(failed.isEmpty(), "Links failed validation: " + failed);

		// Back to Portfolio > Stocks & ETFs
		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should end back on Portfolio > Stocks & ETFs after full link validation");

		logger.info("Console-OTP journey complete — all {} links validated", results.size());
	}

	// ============================================================
	// COMPREHENSIVE LINK VALIDATION — every navigable link on the tab
	// ============================================================

	@Test(description =
			"Journey: tap every link on Stocks & ETFs, validate the screen each opens, return")
	public void verifyAllLinksAndValidateScreens() {

		List<FundFinvuAA_Page.LinkResult> results = finvuAAPage.tapAllLinksAndValidateScreens();

		Assert.assertFalse(results.isEmpty(), "No navigable links were discovered on Stocks & ETFs tab");

		List<String> failed = new ArrayList<>();
		for (FundFinvuAA_Page.LinkResult result : results) {
			logger.info("Link result: {}", result);
			if (!result.passed) {
				failed.add(result.name);
			}
		}

		Assert.assertTrue(failed.isEmpty(), "Links failed validation: " + failed);

		Assert.assertTrue(finvuAAPage.isStocksEtfsScreenDisplayed(),
				"Should end back on Portfolio > Stocks & ETFs after full link validation");

		logger.info("All {} links validated on Stocks & ETFs tab", results.size());
	}

	@Test(description = "Full regression: verify overall link validation passes across Stocks & ETFs")
	public void verifyAllLinksOverallPass() {

		Assert.assertTrue(finvuAAPage.verifyAllLinksOpenScreens(),
				"One or more Stocks & ETFs links failed to open their expected screen");

		logger.info("Overall Stocks & ETFs link validation passed");
	}

	// ============================================================
	// PRIVATE HELPERS
	// ============================================================

	/**
	 * Blocks and prompts for the real OTP on the console — the mobile number
	 * entered for AA verification receives a live OTP that no local mock/API
	 * can supply, so a human enters it while the test is running.
	 */
	private String readOtpFromConsole() {
		System.out.println("=========================================================");
		System.out.println(" FinvuAATest: enter the OTP received for mobile " + TEST_MOBILE);
		System.out.println(" and press Enter to continue the test...");
		System.out.println("=========================================================");
		logger.info("Waiting for OTP to be entered on the console...");

		Scanner scanner = new Scanner(System.in);
		String otp = scanner.nextLine().trim();

		logger.info("OTP received from console (length {})", otp.length());
		return otp;
	}
}
