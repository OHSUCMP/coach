package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.model.FHIRCredentials;
import edu.ohsu.cmp.htnu18app.registry.FHIRRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class SessionController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostMapping("/session/setFHIRCredentials")
    public ResponseEntity<?> setFHIRCredentials(HttpSession session,
                                                @RequestParam("serverUrl") String serverUrl,
                                                @RequestParam("bearerToken") String bearerToken,
                                                @RequestParam("patientId") String patientId,
                                                @RequestParam("userId") String userId) {

        FHIRRegistry registry = FHIRRegistry.getInstance();

        if (registry.exists(session.getId())) {
            return ResponseEntity.ok("FHIR credentials already set");

        } else {
            registry.set(session.getId(), new FHIRCredentials(serverUrl, bearerToken, patientId, userId));
            return ResponseEntity.ok("FHIR credentials set successfully");
        }
    }
}
