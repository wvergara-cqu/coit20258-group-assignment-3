package drs.server.service;

import java.sql.SQLException;
import java.util.List;

import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.PublicWarning;
import drs.common.model.User;
import drs.server.db.WarningDAO;

/**
 * Enhanced Feature 1 - public warning and evacuation advisory service (UC-6, FR-5).
 */
public class WarningService {

    private final WarningDAO warnings;
    private final ReportService reports;
    private final WarningAdvisor advisor = new WarningAdvisor();

    public WarningService(WarningDAO warnings, ReportService reports) {
        this.warnings = warnings;
        this.reports = reports;
    }

    /** @return a pre-filled warning for the controller to review */
    public PublicWarning suggest(int reportId) throws ServiceException, SQLException {
        DisasterReport r = reports.get(reportId);
        return advisor.suggest(r);
    }

    /**
     * Issues a warning. Only one active warning per incident is allowed; to change
     * level the controller cancels and re-issues, which keeps the history clear.
     */
    public PublicWarning issue(PublicWarning w, User user, String signature) throws ServiceException, SQLException {
        String err = advisor.validate(w);
        if (err != null) {
            throw new ServiceException(err);
        }
        DisasterReport r = reports.get(w.getReportId());
        if (r.getStatus() == DisasterStatus.RESOLVED) {
            throw new ServiceException("Report #" + r.getId() + " is resolved; a warning cannot be issued.");
        }
        if (!warnings.findActiveForReport(r.getId()).isEmpty()) {
            throw new ServiceException("Report #" + r.getId()
                    + " already has an active warning. Cancel it before issuing a new level.");
        }
        w.setAffectedArea(w.getAffectedArea().trim());
        w.setMessage(w.getMessage().trim());
        w.setRecommendedAction(w.getRecommendedAction().trim());
        warnings.insert(w, user.getId(), signature);
        w.setDisasterType(r.getType());
        w.setIssuedBy(user.getUsername());
        return w;
    }

    /** Cancels an active warning. */
    public void cancel(int warningId) throws ServiceException, SQLException {
        if (!warnings.cancel(warningId)) {
            throw new ServiceException("Warning #" + warningId + " is not active.");
        }
    }

    public List<PublicWarning> active() throws SQLException { return warnings.findActive(); }
    public List<PublicWarning> all() throws SQLException { return warnings.findAll(); }

    /** @return number of warnings that just expired */
    public int expireOverdue() throws SQLException { return warnings.expireOverdue(); }

    /** @return the advisor (exposed for unit tests) */
    public WarningAdvisor getAdvisor() { return advisor; }
}
