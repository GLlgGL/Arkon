package com.fms.web;

import com.fms.service.DashboardService;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /** The personal dashboard. */
    @GetMapping("/home")
    public String home(Principal principal, Model model) {
        var u = currentUser.of(principal);
        var widgets = dashboard.layout(u);
        model.addAttribute("widgets", widgets);
        model.addAttribute("w", dashboard.data(u, widgets));
        return "home";
    }

    @GetMapping("/home/customize")
    public String customize(Principal principal, Model model) {
        var u = currentUser.of(principal);
        var layout = dashboard.layout(u);
        var others = dashboard.allowed(u).stream().filter(x -> !layout.contains(x)).toList();
        model.addAttribute("layout", layout);
        model.addAttribute("others", others);
        model.addAttribute("custom", dashboard.isCustom(u));
        return "dashboard-customize";
    }

    @PostMapping("/home/customize")
    public String save(@RequestParam(defaultValue = "") String widgets, Principal principal, RedirectAttributes redirect) {
        dashboard.save(currentUser.of(principal), Arrays.stream(widgets.split(",")).filter(s -> !s.isBlank()).toList());
        redirect.addFlashAttribute("message", "Dashboard saved.");
        return "redirect:/home";
    }

    @PostMapping("/home/customize/reset")
    public String reset(Principal principal, RedirectAttributes redirect) {
        dashboard.reset(currentUser.of(principal));
        redirect.addFlashAttribute("message", "Dashboard reset to the default.");
        return "redirect:/home";
    }
}
