package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, Long> {
}
