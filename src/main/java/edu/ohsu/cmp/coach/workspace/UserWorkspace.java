package edu.ohsu.cmp.coach.workspace;

import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.Payload;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.fhir.FhirQueryManager;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.omron.OmronStatus;
import edu.ohsu.cmp.coach.model.omron.OmronStatusData;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.model.recommendation.Suggestion;
import edu.ohsu.cmp.coach.model.redcap.RandomizationGroup;
import edu.ohsu.cmp.coach.service.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class UserWorkspace {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int POOL_SIZE = 5;

    private static final String CACHE_PATIENT = "Patient";
    private static final String CACHE_ENCOUNTER = "Encounter";
    private static final String CACHE_PROTOCOL = "Protocol";
    private static final String CACHE_BP = "BP";
    private static final String CACHE_PULSE = "Pulse";
    private static final String CACHE_ADVERSE_EVENT = "AdverseEvent";
    private static final String CACHE_GOAL = "Goal";
    private static final String CACHE_MEDICATION = "Medication";
    private static final String CACHE_ORDER_SERVICE_REQUEST = "OrderServiceRequest";

    private static final String CACHE_CONDITION_ENCOUNTER_DIAGNOSIS = "EncounterDiagnosisCondition";
    private static final String CACHE_PROBLEM_LIST_CONDITION = "ProblemListCondition";
    private static final String CACHE_SMOKING_OBSERVATIONS = "SmokingObservations";
    private static final String CACHE_DRINKING_OBSERVATIONS = "DrinkingObservations";
    private static final String CACHE_OTHER_SUPPLEMENTAL_RESOURCES = "OtherSupplementalResources";

    private final ApplicationContext ctx;
    private final String sessionId;
    private final Audience audience;
    private final RandomizationGroup randomizationGroup;
    private final boolean requiresEnrollment;
    private final boolean hasCompletedStudy;
    private final FHIRCredentialsWithClient fhirCredentialsWithClient;
    private final FhirQueryManager fqm;
    private final FhirConfigManager fcm;
    private final Long internalPatientId;
    private VendorTransformer vendorTransformer = null;

    private final Cache cache;
    private final Cache cardCache;
    private final Cache<String, Bundle> bundleCache;
    private final ExecutorService executorService;

    private final AuditService auditService;

    // Omron stuff
    private MyOmronTokenData omronTokenData = null;
    private Date omronLastUpdated = null;
    private String redcapId = null;
    private Boolean bpGoalUpdated = null;
    private Boolean confirmedEndOfStudy = null;
    private Boolean omronSynchronizing = false;
    private Integer omronCurrentItem = null;
    private Integer omronTotalItems = null;

    protected UserWorkspace(ApplicationContext ctx, String sessionId, Audience audience,
                            RandomizationGroup randomizationGroup,
                            boolean requiresEnrollment, boolean hasCompletedStudy,
                            FHIRCredentialsWithClient fhirCredentialsWithClient,
                            FhirQueryManager fqm, FhirConfigManager fcm) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.audience = audience;
        this.randomizationGroup = randomizationGroup;
        this.requiresEnrollment = requiresEnrollment;
        this.hasCompletedStudy = hasCompletedStudy;

        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.fqm = fqm;
        this.fcm = fcm;

        this.auditService = ctx.getBean(AuditService.class);

        PatientService patientService = ctx.getBean(PatientService.class);
        MyPatient myPatient = patientService.getMyPatient(
                fhirCredentialsWithClient.getCredentials().getPatientId()
        );
        this.internalPatientId = myPatient.getId();
        this.omronLastUpdated = myPatient.getOmronLastUpdated();
        this.redcapId = myPatient.getRedcapId();
        this.bpGoalUpdated = myPatient.getBpGoalUpdated();
        this.confirmedEndOfStudy = myPatient.getConfirmedEndOfStudy();

        if (Audience.PATIENT.equals(audience) && this.confirmedEndOfStudy && ! hasCompletedStudy) {
            // it should never be the case where a participant a) hasn't yet completed the study, but b) has
            // confirmed that they've ended the study.  if this occurs, it's likely due to the participant having been
            // marked completed, but then that status was reverted.  in any case, if hasCompletedStudy == false,
            // isConfirmedEndOfStudy should be false too.
            patientService.setConfirmedEndOfStudy(internalPatientId, false);
            this.confirmedEndOfStudy = false;
        }

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

        setupAutoShutdownJob();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Audience getAudience() {
        return audience;
    }

    public RandomizationGroup getRandomizationGroup() {
        return randomizationGroup;
    }

    public RandomizationGroup getActiveRandomizationGroup() {
        return hasCompletedStudy ?
                RandomizationGroup.ENHANCED :
                randomizationGroup;
    }

    public boolean getRequiresEnrollment() {
        return requiresEnrollment;
    }

    public boolean isHasCompletedStudy() {
        return hasCompletedStudy;
    }

    public FHIRCredentialsWithClient getFhirCredentialsWithClient() {
        return fhirCredentialsWithClient;
    }

    public FhirQueryManager getFhirQueryManager() {
        return fqm;
    }

    public FhirConfigManager getFhirConfigManager() {
        return fcm;
    }

    public Long getInternalPatientId() {
        return internalPatientId;
    }

    public Date getOmronLastUpdated() {
        return omronLastUpdated;
    }

    public void setOmronLastUpdated(Date omronLastUpdated) {
        this.omronLastUpdated = omronLastUpdated;
    }

    public String getRedcapId() {
        return redcapId;
    }

    public Boolean getBpGoalUpdated() {
        return bpGoalUpdated;
    }

    public void setBpGoalUpdated(Boolean bpGoalUpdated) {
        this.bpGoalUpdated = bpGoalUpdated;
        PatientService patientService = ctx.getBean(PatientService.class);
        patientService.setBPGoalUpdated(internalPatientId, bpGoalUpdated);
    }

    public Boolean isConfirmedEndOfStudy() {
        return confirmedEndOfStudy;
    }

    public void setConfirmedEndOfStudy(Boolean confirmedEndOfStudy) {
        if (Audience.PATIENT.equals(audience)) {
            this.confirmedEndOfStudy = confirmedEndOfStudy;
            PatientService patientService = ctx.getBean(PatientService.class);
            patientService.setConfirmedEndOfStudy(internalPatientId, confirmedEndOfStudy);
        }
    }

    public void populate() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                logger.info("BEGIN populating workspace for session=" + sessionId);
                getPatient();
                getOrderServiceRequests();
                getRemoteGoals();
                doBPGoalCheck();
                getEncounters();
                getProtocolObservations();
                getRemoteBloodPressures();
                getRemotePulses();
                getEncounterDiagnosisConditions();
                getRemoteAdverseEvents();
                getMedications();
                getProblemListConditions();
                getSmokingObservations();
                getDrinkingObservations();
                getOtherSupplementalResources();
                refreshHypotensionAdverseEvents();
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
        logger.info("clearing caches for session=" + sessionId);
        cache.invalidateAll();
        cardCache.invalidateAll();
        bundleCache.invalidateAll();
    }

    public void clearVitalsCaches() {
        logger.info("clearing BP and Pulse caches for session=" + sessionId);
        cache.invalidate(CACHE_BP);
        cache.invalidate(CACHE_PULSE);
    }

    public void shutdown() {
        logger.info("shutting down workspace for session=" + sessionId);
        executorService.shutdown();

        clearCaches();

        cache.cleanUp();
        cardCache.cleanUp();
        bundleCache.cleanUp();

        shutdownJobs();
    }

    private void shutdownJobs() {
        logger.info("clearing triggers and jobs for session=" + sessionId);
        Scheduler scheduler = ctx.getBean(Scheduler.class);
        try {
            for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.groupEquals(sessionId))) {
                logger.debug("unscheduling trigger: " + triggerKey.getName());
                scheduler.unscheduleJob(triggerKey);
            }

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(sessionId))) {
                logger.debug("deleting job: " + jobKey.getName());
                scheduler.deleteJob(jobKey);
            }

        } catch (SchedulerException e) {
            logger.error("caught " + e.getClass().getName() + " shutting down jobs for session " + sessionId + " - " +
                    e.getMessage(), e);
        }
    }

    private void setupAutoShutdownJob() {
        Scheduler scheduler = ctx.getBean(Scheduler.class);
        Date shutdownTimestamp = deriveExpirationTimestamp(fhirCredentialsWithClient.getCredentials().getBearerToken());

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ShutdownWorkspaceJob.JOBDATA_APPLICATIONCONTEXT, ctx);
        jobDataMap.put(ShutdownWorkspaceJob.JOBDATA_SESSIONID, sessionId);

        JobKey jobKey = new JobKey("shutdownWorkspaceJob-" + sessionId, sessionId);
        JobDetail job = JobBuilder.newJob(ShutdownWorkspaceJob.class)
                .storeDurably()
                .withIdentity(jobKey)
                .withDescription("Auto-shutdown User Workspace for session " + sessionId + " at " + shutdownTimestamp)
                .usingJobData(jobDataMap)
                .build();

        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(ShutdownWorkspaceJob.class);
        jobDetailFactory.setDescription("Invoke Shutdown User Workspace Job service...");
        jobDetailFactory.setDurability(true);

        Trigger trigger = TriggerBuilder.newTrigger().forJob(job)
                .withIdentity("shutdownWorkspaceTrigger-" + sessionId, sessionId)
                .withDescription("Shutdown Workspace trigger")
                .startAt(shutdownTimestamp)
                .build();

        try {
            if ( ! scheduler.isStarted() ) {
                scheduler.start();
            }

            if (scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                logger.warn("found pre-existing auto-shutdown job for session " + sessionId +
                        ", but this should have been cleared earlier, it shouldn't have gotten this far.  ???");
                logger.info("deleting job: " + jobDetail.getDescription());
                scheduler.deleteJob(jobKey);
            }

            logger.info("scheduling job: " + job.getDescription());
            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private Date deriveExpirationTimestamp(String bearerToken) {
        try {
            String[] parts = bearerToken.split("\\.");
            String payloadJSON = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JWTParser parser = new JWTParser();
            Payload payload = parser.parsePayload(payloadJSON);
            return payload.getExpiresAt();

        } catch (Exception e) {
            logger.warn("couldn't parse token for session=" + sessionId + " - will auto-shutdown workspace after 1 day");
            logger.debug("caught " + e.getClass().getName() + " parsing bearer token for session=" + sessionId + " - " +
                    e.getMessage(), e);

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, 1);
            return cal.getTime();
        }
    }

