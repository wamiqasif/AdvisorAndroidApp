package utils;

/**
 * Derives plan type and ETF status from a fund name.
 *
 * Plan-type suffixes (client-approved):
 *   Dir-G     → Direct Growth
 *   Reg-G     → Regular Growth
 *   Dir-IDCW  → Direct Dividend
 *   Reg-IDCW  → Regular Dividend
 *
 * ETF indicators in fund name:
 *   "ETF"  → definite ETF
 *   "BeES" → definite ETF (Nippon India BeES series)
 *   Note: Fund name alone is not always sufficient to identify ETFs reliably.
 *         If an ETF cannot be identified from name, EtfResult.indeterminate() is returned.
 *
 * Do NOT add new plan-type or ETF rules without client PRD approval.
 */
public final class PlanTypeResolver {

    // ----------------------------------------------------------------
    // Plan type enum
    // ----------------------------------------------------------------

    public enum PlanType {
        DIRECT_GROWTH    ("Direct Growth"),
        REGULAR_GROWTH   ("Regular Growth"),
        DIRECT_DIVIDEND  ("Direct Dividend"),
        REGULAR_DIVIDEND ("Regular Dividend"),
        UNKNOWN          ("Unknown");

        public final String displayName;
        PlanType(String n) { this.displayName = n; }
    }

    // ----------------------------------------------------------------
    // ETF detection result
    // ----------------------------------------------------------------

    public static final class EtfResult {
        public final boolean definitelyEtf;
        public final boolean indeterminate;
        public final String  reason;

        private EtfResult(boolean etf, boolean indeterminate, String reason) {
            this.definitelyEtf  = etf;
            this.indeterminate  = indeterminate;
            this.reason         = reason;
        }

        public static EtfResult yes(String reason)           { return new EtfResult(true,  false, reason); }
        public static EtfResult no()                         { return new EtfResult(false, false, "No ETF indicator in name"); }
        public static EtfResult indeterminate(String reason) { return new EtfResult(false, true,  reason); }

        /** True when the fund cannot be confirmed as ETF or non-ETF from name alone. */
        public boolean cannotDetermine() { return indeterminate; }
    }

    // ----------------------------------------------------------------
    // Plan type resolution
    // ----------------------------------------------------------------

    /**
     * Resolves plan type from fund name using client-approved suffix patterns.
     * Returns PlanType.UNKNOWN if no recognised suffix is found.
     */
    public PlanType resolvePlanType(String fundName) {
        if (fundName == null || fundName.isBlank()) return PlanType.UNKNOWN;

        // Order matters: check Dir-IDCW / Reg-IDCW before Dir-G / Reg-G
        // to prevent "Dir-G" inside "Dir-IDCW" from matching first.
        if (fundName.contains("Dir-IDCW")) return PlanType.DIRECT_DIVIDEND;
        if (fundName.contains("Reg-IDCW")) return PlanType.REGULAR_DIVIDEND;
        if (fundName.contains("Dir-G"))    return PlanType.DIRECT_GROWTH;
        if (fundName.contains("Reg-G"))    return PlanType.REGULAR_GROWTH;
        if (fundName.contains("Inst-G"))   return PlanType.REGULAR_GROWTH;

        return PlanType.UNKNOWN;
    }

    
    public boolean isEtfFund(String fundName) {

        if (fundName == null) {
            return false;
        }

        String name = fundName.toUpperCase();

        return name.contains("ETF")
                || name.contains("BEES");
    }
    // ----------------------------------------------------------------
    // ETF detection
    // ----------------------------------------------------------------

    /**
     * Attempts to identify whether a fund is an ETF using name-based heuristics.
     *
     * When EtfResult.indeterminate() is returned, the calculator will skip
     * ETF-exception validation and report the missing field.
     */
    public EtfResult detectEtf(String fundName) {
        if (fundName == null || fundName.isBlank())
            return EtfResult.indeterminate("Fund name is null/blank");

        String upper = fundName.toUpperCase();

        // Definite ETF indicators
        if (upper.contains("ETF"))
            return EtfResult.yes("Name contains 'ETF'");
        if (fundName.contains("BeES"))
            return EtfResult.yes("Name contains 'BeES' (Nippon BeES ETF series)");

        // No ETF indicator found — treat as non-ETF.
        // NOTE: Some index funds are structured as ETFs but may not carry 'ETF' in their name.
        // API does not expose an isETF flag. If such a fund is encountered, the ETF exception
        // rule cannot be applied and the validation should be treated as SKIPPED for that
        // specific rule. The name-based check here is best-effort for available data.
        return EtfResult.no();
    }
}
