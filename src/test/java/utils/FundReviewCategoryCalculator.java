package utils;

/**
 * Calculates the expected Fund Review category from PRD rules.
 *
 * Source of truth: Fund Opinion API (opinion_name / provisional_opinion_name).
 * Input must NEVER come from Fund Review API's classification_type.
 *
 * Client-approved PRD rules (strictly as specified — do not add rules):
 *
 *   opinion_name = "Exit"        → EXIT   (plan type irrelevant)
 *   opinion_name = "New Fund"    → NEW-FUND (plan type irrelevant)
 *
 *   opinion_name = "Good"
 *     Direct Growth   → GOOD
 *     Regular Growth  → OPTIMIZE
 *     Regular Dividend→ OPTIMIZE
 *     Direct Dividend → OPTIMIZE
 *     [ETF exception: if fund is ETF, Direct/Regular Growth stays GOOD, not OPTIMIZE]
 *
 *   opinion_name = "Good Start"  → GOOD   (plan type irrelevant)
 *
 *   opinion_name = "Steady"
 *     Direct Growth   → STEADY
 *     Regular Growth  → OPTIMIZE
 *     Regular Dividend→ OPTIMIZE
 *     Direct Dividend → OPTIMIZE
 *     [ETF exception: if fund is ETF, stays STEADY, not OPTIMIZE]
 *
 *   opinion_name = "Steady Start"→ STEADY (plan type irrelevant)
 *   opinion_name = "Poor Start"  → STEADY (plan type irrelevant)
 *
 * Centralised here so PRD changes require only one-file edits.
 */
public final class FundReviewCategoryCalculator {

    // ----------------------------------------------------------------
    // Result wrapper
    // ----------------------------------------------------------------

    public static final class CalculationResult {

        public enum Outcome { CALCULATED, SKIPPED }

        public final Outcome outcome;
        public final String  expectedCategory;  // non-null when CALCULATED
        public final String  planType;          // resolved plan type display name
        public final String  skipReason;        // non-empty when SKIPPED
        public final String  note;              // supplementary info (ETF exception, provisional, etc.)

        private CalculationResult(Outcome o, String cat, String pt, String skip, String note) {
            this.outcome          = o;
            this.expectedCategory = cat;
            this.planType         = pt != null ? pt : "";
            this.skipReason       = skip != null ? skip : "";
            this.note             = note != null ? note : "";
        }

        public static CalculationResult of(String category, String planType) {
            return new CalculationResult(Outcome.CALCULATED, category, planType, null, null);
        }

        public static CalculationResult of(String category, String planType, String note) {
            return new CalculationResult(Outcome.CALCULATED, category, planType, null, note);
        }

        public static CalculationResult skip(String reason) {
            return new CalculationResult(Outcome.SKIPPED, null, null, reason, null);
        }

        public static CalculationResult skip(String reason, String planType) {
            return new CalculationResult(Outcome.SKIPPED, null, planType, reason, null);
        }

        public boolean isCalculated() { return outcome == Outcome.CALCULATED; }
        public boolean isSkipped()    { return outcome == Outcome.SKIPPED; }
    }

    // ----------------------------------------------------------------

    private final PlanTypeResolver planTypeResolver;

    public FundReviewCategoryCalculator() {
        this.planTypeResolver = new PlanTypeResolver();
    }

    // ----------------------------------------------------------------
    // Public entry point
    // ----------------------------------------------------------------

