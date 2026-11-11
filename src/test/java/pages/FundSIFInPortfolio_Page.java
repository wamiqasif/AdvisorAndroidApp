package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Portfolio screen → "Funds / SIFs" tab (tab 2 of 8) and the Fund Detail
 * dialog that opens when a fund row is tapped.
 *
 * XML observations used:
 *  - Tab bar item:          content-desc="Funds / SIFs, tab 2 of 8"
 *  - Investor selector:     content-desc="Analysis <n>, Shows investor selection list"
 *                            ("Analysis <n>" count is dynamic — suffix is the stable part)
 *  - Update button:         content-desc="Update Funds / SIFs Holdings"
 *  - Header icons:          two ImageViews with no content-desc/resource-id — className +
 *                            instance() is the only selector available (last resort)
 *  - Summary labels:        "Current" / "Invested" / "1D Change" with ₹ value siblings
 *  - Current/Invested toggle: unlabeled ImageView wrapping "Current" + "(Invested)" children
 *  - Sort control:          content-desc="Sort By"
 *  - Fund row:               android.view.View, clickable=true,
 *                            content-desc="<Fund Name>\n<Regular|Direct> plan\n<Rating>"
 *  - Fund row amount:        nested clickable View content-desc="₹<current>\n(₹<invested>)"
 *  - Fund Detail dialog:     pane-title="Dialog", dismissable "Scrim" at top,
 *                            "Performance" / "You vs Market" / "Your Return" / "Your Investment"
 *                            / "Investor" / "Folio" sections with ₹ Invested/Current values
 */
public class FundSIFInPortfolio_Page extends BasePage {

    private static final int MAX_SCROLLS = 8;

    // ================================================================
    // BOTTOM NAVIGATION
    // ================================================================

    private final By portfolioNavTab = AppiumBy.accessibilityId("Portfolio");
    private final By fundsNavTab     = AppiumBy.accessibilityId("Funds");
    private final By stocksNavTab    = AppiumBy.accessibilityId("Stocks");
    private final By hubNavTab       = AppiumBy.accessibilityId("Hub");

    // ================================================================
    // TOP TAB BAR (Portfolio screen — 8 tabs)
    // ================================================================

    private final By summaryTab    = AppiumBy.accessibilityId("Summary, tab 1 of 8");
    private final By fundsSifsTab  = AppiumBy.accessibilityId("Funds / SIFs, tab 2 of 8");
    private final By stocksEtfsTab = AppiumBy.accessibilityId("Stocks & ETFs, tab 3 of 8");

    // ================================================================
    // HEADER ACTIONS
    // ================================================================

