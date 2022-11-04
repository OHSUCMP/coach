package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.service.FHIRService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class EpicVendorTransformer extends BaseVendorTransformer implements VendorTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String PROTOCOL_NOTE_TAG = "COACH_PROTOCOL::";

    private final DefaultVendorTransformer defaultTransformer;

    public EpicVendorTransformer(UserWorkspace workspace) {
        super(workspace);
        defaultTransformer = new DefaultVendorTransformer(workspace);
    }

    @Override
    public Bundle writeRemote(String sessionId, FHIRService fhirService, Bundle bundle) throws DataException, IOException, ConfigurationException, ScopeException {

        // in Epic, we need to post resources to flowsheets one at a time

        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();

        Bundle responseBundle = new Bundle();
        responseBundle.setType(Bundle.BundleType.COLLECTION);

        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource r = entry.getResource();
                if (r instanceof IDomainResource && FhirUtil.isUUID(r.getId())) {
                    try {
                        FhirUtil.appendResourceToBundle(responseBundle,
                                fhirService.transact(fcc, (DomainResource) r)
                        );

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getSimpleName() + " attempting to transact " +
                                r.getClass().getSimpleName() + " - " + e.getMessage() + " - skipping -", e);
                        if (logger.isDebugEnabled()) {
                            logger.debug(r.getClass().getSimpleName() + " resource : " + FhirUtil.toJson(r));
                        }
                    }
                }
            }
        }

        return responseBundle;
    }

    @Override
    public List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        List<Coding> bpCodings = fcm.getAllBpCodings();

        List<BloodPressureModel> list = new ArrayList<>();

        for (Encounter encounter : getAllEncounters(bundle)) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> bpObservationList = new ArrayList<>();    // potentially many per encounter
                Observation protocolObservation = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), bpCodings)) {
                        logger.debug("bpObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        bpObservationList.add(o);
                        iter.remove();

                    } else if (protocolObservation == null && FhirUtil.hasCoding(o.getCode(), fcm.getProtocolCoding())) {
                        logger.debug("protocolObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        protocolObservation = o;
                        iter.remove();
                    }
                }

                for (Observation bpObservation : bpObservationList) {
                    BloodPressureModel bpm = new BloodPressureModel(encounter, bpObservation, protocolObservation, fcm);

                    // Epic hack to set protocol information from custom-serialized note in the Observation resource
                    // if no protocol resource is found

                    if (protocolObservation == null) {
                        Boolean followedProtocol = getFollowedProtocolFromNote(bpObservation, fcm);
                        if (followedProtocol != null) {
                            bpm.setFollowedProtocol(followedProtocol);
                        }
                    }

                    list.add(bpm);
                }

                bpObservationList.clear();

            } else {
                logger.debug("no Observations found for Encounter " + encounter.getId());
            }
        }

        // there may be BP observations in the system that aren't tied to any encounters.  we still want to capture these
        // of course, we can't associate any other observations with them (e.g. protocol), but whatever.  better than nothing

        // these observations without Encounters are probably stored in flowsheets as individual systolic
        // and diastolic readings.  these need to be combined into a single BloodPresureModel object for any
        // pair of (systolic, diastolic) that have the same timestamp

        List<Coding> systolicCodings = fcm.getSystolicCodings();
        List<Coding> diastolicCodings = fcm.getDiastolicCodings();
        List<Coding> bpPanelCodings = fcm.getBpPanelCodings();

        Map<String, List<Observation>> dateObservationsMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
            if (entry.getValue() != null) {
                for (Observation o : entry.getValue()) {
                    if (o.hasCode()) {
                        if (FhirUtil.hasCoding(o.getCode(), bpPanelCodings)) {
                            logger.debug("bpObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                            BloodPressureModel bpm = new BloodPressureModel(o, fcm);

                            // in Epic, protocol information is represented in a custom-serialized note on the Observation resource
                            // if no Observation resource for the protocol exists

                            Boolean followedProtocol = getFollowedProtocolFromNote(o, fcm);
                            if (followedProtocol != null) {
                                bpm.setFollowedProtocol(followedProtocol);
                            }

                            list.add(bpm);

                        } else if (FhirUtil.hasCoding(o.getCode(), systolicCodings) || FhirUtil.hasCoding(o.getCode(), diastolicCodings)) {
                            String dateStr = o.getEffectiveDateTimeType().getValueAsString();
                            if ( ! dateObservationsMap.containsKey(dateStr) ) {
                                dateObservationsMap.put(dateStr, new ArrayList<>());
                            }
                            dateObservationsMap.get(dateStr).add(o);

                        } else {
                            logger.debug("did not process Observation " + o.getId());
                        }
                    }
                }
            }
        }

        // now process dateObservationsMap, which should only include individual systolic and diastolic readings

        for (Map.Entry<String, List<Observation>> entry : dateObservationsMap.entrySet()) {
            List<Observation> list2 = entry.getValue();

            if (list2.size() == 1) {        // systolic OR diastolic only; treat the same as above
                Observation o = list2.get(0);
                logger.debug("bpObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                BloodPressureModel bpm = new BloodPressureModel(o, fcm);

                // in Epic, protocol information is represented in a custom-serialized note on the Observation resource
                // if no Observation resource for the protocol exists

                Boolean followedProtocol = getFollowedProtocolFromNote(o, fcm);
                if (followedProtocol != null) {
                    bpm.setFollowedProtocol(followedProtocol);
                }

                list.add(bpm);

            } else if (list2.size() == 2) { // probably both systolic and diastolic, but check for sure
                Observation o1 = list2.get(0);
                Observation o2 = list2.get(1);

                Observation systolicObservation;
                Observation diastolicObservation;

                if (o1.hasCode() && FhirUtil.hasCoding(o1.getCode(), systolicCodings) &&
                        o2.hasCode() && FhirUtil.hasCoding(o2.getCode(), diastolicCodings)) {
                    systolicObservation = o1;
                    diastolicObservation = o2;

                } else if (o1.hasCode() && FhirUtil.hasCoding(o1.getCode(), diastolicCodings) &&
                        o2.hasCode() && FhirUtil.hasCoding(o2.getCode(), systolicCodings)) {
                    systolicObservation = o2;
                    diastolicObservation = o1;

                } else {
                    logger.warn("unexpected Observation pair building BloodPressureModel for ids=[" +
                            o1.getId() + ", " + o2.getId() + "] - skipping -");
                    continue;
                }

                BloodPressureModel bpm = new BloodPressureModel(systolicObservation, diastolicObservation, fcm);

                Boolean followedProtocol = getFollowedProtocolFromNote(systolicObservation, fcm);
                if (followedProtocol == null) {
                    followedProtocol = getFollowedProtocolFromNote(diastolicObservation, fcm);
                }
                if (followedProtocol != null) {
                    bpm.setFollowedProtocol(followedProtocol);
                }

                list.add(bpm);

            } else {                        // more readings than expected, handle somehow?
                logger.warn("too many observations found with readingDate=" + entry.getKey());
            }
        }

        return list;
    }

    @Override
    public Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException {
        if (model == null) return null;

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        FhirConfigManager fcm = workspace.getFhirConfigManager();

        // if the observation came from the EHR, just package it up and send it along
        if (model.getSourceBPObservation() != null) {
            // note : do not include Encounter for Epic
            //        also, combine protocol info into the bp observation if it exists
            //        also, the BP observation needs to be split into separate systolic and diastolic resources
            Observation systolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.SYSTOLIC, model.getSourceBPObservation(), fcm);
            appendProtocolAnswerToObservationIfNeeded(systolicObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.DIASTOLIC, model.getSourceBPObservation(), fcm);
            appendProtocolAnswerToObservationIfNeeded(diastolicObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, diastolicObservation);

        } else if (model.getSourceSystolicObservation() != null && model.getSourceDiastolicObservation() != null) {
            Observation systolicObservation = model.getSourceSystolicObservation().copy();
            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = model.getSourceDiastolicObservation().copy();
            FhirUtil.appendResourceToBundle(bundle, diastolicObservation);

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

            // When transforming a BP model to be used in Epic, we need to set a custom serialized note that
            // contains protocol information since we can't store that record separately in its own flowsheet record

            // in Epic context, BP Observations do not have Encounters, but instead use timestamp as a mechanism
            // to associated resources together

            Observation systolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.SYSTOLIC, model, patientIdRef, fcm);
            appendProtocolAnswerToObservationIfNeeded(systolicObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.DIASTOLIC, model, patientIdRef, fcm);
            appendProtocolAnswerToObservationIfNeeded(diastolicObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, diastolicObservation);
        }

        return bundle;
    }

    @Override
    public List<PulseModel> transformIncomingPulseReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();

        List<PulseModel> list = new ArrayList<>();

        for (Encounter encounter : getAllEncounters(bundle)) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> pulseObservationList = new ArrayList<>();    // potentially many per encounter
                Observation protocolObservation = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if (FhirUtil.hasCoding(o.getCode(), fcm.getPulseCodings())) {
                        logger.debug("pulseObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        pulseObservationList.add(o);
                        iter.remove();

                    } else if (protocolObservation == null && FhirUtil.hasCoding(o.getCode(), fcm.getProtocolCoding())) {
                        logger.debug("protocolObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        protocolObservation = o;
                        iter.remove();
                    }
                }

                for (Observation pulseObservation : pulseObservationList) {
                    PulseModel pm = new PulseModel(encounter, pulseObservation, protocolObservation, fcm);

                    // in Epic, protocol information is represented in a custom-serialized note on the Observation resource
                    // if no Observation resource for the protocol exists

                    if (protocolObservation == null) {
                        Boolean followedProtocol = getFollowedProtocolFromNote(pulseObservation, fcm);
                        if (followedProtocol != null) {
                            pm.setFollowedProtocol(followedProtocol);
                        }
                    }

                    list.add(pm);
                }

                pulseObservationList.clear();

            } else {
                logger.debug("no Observations found for Encounter " + encounter.getId());
            }
        }

        // there may be pulse observations in the system that aren't tied to any encounters.  we still want to capture these
        // of course, we can't associate any other observations with them (e.g. protocol), but whatever.  better than nothing

        for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
            if (entry.getValue() != null) {
                for (Observation o : entry.getValue()) {
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), fcm.getPulseCodings())) {
                        logger.debug("pulseObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        PulseModel pm = new PulseModel(o, fcm);

                        // in Epic, protocol information is represented in a custom-serialized note on the Observation resource
                        // if no Observation resource for the protocol exists

                        Boolean followedProtocol = getFollowedProtocolFromNote(o, fcm);
                        if (followedProtocol != null) {
                            pm.setFollowedProtocol(followedProtocol);
                        }

                        list.add(pm);

                    } else {
                        logger.debug("did not process Observation " + o.getId());
                    }
                }
            }
        }

        return list;
    }

    @Override
    public Bundle transformOutgoingPulseReading(PulseModel model) throws DataException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        FhirConfigManager fcm = workspace.getFhirConfigManager();

        if (model.getSourcePulseObservation() != null) {
            // note : do not include Encounter for Epic
            //        also, combine protocol info into the pulse observation if it exists
            Observation pulseObservation = model.getSourcePulseObservation().copy();
            appendProtocolAnswerToObservationIfNeeded(pulseObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, pulseObservation);

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

            // in Epic context, Pulse Observations do not have Encounters, but instead use timestamp as a mechanism
            // to associated resources together

            // in Epic, protocol information is represented in a custom-serialized note on the Observation resource
            // if no Observation resource for the protocol exists

            Observation pulseObservation = buildPulseObservation(model, patientIdRef, fcm);
            appendProtocolAnswerToObservationIfNeeded(pulseObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, pulseObservation);
        }

        return bundle;
    }

    @Override
    public List<GoalModel> transformIncomingGoals(Bundle bundle) throws DataException {
        return defaultTransformer.transformIncomingGoals(bundle);
    }

    @Override
    public Bundle transformOutgoingGoal(GoalModel model) throws DataException {
        return defaultTransformer.transformOutgoingGoal(model);
    }

