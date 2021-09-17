package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEvent;
import edu.ohsu.cmp.htnu18app.entity.app.MyAdverseEventOutcome;
import edu.ohsu.cmp.htnu18app.entity.app.Outcome;
import edu.ohsu.cmp.htnu18app.repository.app.AdverseEventOutcomeRepository;
import edu.ohsu.cmp.htnu18app.repository.app.AdverseEventRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AdverseEventService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private AdverseEventRepository repository;

    @Autowired
    private AdverseEventOutcomeRepository outcomeRepository;

    public List<MyAdverseEvent> getAll() {
        return repository.findAll();
    }

    public MyAdverseEventOutcome getOutcome(String adverseEventId) {
        String adverseEventIdHash = DigestUtils.sha256Hex(adverseEventId + salt);

        MyAdverseEventOutcome outcome;
        if (outcomeRepository.exists(adverseEventIdHash)) {
            outcome = outcomeRepository.findOneByAdverseEventIdHash(adverseEventIdHash);
            logger.debug("outcome with adverseEventIdHash=" + adverseEventIdHash + " exists (id=" + outcome.getId() + ")");

        } else {
            outcome = new MyAdverseEventOutcome(adverseEventIdHash, Outcome.ONGOING);
            outcome.setCreatedDate(new Date());
            outcome = outcomeRepository.saveAndFlush(outcome);
            logger.debug("outcome with adverseEventIdHash=" + adverseEventIdHash + " does NOT exist.  created (id=" + outcome.getId() + ")");
        }

        return outcome;
    }
}
