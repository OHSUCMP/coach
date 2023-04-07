package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.ContactMessage;
import edu.ohsu.cmp.coach.service.ContactMessageService;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.util.MustacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
        if (userWorkspaceService.exists(session.getId())) {
            logger.info("showing contact form for session " + session.getId());

            model.addAttribute("applicationName", applicationName);
            model.addAttribute("patient", userWorkspaceService.get(session.getId()).getPatient());

            String mychartLoginLink = env.getProperty("mychart.login.url");
            String mychartMessageLink = env.getProperty("mychart.askAMedicalQuestion.url");

            ContactMessage contactMessage = contactMessageService.getMessage(token);
            String message = "";
            String subject = "";
            if (contactMessage != null) {
                message = contactMessage.getBody();
                subject = contactMessage.getSubject();
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("subject", URLEncoder.encode(subject, StandardCharsets.UTF_8));
                    mychartMessageLink = MustacheUtil.compileMustache(mychartMessageLink, map);

                } catch (IOException e) {
                    logger.error("caught " + e.getClass().getName() + " compiling MyChart Message Link template: " + e.getMessage(), e);
                }
            }
            model.addAttribute("subject", subject);
            model.addAttribute("message", message);

            model.addAttribute("mychartLoginLink", mychartLoginLink);
            model.addAttribute("mychartMessageLink", mychartMessageLink);
        }

        return "contact";
    }

    @GetMapping("bcontact")
    public String bview(HttpSession session, Model model, @RequestParam("token") String token) {
        if (userWorkspaceService.exists(session.getId())) {
            logger.info("showing contact form for session " + session.getId());

            model.addAttribute("applicationName", applicationName);
            model.addAttribute("patient", userWorkspaceService.get(session.getId()).getPatient());

            String mychartLoginLink = env.getProperty("mychart.login.url");
            String mychartMessageLink = env.getProperty("mychart.askAMedicalQuestion.url");

            ContactMessage contactMessage = contactMessageService.getMessage(token);
            String message = "";
            String subject = "";
            if (contactMessage != null) {
                message = contactMessage.getBody();
                subject = contactMessage.getSubject();
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("subject", URLEncoder.encode(subject, StandardCharsets.UTF_8));
                    mychartMessageLink = MustacheUtil.compileMustache(mychartMessageLink, map);

                } catch (IOException e) {
                    logger.error("caught " + e.getClass().getName() + " compiling MyChart Message Link template: " + e.getMessage(), e);
                }
            }
            model.addAttribute("subject", subject);
            model.addAttribute("message", message);

            model.addAttribute("mychartLoginLink", mychartLoginLink);
            model.addAttribute("mychartMessageLink", mychartMessageLink);
            model.addAttribute("pageStyles", new String[] { "bcontact.css" });
        }

        return "bcontact";
    }

}