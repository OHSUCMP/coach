package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.MyAdverseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdverseEventRepository extends JpaRepository<MyAdverseEvent, Long> {
}
