package drs.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A coordinated response to a disaster report: the departments and responders
 * assigned to it, the activities requested and the instructions for field teams.
 */
public class DisasterResponse implements Serializable, Signable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private int reportId;
    private List<Department> assignedDepartments = new ArrayList<>();
    private List<Responder> assignedResponders = new ArrayList<>();
    private String notes;
    private String createdBy;
    private String createdAt;

    /** No-args constructor. */
    public DisasterResponse() { }

    /**
     * Full constructor.
     *
     * @param id          identifier (0 for new)
     * @param reportId    the report being responded to
     * @param departments departments assigned
     * @param responders  responders assigned
     * @param notes       coordinator notes / field instructions
     */
    public DisasterResponse(int id, int reportId, List<Department> departments,
                            List<Responder> responders, String notes) {
        this.id = id;
        this.reportId = reportId;
        this.assignedDepartments = new ArrayList<>(departments);
        this.assignedResponders = new ArrayList<>(responders);
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }
    public List<Department> getAssignedDepartments() { return assignedDepartments; }
    public void setAssignedDepartments(List<Department> list) { this.assignedDepartments = new ArrayList<>(list); }
    public List<Responder> getAssignedResponders() { return assignedResponders; }
    public void setAssignedResponders(List<Responder> list) { this.assignedResponders = new ArrayList<>(list); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** @return number of departments, for table display */
    public int getDepartmentCount() { return assignedDepartments.size(); }

    /** @return number of responders, for table display */
    public int getResponderCount() { return assignedResponders.size(); }

    /** @return comma separated department names, for table display */
    public String getDepartmentNames() {
        return assignedDepartments.stream().map(Department::getName).collect(Collectors.joining(", "));
    }

    /** Adds a department if not already assigned. */
    public void addDepartment(Department d) {
        if (!assignedDepartments.contains(d)) {
            assignedDepartments.add(d);
        }
    }

    /** Adds a responder if not already assigned. */
    public void addResponder(Responder r) {
        if (!assignedResponders.contains(r)) {
            assignedResponders.add(r);
        }
    }

    @Override
    public String canonicalForm() {
        String depts = assignedDepartments.stream().map(d -> String.valueOf(d.getId()))
                .sorted().collect(Collectors.joining(","));
        String resps = assignedResponders.stream().map(r -> String.valueOf(r.getId()))
                .sorted().collect(Collectors.joining(","));
        return String.join("|", "RESPONSE", String.valueOf(reportId), depts, resps,
                notes == null ? "" : notes.trim());
    }

    @Override
    public String toString() {
        return String.format("Response #%d for report #%d | %d departments | %d responders | %s",
                id, reportId, getDepartmentCount(), getResponderCount(), createdAt);
    }
}
