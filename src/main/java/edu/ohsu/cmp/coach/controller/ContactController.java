package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.entity.app.ContactMessage;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.service.ContactMessageService;
import edu.ohsu.cmp.coach.service.EHRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class ContactController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment env;

    @Autowired
    private EHRService ehrService;

    @Autowired
    private ContactMessageService contactMessageService;

    @GetMapping("contact")
    public String view(HttpSession session, Model model, @RequestParam("token") String token) {
        if (SessionCache.getInstance().exists(session.getId())) {
            logger.info("showing contact form for session " + session.getId());

            model.addAttribute("applicationName", applicationName);
            model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

            ContactMessage contactMessage = contactMessageService.getMessage(token);
            String message = contactMessage != null ?
                    contactMessage.getBody() :
                    "";
            model.addAttribute("message", message);

            String mychartLoginLink = env.getProperty("mychart.login.url");
            String mychartMessageLink = env.getProperty("mychart.askAMedicalQuestion.url");

            model.addAttribute("mychartLoginLink", mychartLoginLink);
            model.addAttribute("mychartMessageLink", mychartMessageLink);
        }

        return "contact";
    }
}