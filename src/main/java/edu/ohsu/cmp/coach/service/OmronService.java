package edu.ohsu.cmp.coach.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.entity.MyOmronVitals;
import edu.ohsu.cmp.coach.model.redcap.RedcapParticipantInfo;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.NotAuthenticatedException;
import edu.ohsu.cmp.coach.exception.OmronException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.MyOmronTokenData;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.model.omron.*;
import edu.ohsu.cmp.coach.repository.OmronVitalsCacheRepository;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class OmronService extends AbstractService {
    private static final DateFormat OMRON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // todo : build out services for retrieving vitals data from Omron here

    @Autowired
    private OmronVitalsCacheRepository repository;

    @Autowired
    private PatientService patientService;

    @Value("${omron.application-id}")
    private String clientId;

    @Value("${omron.secret-key}")
    private String secretKey;

    @Value("${omron.scope}")
    private String scope;

    @Value("${omron.authorize.url}")
    private String omronAuthorizeUrl;

    @Value("${omron.url}")
    private String omronUrl;

    @Value("${omron.redirect.url}")
    private String redirectUrl;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private BloodPressureService bloodPressureService;

    @Autowired
    private PulseService pulseService;

    @Autowired
    private REDCapService redCapService;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-f0-9]{64}$");

    public boolean isOmronEnabled() {
        return KEY_PATTERN.matcher(secretKey).matches();
    }

    public String getAuthorizationRequestUrl() {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not generating authorization request URL");
            return null;
        }

        try {
            // https://stg-oauth-website.ohiomron.com/connect/authorize?client_id=coach-api&response_type=code&redirect_uri=http://localhost:8082/omron/oauth&scope=bloodpressure+activity+openid+offline_access
            URLCodec urlCodec = new URLCodec();
            return omronAuthorizeUrl + "/connect/authorize?client_id=" + urlCodec.encode(clientId) +
                    "&response_type=code&redirect_uri=" + urlCodec.encode(redirectUrl) +
                    "&scope=" + urlCodec.encode(scope);

        } catch (EncoderException e) {
            throw new RuntimeException("caught " + e.getClass().getName() + " encoding Omron Authorization Request URL params - " + e.getMessage(), e);
        }
    }

    public AccessTokenResponse requestAccessToken(String authorizationCode) throws IOException, OmronException {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not requesting access token");
            return null;
        }

        logger.debug("requesting access token for authorizationCode=" + authorizationCode);

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", secretKey);
        bodyParams.put("grant_type", "authorization_code");
        bodyParams.put("scope", scope);
        bodyParams.put("redirect_uri", redirectUrl);
        bodyParams.put("code", authorizationCode);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");

        HttpResponse httpResponse = new HttpRequest().post(omronUrl + "/connect/token", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();
        String body = httpResponse.getResponseBody();

        logger.debug("got response code=" + code + ", body=" + body);

        if (code < 200 || code > 299) {
            logger.error("Omron access token request error: " + body);
            throw new OmronException("received code " + code + " getting Omron access token");

        } else {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(body, new TypeToken<AccessTokenResponse>() {}.getType());
        }
    }

    public RefreshTokenResponse refreshAccessToken(String refreshToken) throws IOException, OmronException {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not refreshing access token");
            return null;
        }

        logger.debug("refreshing access token for refreshToken=" + refreshToken);

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", secretKey);
        bodyParams.put("grant_type", "refresh_token");
        bodyParams.put("redirect_uri", redirectUrl);
        bodyParams.put("refresh_token", refreshToken);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");

        HttpResponse httpResponse = new HttpRequest().post(omronUrl + "/connect/token", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();
        String body = httpResponse.getResponseBody();

        logger.debug("got response code=" + code + ", body=" + body);

        if (code < 200 || code > 299) {
            logger.error("Omron access token refresh error: " + body);
            throw new OmronException("received HTTP " + code + " refreshing access token");

        } else {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(body, new TypeToken<RefreshTokenResponse>() {}.getType());
        }
    }

