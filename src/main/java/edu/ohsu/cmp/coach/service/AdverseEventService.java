package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.MyAdverseEvent;
import edu.ohsu.cmp.coach.entity.MyAdverseEventOutcome;
import edu.ohsu.cmp.coach.entity.Outcome;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.AdverseEventModel;
import edu.ohsu.cmp.coach.repository.AdverseEventOutcomeRepository;
import edu.ohsu.cmp.coach.repository.AdverseEventRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdverseEventService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String URN_OID_PREFIX = "urn:oid:";
    private static final String URL_PREFIX = "http://";

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
    private AdverseEventRepository repository;

    @Autowired
    private AdverseEventOutcomeRepository outcomeRepository;

    @Autowired
    private EHRService ehrService;

    public List<AdverseEventModel> buildAdverseEvents(String sessionId) throws DataException {
        List<AdverseEventModel> list = new ArrayList<>();

        Bundle b = buildAdverseEventConditions(sessionId);
        if (b == null) return null;

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.getResource() instanceof Condition) {
                Condition c = (Condition) entry.getResource();
                AdverseEvent ae = convertConditionToAdverseEvent(sessionId, c);
                list.add(new AdverseEventModel(ae, c));
            }
        }

        return list;
    }

    public AdverseEvent convertConditionToAdverseEvent(String sessionId, Condition c) {
        String aeid = "adverseevent-" + DigestUtils.sha256Hex(c.getId() + salt);

        AdverseEvent ae = new AdverseEvent();
        ae.setId(aeid);

        Patient p = workspaceService.get(sessionId).getPatient().getSourcePatient();
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

        MyAdverseEventOutcome outcome = getOutcome(aeid);
        ae.getOutcome().addCoding(new Coding()
                .setCode(outcome.getOutcome().getFhirValue())
                .setSystem("http://terminology.hl7.org/CodeSystem/adverse-event-outcome"));

        return ae;
    }

    public List<MyAdverseEvent> getAll() {
        return repository.findAll();
    }

    public MyAdverseEventOutcome getOutcome(String adverseEventId) {
        String adverseEventIdHash = hash(adverseEventId);

        MyAdverseEventOutcome outcome;
        if (outcomeRepository.exists(adverseEventIdHash)) {
            outcome = outcomeRepository.findOneByAdverseEventIdHash(adverseEventIdHash);
            logger.debug("outcome with adverseEventIdHash=" + adverseEventIdHash + " exists (id=" + outcome.getId() + ")");

        } else {
            outcome = new MyAdverseEventOutcome(adverseEventIdHash, Outcome.ONGOING);
            outcome.setCreatedDate(new Date());
            outcome = outcomeRepository.saveAndFlush(outcome);
            logger.debug("outcome with adverseEventIdHash=" + adverseEventIdHash + " does NOT exist.  created (id=" + outcome.getId() + ")");
        }

        return outcome;
    }

    public boolean setOutcome(String adverseEventId, Outcome outcome) {
        String adverseEventIdHash = hash(adverseEventId);
        if (outcomeRepository.exists(adverseEventIdHash)) {
            MyAdverseEventOutcome aeo = outcomeRepository.findOneByAdverseEventIdHash(adverseEventIdHash);
            aeo.setOutcome(outcome);
            aeo.setModifiedDate(new Date());
            outcomeRepository.save(aeo);
            return true;

        } else {
            logger.warn("attempted to set outcome=" + outcome + " for adverseEventIdHash=" + adverseEventIdHash +
                    " but no such record found!  this shouldn't happen.  skipping -");
            return false;
        }
    }

    private String hash(String s) {
        return DigestUtils.sha256Hex(s + salt);
    }

    private Bundle buildAdverseEventConditions(String sessionId) {
        Bundle conditions = workspaceService.get(sessionId).getEncounterDiagnosisConditions().copy();

        Map<String, List<MyAdverseEvent>> codesWeCareAbout = new HashMap<>();
        for (MyAdverseEvent mae : getAll()) {
            if ( ! codesWeCareAbout.containsKey(mae.getConceptCode()) ) {
                codesWeCareAbout.put(mae.getConceptCode(), new ArrayList<>());
            }
            codesWeCareAbout.get(mae.getConceptCode()).add(mae);
        }

        // filter out any of the patient's conditions that don't match a code we're interested in
        Iterator<Bundle.BundleEntryComponent> iter = conditions.getEntry().iterator();
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
        iter = conditions.getEntry().iterator();
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
                Encounter e = workspaceService.get(sessionId).getEncounter(c.getEncounter());

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

        // todo: filter duplicates by code?  is this still a thing?

        return conditions;
    }
}
