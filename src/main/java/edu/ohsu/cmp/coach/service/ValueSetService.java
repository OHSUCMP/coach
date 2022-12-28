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
            ValueSet valueSet = getValueSet(oid);
            if (valueSet != null) {
                logger.info("refreshing ValueSet with oid=" + oid);

                // the ValueSet already exists in the local DB, so it will be prepopulated with Concepts that also
                // exist in the local DB.  calling valueSet.update() with the fresh data from VSAC will apply those
                // fresh chances to the persisted objects, adding any that don't exist and removing those that are
                // missing

                valueSet.update(vsacService.getValueSet(oid));

            } else {
                logger.info("acquiring new ValueSet with oid=" + oid);
                valueSet = vsacService.getValueSet(oid);

                // the ValueSet does not yet exist in the local DB, so the object we're working with is straight
                // from VSAC.  none of its Concepts come from the local DB and don't have IDs assigned.  however, these
                // referenced Concepts *may* be represented in the local DB.  if they are, we *need* to replace those
                // copies from VSAC with the locally persisted copies, so that JPA will perform an update instead of
                // trying to recreate them with inserts.  this block of code below performs that operation

                Set<Concept> concepts = new LinkedHashSet<>();
                for (Concept c : valueSet.getConcepts()) {
                    Concept existingConcept = conceptService.getConcept(c.getCode(), c.getCodeSystem(), c.getCodeSystemVersion());
                    if (existingConcept != null) {
                        concepts.add(existingConcept);
                    } else {
                        concepts.add(c);
                    }
                }
                valueSet.setConcepts(concepts);
            }

            repository.save(valueSet);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " refreshing ValueSet with oid=" + oid, e);
        }
    }
}
