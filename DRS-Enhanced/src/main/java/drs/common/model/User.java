package drs.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * An authenticated DRS user. Password material never leaves the server, so this
 * class only carries identity and role information.
 */
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String fullName;
    private Role role;
    private String createdAt;

    /** No-args constructor. */
    public User() { }

    /**
     * Full constructor.
     *
     * @param id       database identifier
     * @param username unique login name
     * @param fullName display name
     * @param role     access role
     */
    public User(int id, String username, String fullName, Role role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** @return true if the user has controller-level (or admin) authority */
    public boolean isStaff() {
        return role == Role.CONTROLLER || role == Role.ADMIN;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", fullName, username, role.getLabel());
    }
}
