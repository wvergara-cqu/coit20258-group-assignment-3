package drs.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import drs.common.model.DisasterType;
import drs.common.model.PublicWarning;
import drs.common.model.WarningLevel;
import drs.common.model.WarningStatus;

/**
 * Data access for public_warnings (Enhanced Feature 1).
 */
public class WarningDAO {

    private static final String SELECT = "SELECT w.*, r.type AS disaster_type, u.username "
            + "FROM public_warnings w JOIN disaster_reports r ON r.id = w.report_id "
            + "LEFT JOIN users u ON u.id = w.issued_by ";

    private final ConnectionPool pool;

    public WarningDAO(ConnectionPool pool) {
        this.pool = pool;
    }

    /** Inserts a warning; the ID, issue time and expiry are written back. */
    public int insert(PublicWarning w, int userId, String signature) throws SQLException {
        Timestamp now = DbTime.now();
        Timestamp expires = DbTime.plusHours(w.getValidHours());
        int id = pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO public_warnings (report_id, level, affected_area, radius_km, message, "
                            + "recommended_action, valid_hours, status, issued_by, signature, issued_at, expires_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, w.getReportId());
                ps.setString(2, w.getLevel().name());
                ps.setString(3, w.getAffectedArea());
                ps.setInt(4, w.getRadiusKm());
                ps.setString(5, w.getMessage());
                ps.setString(6, w.getRecommendedAction());
                ps.setInt(7, w.getValidHours());
                ps.setString(8, WarningStatus.ACTIVE.name());
                ps.setInt(9, userId);
                ps.setString(10, signature);
                ps.setTimestamp(11, now);
                ps.setTimestamp(12, expires);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        });
        w.setId(id);
        w.setStatus(WarningStatus.ACTIVE);
        w.setIssuedAt(DbTime.format(now));
        w.setExpiresAt(DbTime.format(expires));
        return id;
    }

    /** @return active, unexpired warnings, most severe first */
    public List<PublicWarning> findActive() throws SQLException {
        return query(SELECT + "WHERE w.status = 'ACTIVE' AND w.expires_at > NOW() "
                + "ORDER BY FIELD(w.level,'EMERGENCY_WARNING','WATCH_AND_ACT','ADVICE'), w.issued_at DESC");
    }

    /** @return all warnings, newest first */
    public List<PublicWarning> findAll() throws SQLException {
        return query(SELECT + "ORDER BY w.issued_at DESC, w.id DESC");
    }

    /** @return active warnings already issued for a report */
    public List<PublicWarning> findActiveForReport(int reportId) throws SQLException {
        return query(SELECT + "WHERE w.report_id = ? AND w.status = 'ACTIVE' AND w.expires_at > NOW()", reportId);
    }

    /** Cancels an active warning. */
    public boolean cancel(int id) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE public_warnings SET status = 'CANCELLED' WHERE id = ? AND status = 'ACTIVE'")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /**
     * Marks every overdue warning EXPIRED. Called periodically by the server.
     *
     * @return number of warnings expired
     */
    public int expireOverdue() throws SQLException {
        return pool.execute(c -> {
            try (Statement s = c.createStatement()) {
                return s.executeUpdate("UPDATE public_warnings SET status = 'EXPIRED' "
                        + "WHERE status = 'ACTIVE' AND expires_at <= NOW()");
            }
        });
    }

    /** Cancels every active warning linked to a report (used when it is resolved). */
    public int cancelForReport(int reportId) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE public_warnings SET status = 'CANCELLED' WHERE report_id = ? AND status = 'ACTIVE'")) {
                ps.setInt(1, reportId);
                return ps.executeUpdate();
            }
        });
    }

    private List<PublicWarning> query(String sql, Object... params) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                List<PublicWarning> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PublicWarning w = new PublicWarning(rs.getInt("report_id"),
                                WarningLevel.valueOf(rs.getString("level")), rs.getString("affected_area"),
                                rs.getInt("radius_km"), rs.getString("message"),
                                rs.getString("recommended_action"), rs.getInt("valid_hours"));
                        w.setId(rs.getInt("id"));
                        w.setStatus(WarningStatus.valueOf(rs.getString("status")));
                        w.setDisasterType(DisasterType.valueOf(rs.getString("disaster_type")));
                        w.setIssuedBy(rs.getString("username"));
                        w.setIssuedAt(DbTime.format(rs.getTimestamp("issued_at")));
                        w.setExpiresAt(DbTime.format(rs.getTimestamp("expires_at")));
                        list.add(w);
                    }
                }
                return list;
            }
        });
    }
}
