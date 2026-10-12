package drs.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import drs.common.model.Department;
import drs.common.model.Responder;

/**
 * Data access for departments and responders.
 */
public class ResourceDAO {

    private final ConnectionPool pool;

    public ResourceDAO(ConnectionPool pool) {
        this.pool = pool;
    }

    /** @return generated department ID */
    public int insertDepartment(Department d) throws SQLException {
        int id = pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO departments (name, type, contact_person, phone, available, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, d.getName());
                ps.setString(2, d.getType());
                ps.setString(3, d.getContactPerson());
                ps.setString(4, d.getPhone());
                ps.setBoolean(5, d.isAvailable());
                ps.setTimestamp(6, DbTime.now());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        });
        d.setId(id);
        return id;
    }

    /** @return generated responder ID */
    public int insertResponder(Responder r) throws SQLException {
        int id = pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO responders (name, role, phone, department_id, username, available, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, r.getName());
                ps.setString(2, r.getRole());
                ps.setString(3, r.getPhone());
                ps.setInt(4, r.getDepartment().getId());
                ps.setString(5, r.getUsername());
                ps.setBoolean(6, r.isAvailable());
                ps.setTimestamp(7, DbTime.now());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        });
        r.setId(id);
        return id;
    }

    /** @return every department ordered by ID */
    public List<Department> findAllDepartments() throws SQLException {
        return pool.execute(c -> {
            List<Department> list = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT * FROM departments ORDER BY id")) {
                while (rs.next()) {
                    list.add(mapDepartment(rs));
                }
            }
            return list;
        });
    }

    /** @return every responder with its department, ordered by ID */
    public List<Responder> findAllResponders() throws SQLException {
        return pool.execute(c -> {
            Map<Integer, Department> depts = new HashMap<>();
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM departments")) {
                while (rs.next()) {
                    Department d = mapDepartment(rs);
                    depts.put(d.getId(), d);
                }
            }
            List<Responder> list = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT * FROM responders ORDER BY id")) {
                while (rs.next()) {
                    Responder r = new Responder(rs.getInt("id"), rs.getString("name"), rs.getString("role"),
                            rs.getString("phone"), depts.get(rs.getInt("department_id")));
                    r.setUsername(rs.getString("username"));
                    r.setAvailable(rs.getBoolean("available"));
                    list.add(r);
                }
            }
            return list;
        });
    }

    /** @return the responder linked to a login, if any */
    public Optional<Responder> findResponderByUsername(String username) throws SQLException {
        return findAllResponders().stream().filter(r -> username.equals(r.getUsername())).findFirst();
    }

    /** Sets a department's availability. */
    public boolean setDepartmentAvailable(int id, boolean available) throws SQLException {
        return setFlag("UPDATE departments SET available = ?, updated_at = ? WHERE id = ?", available, id);
    }

    /** Sets a responder's availability. */
    public boolean setResponderAvailable(int id, boolean available) throws SQLException {
        return setFlag("UPDATE responders SET available = ?, updated_at = ? WHERE id = ?", available, id);
    }

    /** Flips a department's availability. */
    public boolean toggleDepartment(int id) throws SQLException {
        return setFlagRaw("UPDATE departments SET available = NOT available, updated_at = ? WHERE id = ?", id);
    }

    /** Flips a responder's availability. */
    public boolean toggleResponder(int id) throws SQLException {
        return setFlagRaw("UPDATE responders SET available = NOT available, updated_at = ? WHERE id = ?", id);
    }

    /** @return number of departments */
    public int countDepartments() throws SQLException {
        return pool.execute(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM departments")) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    private boolean setFlag(String sql, boolean value, int id) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBoolean(1, value);
                ps.setTimestamp(2, DbTime.now());
                ps.setInt(3, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    private boolean setFlagRaw(String sql, int id) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setTimestamp(1, DbTime.now());
                ps.setInt(2, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    static Department mapDepartment(ResultSet rs) throws SQLException {
        Department d = new Department(rs.getInt("id"), rs.getString("name"), rs.getString("type"),
                rs.getString("contact_person"), rs.getString("phone"));
        d.setAvailable(rs.getBoolean("available"));
        return d;
    }
}
