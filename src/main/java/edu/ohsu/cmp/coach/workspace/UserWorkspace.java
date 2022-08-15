package edu.ohsu.cmp.coach.workspace;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.model.recommendation.Suggestion;
import edu.ohsu.cmp.coach.service.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class UserWorkspace {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int POOL_SIZE = 2;

    private static final String CACHE_PATIENT = "Patient";
    private static final String CACHE_ENCOUNTER = "Encounter";
    private static final String CACHE_PROTOCOL = "Protocol";
    private static final String CACHE_BP = "BP";
    private static final String CACHE_PULSE = "Pulse";
    private static final String CACHE_ADVERSE_EVENT = "AdverseEvent";
    private static final String CACHE_GOAL = "Goal";
    private static final String CACHE_MEDICATION = "Medication";

    private static final String CACHE_CONDITION_ENCOUNTER_DIAGNOSIS = "ConditionEncounterDiagnosis";
    private static final String CACHE_SUPPLEMENTAL_RESOURCES = "SupplementalResources";

    private final ApplicationContext ctx;
    private final String sessionId;
    private final Audience audience;
    private final FHIRCredentialsWithClient fhirCredentialsWithClient;
    private final FhirConfigManager fcm;
    private final Long internalPatientId;

    private Cache cache;
    private Cache cardCache;
    private Cache<String, Bundle> bundleCache;

    private ExecutorService executorService;

    protected UserWorkspace(ApplicationContext ctx, String sessionId, Audience audience,
                            FHIRCredentialsWithClient fhirCredentialsWithClient, FhirConfigManager fcm) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.audience = audience;
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.fcm = fcm;

        PatientService patientService = ctx.getBean(PatientService.class);
        this.internalPatientId = patientService.getInternalPatientId(
                fhirCredentialsWithClient.getCredentials().getPatientId()
        );

        cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        cardCache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        bundleCache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        executorService = Executors.newFixedThreadPool(POOL_SIZE);
    }

    public Audience getAudience() {
        return audience;
    }

    public FHIRCredentialsWithClient getFhirCredentialsWithClient() {
        return fhirCredentialsWithClient;
    }

    public Long getInternalPatientId() {
        return internalPatientId;
    }

    public void populate() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                logger.info("BEGIN populating workspace for session=" + sessionId);
                getPatient();
                getGoals();
                getEncounters();
                getProtocolObservations();
                getBloodPressures();
                getPulses();
                getEncounterDiagnosisConditions();
                getAdverseEvents();
                getMedications();
                getSupplementalResources();
                getAllCards();
                logger.info("DONE populating workspace for session=" + sessionId +
                        " (took " + (System.currentTimeMillis() - start) + "ms)");
            }
        };
        executorService.submit(runnable);
    }

    public void runRecommendations() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                deleteAllCards();
                getAllCards();
            }
        };
        executorService.submit(runnable);
    }

    public void clearCaches() {
        cache.invalidateAll();
        cardCache.invalidateAll();
        bundleCache.invalidateAll();
    }

    public void shutdown() {
        clearCaches();
        cache.cleanUp();
        cardCache.cleanUp();
        bundleCache.cleanUp();
    }

