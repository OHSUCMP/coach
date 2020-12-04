package edu.ohsu.cmp.htnu18app.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.registry.FHIRRegistry;
import edu.ohsu.cmp.htnu18app.registry.model.FHIRCredentials;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


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

        logger.info("requesting data for session " + session.getId());
        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            FHIRCredentials credentials = registry.getCredentials(session.getId());
            IGenericClient client = registry.getClient(session.getId());

            Patient p = client.read().resource(Patient.class).withId(credentials.getPatientId()).execute();
            PatientModel pd = new PatientModel(p);
            model.addAttribute("patient", pd);

            Bundle buCon = client
                    .search()
                    .forResource((Observation.class))
                    .and(Observation.PATIENT.hasId(credentials.getPatientId()))
                    .and(Observation.CODE.exactly().systemAndCode(BloodPressureModel.SYSTEM, BloodPressureModel.CODE))
                    .returnBundle(Bundle.class)
                    .execute();

            List<BloodPressureModel> bpList = new ArrayList<BloodPressureModel>();
            for (Bundle.BundleEntryComponent entryCon: buCon.getEntry()) {
                Observation o = (Observation) entryCon.getResource();
                try {
                    bpList.add(new BloodPressureModel(o));

                } catch (DataException e) {
                    logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                }
            }

            model.addAttribute("bp", bpList);

        } else {
            // todo: redirect the user to the standalone launch page
        }

        return "index2";
    }
}
