package edu.ohsu.cmp.htnu18app.repository.app;

import edu.ohsu.cmp.htnu18app.entity.app.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsPatientByPatIdHash(@Param("patIdHash") String patIdHash);

    @Query("select p from Patient p where p.patIdHash=:patIdHash")
    List<Patient> findByPatIdHash(@Param("patIdHash") String patIdHash);
    Patient findOneByPatIdHash(@Param("patIdHash") String patIdHash);
}
