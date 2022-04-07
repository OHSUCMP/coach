package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MethodNotImplementedException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BloodPressureService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private FHIRService fhirService;

    @Value("${fhir.observation.bp-write}")
    private Boolean storeRemotely;

    public List<BloodPressureModel> buildBloodPressureList(String sessionId) throws DataException {
        Map<String, List<Observation>> encounterObservationsMap = new HashMap<>();

        Bundle bpObservations = ehrService.getObservations(sessionId, fcm.getBpSystem() + "|" + fcm.getBpCode(), fcm.getBpLimit());
        Set<String> bpObservationIDs = null;
        if (logger.isDebugEnabled()) {
            bpObservationIDs = new HashSet<>();
            if (bpObservations.hasEntry()) {
                for (Bundle.BundleEntryComponent entry : bpObservations.getEntry()) {
                    if (entry.hasResource() && entry.getResource() instanceof Observation) {
                        bpObservationIDs.add(entry.getResource().getId());
                    }
                }
            }
        }

        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(bpObservations);
        compositeBundle.consume(ehrService.getObservations(sessionId, fcm.getPulseSystem() + "|" + fcm.getPulseCode(), null));
        compositeBundle.consume(ehrService.getObservations(sessionId, fcm.getProtocolSystem() + "|" + fcm.getProtocolCode(), null));
        Bundle observationBundle = compositeBundle.getBundle();

        if (observationBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : observationBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();
                    for (String key : buildKeys(observation.getEncounter())) {
                        if ( ! encounterObservationsMap.containsKey(key) ) {
                            encounterObservationsMap.put(key, new ArrayList<>());
                        }
                        encounterObservationsMap.get(key).add(observation);
                    }
                }
            }
        }

        List<BloodPressureModel> list = new ArrayList<>();

        for (Encounter encounter : workspaceService.get(sessionId).getEncounters()) {
            logger.debug("processing Encounter: " + encounter.getId());

            // don't we always want to get supplemental pulse and protocol observations if they exist?
            // I think so ... ?
            boolean doSupplemental = true; // storeRemotely && BloodPressureModel.isHomeHealthEncounter(encounter);

            Observation bpObservation = null;
            Observation pulseObservation = null;
            Observation protocolObservation = null;

            for (String key : buildKeys(encounter.getId(), encounter.getIdentifier())) {
                if (encounterObservationsMap.containsKey(key)) {
                    logger.debug("found encounterObservationMap key: " + key);

                    for (Observation o : encounterObservationsMap.get(key)) {
                        if (bpObservation == null && o.hasCode() && o.getCode().hasCoding(fcm.getBpSystem(), fcm.getBpCode())) {
                            logger.debug("bpObservation = " + o.getId());
                            bpObservation = o;

                        } else if (doSupplemental && pulseObservation == null && o.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode())) {
                            logger.debug("pulseObservation = " + o.getId());
                            pulseObservation = o;

                        } else if (doSupplemental && protocolObservation == null && o.getCode().hasCoding(fcm.getProtocolSystem(), fcm.getProtocolCode())) {
                            logger.debug("protocolObservation = " + o.getId());
                            protocolObservation = o;
                        }
                    }
                }
            }

            if (bpObservation != null) {
                if (bpObservationIDs != null) {
                    bpObservationIDs.remove(bpObservation.getId());
                }

                list.add(new BloodPressureModel(
                        encounter, bpObservation, pulseObservation, protocolObservation, fcm)
                );
            }
        }

        // if there are any BP observations that didn't get processed, we want to know about it
        if (logger.isDebugEnabled() && bpObservationIDs != null && bpObservationIDs.size() > 0) {
            logger.debug("could not process the following BP observations:");
            logger.debug(String.join("\n", bpObservationIDs));
        }

        return list;
    }

    public List<BloodPressureModel> getHomeBloodPressureReadings(String sessionId) {
        List<BloodPressureModel> list = new ArrayList<>();
        for (BloodPressureModel entry : getBloodPressureReadings(sessionId)) {
            if (entry.getSource() == BloodPressureModel.Source.HOME) {
                list.add(entry);
            }
        }
        return list;
    }

    public List<BloodPressureModel> getBloodPressureReadings(String sessionId) {
        List<BloodPressureModel> list = new ArrayList<>();
        list.addAll(workspaceService.get(sessionId).getBloodPressures());

        if ( ! storeRemotely ) {
            list.addAll(buildLocalBloodPressureReadings(sessionId));
        }

        Collections.sort(list, (o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public BloodPressureModel create(String sessionId, BloodPressureModel bpm) {
        UserWorkspace workspace = workspaceService.get(sessionId);

        // create home BP reading

        if (storeRemotely) {
            String patientId = workspaceService.get(sessionId).getPatient().getSourcePatient().getId();
            Bundle bundle = bpm.toBundle(patientId, fcm);

            // modify bundle to be appropriate for submission as a CREATE TRANSACTION

            bundle.setType(Bundle.BundleType.TRANSACTION);

            // prepare bundle for POSTing resources (create)
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                entry.setFullUrl(BloodPressureModel.URN_UUID + entry.getResource().getId())
                        .setRequest(new Bundle.BundleEntryRequestComponent()
                                .setMethod(Bundle.HTTPVerb.POST)
                                .setUrl(entry.getResource().fhirType()));
            }

            // write BP reading to the FHIR server

            FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
            Bundle responseBundle = fhirService.transact(fcc, bundle);

            // read each response resource, and append to the BP cache if created
            // also, create a fresh BloodPressureModel resource constructed from the actual resources

            Encounter encounter = null;
            Observation bpObservation = null;
            Observation pulseObservation = null;
            Observation protocolObservation = null;

            for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
                if (entry.hasResponse()) {
                    Bundle.BundleEntryResponseComponent response = entry.getResponse();
                    if (response.hasStatus() && response.getStatus().equals("201 Created")) {
                        logger.debug("successfully created " + response.getLocation());
                        if (entry.hasResource()) {
                            Resource r = entry.getResource();

                            if (r instanceof Encounter) {
                                encounter = (Encounter) r;

                            } else if (r instanceof Observation) {
                                Observation o = (Observation) r;
                                if (o.hasCode()) {
                                    if (o.getCode().hasCoding(fcm.getBpSystem(), fcm.getBpCode())) {
                                        bpObservation = o;

                                    } else if (o.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode())) {
                                        pulseObservation = o;

                                    } else if (o.getCode().hasCoding(fcm.getProtocolSystem(), fcm.getProtocolCode())) {
                                        protocolObservation = o;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            try {
                BloodPressureModel bpm2 = new BloodPressureModel(encounter, bpObservation, pulseObservation,
                        protocolObservation, fcm);
                workspaceService.get(sessionId).getBloodPressures().add(bpm2);
                return bpm2;

            } catch (DataException de) {
                logger.error("caught " + de.getClass().getName() +
                        " attempting to construct BloodPressureModel from remote create response - " +
                        de.getMessage(), de);
            }

        } else {
            try {
                HomeBloodPressureReading hbpr = new HomeBloodPressureReading(bpm);
                HomeBloodPressureReading response = hbprService.create(sessionId, hbpr);
                return new BloodPressureModel(response, fcm);

            } catch (DataException de) {
                logger.error("caught " + de.getClass().getName() + " attempting to create BloodPressureModel " + bpm);
            }
        }

        return null;
    }

    public Boolean delete(String sessionId, String id) {
        // delete home BP reading (do not allow deleting office BP readings!)

        try {
            if (storeRemotely) {
                throw new MethodNotImplementedException("remote delete is not implemented");

            } else {
                Long longId = Long.parseLong(id);
                hbprService.delete(sessionId, longId);
            }

            return true;

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to delete resource with id=" + id +
                    " for session " + sessionId, e);
            return false;
        }
    }

///////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private List<BloodPressureModel> buildLocalBloodPressureReadings(String sessionId) {
        List<BloodPressureModel> list = new ArrayList<>();

        // now incorporate Home Blood Pressure Readings that the user entered themself into the system
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
        for (HomeBloodPressureReading item : hbprList) {
            list.add(new BloodPressureModel(item, fcm));
        }

        return list;
    }
}
