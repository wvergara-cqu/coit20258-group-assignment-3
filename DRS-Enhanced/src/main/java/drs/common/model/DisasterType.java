package drs.common.model;

/**
 * Types of disaster the DRS can handle. Each constant carries a UI label and the
 * weight it contributes to the automatically computed priority score.
 */
public enum DisasterType {

    HURRICANE("Hurricane", 10),
    EARTHQUAKE("Earthquake", 9),
    FIRE("Fire", 8),
    FLOOD("Flood", 7),
    OTHER("Other", 5);

    private final String label;
    private final int priorityWeight;

    DisasterType(String label, int priorityWeight) {
        this.label = label;
        this.priorityWeight = priorityWeight;
    }

    /** @return the human readable label */
    public String getLabel() { return label; }

    /** @return the points this type contributes to the priority score */
    public int getPriorityWeight() { return priorityWeight; }

    @Override
    public String toString() { return label; }
}
