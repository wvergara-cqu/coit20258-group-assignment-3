package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Read-only dashboard statistics returned by the server (counts by status).
 */
public class IncidentSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int reported;
    private int assessing;
    private int responding;
    private int resolved;
    private int activeWarnings;
    private int openTasks;

    public int getReported() { return reported; }
    public void setReported(int reported) { this.reported = reported; }
    public int getAssessing() { return assessing; }
    public void setAssessing(int assessing) { this.assessing = assessing; }
    public int getResponding() { return responding; }
    public void setResponding(int responding) { this.responding = responding; }
    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }
    public int getActiveWarnings() { return activeWarnings; }
    public void setActiveWarnings(int activeWarnings) { this.activeWarnings = activeWarnings; }
    public int getOpenTasks() { return openTasks; }
    public void setOpenTasks(int openTasks) { this.openTasks = openTasks; }

    @Override
    public String toString() {
        return String.format("Reported %d | Assessing %d | Responding %d | Resolved %d | Active warnings %d | Open tasks %d",
                reported, assessing, responding, resolved, activeWarnings, openTasks);
    }
}
