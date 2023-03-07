package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.MyOmronVitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OmronVitalsCacheRepository extends JpaRepository<MyOmronVitals, Long> {
    @Query("select case when count(v) > 0 then true else false end from MyOmronVitals v where v.omronId=:omronId")
    boolean existsByOmronId(@Param("omronId") Long omronId);

    @Query("select v from MyOmronVitals v where v.patId=:patId")
    List<MyOmronVitals> findAllByPatId(@Param("patId") Long patId);

    @Modifying
    @Transactional
    @Query("delete from MyOmronVitals where patId=:patId")
    void deleteAllByPatId(@Param("patId") Long patId);
}
