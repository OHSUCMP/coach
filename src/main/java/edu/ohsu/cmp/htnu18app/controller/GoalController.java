package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cqfruler.CQFRulerService;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/goals")
public class GoalController extends AuthenticatedController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientController patientController;

    @Autowired
    private GoalService goalService;

    @Autowired
    private CQFRulerService cqfRulerService;

    @GetMapping(value={"", "/"})
    public String getGoals(HttpSession session, Model model) {
        try {
            patientController.populatePatientModel(session.getId(), model);

            List<CDSHook> list = cqfRulerService.getCDSHooks();
            model.addAttribute("cdshooks", list);

        } catch (Exception e) {
            logger.error("error populating patient model", e);
        }

        List<Goal> list = goalService.getGoalList(session.getId());
        model.addAttribute("goals", list);

        return "goals";
    }

    @GetMapping("list")
    public ResponseEntity<List<Goal>> getGoalsList(HttpSession session) {
        List<Goal> list = goalService.getGoalList(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("create")
    public ResponseEntity<Goal> create(HttpSession session,
                                       @RequestParam("goalId") String goalId,
                                       @RequestParam("goalText") String goalText,
                                       @RequestParam("followUpDays") Integer followUpDays) {

        if (goalId.isEmpty()) {
            // autogenerate goal ID
            goalId = DigestUtils.sha256Hex(goalText);
        }

        Goal goal = goalService.getGoal(session.getId(), goalId);
        if (goal == null) {
            goal = new Goal(goalId, goalText, followUpDays);
            goal = goalService.create(session.getId(), goal);
            return new ResponseEntity<>(goal, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }

    @PutMapping("setCompleted")
    public ResponseEntity<Goal> setCompleted(HttpSession session,
                                             @RequestParam("goalId") String goalId,
                                             @RequestParam("completed") Boolean completed) {

        Goal goal = goalService.getGoal(session.getId(), goalId);
        if (goal != null) {
            goal.setCompleted(completed);

            Date completedDate = completed ? new Date() : null;
            goal.setCompletedDate(completedDate);

            goal = goalService.update(goal);
            return new ResponseEntity<>(goal, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping("delete")
    public ResponseEntity<String> delete(HttpSession session,
                                         @RequestParam("goalId") String goalId) {
        try {
            goalService.delete(session.getId(), goalId);
            return new ResponseEntity<>(goalId, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Caught " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
