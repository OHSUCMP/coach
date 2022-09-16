package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HomeBloodPressureReadingRepository extends JpaRepository<HomeBloodPressureReading, Long> {
    @Query("select bpr from HomeBloodPressureReading bpr where bpr.patId=:patId order by bpr.readingDate desc")
    List<HomeBloodPressureReading> findAllByPatId(@Param("patId") Long patId);


    @Modifying
    @Transactional
    @Query("delete from HomeBloodPressureReading where id=:id and patId=:patId")
    void deleteByIdForPatient(@Param("id") Long id, @Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from HomeBloodPressureReading where patId=:patId")
    void deleteAllByPatId(@Param("patId") Long patId);
}
