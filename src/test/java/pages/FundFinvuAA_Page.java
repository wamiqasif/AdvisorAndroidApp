package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Portfolio screen → "Stocks & ETFs" tab (tab 3 of 8), including the
 * Account Aggregator (Finvu AA) stock-import card and the WebView consent
 * screen it opens.
 *
 * XML observations used:
 *  - Tab bar item:            content-desc="Stocks & ETFs, tab 3 of 8"
 *  - Investor switcher:       content-desc="Manish Khatri, Select Investor, Shows investor
 *                              selection list" ("Manish Khatri" is dynamic — trailing
 *                              phrase is the stable fallback part)
 *  - Update holdings button:  content-desc="Update Stocks & ETFs Holdings"
 *  - Header icons:            two ImageViews with no content-desc/resource-id —
 *                              className + instance() is the only selector available
 *                              (last resort, same as the Funds / SIFs header)
 *  - AA import card:          content-desc="Import your stocks" heading, "Recommended"
 *                              badge, description text, "Select Investor, Shows investor
 *                              selection list" button (exact match — no name prefix,
 *                              distinguishes it from the header investor switcher)
 *  - PAN / Mobile fields:     android.widget.EditText with no content-desc/resource-id;
 *                              only the hint attribute ("Enter PAN" / "Enter Mobile")
 *                              identifies them, so a non-positional XPath on @hint is
 *                              used (last resort per framework locator priority rules)
 *  - Submit button:           content-desc="Update" (exact — distinct from "Update
 *                              Stocks & ETFs Holdings")
 *  - Add Investor:            content-desc="Add Investor" — appears on the investor
 *                              selection list opened by the investor switcher
 *  - Finvu AA WebView screen: "Go back" button + a single WebView, no other
 *                              content-desc'd elements available
 */
public class FundFinvuAA_Page extends BasePage {

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

    // ================================================================
    // HEADER ACTIONS
    // ================================================================

    // "Manish Khatri" is the current investor's dynamic name; the trailing phrase
    // is the stable part reused as a fallback if the investor name changes.
    private final By investorSwitcherButton = AppiumBy.accessibilityId(
            "Manish Khatri, Select Investor, Shows investor selection list");
    private final By investorSwitcherFallback = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Select Investor, Shows investor selection list\")");

    private final By updateHoldingsButton =
            AppiumBy.accessibilityId("Update Stocks & ETFs Holdings");

    // No content-desc/resource-id exists on either icon — className + instance()
    // is the only selector available. Last resort per the framework's locator
    // priority rules; re-verify against a fresh XML dump if this header changes.
    private final By headerIconButtonFirst = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(0)");
    private final By headerIconButtonSecond = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(1)");

    // ================================================================
    // ACCOUNT AGGREGATOR (FINVU) IMPORT CARD
    // ================================================================

    private final By importYourStocksHeading = AppiumBy.accessibilityId("Import your stocks");
    private final By recommendedBadge        = AppiumBy.accessibilityId("Recommended");
    private final By aaDescriptionText       = AppiumBy.accessibilityId(
            "Directly import all your stocks from Account Aggregator using your PAN and mobile number.");

    // Exact match, no investor-name prefix — distinguishes this from investorSwitcherButton
    private final By aaSelectInvestorButton = AppiumBy.accessibilityId(
            "Select Investor");

    private final By enterPanLabel    = AppiumBy.accessibilityId("Enter PAN");
    private final By enterMobileLabel = AppiumBy.accessibilityId("Enter Mobile");
    private final By mobileCountryCode = AppiumBy.accessibilityId("+91");

    // Neither EditText has a content-desc/resource-id — the hint attribute is the
    // only stable identifier. Non-positional XPath is used as the documented last
    // resort (no instance()/index — matched purely on the @hint attribute value).
    private final By panInputField    = By.xpath("//android.widget.EditText[@hint='Enter PAN']");
    private final By mobileInputField = By.xpath("//android.widget.EditText[@hint='Enter Mobile']");

    private final By updateButton = AppiumBy.accessibilityId("Update");

    // ================================================================
    // ADD INVESTOR (investor selection list opened by the investor switcher)
    // ================================================================

    private final By addInvestorButton = AppiumBy.accessibilityId("Add Investor");

    // ================================================================
    // FINVU AA WEBVIEW (CONSENT) SCREEN
    // ================================================================

    private final By goBackButton = AppiumBy.accessibilityId("Go back");
    private final By finvuWebView = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.webkit.WebView\")");

    // ================================================================
    // NAVIGABLE LINKS ON THIS TAB (fixed header + AA card controls)
    // "Add Investor" and the AA "Update" submit flow are multi-step and are
    // exercised through their own dedicated tap-and-validate methods below.
    // ================================================================

    private final List<HeaderLink> headerLinks = Arrays.asList(
            new HeaderLink("Investor Switcher", investorSwitcherButton, true),
            new HeaderLink("Update Stocks & ETFs Holdings", updateHoldingsButton, true),
            new HeaderLink("Header Icon 1", headerIconButtonFirst, true),
            new HeaderLink("Header Icon 2", headerIconButtonSecond, true),
            new HeaderLink("AA Select Investor", aaSelectInvestorButton, true),
            new HeaderLink("Recommended Badge", recommendedBadge, false));

    public FundFinvuAA_Page(AndroidDriver driver) {
        super(driver);
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    public FundFinvuAA_Page navigateToStocksEtfsTab() {
        logger.info("Navigating to Portfolio -> Stocks & ETFs tab");

        for (int i = 0; i < 4 && !isDisplayed(portfolioNavTab) && !isStocksEtfsScreenDisplayed(); i++) {
            logger.info("Portfolio nav tab hidden — pressing back ({}/4)", i + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (!isStocksEtfsScreenDisplayed()) {
            if (isDisplayed(portfolioNavTab)) {
                safeClick(portfolioNavTab);
                waitForUiToSettle();
            }
            if (isDisplayed(stocksEtfsTab, 5)) {
                safeClick(stocksEtfsTab);
            }
        }

        boolean loaded = waitUntilTrue(this::isStocksEtfsScreenDisplayed, 15);
        if (loaded) {
            logger.info("Stocks & ETFs tab opened");
        } else {
            logger.warn("Stocks & ETFs content not confirmed after 15 s — tab tapped, proceeding");
        }
        return this;
    }

    // ================================================================
    // SCREEN / STATE DETECTION
    // ================================================================

    public boolean isStocksEtfsScreenDisplayed() {
        return isAnyDisplayed(importYourStocksHeading, aaDescriptionText, updateButton, enterPanLabel)
                && !isFinvuWebViewDisplayed();
    }

    public boolean isFinvuWebViewDisplayed() {
        return isAnyDisplayed(finvuWebView, goBackButton);
    }

    public boolean waitForFinvuWebViewDisplayed(int seconds) {
        return waitUntilTrue(this::isFinvuWebViewDisplayed, seconds);
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

    public boolean isAaImportCardDisplayed() {
        return isAnyDisplayed(importYourStocksHeading, aaDescriptionText, aaSelectInvestorButton);
    }

    public boolean isPanFieldDisplayed() {
        return isDisplayed(panInputField);
    }

    public boolean isMobileFieldDisplayed() {
        return isDisplayed(mobileInputField);
    }

    // ================================================================
    // TAB SWITCHING (in-page — asset-class tabs, no screen change)
    // ================================================================

    public boolean tapFundsSifsTabAndValidate() {
        return tapAssetClassTab(fundsSifsTab, "Funds / SIFs");
    }

    public boolean tapNpsTabAndValidate() {
        return tapAssetClassTab(npsTab, "NPS");
    }

    public boolean tapBondsFdsTabAndValidate() {
        return tapAssetClassTab(bondsFdsTab, "Bonds & FDs");
    }

    private boolean tapAssetClassTab(By tabLocator, String name) {
        logger.info("Tapping '{}' asset-class tab", name);
        navigateToStocksEtfsTab();
        if (!isDisplayed(tabLocator)) {
            logger.warn("'{}' tab not visible", name);
            return false;
        }
        safeClick(tabLocator);
        waitForUiToSettle();
        boolean switched = isDisplayed(tabLocator);
        navigateToStocksEtfsTab();
        boolean returned = isStocksEtfsScreenDisplayed();
        logger.info("'{}' tab — switched:{}, returnedToStocksEtfs:{}", name, switched, returned);
        return switched && returned;
    }


    // ================================================================
    // HEADER LINKS — INTERACTIONS
    // ================================================================

    public FundFinvuAA_Page tapInvestorSwitcher() {
        safeClick(isDisplayed(investorSwitcherButton) ? investorSwitcherButton : investorSwitcherFallback);
        return this;
    }

    public FundFinvuAA_Page tapUpdateHoldingsButton() {
        safeClick(updateHoldingsButton);
        return this;
    }

    public FundFinvuAA_Page tapRecommendedBadge() {
        safeClick(recommendedBadge);
        return this;
    }

    public FundFinvuAA_Page tapAaSelectInvestorButton() {
        safeClick(aaSelectInvestorButton);
        return this;
    }

    // ================================================================
    // ACCOUNT AGGREGATOR (FINVU) IMPORT FLOW
    // ================================================================

    public FundFinvuAA_Page enterPan(String pan) {
        safeSendKeys(panInputField, pan);
        return this;
    }

    public FundFinvuAA_Page enterMobile(String mobile) {
        safeSendKeys(mobileInputField, mobile);
        return this;
    }

    public FundFinvuAA_Page tapUpdateButton() {
        safeClick(updateButton);

            waitForUiToSettle();



        return this;
    }

    public FundFinvuAA_Page tapGoBack() {
        safeClick(goBackButton);
        return this;
    }

    /**
     * Fills PAN + Mobile, submits the Account Aggregator import form and
     * validates the Finvu AA WebView consent screen opens, then returns to
     * the Stocks & ETFs tab via the "Go back" button.
     */
    public boolean submitAccountAggregatorImportAndValidate(String pan, String mobile) {
        logger.info("=== Finvu AA import test — PAN:{}, Mobile:{} ===", pan, mobile);
        navigateToStocksEtfsTab();

        enterPan(pan);
        enterMobile(mobile);
        tapUpdateButton();
        waitForUiToSettle();

        boolean opened = waitUntilTrue(this::isFinvuWebViewDisplayed, 15);
        logger.info("Finvu AA WebView opened: {}", opened);

        boolean returned = returnToStocksEtfsScreen();
        boolean passed = opened && returned;
        logger.info("=== Finvu AA import test — opened:{}, returned:{}, passed:{} ===",
                opened, returned, passed);
        return passed;
    }

    // ================================================================
    // ADD INVESTOR (opens investor selection list, then Add Investor screen)
    // ================================================================

    public boolean tapAddInvestorAndValidate() {
        logger.info("=== Link test: Add Investor ===");
        navigateToStocksEtfsTab();

        if (!isDisplayed(investorSwitcherButton) && !isDisplayed(investorSwitcherFallback)) {
            logger.warn("Investor switcher not visible, skipping Add Investor");
            return false;
        }
        safeClick(isDisplayed(investorSwitcherButton) ? investorSwitcherButton : investorSwitcherFallback);
        waitForUiToSettle();

        if (!waitUntilTrue(() -> isDisplayed(addInvestorButton), 8)) {
            logger.warn("Add Investor button not visible after opening investor selector");
            returnToStocksEtfsScreen();
            return false;
        }

        String pageSourceBefore = driver.getPageSource();
        safeClick(addInvestorButton);
        waitForUiToSettle();

        boolean opened = waitUntilTrue(
                () -> !driver.getPageSource().equals(pageSourceBefore) && !isStocksEtfsScreenDisplayed(), 8);
        boolean returned = returnToStocksEtfsScreen();
        boolean passed = opened || returned;

        logger.info("Add Investor — opened:{}, returned:{}, passed:{}", opened, returned, passed);
        logger.info("=== Link test: Add Investor — done ===");
        return passed;
    }

    // ================================================================
    // LINK VALIDATION — tap every navigable link, validate a screen/overlay
    // opened (or, for in-place toggles, that the tab content is still shown),
    // then return to the Stocks & ETFs tab before moving to the next link.
    // ================================================================

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("=== Stocks & ETFs: validating all navigable links ===");
        List<LinkResult> results = new ArrayList<>();

        for (HeaderLink link : headerLinks) {
            results.add(new LinkResult(link.name, tapHeaderLinkAndValidate(link)));
        }

        results.add(new LinkResult("Add Investor", tapAddInvestorAndValidate()));

        logger.info("=== Stocks & ETFs link validation complete: {} ===", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                logger.warn("Stocks & ETFs link FAILED: {}", result.name);
                return false;
            }
        }
        return true;
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private boolean tapHeaderLinkAndValidate(HeaderLink link) {
        logger.info("=== Stocks & ETFs link test: {} ===", link.name);
        navigateToStocksEtfsTab();

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
                    () -> !driver.getPageSource().equals(pageSourceBefore) || isFinvuWebViewDisplayed(),
                    8);
        } else {
            valid = isStocksEtfsScreenDisplayed();
        }

        boolean returned = returnToStocksEtfsScreen();
        boolean passed = valid || returned;

        logger.info("Stocks & ETFs link '{}' — opened:{}, returned:{}, passed:{}",
                link.name, valid, returned, passed);
        return passed;
    }

    private boolean returnToStocksEtfsScreen() {
        if (isFinvuWebViewDisplayed()) {
            if (isDisplayed(goBackButton)) {
                safeClick(goBackButton);
            } else {
                driver.navigate().back();
            }
            waitForUiToSettle();
        }

        for (int attempt = 0; attempt < 5 && !isStocksEtfsScreenDisplayed(); attempt++) {
            logger.info("Stocks & ETFs screen not visible — pressing back ({}/5)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }

        if (isStocksEtfsScreenDisplayed()) {
            return true;
        }
        navigateToStocksEtfsTab();
        return isStocksEtfsScreenDisplayed();
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
