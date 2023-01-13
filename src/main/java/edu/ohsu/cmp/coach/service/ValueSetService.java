package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Concept;
import edu.ohsu.cmp.coach.entity.ValueSet;
import edu.ohsu.cmp.coach.repository.ValueSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class ValueSetService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private VSACService vsacService;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ValueSetRepository repository;

    public ValueSet getValueSet(String oid) {
        return repository.findOneByOid(oid);
    }

    public void refresh(String oid) {
        try {
            logger.info("acquiring ValueSet with oid=" + oid + " from VSAC");
            ValueSet fresh = vsacService.getValueSet(oid);

            // update incoming ValueSet concepts to reference existing persistence records if they exist
            Set<Concept> concepts = new LinkedHashSet<>();
            for (Concept c : fresh.getConcepts()) {
                Concept existingConcept = conceptService.getConcept(c.getCode(), c.getCodeSystem(), c.getCodeSystemVersion());
                if (existingConcept != null) {
                    concepts.add(existingConcept);
                } else {
                    concepts.add(c);
                }
            }
            fresh.setConcepts(concepts);

            ValueSet existing = getValueSet(oid);
            if (existing != null) {
                logger.info("updating existing ValueSet with oid=" + oid);
                existing.update(fresh);
                repository.save(existing);

            } else {
                logger.info("creating new ValueSet with oid=" + oid);
                repository.save(fresh);
            }

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " refreshing ValueSet with oid=" + oid, e);
        }
    }
}
