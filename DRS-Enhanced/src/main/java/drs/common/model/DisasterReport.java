package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * A single disaster report. Priority is computed automatically from the
 * disaster type and severity (FR-2) and can be overridden by a controller.
 * Reporter name and phone are personal data: they are encrypted at rest by the
 * server and masked for roles that do not need them.
 */
public class DisasterReport implements Serializable, Signable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Lowest possible manual priority. */
    public static final int MIN_PRIORITY = 1;
    /** Highest possible manual priority. */
    public static final int MAX_PRIORITY = 100;

    private int id;
    private DisasterType type;
    private Severity severity;
    private String location;
    private String description;
    private String reporterName;
    private String reporterPhone;
    private DisasterStatus status;
    private int priority;
    private String reportedBy;      // username of the submitting account
    private String timestamp;       // server time of submission
    private String updatedAt;       // server time of last change

    /** No-args constructor; status defaults to REPORTED. */
    public DisasterReport() {
        this.status = DisasterStatus.REPORTED;
    }

    /**
     * Full constructor that computes priority automatically.
     *
     * @param id            identifier (0 for a new report; the database assigns it)
     * @param type          disaster type
     * @param severity      severity level
     * @param location      location description
     * @param description   free text description
     * @param reporterName  name of the reporter
     * @param reporterPhone phone of the reporter
     */
    public DisasterReport(int id, DisasterType type, Severity severity, String location,
                          String description, String reporterName, String reporterPhone) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.location = location;
        this.description = description;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
        this.status = DisasterStatus.REPORTED;
        this.priority = computePriority(type, severity);
    }

    /**
     * Computes the automatic priority score. Range 15 (Other + Low) to 50 (Hurricane + Critical).
     *
     * @param type     disaster type
     * @param severity severity level
     * @return the priority score
     */
    public static int computePriority(DisasterType type, Severity severity) {
        if (type == null || severity == null) {
            return 0;
        }
        return type.getPriorityWeight() + severity.getPriorityWeight();
    }

    /**
     * @param priority a candidate priority value
     * @return true if the value is inside the permitted manual override range
     */
    public static boolean isValidPriority(int priority) {
        return priority >= MIN_PRIORITY && priority <= MAX_PRIORITY;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public DisasterType getType() { return type; }

    /** Sets the type and recomputes priority. */
    public void setType(DisasterType type) {
        this.type = type;
        this.priority = computePriority(this.type, this.severity);
    }

    public Severity getSeverity() { return severity; }

    /** Sets the severity and recomputes priority. */
    public void setSeverity(Severity severity) {
        this.severity = severity;
        this.priority = computePriority(this.type, this.severity);
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }
    public DisasterStatus getStatus() { return status; }
    public void setStatus(DisasterStatus status) { this.status = status; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /** @return a short label suitable for combo boxes */
    public String getSummary() {
        return String.format("#%d %s (%s) - %s [P%d]", id, type, severity, location, priority);
    }

    @Override
    public String canonicalForm() {
        return String.join("|", "REPORT", String.valueOf(type), String.valueOf(severity),
                safe(location), safe(description), safe(reporterName), safe(reporterPhone));
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    public String toString() {
        return String.format("Report #%d | %s | %s | %s | %s | priority %d",
                id, type, severity, location, status, priority);
    }
}
