package drs.server;

import java.io.Serializable;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import drs.common.model.AuditEntry;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterResponse;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.FieldTask;
import drs.common.model.PublicWarning;
import drs.common.model.Signable;
import drs.common.model.TaskStatusUpdate;
import drs.common.model.User;
import drs.common.net.Command;
import drs.common.net.Request;
import drs.common.net.Response;
import drs.common.security.SignatureUtil;
import drs.server.security.AccessControl;
import drs.server.security.Session;
import drs.server.service.AuditService;
import drs.server.service.AuthService;
import drs.server.service.CoordinationService;
import drs.server.service.ReportService;
import drs.server.service.ServiceException;
import drs.server.service.TaskService;
import drs.server.service.WarningService;

/**
 * Routes each Request to the appropriate service after enforcing access control
 * and, for critical commands, verifying the client's digital signature. One
 * dispatcher instance is shared by all client-handler threads; it holds no
 * per-client state (that lives in Session).
 */
public class RequestDispatcher {

    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());
    private static final int AUDIT_LIMIT = 300;

    private final AuthService auth;
    private final ReportService reports;
    private final CoordinationService coordination;
    private final WarningService warnings;
    private final TaskService tasks;
    private final AuditService audit;

    public RequestDispatcher(AuthService auth, ReportService reports, CoordinationService coordination,
                             WarningService warnings, TaskService tasks, AuditService audit) {
        this.auth = auth;
        this.reports = reports;
        this.coordination = coordination;
        this.warnings = warnings;
        this.tasks = tasks;
        this.audit = audit;
    }

    /**
     * Handles one request. Never throws: every failure becomes an error Response.
     */
    public Response handle(Request request, Session session) {
        if (request == null || request.getCommand() == null) {
            return Response.error("Malformed request.");
        }
        Command cmd = request.getCommand();
        User user = session.getUser();
        if (!AccessControl.isAllowed(user == null ? null : user.getRole(), cmd)) {
            audit.record(session, "DENIED_" + cmd, "COMMAND", 0);
            return Response.error(user == null ? "Please log in first."
                    : "Access denied: your role (" + user.getRole() + ") cannot perform " + cmd + ".");
        }
        try {
            return route(cmd, request, session);
        } catch (ServiceException e) {
            return Response.error(e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error on " + cmd, e);
            return Response.error("Database error - please try again.");
        } catch (ClassCastException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            return Response.error("Invalid data for " + cmd + ".");
        }
    }

    private Response route(Command cmd, Request req, Session session) throws ServiceException, SQLException {
        User user = session.getUser();
        switch (cmd) {
            case PING:
                return Response.ok("pong");
            case LOGIN:
                return login(req, session);
            case REGISTER: {
                Object[] a = args(req);
                User u = auth.registerCitizen((String) a[0], (String) a[1], (String) a[2]);
                Session tmp = new Session(session.getRemoteAddress());
                tmp.setUser(u);
                audit.record(tmp, "REGISTER", "USER", u.getId());
                return Response.ok("Account created. You can now log in.", u);
            }
            case LOGOUT:
                audit.record(session, "LOGOUT", "USER", user.getId());
                session.clear();
                return Response.ok("Logged out.");

            // ----- reports -----
            case SUBMIT_REPORT: {
                DisasterReport r = (DisasterReport) req.getPayload();
                String sig = requireSignature(req, session, r, "SUBMIT_REPORT", "REPORT");
                reports.submit(r, user, sig);
                audit.record(session, "SUBMIT_REPORT", "REPORT", r.getId(), r.canonicalForm(), sig, true);
                return Response.ok("Report #" + r.getId() + " received. Tracking ID: DRS-" + r.getId()
                        + ". Priority: " + r.getPriority(), r);
            }
            case GET_REPORTS:
                return list(reports.all(), user);
            case GET_MY_REPORTS:
                return list(reports.byUser(user), user);
            case FILTER_REPORTS: {
                Object[] a = args(req);
                return list(reports.filter((DisasterType) a[0], (DisasterStatus) a[1]), user);
            }
            case GET_ACTIVE_REPORTS:
                return list(reports.active(), user);
            case UPDATE_REPORT_STATUS: {
                Object[] a = args(req);
                DisasterReport r = reports.updateStatus((Integer) a[0], (DisasterStatus) a[1]);
                audit.record(session, "STATUS_" + r.getStatus().name(), "REPORT", r.getId());
                return Response.ok("Report #" + r.getId() + " is now " + r.getStatus() + ".", r);
            }
            case UPDATE_REPORT_PRIORITY: {
                Object[] a = args(req);
                DisasterReport r = reports.updatePriority((Integer) a[0], (Integer) a[1]);
                audit.record(session, "PRIORITY_OVERRIDE", "REPORT", r.getId());
                return Response.ok("Priority of report #" + r.getId() + " set to " + r.getPriority() + ".", r);
            }
            case GET_NEXT_IN_TRIAGE: {
                DisasterReport next = reports.nextInTriage();
                return next == null ? Response.ok("The triage queue is empty.")
                        : Response.ok(reports.getTriageQueue().size() + " report(s) awaiting assessment.",
                        ReportService.mask(reports.get(next.getId()), user));
            }
            case GET_SUMMARY:
                return Response.ok("Summary", reports.summary());

            // ----- coordination -----
            case GET_DEPARTMENTS:
                return Response.ok("Departments", new ArrayList<>(coordination.departments()));
            case GET_RESPONDERS:
                return Response.ok("Responders", new ArrayList<>(coordination.responders()));
            case GET_ASSIGNABLE_DEPARTMENTS:
                return Response.ok("Assignable departments", new ArrayList<>(coordination.assignableDepartments()));
            case GET_RECOMMENDED_DEPARTMENTS:
                return Response.ok("Recommended departments",
                        new ArrayList<>(coordination.recommended((Integer) req.getPayload())));
            case TOGGLE_DEPARTMENT: {
                int id = (Integer) req.getPayload();
                coordination.toggleDepartment(id);
                audit.record(session, "TOGGLE_DEPARTMENT", "DEPARTMENT", id);
                return Response.ok("Department #" + id + " availability changed.");
            }
            case TOGGLE_RESPONDER: {
                int id = (Integer) req.getPayload();
                coordination.toggleResponder(id);
                audit.record(session, "TOGGLE_RESPONDER", "RESPONDER", id);
                return Response.ok("Responder #" + id + " availability changed.");
            }
            case SAVE_RESPONSE: {
                DisasterResponse dr = (DisasterResponse) req.getPayload();
                String sig = requireSignature(req, session, dr, "SAVE_RESPONSE", "RESPONSE");
                coordination.saveResponse(dr, user, sig);
                audit.record(session, "SAVE_RESPONSE", "RESPONSE", dr.getId(), dr.canonicalForm(), sig, true);
                return Response.ok("Response #" + dr.getId() + " saved for report #" + dr.getReportId() + "; "
                        + dr.getResponderCount() + " field task(s) dispatched.", dr);
            }
            case GET_RESPONSES:
                return Response.ok("Responses", new ArrayList<>(coordination.allResponses()));

            // ----- enhanced feature 1: public warnings -----
            case SUGGEST_WARNING:
                return Response.ok("Suggested warning", warnings.suggest((Integer) req.getPayload()));
            case ISSUE_WARNING: {
                PublicWarning w = (PublicWarning) req.getPayload();
                String sig = requireSignature(req, session, w, "ISSUE_WARNING", "WARNING");
                warnings.issue(w, user, sig);
                audit.record(session, "ISSUE_WARNING", "WARNING", w.getId(), w.canonicalForm(), sig, true);
                return Response.ok(w.getLevel() + " #" + w.getId() + " broadcast for " + w.getAffectedArea()
                        + " until " + w.getExpiresAt() + ".", w);
            }
            case CANCEL_WARNING: {
                int id = (Integer) req.getPayload();
                warnings.cancel(id);
                audit.record(session, "CANCEL_WARNING", "WARNING", id);
                return Response.ok("Warning #" + id + " cancelled.");
            }
            case GET_ACTIVE_WARNINGS:
                return Response.ok("Active warnings", new ArrayList<>(warnings.active()));
            case GET_ALL_WARNINGS:
                return Response.ok("All warnings", new ArrayList<>(warnings.all()));

            // ----- enhanced feature 2: field tasks -----
            case GET_MY_TASKS:
                return Response.ok("My tasks", new ArrayList<>(tasks.myTasks(user)));
            case GET_ALL_TASKS:
                return Response.ok("All tasks", new ArrayList<>(tasks.all()));
            case UPDATE_TASK_STATUS: {
                TaskStatusUpdate u = (TaskStatusUpdate) req.getPayload();
                String sig = requireSignature(req, session, u, "UPDATE_TASK_STATUS", "TASK");
                tasks.update(u, user, sig, true);
                audit.record(session, "TASK_" + u.getToStatus().name(), "TASK", u.getTaskId(),
                        u.canonicalForm(), sig, true);
                return Response.ok("Task #" + u.getTaskId() + " is now " + u.getToStatus() + ".", u);
            }
            case GET_TASK_HISTORY:
                return Response.ok("History", new ArrayList<>(tasks.history((Integer) req.getPayload(), user)));

            // ----- security -----
            case GET_AUDIT_LOG: {
                List<AuditEntry> log = audit.recent(AUDIT_LIMIT);
                return Response.ok("Audit log", new ArrayList<>(log));
            }
            default:
                return Response.error("Unsupported command " + cmd + ".");
        }
    }

    private Response login(Request req, Session session) throws ServiceException, SQLException {
        Object[] a = args(req);
        User u;
        try {
            u = auth.login((String) a[0], (String) a[1]);
        } catch (ServiceException e) {
            audit.record(session, "LOGIN_FAILED", "USER", 0);
            throw e;
        }
        PublicKey key = auth.enrolKey(u, (String) a[2]);
        session.setUser(u);
        session.setPublicKey(key);
        audit.record(session, "LOGIN", "USER", u.getId());
        return Response.ok("Welcome, " + u.getFullName() + ".", u);
    }

    /**
     * Verifies the signature over the payload's canonical form. Unsigned or
     * badly signed requests are rejected and audited, so every stored critical
     * action is provably attributable to its author.
     *
     * @return the verified signature
     */
    private String requireSignature(Request req, Session session, Signable payload, String action, String entity)
            throws ServiceException {
        String sig = req.getSignature();
        boolean ok = payload != null && SignatureUtil.verify(payload.canonicalForm(), sig, session.getPublicKey());
        if (!ok) {
            audit.record(session, "REJECTED_" + action, entity, 0,
                    payload == null ? null : payload.canonicalForm(), sig, false);
            throw new ServiceException("Digital signature verification failed - request rejected.");
        }
        return sig;
    }

    private static Object[] args(Request req) {
        return (Object[]) req.getPayload();
    }

    private static Response list(List<DisasterReport> list, User viewer) {
        ArrayList<DisasterReport> out = new ArrayList<>(list.size());
        for (DisasterReport r : list) {
            out.add(ReportService.mask(r, viewer));
        }
        return Response.ok(out.size() + " report(s)", (Serializable) out);
    }
}
