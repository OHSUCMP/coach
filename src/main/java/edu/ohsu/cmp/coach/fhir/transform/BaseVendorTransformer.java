package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;

import java.util.*;

public abstract class BaseVendorTransformer implements VendorTransformer {
    protected static final String NO_ENCOUNTERS_KEY = null; // intentionally instantiated with null value

    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";


    protected UserWorkspace workspace;

    public BaseVendorTransformer(UserWorkspace workspace) {
        this.workspace = workspace;
    }

    protected Map<String, List<Observation>> buildEncounterObservationsMap(Bundle bundle) {
        Map<String, List<Observation>> map = new HashMap<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();
                    if (observation.hasEncounter()) {
                        List<String> keys = buildKeys(observation.getEncounter());

                        // we want to associate THE SAME list with each key, NOT separate instances of identical lists

                        List<Observation> list = null;
                        for (String key : keys) {
                            if (map.containsKey(key)) {
                                list = map.get(key);
                                break;
                            }
                        }
                        if (list == null) {
                            list = new ArrayList<>();
                            for (String key : keys) {
                                map.put(key, list);
                            }
                        }

                        map.get(keys.get(0)).add(observation);

                    } else {
                        List<Observation> list = map.get(NO_ENCOUNTERS_KEY);
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(NO_ENCOUNTERS_KEY, list);
                        }
                        list.add(observation);
                    }
                }
            }
        }
        return map;
    }

    protected List<Observation> getObservationsFromMap(Encounter encounter, Map<String, List<Observation>> map) {
        List<Observation> list = null;
        for (String key : buildKeys(encounter.getId(), encounter.getIdentifier())) {
            if (map.containsKey(key)) {     // the same exact list may be represented multiple times for different keys.  we only care about the first
                if (list == null) {
                    list = map.remove(key);
                } else {
                    map.remove(key);
                }
            }
        }
        return list;
    }

    protected List<String> buildKeys(Reference reference) {
        return FhirUtil.buildKeys(reference);
    }

    protected List<String> buildKeys(String id, Identifier identifier) {
        return FhirUtil.buildKeys(id, identifier);
    }

    protected List<String> buildKeys(String id, List<Identifier> identifiers) {
        return FhirUtil.buildKeys(id, identifiers);
    }

    protected String genTemporaryId() {
        return UUID.randomUUID().toString();
    }

//    adapted from CDSHooksExecutor.buildHomeBloodPressureObservation()
//    used when creating new Home Health (HH) Blood Pressure Observations

    protected Observation buildHomeHealthBloodPressureObservation(BloodPressureModel model, String patientId, FhirConfigManager fcm) throws DataException {
        return buildHomeHealthBloodPressureObservation(model, null, patientId, fcm);
    }

    protected Observation buildHomeHealthBloodPressureObservation(BloodPressureModel model, Encounter encounter, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        if (encounter != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(encounter)));
        } else if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        FhirUtil.addHomeSettingExtension(o);

        if (model.getSystolic() != null && model.getDiastolic() == null) {            // systolic only
            o.getCode().addCoding(fcm.getBpSystolicCoding());
            o.getCode().addCoding(fcm.getBpHomeBluetoothSystolicCoding());
            o.setValue(new Quantity());
            setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

        } else if (model.getSystolic() == null && model.getDiastolic() != null) {     // diastolic only
            o.getCode().addCoding(fcm.getBpDiastolicCoding());
            o.getCode().addCoding(fcm.getBpHomeBluetoothDiastolicCoding());
            o.setValue(new Quantity());
            setBPValue(o.getValueQuantity(), model.getDiastolic(), fcm);

        } else if (model.getSystolic() != null && model.getDiastolic() != null) {     // both systolic and diastolic
            o.getCode().addCoding(fcm.getBpCoding());
            for (Coding c : fcm.getBpHomeCodings()) {
                o.getCode().addCoding(c);
            }

            Observation.ObservationComponentComponent occSystolic = new Observation.ObservationComponentComponent();
            occSystolic.getCode().addCoding(fcm.getBpSystolicCoding());
            occSystolic.setValue(new Quantity());
            setBPValue(occSystolic.getValueQuantity(), model.getSystolic(), fcm);
            o.getComponent().add(occSystolic);

            Observation.ObservationComponentComponent occDiastolic = new Observation.ObservationComponentComponent();
            occDiastolic.getCode().addCoding(fcm.getBpDiastolicCoding());
            occDiastolic.setValue(new Quantity());
            setBPValue(occDiastolic.getValueQuantity(), model.getDiastolic(), fcm);
            o.getComponent().add(occDiastolic);

        } else {
            throw new DataException("BP observation requires systolic and / or diastolic");
        }

        o.setEffective(new DateTimeType(model.getReadingDate()));

        return o;
    }
    protected Observation buildProtocolObservation(AbstractVitalsModel model, String patientId, FhirConfigManager fcm) throws DataException {
        return buildProtocolObservation(model, null, patientId, fcm);
    }

    protected Observation buildProtocolObservation(AbstractVitalsModel model, Encounter encounter, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        if (encounter != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(encounter)));
        } else if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding(fcm.getProtocolCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        String answerValue = model.getFollowedProtocol() ?
                fcm.getProtocolAnswerYes() :
                fcm.getProtocolAnswerNo();

        o.setValue(new CodeableConcept());
        o.getValueCodeableConcept()
                .setText(answerValue)
                .addCoding(fcm.getProtocolAnswerCoding());

        return o;
    }
    protected Observation buildPulseObservation(PulseModel model, String patientId, FhirConfigManager fcm) throws DataException {
        return buildPulseObservation(model, null, patientId, fcm);
    }

    protected Observation buildPulseObservation(PulseModel model, Encounter encounter, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        if (encounter != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(encounter)));
        } else if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }
