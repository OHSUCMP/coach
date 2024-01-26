package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Audit;
import edu.ohsu.cmp.coach.entity.MyPatient;
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
        if (userWorkspaceService.exists(sessionId)) {
            Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
            doAudit(new Audit(patId, level, action));

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": level=" + level +
                    ", action=" + action);
        }
    }

    public void doAudit(String sessionId, AuditLevel level, String action, String details) {
        if (userWorkspaceService.exists(sessionId)) {
            Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
            doAudit(new Audit(patId, level, action, details));

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": level=" + level +
                    ", action=" + action + ", details=" + details);
        }
    }

    public void doAudit(MyPatient myPatient, AuditLevel level, String action) {
        doAudit(myPatient, level, action, null);
    }

    public void doAudit(MyPatient myPatient, AuditLevel level, String action, String details) {
        doAudit(new Audit(myPatient.getId(), level, action, details));
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
