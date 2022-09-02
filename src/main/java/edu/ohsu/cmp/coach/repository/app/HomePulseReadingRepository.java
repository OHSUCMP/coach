package edu.ohsu.cmp.coach.repository.app;

import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.entity.app.HomePulseReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HomePulseReadingRepository extends JpaRepository<HomePulseReading, Long> {
    @Query("select pr from HomePulseReading pr where pr.patId=:patId order by pr.readingDate desc")
    List<HomePulseReading> findAllByPatId(@Param("patId") Long patId);


    @Modifying
    @Transactional
    @Query("delete from HomePulseReading where id=:id and patId=:patId")
    void deleteByIdForPatient(@Param("id") Long id, @Param("patId") Long patId);


    @Modifying
    @Transactional
    @Query("delete from HomePulseReading where patId=:patId")
    void deleteAllByPatId(@Param("patId") Long patId);
}
