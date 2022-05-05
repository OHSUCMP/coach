package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.model.PulseModel;
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
import java.util.function.Function;

@Service
public class BloodPressureService extends AbstractVitalsService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    public List<BloodPressureModel> buildBloodPressureList(String sessionId) throws DataException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getObservations(sessionId, fcm.getBpSystem() + "|" + fcm.getBpCode(), null));
        compositeBundle.consume(workspaceService.get(sessionId).getProtocolObservations());
        Bundle observationBundle = compositeBundle.getBundle();

        List<BloodPressureModel> list = new ArrayList<>();
        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(observationBundle);

        for (Encounter encounter : workspaceService.get(sessionId).getEncounters()) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> bpObservationList = new ArrayList<>();    // potentially many per encounter
                Observation protocolObservation = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if (o.hasCode() && o.getCode().hasCoding(fcm.getBpSystem(), fcm.getBpCode())) {
                        logger.debug("bpObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        bpObservationList.add(o);
                        iter.remove();

                    } else if (protocolObservation == null && o.getCode().hasCoding(fcm.getProtocolSystem(), fcm.getProtocolCode())) {
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

        // if there are any BP observations that didn't get processed, we want to know about it
        if (logger.isDebugEnabled()) {
            for (Map.Entry<String, List<Observation>> entry : encounterObservationsMap.entrySet()) {
                if (entry.getValue() != null) {
                    for (Observation o : entry.getValue()) {
                        logger.debug("did not process Observation " + o.getId());
                    }
                }
            }
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
        if (storeRemotely) {
            Bundle responseBundle = writeRemote(sessionId, bpm);

            // read each response resource, and append to the BP cache if created
            // also, create a fresh BloodPressureModel resource constructed from the actual resources

            Encounter encounter = null;
            Observation bpObservation = null;
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

//                                    } else if (o.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode())) {
//                                        pulseObservation = o;

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
                BloodPressureModel bpm2 = new BloodPressureModel(encounter, bpObservation, protocolObservation, fcm);
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

        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
        for (HomeBloodPressureReading item : hbprList) {
            list.add(new BloodPressureModel(item, fcm));
        }

        return list;
    }
}
