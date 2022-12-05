package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class BaseVendorTransformer implements VendorTransformer {
    protected static final String NO_ENCOUNTERS_KEY = null; // intentionally instantiated with null value

    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    protected static final String UUID_NOTE_TAG = "COACH_OBSERVATION_GROUP_UUID::";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected UserWorkspace workspace;

    public BaseVendorTransformer(UserWorkspace workspace) {
        this.workspace = workspace;
    }


    protected abstract BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation bpObservation, Observation protocolObservation, FhirConfigManager fcm) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation systolicObservation, Observation diastolicObservation, Observation protocolObservation, FhirConfigManager fcm) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Observation o, FhirConfigManager fcm) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Observation systolicObservation, Observation diastolicObservation, FhirConfigManager fcm) throws DataException;

    @Override
    public List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();

        List<Coding> bpPanelCodings = fcm.getBpPanelCodings();
        List<Coding> systolicCodings = fcm.getSystolicCodings();
        List<Coding> diastolicCodings = fcm.getDiastolicCodings();

        List<BloodPressureModel> list = new ArrayList<>();

        for (Encounter encounter : getAllEncounters(bundle)) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> bpObservationList = new ArrayList<>();            // potentially many per encounter
                Map<String, SystolicDiastolicPair> map = new LinkedHashMap<>();     // potentially many per encounter

                Observation protocol = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if ( ! o.hasCode() ) {
                        logger.warn("observation " + o.getId() + " missing code - skipping -");
                        continue;
                    }

                    if (FhirUtil.hasCoding(o.getCode(), bpPanelCodings)) {
                        bpObservationList.add(o);
                        iter.remove();

                    } else if (FhirUtil.hasCoding(o.getCode(), systolicCodings)) {
                        String key = getObservationMatchKey(o);
                        if ( ! map.containsKey(key) ) {
                            map.put(key, new SystolicDiastolicPair());
                        }
                        map.get(key).setSystolicObservation(o);

                    } else if (FhirUtil.hasCoding(o.getCode(), diastolicCodings)) {
                        String key = getObservationMatchKey(o);
                        if ( ! map.containsKey(key) ) {
                            map.put(key, new SystolicDiastolicPair());
                        }
                        map.get(key).setDiastolicObservation(o);

                    } else if (protocol == null && FhirUtil.hasCoding(o.getCode(), fcm.getProtocolCoding())) {
                        logger.debug("protocolObservation = " + o.getId() + " (encounter=" + encounter.getId() +
                                ") (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        protocol = o;
                        iter.remove();
                    }
                }

                for (Observation bp : bpObservationList) {
                    logger.debug("bpObservation = " + bp.getId() + " (encounter=" + encounter.getId() +
                            ") (effectiveDateTime=" + bp.getEffectiveDateTimeType().getValueAsString() + ")");
                    try {
                        list.add(buildBloodPressureModel(encounter, bp, protocol, fcm));

                    } catch (DataException e) {
                        logger.warn("caught " + e.getClass().getSimpleName() +
                                " building BloodPressureModel from Observation with id=" + bp.getId() + " - " +
                                e.getMessage() + " - skipping -", e);
                    }
                }

                for (Map.Entry<String, SystolicDiastolicPair> entry : map.entrySet()) {
                    SystolicDiastolicPair sdp = entry.getValue();
                    if (sdp.isValid()) {
                        Observation systolic = sdp.getSystolicObservation();
                        Observation diastolic = sdp.getDiastolicObservation();

                        logger.debug("systolicObservation = " + systolic.getId() + " (effectiveDateTime=" +
                                systolic.getEffectiveDateTimeType().getValueAsString() + ")");
                        logger.debug("diastolicObservation = " + diastolic.getId() + " (effectiveDateTime=" +
                                diastolic.getEffectiveDateTimeType().getValueAsString() + ")");

                        try {
                            list.add(buildBloodPressureModel(encounter, systolic, diastolic, protocol, fcm));

                        } catch (DataException e) {
                            logger.warn("caught " + e.getClass().getSimpleName() +
                                    " building BloodPressureModel from (systolic, diastolic) Observations with systolic.id=" +
                                    systolic.getId() + ", diastolic.id=" + diastolic.getId() + " - " +
                                    e.getMessage() + " - skipping -", e);
                        }

                    } else {
                        logger.warn("found incomplete systolic-diastolic pair for readingDate=" + entry.getKey() + " - skipping -");
                    }
                }

            } else {
                logger.debug("no Observations found for Encounter " + encounter.getId());
            }
        }

        // there may be BP observations in the system that aren't tied to any encounters.  we still want to capture these
        // of course, we can't associate any other observations with them (e.g. protocol), but whatever.  better than nothing

        // these observations without Encounters that also have identical timestamps are presumed to be related.
        // these need to be combined into a single BloodPresureModel object for any pair of (systolic, diastolic) that
        // have the same timestamp

        Map<String, SystolicDiastolicPair> map = new LinkedHashMap<>();

        for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
            if (entry.getValue() != null) {
                for (Observation o : entry.getValue()) {
                    if (o.hasCode()) {
                        if (FhirUtil.hasCoding(o.getCode(), bpPanelCodings)) {
                            logger.debug("bpObservation = " + o.getId() + " (no encounter) (effectiveDateTime=" +
                                    o.getEffectiveDateTimeType().getValueAsString() + ")");

                            try {
                                list.add(buildBloodPressureModel(o, fcm));

                            } catch (DataException e) {
                                logger.warn("caught " + e.getClass().getSimpleName() +
                                        " building BloodPressureModel from Observation with id=" + o.getId() + " - " +
                                        e.getMessage() + " - skipping -", e);
                            }

                        } else if (FhirUtil.hasCoding(o.getCode(), systolicCodings)) {
                            String key = getObservationMatchKey(o);
                            if ( ! map.containsKey(key) ) {
                                map.put(key, new SystolicDiastolicPair());
                            }
                            map.get(key).setSystolicObservation(o);

                        } else if (FhirUtil.hasCoding(o.getCode(), diastolicCodings)) {
                            String key = getObservationMatchKey(o);
                            if ( ! map.containsKey(key) ) {
                                map.put(key, new SystolicDiastolicPair());
                            }
                            map.get(key).setDiastolicObservation(o);

                        } else {
                            logger.debug("did not process Observation " + o.getId());
                        }
                    }
                }
            }
        }

        // now process dateObservationsMap, which should only include individual systolic and diastolic readings

        for (Map.Entry<String, SystolicDiastolicPair> entry : map.entrySet()) {
            SystolicDiastolicPair sdp = entry.getValue();
            if (sdp.isValid()) {
                Observation systolic = sdp.getSystolicObservation();
                Observation diastolic = sdp.getDiastolicObservation();

                logger.debug("systolicObservation = " + systolic.getId() + " (effectiveDateTime=" +
                        systolic.getEffectiveDateTimeType().getValueAsString() + ")");
                logger.debug("diastolicObservation = " + diastolic.getId() + " (effectiveDateTime=" +
                        diastolic.getEffectiveDateTimeType().getValueAsString() + ")");

                try {
                    list.add(buildBloodPressureModel(systolic, diastolic, fcm));

                } catch (DataException e) {
                    logger.warn("caught " + e.getClass().getSimpleName() +
                            " building BloodPressureModel from (systolic, diastolic) Observations with systolic.id=" +
                            systolic.getId() + ", diastolic.id=" + diastolic.getId() + " - " +
                            e.getMessage() + " - skipping -", e);
                }

            } else {
                logger.warn("found incomplete systolic-diastolic pair for readingDate=" + entry.getKey() + " - skipping -");
            }
        }

        return list;
    }

    // helper class for organizing working objects
    private static final class SystolicDiastolicPair {
        private Observation systolicObservation = null;
        private Observation diastolicObservation = null;

        public boolean isValid() {
            return systolicObservation != null && diastolicObservation != null;
        }

        public Observation getSystolicObservation() {
            return systolicObservation;
        }

        public void setSystolicObservation(Observation systolicObservation) {
            this.systolicObservation = systolicObservation;
        }

        public Observation getDiastolicObservation() {
            return diastolicObservation;
        }

        public void setDiastolicObservation(Observation diastolicObservation) {
            this.diastolicObservation = diastolicObservation;
        }
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

//    protected Observation buildHomeHealthBloodPressureObservation(BloodPressureModel model, String patientId, FhirConfigManager fcm) throws DataException {
//        return buildHomeHealthBloodPressureObservation(model, null, patientId, fcm);
//    }

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

    // most Encounters will be in the workspace cache, but newly created ones will not be in there yet,
    // although they *will* be in the bundle passed in as a parameter.  so consolidate those into one list
    protected List<Encounter> getAllEncounters(Bundle bundle) {
        List<Encounter> list = new ArrayList<>();

        list.addAll(workspace.getEncounters());

        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    if (entry.getResource() instanceof Encounter) {
                        list.add((Encounter) entry.getResource());
                    }
                }
            }
        }

        return list;
    }

    private String getObservationMatchKey(Observation observation) {
        String uuid = getUUIDFromNote(observation); // used by Epic, but Default should still understand and use this if it exists
        return uuid != null ?
                uuid :
                observation.getEffectiveDateTimeType().getValueAsString();
    }

    private String getUUIDFromNote(Observation observation) {
        if (observation != null && observation.hasNote()) {
            for (Annotation annotation : observation.getNote()) {
                if (annotation.hasText() && annotation.getText().startsWith(UUID_NOTE_TAG)) {
                    return annotation.getText().substring(UUID_NOTE_TAG.length() + 1);
                }
            }
        }
        return null;
    }
}
