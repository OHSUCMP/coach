package edu.ohsu.cmp.coach.cache;

import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.model.recommendation.Suggestion;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UserCache {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int POOL_SIZE = 5;
    private static final int THREAD_SLEEP_MS = 1000;

    private final ExecutorService pool;

    private Audience audience;
    private FHIRCredentialsWithClient fhirCredentialsWithClient;
    private Long internalPatientId;
//    private Patient patient;
//    private Bundle observations;
//    private Bundle conditions;
//    private Bundle currentGoals;
//    private Bundle medications;
//    private Bundle adverseEvents;
    private Map<String, List<Card>> cards;

    public enum CacheType {
        PATIENT,
        OBSERVATIONS,
        CONDITIONS,
        CURRENT_GOALS,
        MEDICATIONS,
        ADVERSE_EVENTS,
        CARDS
    }

    private static final class CacheItem {
        private CacheType cacheType;
        private Object object = null;
        private boolean isPopulating = false;
        private boolean populationFailed = false;

        private CacheItem(CacheType cacheType) {
            this.cacheType = cacheType;
        }
    }

    private Map<CacheType, CacheItem> resources;

    public UserCache(Audience audience, FHIRCredentialsWithClient fhirCredentialsWithClient, Long internalPatientId) {

        // todo : make thread pool size configurable?

        this.pool = Executors.newFixedThreadPool(POOL_SIZE);

        // todo : add pool shutdown hook ?

        this.audience = audience;
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.internalPatientId = internalPatientId;
        this.cards = new LinkedHashMap<String, List<Card>>();
        this.resources = new HashMap<CacheType, CacheItem>();
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public FHIRCredentialsWithClient getFhirCredentialsWithClient() {
        return fhirCredentialsWithClient;
    }

    public void setFhirCredentialsWithClient(FHIRCredentialsWithClient fhirCredentialsWithClient) {
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
    }

    public Long getInternalPatientId() {
        return internalPatientId;
    }

    public void setInternalPatientId(Long internalPatientId) {
        this.internalPatientId = internalPatientId;
    }

    public <T extends Resource> T getResource(CacheType cacheType, Class<T> aClass) {
        return getResource(cacheType, (Callable<T>) null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Resource> T getResource(CacheType cacheType, Callable<T> populateFunction) {
        logger.debug("requesting resource " + cacheType + " from cache");

        if ( ! resources.containsKey(cacheType) ) {
            resources.put(cacheType, new CacheItem(cacheType));
        }

        CacheItem item = resources.get(cacheType);

        if (item.object == null) {
            if (item.isPopulating) {
                logger.debug("cache for " + cacheType + " is currently populating.  sleeping until populated -");
                while (item.isPopulating && ! item.populationFailed) {
                    try {
                        Thread.sleep(THREAD_SLEEP_MS); // sleep 5 secs

                    } catch (InterruptedException e) {
                        break;
                    }
                }
                logger.debug("cache for " + cacheType + " is no longer populating, resuming operation -");

            } else if (populateFunction != null) {
                item.isPopulating = true;

                logger.debug("request for " + cacheType + " contains populate function and cache not populated; populating -");
                long start = System.currentTimeMillis();

                try {
                    Future<T> future = pool.submit(populateFunction);
                    item.object = future.get();

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " populating cache for cacheType=" + cacheType, e);
                    item.populationFailed = true;

                } finally {
                    item.isPopulating = false;
                    logger.debug("finished populating " + cacheType + " cache (took " + (System.currentTimeMillis() - start) + " ms)");
                }

            } else {
                logger.debug("cache for " + cacheType + " is empty, not populating, and no populate function specified.  sleeping until populated - ");
                while (item.object == null && ! item.populationFailed) {
                    try {
                        Thread.sleep(THREAD_SLEEP_MS); // sleep 5 secs

                    } catch (InterruptedException e) {
                        break;
                    }
                }
                logger.debug("object in cache for " + cacheType + " is no longer null, resuming operation -");
            }
        }

        return (T) item.object;
    }

    public boolean addResourceToBundle(CacheType cacheType, Resource resource) {
        if (resources.containsKey(cacheType)) {
            CacheItem item = resources.get(cacheType);
            if (item != null && item.object != null && item.object instanceof Bundle) {
                ((Bundle) item.object).addEntry().setResource(resource);
                return true;

            } else {
                logger.warn("couldn't add resource " + resource.getClass().getName() + " to cache " +
                        cacheType + "!  skipping -");
            }
        }
        return false;
    }

    public void clearResources(CacheType cacheType) {
        resources.remove(cacheType);
    }

    public boolean containsCards(String recommendationId) {
        return cards.containsKey(recommendationId);
    }

    public void setCards(String recommendationId, List<Card> list) {
        cards.put(recommendationId, list);
    }

    public List<Card> getCards(String recommendationId) {
        return cards.get(recommendationId);
    }

    public boolean deleteCards(String recommendationId) {
        if (cards.containsKey(recommendationId)) {
            cards.remove(recommendationId);
            return true;
        }
        return false;
    }

    public void deleteAllCards() {
        cards.clear();
    }

    /**
     * used to clear a particular Suggestion from the cache, by ID.  very useful for updating the cache in-place
     * after the user performs an action that should make that suggestion disappear
     * @param id
     * @return
     */
    public boolean deleteSuggestion(String id) {
        boolean rval = false;
        for (Map.Entry<String, List<Card>> entry : cards.entrySet()) {
            for (Card c : entry.getValue()) {
                if (c.getSuggestions() != null) {
                    Iterator<Suggestion> iter = c.getSuggestions().iterator();
                    while (iter.hasNext()) {
                        Suggestion s = iter.next();
                        if (s.getId() != null && s.getId().equals(id)) {    // ignore suggestions without IDs
                            iter.remove();
                            rval = true;
                        }
                    }
                }
            }
        }
        return rval;
    }
}
