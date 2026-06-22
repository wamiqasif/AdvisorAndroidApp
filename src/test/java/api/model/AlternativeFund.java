package api.model;

public final class AlternativeFund {

    public final int planId;
    public final String fundName;
    public final String categoryName;
    public final boolean isEtf;
    public final boolean isChecked;

    public AlternativeFund(
            int planId,
            String fundName,
            String categoryName,
            boolean isEtf,
            boolean isChecked) {

        this.planId = planId;
        this.fundName = fundName != null ? fundName.trim() : "";
        this.categoryName = categoryName != null ? categoryName.trim() : "";
        this.isEtf = isEtf;
        this.isChecked = isChecked;
    }

    public boolean isChecked() {
        return isChecked;
    }
}
