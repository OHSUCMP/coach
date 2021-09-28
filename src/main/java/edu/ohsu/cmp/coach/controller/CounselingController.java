package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.CounselingPageModel;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.service.CounselingService;
import edu.ohsu.cmp.coach.service.EHRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/counseling")
public class CounselingController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private CounselingService counselingService;

    @GetMapping("/{key}")
    public String view(HttpSession session, Model model, @PathVariable(value="key") String key) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

        CounselingPageModel page = new CounselingPageModel(counselingService.getPage(key));

        model.addAttribute("page", page);

        return "counseling";
    }
}
