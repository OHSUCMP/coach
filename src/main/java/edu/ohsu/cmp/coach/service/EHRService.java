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
import org.apache.commons.lang3.StringUtils;
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
    private static final String URN_OID_PREFIX = "urn:oid:";
    private static final String URL_PREFIX = "http://";

    private static final String CONDITION_CLINICALSTATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String CONDITION_VERIFICATIONSTATUS_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-ver-status";

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
                ), fcm.getBpLimit()
            );

            // handle modifier flags
            Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                Resource r = entry.getResource();
                if (r instanceof Observation) {
                    Observation o = (Observation) r;

                    // only allow "final", "amended", "corrected" status to pass

                    if (o.getStatus() != Observation.ObservationStatus.FINAL &&
                            o.getStatus() != Observation.ObservationStatus.AMENDED &&
                            o.getStatus() != Observation.ObservationStatus.CORRECTED) {
                        logger.debug("removing Observation " + o.getId() + " (invalid status)");
                        iter.remove();
                    }
                }
            }

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

            // handle modifier flags
            filterConditions(b);

            cache.setConditions(b);
        }
        return b;
    }

    /**
     * filters Conditions in a Bundle by their necessary modifier and other flags or codes
     * @param b
     */
    private void filterConditions(Bundle b) {
        Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
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
    }

    public Bundle getCurrentGoals(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Bundle b = cache.getCurrentGoals();
        if (b == null) {
            logger.info("requesting Goals for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.search(fhirQueryManager.getGoalQuery(fcc.getCredentials().getPatientId()));

            // handle modifier and other flags
            Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                Resource r = entry.getResource();
                if (r instanceof Goal) {
                    Goal g = (Goal) r;

                    if (g.getLifecycleStatus() != Goal.GoalLifecycleStatus.ACTIVE) {
                        // only allow "active" lifecycleStatus
                        logger.debug("removing Goal " + g.getId() + " (invalid lifecycleStatus)");
                        iter.remove();
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

            cache.setCurrentGoals(b);
        }

        return b;
    }

    @Transactional
    public Bundle getMedications(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle b = cache.getMedications();
        if (b == null) {
            logger.info("requesting Medication data for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = getMedicationStatements(fcc);
            if (b == null) b = getMedicationRequests(fcc);
            cache.setMedications(b);
        }

        return b;
    }

    private Bundle getMedicationStatements(FHIRCredentialsWithClient fcc) {
        Bundle b = fcc.search(fhirQueryManager.getMedicationStatementQuery(fcc.getCredentials().getPatientId()));
        if (b == null) return null;

        // handle modifier and other flags

        Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
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

        return b;
    }

    private Bundle getMedicationRequests(FHIRCredentialsWithClient fcc) {
        Bundle b = fcc.search(fhirQueryManager.getMedicationRequestQuery(fcc.getCredentials().getPatientId()));
        if (b == null) return null;

        // handle modifier and other flags
        // MUST HAPPEN BEFORE we append associated Medication records (below)

        Iterator<Bundle.BundleEntryComponent> iter = b.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            Resource r = entry.getResource();
            if (r instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) r;

                // status = active
                if (mr.getStatus() != MedicationRequest.MedicationRequestStatus.ACTIVE) {
                    logger.debug("removing MedicationRequest " + mr.getId() + " (invalid status)");
                    iter.remove();
                }

                // intent = order
                if (mr.getIntent() != MedicationRequest.MedicationRequestIntent.ORDER) {
                    logger.debug("removing MedicationRequest " + mr.getId() + " (invalid intent)");
                    iter.remove();
                }

                // doNotPerform = false
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
            logger.info("requesting AdverseEvent data for session " + sessionId);

            b = buildAdverseEvents(sessionId);
            cache.setAdverseEvents(b);
        }

        return b;
    }

    private Bundle buildAdverseEvents(String sessionId) {
        Bundle adverseEventBundle = new Bundle();
        adverseEventBundle.setType(Bundle.BundleType.COLLECTION);

        Bundle b = getAdverseEventConditions(sessionId);
        if (b == null) return null;

        // handle modifier and other flags

        filterConditions(b);

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.getResource() instanceof Condition) {
                Condition c = (Condition) entry.getResource();

                AdverseEvent ae = buildAdverseEvent(sessionId, c);

                adverseEventBundle.addEntry().setFullUrl("http://hl7.org/fhir/AdverseEvent/" + ae.getId()).setResource(ae);
            }
        }

        return adverseEventBundle;
    }

    private AdverseEvent buildAdverseEvent(String sessionId, Condition c) {
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

        return ae;
    }

    private Bundle getAdverseEventConditions(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
        Bundle b = fcc.search(fhirQueryManager.getAdverseEventQuery(fcc.getCredentials().getPatientId()));
        if (b == null) return null;

        Map<String, List<MyAdverseEvent>> codesWeCareAbout = new HashMap<>();
//        Set<String> codesWeCareAbout = new HashSet<String>();
        for (MyAdverseEvent mae : adverseEventService.getAll()) {
            if ( ! codesWeCareAbout.containsKey(mae.getConceptCode()) ) {
                codesWeCareAbout.put(mae.getConceptCode(), new ArrayList<>());
            }
            codesWeCareAbout.get(mae.getConceptCode()).add(mae);
//            codesWeCareAbout.add(mae.getConceptSystem() + "|" + mae.getConceptCode());
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
                        List<MyAdverseEvent> list = codesWeCareAbout.get(coding.getCode());
                        if (list != null) {
                            for (MyAdverseEvent mae : list) {
                                if (StringUtils.startsWith(coding.getSystem(), URN_OID_PREFIX)) {
                                    if (StringUtils.equals(coding.getSystem(), URN_OID_PREFIX + mae.getConceptSystemOID())) {
                                        // e.g. "urn:oid:2.16.840.1.113883.6.90"
                                        exists = true;
                                    }

                                } else if (StringUtils.startsWith(coding.getSystem(), URL_PREFIX)) {
                                    if (StringUtils.equals(coding.getSystem(), mae.getConceptSystem())) {
                                        // e.g. "http://snomed.info/sct"
                                        exists = true;
                                    } else if (StringUtils.startsWith(coding.getSystem(), mae.getConceptSystem() + "/")) {
                                        // e.g. "http://hl7.org/fhir/sid/icd-9-cm/diagnosis"
                                        exists = true;
                                    }

                                } else if (StringUtils.equals(coding.getSystem(), mae.getConceptSystem())) {
                                    // for any system that's not an OID or a URL, demand exact match.  I can't think of
                                    // any examples of what this might be in practice, but if it matches exactly, who
                                    // am I to judge?
                                    exists = true;
                                }

                                if (exists) break;
                            }
                        }

                        if (exists) break;
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