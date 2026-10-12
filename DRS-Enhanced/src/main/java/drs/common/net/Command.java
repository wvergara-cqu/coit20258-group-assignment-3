package drs.common.net;

/**
 * Every operation the client can ask the multi-threaded server to perform.
 * Access to each command is governed by drs.server.security.AccessControl.
 */
public enum Command {
    // Session
    PING, LOGIN, REGISTER, LOGOUT,
    // Disaster reports (A2 features, now persisted in MySQL)
    SUBMIT_REPORT, GET_REPORTS, GET_MY_REPORTS, FILTER_REPORTS, GET_ACTIVE_REPORTS,
    UPDATE_REPORT_STATUS, UPDATE_REPORT_PRIORITY, GET_NEXT_IN_TRIAGE, GET_SUMMARY,
    // Coordination and resources
    GET_DEPARTMENTS, GET_RESPONDERS, GET_ASSIGNABLE_DEPARTMENTS, GET_RECOMMENDED_DEPARTMENTS,
    TOGGLE_DEPARTMENT, TOGGLE_RESPONDER, SAVE_RESPONSE, GET_RESPONSES,
    // Enhanced feature 1 - public warnings
    SUGGEST_WARNING, ISSUE_WARNING, CANCEL_WARNING, GET_ACTIVE_WARNINGS, GET_ALL_WARNINGS,
    // Enhanced feature 2 - field task tracking
    GET_MY_TASKS, GET_ALL_TASKS, UPDATE_TASK_STATUS, GET_TASK_HISTORY,
    // Security
    GET_AUDIT_LOG
}