///////////////////////////////////////////////////////////////////////////////////////

    public List<Encounter> getEncounters() {
        List<Encounter> list = new ArrayList<>();
        Set<String> foundIds = new HashSet<>();
        for (Encounter encounter : getEncounterMap().values()) {
            if ( ! foundIds.contains(encounter.getId()) ) {
                list.add(encounter);
                foundIds.add(encounter.getId());
            }
        }
        return list;
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
                try {
                    for (Encounter encounter : svc.getEncounters(sessionId)) {
                        for (String key : FhirUtil.buildKeys(encounter.getId(), encounter.getIdentifier())) {
                            map.put(key, encounter);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
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
                PatientModel patient = null;
                try {
                    patient = svc.buildPatient(sessionId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

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
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    compositeBundle.consume(svc.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getProtocolCoding()), fcm.getProtocolLookbackPeriod(), null));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Protocol Observations for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public List<BloodPressureModel> getRemoteBloodPressures() {
        return (List<BloodPressureModel>) cache.get(CACHE_BP, new Function<String, List<BloodPressureModel>>() {
            @Override
            public List<BloodPressureModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build remote Blood Pressures for session=" + sessionId);

                BloodPressureService svc = ctx.getBean(BloodPressureService.class);
                try {
                    List<BloodPressureModel> list = svc.buildRemoteBloodPressureList(sessionId);

                    logger.info("DONE building remote Blood Pressures for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public List<PulseModel> getRemotePulses() {
        return (List<PulseModel>) cache.get(CACHE_PULSE, new Function<String, List<PulseModel>>() {
            @Override
            public List<PulseModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build remote Pulses for session=" + sessionId);

                PulseService svc = ctx.getBean(PulseService.class);
                try {
                    List<PulseModel> list = svc.buildRemotePulseList(sessionId);

                    logger.info("DONE building remote Pulses for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (Exception e) {
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
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    compositeBundle.consume(svc.getEncounterDiagnosisConditions(sessionId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Encounter Diagnosis Conditions for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public List<AdverseEventModel> getRemoteAdverseEvents() {
        return (List<AdverseEventModel>) cache.get(CACHE_ADVERSE_EVENT, new Function<String, List<AdverseEventModel>>() {
            @Override
            public List<AdverseEventModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN building remote Adverse Events for session=" + sessionId);

                AdverseEventService svc = ctx.getBean(AdverseEventService.class);
                try {
                    List<AdverseEventModel> list = svc.buildRemoteAdverseEvents(sessionId);

                    logger.info("DONE building remote Adverse Events for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Bundle getOrderServiceRequests() {
        return bundleCache.get(CACHE_ORDER_SERVICE_REQUEST, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Order Service Requests for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    compositeBundle.consume(svc.getOrderServiceRequests(sessionId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Order ServiceRequests for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public List<GoalModel> getRemoteGoals() {
        return (List<GoalModel>) cache.get(CACHE_GOAL, new Function<String, List<GoalModel>>() {
            @Override
            public List<GoalModel> apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build remote Goals for session=" + sessionId);

                GoalService svc = ctx.getBean(GoalService.class);
                try {
                    List<GoalModel> list = svc.buildRemoteGoals(sessionId);

                    logger.info("DONE building remote Goals for session=" + sessionId +
                            " (size=" + list.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                    return list;

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void doBPGoalCheck() {                  // ONLY CHECK REMOTE GOALS!  local BP goal update will be set via GoalsController.updatebp().
                                                    // checking local BP goal here will not differentiate between intentional user-defined and default goals
        if ( ! bpGoalUpdated ) {
            for (GoalModel goal : getRemoteGoals()) {     // these come from the FHIR server.
                if (goal.isBPGoal()) {
                    setBpGoalUpdated(true);
                    break;
                }
            }
        }
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

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Bundle getProblemListConditions() {
        return bundleCache.get(CACHE_PROBLEM_LIST_CONDITION, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Problem List Conditions Resources for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    compositeBundle.consume(svc.getProblemListConditions(sessionId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Problem List Resources for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public Bundle getSmokingObservations() {
        return bundleCache.get(CACHE_SMOKING_OBSERVATIONS, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Tobacco Smoking Observations for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    compositeBundle.consume(svc.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getSmokingCodings()), fcm.getSmokingLookbackPeriod(), null));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Tobacco Smoking Observations for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public Bundle getDrinkingObservations() {
        return bundleCache.get(CACHE_DRINKING_OBSERVATIONS, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Alcohol Drinking Observations for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    String lookback = fcm.isDrinkingIncludeLookbackInQuery() ?
                            fcm.getDrinkingLookbackPeriod() :
                            null;
                    compositeBundle.consume(svc.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getDrinkingCodings()), lookback, null));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Alcohol Drinking Observations for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    public Bundle getOtherSupplementalResources() {
        return bundleCache.get(CACHE_OTHER_SUPPLEMENTAL_RESOURCES, new Function<String, Bundle>() {
            @Override
            public Bundle apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build Supplemental Resources for session=" + sessionId);

                EHRService svc = ctx.getBean(EHRService.class);
                CompositeBundle compositeBundle = new CompositeBundle();

                try {
                    compositeBundle.consume(svc.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getBmiCoding()), fcm.getBmiLookbackPeriod(),null));
                    compositeBundle.consume(svc.getCounselingProcedures(sessionId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building Supplemental Resources for session=" + sessionId +
                        " (size=" + compositeBundle.size() + ", took " + (System.currentTimeMillis() - start) + "ms)");

                return compositeBundle.getBundle();
            }
        });
    }

    private void refreshHypotensionAdverseEvents() {
        HypotensionAdverseEventService svc = ctx.getBean(HypotensionAdverseEventService.class);
        try {
            svc.refresh(sessionId);
            logger.info("DONE refreshing hypotension AdverseEvent resources for session=" + sessionId);

        } catch (DataException e) {
            logger.warn("caught " + e.getClass().getName() + " refreshing hypotension AdverseEvent resources for session=" +
                    sessionId + " - " + e.getMessage(), e);
        }
    }

    public Map<String, List<Card>> getAllCards() {
        Map<String, List<Card>> map = new LinkedHashMap<>();
        RecommendationService svc = ctx.getBean(RecommendationService.class);
        try {
            for (CDSHook hook : svc.getOrderedCDSHooks(sessionId)) {
                try {
                    map.put(hook.getId(), getCards(hook.getId()));

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " getting cards for hook=" + hook.getId() + " - " +
                            e.getMessage(), e);

                    auditService.doAudit(sessionId, AuditSeverity.ERROR, "recommendation exception", "encountered " +
                            e.getClass().getSimpleName() + " getting recommendations for " + hook.getId() + " - " +
                            e.getMessage());

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

    public void clearSupplementalData() {
        HomeBloodPressureReadingService hbprService = ctx.getBean(HomeBloodPressureReadingService.class);
        hbprService.deleteAll(sessionId);

        // todo : also clear hypotension adverse events

        HomePulseReadingService hprService = ctx.getBean(HomePulseReadingService.class);
        hprService.deleteAll(sessionId);

        GoalService gService = ctx.getBean(GoalService.class);
        gService.deleteAll(sessionId);

        setBpGoalUpdated(false);    // if we're flushing goals, this flag needs to be reset as well

        CounselingService cService = ctx.getBean(CounselingService.class);
        cService.deleteAll(sessionId);

        OmronService omronService = ctx.getBean(OmronService.class);
        omronService.deleteAll(sessionId);
        omronService.resetLastUpdated(sessionId);
        omronLastUpdated = null;

        // todo : clear Omron token data
        // todo : cancel any scheduled tasks that may exist for refreshing Omron token data
    }

    public VendorTransformer getVendorTransformer() {
        return vendorTransformer;
    }

    public void setVendorTransformer(VendorTransformer vendorTransformer) {
        this.vendorTransformer = vendorTransformer;
    }

    public MyOmronTokenData getOmronTokenData() {
        return omronTokenData;
    }

    public void setOmronTokenData(MyOmronTokenData omronTokenData) {
        this.omronTokenData = omronTokenData;
    }

    public void initiateSynchronousOmronUpdate() {
        OmronService omronService = ctx.getBean(OmronService.class);
        if (omronSynchronizing) {
            logger.warn("Omron is still synchronizing; aborting new synchronize request");
            return;
        }

        if ( ! omronService.isOmronEnabled() ) {
            logger.warn("Omron integration is currently disabled; aborting synchronize request");
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                logger.info("BEGIN Omron synchronization for session=" + sessionId);
                try {
                    omronSynchronizing = true;
                    omronService.synchronize(sessionId);

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " synchronizing with Omron - " + e.getMessage(), e);

                } finally {
                    omronSynchronizing = false;
                    omronCurrentItem = null;
                    omronTotalItems = null;
                }
                logger.info("DONE Omron synchronization for session=" + sessionId +
                        " (took " + (System.currentTimeMillis() - start) + "ms)");
            }
        };

        executorService.submit(runnable);
    }

    public Boolean isOmronSynchronizing() {
        return omronSynchronizing;
    }

    public void setOmronSynchronizationProgress(int current, int total) {
        logger.debug("setting Omron sync progress: current=" + current + ", total=" + total);
        if (omronSynchronizing) {
            omronCurrentItem = current;
            omronTotalItems = total;

        } else {
            logger.warn("not setting Omron sync progress (current=" + current + ", total=" + total + ") because omronSynchronizing=false");
        }
    }

    public OmronStatusData getOmronSynchronizationStatus() {
        OmronService omronService = ctx.getBean(OmronService.class);
        if (omronService.isOmronEnabled()) {
            OmronStatus status;
            if (omronSynchronizing) {
                status = omronCurrentItem == null && omronTotalItems == null ?
                        OmronStatus.INITIATING_SYNC :
                        OmronStatus.SYNCHRONIZING;
            } else {
                status = OmronStatus.READY;
            }
            return new OmronStatusData(status, omronLastUpdated, omronCurrentItem, omronTotalItems);

        } else {
            return new OmronStatusData(OmronStatus.DISABLED, null, null, null);
        }
    }
}
