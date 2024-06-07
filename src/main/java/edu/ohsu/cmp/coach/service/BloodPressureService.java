package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.FhirStrategy;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.AuditLevel;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BloodPressureService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${fhir.bp-writeback-strategy}")
    private FhirStrategy writebackStrategy;

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    public List<BloodPressureModel> buildRemoteBloodPressureList(String sessionId) throws DataException, ConfigurationException, IOException {
        CompositeBundle compositeBundle = new CompositeBundle();

        List<Coding> codings = new ArrayList<>();
        codings.addAll(fcm.getBpPanelCodings());
        codings.addAll(fcm.getBpSystolicCodings());
        codings.addAll(fcm.getBpDiastolicCodings());

        compositeBundle.consume(ehrService.getObservations(sessionId, FhirUtil.toCodeParamString(codings), fcm.getBpLookbackPeriod(), null));
        compositeBundle.consume(userWorkspaceService.get(sessionId).getProtocolObservations());

        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return workspace.getVendorTransformer().transformIncomingBloodPressureReadings(compositeBundle.getBundle());
    }

    public List<BloodPressureModel> getHomeBloodPressureReadings(String sessionId) throws DataException {
        List<BloodPressureModel> list = new ArrayList<>();

        for (BloodPressureModel entry : getBloodPressureReadings(sessionId, false)) {
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

    public List<BloodPressureModel> getBloodPressureReadings(String sessionId) throws DataException {
        return getBloodPressureReadings(sessionId, true);
    }

    public List<BloodPressureModel> getBloodPressureReadings(String sessionId, boolean doLimit) throws DataException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        // add remote BPs first
        List<BloodPressureModel> remoteList = workspace.getRemoteBloodPressures();
        Set<String> remoteBPKeySet = new HashSet<>();
        for (BloodPressureModel bpm : remoteList) {
            String key = bpm.getLogicalEqualityKey();
            logger.debug("found remote BP with key: " + key);
            remoteBPKeySet.add(key);
        }

        // now add any locally-stored BPs that do *not* logically match a BP already retrieved remotely
        List<BloodPressureModel> list = new ArrayList<>();
        list.addAll(remoteList);

        for (BloodPressureModel bpm : buildLocalBloodPressureReadings(sessionId)) {
            String key = bpm.getLogicalEqualityKey();
            if (remoteBPKeySet.contains(key)) {
                logger.debug("NOT ADDING local BP matching remote BP with key: " + key);

            } else {
                logger.debug("adding local BP with key: " + key);
                list.add(bpm);
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

    public BloodPressureModel create(String sessionId, BloodPressureModel bpm) throws DataException, ConfigurationException, IOException, ScopeException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        BloodPressureModel bpm2 = null;

        if (writebackStrategy != FhirStrategy.DISABLED) {
            logger.info("attempting writeback of BP " + bpm + " using strategy " + writebackStrategy);
            try {
                VendorTransformer transformer = workspace.getVendorTransformer();
                Bundle outgoingBundle = transformer.transformOutgoingBloodPressureReading(bpm);
                List<BloodPressureModel> list = transformer.transformIncomingBloodPressureReadings(
                        transformer.writeRemote(sessionId, writebackStrategy, fhirService, outgoingBundle)
                );
                if (list.size() >= 1) {
                    bpm2 = list.get(0);
                    workspace.getRemoteBloodPressures().add(bpm2);
                }

                auditService.doAudit(sessionId, AuditLevel.INFO, "wrote BP remotely", bpm.getSystolic() + "/" +
                        bpm.getDiastolic() + " at " + bpm.getReadingDateString());

            } catch (Exception e) {
                // remote errors are tolerable, since we will always store locally too
                logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create BP remotely - " +
                        "BP=" + bpm.getSystolic() + "/" + bpm.getDiastolic() + " at " + bpm.getReadingDateString() +
                        ", message=" + e.getMessage(), e);

                auditService.doAudit(sessionId, AuditLevel.WARN, "failed to write BP remotely", "BP=" +
                        bpm.getSystolic() + "/" + bpm.getDiastolic() + " at " + bpm.getReadingDateString() +
                                ", message=" + e.getMessage());
            }
        }

        try {
            HomeBloodPressureReading hbpr = new HomeBloodPressureReading(bpm);
            HomeBloodPressureReading response = hbprService.create(sessionId, hbpr);

            if (bpm2 == null) { // give priority to the remotely created resource, if it exists
                bpm2 = new BloodPressureModel(response, fcm);
            }

            auditService.doAudit(sessionId, AuditLevel.INFO, "created BP", "id=" + response.getId() +
                    ", BP=" + bpm.getSystolic() + "/" + bpm.getDiastolic() + " at " + bpm.getReadingDateString());

        } catch (DataException de) {
            // okay if it's failing to write locally, that's a problem.
            logger.error("caught " + de.getClass().getName() + " attempting to create BloodPressureModel " + bpm, de);

            auditService.doAudit(sessionId, AuditLevel.ERROR, "failed to create BP", "BP=" +
                    bpm.getSystolic() + "/" + bpm.getDiastolic() + " at " + bpm.getReadingDateString() +
                            ", message=" + de.getMessage());
        }

        return bpm2;
    }


///////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private List<BloodPressureModel> buildLocalBloodPressureReadings(String sessionId) throws DataException {
        List<BloodPressureModel> list = new ArrayList<>();

        // add manually-entered BPs
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
        for (HomeBloodPressureReading item : hbprList) {
            list.add(new BloodPressureModel(item, fcm));
        }

        return list;
    }
}
