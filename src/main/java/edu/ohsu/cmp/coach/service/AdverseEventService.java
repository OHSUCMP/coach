package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.app.MyAdverseEvent;
import edu.ohsu.cmp.coach.entity.app.MyAdverseEventOutcome;
import edu.ohsu.cmp.coach.entity.app.Outcome;
import edu.ohsu.cmp.coach.repository.app.AdverseEventOutcomeRepository;
import edu.ohsu.cmp.coach.repository.app.AdverseEventRepository;
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
        String adverseEventIdHash = hash(adverseEventId);

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

    public boolean setOutcome(String adverseEventId, Outcome outcome) {
        String adverseEventIdHash = hash(adverseEventId);
        if (outcomeRepository.exists(adverseEventIdHash)) {
            MyAdverseEventOutcome aeo = outcomeRepository.findOneByAdverseEventIdHash(adverseEventIdHash);
            aeo.setOutcome(outcome);
            aeo.setModifiedDate(new Date());
            outcomeRepository.save(aeo);
            return true;

        } else {
            logger.warn("attempted to set outcome=" + outcome + " for adverseEventIdHash=" + adverseEventIdHash +
                    " but no such record found!  this shouldn't happen.  skipping -");
            return false;
        }
    }

    private String hash(String s) {
        return DigestUtils.sha256Hex(s + salt);
    }
}
