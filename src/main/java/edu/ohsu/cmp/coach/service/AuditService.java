package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Audit;
import edu.ohsu.cmp.coach.model.AuditLevel;
import edu.ohsu.cmp.coach.repository.AuditRepository;
import edu.ohsu.cmp.coach.workspace.UserWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserWorkspaceService userWorkspaceService;

    @Autowired
    private AuditRepository repository;

    public void doAudit(String sessionId, AuditLevel level, String action) {
        Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
        doAudit(new Audit(patId, level, action));
    }

    public void doAudit(String sessionId, AuditLevel level, String action, String details) {
        Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
        doAudit(new Audit(patId, level, action, details));
    }

    private void doAudit(Audit audit) {
        try {
            if (logger.isDebugEnabled()) {
                Audit generatedAudit = repository.save(audit);
                logger.debug("generated " + generatedAudit);

            } else {
                repository.save(audit);
            }

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to create " + audit + " - " + e.getMessage(), e);
        }
    }
}
