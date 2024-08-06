package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.model.redcap.RandomizationGroup;
import edu.ohsu.cmp.coach.model.redcap.RedcapParticipantInfo;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.model.Audience;
import edu.ohsu.cmp.coach.model.AuditLevel;
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

    @Value("${smart.scope}")
    private String scope;

    @Value("${smart.redirectUri}")
    private String redirectURI;

    @Value("${smart.iss}")
    private String iss;

    @Value("${smart.patient.clientId}")
    private String patientClientId;

    @Value("${smart.provider.clientId}")
    private String providerClientId;

    @Value("${redcap.data-access-group}")
    private String redcapDataAccessGroupStr;

    @Value("#{new Boolean('${end-of-study.permit-continued-use}')}")
    protected Boolean endOfStudyPermitContinuedUse;

    @GetMapping("health")
    public String health() {
        return "health";
    }

    @GetMapping(value = {"launch-provider", "launch-ehr"}) // launch-ehr is deprecated
    public String launchProvider(HttpSession session, Model model) {
        sessionService.expireAll(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", providerClientId);
        model.addAttribute("scope", scope);
        model.addAttribute("redirectUri", redirectURI);
        model.addAttribute("iss", iss);
        return "launch-provider";
    }

    @GetMapping("launch-patient")
    public String launchPatient(HttpSession session, Model model) {
        sessionService.expireAll(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", patientClientId);
        model.addAttribute("scope", scope);
        model.addAttribute("redirectUri", redirectURI);
        model.addAttribute("iss", iss);
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
        } else if (StringUtils.equals(clientId, providerClientId)) {
            audience = Audience.CARE_TEAM;
        } else {
            throw new CaseNotHandledException("couldn't determine audience from clientId=" + clientId);
        }

        logger.debug("preparing " + audience + " session " + session.getId());

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);
        MyPatient myPatient = patientService.getMyPatient(patientId);

        boolean requiresEnrollment = false;
        boolean hasCompletedStudy = false;
        RandomizationGroup randomizationGroup = RandomizationGroup.ENHANCED;

        if (redCapService.isRedcapEnabled()) {
            RedcapParticipantInfo redcapParticipantInfo = redCapService.getParticipantInfo(myPatient.getRedcapId());
            if (redcapParticipantInfo.getIsActivelyEnrolled()) {
                randomizationGroup = redcapParticipantInfo.getRandomizationGroup();

            } else if (redcapParticipantInfo.isHasCompletedStudy()) {
                hasCompletedStudy = true;

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

            } else if (hasCompletedStudy) {
                if (endOfStudyPermitContinuedUse) {
                    sessionService.prepareSession(session.getId(), credentials, audience, randomizationGroup, false, true);
                    return ResponseEntity.ok("session configured successfully");

                } else {
                    sessionService.prepareProvisionalSession(session.getId(), credentials, audience);
                    return ResponseEntity.ok("session provisionally established");
                }

            } else {
                sessionService.prepareSession(session.getId(), credentials, audience, randomizationGroup, false, false);
                return ResponseEntity.ok("session configured successfully");
            }

        } else if (audience == Audience.CARE_TEAM) {
            RedcapDataAccessGroup dag = RedcapDataAccessGroup.fromTag(redcapDataAccessGroupStr);  // this is for sure a valid value at this point, see RedcapConfigurationValidator for details

            if (dag == RedcapDataAccessGroup.OHSU || dag == RedcapDataAccessGroup.MU) {
                // for OHSU and MU, simply display the enhanced view for the patient, irrespective of the patient's
                // randomization group, and irrespective of whether or not the patient is actively enrolled

                sessionService.prepareSession(session.getId(), credentials, audience, RandomizationGroup.ENHANCED, requiresEnrollment, false);
                return ResponseEntity.ok("care team session established");

            } else if (dag == RedcapDataAccessGroup.VUMC) {
                // for VUMC, the flow is a little different.  we still want to display the enhanced view for the patient
                // irrespective of their randomization group, but if the patient is not actively enrolled, we want to display
                // a static "patient not active" page to the care team

                if (requiresEnrollment) {
                    // REDCap is enabled and the patient isn't actively enrolled.  display static "patient not active" page
                    return ResponseEntity.ok(PATIENT_NOT_ACTIVE_RESPONSE);

                } else {
                    sessionService.prepareSession(session.getId(), credentials, audience, RandomizationGroup.ENHANCED, requiresEnrollment, false);
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
        logger.debug("validating session " + session.getId() + " - exists? --> " + userWorkspaceService.exists(session.getId()));
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
        auditService.doAudit(session.getId(), AuditLevel.INFO, "logged out"); // must occur before expire action
        sessionService.expireAll(session.getId());
        return "logout";
    }

    @GetMapping("inactivity-logout")
    public String inactivityLogout(HttpSession session) {
        auditService.doAudit(session.getId(), AuditLevel.INFO, "logged out due to inactivity"); // must occur before expire action
        sessionService.expireAll(session.getId());
        return "inactivity-logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        sessionService.expireAll(session.getId());
        return ResponseEntity.ok("session cleared");
    }

    @PostMapping("confirm-end-of-study")
    public ResponseEntity<?> confirmEndOfStudy(HttpSession session) {
        userWorkspaceService.get(session.getId()).setConfirmedEndOfStudy(true);
        return ResponseEntity.ok("end-of-study confirmed");
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
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        boolean permitClearSupplementalData = StringUtils.equalsIgnoreCase(env.getProperty("feature.clear-supplemental-data.enabled"), "true");
        if (permitClearSupplementalData) {
            logger.info("clearing supplemental data for session=" + session.getId());
            workspace.clearSupplementalData();

            auditService.doAudit(session.getId(), AuditLevel.INFO, "cleared supplemental data");

            return ResponseEntity.ok("supplemental data cleared");

        } else {
            logger.warn("attempted to clear supplemental data for session=" + session.getId() + ", but this action is not permitted.");

            auditService.doAudit(session.getId(), AuditLevel.WARN, "unauthorized attempt to clear supplemental data");

            return ResponseEntity.badRequest().build();
        }
    }
}
