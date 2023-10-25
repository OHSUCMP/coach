package edu.ohsu.cmp.coach.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

// This is linked in the recommendations. Don't change the URL.
@Controller
public class ResourcesController extends BaseController {
    
    @GetMapping("/symptoms-911")
    public String symptoms(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        return "symptoms";
    }

    @GetMapping("/side-effects")
    public String sideEffects(HttpSession session, Model model) {
        model.addAttribute("applicationName", applicationName);
        return "side-effects";
    }

}
