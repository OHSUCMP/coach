package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
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
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/bp-readings")
public class BPReadingsController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientController patientController;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @GetMapping(value={"", "/"})
    public String bpReadings(HttpSession session, Model model) {
        try {
            patientController.populatePatientModel(session.getId(), model);
            List<HomeBloodPressureReading> bpreadings = hbprService.getHomeBloodPressureReadings(session.getId());
            model.addAttribute("bpreadings", bpreadings);

        } catch (SessionMissingException e) {
            logger.error("error populating patient model", e);
        }

        return "bp-readings";
    }

    @PostMapping("create")
    public ResponseEntity<HomeBloodPressureReading> create(HttpSession session,
                                                           @RequestParam("systolic") Integer systolic,
                                                           @RequestParam("diastolic") Integer diastolic,
                                                           @RequestParam("timestamp") Long timestamp) {

        Date date = new Date(timestamp);
        HomeBloodPressureReading bpreading = new HomeBloodPressureReading(systolic, diastolic, date);
        bpreading = hbprService.create(session.getId(), bpreading);

        CacheData cache = SessionCache.getInstance().get(session.getId());
        cache.deleteAllCards();

        return new ResponseEntity<>(bpreading, HttpStatus.OK);
    }

    @PostMapping("delete")
    public ResponseEntity<String> delete(HttpSession session,
                                         @RequestParam("id") Long id) {
        try {
            hbprService.delete(session.getId(), id);

            CacheData cache = SessionCache.getInstance().get(session.getId());
            cache.deleteAllCards();

            return new ResponseEntity<>("OK", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Caught " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
