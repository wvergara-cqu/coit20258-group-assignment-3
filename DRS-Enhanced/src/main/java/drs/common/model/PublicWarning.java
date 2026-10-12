package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enhanced Feature 1 - a localised public warning / evacuation advisory linked to
 * a disaster report (UC-6, FR-5). Issued by a controller, signed for
 * non-repudiation, and displayed on the public warnings board until it expires
 * or is cancelled.
 */
public class PublicWarning implements Serializable, Signable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private int reportId;
    private DisasterType disasterType;
    private WarningLevel level;
    private String affectedArea;
    private int radiusKm;
    private String message;
    private String recommendedAction;
    private int validHours;
    private WarningStatus status = WarningStatus.ACTIVE;
    private String issuedBy;
    private String issuedAt;
    private String expiresAt;

    /** No-args constructor. */
    public PublicWarning() { }

    /**
     * Full constructor for a new warning.
     *
     * @param reportId          linked disaster report
     * @param level             warning level
     * @param affectedArea      suburb / area description
     * @param radiusKm          radius of the affected zone in km
     * @param message           advisory text shown to the public
     * @param recommendedAction call to action
     * @param validHours        hours until the warning expires
     */
    public PublicWarning(int reportId, WarningLevel level, String affectedArea, int radiusKm,
                         String message, String recommendedAction, int validHours) {
        this.reportId = reportId;
        this.level = level;
        this.affectedArea = affectedArea;
        this.radiusKm = radiusKm;
        this.message = message;
        this.recommendedAction = recommendedAction;
        this.validHours = validHours;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }
    public DisasterType getDisasterType() { return disasterType; }
    public void setDisasterType(DisasterType disasterType) { this.disasterType = disasterType; }
    public WarningLevel getLevel() { return level; }
    public void setLevel(WarningLevel level) { this.level = level; }
    public String getAffectedArea() { return affectedArea; }
    public void setAffectedArea(String affectedArea) { this.affectedArea = affectedArea; }
    public int getRadiusKm() { return radiusKm; }
    public void setRadiusKm(int radiusKm) { this.radiusKm = radiusKm; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public int getValidHours() { return validHours; }
    public void setValidHours(int validHours) { this.validHours = validHours; }
    public WarningStatus getStatus() { return status; }
    public void setStatus(WarningStatus status) { this.status = status; }
    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }
    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    @Override
    public String canonicalForm() {
        return String.join("|", "WARNING", String.valueOf(reportId), String.valueOf(level),
                trim(affectedArea), String.valueOf(radiusKm), trim(message),
                trim(recommendedAction), String.valueOf(validHours));
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%d km) until %s",
                level, disasterType == null ? "Incident" : disasterType, affectedArea, radiusKm, expiresAt);
    }
}
