package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.vsac.Concept;
import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
import edu.ohsu.cmp.htnu18app.model.MedicationModel;
import edu.ohsu.cmp.htnu18app.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Bundle filtered = new Bundle();
        filtered.setType(Bundle.BundleType.COLLECTION);

        // build concept info as a simple set we can query to test inclusion
        // (these are the meds we want to show)
        ValueSet valueSet = valueSetService.getValueSet(valueSetOid);
        Set<String> concepts = new HashSet<>();
        for (Concept c : valueSet.getConcepts()) {
            String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
            concepts.add(codeSystem + "|" + c.getCode());
        }

        // filter out any of the patient's meds that aren't included in set we want to show
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            boolean matches = false;
            if (entry.getResource() instanceof MedicationStatement) {
                MedicationStatement ms = (MedicationStatement) entry.getResource();
                CodeableConcept cc = ms.getMedicationCodeableConcept();
                for (Coding c : cc.getCoding()) {
                    if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                        matches = true;
                        break;
                    }
                }

            } else if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest mr = (MedicationRequest) entry.getResource();
                if (mr.hasMedicationCodeableConcept()) {
                    CodeableConcept cc = mr.getMedicationCodeableConcept();
                    for (Coding c : cc.getCoding()) {
                        if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                            matches = true;
                            break;
                        }
                    }

                } else if (mr.hasMedicationReference()) {
                    Medication m = FhirUtil.getResourceFromBundleByReference(b, Medication.class,
                            mr.getMedicationReference().getReference());

                    for (Coding c : m.getCode().getCoding()) {
                        if (concepts.contains(c.getSystem() + "|" + c.getCode())) {
                            matches = true;
                            break;
                        }
                    }
                }
            }

            if ((matches && includeOnMatch) || (!matches && !includeOnMatch)) {
                filtered.addEntry(entry);
            }
        }

        return filtered;
    }
}
