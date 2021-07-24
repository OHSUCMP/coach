package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.AchievementStatus;
import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import edu.ohsu.cmp.htnu18app.entity.app.GoalHistory;
import edu.ohsu.cmp.htnu18app.repository.app.GoalHistoryRepository;
import edu.ohsu.cmp.htnu18app.repository.app.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoalService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalHistoryRepository goalHistoryRepository;

    public List<String> getExtGoalIdList(String sessionId) {
        List<String> list = new ArrayList<>();
        for (Goal g : getGoalList(sessionId)) {
            list.add(g.getExtGoalId());
        }
        return list;
    }

    public List<Goal> getGoalList(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return goalRepository.findAllByPatId(cache.getInternalPatientId());
    }

    public Goal getGoal(String sessionId, String extGoalId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return goalRepository.findOneByPatIdAndExtGoalId(cache.getInternalPatientId(), extGoalId);
    }

    public Goal getCurrentBPGoal(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Goal g = goalRepository.findCurrentBPGoal(cache.getInternalPatientId());
        if (g == null) {
            g = create(sessionId, new Goal(Goal.DEFAULT_BP_GOAL_ID,
                    Goal.BP_GOAL_REFERENCE_SYSTEM, Goal.BP_GOAL_REFERENCE_CODE,
                    Goal.DEFAULT_BP_GOAL_SYSTOLIC, Goal.DEFAULT_BP_GOAL_DIASTOLIC));
        }

        return g;
    }

    public Goal create(String sessionId, Goal goal) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        goal.setPatId(cache.getInternalPatientId());
        goal.setCreatedDate(new Date());

        Goal g = goalRepository.save(goal);

        GoalHistory gh = goalHistoryRepository.save(new GoalHistory(AchievementStatus.IN_PROGRESS, g));
        Set<GoalHistory> set = new HashSet<>();
        set.add(gh);
        g.setHistory(set);

        return g;
    }

    public Goal update(String sessionId, Goal goal) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        return goalRepository.save(goal);
    }

    public void delete(String sessionId, String extGoalId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        goalRepository.deleteByGoalIdForPatient(extGoalId, cache.getInternalPatientId());
    }
}
