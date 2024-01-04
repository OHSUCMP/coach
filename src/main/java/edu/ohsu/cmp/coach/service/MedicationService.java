package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Concept;
import edu.ohsu.cmp.coach.entity.ValueSet;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.MedicationModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MedicationService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private ValueSetService valueSetService;

    @Scheduled(cron = "${antihypertensive.medication.valueset.refresh-cron}") // 0 0 5 * * SUN
    public void refreshAntihypertensiveMedicationValueSets() {
        logger.info("refreshing anti-hypertensive medication ValueSets -");
        for (String oid : getAntihypertensiveMedicationValueSetOIDsList()) {
            try {
                valueSetService.refresh(oid);
            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " refreshing ValueSet with OID=" + oid + " - " + e.getMessage(), e);
            }
        }
        logger.info("done refreshing anti-hypertensive medication ValueSets.");
    }

    public List<MedicationModel> buildMedications(String sessionId) throws DataException, ConfigurationException, IOException {
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
                    try {
                        list.add(new MedicationModel(medicationRequest, medicationRequestsBundle));

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getName() + " - " + e.getMessage() + " - " +
                                " attempting to build MedicationModel for MedicationRequest:\n" +
                                FhirUtil.toJson(medicationRequest));
                        logger.warn("MedicationRequest " + medicationRequest.getId() + " not added to Medications list!");
                    }
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
        logger.debug("getting Medications for session " + sessionId + ", includeAntihypertensive=" + includeAntihypertensive);
        return filterByValueSet(userWorkspaceService.get(sessionId).getMedications(),
                getAntihypertensiveMedicationValueSetOIDsList(),
                includeAntihypertensive);
    }

    private List<MedicationModel> filterByValueSet(List<MedicationModel> list, String valueSetOID, boolean includeOnMatch) {
        return filterByValueSet(list, Arrays.asList(valueSetOID), includeOnMatch);
    }

    private List<MedicationModel> filterByValueSet(List<MedicationModel> list, List<String> valueSetOIDList, boolean includeOnMatch) {
        if (list == null) return null;

        List<MedicationModel> filtered = new ArrayList<>();

        logger.debug("in filterByValueSet(includeOnMatch=" + includeOnMatch + ") - list.size() = " + list.size());

        List<Concept> concepts = new ArrayList<>();
        for (String oid : valueSetOIDList) {
            ValueSet valueSet = valueSetService.getValueSet(oid);
            if (valueSet != null && valueSet.getConcepts() != null) {
                concepts.addAll(valueSet.getConcepts());
            } else {
                logger.warn("ValueSet with OID=" + oid + " does not exist and / or has no concepts!");
            }
        }

        logger.debug("filtering Medications -");
        for (MedicationModel item : list) {
            logger.debug(" - processing " + item.getDescription() + " (id=" + item.getSourceId() + ") -");

            boolean matches = false;
            for (Concept c : concepts) {
                String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
                if (item.matches(codeSystem, c.getCode())) {
                    logger.debug("   - matches codeSystem=" + codeSystem + ", code=" + c.getCode() + "!");
                    matches = true;
                    break;
                }
            }

            if ((includeOnMatch && matches) || (!includeOnMatch && !matches)) {
                logger.debug("   - adding to filtered list");
                filtered.add(item);
            }
        }
        logger.debug("done filtering Medications.  filtered.size() = " + filtered.size());

        return filtered;
    }

    private List<String> getAntihypertensiveMedicationValueSetOIDsList() {
        String csv = env.getProperty("antihypertensive.medication.valueset.oid.csv");
        return Arrays.asList(csv.split("\\s*,\\s*"));
    }
}
