package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.GoalModel;
import edu.ohsu.cmp.htnu18app.model.MedicationModel;
import edu.ohsu.cmp.htnu18app.model.PatientModel;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.service.EHRService;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
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

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
public class HomeController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private CQFRulerService cqfRulerService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private GoalService goalService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) {
        if (SessionCache.getInstance().exists(session.getId())) {
            logger.info("requesting data for session " + session.getId());

            try {
                model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

                GoalModel currentBPGoal = goalService.getCurrentBPGoal(session.getId());
                model.addAttribute("currentBPGoal", currentBPGoal);

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
            return "fhir-complete-handshake";
        }
    }

    @PostMapping("blood-pressure-observations")
    public ResponseEntity<List<BloodPressureModel>> getBloodPressureObservations(HttpSession session) {
        Set<BloodPressureModel> set = new TreeSet<>();

        // first add BP observations from configured FHIR server
        Bundle bundle = ehrService.getBloodPressureObservations(session.getId());
        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            if (entryCon.getResource() instanceof Observation) {
                Observation o = (Observation) entryCon.getResource();
                try {
                    set.add(new BloodPressureModel(o));

                } catch (DataException e) {
                    logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                }

            } else {
                Resource r = entryCon.getResource();
                logger.warn("ignoring " + r.getClass().getName() + " (id=" + r.getId() + ") while building Blood Pressure Observations");
            }
        }

        // now incorporate Home Blood Pressure Readings that the user entered themself into the system
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(session.getId());
        for (HomeBloodPressureReading item : hbprList) {
            set.add(new BloodPressureModel(item));
        }

        return new ResponseEntity<>(new ArrayList<>(set), HttpStatus.OK);
    }

    @PostMapping("recommendation")
    public ResponseEntity<List<Card>> getRecommendation(HttpSession session,
                                                        @RequestParam("id") String hookId) {

        // attempt to get cached recommendations every 5 seconds for up to what, 1 hour?

        CacheData cache = SessionCache.getInstance().get(session.getId());

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

    @PostMapping("medications")
    public ResponseEntity<List<MedicationModel>> getMedications(HttpSession session) {
        Set<MedicationModel> set = new LinkedHashSet<MedicationModel>();

        // first add BP observations from configured FHIR server
        Bundle bundle = ehrService.getMedicationStatements(session.getId());
        for (Bundle.BundleEntryComponent entryCon: bundle.getEntry()) {
            if (entryCon.getResource() instanceof MedicationStatement) {
                MedicationStatement ms = (MedicationStatement) entryCon.getResource();
                try {
                    MedicationModel model = new MedicationModel(ms);
                    logger.info("got medication: " + model.getSystem() + "|" + model.getCode() + ": " + model.getDescription());
                    set.add(model);

                } catch (DataException e) {
                    logger.error("caught " + e.getClass().getName() + " - " + e.getMessage(), e);
                }

            } else {
                Resource r = entryCon.getResource();
                logger.warn("ignoring " + r.getClass().getName() + " (id=" + r.getId() + ") while building Medications");
            }
        }

        return new ResponseEntity<>(new ArrayList<>(set), HttpStatus.OK);
    }
}
