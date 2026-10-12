package drs.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A government or emergency department (agency) that can be assigned to a response.
 */
public class Department implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String type;
    private String contactPerson;
    private String phone;
    private boolean available;

    /** No-args constructor; available by default. */
    public Department() {
        this.available = true;
    }

    /**
     * Full constructor.
     *
     * @param id            identifier
     * @param name          department name
     * @param type          category, e.g. "Medical"
     * @param contactPerson primary contact
     * @param phone         contact phone
     */
    public Department(int id, String name, String type, String contactPerson, String phone) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.available = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    /** @return "Available" or "Deployed" for table display */
    public String getAvailabilityStatus() {
        return available ? "Available" : "Deployed";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Department d && d.id == id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, type);
    }
}
