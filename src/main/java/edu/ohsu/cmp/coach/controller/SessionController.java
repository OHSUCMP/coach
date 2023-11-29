package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.entity.RandomizationGroup;
import edu.ohsu.cmp.coach.entity.RedcapParticipantInfo;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.service.PatientService;
import edu.ohsu.cmp.coach.service.REDCapService;
import edu.ohsu.cmp.coach.session.SessionService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private REDCapService redCapService;

    @GetMapping("health")
    public String health() {
        return "health";
    }

    @GetMapping("launch-ehr")
    public String launchEHR(HttpSession session, Model model) {
        sessionService.expireAll(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", env.getProperty("smart.ehr.clientId"));
        model.addAttribute("scope", env.getProperty("smart.ehr.scope"));
        model.addAttribute("redirectUri", env.getProperty("smart.ehr.redirectUri"));
        return "launch-ehr";
    }

    @GetMapping("launch-patient")
    public String launchPatient(HttpSession session, Model model) {
        sessionService.expireAll(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", env.getProperty("smart.patient.clientId"));
        model.addAttribute("scope", env.getProperty("smart.patient.scope"));
        model.addAttribute("redirectUri", env.getProperty("smart.patient.redirectUri"));
        model.addAttribute("iss", env.getProperty("smart.patient.iss"));
        return "launch-patient";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam String clientId,
                                            @RequestParam String serverUrl,
                                            @RequestParam String bearerToken,
                                            @RequestParam String patientId,
                                            @RequestParam String userId,
                                            @RequestParam("audience") String audienceStr) throws ConfigurationException, REDCapException, IOException {

        logger.debug("in prepare-session for session " + session.getId());

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);
        Audience audience = Audience.fromTag(audienceStr);

        MyPatient myPatient = patientService.getMyPatient(patientId);

        boolean activelyEnrolled = true;
        RandomizationGroup randomizationGroup = RandomizationGroup.ENHANCED;
        // TODO: Also need a check for whether the person logging in is a provider. If so, bypass REDCap
        if (redCapService.isRedcapEnabled()) {
            RedcapParticipantInfo redcapParticipantInfo = redCapService.getParticipantInfo(myPatient.getRedcapId());
            activelyEnrolled = redcapParticipantInfo.getIsActivelyEnrolled();
            if (activelyEnrolled) {
                randomizationGroup = redcapParticipantInfo.getRandomizationGroup();
            }
            logger.debug("REDCap activelyEnrolled = " + redcapParticipantInfo.getIsActivelyEnrolled());
        }

        if (activelyEnrolled) {
            sessionService.prepareSession(session.getId(), credentials, audience, randomizationGroup);

            return ResponseEntity.ok("session configured successfully");

        } else {
            // REDCap is enabled and the participant is not actively enrolled (for any number of reasons)
            // cache session data somewhere well-segregated from the UserWorkspace, as a UserWorkspace must only be
            // set up for authorized users.
            // HomeController.view will handle the next step of this workflow
            sessionService.prepareProvisionalSession(session.getId(), credentials, audience);

            return ResponseEntity.ok("session provisionally established");
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
