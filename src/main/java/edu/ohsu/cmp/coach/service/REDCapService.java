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

@Service
public class REDCapService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /* Standard REDCap Values */
    public static final String YES = "1";
    public static final String NO = "0";
    public static final String FORM_INCOMPLETE = "0";
    public static final String FORM_UNVERIFIED = "1";
    public static final String FORM_COMPLETE = "2";

    private static final String PARTICIPANT_BASELINE_EVENT = "baseline_arm_1";
    private static final String PARTICIPANT_ONGOING_EVENT = "ongoing_arm_1";
    private static final String PARTICIPANT_INFO_FORM = "participant_info";
    private static final String ENTRY_FORM = "information_sheet";

    @Autowired
    RedcapConfiguration redcapConfiguration;

    @Value("${launch.url}")
    private String launchUrl;

    /**
     * Return whether the REDCap flow is enabled for this application
     * @return
     */
    public boolean isRedcapEnabled() {
        return redcapConfiguration.getEnabled();
    }

    /**
     * Return a summary of the participant's status in the study
     * @param redcapId
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    public RedcapParticipantInfo getParticipantInfo(String redcapId) throws IOException, REDCapException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "record");
        bodyParams.put("format", "json");
        bodyParams.put("type", "flat");
        bodyParams.put("rawOrLabel", "raw");
        bodyParams.put("events", "'"+ PARTICIPANT_BASELINE_EVENT + "', '" + PARTICIPANT_ONGOING_EVENT + "'");
        bodyParams.put("records", redcapId);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            Gson gson = new Gson();
            List<Map<String, String>> records = gson.fromJson(response.getResponseBody(), new TypeToken<List<Map<String, String>>>(){}.getType());

            if (records.isEmpty()) {
                return RedcapParticipantInfo.buildNotExists(redcapId);
            } else if (records.size() == 1) {
                return RedcapParticipantInfo.buildFromRecord(redcapId, records.get(0), new LinkedHashMap<String,String>());
            } else if (records.size() == 2) {
                // Distinguish the baseline and ongoing events.
                Map<String,String> record1 = records.get(0);
                Map<String,String> record2 = records.get(1);
                if (StringUtils.equals(record1.get("redcap_event_name"), PARTICIPANT_BASELINE_EVENT)) {
                    return RedcapParticipantInfo.buildFromRecord(redcapId, record1, record2);
                } else {
                    return RedcapParticipantInfo.buildFromRecord(redcapId, record2, record1);
                }                
            } else {
                throw new REDCapException("found too many records for (" + redcapId + ") - expected 1, found " + records.size());
            }
        }
    }

    /**
     * Create a new record for the subject in REDCap
     * @param redcapId
     * @return
     * @throws IOException
     * @throws REDCapException
     */
    public boolean createSubjectInfoRecord(String redcapId) throws IOException, REDCapException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("record_id", redcapId);
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
        bodyParams.put("data", data);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            Map<String, String> record = gson.fromJson(response.getResponseBody(), new TypeToken<Map<String, String>>(){}.getType());

            return record.get("count").equals("1");
        }
    }

    /**
     * Get the url for the entry survey into the REDCap flow
     * @param redcapId
     * @return
     * @throws REDCapException
     * @throws IOException
     */
    public String getEntrySurveyLink(String redcapId) throws REDCapException, IOException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "surveyLink");
        bodyParams.put("record", redcapId);
        bodyParams.put("event", PARTICIPANT_BASELINE_EVENT);
        bodyParams.put("instrument", ENTRY_FORM);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            return response.getResponseBody();
        }
    }

    /**
     * Get the url to the participant's survey queue
     * @param redcapId
     * @return
     * @throws REDCapException
     * @throws IOException
     */
    public String getSurveyQueueLink(String redcapId) throws REDCapException, IOException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("token", redcapConfiguration.getApiToken());
        bodyParams.put("content", "surveyQueueLink");
        bodyParams.put("record", redcapId);

        HttpResponse response = new HttpRequest().post(
                redcapConfiguration.getApiUrl(),
                null,
                requestHeaders,
                bodyParams
        );

        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            logger.error("REDCap ERROR: received code " + code + " - body=" + response.getResponseBody());
            throw new REDCapException("received code " + code + " from REDCap");

        } else {
            logger.debug("REDCap: received code " + code + " - body=" + response.getResponseBody());
            return response.getResponseBody();
        }
    }

}
