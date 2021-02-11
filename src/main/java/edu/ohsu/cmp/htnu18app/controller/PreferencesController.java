package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/preferences")
public class PreferencesController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientController patientController;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {

        try {
            patientController.populatePatientModel(session.getId(), model);

        } catch (SessionMissingException e) {
            logger.error("error populating patient model", e);
        }

        return "preferences";
    }
}
