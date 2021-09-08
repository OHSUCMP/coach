package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.service.EHRService;
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
public class PreferencesController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

        return "preferences";
    }
}
