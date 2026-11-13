package edu.ohsu.cmp.coach.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import edu.ohsu.cmp.coach.model.redcap.RandomizationGroup;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.model.Audience;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.session.ExpireProvisionalSessionJob;
import edu.ohsu.cmp.coach.session.ProvisionalSessionCacheData;
import edu.ohsu.cmp.coach.util.FhirUtil;
import jakarta.annotation.PostConstruct;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // fallbacks so a context can never land on HAPI's defaults (10s socket timeout, 20 connections)
    public static final int DEFAULT_SOCKET_TIMEOUT = 5 * 60 * 1000;
    public static final int DEFAULT_POOL_MAX_TOTAL = 200;
    public static final int DEFAULT_POOL_MAX_PER_ROUTE = 100;
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 60 * 1000;

    @Value("${fhir.socket.timeout:" + DEFAULT_SOCKET_TIMEOUT + "}")
    private int socketTimeout;

    @Value("${fhir.pool.maxTotal:" + DEFAULT_POOL_MAX_TOTAL + "}")
    private int poolMaxTotal;

    @Value("${fhir.pool.maxPerRoute:" + DEFAULT_POOL_MAX_PER_ROUTE + "}")
    private int poolMaxPerRoute;

    @Value("${fhir.pool.connectionRequestTimeout:" + DEFAULT_CONNECTION_REQUEST_TIMEOUT + "}")
    private int connectionRequestTimeout;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Scheduler scheduler;

    private final Map<String, ProvisionalSessionCacheData> provisionalCache;

    private FhirContext fhirContext;

    @PostConstruct
    private void initFhirContext() {
        // initialize fhirContext in @PostConstruct to ensure that injected @Values are populated
        FhirContext ctx = FhirContext.forR4();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.ONCE);
        configureClientFactory(ctx, socketTimeout, poolMaxTotal, poolMaxPerRoute, connectionRequestTimeout);
        fhirContext = ctx;
    }

    private void configureClientFactory(FhirContext ctx, int socketTimeout, int poolMaxTotal,
                                        int poolMaxPerRoute, int connectionRequestTimeout) {
        var factory = ctx.getRestfulClientFactory();
        factory.setSocketTimeout(socketTimeout);
        factory.setPoolMaxTotal(poolMaxTotal);
        factory.setPoolMaxPerRoute(poolMaxPerRoute);

        // an exhausted pool otherwise fails the lease after 10s, and callers treat that as a dead
        // server rather than backpressure.  waiting is cheap on a virtual thread, so wait instead
        factory.setConnectionRequestTimeout(connectionRequestTimeout);
    }

    public SessionService() {
        provisionalCache = new ConcurrentHashMap<>();
    }

    public void prepareSession(String sessionId, FHIRCredentials credentials, Audience audience, RandomizationGroup randomizationGroup,
                               boolean requiresEnrollment, boolean hasCompletedStudy) throws ConfigurationException {
        logger.debug("preparing session " + sessionId + " with credentials=" + credentials);
        IGenericClient client = FhirUtil.buildClient(
                fhirContext,
                credentials.getServerURL(),
                credentials.getBearerToken()
        );
        FHIRCredentialsWithClient fcc = new FHIRCredentialsWithClient(credentials, client);

        userWorkspaceService.init(sessionId, audience, randomizationGroup, requiresEnrollment, hasCompletedStudy, fcc);
        userWorkspaceService.get(sessionId).populate();

        auditService.doAudit(sessionId, AuditSeverity.INFO, "session established", "sessionId=" + sessionId +
                ", audience=" + audience + ", randomizationGroup=" + randomizationGroup);
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

    public void forceExpiration(String sessionId) {
        if (userWorkspaceService.exists(sessionId)) {
            logger.info("expiring credentials for session {}", sessionId);
            auditService.doAudit(sessionId, AuditSeverity.INFO, "session expired", sessionId);
            userWorkspaceService.shutdown(sessionId);
        }
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
