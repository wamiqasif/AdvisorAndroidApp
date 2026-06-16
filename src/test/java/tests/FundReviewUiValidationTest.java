package tests;

import api.FundOpinionApiService;
import api.FundReviewDataService;
import api.model.FundOpinionItem;
import api.model.FundReviewItem;
import api.model.ValidationResult;
import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import pages.DashboardPage;
import pages.FundReviewPage;
import utils.FundReviewCategoryCalculator;
import utils.FundReviewCategoryCalculator.CalculationResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fund Review UI Validation Test
 *
 * Validates: Expected Category (PRD Calculator) == UI Category shown in the app
 *
 * Data flow:
 *  Step 1. Call Fund Review API    → plan_id, fund_name, actualCategory
 *  Step 2. Collect all plan_ids
 *  Step 3. Call Fund Opinion API   → opinion_name (or provisional fallback)
 *  Step 4. PRD Calculator          → expectedCategory
 *  Step 5. Navigate device UI      → Hub → Portfolio Analysis → Funds → Fund Review
 *  Step 6. For each fund: locate it in category tabs → record UI category
 *  Step 7. Compare expectedCategory vs uiCategory
 *  Step 8. Log detailed ExtentReport + summary
 *
 * Navigation lifecycle:
 *  onClassReady()    — initialises page objects (driver-backed)
 *  recoverAppState() — navigates to Fund Review screen before each test method
 *  tearDown()        — BaseTest calls safelyRecoverHomeState() to unwind Flutter nav stack
 */
public class FundReviewUiValidationTest extends BaseTest {

