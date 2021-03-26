package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.vsac.Concept;
import edu.ohsu.cmp.htnu18app.repository.vsac.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {
    @Autowired
    private ConceptRepository repository;

    public Concept getConcept(String codeSystemName, String code) {
        Concept concept = repository.findOneByCodeSystemNameAndCode(codeSystemName, code);
        return concept;
    }
}
