package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.repository.HomeBloodPressureReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeBloodPressureReadingService {

    @Autowired
    private HomeBloodPressureReadingRepository repository;

    public List<HomeBloodPressureReading> getHomeBloodPressureReadings(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        // todo: implement this

        return null;
    }
}
