package edu.ohsu.cmp.coach.repository.app;

import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalHistoryRepository extends JpaRepository<GoalHistory, Long> {
//    @Query("select gh from GoalHistory gh join Goal g on g.id=gh.goalId where g.patId=:patId and g.extGoalId=:extGoalId")
//    List<GoalHistory> findAllByPatIdAndExtGoalId(@Param("patId") Long patId, @Param("extGoalId") String extGoalId);

    List<GoalHistory> findByGoalId(Long goalId);
}
