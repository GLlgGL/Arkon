package com.fms.web;

import com.fms.service.CurrencyService;
import com.fms.service.FxRevaluationService;
import com.fms.service.OrderException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Accounting → Exchange rate adjustment (year-end revaluation of EUR / USD balances). */
@Controller
public class FxRevaluationController {

    private final FxRevaluationService fx;
    private final CurrencyService currency;
    private final CurrentUser currentUser;

    public FxRevaluationController(FxRevaluationService fx, CurrencyService currency, CurrentUser currentUser) {
        this.fx = fx; this.currency = currency; this.currentUser = currentUser;
    }

    private Map<String, BigDecimal> rates(LocalDate d, BigDecimal eur, BigDecimal usd) {
        Map<String, BigDecimal> m = new HashMap<>();
        BigDecimal e = eur != null ? eur : currency.rateOn("EUR", d), u = usd != null ? usd : currency.rateOn("USD", d);
        if (e != null) m.put("EUR", e);
        if (u != null) m.put("USD", u);
        return m;
    }

    @GetMapping("/accounting/fx-revaluation")
    public String page(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) BigDecimal eur, @RequestParam(required = false) BigDecimal usd, Model model) {
        LocalDate d = date != null ? date : LocalDate.of(LocalDate.now().getYear() - (LocalDate.now().getMonthValue() <= 6 ? 1 : 0), 12, 31);
        Map<String, BigDecimal> r = rates(d, eur, usd);
        model.addAttribute("date", d);
        model.addAttribute("rates", r);
        try {
            model.addAttribute("items", fx.preview(d, r));
        } catch (OrderException e) {
            model.addAttribute("missing", e.getMessage());
        }
        model.addAttribute("history", fx.history());
        return "accounting/fx-revaluation";
    }

    @PostMapping("/accounting/fx-revaluation")
    public String post(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) BigDecimal eur, @RequestParam(required = false) BigDecimal usd,
                       @RequestParam(defaultValue = "false") boolean reverse, Principal principal, RedirectAttributes redirect) {
        try {
            int n = fx.post(date, rates(date, eur, usd), reverse, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Exchange rate adjustment posted: " + n + " items.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/fx-revaluation?date=" + date;
    }

    @PostMapping("/accounting/fx-revaluation/undo")
    public String undo(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, RedirectAttributes redirect) {
        try {
            fx.undo(date);
            redirect.addFlashAttribute("message", "Exchange rate adjustment undone.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/fx-revaluation?date=" + date;
    }
}
