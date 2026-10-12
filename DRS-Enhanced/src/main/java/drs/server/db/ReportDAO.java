package drs.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.Severity;
import drs.server.security.CryptoService;

/**
 * Data access for disaster_reports. Reporter name and phone are encrypted with
 * AES-256-GCM before INSERT and decrypted after SELECT, so they are never stored
 * in plain text.
 */
public class ReportDAO {

    private static final String SELECT = "SELECT r.*, u.username AS reported_by_username "
            + "FROM disaster_reports r LEFT JOIN users u ON u.id = r.reported_by ";
    private static final String ORDER = " ORDER BY r.priority DESC, r.created_at ASC";

    private final ConnectionPool pool;
    private final CryptoService crypto;

    public ReportDAO(ConnectionPool pool, CryptoService crypto) {
        this.pool = pool;
        this.crypto = crypto;
    }

    /**
     * Inserts a report. The generated ID and server timestamps are written back
     * into the supplied object.
     *
     * @param userId    submitting user (nullable for imported data)
     * @param signature client signature (nullable for imported data)
     * @return the generated report ID
     */
    public int insert(DisasterReport r, Integer userId, String signature) throws SQLException {
        Timestamp now = DbTime.now();
        return insert(r, userId, signature, now);
    }

    /** Inserts a report with an explicit creation time (used by the A2 data loader). */
    public int insert(DisasterReport r, Integer userId, String signature, Timestamp createdAt) throws SQLException {
        int id = pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO disaster_reports (type, severity, location, description, reporter_name_enc, "
                            + "reporter_phone_enc, status, priority, reported_by, signature, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, r.getType().name());
                ps.setString(2, r.getSeverity().name());
                ps.setString(3, r.getLocation());
                ps.setString(4, r.getDescription());
                ps.setString(5, crypto.encrypt(r.getReporterName()));
                ps.setString(6, crypto.encrypt(r.getReporterPhone()));
                ps.setString(7, r.getStatus().name());
                ps.setInt(8, r.getPriority());
                if (userId == null) {
                    ps.setNull(9, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(9, userId);
                }
                ps.setString(10, signature);
                ps.setTimestamp(11, createdAt);
                ps.setTimestamp(12, createdAt);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        });
        r.setId(id);
        r.setTimestamp(DbTime.format(createdAt));
        r.setUpdatedAt(DbTime.format(createdAt));
        return id;
    }

    /** @return all reports, highest priority first */
    public List<DisasterReport> findAll() throws SQLException {
        return query(SELECT + ORDER);
    }

    /** @return the report with the given ID */
    public Optional<DisasterReport> findById(int id) throws SQLException {
        List<DisasterReport> list = query(SELECT + "WHERE r.id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** @return reports with the given status, highest priority first */
    public List<DisasterReport> findByStatus(DisasterStatus status) throws SQLException {
        return query(SELECT + "WHERE r.status = ?" + ORDER, status.name());
    }

    /** @return ASSESSING and RESPONDING reports */
    public List<DisasterReport> findActive() throws SQLException {
        return query(SELECT + "WHERE r.status IN ('ASSESSING','RESPONDING')" + ORDER);
    }

    /** @return reports submitted by a user */
    public List<DisasterReport> findByUser(int userId) throws SQLException {
        return query(SELECT + "WHERE r.reported_by = ? ORDER BY r.created_at DESC", userId);
    }

    /** @return reports filtered by optional type and status (null = any) */
    public List<DisasterReport> filter(DisasterType type, DisasterStatus status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (type != null) {
            sql.append(" AND r.type = ?");
            params.add(type.name());
        }
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status.name());
        }
        sql.append(ORDER);
        return query(sql.toString(), params.toArray());
    }

    /** @return true if a row was updated */
    public boolean updateStatus(int id, DisasterStatus status) throws SQLException {
        return update("UPDATE disaster_reports SET status = ?, updated_at = ? WHERE id = ?", status.name(), id);
    }

    /** @return true if a row was updated */
    public boolean updatePriority(int id, int priority) throws SQLException {
        return update("UPDATE disaster_reports SET priority = ?, updated_at = ? WHERE id = ?", priority, id);
    }

    /** @return report counts keyed by status */
    public Map<DisasterStatus, Integer> countByStatus() throws SQLException {
        return pool.execute(c -> {
            Map<DisasterStatus, Integer> counts = new EnumMap<>(DisasterStatus.class);
            for (DisasterStatus s : DisasterStatus.values()) {
                counts.put(s, 0);
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT status, COUNT(*) FROM disaster_reports GROUP BY status")) {
                while (rs.next()) {
                    counts.put(DisasterStatus.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }
            return counts;
        });
    }

    private boolean update(String sql, Object value, int id) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, value);
                ps.setTimestamp(2, DbTime.now());
                ps.setInt(3, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    private List<DisasterReport> query(String sql, Object... params) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                List<DisasterReport> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
                return list;
            }
        });
    }

    private DisasterReport map(ResultSet rs) throws SQLException {
        DisasterReport r = new DisasterReport();
        r.setId(rs.getInt("id"));
        r.setType(DisasterType.valueOf(rs.getString("type")));
        r.setSeverity(Severity.valueOf(rs.getString("severity")));
        r.setLocation(rs.getString("location"));
        r.setDescription(rs.getString("description"));
        r.setReporterName(crypto.decrypt(rs.getString("reporter_name_enc")));
        r.setReporterPhone(crypto.decrypt(rs.getString("reporter_phone_enc")));
        r.setStatus(DisasterStatus.valueOf(rs.getString("status")));
        r.setPriority(rs.getInt("priority"));        // after setType/setSeverity so overrides survive
        r.setReportedBy(rs.getString("reported_by_username"));
        r.setTimestamp(DbTime.format(rs.getTimestamp("created_at")));
        r.setUpdatedAt(DbTime.format(rs.getTimestamp("updated_at")));
        return r;
    }
}
