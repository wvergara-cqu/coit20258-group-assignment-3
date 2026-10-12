package drs.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * An individual field responder belonging to a department. A responder may be
 * linked to a login account (username) so that they can update their own tasks.
 */
public class Responder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String role;
    private String phone;
    private Department department;
    private String username;
    private boolean available;

    /** No-args constructor; available by default. */
    public Responder() {
        this.available = true;
    }

    /**
     * Full constructor.
     *
     * @param id         identifier
     * @param name       full name
     * @param role       job title
     * @param phone      contact phone
     * @param department owning department
     */
    public Responder(int id, String name, String role, String phone, Department department) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.department = department;
        this.available = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    /** @return the department name or "N/A" */
    public String getDepartmentName() {
        return department != null ? department.getName() : "N/A";
    }

    /** @return "Available" or "Deployed" for table display */
    public String getAvailabilityStatus() {
        return available ? "Available" : "Deployed";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Responder r && r.id == id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s - %s, %s", name, role, getDepartmentName());
    }
}
