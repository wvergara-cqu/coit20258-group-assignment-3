package drs.server.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import drs.common.model.Department;
import drs.common.model.DisasterType;
import drs.common.model.Severity;

/**
 * Recommends which departments to dispatch for a disaster (US-6, UC-3 step 2).
 * Routing rules are held in an EnumMap keyed by disaster type.
 */
public class AgencyRecommender {

    private final Map<DisasterType, List<String>> rules = new EnumMap<>(DisasterType.class);

    /** Builds the default routing rules. */
    public AgencyRecommender() {
        rules.put(DisasterType.HURRICANE, List.of("Fire & Emergency", "Electricity Authority",
                "Transportation Dept", "Waste Management", "Hospital Services"));
        rules.put(DisasterType.EARTHQUAKE, List.of("Fire & Emergency", "Hospital Services",
                "Transportation Dept", "Water Supply Authority", "Law Enforcement"));
        rules.put(DisasterType.FIRE, List.of("Fire & Emergency", "Hospital Services",
                "Law Enforcement", "Electricity Authority"));
        rules.put(DisasterType.FLOOD, List.of("Fire & Emergency", "Water Supply Authority",
                "Transportation Dept", "Hospital Services"));
        rules.put(DisasterType.OTHER, List.of("Fire & Emergency", "Law Enforcement"));
    }

    /**
     * @return department names recommended for the disaster, in priority order.
     *         Critical incidents also bring in School Administration for shelters.
     */
    public List<String> recommendNames(DisasterType type, Severity severity) {
        Set<String> names = new LinkedHashSet<>(rules.getOrDefault(type, List.of()));
        if (severity == Severity.CRITICAL) {
            names.add("School Administration");
        }
        return new ArrayList<>(names);
    }

    /** @return the subset of departments recommended, in recommendation order */
    public List<Department> recommend(DisasterType type, Severity severity, List<Department> all) {
        List<Department> result = new ArrayList<>();
        for (String name : recommendNames(type, severity)) {
            all.stream().filter(d -> d.getName().equalsIgnoreCase(name)).findFirst().ifPresent(result::add);
        }
        return result;
    }
}
