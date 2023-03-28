package edu.ohsu.cmp.coach.session;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ExpireProvisionalSessionJob implements Job {
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

        SessionService sessionService = ctx.getBean(SessionService.class);
        try {
            sessionService.expireProvisional(sessionId);

        } catch (Exception e) {
            throw new JobExecutionException("caught " + e.getClass().getName() + " executing job for session=" +
                    sessionId + " - " + e.getMessage(), e);
        }
    }
}
