package api.model;

/**
 * Lean model for one Fund Review API record.
 *
 * Source: GET /api/v1/advisory/fund-review?label-ids={labelId}
 * Fields read:  plan_data.plan_id, plan_data.name, classification_type
 *
 * NOTE: classification_type is stored as actualCategory for comparison
 * against the PRD-calculated expected category. It is NEVER used as
 * input to FundReviewCategoryCalculator.
 */
public final class FundReviewItem {

    public final int    planId;
    public final String fundName;
    public final String actualCategory; // classification_type from Fund Review API

    public FundReviewItem(int planId, String fundName, String actualCategory) {
        this.planId         = planId;
        this.fundName       = fundName != null ? fundName.trim() : "";
        this.actualCategory = actualCategory != null ? actualCategory.trim().toUpperCase() : "";
    }

    @Override
    public String toString() {
        return "FundReviewItem{planId=" + planId
                + ", name='" + fundName
                + "', actualCategory='" + actualCategory + "'}";
    }
}
