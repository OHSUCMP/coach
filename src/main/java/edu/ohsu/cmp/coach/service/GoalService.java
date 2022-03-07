package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.cache.UserCache;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.entity.app.AchievementStatus;
import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.repository.app.GoalHistoryRepository;
import edu.ohsu.cmp.coach.repository.app.GoalRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoalService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalHistoryRepository goalHistoryRepository;

    public List<String> getExtGoalIdList(String sessionId) {
        List<String> list = new ArrayList<>();
        for (MyGoal g : getGoalList(sessionId)) {
            list.add(g.getExtGoalId());
        }
        return list;
    }

    public List<MyGoal> getGoalList(String sessionId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        return goalRepository.findAllByPatId(cache.getInternalPatientId());
    }

    public MyGoal getGoal(String sessionId, String extGoalId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        return goalRepository.findOneByPatIdAndExtGoalId(cache.getInternalPatientId(), extGoalId);
    }

    /**
     * @param sessionId
     * @return the most recent, current BP goal from either the EHR or from the app, and create an
     * app-based BP goal if no goal exists
     */
    public GoalModel getCurrentBPGoal(String sessionId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);

        GoalModel ehrBPGoal = getCurrentEHRBPGoal(sessionId);

        MyGoal g = getCurrentAppBPGoal(sessionId);
        GoalModel bpGoal = g != null ?
                new GoalModel(g) :
                null;

        if (ehrBPGoal != null && bpGoal != null) {
            int comparison = ehrBPGoal.getCreatedDate().compareTo(bpGoal.getCreatedDate());
            if (comparison < 0) return bpGoal;
            else if (comparison > 0) return ehrBPGoal;
            else {
                // conflicting dates!  default to app goal I guess?
                return bpGoal;
            }

        } else if (ehrBPGoal != null) {
            return ehrBPGoal;

        } else if (bpGoal != null) {
            return bpGoal;

        } else {
            return new GoalModel(create(sessionId, new MyGoal(
                    fcm.getBpSystem(),
                    fcm.getBpCode(),
                    "Blood Pressure",
                    GoalModel.BP_GOAL_DEFAULT_SYSTOLIC,
                    GoalModel.BP_GOAL_DEFAULT_DIASTOLIC
            )));
        }
    }

    public MyGoal getCurrentAppBPGoal(String sessionId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        return goalRepository.findCurrentBPGoal(cache.getInternalPatientId());
    }

    // utility function to get the latest BP goal from the EHR
    private GoalModel getCurrentEHRBPGoal(String sessionId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);

        org.hl7.fhir.r4.model.Goal currentEHRBPGoal = null;

        Bundle bundle = ehrService.getCurrentGoals(sessionId);

        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            if (entryCon.getResource() instanceof org.hl7.fhir.r4.model.Goal) {
                org.hl7.fhir.r4.model.Goal g = (org.hl7.fhir.r4.model.Goal) entryCon.getResource();

                boolean hasSystolicTarget = false;
                boolean hasDiastolicTarget = false;

                for (org.hl7.fhir.r4.model.Goal.GoalTargetComponent gtc : g.getTarget()) {
                    if (gtc.getMeasure().hasCoding(fcm.getBpSystem(), fcm.getBpSystolicCode())) {
                        hasSystolicTarget = true;

                    } else if (gtc.getMeasure().hasCoding(fcm.getBpSystem(), fcm.getBpDiastolicCode())) {
                        hasDiastolicTarget = true;
                    }
                }

                if (hasSystolicTarget && hasDiastolicTarget) {
                    if (currentEHRBPGoal == null) {
                        currentEHRBPGoal = g;

                    } else if (g.getStartDateType().getValue().compareTo(currentEHRBPGoal.getStartDateType().getValue()) < 0) {
                        currentEHRBPGoal = g;
                    }
                }

            } else {
                Resource r = entryCon.getResource();
                logger.warn("ignoring " + r.getClass().getName() + " (id=" + r.getId() + ") while finding Current EHR BP Goal");
            }
        }

        if (currentEHRBPGoal != null) {
            return new GoalModel(currentEHRBPGoal, cache.getInternalPatientId(), fcm.getBpSystem(), fcm.getBpCode(),
                    "Blood Pressure", fcm.getBpSystolicCode(), fcm.getBpDiastolicCode());

        } else {
            return null;
        }
    }

    public boolean hasAnyNonBPGoals(String sessionId) {
        for (MyGoal g : getGoalList(sessionId)) {
            if ( ! g.isBloodPressureGoal() ) {
                return true;
            }
        }
        return false;
    }

    public List<MyGoal> getAllNonBPGoals(String sessionId) {
        List<MyGoal> list = getGoalList(sessionId);
        Iterator<MyGoal> iter = list.iterator();
        while (iter.hasNext()) {
            MyGoal g = iter.next();
            if (g.isBloodPressureGoal()) {
                iter.remove();
            }
        }
        return list;
    }

    public MyGoal create(String sessionId, MyGoal goal) {
        UserCache cache = SessionCache.getInstance().get(sessionId);

        goal.setPatId(cache.getInternalPatientId());
        goal.setCreatedDate(new Date());

        MyGoal g = goalRepository.save(goal);

        GoalHistory gh = goalHistoryRepository.save(new GoalHistory(AchievementStatus.IN_PROGRESS, g));
        Set<GoalHistory> set = new HashSet<>();
        set.add(gh);
        g.setHistory(set);

        return g;
    }

    public MyGoal update(MyGoal goal) {
        return goalRepository.save(goal);
    }

    public void deleteByGoalId(String sessionId, String extGoalId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        goalRepository.deleteByGoalIdForPatient(extGoalId, cache.getInternalPatientId());
    }

    public void deleteBPGoalIfExists(String sessionId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        goalRepository.deleteBPGoalForPatient(cache.getInternalPatientId());
    }
}
