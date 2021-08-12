package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEvent;
import edu.ohsu.cmp.htnu18app.repository.app.AdverseEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdverseEventService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AdverseEventRepository repository;

    public List<MyAdverseEvent> getAll() {
        return repository.findAll();
    }
}
