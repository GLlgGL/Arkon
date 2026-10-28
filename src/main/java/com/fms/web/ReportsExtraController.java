package com.fms.web;

import com.fms.domain.OrderType;
import com.fms.service.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Aged receivables and payables; the VAT books in the tax office's format; the statements in Excel. */
@Controller
public class ReportsExtraController {

    @org.springframework.beans.factory.annotation.Autowired
    private RecordService records;
    @org.springframework.beans.factory.annotation.Autowired
    private CurrentUser currentUser;

    private final AgingService aging;
    private final AccountingService accounting;
    private final VatBookExcel vatBooks;
    private final ExcelService excel;
    private final PartyService parties;
    private final MessageSource messages;

    public ReportsExtraController(AgingService aging, AccountingService accounting, VatBookExcel vatBooks, ExcelService excel,
                                  PartyService parties, MessageSource messages) {
        this.aging = aging; this.accounting = accounting; this.vatBooks = vatBooks; this.excel = excel;
        this.parties = parties; this.messages = messages;
    }

    private String t(String k) { return messages.getMessage(k, null, k, LocaleContextHolder.getLocale()); }

    @GetMapping("/debts/aging/{side:sales|purchases}")
    public String agingPage(@PathVariable String side, Model model, java.security.Principal principal) {
        if (side != null) return "redirect:/debts/" + side + "?view=age";     // now a view of the Unpaid page
        OrderType at = "sales".equals(side) ? OrderType.SALE : OrderType.PURCHASE;
        var vis = records.visibility(currentUser.of(principal));
        model.addAttribute("a", aging.aging(at, r -> vis.sees(at, r.code(), r.createdBy())));
        model.addAttribute("side", side);
        return "debts/aging";
    }

    @GetMapping("/debts/aging/{side:sales|purchases}/export")
    public ResponseEntity<byte[]> agingExport(@PathVariable String side, java.security.Principal principal) {
        OrderType at = "sales".equals(side) ? OrderType.SALE : OrderType.PURCHASE;
        var vis = records.visibility(currentUser.of(principal));
        var a = aging.aging(at, r -> vis.sees(at, r.code(), r.createdBy()));
        List<Object[]> rows = new ArrayList<>();
        a.rows().forEach(r -> rows.add(new Object[] {r.party(), r.invoices(), r.b()[0], r.b()[1], r.b()[2], r.b()[3], r.b()[4], r.total()}));
        rows.add(new Object[] {t("t.total"), null, a.totals()[0], a.totals()[1], a.totals()[2], a.totals()[3], a.totals()[4], a.total()});
        String[] h = {"sales".equals(side) ? t("party.Client") : t("pdf.supplier"), t("ag.invoices"), t("ag.current"), "1–30", "31–60", "61–90", t("ag.over90"), t("t.total")};
        return OrderController.xlsx(excel.table("Aging", h, rows), "aging-" + side + ".xlsx");
    }

