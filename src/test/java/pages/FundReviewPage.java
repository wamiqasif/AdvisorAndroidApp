package pages;

import io.appium.java_client.android.AndroidDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension of PortfolioAnalysisFundsPage that adds getUiCategory().
 *
 * Does NOT modify PortfolioAnalysisFundsPage — extends it via inheritance.
 *
 * getUiCategory(String fundName):
 *   Iterates all category tabs discovered dynamically from the UI,
 *   switches to each, and checks whether the fund is visible (with scroll).
 *   Returns the API-style category name (EXIT, GOOD, STEADY, OPTIMIZE, NEW-FUND)
 *   of the tab where the fund is found, or "" if not found in any tab.
 *
 * getUiCategory(String fundName, String expectedCategory):
 *   Optimised version: checks the expected tab first (fast path), then
 *   falls back to all other tabs. Use when expected category is already known
 *   to reduce navigation time.
 *
 * UI tab label → API category mapping:
 *   "Exit"   → EXIT
 *   "Good"   → GOOD
 *   "Steady" → STEADY
 *   "Watch"  → OPTIMIZE
 *   "New"    → NEW-FUND
 *   (derived from XML content-desc patterns "Exit (1), tab 1 of 5" etc.)
 */
public class FundReviewPage extends PortfolioAnalysisFundsPage {

    /** Ordered list of all API categories to check when no hint is given. */
    private static final List<String> ALL_API_CATEGORIES =
            Collections.unmodifiableList(Arrays.asList("EXIT", "GOOD", "STEADY", "OPTIMIZE", "NEW-FUND"));

    /**
     * Maps UI tab label prefix → API category name.
     * Keys match the prefix extracted by getAllCategoryCountsFromUi()
     * from content-desc like "Exit (1), tab 1 of 5".
     */
    private static final Map<String, String> UI_LABEL_TO_API;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("Exit",   "EXIT");
        m.put("Good",   "GOOD");
        m.put("Steady", "STEADY");
        m.put("Watch",  "OPTIMIZE");
        m.put("New",    "NEW-FUND");
        UI_LABEL_TO_API = Collections.unmodifiableMap(m);
    }

    public FundReviewPage(AndroidDriver driver) {
        super(driver);
    }

    // ----------------------------------------------------------------
    // getUiCategory — dynamic tab discovery
    // ----------------------------------------------------------------

    /**
     * Returns the API-style category of the tab where fundName is visible,
     * or "" if the fund is not found in any tab.
     *
     * Tabs are discovered dynamically from the UI so unknown tab labels
     * are handled gracefully (mapped via UI_LABEL_TO_API or uppercased).
     */
    public String getUiCategory(String fundName) {
        Map<String, Integer> tabs = getAllCategoryCountsFromUi();
        for (String uiLabel : tabs.keySet()) {
            String apiCategory = UI_LABEL_TO_API.getOrDefault(uiLabel, uiLabel.toUpperCase());
            openCategory(apiCategory);
            waitForUiToSettle();
            if (findFund(fundName)) {
                logger.info("UI category for '{}' → {} (tab label: '{}')", fundName, apiCategory, uiLabel);
                return apiCategory;
            }
        }
        logger.warn("Fund '{}' not found in any UI category tab", fundName);
        return "";
    }

    /**
     * Optimised overload: checks expectedCategory tab first, then falls back.
     * Use when PRD expected category is already known to avoid unnecessary tab switches.
     *
     * @param fundName         fund name to locate
     * @param expectedCategory expected API-style category (EXIT, GOOD, STEADY, OPTIMIZE, NEW-FUND)
     * @return API-style category where found, or "" if absent in all tabs
     */
    public String getUiCategory(String fundName, String expectedCategory) {
        // Fast path — check expected tab first
        if (expectedCategory != null && !expectedCategory.isBlank()) {
            openCategory(expectedCategory);
            waitForUiToSettle();
            if (findFund(fundName)) {
                logger.info("UI category confirmed (fast path) for '{}' → {}", fundName, expectedCategory);
                return expectedCategory;
            }
        }

        // Slow path — check remaining tabs
        Map<String, Integer> tabs = getAllCategoryCountsFromUi();
        for (String uiLabel : tabs.keySet()) {
            String apiCategory = UI_LABEL_TO_API.getOrDefault(uiLabel, uiLabel.toUpperCase());
            if (apiCategory.equals(expectedCategory)) continue; // already checked above
            openCategory(apiCategory);
            waitForUiToSettle();
            if (findFund(fundName)) {
                logger.info("UI category for '{}' → {} (unexpected, expected: {})",
                        fundName, apiCategory, expectedCategory);
                return apiCategory;
            }
        }

        // Last resort: iterate ALL_API_CATEGORIES in case a tab wasn't surfaced by getAllCategoryCountsFromUi
        for (String category : ALL_API_CATEGORIES) {
            if (tabs.values().isEmpty()) break; // no tabs at all — skip
            if (category.equals(expectedCategory)) continue;
            if (tabs.keySet().stream().anyMatch(l ->
                    UI_LABEL_TO_API.getOrDefault(l, l.toUpperCase()).equals(category))) continue;
            // category was NOT discovered dynamically — try it anyway
            try {
                openCategory(category);
                waitForUiToSettle();
                if (findFund(fundName)) {
                    logger.info("UI category for '{}' → {} (found via fallback list)", fundName, category);
                    return category;
                }
            } catch (Exception e) {
                logger.debug("Category tab '{}' not available: {}", category, e.getMessage());
            }
        }

        logger.warn("Fund '{}' not found in any UI category tab", fundName);
        return "";
    }
}
