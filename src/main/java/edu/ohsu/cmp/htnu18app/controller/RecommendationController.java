package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
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
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/recommendations")
public class RecommendationController extends AuthenticatedController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CQFRulerService cqfRulerService;

    @GetMapping(value={"", "/"})
    public ResponseEntity<List<CDSHook>> getList(HttpSession session) {
        logger.info("requesting cds-hooks for session " + session.getId());

        try {
            List<CDSHook> list = cqfRulerService.getCDSHooks();
            return new ResponseEntity<>(list, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " getting CDS Services", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("execute")
    public ResponseEntity<List<Card>> execute(HttpSession session,
                                              @RequestParam("id") String hookId) {
        try {
            List<Card> cards = cqfRulerService.executeHook(session.getId(), hookId);
            logger.info("got cards " + cards);
            return new ResponseEntity<>(cards, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("caught " + e.getClass().getName() + " executing hook '" + hookId + "'", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
