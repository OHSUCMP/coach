package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.service.FHIRService;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;
import java.util.List;

public interface VendorTransformer {

    String getPatientLookup(String id);
    String getEncounterQuery(String patientId);
    String getEncounterQuery(String patientId, String lookbackPeriod);
    String getObservationCategoryQuery(String patientId, String category);
    String getObservationCategoryQuery(String patientId, String category, String lookbackPeriod);
    String getObservationCodeQuery(String patientId, String code);
    String getObservationCodeQuery(String patientId, String code, String lookbackPeriod);
    String getConditionQuery(String patientId, String category);
    String getGoalQuery(String patientId);
    String getMedicationStatementQuery(String patientId);
    String getMedicationRequestQuery(String patientId);
    String getProcedureQuery(String patientId);
    String getServiceRequestQuery(String patientId);

    Bundle writeRemote(String sessionId, FHIRService fhirService, Bundle bundle) throws DataException, IOException, ConfigurationException, ScopeException;

    List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException;
    Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException;

    List<PulseModel> transformIncomingPulseReadings(Bundle bundle) throws DataException;
    Bundle transformOutgoingPulseReading(PulseModel model) throws DataException;

    List<GoalModel> transformIncomingGoals(Bundle bundle) throws DataException;
    Bundle transformOutgoingGoal(GoalModel model) throws DataException;
}
