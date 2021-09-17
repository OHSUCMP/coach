package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEventOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdverseEventOutcomeRepository extends JpaRepository<MyAdverseEventOutcome, Long> {
    @Query("select case when count(aeo) > 0 then true else false end from MyAdverseEventOutcome aeo where aeo.adverseEventIdHash=:adverseEventIdHash")
    boolean exists(@Param("adverseEventIdHash") String adverseEventIdHash);

    MyAdverseEventOutcome findOneByAdverseEventIdHash(@Param("adverseEventIdHash") String adverseEventIdHash);

}