////////////////////////////////////////////////////////////////////////
// private methods
//

    private enum ResourceType {
        SYSTOLIC,
        DIASTOLIC
    }

    // note : bpObservation may be a) full BP (systolic + diastolic); b) systolic only, or c) diastolic only
    private Observation buildHomeHealthBloodPressureObservation(ResourceType type, Observation bpObservation, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(bpObservation.getSubject());

        if (bpObservation.hasEncounter()) {
            o.setEncounter(bpObservation.getEncounter());
        }

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        FhirUtil.addHomeSettingExtension(o);

        if (bpObservation.hasCode()) {
            CodeableConcept code = bpObservation.getCode();
            if (type == ResourceType.SYSTOLIC && FhirUtil.hasCoding(code, fcm.getSystolicCodings())) {
//                o.getCode().addCoding(fcm.getBpSystolicCoding());     // do not include LOINC codes in Epic-destined observations
                o.getCode().addCoding(fcm.getBpEpicSystolicCoding());
                o.setValue(bpObservation.getValueQuantity());

            } else if (type == ResourceType.DIASTOLIC && FhirUtil.hasCoding(code, fcm.getDiastolicCodings())) {
//                o.getCode().addCoding(fcm.getBpDiastolicCoding());    // do not include LOINC codes in Epic-destined observations
                o.getCode().addCoding(fcm.getBpEpicDiastolicCoding());
                o.setValue(bpObservation.getValueQuantity());

            } else if (FhirUtil.hasCoding(code, fcm.getBpPanelCodings())) {
                if (bpObservation.hasComponent()) {
                    if (type == ResourceType.SYSTOLIC) {
                        Observation.ObservationComponentComponent component = getComponentHavingCoding(bpObservation, fcm.getSystolicCodings());
                        o.setValue(component.getValueQuantity());

                    } else if (type == ResourceType.DIASTOLIC) {
                        Observation.ObservationComponentComponent component = getComponentHavingCoding(bpObservation, fcm.getDiastolicCodings());
                        o.setValue(component.getValueQuantity());

                    } else {
                        throw new CaseNotHandledException("invalid type");
                    }
                }

            } else {
                throw new DataException("invalid coding");
            }
        } else {
            throw new DataException("missing coding");
        }

        o.getValueQuantity().setUnit(null);         // Epic doesn't allow units to be specified

        o.setEffective(bpObservation.getEffective());

        return o;
    }

    private Observation buildHomeHealthBloodPressureObservation(ResourceType type, BloodPressureModel model, String patientIdRef, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientIdRef));

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        FhirUtil.addHomeSettingExtension(o);

        if (type == ResourceType.SYSTOLIC) {
            if (model.getSystolic() != null) {
//                o.getCode().addCoding(fcm.getBpSystolicCoding()); // Epic flowsheet observations should not have LOINC code
                o.getCode().addCoding(fcm.getBpEpicSystolicCoding());
                o.setValue(new Quantity());
                setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

            } else {
                throw new DataException("Cannot create systolic Observation - model is missing systolic value");
            }

        } else if (type == ResourceType.DIASTOLIC) {
            if (model.getDiastolic() != null) {
//                o.getCode().addCoding(fcm.getBpDiastolicCoding()); // Epic flowsheet observations should not have LOINC code
                o.getCode().addCoding(fcm.getBpEpicDiastolicCoding());
                o.setValue(new Quantity());
                setBPValue(o.getValueQuantity(), model.getDiastolic(), fcm);

            } else {
                throw new DataException("Cannot create diastolic Observation - model is missing diastolic value");
            }

        } else {
            throw new DataException("type must be SYSTOLIC or DIASTOLIC");
        }

        o.setEffective(new DateTimeType(model.getReadingDate()));

        return o;
    }

    private Boolean getFollowedProtocolFromNote(Observation bpObservation, FhirConfigManager fcm) {
        // Epic hack to handle when protocol-followed info is custom serialized into a note field
        if (bpObservation.hasNote()) {
            for (Annotation annotation : bpObservation.getNote()) {
                if (annotation.hasText()) {
                    if (annotation.getText().equals(PROTOCOL_NOTE_TAG + fcm.getProtocolAnswerYes())) {
                        return true;

                    } else if (annotation.getText().equals(PROTOCOL_NOTE_TAG + fcm.getProtocolAnswerNo())) {
                        return false;
                    }
                }
            }
        }
        return null;
    }

    private void appendProtocolAnswerToObservationIfNeeded(Observation observation, AbstractVitalsModel model, FhirConfigManager fcm) {
        if ( ! hasProtocolAnswerNote(observation) ) {
            if (model.getSourceProtocolObservation() != null) {
                Observation o = model.getSourceProtocolObservation();
                if (o.hasValueCodeableConcept()) {
                    CodeableConcept cc = o.getValueCodeableConcept();
                    if (cc.hasText()) {
                        appendProtocolAnswerNote(observation, cc.getText());
                    }
                }

            } else if (model.getFollowedProtocol() != null) {
                String s = model.getFollowedProtocol() ? fcm.getProtocolAnswerYes() : fcm.getProtocolAnswerNo();
                appendProtocolAnswerNote(observation, s);
            }
        }
    }

    private boolean hasProtocolAnswerNote(Observation observation) {
        if (observation == null) return false;
        if (observation.hasNote()) {
            for (Annotation annotation : observation.getNote()) {
                if (annotation.hasText() && annotation.getText().startsWith(PROTOCOL_NOTE_TAG)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void appendProtocolAnswerNote(Observation observation, String note) {
        observation.getNote().add(new Annotation().setText(PROTOCOL_NOTE_TAG + note));
    }

    private Observation.ObservationComponentComponent getComponentHavingCoding(Observation observation, List<Coding> coding) throws DataException {
        if (observation == null) return null;
        if (observation.hasComponent()) {
            for (Observation.ObservationComponentComponent component : observation.getComponent()) {
                if (component.hasCode() && FhirUtil.hasCoding(component.getCode(), coding)) {
                    return component;
                }
            }
        }
        throw new DataException("invalid coding");
    }

    private void setBPValue(Quantity q, QuantityModel qm, FhirConfigManager fcm) {
        q.setCode(fcm.getBpValueCode())
                .setSystem(fcm.getBpValueSystem())
//                .setUnit(fcm.getBpValueUnit())        // Epic doesn't allow units to be specified
                .setValue(qm.getValue().intValue());
    }

    protected Observation buildPulseObservation(PulseModel model, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        // Epic doesn't use encounters for user-generated records, but if it came in with one, add it

        if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        o.getCode().addCoding(fcm.getPulseEpicCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        o.setValue(new Quantity());
        o.getValueQuantity()
                .setCode(fcm.getPulseValueCode())
                .setSystem(fcm.getPulseValueSystem())
//                .setUnit(fcm.getPulseValueUnit())     // Epic doesn't like units
                .setValue(model.getPulse().getValue().intValue());

        return o;
    }

}
