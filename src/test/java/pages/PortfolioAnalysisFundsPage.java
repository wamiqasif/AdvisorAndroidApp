package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Covers two screens:
 *  1. Portfolio Analysis (Hub → "Open portfolio analysis screen") — Summary/Funds/Stocks tabs
 *  2. Fund Review (opens after tapping any fund on the Funds tab) — Exit/Good/Steady/Watch/New-Fund
 *
 * Locator strategy: content-desc (accessibility ID) first, UiAutomator second.
 * Zero XPath, zero positional index selectors.
 *
 * XML observations used:
 *  - Portfolio Analysis title:  content-desc="Portfolio Analysis"
 *  - Funds tab on PA:           content-desc="Funds, tab 2 of 3"
 *  - Fund Review title:         content-desc="Fund Review"
 *  - Category tabs pattern:     "Exit (1), tab 1 of 5"  (count in parens)
 *  - Fund items on PA:          android.view.View, clickable=true, content-desc=fund name,
 *                                children include android.widget.ImageView
 *  - Fund items on FR:          android.widget.ImageView or android.view.View,
 *                                clickable=true, content-desc=fund name
 *  - Advice label:              content-desc="Our Advice"
 *  - Reason label (exit):       content-desc="Why exit"
 *  - Reason label (others):     content-desc="Rationale"
 *  - Action button:             android.widget.ImageView clickable=true (e.g. "See Alternatives")
 *  - Investor header button:    content-desc contains "Shows investor selection list"
 *  - Back button:               content-desc="Go back"
 */
public class PortfolioAnalysisFundsPage extends BasePage {

    // ----------------------------------------------------------------
    // Hub → Portfolio Analysis
    // ----------------------------------------------------------------

    private final By openPortfolioAnalysisButton =
            AppiumBy.accessibilityId("Open portfolio analysis screen");

    // ----------------------------------------------------------------
    // Portfolio Analysis screen
    // ----------------------------------------------------------------

    private final By portfolioAnalysisTitle =
            AppiumBy.accessibilityId("Portfolio Analysis");

    private final By fundsTabOnPA =
            AppiumBy.accessibilityId("Funds, tab 2 of 3");

    // ----------------------------------------------------------------
    // Fund Review screen
    // ----------------------------------------------------------------

    private final By fundReviewTitle =
            AppiumBy.accessibilityId("Fund Review");

    private final By goBackButton =
            AppiumBy.accessibilityId("Go back");

    private final By ourAdviceLabel =
            AppiumBy.accessibilityId("Our Advice");

    private final By whyExitLabel =
            AppiumBy.accessibilityId("Why exit");

    private final By rationaleLabel =
            AppiumBy.accessibilityId("Rationale");

    // ----------------------------------------------------------------
    // Category-tab discovery (HorizontalScrollView children)
    // content-desc pattern: "Exit (1), tab 1 of 5"
    // ----------------------------------------------------------------

    private final By allCategoryTabs = AppiumBy.androidUIAutomator(
            "new UiSelector().focusable(true).descriptionContains(\", tab \").descriptionContains(\" of 5\")");

    // ----------------------------------------------------------------

