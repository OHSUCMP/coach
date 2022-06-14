package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.vsac.Concept;
import edu.ohsu.cmp.coach.entity.vsac.ValueSet;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.MedicationModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MedicationService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private ValueSetService valueSetService;

    public List<MedicationModel> buildMedications(String sessionId) throws DataException {
        List<MedicationModel> list = new ArrayList<>();

        Bundle medicationStatementsBundle = ehrService.getMedicationStatements(sessionId);
        if (medicationStatementsBundle != null && medicationStatementsBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : medicationStatementsBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof MedicationStatement) {
                    MedicationStatement medicationStatement = (MedicationStatement) entry.getResource();
                    list.add(new MedicationModel(medicationStatement));
                }
            }
        }

        Bundle medicationRequestsBundle = ehrService.getMedicationRequests(sessionId);
        if (medicationRequestsBundle != null && medicationRequestsBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : medicationRequestsBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof MedicationRequest) {
                    MedicationRequest medicationRequest = (MedicationRequest) entry.getResource();
                    list.add(new MedicationModel(medicationRequest, medicationRequestsBundle));
                }
            }
        }

        return list;
    }

    public List<MedicationModel> getAntihypertensiveMedications(String sessionId) {
        return getMedications(sessionId, true);
    }

    public List<MedicationModel> getOtherMedications(String sessionId) {
        return getMedications(sessionId, false);
    }

    private List<MedicationModel> getMedications(String sessionId, boolean includeAntihypertensive) {
        List<MedicationModel> list = filterByValueSet(workspaceService.get(sessionId).getMedications(),
                getAntihypertensiveMedicationValueSetOIDsList(),
                includeAntihypertensive);

        return filterDuplicates(list);
    }

    private List<MedicationModel> filterByValueSet(List<MedicationModel> list, String valueSetOID, boolean includeOnMatch) {
        return filterByValueSet(list, Arrays.asList(valueSetOID), includeOnMatch);
    }

    private List<MedicationModel> filterByValueSet(List<MedicationModel> list, List<String> valueSetOIDList, boolean includeOnMatch) {
        if (list == null) return null;

        List<MedicationModel> filtered = new ArrayList<>();

        List<Concept> concepts = new ArrayList<>();
        for (String oid : valueSetOIDList) {
            ValueSet valueSet = valueSetService.getValueSet(oid);
            if (valueSet != null && valueSet.getConcepts() != null) {
                concepts.addAll(valueSet.getConcepts());
            }
        }

        for (Concept c : concepts) {
            String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
            for (MedicationModel item : list) {
                boolean matches = item.matches(codeSystem, c.getCode());
                if ((includeOnMatch && matches) || (!includeOnMatch && !matches)) {
                    filtered.add(item);
                    break;
                }
            }
        }

        return filtered;
    }

    private List<String> getAntihypertensiveMedicationValueSetOIDsList() {
        String csv = env.getProperty("antihypertensive.medication.valueset.oid.csv");
        return Arrays.asList(csv.split("\\s*,\\s*"));
    }

    private List<MedicationModel> filterDuplicates(List<MedicationModel> modelList) {
        Map<String, MedicationModel> map = new LinkedHashMap<String, MedicationModel>();

        final String delim = "|";
        for (MedicationModel m : modelList) {
            String key = m.getDescription() + delim
                    + m.getReason() + delim
                    + m.getDose() + delim
                    + m.getPrescribingClinician() + delim
                    + m.getIssues() + delim
                    + m.getPriority();

            if (map.containsKey(key)) {
                Long tsNew = m.getEffectiveTimestamp();
                if (tsNew != null) {    // if the new one has no timestamp, keep the existing one
                    Long tsMapped = map.get(key).getEffectiveTimestamp();
                    if (tsMapped == null || tsNew > tsMapped) {
                        map.put(key, m);
                    }
                }

            } else {
                map.put(key, m);
            }
        }

        return new ArrayList<>(map.values());
    }
}
