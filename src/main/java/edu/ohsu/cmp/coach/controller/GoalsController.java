package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.cache.CacheData;
import edu.ohsu.cmp.coach.cache.SessionCache;
import edu.ohsu.cmp.coach.entity.app.AchievementStatus;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.model.GoalHistoryModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.service.GoalHistoryService;
import edu.ohsu.cmp.coach.service.GoalService;
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
@RequestMapping("/goals")
public class GoalsController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EHRService ehrService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalHistoryService goalHistoryService;

//    @Autowired
//    private CQFRulerService cqfRulerService;

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", new PatientModel(ehrService.getPatient(session.getId())));
        model.addAttribute("bpGoal", goalService.getCurrentBPGoal(session.getId()));

        List<GoalModel> otherGoals = new ArrayList<>();
        for (MyGoal g : goalService.getGoalList(session.getId())) {
            if ( ! g.isBloodPressureGoal() ) {
                otherGoals.add(new GoalModel(g));
            }
        }
        model.addAttribute("hasOtherGoals", otherGoals.size() > 0);
        model.addAttribute("otherGoals", otherGoals);

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

        MyGoal myGoal = goalService.getGoal(session.getId(), extGoalId);
        if (myGoal == null) {
            myGoal = new MyGoal(extGoalId, referenceSystem, referenceCode, goalText, targetDate);
            myGoal = goalService.create(session.getId(), myGoal);

            // remove goal from cache
            CacheData cache = SessionCache.getInstance().get(session.getId());
            cache.deleteSuggestion(extGoalId);

            return new ResponseEntity<>(new GoalModel(myGoal), HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }

    @PostMapping("update-bp")
    public ResponseEntity<GoalModel> updatebp(HttpSession session,
//                                              @RequestParam("extGoalId") String extGoalId,
//                                              @RequestParam("referenceSystem") String referenceSystem,
//                                              @RequestParam("referenceCode") String referenceCode,
                                              @RequestParam("systolicTarget") Integer systolicTarget,
                                              @RequestParam("diastolicTarget") Integer diastolicTarget) {

        // THERE CAN BE ONLY ONE!!!
        // (blood pressure goal, that is)

//        goalService.deleteBPGoalIfExists(session.getId());

        MyGoal goal = goalService.getCurrentAppBPGoal(session.getId());
        if (goal != null) {
            goal.setSystolicTarget(systolicTarget);
            goal.setDiastolicTarget(diastolicTarget);
            goal = goalService.update(goal);

        } else {
            goal = goalService.create(session.getId(), new MyGoal(
                    fcm.getBpSystem(), fcm.getBpCode(),
                    systolicTarget, diastolicTarget));
        }

        return new ResponseEntity<>(new GoalModel(goal), HttpStatus.OK);
    }

    @PostMapping("set-status")
    public ResponseEntity<GoalHistoryModel> updateStatus(HttpSession session,
                                                         @RequestParam("extGoalId") String extGoalId,
                                                         @RequestParam("achievementStatus") String achievementStatusStr) {

        MyGoal g = goalService.getGoal(session.getId(), extGoalId);
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
