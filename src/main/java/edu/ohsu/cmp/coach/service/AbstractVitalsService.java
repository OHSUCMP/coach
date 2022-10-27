package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public abstract class AbstractVitalsService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String NO_ENCOUNTERS_KEY = null; // intentionally instantiated with null value

    protected Bundle writeRemote(String sessionId, Bundle bundle) throws DataException, IOException, ConfigurationException, ScopeException {
        UserWorkspace workspace = workspaceService.get(sessionId);

        if (bundle.getType() != Bundle.BundleType.TRANSACTION) {
            throw new ConfigurationException("bundle passed to writeRemote must be of type=TRANSACTION");
        }

        // prepare bundle for POSTing resources (create)
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            entry.setRequest(new Bundle.BundleEntryRequestComponent()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl(entry.getResource().fhirType()));
        }

        // write reading to the FHIR server

        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Bundle responseBundle = fhirService.transact(fcc, bundle, true);

        // remove any responses that didn't result in a 201 Created response
        Iterator<Bundle.BundleEntryComponent> iter = responseBundle.getEntry().iterator();
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            if (entry.hasResponse()) {
                Bundle.BundleEntryResponseComponent response = entry.getResponse();
                if (response.hasStatus() && response.getStatus().equals("201 Created")) {
                    logger.debug("successfully created " + response.getLocation());
                } else {
                    logger.warn("got status = " + response.getStatus() + " attempting to write " + entry + " - removing");
                    iter.remove();
                }
            }
        }

        return responseBundle;
    }

    protected Map<String, List<Observation>> buildEncounterObservationsMap(Bundle bundle) {
        Map<String, List<Observation>> map = new HashMap<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();
                    if (observation.hasEncounter()) {
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

                    } else {
                        List<Observation> list = map.get(NO_ENCOUNTERS_KEY);
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(NO_ENCOUNTERS_KEY, list);
                        }
                        list.add(observation);
                    }
                }
            }
        }
        return map;
    }

    protected List<Observation> getObservationsFromMap(Encounter encounter, Map<String, List<Observation>> map) {
        List<Observation> list = null;
        for (String key : buildKeys(encounter.getId(), encounter.getIdentifier())) {
            if (map.containsKey(key)) {     // the same exact list may be represented multiple times for different keys.  we only care about the first
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
