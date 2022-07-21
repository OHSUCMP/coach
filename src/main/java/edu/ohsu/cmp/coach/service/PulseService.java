package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.app.HomePulseReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.model.PulseModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PulseService extends AbstractVitalsService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomePulseReadingService hprService;

    public List<PulseModel> buildPulseList(String sessionId) throws DataException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getObservations(sessionId, fcm.getPulseSystem() + "|" + fcm.getPulseCode(), fcm.getPulseLookbackPeriod(),null));
        compositeBundle.consume(workspaceService.get(sessionId).getProtocolObservations());
        Bundle observationBundle = compositeBundle.getBundle();

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(observationBundle);

        List<PulseModel> list = new ArrayList<>();

        for (Encounter encounter : workspaceService.get(sessionId).getEncounters()) {
            logger.debug("processing Encounter: " + encounter.getId());

            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations != null) {
                logger.debug("building Observations for Encounter " + encounter.getId());

                List<Observation> pulseObservationList = new ArrayList<>();    // potentially many per encounter
                Observation protocolObservation = null;

                Iterator<Observation> iter = encounterObservations.iterator();
                while (iter.hasNext()) {
                    Observation o = iter.next();
                    if (o.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode())) {
                        logger.debug("pulseObservation = " + o.getId() + " (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                        pulseObservationList.add(o);
                        iter.remove();

                    } else if (protocolObservation == null && o.getCode().hasCoding(fcm.getProtocolSystem(), fcm.getProtocolCode())) {
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

    public List<PulseModel> getHomePulseReadings(String sessionId) {
        List<PulseModel> list = new ArrayList<>();
        for (PulseModel entry : getPulseReadings(sessionId)) {
            if (entry.getSource() == PulseModel.Source.HOME) {
                list.add(entry);
            }
        }
        return list;
    }

    public List<PulseModel> getPulseReadings(String sessionId) {
        List<PulseModel> list = new ArrayList<>();
        list.addAll(workspaceService.get(sessionId).getPulses());

        if ( ! storeRemotely ) {
            list.addAll(buildLocalPulseReadings(sessionId));
        }

        Collections.sort(list, (o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public PulseModel create(String sessionId, PulseModel pm) {
        if (storeRemotely) {
            Bundle responseBundle = writeRemote(sessionId, pm);

            // read each response resource, and append to the BP cache if created
            // also, create a fresh BloodPressureModel resource constructed from the actual resources

            Encounter encounter = null;
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
                                    if (o.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode())) {
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
                PulseModel pm2 = new PulseModel(encounter, pulseObservation, protocolObservation, fcm);
                workspaceService.get(sessionId).getPulses().add(pm2);
                return pm2;

            } catch (DataException de) {
                logger.error("caught " + de.getClass().getName() +
                        " attempting to construct PulseModel from remote create response - " +
                        de.getMessage(), de);
            }

        } else {
            try {
                HomePulseReading hpr = new HomePulseReading(pm);
                HomePulseReading response = hprService.create(sessionId, hpr);
                return new PulseModel(response, fcm);

            } catch (DataException de) {
                logger.error("caught " + de.getClass().getName() + " attempting to create BloodPressureModel " + pm);
            }
        }

        return null;
    }


///////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private List<PulseModel> buildLocalPulseReadings(String sessionId) {
        List<PulseModel> list = new ArrayList<>();

        List<HomePulseReading> hbprList = hprService.getHomePulseReadings(sessionId);
        for (HomePulseReading item : hbprList) {
            list.add(new PulseModel(item, fcm));
        }

        return list;
    }
}
