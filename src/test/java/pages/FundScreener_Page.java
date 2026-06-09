package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * "Fund Screener" screen (reached from the Funds / Hub area).
 *
 * Layout (from the captured UiAutomator2 hierarchies):
 *  - Header "Fund Screener" with three tabs: Premium / Categories / Saved.
 *  - Premium tab: cards "Top Rated Funds", "Rs. 100 SIP Funds",
 *    "Funds for first-time investor" - each opens a filtered fund list.
 *  - Categories tab: filter chips (Popular/Equity/Debt/Hybrid) and category
 *    cards ("Equity Large Cap 123 Funds", ...) - each opens a fund list.
 *  - Fund list screen: column tabs, fund rows, "Filters" and "Columns" actions.
 *  - Bottom navigation: Funds / Stocks / Portfolio / Hub.
 *
 * Locator strategy: content-desc (accessibilityId / descriptionContains) is the
 * stable primary strategy. Card content-descs carry a newline + description, so
 * descriptionContains on the leading title is used.
 *
 * Recursive link crawl (requirement): tap every navigating link, follow each
 * opened screen for further links, recurse until none remain. The crawler skips
 * elements that do NOT open a new screen:
 *  - bottom navigation (Funds/Stocks/Portfolio/Hub) and Go back - they leave the screen
 *  - the "..., tab N of M" tab switchers and the Popular/Equity/Debt/Hybrid filter
 *    chips - they switch content in place
 * It is bounded by depth, a visited-set, and a per-level breadth cap (the fund
 * list can contain 50+ rows; the cap is logged, never silent).
 */
public class FundScreener_Page extends BasePage {

    private static final int MAX_CRAWL_DEPTH = 3;
    private static final int MAX_LINKS_PER_LEVEL = 6;

    // Exact content-descs the crawler must never tap (navigation / in-place filters).
    private static final Set<String> SKIP_LINKS = new HashSet<>(Arrays.asList(
            "Go back", "Back", "Funds", "Stocks", "Portfolio", "Hub",
            "Logout", "Log out", "Sign out", "Delete", "Remove", "Close",
            "Premium", "Categories", "Saved",
            "Popular", "Equity", "Debt", "Hybrid"));

    // ============================================================
    // ENTRY POINT / NAVIGATION
    // ============================================================

    private final By fundScreenerEntry = AppiumBy.accessibilityId("Fund Screener");
    private final By backButton = AppiumBy.accessibilityId("Go back");

    // ============================================================
    // SCREEN ANCHORS
    // ============================================================

    private final By screenHeading = AppiumBy.accessibilityId("Fund Screener");

