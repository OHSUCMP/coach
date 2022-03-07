package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.cache.UserCache;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.repository.app.HomeBloodPressureReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class HomeBloodPressureReadingService extends BaseService {

    @Autowired
    private HomeBloodPressureReadingRepository repository;

    public List<HomeBloodPressureReading> getHomeBloodPressureReadings(String sessionId) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        return repository.findAllByPatId(cache.getInternalPatientId());
    }

    public HomeBloodPressureReading create(String sessionId, HomeBloodPressureReading bpreading) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        bpreading.setPatId(cache.getInternalPatientId());
        bpreading.setCreatedDate(new Date());
        return repository.save(bpreading);
    }

    public void delete(String sessionId, Long id) {
        UserCache cache = SessionCache.getInstance().get(sessionId);
        repository.deleteByIdForPatient(id, cache.getInternalPatientId());
    }
}
