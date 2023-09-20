package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.MedicationForm;
import edu.ohsu.cmp.coach.entity.MedicationRoute;
import edu.ohsu.cmp.coach.fhir.EncounterMatcher;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.ResourceWithBundle;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.repository.MedicationFormRepository;
import edu.ohsu.cmp.coach.repository.MedicationRouteRepository;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
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
public class EHRService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int MAX_CODES_PER_QUERY = 32; // todo: auto-identify this, or at least put it in the config

    private static final String CONDITION_CLINICALSTATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String CONDITION_VERIFICATIONSTATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-ver-status";

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private FHIRService fhirService;

    @Autowired
    private MedicationFormRepository medicationFormRepository;

    @Autowired
    private MedicationRouteRepository medicationRouteRepository;

    public Patient getPatient(String sessionId) {
        logger.info("getting Patient for session=" + sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.readByReference(fcc, Patient.class,
                workspace.getVendorTransformer().getPatientLookup(fcc.getCredentials().getPatientId())
        );
    }

    public List<Encounter> getEncounters(String sessionId) {
        logger.info("getting Encounters for session=" + sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Bundle bundle = fhirService.search(fcc,
                workspace.getVendorTransformer().getEncounterQuery(fcc.getCredentials().getPatientId(), fcm.getEncounterLookbackPeriod()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof Encounter) {
                            Encounter encounter = (Encounter) resource;
                            if (encounter.getStatus() != Encounter.EncounterStatus.FINISHED &&
                                encounter.getStatus() != Encounter.EncounterStatus.ARRIVED &&
                                encounter.getStatus() != Encounter.EncounterStatus.TRIAGED &&
                                encounter.getStatus() != Encounter.EncounterStatus.INPROGRESS) {
                                logger.debug("removing Encounter " + encounter.getId() + " - invalid status");
                                return false;
                            }

                            EncounterMatcher matcher = new EncounterMatcher(fcm, true);
                            boolean isOffice = matcher.isOfficeEncounter(encounter);
                            boolean isHome = matcher.isHomeEncounter(encounter);
                            if ( ! isOffice && ! isHome ) {
                                logger.debug("removing Encounter " + encounter.getId() + " - not Office or Home");
                                return false;

                            } else {
                                logger.debug("keeping Encounter " + encounter.getId() + " (Office=" + isOffice + ", Home=" + isHome + ")");
                            }
                        }

                        return true;
                    }
                }
        );

        List<Encounter> list = new ArrayList<>();

        if (bundle != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Encounter bundle = " + FhirUtil.toJson(bundle));
            }

            if (bundle.hasEntry()) {
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    if (entry.hasResource() && entry.getResource() instanceof Encounter) {
                        list.add((Encounter) entry.getResource());
                    }
                }
            }
        }

        return list;
    }

    /**
     * @param sessionId
     * @param code a comma-separated list of one or more strings of the form "system|code"
     *             e.g. "http://loinc.org|55284-4"
     *             e.g. "http://loinc.org|55284|4,http://loinc.org|72076-3
     * @param lookbackPeriod
     * @param limit specifies a maximum number of search results to return.  May be null
     * @return
     */
    public Bundle getObservations(String sessionId, String code, String lookbackPeriod, @Nullable Integer limit) {
        logger.info("getting Observations for session=" + sessionId + " having code(s): " + code);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc,
                workspace.getVendorTransformer().getObservationCodeQuery(fcc.getCredentials().getPatientId(), code, lookbackPeriod),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof Observation) {
                            Observation observation = (Observation) resource;
                            if (observation.getStatus() != Observation.ObservationStatus.FINAL &&
                                    observation.getStatus() != Observation.ObservationStatus.AMENDED &&
                                    observation.getStatus() != Observation.ObservationStatus.CORRECTED) {
                                logger.debug("removing Observation " + observation.getId() + " - invalid status");
                                return false;
                            }

// storer 2022-08-15 - can now handle observations that don't have associated encounters, as janky as that might be
//                            if (!observation.hasEncounter()) {
//                                logger.debug("removing Observation " + observation.getId() + " - no Encounter referenced");
//                                return false;
//                            }
                        }

                        return true;
                    }
        });
    }

    public Bundle getEncounterDiagnosisConditions(String sessionId) {
        return getConditions(sessionId, "encounter-diagnosis");
    }

    public Bundle getProblemListConditions(String sessionId) {
        return getConditions(sessionId, "problem-list-item");
    }

    public Bundle getConditions(String sessionId, String category) {
        logger.info("getting " + category + " Conditions for session=" + sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc,
                workspace.getVendorTransformer().getConditionQuery(fcc.getCredentials().getPatientId(), category),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
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
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc,
                workspace.getVendorTransformer().getGoalQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
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
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc,
                workspace.getVendorTransformer().getMedicationStatementQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
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
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();

        final List<Coding> validRouteCodings = getValidMedicationRouteCodings();
        final List<Coding> validFormCodings = getValidMedicationFormCodings();

        Bundle bundle = fhirService.search(fcc,
                workspace.getVendorTransformer().getMedicationRequestQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof MedicationRequest) {
                            MedicationRequest mr = (MedicationRequest) resource;

                            if (mr.getStatus() != MedicationRequest.MedicationRequestStatus.ACTIVE) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - invalid status");
                                return false;
                            }

                            if (mr.getIntent() != MedicationRequest.MedicationRequestIntent.ORDER &&
                                    mr.getIntent() != MedicationRequest.MedicationRequestIntent.PLAN) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - invalid intent");
                                return false;
                            }

                            if (mr.hasDoNotPerform() && mr.getDoNotPerform()) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - doNotPerform");
                                return false;
                            }

                            boolean hasGoodRoute = false;
                            boolean hasGoodForm = false;

                            if (mr.hasDosageInstruction()) {
                                for (Dosage d : mr.getDosageInstruction()) {
                                    if (d.hasRoute() && FhirUtil.hasCoding(d.getRoute(), validRouteCodings)) {
                                        hasGoodRoute = true;
                                        break;
                                    }
                                }
                            }

                            if ( ! hasGoodRoute && mr.hasMedicationReference()) {
                                logger.debug("invalid or missing route for MedicationRequest " + mr.getId() + " - checking medication form");
                                Bundle bundle = resourceWithBundle.getBundle();
                                Medication m = FhirUtil.getResourceFromBundleByReference(bundle, Medication.class, mr.getMedicationReference().getReference());
                                if (m == null) {
                                    logger.warn("couldn't find Medication with reference=" + mr.getMedicationReference().getReference() +
                                            " - attempting to read from FHIR server - ");
                                    m = fhirService.readByReference(fcc, Medication.class, mr.getMedicationReference());
                                }
                                if (m != null && m.hasForm() && FhirUtil.hasCoding(m.getForm(), validFormCodings)) {
                                    hasGoodForm = true;
                                }
                            }

                            if ( ! hasGoodRoute && ! hasGoodForm ) {
                                logger.debug("removing MedicationRequest " + mr.getId() + " - invalid route and form");
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

    public Bundle getCounselingProcedures(String sessionId) {
        logger.info("getting Counseling Procedures for session=" + sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc,
                workspace.getVendorTransformer().getProcedureQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof Procedure) {
                            Procedure p = (Procedure) resource;
                            if (p.getStatus() != Procedure.ProcedureStatus.COMPLETED) {
                                logger.debug("removing Procedure " + p.getId() + " - invalid status");
                                return false;
                            }

                            if ( ! p.hasCategory() ) {
                                logger.debug("removing Procedure " + p.getId() + " - no category");
                                return false;

                            } else if ( ! FhirUtil.hasCoding(p.getCategory(), fcm.getProcedureCounselingCoding()) ) {
                                logger.debug("removing Procedure " + p.getId() + " - invalid category");
                                return false;
                            }
                        }

                        return true;
                    }
                }
        );
    }

    private List<Coding> getValidMedicationRouteCodings() {
        List<Coding> list = new ArrayList<>();

        for (MedicationRoute mr : medicationRouteRepository.findAll()) {
            Coding c = new Coding()
                    .setCode(mr.getConceptCode())
                    .setSystem(mr.getConceptSystem())
                    .setDisplay(mr.getDescription());
            list.add(c);
        }

        return list;
    }

    private List<Coding> getValidMedicationFormCodings() {
        List<Coding> list = new ArrayList<>();

        for (MedicationForm mf : medicationFormRepository.findAll()) {
            Coding c = new Coding()
                    .setCode(mf.getConceptCode())
                    .setSystem(mf.getConceptSystem())
                    .setDisplay(mf.getDescription());
            list.add(c);
        }

        return list;
    }
}