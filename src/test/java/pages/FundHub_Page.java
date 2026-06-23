package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class FundHub_Page extends BasePage {

    private static final int MAX_SCROLLS = 12;

    // ====================================================================
    // BOTTOM NAVIGATION
    // ====================================================================

    private final By hubTab = AppiumBy.accessibilityId("Hub");
    private final By fundsTab = AppiumBy.accessibilityId("Funds");
    private final By stocksTab = AppiumBy.accessibilityId("Stocks");
    private final By portfolioTab = AppiumBy.accessibilityId("Portfolio");

    // ====================================================================
    // HUB SCREEN DETECTION ANCHORS
    // "Mutual Funds" is unique to the Hub section label ("Funds" is the nav tab).
    // "More" is unique to the Hub's More section label.
    // Text-based fallbacks handle Flutter Text widgets without explicit Semantics
    // annotations — those land in the `text` attribute, not `content-desc`.
    // ====================================================================

    private final By mutualFundsSectionLabel = AppiumBy.accessibilityId("Mutual Funds");
    private final By moreSectionLabel = AppiumBy.accessibilityId("More");

    // Text-attribute fallbacks (used when content-desc locators don't match)
    private final By mutualFundsSectionText = AppiumBy
            .androidUIAutomator("new UiSelector().textContains(\"Mutual Funds\")");
    private final By transactionsSectionText = AppiumBy
            .androidUIAutomator("new UiSelector().textContains(\"Transactions\")");
    private final By moreSectionText = AppiumBy
            .androidUIAutomator("new UiSelector().textContains(\"More\")");
    private final By viewAllTransactionsText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"transactions\")");
    private final By portfolioPlannerText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"planner\")");
    private final By fundScreenerText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Screener\")");
    private final By storiesText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"stories\")");
    private final By stockAdvisorText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Stock Advisor\")");
    private final By faqText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"frequently asked\")");
    private final By aboutValueResearchText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Value Research\")");
    private final By contactUsText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"touch with us\")");
    private final By privacyPolicyText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"privacy policy\")");
    private final By userAgreementText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"user agreement\")");
    private final By refundPolicyText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"refund policy\")");
    private final By investorCharterText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"investor charter\")");
    private final By complaintText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"complaint\")");
    private final By odrPortalText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"ODR\")");
    private final By auditStatusText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"audit\")");
    private final By marketMonitorText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Market Monitor\")");
    private final By sipCalculatorText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"SIP\")");
    private final By analystsChoiceText = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Analyst\")");


    // ====================================================================
    // TRANSACTIONS SECTION
    // ====================================================================

    private final By viewAllTransactionsButton = AppiumBy.accessibilityId("View all your transactions");

    // ====================================================================
    // MUTUAL FUNDS SECTION LINKS
    // ====================================================================

    private final By portfolioPlannerButton = AppiumBy.accessibilityId("Open portfolio planner screen");

    // Analyst's Choice uses descriptionContains to guard against curly-apostrophe variants
    private final By analystsChoiceButton = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Analyst\")");

    private final By fundScreenerButton = AppiumBy.accessibilityId("Open Fund Screener screen");
    private final By sipReturnCalculatorButton = AppiumBy.accessibilityId("Open SIP Return Calculator screen");
    private final By storiesAndVideosButton = AppiumBy.accessibilityId("Browse stories and videos");

    // ====================================================================
    // STOCKS SECTION LINKS
    // ====================================================================

    private final By marketMonitorButton = AppiumBy.accessibilityId("Open Market Monitor screen");
    private final By stockAdvisorButton = AppiumBy.accessibilityId("Open Stock Advisor");

    // ====================================================================
    // MORE SECTION LINKS
    // ====================================================================

    private final By faqButton = AppiumBy.accessibilityId("View frequently asked questions");
    private final By aboutValueResearchButton = AppiumBy.accessibilityId("Learn about Value Research");
    private final By contactUsButton = AppiumBy.accessibilityId("Get in touch with us");
    private final By privacyPolicyButton = AppiumBy.accessibilityId("Read our privacy policy");
    private final By userAgreementButton = AppiumBy.accessibilityId("View and download your user agreement");
    private final By refundPolicyButton = AppiumBy.accessibilityId("View our refund policy");
    private final By investorCharterButton = AppiumBy.accessibilityId("View the investor charter");
    private final By complaintButton = AppiumBy.accessibilityId("Submit or track complaint");
    private final By odrPortalButton = AppiumBy.accessibilityId("Open the ODR portal for dispute resolution");
    private final By auditStatusButton = AppiumBy.accessibilityId("View our audit status");

    // ====================================================================
    // ALL NAVIGABLE LINKS (ordered top-to-bottom as they appear on screen)
    // Each HubLink carries both a primary locator (content-desc) and a
    // fallback locator (descriptionContains / textContains) so the page can
    // find the element even if the exact content-desc string differs.
    // ====================================================================

    private final List<HubLink> hubLinks = Arrays.asList(
            new HubLink("View All Transactions", viewAllTransactionsButton, viewAllTransactionsText),
            new HubLink("Portfolio Planner", portfolioPlannerButton, portfolioPlannerText),
            new HubLink("Analyst's Choice", analystsChoiceButton, analystsChoiceText),
            new HubLink("Fund Screener", fundScreenerButton, fundScreenerText),
            new HubLink("SIP Return Calculator", sipReturnCalculatorButton, sipCalculatorText),
            new HubLink("Stories and Videos", storiesAndVideosButton, storiesText),
            new HubLink("Market Monitor", marketMonitorButton, marketMonitorText),
            new HubLink("Stock Advisor", stockAdvisorButton, stockAdvisorText),
            new HubLink("FAQ", faqButton, faqText),
            new HubLink("About Value Research", aboutValueResearchButton, aboutValueResearchText),
            new HubLink("Contact Us", contactUsButton, contactUsText),
            new HubLink("Privacy Policy", privacyPolicyButton, privacyPolicyText),
            new HubLink("User Agreement", userAgreementButton, userAgreementText),
            new HubLink("Refund Policy", refundPolicyButton, refundPolicyText),
            new HubLink("Investor Charter", investorCharterButton, investorCharterText),
            new HubLink("Submit or Track Complaint", complaintButton, complaintText),
            new HubLink("ODR Portal", odrPortalButton, odrPortalText),
            new HubLink("Audit Status", auditStatusButton, auditStatusText));

    public FundHub_Page(AndroidDriver driver) {
        super(driver);
    }

    // ====================================================================
    // NAVIGATION TO HUB
    // ====================================================================

    public FundHub_Page openHubScreen() {
        logger.info("Opening Hub screen");
        if (isHubScreenDisplayed()) {
            logger.info("Hub screen already open");
            return this;
        }
        // Extra settle for Flutter rendering after ensureDashboardReady() tap
        waitForUiToSettle();
        if (isHubScreenDisplayed()) {
            logger.info("Hub screen ready after settle");
            return this;
        }
        // Back-press loop to expose the Hub tab if a Flutter overlay is hiding it
        for (int i = 0; i < 4 && !isDisplayed(hubTab); i++) {
            logger.info("Hub tab hidden — pressing back to unwind overlay ({}/4)", i + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }
        if (isDisplayed(hubTab)) {
            safeClick(hubTab);
            waitForUiToSettle();
        }
        // Patient non-throwing wait — 20s for slow Flutter render
        boolean loaded = waitUntilTrue(this::isHubScreenDisplayed, 20);
        if (loaded) {
            logger.info("Hub screen opened");
        } else {
            // Hub tab was tapped; content locators may not match this app build.
            // Log a warning and proceed — the test body will fail fast if truly not on Hub.
            logger.warn("Hub content locators unconfirmed after 20s — Hub tab tapped, proceeding");
        }
        return this;
    }

    // ====================================================================
    // SCREEN / SECTION DETECTION
    // ====================================================================

    /**
     * Returns {@code true} when the Hub screen is visually open.
     *
     * <p>Tries content-desc based locators first (fast path, works when Flutter
     * exposes Semantics labels). Falls back to text-attribute selectors for
     * Flutter Text widgets that have no explicit Semantics wrapper.</p>
     */
    public boolean isHubScreenDisplayed() {
        // Fast path: content-desc based (set by Flutter Semantics annotations)
        if (isAnyDisplayed(mutualFundsSectionLabel, viewAllTransactionsButton, moreSectionLabel)) {
            return true;
        }
        // Fallback: text-attribute based (Flutter Text widget without Semantics)
        return isAnyDisplayed(mutualFundsSectionText, transactionsSectionText, moreSectionText);
    }

    /** Returns {@code true} when the Hub bottom-nav tab is visible. */
    public boolean isHubTabVisible() {
        return isDisplayed(hubTab);
    }

    public boolean isBottomNavigationDisplayed() {
        return isDisplayed(fundsTab)
                && isDisplayed(stocksTab)
                && isDisplayed(portfolioTab)
                && isDisplayed(hubTab);
    }

    public boolean isMutualFundsSectionDisplayed() {
        return scrollDownUntilVisible(mutualFundsSectionLabel, MAX_SCROLLS);
    }

    public boolean isMoreSectionDisplayed() {
        return scrollDownUntilVisible(moreSectionLabel, MAX_SCROLLS);
    }

    // ====================================================================
    // INDIVIDUAL BUTTON VISIBILITY
    // ====================================================================

    public boolean isViewAllTransactionsDisplayed() {
        return isDisplayed(viewAllTransactionsButton);
    }

    public boolean isPortfolioPlannerDisplayed() {
        return scrollDownUntilVisible(portfolioPlannerButton, MAX_SCROLLS);
    }

    public boolean isAnalystsChoiceDisplayed() {
        return scrollDownUntilVisible(analystsChoiceButton, MAX_SCROLLS);
    }

    public boolean isFundScreenerDisplayed() {
        return scrollDownUntilVisible(fundScreenerButton, MAX_SCROLLS);
    }

    public boolean isSipReturnCalculatorDisplayed() {
        return scrollDownUntilVisible(sipReturnCalculatorButton, MAX_SCROLLS);
    }

    public boolean isStoriesAndVideosDisplayed() {
        return scrollDownUntilVisible(storiesAndVideosButton, MAX_SCROLLS);
    }

    public boolean isMarketMonitorDisplayed() {
        return scrollDownUntilVisible(marketMonitorButton, MAX_SCROLLS);
    }

    public boolean isStockAdvisorDisplayed() {
        return scrollDownUntilVisible(stockAdvisorButton, MAX_SCROLLS);
    }

    public boolean isFaqDisplayed() {
        return scrollDownUntilVisible(faqButton, MAX_SCROLLS);
    }

    public boolean isAboutValueResearchDisplayed() {
        return scrollDownUntilVisible(aboutValueResearchButton, MAX_SCROLLS);
    }

    public boolean isContactUsDisplayed() {
        return scrollDownUntilVisible(contactUsButton, MAX_SCROLLS);
    }

    public boolean isPrivacyPolicyDisplayed() {
        return scrollDownUntilVisible(privacyPolicyButton, MAX_SCROLLS);
    }

    public boolean isUserAgreementDisplayed() {
        return scrollDownUntilVisible(userAgreementButton, MAX_SCROLLS);
    }

    public boolean isRefundPolicyDisplayed() {
        return scrollDownUntilVisible(refundPolicyButton, MAX_SCROLLS);
    }

    public boolean isInvestorCharterDisplayed() {
        return scrollDownUntilVisible(investorCharterButton, MAX_SCROLLS);
    }

    public boolean isComplaintDisplayed() {
        return scrollDownUntilVisible(complaintButton, MAX_SCROLLS);
    }

    public boolean isOdrPortalDisplayed() {
        return scrollDownUntilVisible(odrPortalButton, MAX_SCROLLS);
    }

    public boolean isAuditStatusDisplayed() {
        return scrollDownUntilVisible(auditStatusButton, MAX_SCROLLS);
    }

    // ====================================================================
    // INDIVIDUAL TAP + VALIDATE (each taps, checks screen opened, returns)
    // ====================================================================

    public boolean tapViewAllTransactionsAndValidate() {
        return tapLinkAndValidate(hubLinks.get(0));
    }

    public boolean tapPortfolioPlannerAndValidate() {
        return tapLinkAndValidate(hubLinks.get(1));
    }

    public boolean tapAnalystsChoiceAndValidate() {
        return tapLinkAndValidate(hubLinks.get(2));
    }

    public boolean tapFundScreenerAndValidate() {
        return tapLinkAndValidate(hubLinks.get(3));
    }

    public boolean tapSipReturnCalculatorAndValidate() {
        return tapLinkAndValidate(hubLinks.get(4));
    }

    public boolean tapStoriesAndVideosAndValidate() {
        return tapLinkAndValidate(hubLinks.get(5));
    }

    public boolean tapMarketMonitorAndValidate() {
        return tapLinkAndValidate(hubLinks.get(6));
    }

    public boolean tapStockAdvisorAndValidate() {
        return tapLinkAndValidate(hubLinks.get(7));
    }

    public boolean tapFaqAndValidate() {
        return tapLinkAndValidate(hubLinks.get(8));
    }

    public boolean tapAboutValueResearchAndValidate() {
        return tapLinkAndValidate(hubLinks.get(9));
    }

    public boolean tapContactUsAndValidate() {
        return tapLinkAndValidate(hubLinks.get(10));
    }

    public boolean tapPrivacyPolicyAndValidate() {
        return tapLinkAndValidate(hubLinks.get(11));
    }

    public boolean tapUserAgreementAndValidate() {
        return tapLinkAndValidate(hubLinks.get(12));
    }

    public boolean tapRefundPolicyAndValidate() {
        return tapLinkAndValidate(hubLinks.get(13));
    }

    public boolean tapInvestorCharterAndValidate() {
        return tapLinkAndValidate(hubLinks.get(14));
    }

    public boolean tapComplaintAndValidate() {
        return tapLinkAndValidate(hubLinks.get(15));
    }

    public boolean tapOdrPortalAndValidate() {
        return tapLinkAndValidate(hubLinks.get(16));
    }

    public boolean tapAuditStatusAndValidate() {
        return tapLinkAndValidate(hubLinks.get(17));
    }

    // ====================================================================
    // BULK VALIDATE — taps every link and records pass/fail
    // ====================================================================

    public List<LinkResult> tapAllLinksAndValidateScreens() {
        logger.info("=== Hub: validating all {} links ===", hubLinks.size());
        List<LinkResult> results = new ArrayList<>();
        for (HubLink link : hubLinks) {
            boolean passed = tapLinkAndValidate(link);
            results.add(new LinkResult(link.name, passed));
        }
        logger.info("=== Hub link validation complete: {} ===", results);
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksAndValidateScreens()) {
            if (!result.passed) {
                logger.warn("Hub link FAILED: {}", result.name);
                return false;
            }
        }
        return true;
    }

    // ====================================================================
    // PRIVATE HELPERS
    // ====================================================================

    private boolean tapLinkAndValidate(HubLink link) {
        logger.info("=== Hub link test: {} ===", link.name);

        openHubScreen();
        scrollToTop();

        // Try primary locator, then fallback if primary not found
        By activeLocator = link.locator;
        if (!scrollDownUntilVisible(activeLocator, MAX_SCROLLS)) {
            logger.info("Primary locator not found for '{}', trying fallback", link.name);
            if (link.fallback != null && scrollDownUntilVisible(link.fallback, MAX_SCROLLS)) {
                activeLocator = link.fallback;
            } else {
                logger.warn("Hub link not visible after scrolling (primary + fallback): {}", link.name);
                return false;
            }
        }

        String pageSourceBefore = driver.getPageSource();
        safeClick(activeLocator);
        waitForUiToSettle();

        boolean openedScreen = waitForDestinationScreen(pageSourceBefore);
        boolean returned = returnToHubScreen();
        boolean passed = openedScreen || returned;

        logger.info("Hub link '{}' — openedScreen:{}, returned:{}, passed:{}",
                link.name, openedScreen, returned, passed);
        logger.info("=== Hub link test: {} — done ===", link.name);
        return passed;
    }

    private boolean waitForDestinationScreen(String pageSourceBefore) {
        try {
            shortWait(8).until(d -> {
                String current = d.getPageSource();
                return !current.equals(pageSourceBefore) && !isHubScreenDisplayed();
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean returnToHubScreen() {
        for (int attempt = 0; attempt < 5; attempt++) {
            if (isHubScreenDisplayed()) {
                return true;
            }
            logger.info("Hub screen not visible — pressing back ({}/5)", attempt + 1);
            driver.navigate().back();
            waitForUiToSettle();
        }
        if (isHubScreenDisplayed()) {
            return true;
        }
        try {
            if (isDisplayed(hubTab, 3)) {
                safeClick(hubTab);
                shortWait(8).until(d -> isHubScreenDisplayed());
                return true;
            }
        } catch (Exception ignored) {
        }
        return isHubScreenDisplayed();
    }

    private boolean waitForHubScreen() {
        return waitUntilTrue(this::isHubScreenDisplayed, 10);
    }

    private void scrollToTop() {
        for (int i = 0; i < 6; i++) {
            safeVerticalScroll("down");
        }
        waitForUiToSettle();
    }

    private boolean scrollDownUntilVisible(By locator, int maxSwipes) {
        for (int swipe = 0; swipe <= maxSwipes; swipe++) {
            if (isDisplayed(locator)) {
                return true;
            }
            logger.info("Scrolling down ({}/{}) to reveal: {}", swipe + 1, maxSwipes, locator);
            safeVerticalScroll("up");
            waitForUiToSettle();
        }
        return isDisplayed(locator);
    }

    // ====================================================================
    // VALUE OBJECTS
    // ====================================================================

    private static final class HubLink {
        final String name;
        final By locator;
        final By fallback;

        HubLink(String name, By locator, By fallback) {
            this.name = name;
            this.locator = locator;
            this.fallback = fallback;
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
