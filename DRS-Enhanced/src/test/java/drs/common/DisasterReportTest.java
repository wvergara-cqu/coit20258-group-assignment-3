package drs.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.Severity;

/** Unit tests for DisasterReport (ported from DRS-Initial and extended). */
class DisasterReportTest {

    private DisasterReport report;

    @BeforeEach
    void setUp() {
        report = new DisasterReport(1, DisasterType.FIRE, Severity.HIGH, "12 Main Street, Brisbane",
                "Residential building fire.", "Jane Doe", "0400-123-456");
    }

    @Test
    @DisplayName("Full constructor sets every field and defaults status to REPORTED")
    void constructorSetsFields() {
        assertEquals(1, report.getId());
        assertEquals(DisasterType.FIRE, report.getType());
        assertEquals(Severity.HIGH, report.getSeverity());
        assertEquals("12 Main Street, Brisbane", report.getLocation());
        assertEquals("Jane Doe", report.getReporterName());
        assertEquals(DisasterStatus.REPORTED, report.getStatus());
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({"HURRICANE,CRITICAL,50", "EARTHQUAKE,HIGH,39", "FIRE,HIGH,38", "FLOOD,MEDIUM,27", "OTHER,LOW,15"})
    @DisplayName("Priority = type weight + severity weight")
    void computePriority(DisasterType type, Severity sev, int expected) {
        assertEquals(expected, DisasterReport.computePriority(type, sev));
    }

    @Test
    @DisplayName("Changing severity recomputes priority")
    void setSeverityRecomputes() {
        report.setSeverity(Severity.CRITICAL);
        assertEquals(48, report.getPriority());
    }

    @Test
    @DisplayName("Priority override range is 1..100")
    void priorityRange() {
        assertFalse(DisasterReport.isValidPriority(0));
        assertTrue(DisasterReport.isValidPriority(1));
        assertTrue(DisasterReport.isValidPriority(100));
        assertFalse(DisasterReport.isValidPriority(101));
    }

    @Test
    @DisplayName("Canonical form excludes server-assigned id and status")
    void canonicalExcludesServerFields() {
        String before = report.canonicalForm();
        report.setId(99);
        report.setStatus(DisasterStatus.RESOLVED);
        report.setTimestamp("2026-01-01 00:00:00");
        assertEquals(before, report.canonicalForm());
        report.setLocation("Elsewhere");
        assertNotEquals(before, report.canonicalForm());
    }

    @Test
    @DisplayName("Custom toString is readable")
    void toStringReadable() {
        assertEquals("Report #1 | Fire | High | 12 Main Street, Brisbane | Reported | priority 38", report.toString());
    }
}
