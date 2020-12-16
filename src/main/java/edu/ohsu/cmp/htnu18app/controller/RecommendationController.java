package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.cqfruler.model.Card;
import edu.ohsu.cmp.htnu18app.registry.FHIRRegistry;
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
@RequestMapping("recommendation")
public class RecommendationController extends AuthenticatedController {
    private static final String CONFIG_URL_KEY = "cqfruler.cdshooks.endpoint.url";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CQFRulerService cqfRulerService;

    @GetMapping("list")
    public ResponseEntity<List<CDSHook>> getList(HttpSession session) {
        logger.info("requesting cds-hooks for session " + session.getId());

        try {
            List<CDSHook> list = cqfRulerService.getCDSHooks();
            return new ResponseEntity<>(list, HttpStatus.OK);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("execute")
    public ResponseEntity<?> execute(HttpSession session,
                                     @RequestParam("id") String hookId) {

        FHIRRegistry registry = FHIRRegistry.getInstance();
        if (registry.exists(session.getId())) {
            try {
                List<Card> cards = cqfRulerService.executeHook(registry.get(session.getId()), hookId);
                logger.info("got cards " + cards);
                return ResponseEntity.ok("success!");

            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
            }
        } else {
            return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).build();
        }
    }
}
