package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class BPReadingsController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientController patientController;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @GetMapping("/bp-readings")
    public String bpReadings(HttpSession session, Model model) {
        logger.info("requesting data for session " + session.getId());

        try {
            patientController.populatePatientModel(session.getId(), model);
            List<HomeBloodPressureReading> bpreadings = hbprService.getHomeBloodPressureReadings(session.getId());
            model.addAttribute("bpreadings", bpreadings);

        } catch (SessionMissingException e) {
            logger.error("error populating patient model", e);
        }

        return "bp-readings";
    }
}
