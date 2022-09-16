package edu.ohsu.cmp.coach.repository.app;

import edu.ohsu.cmp.coach.entity.MyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GoalRepository extends JpaRepository<MyGoal, Long> {
    @Query("select g from MyGoal g where g.patId=:patId")
    List<MyGoal> findAllByPatId(@Param("patId") Long patId);

    MyGoal findOneByPatIdAndExtGoalId(@Param("patId") Long patId, @Param("extGoalId") String extGoalId);

    @Query("select g from MyGoal g where g.patId=:patId and g.systolicTarget is not null and g.diastolicTarget is not null")
    MyGoal findCurrentBPGoal(@Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from MyGoal where extGoalId=:extGoalId and patId=:patId")
    void deleteByGoalIdForPatient(@Param("extGoalId") String extGoalId, @Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from MyGoal where systolicTarget is not null and diastolicTarget is not null and patId=:patId")
    void deleteBPGoalForPatient(@Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from MyGoal where patId=:patId")
    void deleteAllByPatId(@Param("patId") Long patId);
}