    public PortfolioAnalysisFundsPage(AndroidDriver driver) {
        super(driver);
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * From Hub screen: tap the Portfolio Analysis card to open the screen.
     */
    public void openPortfolioAnalysis() {
        logger.info("Opening Portfolio Analysis from Hub");
        safeClick(openPortfolioAnalysisButton);
        waitForPage(portfolioAnalysisTitle);
        waitForFlutterToSettle();
        logger.info("Portfolio Analysis screen opened");
    }

    /**
     * On Portfolio Analysis screen: tap the Funds tab.
     */
    public void openFundsTab() {
        logger.info("Opening Funds tab on Portfolio Analysis");
        safeClick(fundsTabOnPA);
        waitForFlutterToSettle();
        logger.info("Funds tab opened");
    }

    /**
     * On Portfolio Analysis Funds tab: tap the first visible fund item to open Fund Review.
     * Fund items are android.view.View elements that are clickable and contain ImageView children.
     */
    public void openFundReview() {
        logger.info("Opening Fund Review by tapping first fund item");
        List<WebElement> funds = driver.findElements(
        	    AppiumBy.xpath(
        	        "//android.view.View[@clickable='true' and @content-desc]"
        	    )
        	);

        	WebElement firstFund = funds.stream()
        	    .filter(e -> {
        	        String text = e.getAttribute("content-desc");

        	        return text != null
        	            && !text.isBlank()
        	            && !text.contains("Funds that can do better")
        	            && !text.contains("more")
        	            && !text.contains("%")
        	            && !text.contains("Portfolio Analysis")
        	            && !text.contains("Summary")
        	            && !text.contains("Funds, tab")
        	            && !text.contains("Stocks, tab");
        	    })
        	    .findFirst()
        	    .orElseThrow(() ->
        	        new AssertionError("No fund found"));

        logger.info("Tapping fund: {}", firstFund.getAttribute("content-desc"));
        firstFund.click();
        waitForPage(fundReviewTitle);
        waitForFlutterToSettle();
        logger.info("Fund Review screen opened");
    }

    // ================================================================
    // FUND REVIEW — CATEGORY TABS
    // ================================================================

    /**
     * Opens a category tab on Fund Review screen.
     * @param apiCategory  classification_type value from API: EXIT, GOOD, STEADY, OPTIMIZE, NEW-FUND
     */
    public void openCategory(String apiCategory) {
        String uiLabel = toUiCategoryLabel(apiCategory);
        logger.info("Opening category tab: {} (UI label: {})", apiCategory, uiLabel);

        scrollCategoryTabIntoView(uiLabel);
        By tabLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().focusable(true)"
                        + ".descriptionContains(\"" + uiLabel + "\")"
                        + ".descriptionContains(\", tab\")");
        safeClick(tabLocator);
        waitForFlutterToSettle();
        logger.info("Category tab '{}' opened", uiLabel);
    }

