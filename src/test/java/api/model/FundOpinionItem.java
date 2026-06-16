package api.model;

/**
 * Model for one Fund Opinion API record.
 *
 * Source: GET /api/v1/funds/fund-opinion-data/get-list?plan_id={csv}
 * Fields read: plan_id, opinion_name, provisional_opinion_name
 *
 * Rule: use opinion_name as primary source.
 *       Fall back to provisional_opinion_name only when opinion_name
 *       is null, blank, or unavailable.
 */
public final class FundOpinionItem {

    public final int    planId;
    public final String opinionName;             // primary
    public final String provisionalOpinionName;  // fallback
    public boolean isProvisional;

    public FundOpinionItem(
            int planId,
            boolean isProvisional,
            String opinionName,
            String provisionalOpinionName) {

        this.planId = planId;
        this.isProvisional = isProvisional;
        this.opinionName =
                opinionName != null ? opinionName.trim() : "";

        this.provisionalOpinionName =
                provisionalOpinionName != null
                        ? provisionalOpinionName.trim()
                        : "";
    }

    /**
     * Returns the effective opinion following the fallback rule:
     *  1. opinion_name if non-blank
     *  2. provisional_opinion_name if non-blank
     *  3. "" (blank — calculation will be skipped)
     */
    public String effectiveOpinion() {

        if (isProvisional
                && !provisionalOpinionName.isBlank()) {
            return provisionalOpinionName;
        }

        if (!opinionName.isBlank()) {
            return opinionName;
        }

        return provisionalOpinionName;
    }

    public boolean usingProvisional() {
        return isProvisional
                && !provisionalOpinionName.isBlank();
    }


    /** True when the effective opinion came from the provisional fallback. */
    public boolean usingProvisional2() {
        return opinionName.isBlank() && !provisionalOpinionName.isBlank();
    }

    @Override
    public String toString() {
        return "FundOpinionItem{planId=" + planId
                + ", opinion='" + opinionName
                + "', provisional='" + provisionalOpinionName + "'}";
    }
}
