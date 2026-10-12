package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enhanced Feature 2 - a task dispatched to one field responder as part of a
 * coordinated response. Responders progress the task through the TaskStatus
 * state machine from the field.
 */
public class FieldTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private int responseId;
    private int reportId;
    private int responderId;
    private String responderName;
    private String departmentName;
    private DisasterType disasterType;
    private String location;
    private String instructions;
    private TaskStatus status = TaskStatus.DISPATCHED;
    private String createdAt;
    private String updatedAt;

    /** No-args constructor. */
    public FieldTask() { }

    /**
     * Constructor for a new task.
     *
     * @param responseId   parent response
     * @param reportId     disaster report
     * @param responderId  assigned responder
     * @param instructions field instructions
     */
    public FieldTask(int responseId, int reportId, int responderId, String instructions) {
        this.responseId = responseId;
        this.reportId = reportId;
        this.responderId = responderId;
        this.instructions = instructions;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getResponseId() { return responseId; }
    public void setResponseId(int responseId) { this.responseId = responseId; }
    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }
    public int getResponderId() { return responderId; }
    public void setResponderId(int responderId) { this.responderId = responderId; }
    public String getResponderName() { return responderName; }
    public void setResponderName(String responderName) { this.responderName = responderName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public DisasterType getDisasterType() { return disasterType; }
    public void setDisasterType(DisasterType disasterType) { this.disasterType = disasterType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("Task #%d | report #%d | %s | %s | %s",
                id, reportId, responderName, location, status);
    }
}
