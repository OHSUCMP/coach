package edu.ohsu.cmp.coach.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.config.RedcapConfiguration;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.RedcapDataAccessGroup;
import edu.ohsu.cmp.coach.model.redcap.RedcapParticipantInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class REDCapService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Cache<String, String> recordIdCache;
    private final Cache<String, RedcapParticipantInfo> participantInfoCache;

    /* Standard REDCap Values */
    public static final String YES = "1";
    public static final String NO = "0";
    public static final String FORM_INCOMPLETE = "0";
    public static final String FORM_UNVERIFIED = "1";
    public static final String FORM_COMPLETE = "2";

    // REDCap Events
    public static final String PARTICIPANT_BASELINE_EVENT = "baseline_arm_1";
    public static final String PARTICIPANT_ONGOING_EVENT = "ongoing_arm_1";

    // REDCap Forms
    public static final String PARTICIPANT_INFO_FORM = "participant_info";
    public static final String PARTICIPANT_INFORMATION_SHEET_FORM = "information_sheet";
    public static final String PARTICIPANT_CONSENT_FORM = "coach_informed_consent";
    public static final String PARTICIPANT_RANDOMIZATION_FORM = "staff_coach_randomization";
    public static final String PARTICIPANT_DISPOSITION_FORM = "staff_participant_administration_and_disposition";
    public static final String ADVERSE_EVENT_FORM = "adverse_event_form";

    // REDCap Fields
    public static final String PARTICIPANT_RECORD_ID_FIELD = "record_id";
    public static final String PARTICIPANT_COACH_ID_FIELD = "coach_id";
    public static final String PARTICIPANT_COACH_URL_FIELD = "coach_url";
    public static final String PARTICIPANT_RECORD_DATE_FIELD = "record_dat";
    public static final String PARTICIPANT_CONSENT_FIELD = "icf_consent_73fb68";
    public static final String PARTICIPANT_VUMC_ADDITIONAL_CONSENT = "authorize_vumc_hipaa";
    public static final String PARTICIPANT_RANDOMIZATION_FIELD = "randomized_assignment";
    public static final String PARTICIPANT_RANDOMIZATION_DATE_FIELD = "randomization_date";
    public static final String PARTICIPANT_DISPOSITION_WITHDRAW_FIELD = "withdraw";
    public static final String PARTICIPANT_COMPLETED_PER_PROTOCOL_FIELD = "per_protocol";

    @Autowired
    RedcapConfiguration redcapConfiguration;

    @Value("${redcap.patient-launch-url}")
    private String launchUrl;

    @Value("${redcap.data-access-group}")
    private String redcapDataAccessGroupStr;

    @Value("#{new Boolean('${redcap.users-without-redcap-record-bypass-study.enabled}')}")
    private Boolean usersWithoutRedcapRecordBypassStudyEnabled;

    public REDCapService() {
        recordIdCache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        participantInfoCache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();
    }

    /**
     * Return whether the REDCap flow is enabled for this application
     * @return
     */
    public boolean isRedcapEnabled() {
        return redcapConfiguration.getEnabled();
    }

    public boolean isUsersWithoutRedcapRecordBypassStudyEnabled() {
        return usersWithoutRedcapRecordBypassStudyEnabled;
    }

    public void clearCaches(String coachId) {
        logger.debug("clearing REDCap caches for coachId=" + coachId);
        recordIdCache.invalidate(coachId);
        participantInfoCache.invalidate(coachId);
    }

    private String getRecordId(String coachId) throws IOException, REDCapException {
        return recordIdCache.get(coachId, new Function<String, String>() {
            @Override
            public String apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build REDCap record ID for coachId=" + s);

                String recordId = null;
                try {
                    recordId = buildRecordId(s);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building REDCap record ID for coachId=" + s +
                        " (took " + (System.currentTimeMillis() - start) + "ms)");

                return recordId;
            }
        });
    }

    /**
     * Using the coach uuid, get the record id in REDCap
     * @param coachId
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    private String buildRecordId(String coachId) throws IOException, REDCapException {
        logger.debug("buildRecordId for coachId=" + coachId);

        Assert.notNull(coachId, "A COACH Id must be provided to get the REDCap record.");
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("rawOrLabel", "raw");
        bodyParams.put("events", PARTICIPANT_BASELINE_EVENT);
        bodyParams.put("fields", PARTICIPANT_RECORD_ID_FIELD);
        bodyParams.put("filterLogic", "[coach_id]='" + coachId + "'");

        HttpResponse response = new HttpRequest().post(
            redcapConfiguration.getApiUrl(),
            null,
            requestHeaders,
            bodyParams
        );

        checkException(response);

        Gson gson = new Gson();
        List<Map<String, String>> records = gson.fromJson(response.getResponseBody(), new TypeToken<List<Map<String, String>>>(){}.getType());

        if (records.isEmpty()) {
            return null;
        } else {
            return records.get(0).get(PARTICIPANT_RECORD_ID_FIELD);
        }
    }

    public RedcapParticipantInfo getParticipantInfo(String coachId) throws IOException, REDCapException {
        return participantInfoCache.get(coachId, new Function<String, RedcapParticipantInfo>() {
            @Override
            public RedcapParticipantInfo apply(String s) {
                long start = System.currentTimeMillis();
                logger.info("BEGIN build RedcapParticipantInfo for coachId=" + s);

                RedcapParticipantInfo participantInfo = null;
                try {
                    participantInfo = buildParticipantInfo(s);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                logger.info("DONE building RedcapParticipantInfo for coachId=" + s +
                        " (took " + (System.currentTimeMillis() - start) + "ms)");

                return participantInfo;
            }
        });
    }

    /**
     * Return a summary of the participant's status in the study
     * @param coachId The uuid COACH generates and stores to map to REDCap
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    private RedcapParticipantInfo buildParticipantInfo(String coachId) throws IOException, REDCapException {
        logger.debug("buildParticipantInfo for coachId=" + coachId);

        RedcapDataAccessGroup dag = RedcapDataAccessGroup.fromTag(redcapDataAccessGroupStr);
        
        String recordId = getRecordId(coachId);
        if (recordId == null) {
            logger.info("No record found in REDCap. Returning provisional participant info record for " + coachId);
            return RedcapParticipantInfo.buildNotExists(coachId);
        }

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("rawOrLabel", "raw");
        bodyParams.put("events", PARTICIPANT_BASELINE_EVENT + "," + PARTICIPANT_ONGOING_EVENT);
        // Forms must be specified so we don't get repeat instances of other forms impacting the expected results
        bodyParams.put("forms", PARTICIPANT_INFO_FORM + "," + PARTICIPANT_INFORMATION_SHEET_FORM + "," + PARTICIPANT_CONSENT_FORM + "," + PARTICIPANT_RANDOMIZATION_FORM + "," + PARTICIPANT_DISPOSITION_FORM);
        bodyParams.put("records", recordId);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        checkException(response);

        Gson gson = new Gson();
        List<Map<String, String>> records = gson.fromJson(response.getResponseBody(), new TypeToken<List<Map<String, String>>>(){}.getType());

        if (records.size() == 1) {
            return RedcapParticipantInfo.buildFromRecord(coachId, dag, records.get(0), new LinkedHashMap<String,String>());
        } else if (records.size() == 2) {
            // Distinguish the baseline and ongoing events.
            Map<String,String> record1 = records.get(0);
            Map<String,String> record2 = records.get(1);
            if (StringUtils.equals(record1.get("redcap_event_name"), PARTICIPANT_BASELINE_EVENT)) {
                return RedcapParticipantInfo.buildFromRecord(coachId, dag, record1, record2);
            } else {
                return RedcapParticipantInfo.buildFromRecord(coachId, dag, record2, record1);
            }                
        } else {
            throw new REDCapException(422, "found too many records for (" + recordId + ") - expected 1, found " + records.size(), null);
        }

    }

    /**
     * Create a new record for the subject in REDCap
     * @param coachId
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    public String createSubjectInfoRecord(String coachId) throws IOException, REDCapException {
        logger.debug("createSubjectInfoRecord for coachId=" + coachId);

        Map<String, String> map = new LinkedHashMap<>();
        map.put(PARTICIPANT_RECORD_ID_FIELD, coachId);
        map.put(PARTICIPANT_COACH_ID_FIELD, coachId);
        map.put("redcap_event_name", PARTICIPANT_BASELINE_EVENT);
        map.put(PARTICIPANT_INFO_FORM + "_complete", FORM_COMPLETE);
        map.put("redcap_data_access_group", redcapConfiguration.getDataAccessGroup());
        map.put(PARTICIPANT_COACH_URL_FIELD, launchUrl);
        map.put(PARTICIPANT_RECORD_DATE_FIELD, LocalDate.now().toString());

        List<Map<String, String>> list = new ArrayList<>();
        list.add(map);

        Gson gson = new Gson();
        String data = gson.toJson(list, new TypeToken<List<Map<String, String>>>(){}.getType());

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("forceAutoNumber", "true");
        bodyParams.put("data", data);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        checkException(response);
        return getRecordId(coachId);
    }

    /**
     * Get the url for the entry survey into the REDCap flow
     * @param recordId
     * @return
     * @throws REDCapException
     * @throws IOException
     */
    public String getEntrySurveyLink(String recordId) throws REDCapException, IOException {
        logger.debug("getEntrySurveyLink for recordId=" + recordId);

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "surveyLink");
        bodyParams.put("record", recordId);
        bodyParams.put("event", PARTICIPANT_BASELINE_EVENT);
        bodyParams.put("instrument", PARTICIPANT_INFORMATION_SHEET_FORM);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        checkException(response);
        return response.getResponseBody();
    }

    /**
     * Get the url to the participant's survey queue
     * @param recordId
     * @return
     * @throws REDCapException
     * @throws IOException
     */
    public String getSurveyQueueLink(String recordId) throws REDCapException, IOException {
        logger.debug("getSurveyQueueLink for recordId=" + recordId);

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "surveyQueueLink");
        bodyParams.put("record", recordId);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        checkException(response);
        return response.getResponseBody();
    }

    /**
     * Get the url for the next AE report for the participant
     * @param coachId
     * @return
     * @throws REDCapException
     * @throws IOException
     */
    public String getAESurveyLink(String coachId) throws REDCapException, IOException {
        // First get the recordId from the REDCap id
        logger.debug("getAESurveyLink for coachId=" + coachId);

        String recordId = getRecordId(coachId);
        Assert.notNull(recordId, "No REDCap record exists for COACH id " + coachId);

        // Next, see if there are completed AEs and calculate the repeat instance
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("rawOrLabel", "raw");
        bodyParams.put("events", PARTICIPANT_ONGOING_EVENT);
        bodyParams.put("records", recordId);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        checkException(response);
        Gson gson = new Gson();
        List<Map<String, String>> records = gson.fromJson(response.getResponseBody(), new TypeToken<List<Map<String, String>>>(){}.getType());

        if (records.isEmpty()) {
            return getAESurveyLinkForRepeatInstance(recordId, 1);
        } else {
            Optional<Integer> maxRepeatInstance = records.stream().filter(it->ADVERSE_EVENT_FORM.equals(it.get("redcap_repeat_instrument"))).map(it -> Integer.parseInt(it.get("redcap_repeat_instance"))).max(Integer::compare);
            Integer repeatInstance = maxRepeatInstance.isEmpty() ? 1 : maxRepeatInstance.get() + 1;
            return getAESurveyLinkForRepeatInstance(recordId, repeatInstance);
        }
    }

    private String getAESurveyLinkForRepeatInstance(String recordId, int i) throws REDCapException, IOException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "surveyLink");
        bodyParams.put("record", recordId);
        bodyParams.put("event", PARTICIPANT_ONGOING_EVENT);
        bodyParams.put("instrument", ADVERSE_EVENT_FORM);
        bodyParams.put("repeat_instance", String.valueOf(i));

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        checkException(response);
        return response.getResponseBody();
    }

    private void checkException(HttpResponse response) throws REDCapException {
        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            // We obscure the details of the REDCap error from the user. If the SessionController can't reach REDCap,
            // perhaps because it's down for maintenance, the user will get this nicer message so they don't keep trying.
            throw new REDCapException(503, "COACH is temporarily unavailable. Please try again later.", null);
        }
    }

}
