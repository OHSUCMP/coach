package edu.ohsu.cmp.coach.workspace;

import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.service.AuditService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ShutdownWorkspaceJob implements Job {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String JOBDATA_APPLICATIONCONTEXT = "applicationContext";
    public static final String JOBDATA_SESSIONID = "sessionId";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String name = jobExecutionContext.getJobDetail().getKey().getName();

        logger.info("running job {} fired at {}", name, jobExecutionContext.getFireTime());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        ApplicationContext ctx = (ApplicationContext) jobDataMap.get(JOBDATA_APPLICATIONCONTEXT);
        String sessionId = jobDataMap.getString(JOBDATA_SESSIONID);

        UserWorkspaceService userWorkspaceService = ctx.getBean(UserWorkspaceService.class);
        if (userWorkspaceService.exists(sessionId)) {
            logger.info("expiring credentials for session " + sessionId);
            AuditService auditService = ctx.getBean(AuditService.class);
            auditService.doAudit(sessionId, AuditSeverity.INFO, "session expired", sessionId);
            userWorkspaceService.shutdown(sessionId);

        } else {
            logger.info("user workspace for session " + sessionId + " no longer exists, so nothing to shut down.");
        }
    }
}
