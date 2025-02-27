package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.fhir.FhirStrategy;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class SpecialVendorTransformer extends BaseVendorTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String PROTOCOL_NOTE_TAG = "COACH_PROTOCOL::";
    protected static final String URN_OID_PREFIX = "urn:oid:";

    protected final DefaultVendorTransformer defaultTransformer;

    public SpecialVendorTransformer(UserWorkspace workspace) {
        super(workspace);
        defaultTransformer = new DefaultVendorTransformer(workspace);
    }

    @Override
    public Bundle writeRemote(String sessionId, FhirStrategy strategy, FHIRService fhirService, Bundle bundle) throws DataException, IOException, ConfigurationException, ScopeException {

        // for "special" vendors, we want to post resources one at a time

        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();

        Bundle responseBundle = new Bundle();
        responseBundle.setType(Bundle.BundleType.COLLECTION);

        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource r = entry.getResource();
                if (r instanceof IDomainResource && FhirUtil.isUUID(r.getId())) {
                    try {
                        FhirUtil.appendResourceToBundle(responseBundle,
                                fhirService.transact(fcc, strategy, (DomainResource) r)
                        );

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getSimpleName() + " attempting to transact " +
                                r.getClass().getSimpleName() + " - " + e.getMessage() + " - skipping -", e);
                        if (logger.isDebugEnabled()) {
                            logger.debug("EXCEPTION DETAILS: using strategy=" + strategy + ", credentials=" + fcc.getCredentials() +
                                    ", resourceType=" + r.getClass().getSimpleName() + ", json=" + FhirUtil.toJson(r));
                        }
                        if      (e instanceof IOException)              throw (IOException) e;
                        else if (e instanceof ConfigurationException)   throw (ConfigurationException) e;
                        else if (e instanceof DataException)            throw (DataException) e;
                        else if (e instanceof RuntimeException)         throw (RuntimeException) e;
                    }
                }
            }
        }

        return responseBundle;
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation bpObservation, Observation protocolObservation) throws DataException {
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        BloodPressureModel bpm = new BloodPressureModel(encounter, bpObservation, protocolObservation, fcm);

        // "special" vendors generally restrict the types of resources that can be written back, usually just one Observation
        // for systolic, and one for diastolic.  auxiliary resources are not generally supported.  as such, we implement this hack
        // to set protocol information from custom-serialized note in the Observation resource if no protocol resource is found

        if (protocolObservation == null) {
            Boolean followedProtocol = getFollowedProtocolFromNote(bpObservation, fcm);
            if (followedProtocol != null) {
                bpm.setFollowedProtocol(followedProtocol);
            }
        }

        return bpm;
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation systolicObservation, Observation diastolicObservation, Observation protocolObservation) throws DataException {
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        BloodPressureModel bpm = new BloodPressureModel(encounter, systolicObservation, diastolicObservation, protocolObservation, fcm);

        // "special" vendors generally restrict the types of resources that can be written back, usually just one Observation
        // for systolic, and one for diastolic.  auxiliary resources are not generally supported.  as such, we implement this hack
        // to set protocol information from custom-serialized note in the Observation resource if no protocol resource is found

        if (protocolObservation == null) {
            Boolean followedProtocol = getFollowedProtocolFromNote(systolicObservation, fcm);
            if (followedProtocol == null) {
                followedProtocol = getFollowedProtocolFromNote(diastolicObservation, fcm);
            }
            if (followedProtocol != null) {
                bpm.setFollowedProtocol(followedProtocol);
            }
        }

        return bpm;
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Observation o) throws DataException {
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        BloodPressureModel bpm = new BloodPressureModel(o, fcm);

        // "special" vendors generally restrict the types of resources that can be written back, usually just one Observation
        // for systolic, and one for diastolic.  auxiliary resources are not generally supported.  as such, we implement this hack
        // to set protocol information from custom-serialized note in the Observation resource if no protocol resource is found

        Boolean followedProtocol = getFollowedProtocolFromNote(o, fcm);
        if (followedProtocol != null) {
            bpm.setFollowedProtocol(followedProtocol);
        }

        return bpm;
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Observation systolicObservation, Observation diastolicObservation) throws DataException {
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        BloodPressureModel bpm = new BloodPressureModel(systolicObservation, diastolicObservation, fcm);

        // "special" vendors generally restrict the types of resources that can be written back, usually just one Observation
        // for systolic, and one for diastolic.  auxiliary resources are not generally supported.  as such, we implement this hack
        // to set protocol information from custom-serialized note in the Observation resource if no protocol resource is found

        Boolean followedProtocol = getFollowedProtocolFromNote(systolicObservation, fcm);
        if (followedProtocol == null) {
            followedProtocol = getFollowedProtocolFromNote(diastolicObservation, fcm);
        }
        if (followedProtocol != null) {
            bpm.setFollowedProtocol(followedProtocol);
        }

        return bpm;
    }

    @Override
    public Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException {
        if (model == null) return null;

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        FhirConfigManager fcm = workspace.getFhirConfigManager();

        // if a source Observation exists as a panel, split it into two, one for systolic and another for diastolic
        if (model.getSourceBPObservation() != null) {
            // note : do not include Encounter for "special" vendors
            //        also, combine protocol info into the BP Observation if it exists
            //        also, the BP Observation needs to be split into separate systolic and diastolic resources
            //        also, link the resultant Observations together using a UUID in the note field, so we can reliably recombine them later
            //        resultant Observations will have the same effective time, which can also be used to rejoin them later

            String uuid = genTemporaryId();

            Observation systolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.SYSTOLIC, model.getSourceBPObservation(), fcm);
            appendProtocolAnswerToObservationIfNeeded(systolicObservation, model, fcm);
            appendUUIDNoteToObservationIfNeeded(systolicObservation, uuid);
            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.DIASTOLIC, model.getSourceBPObservation(), fcm);
            appendProtocolAnswerToObservationIfNeeded(diastolicObservation, model, fcm);
            appendUUIDNoteToObservationIfNeeded(diastolicObservation, uuid);
            FhirUtil.appendResourceToBundle(bundle, diastolicObservation);

        } else if (model.getSourceSystolicObservation() != null && model.getSourceDiastolicObservation() != null) {
            Observation systolicObservation = model.getSourceSystolicObservation().copy();
            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = model.getSourceDiastolicObservation().copy();
            FhirUtil.appendResourceToBundle(bundle, diastolicObservation);

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

            // when transforming a BP model to be sent to "special" vendors, we need to set a custom serialized note that
            // contains protocol information since the target server generally will not permit resources to be posted that
            // aren't directly related to vitals

            // for "special" vendors, we do not include Encounters with BP Observations, but instead use a UUID in a note
            // field to explicitly link resources together.  we also set the same effective timestamp on both, which can
            // also be used to associated resources together

            // "special" vendors generally restrict the types of resources that can be written back, and auxiliary
            // resources are not generally supported.  as such, we implement this hack to set protocol information from
            // custom-serialized note in the Observation resource if no protocol resource is found

            String uuid = genTemporaryId();

            Observation systolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.SYSTOLIC, model, patientIdRef, fcm);
            appendProtocolAnswerToObservationIfNeeded(systolicObservation, model, fcm);
            appendUUIDNoteToObservationIfNeeded(systolicObservation, uuid);
            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = buildHomeHealthBloodPressureObservation(ResourceType.DIASTOLIC, model, patientIdRef, fcm);
            appendProtocolAnswerToObservationIfNeeded(diastolicObservation, model, fcm);
            appendUUIDNoteToObservationIfNeeded(diastolicObservation, uuid);
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

                    // for "special" vendors, protocol information is represented in a custom-serialized note on the
                    // Observation resource if no Observation resource for the protocol exists

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

        // there may be pulse Observations in the system that aren't tied to any Encounters.  we still want to capture these
        // of course, we can't associate any other Observations with them (e.g. protocol), but whatever.  better than nothing

        for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
            if (entry.getValue() != null) {
                for (Observation o : entry.getValue()) {
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), fcm.getPulseCodings())) {
                        logger.debug("pulseObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        PulseModel pm = new PulseModel(o, fcm);

                        // for "special" vendors, protocol information is represented in a custom-serialized note on the
                        // Observation resource if no Observation resource for the protocol exists

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
            // note : do not include Encounter for "special" vendors
            //        also, combine protocol info into the pulse observation if it exists
            Observation pulseObservation = model.getSourcePulseObservation().copy();
            appendProtocolAnswerToObservationIfNeeded(pulseObservation, model, fcm);
            FhirUtil.appendResourceToBundle(bundle, pulseObservation);

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

            // "special" vendors generally restrict the types of resources that can be written back, and auxiliary
            // resources are not generally supported.  as such, we implement this hack to set protocol information from
            // custom-serialized note in the Observation resource if no protocol resource is found

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

    protected enum ResourceType {
        SYSTOLIC,
        DIASTOLIC
    }

    protected abstract Observation buildHomeHealthBloodPressureObservation(ResourceType type, Observation bpObservation, FhirConfigManager fcm) throws DataException;


    protected abstract Observation buildHomeHealthBloodPressureObservation(ResourceType type, BloodPressureModel model, String patientIdRef, FhirConfigManager fcm) throws DataException;

    private Boolean getFollowedProtocolFromNote(Observation bpObservation, FhirConfigManager fcm) {
        // Epic hack to handle when protocol-followed info is custom serialized into a note field
        if (bpObservation != null && bpObservation.hasNote()) {
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
        if ( ! hasNoteStartingWith(observation, PROTOCOL_NOTE_TAG) ) {
            if (model.getSourceProtocolObservation() != null) {
                Observation o = model.getSourceProtocolObservation();
                if (o.hasValueCodeableConcept()) {
                    CodeableConcept cc = o.getValueCodeableConcept();
                    if (cc.hasText()) {
                        appendNote(observation, PROTOCOL_NOTE_TAG + cc.getText());
                    }
                }

            } else if (model.getFollowedProtocol() != null) {
                String answerValue = model.getFollowedProtocol() ?
                        fcm.getProtocolAnswerYes() :
                        fcm.getProtocolAnswerNo();
                appendNote(observation, PROTOCOL_NOTE_TAG + answerValue);
            }
        }
    }

    private void appendUUIDNoteToObservationIfNeeded(Observation observation, String uuid) {
        if ( ! hasNoteStartingWith(observation, UUID_NOTE_TAG) ) {
            appendNote(observation, UUID_NOTE_TAG + uuid);
        }
    }

    private boolean hasNoteStartingWith(Observation observation, String s) {
        if (observation == null) return false;
        if (observation.hasNote()) {
            for (Annotation annotation : observation.getNote()) {
                if (annotation.hasText() && annotation.getText().startsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void appendNote(Observation observation, String note) {
        observation.getNote().add(new Annotation().setText(note));
    }

    protected Observation.ObservationComponentComponent getComponentHavingCoding(Observation observation, List<Coding> coding) throws DataException {
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

    protected void setBPValue(Quantity q, QuantityModel qm, FhirConfigManager fcm) {
        q.setCode(fcm.getBpValueCode())
                .setSystem(fcm.getBpValueSystem())
//                .setUnit(fcm.getBpValueUnit())        // Epic doesn't allow units to be specified
                .setValue(qm.getValue().intValue());
    }

    protected abstract Observation buildPulseObservation(PulseModel model, String patientId, FhirConfigManager fcm) throws DataException;

}