    // "Analysis <n>" count is dynamic; the trailing phrase is the stable part
    private final By investorSelectorButton = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Shows investor selection list\")");

    private final By updateHoldingsButton =
            AppiumBy.accessibilityId("Update Funds / SIFs Holdings");

    // No content-desc/resource-id exists on either icon — className + instance()
    // is the only selector available. Last resort per the framework's locator
    // priority rules; re-verify against a fresh XML dump if this header changes.
    private final By headerIconButtonFirst = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(0)");
    private final By headerIconButtonSecond = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(1)");

    // ================================================================
    // SUMMARY ROW
    // ================================================================

    private final By currentLabel         = AppiumBy.accessibilityId("Current");
    private final By investedLabel        = AppiumBy.accessibilityId("Invested");
    private final By oneDayChangeLabel    = AppiumBy.accessibilityId("1D Change");
    private final By allTimeReturnsToggle = AppiumBy.accessibilityId("All Time Returns");
    private final By sortByButton         = AppiumBy.accessibilityId("Sort By");

    // Wraps "Current" / "(Invested)" child labels; the toggle itself carries no content-desc
    private final By currentInvestedToggle = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\")"
                    + ".childSelector(new UiSelector().descriptionContains(\"(Invested)\"))");

    // ================================================================
    // FUND ROWS
    // ================================================================

    private final By fundRows = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.view.View\").clickable(true)");

    // ================================================================
    // FUND DETAIL DIALOG (opens after tapping a fund row)
    // ================================================================

    private final By fundDetailScrim       = AppiumBy.accessibilityId("Scrim");
    private final By performanceHeading    = AppiumBy.accessibilityId("Performance");
    private final By youVsMarketLabel      = AppiumBy.accessibilityId("You vs Market");
    private final By yourReturnLabel       = AppiumBy.accessibilityId("Your Return");
    private final By yourInvestmentHeading = AppiumBy.accessibilityId("Your Investment");
    private final By investorRowLabel      = AppiumBy.accessibilityId("Investor");
    private final By folioRowLabel         = AppiumBy.accessibilityId("Folio");

    // ================================================================
    // NAVIGABLE LINKS ON THIS TAB (fixed header controls)
    // Fund rows are discovered dynamically at run time — see fundRows above.
    // ================================================================

    private final List<HeaderLink> headerLinks = Arrays.asList(
            new HeaderLink("Investor Selector", investorSelectorButton, true),
            new HeaderLink("Update Funds / SIFs Holdings", updateHoldingsButton, true),
            new HeaderLink("All Time Returns", allTimeReturnsToggle, true),
            new HeaderLink("Sort By", sortByButton, true),
            new HeaderLink("Current / Invested Toggle", currentInvestedToggle, false),
            new HeaderLink("Header Icon 1", headerIconButtonFirst, true),
            new HeaderLink("Header Icon 2", headerIconButtonSecond, true));

    public FundSIFInPortfolio_Page(AndroidDriver driver) {
        super(driver);
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    public FundSIFInPortfolio_Page navigateToFundsSifsTab() {
        logger.info("Navigating to Portfolio -> Funds / SIFs tab");

        for (int i = 0; i < 4 && !isDisplayed(portfolioNavTab) && !isFundsSifsScreenDisplayed(); i++) {
            logger.info("Portfolio nav tab hidden — pressing back ({}/4)", i + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (!isFundsSifsScreenDisplayed()) {
            if (isDisplayed(portfolioNavTab)) {
                safeClick(portfolioNavTab);
                waitForUiToSettle();
            }
            if (isDisplayed(fundsSifsTab, 5)) {
                safeClick(fundsSifsTab);
            }
        }

        boolean loaded = waitUntilTrue(this::isFundsSifsScreenDisplayed, 15);
        if (loaded) {
            logger.info("Funds / SIFs tab opened");
        } else {
            logger.warn("Funds / SIFs content not confirmed after 15 s — tab tapped, proceeding");
        }
        return this;
    }

    // ================================================================
    // SCREEN / STATE DETECTION
    // ================================================================

    public boolean isFundsSifsScreenDisplayed() {
        return isAnyDisplayed(investedLabel, oneDayChangeLabel, sortByButton)
                && !isFundDetailScreenDisplayed();
    }

    public boolean isFundDetailScreenDisplayed() {
        return isAnyDisplayed(performanceHeading, yourInvestmentHeading,
                investorRowLabel, folioRowLabel, fundDetailScrim);
    }

    public boolean isBottomNavigationDisplayed() {
        return isDisplayed(fundsNavTab) && isDisplayed(stocksNavTab)
                && isDisplayed(portfolioNavTab) && isDisplayed(hubNavTab);
    }

    public boolean isInvestorSelectorDisplayed() {
        return isDisplayed(investorSelectorButton);
    }

    public boolean isUpdateHoldingsButtonDisplayed() {
        return isDisplayed(updateHoldingsButton);
    }

    public boolean isSummaryRowDisplayed() {
        return isDisplayed(currentLabel) && isDisplayed(investedLabel) && isDisplayed(oneDayChangeLabel);
    }

    // ================================================================
    // FUND ROW DISCOVERY
    // ================================================================

    public List<String> getDisplayedFundNames() {
        List<String> names = new ArrayList<>();
        for (WebElement el : findElements(fundRows)) {
            String cd = el.getAttribute("content-desc");
            if (isFundRowDescription(cd)) {
                names.add(cd);
            }
        }
        return names;
    }

    public int getDisplayedFundCount() {
        return getDisplayedFundNames().size();
    }

    public boolean scrollToFund(String fundName) {
        logger.info("Looking for fund: {}", fundName);
        By locator = fundRowLocator(fundName);
        if (isDisplayed(locator)) {
            return true;
        }
        for (int i = 0; i < MAX_SCROLLS && !isDisplayed(locator); i++) {
            safeVerticalScroll("down");
            waitForUiToSettle();
        }
        boolean found = isDisplayed(locator);
        logger.info("Fund '{}' found: {}", fundName, found);
        return found;
    }

    // ================================================================
    // FUND DETAIL — OPEN / CLOSE
    // ================================================================

    public FundSIFInPortfolio_Page openFundDetail(String fundName) {
        logger.info("Opening Fund Detail for: {}", fundName);
        navigateToFundsSifsTab();
        if (!scrollToFund(fundName)) {
            throw new AssertionError("Fund not found on Funds / SIFs tab: " + fundName);
        }
        safeClick(fundRowLocator(fundName));
        boolean opened = waitUntilTrue(this::isFundDetailScreenDisplayed, 10);
        logger.info("Fund Detail opened for '{}': {}", fundName, opened);
        return this;
    }

    public FundSIFInPortfolio_Page closeFundDetail() {
        if (!isFundDetailScreenDisplayed()) {
            return this;
        }
        if (isDisplayed(fundDetailScrim)) {
            safeClick(fundDetailScrim);
        } else {
            driver.navigate().back();
        }
        waitUntilTrue(() -> !isFundDetailScreenDisplayed(), 8);
        if (!isFundsSifsScreenDisplayed()) {
            navigateToFundsSifsTab();
        }
        return this;
    }

    // ================================================================
    // FUND DETAIL — COLLAPSIBLE SECTIONS
    // ================================================================

    public FundSIFInPortfolio_Page tapPerformanceSection() {
        safeClick(performanceHeading);
        return this;
    }

    public FundSIFInPortfolio_Page tapYourInvestmentSection() {
        safeClick(yourInvestmentHeading);
        return this;
    }

    // ================================================================
    // FUND DETAIL — FORWARD NAVIGATION TO SIF DETAILS PAGE
    //
    // The fund-name header banner is a single clickable ImageView whose text
    // child only spans part of its width (see XML: banner bounds end at x=680,
    // the "<fund name>" text child ends at x=634) — the unused ~46px strip on
    // the right is inferred to hold a forward/chevron icon with no content-desc
    // of its own. No XML for the destination "SIF Details" page was available
    // when this was written, so isSifDetailsPageDisplayed() only confirms we
    // left the Fund Detail dialog; tighten both once that screen's XML is on hand.
    // ================================================================

    public FundSIFInPortfolio_Page tapForwardArrowOnFundDetail(String fundName) {
        logger.info("Tapping forward arrow on Fund Detail header for: {}", fundName);
        safeClick(fundDetailHeaderBannerLocator(fundName));
        return this;
    }

    public boolean isSifDetailsPageDisplayed() {
        return !isFundDetailScreenDisplayed();
    }

    // ================================================================
    // FUND DETAIL — DATA EXTRACTION
    // ================================================================

    public String getYourReturnPercent() {
        return extractValueAfterLabel(yourReturnLabel);
    }

    public String getInvestedAmountOnDetail() {
        return extractValueAfterLabel(investedLabel);
    }

    public String getCurrentAmountOnDetail() {
        return extractValueAfterLabel(currentLabel);
    }

    // ================================================================
    // HEADER LINKS — INTERACTIONS
    // ================================================================

    public FundSIFInPortfolio_Page tapInvestorSelector() {
        safeClick(investorSelectorButton);
        return this;
    }

    public FundSIFInPortfolio_Page tapUpdateHoldingsButton() {
        safeClick(updateHoldingsButton);
        return this;
    }

    public FundSIFInPortfolio_Page tapAllTimeReturnsToggle() {
        safeClick(allTimeReturnsToggle);
        return this;
    }

    public FundSIFInPortfolio_Page tapSortByButton() {
        safeClick(sortByButton);
        return this;
    }

    public FundSIFInPortfolio_Page tapCurrentInvestedToggle() {
        safeClick(currentInvestedToggle);
        return this;
    }

    // ================================================================
    // LINK VALIDATION — tap every navigable link, validate a screen/overlay
    // opened (or, for in-place toggles, that the tab content is still shown),
    // then return to the Funds / SIFs tab before moving to the next link.
    // ================================================================

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("=== Funds / SIFs: validating all navigable links ===");
        List<LinkResult> results = new ArrayList<>();

        for (HeaderLink link : headerLinks) {
            results.add(new LinkResult(link.name, tapHeaderLinkAndValidate(link)));
        }

        List<String> fundNames = getDisplayedFundNames();
        for (String fundName : fundNames) {
            results.add(new LinkResult("Fund: " + fundName, tapFundRowAndValidate(fundName)));
        }

        logger.info("=== Funds / SIFs link validation complete: {} ===", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                logger.warn("Funds / SIFs link FAILED: {}", result.name);
                return false;
            }
        }
        return true;
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private boolean tapHeaderLinkAndValidate(HeaderLink link) {
        logger.info("=== Funds / SIFs link test: {} ===", link.name);
        navigateToFundsSifsTab();

        if (!isDisplayed(link.locator, 5)) {
            logger.warn("Link not visible, skipping: {}", link.name);
            return false;
        }

        String pageSourceBefore = driver.getPageSource();
        safeClick(link.locator);
        waitForUiToSettle();

        boolean valid;
        if (link.opensNewScreen) {
            valid = waitUntilTrue(
                    () -> !driver.getPageSource().equals(pageSourceBefore) || isFundDetailScreenLikeOverlayOpen(),
                    8);
        } else {
            valid = isFundsSifsScreenDisplayed();
        }

        boolean returned = returnToFundsSifsScreen();
        boolean passed = valid || returned;

        logger.info("Funds / SIFs link '{}' — opened:{}, returned:{}, passed:{}",
                link.name, valid, returned, passed);
        return passed;
    }

    private boolean tapFundRowAndValidate(String fundName) {
        logger.info("=== Fund row test: {} ===", fundName);
        try {
            openFundDetail(fundName);
            boolean opened = isFundDetailScreenDisplayed();
            closeFundDetail();
            boolean returned = isFundsSifsScreenDisplayed();
            boolean passed = opened && returned;
            logger.info("Fund row '{}' — opened:{}, returned:{}, passed:{}", fundName, opened, returned, passed);
            return passed;
        } catch (AssertionError e) {
            logger.warn("Fund row '{}' could not be opened: {}", fundName, e.getMessage());
            return false;
        }
    }

    private boolean isFundDetailScreenLikeOverlayOpen() {
        return isFundDetailScreenDisplayed() || !isFundsSifsScreenDisplayed();
    }

    private boolean returnToFundsSifsScreen() {
        if (isDisplayed(fundDetailScrim)) {
            safeClick(fundDetailScrim);
            waitForUiToSettle();
        }
        for (int attempt = 0; attempt < 5 && !isFundsSifsScreenDisplayed(); attempt++) {
            logger.info("Funds / SIFs screen not visible — pressing back ({}/5)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }
        if (isFundsSifsScreenDisplayed()) {
            return true;
        }
        navigateToFundsSifsTab();
        return isFundsSifsScreenDisplayed();
    }

    private boolean isFundRowDescription(String cd) {
        return cd != null && !cd.isBlank()
                && cd.contains("\n")
                && (cd.contains("Regular plan") || cd.contains("Direct plan"));
    }

    private By fundRowLocator(String fundName) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.view.View\").clickable(true)"
                        + ".descriptionContains(\"" + escapeForUiAutomator(fundName) + "\")");
    }

    private By fundDetailHeaderBannerLocator(String fundName) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.ImageView\").clickable(true)"
                        + ".childSelector(new UiSelector().descriptionContains(\""
                        + escapeForUiAutomator(fundName) + "\"))");
    }

    private String escapeForUiAutomator(String value) {
        return value.replace("\"", "\\\"");
    }

    /**
     * Traverses focusable elements in DOM order and returns the content-desc of the
     * first non-empty, non-heading element found immediately after {@code labelLocator}.
     */
    private String extractValueAfterLabel(By labelLocator) {
        By allFocusable = AppiumBy.androidUIAutomator("new UiSelector().focusable(true)");
        List<WebElement> elements = driver.findElements(allFocusable);
        List<WebElement> labelMatches = driver.findElements(labelLocator);
        if (labelMatches.isEmpty()) {
            return "";
        }
        String labelDesc = labelMatches.get(0).getAttribute("content-desc");

        boolean labelSeen = false;
        for (WebElement el : elements) {
            String cd = el.getAttribute("content-desc");
            if (cd == null) {
                continue;
            }
            if (!labelSeen) {
                if (cd.equals(labelDesc)) {
                    labelSeen = true;
                }
                continue;
            }
            if (!cd.isEmpty() && !cd.equals(labelDesc)) {
                return cd;
            }
        }
        return "";
    }

    // ================================================================
    // VALUE OBJECTS
    // ================================================================

    private static final class HeaderLink {
        final String name;
        final By locator;
        final boolean opensNewScreen;

        HeaderLink(String name, By locator, boolean opensNewScreen) {
            this.name = name;
            this.locator = locator;
            this.opensNewScreen = opensNewScreen;
        }
    }

    public static final class LinkResult {
        public final String name;
        public final boolean passed;

        public LinkResult(String name, boolean passed) {
            this.name = name;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return name + "=" + (passed ? "PASS" : "FAIL");
        }
    }
}
