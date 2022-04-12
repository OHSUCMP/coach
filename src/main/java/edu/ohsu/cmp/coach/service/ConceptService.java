package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.vsac.Concept;
import edu.ohsu.cmp.coach.repository.vsac.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService extends AbstractService {
    @Autowired
    private ConceptRepository repository;

    public Concept getConcept(String codeSystemName, String code) {
        Concept concept = repository.findOneByCodeSystemNameAndCode(codeSystemName, code);
        return concept;
    }
}
