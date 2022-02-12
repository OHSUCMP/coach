package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
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

    public List<BloodPressureModel> getBloodPressureReadings(String sessionId) {
        List<BloodPressureModel> list = buildRemoteBloodPressureReadings(sessionId);

        if ( ! storeRemotely ) {
            list.addAll(buildLocalBloodPressureReadings(sessionId));
        }

        Collections.sort(list, (o1, o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public BloodPressureModel create(String sessionId, BloodPressureModel bpm) {
        // create home BP reading

        if (storeRemotely) {
            // write BP reading to the FHIR server

            // todo : implement this

        } else {
//            return new BloodPressureModel(
//                    hbprService.create(sessionId, new HomeBloodPressureReading(bpm))
//            );
        }

        return null;
    }

    public Boolean delete(String sessionId, String id) {
        // delete home BP reading (do not allow deleting office BP readings!)

        // todo : implement this

        if (storeRemotely) {
            // id is a FHIR ID

        } else {
            Long longId = Long.parseLong(id);

//            hbprService.delete(sessionId, bpm.ge)
        }

        return null;
    }

    private List<BloodPressureModel> buildRemoteBloodPressureReadings(String sessionId) {
        List<BloodPressureModel> list = new ArrayList<>();

        Bundle bundle = ehrService.getBloodPressureObservations(sessionId);
        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            if (entryCon.getResource() instanceof Observation) {
                Observation o = (Observation) entryCon.getResource();
                try {
                    list.add(new BloodPressureModel(o, fcm));

                } catch (DataException e) {
                    logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                }

            } else {
                Resource r = entryCon.getResource();
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
            list.add(new BloodPressureModel(item));
        }

        return list;
    }
}