    /**
     * Calculates the expected category.
     *
     * @param effectiveOpinion  opinion_name (or provisional fallback) from Fund Opinion API
     * @param fundName          fund name from Fund Review API (for plan type + ETF detection)
     * @return CalculationResult — CALCULATED with category, or SKIPPED with reason
     */
//    public CalculationResult calculateExpectedCategory2(String effectiveOpinion, String fundName) {
//
//        // Guard: opinion must be present
//        if (effectiveOpinion == null || effectiveOpinion.isBlank()) {
//            return CalculationResult.skip(
//                    "Missing field: opinion_name and provisional_opinion_name are both null/blank. "
//                    + "Impact: category cannot be calculated.");
//        }
//
//        String opinion = effectiveOpinion.trim();
//        
//        
//        
//
//     // =========================
//     // ETF EXCEPTION
//     // =========================
//     if (planTypeResolver.isEtfFund(fundName)) {
//
//         switch (opinion.toUpperCase()) {
//
//             case "GOOD":
//                 return CalculationResult.of("GOOD", "ETF");
//
//             case "STEADY":
//                 return CalculationResult.of("STEADY", "ETF");
//
//             case "GOOD START":
//                 return CalculationResult.of("GOOD", "ETF");
//
//             case "STEADY START":
//                 return CalculationResult.of("STEADY", "ETF");
//
//             case "POOR START":
//                 return CalculationResult.of("STEADY", "ETF");
//
//             case "EXIT":
//                 return CalculationResult.of("EXIT", "ETF");
//
//             case "NEW FUND":
//                 return CalculationResult.of("NEW-FUND", "ETF");
//         }
//     }
//
//     // Existing logic continues here
//     if ("Exit".equalsIgnoreCase(opinion))
//         return CalculationResult.of("EXIT", "N/A");
//
//        // ── Plan-type-independent opinions ──────────────────────────
//        if ("Exit".equalsIgnoreCase(opinion))        return CalculationResult.of("EXIT",     "N/A");
//        if ("New Fund".equalsIgnoreCase(opinion))    return CalculationResult.of("NEW-FUND", "N/A");
//        if ("Good Start".equalsIgnoreCase(opinion))  return CalculationResult.of("GOOD",     "N/A",
//                "Good Start opinion maps directly to GOOD regardless of plan type");
//        if ("Steady Start".equalsIgnoreCase(opinion)) return CalculationResult.of("STEADY",  "N/A",
//                "Steady Start opinion maps directly to STEADY regardless of plan type");
//        if ("Poor Start".equalsIgnoreCase(opinion))  return CalculationResult.of("STEADY",   "N/A",
//                "Poor Start opinion maps directly to STEADY regardless of plan type");
//
//        // ── Plan-type-dependent opinions: "Good" and "Steady" ───────
//        if ("Good".equalsIgnoreCase(opinion))   return applyPlanTypeRules(fundName, "GOOD");
//        if ("Steady".equalsIgnoreCase(opinion)) return applyPlanTypeRules(fundName, "STEADY");
//
//        // ── Unrecognised opinion ─────────────────────────────────────
//        return CalculationResult.skip(
//                "Missing rule: opinion '" + opinion + "' has no PRD mapping. "
//                + "Impact: category cannot be calculated. "
//                + "Action: add rule to FundReviewCategoryCalculator if client approves.");
//    }
   
    public CalculationResult calculateExpectedCategory(
            String effectiveOpinion,
            String fundName,
            boolean overConcentrated) {

        // Guard: opinion must be present
        if (effectiveOpinion == null || effectiveOpinion.isBlank()) {
            return CalculationResult.skip(
                    "Missing field: opinion_name and provisional_opinion_name are both null/blank. "
                    + "Impact: category cannot be calculated.");
        }

        String opinion = effectiveOpinion.trim();

        // ============================================================
        // ETF EXCEPTION
        // ============================================================
        if (planTypeResolver.isEtfFund(fundName)) {

            switch (opinion.toUpperCase()) {

                case "GOOD":
                    return CalculationResult.of("GOOD", "ETF");

                case "STEADY":
                    return CalculationResult.of("STEADY", "ETF");

                case "GOOD START":
                    return CalculationResult.of("GOOD", "ETF");

                case "STEADY START":
                    return CalculationResult.of("STEADY", "ETF");

                case "POOR START":
                    return CalculationResult.of("STEADY", "ETF");

                case "EXIT":
                    return CalculationResult.of("EXIT", "ETF");

                case "NEW FUND":
                    return CalculationResult.of("NEW-FUND", "ETF");
            }
        }

        // ============================================================
        // PLAN-TYPE-INDEPENDENT OPINIONS
        // ============================================================

//        if ("Exit".equalsIgnoreCase(opinion)) {
//            return CalculationResult.of("EXIT", "N/A");
//        }
        
       
        
        if ("New Fund".equalsIgnoreCase(opinion)) {
            return CalculationResult.of("NEW-FUND", "N/A");
        }

        if ("Good Start".equalsIgnoreCase(opinion)) {
            return CalculationResult.of(
                    "GOOD",
                    "N/A",
                    "Good Start opinion maps directly to GOOD regardless of plan type");
        }

        if ("Steady Start".equalsIgnoreCase(opinion)) {
            return CalculationResult.of(
                    "STEADY",
                    "N/A",
                    "Steady Start opinion maps directly to STEADY regardless of plan type");
        }

        if ("Poor Start".equalsIgnoreCase(opinion)) {
            return CalculationResult.of(
                    "STEADY",
                    "N/A",
                    "Poor Start opinion maps directly to STEADY regardless of plan type");
        }

        // ============================================================
        // PLAN-TYPE-DEPENDENT OPINIONS
        // ============================================================

        CalculationResult result = null;
        
        if ("Exit".equalsIgnoreCase(opinion)) {
            result = CalculationResult.of("EXIT", "N/A");
        }

        if ("Good".equalsIgnoreCase(opinion)) {
            result = applyPlanTypeRules(fundName, "GOOD");
        }
        else if ("Steady".equalsIgnoreCase(opinion)) {
            result = applyPlanTypeRules(fundName, "STEADY");
        }

        
        boolean canApplyOverride =
                "Good".equalsIgnoreCase(opinion)
                || "Steady".equalsIgnoreCase(opinion)
                || "Exit".equalsIgnoreCase(opinion);

        if (result != null
                && canApplyOverride
                && overConcentrated) {

            return CalculationResult.of(
                    "OPTIMIZE",
                    "OVER_CONCENTRATION",
                    "Over concentration override applied");
        }

        if (result != null) {
            return result;
        }
        // ============================================================
        // OVER-CONCENTRATION OVERRIDE
        // Change Request:
        // GOOD/STEADY -> OPTIMIZE when concentration rule triggers
        // ============================================================

        if (result != null
                && overConcentrated
                && ("GOOD".equals(result.expectedCategory)
                    || "STEADY".equals(result.expectedCategory))) {

            return CalculationResult.of(
                    "OPTIMIZE",
                    "OVER_CONCENTRATION",
                    "Over concentration override applied");
        }

        if (result != null) {
            return result;
        }

        // ============================================================
        // UNKNOWN OPINION
        // ============================================================

        return CalculationResult.skip(
                "Missing rule: opinion '" + opinion + "' has no PRD mapping. "
                + "Impact: category cannot be calculated. "
                + "Action: add rule to FundReviewCategoryCalculator if client approves.");
    }

