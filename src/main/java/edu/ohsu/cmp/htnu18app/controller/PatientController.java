package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.registry.FHIRRegistry;
import edu.ohsu.cmp.htnu18app.registry.model.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("Hypertension U18 Application")
    private String title;

    @Autowired
    private PatientService patientService;

    @GetMapping(value = {"/", "index"})
    public String index(HttpSession session, Model model) {
        model.addAttribute("title", title);

        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            model.addAttribute("fhirCredentials", registry.get(session.getId()).getCredentials());
        }

        return "index";
    }

    @GetMapping(value = "index2")
    public String index2(HttpSession session, Model model) {
        model.addAttribute("title", title);

        logger.info("requesting data for session " + session.getId());
        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            FHIRCredentialsWithClient fcc = registry.get(session.getId());

//            Patient p = fcc.getClient()
//                    .read()
//                    .resource(Patient.class)
//                    .withId(fcc.getCredentials().getPatientId())
//                    .execute();

            Patient p = patientService.getPatient(fcc.getClient(), fcc.getCredentials().getPatientId());

            PatientModel pd = new PatientModel(p);
            model.addAttribute("patient", pd);

        } else {
            // todo: redirect the user to the standalone launch page
        }

        return "index2";
    }

    @GetMapping("/patient/bpList")
    public ResponseEntity<List<BloodPressureModel>> getBPData(HttpSession session) {
        logger.info("requesting blood pressures for session " + session.getId());
        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            FHIRCredentialsWithClient fcc = registry.get(session.getId());

//            Bundle buCon = fcc.getClient()
//                    .search()
//                    .forResource((Observation.class))
//                    .and(Observation.PATIENT.hasId(fcc.getCredentials().getPatientId()))
//                    .and(Observation.CODE.exactly().systemAndCode(BloodPressureModel.SYSTEM, BloodPressureModel.CODE))
//                    .returnBundle(Bundle.class)
//                    .execute();

            Bundle buCon = patientService.getBloodPressureObservations(fcc.getClient(), fcc.getCredentials().getPatientId());

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

        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}
