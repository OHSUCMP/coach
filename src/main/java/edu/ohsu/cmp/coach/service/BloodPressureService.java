package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.cache.CacheData;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BloodPressureService {

    // todo : complete this

    @Value("${fhir.observation.bp-write}")
    private Boolean doRemote;

    public List<BloodPressureModel> getBloodPressureReadings(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        if (doRemote) {
            // get BP readings only from remote server, no extra work required

        } else {
            // get office BP readings from remote server
            // integrate locally-stored home BP readings
        }

        return null;
    }

    public Observation create(String sessionId, BloodPressureModel bpm) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        if (doRemote) {
            // write BP reading to the FHIR server

        } else {
            // get office BP readings from remote server
            // integrate locally-stored home BP readings
        }

        // create home BP reading

        return null;
    }

    public Boolean delete(String sessionId, BloodPressureModel bpm) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        // delete home BP reading (do not allow deleting office BP readings!)

        return null;
    }
}
