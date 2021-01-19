package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
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
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@Controller
@RequestMapping("/patient")
public class PatientController extends AuthenticatedController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientService patientService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    protected void populatePatientModel(String sessionId, Model model) throws SessionMissingException {
        Patient p = patientService.getPatient(sessionId);
        model.addAttribute("patient", new PatientModel(p));
    }

    @GetMapping("bpList")
    public ResponseEntity<List<BloodPressureModel>> getBPData(HttpSession session) {
        List<BloodPressureModel> list = new ArrayList<BloodPressureModel>();

        // first add BP observations from configured FHIR server
        Bundle bundle = patientService.getBloodPressureObservations(session.getId());
        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            Observation o = (Observation) entryCon.getResource();
            try {
                list.add(new BloodPressureModel(o));

            } catch (DataException e) {
                logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
            }
        }

        // now incorporate Home Blood Pressure Readings that the user entered themself into the system
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(session.getId());
        for (HomeBloodPressureReading item : hbprList) {
            list.add(new BloodPressureModel(item));
        }

        return new ResponseEntity<>(list, HttpStatus.OK);
    }
}
