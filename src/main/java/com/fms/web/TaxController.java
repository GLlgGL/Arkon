package com.fms.web;

import com.fms.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Tax due (detyrimet tatimore). Administrator-only; optional module. */
@Controller
public class TaxController {

    private final TaxService taxes;
    private final CashService cash;
    private final ConfigService config;
    private final CurrentUser currentUser;

    public TaxController(TaxService taxes, CashService cash, ConfigService config, CurrentUser currentUser) {
        this.taxes = taxes;
        this.cash = cash;
        this.config = config;
        this.currentUser = currentUser;
    }

    @GetMapping("/taxes")
    public String overview(Model model) {
        model.addAttribute("rows", taxes.overview());
        model.addAttribute("payments", taxes.payments());
        if (config.get().isModCash()) {
            model.addAttribute("accounts", cash.activeAccounts());
        }
        model.addAttribute("today", LocalDate.now());
        return "taxes/overview";
    }

    @PostMapping("/taxes/pay")
    public String pay(@RequestParam String kind, @RequestParam String period, @RequestParam(required = false) BigDecimal amount,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) Long accountId, @RequestParam(required = false) String note,
                      Principal principal, RedirectAttributes redirect) {
        try {
            taxes.pay(kind, period, amount, date, accountId, note, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Tax payment recorded.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/taxes";
    }

    @PostMapping("/taxes/payments/{id:\\d+}/delete")
    public String deletePayment(@PathVariable Long id, RedirectAttributes redirect) {
        taxes.deletePayment(id);
        redirect.addFlashAttribute("message", "Tax payment deleted.");
        return "redirect:/taxes";
    }

    @PostMapping("/taxes/settings")
    public String settings(@RequestParam int vatDay, @RequestParam int payrollDay, RedirectAttributes redirect) {
        var c = config.get();
        c.setTaxVatDay(Math.max(1, Math.min(28, vatDay)));
        c.setTaxPayrollDay(Math.max(1, Math.min(28, payrollDay)));
        config.save(c);
        redirect.addFlashAttribute("message", "Due dates saved.");
        return "redirect:/taxes";
    }
}
