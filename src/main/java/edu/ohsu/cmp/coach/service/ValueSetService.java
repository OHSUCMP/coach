package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.vsac.ValueSet;
import edu.ohsu.cmp.coach.repository.vsac.ValueSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValueSetService extends BaseService {
    @Autowired
    private ValueSetRepository repository;

    public ValueSet getValueSet(String oid) {
        ValueSet valueSet = repository.findOneByOid(oid);
        return valueSet;
    }
}
