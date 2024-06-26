package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Concept;
import edu.ohsu.cmp.coach.repository.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService extends AbstractService {
    @Autowired
    private ConceptRepository repository;

    public Concept getConcept(String code, String codeSystem, String codeSystemVersion) {
        Concept concept = repository.findConcept(code, codeSystem, codeSystemVersion);
        return concept;
    }
}
