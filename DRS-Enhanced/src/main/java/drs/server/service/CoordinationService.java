package drs.server.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import drs.common.model.Department;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterResponse;
import drs.common.model.DisasterStatus;
import drs.common.model.Responder;
import drs.common.model.User;
import drs.server.db.ResourceDAO;
import drs.server.db.ResponseDAO;

/**
 * Coordination of departments and responders (UC-3), including agency
 * recommendation and safe concurrent assignment of resources.
 */
public class CoordinationService {

    private final ResourceDAO resources;
    private final ResponseDAO responses;
    private final ReportService reports;
    private final AgencyRecommender recommender = new AgencyRecommender();

    /** Guards resource assignment so two controllers cannot book the same responder. */
    private final Object assignmentLock = new Object();

    public CoordinationService(ResourceDAO resources, ResponseDAO responses, ReportService reports) {
        this.resources = resources;
        this.responses = responses;
        this.reports = reports;
    }

    public List<Department> departments() throws SQLException { return resources.findAllDepartments(); }
    public List<Responder> responders() throws SQLException { return resources.findAllResponders(); }
    public List<DisasterResponse> allResponses() throws SQLException { return responses.findAll(); }

    /** @return available departments that still have at least one available responder */
    public List<Department> assignableDepartments() throws SQLException {
        Set<Integer> withStaff = resources.findAllResponders().stream()
                .filter(Responder::isAvailable)
                .map(r -> r.getDepartment().getId())
                .collect(Collectors.toCollection(HashSet::new));
        return resources.findAllDepartments().stream()
                .filter(Department::isAvailable)
                .filter(d -> withStaff.contains(d.getId()))
                .collect(Collectors.toList());
    }

    /** @return recommended departments for a report (US-6) */
    public List<Department> recommended(int reportId) throws ServiceException, SQLException {
        DisasterReport r = reports.get(reportId);
        return recommender.recommend(r.getType(), r.getSeverity(), resources.findAllDepartments());
    }

    /** @return the recommender (exposed for unit tests) */
    public AgencyRecommender getRecommender() {
        return recommender;
    }

    public boolean toggleDepartment(int id) throws SQLException { return resources.toggleDepartment(id); }
    public boolean toggleResponder(int id) throws SQLException { return resources.toggleResponder(id); }

    /**
     * Validates and saves a response. Availability is re-checked against the
     * database inside a lock, so a responder booked by another controller a
     * moment earlier is rejected rather than double-booked.
     *
     * @return the saved response with its ID
     */
    public DisasterResponse saveResponse(DisasterResponse response, User user, String signature)
            throws ServiceException, SQLException {
        if (response == null) {
            throw new ServiceException("Response details are missing.");
        }
        DisasterReport report = reports.get(response.getReportId());
        if (report.getStatus() == DisasterStatus.RESOLVED) {
            throw new ServiceException("Report #" + report.getId() + " is already resolved.");
        }
        if (response.getAssignedDepartments().isEmpty() && response.getAssignedResponders().isEmpty()) {
            throw new ServiceException("Select at least one department or responder.");
        }
        synchronized (assignmentLock) {
            Map<Integer, Responder> current = new HashMap<>();
            for (Responder r : resources.findAllResponders()) {
                current.put(r.getId(), r);
            }
            for (Responder r : response.getAssignedResponders()) {
                Responder live = current.get(r.getId());
                if (live == null || !live.isAvailable()) {
                    throw new ServiceException(r.getName() + " is no longer available. Refresh and try again.");
                }
            }
            responses.saveWithTasks(response, user.getId(), signature);
            reports.getTriageQueue().remove(report.getId());
        }
        response.setCreatedBy(user.getUsername());
        return response;
    }
}
