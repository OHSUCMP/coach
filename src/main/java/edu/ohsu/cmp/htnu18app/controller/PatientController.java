package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@Controller
public class PatientController extends AuthenticatedController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientService patientService;

    @Autowired
    private CQFRulerService cqfRulerService;

    @GetMapping(value = {"/", "index"})
    public String index(HttpSession session, Model model) {
        logger.info("requesting data for session " + session.getId());

        try {
            populatePatientModel(session.getId(), model);

            List<CDSHook> list = cqfRulerService.getCDSHooks();
            model.addAttribute("cdshooks", list);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " building index page", e);
            // todo: redirect the user to the standalone launch page
        }

        return "index";
    }

    protected void populatePatientModel(String sessionId, Model model) throws SessionMissingException {
        Patient p = patientService.getPatient(sessionId);
        model.addAttribute("patient", new PatientModel(p));
    }

    @GetMapping("/patient/bpList")
    public ResponseEntity<List<BloodPressureModel>> getBPData(HttpSession session) {
        Bundle buCon = patientService.getBloodPressureObservations(session.getId());
        List<BloodPressureModel> bpList = new ArrayList<BloodPressureModel>();
        for (Bundle.BundleEntryComponent entryCon: buCon.getEntry()) {
            Observation o = (Observation) entryCon.getResource();
            try {
                bpList.add(new BloodPressureModel(o));

            } catch (DataException e) {
                logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
            }
        }
        return new ResponseEntity<>(bpList, HttpStatus.OK);
    }
}
