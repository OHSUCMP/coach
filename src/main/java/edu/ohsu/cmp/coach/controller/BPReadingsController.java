package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.cqfruler.CQFRulerService;
import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.service.HomeBloodPressureReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/bp-readings")
public class BPReadingsController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private CQFRulerService cqfRulerService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));

        List<HomeBloodPressureReading> bpreadings = hbprService.getHomeBloodPressureReadings(session.getId());
        model.addAttribute("bpreadings", bpreadings);

        return "bp-readings";
    }

    @PostMapping("create")
    public ResponseEntity<List<HomeBloodPressureReading>> create(HttpSession session,
                                                                 @RequestParam Integer systolic1,
                                                                 @RequestParam Integer diastolic1,
                                                                 @RequestParam(required = false) Integer pulse1,
                                                                 @RequestParam(required = false) Integer systolic2,
                                                                 @RequestParam(required = false) Integer diastolic2,
                                                                 @RequestParam(required = false) Integer pulse2,
                                                                 @RequestParam Long readingDateTS,
                                                                 @RequestParam Boolean followedInstructions) {

        // get the cache just to make sure it's defined and the user is properly authenticated
        SessionCache.getInstance().get(session.getId());

        Date readingDate = new Date(readingDateTS);

        List<HomeBloodPressureReading> list = new ArrayList<>();

        HomeBloodPressureReading bpreading1 = new HomeBloodPressureReading(systolic1, diastolic1, pulse1, readingDate, followedInstructions);
        bpreading1 = hbprService.create(session.getId(), bpreading1);
        list.add(bpreading1);

        if (systolic2 != null && diastolic2 != null) {
            HomeBloodPressureReading bpreading2 = new HomeBloodPressureReading(systolic2, diastolic2, pulse2, readingDate, followedInstructions);
            bpreading2 = hbprService.create(session.getId(), bpreading2);
            list.add(bpreading2);
        }

        cqfRulerService.requestHooksExecution(session.getId());

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("delete")
    public ResponseEntity<String> delete(HttpSession session,
                                         @RequestParam("id") Long id) {

        // get the cache just to make sure it's defined and the user is properly authenticated
        SessionCache.getInstance().get(session.getId());

        try {
            hbprService.delete(session.getId(), id);
            cqfRulerService.requestHooksExecution(session.getId());

            return new ResponseEntity<>("OK", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Caught " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
