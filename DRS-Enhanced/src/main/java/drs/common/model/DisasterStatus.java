package drs.common.model;

/**
 * Lifecycle status of a disaster report: REPORTED -> ASSESSING -> RESPONDING -> RESOLVED.
 */
public enum DisasterStatus {

    REPORTED("Reported"),
    ASSESSING("Assessing"),
    RESPONDING("Responding"),
    RESOLVED("Resolved");

    private final String label;

    DisasterStatus(String label) { this.label = label; }

    /** @return the human readable label */
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
