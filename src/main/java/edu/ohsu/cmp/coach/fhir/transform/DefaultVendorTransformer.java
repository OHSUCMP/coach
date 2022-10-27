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

public class DefaultVendorTransformer extends BaseVendorTransformer implements VendorTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultVendorTransformer(UserWorkspace workspace) {
        super(workspace);
    }

    /**
     * Transforms incoming Blood Pressure Observations in an idealized manner.
      * @param bundle a Bundle of FHIR Resources including BP and special-circumstance Observations with
     *               optional Encounters.
     * @return a List of one or more populated BloodPressureModel objects.
     * @throws DataException
     */
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
                    list.add(new BloodPressureModel(encounter, bpObservation, protocolObservation, fcm));
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
                        list.add(new BloodPressureModel(o, fcm));

                    } else {
                        logger.debug("did not process Observation " + o.getId());
                    }
                }
            }
        }

        return list;
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

        } else {
            String patientId = workspace.getPatient().getSourcePatient().getId(); //workspace.getFhirCredentialsWithClient().getCredentials().getPatientId();
            String patientIdRef = FhirUtil.toRelativeReference(patientId);
            FhirConfigManager fcm = workspace.getFhirConfigManager();

            // the BP observation didn't come from the EHR, so it necessarily came from COACH, and is
            // thereby necessarily a HOME based observation.

            // in the default scenario, BP observations should have an associated Encounter that ties associated
            // resources together

            Encounter enc = buildNewHomeHealthEncounter(model.getReadingDate(), fcm, patientIdRef);
            FhirUtil.appendResourceToBundle(bundle, enc);

            Observation bpObservation = buildHomeHealthBloodPressureObservation(model, enc, patientIdRef, fcm);
            FhirUtil.appendResourceToBundle(bundle, bpObservation);

            if (model.getFollowedProtocol() != null) {
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
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), fcm.getPulseCoding())) {
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

            Encounter enc = buildNewHomeHealthEncounter(model.getReadingDate(), fcm, patientIdRef);
            FhirUtil.appendResourceToBundle(bundle, enc);

            Observation pulseObservation = buildPulseObservation(model, enc, patientIdRef, fcm);
            FhirUtil.appendResourceToBundle(bundle, pulseObservation);

            if (model.getFollowedProtocol() != null) {
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
}
