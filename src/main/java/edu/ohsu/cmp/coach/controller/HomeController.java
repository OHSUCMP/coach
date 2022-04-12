package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.service.*;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.entity.app.Outcome;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;

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

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) {
        boolean sessionEstablished = workspaceService.exists(session.getId());

        model.addAttribute("applicationName", applicationName);
        model.addAttribute("sessionEstablished", String.valueOf(sessionEstablished));

        if (sessionEstablished) {
            logger.info("requesting data for session " + session.getId());

            try {
                UserWorkspace workspace = workspaceService.get(session.getId());

                model.addAttribute("patient", workspace.getPatient());

                model.addAttribute("medicationsOfInterestName", medicationService.getMedicationsOfInterestName());

                List<CDSHook> list = recommendationService.getOrderedCDSHooks();
                model.addAttribute("cdshooks", list);

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " building home page", e);
            }

            return "home";

        } else {
            Boolean cacheCredentials = StringUtils.equals(env.getProperty("security.browser.cache-credentials"), "true");
            model.addAttribute("cacheCredentials", cacheCredentials);
            return "fhir-complete-handshake";
        }
    }

    @PostMapping("blood-pressure-observations-list")
    public ResponseEntity<List<BloodPressureModel>> getBloodPressureObservations(HttpSession session) {
        List<BloodPressureModel> list = bpService.getBloodPressureReadings(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("pulse-observations-list")
    public ResponseEntity<List<PulseModel>> getPulseObservations(HttpSession session) {
        List<PulseModel> list = pulseService.getPulseReadings(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("current-bp-goal")
    public ResponseEntity<GoalModel> getCurrentBPGoal(HttpSession session) {
        GoalModel goal = goalService.getCurrentBPGoal(session.getId());
        return new ResponseEntity<>(goal, HttpStatus.OK);
    }

    @PostMapping("recommendation")
    public ResponseEntity<List<Card>> getRecommendation(HttpSession session,
                                                        @RequestParam("id") String hookId) {

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

    @PostMapping("medications-list")
    public ResponseEntity<List<MedicationModel>> getMedications(HttpSession session) {
        try {
            List<MedicationModel> list = medicationService.getMedicationsOfInterest(session.getId());

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
}
