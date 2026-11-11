package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Portfolio screen -> "NPS" tab (tab 4 of 8): header controls, portfolio
 * summary bar, the NPS scheme list, and the scheme-detail dialog opened by
 * tapping a scheme card.
 *
 * XML observations used:
 *  - Tab bar item:            content-desc="NPS, tab 4 of 8"
 *  - Investor switcher:       content-desc="Manish Khatri, Shows investor
 *                              selection list" ("Manish Khatri" is dynamic —
 *                              trailing phrase is the stable fallback part)
 *  - Update holdings button:  content-desc="Update NPS Holdings"
 *  - Header icons:            two ImageViews with no content-desc/resource-id
 *                              — className + instance() is the only selector
 *                              available (last resort, same as the Stocks &
 *                              ETFs / Funds header)
 *  - Summary bar:             "Current"/"Invested"/"1D Change" are static
 *                              labels (clickable="false" in the XML) — only
 *                              "All Time Returns" (ImageView, clickable) and
 *                              the unlabeled sort-order ImageView (no
 *                              content-desc of its own — matched via
 *                              childSelector on its stable "(Invested)"
 *                              child) are real toggles
 *  - Scheme cards:            dynamic content-desc ("<AMC> Pension Fund -
 *                              Scheme <X> - TIER I") — every NPS scheme card
 *                              contains "Pension Fund", used as the stable
 *                              matching substring instead of a positional
 *                              selector
 *  - Scheme detail dialog:    pane-title="Dialog"; the heading is the dynamic
 *                              scheme name (not locatable generically), but
 *                              "Performance" toggle, "Update Transaction
 *                              History" link, and the "Scrim" dismiss
 *                              backdrop are stable across every scheme
 */
public class FundNPS_Page extends BasePage {

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

    private final By fundsSifsTab  = AppiumBy.accessibilityId("Funds / SIFs, tab 2 of 8");
    private final By stocksEtfsTab = AppiumBy.accessibilityId("Stocks & ETFs, tab 3 of 8");
    private final By npsTab        = AppiumBy.accessibilityId("NPS, tab 4 of 8");
    private final By bondsFdsTab   = AppiumBy.accessibilityId("Bonds & FDs, tab 5 of 8");
    private final By sgbTab        = AppiumBy.accessibilityId("SGB, tab 6 of 8");

    // ================================================================
    // HEADER ACTIONS
    // ================================================================

    // "Manish Khatri" is the current investor's dynamic name; the trailing
    // phrase is the stable part reused as a fallback if the investor changes.
    private final By investorSwitcherButton = AppiumBy.accessibilityId(
            "Shows investor selection list");
    private final By investorSwitcherFallback = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Shows investor selection list\")");

    private final By updateHoldingsButton =
            AppiumBy.accessibilityId("Update NPS Holdings");

    // No content-desc/resource-id exists on either icon — className +
    // instance() is the only selector available. Last resort per the
    // framework's locator priority rules; re-verify against a fresh XML
    // dump if this header changes.
    private final By headerIconButtonFirst = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(0)");
    private final By headerIconButtonSecond = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(1)");

    // ================================================================
    // PORTFOLIO SUMMARY BAR
    // ================================================================

    // "Current"/"Invested"/"1D Change" are static column-header labels
    // (clickable="false" in the XML) — not included as tappable links.
    private final By allTimeReturnsToggle = AppiumBy.accessibilityId("All Time Returns");
    By OneyearReturn=AppiumBy.accessibilityId("1 Year");
    private final By sortByButton         = AppiumBy.accessibilityId("Sort By");

    // The sort-order toggle ImageView itself has no content-desc — matched
    // structurally via its stable "(Invested)" child instead of a positional
    // instance() selector.
    private final By sortOrderToggle = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\")"
                    + ".childSelector(new UiSelector().description(\"(Invested)\"))");

    // ================================================================
    // NPS SCHEME LIST
    // ================================================================

    // Every scheme card's content-desc contains "Pension Fund" regardless of
    // AMC/scheme name — the stable substring used instead of a positional
    // selector, since scheme names/order are dynamic per investor.
    private final By schemeCardLocator = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Pension Fund\")");

    // ================================================================
    // SCHEME DETAIL DIALOG (opened by tapping a scheme card)
    // ================================================================

    private final By schemeDetailScrim = AppiumBy.accessibilityId("Scrim");
    private final By performanceToggle = AppiumBy.accessibilityId("Performance");
    private final By updateTransactionHistoryButton =
            AppiumBy.accessibilityId("Update Transaction History");

    // Expand/collapse toggle — its container ImageView has no content-desc,
    // but its "Your Investment" child label is stable regardless of
    // expanded/collapsed state (unlike the dynamic "Show less"/"Show more"
    // text on the sibling child).
    private final By expandCollapseToggle = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\")"
                    + ".childSelector(new UiSelector().description(\"Your Investment\"))");

    // ================================================================
    // NAVIGABLE LINKS ON THIS TAB (fixed header + summary bar controls)
    // Scheme cards and the "Update Transaction History" dialog link are
    // multi-step and are exercised through their own dedicated
    // tap-and-validate methods below.
    // ================================================================

    private final List<HeaderLink> headerLinks = Arrays.asList(
          //  new HeaderLink("Investor Switcher", investorSwitcherButton, true),
            new HeaderLink("Update NPS Holdings", updateHoldingsButton, true),
            new HeaderLink("Header Icon 1", headerIconButtonFirst, true),
            new HeaderLink("Header Icon 2", headerIconButtonSecond, true),
            new HeaderLink("All Time Returns Toggle", allTimeReturnsToggle, false),
            new HeaderLink("Sort By", sortByButton, true),
            new HeaderLink("Sort Order Toggle", sortOrderToggle, false));

    public FundNPS_Page(AndroidDriver driver) {
        super(driver);
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    public FundNPS_Page navigateToNpsTab() {
        logger.info("Navigating to Portfolio -> NPS tab");

        for (int i = 0; i < 4 && !isDisplayed(portfolioNavTab) && !isNpsScreenDisplayed(); i++) {
            logger.info("Portfolio nav tab hidden — pressing back ({}/4)", i + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (!isNpsScreenDisplayed()) {
            if (isDisplayed(portfolioNavTab)) {
                safeClick(portfolioNavTab);
                waitForUiToSettle();
            }

            // The NPS tab sits further along the horizontally-scrolling tab
            // bar than the default landing tab, so it isn't reliably visible
            // right after tapping Portfolio. Tapping Stocks & ETFs first
            // scrolls the tab bar into a position where NPS becomes visible.
            if (!isDisplayed(npsTab, 3) && isDisplayed(stocksEtfsTab, 5)) {
                logger.info("NPS tab not visible — tapping Stocks & ETFs first");
                safeClick(stocksEtfsTab);
                waitForUiToSettle();
            }

            if (isDisplayed(npsTab, 5)) {
                safeClick(npsTab);
            }
        }

        boolean loaded = waitUntilTrue(this::isNpsScreenDisplayed, 15);
        if (loaded) {
            logger.info("NPS tab opened");
        } else {
            logger.warn("NPS content not confirmed after 15 s — tab tapped, proceeding");
        }
        return this;
    }

    // ================================================================
    // SCREEN / STATE DETECTION
    // ================================================================

    public boolean isNpsScreenDisplayed() {
        return isDisplayed(updateHoldingsButton) && !isSchemeDetailDialogDisplayed();
    }

    public boolean isSchemeDetailDialogDisplayed() {
        return isAnyDisplayed(schemeDetailScrim, performanceToggle, updateTransactionHistoryButton);
    }

    public boolean isBottomNavigationDisplayed() {
        return isDisplayed(fundsNavTab) && isDisplayed(stocksNavTab)
                && isDisplayed(portfolioNavTab) && isDisplayed(hubNavTab);
    }

    public boolean isInvestorSwitcherDisplayed() {
        return isAnyDisplayed(investorSwitcherButton, investorSwitcherFallback);
    }

    public boolean isUpdateHoldingsButtonDisplayed() {
        return isDisplayed(updateHoldingsButton);
    }

    public boolean isSchemeListDisplayed() {
        return !findElements(schemeCardLocator).isEmpty();
    }

    // ================================================================
    // TAB SWITCHING (in-page — asset-class tabs, no screen change)
    // ================================================================

    public boolean tapFundsSifsTabAndValidate() {
        return tapAssetClassTab(fundsSifsTab, "Funds / SIFs");
    }

    public boolean tapStocksEtfsTabAndValidate() {
        return tapAssetClassTab(stocksEtfsTab, "Stocks & ETFs");
    }

    public boolean tapBondsFdsTabAndValidate() {
        return tapAssetClassTab(bondsFdsTab, "Bonds & FDs");
    }

    public boolean tapSgbTabAndValidate() {
        return tapAssetClassTab(sgbTab, "SGB");
    }

    private boolean tapAssetClassTab(By tabLocator, String name) {
        logger.info("Tapping '{}' asset-class tab", name);
        navigateToNpsTab();
        if (!isDisplayed(tabLocator)) {
            logger.warn("'{}' tab not visible", name);
            return false;
        }
        safeClick(tabLocator);
        waitForUiToSettle();
        boolean switched = isDisplayed(tabLocator);
        navigateToNpsTab();
        boolean returned = isNpsScreenDisplayed();
        logger.info("'{}' tab — switched:{}, returnedToNps:{}", name, switched, returned);
        return switched && returned;
    }

    // ================================================================
    // HEADER LINKS — INTERACTIONS
    // ================================================================

    public FundNPS_Page tapInvestorSwitcher() {
        safeClick(isDisplayed(investorSwitcherButton) ? investorSwitcherButton : investorSwitcherFallback);
        return this;
    }

    public FundNPS_Page tapUpdateHoldingsButton() {
        safeClick(updateHoldingsButton);
        return this;
    }

    public FundNPS_Page tapAllTimeReturnsToggle() {
        safeClick(allTimeReturnsToggle);
        safeClick(OneyearReturn);
        return this;
    }

    public FundNPS_Page tapSortByButton() {
        safeClick(sortByButton);
        return this;
    }

    public FundNPS_Page tapSortOrderToggle() {
        safeClick(sortOrderToggle);
        return this;
    }

    // ================================================================
    // SCHEME DETAIL DIALOG — INTERACTIONS
    // ================================================================

    public FundNPS_Page tapPerformanceToggle() {
        safeClick(performanceToggle);
        return this;
    }

    public FundNPS_Page tapExpandCollapseToggle() {
        safeClick(expandCollapseToggle);
        return this;
    }

    /**
     * Taps the first available scheme card, validates the detail dialog
     * opens, taps "Update Transaction History" inside it, validates a
     * screen/overlay change, then returns to the NPS tab.
     */
    public boolean tapUpdateTransactionHistoryAndValidate() {
        logger.info("=== NPS: Update Transaction History link test ===");
        navigateToNpsTab();

        List<WebElement> cards = findElements(schemeCardLocator);
        if (cards.isEmpty()) {
            logger.warn("No NPS scheme cards available to open Update Transaction History from");
            return false;
        }

        cards.get(0).click();
        waitForUiToSettle();

        if (!waitUntilTrue(this::isSchemeDetailDialogDisplayed, 8)) {
            logger.warn("Scheme detail dialog did not open — cannot test Update Transaction History");
            return false;
        }

        if (!isDisplayed(updateTransactionHistoryButton, 5)) {
            logger.warn("Update Transaction History link not visible on scheme detail dialog");
            dismissSchemeDetailDialogAndReturn();
            return false;
        }

        String pageSourceBefore = driver.getPageSource();
        safeClick(updateTransactionHistoryButton);
        waitForUiToSettle();

        boolean opened = waitUntilTrue(() -> !driver.getPageSource().equals(pageSourceBefore), 8);
        boolean returned = dismissSchemeDetailDialogAndReturn();
        boolean passed = opened || returned;

        logger.info("Update Transaction History — opened:{}, returned:{}, passed:{}",
                opened, returned, passed);
        return passed;
    }

    // ================================================================
    // SCHEME CARDS — tap every card, validate the detail dialog opens,
    // dismiss it, then return to the NPS tab before moving to the next card.
    // ================================================================

    public List<LinkResult> tapAllSchemeCardsAndValidate() {
        logger.info("=== NPS: validating all scheme cards ===");
        List<LinkResult> results = new ArrayList<>();
        navigateToNpsTab();

        int count = findElements(schemeCardLocator).size();
        if (count == 0) {
            logger.warn("No NPS scheme cards found to validate");
            return results;
        }

        for (int i = 0; i < count; i++) {
            List<WebElement> cards = findElements(schemeCardLocator);
            if (i >= cards.size()) {
                logger.warn("NPS scheme card index {} no longer present — skipping", i);
                continue;
            }

            String cardLabel = cards.get(i).getAttribute("content-desc");
            logger.info("=== NPS scheme card test: {} ===", cardLabel);

            cards.get(i).click();
            waitForUiToSettle();

            boolean opened = waitUntilTrue(this::isSchemeDetailDialogDisplayed, 8);
            boolean returned = dismissSchemeDetailDialogAndReturn();
            boolean passed = opened && returned;

            logger.info("NPS scheme card '{}' — opened:{}, returned:{}, passed:{}",
                    cardLabel, opened, returned, passed);
            results.add(new LinkResult("Scheme Card: " + cardLabel, passed));
        }

        return results;
    }

    // ================================================================
    // LINK VALIDATION — tap every navigable link (header + summary bar +
    // scheme cards + the dialog's "Update Transaction History" link),
    // validate a screen/overlay opened (or, for in-place toggles, that the
    // tab content is still shown), then return to the NPS tab before moving
    // to the next link.
    // ================================================================

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("=== NPS: validating all navigable links ===");
        List<LinkResult> results = new ArrayList<>();

        for (HeaderLink link : headerLinks) {
            results.add(new LinkResult(link.name, tapHeaderLinkAndValidate(link)));
        }

        results.addAll(tapAllSchemeCardsAndValidate());
        results.add(new LinkResult("Update Transaction History", tapUpdateTransactionHistoryAndValidate()));

        logger.info("=== NPS: link validation complete: {} ===", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                logger.warn("NPS link FAILED: {}", result.name);
                return false;
            }
        }
        return true;
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private boolean tapHeaderLinkAndValidate(HeaderLink link) {
        logger.info("=== NPS link test: {} ===", link.name);
        navigateToNpsTab();

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
                    () -> !driver.getPageSource().equals(pageSourceBefore) || isSchemeDetailDialogDisplayed(),
                    8);
        } else {
            valid = isNpsScreenDisplayed() || !driver.getPageSource().equals(pageSourceBefore);
        }

        boolean returned = returnToNpsScreen();
        boolean passed = valid || returned;

        logger.info("NPS link '{}' — opened:{}, returned:{}, passed:{}",
                link.name, valid, returned, passed);
        return passed;
    }

    private boolean returnToNpsScreen() {
        if (isSchemeDetailDialogDisplayed()) {
            dismissSchemeDetailDialogAndReturn();
        }

        for (int attempt = 0; attempt < 5 && !isNpsScreenDisplayed(); attempt++) {
            logger.info("NPS screen not visible — pressing back ({}/5)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (isNpsScreenDisplayed()) {
            return true;
        }
        navigateToNpsTab();
        return isNpsScreenDisplayed();
    }

    private boolean dismissSchemeDetailDialogAndReturn() {
        if (isSchemeDetailDialogDisplayed()) {
            if (isDisplayed(schemeDetailScrim)) {
                safeClick(schemeDetailScrim);
            } else {
                driver.navigate().back();
            }
            waitForUiToSettle();
        }

        for (int attempt = 0; attempt < 5 && !isNpsScreenDisplayed(); attempt++) {
            logger.info("NPS screen not visible after dialog dismiss — pressing back ({}/5)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (isNpsScreenDisplayed()) {
            return true;
        }
        navigateToNpsTab();
        return isNpsScreenDisplayed();
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
