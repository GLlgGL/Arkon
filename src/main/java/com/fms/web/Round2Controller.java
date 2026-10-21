package com.fms.web;

import com.fms.config.TwoFactorGuard;
import com.fms.repo.AppUserRepository;
import com.fms.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Two-step login, price lists, budgets and the cash-flow forecast. Each is an optional module. */
@Controller
public class Round2Controller {

    private static final String NEW_SECRET = "tfaNewSecret";
    private static final String FAILS = "tfaFails";

    private final TotpService totp;
    private final AppUserRepository users;
    private final PriceListService priceLists;
    private final BudgetService budgets;
    private final ConfigService config;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public Round2Controller(TotpService totp, AppUserRepository users, PriceListService priceLists, BudgetService budgets,
                            ConfigService config, AuditService audit, CurrentUser currentUser) {
        this.totp = totp; this.users = users; this.priceLists = priceLists; this.budgets = budgets;
        this.config = config; this.audit = audit; this.currentUser = currentUser;
    }

    // ----------------------------------------------------------- two-step login

    @GetMapping("/login/2fa")
    public String codePage(HttpSession session) {
        return session.getAttribute(TwoFactorGuard.PENDING) == null ? "redirect:/home" : "login-2fa";
    }

    @PostMapping("/login/2fa")
    public String checkCode(@RequestParam String code, Principal principal, HttpSession session, HttpServletRequest request,
                            RedirectAttributes redirect) {
        var u = currentUser.of(principal);
        if (totp.verify(u.getTotpSecret(), code)) {
            session.removeAttribute(TwoFactorGuard.PENDING);
            session.removeAttribute(FAILS);
            audit.logAs(u.getUsername(), "LOGIN_2FA", "User", u.getUsername(), request.getRemoteAddr());
            return "redirect:/home";
        }
        int fails = session.getAttribute(FAILS) == null ? 1 : (Integer) session.getAttribute(FAILS) + 1;
        session.setAttribute(FAILS, fails);
        audit.logAs(u.getUsername(), "LOGIN_2FA_FAILED", "User", u.getUsername(), request.getRemoteAddr());
        if (fails >= 5) {
            session.invalidate();
            return "redirect:/login?error";
        }
        redirect.addFlashAttribute("error", "That code isn't right. Check the time on your phone and try again.");
        return "redirect:/login/2fa";
    }

    @GetMapping("/account/2fa")
    public String setupPage(Principal principal, HttpSession session, Model model) {
        var u = currentUser.of(principal);
        if (!u.isTotpEnabled()) {
            String secret = (String) session.getAttribute(NEW_SECRET);
            if (secret == null) {
                secret = totp.newSecret();
                session.setAttribute(NEW_SECRET, secret);
            }
            model.addAttribute("qr", totp.qr(totp.uri(u.getUsername(), secret)));
            model.addAttribute("secret", TotpService.grouped(secret));
        }
        model.addAttribute("enabled", u.isTotpEnabled());
        model.addAttribute("required", config.get().isTfaRequired());
        model.addAttribute("mustSetUp", session.getAttribute(TwoFactorGuard.SETUP) != null);
        return "account-2fa";
    }

    @PostMapping("/account/2fa/enable")
    public String enable(@RequestParam String code, Principal principal, HttpSession session, RedirectAttributes redirect) {
        var u = currentUser.of(principal);
        String secret = (String) session.getAttribute(NEW_SECRET);
        if (secret == null || !totp.verify(secret, code)) {
            redirect.addFlashAttribute("error", "That code isn't right. Check the time on your phone and try again.");
            return "redirect:/account/2fa";
        }
        u.setTotpSecret(secret);
        u.setTotpEnabled(true);
        users.save(u);
        session.removeAttribute(NEW_SECRET);
        session.removeAttribute(TwoFactorGuard.SETUP);
        audit.log("ENABLE_2FA", "User", u.getUsername(), null);
        redirect.addFlashAttribute("message", "Two-step login is on.");
        return "redirect:/home";
    }

    @PostMapping("/account/2fa/disable")
    public String disable(@RequestParam String code, Principal principal, RedirectAttributes redirect) {
        var u = currentUser.of(principal);
        if (config.get().isTfaRequired()) {
            redirect.addFlashAttribute("error", "Two-step login is required for everyone, so it can't be switched off.");
            return "redirect:/account/2fa";
        }
        if (!totp.verify(u.getTotpSecret(), code)) {
            redirect.addFlashAttribute("error", "That code isn't right. Check the time on your phone and try again.");
            return "redirect:/account/2fa";
        }
        u.setTotpEnabled(false);
        u.setTotpSecret(null);
        users.save(u);
        audit.log("DISABLE_2FA", "User", u.getUsername(), null);
        redirect.addFlashAttribute("message", "Two-step login is off.");
        return "redirect:/account/2fa";
    }

    @PostMapping("/settings/2fa")
    public String tfaSettings(@RequestParam(defaultValue = "false") boolean required, RedirectAttributes redirect) {
        var c = config.get();
        c.setTfaRequired(required);
        config.save(c);
        audit.log("UPDATE", "Settings", "Two-step login", required ? "required for everyone" : "optional");
        redirect.addFlashAttribute("message", "Two-step login settings saved.");
        return "redirect:/settings";
    }

