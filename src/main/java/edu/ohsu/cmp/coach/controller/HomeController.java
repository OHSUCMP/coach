package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.entity.Outcome;
import edu.ohsu.cmp.coach.entity.RedcapParticipantInfo;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.service.*;
import edu.ohsu.cmp.coach.session.ProvisionalSessionCacheData;
import edu.ohsu.cmp.coach.session.SessionService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Controller
public class HomeController extends BaseController {
    private static final DateFormat OMRON_LAST_UPDATED = new SimpleDateFormat("EEEE, MMMM d, YYYY 'at' h:mm a");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private PulseService pulseService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private OmronService omronService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private REDCapService redCapService;

    @Autowired
    private AdverseEventService adverseEventService;

    @Value("#{new Boolean('${security.browser.cache-credentials}')}")
    Boolean cacheCredentials;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model,
                       @RequestParam(name = "bandwidth", required = false) Number bandwidthOverride) {

        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            logger.info("session exists.  requesting data for session " + sessionId);

            UserWorkspace workspace = userWorkspaceService.get(sessionId);
            logger.info("Randomization group for user: " + workspace.getRandomizationGroup());

            try {
                setCommonViewComponents(model);
                model.addAttribute("sessionEstablished", true);
                model.addAttribute("loessBandwidth", bandwidthOverride == null ? -1:bandwidthOverride);
                model.addAttribute("pageStyles", new String[] { "home.css?v=4", "recommendations.css?v=1" });
                model.addAttribute("pageNodeScripts", new String[] { "jquery.inputmask.js", "bindings/inputmask.binding.js" });
                model.addAttribute("pageScripts", new String[] { "science.js/science.v1.js", "science.js/lib/d3/d3.js", "home.js?v=2", "recommendations.js?v=1" });
                model.addAttribute("patient", workspace.getPatient());
                model.addAttribute("bpGoal", goalService.getCurrentBPGoal(sessionId));
                model.addAttribute("bpGoalUpdated", workspace.getBpGoalUpdated());
                model.addAttribute("randomizationGroup", String.valueOf(workspace.getRandomizationGroup()));

                if (redCapService.isRedcapEnabled()) {
                    model.addAttribute("aeSurveyLink", redCapService.getAESurveyLink(workspace.getRedcapId()));
                }

                Boolean showClearSupplementalData = StringUtils.equalsIgnoreCase(env.getProperty("feature.button.clear-supplemental-data.show"), "true");
                model.addAttribute("showClearSupplementalData", showClearSupplementalData);

                if (workspace.getOmronTokenData() == null) {
                    model.addAttribute("omronAuthRequestUrl", omronService.getAuthorizationRequestUrl());
                }

                if (workspace.isOmronSynchronizing()) {
                    model.addAttribute("omronSynchronizing", true);
                } else if (workspace.getOmronLastUpdated() != null) {
                    model.addAttribute("omronLastUpdated", OMRON_LAST_UPDATED.format(workspace.getOmronLastUpdated()));
                }

                List<CDSHook> list = recommendationService.getOrderedCDSHooks(sessionId);
                model.addAttribute("cdshooks", list);

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " building home page", e);
            }

            return "home";

        } else if (sessionService.existsProvisional(sessionId)) {
            logger.info("provisional session exists.  starting REDCap workflow for session " + sessionId);

            // we get here if the user hasn't completed REDCap enrollment, has denied consent, or withdrew
            ProvisionalSessionCacheData cacheData = sessionService.getProvisionalSessionData(sessionId);
            MyPatient patient = patientService.getMyPatient(cacheData.getCredentials().getPatientId());
            sessionService.expireProvisional(sessionId);

            try {
                RedcapParticipantInfo redcapParticipantInfo = redCapService.getParticipantInfo(patient.getRedcapId());
                if ( ! redcapParticipantInfo.getExists() ) {
                    // If they are not in REDCap yet, create them and forward them to the entry survey
                    String recordId = redCapService.createSubjectInfoRecord(redcapParticipantInfo.getCoachId());
                    String entrySurveyLink = redCapService.getEntrySurveyLink(recordId);
                    return "redirect:" + entrySurveyLink;

                } else if ( ! redcapParticipantInfo.getIsInformationSheetComplete() ) {
                    // If they haven't gotten past the entry survey and don't have a queue yet, send them back to the entry survey
                    String entrySurveyLink = redCapService.getEntrySurveyLink(redcapParticipantInfo.getRecordId());
                    return "redirect:" + entrySurveyLink;

                } else if (redcapParticipantInfo.getHasConsentRecord() && ! redcapParticipantInfo.getIsConsentGranted()) {
                    // If consent record exists and the answer is no, exit
                    setCommonViewComponents(model);
                    return "consent-previously-denied";

                } else if ( ! redcapParticipantInfo.getHasConsentRecord() || ! redcapParticipantInfo.getIsRandomized()) {
                    // If there is no consent or randomization record, forward them to their survey queue
                    String surveyQueueLink = redCapService.getSurveyQueueLink(redcapParticipantInfo.getRecordId());
                    return "redirect:" + surveyQueueLink;                

                } else if (redcapParticipantInfo.getIsWithdrawn()) {
                    // If withdrawn, exit
                    setCommonViewComponents(model);
                    return "withdrawn";

                } else {
                    logger.error("REDCap participant " + redcapParticipantInfo.getRecordId() + "is actively enrolled but cannot access COACH.");
                    return "error";
                }

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                return "error";
            }

        } else {
            logger.info("no session exists.  completing SMART-on-FHIR handshake for session " + sessionId);
            setCommonViewComponents(model);
            model.addAttribute("cacheCredentials", cacheCredentials);
            model.addAttribute("patientNotActiveEndpoint", "/patient-not-active");
            model.addAttribute("patientNotActiveResponse", SessionController.PATIENT_NOT_ACTIVE_RESPONSE);
            return "fhir-complete-handshake";
        }
    }

    @PostMapping("blood-pressure-observations-list")
    public ResponseEntity<List<BloodPressureModel>> getBloodPressureObservations(HttpSession session) throws DataException {
        List<BloodPressureModel> list = bpService.getBloodPressureReadings(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("pulse-observations-list")
    public ResponseEntity<List<PulseModel>> getPulseObservations(HttpSession session) throws DataException {
        List<PulseModel> list = pulseService.getPulseReadings(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("recommendation")
    public Callable<ResponseEntity<List<Card>>> getRecommendation(HttpSession session,
                                                                  @RequestParam("id") String hookId) {
        return new Callable<>() {
            public ResponseEntity<List<Card>> call() throws Exception {
                try {
                    UserWorkspace workspace = userWorkspaceService.get(session.getId());

                    List<Card> cards = workspace.getCards(hookId);
                    logger.info("got cards for hookId=" + hookId + "!");

                    return new ResponseEntity<>(cards, HttpStatus.OK);

                } catch (RuntimeException re) {
                    logger.error("caught " + re.getClass().getName() + " getting recommendations for " + hookId + " - " +
                            re.getMessage(), re);
                    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @PostMapping("medications-list")
    public ResponseEntity<List<MedicationModel>> getMedications(HttpSession session) {
        try {
            List<MedicationModel> list = filterDuplicates(medicationService.getAntihypertensiveMedications(session.getId()));

            return new ResponseEntity<>(new ArrayList<>(list), HttpStatus.OK);

        } catch (HttpServerErrorException.InternalServerError ise) {
            logger.error("caught " + ise.getClass().getName() + " getting medications - " + ise.getMessage(), ise);
            throw ise;
        }
    }

    @PostMapping("adverse-events-list")
    public ResponseEntity<List<AdverseEventModel>> getAdverseEvents(HttpSession session) throws DataException {
        try {
            List<AdverseEventModel> list = new ArrayList<>();

            for (AdverseEventModel ae : adverseEventService.getAdverseEvents(session.getId())) {
                if (ae.hasOutcome(Outcome.ONGOING)) {
                    list.add(ae);
                }
            }

            return new ResponseEntity<>(new ArrayList<>(list), HttpStatus.OK);

        } catch (HttpServerErrorException.InternalServerError ise) {
            logger.error("caught " + ise.getClass().getName() + " getting adverse events - " + ise.getMessage(), ise);
            throw ise;
        }
    }

    private List<MedicationModel> filterDuplicates(List<MedicationModel> modelList) {
        Map<String, MedicationModel> map = new LinkedHashMap<String, MedicationModel>();

        for (MedicationModel m : modelList) {
            String key = m.getDescription();

            if (map.containsKey(key)) {
                Long tsNew = m.getEffectiveTimestamp();
                if (tsNew != null) {    // if the new one has no timestamp, keep the existing one
                    Long tsMapped = map.get(key).getEffectiveTimestamp();
                    if (tsMapped == null || tsNew > tsMapped) {
                        map.put(key, m);
                    }
                }

            } else {
                map.put(key, m);
            }
        }

        return new ArrayList<>(map.values());
    }

    @GetMapping("metadata")
    public ResponseEntity<String> getMetadata(HttpSession session) {
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        return new ResponseEntity<>(FhirUtil.toJson(workspace.getFhirCredentialsWithClient().getMetadata()), HttpStatus.OK);
    }
}
