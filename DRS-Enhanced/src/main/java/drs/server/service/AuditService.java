package drs.server.service;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import drs.common.model.AuditEntry;
import drs.common.security.SignatureUtil;
import drs.server.db.AuditDAO;
import drs.server.security.Session;

/**
 * Records every state-changing action with a server timestamp, the SHA-256 hash
 * of the payload and the signature verification result (non-repudiation).
 */
public class AuditService {

    private static final Logger LOG = Logger.getLogger(AuditService.class.getName());

    private final AuditDAO dao;

    public AuditService(AuditDAO dao) {
        this.dao = dao;
    }

    /**
     * Writes an audit entry. Failures are logged but never block the action.
     *
     * @param canonical signed text, or null when nothing was signed
     * @param signature client signature, or null
     * @param verified  result of verifying the signature
     */
    public void record(Session session, String action, String entity, int entityId,
                       String canonical, String signature, boolean verified) {
        String hash = canonical == null ? null : SignatureUtil.sha256Hex(canonical);
        AuditEntry e = new AuditEntry(session.getUsername(), action, entity, entityId, hash,
                signature != null, verified);
        Integer userId = session.isAuthenticated() ? session.getUser().getId() : null;
        try {
            dao.insert(e, userId, signature);
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Audit write failed for " + action, ex);
        }
    }

    /** Convenience overload for unsigned actions. */
    public void record(Session session, String action, String entity, int entityId) {
        record(session, action, entity, entityId, null, null, false);
    }

    /** @return the most recent entries */
    public List<AuditEntry> recent(int limit) throws SQLException {
        return dao.findRecent(limit);
    }
}
