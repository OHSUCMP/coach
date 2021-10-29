package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.vsac.Concept;
import edu.ohsu.cmp.coach.entity.vsac.ValueSet;
import edu.ohsu.cmp.coach.model.MedicationModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MedicationService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private ValueSetService valueSetService;

    public String getMedicationsOfInterestName() {
        ValueSet valueSet = valueSetService.getValueSet(fcm.getMedicationValueSetOid());
        return valueSet.getDisplayName();
    }

    public List<MedicationModel> getMedicationsOfInterest(String sessionId) {
        return getMedications(sessionId, true);
    }

    public List<MedicationModel> getOtherMedications(String sessionId) {
        return getMedications(sessionId, false);
    }

    private List<MedicationModel> getMedications(String sessionId, boolean includeOfInterest) {
        List<MedicationModel> list = new ArrayList<>();

        Bundle b = filterByValueSet(ehrService.getMedications(sessionId), fcm.getMedicationValueSetOid(), includeOfInterest);
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            try {
                if (entry.getResource() instanceof MedicationStatement || entry.getResource() instanceof MedicationRequest) {
                    list.add(new MedicationModel(entry.getResource(), b));
                }

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " processing " + entry.getResource().getId(), e);
            }
        }

        return list;
    }

    private Bundle filterByValueSet(Bundle b, String valueSetOid, boolean includeOnMatch) {
        // build concept info as a simple set we can query to test inclusion
        // (these are the meds we want to show)
        ValueSet valueSet = valueSetService.getValueSet(valueSetOid);
        Set<String> concepts = new HashSet<>();
        for (Concept c : valueSet.getConcepts()) {
            String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
            concepts.add(codeSystem + "|" + c.getCode());
        }

        Set<String> matchingIds = new HashSet<>();

        // make 2 passes over bundle entries:
        //  1st pass: identify all of the IDs for resources we want to keep, including referenced resources
        //  2nd pass: filter bundle resources based on results of the 1st pass

        // 1st pass:
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.getResource() instanceof MedicationStatement) {
                MedicationStatement ms = (MedicationStatement) entry.getResource();
                CodeableConcept cc = ms.getMedicationCodeableConcept();
                for (Coding c : cc.getCoding()) {
                    if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                        matchingIds.add(ms.getId());
                        break;
                    }
                }

            } else if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) entry.getResource();
                if (mr.hasMedicationCodeableConcept()) {
                    CodeableConcept cc = mr.getMedicationCodeableConcept();
                    for (Coding c : cc.getCoding()) {
                        if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                            matchingIds.add(mr.getId());
                            break;
                        }
                    }

                } else if (mr.hasMedicationReference()) {
                    Medication m = FhirUtil.getResourceFromBundleByReference(b, Medication.class,
                            mr.getMedicationReference().getReference());

                    if (m != null && m.hasCode()) {
                        CodeableConcept cc = m.getCode();
                        if (cc.hasCoding()) {
                            for (Coding c : cc.getCoding()) {
                                if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                                    matchingIds.add(mr.getId());
                                    matchingIds.add(m.getId()); // also include the Medication resource
                                    break;
                                }
                            }
                        }

                    } else if (m == null) {
                        logger.warn("no Medication found in bundle for reference=" +
                                mr.getMedicationReference().getReference());
                    }
                }
            }
        }

        Bundle filtered = new Bundle();
        filtered.setType(Bundle.BundleType.COLLECTION);

        // 2nd pass:
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (includeOnMatch && matchingIds.contains(entry.getResource().getId())) {
                filtered.addEntry(entry);

            } else if (!includeOnMatch && !matchingIds.contains(entry.getResource().getId())) {
                filtered.addEntry(entry);
            }
        }

        return filtered;
    }
}
