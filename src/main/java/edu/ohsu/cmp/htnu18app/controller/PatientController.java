package edu.ohsu.cmp.htnu18app.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.htnu18app.model.FHIRCredentials;
import edu.ohsu.cmp.htnu18app.registry.FHIRRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;


@Controller
public class PatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("Hypertension U18 Application")
    private String title;

    @GetMapping(value = {"/", "index"})
    public String index(HttpSession session, Model model) {
        model.addAttribute("title", title);

        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            model.addAttribute("fhirCredentials", registry.getCredentials(session.getId()));
        }

        return "index";
    }

    @GetMapping(value = "index2")
    public String index2(HttpSession session, Model model) {
        model.addAttribute("title", title);

        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            FHIRCredentials credentials = registry.getCredentials(session.getId());
            model.addAttribute("patientId", credentials.getPatientId());
            model.addAttribute("userId", credentials.getUserId());
            model.addAttribute("serverUrl", credentials.getServerURL());
            model.addAttribute("sessionId", session.getId());
            model.addAttribute("bearerToken", credentials.getBearerToken());

            IGenericClient client = registry.getClient(session.getId());

            // todo: get patient and populate it into the template

        } else {
            // todo: redirect the user to the standalone launch page
        }

        return "index2";
    }
}
