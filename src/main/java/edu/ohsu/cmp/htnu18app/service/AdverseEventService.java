package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEvent;
import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEventOutcome;
import edu.ohsu.cmp.htnu18app.entity.app.Outcome;
import edu.ohsu.cmp.htnu18app.repository.app.AdverseEventOutcomeRepository;
import edu.ohsu.cmp.htnu18app.repository.app.AdverseEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdverseEventService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AdverseEventRepository repository;

    @Autowired
    private AdverseEventOutcomeRepository outcomeRepository;

    public List<MyAdverseEvent> getAll() {
        return repository.findAll();
    }

    public MyAdverseEventOutcome getOutcome(Long internalPatientId, String adverseEventId) {
        MyAdverseEventOutcome outcome;
        if (outcomeRepository.existsAdverseEventForPatient(internalPatientId, adverseEventId)) {
            outcome = outcomeRepository.findOneByPatIdAndAdverseEventId(internalPatientId, adverseEventId);

        } else {
            outcome = new MyAdverseEventOutcome(internalPatientId, adverseEventId, Outcome.ONGOING);
            outcome = outcomeRepository.saveAndFlush(outcome);
        }

        return outcome;
    }
}
