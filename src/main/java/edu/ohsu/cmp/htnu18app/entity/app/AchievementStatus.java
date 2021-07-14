package edu.ohsu.cmp.htnu18app.entity.app;

import java.util.HashMap;
import java.util.Map;

public enum AchievementStatus {
    // these values taken from https://www.hl7.org/fhir/valueset-goal-achievement.html
    IN_PROGRESS("in-progress"),
//    IMPROVING("improving"),
//    WORSENING("worsening"),
//    NO_CHANGE("no-change"),
    ACHIEVED("achieved"),
//    SUSTAINING("sustaining"),
    NOT_ACHIEVED("not-achieved");
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

    AchievementStatus(String fhirValue) {
        this.fhirValue = fhirValue;
    }

    public String getFhirValue() {
        return fhirValue;
    }
}
