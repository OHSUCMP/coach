package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.cache.CacheData;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.entity.app.Counseling;
import edu.ohsu.cmp.coach.entity.app.CounselingPage;
import edu.ohsu.cmp.coach.repository.app.CounselingPageRepository;
import edu.ohsu.cmp.coach.repository.app.CounselingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CounselingService extends BaseService {

    @Autowired
    private CounselingRepository repository;

    @Autowired
    private CounselingPageRepository pageRepository;

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

    public CounselingPage getPage(String key) {
        return pageRepository.findOneByPageKey(key);
    }
}
