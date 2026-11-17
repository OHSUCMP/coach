package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.MedicationModel;
import edu.ohsu.cmp.coach.service.MedicationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/medications")
public class MedicationsController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MedicationService medicationService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        String sessionId = session.getId();
        if (userWorkspaceService.exists(sessionId)) {
            setCommonViewComponents(sessionId, model);
            model.addAttribute("patient", userWorkspaceService.get(sessionId).getPatient());

            List<MedicationModel> antihypertensiveMedications = filterDuplicates(medicationService.getAntihypertensiveMedications(sessionId));
            antihypertensiveMedications.sort((o1, o2) -> StringUtils.compare(o1.getDescription(), o2.getDescription()));

            List<MedicationModel> otherMedications = filterDuplicates(medicationService.getOtherMedications(sessionId));
            otherMedications.sort((o1, o2) -> StringUtils.compare(o1.getDescription(), o2.getDescription()));

            model.addAttribute("antihypertensiveMedications", antihypertensiveMedications);
            model.addAttribute("otherMedications", otherMedications);

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited medications page");

            return "medications";

        } else {
            return "redirect:" + getLaunchRedirect();
        }
    }
    
    private List<MedicationModel> filterDuplicates(List<MedicationModel> modelList) {
        Map<String, MedicationModel> map = new LinkedHashMap<String, MedicationModel>();

        logger.debug("filtering duplicate medications - original size={}", modelList.size());

        final String delim = "|";
        for (MedicationModel m : modelList) {
            String key = m.getDescription() + delim
                    + m.getReason() + delim
                    + m.getDose() + delim
                    + m.getPrescribingClinician() + delim
                    + m.getIssues() + delim
                    + m.getPriority();

            logger.debug("processing MedicationModel {} (key={})", m.getDescription(), key);

            if (map.containsKey(key)) {
                Long tsNew = m.getEffectiveTimestamp();

                logger.debug(" - found {} in map - tsNew={}", key, tsNew);

                if (tsNew != null) {    // if the new one has no timestamp, keep the existing one
                    Long tsMapped = map.get(key).getEffectiveTimestamp();
                    if (tsMapped == null || tsNew > tsMapped) {
                        logger.debug(" - tsNew > tsMapped ({}) - replacing -", tsMapped);
                        map.put(key, m);
                    }
                }

            } else {
                logger.debug(" - did not find {} in map.  adding -", key);
                map.put(key, m);
            }
        }

        logger.debug("done filtering duplicate medications.  new size={}", map.values().size());

        return new ArrayList<>(map.values());
    }
}
