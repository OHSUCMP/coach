package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.AchievementStatus;
import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import edu.ohsu.cmp.htnu18app.entity.app.GoalHistory;
import edu.ohsu.cmp.htnu18app.entity.app.LifecycleStatus;
import edu.ohsu.cmp.htnu18app.repository.app.GoalHistoryRepository;
import edu.ohsu.cmp.htnu18app.repository.app.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public Goal create(String sessionId, Goal goal) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        goal.setPatId(cache.getInternalPatientId());
        goal.setCreatedDate(new Date());
        goal.setCompleted(false);

        Goal g = goalRepository.save(goal);

        goalHistoryRepository.save(new GoalHistory(LifecycleStatus.ACTIVE, AchievementStatus.NO_PROGRESS, g));

        return g;
    }

    public Goal update(Goal goal) {
        return goalRepository.save(goal);
    }

    public void delete(String sessionId, String extGoalId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        goalRepository.deleteByGoalIdForPatient(extGoalId, cache.getInternalPatientId());
    }
}
