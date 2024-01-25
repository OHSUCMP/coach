package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.Counseling;
import edu.ohsu.cmp.coach.model.AuditLevel;
import edu.ohsu.cmp.coach.model.CounselingPageModel;
import edu.ohsu.cmp.coach.service.CounselingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/counseling")
public class CounselingController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CounselingService counselingService;

    @GetMapping("{key}")
    public String view(HttpSession session, Model model, @PathVariable(value="key") String key) {
        setCommonViewComponents(model);
        model.addAttribute("patient", userWorkspaceService.get(session.getId()).getPatient());

        CounselingPageModel page = new CounselingPageModel(counselingService.getPage(key));

        model.addAttribute("page", page);

        auditService.doAudit(session.getId(), AuditLevel.INFO, "visited counseling page", "key=" + key);

        return "counseling";
    }

    @PostMapping("create")
    public ResponseEntity<Counseling> create(HttpSession session,
                                             @RequestParam String extCounselingId,
                                             @RequestParam String referenceSystem,
                                             @RequestParam String referenceCode,
                                             @RequestParam String counselingText) {

        Counseling c = counselingService.getLocalCounseling(session.getId(), extCounselingId);

        if (c == null) {
            c = counselingService.create(session.getId(),
                    new Counseling(extCounselingId, referenceSystem, referenceCode, counselingText));

            auditService.doAudit(session.getId(), AuditLevel.INFO, "created counseling record", "id=" + c.getId());

            return new ResponseEntity<>(c, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(c, HttpStatus.NOT_MODIFIED);
        }
    }
}
