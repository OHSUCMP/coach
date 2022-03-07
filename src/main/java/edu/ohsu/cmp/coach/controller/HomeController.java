package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.cache.UserCache;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.cqfruler.CQFRulerService;
import edu.ohsu.cmp.coach.cqfruler.model.CDSHook;
import edu.ohsu.cmp.coach.entity.app.Outcome;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.service.BloodPressureService;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.service.GoalService;
import edu.ohsu.cmp.coach.service.MedicationService;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.AdverseEvent;
import org.hl7.fhir.r4.model.Bundle;
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
    private EHRService ehrService;

    @Autowired
    private CQFRulerService cqfRulerService;

    @Autowired
    private BloodPressureService bpService;

//    @Autowired
//    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private MedicationService medicationService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) {
        SessionCache cache = SessionCache.getInstance();
        boolean sessionEstablished = cache.exists(session.getId());

        model.addAttribute("applicationName", applicationName);
        model.addAttribute("sessionEstablished", String.valueOf(sessionEstablished));

        if (sessionEstablished) {
            logger.info("requesting data for session " + session.getId());

            try {
                model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

                GoalModel currentBPGoal = goalService.getCurrentBPGoal(session.getId());
                model.addAttribute("currentBPGoal", currentBPGoal);

                model.addAttribute("medicationsOfInterestName", medicationService.getMedicationsOfInterestName());

                List<CDSHook> list = cqfRulerService.getCDSHooks();
                model.addAttribute("cdshooks", list);

                // CQF Ruler is a performance bottleneck.  It is presumed that hitting it with many
                // concurrent requests will further degrade performance for everyone.  Therefore,
                // the app places all Ruler requests into a queue, such that only one request is made
                // at a time.  The queue is constructed in such a way that if any user submits a Ruler
                // request, and they already have a request in the queue, that existing queue item
                // is replaced with the new one, efficiently improving performance
                int pos = cqfRulerService.getQueuePosition(session.getId());
                String queuePosition;
                if (pos == -1)      queuePosition = "NOT QUEUED";
                else if (pos == 0)  queuePosition = "CURRENTLY RUNNING";
                else                queuePosition = String.valueOf(pos);
                model.addAttribute("queuePosition", queuePosition);

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

    @PostMapping("recommendation")
    public ResponseEntity<List<Card>> getRecommendation(HttpSession session,
                                                        @RequestParam("id") String hookId) {

        // attempt to get cached recommendations every 5 seconds for up to what, 1 hour?

        // DOES NOT ACTUALLY FIRE A CALL ANYWHERE - ONLY SEARCHES CACHE
        // actual calls are fired in an asynchronous thread by CQFRulerService on login

        UserCache cache = SessionCache.getInstance().get(session.getId());

        List<Card> cards = null;
        HttpStatus status = HttpStatus.REQUEST_TIMEOUT;

        for (int i = 0; i < 720; i ++) {
            cards = cache.getCards(hookId);

            if (cards != null) {
                logger.info("got cards for hookId=" + hookId + "!");
                status = HttpStatus.OK;
                break;

            } else {
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    logger.error("caught " + e.getClass().getName() + " getting cached cards for hookId=" + hookId +
                            " (attempt " + i + ")", e);
                    status = HttpStatus.INTERNAL_SERVER_ERROR;
                    break;
                }
            }
        }

        return new ResponseEntity<>(cards, status);
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

            Bundle bundle = ehrService.getAdverseEvents(session.getId());
            for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
                if (entry.getResource() instanceof AdverseEvent) {
                    try {
                        AdverseEvent ae = (AdverseEvent) entry.getResource();
                        AdverseEventModel model = new AdverseEventModel(ae);
                        if (model.hasOutcome(Outcome.ONGOING)) {
                            list.add(new AdverseEventModel(ae));
                        }

                    } catch (DataException e) {
                        logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                    }
                }
            }

            return new ResponseEntity<>(new ArrayList<>(list), HttpStatus.OK);

        } catch (HttpServerErrorException.InternalServerError ise) {
            logger.error("caught " + ise.getClass().getName() + " getting adverse events - " + ise.getMessage(), ise);
            throw ise;
        }
    }
}