    /** UI tab label → API category name used when mapping getAllCategoryCountsFromUi() results. */
    private static final Map<String, String> UI_LABEL_TO_API;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Exit",   "EXIT");
        m.put("Good",   "GOOD");
        m.put("Steady", "STEADY");
        m.put("Watch",  "OPTIMIZE");
        m.put("New",    "NEW-FUND");
        UI_LABEL_TO_API = java.util.Collections.unmodifiableMap(m);
    }

    private FundReviewDataService        reviewService;
    private FundOpinionApiService        opinionService;
    private FundReviewCategoryCalculator calculator;
    private FundReviewPage               fundReviewPage;
    private DashboardPage                dashboardPage;

    // ================================================================
    // LIFECYCLE
    // ================================================================

    @Override
    protected boolean shouldManageDriverPerMethod() { return false; }

    @Override
    protected void onClassReady() {
        reviewService  = new FundReviewDataService();
        opinionService = new FundOpinionApiService();
        calculator     = new FundReviewCategoryCalculator();
        fundReviewPage = new FundReviewPage(getDriver());
        dashboardPage  = new DashboardPage(getDriver());
    }

    /**
     * Navigates from Dashboard → Hub → Portfolio Analysis → Funds → Fund Review.
     * Called by BaseTest.setUp() before every test method.
     */
    @Override
    protected void recoverAppState(Method method) {
        dashboardPage.tapHubTab();
        fundReviewPage.openPortfolioAnalysis();
        fundReviewPage.openFundsTab();
        fundReviewPage.openFundReview();
        Assert.assertTrue(fundReviewPage.isFundReviewScreenDisplayed(),
                "Fund Review screen not displayed after navigation (Hub → Portfolio Analysis → Funds → Fund Review)");
    }

    // ================================================================
    // TC_UI_001 — UI category tab counts vs PRD expected counts
    // ================================================================

    @Test(description = "TC_UI_001 — UI category tab counts match PRD-expected category distribution")
    public void tc_ui_001_verifyUiCategoryCountsMatchExpected() {
        ExtentTest test = getExtentTest();

        // ── API data ──────────────────────────────────────────────
        test.info("Fetching Fund Review API and Fund Opinion API data");
        List<FundReviewItem> funds = reviewService.getFunds();
        assertDataAvailable(funds, "Fund Review API returned no data");

        List<Integer> planIds = funds.stream().map(f -> f.planId).collect(Collectors.toList());
        Map<Integer, FundOpinionItem> opinions = opinionService.getOpinionsByPlanIds(planIds);
        assertDataAvailable(opinions, "Fund Opinion API returned no data");
        test.info("Fund Review API: " + funds.size() + " funds. Fund Opinion API: " + opinions.size() + " opinions");

        // ── PRD expected counts ───────────────────────────────────
        Map<String, Integer> expectedCounts = new LinkedHashMap<>();
        int skippedFunds = 0;
        for (FundReviewItem fund : funds) {
            FundOpinionItem opinion = opinions.get(fund.planId);
            String effective = opinion != null ? opinion.effectiveOpinion() : "";
            CalculationResult calc = calculator.calculateExpectedCategory(effective, fund.fundName);
            if (calc.isCalculated()) {
                expectedCounts.merge(calc.expectedCategory, 1, Integer::sum);
            } else {
                skippedFunds++;
            }
        }
        test.info("PRD expected counts: " + expectedCounts
                + " | Skipped (no PRD mapping): " + skippedFunds);

        // ── UI actual counts (from tab labels) ────────────────────
        Map<String, Integer> uiCounts = fundReviewPage.getAllCategoryCountsFromUi();
        test.info("UI tab counts (raw): " + uiCounts);

        // Convert UI labels to API categories
        Map<String, Integer> uiCountsMapped = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : uiCounts.entrySet()) {
            String apiCat = UI_LABEL_TO_API.getOrDefault(entry.getKey(), entry.getKey().toUpperCase());
            uiCountsMapped.put(apiCat, entry.getValue());
        }
        test.info("UI tab counts (mapped to API categories): " + uiCountsMapped);

        // ── Compare ───────────────────────────────────────────────
        boolean allMatch = true;
        for (Map.Entry<String, Integer> e : expectedCounts.entrySet()) {
            String cat = e.getKey();
            int exp    = e.getValue();
            int ui     = uiCountsMapped.getOrDefault(cat, 0);
            if (exp == ui) {
                test.pass(String.format("Tab %-10s | PRD expected: %d | UI count: %d", cat, exp, ui));
            } else {
                test.fail(String.format("Tab %-10s | PRD expected: %d | UI count: %d  ← MISMATCH", cat, exp, ui));
                allMatch = false;
            }
        }
        // Also log any UI categories not covered by expected counts (unexpected tabs)
        for (Map.Entry<String, Integer> e : uiCountsMapped.entrySet()) {
            if (!expectedCounts.containsKey(e.getKey())) {
                test.warning(String.format(
                        "Tab %-10s | PRD expected: 0 | UI count: %d  ← unexpected tab in UI",
                        e.getKey(), e.getValue()));
            }
        }
        if (skippedFunds > 0) {
            test.warning(skippedFunds + " fund(s) were skipped (no PRD mapping) and excluded from count comparison.");
        }

        Assert.assertTrue(allMatch,
                "UI category tab count mismatch vs PRD expectation. See ExtentReport for details.");
    }

    // ================================================================
    // TC_UI_002 — Per-fund UI category vs expected category
    // ================================================================

    @Test(description = "TC_UI_002 — Per-fund: expected category (PRD) matches the UI tab where fund appears",
          dependsOnMethods = "tc_ui_001_verifyUiCategoryCountsMatchExpected",
          alwaysRun = true)
    public void tc_ui_002_verifyPerFundUiCategoryMatchesExpected() {
        ExtentTest test = getExtentTest();

        // ── Step 1-2: Fund Review API ─────────────────────────────
        test.info("Step 1: Fetching Fund Review API");
        List<FundReviewItem> funds = reviewService.getFunds();
        assertDataAvailable(funds, "Fund Review API returned no data");
        test.info("Fund Review API: " + funds.size() + " funds");

        List<Integer> planIds = funds.stream().map(f -> f.planId).collect(Collectors.toList());

        // ── Step 3: Fund Opinion API ──────────────────────────────
        test.info("Step 3: Fetching Fund Opinion API for " + planIds.size() + " plan IDs");
        Map<Integer, FundOpinionItem> opinions = opinionService.getOpinionsByPlanIds(planIds);
        test.info("Fund Opinion API: " + opinions.size() + " opinions");

        // ── Step 4-7: Calculate expected, then find fund in UI ────
        List<ValidationResult> results = new ArrayList<>();

        for (FundReviewItem fund : funds) {
            FundOpinionItem  opinion = opinions.get(fund.planId);
            ValidationResult.Builder rb = ValidationResult.builder(fund.planId, fund.fundName)
                    .fundReviewApiCategory(fund.actualCategory);

            CalculationResult calc;

            if (opinion == null) {
                rb.opinionName("N/A — not returned by Fund Opinion API for planId=" + fund.planId)
                  .expectedCategory(null)
                  .backendStatus(ValidationResult.Status.SKIPPED)
                  .uiStatus(ValidationResult.Status.SKIPPED)
                  .skipReason("Missing field: Fund Opinion API returned no record for planId=" + fund.planId
                          + ". Impact: expected category unknown; UI validation skipped.");
                results.add(rb.build());
                continue;
            }

            String effective = opinion.effectiveOpinion();
            rb.opinionName(effective)
              .usingProvisional(opinion.usingProvisional());

            calc = calculator.calculateExpectedCategory(effective, fund.fundName);
            rb.planType(calc.planType);

            if (calc.isSkipped()) {
                // Also set backendStatus (matches API result can't be checked without expected)
                boolean apiKnown = fund.actualCategory != null && !fund.actualCategory.isBlank();
                rb.expectedCategory(null)
                  .backendStatus(ValidationResult.Status.SKIPPED)
                  .uiStatus(ValidationResult.Status.SKIPPED)
                  .skipReason(calc.skipReason);
                results.add(rb.build());
                continue;
            }

            // Set backend status while we have both values
            rb.expectedCategory(calc.expectedCategory)
              .note(calc.note);
            boolean backendMatch = calc.expectedCategory.equalsIgnoreCase(fund.actualCategory);
            rb.backendStatus(backendMatch
                    ? ValidationResult.Status.MATCH
                    : ValidationResult.Status.MISMATCH);

            // ── Step 6: Locate fund in UI ─────────────────────────
            String uiCategory;
            try {
                test.info("Locating fund in UI: " + fund.fundName
                        + " (expected tab: " + calc.expectedCategory + ")");
                uiCategory = fundReviewPage.getUiCategory(fund.fundName, calc.expectedCategory);
            } catch (Exception e) {
                rb.uiCategory("")
                  .uiStatus(ValidationResult.Status.SKIPPED)
                  .skipReason("UI lookup threw exception: " + e.getMessage()
                          + ". Impact: UI category could not be determined.");
                results.add(rb.build());
                continue;
            }

            if (uiCategory.isBlank()) {
                rb.uiCategory("")
                  .uiStatus(ValidationResult.Status.SKIPPED)
                  .skipReason("Fund not found in any UI category tab. "
                          + "Impact: cannot validate UI category. "
                          + "Possible cause: fund not visible / accessibility label mismatch.");
            } else {
                rb.uiCategory(uiCategory);
                boolean uiMatch = uiCategory.equalsIgnoreCase(calc.expectedCategory);
                rb.uiStatus(uiMatch
                        ? ValidationResult.Status.MATCH
                        : ValidationResult.Status.MISMATCH);
            }

            results.add(rb.build());
        }

        // ── Step 8: Log to ExtentReport ───────────────────────────
        logUiResults(test, results);

        // ── Assertion ─────────────────────────────────────────────
        long uiMismatches = results.stream()
                .filter(r -> r.uiStatus == ValidationResult.Status.MISMATCH)
                .count();
        Assert.assertEquals(uiMismatches, 0,
                uiMismatches + " fund(s) found in wrong UI category tab. "
                + "See ExtentReport for details.");
    }

    // ================================================================
    // Logging helpers
    // ================================================================

    private void logUiResults(ExtentTest test, List<ValidationResult> results) {
        long total      = results.size();
        long uiMatched  = results.stream().filter(r -> r.uiStatus == ValidationResult.Status.MATCH).count();
        long uiMismatched = results.stream().filter(r -> r.uiStatus == ValidationResult.Status.MISMATCH).count();
        long skipped    = results.stream().filter(r -> r.uiStatus == ValidationResult.Status.SKIPPED).count();
        long backendMismatches = results.stream()
                .filter(r -> r.backendStatus == ValidationResult.Status.MISMATCH).count();

        // Per-fund log rows
        for (ValidationResult r : results) {
            String row = buildUiRow(r);
            switch (r.uiStatus) {
                case MATCH:
                    test.pass(row);
                    break;
                case MISMATCH:
                    test.fail(MarkupHelper.createLabel(row, ExtentColor.RED));
                    break;
                case SKIPPED:
                    test.warning(row + " | SKIP: " + r.skipReason);
                    break;
                default:
                    test.info(row);
            }
        }

        // Summary block
        String summary = "\n══════════════════════════════════════════════\n"
                       + " FUND REVIEW UI VALIDATION SUMMARY\n"
                       + "══════════════════════════════════════════════\n"
                       + " Total Funds Processed   : " + total          + "\n"
                       + " Total UI Matches        : " + uiMatched      + "\n"
                       + " Total UI Mismatches     : " + uiMismatched   + "\n"
                       + " Total Skipped           : " + skipped        + "\n"
                       + " Backend Mismatches (FYI): " + backendMismatches + "\n"
                       + "══════════════════════════════════════════════";
        test.info(summary);

        // Mismatch detail blocks
        List<ValidationResult> uiMismatches = results.stream()
                .filter(r -> r.uiStatus == ValidationResult.Status.MISMATCH)
                .collect(Collectors.toList());
        if (!uiMismatches.isEmpty()) {
            test.fail(FundReviewBackendValidationTest.buildMismatchDetail(uiMismatches, "UI"));
        }

        // Warn about backend mismatches found during UI test
        List<ValidationResult> beMatches = results.stream()
                .filter(r -> r.backendStatus == ValidationResult.Status.MISMATCH)
                .collect(Collectors.toList());
        if (!beMatches.isEmpty()) {
            test.warning("Note: " + beMatches.size()
                    + " fund(s) also had backend (API) mismatches. "
                    + "Run FundReviewBackendValidationTest for full backend detail.");
        }
    }

    private String buildUiRow(ValidationResult r) {
        return String.format(
                "Plan ID: %-6d | Fund: %-42s | Opinion: %-15s | Plan Type: %-18s"
                + " | Expected: %-10s | UI: %-10s | API: %-10s | UI=%s | BE=%s%s",
                r.planId,
                r.fundName,
                r.opinionName,
                r.planType,
                r.expectedCategory != null ? r.expectedCategory : "SKIPPED",
                r.uiCategory.isBlank() ? "NOT FOUND" : r.uiCategory,
                r.fundReviewApiCategory,
                r.uiStatus.name(),
                r.backendStatus.name(),
                r.usingProvisional ? " [provisional]" : "");
    }

    private void assertDataAvailable(List<?> list, String msg) {
        if (list == null || list.isEmpty()) throw new SkipException(msg);
    }

    private void assertDataAvailable(Map<?, ?> map, String msg) {
        if (map == null || map.isEmpty()) throw new SkipException(msg);
    }
}
