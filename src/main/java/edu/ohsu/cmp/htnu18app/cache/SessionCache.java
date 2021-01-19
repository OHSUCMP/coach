package edu.ohsu.cmp.htnu18app.cache;

import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;

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

    public synchronized void set(String sessionId, FHIRCredentialsWithClient fhirCredentialsWithClient, Long internalPatientId) {
        CacheData cacheData = new CacheData(fhirCredentialsWithClient, internalPatientId);
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
}
