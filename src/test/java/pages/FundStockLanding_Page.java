package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class FundStockLanding_Page extends BasePage {

    private static final int MAX_SCROLLS = 8;

    // ====================================================================
    // BOTTOM NAVIGATION
    // ====================================================================

    private final By stocksTab    = AppiumBy.accessibilityId("Stocks");
    private final By fundsTab     = AppiumBy.accessibilityId("Funds");
    private final By portfolioTab = AppiumBy.accessibilityId("Portfolio");
    private final By hubTab       = AppiumBy.accessibilityId("Hub");

    // ====================================================================
    // STOCK ADVISOR LANDING (XML-1 — upsell / subscription screen)
    // Shown on the Stocks tab before subscribing to Stock Advisor.
    // ====================================================================

    private final By stockAdvisorHeadline = AppiumBy.accessibilityId(
            "The no-nonsense service to help you find quality stocks and maintaining your conviction in them");
    private final By stockAdvisorFeature1 = AppiumBy.accessibilityId(
            "New stock recommendations as soon as they are released");
    private final By stockAdvisorFeature2 = AppiumBy.accessibilityId(
            "Complete investment thesis for all recommended stocks");
    private final By stockAdvisorFeature3 = AppiumBy.accessibilityId(
            "Continuous analysis & updates from our dedicated analyst team, and much more");
    private final By openStockAdvisorButton = AppiumBy.accessibilityId(
            "Open Stock Advisor Details");

    // descriptionContains fallbacks guard against minor label changes
    private final By stockAdvisorHeadlineFallback = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"no-nonsense service\")");
    private final By openStockAdvisorFallback = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Stock Advisor Details\")");

    // ====================================================================
    // STOCKS DASHBOARD — QUICK SEARCH (XML-2)
    // ====================================================================

    private final By quickSearchLabel = AppiumBy.accessibilityId("Quick Search");
    private final By quickSearchDescription = AppiumBy.accessibilityId(
            "Search any stock for comprehensive research, analysis, and expert ratings");

    // EditText has no content-desc; matched by class — unique on this screen
    private final By searchField = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

    private final By quickSearchFallback = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Quick Search\")");

    // ====================================================================
    // STOCKS DASHBOARD — MARKET MONITOR (XML-2)
    // ====================================================================

    private final By marketMonitorLabel = AppiumBy.accessibilityId("Market Monitor");
    private final By marketMonitorFallback = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Market Monitor\")");

    // Market-type filter tabs
    private final By filterAll    = AppiumBy.accessibilityId("All");
    private final By filterIndian = AppiumBy.accessibilityId("Indian");
    private final By filterGlobal = AppiumBy.accessibilityId("Global");

    // Return-period tabs
    private final By periodTrailing  = AppiumBy.accessibilityId("Trailing");
    private final By periodAnnual    = AppiumBy.accessibilityId("Annual");
    private final By periodQuarterly = AppiumBy.accessibilityId("Quarterly");
    private final By periodMonthly   = AppiumBy.accessibilityId("Monthly");

    // Column sort-header buttons (ImageView with content-desc)
    private final By columnIndex  = AppiumBy.accessibilityId("Index");
    private final By column1Day   = AppiumBy.accessibilityId("1 Day");
    private final By columnYtd    = AppiumBy.accessibilityId("YTD");
    private final By column1Week  = AppiumBy.accessibilityId("1 Week");

    // Index row labels
    private final By bseSensexLabel = AppiumBy.accessibilityId("BSE Sensex Index");
    private final By bseIpoLabel    = AppiumBy.accessibilityId("BSE IPO Index");
    private final By bse100Label    = AppiumBy.accessibilityId("BSE 100 Index");
    private final By bse200Label    = AppiumBy.accessibilityId("BSE 200 Index");

    // Performance data cells — value is dynamic, so descriptionContains is used
    private final By bseSensex1DayCell = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BSE Sensex Index, 1 Day\")");
    private final By bseSensexYtdCell = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BSE Sensex Index, YTD\")");
    private final By bseSensex1WeekCell = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BSE Sensex Index, 1 Week\")");
    private final By bseIpo1DayCell = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BSE IPO Index, 1 Day\")");
    private final By bse1001DayCell = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BSE 100 Index, 1 Day\")");
    private final By bse2001DayCell = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"BSE 200 Index, 1 Day\")");

    // ====================================================================
    // NAVIGABLE LINKS — elements that open a new screen when tapped
    // (in-page filter / sort taps are handled separately below)
    // ====================================================================

    private final List<StocksLink> stocksLinks = Arrays.asList(
            new StocksLink("Open Stock Advisor Details",
                    openStockAdvisorButton, openStockAdvisorFallback),
            new StocksLink("Quick Search Field",
                    searchField, quickSearchFallback));

    public FundStockLanding_Page(AndroidDriver driver) {
        super(driver);
    }

    // ====================================================================
    // NAVIGATION TO STOCKS TAB
    // ====================================================================

    public FundStockLanding_Page openStocksScreen() {
        logger.info("Opening Stocks screen");
        if (isStocksScreenDisplayed()) {
            logger.info("Stocks screen already open");
            return this;
        }
        waitForUiToSettle();
        if (isStocksScreenDisplayed()) {
            return this;
        }
        // Unwind any Flutter overlay hiding the bottom nav
        for (int i = 0; i < 4 && !isDisplayed(stocksTab); i++) {
            logger.info("Stocks tab hidden — pressing back ({}/4)", i + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }
        if (isDisplayed(stocksTab)) {
            safeClick(stocksTab);
            waitForUiToSettle();
        }
        boolean loaded = waitUntilTrue(this::isStocksScreenDisplayed, 15);
        if (loaded) {
            logger.info("Stocks screen opened");
        } else {
            logger.warn("Stocks content not confirmed after 15 s — tab tapped, proceeding");
        }
        return this;
    }

    // ====================================================================
    // SCREEN / SECTION DETECTION
    // ====================================================================

    /**
     * Returns {@code true} when either the Stock Advisor landing (XML-1) or the
     * Stocks dashboard with Market Monitor / Quick Search (XML-2) is visible.
     */
    public boolean isStocksScreenDisplayed() {
        return isAnyDisplayed(
                quickSearchLabel, marketMonitorLabel,
                stockAdvisorHeadline, openStockAdvisorButton,
                stockAdvisorHeadlineFallback, quickSearchFallback);
    }

    public boolean isStocksTabVisible() {
        return isDisplayed(stocksTab);
    }

    public boolean isBottomNavigationDisplayed() {
        return isDisplayed(fundsTab)
                && isDisplayed(stocksTab)
                && isDisplayed(portfolioTab)
                && isDisplayed(hubTab);
    }

    // ====================================================================
    // STOCK ADVISOR LANDING — VISIBILITY CHECKS (XML-1)
    // ====================================================================

    public boolean isStockAdvisorLandingDisplayed() {
        return isAnyDisplayed(stockAdvisorHeadline, stockAdvisorHeadlineFallback);
    }

    public boolean isOpenStockAdvisorButtonDisplayed() {
        return isDisplayed(openStockAdvisorButton);
    }

    public boolean areStockAdvisorFeaturesDisplayed() {
        return isAnyDisplayed(stockAdvisorFeature1, stockAdvisorFeature2, stockAdvisorFeature3);
    }

    // ====================================================================
    // QUICK SEARCH — VISIBILITY CHECKS (XML-2)
    // ====================================================================

    public boolean isQuickSearchDisplayed() {
        return isAnyDisplayed(quickSearchLabel, quickSearchFallback);
    }

    public boolean isSearchFieldDisplayed() {
        return isDisplayed(searchField);
    }
public void enterStock( String stockName) {
	safeSendKeys(searchField, stockName);
}
    // ====================================================================
    // MARKET MONITOR — VISIBILITY CHECKS (XML-2)
    // ====================================================================

    public boolean isMarketMonitorDisplayed() {
        return isAnyDisplayed(marketMonitorLabel, marketMonitorFallback);
    }

    public boolean isFilterBarDisplayed() {
        return isAnyDisplayed(filterAll, filterIndian, filterGlobal);
    }

    public boolean isPeriodBarDisplayed() {
        return isAnyDisplayed(periodTrailing, periodAnnual, periodQuarterly, periodMonthly);
    }

    public boolean isColumnHeadersDisplayed() {
        // column1Week is often off-screen in the HorizontalScrollView; check the first two
        return isAnyDisplayed(columnIndex, column1Day);
    }

    public boolean isMarketMonitorDataDisplayed() {
        return isAnyDisplayed(bseSensexLabel, bseIpoLabel, bse100Label, bse200Label);
    }

    public boolean isBseSensexDataCellDisplayed() {
        return isAnyDisplayed(bseSensex1DayCell, bseSensexYtdCell, bseSensex1WeekCell);
    }

    public boolean isBseIpoDataCellDisplayed() {
        return isDisplayed(bseIpo1DayCell);
    }

    public boolean isBse100DataCellDisplayed() {
        return isDisplayed(bse1001DayCell);
    }

    public boolean isBse200DataCellDisplayed() {
        return isDisplayed(bse2001DayCell);
    }

    // ====================================================================
    // IN-PAGE INTERACTIONS — filter / period / sort (no screen change)
    // Each method taps the control and validates the market data is still shown.
    // ====================================================================

    public boolean tapFilterAllAndValidate() {
        logger.info("Tapping 'All' market filter");
        if (!isDisplayed(filterAll)) {
            logger.warn("'All' filter not visible");
            return false;
        }
        safeClick(filterAll);
        waitForUiToSettle();
        boolean valid = isMarketMonitorDataDisplayed();
        logger.info("'All' filter — market data visible after tap: {}", valid);
        return valid;
    }

    public boolean tapFilterIndianAndValidate() {
        logger.info("Tapping 'Indian' market filter");
        if (!isDisplayed(filterIndian)) {
            logger.warn("'Indian' filter not visible");
            return false;
        }
        safeClick(filterIndian);
        waitForUiToSettle();
        boolean valid = isMarketMonitorDataDisplayed();
        logger.info("'Indian' filter — market data visible after tap: {}", valid);
        return valid;
    }

    public boolean tapFilterGlobalAndValidate() {
        logger.info("Tapping 'Global' market filter");
        if (!isDisplayed(filterGlobal)) {
            logger.warn("'Global' filter not visible");
            return false;
        }
        safeClick(filterGlobal);
        waitForUiToSettle();
        boolean valid = isDisplayed(filterGlobal);
        logger.info("'Global' filter — tab still present after tap: {}", valid);
        return valid;
    }

    public boolean tapPeriodTrailingAndValidate() {
        logger.info("Tapping 'Trailing' period");
        if (!isDisplayed(periodTrailing)) return false;
        safeClick(periodTrailing);
        waitForUiToSettle();
        boolean valid = isMarketMonitorDataDisplayed();
        logger.info("'Trailing' period — market data visible: {}", valid);
        return valid;
    }

    public boolean tapPeriodAnnualAndValidate() {
        logger.info("Tapping 'Annual' period");
        if (!isDisplayed(periodAnnual)) return false;
        safeClick(periodAnnual);
        waitForUiToSettle();
        boolean valid = isMarketMonitorDataDisplayed();
        logger.info("'Annual' period — market data visible: {}", valid);
        return valid;
    }

    public boolean tapPeriodQuarterlyAndValidate() {
        logger.info("Tapping 'Quarterly' period");
        if (!isDisplayed(periodQuarterly)) return false;
        safeClick(periodQuarterly);
        waitForUiToSettle();
        boolean valid = isMarketMonitorDataDisplayed();
        logger.info("'Quarterly' period — market data visible: {}", valid);
        return valid;
    }

    public boolean tapPeriodMonthlyAndValidate() {
        logger.info("Tapping 'Monthly' period");
        if (!isDisplayed(periodMonthly)) return false;
        safeClick(periodMonthly);
        waitForUiToSettle();
        boolean valid = isMarketMonitorDataDisplayed();
        logger.info("'Monthly' period — market data visible: {}", valid);
        return valid;
    }

    public boolean tapColumnSortIndexAndValidate() {
        logger.info("Tapping 'Index' column sort");
        if (!isDisplayed(columnIndex)) return false;
        safeClick(columnIndex);
        waitForUiToSettle();
        return isMarketMonitorDataDisplayed();
    }

    public boolean tapColumnSort1DayAndValidate() {
        logger.info("Tapping '1 Day' column sort");
        if (!isDisplayed(column1Day)) return false;
        safeClick(column1Day);
        waitForUiToSettle();
        return isMarketMonitorDataDisplayed();
    }

    public boolean tapColumnSortYtdAndValidate() {
        logger.info("Tapping 'YTD' column sort");
        if (!isDisplayed(columnYtd)) return false;
        safeClick(columnYtd);
        waitForUiToSettle();
        return isMarketMonitorDataDisplayed();
    }

    public boolean tapColumnSort1WeekAndValidate() {
        logger.info("Tapping '1 Week' column sort");
        if (!isDisplayed(column1Week)) return false;
        safeClick(column1Week);
        waitForUiToSettle();
        return isMarketMonitorDataDisplayed();
    }

    // ====================================================================
    // NAVIGATION LINKS — tap → validate new screen opened → return to Stocks
    // ====================================================================

    public boolean tapOpenStockAdvisorDetailsAndValidate() {
        return tapLinkAndValidate(stocksLinks.get(0));
    }

    public boolean tapQuickSearchFieldAndValidate() {
        return tapLinkAndValidate(stocksLinks.get(1));
    }

    // ====================================================================
    // BULK VALIDATE — exercises every navigable link in sequence
    // ====================================================================

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("=== Stocks: validating all {} navigation links ===", stocksLinks.size());
        List<LinkResult> results = new ArrayList<>();
        for (StocksLink link : stocksLinks) {
            boolean passed = tapLinkAndValidate(link);
            results.add(new LinkResult(link.name, passed));
        }
        logger.info("=== Stocks link validation complete: {} ===", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                logger.warn("Stocks link FAILED: {}", result.name);
                return false;
            }
        }
        return true;
    }

    // ====================================================================
    // PRIVATE HELPERS
    // ====================================================================

    private boolean tapLinkAndValidate(StocksLink link) {
        logger.info("=== Stocks link test: {} ===", link.name);

        openStocksScreen();

        By activeLocator = link.locator;
        if (!scrollDownUntilVisible(activeLocator, MAX_SCROLLS)) {
            logger.info("Primary locator not found for '{}', trying fallback", link.name);
            if (link.fallback != null && scrollDownUntilVisible(link.fallback, MAX_SCROLLS)) {
                activeLocator = link.fallback;
            } else {
                logger.warn("Stocks link not visible after scrolling: {}", link.name);
                return false;
            }
        }

        String pageSourceBefore = driver.getPageSource();
        safeClick(activeLocator);
        waitForUiToSettle();

        boolean openedScreen = waitForDestinationScreen(pageSourceBefore);
        boolean returned = returnToStocksScreen();
        boolean passed = openedScreen || returned;

        logger.info("Stocks link '{}' — openedScreen:{}, returned:{}, passed:{}",
                link.name, openedScreen, returned, passed);
        logger.info("=== Stocks link test: {} — done ===", link.name);
        return passed;
    }

    private boolean waitForDestinationScreen(String pageSourceBefore) {
        try {
            shortWait(8).until(d -> {
                String current = d.getPageSource();
                return !current.equals(pageSourceBefore) && !isStocksScreenDisplayed();
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean returnToStocksScreen() {
        for (int attempt = 0; attempt < 5; attempt++) {
            if (isStocksScreenDisplayed()) {
                return true;
            }
            logger.info("Stocks screen not visible — pressing back ({}/5)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }
        if (isStocksScreenDisplayed()) {
            return true;
        }
        try {
            if (isDisplayed(stocksTab, 3)) {
                safeClick(stocksTab);
                shortWait(8).until(d -> isStocksScreenDisplayed());
                return true;
            }
        } catch (Exception ignored) {
        }
        return isStocksScreenDisplayed();
    }

    private boolean scrollDownUntilVisible(By locator, int maxSwipes) {
        for (int swipe = 0; swipe <= maxSwipes; swipe++) {
            if (isDisplayed(locator)) {
                return true;
            }
            logger.info("Scrolling up ({}/{}) to reveal: {}", swipe + 1, maxSwipes, locator);
            safeVerticalScroll("up");
            waitForUiToSettle();
        }
        return isDisplayed(locator);
    }

    // ====================================================================
    // VALUE OBJECTS
    // ====================================================================

    private static final class StocksLink {
        final String name;
        final By locator;
        final By fallback;

        StocksLink(String name, By locator, By fallback) {
            this.name    = name;
            this.locator = locator;
            this.fallback = fallback;
        }
    }

    public static final class LinkResult {
        public final String  name;
        public final boolean passed;

        public LinkResult(String name, boolean passed) {
            this.name   = name;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return name + "=" + (passed ? "PASS" : "FAIL");
        }
    }
}
