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
import utils.FundReviewCategoryCalculator;
import utils.FundReviewCategoryCalculator.CalculationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fund Review Backend Validation Test
 *
 * Validates: Expected Category (PRD Calculator) == Fund Review API classification_type
 *
 * Data flow:
 *  Step 1. Call Fund Review API  → get plan_id, fund_name, actualCategory (classification_type)
 *  Step 2. Collect all plan_ids
 *  Step 3. Call Fund Opinion API → get opinion_name (or provisional fallback)
 *  Step 4. Run PRD Calculator    → expectedCategory
 *  Step 5. Compare expectedCategory vs actualCategory
 *  Step 6. Log detailed ExtentReport + summary
 *
 * Source of truth: Fund Opinion API. Fund Review API classification_type is NEVER
 * used as input to the calculator — it is only the "actual" value for comparison.
 *
 * This test does NOT navigate the device UI; it runs purely against the APIs.
 * BaseTest lifecycle still initialises the driver (framework requirement),
 * but the test methods themselves make no Appium calls.
 */
public class FundReviewBackendValidationTest extends BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(FundReviewBackendValidationTest.class);

    /** Display order for count tables and per-category comparison. */
    private static final List<String> CATEGORY_ORDER =
            List.of("GOOD", "OPTIMIZE", "STEADY", "EXIT", "NEW-FUND");

    private FundReviewDataService        reviewService;
    private FundOpinionApiService        opinionService;
    private FundReviewCategoryCalculator calculator;

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
    }

    @Override
    protected void recoverAppState(Method method) {
        // No device navigation required for backend API-only tests.
        // BaseTest has already ensured the app is on Hub (ensureDashboardReady).
    }

    // ================================================================
    // TC_BACKEND_001 — Category count alignment
    // ================================================================

    @Test(description = "TC_BACKEND_001 — Fund Review API category counts align with Fund Opinion API data")
    public void tc_backend_001_verifyCategoryCountsAlignWithOpinionData() {
        ExtentTest test = getExtentTest();

        // ── Fetch data ────────────────────────────────────────────────
        test.info("Step 1/2: Fetching Fund Review API data");
        List<FundReviewItem> funds = reviewService.getFunds();
        assertDataAvailable(funds, "Fund Review API returned no data");

        List<Integer> planIds = funds.stream().map(f -> f.planId).collect(Collectors.toList());
        test.info("Step 3: Fetching Fund Opinion API data for " + planIds.size() + " plan IDs");
        Map<Integer, FundOpinionItem> opinions = opinionService.getOpinionsByPlanIds(planIds);
        assertDataAvailable(opinions, "Fund Opinion API returned no data");

        // ── Count maps — pre-populated so all 5 categories always appear ──
        LinkedHashMap<String, Integer> expectedCounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> actualCounts   = new LinkedHashMap<>();
        for (String cat : CATEGORY_ORDER) {
            expectedCounts.put(cat, 0);
            actualCounts.put(cat, 0);
        }

        // Collectors for per-fund rows
        // row: [planId, fundName, opinion, expectedCat, actualCat, status, skipReason]
        List<String[]> perFundRows  = new ArrayList<>();
        List<String[]> mismatches   = new ArrayList<>();   // [planId, name, opinion, expected, actual]
        List<String[]> skippedFunds = new ArrayList<>();   // [planId, name, opinion, skipReason]

        // ── Per-fund processing ───────────────────────────────────────
        for (FundReviewItem fund : funds) {
            actualCounts.merge(fund.actualCategory, 1, Integer::sum);

            FundOpinionItem opinionItem    = opinions.get(fund.planId);
            String effective               = opinionItem != null ? opinionItem.effectiveOpinion()          : "";
            String isProvStr               = opinionItem != null ? String.valueOf(opinionItem.isProvisional) : "N/A";
            String rawOpinionName          = opinionItem != null ? opinionItem.opinionName                 : "";
            String provOpinionName         = opinionItem != null ? opinionItem.provisionalOpinionName      : "";
            CalculationResult calc = calculator.calculateExpectedCategory(effective, fund.fundName);

            String expectedCat;
            String status;
            String skipReason = "";

            if (calc.isSkipped()) {
                expectedCat = "SKIPPED";
                status      = "SKIPPED";
                skipReason  = calc.skipReason;
                skippedFunds.add(new String[]{
                    String.valueOf(fund.planId), fund.fundName, effective, skipReason,
                    isProvStr, rawOpinionName, provOpinionName
                });
            } else {
                expectedCounts.merge(calc.expectedCategory, 1, Integer::sum);
                boolean match = calc.expectedCategory.equalsIgnoreCase(fund.actualCategory);
                expectedCat   = calc.expectedCategory;
                status        = match ? "MATCH" : "MISMATCH";
                if (!match) {
                    mismatches.add(new String[]{
                        String.valueOf(fund.planId), fund.fundName, effective,
                        calc.expectedCategory, fund.actualCategory,
                        isProvStr, rawOpinionName, provOpinionName
                    });
                }
            }

            // indices: 0=planId, 1=fundName, 2=effective, 3=expectedCat, 4=actualCat,
            //          5=status, 6=skipReason, 7=isProvisional, 8=rawOpinionName, 9=provOpinionName
            perFundRows.add(new String[]{
                String.valueOf(fund.planId), fund.fundName, effective,
                expectedCat, fund.actualCategory, status, skipReason,
                isProvStr, rawOpinionName, provOpinionName
            });
        }

        // ── Log per-fund details ──────────────────────────────────────
        test.info("─────────────────────── Per-Fund Processing Log ───────────────────────");
        for (String[] row : perFundRows) {
            String planId      = row[0];
            String name        = row[1];
            String effective   = row[2];
            String expected    = row[3];
            String actual      = row[4];
            String rowStatus   = row[5];
            String reason      = row[6];
            String isProvStr   = row[7];
            String rawOpinion  = row[8];
            String provOpinion = row[9];

            String entry = String.format(
                "PLAN_ID=%s | FUND_NAME=%s | IS_PROVISIONAL=%s | OPINION_NAME=%s"
                + " | PROVISIONAL_OPINION_NAME=%s | EFFECTIVE_OPINION=%s"
                + " | EXPECTED_CATEGORY=%s | ACTUAL_CATEGORY=%s",
                planId, name, isProvStr, rawOpinion, provOpinion, effective, expected, actual);
            logger.info(entry);

            switch (rowStatus) {
                case "MATCH":
                    test.pass(entry);
                    break;
                case "MISMATCH":
                    String mismatchMsg =
                        "CATEGORY MISMATCH\n"
                      + "  PLAN_ID                  = " + planId      + "\n"
                      + "  FUND_NAME                = " + name        + "\n"
                      + "  IS_PROVISIONAL           = " + isProvStr   + "\n"
                      + "  OPINION_NAME             = " + rawOpinion  + "\n"
                      + "  PROVISIONAL_OPINION_NAME = " + provOpinion + "\n"
                      + "  EFFECTIVE_OPINION        = " + effective   + "\n"
                      + "  EXPECTED_CATEGORY        = " + expected    + "\n"
                      + "  ACTUAL_CATEGORY          = " + actual;
                    logger.error(mismatchMsg);
                    test.fail(MarkupHelper.createLabel(mismatchMsg, ExtentColor.RED));
                    break;
                case "SKIPPED":
                    String skipMsg = entry + "\n  SKIP: " + reason;
                    logger.warn(skipMsg);
                    test.warning(skipMsg);
                    break;
                default:
                    test.info(entry);
            }
        }

        // ── Count tables ──────────────────────────────────────────────
        StringBuilder countTable = new StringBuilder();
        countTable.append("\nEXPECTED COUNTS (PRD Calculator)\n");
        for (Map.Entry<String, Integer> e : expectedCounts.entrySet()) {
            countTable.append(String.format("  %-10s = %d%n", e.getKey(), e.getValue()));
        }
        countTable.append("\nACTUAL COUNTS (Fund Review API)\n");
        for (Map.Entry<String, Integer> e : actualCounts.entrySet()) {
            countTable.append(String.format("  %-10s = %d%n", e.getKey(), e.getValue()));
        }
        logger.info(countTable.toString());
        test.info(countTable.toString());

        // ── Per-category count comparison ─────────────────────────────
        boolean allMatch = true;
        for (String cat : CATEGORY_ORDER) {
            int exp = expectedCounts.getOrDefault(cat, 0);
            int act = actualCounts.getOrDefault(cat, 0);
            if (exp == act) {
                test.pass(String.format("Category %-10s | Expected: %d | API: %d", cat, exp, act));
            } else {
                test.fail(String.format("Category %-10s | Expected: %d | API: %d  ← COUNT MISMATCH", cat, exp, act));
                allMatch = false;
            }
        }

        // ── Final diagnostic summary ──────────────────────────────────
        String summary =
            "\n══════════════════════════════════════════════\n"
          + " TC_BACKEND_001 DIAGNOSTIC SUMMARY\n"
          + "══════════════════════════════════════════════\n"
          + " Total Funds Processed : " + funds.size()        + "\n"
          + " Total Mismatches      : " + mismatches.size()   + "\n"
          + " Total Skipped         : " + skippedFunds.size() + "\n"
          + "══════════════════════════════════════════════";
        logger.info(summary);
        test.info(summary);

        if (!mismatches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n────────────────────────────────────────\n");
            sb.append(" MISMATCHED FUNDS\n");
            sb.append("────────────────────────────────────────\n");
            for (String[] m : mismatches) {
                // indices: 0=planId, 1=fundName, 2=effective, 3=expectedCat, 4=actualCat,
                //          5=isProvisional, 6=rawOpinionName, 7=provOpinionName
                sb.append(String.format(
                    " PLAN_ID                  : %s%n"
                  + " FUND_NAME                : %s%n"
                  + " IS_PROVISIONAL           : %s%n"
                  + " OPINION_NAME             : %s%n"
                  + " PROVISIONAL_OPINION_NAME : %s%n"
                  + " EFFECTIVE_OPINION        : %s%n"
                  + " EXPECTED_CATEGORY        : %s%n"
                  + " ACTUAL_CATEGORY          : %s%n"
                  + "────────────────────────────────────────%n",
                    m[0], m[1], m[5], m[6], m[7], m[2], m[3], m[4]));
            }
            String mismatchBlock = sb.toString();
            logger.error(mismatchBlock);
            test.fail(mismatchBlock);
        }

        if (!skippedFunds.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n────────────────────────────────────────\n");
            sb.append(" SKIPPED FUNDS\n");
            sb.append("────────────────────────────────────────\n");
            for (String[] s : skippedFunds) {
                sb.append(String.format(
                    " PLAN_ID : %s%n FUND    : %s%n OPINION : %s%n REASON  : %s%n"
                  + "────────────────────────────────────────%n",
                    s[0], s[1], s[2], s[3]));
            }
            test.warning(sb.toString());
        }

        Assert.assertTrue(allMatch, "Category count mismatch between PRD expectation and Fund Review API");
    }

    // ================================================================
    // TC_BACKEND_002 — Per-fund expected vs actual category
    // ================================================================

    @Test(description = "TC_BACKEND_002 — Per-fund: expected category (PRD) matches Fund Review API classification_type")
    public void tc_backend_002_verifyPerFundCategoryMatchesApi() {
        ExtentTest test = getExtentTest();

        // ── Step 1-2: Fund Review API ─────────────────────────────
        test.info("Step 1: Fetching Fund Review API");
        List<FundReviewItem> funds = reviewService.getFunds();
        assertDataAvailable(funds, "Fund Review API returned no data");
        test.info("Fund Review API: " + funds.size() + " funds fetched");

        List<Integer> planIds = funds.stream().map(f -> f.planId).collect(Collectors.toList());

        // ── Step 3: Fund Opinion API ──────────────────────────────
        test.info("Step 3: Fetching Fund Opinion API for " + planIds.size() + " plan IDs");
        Map<Integer, FundOpinionItem> opinions = opinionService.getOpinionsByPlanIds(planIds);
        test.info("Fund Opinion API: " + opinions.size() + " opinions fetched");

        // ── Step 4-5: Calculate and compare ──────────────────────
        List<ValidationResult> results = new ArrayList<>();

        for (FundReviewItem fund : funds) {
            FundOpinionItem  opinion = opinions.get(fund.planId);
            ValidationResult.Builder rb = ValidationResult.builder(fund.planId, fund.fundName)
                    .fundReviewApiCategory(fund.actualCategory);

            if (opinion == null) {
                rb.opinionName("N/A — opinion not returned by Fund Opinion API for planId=" + fund.planId)
                  .expectedCategory(null)
                  .backendStatus(ValidationResult.Status.SKIPPED)
                  .skipReason("Missing field: Fund Opinion API returned no record for planId=" + fund.planId
                          + ". Impact: cannot calculate expected category.");
            } else {
                String effective = opinion.effectiveOpinion();
                rb.isProvisional(opinion.isProvisional)
                  .rawOpinionName(opinion.opinionName)
                  .provisionalOpinionName(opinion.provisionalOpinionName)
                  .opinionName(effective)
                  .usingProvisional(opinion.usingProvisional());

                CalculationResult calc = calculator.calculateExpectedCategory(effective, fund.fundName);
                rb.planType(calc.planType);

                if (calc.isSkipped()) {
                    rb.expectedCategory(null)
                      .backendStatus(ValidationResult.Status.SKIPPED)
                      .skipReason(calc.skipReason);
                } else {
                    rb.expectedCategory(calc.expectedCategory)
                      .note(calc.note);
                    boolean match = calc.expectedCategory.equalsIgnoreCase(fund.actualCategory);
                    rb.backendStatus(match
                            ? ValidationResult.Status.MATCH
                            : ValidationResult.Status.MISMATCH);
                }
            }

            results.add(rb.build());
        }

        // ── Step 6: Log to ExtentReport ───────────────────────────
        logBackendResults(test, results);

        // ── Summary assertion ─────────────────────────────────────
        long mismatches = results.stream()
                .filter(r -> r.backendStatus == ValidationResult.Status.MISMATCH)
                .count();
        Assert.assertEquals(mismatches, 0,
                mismatches + " fund(s) have category mismatch between PRD calculation and Fund Review API. "
                + "See ExtentReport for details.");
    }

    // ================================================================
    // Report helpers
    // ================================================================

    private void logBackendResults(ExtentTest test, List<ValidationResult> results) {
        long total     = results.size();
        long matched   = results.stream().filter(r -> r.backendStatus == ValidationResult.Status.MATCH).count();
        long mismatched= results.stream().filter(r -> r.backendStatus == ValidationResult.Status.MISMATCH).count();
        long skipped   = results.stream().filter(r -> r.backendStatus == ValidationResult.Status.SKIPPED).count();

        // Per-fund rows
        for (ValidationResult r : results) {
            String row = buildBackendRow(r);
            switch (r.backendStatus) {
                case MATCH:
                    test.pass(row);
                    break;
                case MISMATCH:
                    test.fail(MarkupHelper.createLabel(row, ExtentColor.RED));
                    break;
                case SKIPPED:
                    test.warning(row + " | SKIP REASON: " + r.skipReason);
                    break;
                default:
                    test.info(row);
            }
        }

        // Summary block
        String summary = buildSummaryBlock(
                total, matched, mismatched, skipped, "BACKEND");
        test.info(summary);

        // Mismatch detail block
        List<ValidationResult> mismatches = results.stream()
                .filter(r -> r.backendStatus == ValidationResult.Status.MISMATCH)
                .collect(Collectors.toList());
        if (!mismatches.isEmpty()) {
            test.fail(buildMismatchDetail(mismatches, "BACKEND"));
        }
    }

    private String buildBackendRow(ValidationResult r) {
        return String.format(
                "PLAN_ID=%-6d | FUND_NAME=%-42s | IS_PROVISIONAL=%-5s"
                + " | OPINION_NAME=%-15s | PROVISIONAL_OPINION_NAME=%-15s | EFFECTIVE_OPINION=%-15s"
                + " | PLAN_TYPE=%-18s | EXPECTED_CATEGORY=%-10s | ACTUAL_CATEGORY=%-10s | %s",
                r.planId,
                r.fundName,
                r.isProvisional,
                r.rawOpinionName,
                r.provisionalOpinionName,
                r.opinionName,
                r.planType,
                r.expectedCategory != null ? r.expectedCategory : "SKIPPED",
                r.fundReviewApiCategory,
                r.backendStatus.name());
    }

    private void assertDataAvailable(java.util.Collection<?> col, String msg) {
        if (col == null || col.isEmpty()) throw new SkipException(msg);
    }

    private void assertDataAvailable(java.util.Map<?,?> map, String msg) {
        if (map == null || map.isEmpty()) throw new SkipException(msg);
    }

    static String buildSummaryBlock(long total, long matched, long mismatched, long skipped, String label) {
        return "\n══════════════════════════════════════════════\n"
             + " FUND REVIEW " + label + " VALIDATION SUMMARY\n"
             + "══════════════════════════════════════════════\n"
             + " Total Funds Processed  : " + total     + "\n"
             + " Total " + label + " Matches   : " + matched    + "\n"
             + " Total " + label + " Mismatches: " + mismatched + "\n"
             + " Total Skipped          : " + skipped   + "\n"
             + "══════════════════════════════════════════════";
    }

    static String buildMismatchDetail(List<ValidationResult> mismatches, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n────────────────────────────────────────\n");
        sb.append(" ").append(label).append(" MISMATCHES\n");
        sb.append("────────────────────────────────────────\n");
        for (ValidationResult r : mismatches) {
            sb.append(String.format(
                " Plan ID                  : %d%n"
              + " Fund Name                : %s%n"
              + " Is Provisional           : %s%n"
              + " Opinion Name             : %s%n"
              + " Provisional Opinion Name : %s%n"
              + " Effective Opinion        : %s%n"
              + " Plan Type                : %s%n"
              + " Expected Category        : %s%n"
              + " API Actual               : %s%n"
              + " UI Actual                : %s%n"
              + " Note                     : %s%n"
              + "────────────────────────────────────────%n",
                r.planId, r.fundName, r.isProvisional, r.rawOpinionName,
                r.provisionalOpinionName, r.opinionName, r.planType,
                r.expectedCategory,
                r.fundReviewApiCategory,
                r.uiCategory.isBlank() ? "N/A" : r.uiCategory,
                r.note.isBlank() ? "—" : r.note));
        }
        return sb.toString();
    }
}
