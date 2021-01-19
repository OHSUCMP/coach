package edu.ohsu.cmp.htnu18app.repository;

import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HomeBloodPressureReadingRepository extends CrudRepository<HomeBloodPressureReading, Long> {
    @Query("select bpr from HomeBloodPressureReading bpr where bpr.patId=:patId order by bpr.readingDate desc")
    List<HomeBloodPressureReading> findAllByPatId(@Param("patId") Long patId);


    @Modifying
    @Transactional
    @Query("delete from HomeBloodPressureReading where id=:id and patId=:patId")
    void deleteByIdForPatient(@Param("id") Long id, @Param("patId") Long patId);
}
