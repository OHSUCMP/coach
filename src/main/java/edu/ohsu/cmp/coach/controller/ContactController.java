package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.ContactMessage;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.BloodPressureSummaryModel;
import edu.ohsu.cmp.coach.model.MedicationModel;
import edu.ohsu.cmp.coach.service.BloodPressureService;
import edu.ohsu.cmp.coach.service.ContactMessageService;
import edu.ohsu.cmp.coach.service.MedicationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
public class ContactController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String TOKEN_READINGS_COUNT = "readings";
    public static final String TOKEN_DAYS_COUNT = "days";
    public static final String TOKEN_SYSTOLIC = "systolic";
    public static final String TOKEN_DIASTOLIC = "diastolic";
    public static final String TOKEN_MEDS = "meds";

    @Autowired
    private Environment env;

    @Autowired
    private ContactMessageService contactMessageService;

    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private MedicationService medicationService;

    @Value("${contact.instructions-html}")
    private String contactInstructions;

    @GetMapping("contact")
    public String view(HttpSession session, Model model, @RequestParam("token") String token) throws DataException {
        if (userWorkspaceService.exists(session.getId())) {
            logger.info("showing contact form for session " + session.getId());

            setCommonViewComponents(model);
            model.addAttribute("patient", userWorkspaceService.get(session.getId()).getPatient());

            ContactMessage contactMessage = contactMessageService.getMessage(token);
            String message = "";
            String subject = "";
            String aboveText = "";
            String belowText = "";
            if (contactMessage != null) {
                Map<String, String> tokenMap = buildTokenMap(session.getId());
                message = replaceTokens(contactMessage.getBody(), tokenMap);
                subject = replaceTokens(contactMessage.getSubject(), tokenMap);
                aboveText = contactMessage.getAboveText();
                belowText = contactMessage.getBelowText();
            }
            model.addAttribute("subject", subject);
            model.addAttribute("message", message);
            model.addAttribute("aboveText", aboveText);
            model.addAttribute("belowText", belowText);

            Map<String, String> map = new HashMap<>();
            map.put("subject", URLEncoder.encode(subject, StandardCharsets.UTF_8));
            String instructions = replaceTokens(contactInstructions, map);
            if (StringUtils.isNotBlank(instructions)) {
                model.addAttribute("instructions", instructions);
            }

            model.addAttribute("pageStyles", new String[] { "contact.css" });
        }

        return "contact";
    }

    private Map<String, String> buildTokenMap(String sessionId) throws DataException {
        BloodPressureSummaryModel summaryModel = new BloodPressureSummaryModel(bpService.getBloodPressureReadings(sessionId));
        Map<String, String> map = new LinkedHashMap<>();
        map.put(TOKEN_READINGS_COUNT, String.valueOf(summaryModel.getRecentHomeBPReadingsCount()));
        map.put(TOKEN_DAYS_COUNT, String.valueOf(summaryModel.getRecentHomeBPReadingsDayCount()));
        map.put(TOKEN_SYSTOLIC, String.valueOf(summaryModel.getAvgSystolic()));
        map.put(TOKEN_DIASTOLIC, String.valueOf(summaryModel.getAvgDiastolic()));
        map.put(TOKEN_MEDS, getAntihypertensiveMeds(sessionId));
        return map;
    }

    private String getAntihypertensiveMeds(String sessionId) {
        List<String> list = new ArrayList<>();
        for (MedicationModel m : medicationService.getAntihypertensiveMedications(sessionId)) {
            list.add(m.getDescription());
        }
        return toEnglishList(list, "no anti-hypertensive medications");
    }

    private String replaceTokens(String s, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            s = s.replaceAll("\\{" + entry.getKey() + "}", entry.getValue());
        }
        return s;
    }

    private String toEnglishList(List<String> list, String textIfEmpty) {
        if (list.size() > 2) {
            String s = String.join(", ", list);
            int index = s.lastIndexOf(", ");
            if (index > 0) {
                s = s.substring(0, index + 1) + " and" + s.substring(index + 1);
            }
            return s;

        } else if (list.size() == 2) {
            return list.get(0) + " and " + list.get(1);

        } else if (list.size() == 1) {
            return list.get(0);

        } else {
            return textIfEmpty;
        }
    }
}