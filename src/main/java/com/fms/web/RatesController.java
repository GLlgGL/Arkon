package com.fms.web;

import com.fms.service.CurrencyService;
import com.fms.service.OrderException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Exchange rates (Administration). Multi-currency module. */
@Controller
public class RatesController {

    private final CurrencyService fx;

    public RatesController(CurrencyService fx) {
        this.fx = fx;
    }

    @GetMapping("/admin/rates")
    public String rates(Model model) {
        model.addAttribute("rates", fx.recent());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("eurToday", fx.rateOn("EUR", LocalDate.now()));
        model.addAttribute("usdToday", fx.rateOn("USD", LocalDate.now()));
        return "admin/rates";
    }

    @PostMapping("/admin/rates")
    public String save(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) BigDecimal eur, @RequestParam(required = false) BigDecimal usd,
                       RedirectAttributes redirect) {
        try {
            if (eur != null) fx.saveRate(date, "EUR", eur);
            if (usd != null) fx.saveRate(date, "USD", usd);
            if (eur == null && usd == null) throw new OrderException("Enter a rate greater than 0.");
            redirect.addFlashAttribute("message", "Exchange rates saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rates";
    }

    @PostMapping("/admin/rates/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        fx.deleteRate(id);
        redirect.addFlashAttribute("message", "Exchange rate deleted.");
        return "redirect:/admin/rates";
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.PaymentRepository paymentRepo;

    /** Every foreign-currency payment with an exchange difference. */
    @GetMapping("/reports/fx-differences")
    public String differences(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        LocalDate f = from == null ? LocalDate.now().withDayOfYear(1) : from, t = to == null ? LocalDate.now() : to;
        var list = paymentRepo.findAll().stream()
                .filter(p -> p.getFxDiff() != null && p.getFxDiff().signum() != 0 && !p.getPaidOn().isBefore(f) && !p.getPaidOn().isAfter(t))
                .sorted(java.util.Comparator.comparing(com.fms.domain.Payment::getPaidOn)).toList();
        BigDecimal gains = list.stream().map(com.fms.domain.Payment::getFxDiff).filter(d -> d.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal losses = list.stream().map(com.fms.domain.Payment::getFxDiff).filter(d -> d.signum() < 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        java.util.Map<Long, BigDecimal> orderRates = new java.util.HashMap<>();
        list.forEach(p -> orderRates.put(p.getId(), fx.ofOrder(p.getOrderType(), p.getOrderCode()).rate()));
        model.addAttribute("payments", list);
        model.addAttribute("orderRates", orderRates);
        model.addAttribute("gains", gains);
        model.addAttribute("losses", losses.negate());
        model.addAttribute("net", gains.add(losses));
        model.addAttribute("from", f);
        model.addAttribute("to", t);
        return "reports/fx-differences";
    }
}
