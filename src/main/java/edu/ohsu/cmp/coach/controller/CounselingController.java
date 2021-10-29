package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.app.Counseling;
import edu.ohsu.cmp.coach.model.CounselingPageModel;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.service.CounselingService;
import edu.ohsu.cmp.coach.service.EHRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/counseling")
public class CounselingController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private CounselingService counselingService;

    @GetMapping("{key}")
    public String view(HttpSession session, Model model, @PathVariable(value="key") String key) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

        CounselingPageModel page = new CounselingPageModel(counselingService.getPage(key));

        model.addAttribute("page", page);

        return "counseling";
    }

    @PostMapping("create")
    public ResponseEntity<Counseling> create(HttpSession session,
                                             @RequestParam("extCounselingId") String extCounselingId,
                                             @RequestParam("referenceSystem") String referenceSystem,
                                             @RequestParam("referenceCode") String referenceCode,
                                             @RequestParam("counselingText") String counselingText) {

        Counseling c = counselingService.getCounseling(session.getId(), extCounselingId);

        if (c == null) {
            c = counselingService.create(session.getId(),
                    new Counseling(extCounselingId, referenceSystem, referenceCode, counselingText));
            return new ResponseEntity<>(c, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(c, HttpStatus.NOT_MODIFIED);
        }
    }
}