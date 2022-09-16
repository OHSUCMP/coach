package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.MyPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<MyPatient, Long> {
    boolean existsPatientByPatIdHash(@Param("patIdHash") String patIdHash);

    @Query("select p from MyPatient p where p.patIdHash=:patIdHash")
    List<MyPatient> findByPatIdHash(@Param("patIdHash") String patIdHash);
    MyPatient findOneByPatIdHash(@Param("patIdHash") String patIdHash);
}
