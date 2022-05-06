package edu.ohsu.cmp.coach.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping("launch-ehr")
    public String launchEHR(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("clientId", env.getProperty("smart.ehr.clientId"));
        model.addAttribute("scope", env.getProperty("smart.ehr.scope"));
        model.addAttribute("redirectUri", env.getProperty("smart.ehr.redirectUri"));
        return "launch-ehr";
    }

    @GetMapping("launch-patient")
    public String launchPatient(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("clientId", env.getProperty("smart.patient.clientId"));
        model.addAttribute("scope", env.getProperty("smart.patient.scope"));
        model.addAttribute("redirectUri", env.getProperty("smart.patient.redirectUri"));
        model.addAttribute("iss", env.getProperty("smart.patient.iss"));
        return "launch-patient";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam("serverUrl") String serverUrl,
                                            @RequestParam("bearerToken") String bearerToken,
                                            @RequestParam("patientId") String patientId,
                                            @RequestParam("userId") String userId,
                                            @RequestParam("audience") String audienceStr,
                                            @RequestParam("isIE") Boolean isIE) {

        logger.info("isIE? " + isIE);

        FHIRCredentials credentials = new FHIRCredentials(serverUrl, bearerToken, patientId, userId);
        IGenericClient client = FhirUtil.buildClient(
                credentials.getServerURL(),
                credentials.getBearerToken(),
                Integer.parseInt(env.getProperty("socket.timeout-seconds"))
        );
        FHIRCredentialsWithClient credentialsWithClient = new FHIRCredentialsWithClient(credentials, client);

        Audience audience = Audience.fromTag(audienceStr);

        String sessionId = session.getId();
        workspaceService.init(sessionId, audience, credentialsWithClient);
        workspaceService.get(sessionId).populate();

        return ResponseEntity.ok("session configured successfully");
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        workspaceService.shutdown(session.getId());
        return "logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        workspaceService.shutdown(session.getId());
        return ResponseEntity.ok("session cleared");
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        logger.info("refreshing data for session=" + session.getId());
        UserWorkspace workspace = workspaceService.get(session.getId());
        workspace.clearCaches();
        workspace.populate();
        return ResponseEntity.ok("refreshing");
    }
}
