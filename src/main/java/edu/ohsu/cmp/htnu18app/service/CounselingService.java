package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.Counseling;
import edu.ohsu.cmp.htnu18app.repository.app.CounselingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CounselingService extends BaseService {

    @Autowired
    private CounselingRepository repository;

    public List<Counseling> getCounselingList(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return repository.findAllByPatId(cache.getInternalPatientId());
    }

    public Counseling getCounseling(String sessionId, String extCounselingId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return repository.findOneByPatIdAndExtCounselingId(cache.getInternalPatientId(), extCounselingId);
    }

    public Counseling create(String sessionId, Counseling counseling) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        counseling.setPatId(cache.getInternalPatientId());
        counseling.setCreatedDate(new Date());
        return repository.save(counseling);
    }
}
