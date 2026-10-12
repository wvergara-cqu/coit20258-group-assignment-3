package drs.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import drs.client.security.ClientKeyStore;
import drs.client.service.ClientException;
import drs.client.service.DRSClient;
import drs.common.model.AuditEntry;
import drs.common.model.Department;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterResponse;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.FieldTask;
import drs.common.model.PublicWarning;
import drs.common.model.Responder;
import drs.common.model.Severity;
import drs.common.model.TaskStatus;
import drs.common.model.TaskStatusUpdate;
import drs.common.model.WarningLevel;
import drs.common.net.Command;
import drs.common.net.Request;
import drs.common.net.Response;
import drs.server.DRSServer;
import drs.server.ServerConfig;
import drs.server.db.DatabaseManager;

/**
 * End-to-end tests: real JavaFX-free clients talk to a real multi-threaded
 * DRSServer over TCP, backed by a throw-away MySQL schema (drs_enhanced_test).
 * Skipped automatically if MySQL is not reachable with the configured account.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerIntegrationTest {

    private ServerConfig config;
    private DRSServer server;
    private int port;
    private int reportId;
    private int taskId;
    private int warningId;

    @BeforeAll
    void startServer() throws Exception {
        config = new ServerConfig();
        config.set("db.name", "drs_enhanced_test");
        config.set("server.port", "0");
        try (Connection c = DriverManager.getConnection(config.getServerJdbcUrl(), config.getDbUser(),
                config.getDbPassword())) {
            assumeTrue(c.isValid(2), "MySQL not available");
        } catch (Exception e) {
            assumeTrue(false, "MySQL not available: " + e.getMessage());
        }
        DatabaseManager.dropDatabase(config);
        ClientKeyStore.setBaseDir(Files.createTempDirectory("drs-keys"));
        server = new DRSServer(config);
        server.start();
        port = server.getPort();
    }

    @AfterAll
    void stopServer() throws Exception {
        if (server != null) {
            server.stop();
            DatabaseManager.dropDatabase(config);
        }
    }

    private DRSClient as(String user, String pass) throws ClientException {
        DRSClient c = new DRSClient("localhost", port);
        c.login(user, pass);
        return c;
    }

    // ------------------------------------------------------------ database & A2 data

    @Test @Order(1)
    @DisplayName("IT-01 Database and 11 tables are created programmatically")
    void tablesCreated() throws Exception {
        try (Connection c = DriverManager.getConnection(config.getJdbcUrl(), config.getDbUser(), config.getDbPassword());
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'drs_enhanced_test'")) {
            rs.next();
            assertEquals(11, rs.getInt(1));
        }
    }

    @Test @Order(2)
    @DisplayName("IT-02 DRS-Initial (A2) data is loaded through the server")
    void a2DataLoaded() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            assertEquals(12, c.getReports().size());
            assertEquals(8, c.getDepartments().size());
            assertEquals(16, c.getResponders().size());
        }
    }

    // ------------------------------------------------------------ authentication & RBAC

    @Test @Order(3)
    @DisplayName("IT-03 Wrong password is rejected with a generic message")
    void badLogin() throws Exception {
        try (DRSClient c = new DRSClient("localhost", port)) {
            ClientException e = assertThrows(ClientException.class, () -> c.login("controller", "wrong-pass1"));
            assertEquals("Invalid username or password.", e.getMessage());
        }
    }

    @Test @Order(4)
    @DisplayName("IT-04 Unauthenticated and unauthorised commands are denied")
    void accessDenied() throws Exception {
        try (DRSClient anon = new DRSClient("localhost", port)) {
            assertThrows(ClientException.class, anon::getActiveWarnings);
        }
        try (DRSClient citizen = as("citizen", "Citizen@2026")) {
            ClientException e = assertThrows(ClientException.class, citizen::getReports);
            assertTrue(e.getMessage().startsWith("Access denied"));
        }
        try (DRSClient responder = as("jsmith", "Field@2026")) {
            assertThrows(ClientException.class, () -> responder.issueWarning(
                    new PublicWarning(1, WarningLevel.ADVICE, "x", 5, "m", "a", 5)));
        }
    }

    @Test @Order(5)
    @DisplayName("IT-05 Citizen self-registration enforces password policy and unique usernames")
    void registration() throws Exception {
        try (DRSClient c = new DRSClient("localhost", port)) {
            assertThrows(ClientException.class, () -> c.register("newbie", "short", "New Person"));
            assertNotNull(c.register("newbie", "Newbie@2026", "New Person"));
            assertThrows(ClientException.class, () -> c.register("newbie", "Newbie@2026", "Dup"));
        }
        try (DRSClient c = as("newbie", "Newbie@2026")) {
            assertEquals("CITIZEN", c.getCurrentUser().getRole().name());
        }
    }

    // ------------------------------------------------------------ reports, encryption, signatures

    @Test @Order(10)
    @DisplayName("IT-10 Citizen submits a signed report and receives a tracking ID")
    void submitReport() throws Exception {
        try (DRSClient c = as("citizen", "Citizen@2026")) {
            DisasterReport r = c.submitReport(new DisasterReport(0, DisasterType.FIRE, Severity.CRITICAL,
                    "Berserker St, Rockhampton QLD", "Shed fire spreading to houses", "Jane Doe", "0400-123-456"));
            reportId = r.getId();
            assertTrue(reportId > 12);
            assertEquals(48, r.getPriority());
            assertTrue(c.getLastMessage().contains("Tracking ID: DRS-" + reportId));
            assertEquals(1, c.getMyReports().size());
        }
    }

    @Test @Order(11)
    @DisplayName("IT-11 Reporter personal data is encrypted at rest in MySQL")
    void encryptedAtRest() throws Exception {
        try (Connection c = DriverManager.getConnection(config.getJdbcUrl(), config.getDbUser(), config.getDbPassword());
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT reporter_name_enc, reporter_phone_enc, signature FROM disaster_reports WHERE id = " + reportId)) {
            assertTrue(rs.next());
            assertFalse(rs.getString(1).contains("Jane"));
            assertFalse(rs.getString(2).contains("0400"));
            assertNotNull(rs.getString(3), "signature stored for non-repudiation");
        }
        try (DRSClient c = as("controller", "Control@2026")) {
            DisasterReport r = c.getReports().stream().filter(x -> x.getId() == reportId).findFirst().orElseThrow();
            assertEquals("Jane Doe", r.getReporterName(), "controller sees decrypted data");
        }
    }

    @Test @Order(12)
    @DisplayName("IT-12 A tampered or unsigned request is rejected (non-repudiation)")
    void tamperedRequestRejected() throws Exception {
        try (DRSClient c = as("citizen", "Citizen@2026")) {
            DisasterReport r = new DisasterReport(0, DisasterType.FLOOD, Severity.LOW, "Yeppoon",
                    "Minor flooding", "Jane Doe", "0400-123-456");
            Request unsigned = new Request(Command.SUBMIT_REPORT, r);
            Response resp = c.send(unsigned);
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("signature"));

            Request forged = new Request(Command.SUBMIT_REPORT, r);
            forged.setSignature("AAAA" + "B".repeat(340));
            assertFalse(c.send(forged).isSuccess());
            assertEquals(1, c.getMyReports().size(), "nothing was stored");
        }
    }

    @Test @Order(13)
    @DisplayName("IT-13 Invalid report input is rejected by the server")
    void invalidReport() throws Exception {
        try (DRSClient c = as("citizen", "Citizen@2026")) {
            ClientException e = assertThrows(ClientException.class, () -> c.submitReport(new DisasterReport(0,
                    DisasterType.FIRE, Severity.LOW, "", "d", "Jane", "0400-123-456")));
            assertEquals("Location is required.", e.getMessage());
        }
    }

    @Test @Order(14)
    @DisplayName("IT-14 Triage queue returns the most urgent unassessed report")
    void triage() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            DisasterReport next = c.nextInTriage();
            assertEquals(55, next.getPriority(), "A2 report with priority override 55 is most urgent");
        }
    }

    @Test @Order(15)
    @DisplayName("IT-15 Priority override validation and forward-only status changes")
    void statusRules() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            assertThrows(ClientException.class, () -> c.updatePriority(reportId, 150));
            assertEquals(60, c.updatePriority(reportId, 60).getPriority());
            assertEquals(DisasterStatus.ASSESSING, c.updateStatus(reportId, DisasterStatus.ASSESSING).getStatus());
            ClientException e = assertThrows(ClientException.class, () -> c.updateStatus(reportId, DisasterStatus.REPORTED));
            assertTrue(e.getMessage().contains("only move forward"));
        }
    }

    // ------------------------------------------------------------ coordination + feature 2

    @Test @Order(20)
    @DisplayName("IT-20 Recommended agencies and signed response dispatch field tasks")
    void saveResponse() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            List<Department> rec = c.getRecommendedDepartments(reportId);
            assertEquals("Fire & Emergency", rec.get(0).getName());
            Responder john = c.getResponders().stream().filter(r -> "jsmith".equals(r.getUsername())).findFirst().orElseThrow();
            DisasterResponse resp = c.saveResponse(new DisasterResponse(0, reportId, List.of(rec.get(0)),
                    List.of(john), "- Search and Rescue\nCheck neighbouring houses"));
            assertTrue(resp.getId() > 0);
            assertEquals(DisasterStatus.RESPONDING, c.getReports().stream()
                    .filter(r -> r.getId() == reportId).findFirst().orElseThrow().getStatus());
            assertFalse(c.getResponders().stream().filter(r -> r.getId() == john.getId()).findFirst()
                    .orElseThrow().isAvailable(), "responder is deployed");
        }
        try (DRSClient r = as("jsmith", "Field@2026")) {
            List<FieldTask> tasks = r.getMyTasks();
            assertEquals(1, tasks.size());
            assertEquals(TaskStatus.DISPATCHED, tasks.get(0).getStatus());
            taskId = tasks.get(0).getId();
        }
    }

    @Test @Order(21)
    @DisplayName("IT-21 Report cannot be resolved while field tasks are open")
    void resolveBlocked() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            ClientException e = assertThrows(ClientException.class, () -> c.updateStatus(reportId, DisasterStatus.RESOLVED));
            assertTrue(e.getMessage().contains("still open"));
        }
    }

    @Test @Order(22)
    @DisplayName("IT-22 Illegal task transition and another responder's update are rejected")
    void taskRules() throws Exception {
        try (DRSClient r = as("jsmith", "Field@2026")) {
            ClientException e = assertThrows(ClientException.class,
                    () -> r.updateTask(new TaskStatusUpdate(taskId, TaskStatus.COMPLETED, "skip")));
            assertTrue(e.getMessage().startsWith("Cannot change task"));
        }
        try (DRSClient other = as("mgarcia", "Field@2026")) {
            ClientException e = assertThrows(ClientException.class,
                    () -> other.updateTask(new TaskStatusUpdate(taskId, TaskStatus.ACKNOWLEDGED, "")));
            assertTrue(e.getMessage().contains("not assigned to you"));
        }
    }

    @Test @Order(23)
    @DisplayName("IT-23 Responder progresses the task to Completed with signed history")
    void taskLifecycle() throws Exception {
        try (DRSClient r = as("jsmith", "Field@2026")) {
            r.updateTask(new TaskStatusUpdate(taskId, TaskStatus.ACKNOWLEDGED, "Received"));
            r.updateTask(new TaskStatusUpdate(taskId, TaskStatus.EN_ROUTE, "Leaving station 3"));
            r.updateTask(new TaskStatusUpdate(taskId, TaskStatus.ON_SCENE, "Two houses evacuated"));
            r.updateTask(new TaskStatusUpdate(taskId, TaskStatus.COMPLETED, "Fire out"));
            List<TaskStatusUpdate> h = r.getTaskHistory(taskId);
            assertEquals(4, h.size());
            assertTrue(h.stream().allMatch(TaskStatusUpdate::isSignatureVerified));
            assertEquals(TaskStatus.DISPATCHED, h.get(0).getFromStatus());
        }
        try (DRSClient c = as("controller", "Control@2026")) {
            assertTrue(c.getResponders().stream().filter(x -> "jsmith".equals(x.getUsername())).findFirst()
                    .orElseThrow().isAvailable(), "responder released when task completed");
        }
    }

    // ------------------------------------------------------------ feature 1: warnings

    @Test @Order(30)
    @DisplayName("IT-30 Warning is pre-filled from the incident and broadcast")
    void issueWarning() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            PublicWarning s = c.suggestWarning(reportId);
            assertEquals(WarningLevel.EMERGENCY_WARNING, s.getLevel());
            assertEquals(15, s.getRadiusKm());
            PublicWarning w = c.issueWarning(s);
            warningId = w.getId();
            assertTrue(warningId > 0);
            assertNotNull(w.getExpiresAt());
            ClientException dup = assertThrows(ClientException.class, () -> c.issueWarning(s));
            assertTrue(dup.getMessage().contains("already has an active warning"));
            assertThrows(ClientException.class, () -> c.issueWarning(
                    new PublicWarning(reportId, WarningLevel.ADVICE, "x", 500, "m", "a", 5)));
        }
    }

    @Test @Order(31)
    @DisplayName("IT-31 Citizens and responders see the active warning")
    void citizensSeeWarning() throws Exception {
        try (DRSClient c = as("citizen", "Citizen@2026")) {
            assertTrue(c.getActiveWarnings().stream().anyMatch(w -> w.getId() == warningId));
        }
        try (DRSClient r = as("jsmith", "Field@2026")) {
            assertFalse(r.getActiveWarnings().isEmpty());
        }
    }

    @Test @Order(32)
    @DisplayName("IT-32 Resolving the incident cancels its warnings")
    void resolveCancelsWarning() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            c.updateStatus(reportId, DisasterStatus.RESOLVED);
            assertTrue(c.getActiveWarnings().stream().noneMatch(w -> w.getId() == warningId));
            assertThrows(ClientException.class, () -> c.cancelWarning(warningId));
        }
    }

    // ------------------------------------------------------------ multi-threading

    @Test @Order(40)
    @DisplayName("IT-40 Twenty concurrent clients submit reports; every ID is unique")
    void concurrentClients() throws Exception {
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int k = i;
            futures.add(pool.submit(() -> {
                try (DRSClient c = as("citizen", "Citizen@2026")) {
                    start.await();
                    ids.add(c.submitReport(new DisasterReport(0, DisasterType.OTHER, Severity.LOW,
                            "Concurrent test " + k, "load test", "Load Tester", "0400-000-000")).getId());
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertEquals(n, ids.size());
    }

    @Test @Order(41)
    @DisplayName("IT-41 Two controllers cannot double-book the same responder")
    void noDoubleBooking() throws Exception {
        int[] reports = new int[2];
        try (DRSClient c = as("controller", "Control@2026")) {
            List<DisasterReport> open = c.getReports().stream()
                    .filter(r -> r.getStatus() == DisasterStatus.REPORTED).toList();
            reports[0] = open.get(0).getId();
            reports[1] = open.get(1).getId();
        }
        Responder anna;
        try (DRSClient c = as("controller", "Control@2026")) {
            anna = c.getResponders().stream().filter(r -> "abrown".equals(r.getUsername())).findFirst().orElseThrow();
        }
        AtomicInteger ok = new AtomicInteger();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int k = i;
            String user = k == 0 ? "controller" : "controller2";
            fs.add(pool.submit(() -> {
                try (DRSClient c = as(user, "Control@2026")) {
                    go.await();
                    c.saveResponse(new DisasterResponse(0, reports[k], List.of(), List.of(anna), "race"));
                    ok.incrementAndGet();
                } catch (ClientException e) {
                    errors.add(e.getMessage());
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertEquals(1, ok.get());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("no longer available"));
    }

    // ------------------------------------------------------------ audit

    @Test @Order(50)
    @DisplayName("IT-50 Audit log records signed actions and rejected forgeries; admin only")
    void auditLog() throws Exception {
        try (DRSClient c = as("controller", "Control@2026")) {
            assertThrows(ClientException.class, c::getAuditLog);
        }
        try (DRSClient a = as("admin", "Admin@2026")) {
            List<AuditEntry> log = a.getAuditLog();
            assertTrue(log.stream().anyMatch(e -> e.getAction().equals("SUBMIT_REPORT") && e.isVerified()));
            assertTrue(log.stream().anyMatch(e -> e.getAction().equals("REJECTED_SUBMIT_REPORT") && !e.isVerified()));
            assertTrue(log.stream().anyMatch(e -> e.getAction().equals("TASK_COMPLETED")));
            assertTrue(log.stream().allMatch(e -> e.getLoggedAt() != null && !e.getLoggedAt().isEmpty()));
        }
    }
}
