package drs.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import drs.common.model.Department;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterType;
import drs.common.model.PublicWarning;
import drs.common.model.Role;
import drs.common.model.Severity;
import drs.common.model.User;
import drs.common.model.WarningLevel;
import drs.server.service.AgencyRecommender;
import drs.server.service.ReportService;
import drs.server.service.TriageQueue;
import drs.server.service.WarningAdvisor;

/** Business rules that do not need a database. */
class DomainLogicTest {

    private static DisasterReport report(int id, DisasterType t, Severity s, String ts) {
        DisasterReport r = new DisasterReport(id, t, s, "Rockhampton", "desc", "Jane Doe", "0400-123-456");
        r.setTimestamp(ts);
        return r;
    }

    @Test
    @DisplayName("Triage queue returns highest priority first, oldest first on ties")
    void triageOrder() {
        TriageQueue q = new TriageQueue();
        q.add(report(1, DisasterType.FLOOD, Severity.LOW, "2026-10-01 10:00:00"));        // 17
        q.add(report(2, DisasterType.HURRICANE, Severity.CRITICAL, "2026-10-01 11:00:00")); // 50
        q.add(report(3, DisasterType.FIRE, Severity.HIGH, "2026-10-01 09:00:00"));         // 38
        q.add(report(4, DisasterType.FIRE, Severity.HIGH, "2026-10-01 08:00:00"));         // 38 older
        assertEquals(2, q.peek().getId());
        assertEquals(List.of(2, 4, 3, 1), q.snapshot().stream().map(DisasterReport::getId).toList());
    }

    @Test
    @DisplayName("Triage queue re-orders after a priority override and removal")
    void triageUpdate() {
        TriageQueue q = new TriageQueue();
        q.add(report(1, DisasterType.FLOOD, Severity.LOW, "a"));
        q.add(report(2, DisasterType.FIRE, Severity.HIGH, "b"));
        q.updatePriority(1, 99);
        assertEquals(1, q.peek().getId());
        assertTrue(q.remove(1));
        assertEquals(2, q.peek().getId());
        assertEquals(1, q.size());
    }

    @Test
    @DisplayName("Agency recommendation follows disaster type, critical adds shelters")
    void agencyRecommendation() {
        AgencyRecommender rec = new AgencyRecommender();
        assertEquals(List.of("Fire & Emergency", "Hospital Services", "Law Enforcement", "Electricity Authority"),
                rec.recommendNames(DisasterType.FIRE, Severity.HIGH));
        assertTrue(rec.recommendNames(DisasterType.FLOOD, Severity.CRITICAL).contains("School Administration"));
        List<Department> all = List.of(new Department(1, "Fire & Emergency", "", "", ""),
                new Department(8, "Law Enforcement", "", "", ""));
        assertEquals(2, rec.recommend(DisasterType.OTHER, Severity.LOW, all).size());
    }

    @Test
    @DisplayName("Warning advisor suggests level, radius and validity from the incident")
    void warningSuggestion() {
        WarningAdvisor a = new WarningAdvisor();
        PublicWarning w = a.suggest(report(7, DisasterType.HURRICANE, Severity.CRITICAL, "t"));
        assertEquals(WarningLevel.EMERGENCY_WARNING, w.getLevel());
        assertEquals(75, w.getRadiusKm());
        assertEquals(6, w.getValidHours());
        assertEquals(7, w.getReportId());
        assertEquals(WarningLevel.ADVICE, a.suggestLevel(Severity.MEDIUM));
        assertEquals(WarningLevel.WATCH_AND_ACT, a.suggestLevel(Severity.HIGH));
        assertEquals(5, a.suggestRadiusKm(DisasterType.FIRE, Severity.LOW));
    }

    @Test
    @DisplayName("Warning validation rejects bad radius, validity and blank text")
    void warningValidation() {
        WarningAdvisor a = new WarningAdvisor();
        PublicWarning ok = new PublicWarning(1, WarningLevel.ADVICE, "Yeppoon", 10, "msg", "act", 12);
        assertNull(a.validate(ok));
        assertNotNull(a.validate(new PublicWarning(1, WarningLevel.ADVICE, "Yeppoon", 0, "m", "a", 12)));
        assertNotNull(a.validate(new PublicWarning(1, WarningLevel.ADVICE, "Yeppoon", 10, "m", "a", 73)));
        assertNotNull(a.validate(new PublicWarning(1, WarningLevel.ADVICE, " ", 10, "m", "a", 12)));
        assertNotNull(a.validate(new PublicWarning(1, null, "Y", 10, "m", "a", 12)));
    }

    @Test
    @DisplayName("Reporter data is masked for responders and other citizens, not for controllers or the owner")
    void masking() {
        User controller = new User(1, "controller", "Riley", Role.CONTROLLER);
        User owner = new User(2, "citizen", "Jane", Role.CITIZEN);
        User other = new User(3, "bob", "Bob", Role.CITIZEN);

        DisasterReport r = report(1, DisasterType.FIRE, Severity.LOW, "t");
        r.setReportedBy("citizen");
        assertEquals("Jane Doe", ReportService.mask(r, controller).getReporterName());
        assertEquals("Jane Doe", ReportService.mask(r, owner).getReporterName());

        DisasterReport r2 = report(1, DisasterType.FIRE, Severity.LOW, "t");
        r2.setReportedBy("citizen");
        ReportService.mask(r2, other);
        assertEquals("J***", r2.getReporterName());
        assertEquals("*******456", r2.getReporterPhone());
    }

    @Test
    @DisplayName("Server-side report validation")
    void reportValidation() {
        assertNull(ReportService.validate(report(0, DisasterType.FIRE, Severity.LOW, "t")));
        DisasterReport bad = report(0, DisasterType.FIRE, Severity.LOW, "t");
        bad.setReporterPhone("abc");
        assertNotNull(ReportService.validate(bad));
        bad = report(0, DisasterType.FIRE, Severity.LOW, "t");
        bad.setLocation("");
        assertEquals("Location is required.", ReportService.validate(bad));
    }

    @Test
    @DisplayName("Seed CSV parser handles quoted commas")
    void csvParsing() {
        String[] f = DataLoader.parseLine("FIRE,HIGH,\"12 Main Street, Brisbane QLD\",desc,Jane,0400,ASSESSING,,52");
        assertEquals(9, f.length);
        assertEquals("12 Main Street, Brisbane QLD", f[2]);
        assertEquals("", f[7]);
        assertEquals(12, DataLoader.readCsv("a2_reports.csv").size());
        assertEquals(16, DataLoader.readCsv("responders.csv").size());
    }
}
