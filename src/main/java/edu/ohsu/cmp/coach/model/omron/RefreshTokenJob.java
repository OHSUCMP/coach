package edu.ohsu.cmp.coach.model.omron;

import edu.ohsu.cmp.coach.model.MyOmronTokenData;
import edu.ohsu.cmp.coach.service.OmronService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.workspace.UserWorkspaceService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenJob implements Job {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String JOBDATA_APPLICATIONCONTEXT = "applicationContext";
    public static final String JOBDATA_SESSIONID = "sessionId";
    public static final String JOBDATA_REFRESHTOKEN = "refreshToken";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String name = jobExecutionContext.getJobDetail().getKey().getName();

        logger.info("running job {} fired at {}", name, jobExecutionContext.getFireTime());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        ApplicationContext ctx = (ApplicationContext) jobDataMap.get(JOBDATA_APPLICATIONCONTEXT);
        String sessionId = jobDataMap.getString(JOBDATA_SESSIONID);
        String refreshToken = jobDataMap.getString(JOBDATA_REFRESHTOKEN);

        UserWorkspaceService userWorkspaceService = ctx.getBean(UserWorkspaceService.class);
        OmronService omronService = ctx.getBean(OmronService.class);

        try {
            RefreshTokenResponse response = omronService.refreshAccessToken(refreshToken);
            UserWorkspace workspace = userWorkspaceService.get(sessionId);
            MyOmronTokenData tokenData = workspace.getOmronTokenData();
            tokenData.update(response);
            workspace.setOmronTokenData(tokenData);

        } catch (Exception e) {
            throw new JobExecutionException("caught " + e.getClass().getName() + " executing job for session=" +
                    sessionId + " - " + e.getMessage(), e);
        }
    }
}
