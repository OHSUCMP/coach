package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.fhir.FhirQueryManager;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


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

    public Patient getPatient(String sessionId) {
        logger.info("getting Patient for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        Patient patient = fcc.readByReference(Patient.class,
                fhirQueryManager.getPatientLookup(fcc.getCredentials().getPatientId())
        );
        return patient;
    }

    public List<Encounter> getEncounters(String sessionId) {
        logger.info("getting Encounters for session=" + sessionId);

        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();

        Bundle bundle = fcc.search(fhirQueryManager.getEncounterQuery(
                fcc.getCredentials().getPatientId()
        ));

        List<Encounter> list = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Encounter) {
                    Encounter encounter = (Encounter) entry.getResource();
                    if (encounter.getStatus() == Encounter.EncounterStatus.FINISHED &&
                            (BloodPressureModel.isAmbEncounter(encounter) || BloodPressureModel.isHomeHealthEncounter(encounter))) {
                        list.add(encounter);
                    }
                }
            }
        }

        return list;
    }

    public Bundle getObservations(String sessionId, String code, Integer limit) {
        logger.info("getting " + code + " Observations for session=" + sessionId);

        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();

        Bundle bundle = fcc.search(fhirQueryManager.getObservationCodeQuery(
                fcc.getCredentials().getPatientId(),
                code, limit
        ), limit);

        if (bundle.hasEntry()) {
            Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();

                    // only allow "final", "amended", "corrected" status to pass

                    if (observation.getStatus() != Observation.ObservationStatus.FINAL &&
                            observation.getStatus() != Observation.ObservationStatus.AMENDED &&
                            observation.getStatus() != Observation.ObservationStatus.CORRECTED) {
                        logger.debug("removing Observation " + observation.getId() + " (invalid status)");
                        iter.remove();
                        continue;
                    }

                    if (!observation.hasEncounter()) {
                        logger.debug("removing Observation " + observation.getId() + " - no Encounter referenced");
                        iter.remove();
                    }
                }
            }
        }

        return bundle;
    }


    public Bundle getEncounterDiagnosisConditions(String sessionId) {
        return getConditions(sessionId, "encounter-diagnosis");
    }

    public Bundle getConditions(String sessionId, String category) {
        logger.info("getting " + category + " Conditions for session=" + sessionId);

        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();
        Bundle bundle = fcc.search(fhirQueryManager.getConditionQuery(
                fcc.getCredentials().getPatientId(),
                category
        ));

        // handle modifier flags
        Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            Resource r = entry.getResource();
            if (r instanceof Condition) {
                Condition c = (Condition) r;

                if (c.hasClinicalStatus()) {
                    // only allow "active", "recurrence", and "relapse" clinicalStatus to pass

                    CodeableConcept cc = c.getClinicalStatus();
                    if (!cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "active") &&
                            !cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "recurrence") &&
                            !cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "relapse")) {
                        logger.debug("removing Condition " + c.getId() + " (invalid clinicalStatus)");
                        iter.remove();
                        continue;
                    }
                }

                if (c.hasVerificationStatus()) {
                    // only allow "confirmed" verificationStatus to pass

                    CodeableConcept cc = c.getVerificationStatus();
                    if (!cc.hasCoding(CONDITION_VERIFICATIONSTATUS_SYSTEM, "confirmed")) {
                        logger.debug("removing Condition " + c.getId() + " (invalid verificationStatus)");
                        iter.remove();
                    }
                }
            }
        }

        return bundle;
    }

    public Bundle getGoals(String sessionId) {
        logger.info("getting Goals for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();

        Bundle bundle = fcc.search(fhirQueryManager.getGoalQuery(
                fcc.getCredentials().getPatientId()
        ));

        Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            Resource r = entry.getResource();
            if (r instanceof Goal) {
                Goal g = (Goal) r;

                if (g.getLifecycleStatus() != Goal.GoalLifecycleStatus.ACTIVE) {
                    // only allow "active" lifecycleStatus
                    logger.debug("removing Goal " + g.getId() + " (invalid lifecycleStatus)");
                    iter.remove();
                    continue;
                }

                if (g.hasAchievementStatus()) {
                    // if achievementStatus is set, require "in-progress"

                    CodeableConcept cc = g.getAchievementStatus();
                    if (!cc.hasCoding(
                            GoalModel.ACHIEVEMENT_STATUS_CODING_SYSTEM,
                            GoalModel.ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE)) {
                        logger.debug("removing Goal " + g.getId() + " (invalid achievementStatus)");
                        iter.remove();
                    }
                }
            }
        }

        return bundle;
    }

    public Bundle getMedicationStatements(String sessionId) {
        logger.info("getting MedicationStatements for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();

        Bundle bundle = fcc.search(fhirQueryManager.getMedicationStatementQuery(
                fcc.getCredentials().getPatientId()
        ));

        if (bundle == null) return null;    // optional considering MedicationRequest

        Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            Resource r = entry.getResource();
            if (r instanceof MedicationStatement) {
                MedicationStatement ms = (MedicationStatement) r;
                if (ms.getStatus() != MedicationStatement.MedicationStatementStatus.ACTIVE) {
                    // require "active" status
                    logger.debug("removing MedicationStatement " + ms.getId() + " (invalid status)");
                    iter.remove();
                }
            }
        }

        return bundle;
    }

    public Bundle getMedicationRequests(String sessionId) {
        logger.info("getting MedicationRequests for session=" + sessionId);
        FHIRCredentialsWithClient fcc = workspaceService.get(sessionId).getFhirCredentialsWithClient();

        Bundle bundle = fcc.search(fhirQueryManager.getMedicationRequestQuery(
                fcc.getCredentials().getPatientId()
        ));

        if (bundle == null) return null;    // optional considering MedicationStatement

        Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            Resource r = entry.getResource();
            if (r instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) r;

                if (mr.getStatus() != MedicationRequest.MedicationRequestStatus.ACTIVE) {
                    logger.debug("removing MedicationRequest " + mr.getId() + " (invalid status)");
                    iter.remove();
                    continue;
                }

                if (mr.getIntent() != MedicationRequest.MedicationRequestIntent.ORDER) {
                    logger.debug("removing MedicationRequest " + mr.getId() + " (invalid intent)");
                    iter.remove();
                    continue;
                }

                if (mr.hasDoNotPerform() && !mr.getDoNotPerform()) {
                    logger.debug("removing MedicationRequest " + mr.getId() + " (doNotPerform)");
                    iter.remove();
                }
            }
        }

        // creating a separate bundle for Medications, instead of adding them directly to the main
        // bundle while iterating over it (below).  this prevents ConcurrentModificationException
        Bundle medicationBundle = new Bundle();
        medicationBundle.setType(Bundle.BundleType.COLLECTION);

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) entry.getResource();
                if (mr.hasMedicationReference()) {
                    if ( ! FhirUtil.bundleContainsReference(bundle, mr.getMedicationReference()) ) {
                        Medication m = fcc.readByReference(Medication.class, mr.getMedicationReference());
                        medicationBundle.addEntry(new Bundle.BundleEntryComponent().setResource(m));
                    }
                }
            }
        }

        if (medicationBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : medicationBundle.getEntry()) {
                bundle.addEntry(entry);
            }
        }

        return bundle;
    }
}