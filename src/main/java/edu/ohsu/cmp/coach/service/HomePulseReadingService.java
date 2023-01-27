package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.HomePulseReading;
import edu.ohsu.cmp.coach.repository.HomePulseReadingRepository;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class HomePulseReadingService extends AbstractService {
    @Autowired
    private HomePulseReadingRepository repository;

    public List<HomePulseReading> getHomePulseReadings(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return repository.findAllByPatId(workspace.getInternalPatientId());
    }

    public HomePulseReading create(String sessionId, HomePulseReading pulseReading) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        pulseReading.setPatId(workspace.getInternalPatientId());
        pulseReading.setCreatedDate(new Date());
        return repository.save(pulseReading);
    }

    public void delete(String sessionId, Long id) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        repository.deleteByIdForPatient(id, workspace.getInternalPatientId());
    }

    public void deleteAll(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        repository.deleteAllByPatId(workspace.getInternalPatientId());
    }
}
