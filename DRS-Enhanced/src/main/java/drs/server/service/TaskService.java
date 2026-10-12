package drs.server.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import drs.common.model.FieldTask;
import drs.common.model.Responder;
import drs.common.model.Role;
import drs.common.model.TaskStatusUpdate;
import drs.common.model.User;
import drs.server.db.ResourceDAO;
import drs.server.db.TaskDAO;

/**
 * Enhanced Feature 2 - field task tracking service (UC-5, FR-4).
 */
public class TaskService {

    /** Maximum length of a field note. */
    public static final int MAX_NOTE = 500;

    private final TaskDAO tasks;
    private final ResourceDAO resources;

    public TaskService(TaskDAO tasks, ResourceDAO resources) {
        this.tasks = tasks;
        this.resources = resources;
    }

    /** @return the responder record linked to a login */
    public Responder responderFor(User user) throws ServiceException, SQLException {
        Optional<Responder> r = resources.findResponderByUsername(user.getUsername());
        if (r.isEmpty()) {
            throw new ServiceException("Your account is not linked to a field responder.");
        }
        return r.get();
    }

    /** @return the tasks assigned to the logged-in responder */
    public List<FieldTask> myTasks(User user) throws ServiceException, SQLException {
        return tasks.findByResponder(responderFor(user).getId());
    }

    /** @return every task (controller dashboard) */
    public List<FieldTask> all() throws SQLException {
        return tasks.findAll();
    }

    /**
     * Validates and applies a status update requested by a responder.
     *
     * @param verified whether the request's signature verified
     * @return the stored update
     */
    public TaskStatusUpdate update(TaskStatusUpdate u, User user, String signature, boolean verified)
            throws ServiceException, SQLException {
        if (u == null || u.getToStatus() == null) {
            throw new ServiceException("Select the new task status.");
        }
        if (u.getNote() != null && u.getNote().length() > MAX_NOTE) {
            throw new ServiceException("Field note must be " + MAX_NOTE + " characters or fewer.");
        }
        FieldTask task = tasks.findById(u.getTaskId())
                .orElseThrow(() -> new ServiceException("Task #" + u.getTaskId() + " does not exist."));
        Responder me = responderFor(user);
        if (task.getResponderId() != me.getId()) {
            throw new ServiceException("Task #" + task.getId() + " is not assigned to you.");
        }
        if (!task.getStatus().canTransitionTo(u.getToStatus())) {
            throw new ServiceException("Cannot change task from " + task.getStatus() + " to " + u.getToStatus()
                    + ". Allowed: " + task.getStatus().nextStatuses());
        }
        u.setFromStatus(task.getStatus());
        u.setNote(u.getNote() == null ? null : u.getNote().trim());
        if (!tasks.applyUpdate(u, user.getId(), signature, verified, me.getId())) {
            throw new ServiceException("Task #" + task.getId() + " was changed by someone else. Refresh and retry.");
        }
        u.setUpdatedBy(user.getUsername());
        return u;
    }

    /** @return the status history; responders may only see their own tasks */
    public List<TaskStatusUpdate> history(int taskId, User user) throws ServiceException, SQLException {
        FieldTask task = tasks.findById(taskId)
                .orElseThrow(() -> new ServiceException("Task #" + taskId + " does not exist."));
        if (user.getRole() == Role.RESPONDER && task.getResponderId() != responderFor(user).getId()) {
            throw new ServiceException("Task #" + taskId + " is not assigned to you.");
        }
        return tasks.findHistory(taskId);
    }
}
