package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.service.ExcelService;
import com.fms.service.PaymentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Who owes us (unpaid sales) and whom we owe (unpaid purchases). Administrator-only. */
@Controller
public class DebtsController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.RecordService records;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.AgingService aging;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.AppUserRepository userRepo;

    private com.fms.domain.AppUser currentUserOf() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : userRepo.findByUsername(auth.getName()).orElse(null);
    }

    private final PaymentService payments;
    private final ExcelService excel;

    public DebtsController(PaymentService payments, ExcelService excel) {
        this.payments = payments;
        this.excel = excel;
    }

    @GetMapping("/debts")
    public String root() {
        return "redirect:/debts/sales";
    }

    @GetMapping("/debts/{type:sales|purchases}")
    public String page(@PathVariable String type, @org.springframework.web.bind.annotation.RequestParam(required = false) String view, Model model) {
        OrderType t = OrderType.fromPath(type);
        model.addAttribute("type", t);
        var vis = records.visibility(currentUserOf());
        if ("age".equals(view)) {                         // the same open invoices, grouped by client and by age
            model.addAttribute("a", aging.aging(t, r -> vis.sees(t, r.code(), r.createdBy())));
            model.addAttribute("side", type);
            return "debts/aging";
        }
        model.addAttribute("debts", payments.debts(t, r -> vis.sees(t, r.code(), r.createdBy())));
        return "debts";
    }

    @GetMapping("/debts/{type:sales|purchases}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type) {
        OrderType t = OrderType.fromPath(type);
        var vis = records.visibility(currentUserOf());
        var d = payments.debts(t, r -> vis.sees(t, r.code(), r.createdBy()));
        List<Object[]> rows = d.orders().stream().map(r -> new Object[] {r.code(), r.party(), String.valueOf(r.date()),
                String.valueOf(r.dueDate()), r.total(), r.paid(), r.remaining(), r.daysOverdue(), r.status().getLabel()}).toList();
        byte[] file = excel.table(t == OrderType.SALE ? "Clients owe" : "We owe",
                new String[] {"Order no", t == OrderType.SALE ? "Client" : "Supplier", "Date", "Due", "Total", "Paid",
                        "Remaining", "Days overdue", "Status"}, rows);
        return OrderController.xlsx(file, (t == OrderType.SALE ? "receivables" : "payables") + ".xlsx");
    }
}
