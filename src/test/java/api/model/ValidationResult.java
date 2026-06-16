package api.model;

/**
 * Per-fund validation record used by both backend and UI tests.
 *
 * Fields:
 *  planId                  — from Fund Review API
 *  fundName                — from Fund Review API
 *  isProvisional           — is_provisional flag from Fund Opinion API
 *  rawOpinionName          — opinion_name from Fund Opinion API (before effective resolution)
 *  provisionalOpinionName  — provisional_opinion_name from Fund Opinion API
 *  opinionName             — effective opinion (result of FundOpinionItem.effectiveOpinion())
 *  usingProvisional        — true when provisional_opinion_name drove the effective opinion
 *  planType                — resolved plan type (e.g. "Direct Growth") or "" if unknown
 *  expectedCategory        — PRD calculator result (null → SKIPPED)
 *  fundReviewApiCategory   — classification_type from Fund Review API
 *  uiCategory              — category found in UI (only populated by FundReviewUiValidationTest)
 *  backendStatus           — MATCH / MISMATCH / SKIPPED
 *  uiStatus                — MATCH / MISMATCH / SKIPPED / PENDING
 *  skipReason              — human-readable reason when status == SKIPPED
 *  note                    — supplementary info (e.g. ETF exception applied)
 */
public final class ValidationResult {

    public enum Status { MATCH, MISMATCH, SKIPPED, PENDING }

    public final int     planId;
    public final String  fundName;
    public final boolean isProvisional;
    public final String  rawOpinionName;
    public final String  provisionalOpinionName;
    public final String  opinionName;           // effective opinion
    public final boolean usingProvisional;
    public final String  planType;
    public final String  expectedCategory;      // null when calculation was skipped
    public final String  fundReviewApiCategory;
    public       String  uiCategory;            // mutable — set by UI test
    public       Status  backendStatus;
    public       Status  uiStatus;
    public final String  skipReason;
    public final String  note;

    private ValidationResult(Builder b) {
        this.planId                = b.planId;
        this.fundName              = b.fundName;
        this.isProvisional         = b.isProvisional;
        this.rawOpinionName        = b.rawOpinionName;
        this.provisionalOpinionName = b.provisionalOpinionName;
        this.opinionName           = b.opinionName;
        this.usingProvisional      = b.usingProvisional;
        this.planType              = b.planType;
        this.expectedCategory      = b.expectedCategory;
        this.fundReviewApiCategory = b.fundReviewApiCategory;
        this.uiCategory            = b.uiCategory;
        this.backendStatus         = b.backendStatus;
        this.uiStatus              = b.uiStatus;
        this.skipReason            = b.skipReason;
        this.note                  = b.note;
    }

    // ----------------------------------------------------------------

    public static Builder builder(int planId, String fundName) {
        return new Builder(planId, fundName);
    }

    public static final class Builder {
        private final int    planId;
        private final String fundName;
        private boolean isProvisional          = false;
        private String  rawOpinionName         = "";
        private String  provisionalOpinionName = "";
        private String  opinionName            = "";
        private boolean usingProvisional       = false;
        private String  planType               = "";
        private String  expectedCategory       = null;
        private String  fundReviewApiCategory  = "";
        private String  uiCategory             = "";
        private Status  backendStatus          = Status.PENDING;
        private Status  uiStatus               = Status.PENDING;
        private String  skipReason             = "";
        private String  note                   = "";

        private Builder(int planId, String fundName) {
            this.planId   = planId;
            this.fundName = fundName;
        }

        public Builder isProvisional(boolean v)            { isProvisional = v;                              return this; }
        public Builder rawOpinionName(String v)            { rawOpinionName = v != null ? v : "";            return this; }
        public Builder provisionalOpinionName(String v)    { provisionalOpinionName = v != null ? v : "";    return this; }
        public Builder opinionName(String v)               { opinionName = v != null ? v : "";               return this; }
        public Builder usingProvisional(boolean v)         { usingProvisional = v;                           return this; }
        public Builder planType(String v)                  { planType = v != null ? v : "";                  return this; }
        public Builder expectedCategory(String v)          { expectedCategory = v;                           return this; }
        public Builder fundReviewApiCategory(String v)     { fundReviewApiCategory = v != null ? v : "";     return this; }
        public Builder uiCategory(String v)                { uiCategory = v != null ? v : "";                return this; }
        public Builder backendStatus(Status v)             { backendStatus = v;                              return this; }
        public Builder uiStatus(Status v)                  { uiStatus = v;                                   return this; }
        public Builder skipReason(String v)                { skipReason = v != null ? v : "";                return this; }
        public Builder note(String v)                      { note = v != null ? v : "";                      return this; }

        public ValidationResult build() { return new ValidationResult(this); }
    }
}
