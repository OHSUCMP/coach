package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.fhir.FhirStrategy;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.model.QuantityModel;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.service.FHIRService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DefaultVendorTransformer extends BaseVendorTransformer implements VendorTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultVendorTransformer(UserWorkspace workspace) {
        super(workspace);
    }

    @Override
    public Bundle writeRemote(String sessionId, FhirStrategy strategy, FHIRService fhirService, Bundle bundle) throws Exception {
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

        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Bundle responseBundle = fhirService.transact(fcc, strategy, bundleToTransact, true);

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

        FhirConfigManager fcm = workspace.getFhirConfigManager();

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (model.getSourceBPObservation() != null) {
            // ensure BP panel Observations are tagged with the common panel coding
            Observation sourceBPObservation = model.getSourceBPObservation().copy();
            appendIfMissing(sourceBPObservation, fcm.getBpPanelCommonCoding());

            // CQF Ruler needs to see the Home Setting extension to positively identify the reading as Home
            if (model.isHomeReading() && ! FhirUtil.hasHomeSettingExtension(sourceBPObservation)) {
                FhirUtil.addHomeSettingExtension(sourceBPObservation);
            }

            FhirUtil.appendResourceToBundle(bundle, sourceBPObservation);

            if (model.getSourceEncounter() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceEncounter().copy());
            }
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation().copy());
            }

        } else if (model.getSourceSystolicObservation() != null && model.getSourceDiastolicObservation() != null) {
            // ensure BP systolic and diastolic Observations are tagged with the common systolic and diastolic codings
            Observation systolicObservation = model.getSourceSystolicObservation().copy();
            appendIfMissing(systolicObservation, fcm.getBpSystolicCommonCoding());

            // CQF Ruler needs to see the Home Setting extension to positively identify the reading as Home
            if (model.isHomeReading() && ! FhirUtil.hasHomeSettingExtension(systolicObservation)) {
                FhirUtil.addHomeSettingExtension(systolicObservation);
            }

            FhirUtil.appendResourceToBundle(bundle, systolicObservation);

            Observation diastolicObservation = model.getSourceDiastolicObservation().copy();
            appendIfMissing(diastolicObservation, fcm.getBpDiastolicCommonCoding());

            // CQF Ruler needs to see the Home Setting extension to positively identify the reading as Home
            if (model.isHomeReading() && ! FhirUtil.hasHomeSettingExtension(diastolicObservation)) {
                FhirUtil.addHomeSettingExtension(diastolicObservation);
            }

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
                    try {
                        list.add(new PulseModel(encounter, pulseObservation, protocolObservation, fcm));
                    } catch (DataException e) {
                        logger.warn("caught " + e.getClass().getName() +
                                " building Pulse from Observation with id=" + pulseObservation.getId() + " - " +
                                e.getMessage() + " - skipping -");
                    }
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
                    try {
                        if (o.hasCode()) {
                            if (FhirUtil.hasCoding(o.getCode(), fcm.getPulseCodings())) {
                                logger.debug("pulseObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                                try {
                                    list.add(new PulseModel(o, fcm));
                                } catch (DataException e) {
                                    logger.warn("caught " + e.getClass().getName() +
                                            " building Pulse from Observation with id=" + o.getId() + " - " +
                                            e.getMessage() + " - skipping -");
                                }

                            } else {
                                logger.debug("did not process Observation " + o.getId() + " - invalid coding");
                            }

                        } else {
                            logger.debug("did not process Observation " + o.getId() + " - no coding");
                        }

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getName() + " processing Observation with id=" + o.getId() + " - " + e.getMessage(), e);
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
        if (model == null) return null;

        FhirConfigManager fcm = workspace.getFhirConfigManager();

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (model.getSourcePulseObservation() != null) {
            // ensure pulse Observations are tagged with the common coding
            Observation sourcePulseObservation = model.getSourcePulseObservation().copy();
            appendIfMissing(sourcePulseObservation, fcm.getPulseCommonCoding());
            FhirUtil.appendResourceToBundle(bundle, sourcePulseObservation);

            if (model.getSourceEncounter() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceEncounter());
            }
            if (model.getSourceProtocolObservation() != null) {
                FhirUtil.appendResourceToBundle(bundle, model.getSourceProtocolObservation());
            }

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

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
            if (entry.hasResource() && entry.getResource() instanceof Goal) {
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

        e.setClass_(fcm.getEncounterClassHomeCoding());
//        e.getClass_().setSystem(fcm.getEncounterClassSystem())
//                .setCode(fcm.getEncounterClassHHCode())
//                .setDisplay(fcm.getEncounterClassHHDisplay());

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
            o.getCode().addCoding(fcm.getBpSystolicCommonCoding());
            o.setValue(new Quantity());
            setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

        } else if (model.getSystolic() == null && model.getDiastolic() != null) {     // diastolic only
            o.getCode().addCoding(fcm.getBpDiastolicCommonCoding());
            o.setValue(new Quantity());
            setBPValue(o.getValueQuantity(), model.getDiastolic(), fcm);

        } else if (model.getSystolic() != null && model.getDiastolic() != null) {     // both systolic and diastolic
            o.getCode().addCoding(fcm.getBpPanelCommonCoding());
            for (Coding c : fcm.getBpHomeCodings()) {
                o.getCode().addCoding(c);
            }

            Observation.ObservationComponentComponent occSystolic = new Observation.ObservationComponentComponent();
            occSystolic.getCode().addCoding(fcm.getBpSystolicCommonCoding());
            occSystolic.setValue(new Quantity());
            setBPValue(occSystolic.getValueQuantity(), model.getSystolic(), fcm);
            o.getComponent().add(occSystolic);

            Observation.ObservationComponentComponent occDiastolic = new Observation.ObservationComponentComponent();
            occDiastolic.getCode().addCoding(fcm.getBpDiastolicCommonCoding());
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

        if (observation.hasCode() && FhirUtil.hasCoding(observation.getCode(), coding)) { //observation.getCode().hasCoding(coding.getSystem(), coding.getCode())) {
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

        o.getCode().addCoding(fcm.getPulseCommonCoding());

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
