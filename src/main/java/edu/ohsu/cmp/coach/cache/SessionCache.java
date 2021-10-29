package edu.ohsu.cmp.coach.cache;

import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCache {
    private static SessionCache cache = null;

    public static SessionCache getInstance() {
        if (cache == null) {
            cache = new SessionCache();
        }
        return cache;
    }

    private Map<String, CacheData> map = new ConcurrentHashMap<String, CacheData>();

    private SessionCache() {
        // private constructor, singleton class
    }

    public synchronized boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public synchronized void set(String sessionId, Audience audience,
                                 FHIRCredentialsWithClient fhirCredentialsWithClient,
                                 Long internalPatientId) {
        CacheData cacheData = new CacheData(audience, fhirCredentialsWithClient, internalPatientId);
        map.put(sessionId, cacheData);
    }

    public synchronized CacheData get(String sessionId) throws SessionMissingException {
        if (map.containsKey(sessionId)) {
            return map.get(sessionId);

        } else {
            throw new SessionMissingException(sessionId);
        }
    }

    public boolean remove(String sessionId) {
        if (map.containsKey(sessionId)) {
            map.remove(sessionId);
            return true;
        }
        return false;
    }

    /**
     * clears all data but retains credentials and other info that should persist
     * @param sessionId
     * @return
     */
    public boolean flush(String sessionId) {
        if (map.containsKey(sessionId)) {
            CacheData cacheData = map.remove(sessionId);

            Audience audience = cacheData.getAudience();
            FHIRCredentialsWithClient fcc = cacheData.getFhirCredentialsWithClient();
            Long internalPatientId = cacheData.getInternalPatientId();

            map.put(sessionId, new CacheData(audience, fcc, internalPatientId));

            return true;

        } else {
            return false;
        }
    }
}
