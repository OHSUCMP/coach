package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.HypotensionAdverseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HypotensionAdverseEventRepository extends JpaRepository<HypotensionAdverseEvent, Long> {
    @Query("select hae from HypotensionAdverseEvent hae where hae.patId=:patId")
    List<HypotensionAdverseEvent> findAllByPatId(@Param("patId") Long patId);
}
