package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.MedicationForm;
import edu.ohsu.cmp.coach.entity.MedicationRoute;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
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
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
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

    @Autowired
    private FHIRService fhirService;

    @Autowired
    private MedicationFormRepository medicationFormRepository;

    @Autowired
    private MedicationRouteRepository medicationRouteRepository;

    public Patient getPatient(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting Patient for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.readByReference(fcc, workspace.getFhirQueryManager().getPatientStrategy(), Patient.class,
                workspace.getVendorTransformer().getPatientLookup(fcc.getCredentials().getPatientId())
        );
    }

    public List<Encounter> getEncounters(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting Encounters for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Bundle bundle = fhirService.search(fcc, workspace.getFhirQueryManager().getEncounterStrategy(),
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
                                logger.debug("removing Encounter {} - invalid status", encounter.getId());
                                return false;
                            }

                            EncounterMatcher matcher = new EncounterMatcher(fcm, true);
                            boolean isOffice = matcher.isOfficeEncounter(encounter);
                            boolean isHome = matcher.isHomeEncounter(encounter);
                            if ( ! isOffice && ! isHome ) {
                                logger.debug("removing Encounter {} - not Office or Home", encounter.getId());
                                return false;

                            } else {
                                logger.debug("keeping Encounter {} (Office={}, Home={})", encounter.getId(), isOffice, isHome);
                            }
                        }

                        return true;
                    }
                }, null
        );

        List<Encounter> list = new ArrayList<>();

        if (bundle != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Encounter bundle = {}", FhirUtil.toJson(bundle));
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
    public Bundle getObservations(String sessionId, String code, String lookbackPeriod,
                                  @Nullable Integer limit) throws DataException, ConfigurationException, IOException {
        logger.info("getting Observations for session={} having code(s): {}", sessionId, code);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc, workspace.getFhirQueryManager().getObservationStrategy(),
                workspace.getVendorTransformer().getObservationQuery(fcc.getCredentials().getPatientId(), code, lookbackPeriod),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof Observation) {
                            Observation observation = (Observation) resource;
                            if (observation.getStatus() != Observation.ObservationStatus.FINAL &&
                                    observation.getStatus() != Observation.ObservationStatus.AMENDED &&
                                    observation.getStatus() != Observation.ObservationStatus.CORRECTED) {
                                logger.debug("removing Observation {} - invalid status", observation.getId());
                                return false;
                            }

// storer 2022-08-15 - can now handle observations that don't have associated encounters, as janky as that might be
//                            if (!observation.hasEncounter()) {
//                                logger.debug("removing Observation {} - no Encounter referenced", observation.getId());
//                                return false;
//                            }
                        }

                        return true;
                    }
                }, null
        );
    }

    public Bundle getEncounterDiagnosisConditions(String sessionId) throws DataException, ConfigurationException, IOException {
        return getConditions(sessionId, "encounter-diagnosis");
    }

    public Bundle getProblemListConditions(String sessionId) throws DataException, ConfigurationException, IOException {
        return getConditions(sessionId, "problem-list-item");
    }

    public Bundle getConditions(String sessionId, String category) throws DataException, ConfigurationException, IOException {
        logger.info("getting {} Conditions for session={}", category, sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc, workspace.getFhirQueryManager().getConditionStrategy(),
                workspace.getVendorTransformer().getConditionQuery(fcc.getCredentials().getPatientId(), category),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        // #114: Epic returns this additional resource that keeps us from accurately counting conditions and inserting a fake one when needed
                        if (resource instanceof OperationOutcome) {
                            logger.debug("removing OperationOutcome from response to Condition search");
                            return false;
                        }
                        if (resource instanceof Condition) {
                            Condition c = (Condition) resource;

                            if (c.hasClinicalStatus()) {
                                CodeableConcept cc = c.getClinicalStatus();
                                if (!cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "active") &&
                                        !cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "recurrence") &&
                                        !cc.hasCoding(CONDITION_CLINICALSTATUS_SYSTEM, "relapse")) {
                                    logger.debug("removing Condition {} - invalid clinicalStatus", c.getId());
                                    return false;
                                }
                            }

                            if (c.hasVerificationStatus()) {
                                CodeableConcept cc = c.getVerificationStatus();
                                if (!cc.hasCoding(CONDITION_VERIFICATIONSTATUS_SYSTEM, "confirmed")) {
                                    logger.debug("removing Condition {} - invalid verificationStatus", c.getId());
                                    return false;
                                }
                            }
                        }

                        return true;
                    }
                }, null
        );
    }

    public Bundle getGoals(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting Goals for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc, workspace.getFhirQueryManager().getGoalStrategy(),
                workspace.getVendorTransformer().getGoalQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof Goal) {
                            Goal g = (Goal) resource;

                            if (g.getLifecycleStatus() != Goal.GoalLifecycleStatus.ACTIVE) {
                                logger.debug("removing Goal {} - invalid lifecycleStatus", g.getId());
                                return false;
                            }

                            if (g.hasAchievementStatus()) {
                                CodeableConcept cc = g.getAchievementStatus();
                                if (!cc.hasCoding(
                                        GoalModel.ACHIEVEMENT_STATUS_CODING_SYSTEM,
                                        GoalModel.ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE)) {
                                    logger.debug("removing Goal {} - invalid achievementStatus", g.getId());
                                    return false;
                                }
                            }
                        }

                        return true;
                    }
                }, null
        );
    }

    public Bundle getOrderServiceRequests(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting Order ServiceRequests for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc, workspace.getFhirQueryManager().getServiceRequestStrategy(),
                workspace.getVendorTransformer().getServiceRequestQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof ServiceRequest) {
                            ServiceRequest sr = (ServiceRequest) resource;

                            if (sr.getStatus() != ServiceRequest.ServiceRequestStatus.ACTIVE) {
                                logger.debug("removing ServiceRequest {} - invalid Status", sr.getId());
                                return false;
                            }

                            if (sr.getIntent() != ServiceRequest.ServiceRequestIntent.ORDER &&
                                    sr.getIntent() != ServiceRequest.ServiceRequestIntent.ORIGINALORDER &&
                                    sr.getIntent() != ServiceRequest.ServiceRequestIntent.REFLEXORDER &&
                                    sr.getIntent() != ServiceRequest.ServiceRequestIntent.FILLERORDER &&
                                    sr.getIntent() != ServiceRequest.ServiceRequestIntent.INSTANCEORDER) {
                                logger.debug("removing ServiceRequest {} - invalid Intent", sr.getId());
                                return false;
                            }

                            if (sr.hasDoNotPerform() && sr.getDoNotPerform()) {
                                logger.debug("removing ServiceRequest {} - doNotPerform", sr.getId());
                                return false;
                            }
                        }

                        return true;
                    }
                }, null
        );
    }

    public Bundle getMedicationStatements(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting MedicationStatements for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();

        final List<Coding> validRouteCodings = getValidMedicationRouteCodings();
        final List<Coding> validFormCodings = getValidMedicationFormCodings();

        Bundle bundle = fhirService.search(fcc, workspace.getFhirQueryManager().getMedicationStatementStrategy(),
                workspace.getVendorTransformer().getMedicationStatementQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof MedicationStatement) {
                            MedicationStatement ms = (MedicationStatement) resource;
                            if (ms.getStatus() != MedicationStatement.MedicationStatementStatus.ACTIVE) {
                                logger.debug("removing MedicationStatement {} - invalid status", ms.getId());
                                return false;
                            }

                            boolean hasGoodRoute = false;
                            boolean hasGoodForm = false;

                            if (ms.hasDosage()) {
                                for (Dosage d : ms.getDosage()) {
                                    if (d.hasRoute() && FhirUtil.hasCoding(d.getRoute(), validRouteCodings)) {
                                        hasGoodRoute = true;
                                        break;
                                    }
                                }
                            }

                            if ( ! hasGoodRoute && ms.hasMedicationReference()) {
                                logger.debug("invalid or missing route for MedicationStatement {} - checking medication form", ms.getId());
                                Bundle bundle = resourceWithBundle.getBundle();

                                Medication m = null;
                                try {
                                    m = FhirUtil.getResourceFromContainedOrBundleByReference(ms, bundle, Medication.class, ms.getMedicationReference());
                                    if (m == null) {
                                        m = fhirService.readByReference(fcc, workspace.getFhirQueryManager().getMedicationStrategy(),
                                                Medication.class, ms.getMedicationReference());
                                    }

                                } catch (Exception e) {
                                    logger.error("caught {} attempting to obtain Medication for MedicationStatement {} - {}",
                                            e.getClass().getName(), ms.getId(), e.getMessage(), e);
                                }

                                if (m != null && m.hasForm() && FhirUtil.hasCoding(m.getForm(), validFormCodings)) {
                                    hasGoodForm = true;
                                }
                            }

                            if ( ! hasGoodRoute && ! hasGoodForm ) {
                                logger.debug("removing MedicationStatement {} - invalid route and form", ms.getId());
                                return false;
                            }
                        }
                        return true;
                    }
                }, null
        );

        if (bundle == null) return null;    // optional considering MedicationRequest

        if (bundle.hasEntry()) {
            // creating a separate bundle for Medications, instead of adding them directly to the main
            // bundle while iterating over it (below).  this prevents ConcurrentModificationException
            Bundle medicationBundle = new Bundle();
            medicationBundle.setType(Bundle.BundleType.COLLECTION);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof MedicationStatement) {
                    MedicationStatement ms = (MedicationStatement) entry.getResource();
                    if (ms.hasMedicationReference()) {
                        try {
                            if ( ! FhirUtil.resourceOrBundleContainsReference(ms, bundle, ms.getMedicationReference()) ) {
                                Medication m = fhirService.readByReference(fcc, workspace.getFhirQueryManager().getMedicationStrategy(),
                                        Medication.class, ms.getMedicationReference());
                                if (m != null) {
                                    medicationBundle.addEntry(new Bundle.BundleEntryComponent().setResource(m));
                                }
                            }

                        } catch (Exception e) {
                            logger.error("caught {} attempting to obtain Medication for MedicationStatement {} - {}",
                                    e.getClass().getName(), ms.getId(), e.getMessage(), e);
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

    public Bundle getMedicationRequests(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting MedicationRequests for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();

        final List<Coding> validRouteCodings = getValidMedicationRouteCodings();
        final List<Coding> validFormCodings = getValidMedicationFormCodings();

        Bundle bundle = fhirService.search(fcc, workspace.getFhirQueryManager().getMedicationRequestStrategy(),
                workspace.getVendorTransformer().getMedicationRequestQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof MedicationRequest) {
                            MedicationRequest mr = (MedicationRequest) resource;

                            if (mr.getStatus() != MedicationRequest.MedicationRequestStatus.ACTIVE) {
                                logger.debug("removing MedicationRequest {} - invalid status", mr.getId());
                                return false;
                            }

                            if (mr.getIntent() != MedicationRequest.MedicationRequestIntent.ORDER &&
                                    mr.getIntent() != MedicationRequest.MedicationRequestIntent.PLAN) {
                                logger.debug("removing MedicationRequest {} - invalid intent", mr.getId());
                                return false;
                            }

                            if (mr.hasDoNotPerform() && mr.getDoNotPerform()) {
                                logger.debug("removing MedicationRequest {} - doNotPerform", mr.getId());
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

                            if ( ! hasGoodRoute && mr.hasMedicationReference() ) {
                                logger.debug("invalid or missing route for MedicationRequest {} - checking medication form", mr.getId());
                                Bundle bundle = resourceWithBundle.getBundle();

                                Medication m = null;
                                try {
                                    m = FhirUtil.getResourceFromContainedOrBundleByReference(mr, bundle, Medication.class, mr.getMedicationReference());
                                    if (m == null) {
                                        m = fhirService.readByReference(fcc, workspace.getFhirQueryManager().getMedicationStrategy(),
                                                Medication.class, mr.getMedicationReference());
                                    }

                                } catch (Exception e) {
                                    logger.error("caught {} attempting to obtain Medication for MedicationRequest {} - {}",
                                            e.getClass().getName(), mr.getId(), e.getMessage(), e);
                                }

                                if (m != null && m.hasForm() && FhirUtil.hasCoding(m.getForm(), validFormCodings)) {
                                    hasGoodForm = true;
                                }
                            }

                            if ( ! hasGoodRoute && ! hasGoodForm ) {
                                logger.debug("removing MedicationRequest {} - invalid route and form", mr.getId());
                                return false;
                            }
                        }

                        return true;
                    }
                }, null
        );

        if (bundle == null) return null;    // optional considering MedicationStatement

        if (bundle.hasEntry()) {
            // creating a separate bundle for Medications, instead of adding them directly to the main
            // bundle while iterating over it (below).  this prevents ConcurrentModificationException
            Bundle medicationBundle = new Bundle();
            medicationBundle.setType(Bundle.BundleType.COLLECTION);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof MedicationRequest) {
                    MedicationRequest mr = (MedicationRequest) entry.getResource();
                    if (mr.hasMedicationReference()) {
                        try {
                            if ( ! FhirUtil.resourceOrBundleContainsReference(mr, bundle, mr.getMedicationReference()) ) {
                                Medication m = fhirService.readByReference(fcc, workspace.getFhirQueryManager().getMedicationStrategy(),
                                        Medication.class, mr.getMedicationReference());
                                if (m != null) {
                                    medicationBundle.addEntry(new Bundle.BundleEntryComponent().setResource(m));
                                }
                            }

                        } catch (Exception e) {
                            logger.error("caught {} attempting to obtain Medication for MedicationRequest {} - {}",
                                    e.getClass().getName(), mr.getId(), e.getMessage(), e);
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

    public Bundle getCounselingProcedures(String sessionId) throws DataException, ConfigurationException, IOException {
        logger.info("getting Counseling Procedures for session={}", sessionId);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.search(fcc, workspace.getFhirQueryManager().getProcedureStrategy(),
                workspace.getVendorTransformer().getProcedureQuery(fcc.getCredentials().getPatientId()),
                new Function<ResourceWithBundle, Boolean>() {
                    @Override
                    public Boolean apply(ResourceWithBundle resourceWithBundle) {
                        Resource resource = resourceWithBundle.getResource();
                        if (resource instanceof Procedure) {
                            Procedure p = (Procedure) resource;
                            if (p.getStatus() != Procedure.ProcedureStatus.COMPLETED) {
                                logger.debug("removing Procedure {} - invalid status", p.getId());
                                return false;
                            }

                            if ( ! p.hasCategory() ) {
                                logger.debug("removing Procedure {} - no category", p.getId());
                                return false;

                            } else if ( ! FhirUtil.hasCoding(p.getCategory(), fcm.getProcedureCounselingCoding()) ) {
                                logger.debug("removing Procedure {} - invalid category", p.getId());
                                return false;
                            }
                        }

                        return true;
                    }
                }, null
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