    // ------------------------------------------------------------- price lists

    @GetMapping("/admin/price-lists")
    public String lists(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("lists", priceLists.all());
        if (edit != null) {
            model.addAttribute("editing", priceLists.get(edit));
            model.addAttribute("prices", priceLists.prices(edit));
            model.addAttribute("products", productRepo.findAllByOrderByNameAsc());
        }
        return "admin/price-lists";
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.ProductRepository productRepo;

    @PostMapping("/admin/price-lists")
    public String saveList(@RequestParam(required = false) Long id, @RequestParam String name,
                           @RequestParam(required = false) String description, @RequestParam(defaultValue = "false") boolean active,
                           RedirectAttributes redirect) {
        try {
            var l = priceLists.save(id, name, description, active);
            redirect.addFlashAttribute("message", "Price list saved.");
            return "redirect:/admin/price-lists?edit=" + l.getId();
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/price-lists";
        }
    }

    @PostMapping("/admin/price-lists/{id:\\d+}/prices")
    public String savePrices(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirect) {
        Map<Long, BigDecimal> m = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("p_")) {
                Long pid = Long.valueOf(k.substring(2));
                try { m.put(pid, v.length == 0 || v[0].isBlank() ? null : new BigDecimal(v[0].trim())); } catch (NumberFormatException ignored) { }
            }
        });
        priceLists.savePrices(id, m);
        redirect.addFlashAttribute("message", "Prices saved.");
        return "redirect:/admin/price-lists?edit=" + id;
    }

    @PostMapping("/admin/price-lists/{id:\\d+}/fill")
    public String fill(@PathVariable Long id, @RequestParam(defaultValue = "0") BigDecimal percent, RedirectAttributes redirect) {
        priceLists.fillFromSalePrice(id, percent);
        redirect.addFlashAttribute("message", "Prices saved.");
        return "redirect:/admin/price-lists?edit=" + id;
    }

    @PostMapping("/admin/price-lists/{id:\\d+}/delete")
    public String deleteList(@PathVariable Long id, RedirectAttributes redirect) {
        priceLists.delete(id);
        redirect.addFlashAttribute("message", "Price list deleted.");
        return "redirect:/admin/price-lists";
    }

    @PostMapping("/admin/clients/{id:\\d+}/price-list")
    public String clientList(@PathVariable Long id, @RequestParam(required = false) Long listId, RedirectAttributes redirect) {
        priceLists.setClientList(id, listId);
        redirect.addFlashAttribute("message", "Price list saved.");
        return "redirect:/admin/clients/" + id;
    }

    // ----------------------------------------------------------------- budgets

    @GetMapping("/budgets")
    public String budget(@RequestParam(required = false) Integer year, Model model) {
        int y = year == null ? LocalDate.now().getYear() : year;
        var b = budgets.budget(y);
        model.addAttribute("year", y);
        model.addAttribute("keys", BudgetService.KEYS);
        model.addAttribute("budget", b);
        model.addAttribute("result", BudgetService.result(b));
        return "budgets/budget";
    }

    @PostMapping("/budgets")
    public String saveBudget(@RequestParam int year, HttpServletRequest request, RedirectAttributes redirect) {
        Map<String, BigDecimal[]> m = new HashMap<>();
        for (String k : BudgetService.KEYS) {
            BigDecimal[] a = new BigDecimal[12];
            for (int i = 1; i <= 12; i++) {
                String v = request.getParameter("b_" + k + "_" + i);
                try { a[i - 1] = v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.trim()); } catch (NumberFormatException e) { a[i - 1] = BigDecimal.ZERO; }
            }
            m.put(k, a);
        }
        budgets.save(year, m);
        redirect.addFlashAttribute("message", "Budget saved.");
        return "redirect:/budgets?year=" + year;
    }

    @PostMapping("/budgets/fill")
    public String fillBudget(@RequestParam int year, @RequestParam(defaultValue = "0") BigDecimal percent, RedirectAttributes redirect) {
        budgets.fillFromLastYear(year, percent);
        redirect.addFlashAttribute("message", "Budget saved.");
        return "redirect:/budgets?year=" + year;
    }

    @GetMapping("/budgets/compare")
    public String compare(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month, Model model) {
        LocalDate now = LocalDate.now();
        int y = year == null ? now.getYear() : year, m = month == null ? (y == now.getYear() ? now.getMonthValue() : 12) : Math.max(1, Math.min(12, month));
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("rows", budgets.compare(y, m));
        model.addAttribute("budgetResult", BudgetService.result(budgets.budget(y)));
        model.addAttribute("actualResult", BudgetService.result(budgets.actual(y)));
        return "budgets/compare";
    }

    @GetMapping("/budgets/forecast")
    public String forecast(Model model) {
        var f = budgets.forecast();
        model.addAttribute("f", f);
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        model.addAttribute("labels", f.weeks().stream().map(w -> w.start().format(fmt)).toList());
        model.addAttribute("balances", f.weeks().stream().map(BudgetService.Week::balance).toList());
        return "budgets/forecast";
    }
}
