package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.service.PatientService;
import edu.ohsu.cmp.coach.session.ProvisionalSessionCacheData;
import edu.ohsu.cmp.coach.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/redcap")
public class REDCapController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PatientService patientService;

    @GetMapping("process-consent")
    public String processConsent(HttpSession session) throws ConfigurationException {
        String sessionId = session.getId();

        ProvisionalSessionCacheData cacheData = sessionService.getProvisionalSessionData(sessionId);
        MyPatient patient = patientService.getMyPatient(cacheData.getCredentials().getPatientId());
        if (isConsentGranted(patient.getRedcapId())) {
            patient.setConsentGranted(true);
            patientService.update(patient);
            sessionService.expireProvisional(sessionId);
            sessionService.prepareSession(sessionId, cacheData.getCredentials(), cacheData.getAudience());
        }

        return "redirect:/";
    }

    private boolean isConsentGranted(String redcapId) {
        // todo : query REDCap for consent status assoicated with the specified redcapId
        return true;
    }
}
