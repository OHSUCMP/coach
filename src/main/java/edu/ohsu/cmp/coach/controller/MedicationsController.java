package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.service.MedicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/medications")
public class MedicationsController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private MedicationService medicationService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

        model.addAttribute("medicationsOfInterestName", medicationService.getMedicationsOfInterestName());
        model.addAttribute("medicationsOfInterest", medicationService.getMedicationsOfInterest(session.getId()));
        model.addAttribute("otherMedications", medicationService.getOtherMedications(session.getId()));

        return "medications";
    }
}
