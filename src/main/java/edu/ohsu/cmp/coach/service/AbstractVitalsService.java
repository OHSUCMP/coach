package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.model.AbstractModel;
import edu.ohsu.cmp.coach.model.AbstractVitalsModel;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.FHIRCompatible;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.poi.ss.formula.functions.T;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
public abstract class AbstractVitalsService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    protected Bundle writeRemote(String sessionId, FHIRCompatible fhirCompatible) {
        UserWorkspace workspace = workspaceService.get(sessionId);

        String patientId = workspace.getPatient().getSourcePatient().getId();
        Bundle bundle = fhirCompatible.toBundle(patientId, fcm);

        // modify bundle to be appropriate for submission as a CREATE TRANSACTION

        bundle.setType(Bundle.BundleType.TRANSACTION);

        // prepare bundle for POSTing resources (create)
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            entry.setFullUrl(URN_UUID + entry.getResource().getId())
                    .setRequest(new Bundle.BundleEntryRequestComponent()
                            .setMethod(Bundle.HTTPVerb.POST)
                            .setUrl(entry.getResource().fhirType()));
        }

        // write BP reading to the FHIR server

        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        return fhirService.transact(fcc, bundle);
    }

    protected Map<String, List<Observation>> buildEncounterObservationsMap(Bundle bundle) {
        Map<String, List<Observation>> map = new HashMap<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();
                    List<String> keys = buildKeys(observation.getEncounter());

                    // we want to associate THE SAME list with each key, NOT separate instances of identical lists

                    List<Observation> list = null;
                    for (String key : keys) {
                        if (map.containsKey(key)) {
                            list = map.get(key);
                            break;
                        }
                    }
                    if (list == null) {
                        list = new ArrayList<>();
                        for (String key : keys) {
                            map.put(key, list);
                        }
                    }

                    map.get(keys.get(0)).add(observation);
                }
            }
        }
        return map;
    }

    protected List<Observation> getObservationsFromMap(Encounter encounter, Map<String, List<Observation>> map) {
        List<Observation> list = null;
        for (String key : buildKeys(encounter.getId(), encounter.getIdentifier())) {
            if (map.containsKey(key)) {
                if (list == null) {
                    list = map.remove(key);
                } else {
                    map.remove(key);
                }
            }
        }
        return list;
    }
}