    /** The VAT book in the tax office's Excel format (Libri i Shitjeve / i Blerjeve, version 4). */
    @GetMapping("/accounting/vat/tatime")
    public ResponseEntity<byte[]> vatBookTatime(@RequestParam(defaultValue = "SALE") String type, @RequestParam(required = false) String month) {
        YearMonth ym;
        try { ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month); } catch (Exception e) { ym = YearMonth.now(); }
        boolean sale = !"PURCHASE".equals(type);
        var book = accounting.vatBook(sale ? OrderType.SALE : OrderType.PURCHASE, ym);
        String company = "", tin = "";
        var names = parties.providerNames(OrderType.SALE);
        if (!names.isEmpty()) {
            company = names.get(0);
            try { var p = parties.provider(OrderType.SALE, company); tin = p == null || p.tin() == null ? "" : p.tin(); } catch (RuntimeException ignored) { }
        }
        byte[] file = sale ? vatBooks.sales(company, tin, ym.getYear(), ym.getMonthValue(), book.rows())
                           : vatBooks.purchases(company, tin, ym.getYear(), ym.getMonthValue(), book.rows());
        return OrderController.xlsx(file, (sale ? "Libri-i-Shitjeve-" : "Libri-i-Blerjeve-") + ym + ".xlsx");
    }

    /** Income statement and balance sheet in Excel. */
    @GetMapping("/accounting/statements/export")
    public ResponseEntity<byte[]> statementsExport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().withDayOfYear(1) : from, tt = to == null ? LocalDate.now() : to;
        var s = accounting.statements(f, tt);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {t("acc.incomeStatement") + " " + f + " – " + tt, null, null});
        rows.add(new Object[] {t("acc.income"), null, null});
        s.income().forEach(r -> rows.add(new Object[] {r.account().getCode(), r.account().getName(), r.amount()}));
        rows.add(new Object[] {null, t("acc.totalIncome"), s.totalIncome()});
        rows.add(new Object[] {t("acc.expenses"), null, null});
        s.expenses().forEach(r -> rows.add(new Object[] {r.account().getCode(), r.account().getName(), r.amount()}));
        rows.add(new Object[] {null, t("acc.totalExpenses"), s.totalExpenses()});
        rows.add(new Object[] {null, t("acc.result"), s.result()});
        rows.add(new Object[] {null, null, null});
        rows.add(new Object[] {t("acc.tab.statements") + " – " + tt, null, null});
        rows.add(new Object[] {t("acc.assets"), null, null});
        s.assets().forEach(r -> rows.add(new Object[] {r.account().getCode(), r.account().getName(), r.amount()}));
        rows.add(new Object[] {null, t("acc.totalAssets"), s.totalAssets()});
        rows.add(new Object[] {t("acc.liabilities"), null, null});
        s.liabilities().forEach(r -> rows.add(new Object[] {r.account().getCode(), r.account().getName(), r.amount()}));
        rows.add(new Object[] {t("acc.equity"), null, null});
        s.equity().forEach(r -> rows.add(new Object[] {r.account().getCode(), r.account().getName(), r.amount()}));
        rows.add(new Object[] {null, t("acc.resultToDate"), s.resultToDate()});
        rows.add(new Object[] {null, t("acc.totalLE"), s.getLiabilitiesAndEquity()});
        return OrderController.xlsx(excel.table("Statements", new String[] {t("acc.account"), t("t.name"), t("t.amount")}, rows),
                "statements-" + f + "-" + tt + ".xlsx");
    }

    @org.springframework.beans.factory.annotation.Autowired
    private OfficialStatements officialStatements;
    @org.springframework.beans.factory.annotation.Autowired
    private OfficialStatementsExcel officialExcel;
    @org.springframework.beans.factory.annotation.Autowired
    private ConfigService configService;

    /** The official financial statements of a year (Law 25/2018), with the year before. */
    @GetMapping("/accounting/official")
    public String official(@RequestParam(required = false) Integer year, Model model) {
        int y = year == null ? LocalDate.now().getYear() - (LocalDate.now().getMonthValue() <= 3 ? 1 : 0) : year;
        model.addAttribute("rep", officialStatements.report(y));
        model.addAttribute("year", y);
        return "accounting/official";
    }

    @GetMapping("/accounting/official/export")
    public ResponseEntity<byte[]> officialExport(@RequestParam int year,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate approved) {
        String company = "", tin = "", address = "";
        var names = parties.providerNames(OrderType.SALE);
        if (!names.isEmpty()) {
            company = names.get(0);
            try {
                var p = parties.provider(OrderType.SALE, company);
                if (p != null) { tin = p.tin() == null ? "" : p.tin(); address = p.address() == null ? "" : p.address(); }
            } catch (RuntimeException ignored) { }
        }
        return OrderController.xlsx(officialExcel.write(year, configService.get(), company, tin, address, approved),
                "Pasqyrat-financiare-" + year + ".xlsx");
    }

    @org.springframework.beans.factory.annotation.Autowired
    private CommissionService commissions;

    @GetMapping("/reports/commissions")
    public String commissionsPage(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(defaultValue = "false") boolean paidOnly, Model model) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : from, t = to == null ? LocalDate.now() : to;
        model.addAttribute("rep", commissions.report(f, t, paidOnly));
        model.addAttribute("from", f);
        model.addAttribute("to", t);
        model.addAttribute("paidOnly", paidOnly);
        return "reports/commissions";
    }

    @GetMapping("/reports/commissions/export")
    public ResponseEntity<byte[]> commissionsExport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(defaultValue = "false") boolean paidOnly) {
        var r = commissions.report(from, to, paidOnly);
        List<Object[]> rows = new ArrayList<>();
        r.rows().forEach(x -> rows.add(new Object[] {x.user(), x.sales(), x.noVat(), x.cost(), x.profit(), "PROFIT".equals(x.base()) ? t("com.base.PROFIT") : t("com.base.SALES"), x.pct(), x.commission()}));
        rows.add(new Object[] {t("t.total"), null, r.noVat(), null, r.profit(), null, null, r.commission()});
        return OrderController.xlsx(excel.table("Commissions", new String[] {t("com.user"), t("com.sales"), t("com.noVat"), t("com.cost"), t("com.profit"),
                t("com.base"), "%", t("com.commission")}, rows), "commissions-" + from + "-" + to + ".xlsx");
    }
}
