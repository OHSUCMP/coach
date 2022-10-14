package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EpicVendorTransformer extends BaseVendorTransformer implements VendorTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String PROTOCOL_NOTE_TAG = "COACH_PROTOCOL::";

    private final DefaultVendorTransformer defaultTransformer;

    public EpicVendorTransformer(UserWorkspace workspace) {
        super(workspace);
        defaultTransformer = new DefaultVendorTransformer(workspace);
    }

    @Override
    public List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        List<Coding> bpCodings = fcm.getAllBpCodings();

        List<BloodPressureModel> list = new ArrayList<>();

        for (Encounter encounter : workspace.getEncounters()) {
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

        for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
            if (entry.getValue() != null) {
                for (Observation o : entry.getValue()) {
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), bpCodings)) {
                        logger.debug("bpObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        BloodPressureModel bpm = new BloodPressureModel(o, fcm);

                        // Epic hack to set protocol information from custom-serialized note in the Observation resource
                        // if no protocol resource is found
                        Boolean followedProtocol = getFollowedProtocolFromNote(o, fcm);
                        if (followedProtocol != null) {
                            bpm.setFollowedProtocol(followedProtocol);
                        }

                        list.add(bpm);

                    } else {
                        logger.debug("did not process Observation " + o.getId());
                    }
                }
            }
        }

        return list;
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

    @Override
    public Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException {
        if (model == null) return null;

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // if the observation came from the EHR, just package it up and send it along
        if (model.getSourceBPObservation() != null) {
            FhirUtil.appendResourceToBundle(bundle, model.getSourceBPObservation());
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());
            }

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);
            FhirConfigManager fcm = workspace.getFhirConfigManager();

            // When transforming a BP model to be used in Epic, we need to set a custom serialized note that
            // contains protocol information since we can't store that record separately in its own flowsheet record
            Observation bpObservation = buildHomeHealthBloodPressureObservation(model, patientIdRef, fcm);
            if (model.getFollowedProtocol()) {
                String s = model.getFollowedProtocol() ? fcm.getProtocolAnswerYes() : fcm.getProtocolAnswerNo();
                bpObservation.getNote().add(new Annotation().setText(PROTOCOL_NOTE_TAG + s));
            }
            FhirUtil.appendResourceToBundle(bundle, bpObservation);
        }

        return bundle;
    }

    @Override
    public List<PulseModel> transformIncomingPulseReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();

        List<PulseModel> list = new ArrayList<>();

        for (Encounter encounter : workspace.getEncounters()) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> pulseObservationList = new ArrayList<>();    // potentially many per encounter
                Observation protocolObservation = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if (FhirUtil.hasCoding(o.getCode(), fcm.getPulseCoding())) {
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

                    // Epic hack to set protocol information from custom-serialized note in the Observation resource
                    // if no protocol resource is found
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
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), fcm.getPulseCoding())) {
                        logger.debug("pulseObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        PulseModel pm = new PulseModel(o, fcm);

                        // Epic hack to set protocol information from custom-serialized note in the Observation resource
                        // if no protocol resource is found
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

        if (model.getSourcePulseObservation() != null) {
            FhirUtil.appendResourceToBundle(bundle, model.getSourcePulseObservation());
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());
            }

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);
            FhirConfigManager fcm = workspace.getFhirConfigManager();

            // When transforming a Pulse model to be used in Epic, we need to set a custom serialized note that
            // contains protocol information since we can't store that record separately in its own flowsheet record
            Observation pulseObservation = buildPulseObservation(model, patientIdRef, fcm);
            if (model.getFollowedProtocol() != null) {
                String s = model.getFollowedProtocol() ? fcm.getProtocolAnswerYes() : fcm.getProtocolAnswerNo();
                pulseObservation.getNote().add(new Annotation().setText(PROTOCOL_NOTE_TAG + s));
            }
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
}
