package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MethodNotImplementedException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.omron.OmronVitals;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class BloodPressureService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private OmronService omronService;

    public List<BloodPressureModel> buildRemoteBloodPressureList(String sessionId) throws DataException, ConfigurationException {
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
        for (BloodPressureModel entry : getBloodPressureReadings(sessionId)) {
            if (entry.isHomeReading()) {
                list.add(entry);
            }
        }
        return list;
    }

    public List<BloodPressureModel> getBloodPressureReadings(String sessionId) throws DataException {
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
                boolean added = false;
// storer 2023-10-06 - commenting this out temporarily, this function gets called a lot in parallel and for some reason some local readings
//                     that aren't retrieved remotely aren't writing remotely because they already exist, somehow.  need to debug that.  in the
//                     meantime, we don't want to execute a dozen write attempts that fail for these
//                if (storeRemotely) {
//                    try {
//                        logger.info("attempting to store remotely BP: " + bpm + "(" + bpm.getSystolic() + "/" + bpm.getDiastolic() +
//                                " @ " + bpm.getReadingDateString() + ")");
//                        VendorTransformer transformer = workspace.getVendorTransformer();
//                        Bundle outgoingBundle = transformer.transformOutgoingBloodPressureReading(bpm);
//                        List<BloodPressureModel> list2 = transformer.transformIncomingBloodPressureReadings(
//                                transformer.writeRemote(sessionId, fhirService, outgoingBundle)
//                        );
//                        if (list2.size() >= 1) {
//                            BloodPressureModel bpm2 = list2.get(0);
//                            workspace.getRemoteBloodPressures().add(bpm2);
//                            list.add(bpm2);
//                            added = true;
//                        }
//
//                    } catch (Exception e) {
//                        // remote errors are tolerable, since we will always store locally too
//                        logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create BP remotely - " + e.getMessage(), e);
//                    }
//                }
                if ( ! added ) {
                    logger.debug("adding local BP with key: " + key);
                    list.add(bpm);
                }
            }
        }

        Collections.sort(list, (o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public BloodPressureModel create(String sessionId, BloodPressureModel bpm) throws DataException, ConfigurationException, IOException, ScopeException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        BloodPressureModel bpm2 = null;

        if (storeRemotely) {
            try {
                VendorTransformer transformer = workspace.getVendorTransformer();
                Bundle outgoingBundle = transformer.transformOutgoingBloodPressureReading(bpm);
                List<BloodPressureModel> list = transformer.transformIncomingBloodPressureReadings(
                        transformer.writeRemote(sessionId, fhirService, outgoingBundle)
                );
                if (list.size() >= 1) {
                    bpm2 = list.get(0);
                    workspace.getRemoteBloodPressures().add(bpm2);
                }

            } catch (Exception e) {
                // remote errors are tolerable, since we will always store locally too
                logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create BP remotely - " + e.getMessage(), e);
            }
        }

        try {
            HomeBloodPressureReading hbpr = new HomeBloodPressureReading(bpm);
            HomeBloodPressureReading response = hbprService.create(sessionId, hbpr);

            if (bpm2 == null) { // give priority to the remotely created resource, if it exists
                bpm2 = new BloodPressureModel(response, fcm);
            }

        } catch (DataException de) {
            // okay if it's failing to write locally, that's a problem.
            logger.error("caught " + de.getClass().getName() + " attempting to create BloodPressureModel " + bpm);
        }

        return bpm2;
    }

    public Boolean delete(String sessionId, String id) {
        // delete home BP reading (do not allow deleting office BP readings!)

        try {
            if (storeRemotely) {
                throw new MethodNotImplementedException("remote delete is not implemented");

            } else {
                Long longId = Long.parseLong(id);
                hbprService.delete(sessionId, longId);
            }

            return true;

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to delete resource with id=" + id +
                    " for session " + sessionId, e);
            return false;
        }
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

        // add Omron-sourced BPs
        List<OmronVitals> omronList = omronService.readFromPersistentCache(sessionId);
        for (OmronVitals item : omronList) {
            list.add(item.getBloodPressureModel());
        }

        return list;
    }
}
