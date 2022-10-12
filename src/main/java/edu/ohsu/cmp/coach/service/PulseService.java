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
public class PulseService extends AbstractVitalsService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomePulseReadingService hprService;

    public List<PulseModel> buildPulseList(String sessionId) throws DataException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getObservations(sessionId, FhirUtil.toCodeParamString(fcm.getPulseCoding()), fcm.getPulseLookbackPeriod(),null));
        compositeBundle.consume(workspaceService.get(sessionId).getProtocolObservations());

        UserWorkspace workspace = workspaceService.get(sessionId);
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
        List<PulseModel> list = new ArrayList<>();
        list.addAll(workspaceService.get(sessionId).getPulses());

        if ( ! storeRemotely ) {
            list.addAll(buildLocalPulseReadings(sessionId));
        }

        Collections.sort(list, (o1, o2) -> o1.getReadingDate().compareTo(o2.getReadingDate()) * -1);

        Integer limit = fcm.getBpLimit();
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list;
    }

    public PulseModel create(String sessionId, PulseModel pm) throws DataException, ConfigurationException, IOException, ScopeException {
        UserWorkspace workspace = workspaceService.get(sessionId);
        if (storeRemotely) {
            VendorTransformer transformer = workspace.getVendorTransformer();
            Bundle outgoingBundle = transformer.transformOutgoingPulseReading(pm);
            outgoingBundle.setType(Bundle.BundleType.TRANSACTION);
            List<PulseModel> list = transformer.transformIncomingPulseReadings(writeRemote(sessionId, outgoingBundle));
            if (list.size() >= 1) {
                PulseModel pm2 = list.get(0);
                workspace.getPulses().add(pm2);
                return pm2;
            }
        } else {
            try {
                HomePulseReading hpr = new HomePulseReading(pm);
                HomePulseReading response = hprService.create(sessionId, hpr);
                return new PulseModel(response, fcm);

            } catch (DataException de) {
                logger.error("caught " + de.getClass().getName() + " attempting to create BloodPressureModel " + pm);
            }
        }

        return null;
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
