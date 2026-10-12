package drs.common.model;

/**
 * Public warning levels, modelled on the three-tier Australian Warning System.
 * Each level carries a default call-to-action and a colour used by the GUI.
 */
public enum WarningLevel {

    ADVICE("Advice", "Stay informed and monitor conditions.", "#f1c40f", 1),
    WATCH_AND_ACT("Watch and Act", "Conditions are changing. Prepare to leave or take protective action now.", "#e67e22", 2),
    EMERGENCY_WARNING("Emergency Warning", "Leave immediately if the path is safe, otherwise shelter in place now.", "#c0392b", 3);

    private final String label;
    private final String defaultAction;
    private final String colourHex;
    private final int rank;

    WarningLevel(String label, String defaultAction, String colourHex, int rank) {
        this.label = label;
        this.defaultAction = defaultAction;
        this.colourHex = colourHex;
        this.rank = rank;
    }

    public String getLabel() { return label; }
    public String getDefaultAction() { return defaultAction; }
    public String getColourHex() { return colourHex; }

    /** @return 1 (lowest) to 3 (highest); used to sort the warnings board */
    public int getRank() { return rank; }

    @Override
    public String toString() { return label; }
}
