package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdverseEventRepository extends JpaRepository<MyAdverseEvent, Long> {
}
