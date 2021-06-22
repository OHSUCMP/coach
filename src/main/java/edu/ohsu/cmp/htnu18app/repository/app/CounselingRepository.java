package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.Counseling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CounselingRepository extends JpaRepository<Counseling, Long> {
    @Query("select c from Counseling c where c.patId=:patId")
    List<Counseling> findAllByPatId(@Param("patId") Long patId);

    Counseling findOneByPatIdAndExtCounselingId(@Param("patId") Long patId, @Param("extCounselingId") String extCounselingId);
}
