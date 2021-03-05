package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.entity.Goal;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/goals")
public class GoalController extends AuthenticatedController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private GoalService goalService;

    @GetMapping(value={"", "/"})
    public ResponseEntity<List<Goal>> getGoals(HttpSession session) {
        List<Goal> list = goalService.getGoals(session.getId());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PutMapping("put")
    public ResponseEntity<Goal> putGoal(HttpSession session,
                                        @RequestParam("goalId") String goalId,
                                        @RequestParam("followUpDays") Integer followUpDays,
                                        @RequestParam("value") String value) {

        Goal goal = goalService.getGoal(session.getId(), goalId);
        if (goal != null) {
            goal.setValue(value);
            goal = goalService.update(goal);

        } else {
            goal = new Goal(goalId, followUpDays, value);
            goal = goalService.create(session.getId(), goal);
        }

        return new ResponseEntity<>(goal, HttpStatus.OK);
    }
}