    /**
     * Returns the fund count shown on a category tab (e.g. for "Exit (1), tab 1 of 5" returns 1).
     * Returns -1 if the tab is not found or the count cannot be parsed.
     */
    public int getCategoryCount(String apiCategory) {
        String uiLabel = toUiCategoryLabel(apiCategory);
        scrollCategoryTabIntoView(uiLabel);

        By tabLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().focusable(true)"
                        + ".descriptionContains(\"" + uiLabel + "\")"
                        + ".descriptionContains(\", tab\")");
        List<WebElement> tabs = driver.findElements(tabLocator);
        if (tabs.isEmpty()) {
            logger.warn("Category tab not found for: {}", apiCategory);
            return -1;
        }
        String contentDesc = tabs.get(0).getAttribute("content-desc");
        return extractCountFromTabDesc(contentDesc);
    }

    /**
     * Returns a map of { uiCategoryLabel → count } for all visible category tabs.
     * Example: {"Exit" → 1, "Good" → 11, "Steady" → 4}
     */
    public java.util.Map<String, Integer> getAllCategoryCountsFromUi() {
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        List<WebElement> tabs = driver.findElements(allCategoryTabs);
        for (WebElement tab : tabs) {
            String cd = tab.getAttribute("content-desc");
            if (cd == null || cd.isEmpty()) continue;
            String label = extractLabelFromTabDesc(cd);
            int count    = extractCountFromTabDesc(cd);
            if (!label.isEmpty() && count >= 0) {
                result.put(label, count);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    // ================================================================
    // FUND REVIEW — FUND ITEMS
    // ================================================================

    /**
     * Returns content-desc values of all currently visible fund items on Fund Review screen.
     * Filters out known navigation/UI elements.
     */
    public List<String> getDisplayedFunds() {
        List<String> names = new ArrayList<>();
        By clickableElements = AppiumBy.androidUIAutomator(
                "new UiSelector().clickable(true)");
        List<WebElement> elements = driver.findElements(clickableElements);
        for (WebElement el : elements) {
            String cd = el.getAttribute("content-desc");
            if (cd != null && !cd.isEmpty() && !isKnownUiElement(cd)) {
                names.add(cd);
            }
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Scrolls to find the fund by name on the current category tab.
     * Returns true if found, false otherwise.
     */
    public boolean findFund(String fundName) {
        logger.info("Looking for fund: {}", fundName);
        By locator = AppiumBy.accessibilityId(fundName);
        if (isDisplayed(locator)) {
            logger.info("Fund '{}' visible without scroll", fundName);
            return true;
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().description(\""
                            + fundName.replace("\"", "\\\"") + "\"))"));
            boolean found = isDisplayed(locator, 3);
            logger.info("Fund '{}' scroll result: {}", fundName, found);
            return found;
        } catch (Exception e) {
            logger.warn("Fund '{}' not found after scroll: {}", fundName, e.getMessage());
            return false;
        }
    }

    /**
     * Returns the advice text visible after "Our Advice" heading for a specific fund.
     * Scrolls to the fund first, then locates the advice section.
     */
    public String getAdviceText(String fundName) {
        findFund(fundName);
        // Scroll slightly to reveal the advice section below the fund name
        safeVerticalScroll("up");
        waitForUiToSettle();
        return extractTextAfterLabel(ourAdviceLabel);
    }

    /**
     * Returns the reason/rationale text for a fund.
     * Handles both "Why exit" (EXIT category) and "Rationale" (all other categories).
     */
    public String getReasonText(String fundName) {
        findFund(fundName);
        safeVerticalScroll("up");
        waitForUiToSettle();
        if (isDisplayed(whyExitLabel)) {
            return extractTextAfterLabel(whyExitLabel);
        }
        return extractTextAfterLabel(rationaleLabel);
    }

    /**
     * Returns the content-desc of the action button associated with a fund, or "" if absent.
     * Action buttons are clickable ImageViews that are not navigation elements (e.g. "See Alternatives").
     */
    public String getActionButtonText(String fundName) {
        findFund(fundName);
        safeVerticalScroll("up");
        waitForUiToSettle();

        By actionButtons = AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.ImageView\").clickable(true)");
        List<WebElement> buttons = driver.findElements(actionButtons);
        for (WebElement btn : buttons) {
            String cd = btn.getAttribute("content-desc");
            if (cd != null && !cd.isEmpty() && !isKnownUiElement(cd)) {
                return cd;
            }
        }
        return "";
    }

    /**
     * Returns true if the fund has a lock-in indicator visible on screen.
     * Lock-in state manifests as an element whose content-desc contains "lock" or "Lock".
     */
    public boolean getLockState(String fundName) {
        findFund(fundName);
        waitForUiToSettle();
        By lockIndicator = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"lock\")");
        By lockIndicatorUpper = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Lock\")");
        return isDisplayed(lockIndicator) || isDisplayed(lockIndicatorUpper);
    }

    // ================================================================
    // SCREEN STATE CHECKS
    // ================================================================

    public boolean isPortfolioAnalysisDisplayed() {
        return isDisplayed(portfolioAnalysisTitle);
    }

    public boolean isFundReviewScreenDisplayed() {
        return isDisplayed(fundReviewTitle, 5);
    }

    public boolean isFundsTabActive() {
        return isDisplayed(fundsTabOnPA);
    }

    /** Returns true if "Our Advice" label is visible anywhere on the current screen. */
    public boolean isAdviceSectionVisible() {
        return isDisplayed(ourAdviceLabel);
    }

    /** Returns true if a clickable "See Alternatives" button is visible. */
    public boolean isSeeAlternativesButtonVisible() {
        return isDisplayed(AppiumBy.accessibilityId("See Alternatives"));
    }

    /** Returns true if a clickable button whose content-desc contains "Switch" is visible. */
    public boolean isSwitchButtonVisible() {
        return isDisplayed(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Switch\").clickable(true)"));
    }

    /** Returns true if any action button (clickable ImageView with non-empty, non-nav content-desc) is visible. */
    public boolean isAnyActionButtonVisible() {
        By actionButtons = AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.ImageView\").clickable(true)");
        List<WebElement> buttons = driver.findElements(actionButtons);
        return buttons.stream().anyMatch(btn -> {
            String cd = btn.getAttribute("content-desc");
            return cd != null && !cd.isEmpty() && !isKnownUiElement(cd);
        });
    }

    public void goBack() {
        safeClick(goBackButton);
        waitForFlutterToSettle();
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    /**
     * Scrolls the HorizontalScrollView (category tab bar) to bring the given tab label into view.
     */
    private void scrollCategoryTabIntoView(String uiLabel) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)"
                            + ".className(\"android.widget.HorizontalScrollView\"))"
                            + ".scrollIntoView(new UiSelector()"
                            + ".descriptionContains(\"" + uiLabel + "\"))"));
        } catch (Exception e) {
            logger.debug("HorizontalScrollView scroll for '{}': {}", uiLabel, e.getMessage());
        }
    }

    /**
     * Traverses all focusable elements in DOM order, finds the label element, and returns
     * the content-desc of the next non-empty, non-navigation element after it.
     */
    private String extractTextAfterLabel(By labelLocator) {
        By allFocusable = AppiumBy.androidUIAutomator("new UiSelector().focusable(true)");
        List<WebElement> elements = driver.findElements(allFocusable);
        boolean labelSeen = false;
        for (WebElement el : elements) {
            String cd = el.getAttribute("content-desc");
            if (cd == null) continue;
            if (!labelSeen) {
                List<WebElement> labels = driver.findElements(labelLocator);
                if (!labels.isEmpty() && cd.equals(labels.get(0).getAttribute("content-desc"))) {
                    labelSeen = true;
                    continue;
                }
            } else {
                if (!cd.isEmpty() && !isKnownUiElement(cd)) {
                    return cd;
                }
            }
        }
        return "";
    }

    /**
     * Maps API classification_type to the UI label prefix used in category tab content-desc.
     * Tab content-desc pattern: "Exit (1), tab 1 of 5"
     */
    private String toUiCategoryLabel(String apiCategory) {
        if (apiCategory == null) return "";
        switch (apiCategory.toUpperCase()) {
            case "EXIT":     return "Exit";
            case "GOOD":     return "Good";
            case "STEADY":   return "Steady";
            case "OPTIMIZE": return "Watch";
            case "NEW-FUND": return "New";
            default:         return apiCategory;
        }
    }

    /**
     * Parses count from tab content-desc like "Exit (1), tab 1 of 5" → 1.
     */
    private int extractCountFromTabDesc(String contentDesc) {
        if (contentDesc == null) return -1;
        Matcher m = Pattern.compile("\\((\\d+)\\)").matcher(contentDesc);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Parses label from tab content-desc like "Exit (1), tab 1 of 5" → "Exit".
     */
    private String extractLabelFromTabDesc(String contentDesc) {
        if (contentDesc == null) return "";
        int parenIdx = contentDesc.indexOf('(');
        return parenIdx > 0 ? contentDesc.substring(0, parenIdx).trim() : "";
    }

    /**
     * Returns true if the content-desc belongs to a known navigation or structural UI element
     * (not a fund name).
     */
    private boolean isKnownUiElement(String cd) {
        if (cd == null || cd.isEmpty()) return true;
        if (cd.equals("Go back"))                         return true;
        if (cd.contains("Shows investor selection list")) return true;
        if (cd.contains(", tab ") && cd.contains(" of ")) return true; // category tabs
        if (cd.matches("\\+\\d+ more"))                   return true; // "+9 more"
        if (cd.startsWith("₹"))                           return true; // value displays
        if (cd.contains(" units"))                        return true; // unit counts
        if (cd.equals("Our Advice"))                      return true;
        if (cd.equals("Why exit"))                        return true;
        if (cd.equals("Rationale"))                       return true;
        if (cd.equals("Who is it suitable for?"))         return true;
        if (cd.equals("Funds to exit"))                   return true;
        if (cd.equals("Fund Review"))                     return true;
        if (cd.equals("Portfolio Analysis"))              return true;
        return false;
    }
}
