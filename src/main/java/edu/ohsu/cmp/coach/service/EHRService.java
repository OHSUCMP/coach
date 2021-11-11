package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.cache.CacheData;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.entity.app.MyAdverseEvent;
import edu.ohsu.cmp.coach.entity.app.MyAdverseEventOutcome;
import edu.ohsu.cmp.coach.fhir.FhirQueryManager;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class EHRService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int MAX_CODES_PER_QUERY = 32; // todo: auto-identify this, or at least put it in the config

    private static final Date ONE_MONTH_AGO;
    static {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        ONE_MONTH_AGO = cal.getTime();
    }

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private FhirQueryManager fhirQueryManager;

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
            p = fcc.read(Patient.class, fhirQueryManager.getPatientLookup(fcc.getCredentials().getPatientId()));
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
            b = fcc.search(fhirQueryManager.getObservationQueryCode(
                    fcc.getCredentials().getPatientId(),
                    fcm.getBpSystem(), fcm.getBpCode()
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
            b = fcc.search(fhirQueryManager.getConditionQuery(fcc.getCredentials().getPatientId()));

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
            b = fcc.search(fhirQueryManager.getGoalQuery(fcc.getCredentials().getPatientId()));

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
                if (!active || !inProgress) {
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
        return fcc.search(fhirQueryManager.getMedicationStatementQuery(fcc.getCredentials().getPatientId()));
    }

    private Bundle getMedicationRequests(FHIRCredentialsWithClient fcc) {
        Bundle b = fcc.search(fhirQueryManager.getMedicationRequestQuery(fcc.getCredentials().getPatientId()));
        if (b == null) return null;

        // creating a separate bundle for Medications, instead of adding them directly to the main
        // bundle while iterating over it (below).  this prevents ConcurrentModificationException
        Bundle medicationBundle = new Bundle();
        medicationBundle.setType(Bundle.BundleType.COLLECTION);

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) entry.getResource();
                if (mr.hasMedicationReference()) {
                    if ( ! FhirUtil.bundleContainsReference(b, mr.getMedicationReference().getReference()) ) {
                        Medication m = fcc.read(Medication.class, mr.getMedicationReference().getReference());
                        medicationBundle.addEntry(new Bundle.BundleEntryComponent().setResource(m));
                    }
                }
            }
        }

        if (medicationBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : medicationBundle.getEntry()) {
                b.addEntry(entry);
            }
        }

        return b;
    }

    public Bundle getAdverseEvents(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle b = cache.getAdverseEvents();
        if (b == null) {
            b = buildAdverseEvents(sessionId);
            cache.setAdverseEvents(b);
        }

        return b;
    }

    private Bundle buildAdverseEvents(String sessionId) {
        Bundle b = new Bundle();
        b.setType(Bundle.BundleType.COLLECTION);

        for (Bundle.BundleEntryComponent entry : getAdverseEventConditions(sessionId).getEntry()) {
            if (entry.getResource() instanceof Condition) {
                Condition c = (Condition) entry.getResource();

                String aeid = "adverseevent-" + DigestUtils.sha256Hex(c.getId() + salt);

                AdverseEvent ae = new AdverseEvent();
                ae.setId(aeid);

                Patient p = getPatient(sessionId);
                ae.setSubject(new Reference().setReference(p.getId()));

                ae.setEvent(c.getCode().copy());

                // HACK: adding custom event code to make it clear to CQF-Ruler that these AdverseEvent resources
                //       originated within the COACH application.  we don't want CQF-Ruler using AdverseEvent
                //       resources from the EHR, so we need a reliable way to differentiate them
                ae.getEvent().addCoding(new Coding()
                        .setCode("coach-adverse-event")
                        .setSystem("https://coach.ohsu.edu")
                        .setDisplay("Adverse Event reported by COACH"));

                ae.getResultingCondition().add(new Reference().setReference(c.getId()));

                if (c.hasOnsetDateTimeType()) {
                    ae.setDate(c.getOnsetDateTimeType().getValue());
                } else if (c.hasRecordedDate()) {
                    ae.setDate(c.getRecordedDate());
                }

                if (c.hasOnsetDateTimeType()) {
                    ae.setDetected(c.getOnsetDateTimeType().getValue());
                }

                if (c.hasRecordedDate()) {
                    ae.setRecordedDate(c.getRecordedDate());
                }

                MyAdverseEventOutcome outcome = adverseEventService.getOutcome(aeid);
                ae.getOutcome().addCoding(new Coding()
                        .setCode(outcome.getOutcome().getFhirValue())
                        .setSystem("http://terminology.hl7.org/CodeSystem/adverse-event-outcome"));

                b.addEntry().setFullUrl("http://hl7.org/fhir/AdverseEvent/" + ae.getId()).setResource(ae);
            }
        }

        return b;
    }

    private Bundle getAdverseEventConditions(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
        Bundle b = fcc.search(fhirQueryManager.getAdverseEventQuery(fcc.getCredentials().getPatientId()));
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
            if (!exists) {
                iter.remove();
            }
        }

        // filter out any conditions that a) have no effective date, or b) the effective date is > 1 month ago
        iter = b.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            Condition c = (Condition) entry.getResource();
            if (c.hasOnsetDateTimeType()) {
                if (c.getOnsetDateTimeType().getValue().before(ONE_MONTH_AGO)) {
                    iter.remove();
                }
            } else if (c.hasRecordedDate()) {
                if (c.getRecordedDate().before(ONE_MONTH_AGO)) {
                    iter.remove();
                }
            } else if (c.hasEncounter()) {
                Encounter e = fcc.read(Encounter.class, c.getEncounter().getReference(), b);
                if (e != null) {
                    if (e.getStatus().equals(Encounter.EncounterStatus.FINISHED)) {
                        if (e.hasPeriod()) {
                            Period p = e.getPeriod();
                            if (p.hasStart() && p.getStart().before(ONE_MONTH_AGO)) {
                                iter.remove();
                            } else if (p.hasEnd() && p.getEnd().before(ONE_MONTH_AGO)) {
                                iter.remove();
                            }
                        }
                    }
                } else {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }

        return b;
    }
}