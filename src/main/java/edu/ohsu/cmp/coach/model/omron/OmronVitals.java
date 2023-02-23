package edu.ohsu.cmp.coach.model.omron;

import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.PulseModel;

public class OmronVitals {
    private BloodPressureModel bloodPressureModel;
    private PulseModel pulseModel;

    public OmronVitals(BloodPressureModel bloodPressureModel, PulseModel pulseModel) {
        this.bloodPressureModel = bloodPressureModel;
        this.pulseModel = pulseModel;
    }

    public BloodPressureModel getBloodPressureModel() {
        return bloodPressureModel;
    }

    public PulseModel getPulseModel() {
        return pulseModel;
    }
}
