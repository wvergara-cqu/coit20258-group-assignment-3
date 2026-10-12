package drs.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import drs.common.model.Department;
import drs.common.model.DisasterResponse;
import drs.common.model.Responder;
import drs.common.model.TaskStatus;

/**
 * Data access for disaster_responses and their join tables. Saving a response is
 * a single transaction that also deploys the chosen resources, dispatches one
 * field task per responder (Enhanced Feature 2) and moves the report to
 * RESPONDING.
 */
public class ResponseDAO {

    private final ConnectionPool pool;

    public ResponseDAO(ConnectionPool pool) {
        this.pool = pool;
    }

    /**
     * Saves the response atomically.
     *
     * @param userId    coordinator who created it
     * @param signature client signature of the canonical form
     * @return the generated response ID
     */
    public int saveWithTasks(DisasterResponse response, int userId, String signature) throws SQLException {
        Timestamp now = DbTime.now();
        int id = pool.transaction(c -> {
            int responseId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO disaster_responses (report_id, notes, created_by, signature, created_at) "
                            + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, response.getReportId());
                ps.setString(2, response.getNotes());
                ps.setInt(3, userId);
                ps.setString(4, signature);
                ps.setTimestamp(5, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    responseId = keys.getInt(1);
                }
            }
            for (Department d : response.getAssignedDepartments()) {
                exec(c, "INSERT INTO response_departments (response_id, department_id) VALUES (?, ?)", responseId, d.getId());
                exec(c, "UPDATE departments SET available = FALSE, updated_at = NOW() WHERE id = ?", d.getId());
            }
            for (Responder r : response.getAssignedResponders()) {
                exec(c, "INSERT INTO response_responders (response_id, responder_id) VALUES (?, ?)", responseId, r.getId());
                exec(c, "UPDATE responders SET available = FALSE, updated_at = NOW() WHERE id = ?", r.getId());
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO field_tasks (response_id, report_id, responder_id, instructions, status, "
                                + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, responseId);
                    ps.setInt(2, response.getReportId());
                    ps.setInt(3, r.getId());
                    ps.setString(4, response.getNotes());
                    ps.setString(5, TaskStatus.DISPATCHED.name());
                    ps.setTimestamp(6, now);
                    ps.setTimestamp(7, now);
                    ps.executeUpdate();
                }
            }
            exec(c, "UPDATE disaster_reports SET status = 'RESPONDING', updated_at = NOW() "
                    + "WHERE id = ? AND status IN ('REPORTED','ASSESSING')", response.getReportId());
            return responseId;
        });
        response.setId(id);
        response.setCreatedAt(DbTime.format(now));
        return id;
    }

    private static void exec(Connection c, String sql, int... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    /** @return every response with its departments and responders, newest first */
    public List<DisasterResponse> findAll() throws SQLException {
        return pool.execute(c -> {
            Map<Integer, DisasterResponse> byId = new HashMap<>();
            List<DisasterResponse> ordered = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT r.*, u.username FROM disaster_responses r "
                         + "LEFT JOIN users u ON u.id = r.created_by ORDER BY r.id DESC")) {
                while (rs.next()) {
                    DisasterResponse dr = new DisasterResponse();
                    dr.setId(rs.getInt("id"));
                    dr.setReportId(rs.getInt("report_id"));
                    dr.setNotes(rs.getString("notes"));
                    dr.setCreatedBy(rs.getString("username"));
                    dr.setCreatedAt(DbTime.format(rs.getTimestamp("created_at")));
                    byId.put(dr.getId(), dr);
                    ordered.add(dr);
                }
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT rd.response_id, d.* FROM response_departments rd "
                         + "JOIN departments d ON d.id = rd.department_id")) {
                while (rs.next()) {
                    DisasterResponse dr = byId.get(rs.getInt("response_id"));
                    if (dr != null) {
                        dr.addDepartment(ResourceDAO.mapDepartment(rs));
                    }
                }
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT rr.response_id, p.id, p.name, p.role, p.phone "
                         + "FROM response_responders rr JOIN responders p ON p.id = rr.responder_id")) {
                while (rs.next()) {
                    DisasterResponse dr = byId.get(rs.getInt("response_id"));
                    if (dr != null) {
                        dr.addResponder(new Responder(rs.getInt("id"), rs.getString("name"),
                                rs.getString("role"), rs.getString("phone"), null));
                    }
                }
            }
            return ordered;
        });
    }
}
