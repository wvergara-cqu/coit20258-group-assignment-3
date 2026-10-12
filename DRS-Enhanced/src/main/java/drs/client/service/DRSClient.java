package drs.client.service;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;

import drs.client.net.ServerConnection;
import drs.client.security.ClientKeyStore;
import drs.common.model.AuditEntry;
import drs.common.model.Department;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterResponse;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.FieldTask;
import drs.common.model.IncidentSummary;
import drs.common.model.PublicWarning;
import drs.common.model.Responder;
import drs.common.model.Signable;
import drs.common.model.TaskStatusUpdate;
import drs.common.model.User;
import drs.common.net.Command;
import drs.common.net.Request;
import drs.common.net.Response;
import drs.common.security.SignatureUtil;

/**
 * Client-side model facade. JavaFX controllers call these typed methods and never
 * touch sockets or protocol objects directly, keeping the View/Controller layer
 * independent of the network tier.
 */
public class DRSClient implements AutoCloseable {

    private final ServerConnection connection;
    private KeyPair keyPair;
    private User currentUser;
    private String lastMessage = "";

    /** Connects to the server. */
    public DRSClient(String host, int port) throws ClientException {
        this.connection = new ServerConnection(host, port);
        try {
            connection.connect();
        } catch (IOException e) {
            throw new ClientException("Cannot reach the DRS server at " + host + ":" + port
                    + ". Is DRSServer running?", e);
        }
    }

    /** @return the message of the most recent successful call */
    public String getLastMessage() { return lastMessage; }

    /** @return the logged-in user, or null */
    public User getCurrentUser() { return currentUser; }

    public String getHost() { return connection.getHost(); }
    public int getPort() { return connection.getPort(); }

    // ------------------------------------------------------------------ session

    /** Logs in and enrols this machine's public signing key. */
    public User login(String username, String password) throws ClientException {
        try {
            keyPair = ClientKeyStore.loadOrCreate(username.trim());
        } catch (GeneralSecurityException | IOException e) {
            throw new ClientException("Cannot load the local signing key: " + e.getMessage(), e);
        }
        String pub = SignatureUtil.encodePublicKey(keyPair.getPublic());
        currentUser = call(Command.LOGIN, new Object[]{username.trim(), password, pub});
        return currentUser;
    }

    /** Self-registers a citizen account. */
    public User register(String username, String password, String fullName) throws ClientException {
        return call(Command.REGISTER, new Object[]{username, password, fullName});
    }

    /** Ends the session on the server. */
    public void logout() throws ClientException {
        call(Command.LOGOUT, null);
        currentUser = null;
    }

    // ------------------------------------------------------------------ reports

    public DisasterReport submitReport(DisasterReport r) throws ClientException { return callSigned(Command.SUBMIT_REPORT, r); }
    public List<DisasterReport> getReports() throws ClientException { return call(Command.GET_REPORTS, null); }
    public List<DisasterReport> getMyReports() throws ClientException { return call(Command.GET_MY_REPORTS, null); }
    public List<DisasterReport> getActiveReports() throws ClientException { return call(Command.GET_ACTIVE_REPORTS, null); }

    public List<DisasterReport> filterReports(DisasterType type, DisasterStatus status) throws ClientException {
        return call(Command.FILTER_REPORTS, new Object[]{type, status});
    }

    public DisasterReport updateStatus(int id, DisasterStatus status) throws ClientException {
        return call(Command.UPDATE_REPORT_STATUS, new Object[]{id, status});
    }

    public DisasterReport updatePriority(int id, int priority) throws ClientException {
        return call(Command.UPDATE_REPORT_PRIORITY, new Object[]{id, priority});
    }

    public DisasterReport nextInTriage() throws ClientException { return call(Command.GET_NEXT_IN_TRIAGE, null); }
    public IncidentSummary getSummary() throws ClientException { return call(Command.GET_SUMMARY, null); }

    // ------------------------------------------------------------- coordination

