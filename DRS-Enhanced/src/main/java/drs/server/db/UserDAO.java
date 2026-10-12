package drs.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import drs.common.model.Role;
import drs.common.model.User;

/**
 * Data access for the users table.
 */
public class UserDAO {

    /** A user row including credential material that never leaves the server. */
    public static final class Credentials {
        public final User user;
        public final String passwordHash;
        public final String salt;
        public final String publicKey;

        Credentials(User user, String passwordHash, String salt, String publicKey) {
            this.user = user;
            this.passwordHash = passwordHash;
            this.salt = salt;
            this.publicKey = publicKey;
        }
    }

    private final ConnectionPool pool;

    public UserDAO(ConnectionPool pool) {
        this.pool = pool;
    }

    /** @return the credentials for username, if the user exists */
    public Optional<Credentials> findByUsername(String username) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, username, full_name, role, password_hash, salt, public_key, created_at "
                            + "FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    User u = new User(rs.getInt("id"), rs.getString("username"),
                            rs.getString("full_name"), Role.valueOf(rs.getString("role")));
                    u.setCreatedAt(DbTime.format(rs.getTimestamp("created_at")));
                    return Optional.of(new Credentials(u, rs.getString("password_hash"),
                            rs.getString("salt"), rs.getString("public_key")));
                }
            }
        });
    }

    /** @return the public key of a user, or null */
    public String findPublicKey(int userId) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT public_key FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    /**
     * Inserts a user; MySQL assigns the ID.
     *
     * @return the generated user ID
     */
    public int insert(String username, String fullName, Role role, String hash, String salt) throws SQLException {
        return pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (username, full_name, role, password_hash, salt, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, fullName);
                ps.setString(3, role.name());
                ps.setString(4, hash);
                ps.setString(5, salt);
                ps.setTimestamp(6, DbTime.now());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        });
    }

    /** Stores the user's RSA public key used to verify their signatures. */
    public void updatePublicKey(int userId, String publicKey) throws SQLException {
        pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET public_key = ? WHERE id = ?")) {
                ps.setString(1, publicKey);
                ps.setInt(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** Time-stamps a successful login. */
    public void updateLastLogin(int userId) throws SQLException {
        pool.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET last_login = ? WHERE id = ?")) {
                ps.setTimestamp(1, DbTime.now());
                ps.setInt(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** @return number of users */
    public int count() throws SQLException {
        return pool.execute(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }
}
