package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEvent;
import edu.ohsu.cmp.htnu18app.entity.vsac.Concept;
import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
import edu.ohsu.cmp.htnu18app.fhir.QueryManager;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.GoalModel;
import edu.ohsu.cmp.htnu18app.model.MedicationModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Service
public class EHRService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int MAX_CODES_PER_QUERY = 32; // todo: auto-identify this, or at least put it in the config

    @Autowired
    private QueryManager queryManager;

    @Autowired
    private ValueSetService valueSetService;

    @Autowired
    private AdverseEventService adverseEventService;

    public Patient getPatient(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Patient p = cache.getPatient();
        if (p == null) {
            logger.info("requesting Patient data for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            p = fcc.read(Patient.class, queryManager.getPatientLookup(fcc.getCredentials().getPatientId()));
            cache.setPatient(p);
        }
        return p;
    }

    public Bundle getBloodPressureObservations(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Bundle b = cache.getObservations();
        if (b == null) {
            logger.info("requesting Blood Pressure Observations for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.search(queryManager.getObservationQueryCode(
                    fcc.getCredentials().getPatientId(),
                    BloodPressureModel.SYSTEM,
                    BloodPressureModel.CODE
            ));

            cache.setObservations(b);
        }
        return b;
    }

    public Bundle getConditions(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Bundle b = cache.getConditions();
        if (b == null) {
            logger.info("requesting Conditions for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.search(queryManager.getConditionQuery(fcc.getCredentials().getPatientId()));

//            List<String> hypertensionValueSetOIDs = new ArrayList<>();
//            hypertensionValueSetOIDs.add("2.16.840.1.113883.3.3157.4012");
//            hypertensionValueSetOIDs.add("2.16.840.1.113762.1.4.1032.10");
//
//            Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
//            while (iter.hasNext()) {
//                Bundle.BundleEntryComponent item = iter.next();
//                if (item.getResource() instanceof Condition) {
//                    Condition c = (Condition) item.getResource();
//
//                    // todo : filter conditions down to only those that have codings associated with the indicated value sets
//                }
//            }

            cache.setConditions(b);
        }
        return b;
    }

    public Bundle getCurrentGoals(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Bundle b = cache.getCurrentGoals();
        if (b == null) {
            logger.info("requesting Goals for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.search(queryManager.getGoalQuery(fcc.getCredentials().getPatientId()));

            // filter out any resources that aren't Active, In-Progress Goals
            Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent btc = iter.next();
                boolean active = false;
                boolean inProgress = false;
                if (btc.getResource() instanceof org.hl7.fhir.r4.model.Goal) {
                    org.hl7.fhir.r4.model.Goal g = (org.hl7.fhir.r4.model.Goal) btc.getResource();
                    active = g.getLifecycleStatus() == org.hl7.fhir.r4.model.Goal.GoalLifecycleStatus.ACTIVE;
                    inProgress = g.getAchievementStatus().hasCoding(
                            GoalModel.ACHIEVEMENT_STATUS_CODING_SYSTEM,
                            GoalModel.ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE
                    );
                }
                if ( ! active || ! inProgress ) {
                    iter.remove();
                }
            }

            cache.setCurrentGoals(b);
        }

        return b;
    }

    @Transactional
    public Bundle getMedications(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle b = cache.getMedications();
        if (b == null) {
            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = getMedicationStatements(fcc);
            if (b == null) b = getMedicationRequests(fcc);
            cache.setMedications(b);
        }

        return b;
    }

    private Bundle getMedicationStatements(FHIRCredentialsWithClient fcc) {
        Bundle b = fcc.search(queryManager.getMedicationStatementQuery(fcc.getCredentials().getPatientId()));
        if (b == null) return null;

        // build concept info as a simple set we can query to test inclusion
        // (these are the meds we want to show)
        ValueSet valueSet = valueSetService.getValueSet(MedicationModel.VALUE_SET_OID);
        Set<String> concepts = new HashSet<>();
        for (Concept c : valueSet.getConcepts()) {
            String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
            concepts.add(codeSystem + "|" + c.getCode());
        }

        // filter out any of the patient's meds that aren't included in set we want to show
        Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            boolean exists = false;
            if (entry.getResource() instanceof MedicationStatement) {
                MedicationStatement ms = (MedicationStatement) entry.getResource();
                CodeableConcept cc = ms.getMedicationCodeableConcept();
                for (Coding c : cc.getCoding()) {
                    if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                        exists = true;
                        break;
                    }
                }
            }
            if ( ! exists ) {
                iter.remove();
            }
        }

        return b;
    }

    private Bundle getMedicationRequests(FHIRCredentialsWithClient fcc) {
        Bundle b = fcc.search(queryManager.getMedicationRequestQuery(fcc.getCredentials().getPatientId()));
        if (b == null) return null;

        // build concept info as a simple set we can query to test inclusion
        // (these are the meds we want to show)
        ValueSet valueSet = valueSetService.getValueSet(MedicationModel.VALUE_SET_OID);
        Set<String> concepts = new HashSet<>();
        for (Concept c : valueSet.getConcepts()) {
            String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
            concepts.add(codeSystem + "|" + c.getCode());
        }

        // filter out any of the patient's meds that aren't included in set we want to show
        Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            boolean exists = false;
            if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) entry.getResource();
                if (mr.hasMedicationCodeableConcept()) {
                    CodeableConcept cc = mr.getMedicationCodeableConcept();
                    for (Coding c : cc.getCoding()) {
                        if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                            exists = true;
                            break;
                        }
                    }

                } else if (mr.hasMedicationReference()) {
                    Medication m = fcc.read(Medication.class, mr.getMedicationReference().getReference());
                    for (Coding c : m.getCode().getCoding()) {
                        if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                            exists = true;
                            break;
                        }
                    }
                }
            }
            if ( ! exists ) {
                iter.remove();
            }
        }

        return b;
    }

    public Bundle getAdverseEventConditions(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle b = cache.getAdverseEvents();
        if (b == null) {
            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.search(queryManager.getAdverseEventQuery(fcc.getCredentials().getPatientId()));
            if (b == null) return null;

            Set<String> codesWeCareAbout = new HashSet<String>();
            for (MyAdverseEvent mae : adverseEventService.getAll()) {
                codesWeCareAbout.add(mae.getConceptSystem() + "|" + mae.getConceptCode());
            }

            // filter out any of the patient's conditions that don't match a code we're interested in
            Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                boolean exists = false;
                if (entry.getResource() instanceof Condition) {
                    Condition c = (Condition) entry.getResource();
                    if (c.getCode().hasCoding()) {
                        for (Coding coding : c.getCode().getCoding()) {
                            String item = coding.getSystem() + "|" + coding.getCode();
                            if (codesWeCareAbout.contains(item)) {
                                exists = true;
                                break;
                            }
                        }
                    }
                }
                if ( ! exists ) {
                    iter.remove();
                }
            }

            cache.setAdverseEvents(b);
        }

        return b;
    }
}
