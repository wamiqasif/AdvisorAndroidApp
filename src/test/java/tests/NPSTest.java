package tests;

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.FundNPS_Page;

/**
 * Portfolio > NPS tab test suite.
 *
 * Journey for every test: Portfolio -> NPS (recoverAppState) -> tap the
 * link(s) / scheme card(s) under test -> validate the resulting screen ->
 * return to Portfolio > NPS. The framework repeats this journey before each
 * test method via recoverAppState().
 *
 * Uses ONLY methods that exist on FundNPS_Page.
 */
public class NPSTest extends BaseTest {

	private static final Logger logger =
			LoggerFactory.getLogger(NPSTest.class);

	private FundNPS_Page npsPage;

	@Override
	protected boolean shouldManageDriverPerMethod() {
		return false;
	}

	@Override
	protected void onClassReady() {
		npsPage = new FundNPS_Page(getDriver());
	}

	@Override
	protected void recoverAppState(Method method) {
		npsPage.navigateToNpsTab();
		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Portfolio > NPS screen should be visible before " + method.getName());
	}

	// ============================================================
	// SMOKE TESTS
	// ============================================================

	@Test(description = "Verify Portfolio > NPS screen loads properly")
	public void verifyNpsScreenLoads() {

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"NPS screen must display");

		logger.info("NPS smoke validation passed");
	}

	@Test(description = "Verify bottom navigation is visible on NPS tab")
	public void verifyBottomNavigationDisplayed() {

		Assert.assertTrue(npsPage.isBottomNavigationDisplayed(),
				"Bottom navigation (Funds / Stocks / Portfolio / Hub) must be visible");

		logger.info("Bottom navigation validated");
	}

	@Test(description = "Verify the NPS scheme list is displayed")
	public void verifySchemeListDisplayed() {

		Assert.assertTrue(npsPage.isSchemeListDisplayed(),
				"NPS scheme list must display at least one scheme card");

		logger.info("NPS scheme list validated");
	}

	// ============================================================
	// ASSET-CLASS TAB SWITCHING (in-page, no screen change)
	// ============================================================

	@Test(description = "Verify switching to Funds / SIFs tab and back to NPS")
	public void verifyFundsSifsTabSwitch() {
		Assert.assertTrue(npsPage.tapFundsSifsTabAndValidate(),
				"Funds / SIFs tab should open and return to NPS");
	}

	@Test(description = "Verify switching to Stocks & ETFs tab and back to NPS")
	public void verifyStocksEtfsTabSwitch() {
		Assert.assertTrue(npsPage.tapStocksEtfsTabAndValidate(),
				"Stocks & ETFs tab should open and return to NPS");
	}

	@Test(description = "Verify switching to Bonds & FDs tab and back to NPS")
	public void verifyBondsFdsTabSwitch() {
		Assert.assertTrue(npsPage.tapBondsFdsTabAndValidate(),
				"Bonds & FDs tab should open and return to NPS");
	}

	@Test(description = "Verify switching to SGB tab and back to NPS")
	public void verifySgbTabSwitch() {
		Assert.assertTrue(npsPage.tapSgbTabAndValidate(),
				"SGB tab should open and return to NPS");
	}

	// ============================================================
	// HEADER / SUMMARY BAR LINK INTERACTIONS
	// ============================================================

	@Test(description = "Verify Investor Switcher link opens and returns to NPS")
	public void verifyInvestorSwitcherInteraction() {

		Assert.assertTrue(npsPage.isInvestorSwitcherDisplayed(), "Investor Switcher must be visible");

		npsPage.tapInvestorSwitcher();
		npsPage.navigateToNpsTab();

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should return to NPS after Investor Switcher interaction");

		logger.info("Investor Switcher interaction validated");
	}

	@Test(description = "Verify Update NPS Holdings link opens and returns to NPS")
	public void verifyUpdateHoldingsButtonInteraction() {

		Assert.assertTrue(npsPage.isUpdateHoldingsButtonDisplayed(),
				"Update NPS Holdings button must be visible");

		npsPage.tapUpdateHoldingsButton();
		npsPage.navigateToNpsTab();

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should return to NPS after Update Holdings interaction");

		logger.info("Update NPS Holdings interaction validated");
	}

	@Test(description = "Verify 'All Time Returns' toggle keeps the NPS tab in place")
	public void verifyAllTimeReturnsToggleInteraction() {

		npsPage.tapAllTimeReturnsToggle();

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"'All Time Returns' toggle should not navigate away from NPS");

		logger.info("All Time Returns toggle interaction validated");
	}

	@Test(description = "Verify sort-order toggle keeps the NPS tab in place")
	public void verifySortOrderToggleInteraction() {

		npsPage.tapSortOrderToggle();

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Sort-order toggle should not navigate away from NPS");

		logger.info("Sort-order toggle interaction validated");
	}

	@Test(description = "Verify 'Sort By' button opens its options and returns to NPS")
	public void verifySortByButtonInteraction() {

		npsPage.tapSortByButton();
		npsPage.navigateToNpsTab();

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should return to NPS after Sort By interaction");

		logger.info("Sort By button interaction validated");
	}

	// ============================================================
	// SCHEME CARDS
	// ============================================================

	@Test(description = "Verify tapping every NPS scheme card opens its detail dialog and returns to NPS")
	public void verifyAllSchemeCardsAndValidate() {

		List<FundNPS_Page.LinkResult> results = npsPage.tapAllSchemeCardsAndValidate();

		Assert.assertFalse(results.isEmpty(), "No NPS scheme cards were discovered on the NPS tab");

		for (FundNPS_Page.LinkResult result : results) {
			logger.info("Scheme card result: {}", result);
			Assert.assertTrue(result.passed, "Scheme card failed validation: " + result.name);
		}

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should end back on Portfolio > NPS after all scheme cards validated");

		logger.info("All {} scheme cards validated", results.size());
	}

	// ============================================================
	// CORE JOURNEY: Portfolio > NPS -> tap a scheme card (e.g. "ICICI
	// Prudential Pension...") -> Update Transaction History -> validate each
	// screen -> back to Portfolio
	// ============================================================

	@Test(description =
			"Journey: NPS -> tap a scheme card -> detail dialog opens -> Update Transaction History -> "
					+ "screen opens -> back to NPS -> back to Portfolio")
	public void verifySchemeToUpdateTransactionHistoryJourney() {

		logger.info("=== NPS journey: scheme card -> Update Transaction History ===");

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should start the journey on Portfolio > NPS");

		Assert.assertTrue(npsPage.tapUpdateTransactionHistoryAndValidate(),
				"Scheme detail dialog should open and Update Transaction History should open its screen");

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should be back on Portfolio > NPS after the Update Transaction History journey");

		logger.info("=== NPS journey: scheme card -> Update Transaction History — done ===");
	}

	// ============================================================
	// COMPREHENSIVE LINK VALIDATION — every navigable link on the tab
	// ============================================================

	@Test(description =
			"Journey: tap every link on NPS (header, summary bar, scheme cards, Update Transaction "
					+ "History), validate the screen each opens, return")
	public void verifyAllLinksAndValidateScreens() {

		List<FundNPS_Page.LinkResult> results = npsPage.tapAllLinksAndValidateScreens();

		Assert.assertFalse(results.isEmpty(), "No navigable links were discovered on NPS tab");

		for (FundNPS_Page.LinkResult result : results) {
			logger.info("Link result: {}", result);
			Assert.assertTrue(result.passed, "Link failed validation: " + result.name);
		}

		Assert.assertTrue(npsPage.isNpsScreenDisplayed(),
				"Should end back on Portfolio > NPS after full link validation");

		logger.info("All {} links validated on NPS tab", results.size());
	}

	@Test(description = "Full regression: verify overall link validation passes across NPS")
	public void verifyAllLinksOverallPass() {

		Assert.assertTrue(npsPage.verifyAllLinksOpenScreens(),
				"One or more NPS links failed to open their expected screen");

		logger.info("Overall NPS link validation passed");
	}
}