// storer 2023-10-02 - Omron updated their APIs and documentation, and they no longer document revoking access tokens
//                     as we don't use revocations in COACH, I'm commenting out this function as I don't think the API
//                     for it exists anymore.
//
//    public void revokeAccessToken(String accessToken) throws IOException, OmronException {
//        if ( ! isOmronEnabled() ) {
//            logger.warn("Omron is not enabled - not revoking access token");
//            return;
//        }
//
//        logger.debug("revoking accessToken=" + accessToken);
//
//        Map<String, String> bodyParams = new LinkedHashMap<>();
//        bodyParams.put("client_id", clientId);
//        bodyParams.put("client_secret", secretKey);
//        bodyParams.put("token", accessToken);
//        bodyParams.put("token_type_hint", "access_token");
//
//        Map<String, String> headers = new LinkedHashMap<>();
//        headers.put("Content-Type", "application/x-www-form-urlencoded");
//        headers.put("Cache-Control", "no-cache");
//
//        HttpResponse httpResponse = new HttpRequest().post(oauthHostUrl + "/connect/revocation", null, headers, bodyParams);
//        int code = httpResponse.getResponseCode();
//
//        logger.debug("got response code=" + code);
//
//        if (code < 200 || code > 299) {
//            logger.error("Omron access token revocation error: " + code);
//            throw new OmronException("received HTTP " + code + " revoking access token");
//        }
//    }

    public void scheduleAccessTokenRefresh(String sessionId) {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not scheduling access token refresh");
            return;
        }

        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        MyOmronTokenData omronTokenData = workspace.getOmronTokenData();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(RefreshTokenJob.JOBDATA_APPLICATIONCONTEXT, ctx);
        jobDataMap.put(RefreshTokenJob.JOBDATA_SESSIONID, sessionId);
        jobDataMap.put(RefreshTokenJob.JOBDATA_REFRESHTOKEN, omronTokenData.getRefreshToken());

        String id = UUID.randomUUID().toString();

        JobDetail job = JobBuilder.newJob(RefreshTokenJob.class)
                .storeDurably()
                .withIdentity("refreshTokenJob-" + id, sessionId)
                .withDescription("Refreshes Omron token for session " + sessionId)
                .usingJobData(jobDataMap)
                .build();

        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(RefreshTokenJob.class);
        jobDetailFactory.setDescription("Invoke Omron Refresh Token Job service...");
        jobDetailFactory.setDurability(true);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(omronTokenData.getExpirationTimestamp());
        calendar.add(Calendar.SECOND, -10);
//        calendar.setTime(new Date());                         // one minute refresh for debugging
//        calendar.add(Calendar.MINUTE, 1);
        Date startAtTimestamp = calendar.getTime();

        Trigger trigger = TriggerBuilder.newTrigger().forJob(job)
                .withIdentity("refreshTokenTrigger-" + id, sessionId)
                .withDescription("Refresh Token trigger")
                .startAt(startAtTimestamp)
                .build();

        try {
            if ( ! scheduler.isStarted() ) {
                scheduler.start();
            }

            logger.info("scheduling Omron token refresh for session {} at {}", sessionId, startAtTimestamp);

            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void synchronize(String sessionId) {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not synchronizing");
            return;
        }
        
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        auditService.doAudit(sessionId, AuditSeverity.INFO, "initiated Omron synchronization");

        try {
            MeasurementResult result = requestMeasurements(sessionId, workspace.getOmronLastUpdated());
            if (result.hasBloodPressures()) {
                List<OmronBloodPressureModel> list = result.getBloodPressure();
                for (int i = 0; i < list.size(); i ++) {
                    logger.debug("processing Omron record " + i + " of " + list.size());

                    workspace.setOmronSynchronizationProgress(i + 1, list.size());

                    OmronBloodPressureModel model = list.get(i);

                    // first persist the original object to local cache
                    MyOmronVitals vitals = null;
                    try {
                        vitals = writeToPersistentCache(workspace.getInternalPatientId(), model);

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getName() + " persisting Omron vitals - " + e.getMessage() + " - skipping -", e);
                        logger.debug("vitals = " + model);
                    }

                    if (vitals != null) {
                        // now create this blood pressure into our home blood pressure model table (if applicable)
                        if (vitals.getSystolic() != null && vitals.getDiastolic() != null) {
                            try {
                                bloodPressureService.create(sessionId, new BloodPressureModel(vitals, fcm));

                            } catch (Exception e) {
                                // remote errors are tolerable, since we will always store locally too
                                logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create BP - " + e.getMessage(), e);
                            }
                        }

                        // and also persist pulse into our home pulse model table (if applicable)
                        if (vitals.getPulse() != null) {
                            try {
                                pulseService.create(sessionId, new PulseModel(vitals, fcm));

                            } catch (Exception e) {
                                // remote errors are tolerable, since we will always store locally too
                                logger.warn("caught " + e.getClass().getSimpleName() + " attempting to create Pulse - " + e.getMessage(), e);
                            }
                        }
                    }

// storer : useful for debugging
//                    try {
//                        logger.debug("sleeping 800ms");
//                        Thread.sleep(800);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                }
            }

            Date lastUpdated = new Date();
            patientService.setOmronLastUpdated(workspace.getInternalPatientId(), lastUpdated);
            workspace.setOmronLastUpdated(lastUpdated);
            logger.debug("completed Omron synchronization");

        } catch (OmronException e) {
            logger.error("caught " + e.getClass().getName() + " updating vitals cache - " + e.getMessage(), e);
        } catch (NotAuthenticatedException nae) {
            // handle silently
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            workspace.clearCaches();
        }
    }

    public List<OmronVitals> readFromPersistentCache(String sessionId) {
        // local operation only - permit even if Omron is disabled
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        List<OmronVitals> list = new ArrayList<>();
        for (MyOmronVitals vitals : repository.findAllByPatId(workspace.getInternalPatientId())) {
            try {
                BloodPressureModel bpModel = null;
                PulseModel pulseModel = null;

                if (vitals.getSystolic() != null && vitals.getDiastolic() != null) {
                    try {
                        bpModel = new BloodPressureModel(vitals, fcm);
                    } catch (DataException e) {
                        logger.error("caught " + e.getClass().getName() + " building BloodPressureModel from MyOmronVitals - " + e.getMessage(), e);
                    }
                }

                if (vitals.getPulse() != null) {
                    try {
                        pulseModel = new PulseModel(vitals, fcm);
                    } catch (DataException e) {
                        logger.error("caught " + e.getClass().getName() + " building PulseModel from MyOmronVitals - " + e.getMessage(), e);
                    }
                }

                list.add(new OmronVitals(bpModel, pulseModel));

            } catch (ParseException e) {
                logger.error("parse error processing Omron vitals with id=" + vitals.getId() + " - skipping -");
            }
        }
        return list;
    }

    public void deleteAll(String sessionId) {
        // local operation only - permit even if Omron is disabled
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        repository.deleteAllByPatId(workspace.getInternalPatientId());
    }

    public void resetLastUpdated(String sessionId) {
        // local operation only - permit even if Omron is disabled
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        patientService.setOmronLastUpdated(workspace.getInternalPatientId(), null);
    }


