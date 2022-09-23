package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.service.*;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.entity.Outcome;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Controller
public class HomeController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private PulseService pulseService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private MedicationService medicationService;

    @Value("${graph.trendline.loess-bandwidth}")
    private Number loessBandwidth;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) {
        boolean sessionEstablished = workspaceService.exists(session.getId());

        model.addAttribute("applicationName", applicationName);
        model.addAttribute("sessionEstablished", String.valueOf(sessionEstablished));
        model.addAttribute("loessBandwidth", loessBandwidth);
        if (sessionEstablished) {
            logger.info("requesting data for session " + session.getId());
            UserWorkspace workspace = workspaceService.get(session.getId());

            try {
                model.addAttribute("patient", workspace.getPatient());
                model.addAttribute("bpGoal", goalService.getCurrentBPGoal(session.getId()));

                Boolean showClearSupplementalData = StringUtils.equalsIgnoreCase(env.getProperty("feature.button.clear-supplemental-data.show"), "true");
                model.addAttribute("showClearSupplementalData", showClearSupplementalData);

                List<CDSHook> list = recommendationService.getOrderedCDSHooks();
                model.addAttribute("cdshooks", list);

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " building home page", e);
            }

            return "home";

        } else {
            Boolean cacheCredentials = StringUtils.equalsIgnoreCase(env.getProperty("security.browser.cache-credentials"), "true");
            model.addAttribute("cacheCredentials", cacheCredentials);
            return "fhir-complete-handshake";
        }
    }

    @PostMapping("blood-pressure-observations-list")
    public ResponseEntity<List<BloodPressureModel>> getBloodPressureObservations(HttpSession session) throws DataException {
        List<BloodPressureModel> list = bpService.getBloodPressureReadings(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("pulse-observations-list")
    public ResponseEntity<List<PulseModel>> getPulseObservations(HttpSession session) {
        List<PulseModel> list = pulseService.getPulseReadings(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

//    @PostMapping("current-bp-goal")
//    public ResponseEntity<GoalModel> getCurrentBPGoal(HttpSession session) {
//        GoalModel goal = goalService.getCurrentBPGoal(session.getId());
//        return new ResponseEntity<>(goal, HttpStatus.OK);
//    }

//    @PostMapping("recommendation")
//    public ResponseEntity<List<Card>> getRecommendation(HttpSession session,
//                                                        @RequestParam("id") String hookId) {
//
//        try {
//            UserWorkspace workspace = workspaceService.get(session.getId());
//
//            List<Card> cards = workspace.getCards(hookId);
//            logger.info("got cards for hookId=" + hookId + "!");
//
//            return new ResponseEntity<>(cards, HttpStatus.OK);
//
//        } catch (RuntimeException re) {
//            logger.error("caught " + re.getClass().getName() + " getting recommendations for " + hookId + " - " +
//                    re.getMessage(), re);
//            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @PostMapping("recommendation")
    public Callable<ResponseEntity<List<Card>>> getRecommendation(HttpSession session,
                                                                  @RequestParam("id") String hookId) {
        return new Callable<>() {
            public ResponseEntity<List<Card>> call() throws Exception {
                try {
                    UserWorkspace workspace = workspaceService.get(session.getId());

                    List<Card> cards = workspace.getCards(hookId);
                    logger.info("got cards for hookId=" + hookId + "!");

                    return new ResponseEntity<>(cards, HttpStatus.OK);

                } catch (RuntimeException re) {
                    logger.error("caught " + re.getClass().getName() + " getting recommendations for " + hookId + " - " +
                            re.getMessage(), re);
                    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @PostMapping("medications-list")
    public ResponseEntity<List<MedicationModel>> getMedications(HttpSession session) {
        try {
            List<MedicationModel> list = filterDuplicates(medicationService.getAntihypertensiveMedications(session.getId()));

            return new ResponseEntity<>(new ArrayList<>(list), HttpStatus.OK);

        } catch (HttpServerErrorException.InternalServerError ise) {
            logger.error("caught " + ise.getClass().getName() + " getting medications - " + ise.getMessage(), ise);
            throw ise;
        }
    }

    @PostMapping("adverse-events-list")
    public ResponseEntity<List<AdverseEventModel>> getAdverseEvents(HttpSession session) {
        try {
            List<AdverseEventModel> list = new ArrayList<>();

            for (AdverseEventModel ae : workspaceService.get(session.getId()).getAdverseEvents()) {
                if (ae.hasOutcome(Outcome.ONGOING)) {
                    list.add(ae);
                }
            }

            return new ResponseEntity<>(new ArrayList<>(list), HttpStatus.OK);

        } catch (HttpServerErrorException.InternalServerError ise) {
            logger.error("caught " + ise.getClass().getName() + " getting adverse events - " + ise.getMessage(), ise);
            throw ise;
        }
    }

    private List<MedicationModel> filterDuplicates(List<MedicationModel> modelList) {
        Map<String, MedicationModel> map = new LinkedHashMap<String, MedicationModel>();

        for (MedicationModel m : modelList) {
            String key = m.getDescription();

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
