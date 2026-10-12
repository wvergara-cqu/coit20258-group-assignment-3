package drs.server.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import drs.common.model.DisasterReport;

/**
 * In-memory triage queue of REPORTED (not yet assessed) incidents, ordered by
 * priority (highest first) then by submission time (oldest first). A binary-heap
 * PriorityQueue gives O(log n) insertion and O(1) access to the most urgent
 * incident, which is what controllers need under heavy load (US-4). All methods
 * are synchronised because many client-handler threads share one queue.
 */
public class TriageQueue {

    /** Highest priority first; ties broken by older timestamp, then lower ID. */
    public static final Comparator<DisasterReport> TRIAGE_ORDER =
            Comparator.comparingInt(DisasterReport::getPriority).reversed()
                    .thenComparing(r -> r.getTimestamp() == null ? "" : r.getTimestamp())
                    .thenComparingInt(DisasterReport::getId);

    private final PriorityQueue<DisasterReport> heap = new PriorityQueue<>(TRIAGE_ORDER);

    /** Replaces the queue contents (called at server start-up). */
    public synchronized void rebuild(Collection<DisasterReport> reported) {
        heap.clear();
        heap.addAll(reported);
    }

    /** Adds a newly submitted report. */
    public synchronized void add(DisasterReport report) {
        heap.add(report);
    }

    /** Removes a report once it leaves the REPORTED state. */
    public synchronized boolean remove(int reportId) {
        return heap.removeIf(r -> r.getId() == reportId);
    }

    /** Re-positions a report after a priority override. */
    public synchronized void updatePriority(int reportId, int newPriority) {
        DisasterReport found = null;
        for (DisasterReport r : heap) {
            if (r.getId() == reportId) {
                found = r;
                break;
            }
        }
        if (found != null) {
            heap.remove(found);
            found.setPriority(newPriority);
            heap.add(found);
        }
    }

    /** @return the most urgent unassessed report without removing it, or null */
    public synchronized DisasterReport peek() {
        return heap.peek();
    }

    /** @return the number of reports awaiting assessment */
    public synchronized int size() {
        return heap.size();
    }

    /** @return a sorted copy of the queue */
    public synchronized List<DisasterReport> snapshot() {
        List<DisasterReport> list = new ArrayList<>(heap);
        list.sort(TRIAGE_ORDER);
        return list;
    }
}
