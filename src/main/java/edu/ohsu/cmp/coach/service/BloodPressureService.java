package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MethodNotImplementedException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
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

    public List<BloodPressureModel> buildBloodPressureList(String sessionId) throws DataException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getAllBpCodings()), fcm.getBpLookbackPeriod(), null));
        compositeBundle.consume(workspaceService.get(sessionId).getProtocolObservations());

        UserWorkspace workspace = workspaceService.get(sessionId);
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
        List<BloodPressureModel> list = new ArrayList<>();
        list.addAll(workspaceService.get(sessionId).getBloodPressures());

        if ( ! storeRemotely ) {
            list.addAll(buildLocalBloodPressureReadings(sessionId));
        }

        Collections.sort(list, (o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public BloodPressureModel create(String sessionId, BloodPressureModel bpm) throws DataException, ConfigurationException, IOException, ScopeException {
        UserWorkspace workspace = workspaceService.get(sessionId);
        if (storeRemotely) {
            VendorTransformer transformer = workspace.getVendorTransformer();
            Bundle outgoingBundle = transformer.transformOutgoingBloodPressureReading(bpm);
            List<BloodPressureModel> list = transformer.transformIncomingBloodPressureReadings(
                    transformer.writeRemote(sessionId, fhirService, outgoingBundle)
            );
            if (list.size() >= 1) {
                BloodPressureModel bpm2 = list.get(0);
                workspace.getBloodPressures().add(bpm2);
                return bpm2;
            }

        } else {
            try {
                HomeBloodPressureReading hbpr = new HomeBloodPressureReading(bpm);
                HomeBloodPressureReading response = hbprService.create(sessionId, hbpr);
                return new BloodPressureModel(response, fcm);

            } catch (DataException de) {
                logger.error("caught " + de.getClass().getName() + " attempting to create BloodPressureModel " + bpm);
            }
        }

        return null;
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

        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
        for (HomeBloodPressureReading item : hbprList) {
            list.add(new BloodPressureModel(item, fcm));
        }

        return list;
    }
}
