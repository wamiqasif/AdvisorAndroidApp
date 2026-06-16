package api.model;

public class FundItem {

    public final int    labelId;
    public final int    planId;
    public final String fundName;
    public final String classificationType;
    public final String adviceData;
    public final String actionData;
    public final boolean lockInState;
    public final String reason;

    public FundItem(int labelId, int planId, String fundName, String classificationType,
                    String adviceData, String actionData, boolean lockInState, String reason) {
        this.labelId           = labelId;
        this.planId            = planId;
        this.fundName          = fundName;
        this.classificationType = classificationType != null ? classificationType.toUpperCase() : "";
        this.adviceData        = adviceData  != null ? adviceData  : "";
        this.actionData        = actionData  != null ? actionData  : "";
        this.lockInState       = lockInState;
        this.reason            = reason      != null ? reason      : "";
    }

    @Override
    public String toString() {
        return "FundItem{planId=" + planId + ", name='" + fundName
                + "', type='" + classificationType + "', lock=" + lockInState + "}";
    }
}
