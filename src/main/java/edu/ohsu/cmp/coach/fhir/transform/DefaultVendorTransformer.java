package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.fhir.IFHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.service.FHIRService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class DefaultVendorTransformer extends BaseVendorTransformer implements VendorTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultVendorTransformer(UserWorkspace workspace) {
        super(workspace);
    }

    @Override
    public Bundle writeRemote(String sessionId, FHIRService fhirService, Bundle bundle) throws DataException, IOException, ConfigurationException, ScopeException {
        Bundle bundleToTransact = new Bundle();
        bundleToTransact.setType(Bundle.BundleType.TRANSACTION);

        // prepare bundle for POSTing resources
        // ONLY permit NEW resources (i.e. those with UUID identifiers) to pass
        // based on the reasonable assumption that we NEVER want to update existing resources
        for (Bundle.BundleEntryComponent sourceEntry : bundle.getEntry()) {
            if (FhirUtil.isUUID(sourceEntry.getResource().getId())) {
                Bundle.BundleEntryComponent entry = sourceEntry.copy();
                entry.setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.POST)
                        .setUrl(sourceEntry.getResource().fhirType()));
                bundleToTransact.getEntry().add(entry);
            }
        }

        // write resources to the FHIR server

        IFHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Bundle responseBundle = fhirService.transact(fcc, bundleToTransact, true);

        // remove any responses that didn't result in a 201 Created response
        Iterator<Bundle.BundleEntryComponent> iter = responseBundle.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            if (entry.hasResponse()) {
                Bundle.BundleEntryResponseComponent response = entry.getResponse();
                if (response.hasStatus() && response.getStatus().equals("201 Created")) {
                    logger.debug("successfully created " + response.getLocation());
                } else {
                    logger.warn("got status = " + response.getStatus() + " attempting to write " + entry + " - removing");
                    iter.remove();
                }
            }
        }

        return responseBundle;
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation bpObservation, Observation protocolObservation) throws DataException {
        return new BloodPressureModel(encounter, bpObservation, protocolObservation, workspace.getFhirConfigManager());
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation systolicObservation, Observation diastolicObservation, Observation protocolObservation) throws DataException {
        return new BloodPressureModel(encounter, systolicObservation, diastolicObservation, protocolObservation, workspace.getFhirConfigManager());
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Observation o) throws DataException {
        return new BloodPressureModel(o, workspace.getFhirConfigManager());
    }

    @Override
    protected BloodPressureModel buildBloodPressureModel(Observation systolicObservation, Observation diastolicObservation) throws DataException {
        return new BloodPressureModel(systolicObservation, diastolicObservation, workspace.getFhirConfigManager());
    }

    /**
     * Transforms outgoing Blood Pressure Observations in an idealized manner.
     * @param model a populated BloodPressureModel object
     * @return a Bundle containing FHIR Resources that represents the information in the passed BloodPressureModel
     * object.
     * @throws DataException
     */
    @Override
    public Bundle transformOutgoingBloodPressureReading(BloodPressureModel model) throws DataException {
        if (model == null) return null;

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // if the observation came from the EHR, just package it up and send it along
        if (model.getSourceBPObservation() != null) {
            FhirUtil.appendResourceToBundle(bundle, model.getSourceBPObservation());
            if (model.getSourceEncounter() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceEncounter());
            }
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());
            }

        } else if (model.getSourceSystolicObservation() != null && model.getSourceDiastolicObservation() != null) {
            FhirConfigManager fcm = workspace.getFhirConfigManager();

            // enforce presence of LOINC codes, as this block of logic really only comes from Epic source
            // where LOINC codes are prohibited

            Observation systolicObservation = model.getSourceSystolicObservation().copy();
            appendIfMissing(systolicObservation, fcm.getBpSystolicCoding());
            FhirUtil.appendResourceToBundle(bundle,systolicObservation);

            Observation diastolicObservation = model.getSourceDiastolicObservation().copy();
            appendIfMissing(diastolicObservation, fcm.getBpDiastolicCoding());
            FhirUtil.appendResourceToBundle(bundle, diastolicObservation);

            if (model.getSourceEncounter() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceEncounter());
            }
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());
            }

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);
            FhirConfigManager fcm = workspace.getFhirConfigManager();

            // the BP observation didn't come from the EHR, so it necessarily came from COACH, and is
            // thereby necessarily a HOME based observation.

            // in the default scenario, BP observations should have an associated Encounter that ties associated
            // resources together

            Encounter enc = model.getSourceEncounter() != null ?
                    model.getSourceEncounter() :
                    buildNewHomeHealthEncounter(model.getReadingDate(), fcm, patientIdRef);
            FhirUtil.appendResourceToBundle(bundle, enc);

            Observation bpObservation = buildHomeHealthBloodPressureObservation(model, enc, patientIdRef, fcm);
            FhirUtil.appendResourceToBundle(bundle, bpObservation);

            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());

            } else if (model.getFollowedProtocol() != null) {
                Observation protocolObservation = buildProtocolObservation(model, enc, patientIdRef, fcm);
                FhirUtil.appendResourceToBundle(bundle, protocolObservation);
            }
        }

        return bundle;
    }


    /**
     * Transforms incoming Pulse Observations in an idealized manner.
     * @param bundle a Bundle of FHIR Resources including Pulse and special-circumstance Observations with
     *               optional Encounters.
     * @return a List of one or more populated PulseModel objects.
     * @throws DataException
     */
    @Override
    public List<PulseModel> transformIncomingPulseReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();

        List<PulseModel> list = new ArrayList<>();

        // todo : outer loop should iterate over encounterObservationsMap, ignoring items where the key is null

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
                    list.add(new PulseModel(encounter, pulseObservation, protocolObservation, fcm));
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
                        list.add(new PulseModel(o, fcm));

                    } else {
                        logger.debug("did not process Observation " + o.getId());
                    }
                }
            }
        }

        return list;
    }

    /**
     * Transforms outgoing Pulse Observations in an idealized manner.
     * @param model a populated PulseModel object
     * @return a Bundle containing FHIR Resources that represents the information in the passed PulseModel
     * object.
     * @throws DataException
     */
    @Override
    public Bundle transformOutgoingPulseReading(PulseModel model) throws DataException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (model.getSourcePulseObservation() != null) {
            FhirUtil.appendResourceToBundle(bundle, model.getSourcePulseObservation());
            if (model.getSourceEncounter() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceEncounter());
            }
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());
            }

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);
            FhirConfigManager fcm = workspace.getFhirConfigManager();

            // in the default scenario, Pulse observations should have an associated Encounter to tie it to
            // associated resources

            Encounter enc = model.getSourceEncounter() != null ?
                    model.getSourceEncounter() :
                    buildNewHomeHealthEncounter(model.getReadingDate(), fcm, patientIdRef);
            FhirUtil.appendResourceToBundle(bundle, enc);

            Observation pulseObservation = buildPulseObservation(model, enc, patientIdRef, fcm);
            FhirUtil.appendResourceToBundle(bundle, pulseObservation);

            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());

            } else if (model.getFollowedProtocol() != null) {
                Observation protocolObservation = buildProtocolObservation(model, enc, patientIdRef, fcm);
                FhirUtil.appendResourceToBundle(bundle, protocolObservation);
            }
        }

        return bundle;
    }

    /**
     * Transforms incoming Goal Observations in an idealized manner.
     * @param bundle a Bundle of FHIR Resources including Goal Observations.
     * @return a List of one or more populated GoalModel objects.
     * @throws DataException
     */
    @Override
    public List<GoalModel> transformIncomingGoals(Bundle bundle) throws DataException {
        if (bundle == null) return null;
        FhirConfigManager fcm = workspace.getFhirConfigManager();
        List<GoalModel> list = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Goal) {
                Goal g = (Goal) entry.getResource();
                list.add(new GoalModel(g, fcm));
            }
        }

        return list;
    }

    /**
     * Transforms outgoing Goal Observations in an idealized manner.
     * @param model a populated GoalModel object
     * @return a Bundle containing FHIR Resources that represents the information in the passed GoalModel
     * object.
     * @throws DataException
     */
    @Override
    public Bundle transformOutgoingGoal(GoalModel model) throws DataException {
        String patientId = workspace.getPatient().getSourcePatient().getId();
        String patientIdRef = FhirUtil.toRelativeReference(patientId);
        FhirConfigManager fcm = workspace.getFhirConfigManager();

        Goal goal = model.getSourceGoal() != null ?
                model.getSourceGoal() :
                buildGoal(model, patientIdRef, fcm);

        return FhirUtil.bundleResources(goal);
    }

