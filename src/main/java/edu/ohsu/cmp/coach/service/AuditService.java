package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Audit;
import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.repository.AuditRepository;
import edu.ohsu.cmp.coach.util.logging.LogRedactor;
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

    public void doAudit(String sessionId, AuditSeverity severity, String event) {
        if (userWorkspaceService.exists(sessionId)) {
            Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
            doAudit(patId, severity, event, null);

        } else {
            logger.warn("attempted to generate audit for nonexistent session {}: severity={}, event={}",
                    sessionId, severity, event);
        }
    }

    public void doAudit(String sessionId, AuditSeverity severity, String event, String details) {
        if (userWorkspaceService.exists(sessionId)) {
            Long patId = userWorkspaceService.get(sessionId).getInternalPatientId();
            doAudit(patId, severity, event, details);

        } else {
            logger.warn("attempted to generate audit for nonexistent session {}: severity={}, event={}, details={}",
                    sessionId, severity, event, details);
        }
    }

    public void doAudit(MyPatient myPatient, AuditSeverity severity, String event) {
        doAudit(myPatient, severity, event, null);
    }

    public void doAudit(MyPatient myPatient, AuditSeverity severity, String event, String details) {
        doAudit(myPatient.getId(), severity, event, details);
    }


    private void doAudit(Long patientId, AuditSeverity severity, String event, String details) {
        Audit auditData = new Audit(
                patientId,
                severity,
                event,
                LogRedactor.redact(details)
        );

        try {
            repository.save(auditData);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to create " + auditData + " - " + e.getMessage(), e);
        }
    }
}
