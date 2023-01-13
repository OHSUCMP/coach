package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.service.FHIRService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class CernerVendorTransformer extends BaseVendorTransformer implements VendorTransformer {
    private static final DateFormat CERNER_FHIR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DefaultVendorTransformer defaultTransformer;

    public CernerVendorTransformer(UserWorkspace workspace) {
        super(workspace);
        this.defaultTransformer = new DefaultVendorTransformer(workspace);
    }

    @Override
    protected DateFormat getFhirDateFormat() {
        return CERNER_FHIR_DATE_FORMAT;
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation bpObservation, Observation protocolObservation) throws DataException {
        return defaultTransformer.buildBloodPressureModel(encounter, bpObservation, protocolObservation);
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation systolicObservation, Observation diastolicObservation, Observation protocolObservation) throws DataException {
        return defaultTransformer.buildBloodPressureModel(encounter, systolicObservation, diastolicObservation, protocolObservation);
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Observation o) throws DataException {
        return defaultTransformer.buildBloodPressureModel(o);
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Observation systolicObservation, Observation diastolicObservation) throws DataException {
        return defaultTransformer.buildBloodPressureModel(systolicObservation, diastolicObservation);
    }

    @Override
    public Bundle writeRemote(String sessionId, FHIRService fhirService, Bundle bundle) throws DataException, IOException, ConfigurationException, ScopeException {
        return defaultTransformer.writeRemote(sessionId, fhirService, bundle);
    }

    @Override
    public Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException {
        return defaultTransformer.transformOutgoingBloodPressureReading(model);
    }

    @Override
    public List<PulseModel> transformIncomingPulseReadings(Bundle bundle) throws DataException {
        return defaultTransformer.transformIncomingPulseReadings(bundle);
    }

    @Override
    public Bundle transformOutgoingPulseReading(PulseModel model) throws DataException {
        return defaultTransformer.transformOutgoingPulseReading(model);
    }

    @Override
    public List<GoalModel> transformIncomingGoals(Bundle bundle) throws DataException {
        return defaultTransformer.transformIncomingGoals(bundle);
    }

    @Override
    public Bundle transformOutgoingGoal(GoalModel model) throws DataException {
        return defaultTransformer.transformOutgoingGoal(model);
    }
}