package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.MedicationForm;
import edu.ohsu.cmp.coach.entity.MyAdverseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationFormRepository extends JpaRepository<MedicationForm, Long> {
}
