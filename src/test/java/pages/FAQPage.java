package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * Page object for the FAQ (Frequently Asked Questions) screen of the
 * ValueResearch Advisor app.
 *
 * <p>The screen is reached from the Hub via the "FAQs" entry. It renders a
 * heading ("Frequently Asked Questions") followed by a list of FAQ category
 * links. Each category link is a clickable {@code ImageView} carrying a stable
 * {@code content-desc}, so {@link AppiumBy#accessibilityId(String)} is the
 * preferred locator. Tapping a category opens a sub-screen listing that
 * category's questions; this page taps every link and validates that the
 * sub-screen actually opened before returning.</p>
 */
public class FAQPage extends BasePage {

    // ============================================================
    // ENTRY POINT (from Hub)
    // ============================================================

    private final By faqsEntry = AppiumBy.accessibilityId("View frequently asked questions");

    // ============================================================
    // SCREEN ANCHORS / NAVIGATION
    // ============================================================

    private final By backButton = AppiumBy.accessibilityId("Go back");

    private final By faqHeading = AppiumBy.accessibilityId("Frequently Asked Questions");

    // Top-right header icon has no content-desc — positional fallback.
    // It is the first ImageView in the header row (bounds [610,48][680,160]).
    private final By headerActionIcon = AppiumBy
            .androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(0)");

    // ============================================================
    // FAQ CATEGORY LINKS
    // Each is a clickable ImageView with a stable content-desc.
    // ============================================================

    private final By researchAndAdviceLink = AppiumBy.accessibilityId("Research and Advice");
    private final By buyingSellingFundsLink = AppiumBy.accessibilityId("Buying and Selling Mutual Funds");
    private final By fundAdvisorAccountLink = AppiumBy.accessibilityId("Fund Advisor Account");

    /** Ordered list of every FAQ category link rendered on the screen. */
    private final List<FaqCategory> categories = Arrays.asList(
            new FaqCategory("Research and Advice", researchAndAdviceLink),
            new FaqCategory("Buying and Selling Mutual Funds", buyingSellingFundsLink),
            new FaqCategory("Fund Advisor Account", fundAdvisorAccountLink));

    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    private final By fundsBottomTab = AppiumBy.accessibilityId("Funds");
    private final By stocksBottomTab = AppiumBy.accessibilityId("Stocks");
    private final By portfolioBottomTab = AppiumBy.accessibilityId("Portfolio");
    private final By hubBottomTab = AppiumBy.accessibilityId("Hub");

    // ============================================================
    // SUB-SCREEN (post-tap) ANCHORS
    // After tapping a category, the questions sub-screen renders one or more
    // question rows ending with "?". The back button remains available.
    // ============================================================

    private final By faqQuestionRow = AppiumBy
            .androidUIAutomator("new UiSelector().descriptionContains(\"?\")");

    public FAQPage(AndroidDriver driver) {
        super(driver);
    }

    // ============================================================
    // NAVIGATION TO SCREEN
    // ============================================================

    /**
     * Opens the FAQ screen from its entry point ("FAQs") on the Hub if not
     * already there. Scrolls the Hub down to the "FAQs" entry when it is not
     * immediately visible.
     */
    public FAQPage openFaqScreen() {
        logger.info("Opening FAQ screen");
        if (isFaqScreenDisplayed()) {
            logger.info("FAQ screen already open");
            return this;
        }
        if (!isDisplayed(faqsEntry)) {
            logger.info("'FAQs' entry not visible — scrolling Hub to locate it");
            scrollDownUntilVisible(faqsEntry, 6);
        }
        safeClick(faqsEntry);
        waitForUiToSettle();
        waitForVisible(faqHeading);
        logger.info("FAQ screen opened");
        return this;
    }

    // ============================================================
    // SCREEN DETECTION
    // ============================================================

    public boolean isFaqScreenDisplayed() {
        logger.info("Checking FAQ screen visibility");
        return isDisplayed(faqHeading);
    }

    public boolean isFaqHeadingDisplayed() {
        logger.info("Checking FAQ heading visibility");
        waitForVisible(faqHeading);
        return isDisplayed(faqHeading);
    }

    public boolean isBackButtonDisplayed() {
        logger.info("Checking back button visibility");
        waitForVisible(backButton);
        return isDisplayed(backButton);
    }

    public boolean isHeaderActionIconDisplayed() {
        logger.info("Checking header action icon visibility");
        return isDisplayed(headerActionIcon);
    }

    /** True when at least one expanded FAQ question row (ending with "?") is visible. */
    public boolean areQuestionsDisplayed() {
        logger.info("Checking FAQ question rows visibility");
        return isDisplayed(faqQuestionRow);
    }

    // ============================================================
    // CATEGORY LINK VISIBILITY
    // ============================================================

    public boolean isResearchAndAdviceLinkDisplayed() {
        logger.info("Checking 'Research and Advice' link visibility");
        return isDisplayed(researchAndAdviceLink);
    }

    public boolean isBuyingSellingFundsLinkDisplayed() {
        logger.info("Checking 'Buying and Selling Mutual Funds' link visibility");
        return isDisplayed(buyingSellingFundsLink);
    }

    public boolean isFundAdvisorAccountLinkDisplayed() {
        logger.info("Checking 'Fund Advisor Account' link visibility");
        return isDisplayed(fundAdvisorAccountLink);
    }

    /** True only when every known FAQ category link is visible on the list screen. */
    public boolean areAllCategoryLinksDisplayed() {
        logger.info("Checking that all FAQ category links are visible");
        for (FaqCategory category : categories) {
            if (!isDisplayed(category.locator)) {
                logger.info("Category link missing: {}", category.name);
                return false;
            }
        }
        return true;
    }

    /** Number of FAQ category links currently visible. */
    public int getVisibleCategoryCount() {
        int count = 0;
        for (FaqCategory category : categories) {
            if (isDisplayed(category.locator)) {
                count++;
            }
        }
        logger.info("Visible FAQ category links: {}", count);
        return count;
    }

    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    public boolean isBottomNavigationDisplayed() {
        logger.info("Checking bottom navigation visibility");
        return isAnyDisplayed(fundsBottomTab, stocksBottomTab, portfolioBottomTab, hubBottomTab);
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    public FAQPage tapBack() {
        logger.info("Tapping back button on FAQ screen");
        safeClick(backButton);
        waitForUiToSettle();
        logger.info("Back button tapped");
        return this;
    }

    /**
     * Swipes the screen down until the given locator is visible. Flutter labels
     * live in {@code content-desc} (the {@code text} attribute is empty), so
     * {@code UiScrollable.scrollIntoView(text...)} cannot find them — manual
     * swipes are used instead.
     *
     * @return {@code true} if the locator became visible within maxSwipes
     */
    private boolean scrollDownUntilVisible(By locator, int maxSwipes) {
        for (int i = 0; i < maxSwipes; i++) {
            if (isDisplayed(locator)) {
                return true;
            }
            logger.info("Scrolling down ({} / {}) to reveal element", i + 1, maxSwipes);
            safeVerticalScroll("up");
            waitForUiToSettle();
        }
        return isDisplayed(locator);
    }

    // ============================================================
    // TAP LINK + VALIDATE EXPANDED CONTENT (accordion, single screen)
    // ============================================================

    /**
     * Taps a single FAQ category link and validates that its questions expand
     * inline on the FAQ screen.
     *
     * <p>The FAQ screen is an accordion on one long scrollable page: tapping a
     * category reveals its question rows (each ending with {@code "?"}) directly
     * below it and pushes the remaining categories further down. We therefore do
     * <strong>not</strong> navigate back — the next category is reached by
     * scrolling down. The link is considered opened when at least one question
     * row becomes visible.</p>
     *
     * @param category the category whose link should be tapped
     * @return {@code true} if the category's questions expanded
     */
    /**
     * Convenience overload that taps the category with the given display name.
     *
     * @throws IllegalArgumentException if no category with that name exists
     */
    public boolean tapCategoryAndVerify(String categoryName) {
        for (FaqCategory category : categories) {
            if (category.name.equals(categoryName)) {
                return tapCategoryAndVerify(category);
            }
        }
        throw new IllegalArgumentException("Unknown FAQ category: " + categoryName);
    }

    public boolean tapCategoryAndVerify(FaqCategory category) {
        logger.info("=== FAQ link test: {} ===", category.name);

        if (!scrollDownUntilVisible(category.locator, 6)) {
            logger.warn("Category link not found after scrolling: {}", category.name);
            return false;
        }

        safeClick(category.locator);
        waitForUiToSettle();

        boolean hasQuestions = isDisplayed(faqQuestionRow, 5);
        boolean onFaqScreen = isAnyDisplayed(faqQuestionRow, backButton, faqHeading);
        boolean verified = hasQuestions && onFaqScreen;

        logger.info("Expanded '{}' — hasQuestions:{}, onFaqScreen:{}, verified:{}",
                category.name, hasQuestions, onFaqScreen, verified);
        logger.info("=== FAQ link test: {} — done ===", category.name);
        return verified;
    }

    /**
     * Taps every FAQ category link in screen order, validating that each one's
     * questions expand inline. Scrolls down between categories (no back press),
     * because expanding one category pushes the next one further down the page.
     *
     * @return list of per-category validation results, in screen order
     */
    public List<LinkResult> tapAllCategoriesAndVerify() {
        logger.info("=== verifyAllLinks: tapping every FAQ category link ===");
        List<LinkResult> results = new ArrayList<>();
        for (FaqCategory category : categories) {
            boolean ok = tapCategoryAndVerify(category);
            results.add(new LinkResult(category.name, ok));
        }
        logger.info("=== verifyAllLinks: complete — {} ===", results);
        return results;
    }

    /** Convenience: true only if every category link expanded its questions. */
    public boolean verifyAllCategoryLinks() {
        for (LinkResult result : tapAllCategoriesAndVerify()) {
            if (!result.passed) {
                return false;
            }
        }
        return true;
    }

    // ============================================================
    // VALUE OBJECTS
    // ============================================================

    /** A single FAQ category link: its display name and its locator. */
    public static final class FaqCategory {
        public final String name;
        public final By locator;

        public FaqCategory(String name, By locator) {
            this.name = name;
            this.locator = locator;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Result of tapping one FAQ category link and validating its sub-screen. */
    public static final class LinkResult {
        public final String name;
        public final boolean passed;

        LinkResult(String name, boolean passed) {
            this.name = name;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return name + "=" + (passed ? "PASS" : "FAIL");
        }
    }
}
