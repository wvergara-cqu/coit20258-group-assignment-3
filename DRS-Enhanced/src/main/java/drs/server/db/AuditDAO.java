package drs.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import drs.common.model.AuditEntry;

/**
 * Append-only data access for the audit_log table. There is deliberately no
 * update or delete method.
 */
public class AuditDAO {

    private final ConnectionPool pool;

    public AuditDAO(ConnectionPool pool) {
        this.pool = pool;
    }

    /** Appends an audit entry, time-stamped by the server. */
    public void insert(AuditEntry e, Integer userId, String signature) throws SQLException {
        pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (user_id, username, action, entity, entity_id, payload_hash, signature, "
                            + "signed, verified, logged_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                if (userId == null) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(1, userId);
                }
                ps.setString(2, e.getUsername());
                ps.setString(3, e.getAction());
                ps.setString(4, e.getEntity());
                ps.setInt(5, e.getEntityId());
                ps.setString(6, e.getPayloadHash());
                ps.setString(7, signature);
                ps.setBoolean(8, e.isSigned());
                ps.setBoolean(9, e.isVerified());
                ps.setTimestamp(10, DbTime.now());
                return ps.executeUpdate();
            }
        });
    }

    /** @return the most recent entries, newest first */
    public List<AuditEntry> findRecent(int limit) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM audit_log ORDER BY id DESC LIMIT ?")) {
                ps.setInt(1, limit);
                List<AuditEntry> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AuditEntry e = new AuditEntry(rs.getString("username"), rs.getString("action"),
                                rs.getString("entity"), rs.getInt("entity_id"), rs.getString("payload_hash"),
                                rs.getBoolean("signed"), rs.getBoolean("verified"));
                        e.setId(rs.getInt("id"));
                        e.setLoggedAt(DbTime.format(rs.getTimestamp("logged_at")));
                        list.add(e);
                    }
                }
                return list;
            }
        });
    }
}