///////////////////////////////////////////////////////////////////////////////////////

    public List<Encounter> getEncounters() {
        return List.copyOf(getEncounterMap().values());
    }

    public Encounter getEncounter(Reference encounterReference) {
        Map<String, Encounter> map = getEncounterMap();
        for (String key : FhirUtil.buildKeys(encounterReference)) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private Map<String, Encounter> getEncounterMap() {
        return (Map<String, Encounter>) cache.get(CACHE_ENCOUNTER, new Function<String, Map<String, Encounter>>() {
            @Override
            public Map<String, Encounter> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Encounters for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                Map<String, Encounter> map = new LinkedHashMap<>();
                for (Encounter encounter : svc.getEncounters(sessionId)) {
                    for (String key : FhirUtil.buildKeys(encounter.getId(), encounter.getIdentifier())) {
                        map.put(key, encounter);
                    }
                }

                logger.info("DONE building Encounters for session=" + sessionId +
                        " (size=" + map.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return map;
            }
        });
    }

    public PatientModel getPatient() {
        return (PatientModel) cache.get(CACHE_PATIENT, new Function<String, PatientModel>() {
            @Override
            public PatientModel apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Patient for session=" + sessionId);

                PatientService svc = ctx.getBean(PatientService.class);
                PatientModel patient = svc.buildPatient(sessionId);

                logger.info("DONE building Patient for session=" + sessionId +
                        " (took " + (System.currentTimeMillis() - start) + "ms)");

                return patient;
            }
        });
    }

    public Bundle getProtocolObservations() {
        return bundleCache.get(CACHE_PROTOCOL, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Protocol Observations for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                Bundle bundle = svc.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getProtocolCoding()), fcm.getProtocolLookbackPeriod(),null);

                int size = bundle.hasEntry() ? bundle.getEntry().size() : 0;
                logger.info("DONE building Protocol Observations for session=" + sessionId +
                        " (size=" + size + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return bundle;
            }
        });
    }

    public List<BloodPressureModel> getBloodPressures() {
        return (List<BloodPressureModel>) cache.get(CACHE_BP, new Function<String, List<BloodPressureModel>>() {
            @Override
            public List<BloodPressureModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Blood Pressures for session=" + sessionId);

                BloodPressureService svc = ctx.getBean(BloodPressureService.class);
                try {
                    List<BloodPressureModel> list = svc.buildBloodPressureList(sessionId);

                    logger.info("DONE building Blood Pressures for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public List<PulseModel> getPulses() {
        return (List<PulseModel>) cache.get(CACHE_PULSE, new Function<String, List<PulseModel>>() {
            @Override
            public List<PulseModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Pulses for session=" + sessionId);

                PulseService svc = ctx.getBean(PulseService.class);
                try {
                    List<PulseModel> list = svc.buildPulseList(sessionId);

                    logger.info("DONE building Pulses for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Bundle getEncounterDiagnosisConditions() {
        return bundleCache.get(CACHE_CONDITION_ENCOUNTER_DIAGNOSIS, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Encounter Diagnosis Conditions for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                Bundle bundle = svc.getEncounterDiagnosisConditions(sessionId);

                int size = bundle.hasEntry() ? bundle.getEntry().size() : 0;
                logger.info("DONE building Encounter Diagnosis Conditions for session=" + sessionId +
                        " (size=" + size + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return bundle;
            }
        });
    }

    public List<AdverseEventModel> getAdverseEvents() {
        return (List<AdverseEventModel>) cache.get(CACHE_ADVERSE_EVENT, new Function<String, List<AdverseEventModel>>() {
            @Override
            public List<AdverseEventModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Adverse Events for session=" + sessionId);

                AdverseEventService svc = ctx.getBean(AdverseEventService.class);
                try {
                    List<AdverseEventModel> list = svc.buildAdverseEvents(sessionId);

                    logger.info("DONE building Adverse Events for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public List<GoalModel> getGoals() {
        return (List<GoalModel>) cache.get(CACHE_GOAL, new Function<String, List<GoalModel>>() {
            @Override
            public List<GoalModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Goals for session=" + sessionId);

                GoalService svc = ctx.getBean(GoalService.class);
                List<GoalModel> list = svc.buildCurrentGoals(sessionId);

                logger.info("DONE building Goals for session=" + sessionId +
                        " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return list;
            }
        });
    }

    public List<MedicationModel> getMedications() {
        return (List<MedicationModel>) cache.get(CACHE_MEDICATION, new Function<String, List<MedicationModel>>() {
            @Override
            public List<MedicationModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Medications for session=" + sessionId);

                MedicationService svc = ctx.getBean(MedicationService.class);
                try {
                    List<MedicationModel> list = svc.buildMedications(sessionId);

                    logger.info("DONE building Medications for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Bundle getSupplementalResources() {
        return bundleCache.get(CACHE_SUPPLEMENTAL_RESOURCES, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Supplemental Resources for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                CompositeBundle compositeBundle = new CompositeBundle();

                compositeBundle.consume(svc.getProblemListConditions(sessionId));

                List<Coding> codings = new ArrayList<>();
                codings.add(fcm.getBmiCoding());
                codings.add(fcm.getSmokingCoding());
                codings.add(fcm.getDrinksCoding());
                compositeBundle.consume(svc.getObservations(sessionId, FhirUtil.toCodeParamString(codings), fcm.getBmiLookbackPeriod(),null));

                compositeBundle.consume(svc.getCounselingProcedures(sessionId));

                logger.info("DONE building Supplemental Resources for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public Map<String, List<Card>> getAllCards() {
        Map<String, List<Card>> map = new LinkedHashMap<>();
        RecommendationService svc = ctx.getBean(RecommendationService.class);
        try {
            for (CDSHook hook : svc.getOrderedCDSHooks()) {
                try {
                    map.put(hook.getId(), getCards(hook.getId()));
                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " getting cards for hook=" + hook.getId() + " - " + e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return map;
    }

    public List<Card> getCards(String recommendationId) {
        return (List<Card>) cardCache.get(recommendationId, new Function<String, List<Card>>() {
            @Override
            public List<Card> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Cards for session=" + sessionId);

                RecommendationService svc = ctx.getBean(RecommendationService.class);
                try {
                    List<Card> list = svc.getCards(sessionId, s);

                    logger.info("DONE building Cards for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void deleteCards(String recommendationId) {
        cardCache.invalidate(recommendationId);
    }

    public void deleteAllCards() {
        cardCache.invalidateAll();
    }

    /**
     * used to clear a particular Suggestion from the cache, by ID.  very useful for updating the cache in-place
     * after the user performs an action that should make that suggestion disappear
     * @param id
     * @return
     */
    public boolean deleteSuggestion(String id) {
        boolean rval = false;
        for (Map.Entry<String, List<Card>> entry : ((Map<String, List<Card>>) cardCache.asMap()).entrySet()) {
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
