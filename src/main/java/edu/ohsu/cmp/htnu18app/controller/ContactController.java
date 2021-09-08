package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.service.EHRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class ContactController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment env;

    @Autowired
    private EHRService ehrService;

    @GetMapping("contact")
    public String view(HttpSession session, Model model) {
        if (SessionCache.getInstance().exists(session.getId())) {
            logger.info("showing contact form for session " + session.getId());

            model.addAttribute("applicationName", applicationName);
            model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

            // todo : generate this message from somewhere
            // todo : should it be a mustache template itself?  probably.  then we can fill it with user variables
            model.addAttribute("message", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");

            String mychartLoginLink = env.getProperty("mychart.login.url");
            String mychartMessageLink = env.getProperty("mychart.askAMedicalQuestion.url");

            model.addAttribute("mychartLoginLink", mychartLoginLink);
            model.addAttribute("mychartMessageLink", mychartMessageLink);
        }

        return "contact";
    }
}