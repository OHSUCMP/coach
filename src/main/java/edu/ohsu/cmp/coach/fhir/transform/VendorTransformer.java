package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

public interface VendorTransformer {
    List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException;
    Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException;

    List<PulseModel> transformIncomingPulseReadings(Bundle bundle) throws DataException;
    Bundle transformOutgoingPulseReading(PulseModel model) throws DataException;

    List<GoalModel> transformIncomingGoals(Bundle bundle) throws DataException;
    Bundle transformOutgoingGoal(GoalModel model) throws DataException;
}