    private final By premiumTab = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Premium, tab\")");
    private final By categoriesTab = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Categories, tab\")");
    private final By savedTab = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Saved, tab\")");

    // ============================================================
    // PREMIUM TAB CARDS
    // ============================================================

    private final By topRatedFundsCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Top Rated Funds\")");
    private final By sip100Card = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Rs. 100 SIP Funds\")");
    private final By firstTimeInvestorCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Funds for first-time investor\")");

    // ============================================================
    // CATEGORIES TAB
    // ============================================================

    private final By popularChip = AppiumBy.accessibilityId("Popular");
    private final By equityChip = AppiumBy.accessibilityId("Equity");
    private final By debtChip = AppiumBy.accessibilityId("Debt");
    private final By hybridChip = AppiumBy.accessibilityId("Hybrid");

    // Category cards carry "<asset>\n<category>\n<n> Funds".
    private final By anyCategoryCard = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\".*[0-9]+ Funds\")");

    // ============================================================
    // FUND LIST SCREEN (post-card)
    // ============================================================

    private final By fundsFilteredHeader = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"Funds Filtered\")");
    private final By fundColumnHeader = AppiumBy.accessibilityId("Fund");
    private final By ratingColumnHeader = AppiumBy.accessibilityId("Rating");
    private final By categoryColumnHeader = AppiumBy.accessibilityId("Category");
    private final By filtersButton = AppiumBy.accessibilityId("Filters");
    private final By columnsButton = AppiumBy.accessibilityId("Columns");
                  By AddFilterBtn=AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").description(\"Add Filter\")");
    // ============================================================
    // FILTERS PANEL (opened from the fund list via "Filters")
    // ============================================================

    private final By filtersPanelTitle = AppiumBy.accessibilityId("Add Filter");
    private final By searchFiltersInput = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");
    private final By applyFiltersButton = AppiumBy.accessibilityId("Apply Filters");
    private final By resetFiltersButton = AppiumBy.accessibilityId("Reset");
    // "9 Filters: " - count of currently active filters on the panel.
    private final By activeFilterCountLabel = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionMatches(\"[0-9]+ Filters:.*\")");

    // Left-column filter categories - stable content-descs.
    private static final java.util.List<String> FILTER_CATEGORIES = Arrays.asList(
            "Our Opinion", "AMC", "Category", "Plan", "Active/ Passive",
            "Rating", "Return", "Risk", "Investment amount");

    // Panel controls / labels that are NOT selectable filter options.
    private static final Set<String> FILTER_NON_OPTIONS = new HashSet<>(Arrays.asList(
            "Add Filter", "Apply Filters", "Reset", "Search filters"));

    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    private final By fundsTab = AppiumBy.accessibilityId("Funds");
    private final By stocksTab = AppiumBy.accessibilityId("Stocks");
    private final By portfolioTab = AppiumBy.accessibilityId("Portfolio");
    private final By hubTab = AppiumBy.accessibilityId("Hub");

    // Generic clickable-link selector used by the recursive crawler.
    private final By anyClickable = AppiumBy
            .androidUIAutomator("new UiSelector().clickable(true)");

    // ============================================================

    public FundScreener_Page(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /**
     * Opens the Fund Screener screen. If already on it, returns immediately.
     * Otherwise taps the "Fund Screener" entry (scrolling to it if needed).
     */
    public FundScreener_Page openFundScreener() {
        logger.info("Opening Fund Screener screen");

        if (isFundScreenerDisplayed()) {
            logger.info("Fund Screener already open");
            return this;
        }

        if (!isDisplayed(fundScreenerEntry)) {
            scrollDownUntilVisible(fundScreenerEntry, 8);
        }
        safeClick(fundScreenerEntry);
        waitForFundScreener();
        logger.info("Fund Screener opened");
        return this;
    }

    private void waitForFundScreener() {
        try {
            wait.until(driver -> isFundScreenerDisplayed());
        } catch (TimeoutException e) {
            throw new AssertionError("Fund Screener screen failed to load", e);
        }
    }

    // ============================================================
    // SCREEN DETECTION
    // ============================================================

    public boolean isFundScreenerDisplayed() {
        return isDisplayed(screenHeading) && isAnyDisplayed(premiumTab, categoriesTab);
    }

    public boolean isFundListDisplayed() {
        return isAnyDisplayed(fundsFilteredHeader, filtersButton)
                || (isDisplayed(fundColumnHeader) && isDisplayed(ratingColumnHeader));
    }

    // ============================================================
    // TABS
    // ============================================================

    public boolean isPremiumTabDisplayed() {
        return isDisplayed(premiumTab);
    }

    public boolean isCategoriesTabDisplayed() {
        return isDisplayed(categoriesTab);
    }

    public boolean isSavedTabDisplayed() {
        return isDisplayed(savedTab);
    }

    public boolean isPremiumTabSelected() {
        return isTabSelected(premiumTab);
    }

    public boolean isCategoriesTabSelected() {
        return isTabSelected(categoriesTab);
    }

    public FundScreener_Page tapPremiumTab() {
        logger.info("Tapping Premium tab");
        safeClick(premiumTab);
        waitForUiToSettle();
        return this;
    }

    public FundScreener_Page tapCategoriesTab() {
        logger.info("Tapping Categories tab");
        safeClick(categoriesTab);
        waitForUiToSettle();
        return this;
    }
    public void tapOnAddFilter() {
    	safeClick(AddFilterBtn);
    }

    public FundScreener_Page tapSavedTab() {
        logger.info("Tapping Saved tab");
        safeClick(savedTab);
        waitForUiToSettle();
        return this;
    }

    private boolean isTabSelected(By tab) {
        try {
            String selected = waitForVisible(tab).getAttribute("selected");
            return "true".equalsIgnoreCase(selected);
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // PREMIUM CARD VISIBILITY
    // ============================================================

    public boolean isTopRatedFundsCardDisplayed() {
        return isDisplayed(topRatedFundsCard);
    }

    public boolean isSip100CardDisplayed() {
        return isDisplayed(sip100Card);
    }

    public boolean isFirstTimeInvestorCardDisplayed() {
        return isDisplayed(firstTimeInvestorCard);
    }

    // ============================================================
    // CATEGORIES VISIBILITY
    // ============================================================

    public boolean isCategoryChipsDisplayed() {
        return isAnyDisplayed(popularChip, equityChip, debtChip, hybridChip);
    }

    public boolean isAnyCategoryCardDisplayed() {
        return isDisplayed(anyCategoryCard);
    }

    // ============================================================
    // FUND LIST CONTROLS VISIBILITY
    // ============================================================

    public boolean isFiltersButtonDisplayed() {
        return isDisplayed(filtersButton);
    }

    public boolean isColumnsButtonDisplayed() {
        return isDisplayed(columnsButton);
    }

    public boolean isBackButtonDisplayed() {
        return isDisplayed(backButton);
    }

    // ============================================================
    // SINGLE-LINK TAP + VALIDATE + RETURN
    // ============================================================

    public boolean tapTopRatedFundsAndVerify() {
        return tapLinkAndVerify(topRatedFundsCard, "Top Rated Funds");
    }

    public boolean tapSip100FundsAndVerify() {
        return tapLinkAndVerify(sip100Card, "Rs. 100 SIP Funds");
    }

    public boolean tapFirstTimeInvestorAndVerify() {
        return tapLinkAndVerify(firstTimeInvestorCard, "Funds for first-time investor");
    }

    public boolean tapFiltersAndVerify() {
        if (!isFiltersButtonDisplayed()) {
            logger.warn("Filters button not visible - skipping");
            return false;
        }
        return tapLinkAndVerify(filtersButton, "Filters");
    }

    public boolean tapColumnsAndVerify() {
        if (!isColumnsButtonDisplayed()) {
            logger.warn("Columns button not visible - skipping");
            return false;
        }
        return tapLinkAndVerify(columnsButton, "Columns");
    }

    public boolean tapBackAndVerify() {
        logger.info("=== Link test: Back ===");
        safeClick(backButton);
        waitForUiToSettle();
        boolean leftScreen = !isFundScreenerDisplayed();
        logger.info("Navigated away from Fund Screener: {}", leftScreen);
        return leftScreen;
    }

    private boolean tapLinkAndVerify(By link, String name) {
        logger.info("=== Link test: {} ===", name);
        String before = screenSignature();
        safeClick(link);
        waitForUiToSettle();
        boolean opened = screenChanged(before);
        logger.info("Link '{}' opened a new screen: {}", name, opened);
        returnToFundScreener();
        logger.info("=== Link test: {} - done ===", name);
        return opened;
    }

    // ============================================================
    // CUSTOMISED FILTER FLOW
    // open screener -> open a premium card (stay on fund list) ->
    // open Filters -> add/select a filter -> Apply Filters
    // ============================================================

    /** Opens the Top Rated Funds list and STAYS on it (unlike tap...AndVerify). */
    public boolean openTopRatedFundList() {
        logger.info("Opening Top Rated Funds list");
        safeClick(topRatedFundsCard);
        waitForUiToSettle();
        boolean opened = waitUntilDisplayed2(this::isFundListDisplayed, 12);
        logger.info("Top Rated fund list opened: {}", opened);
        return opened;
    }

    /** Opens a premium card by its title and stays on the resulting fund list. */
    public boolean openPremiumCardFundList(By card, String name) {
        logger.info("Opening fund list for: {}", name);
        safeClick(card);
        waitForUiToSettle();
        return waitUntilDisplayed2(this::isFundListDisplayed, 12);
    }

    /** From a fund list, opens the Filters panel. */
    public boolean openFiltersPanel() {
        logger.info("Opening Filters panel");
        safeClick(filtersButton);
        waitForUiToSettle();
        boolean opened = waitUntilDisplayed2(this::isFiltersPanelDisplayed, 12);
        logger.info("Filters panel opened: {}", opened);
        return opened;
    }

    public boolean isFiltersPanelDisplayed() {
        return isDisplayed(filtersPanelTitle) || isDisplayed(applyFiltersButton);
    }

    /** Taps a left-column filter category by name (e.g. "Rating", "Category", "Plan"). */
    public FundScreener_Page tapFilterCategory(String categoryName) {
        logger.info("Tapping filter category: {}", categoryName);
        safeClick(AppiumBy.accessibilityId(categoryName));
        waitForUiToSettle();
        return this;
    }

    /**
     * Selects the first selectable option in the right pane of the Filters panel.
     * Generic so it does not depend on a specific category's option labels: it taps
     * the first clickable element with a content-desc that is neither a left-column
     * category nor a panel control/label. Returns false if none is found.
     */
    public boolean selectFirstOptionInRightPane() {
        try {
            for (WebElement el : findElements(anyClickable)) {
                String d;
                try {
                    d = el.getAttribute("content-desc");
                } catch (Exception e) {
                    continue;
                }
                if (d == null || d.trim().isEmpty()) {
                    continue;
                }
                if (FILTER_CATEGORIES.contains(d) || FILTER_NON_OPTIONS.contains(d)) {
                    continue;
                }
                if (d.contains("Filters:") || d.contains("Funds Filtered")) {
                    continue;
                }
                logger.info("Selecting filter option: {}", d);
                el.click();
                waitForUiToSettle();
                return true;
            }
        } catch (Exception e) {
            logger.warn("Unable to select a filter option: {}", e.getMessage());
        }
        logger.info("No selectable filter option found in the right pane");
        return false;
    }

    /** Selects a specific filter option by its content-desc. */
    public boolean selectFilterOption(String optionContentDesc) {
        By option = AppiumBy.accessibilityId(optionContentDesc);
        if (!isDisplayed(option)) {
            logger.warn("Filter option not visible: {}", optionContentDesc);
            return false;
        }
        logger.info("Selecting filter option: {}", optionContentDesc);
        safeClick(option);
        waitForUiToSettle();
        return true;
    }

    /** Reads the active-filter count from the "N Filters:" label (-1 if unavailable). */
    public int getActiveFilterCount() {
        try {
            String d = waitForVisible(activeFilterCountLabel).getAttribute("content-desc");
            String first = d.trim().split("\\s+")[0];
            return Integer.parseInt(first);
        } catch (Exception e) {
            logger.warn("Unable to read active filter count");
            return -1;
        }
    }

    public FundScreener_Page resetFilters() {
        logger.info("Resetting filters");
        safeClick(resetFiltersButton);
        waitForUiToSettle();
        return this;
    }

    /** Taps "Apply Filters" and waits to land back on the (now filtered) fund list. */
    public boolean applyFilters() {
        logger.info("Applying filters");
        safeClick(applyFiltersButton);
        waitForUiToSettle();
        boolean applied = waitUntilDisplayed2(this::isFundListDisplayed, 12);
        logger.info("Filters applied, fund list shown: {}", applied);
        return applied;
    }

    /**
     * Full flow: from the Fund Screener, open Top Rated Funds, open Filters, select
     * an option under the given category, and Apply. Returns true when a filtered
     * fund list is shown afterwards. Option selection is best-effort (see
     * selectFirstOptionInRightPane) so it does not depend on unknown option labels.
     */
    public boolean openTopRatedThenCustomiseAndApply(String filterCategory) {
        if (!openTopRatedFundList()) {
            return false;
        }
        if (!openFiltersPanel()) {
            return false;
        }
        tapFilterCategory(filterCategory);
        tapOnAddFilter();
       // selectFirstOptionInRightPane();
        return applyFilters();
    }

    private boolean waitUntilDisplayed2(java.util.function.BooleanSupplier condition, int seconds) {
        return waitUntilTrue(condition, seconds);
    }

    // ============================================================
    // RECURSIVE LINK CRAWL
    // ============================================================

    /**
     * Depth-first crawl of every content-desc'd navigating link reachable from
     * the current Fund Screener tab. Validates that each tap opens a new screen,
     * follows nested links, and backs out between branches. Returns one LinkResult
     * per link tapped (passed = a new screen was detected after the tap).
     */
    public List<LinkResult> tapAllLinksRecursively() {
        logger.info("=== Recursive link crawl: start ===");
        List<LinkResult> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        crawl(0, results, visited);

        returnToFundScreener();
        logger.info("=== Recursive link crawl: complete - {} link(s) tapped ===", results.size());
        return results;
    }

    public boolean verifyAllLinksOpenScreens() {
        for (LinkResult result : tapAllLinksRecursively()) {
            if (!result.passed) {
                return false;
            }
        }
        return true;
    }

    private void crawl(int depth, List<LinkResult> results, Set<String> visited) {
        if (depth >= MAX_CRAWL_DEPTH) {
            logger.info("Crawl depth {} reached - stopping this branch", MAX_CRAWL_DEPTH);
            return;
        }

        List<String> linkDescs = currentLinkDescs();
        if (linkDescs.size() > MAX_LINKS_PER_LEVEL) {
            logger.info("Depth {}: {} links found, capping at {} (skipping {} this level)",
                    depth, linkDescs.size(), MAX_LINKS_PER_LEVEL, linkDescs.size() - MAX_LINKS_PER_LEVEL);
            linkDescs = linkDescs.subList(0, MAX_LINKS_PER_LEVEL);
        }
        logger.info("Depth {}: {} candidate link(s): {}", depth, linkDescs.size(), linkDescs);

        for (String desc : linkDescs) {
            if (visited.contains(desc) || isSkipped(desc)) {
                continue;
            }
            visited.add(desc);

            By linkLocator = AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"" + desc + "\")");
            if (!isDisplayed(linkLocator)) {
                continue;
            }

            try {
                String before = screenSignature();
                logger.info("Depth {}: tapping link '{}'", depth, oneLine(desc));
                safeClick(linkLocator);
                waitForUiToSettle();

                boolean opened = screenChanged(before);
                results.add(new LinkResult(oneLine(desc), opened));

                if (opened) {
                    crawl(depth + 1, results, visited);
                }
            } catch (Exception e) {
                logger.warn("Depth {}: link '{}' failed: {}", depth, oneLine(desc), e.getMessage());
                results.add(new LinkResult(oneLine(desc), false));
            } finally {
                navigateBackOnce();
                waitForUiToSettle();
            }
        }
    }

    /** Content-descs of visible clickable elements, minus skipped (nav / in-place) ones. */
    private List<String> currentLinkDescs() {
        List<String> descs = new ArrayList<>();
        try {
            List<WebElement> clickables = findElements(anyClickable);
            for (WebElement el : clickables) {
                try {
                    String d = el.getAttribute("content-desc");
                    if (d != null && !d.trim().isEmpty() && !descs.contains(d) && !isSkipped(d)) {
                        descs.add(d);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to enumerate clickable links: {}", e.getMessage());
        }
        return descs;
    }

    /**
     * A link is skipped when it is an exact navigation/filter label, or when it is
     * a "..., tab N of M" tab switcher (these change content in place rather than
     * opening a new screen).
     */
    private boolean isSkipped(String desc) {
        if (SKIP_LINKS.contains(desc)) {
            return true;
        }
        return desc.contains(", tab ") && desc.contains(" of ");
    }

    // ============================================================
    // INTERNAL HELPERS
    // ============================================================

    /** Lightweight screen signature from clickable content-descs (avoids getPageSource). */
    private String screenSignature() {
        StringBuilder sb = new StringBuilder();
        try {
            for (WebElement el : findElements(anyClickable)) {
                try {
                    String d = el.getAttribute("content-desc");
                    if (d != null && !d.trim().isEmpty()) {
                        sb.append(d).append("|");
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private boolean screenChanged(String beforeSignature) {
        try {
            shortWait(8).until(driver -> !screenSignature().equals(beforeSignature));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void navigateBackOnce() {
        try {
            driver.navigate().back();
        } catch (Exception e) {
            logger.warn("navigate().back() failed: {}", e.getMessage());
        }
    }

    /** Presses back until the Fund Screener screen is shown again (bounded). */
    private boolean returnToFundScreener() {
        for (int attempt = 0; attempt < MAX_CRAWL_DEPTH + 2; attempt++) {
            if (isFundScreenerDisplayed()) {
                return true;
            }
            navigateBackOnce();
            waitForUiToSettle();
        }
        return isFundScreenerDisplayed();
    }

    private boolean scrollDownUntilVisible(By locator, int maxSwipes) {
        for (int swipe = 0; swipe <= maxSwipes; swipe++) {
            if (isDisplayed(locator)) {
                return true;
            }
            safeVerticalScroll("up");
            waitForUiToSettle();
        }
        return isDisplayed(locator);
    }

    /** Collapses multi-line content-descs to a single line for readable logging/results. */
    private String oneLine(String desc) {
        return desc == null ? "" : desc.replace("\n", " ").trim();
    }

    // ============================================================

    public static class LinkResult {

        public final String name;
        public final boolean passed;

        public LinkResult(String name, boolean passed) {
            this.name = name;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return name + "=" + passed;
        }
    }
}
