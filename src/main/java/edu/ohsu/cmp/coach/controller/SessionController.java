package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.entity.RandomizationGroup;
import edu.ohsu.cmp.coach.entity.RedcapParticipantInfo;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.model.Audience;
import edu.ohsu.cmp.coach.model.RedcapDataAccessGroup;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.service.PatientService;
import edu.ohsu.cmp.coach.service.REDCapService;
import edu.ohsu.cmp.coach.session.SessionService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String PATIENT_NOT_ACTIVE_RESPONSE = "PATIENT_NOT_ACTIVE";

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private REDCapService redCapService;

    @Value("${smart.patient.clientId}")
    private String patientClientId;

    @Value("${smart.patient.scope}")
    private String patientScope;

    @Value("${smart.patient.redirectUri}")
    private String patientRedirectURI;

    @Value("${smart.patient.iss}")
    private String patientISS;

    @Value("${smart.ehr.clientId}")
    private String ehrClientId;

    @Value("${smart.ehr.scope}")
    private String ehrScope;

    @Value("${smart.ehr.redirectUri}")
    private String ehrRedirectURI;


    @Value("${redcap.data-access-group}")
    private String redcapDataAccessGroupStr;

    @GetMapping("health")
    public String health() {
        return "health";
    }

    @GetMapping("launch-ehr")
    public String launchEHR(HttpSession session, Model model) {
        sessionService.expireAll(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", ehrClientId);
        model.addAttribute("scope", ehrScope);
        model.addAttribute("redirectUri", ehrRedirectURI);
        return "launch-ehr";
    }

    @GetMapping("launch-patient")
    public String launchPatient(HttpSession session, Model model) {
        sessionService.expireAll(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", patientClientId);
        model.addAttribute("scope", patientScope);
        model.addAttribute("redirectUri", patientRedirectURI);
        model.addAttribute("iss", patientISS);
        return "launch-patient";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam String clientId,
                                            @RequestParam String serverUrl,
                                            @RequestParam String bearerToken,
                                            @RequestParam String patientId,
                                            @RequestParam String userId) throws ConfigurationException, REDCapException, IOException {

        Audience audience;
        if (StringUtils.equals(clientId, patientClientId)) {
            audience = Audience.PATIENT;
        } else if (StringUtils.equals(clientId, ehrClientId)) {
            audience = Audience.CARE_TEAM;
        } else {
            throw new CaseNotHandledException("couldn't determine audience from clientId=" + clientId);
        }

        logger.debug("preparing " + audience + " session " + session.getId());

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);
        MyPatient myPatient = patientService.getMyPatient(patientId);

        boolean requiresEnrollment = false;
        RandomizationGroup randomizationGroup = RandomizationGroup.ENHANCED;

        if (redCapService.isRedcapEnabled()) {
            RedcapParticipantInfo redcapParticipantInfo = redCapService.getParticipantInfo(myPatient.getRedcapId());
            if (redcapParticipantInfo.getIsActivelyEnrolled()) {
                randomizationGroup = redcapParticipantInfo.getRandomizationGroup();
            } else {
                requiresEnrollment = true;
            }
            logger.debug("REDCap requiresEnrollment = " + requiresEnrollment);
        }

        if (audience == Audience.PATIENT) {
            if (requiresEnrollment) {
                // REDCap is enabled and the participant is not actively enrolled (for any number of reasons)
                // cache session data somewhere well-segregated from the UserWorkspace, as a UserWorkspace must only be
                // set up for authorized users.
                // HomeController.view will handle the next step of this workflow

                sessionService.prepareProvisionalSession(session.getId(), credentials, audience);
                return ResponseEntity.ok("session provisionally established");

            } else {
                sessionService.prepareSession(session.getId(), credentials, audience, randomizationGroup);
                return ResponseEntity.ok("session configured successfully");
            }

        } else if (audience == Audience.CARE_TEAM) {
            RedcapDataAccessGroup dag = RedcapDataAccessGroup.fromTag(redcapDataAccessGroupStr);  // this is for sure a valid value at this point, see RedcapConfigurationValidator for details

            if (dag == RedcapDataAccessGroup.OHSU || dag == RedcapDataAccessGroup.MU) {
                // for OHSU and MU, simply display the enhanced view for the patient, irrespective of the patient's
                // randomization group, and irrespective of whether or not the patient is actively enrolled

                sessionService.prepareSession(session.getId(), credentials, audience, RandomizationGroup.ENHANCED);
                return ResponseEntity.ok("care team session established");

            } else if (dag == RedcapDataAccessGroup.VUMC) {
                // for VUMC, the flow is a little different.  we still want to display the enhanced view for the patient
                // irrespective of their randomization group, but if the patient is not actively enrolled, we want to display
                // a static "patient not active" page to the care team

                if (requiresEnrollment) {
                    // REDCap is enabled and the patient hasn't isn't actively enrolled.  display static "patient not active" page
                    return ResponseEntity.ok(PATIENT_NOT_ACTIVE_RESPONSE);

                } else {
                    sessionService.prepareSession(session.getId(), credentials, audience, RandomizationGroup.ENHANCED);
                    return ResponseEntity.ok("care team session established");
                }

            } else {
                throw new CaseNotHandledException("no case exists for handling data access group=" + dag);
            }

        } else {
            throw new CaseNotHandledException("no case exists for handling audience=" + audience);
        }
    }

    @PostMapping("validate-session")
    public ResponseEntity<?> validateSession(HttpSession session) {
        logger.info("validating session " + session.getId() + " - exists? --> " + userWorkspaceService.exists(session.getId()));
        return userWorkspaceService.exists(session.getId()) ?
                ResponseEntity.ok(true) :
                ResponseEntity.ok(false);
    }

    @GetMapping("unauthorized")
    public String unauthorized(HttpSession session) {
        return "unauthorized";
    }

    @GetMapping("patient-not-active")
    public String patientNotActive(HttpSession session, Model model) {
        setCommonViewComponents(model);
        return "patient-not-active";
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        sessionService.expireAll(session.getId());
        return "logout";
    }

    @GetMapping("inactivity-logout")
    public String inactivityLogout(HttpSession session) {
        sessionService.expireAll(session.getId());
        return "inactivity-logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        sessionService.expireAll(session.getId());
        return ResponseEntity.ok("session cleared");
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        logger.info("refreshing data for session=" + session.getId());
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        workspace.clearCaches();
        workspace.populate();
        return ResponseEntity.ok("refreshing");
    }

    @PostMapping("clear-supplemental-data")
    public ResponseEntity<?> clearSupplementalData(HttpSession session) {
        boolean permitClearSupplementalData = StringUtils.equalsIgnoreCase(env.getProperty("feature.button.clear-supplemental-data.show"), "true");
        if (permitClearSupplementalData) {
            logger.info("clearing supplemental data for session=" + session.getId());
            UserWorkspace workspace = userWorkspaceService.get(session.getId());
            workspace.clearSupplementalData();
            return ResponseEntity.ok("supplemental data cleared");

        } else {
            logger.warn("attempted to clear supplemental data for session=" + session.getId() + ", but this action is not permitted.");
            return ResponseEntity.badRequest().build();
        }
    }
}
