package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import edu.ohsu.cmp.htnu18app.repository.app.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class GoalService {

    @Autowired
    private GoalRepository repository;

    public List<Goal> getGoals(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return repository.findAllByPatId(cache.getInternalPatientId());
    }

    public Goal getGoal(String sessionId, String goalId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return repository.findOneByPatIdAndGoalId(cache.getInternalPatientId(), goalId);
    }

    public Goal create(String sessionId, Goal goal) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        goal.setPatId(cache.getInternalPatientId());
        goal.setCreatedDate(new Date());
        return repository.save(goal);
    }

    public Goal update(Goal goal) {
        return repository.save(goal);
    }

    public void delete(String sessionId, String goalId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        repository.deleteByGoalIdForPatient(goalId, cache.getInternalPatientId());
    }
}
