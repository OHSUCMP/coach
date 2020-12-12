package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
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
}
