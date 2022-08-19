package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.AbstractVitalsModel;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.ObservationSource;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.service.BloodPressureService;
import edu.ohsu.cmp.coach.service.PulseService;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/vitals")
public class VitalsController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private PulseService pulseService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) throws DataException {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", workspaceService.get(session.getId()).getPatient());

        List<AbstractVitalsModel> homeReadings = new ArrayList<>();
        homeReadings.addAll(bpService.getHomeBloodPressureReadings(session.getId()));
        homeReadings.addAll(pulseService.getHomePulseReadings(session.getId()));

        Collections.sort(homeReadings);

        model.addAttribute("homeReadings", homeReadings);

        return "vitals";
    }

    @PostMapping("create")
    public ResponseEntity<List<AbstractVitalsModel>> create(HttpSession session,
                                                            @RequestParam Integer systolic1,
                                                            @RequestParam Integer diastolic1,
                                                            @RequestParam(required = false) Integer pulse1,
                                                            @RequestParam(required = false) Integer systolic2,
                                                            @RequestParam(required = false) Integer diastolic2,
                                                            @RequestParam(required = false) Integer pulse2,
                                                            @RequestParam Long readingDateTS,
                                                            @RequestParam Boolean followedInstructions) throws DataException, ConfigurationException, IOException {

        // get the cache just to make sure it's defined and the user is properly authenticated
        workspaceService.get(session.getId());

        Date readingDate = new Date(readingDateTS);

        List<AbstractVitalsModel> list = new ArrayList<>();
        BloodPressureModel bpm1 = new BloodPressureModel(ObservationSource.HOME,
                systolic1, diastolic1, readingDate, followedInstructions, fcm);
        bpm1 = bpService.create(session.getId(), bpm1);
        list.add(bpm1);

        if (pulse1 != null) {
            PulseModel p1 = new PulseModel(ObservationSource.HOME, pulse1, readingDate, followedInstructions, fcm);
            p1 = pulseService.create(session.getId(), p1);
            list.add(p1);
        }

        if (systolic2 != null && diastolic2 != null) {
            BloodPressureModel bpm2 = new BloodPressureModel(ObservationSource.HOME,
                    systolic2, diastolic2, readingDate, followedInstructions, fcm);
            bpm2 = bpService.create(session.getId(), bpm2);
            list.add(bpm2);
        }

        if (pulse2 != null) {
            PulseModel p2 = new PulseModel(ObservationSource.HOME, pulse2, readingDate, followedInstructions, fcm);
            p2 = pulseService.create(session.getId(), p2);
            list.add(p2);
        }

        workspaceService.get(session.getId()).runRecommendations();

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

//    @PostMapping("delete")
//    public ResponseEntity<String> delete(HttpSession session,
//                                         @RequestParam("id") String id) {
//
//        // get the cache just to make sure it's defined and the user is properly authenticated
//        workspaceService.get(session.getId());
//
//        try {
//            bpService.delete(session.getId(), id);
//            workspaceService.get(session.getId()).runRecommendations();
//
//            return new ResponseEntity<>("OK", HttpStatus.OK);
//
//        } catch (Exception e) {
//            return new ResponseEntity<>("Caught " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}
