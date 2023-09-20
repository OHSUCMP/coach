package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.MedicationRoute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRouteRepository extends JpaRepository<MedicationRoute, Long> {
}
