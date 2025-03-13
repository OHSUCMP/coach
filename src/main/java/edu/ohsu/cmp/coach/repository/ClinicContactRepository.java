package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.ClinicContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClinicContactRepository extends JpaRepository<ClinicContact, Long> {
    @Query("select c from ClinicContact c where c.active=true order by c.name")
    List<ClinicContact> findAllActive();
}
