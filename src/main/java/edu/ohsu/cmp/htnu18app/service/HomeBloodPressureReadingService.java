package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.repository.app.HomeBloodPressureReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class HomeBloodPressureReadingService extends BaseService {

    @Autowired
    private HomeBloodPressureReadingRepository repository;

    public List<HomeBloodPressureReading> getHomeBloodPressureReadings(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        return repository.findAllByPatId(cache.getInternalPatientId());
    }

    public HomeBloodPressureReading create(String sessionId, HomeBloodPressureReading bpreading) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        bpreading.setPatId(cache.getInternalPatientId());
        bpreading.setCreatedDate(new Date());
        return repository.save(bpreading);
    }

    public void delete(String sessionId, Long id) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        repository.deleteByIdForPatient(id, cache.getInternalPatientId());
    }
}
