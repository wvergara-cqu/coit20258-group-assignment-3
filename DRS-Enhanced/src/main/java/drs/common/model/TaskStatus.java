package drs.common.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced Feature 2 - lifecycle of a field task (UC-5, FR-4), implemented as a
 * finite state machine. The allowed transitions are held in an EnumMap of
 * EnumSets, giving O(1) validation of every update.
 */
public enum TaskStatus {

    DISPATCHED("Dispatched", "#7f8c8d"),
    ACKNOWLEDGED("Acknowledged", "#2980b9"),
    EN_ROUTE("En Route", "#8e44ad"),
    ON_SCENE("On Scene", "#e67e22"),
    COMPLETED("Completed", "#27ae60"),
    DECLINED("Declined", "#c0392b");

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        TRANSITIONS.put(DISPATCHED, EnumSet.of(ACKNOWLEDGED, DECLINED));
        TRANSITIONS.put(ACKNOWLEDGED, EnumSet.of(EN_ROUTE, DECLINED));
        TRANSITIONS.put(EN_ROUTE, EnumSet.of(ON_SCENE));
        TRANSITIONS.put(ON_SCENE, EnumSet.of(COMPLETED));
        TRANSITIONS.put(COMPLETED, EnumSet.noneOf(TaskStatus.class));
        TRANSITIONS.put(DECLINED, EnumSet.noneOf(TaskStatus.class));
    }

    private final String label;
    private final String colourHex;

    TaskStatus(String label, String colourHex) {
        this.label = label;
        this.colourHex = colourHex;
    }

    public String getLabel() { return label; }
    public String getColourHex() { return colourHex; }

    /**
     * @param next the proposed next status
     * @return true if moving from this status to next is a legal transition
     */
    public boolean canTransitionTo(TaskStatus next) {
        return next != null && TRANSITIONS.get(this).contains(next);
    }

    /** @return the statuses reachable from this one */
    public Set<TaskStatus> nextStatuses() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    /** @return true if the task can no longer change */
    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    @Override
    public String toString() { return label; }
}
