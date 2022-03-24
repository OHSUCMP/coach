package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.workspace.UserWorkspace;
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
        UserWorkspace workspace = workspaceService.get(sessionId);
        return repository.findAllByPatId(workspace.getInternalPatientId());
    }

    public Counseling getCounseling(String sessionId, String extCounselingId) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        return repository.findOneByPatIdAndExtCounselingId(workspace.getInternalPatientId(), extCounselingId);
    }

    public Counseling create(String sessionId, Counseling counseling) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        counseling.setPatId(workspace.getInternalPatientId());
        counseling.setCreatedDate(new Date());
        return repository.save(counseling);
    }

    public CounselingPage getPage(String key) {
        return pageRepository.findOneByPageKey(key);
    }
}
