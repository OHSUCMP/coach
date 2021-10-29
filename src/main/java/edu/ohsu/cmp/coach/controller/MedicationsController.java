package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.MedicationModel;
import edu.ohsu.cmp.coach.model.PatientModel;
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
import java.util.*;

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

        List<MedicationModel> medicationsOfInterest = filterDuplicates(medicationService.getMedicationsOfInterest(session.getId()));
        medicationsOfInterest.sort((o1, o2) -> StringUtils.compare(o1.getDescription(), o2.getDescription()));

        List<MedicationModel> otherMedications = filterDuplicates(medicationService.getOtherMedications(session.getId()));
        otherMedications.sort((o1, o2) -> StringUtils.compare(o1.getDescription(), o2.getDescription()));

        model.addAttribute("medicationsOfInterestName", medicationService.getMedicationsOfInterestName());
        model.addAttribute("medicationsOfInterest", medicationsOfInterest);
        model.addAttribute("otherMedications", otherMedications);

        return "medications";
    }

    private List<MedicationModel> filterDuplicates(List<MedicationModel> modelList) {
        Map<String, MedicationModel> map = new LinkedHashMap<String, MedicationModel>();

        final String delim = "|";
        for (MedicationModel m : modelList) {
            String key = m.getDescription() + delim
                    + m.getReason() + delim
                    + m.getDose() + delim
                    + m.getPrescribingClinician() + delim
                    + m.getIssues() + delim
                    + m.getPriority();

            if (map.containsKey(key)) {
                Long tsNew = m.getEffectiveTimestamp();
                if (tsNew != null) {    // if the new one has no timestamp, keep the existing one
                    Long tsMapped = map.get(key).getEffectiveTimestamp();
                    if (tsMapped == null || tsNew > tsMapped) {
                        map.put(key, m);
                    }
                }

            } else {
                map.put(key, m);
            }
        }

        return new ArrayList<>(map.values());
    }
}
