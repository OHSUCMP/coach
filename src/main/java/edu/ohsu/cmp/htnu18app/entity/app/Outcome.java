package edu.ohsu.cmp.htnu18app.entity.app;

import org.hl7.fhir.r4.model.codesystems.AdverseEventOutcome;

import java.util.HashMap;
import java.util.Map;

public enum Outcome {
    // these values taken from https://www.hl7.org/fhir/valueset-adverse-event-outcome.html

    RESOLVED("resolved"),
//    RECOVERING("recovering"),
    ONGOING("ongoing");
//    RESOLVED_WITH_SEQUELAE("resolved-with-sequelae"),
//    FATAL("fatal"),
//    UNKNOWN("unknown");

    private static final Map<String, Outcome> MAP = new HashMap<>();
    static {
        for (Outcome o : values()) {
            MAP.put(o.fhirValue, o);
        }
    }

    public static Outcome fromFhirValue(String fhirValue) {
        return MAP.get(fhirValue);
    }

    private final String fhirValue;

    Outcome(String fhirValue) {
        this.fhirValue = fhirValue;
    }

    public String getFhirValue() {
        return fhirValue;
    }

    public AdverseEventOutcome toAdverseEventOutcome() {
        switch (this) {
            case RESOLVED: return AdverseEventOutcome.RESOLVED;
//            case RECOVERING: return AdverseEventOutcome.RECOVERING;
            case ONGOING: return AdverseEventOutcome.ONGOING;
//            case RESOLVED_WITH_SEQUELAE: return AdverseEventOutcome.RESOLVEDWITHSEQUELAE;
//            case FATAL: return AdverseEventOutcome.FATAL;
//            case UNKNOWN: return AdverseEventOutcome.UNKNOWN;
            default: return null;
        }
    }
}
