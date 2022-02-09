package edu.ohsu.cmp.coach.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.cqfruler.CDSHookExecutorService;
import edu.ohsu.cmp.coach.cqfruler.CQFRulerService;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.service.PatientService;
import edu.ohsu.cmp.coach.util.FhirUtil;
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

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientService patientService;

    @Autowired
    private CQFRulerService cqfRulerService;

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
                                            @RequestParam("audience") String audienceStr) {

//        if ( ! cache.exists(session.getId()) ) {
        FHIRCredentials credentials = new FHIRCredentials(serverUrl, bearerToken, patientId, userId);
        IGenericClient client = FhirUtil.buildClient(
                credentials.getServerURL(),
                credentials.getBearerToken(),
                Integer.parseInt(env.getProperty("socket.timeout-seconds"))
        );
        FHIRCredentialsWithClient credentialsWithClient = new FHIRCredentialsWithClient(credentials, client);

        Long internalPatientId = patientService.getInternalPatientId(patientId);

        Audience audience = Audience.fromTag(audienceStr);

        SessionCache.getInstance().set(session.getId(), audience, credentialsWithClient, internalPatientId);

        cqfRulerService.requestHooksExecution(session.getId());

//            Bundle b = patientService.getMedicationStatements(session.getId());
//            logger.info("got medications : " + b);

        return ResponseEntity.ok("session configured successfully");

//        } else {
//            return ResponseEntity.ok("session already configured");
//        }
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        CDSHookExecutorService.getInstance().dequeue(session.getId());
        SessionCache.getInstance().remove(session.getId());
        return "logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        SessionCache.getInstance().remove(session.getId());
        return ResponseEntity.ok("session cleared");
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        logger.info("refreshing data for session " + session.getId());
        SessionCache.getInstance().flush(session.getId());
        cqfRulerService.requestHooksExecution(session.getId());
        return ResponseEntity.ok("flushed cached data");
    }
}
