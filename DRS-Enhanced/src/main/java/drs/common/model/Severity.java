package drs.common.model;

/**
 * Severity levels of a disaster report. Severity contributes to the
 * priority score and drives the suggested public warning level.
 */
public enum Severity {

    LOW("Low", 10),
    MEDIUM("Medium", 20),
    HIGH("High", 30),
    CRITICAL("Critical", 40);

    private final String label;
    private final int priorityWeight;

    Severity(String label, int priorityWeight) {
        this.label = label;
        this.priorityWeight = priorityWeight;
    }

    /** @return the human readable label */
    public String getLabel() { return label; }

    /** @return the points this severity contributes to the priority score */
    public int getPriorityWeight() { return priorityWeight; }

    @Override
    public String toString() { return label; }
}
