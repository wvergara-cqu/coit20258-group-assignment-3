package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * An entry in the tamper-evident audit log. Each entry records who did what and
 * when, the SHA-256 hash of the submitted payload and whether the client's
 * digital signature was verified (non-repudiation).
 */
public class AuditEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String action;
    private String entity;
    private int entityId;
    private String payloadHash;
    private boolean signed;
    private boolean verified;
    private String loggedAt;

    /** No-args constructor. */
    public AuditEntry() { }

    /**
     * Full constructor.
     *
     * @param username    acting user
     * @param action      command name
     * @param entity      entity type affected
     * @param entityId    id of the entity affected
     * @param payloadHash SHA-256 of the canonical payload (may be null)
     * @param signed      whether a signature was supplied
     * @param verified    whether the signature verified
     */
    public AuditEntry(String username, String action, String entity, int entityId,
                      String payloadHash, boolean signed, boolean verified) {
        this.username = username;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.payloadHash = payloadHash;
        this.signed = signed;
        this.verified = verified;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public boolean isSigned() { return signed; }
    public void setSigned(boolean signed) { this.signed = signed; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }

    /** @return short signature status for table display */
    public String getSignatureStatus() {
        if (!signed) {
            return "Not signed";
        }
        return verified ? "Verified" : "INVALID";
    }

    /** @return abbreviated hash for table display */
    public String getShortHash() {
        return payloadHash == null ? "-" : payloadHash.substring(0, Math.min(16, payloadHash.length())) + "...";
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s#%d [%s]", loggedAt, username, action, entity, entityId, getSignatureStatus());
    }
}
