package edu.ohsu.cmp.coach.repository.vsac;

import edu.ohsu.cmp.coach.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    Concept findOneByCodeSystemNameAndCode(String codeSystemName, String code);
}
