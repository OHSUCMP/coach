package edu.ohsu.cmp.htnu18app.repository.vsac;

import edu.ohsu.cmp.htnu18app.entity.vsac.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    Concept findOneByCodeSystemNameAndCode(String codeSystemName, String code);
}
