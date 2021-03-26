package edu.ohsu.cmp.htnu18app.repository.vsac;

import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ValueSetRepository extends JpaRepository<ValueSet, Long> {
    ValueSet findOneByOid(@Param("oid") String oid);
}
