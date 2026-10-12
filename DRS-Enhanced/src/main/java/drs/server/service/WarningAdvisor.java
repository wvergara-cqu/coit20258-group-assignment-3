package drs.server.service;

import java.util.EnumMap;
import java.util.Map;

import drs.common.model.DisasterReport;
import drs.common.model.DisasterType;
import drs.common.model.PublicWarning;
import drs.common.model.Severity;
import drs.common.model.WarningLevel;
import drs.common.util.InputValidator;

/**
 * Enhanced Feature 1 business rules. Pre-fills a public warning from an incident
 * (UC-6 step 2) and validates warnings before they are broadcast.
 */
public class WarningAdvisor {

    /** Largest permitted warning radius in km. */
    public static final int MAX_RADIUS_KM = 200;
    /** Longest permitted validity in hours. */
    public static final int MAX_VALID_HOURS = 72;

    private final Map<DisasterType, Integer> baseRadiusKm = new EnumMap<>(DisasterType.class);
    private final Map<Severity, Double> severityMultiplier = new EnumMap<>(Severity.class);
    private final Map<WarningLevel, Integer> defaultHours = new EnumMap<>(WarningLevel.class);

    /** Builds the default advisory rules. */
    public WarningAdvisor() {
        baseRadiusKm.put(DisasterType.HURRICANE, 25);
        baseRadiusKm.put(DisasterType.EARTHQUAKE, 15);
        baseRadiusKm.put(DisasterType.FLOOD, 10);
        baseRadiusKm.put(DisasterType.FIRE, 5);
        baseRadiusKm.put(DisasterType.OTHER, 3);
        severityMultiplier.put(Severity.LOW, 1.0);
        severityMultiplier.put(Severity.MEDIUM, 1.5);
        severityMultiplier.put(Severity.HIGH, 2.0);
        severityMultiplier.put(Severity.CRITICAL, 3.0);
        defaultHours.put(WarningLevel.ADVICE, 24);
        defaultHours.put(WarningLevel.WATCH_AND_ACT, 12);
        defaultHours.put(WarningLevel.EMERGENCY_WARNING, 6);
    }

    /** @return the warning level suggested for a severity */
    public WarningLevel suggestLevel(Severity severity) {
        return switch (severity) {
            case LOW, MEDIUM -> WarningLevel.ADVICE;
            case HIGH -> WarningLevel.WATCH_AND_ACT;
            case CRITICAL -> WarningLevel.EMERGENCY_WARNING;
        };
    }

    /** @return the affected radius suggested for a disaster type and severity */
    public int suggestRadiusKm(DisasterType type, Severity severity) {
        return (int) Math.round(baseRadiusKm.get(type) * severityMultiplier.get(severity));
    }

    /** @return a pre-filled warning for the report, ready for the controller to edit */
    public PublicWarning suggest(DisasterReport report) {
        WarningLevel level = suggestLevel(report.getSeverity());
        String message = String.format("%s %s reported at %s. Emergency services are responding. %s",
                report.getSeverity(), report.getType().getLabel().toLowerCase(), report.getLocation(),
                level == WarningLevel.ADVICE ? "There is no immediate danger."
                        : "Conditions may be dangerous in the affected area.");
        PublicWarning w = new PublicWarning(report.getId(), level, report.getLocation(),
                suggestRadiusKm(report.getType(), report.getSeverity()), message,
                level.getDefaultAction(), defaultHours.get(level));
        w.setDisasterType(report.getType());
        return w;
    }

    /** @return null if the warning is valid, otherwise the first error message */
    public String validate(PublicWarning w) {
        if (w == null) {
            return "Warning details are missing.";
        }
        if (w.getLevel() == null) {
            return "Select a warning level.";
        }
        String err = InputValidator.required("Affected area", w.getAffectedArea(), InputValidator.MAX_SHORT);
        if (err == null) {
            err = InputValidator.required("Message", w.getMessage(), InputValidator.MAX_LONG);
        }
        if (err == null) {
            err = InputValidator.required("Recommended action", w.getRecommendedAction(), 500);
        }
        if (err != null) {
            return err;
        }
        if (w.getRadiusKm() < 1 || w.getRadiusKm() > MAX_RADIUS_KM) {
            return "Radius must be between 1 and " + MAX_RADIUS_KM + " km.";
        }
        if (w.getValidHours() < 1 || w.getValidHours() > MAX_VALID_HOURS) {
            return "Validity must be between 1 and " + MAX_VALID_HOURS + " hours.";
        }
        return null;
    }
}
