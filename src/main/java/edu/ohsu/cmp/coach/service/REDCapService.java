package edu.ohsu.cmp.coach.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.ohsu.cmp.coach.config.RedcapConfiguration;
import edu.ohsu.cmp.coach.entity.RedcapParticipantInfo;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class REDCapService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
    public static final String PARTICIPANT_CONSENT_FIELD = "icf_consent_73fb68";
    public static final String PARTICIPANT_RANDOMIZATION_FIELD = "randomized_assignment";
    public static final String PARTICIPANT_RANDOMIZATION_DATE_FIELD = "randomization_date";
    public static final String PARTICIPANT_DISPOSITION_WITHDRAW_FIELD = "withdraw";

    @Autowired
    RedcapConfiguration redcapConfiguration;

    @Value("${redcap.patient-launch-url}")
    private String launchUrl;

    /**
     * Return whether the REDCap flow is enabled for this application
     * @return
     */
    public boolean isRedcapEnabled() {
        return redcapConfiguration.getEnabled();
    }

    /**
     * Using the coach uuid, get the record id in REDCap
     * @param coachId
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    private String getRecordId(String coachId) throws IOException, REDCapException {
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

    /**
     * Return a summary of the participant's status in the study
     * @param coachId The uuid COACH generates and stores to map to REDCap
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    public RedcapParticipantInfo getParticipantInfo(String coachId) throws IOException, REDCapException {

        String recordId = getRecordId(coachId);
        if (recordId == null) {
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
            return RedcapParticipantInfo.buildFromRecord(coachId, records.get(0), new LinkedHashMap<String,String>());
        } else if (records.size() == 2) {
            // Distinguish the baseline and ongoing events.
            Map<String,String> record1 = records.get(0);
            Map<String,String> record2 = records.get(1);
            if (StringUtils.equals(record1.get("redcap_event_name"), PARTICIPANT_BASELINE_EVENT)) {
                return RedcapParticipantInfo.buildFromRecord(coachId, record1, record2);
            } else {
                return RedcapParticipantInfo.buildFromRecord(coachId, record2, record1);
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
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PARTICIPANT_RECORD_ID_FIELD, coachId);
        map.put(PARTICIPANT_COACH_ID_FIELD, coachId);
        map.put("redcap_event_name", PARTICIPANT_BASELINE_EVENT);
        map.put(PARTICIPANT_INFO_FORM + "_complete", FORM_COMPLETE);
        map.put("redcap_data_access_group", redcapConfiguration.getDataAccessGroup());
        map.put("coach_url", launchUrl);

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
        String recordId = getRecordId(coachId);

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
        if (code == 400) {
            // This is the code REDCap sends when it is offline for maintenance, but it seems to be thrown in other circumstances too.
            logger.error("REDCap returned a 400, which usually means it's down for maintenance. The response body: " + response.getResponseBody());
            throw new REDCapException(400, "COACH is temporarily unavailable. Please try again later.", null);
        } else if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException(code, response.getResponseBody(), null);
        }
    }

}