//////////////////////////////////////////////////////////////////////////////
// private methods
//

    private MeasurementResult requestMeasurements(String sessionId, Date sinceTimestamp) throws IOException, NotAuthenticatedException, OmronException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        MyOmronTokenData tokenData = workspace.getOmronTokenData();
        if (tokenData == null) {
            throw new NotAuthenticatedException("No Omron authentication token data found");
        }

        Map<String, String> bodyParams = new LinkedHashMap<>();

        if (sinceTimestamp == null) {
            Date date;
            if (redCapService.isRedcapEnabled()) {
                RedcapParticipantInfo redcapParticipantInfo = redCapService.getParticipantInfo(workspace.getRedcapId());
                if (redCapService.isUsersWithoutRedcapRecordBypassStudyEnabled() && ! redcapParticipantInfo.getExists()) {
                    // this is fine, just use right now as a starting date instead of when the user was randomized
                    date = new Date();
                } else {
                    date = redcapParticipantInfo.getRandomizationDate();
                }
            } else {
                date = new Date();
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, -30);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            sinceTimestamp = calendar.getTime();
        }
        bodyParams.put("since", OMRON_DATE_FORMAT.format(sinceTimestamp));

        logger.info("requesting Omron measurements since " + OMRON_DATE_FORMAT.format(sinceTimestamp) + " for session " + sessionId);

//        bodyParams.put("limit", limit);           // optional
        bodyParams.put("type", "bloodpressure");
        bodyParams.put("includeHourlyActivity", "false");
//        bodyParams.put("sortOrder", "desc");        // optional

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + tokenData.getBearerToken());
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse httpResponse = new HttpRequest().post(omronUrl + "/api/measurement", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();
        String body = httpResponse.getResponseBody();

        if (code < 200 || code > 299) {
            logger.error("Omron measurement request error: " + body);
            throw new OmronException("received HTTP " + code + " building vitals for session " + sessionId);

        } else {
            logger.debug("got body (PARSE THIS WITH GSON): " + body);
            Gson gson = new GsonBuilder().create();
            MeasurementResponse response = gson.fromJson(body, new TypeToken<MeasurementResponse>() {}.getType());

            // todo : do something with response.getStatus()?

            return response.getResult();
        }
    }

    private MyOmronVitals writeToPersistentCache(Long internalPatientId, OmronBloodPressureModel model) {
        if ( ! repository.existsByOmronId(model.getId()) ) {
            logger.info("caching Omron vitals with id=" + model.getId() + " for patient with id=" + internalPatientId);
            MyOmronVitals vitals = new MyOmronVitals(model);
            vitals.setPatId(internalPatientId);
            vitals.setCreatedDate(new Date());
            return repository.save(vitals);
        } else {
            logger.debug("not caching Omron vitals with id=" + model.getId() + " - already exists!");
            return null;
        }
    }
}
