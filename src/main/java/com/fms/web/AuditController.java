package com.fms.web;

import com.fms.service.AuditService;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;

/** Audit log: who did what and when. Administrator-only. */
@Controller
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/admin/audit")
    public String page(@RequestParam(required = false) String q,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("page", audit.search(q, from, to, PageRequest.of(Math.max(page, 0), 50)));
        return "admin/audit";
    }

    @PostMapping("/admin/audit/clear")
    public String clear(RedirectAttributes redirect) {
        long n = audit.clearAll();
        redirect.addFlashAttribute("message", "Audit log cleared (" + n + " entries).");
        return "redirect:/admin/audit";
    }
}
