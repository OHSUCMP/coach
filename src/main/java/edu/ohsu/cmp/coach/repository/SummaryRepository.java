package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    @Query("select s from Summary s where s.patId=:patId order by s.createdDate desc")
    List<Summary> findAllByPatId(@Param("patId") Long patId);
}
