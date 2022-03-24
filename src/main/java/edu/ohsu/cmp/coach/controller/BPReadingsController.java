package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.service.BloodPressureService;
import edu.ohsu.cmp.coach.service.EHRService;
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
    private BloodPressureService bpService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", workspaceService.get(session.getId()).getPatient());

        List<BloodPressureModel> bpreadings = bpService.getHomeBloodPressureReadings(session.getId());
        model.addAttribute("bpreadings", bpreadings);

        return "bp-readings";
    }

    @PostMapping("create")
    public ResponseEntity<List<BloodPressureModel>> create(HttpSession session,
                                                           @RequestParam Integer systolic1,
                                                           @RequestParam Integer diastolic1,
                                                           @RequestParam(required = false) Integer pulse1,
                                                           @RequestParam(required = false) Integer systolic2,
                                                           @RequestParam(required = false) Integer diastolic2,
                                                           @RequestParam(required = false) Integer pulse2,
                                                           @RequestParam Long readingDateTS,
                                                           @RequestParam Boolean followedInstructions) {

        // get the cache just to make sure it's defined and the user is properly authenticated
        workspaceService.get(session.getId());

        Date readingDate = new Date(readingDateTS);

        List<BloodPressureModel> list = new ArrayList<>();
        BloodPressureModel bpm1 = new BloodPressureModel(BloodPressureModel.Source.HOME,
                systolic1, diastolic1, pulse1, readingDate, followedInstructions, fcm);
        bpm1 = bpService.create(session.getId(), bpm1);
        list.add(bpm1);

        if (systolic2 != null && diastolic2 != null) {
            BloodPressureModel bpm2 = new BloodPressureModel(BloodPressureModel.Source.HOME,
                    systolic2, diastolic2, pulse2, readingDate, followedInstructions, fcm);
            bpm2 = bpService.create(session.getId(), bpm2);
            list.add(bpm2);
        }

        workspaceService.get(session.getId()).runRecommendations();

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("delete")
    public ResponseEntity<String> delete(HttpSession session,
                                         @RequestParam("id") String id) {

        // get the cache just to make sure it's defined and the user is properly authenticated
        workspaceService.get(session.getId());

        try {
            bpService.delete(session.getId(), id);
            workspaceService.get(session.getId()).runRecommendations();

            return new ResponseEntity<>("OK", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Caught " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
