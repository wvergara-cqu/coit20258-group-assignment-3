package drs.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import drs.common.model.DisasterType;
import drs.common.model.FieldTask;
import drs.common.model.TaskStatus;
import drs.common.model.TaskStatusUpdate;

/**
 * Data access for field_tasks and task_status_updates (Enhanced Feature 2).
 */
public class TaskDAO {

    private static final String SELECT = "SELECT t.*, p.name AS responder_name, d.name AS dept_name, "
            + "r.type AS disaster_type, r.location FROM field_tasks t "
            + "JOIN responders p ON p.id = t.responder_id "
            + "JOIN departments d ON d.id = p.department_id "
            + "JOIN disaster_reports r ON r.id = t.report_id ";

    private final ConnectionPool pool;

    public TaskDAO(ConnectionPool pool) {
        this.pool = pool;
    }

    /** @return tasks for one responder, open tasks first then newest */
    public List<FieldTask> findByResponder(int responderId) throws SQLException {
        return query(SELECT + "WHERE t.responder_id = ? "
                + "ORDER BY (t.status IN ('COMPLETED','DECLINED')), t.updated_at DESC", responderId);
    }

    /** @return every task, newest first */
    public List<FieldTask> findAll() throws SQLException {
        return query(SELECT + "ORDER BY t.updated_at DESC, t.id DESC");
    }

    /** @return a task by ID */
    public Optional<FieldTask> findById(int id) throws SQLException {
        List<FieldTask> list = query(SELECT + "WHERE t.id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Applies a status change atomically. The UPDATE is conditional on the task
     * still being in the expected from-status (optimistic locking), so two
     * concurrent updates cannot both succeed.
     *
     * @return true if the change was applied
     */
    public boolean applyUpdate(TaskStatusUpdate u, int userId, String signature, boolean verified,
                               int responderId) throws SQLException {
        Timestamp now = DbTime.now();
        boolean applied = pool.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE field_tasks SET status = ?, updated_at = ? WHERE id = ? AND status = ?")) {
                ps.setString(1, u.getToStatus().name());
                ps.setTimestamp(2, now);
                ps.setInt(3, u.getTaskId());
                ps.setString(4, u.getFromStatus().name());
                if (ps.executeUpdate() != 1) {
                    return false;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO task_status_updates (task_id, from_status, to_status, note, updated_by, "
                            + "signature, signature_verified, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, u.getTaskId());
                ps.setString(2, u.getFromStatus().name());
                ps.setString(3, u.getToStatus().name());
                ps.setString(4, u.getNote());
                ps.setInt(5, userId);
                ps.setString(6, signature);
                ps.setBoolean(7, verified);
                ps.setTimestamp(8, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    u.setId(keys.getInt(1));
                }
            }
            if (u.getToStatus().isTerminal()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE responders SET available = TRUE, updated_at = ? WHERE id = ?")) {
                    ps.setTimestamp(1, now);
                    ps.setInt(2, responderId);
                    ps.executeUpdate();
                }
            }
            return true;
        });
        if (applied) {
            u.setUpdatedAt(DbTime.format(now));
            u.setSignatureVerified(verified);
        }
        return applied;
    }

    /** @return the status history of a task, oldest first */
    public List<TaskStatusUpdate> findHistory(int taskId) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT h.*, u.username FROM task_status_updates h LEFT JOIN users u ON u.id = h.updated_by "
                            + "WHERE h.task_id = ? ORDER BY h.id")) {
                ps.setInt(1, taskId);
                List<TaskStatusUpdate> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TaskStatusUpdate u = new TaskStatusUpdate(rs.getInt("task_id"),
                                TaskStatus.valueOf(rs.getString("to_status")), rs.getString("note"));
                        u.setId(rs.getInt("id"));
                        u.setFromStatus(TaskStatus.valueOf(rs.getString("from_status")));
                        u.setUpdatedBy(rs.getString("username"));
                        u.setSignatureVerified(rs.getBoolean("signature_verified"));
                        u.setUpdatedAt(DbTime.format(rs.getTimestamp("updated_at")));
                        list.add(u);
                    }
                }
                return list;
            }
        });
    }

    /** @return number of tasks for a report that are not yet in a terminal state */
    public int countOpenForReport(int reportId) throws SQLException {
        return count("SELECT COUNT(*) FROM field_tasks WHERE report_id = " + reportId
                + " AND status NOT IN ('COMPLETED','DECLINED')");
    }

    /** @return number of open tasks overall */
    public int countOpen() throws SQLException {
        return count("SELECT COUNT(*) FROM field_tasks WHERE status NOT IN ('COMPLETED','DECLINED')");
    }

    private int count(String sql) throws SQLException {
        return pool.execute(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    private List<FieldTask> query(String sql, Object... params) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                List<FieldTask> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FieldTask t = new FieldTask(rs.getInt("response_id"), rs.getInt("report_id"),
                                rs.getInt("responder_id"), rs.getString("instructions"));
                        t.setId(rs.getInt("id"));
                        t.setStatus(TaskStatus.valueOf(rs.getString("status")));
                        t.setResponderName(rs.getString("responder_name"));
                        t.setDepartmentName(rs.getString("dept_name"));
                        t.setDisasterType(DisasterType.valueOf(rs.getString("disaster_type")));
                        t.setLocation(rs.getString("location"));
                        t.setCreatedAt(DbTime.format(rs.getTimestamp("created_at")));
                        t.setUpdatedAt(DbTime.format(rs.getTimestamp("updated_at")));
                        list.add(t);
                    }
                }
                return list;
            }
        });
    }
}
