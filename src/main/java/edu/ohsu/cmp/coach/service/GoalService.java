package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.model.AchievementStatus;
import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.repository.app.GoalHistoryRepository;
import edu.ohsu.cmp.coach.repository.app.GoalRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoalService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalHistoryRepository goalHistoryRepository;

    public List<GoalModel> getGoals(String sessionId) {
        List<GoalModel> list = new ArrayList<>();
        list.addAll(workspaceService.get(sessionId).getGoals());
        list.addAll(buildLocalGoals(sessionId));
        return list;
    }

    private List<GoalModel> buildLocalGoals(String sessionId) {
        List<GoalModel> list = new ArrayList<>();

        List<MyGoal> goalList = getLocalGoalList(sessionId);
        for (MyGoal item : goalList) {
            list.add(new GoalModel(item));
        }

        return list;
    }

    public List<GoalModel> buildCurrentGoals(String sessionId) {
        List<GoalModel> list = new ArrayList<>();

        Bundle b = ehrService.getGoals(sessionId);

        if (b == null) return null;

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.getResource() instanceof Goal) {
                Goal g = (Goal) entry.getResource();
                list.add(new GoalModel(g, fcm));
            }
        }

        return list;
    }

    public List<String> getExtGoalIdList(String sessionId) {
        List<String> list = new ArrayList<>();
        for (MyGoal g : getLocalGoalList(sessionId)) {
            list.add(g.getExtGoalId());
        }
        return list;
    }

    private List<MyGoal> getLocalGoalList(String sessionId) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        return goalRepository.findAllByPatId(workspace.getInternalPatientId());
    }

    public MyGoal getLocalGoal(String sessionId, String extGoalId) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        return goalRepository.findOneByPatIdAndExtGoalId(workspace.getInternalPatientId(), extGoalId);
    }

    /**
     * @param sessionId
     * @return the most recent, current BP goal from either the EHR or from the app, and create an
     * app-based BP goal if no goal exists
     */
    public GoalModel getCurrentBPGoal(String sessionId) {
        // goals can be stored locally and in the EHR.  so get current BP goals from each source, then
        // return whichever is more current, creating a fresh local goal if none could be found.

        GoalModel ehrBPGoal = getCurrentEHRBPGoal(sessionId);

        MyGoal g = getCurrentLocalBPGoal(sessionId);
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
            return new GoalModel(create(sessionId, new MyGoal(fcm.getBpCoding(),
                    GoalModel.BP_GOAL_DEFAULT_SYSTOLIC,
                    GoalModel.BP_GOAL_DEFAULT_DIASTOLIC
            )));
        }
    }

    public MyGoal getCurrentLocalBPGoal(String sessionId) {
        return goalRepository.findCurrentBPGoal(
                workspaceService.get(sessionId).getInternalPatientId()
        );
    }

    // utility function to get the latest BP goal from the EHR
    private GoalModel getCurrentEHRBPGoal(String sessionId) {
        GoalModel currentEHRBPGoal = null;

        List<GoalModel> goals = workspaceService.get(sessionId).getGoals();
        for (GoalModel goal : goals) {
            if (goal.isEHRGoal() && goal.isBPGoal()) {
                if (currentEHRBPGoal == null) {
                    currentEHRBPGoal = goal;

                } else if (goal.compareTo(currentEHRBPGoal) < 0) {
                    currentEHRBPGoal = goal;
                }
            }
        }

        return currentEHRBPGoal;
    }

    public boolean hasAnyLocalNonBPGoals(String sessionId) {
        for (MyGoal g : getLocalGoalList(sessionId)) {
            if ( ! g.isBloodPressureGoal() ) {
                return true;
            }
        }
        return false;
    }

    public List<MyGoal> getAllLocalNonBPGoals(String sessionId) {
        List<MyGoal> list = getLocalGoalList(sessionId);
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
        UserWorkspace workspace = workspaceService.get(sessionId);

        goal.setPatId(workspace.getInternalPatientId());
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
        UserWorkspace workspace = workspaceService.get(sessionId);
        goalRepository.deleteByGoalIdForPatient(extGoalId, workspace.getInternalPatientId());
    }

    public void deleteBPGoalIfExists(String sessionId) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        goalRepository.deleteBPGoalForPatient(workspace.getInternalPatientId());
    }

    public GoalHistory createHistory(GoalHistory goalHistory) {
        goalHistory.setCreatedDate(new Date());
        return goalHistoryRepository.save(goalHistory);
    }
}
