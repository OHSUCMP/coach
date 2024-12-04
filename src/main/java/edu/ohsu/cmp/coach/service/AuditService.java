package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Audit;
import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.model.AuditSeverity;
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

    public void doAudit(String sessionId, AuditSeverity severity, String action) {
        if (userWorkspaceService.exists(sessionId)) {
            Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
            doAudit(new Audit(patId, severity, action));

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": severity=" + severity +
                    ", action=" + action);
        }
    }

    public void doAudit(String sessionId, AuditSeverity severity, String action, String details) {
        if (userWorkspaceService.exists(sessionId)) {
            Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
            doAudit(new Audit(patId, severity, action, details));

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": severity=" + severity +
                    ", action=" + action + ", details=" + details);
        }
    }

    public void doAudit(MyPatient myPatient, AuditSeverity severity, String action) {
        doAudit(myPatient, severity, action, null);
    }

    public void doAudit(MyPatient myPatient, AuditSeverity severity, String action, String details) {
        doAudit(new Audit(myPatient.getId(), severity, action, details));
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
