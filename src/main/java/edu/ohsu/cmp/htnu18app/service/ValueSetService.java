package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
import edu.ohsu.cmp.htnu18app.repository.vsac.ValueSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValueSetService {
    @Autowired
    private ValueSetRepository repository;

    public ValueSet getValueSet(String oid) {
        ValueSet valueSet = repository.findOneByOid(oid);
        return valueSet;
    }
}
