package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.REDCapException;
import edu.ohsu.cmp.coach.model.StudyClass;
import edu.ohsu.cmp.coach.service.PatientService;
import edu.ohsu.cmp.coach.service.REDCapService;
import edu.ohsu.cmp.coach.session.ProvisionalSessionCacheData;
import edu.ohsu.cmp.coach.session.SessionService;
import org.apache.commons.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@RequestMapping("/redcap")
public class REDCapController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private REDCapService redCapService;

    @GetMapping("process-consent")
    public String processConsent(HttpSession session) throws ConfigurationException, EncoderException, REDCapException, IOException {
        String sessionId = session.getId();

        ProvisionalSessionCacheData cacheData = sessionService.getProvisionalSessionData(sessionId);
        if (cacheData == null) {
            // evidently the person waited too long to return and their provisional session expired?

            // todo : how should we handle this?  display an error message?  kick off the workflow again?
            // todo : redirecting home is probably not what we want, it'll try to complete the handshake again

            return "redirect:/";
        }

        MyPatient patient = patientService.getMyPatient(cacheData.getCredentials().getPatientId());

        if (patient.getConsentGranted()) {

            // if they already have consent granted on their local record, why are they here?  redirect home
            return "redirect:/";

        } else if (isConsentGranted(patient.getRedcapId())) {
            patient.setConsentGranted(true);
            setRandomStudyClass(patient);
            patientService.update(patient);
            sessionService.expireProvisional(sessionId);
            sessionService.prepareSession(sessionId, cacheData.getCredentials(), cacheData.getAudience());
            return "redirect:/";

        } else {
            sessionService.expireProvisional(sessionId);
            return "no-consent";
        }
    }

    private void setRandomStudyClass(MyPatient p) {
        // randomly sort patient into either Study or Intervention bucket
        int x = (int) (Math.random() * 2); // 0 or 1
        switch (x) {
            case 0: p.setStudyClass(StudyClass.CONTROL.getLabel()); break;
            case 1: p.setStudyClass(StudyClass.INTERVENTION.getLabel()); break;
            default: throw new CaseNotHandledException("couldn't handle case where x=" + x);
        }
    }

    private boolean isConsentGranted(String redcapId) throws EncoderException, REDCapException, IOException {
        return redCapService.isConsentGranted(redcapId);
    }
}
