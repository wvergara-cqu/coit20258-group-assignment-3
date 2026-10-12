package drs.server.security;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import drs.common.model.Role;
import drs.common.net.Command;

/**
 * Role-based access control matrix (FR-6). Held as an EnumMap of EnumSets so the
 * check performed before every command is O(1). Commands not listed for a role
 * are denied by default.
 */
public final class AccessControl {

    /** Commands available before login. */
    private static final Set<Command> ANONYMOUS = EnumSet.of(Command.PING, Command.LOGIN, Command.REGISTER);

    private static final Map<Role, Set<Command>> PERMISSIONS = new EnumMap<>(Role.class);

    static {
        Set<Command> common = EnumSet.of(Command.PING, Command.LOGOUT, Command.GET_ACTIVE_WARNINGS);

        Set<Command> citizen = EnumSet.copyOf(common);
        citizen.addAll(EnumSet.of(Command.SUBMIT_REPORT, Command.GET_MY_REPORTS));

        Set<Command> responder = EnumSet.copyOf(common);
        responder.addAll(EnumSet.of(Command.GET_MY_TASKS, Command.UPDATE_TASK_STATUS, Command.GET_TASK_HISTORY));

        Set<Command> controller = EnumSet.copyOf(common);
        controller.addAll(EnumSet.of(
                Command.SUBMIT_REPORT, Command.GET_MY_REPORTS, Command.GET_REPORTS, Command.FILTER_REPORTS,
                Command.GET_ACTIVE_REPORTS, Command.UPDATE_REPORT_STATUS, Command.UPDATE_REPORT_PRIORITY,
                Command.GET_NEXT_IN_TRIAGE, Command.GET_SUMMARY,
                Command.GET_DEPARTMENTS, Command.GET_RESPONDERS, Command.GET_ASSIGNABLE_DEPARTMENTS,
                Command.GET_RECOMMENDED_DEPARTMENTS, Command.TOGGLE_DEPARTMENT, Command.TOGGLE_RESPONDER,
                Command.SAVE_RESPONSE, Command.GET_RESPONSES,
                Command.SUGGEST_WARNING, Command.ISSUE_WARNING, Command.CANCEL_WARNING, Command.GET_ALL_WARNINGS,
                Command.GET_ALL_TASKS, Command.GET_TASK_HISTORY));

        Set<Command> admin = EnumSet.copyOf(controller);
        admin.add(Command.GET_AUDIT_LOG);

        PERMISSIONS.put(Role.CITIZEN, citizen);
        PERMISSIONS.put(Role.RESPONDER, responder);
        PERMISSIONS.put(Role.CONTROLLER, controller);
        PERMISSIONS.put(Role.ADMIN, admin);
    }

    private AccessControl() { }

    /**
     * @param role    the session's role, or null when not logged in
     * @param command the command requested
     * @return true if the command is permitted
     */
    public static boolean isAllowed(Role role, Command command) {
        if (command == null) {
            return false;
        }
        if (role == null) {
            return ANONYMOUS.contains(command);
        }
        return PERMISSIONS.get(role).contains(command);
    }

    /** @return true if the role may see decrypted reporter personal data */
    public static boolean canViewPersonalData(Role role) {
        return role == Role.CONTROLLER || role == Role.ADMIN;
    }
}
