package com.fms.web;

import com.fms.service.DashboardService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final DashboardService dashboard;
    private final CurrentUser currentUser;

    public HomeController(DashboardService dashboard, CurrentUser currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

    /** Replaces Default.aspx / LogIn.aspx. Spring Security handles the POST. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    /** Replaces Home.aspx: dashboard strip and the eight module tiles. */
    @GetMapping("/home")
    public String home(Principal principal, Model model) {
        model.addAttribute("strip", dashboard.strip(currentUser.of(principal)));
        return "home";
    }
}
