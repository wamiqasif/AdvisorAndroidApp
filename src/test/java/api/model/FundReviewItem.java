package api.model;

public final class FundReviewItem {

    public final int planId;
    public final String fundName;
    public final String actualCategory;
    public final String concentrationTrigger;
    

    public FundReviewItem(
            int planId,
            String fundName,
            String actualCategory,
            String concentrationTrigger) {

        this.planId = planId;

        this.fundName =
                fundName != null
                        ? fundName.trim()
                        : "";

        this.actualCategory =
                actualCategory != null
                        ? actualCategory.trim().toUpperCase()
                        : "";

        this.concentrationTrigger =
                concentrationTrigger != null
                        ? concentrationTrigger.trim()
                        : "";
    }

    public boolean isOverConcentrated() {
        return concentrationTrigger != null
                && !concentrationTrigger.isBlank();
    }
   

    @Override
    public String toString() {
        return "FundReviewItem{"
                + "planId=" + planId
                + ", name='" + fundName + '\''
                + ", actualCategory='" + actualCategory + '\''
                + ", concentrationTrigger='" + concentrationTrigger + '\''
                + '}';
    }
}