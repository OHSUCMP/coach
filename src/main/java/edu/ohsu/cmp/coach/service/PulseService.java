package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HomePulseReading;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.ObservationSource;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class PulseService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomePulseReadingService hprService;

    public List<PulseModel> buildRemotePulseList(String sessionId) throws DataException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getPulseCodings()), fcm.getPulseLookbackPeriod(),null));
        compositeBundle.consume(userWorkspaceService.get(sessionId).getProtocolObservations());

        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return workspace.getVendorTransformer().transformIncomingPulseReadings(compositeBundle.getBundle());
    }

    public List<PulseModel> getHomePulseReadings(String sessionId) {
        List<PulseModel> list = new ArrayList<>();
        for (PulseModel entry : getPulseReadings(sessionId)) {
            if (entry.getSource() == ObservationSource.HOME) {
                list.add(entry);
            }
        }
        return list;
    }

    public List<PulseModel> getPulseReadings(String sessionId) {
        List<PulseModel> remoteList = userWorkspaceService.get(sessionId).getRemotePulses();
        Set<String> remotePulseKeySet = new HashSet<>();
        for (PulseModel pm : remoteList) {
            String key = pm.getLogicalEqualityKey();
            logger.debug("found remote Pulse with key: " + key);
            remotePulseKeySet.add(key);
        }

        List<PulseModel> list = new ArrayList<>();
        list.addAll(remoteList);
        for (PulseModel pm : buildLocalPulseReadings(sessionId)) {
            String key = pm.getLogicalEqualityKey();
            if (remotePulseKeySet.contains(key)) {
                logger.debug("NOT ADDING local Pulse matching remote Pulse with key: " + key);

            } else {
                logger.debug("adding local Pulse with key: " + key);
                list.add(pm);
            }
        }

        Collections.sort(list, (o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public PulseModel create(String sessionId, PulseModel pm) throws DataException, ConfigurationException, IOException, ScopeException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        PulseModel pm2 = null;

        if (storeRemotely) {
            try {
                VendorTransformer transformer = workspace.getVendorTransformer();
                Bundle outgoingBundle = transformer.transformOutgoingPulseReading(pm);
                List<PulseModel> list = transformer.transformIncomingPulseReadings(
                        transformer.writeRemote(sessionId, fhirService, outgoingBundle)
                );
                if (list.size() >= 1) {
                    pm2 = list.get(0);
                    workspace.getRemotePulses().add(pm2);
                }

            } catch (Exception e) {
                // remote errors are tolerable, since we will always store locally too
                logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create Pulse remotely - " + e.getMessage(), e);
            }
        }

        try {
            HomePulseReading hpr = new HomePulseReading(pm);
            HomePulseReading response = hprService.create(sessionId, hpr);

            if (pm2 == null) { // give priority to the remotely created resource, if it exists
                pm2 = new PulseModel(response, fcm);
            }

        } catch (DataException de) {
            // okay if it's failing to write locally, that's a problem.
            logger.error("caught " + de.getClass().getName() + " attempting to create PulseModel " + pm);
        }

        return pm2;
    }


///////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private List<PulseModel> buildLocalPulseReadings(String sessionId) {
        List<PulseModel> list = new ArrayList<>();

        List<HomePulseReading> hbprList = hprService.getHomePulseReadings(sessionId);
        for (HomePulseReading item : hbprList) {
            list.add(new PulseModel(item, fcm));
        }

        return list;
    }
}
