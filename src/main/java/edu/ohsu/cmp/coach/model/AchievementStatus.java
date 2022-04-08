package edu.ohsu.cmp.coach.model;

import java.util.HashMap;
import java.util.Map;

public enum AchievementStatus {
    // these values taken from https://www.hl7.org/fhir/valueset-goal-achievement.html
    IN_PROGRESS("in-progress", "In Progress"),
//    IMPROVING("improving"),
//    WORSENING("worsening"),
//    NO_CHANGE("no-change"),
    ACHIEVED("achieved", "Achieved"),
//    SUSTAINING("sustaining"),
    NOT_ACHIEVED("not-achieved", "Not Achieved");
//    NO_PROGRESS("no-progress"),
//    NOT_ATTAINABLE("not-attainable");

    private static final Map<String, AchievementStatus> MAP = new HashMap<>();
    static {
        for (AchievementStatus a : values()) {
            MAP.put(a.fhirValue, a);
        }
    }

    public static AchievementStatus fromFhirValue(String fhirValue) {
        return MAP.get(fhirValue);
    }

    private final String fhirValue;
    private final String label;

    AchievementStatus(String fhirValue, String label) {
        this.fhirValue = fhirValue;
        this.label = label;
    }

    public String getFhirValue() {
        return fhirValue;
    }

    public String getLabel() {
        return label;
    }
}
