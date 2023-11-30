package edu.ohsu.cmp.coach.session;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.coach.entity.RandomizationGroup;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.service.AbstractService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.*;

@Service
public class SessionService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${socket.timeout:300000}")
    private Integer socketTimeout;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Scheduler scheduler;

    private final Map<String, ProvisionalSessionCacheData> provisionalCache;

    public SessionService() {
        provisionalCache = new HashMap<>();
    }

    public void prepareSession(String sessionId, FHIRCredentials credentials, Audience audience, RandomizationGroup randomizationGroup) throws ConfigurationException {
        logger.debug("preparing session " + sessionId + " with credentials=" + credentials);
        IGenericClient client = FhirUtil.buildClient(
                credentials.getServerURL(),
                credentials.getBearerToken(),
                socketTimeout
        );
        FHIRCredentialsWithClient fcc = new FHIRCredentialsWithClient(credentials, client);

        userWorkspaceService.init(sessionId, audience, randomizationGroup, fcc);
        userWorkspaceService.get(sessionId).populate();
    }

    public void prepareProvisionalSession(String sessionId, FHIRCredentials credentials, Audience audience) {
        logger.debug("preparing provisional session " + sessionId + " with credentials=" + credentials);
        ProvisionalSessionCacheData cacheData = new ProvisionalSessionCacheData(credentials, audience);
        provisionalCache.put(sessionId, cacheData);
        scheduleExpireProvisional(sessionId);
    }

    public boolean exists(String sessionId) {
        return userWorkspaceService.exists(sessionId);
    }

    public boolean existsProvisional(String sessionId) {
        return provisionalCache.containsKey(sessionId);
    }

    public ProvisionalSessionCacheData getProvisionalSessionData(String sessionId) {
        return provisionalCache.get(sessionId);
    }

    public void expireProvisional(String sessionId) {
        logger.info("expiring provisional credentials for session " + sessionId);
        provisionalCache.remove(sessionId);
    }

    public void expireAll(String sessionId) {
        logger.info("expiring credentials for session " + sessionId);
        provisionalCache.remove(sessionId);
        userWorkspaceService.shutdown(sessionId);
    }

/////////////////////////////////////////////////////////////
// private stuff
//

    // provisional cache data is configured to expire after 1 hour, to ensure that sensitive
    // information doesn't persist indefinitely if the user exits the workflow prematurely
    private void scheduleExpireProvisional(String sessionId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ExpireProvisionalSessionJob.JOBDATA_APPLICATIONCONTEXT, ctx);
        jobDataMap.put(ExpireProvisionalSessionJob.JOBDATA_SESSIONID, sessionId);

        String id = UUID.randomUUID().toString();

        JobDetail job = JobBuilder.newJob(ExpireProvisionalSessionJob.class)
                .storeDurably()
                .withIdentity("expireProvisionalJob-" + id, sessionId)
                .withDescription("Expires provisional session " + sessionId)
                .usingJobData(jobDataMap)
                .build();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, 1);
        Date startAtTimestamp = calendar.getTime();

        Trigger trigger = TriggerBuilder.newTrigger().forJob(job)
                .withIdentity("expireProvisionalTrigger-" + id, sessionId)
                .withDescription("Expire provisional session trigger")
                .startAt(startAtTimestamp)
                .build();

        try {
            if ( ! scheduler.isStarted() ) {
                scheduler.start();
            }

            logger.info("scheduling expiration of provisional session {} at {}", sessionId, startAtTimestamp);

            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
