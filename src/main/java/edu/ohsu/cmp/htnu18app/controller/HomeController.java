package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class HomeController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientController patientController;

    @Autowired
    private CQFRulerService cqfRulerService;

    @GetMapping(value = {"", "/", "index"})
    public String index(HttpSession session, Model model) {
        logger.info("requesting data for session " + session.getId());

        try {
            patientController.populatePatientModel(session.getId(), model);

            List<CDSHook> list = cqfRulerService.getCDSHooks();
            model.addAttribute("cdshooks", list);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " building index page", e);
            // todo: redirect the user to the standalone launch page
        }

        return "index";
    }
}
