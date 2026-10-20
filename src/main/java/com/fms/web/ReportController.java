package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.service.ExcelService;
import com.fms.service.Period;
import com.fms.service.ReportService;
import com.fms.service.WarehouseRow;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** PurchaseReports, SaleReports, ProductReports and WarehouseReport. Administrator-only. */
@Controller
public class ReportController {

    private final ReportService reports;
    private final ExcelService excel;
    private final CurrentUser currentUser;
    private final com.fms.service.PartyService parties;

    public ReportController(ReportService reports, ExcelService excel, CurrentUser currentUser,
                            com.fms.service.PartyService parties) {
        this.parties = parties;
        this.reports = reports;
        this.excel = excel;
        this.currentUser = currentUser;
    }

    /** Today / this month / this year side by side, plus a chosen period with its lines. */
    @GetMapping("/reports/{type:purchases|sales}")
    public String period(@PathVariable String type,
                         @RequestParam(defaultValue = "month") String period,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         Principal principal, Model model) {
        OrderType t = OrderType.fromPath(type);
        var user = currentUser.of(principal);
        Period selected = Period.of(period, from, to);
        model.addAttribute("type", t);
        model.addAttribute("today", reports.totals(t, user, Period.today()));
        model.addAttribute("month", reports.totals(t, user, Period.thisMonth()));
        model.addAttribute("year", reports.totals(t, user, Period.thisYear()));
        model.addAttribute("selected", selected);
        model.addAttribute("selectedTotals", reports.totals(t, user, selected));
        model.addAttribute("lines", reports.lines(t, user, selected));
        return "reports/period";
    }

    @GetMapping("/reports/{type:purchases|sales}/export")
    public ResponseEntity<byte[]> exportPeriod(@PathVariable String type,
                                               @RequestParam(defaultValue = "month") String period,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               Principal principal) {
        OrderType t = OrderType.fromPath(type);
        Period p = Period.of(period, from, to);
        byte[] file = excel.orderLines(t.getLabel() + " report", reports.lines(t, currentUser.of(principal), p));
        return OrderController.xlsx(file, t.getLabel() + "-report-" + p.from() + "-to-" + p.to() + ".xlsx");
    }

    @GetMapping("/reports/products")
    public String products(@RequestParam(required = false) String q, Principal principal, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("report", reports.productReport(q, currentUser.of(principal)));
        return "reports/products";
    }

    /**
     * Two views, as the original's radio buttons:
     *   mode=inventory  "Warehouse inventory": stock per product and its value
     *   mode=supply     "For supply": products whose remaining stock is below the
     *                   quantity limit or less than {@code margin} units above it
     */
    @GetMapping("/reports/warehouse")
    public String warehouse(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(defaultValue = "inventory") String mode,
                            @RequestParam(defaultValue = "3") int margin,
                            Model model) {
        Period p = warehousePeriod(from, to);
        boolean supply = "supply".equals(mode);
        List<WarehouseRow> rows = reports.warehouse(p);
        if (supply) {
            rows = rows.stream().filter(r -> r.needsSupply(margin)).toList();
        }
        model.addAttribute("period", p);
        if (supply) {
            model.addAttribute("poMerchants", parties.providerNames(com.fms.domain.OrderType.PURCHASE));
            model.addAttribute("poCompanies", parties.buyerNames(com.fms.domain.OrderType.PURCHASE));
        }
        model.addAttribute("mode", supply ? "supply" : "inventory");
        model.addAttribute("margin", margin);
        model.addAttribute("rows", rows);
        model.addAttribute("totalValue", rows.stream().map(WarehouseRow::value)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        return "reports/warehouse";
    }

    @GetMapping("/reports/warehouse/export")
    public ResponseEntity<byte[]> exportWarehouse(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "inventory") String mode,
                                                  @RequestParam(defaultValue = "3") int margin) {
        Period p = warehousePeriod(from, to);
        List<WarehouseRow> all = reports.warehouse(p);
        if ("supply".equals(mode)) {
            List<Object[]> rows = all.stream().filter(r -> r.needsSupply(margin))
                    .map(r -> new Object[] {r.nr(), r.product(), r.unit(), r.remaining(), r.quantityLimit()})
                    .toList();
            byte[] file = excel.table("For supply",
                    new String[] {"Nr", "Product", "Unit", "Remaining quantity", "Limit quantity"}, rows);
            return OrderController.xlsx(file, "for-supply-" + p.from() + "-to-" + p.to() + ".xlsx");
        }
        List<Object[]> rows = all.stream()
                .map(r -> new Object[] {r.nr(), r.product(), r.unit(), r.purchased(), r.sold(), r.adjusted(), r.remaining(), r.salePrice(), r.value()})
                .toList();
        byte[] file = excel.table("Warehouse",
                new String[] {"Nr", "Product", "Unit", "Purchased", "Sold", "Adjusted", "Remaining", "Sale price", "Value"}, rows);
        return OrderController.xlsx(file, "warehouse-" + p.from() + "-to-" + p.to() + ".xlsx");
    }

    private Period warehousePeriod(LocalDate from, LocalDate to) {
        return Period.custom(from == null ? reports.firstActivityDate() : from, to == null ? LocalDate.now() : to);
    }
}
