package edu.ohsu.cmp.coach.repository.app;

import edu.ohsu.cmp.coach.entity.app.MyAdverseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdverseEventRepository extends JpaRepository<MyAdverseEvent, Long> {
}
