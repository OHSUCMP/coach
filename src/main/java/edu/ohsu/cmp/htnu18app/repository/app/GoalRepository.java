package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    @Query("select g from Goal g where g.patId=:patId")
    List<Goal> findAllByPatId(@Param("patId") Long patId);

    Goal findOneByPatIdAndGoalId(@Param("patId") Long patId, @Param("goalId") String goalId);

    @Modifying
    @Transactional
    @Query("delete from Goal where goalId=:goalId and patId=:patId")
    void deleteByGoalIdForPatient(@Param("goalId") String goalId, @Param("patId") Long patId);
}
