package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.MedicationModel;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import org.hl7.fhir.r4.model.*;
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
import java.util.Set;
import java.util.TreeSet;


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

    @GetMapping("blood-pressure-observations")
    public ResponseEntity<List<BloodPressureModel>> getBloodPressureObservations(HttpSession session) {
        Set<BloodPressureModel> set = new TreeSet<BloodPressureModel>();

        // first add BP observations from configured FHIR server
        Bundle bundle = patientService.getBloodPressureObservations(session.getId());
        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            if (entryCon.getResource() instanceof Observation) {
                Observation o = (Observation) entryCon.getResource();
                try {
                    set.add(new BloodPressureModel(o));

                } catch (DataException e) {
                    logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                }

            } else {
                Resource r = entryCon.getResource();
                logger.warn("ignoring " + r.getClass().getName() + " (id=" + r.getId() + ") while building Blood Pressure Observations");
            }
        }

        // now incorporate Home Blood Pressure Readings that the user entered themself into the system
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(session.getId());
        for (HomeBloodPressureReading item : hbprList) {
            set.add(new BloodPressureModel(item));
        }

        return new ResponseEntity<>(new ArrayList<>(set), HttpStatus.OK);
    }

    @GetMapping("medications")
    public ResponseEntity<List<MedicationModel>> getMedications(HttpSession session) {
        Set<MedicationModel> set = new TreeSet<MedicationModel>();

        // first add BP observations from configured FHIR server
        Bundle bundle = patientService.getMedicationStatements(session.getId());
        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            if (entryCon.getResource() instanceof MedicationStatement) {
                MedicationStatement ms = (MedicationStatement) entryCon.getResource();
                try {
                    set.add(new MedicationModel(ms));

                } catch (DataException e) {
                    logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                }

            } else {
                Resource r = entryCon.getResource();
                logger.warn("ignoring " + r.getClass().getName() + " (id=" + r.getId() + ") while building Medications");
            }
        }

        return new ResponseEntity<>(new ArrayList<>(set), HttpStatus.OK);
    }

}
