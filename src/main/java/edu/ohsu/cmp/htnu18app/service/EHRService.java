package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.vsac.Concept;
import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
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
    private ValueSetService valueSetService;

    public Patient getPatient(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Patient p = cache.getPatient();
        if (p == null) {
            logger.info("requesting Patient data for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            p = fcc.getClient()
                    .read()
                    .resource(Patient.class)
                    .withId(fcc.getCredentials().getPatientId())
                    .execute();
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
            b = fcc.getClient()
                    .search()
                    .forResource(Observation.class)
                    .and(Observation.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .and(Observation.CODE.exactly().systemAndCode(BloodPressureModel.SYSTEM, BloodPressureModel.CODE))
//                    .include(new Include("Observation:encounter"))
//                    .include(new Include("Observation:partOf"))
                    .returnBundle(Bundle.class)
                    .execute();
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
            b = fcc.getClient()
                    .search()
                    .forResource(Condition.class)
                    .and(Condition.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .returnBundle(Bundle.class)
                    .execute();

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
            b = fcc.getClient()
                    .search()
                    .forResource(Goal.class)
                    .and(Goal.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .returnBundle(Bundle.class)
                    .execute();

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
    public Bundle getMedicationStatements(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle b = cache.getMedicationStatements();
        if (b == null) {
            logger.info("requesting MedicationStatements for session " + sessionId);

            // build concept info as a simple set we can query to test inclusion
            // (these are the meds we want to show)
            ValueSet valueSet = valueSetService.getValueSet(MedicationModel.VALUE_SET_OID);
            Set<String> concepts = new HashSet<>();
            for (Concept c : valueSet.getConcepts()) {
                String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
                concepts.add(codeSystem + "|" + c.getCode());
            }

            // get all the patient's meds (yes, all of them)
            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
//            b = new FHIRQuery(fcc).queryByValueSet(MedicationStatement.class, valueSet);
            b = fcc.getClient()
                    .search()
                    .forResource(MedicationStatement.class)
                    .and(MedicationStatement.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .returnBundle(Bundle.class)
                    .execute();

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

            cache.setMedicationStatements(b);
        }

        return b;
    }

    @Transactional
    public Bundle getMedicationRequests(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle b = cache.getMedicationRequests();
        if (b == null) {
            logger.info("requesting MedicationRequests for session " + sessionId);

            // build concept info as a simple set we can query to test inclusion
            // (these are the meds we want to show)
            ValueSet valueSet = valueSetService.getValueSet(MedicationModel.VALUE_SET_OID);
            Set<String> concepts = new HashSet<>();
            for (Concept c : valueSet.getConcepts()) {
                String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
                concepts.add(codeSystem + "|" + c.getCode());
            }

            // get all the patient's meds (yes, all of them)
            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.getClient()
                    .search()
                    .forResource(MedicationRequest.class)
                    .and(MedicationRequest.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .returnBundle(Bundle.class)
                    .execute();

            // filter out any of the patient's meds that aren't included in set we want to show
            Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                boolean exists = false;
                if (entry.getResource() instanceof MedicationRequest) {
                    MedicationRequest mr = (MedicationRequest) entry.getResource();
                    CodeableConcept cc = mr.getMedicationCodeableConcept();
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

            cache.setMedicationRequests(b);
        }

        return b;
    }
}
