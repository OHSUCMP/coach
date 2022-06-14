package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.MedicationModel;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.service.MedicationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        model.addAttribute("patient", workspaceService.get(session.getId()).getPatient());

        List<MedicationModel> antihypertensiveMedications = medicationService.getAntihypertensiveMedications(session.getId());
        antihypertensiveMedications.sort((o1, o2) -> StringUtils.compare(o1.getDescription(), o2.getDescription()));

        List<MedicationModel> otherMedications = medicationService.getOtherMedications(session.getId());
        otherMedications.sort((o1, o2) -> StringUtils.compare(o1.getDescription(), o2.getDescription()));

        model.addAttribute("antihypertensiveMedications", antihypertensiveMedications);
        model.addAttribute("otherMedications", otherMedications);

        return "medications";
    }
}