    // ----------------------------------------------------------------
    // Plan-type-dependent logic (Good / Steady)
    // ----------------------------------------------------------------

    private CalculationResult applyPlanTypeRules(String fundName, String baseCategory) {

        PlanTypeResolver.PlanType planType = planTypeResolver.resolvePlanType(fundName);

        // Guard: plan type must be resolvable
        if (planType == PlanTypeResolver.PlanType.UNKNOWN) {
            return CalculationResult.skip(
                    "Missing field: plan type unresolvable from fund name '" + fundName + "'. "
                    + "Expected suffix: Dir-G, Reg-G, Dir-IDCW, or Reg-IDCW. "
                    + "Impact: cannot determine whether fund maps to " + baseCategory + " or OPTIMIZE.",
                    PlanTypeResolver.PlanType.UNKNOWN.displayName);
        }

        // Direct Growth → stays as baseCategory (GOOD or STEADY)
        if (planType == PlanTypeResolver.PlanType.DIRECT_GROWTH) {
            return CalculationResult.of(baseCategory, planType.displayName);
        }

        // Regular Growth / Regular Dividend / Direct Dividend → OPTIMIZE
        // BUT ETF exception: ETF must NOT become OPTIMIZE solely because of plan type
        if (planType == PlanTypeResolver.PlanType.REGULAR_GROWTH
                || planType == PlanTypeResolver.PlanType.REGULAR_DIVIDEND
                || planType == PlanTypeResolver.PlanType.DIRECT_DIVIDEND) {

            PlanTypeResolver.EtfResult etf = planTypeResolver.detectEtf(fundName);

            if (etf.definitelyEtf) {
                // ETF exception: override OPTIMIZE back to baseCategory
                return CalculationResult.of(baseCategory, planType.displayName,
                        "ETF exception applied: fund identified as ETF (" + etf.reason + "). "
                        + "Stays " + baseCategory + " instead of OPTIMIZE.");
            }

            if (etf.indeterminate) {
                // Cannot confirm ETF or non-ETF from name alone
                // Report the gap and skip ETF-exception validation; still return OPTIMIZE
                // because the plan type rule applies unless proven otherwise.
                return CalculationResult.of("OPTIMIZE", planType.displayName,
                        "ETF status indeterminate: " + etf.reason + ". "
                        + "ETF exception validation SKIPPED for this fund. "
                        + "If this fund is an ETF it should map to " + baseCategory + " — verify manually.");
            }

            // Confirmed non-ETF: plan type rule applies
            return CalculationResult.of("OPTIMIZE", planType.displayName);
        }

        // Should not reach here given the enum is exhaustive except UNKNOWN (handled above)
        return CalculationResult.skip(
                "Unhandled plan type: " + planType.displayName + " for fund '" + fundName + "'.");
    }
}
