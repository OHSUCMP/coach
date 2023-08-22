package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.CounselingPageModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/symptoms")
public class SymptomsController extends BaseController {
    @GetMapping
    public String view(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("patient", userWorkspaceService.get(session.getId()).getPatient());
        return "symptoms";
    }

}
