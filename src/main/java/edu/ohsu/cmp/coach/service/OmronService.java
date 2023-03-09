package edu.ohsu.cmp.coach.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.entity.MyOmronVitals;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.NotAuthenticatedException;
import edu.ohsu.cmp.coach.exception.OmronException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.MyOmronTokenData;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.model.omron.*;
import edu.ohsu.cmp.coach.repository.OmronVitalsCacheRepository;
import edu.ohsu.cmp.coach.util.UUIDUtil;
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

    @Value("${omron.oauth-host.url}")
    private String oauthHostUrl;

    @Value("${omron.api-host.url}")
    private String apiHostUrl;

    @Value("${omron.redirect.url}")
    private String redirectUrl;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Scheduler scheduler;

    public boolean isOmronEnabled() {
        return UUIDUtil.isUUID(secretKey);
    }

    public String getAuthorizationRequestUrl() throws DataException {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not generating authorization request URL");
            return null;
        }

        try {
            URLCodec urlCodec = new URLCodec();
            return oauthHostUrl + "/connect/authorize?client_id=" + urlCodec.encode(clientId) +
                    "&response_type=code&scope=" + urlCodec.encode(scope) +
                    "&redirect_uri=" + urlCodec.encode(redirectUrl);

        } catch (EncoderException e) {
            throw new DataException("caught " + e.getClass().getName() + " encoding Omron Authorization Request URL params - " + e.getMessage(), e);
        }
    }

    public AccessTokenResponse requestAccessToken(String authorizationCode) throws IOException, EncoderException, OmronException {
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

        HttpResponse httpResponse = new HttpRequest().post(oauthHostUrl + "/connect/token", null, headers, bodyParams);
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

    public RefreshTokenResponse refreshAccessToken(String refreshToken) throws IOException, EncoderException, OmronException {
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

        HttpResponse httpResponse = new HttpRequest().post(oauthHostUrl + "/connect/token", null, headers, bodyParams);
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

    public void revokeAccessToken(String accessToken) throws EncoderException, IOException, OmronException {
        if ( ! isOmronEnabled() ) {
            logger.warn("Omron is not enabled - not revoking access token");
            return;
        }

        logger.debug("revoking accessToken=" + accessToken);

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", secretKey);
        bodyParams.put("token", accessToken);
        bodyParams.put("token_type_hint", "access_token");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");

        HttpResponse httpResponse = new HttpRequest().post(oauthHostUrl + "/connect/revocation", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();

        logger.debug("got response code=" + code);

        if (code < 200 || code > 299) {
            logger.error("Omron access token revocation error: " + code);
            throw new OmronException("received HTTP " + code + " revoking access token");
        }
    }

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
        try {
            MeasurementResult result = requestMeasurements(sessionId, workspace.getOmronLastUpdated());
            if (result.hasBloodPressures()) {
                for (OmronBloodPressureModel model : result.getBloodPressure()) {
                    writeToPersistentCache(workspace.getInternalPatientId(), model);
                }
            }

            Date lastUpdated = new Date();
            patientService.setOmronLastUpdated(workspace.getInternalPatientId(), lastUpdated);
            workspace.setOmronLastUpdated(lastUpdated);

            workspace.clearOmronCache();

        } catch (OmronException e) {
            logger.error("caught " + e.getClass().getName() + " updating vitals cache - " + e.getMessage(), e);
        } catch (NotAuthenticatedException nae) {
            // handle silently
        } catch (EncoderException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<OmronVitals> readFromPersistentCache(String sessionId) {
        // local operation only - permit even if Omron is disabled
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        List<OmronVitals> list = new ArrayList<>();
        for (MyOmronVitals vitals : repository.findAllByPatId(workspace.getInternalPatientId())) {
            try {
                BloodPressureModel bpModel = new BloodPressureModel(vitals, fcm);
                PulseModel pulseModel = new PulseModel(vitals, fcm);
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

    private MeasurementResult requestMeasurements(String sessionId, Date sinceTimestamp) throws EncoderException, IOException, NotAuthenticatedException, OmronException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        MyOmronTokenData tokenData = workspace.getOmronTokenData();
        if (tokenData == null) {
            throw new NotAuthenticatedException("No Omron authentication token data found");
        }

        Map<String, String> bodyParams = new LinkedHashMap<>();

        if (sinceTimestamp == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.YEAR, -2);
            sinceTimestamp = calendar.getTime();
        }
        bodyParams.put("since", OMRON_DATE_FORMAT.format(sinceTimestamp));

//        bodyParams.put("limit", limit);           // optional
        bodyParams.put("type", "bloodpressure");
        bodyParams.put("includeHourlyActivity", "false");
//        bodyParams.put("sortOrder", "desc");        // optional

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + tokenData.getBearerToken());
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse httpResponse = new HttpRequest().post(apiHostUrl + "/api/measurement", null, headers, bodyParams);
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

    private void writeToPersistentCache(Long internalPatientId, OmronBloodPressureModel model) {
        if ( ! repository.existsByOmronId(model.getId()) ) {
            logger.info("caching Omron vitals with id=" + model.getId() + " for patient with id=" + internalPatientId);
            MyOmronVitals vitals = new MyOmronVitals(model);
            vitals.setPatId(internalPatientId);
            vitals.setCreatedDate(new Date());
            repository.save(vitals);
        } else {
            logger.debug("not caching Omron vitals with id=" + model.getId() + " - already exists!");
        }
    }
}
