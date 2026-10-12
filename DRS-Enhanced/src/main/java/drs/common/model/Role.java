package drs.common.model;

/**
 * Access roles used for role-based access control (FR-6). The server checks the
 * role of the authenticated session before executing every command.
 */
public enum Role {

    CITIZEN("Public Citizen"),
    CONTROLLER("Response Controller"),
    RESPONDER("Field Responder"),
    ADMIN("Administrator");

    private final String label;

    Role(String label) { this.label = label; }

    /** @return the human readable label */
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
