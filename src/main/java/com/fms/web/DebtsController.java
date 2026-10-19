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
    public String page(@PathVariable String type, Model model) {
        OrderType t = OrderType.fromPath(type);
        model.addAttribute("type", t);
        model.addAttribute("debts", payments.debts(t));
        return "debts";
    }

    @GetMapping("/debts/{type:sales|purchases}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type) {
        OrderType t = OrderType.fromPath(type);
        var d = payments.debts(t);
        List<Object[]> rows = d.orders().stream().map(r -> new Object[] {r.code(), r.party(), String.valueOf(r.date()),
                String.valueOf(r.dueDate()), r.total(), r.paid(), r.remaining(), r.daysOverdue(), r.status().getLabel()}).toList();
        byte[] file = excel.table(t == OrderType.SALE ? "Clients owe" : "We owe",
                new String[] {"Order no", t == OrderType.SALE ? "Client" : "Merchant", "Date", "Due", "Total", "Paid",
                        "Remaining", "Days overdue", "Status"}, rows);
        return OrderController.xlsx(file, (t == OrderType.SALE ? "receivables" : "payables") + ".xlsx");
    }
}
