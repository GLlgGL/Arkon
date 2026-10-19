package com.fms.web;

import com.fms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ChangePassword. The original showed the password in an editable grid and
 * only let administrators in; here every user can change their own password
 * and must confirm the current one. After a successful change the user is
 * logged out and logs in again with the new password.
 */
@Controller
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final UserService users;
    private final com.fms.service.AuditService audit;

    public ProfileController(UserService users, com.fms.service.AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping("/profile")
    public String page() {
        return "profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, Principal principal,
                                 HttpServletRequest request, HttpServletResponse response,
                                 RedirectAttributes redirect) {
        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "The new passwords don't match.");
            return "redirect:/profile";
        }
        try {
            users.changePassword(principal.getName(), currentPassword, newPassword);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }

        log.info("User {} changed their password; logging them out", principal.getName());
        audit.log("CHANGE_PASSWORD", "User", principal.getName(), null);
        // End this session so the new password is used from the next login.
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        // The session (and its flash messages) is gone, so pass the notice in the URL.
        return "redirect:/login?passwordChanged";
    }
}
