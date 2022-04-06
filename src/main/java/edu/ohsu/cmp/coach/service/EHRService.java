package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.fhir.FhirQueryManager;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.util.EncounterMatcher;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * EHRService
 * This service is responsible for executing FHIR queries and returning either individual resources (e.g. Patient), or
 * Bundles of resources.  It handles filtering resources by modifier elements and other general requirements to ensure
 * that only "good" resources are included in results.  No other filtering takes place here.
 */
@Service
public class EHRService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int MAX_CODES_PER_QUERY = 32; // todo: auto-identify this, or at least put it in the config

    private static final String CONDITION_CLINICALSTATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String CONDITION_VERIFICATIONSTATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-ver-status";

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private FhirQueryManager fhirQueryManager;

    @Autowired
    private FHIRService fhirService;

    public Patient getPatient(String sessionId) {
        logger.info("getting Patient for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        return fhirService.readByReference(fcc, Patient.class,
                fhirQueryManager.getPatientLookup(fcc.getCredentials().getPatientId())
        );
    }

    public List<Encounter> getEncounters(String sessionId) {
        logger.info("getting Encounters for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        Bundle bundle = fhirService.search(fcc, fhirQueryManager.getEncounterQuery(fcc.getCredentials().getPatientId()),
                new Function<Resource, Boolean>() {
                    @Override
                    public Boolean apply(Resource resource) {
                        if (resource instanceof Encounter) {
                            Encounter encounter = (Encounter) resource;
                            if (encounter.getStatus() != Encounter.EncounterStatus.FINISHED) {
                                logger.debug("removing Encounter " + encounter.getId() + " - invalid status");
                                return false;
                            }

                            EncounterMatcher matcher = new EncounterMatcher(fcm);
                            if (!matcher.isAmbEncounter(encounter) && !matcher.isHomeHealthEncounter(encounter)) {
                                logger.debug("removing Encounter " + encounter.getId() + " - not AMB or HH");
                                return false;
                            }
                        }

                        return true;
                    }
                }
        );

        List<Encounter> list = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Encounter) {
                    list.add((Encounter) entry.getResource());
                }
            }
        }

        return list;
    }

    public Bundle getObservations(String sessionId, String code, Integer limit) {
        logger.info("getting " + code + " Observations for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        return fhirService.search(fcc, fhirQueryManager.getObservationCodeQuery(fcc.getCredentials().getPatientId(), code),
                new Function<Resource, Boolean>() {
            @Override
            public Boolean apply(Resource resource) {
                if (resource instanceof Observation) {
                    Observation observation = (Observation) resource;
                    if (observation.getStatus() != Observation.ObservationStatus.FINAL &&
                            observation.getStatus() != Observation.ObservationStatus.AMENDED &&
                            observation.getStatus() != Observation.ObservationStatus.CORRECTED) {
                        logger.debug("removing Observation " + observation.getId() + " - invalid status");
                        return false;
                    }

                    if (!observation.hasEncounter()) {
                        logger.debug("removing Observation " + observation.getId() + " - no Encounter referenced");
                        return false;
                    }
                }

                return true;
            }
        }, limit);
    }

    public Bundle getEncounterDiagnosisConditions(String sessionId) {
        return getConditions(sessionId, "encounter-diagnosis");
    }

    public Bundle getConditions(String sessionId, String category) {
        logger.info("getting " + category + " Conditions for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        return fhirService.search(fcc, fhirQueryManager.getConditionQuery(fcc.getCredentials().getPatientId(), category),
                new Function<Resource, Boolean>() {
                    @Override
                    public Boolean apply(Resource resource) {
                        if (resource instanceof Condition) {
                            Condition c = (Condition) resource;

                            if (c.hasClinicalStatus()) {
                                CodeableConcept cc = c.getClinicalStatus();
                                if (!cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "active") &&
                                        !cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "recurrence") &&
                                        !cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "relapse")) {
                                    logger.debug("removing Condition " + c.getId() + " - invalid clinicalStatus");
                                    return false;
                                }
                            }

                            if (c.hasVerificationStatus()) {
                                CodeableConcept cc = c.getVerificationStatus();
                                if (!cc.hasCoding(CONDITION_VERIFICATIONSTATUS_SYSTEM, "confirmed")) {
                                    logger.debug("removing Condition " + c.getId() + " - invalid verificationStatus");
                                    return false;
                                }
                            }
                        }

                        return true;
                    }
                }
        );
    }

    public Bundle getGoals(String sessionId) {
        logger.info("getting Goals for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        return fhirService.search(fcc, fhirQueryManager.getGoalQuery(fcc.getCredentials().getPatientId()),
                new Function<Resource, Boolean>() {
                    @Override
                    public Boolean apply(Resource resource) {
                        if (resource instanceof Goal) {
                            Goal g = (Goal) resource;

                            if (g.getLifecycleStatus() != Goal.GoalLifecycleStatus.ACTIVE) {
                                logger.debug("removing Goal " + g.getId() + " - invalid lifecycleStatus");
                                return false;
                            }

                            if (g.hasAchievementStatus()) {
                                CodeableConcept cc = g.getAchievementStatus();
                                if (!cc.hasCoding(
                                        GoalModel.ACHIEVEMENT_STATUS_CODING_SYSTEM,
                                        GoalModel.ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE)) {
                                    logger.debug("removing Goal " + g.getId() + " - invalid achievementStatus");
                                    return false;
                                }
                            }
                        }

                        return true;
                    }
                }
        );
    }

    public Bundle getMedicationStatements(String sessionId) {
        logger.info("getting MedicationStatements for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        return fhirService.search(fcc, fhirQueryManager.getMedicationStatementQuery(fcc.getCredentials().getPatientId()),
                new Function<Resource, Boolean>() {
                    @Override
                    public Boolean apply(Resource resource) {
                        if (resource instanceof MedicationStatement) {
                            MedicationStatement ms = (MedicationStatement) resource;
                            if (ms.getStatus() != MedicationStatement.MedicationStatementStatus.ACTIVE) {
                                logger.debug("removing MedicationStatement " + ms.getId() + " - invalid status");
                                return false;
                            }
                        }
                        return true;
                    }
                }
        );
    }

    public Bundle getMedicationRequests(String sessionId) {
        logger.info("getting MedicationRequests for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();

        Bundle bundle = fhirService.search(fcc, fhirQueryManager.getMedicationRequestQuery(fcc.getCredentials().getPatientId()),
                new Function<Resource, Boolean>() {
                    @Override
                    public Boolean apply(Resource resource) {
                        if (resource instanceof MedicationRequest) {
                            MedicationRequest mr = (MedicationRequest) resource;

                            if (mr.getStatus() != MedicationRequest.MedicationRequestStatus.ACTIVE) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - invalid status");
                                return false;
                            }

                            if (mr.getIntent() != MedicationRequest.MedicationRequestIntent.ORDER) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - invalid intent");
                                return false;
                            }

                            if (mr.hasDoNotPerform() && !mr.getDoNotPerform()) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - doNotPerform");
                                return false;
                            }
                        }

                        return true;
                    }
                }
        );

        if (bundle == null) return null;    // optional considering MedicationStatement

        if (bundle.hasEntry()) {
            // creating a separate bundle for Medications, instead of adding them directly to the main
            // bundle while iterating over it (below).  this prevents ConcurrentModificationException
            Bundle medicationBundle = new Bundle();
            medicationBundle.setType(Bundle.BundleType.COLLECTION);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof MedicationRequest) {
                    MedicationRequest mr = (MedicationRequest) entry.getResource();
                    if (mr.hasMedicationReference()) {
                        if ( ! FhirUtil.bundleContainsReference(bundle, mr.getMedicationReference()) ) {
                            Medication m = fhirService.readByReference(fcc, Medication.class, mr.getMedicationReference());
                            medicationBundle.addEntry(new Bundle.BundleEntryComponent().setResource(m));
                        }
                    }
                }
            }

            if (medicationBundle.hasEntry()) {
                bundle.getEntry().addAll(medicationBundle.getEntry());
            }
        }

        return bundle;
    }
}