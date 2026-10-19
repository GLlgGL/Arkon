package com.fms.web;

import com.fms.service.ExcelService;
import com.fms.service.ExcelService.SheetData;
import com.fms.service.Period;
import com.fms.service.StatisticsService;
import com.fms.service.StatisticsService.Series;
import com.fms.service.StatisticsService.Stats;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Statistics dashboard (was ListOfStatistics). Administrator-only. */
@Controller
public class StatisticsController {

    private final StatisticsService statistics;
    private final ExcelService excel;

    public StatisticsController(StatisticsService statistics, ExcelService excel) {
        this.statistics = statistics;
        this.excel = excel;
    }

    @GetMapping("/statistics")
    public String page(@RequestParam(defaultValue = "year") String period,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        Period p = Period.of(period, from, to);
        model.addAttribute("selected", p);
        model.addAttribute("stats", statistics.build(p));
        return "statistics";
    }

    /** All statistics in one workbook, one sheet per chart (the original exported the visible chart). */
    @GetMapping("/statistics/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "year") String period,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Period p = Period.of(period, from, to);
        Stats s = statistics.build(p);
        var k = s.kpis();

        List<SheetData> sheets = new ArrayList<>();
        sheets.add(new SheetData("Summary", new String[] {"Figure", "Value"}, List.of(
                new Object[] {"Period", p.from() + " to " + p.to()},
                new Object[] {"Sales (with VAT)", k.salesWithVat()},
                new Object[] {"Sales (no VAT)", k.salesNoVat()},
                new Object[] {"Purchases (with VAT)", k.purchasesWithVat()},
                new Object[] {"Cost of goods sold (average purchase cost)", k.costOfGoodsSold()},
                new Object[] {"Estimated gross profit", k.estimatedProfit()},
                new Object[] {"Gross margin %", k.marginPercent()},
                new Object[] {"Sales orders", k.saleOrders()},
                new Object[] {"Average sale (with VAT)", k.averageSale()},
                new Object[] {"Items sold", k.itemsSold()},
                new Object[] {"Offers made", k.offerOrders()},
                new Object[] {"Offers turned into sales", k.offersConverted()},
                new Object[] {"Offer conversion %", k.conversionPercent()})));

        List<Object[]> trend = new ArrayList<>();
        for (int i = 0; i < s.trend().labels().size(); i++) {
            trend.add(new Object[] {s.trend().labels().get(i), s.trend().sales().get(i),
                    s.trend().purchases().get(i), s.trend().offers().get(i)});
        }
        sheets.add(new SheetData("Trend", new String[] {s.trend().unit().equals("day") ? "Day" : "Month",
                "Sales", "Purchases", "Offers"}, trend));
        sheets.add(series("Clients", "Client", "Bought (with VAT)", s.topClients()));
        sheets.add(series("Products by units", "Product", "Units sold", s.topProductsByUnits()));
        sheets.add(series("Products by revenue", "Product", "Revenue (with VAT)", s.topProductsByRevenue()));
        sheets.add(series("Sellers", "User", "Sold (with VAT)", s.sellers()));
        sheets.add(series("Product classes", "Class", "Revenue (with VAT)", s.byClass()));

        return OrderController.xlsx(excel.workbook(sheets), "statistics-" + p.from() + "-to-" + p.to() + ".xlsx");
    }

    private static SheetData series(String sheet, String labelHeader, String valueHeader, Series s) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < s.labels().size(); i++) {
            rows.add(new Object[] {s.labels().get(i), s.values().get(i)});
        }
        return new SheetData(sheet, new String[] {labelHeader, valueHeader}, rows);
    }
}
