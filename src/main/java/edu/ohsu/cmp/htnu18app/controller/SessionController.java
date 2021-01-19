package edu.ohsu.cmp.htnu18app.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/session")
public class SessionController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientService patientService;

    @PostMapping("prepare")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam("serverUrl") String serverUrl,
                                            @RequestParam("bearerToken") String bearerToken,
                                            @RequestParam("patientId") String patientId,
                                            @RequestParam("userId") String userId) {

        SessionCache cache = SessionCache.getInstance();

        if ( ! cache.exists(session.getId()) ) {
            FHIRCredentials credentials = new FHIRCredentials(serverUrl, bearerToken, patientId, userId);
            IGenericClient client = buildClient(credentials);
            FHIRCredentialsWithClient credentialsWithClient = new FHIRCredentialsWithClient(credentials, client);

            Long internalPatientId = patientService.getInternalPatientId(patientId);

            cache.set(session.getId(), credentialsWithClient, internalPatientId);

            return ResponseEntity.ok("session configured successfully");

        } else {
            return ResponseEntity.ok("session already configured");
        }
    }

    private IGenericClient buildClient(FHIRCredentials credentials) {
        FhirContext ctx = FhirContext.forR4();
        IGenericClient client = ctx.newRestfulGenericClient(credentials.getServerURL());

        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(credentials.getBearerToken());
        client.registerInterceptor(authInterceptor);

        return client;
    }
}
