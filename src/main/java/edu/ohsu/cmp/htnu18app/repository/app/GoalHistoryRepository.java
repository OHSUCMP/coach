package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.GoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoalHistoryRepository extends JpaRepository<GoalHistory, Long> {
    @Query("select gh from GoalHistory gh join Goal g on g.id=gh.goalId where g.patId=:patId and g.extGoalId=:extGoalId")
    List<GoalHistory> findAllByPatIdAndExtGoalId(@Param("patId") Long patId, @Param("extGoalId") String extGoalId);
}
