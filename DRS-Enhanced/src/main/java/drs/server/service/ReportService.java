package drs.server.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.IncidentSummary;
import drs.common.model.Role;
import drs.common.model.User;
import drs.common.util.InputValidator;
import drs.server.db.ReportDAO;
import drs.server.db.TaskDAO;
import drs.server.db.WarningDAO;
import drs.server.security.AccessControl;

/**
 * Disaster report business logic: validation, persistence, triage ordering,
 * status lifecycle rules and personal-data masking.
 */
public class ReportService {

    private final ReportDAO reports;
    private final TaskDAO tasks;
    private final WarningDAO warnings;
    private final TriageQueue triage = new TriageQueue();

    public ReportService(ReportDAO reports, TaskDAO tasks, WarningDAO warnings) {
        this.reports = reports;
        this.tasks = tasks;
        this.warnings = warnings;
    }

    /** Loads unassessed reports into the triage queue. */
    public void loadTriageQueue() throws SQLException {
        triage.rebuild(reports.findByStatus(DisasterStatus.REPORTED));
    }

    /** @return the shared triage queue */
    public TriageQueue getTriageQueue() {
        return triage;
    }

    /** @return null if the report is valid, otherwise an error */
    public static String validate(DisasterReport r) {
        if (r == null) {
            return "Report details are missing.";
        }
        if (r.getType() == null) {
            return "Please select a disaster type.";
        }
        if (r.getSeverity() == null) {
            return "Please select a severity level.";
        }
        String err = InputValidator.required("Location", r.getLocation(), InputValidator.MAX_SHORT);
        if (err == null) {
            err = InputValidator.required("Description", r.getDescription(), InputValidator.MAX_LONG);
        }
        if (err == null) {
            err = InputValidator.required("Reporter name", r.getReporterName(), InputValidator.MAX_SHORT);
        }
        if (err == null) {
            err = InputValidator.phone(r.getReporterPhone());
        }
        return err;
    }

    /**
     * Validates and stores a report. Priority and status are always set by the
     * server so a client cannot inflate its own priority.
     *
     * @return the stored report with its generated ID
     */
    public DisasterReport submit(DisasterReport r, User user, String signature) throws ServiceException, SQLException {
        String err = validate(r);
        if (err != null) {
            throw new ServiceException(err);
        }
        r.setLocation(r.getLocation().trim());
        r.setDescription(r.getDescription().trim());
        r.setReporterName(r.getReporterName().trim());
        r.setReporterPhone(r.getReporterPhone().trim());
        r.setStatus(DisasterStatus.REPORTED);
        r.setPriority(DisasterReport.computePriority(r.getType(), r.getSeverity()));
        reports.insert(r, user.getId(), signature);
        r.setReportedBy(user.getUsername());
        triage.add(r);
        return r;
    }

    public List<DisasterReport> all() throws SQLException { return reports.findAll(); }
    public List<DisasterReport> active() throws SQLException { return reports.findActive(); }
    public List<DisasterReport> byUser(User u) throws SQLException { return reports.findByUser(u.getId()); }

    public List<DisasterReport> filter(DisasterType type, DisasterStatus status) throws SQLException {
        return reports.filter(type, status);
    }

    /** @return the report or a ServiceException */
    public DisasterReport get(int id) throws ServiceException, SQLException {
        Optional<DisasterReport> r = reports.findById(id);
        if (r.isEmpty()) {
            throw new ServiceException("Report #" + id + " does not exist.");
        }
        return r.get();
    }

    /** @return the most urgent unassessed report, or null when the queue is empty */
    public DisasterReport nextInTriage() {
        return triage.peek();
    }

    /**
     * Applies the report lifecycle rules: status can only move forward, and a
     * report cannot be resolved while field tasks are still open.
     */
    public DisasterReport updateStatus(int id, DisasterStatus next) throws ServiceException, SQLException {
        DisasterReport r = get(id);
        if (next == null || next.ordinal() <= r.getStatus().ordinal()) {
            throw new ServiceException("Report #" + id + " is already " + r.getStatus()
                    + "; status can only move forward.");
        }
        if (next == DisasterStatus.RESOLVED) {
            int open = tasks.countOpenForReport(id);
            if (open > 0) {
                throw new ServiceException("Cannot resolve report #" + id + ": " + open
                        + " field task(s) are still open.");
            }
            warnings.cancelForReport(id);
        }
        reports.updateStatus(id, next);
        triage.remove(id);
        r.setStatus(next);
        return r;
    }

    /** Overrides the computed priority (1-100). */
    public DisasterReport updatePriority(int id, int priority) throws ServiceException, SQLException {
        if (!DisasterReport.isValidPriority(priority)) {
            throw new ServiceException("Priority must be between " + DisasterReport.MIN_PRIORITY
                    + " and " + DisasterReport.MAX_PRIORITY + ".");
        }
        DisasterReport r = get(id);
        reports.updatePriority(id, priority);
        triage.updatePriority(id, priority);
        r.setPriority(priority);
        return r;
    }

    /** @return dashboard counts */
    public IncidentSummary summary() throws SQLException {
        Map<DisasterStatus, Integer> c = reports.countByStatus();
        IncidentSummary s = new IncidentSummary();
        s.setReported(c.get(DisasterStatus.REPORTED));
        s.setAssessing(c.get(DisasterStatus.ASSESSING));
        s.setResponding(c.get(DisasterStatus.RESPONDING));
        s.setResolved(c.get(DisasterStatus.RESOLVED));
        s.setActiveWarnings(warnings.findActive().size());
        s.setOpenTasks(tasks.countOpen());
        return s;
    }

    /**
     * Masks reporter personal data for roles that are not entitled to it
     * (privacy - US-10, UR-6). Citizens still see their own details.
     */
    public static DisasterReport mask(DisasterReport r, User viewer) {
        Role role = viewer == null ? null : viewer.getRole();
        boolean own = viewer != null && viewer.getUsername().equals(r.getReportedBy());
        if (!AccessControl.canViewPersonalData(role) && !own) {
            r.setReporterName(maskName(r.getReporterName()));
            r.setReporterPhone(maskPhone(r.getReporterPhone()));
        }
        return r;
    }

    static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim().charAt(0) + "***";
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 3) {
            return "***";
        }
        return "*******" + phone.substring(phone.length() - 3);
    }
}
