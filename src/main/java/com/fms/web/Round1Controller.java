package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Client/supplier statements, stock count, notifications and payment
 * reminders, period lock and year-end closing. Each is an optional module.
 */
@Controller
public class Round1Controller {

    private final StatementService statements;
    private final StockCountService counts;
    private final NotificationService notes;
    private final AccountingService accounting;
    private final ConfigService config;
    private final ExcelService excel;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public Round1Controller(StatementService statements, StockCountService counts, NotificationService notes,
                            AccountingService accounting, ConfigService config, ExcelService excel, AuditService audit,
                            CurrentUser currentUser) {
        this.statements = statements; this.counts = counts; this.notes = notes; this.accounting = accounting;
        this.config = config; this.excel = excel; this.audit = audit; this.currentUser = currentUser;
    }

    // ------------------------------------------------------------- statements

    @GetMapping("/statements/{side:clients|suppliers}")
    public String statement(@PathVariable String side, @RequestParam(required = false) String party,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        OrderType t = "clients".equals(side) ? OrderType.SALE : OrderType.PURCHASE;
        LocalDate f = from == null ? LocalDate.now().withDayOfYear(1) : from, tt = to == null ? LocalDate.now() : to;
        model.addAttribute("side", side);
        model.addAttribute("parties", statements.parties(t));
        model.addAttribute("party", party);
        if (party != null && !party.isBlank()) model.addAttribute("st", statements.statement(t, party, f, tt));
        model.addAttribute("from", f);
        model.addAttribute("to", tt);
        return "statements/statement";
    }

    @GetMapping("/statements/{side:clients|suppliers}/export")
    public ResponseEntity<byte[]> statementExport(@PathVariable String side, @RequestParam String party,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var st = statements.statement("clients".equals(side) ? OrderType.SALE : OrderType.PURCHASE, party, from, to);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {String.valueOf(from), "", "Opening balance", null, null, st.opening()});
        st.rows().forEach(r -> rows.add(new Object[] {String.valueOf(r.date()), r.document(), r.kind(), r.charge(), r.credit(), r.balance()}));
        return OrderController.xlsx(excel.table("Statement", new String[] {"Date", "Document", "Type", "Charge", "Payment / credit", "Balance"}, rows),
                "statement-" + party.replaceAll("[^A-Za-z0-9]+", "-") + ".xlsx");
    }

    // ------------------------------------------------------------ stock count

    @GetMapping("/stock-count")
    public String counts(Model model) {
        model.addAttribute("counts", counts.all());
        return "stock-count/list";
    }

    @PostMapping("/stock-count")
    public String newCount(@RequestParam(required = false) String note, @RequestParam(required = false) Long locationId,
                           Principal principal, RedirectAttributes redirect) {
        try {
            var c = counts.create(note, locationId, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Stock count started.");
            return "redirect:/stock-count/" + c.getCode();
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/stock-count";
        }
    }

    @GetMapping("/stock-count/{code:SC-\\d+}")
    public String count(@PathVariable String code, Model model) {
        var c = counts.get(code);
        var ls = counts.lines(c.getId());
        model.addAttribute("c", c);
        model.addAttribute("lines", ls);
        model.addAttribute("countedN", ls.stream().filter(l -> l.getCounted() != null).count());
        model.addAttribute("diffN", ls.stream().filter(l -> l.getDifference() != null && l.getDifference() != 0).count());
        return "stock-count/count";
    }

    @PostMapping("/stock-count/{code:SC-\\d+}")
    public String saveCount(@PathVariable String code, @RequestParam(defaultValue = "save") String action,
                            jakarta.servlet.http.HttpServletRequest request, Principal principal, RedirectAttributes redirect) {
        Map<Long, Long> counted = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("q_")) {
                Long id = Long.valueOf(k.substring(2));
                try { counted.put(id, v.length == 0 || v[0].isBlank() ? null : Long.valueOf(v[0].trim())); } catch (NumberFormatException ignored) { }
            }
        });
        try {
            counts.save(code, counted);
            if ("apply".equals(action)) {
                int n = counts.apply(code, currentUser.of(principal));
                redirect.addFlashAttribute("message", "Stock count applied: " + n + " corrections.");
            } else if ("cancel".equals(action)) {
                counts.cancel(code);
                redirect.addFlashAttribute("message", "Stock count cancelled.");
            } else {
                redirect.addFlashAttribute("message", "Counts saved.");
            }
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock-count/" + code;
    }

    // ------------------------------------------------- notifications, reminders

    @GetMapping("/api/notifications")
    @ResponseBody
    public List<NotificationService.Note> notifications(Principal principal) {
        if (!config.get().isModReminders()) return List.of();
        return notes.forUser(currentUser.of(principal));
    }

    @GetMapping("/reminders")
    public String reminders(Model model) {
        model.addAttribute("candidates", notes.candidates());
        model.addAttribute("recent", notes.recent());
        return "reminders";
    }

    @PostMapping("/reminders/send")
    public String sendReminders(RedirectAttributes redirect) {
        try {
            int n = notes.sendDue();
            redirect.addFlashAttribute("message", "Reminders sent: " + n + ".");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reminders";
    }

    // ------------------------------------------------------ settings (admin)

    @PostMapping("/settings/period-lock")
    public String periodLock(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lockDate,
                             RedirectAttributes redirect) {
        var c = config.get();
        c.setLockDate(lockDate);
        config.save(c);
        audit.log("UPDATE", "Settings", "Period lock", lockDate == null ? "unlocked" : "locked up to " + lockDate);
        redirect.addFlashAttribute("message", lockDate == null ? "All periods are open." : "Period lock saved.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/reminders")
    public String reminderSettings(@RequestParam int daysBefore, @RequestParam int daysAfter,
                                   @RequestParam(defaultValue = "false") boolean auto, RedirectAttributes redirect) {
        var c = config.get();
        c.setRemDaysBefore(Math.max(0, Math.min(60, daysBefore)));
        c.setRemDaysAfter(Math.max(1, Math.min(60, daysAfter)));
        c.setRemAuto(auto);
        config.save(c);
        audit.log("UPDATE", "Settings", "Reminders", daysBefore + " days before, every " + daysAfter + " days after, auto " + auto);
        redirect.addFlashAttribute("message", "Reminder settings saved.");
        return "redirect:/settings";
    }

    // ------------------------------------------------------- year-end closing

    @PostMapping("/accounting/close-year")
    public String closeYear(@RequestParam int year, @RequestParam(defaultValue = "false") boolean lock,
                            Principal principal, RedirectAttributes redirect) {
        try {
            BigDecimal result = accounting.closeYear(year, currentUser.of(principal));
            var c = config.get();
            if (lock && c.isModPeriodLock()) {
                LocalDate end = LocalDate.of(year, 12, 31);
                if (c.getLockDate() == null || c.getLockDate().isBefore(end)) {
                    c.setLockDate(end);
                    config.save(c);
                }
            }
            redirect.addFlashAttribute("message", "Year " + year + " closed. Result: " + Money.format(result) + ".");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/official?year=" + year;
    }
}
