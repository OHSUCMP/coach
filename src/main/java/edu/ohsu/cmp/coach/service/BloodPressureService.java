package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.cache.UserCache;
import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MethodNotImplementedException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BloodPressureService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    // todo : complete this

    @Value("${fhir.observation.bp-write}")
    private Boolean storeRemotely;

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
        List<BloodPressureModel> list = buildRemoteBloodPressureReadings(sessionId);

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
        UserCache cache = SessionCache.getInstance().get(sessionId);

        // create home BP reading

        if (storeRemotely) {
            String patientId = cache.getResource(UserCache.CacheType.PATIENT, Patient.class).getId();
//            String patientId = cache.getPatient().getId();
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

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            Bundle responseBundle = fcc.transact(bundle);

            // todo : process response

            // read each response resource, and append to the OBSERVATIONS cache if created
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

                            logger.debug("resource " + r.getId() + " returned with response, appending to cache");
                            cache.addResourceToBundle(UserCache.CacheType.OBSERVATIONS, r);
                        }
                    }
                }
            }

            try {
                return new BloodPressureModel(encounter, bpObservation, pulseObservation, protocolObservation, fcm);

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

    private List<BloodPressureModel> buildRemoteBloodPressureReadings(String sessionId) {
        List<BloodPressureModel> list = new ArrayList<>();

        // note : ehrService.getBloodPressureObservations() generates a Bundle containing BP, Pulse, and Protocol
        //        Observations, if any exist.
        Bundle bundle = ehrService.getBloodPressureObservations(sessionId);

        for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
            if (entry.getResource() instanceof Observation) {
                Observation o = (Observation) entry.getResource();
                if (o.hasCode() && o.getCode().hasCoding(fcm.getBpSystem(), fcm.getBpCode())) {
                    // this Observation is a blood-pressure reading

                    try {
                        Encounter enc = FhirUtil.getResourceFromBundleByReference(bundle, Encounter.class, o.getEncounter());

                        Observation pulseObservation = null;
                        Observation protocolObservation = null;

                        if (storeRemotely && BloodPressureModel.isHomeHealthEncounter(enc)) {
                            for (Observation o2 : FhirUtil.getObservationsFromBundleByEncounterReference(bundle, enc.getId())) {
                                if (o2.hasCode()) {
                                    if (pulseObservation == null && o2.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode())) {
                                        pulseObservation = o2;

                                    } else if (protocolObservation == null && o2.getCode().hasCoding(fcm.getProtocolSystem(), fcm.getProtocolCode())) {
                                        protocolObservation = o2;
                                    }
                                }
                            }
                        }

                        list.add(new BloodPressureModel(enc, o, pulseObservation, protocolObservation, fcm));

                    } catch (DataException e) {
                        logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                    }
                }


            } else {
                Resource r = entry.getResource();
                logger.warn("ignoring " + r.getClass().getName() + " (id=" + r.getId() + ") while building Blood Pressure Observations");
            }
        }

        return list;
    }

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
