package drs.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import drs.common.model.Department;
import drs.common.model.DisasterResponse;
import drs.common.model.Responder;

/** Tests for Department, Responder and DisasterResponse (ported from DRS-Initial). */
class ResourceModelTest {

    private final Department fire = new Department(1, "Fire & Emergency", "Emergency Services", "Chief Alan Burns", "0400-111-001");
    private final Responder john = new Responder(1, "John Smith", "Firefighter", "0401-001-001", fire);

    @Test
    void departmentDefaultsToAvailable() {
        assertTrue(fire.isAvailable());
        assertEquals("Available", fire.getAvailabilityStatus());
        fire.setAvailable(false);
        assertEquals("Deployed", fire.getAvailabilityStatus());
    }

    @Test
    void departmentEqualityIsById() {
        assertEquals(fire, new Department(1, "Other name", "x", "y", "z"));
        assertNotEquals(fire, new Department(2, "Fire & Emergency", "x", "y", "z"));
    }

    @Test
    void responderReportsDepartmentName() {
        assertEquals("Fire & Emergency", john.getDepartmentName());
        assertEquals("N/A", new Responder().getDepartmentName());
        assertEquals("John Smith - Firefighter, Fire & Emergency", john.toString());
    }

    @Test
    void responseAddsWithoutDuplicates() {
        DisasterResponse r = new DisasterResponse(0, 5, List.of(), List.of(), "notes");
        r.addDepartment(fire);
        r.addDepartment(fire);
        r.addResponder(john);
        r.addResponder(john);
        assertEquals(1, r.getDepartmentCount());
        assertEquals(1, r.getResponderCount());
        assertEquals("Fire & Emergency", r.getDepartmentNames());
    }

    @Test
    void responseCanonicalFormIsOrderIndependent() {
        Department hosp = new Department(2, "Hospital Services", "Medical", "Dr", "1");
        DisasterResponse a = new DisasterResponse(0, 5, List.of(fire, hosp), List.of(john), "n");
        DisasterResponse b = new DisasterResponse(0, 5, List.of(hosp, fire), List.of(john), "n");
        assertEquals(a.canonicalForm(), b.canonicalForm());
    }
}
