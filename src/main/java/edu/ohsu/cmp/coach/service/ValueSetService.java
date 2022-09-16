package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.ValueSet;
import edu.ohsu.cmp.coach.repository.ValueSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ValueSetService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private VSACService vsacService;

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
                valueSet.update(vsacService.getValueSet(oid));
            } else {
                logger.info("acquiring new ValueSet with oid=" + oid);
                valueSet = vsacService.getValueSet(oid);
            }
            repository.save(valueSet);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " refreshing ValueSet with oid=" + oid, e);
        }
    }
}
