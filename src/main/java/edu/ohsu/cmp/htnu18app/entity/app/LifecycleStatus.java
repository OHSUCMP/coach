package edu.ohsu.cmp.htnu18app.entity.app;

import java.util.HashMap;
import java.util.Map;

public enum LifecycleStatus {
    // these values taken from https://www.hl7.org/fhir/valueset-goal-status.html
    PROPOSED("proposed"),
    PLANNED("planned"),
    ACCEPTED("accepted"),
    ACTIVE("active"),
    ON_HOLD("on-hold"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    ENTERED_IN_ERROR("entered-in-error"),
    REJECTED("rejected");

    private static final Map<String, LifecycleStatus> MAP = new HashMap<>();
    static {
        for (LifecycleStatus a : values()) {
            MAP.put(a.fhirValue, a);
        }
    }

    public static LifecycleStatus fromFhirValue(String fhirValue) {
        return MAP.get(fhirValue);
    }

    private final String fhirValue;

    LifecycleStatus(String fhirValue) {
        this.fhirValue = fhirValue;
    }

    public String getFhirValue() {
        return fhirValue;
    }
}