////////////////////////////////////////////////////////////////////////
// private methods
//

    private Encounter buildNewHomeHealthEncounter(Date readingDate, FhirConfigManager fcm, String patientId) {
        Encounter e = new Encounter();

        e.setId(genTemporaryId());

        e.setStatus(Encounter.EncounterStatus.FINISHED);

        e.getClass_().setSystem(fcm.getEncounterClassSystem())
                .setCode(fcm.getEncounterClassHHCode())
                .setDisplay(fcm.getEncounterClassHHDisplay());

        e.setSubject(new Reference().setReference(patientId));

        Calendar start = Calendar.getInstance();
        start.setTime(readingDate);
        start.add(Calendar.MINUTE, -1);

        Calendar end = Calendar.getInstance();
        end.setTime(readingDate);
        end.add(Calendar.MINUTE, 1);

        e.getPeriod().setStart(start.getTime()).setEnd(end.getTime());

        return e;
    }

    private Observation buildHomeHealthBloodPressureObservation(BloodPressureModel model, Encounter encounter, String patientIdRef, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientIdRef));

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
            o.setValue(new Quantity());
            setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

        } else if (model.getSystolic() == null && model.getDiastolic() != null) {     // diastolic only
            o.getCode().addCoding(fcm.getBpDiastolicCoding());
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

    private void appendIfMissing(Observation observation, Coding coding) {
        if (observation == null) return;

        if (observation.hasCode() && observation.getCode().hasCoding(coding.getSystem(), coding.getCode())) {
            return;
        }

        observation.getCode().addCoding(coding);
    }

    private void setBPValue(Quantity q, QuantityModel qm, FhirConfigManager fcm) {
        q.setCode(fcm.getBpValueCode())
                .setSystem(fcm.getBpValueSystem())
                .setUnit(fcm.getBpValueUnit())
                .setValue(qm.getValue().intValue());
    }

    private Observation buildPulseObservation(PulseModel model, Encounter encounter, String patientId, FhirConfigManager fcm) throws DataException {
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
}
