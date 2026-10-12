package drs.common.model;

/** Lifecycle of a public warning. */
public enum WarningStatus {
    ACTIVE("Active"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String label;

    WarningStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
