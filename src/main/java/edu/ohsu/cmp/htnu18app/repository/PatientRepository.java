package edu.ohsu.cmp.htnu18app.repository;

import edu.ohsu.cmp.htnu18app.entity.Patient;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends CrudRepository<Patient, Long> {
    boolean existsPatientByPatIdHash(String patIdHash);

    @Query("select p from Patient p where p.patIdHash=:patIdHash")
    public List<Patient> findByPatIdHash(@Param("patIdHash") String patIdHash);
}
