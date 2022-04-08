package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.Goal;

import java.util.HashMap;
import java.util.Map;

public enum LifecycleStatus {
    // these values taken from https://www.hl7.org/fhir/valueset-goal-status.html
//    PROPOSED(Goal.GoalLifecycleStatus.PROPOSED),
//    PLANNED(Goal.GoalLifecycleStatus.PLANNED),
//    ACCEPTED(Goal.GoalLifecycleStatus.ACCEPTED),
    ACTIVE(Goal.GoalLifecycleStatus.ACTIVE),
//    ON_HOLD(Goal.GoalLifecycleStatus.ONHOLD),
    COMPLETED(Goal.GoalLifecycleStatus.COMPLETED),
    CANCELLED(Goal.GoalLifecycleStatus.CANCELLED);
//    ENTERED_IN_ERROR(Goal.GoalLifecycleStatus.ENTEREDINERROR),
//    REJECTED(Goal.GoalLifecycleStatus.REJECTED);

    private static final Map<Goal.GoalLifecycleStatus, LifecycleStatus> MAP = new HashMap<>();
    static {
        for (LifecycleStatus a : values()) {
            MAP.put(a.fhirValue, a);
        }
    }

    public static LifecycleStatus fromFhirValue(Goal.GoalLifecycleStatus fhirValue) {
        return MAP.get(fhirValue);
    }

    private final Goal.GoalLifecycleStatus fhirValue;

    LifecycleStatus(Goal.GoalLifecycleStatus fhirValue) {
        this.fhirValue = fhirValue;
    }

    public Goal.GoalLifecycleStatus getFhirValue() {
        return fhirValue;
    }
}
