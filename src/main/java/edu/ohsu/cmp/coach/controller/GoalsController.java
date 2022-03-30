package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.entity.app.AchievementStatus;
import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import edu.ohsu.cmp.coach.model.GoalHistoryModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.service.EHRService;
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
import java.util.Collections;
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

    @GetMapping(value={"", "/"})
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", workspaceService.get(session.getId()).getPatient());
        model.addAttribute("bpGoal", goalService.getCurrentBPGoal(session.getId()));
        model.addAttribute("hasOtherGoals", goalService.hasAnyLocalNonBPGoals(session.getId()));

        return "goals";
    }

    @PostMapping("other-goals")
    public ResponseEntity<List<GoalModel>> getOtherGoalsList(HttpSession session) {
        List<GoalModel> list = new ArrayList<GoalModel>();
        for (MyGoal g : goalService.getAllLocalNonBPGoals(session.getId())) {
            list.add(new GoalModel(g));
        }

        Collections.sort(list);

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("create")
    public ResponseEntity<GoalModel> create(HttpSession session,
                                            @RequestParam("extGoalId") String extGoalId,
                                            @RequestParam("referenceSystem") String referenceSystem,
                                            @RequestParam("referenceCode") String referenceCode,
                                            @RequestParam("referenceDisplay") String referenceDisplay,
                                            @RequestParam("goalText") String goalText,
                                            @RequestParam("targetDateTS") Long targetDateTS) {

        Date targetDate = new Date(targetDateTS);

        MyGoal myGoal = goalService.getLocalGoal(session.getId(), extGoalId);
        if (myGoal == null) {
            myGoal = new MyGoal(extGoalId, referenceSystem, referenceCode, referenceDisplay, goalText, targetDate);
            myGoal = goalService.create(session.getId(), myGoal);

            // the goal was created in response to a suggestion.
            // they took the suggestion, so remove it from the list to consider
            UserWorkspace workspace = workspaceService.get(session.getId());
            workspace.deleteSuggestion(extGoalId);

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

        MyGoal goal = goalService.getCurrentLocalBPGoal(session.getId());
        if (goal != null) {
            goal.setSystolicTarget(systolicTarget);
            goal.setDiastolicTarget(diastolicTarget);
            goal = goalService.update(goal);

        } else {
            goal = goalService.create(session.getId(), new MyGoal(
                    fcm.getBpSystem(), fcm.getBpCode(), "Blood Pressure",
                    systolicTarget, diastolicTarget));
        }

        return new ResponseEntity<>(new GoalModel(goal), HttpStatus.OK);
    }

    @PostMapping("update-status")
    public ResponseEntity<GoalHistoryModel> updateStatus(HttpSession session,
                                                         @RequestParam("extGoalId") String extGoalId,
                                                         @RequestParam("achievementStatus") String achievementStatusStr) {

        MyGoal g = goalService.getLocalGoal(session.getId(), extGoalId);
        GoalHistory gh = new GoalHistory(AchievementStatus.valueOf(achievementStatusStr), g);
        gh = goalService.createHistory(gh);

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
