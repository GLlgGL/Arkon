package com.fms.web;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.repo.AppUserRepository;
import com.fms.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UsersRegistration (not among the uploaded files; rebuilt from the Users columns). */
@Controller
public class UserAdminController {

    private final AppUserRepository users;
    private final UserService userService;

    private final com.fms.service.AuditService audit;
    private final com.fms.service.AccessService access;

    public UserAdminController(AppUserRepository users, UserService userService, com.fms.service.AuditService audit,
                               com.fms.service.AccessService access) {
        this.access = access;
        this.audit = audit;
        this.users = users;
        this.userService = userService;
    }

    @GetMapping("/admin/users")
    public String list(Model model) {
        model.addAttribute("items", users.findAllByOrderByUsernameAsc());
        return "admin/users";
    }

    @GetMapping("/admin/users/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("itemId", null);
        model.addAttribute("areas", com.fms.domain.Area.values());
        model.addAttribute("areasOrdered", areasOrdered());
        model.addAttribute("overrides", java.util.Map.of());
        return "admin/user-form";
    }

    @GetMapping("/admin/users/{id:\\d+}")
    public String editForm(@PathVariable Long id, Model model) {
        AppUser u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        model.addAttribute("form", UserForm.of(u));
        model.addAttribute("itemId", id);
        model.addAttribute("areas", com.fms.domain.Area.values());
        model.addAttribute("areasOrdered", areasOrdered());
        model.addAttribute("overrides", byName(access.overridesOf(u.getUsername())));
        return "admin/user-form";
    }

    @PostMapping({"/admin/users/new", "/admin/users/{id:\\d+}"})
    public String save(@PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("form") UserForm form, BindingResult errors,
                       Principal principal, Model model, RedirectAttributes redirect,
                       jakarta.servlet.http.HttpServletRequest request) {
        AppUser existing = id == null ? null
                : users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        users.findByUsername(form.getUsername())
                .filter(u -> existing == null || !u.getId().equals(existing.getId()))
                .ifPresent(u -> errors.rejectValue("username", "duplicate", "This username is taken."));
        if (existing == null && !StringUtils.hasText(form.getNewPassword())) {
            errors.rejectValue("newPassword", "required", "Set a password for the new user.");
        }
        if (StringUtils.hasText(form.getNewPassword()) && form.getNewPassword().length() < 8) {
            errors.rejectValue("newPassword", "short", "Use at least 8 characters.");
        }
        if (existing != null && existing.getUsername().equals(principal.getName()) && form.getRole() != AppUser.ROLE_ADMIN) {
            errors.rejectValue("role", "self", "You can't remove your own administrator role.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("itemId", id);
            model.addAttribute("areas", com.fms.domain.Area.values());
        model.addAttribute("areasOrdered", areasOrdered());
            model.addAttribute("overrides", existing == null ? java.util.Map.of() : byName(access.overridesOf(existing.getUsername())));
            return "admin/user-form";
        }
        AppUser u = existing == null ? new AppUser() : existing;
        u.setFirstName(form.getFirstName());
        u.setLastName(form.getLastName());
        u.setEmail(form.getEmail());
        u.setUsername(form.getUsername());
        u.setRole(form.getRole() == AppUser.ROLE_ADMIN ? AppUser.ROLE_ADMIN
                : form.getRole() == AppUser.ROLE_ACCOUNTANT ? AppUser.ROLE_ACCOUNTANT : AppUser.ROLE_USER);
        if (StringUtils.hasText(form.getNewPassword())) {
            userService.setPassword(u, form.getNewPassword());
        } else {
            users.save(u);
        }
        // Access per area: "DEFAULT" keeps the role's default; anything else overrides it.
        java.util.Map<com.fms.domain.Area, com.fms.domain.Area.Level> chosen = new java.util.EnumMap<>(com.fms.domain.Area.class);
        for (var a : com.fms.domain.Area.values()) {
            String v = request.getParameter("access_" + a.name());
            if (v != null && !v.equals("DEFAULT")) {
                try { chosen.put(a, com.fms.domain.Area.Level.valueOf(v)); } catch (IllegalArgumentException ignored) { }
            }
        }
        if (!u.isAdmin()) {
            access.save(u, chosen);
        }
        String pctParam = request.getParameter("commissionPct");   // sales commission
        if (pctParam != null) {
            try { u.setCommissionPct(pctParam.isBlank() ? null : new java.math.BigDecimal(pctParam.trim().replace(',', '.'))); } catch (NumberFormatException ignored) { }
            u.setCommissionBase(request.getParameter("commissionBase"));
            users.save(u);
        }
        String scopeParam = request.getParameter("recordScope");   // which records the user sees
        if (scopeParam != null) {
            u.setRecordScope(scopeParam);
            users.save(u);
        }
        String locParam = request.getParameter("locationId");      // Locations module: the user's shop
        if (locParam != null) {
            u.setLocationId(locParam.isBlank() ? null : Long.valueOf(locParam));
            users.save(u);
        }
        if ("true".equals(request.getParameter("resetTfa"))) {
            u.setTotpEnabled(false);
            u.setTotpSecret(null);
            users.save(u);
            audit.log("RESET_2FA", "User", u.getUsername(), null);
        }
        audit.log(existing == null ? "CREATE" : "UPDATE", "User", u.getUsername(),
                (u.isAdmin() ? "Administrator" : u.isAccountant() ? "Accountant" : "User") + (StringUtils.hasText(form.getNewPassword()) ? ", password set" : ""));
        redirect.addFlashAttribute("message", "User saved.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        AppUser u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (u.getUsername().equals(principal.getName())) {
            redirect.addFlashAttribute("error", "You can't delete your own account.");
        } else {
            access.deleteUser(u.getUsername());
            users.delete(u);
            audit.log("DELETE", "User", u.getUsername(), null);
            redirect.addFlashAttribute("message", "User " + u.getUsername() + " deleted.");
        }
        return "redirect:/admin/users";
    }

    /** Area name -> level name, for the access grid (a map keyed by enum doesn't work in page expressions). */
    private static java.util.Map<String, String> byName(java.util.Map<com.fms.domain.Area, com.fms.domain.Area.Level> m) {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        m.forEach((k, v) -> out.put(k.name(), v.name()));
        return out;
    }

    /** Main areas, each followed by its sub-areas (for the access grid). */
    private static java.util.List<com.fms.domain.Area> areasOrdered() {
        java.util.List<com.fms.domain.Area> out = new java.util.ArrayList<>();
        for (var a : com.fms.domain.Area.values()) {
            if (a.isSub()) continue;
            out.add(a);
            for (var s : com.fms.domain.Area.values()) if (s.getParent() == a) out.add(s);
        }
        return out;
    }
}