    public List<Department> getDepartments() throws ClientException { return call(Command.GET_DEPARTMENTS, null); }
    public List<Responder> getResponders() throws ClientException { return call(Command.GET_RESPONDERS, null); }
    public List<Department> getAssignableDepartments() throws ClientException { return call(Command.GET_ASSIGNABLE_DEPARTMENTS, null); }
    public List<Department> getRecommendedDepartments(int reportId) throws ClientException { return call(Command.GET_RECOMMENDED_DEPARTMENTS, reportId); }
    public void toggleDepartment(int id) throws ClientException { call(Command.TOGGLE_DEPARTMENT, id); }
    public void toggleResponder(int id) throws ClientException { call(Command.TOGGLE_RESPONDER, id); }
    public DisasterResponse saveResponse(DisasterResponse r) throws ClientException { return callSigned(Command.SAVE_RESPONSE, r); }
    public List<DisasterResponse> getResponses() throws ClientException { return call(Command.GET_RESPONSES, null); }

    // --------------------------------------------------------- feature 1: warnings

    public PublicWarning suggestWarning(int reportId) throws ClientException { return call(Command.SUGGEST_WARNING, reportId); }
    public PublicWarning issueWarning(PublicWarning w) throws ClientException { return callSigned(Command.ISSUE_WARNING, w); }
    public void cancelWarning(int id) throws ClientException { call(Command.CANCEL_WARNING, id); }
    public List<PublicWarning> getActiveWarnings() throws ClientException { return call(Command.GET_ACTIVE_WARNINGS, null); }
    public List<PublicWarning> getAllWarnings() throws ClientException { return call(Command.GET_ALL_WARNINGS, null); }

    // ------------------------------------------------------------ feature 2: tasks

    public List<FieldTask> getMyTasks() throws ClientException { return call(Command.GET_MY_TASKS, null); }
    public List<FieldTask> getAllTasks() throws ClientException { return call(Command.GET_ALL_TASKS, null); }
    public TaskStatusUpdate updateTask(TaskStatusUpdate u) throws ClientException { return callSigned(Command.UPDATE_TASK_STATUS, u); }
    public List<TaskStatusUpdate> getTaskHistory(int taskId) throws ClientException { return call(Command.GET_TASK_HISTORY, taskId); }

    // ----------------------------------------------------------------- security

    public List<AuditEntry> getAuditLog() throws ClientException { return call(Command.GET_AUDIT_LOG, null); }

    // ----------------------------------------------------------------- plumbing

    /** Sends an unsigned request and unwraps the response. */
    @SuppressWarnings("unchecked")
    <T> T call(Command cmd, Serializable payload) throws ClientException {
        return (T) unwrap(send(new Request(cmd, payload)));
    }

    /** Signs the payload's canonical form with this user's private key, then sends it. */
    @SuppressWarnings("unchecked")
    <T> T callSigned(Command cmd, Signable payload) throws ClientException {
        if (keyPair == null) {
            throw new ClientException("Please log in first.");
        }
        Request req = new Request(cmd, (Serializable) payload);
        try {
            req.setSignature(SignatureUtil.sign(payload.canonicalForm(), keyPair.getPrivate()));
        } catch (GeneralSecurityException e) {
            throw new ClientException("Could not sign the request: " + e.getMessage(), e);
        }
        return (T) unwrap(send(req));
    }

    /** Sends a pre-built request (used by tests to exercise tampering). */
    public Response send(Request req) throws ClientException {
        try {
            return connection.send(req);
        } catch (IOException e) {
            throw new ClientException("Lost connection to the DRS server: " + e.getMessage(), e);
        }
    }

    private Serializable unwrap(Response resp) throws ClientException {
        if (!resp.isSuccess()) {
            throw new ClientException(resp.getMessage());
        }
        lastMessage = resp.getMessage();
        return resp.getData();
    }

    @Override
    public void close() {
        connection.close();
    }
}
