package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.entity.app.Counseling;
import edu.ohsu.cmp.htnu18app.service.CounselingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/counseling")
public class CounselingController extends AuthenticatedController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CounselingService counselingService;

    @GetMapping("list")
    public ResponseEntity<List<Counseling>> getCounselingList(HttpSession session) {
        List<Counseling> list = counselingService.getCounselingList(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("create")
    public ResponseEntity<Counseling> create(HttpSession session,
                                       @RequestParam("extCounselingId") String extCounselingId,
                                       @RequestParam("category") String category,
                                       @RequestParam("counselingText") String counselingText) {

        Counseling counseling = counselingService.getCounseling(session.getId(), extCounselingId);
        if (counseling == null) {
            counseling = new Counseling(extCounselingId, category, counselingText);
            counseling = counselingService.create(session.getId(), counseling);
            return new ResponseEntity<>(counseling, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }
}
