package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HypotensionAdverseEvent;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.AbstractVitalsModel;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.repository.HypotensionAdverseEventRepository;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HypotensionAdverseEventService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HypotensionAdverseEventRepository repository;

    @Autowired
    private BloodPressureService bloodPressureService;

    public boolean refresh(String sessionId) throws DataException {
        logger.info("refreshing hypotension adverse events for session=" + sessionId);

        boolean modified = false;

        // first, catalog the list of hypotension adverse events for this person that are currently persisted
        Map<String, HypotensionAdverseEvent> currentMap = new LinkedHashMap<>();
        for (HypotensionAdverseEvent hae : getHypotensionAdverseEventList(sessionId)) {
            currentMap.put(hae.getLogicalEqualityKey(), hae);
        }

        // next, construct a new index of hypotension AEs based on this person's BP readings
        Map<String, HypotensionAdverseEvent> newMap = new LinkedHashMap<>();
        BloodPressureModel bpm1 = null;
        List<BloodPressureModel> homeBPList = bloodPressureService.getHomeBloodPressureReadings(sessionId);
        homeBPList.sort(Comparator.comparing(AbstractVitalsModel::getReadingDate)); // sort oldest first
        for (BloodPressureModel bpm : homeBPList) {
            if (bpm.isLow()) {
                if (bpm1 != null) {
                    // create AE from bpm and bpm1
                    HypotensionAdverseEvent hae = new HypotensionAdverseEvent(bpm1, bpm);
                    newMap.put(hae.getLogicalEqualityKey(), hae);
                    bpm1 = null;
                } else {
                    bpm1 = bpm;
                }
            } else {
                bpm1 = null;
            }
        }

        // now loop through all the new ones, removing any that exist in both lists
        Iterator<String> iter = newMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            if (currentMap.containsKey(key)) {
                logger.debug("found hypotension adverse event in both current and new lists (will not process) - " + key);
                currentMap.remove(key);
                iter.remove();
            }
        }

        // any items remaining in the old list should be deleted.
        for (HypotensionAdverseEvent hae : currentMap.values()) {
            delete(sessionId, hae);
            modified = true;
        }

        // any items remaining in the new list should be created.
        for (HypotensionAdverseEvent hae : newMap.values()) {
            create(sessionId, hae);
            modified = true;
        }

        return modified;
    }

    public List<HypotensionAdverseEvent> getHypotensionAdverseEventList(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return repository.findAllByPatId(workspace.getInternalPatientId());
    }

    public HypotensionAdverseEvent create(String sessionId, HypotensionAdverseEvent hae) {
        logger.info("creating new hypotension adverse event for session=" + sessionId + " - " + hae.getLogicalEqualityKey());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        hae.setPatId(workspace.getInternalPatientId());
        hae.setCreatedDate(new Date());
        return repository.save(hae);
    }

    public void delete(String sessionId, HypotensionAdverseEvent hae) {
        logger.info("deleting hypotension adverse event that no longer reflects current state for session=" +
                sessionId + " - " + hae.getLogicalEqualityKey());
        repository.delete(hae);
    }
}
