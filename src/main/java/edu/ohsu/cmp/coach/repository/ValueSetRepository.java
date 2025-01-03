package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.ValueSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ValueSetRepository extends JpaRepository<ValueSet, Long> {
    ValueSet findOneByOid(@Param("oid") String oid);
}
