package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HomePulseReading;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.FhirStrategy;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class PulseService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${fhir.pulse-writeback-strategy}")
    private FhirStrategy writebackStrategy;

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomePulseReadingService hprService;

    public List<PulseModel> buildRemotePulseList(String sessionId) throws DataException, ConfigurationException, IOException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getPulseCodings()), fcm.getPulseLookbackPeriod(),null));
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        compositeBundle.consume(workspace.getProtocolObservations());
        return workspace.getVendorTransformer().transformIncomingPulseReadings(compositeBundle.getBundle());
    }

    public List<PulseModel> getHomePulseReadings(String sessionId) throws DataException {
        List<PulseModel> list = new ArrayList<>();

        for (PulseModel entry : getPulseReadings(sessionId, false)) {
            if (entry.isHomeReading()) {
                list.add(entry);
            }
        }

        // *now* we impose the limit, *after* we've filtered to only HOME readings

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public List<PulseModel> getPulseReadings(String sessionId) throws DataException {
        return getPulseReadings(sessionId, true);
    }

    public List<PulseModel> getPulseReadings(String sessionId, boolean doLimit) throws DataException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        // add remote pulses first
        List<PulseModel> remoteList = workspace.getRemotePulses();
        Set<String> remotePulseKeySet = new HashSet<>();
        for (PulseModel pm : remoteList) {
            String key = pm.getLogicalEqualityKey();
            logger.debug("found remote Pulse with key: " + key);
            remotePulseKeySet.add(key);
        }

        // now add any locally-stored pulses that do *not* logically match a pulse already retrieved remotely
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

        list.sort((o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1); // sort newest first

        if (doLimit) {
            Integer limit = fcm.getBpLimit();
            if (limit != null && list.size() > limit) {
                list = list.subList(0, limit);
            }
        }

        return list;
    }

    public PulseModel create(String sessionId, PulseModel pm) throws DataException, ConfigurationException, IOException, ScopeException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        PulseModel pm2 = null;

        if (writebackStrategy != FhirStrategy.DISABLED) {
            logger.info("attempting writeback of Pulse " + pm + " using strategy " + writebackStrategy);
            try {
                VendorTransformer transformer = workspace.getVendorTransformer();
                Bundle outgoingBundle = transformer.transformOutgoingPulseReading(pm);
                List<PulseModel> list = transformer.transformIncomingPulseReadings(
                        transformer.writeRemote(sessionId, writebackStrategy, fhirService, outgoingBundle)
                );
                if (list.size() >= 1) {
                    pm2 = list.get(0);
                    workspace.getRemotePulses().add(pm2);
                }

                auditService.doAudit(sessionId, AuditSeverity.INFO, "wrote pulse remotely", pm.getPulse() +
                        " at " + pm.getReadingDateString());

            } catch (Exception e) {
                // remote errors are tolerable, since we will always store locally too
                logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create Pulse remotely - " +
                        "pulse=" + pm.getPulse() + " at " + pm.getReadingDateString() +
                        ", message=" + e.getMessage(), e);

                auditService.doAudit(sessionId, AuditSeverity.WARN, "failed to write pulse remotely",
                        "pulse=" + pm.getPulse() + " at " + pm.getReadingDateString() +
                                ", message=" + e.getMessage());
            }
        }

        try {
            HomePulseReading hpr = new HomePulseReading(pm);
            HomePulseReading response = hprService.create(sessionId, hpr);

            if (pm2 == null) { // give priority to the remotely created resource, if it exists
                pm2 = new PulseModel(response, fcm);
            }

            auditService.doAudit(sessionId, AuditSeverity.INFO, "created pulse", "id=" + response.getId() +
                    ", pulse=" + pm.getPulse() + " at " + pm.getReadingDateString());

        } catch (DataException de) {
            // okay if it's failing to write locally, that's a problem.
            logger.error("caught " + de.getClass().getName() + " attempting to create PulseModel " + pm, de);

            auditService.doAudit(sessionId, AuditSeverity.ERROR, "failed to create pulse", "pulse=" +
                    pm.getPulse() + " at " + pm.getReadingDateString() + ", message=" + de.getMessage());
        }

        return pm2;
    }


///////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private List<PulseModel> buildLocalPulseReadings(String sessionId) throws DataException {
        List<PulseModel> list = new ArrayList<>();

        // add manually-entered pulses
        List<HomePulseReading> hbprList = hprService.getHomePulseReadings(sessionId);
        for (HomePulseReading item : hbprList) {
            list.add(new PulseModel(item, fcm));
        }

        return list;
    }
}
