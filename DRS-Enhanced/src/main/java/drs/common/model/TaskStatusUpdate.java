package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enhanced Feature 2 - one timestamped, signed status change of a field task.
 * The full list of updates forms the immutable history of the task.
 */
public class TaskStatusUpdate implements Serializable, Signable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private int taskId;
    private TaskStatus fromStatus;
    private TaskStatus toStatus;
    private String note;
    private String updatedBy;
    private String updatedAt;
    private boolean signatureVerified;

    /** No-args constructor. */
    public TaskStatusUpdate() { }

    /**
     * Constructor for a new update request.
     *
     * @param taskId   task being updated
     * @param toStatus requested new status
     * @param note     optional field note
     */
    public TaskStatusUpdate(int taskId, TaskStatus toStatus, String note) {
        this.taskId = taskId;
        this.toStatus = toStatus;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public TaskStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(TaskStatus fromStatus) { this.fromStatus = fromStatus; }
    public TaskStatus getToStatus() { return toStatus; }
    public void setToStatus(TaskStatus toStatus) { this.toStatus = toStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public boolean isSignatureVerified() { return signatureVerified; }
    public void setSignatureVerified(boolean signatureVerified) { this.signatureVerified = signatureVerified; }

    /** @return "Verified" or "Unverified" for table display */
    public String getVerification() {
        return signatureVerified ? "Verified" : "Unverified";
    }

    @Override
    public String canonicalForm() {
        return String.join("|", "TASK_UPDATE", String.valueOf(taskId), String.valueOf(toStatus),
                note == null ? "" : note.trim());
    }

    @Override
    public String toString() {
        return String.format("%s: %s -> %s by %s (%s)", updatedAt, fromStatus, toStatus, updatedBy, getVerification());
    }
}
