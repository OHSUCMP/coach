package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HypotensionAdverseEvent;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.AbstractVitalsModel;
import edu.ohsu.cmp.coach.model.AuditLevel;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.repository.HypotensionAdverseEventRepository;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HypotensionAdverseEventService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int LOOKBACK_DAYS = 14;

    @Autowired
    private HypotensionAdverseEventRepository repository;

    @Autowired
    private BloodPressureService bloodPressureService;

    public boolean refresh(String sessionId) throws DataException {
        logger.info("refreshing hypotension adverse events for session=" + sessionId);

        boolean modified = false;

        // first, catalog the list of hypotension adverse events for this person within the lookback period that are currently persisted
        Map<String, HypotensionAdverseEvent> currentMap = new LinkedHashMap<>();
        for (HypotensionAdverseEvent hae : getHypotensionAdverseEventList(sessionId)) {
            currentMap.put(hae.getLogicalEqualityKey(), hae);
        }

        LocalDate earliestEventDate = LocalDate.now().minusDays(LOOKBACK_DAYS);
        System.out.println("Earliest event date: " + earliestEventDate); 

        // next, construct a new index of hypotension AEs based on this person's BP readings in the lookback period
        Map<String, HypotensionAdverseEvent> newMap = new LinkedHashMap<>();
        BloodPressureModel bpm1 = null;
        List<BloodPressureModel> homeBPList = bloodPressureService.getHomeBloodPressureReadings(sessionId).stream()
            .filter(bp -> {
                LocalDate bpDate = bp.getReadingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return bpDate.isAfter(earliestEventDate);
            }).collect(Collectors.toList());
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

    /**
     * This is limited to Hypotension Adverse Events that started in the last 14 days
     * @param sessionId
     * @return
     */
    public List<HypotensionAdverseEvent> getHypotensionAdverseEventList(String sessionId) {
        LocalDate earliestEventDate = LocalDate.now().minusDays(LOOKBACK_DAYS);
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return repository.findAllByPatId(workspace.getInternalPatientId()).stream()
            .filter(hae -> 
            {
                LocalDate earliestAEDate = hae.getBp1ReadingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return earliestAEDate.isAfter(earliestEventDate);
            }).collect(Collectors.toList());
    }

    public HypotensionAdverseEvent create(String sessionId, HypotensionAdverseEvent hae) {
        logger.info("creating new hypotension adverse event for session=" + sessionId + " - " + hae.getLogicalEqualityKey());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        hae.setPatId(workspace.getInternalPatientId());
        hae.setCreatedDate(new Date());
        HypotensionAdverseEvent hae2 = repository.save(hae);

        auditService.doAudit(sessionId, AuditLevel.INFO, "created hypotension adverse event", "id=" + hae2.getId());

        return hae2;
    }

    public void delete(String sessionId, HypotensionAdverseEvent hae) {
        logger.info("deleting hypotension adverse event that no longer reflects current state for session=" +
                sessionId + " - " + hae.getLogicalEqualityKey());
        repository.delete(hae);

        auditService.doAudit(sessionId, AuditLevel.INFO, "deleted hypotension adverse event", "id=" + hae.getId());
    }
}
