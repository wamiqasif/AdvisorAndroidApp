package pages;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Portfolio Screen Page Object.
 *
 * React Native portfolio dashboard: - summary cards - portfolio value -
 * investor switching - overlays - tabs - bottom navigation
 *
 * Enterprise-grade Appium implementation: - retry-safe actions - RN
 * stabilization - dynamic waits - gesture-safe scrolling - overlay recovery
 */
public class PortfolioPage extends BasePage {

	// ============================================================
	// HEADER
	// ============================================================

	private final By investorDropdown = AppiumBy.androidUIAutomator(
			"new UiSelector()" + ".className(\"android.widget.ImageView\")" + ".descriptionContains(\"Azeem\")");

	private final By updateButton = AppiumBy.accessibilityId("Update");

	// ============================================================
	// TABS
	// ============================================================

	private final By tabSummary = AppiumBy.accessibilityId("Summary");

	private final By tabFunds = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".className(\"android.view.View\")" + ".description(\"Funds\")");

	private final By tabStocksEtfs = AppiumBy.accessibilityId("Stocks & ETFs");

	private final By tabNPS = AppiumBy.accessibilityId("NPS");

	private final By tabBondsFDs = AppiumBy.accessibilityId("Bonds & FDs");

	// ============================================================
	// VALUE SECTION
	// ============================================================

	private final By portfolioValueLabel = AppiumBy.accessibilityId("Portfolio Value");

	private final By oneDayChangeLabel = AppiumBy.accessibilityId("1D Change");

	private final By allTimeReturnsToggle = AppiumBy.accessibilityId("All Time Returns");

	private final By compositeSummaryCard = AppiumBy.androidUIAutomator(
			"new UiSelector()" + ".className(\"android.view.View\")" + ".descriptionContains(\"1D Change:\")");

	private final By rupeeValueTexts = AppiumBy.androidUIAutomator(
			"new UiSelector()" + ".className(\"android.view.View\")" + ".descriptionMatches(\"₹.*\")");

	private final By signedRupeeValueTexts = AppiumBy.androidUIAutomator(
			"new UiSelector()" + ".className(\"android.view.View\")" + ".descriptionMatches(\"₹[+\\\\-].*\")");

	// ============================================================
	// CONTENT
	// ============================================================

	private final By alertBanner = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionContains(\"needs attention\")");

	private final By analysisHeading = AppiumBy.accessibilityId("Analysis");

	private final By viewAllLink = AppiumBy.accessibilityId("View all");

	private final By outdatedPortfolioCard = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionStartsWith(\"Outdated Portfolio\")");

	private final By riskProfileCard = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionStartsWith(\"Risk Profile\")");

	private final By yourInvestmentsHeading = AppiumBy.accessibilityId("Your Investments");

	private final By emptyStateStartNow = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionContains(\"Start Now\")");

	// ============================================================
	// INVESTOR SHEET
	// ============================================================

	private final By investorMultipleOption = AppiumBy.accessibilityId("Multiple");

	private final By investorAddOption = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionContains(\"Add Investor\")");

	private final By investorOldLabels = AppiumBy.accessibilityId("Old Labels");

	// ============================================================
	// DURATION OVERLAY
	// ============================================================

	private final By chooseDurationOverlay = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionContains(\"Duration\")");

	private final By chooseDurationAllTimeOption = AppiumBy
			.androidUIAutomator("new UiSelector()" + ".descriptionContains(\"All Time\")");

	private final By durationSheet = AppiumBy.accessibilityId("Choose Duration");

	private final By scrim = AppiumBy.accessibilityId("Scrim");

	// ============================================================
	// BOTTOM NAVIGATION
	// ============================================================

	private final By portfolioNavTab = AppiumBy.androidUIAutomator(
			"new UiSelector()" + ".className(\"android.widget.ImageView\")" + ".description(\"Portfolio\")");

	private final By hubNavTab = AppiumBy.accessibilityId("Hub");

	private final By fundsNavTab = AppiumBy.androidUIAutomator(
			"new UiSelector()" + ".className(\"android.widget.ImageView\")" + ".description(\"Funds\")");

	private final By stocksNavTab = AppiumBy.accessibilityId("Stocks");

	// ============================================================

	public PortfolioPage(AndroidDriver driver) {

		super(driver);
	}

	// ============================================================
	// PAGE LOAD
	// ============================================================

	public PortfolioPage waitForPortfolioScreen() {
		wait.until(driver -> isDisplayed(portfolioValueLabel) || isDisplayed(tabSummary)
				|| isDisplayed(oneDayChangeLabel) || isDisplayed(portfolioNavTab));
		return this;
	}

	// ============================================================
	// SCREEN DETECTION
	// ============================================================

	public boolean isPortfolioScreenDisplayed() {

		int visibleSignals = 0;

		if (isDisplayed(portfolioValueLabel))
			visibleSignals++;

		if (isDisplayed(tabSummary))
			visibleSignals++;

		if (isDisplayed(oneDayChangeLabel))
			visibleSignals++;

		if (isDisplayed(portfolioNavTab))
			visibleSignals++;

		return visibleSignals >= 2;
	}

	// ============================================================
	// NAVIGATION RECOVERY
	// ============================================================

	public void navigateToPortfolioIfNotThere() {

		if (!isPortfolioScreenDisplayed()) {

			recoverToPortfolioScreen();
		}
	}

	public void recoverToPortfolioScreen() {

	    if (isPortfolioScreenDisplayed()) {

	        return;
	    }

	    safeClick(portfolioNavTab);

	    waitForPortfolioScreen();
	}

	// ============================================================
	// BASIC STATE CHECKS
	// ============================================================

	public boolean isInvestorDropdownDisplayed() {
		return isDisplayed(investorDropdown);
	}

	public boolean isUpdateButtonDisplayed() {
		return isDisplayed(updateButton);
	}

	public boolean isPortfolioValueLabelDisplayed() {
		return isDisplayed(portfolioValueLabel);
	}

	public boolean isCompositeSummaryCardDisplayed() {
		return isDisplayed(compositeSummaryCard);
	}

	public boolean isAlertBannerDisplayed() {
		return isDisplayed(alertBanner);
	}

	public boolean isAnalysisHeadingDisplayed() {
		return isDisplayed(analysisHeading);
	}

	public boolean isViewAllLinkDisplayed() {
		return isDisplayed(viewAllLink);
	}

	public boolean isOutdatedPortfolioCardDisplayed() {
		return isDisplayed(outdatedPortfolioCard);
	}

	public boolean isRiskProfileCardDisplayed() {
		return isDisplayed(riskProfileCard);
	}

	public boolean isYourInvestmentsHeadingDisplayed() {
		return isDisplayed(yourInvestmentsHeading);
	}

	public boolean isEmptyPortfolioStateDisplayed() {
		return isDisplayed(emptyStateStartNow);
	}

	public boolean isPopulatedPortfolioStateDisplayed() {

		return isDisplayed(portfolioValueLabel) || isDisplayed(compositeSummaryCard);
	}

	// ============================================================
	// INVESTOR SHEET
	// ============================================================

	public boolean isInvestorSelectionSheetDisplayed() {

		return isDisplayed(investorMultipleOption) || isDisplayed(investorAddOption) || isDisplayed(investorOldLabels);
	}

	// ============================================================
	// DURATION OVERLAY
	// ============================================================

	public boolean isChooseDurationOverlayDisplayed() {

		return isDisplayed(chooseDurationOverlay) || isDisplayed(chooseDurationAllTimeOption);
	}

	public boolean isDurationBottomSheetVisible() {

		return isDisplayed(durationSheet);
	}

	public boolean isDurationOptionVisible(String option) {

		return isDisplayed(AppiumBy.accessibilityId(option));
	}

	// ============================================================
	// INTERACTIONS
	// ============================================================

	public PortfolioPage tapPortfolioNavTab() {

		safeClick(portfolioNavTab);

		return this;
	}

	public PortfolioPage tapInvestorDropdown() {

		safeClick(investorDropdown);

		return this;
	}

	public PortfolioPage tapHubNavTab() {

		safeClick(hubNavTab);

		return this;
	}

	public PortfolioPage tapFundsNavTab() {

		safeClick(fundsNavTab);

		return this;
	}

	public PortfolioPage tapStocksNavTab() {

		safeClick(stocksNavTab);

		return this;
	}

	public PortfolioPage tapTabSummary() {

		safeClick(tabSummary);

		return this;
	}

	public PortfolioPage tapTabFunds() {

		safeClick(tabFunds);

		return this;
	}

	public PortfolioPage tapTabStocksEtfs() {

		safeClick(tabStocksEtfs);

		return this;
	}

	public PortfolioPage tapTabNPS() {

		safeClick(tabNPS);

		return this;
	}

	public PortfolioPage tapTabBondsFDs() {

		safeClick(tabBondsFDs);

		return this;
	}

	public PortfolioPage tapAllTimeReturnsToggle() {

		safeClick(allTimeReturnsToggle);

		return this;
	}

	public PortfolioPage tapViewAllLink() {

		safeClick(viewAllLink);

		return this;
	}

	public PortfolioPage tapAlertBanner() {

		safeClick(alertBanner);

		return this;
	}

	public PortfolioPage tapOutdatedPortfolioCard() {

		safeClick(outdatedPortfolioCard);

		return this;
	}

	public PortfolioPage tapRiskProfileCard() {

		safeClick(riskProfileCard);

		return this;
	}

	public PortfolioPage tapScrim() {

		safeClick(scrim);

		return this;
	}

	// ============================================================
	// OVERLAY HELPERS
	// ============================================================

	public boolean closeOverlayWithBack() {

		try {

			driver.navigate().back();

			return waitUntilPortfolioScreenVisible();

		} catch (Exception e) {

			return false;
		}
	}

	public boolean openInvestorDropdown() {

		tapInvestorDropdown();

		return isInvestorSelectionSheetDisplayed();
	}

	public boolean openAllTimeReturnsOverlay() {

		tapAllTimeReturnsToggle();

		return isChooseDurationOverlayDisplayed();
	}

	// ============================================================
	// RAPID ACTION STRESS TESTS
	// ============================================================

	public void performRapidTopTabSwitch() {

		safeClick(tabFunds);

		safeClick(tabSummary);

		safeClick(tabFunds);

		safeClick(tabSummary);
	}

	public void performRepeatedBottomNavigationCycle() {

		safeClick(hubNavTab);

		safeClick(portfolioNavTab);

		safeClick(stocksNavTab);

		safeClick(portfolioNavTab);
	}

	// ============================================================
	// DATA EXTRACTION
	// ============================================================

	public String getPortfolioValueText() {

		waitForPortfolioScreen();

		if (findElements(rupeeValueTexts).isEmpty()) {

			return "";
		}

		return findElements(rupeeValueTexts).get(0).getAttribute("content-desc");
	}

	public String getOneDayChangeAmount() {

		waitForPortfolioScreen();

		if (findElements(signedRupeeValueTexts).isEmpty()) {

			return "";
		}

		return findElements(signedRupeeValueTexts).get(0).getAttribute("content-desc");
	}

	public String getAlertBannerText() {

		return waitForVisible(alertBanner).getAttribute("content-desc");
	}

	public String getCompositeSummaryText() {

		return waitForVisible(compositeSummaryCard).getAttribute("content-desc");
	}

	public String getInvestorDropdownText() {

		return waitForVisible(investorDropdown).getAttribute("content-desc");
	}

	// ============================================================
	// SCROLLING
	// ============================================================

	public boolean scrollToAnalysisSection() {
		return scrollUntilVisible(analysisHeading, 5);
	}

	public boolean scrollToOutdatedPortfolioCard() {
		return scrollUntilVisible(outdatedPortfolioCard, 6);
	}

	public boolean scrollToRiskProfileCard() {
		return scrollUntilVisible(riskProfileCard, 6);
	}

	public boolean scrollToYourInvestmentsHeading() {
		return scrollUntilVisible(yourInvestmentsHeading, 7);
	}

	public boolean scrollToViewAllLink() {
		return scrollUntilVisible(viewAllLink, 5);
	}

	private boolean scrollUntilVisible(By locator, int maxScrolls) {

		if (isDisplayed(locator)) {

			return true;
		}

		for (int i = 0; i < maxScrolls; i++) {

			boolean canScrollMore = scrollDownOnce();

			if (isDisplayed(locator)) {

				return true;
			}

			if (!canScrollMore) {

				break;
			}
		}

		return isDisplayed(locator);
	}

	private boolean scrollDownOnce() {

		Dimension size = driver.manage().window().getSize();

		Map<String, Object> params = new HashMap<>();

		params.put("left", 0);
		params.put("top", 250);
		params.put("width", size.getWidth());
		params.put("height", Math.max(size.getHeight() - 500, 1000));
		params.put("direction", "down");
		params.put("percent", 0.75);

		Object result = driver.executeScript("mobile: scrollGesture", params);

		return result instanceof Boolean && (Boolean) result;
	}

	// ============================================================
	// FINAL VALIDATION
	// ============================================================

	public boolean waitUntilPortfolioScreenVisible() {

		try {

			waitForPortfolioScreen();

			return true;

		} catch (TimeoutException e) {

			return false;
		}
	}
}
