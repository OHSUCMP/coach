package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.app.AchievementStatus;
import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import edu.ohsu.cmp.htnu18app.entity.app.GoalHistory;
import edu.ohsu.cmp.htnu18app.model.GoalHistoryModel;
import edu.ohsu.cmp.htnu18app.model.GoalModel;
import edu.ohsu.cmp.htnu18app.service.GoalHistoryService;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
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
    private GoalHistoryService goalHistoryService;

//    @Autowired
//    private CQFRulerService cqfRulerService;

    @GetMapping(value={"", "/"})
    public String getGoals(HttpSession session, Model model) {
        try {
            patientController.populatePatientModel(session.getId(), model);

        } catch (Exception e) {
            logger.error("error populating patient model", e);
        }

        List<GoalModel> list = new ArrayList<>();
        for (Goal g : goalService.getGoalList(session.getId())) {
            list.add(new GoalModel(g));
        }
        model.addAttribute("goals", list);

        return "goals";
    }

    @PostMapping("create")
    public ResponseEntity<GoalModel> create(HttpSession session,
                                            @RequestParam("extGoalId") String extGoalId,
                                            @RequestParam("referenceSystem") String referenceSystem,
                                            @RequestParam("referenceCode") String referenceCode,
                                            @RequestParam("goalText") String goalText,
                                            @RequestParam("targetDateTS") Long targetDateTS) {

        Date targetDate = new Date(targetDateTS);

        Goal goal = goalService.getGoal(session.getId(), extGoalId);
        if (goal == null) {
            goal = new Goal(extGoalId, referenceSystem, referenceCode, goalText, targetDate);
            goal = goalService.create(session.getId(), goal);

            // remove goal from cache
            CacheData cache = SessionCache.getInstance().get(session.getId());
            cache.deleteSuggestion(extGoalId);

            return new ResponseEntity<>(new GoalModel(goal), HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }

    @PostMapping("update-bp")
    public ResponseEntity<GoalModel> updatebp(HttpSession session,
                                              @RequestParam("extGoalId") String extGoalId,
                                              @RequestParam("referenceSystem") String referenceSystem,
                                              @RequestParam("referenceCode") String referenceCode,
                                              @RequestParam("systolicTarget") Integer systolicTarget,
                                              @RequestParam("diastolicTarget") Integer diastolicTarget) {

        // THERE CAN BE ONLY ONE!!!
        // (blood pressure goal, that is)

        Goal currentBPGoal = goalService.getCurrentBPGoal(session.getId());

        currentBPGoal.setExtGoalId(extGoalId);
        currentBPGoal.setReferenceSystem(referenceSystem);
        currentBPGoal.setReferenceCode(referenceCode);
        currentBPGoal.setSystolicTarget(systolicTarget);
        currentBPGoal.setDiastolicTarget(diastolicTarget);
        currentBPGoal = goalService.update(session.getId(), currentBPGoal);

        return new ResponseEntity<>(new GoalModel(currentBPGoal), HttpStatus.OK);
    }

    @PostMapping("set-status")
    public ResponseEntity<GoalHistoryModel> updateStatus(HttpSession session,
                                                         @RequestParam("extGoalId") String extGoalId,
                                                         @RequestParam("achievementStatus") String achievementStatusStr) {

        Goal g = goalService.getGoal(session.getId(), extGoalId);
        GoalHistory gh = new GoalHistory(AchievementStatus.valueOf(achievementStatusStr), g);
        gh = goalHistoryService.create(gh);

        return new ResponseEntity<>(new GoalHistoryModel(gh), HttpStatus.OK);
    }

//    @PostMapping("delete")
//    public ResponseEntity<String> delete(HttpSession session,
//                                         @RequestParam("extGoalId") String extGoalId) {
//        try {
//            goalService.delete(session.getId(), extGoalId);
//            return new ResponseEntity<>(extGoalId, HttpStatus.OK);
//
//        } catch (Exception e) {
//            return new ResponseEntity<>("Caught " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}