//        o.setEncounter(new Reference().setReference(URN_UUID + enc.getId()));

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        o.getCode().addCoding(fcm.getPulseCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        o.setValue(new Quantity());
        o.getValueQuantity()
                .setCode(fcm.getPulseValueCode())
                .setSystem(fcm.getPulseValueSystem())
                .setUnit(fcm.getPulseValueUnit())
                .setValue(model.getPulse().getValue().intValue());

        return o;
    }

    protected Goal buildGoal(GoalModel model, String patientId, FhirConfigManager fcm) {

        // this is only used when building local goals, for which sourceGoal == null

        Goal g = new Goal();

        g.setId(model.getExtGoalId());
        g.setSubject(new Reference().setReference(patientId));
        g.setLifecycleStatus(model.getLifecycleStatus().getFhirValue());
        g.getAchievementStatus().addCoding().setCode(model.getAchievementStatus().getFhirValue())
                .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");
        g.getCategoryFirstRep().addCoding().setCode(model.getReferenceCode()).setSystem(model.getReferenceSystem());
        g.getDescription().setText(model.getGoalText());
        g.setStatusDate(model.getStatusDate());
        g.getTarget().add(new Goal.GoalTargetComponent()
                .setDue(new DateType().setValue(model.getTargetDate())));

        if (model.isBPGoal()) {
            Goal.GoalTargetComponent systolic = new Goal.GoalTargetComponent();
            systolic.getMeasure().addCoding(fcm.getBpSystolicCoding());
            systolic.setDetail(new Quantity());
            systolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            systolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            systolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            systolic.getDetailQuantity().setValue(model.getSystolicTarget());
            g.getTarget().add(systolic);

            Goal.GoalTargetComponent diastolic = new Goal.GoalTargetComponent();
            diastolic.getMeasure().addCoding(fcm.getBpDiastolicCoding());
            diastolic.setDetail(new Quantity());
            diastolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            diastolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            diastolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            diastolic.getDetailQuantity().setValue(model.getDiastolicTarget());
            g.getTarget().add(diastolic);
        }

        return g;
    }

///////////////////////////////////////////////////////////////////////
// private methods
//

    private void setBPValue(Quantity q, QuantityModel qm, FhirConfigManager fcm) {
        q.setCode(fcm.getBpValueCode())
                .setSystem(fcm.getBpValueSystem())
                .setUnit(fcm.getBpValueUnit())
                .setValue(qm.getValue().intValue());
    }
}
