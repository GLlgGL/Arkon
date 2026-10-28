package com.fms.web;

import com.fms.domain.AccAccount;
import com.fms.domain.OrderType;
import com.fms.service.AccountingService;
import com.fms.service.ExcelService;
import com.fms.service.OrderException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Accounting (kontabilitet). Optional module; access per user (area ACCOUNTING). */
@Controller
public class AccountingController {

    private final AccountingService acc;
    private final ExcelService excel;
    private final CurrentUser currentUser;

    public AccountingController(AccountingService acc, ExcelService excel, CurrentUser currentUser) {
        this.acc = acc;
        this.excel = excel;
        this.currentUser = currentUser;
    }

    private static LocalDate from(LocalDate f) { return f == null ? LocalDate.now().withDayOfMonth(1) : f; }
    private static LocalDate to(LocalDate t) { return t == null ? LocalDate.now() : t; }

    private void period(Model model, LocalDate f, LocalDate t) {
        model.addAttribute("from", f);
        model.addAttribute("to", t);
    }

    // ----------------------------------------------------------------- journal

    @GetMapping("/accounting")
    public String journal(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(required = false) String source, Model model) {
        LocalDate f = from(from), t = to(to);
        var list = acc.journal(f, t, source);
        model.addAttribute("entries", list);
        model.addAttribute("total", list.stream().map(com.fms.domain.AccEntry::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("source", source);
        model.addAttribute("sources", List.of("MANUAL", "SALE", "PURCHASE", "PAYMENT", "CASH", "PAYROLL", "DEPRECIATION", "STOCK", "TAX", "OPENING"));
        period(model, f, t);
        return "accounting/journal";
    }

    @PostMapping("/accounting/sync")
    public String sync(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Principal principal, RedirectAttributes redirect) {
        try {
            int n = acc.sync(from, to.isBefore(from) ? from : to, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Automatic entries updated: " + n + ".");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting?from=" + from + "&to=" + to;
    }

    @GetMapping({"/accounting/entry/new", "/accounting/entry/{id:\\d+}"})
    public String entry(@PathVariable(required = false) Long id, Model model) {
        model.addAttribute("entry", id == null ? null : acc.entry(id));
        model.addAttribute("accounts", acc.accounts().stream().filter(AccAccount::isActive).toList());
        model.addAttribute("today", LocalDate.now());
        return "accounting/entry";
    }

    @PostMapping("/accounting/entry")
    public String saveEntry(@RequestParam(required = false) Long id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) String description,
                            @RequestParam(name = "acct", required = false) List<String> acct,
                            @RequestParam(name = "dr", required = false) List<String> dr,
                            @RequestParam(name = "cr", required = false) List<String> cr,
                            @RequestParam(name = "note", required = false) List<String> note,
                            Principal principal, RedirectAttributes redirect) {
        List<AccountingService.ManualLine> ls = new ArrayList<>();
        if (acct != null) {
            for (int i = 0; i < acct.size(); i++) {
                Long a = acct.get(i) == null || acct.get(i).isBlank() ? null : Long.valueOf(acct.get(i));
                ls.add(new AccountingService.ManualLine(a, num(dr, i), num(cr, i), note != null && i < note.size() ? note.get(i) : null));
            }
        }
        try {
            var e = acc.saveManual(id, date, description, ls, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Journal entry saved.");
            return "redirect:/accounting?from=" + e.getEntryDate().withDayOfMonth(1) + "&to=" + e.getEntryDate().withDayOfMonth(e.getEntryDate().lengthOfMonth());
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return id == null ? "redirect:/accounting/entry/new" : "redirect:/accounting/entry/" + id;
        }
    }

    @PostMapping("/accounting/entry/{id:\\d+}/delete")
    public String deleteEntry(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            acc.deleteManual(id);
            redirect.addFlashAttribute("message", "Journal entry deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting";
    }

    private static BigDecimal num(List<String> l, int i) {
        try {
            return l == null || i >= l.size() || l.get(i) == null || l.get(i).isBlank() ? BigDecimal.ZERO : new BigDecimal(l.get(i).trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // ---------------------------------------------------------------- reports

    @GetMapping("/accounting/ledger")
    public String ledger(@RequestParam(required = false) Long account,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        LocalDate f = from(from), t = to(to);
        var all = acc.accounts();
        model.addAttribute("accounts", all);
        boolean everything = account == null || account == 0;        // "All accounts" (the default)
        model.addAttribute("allAccounts", everything);
        model.addAttribute("account", everything ? 0L : account);
        model.addAttribute("ledgers", everything ? acc.ledgerAll(f, t) : java.util.List.of(acc.ledger(account, f, t)));
        period(model, f, t);
        return "accounting/ledger";
    }

    @GetMapping("/accounting/trial")
    public String trial(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        LocalDate f = from(from), t = to(to);
        model.addAttribute("trial", acc.trial(f, t));
        period(model, f, t);
        return "accounting/trial";
    }

    @GetMapping("/accounting/trial/export")
    public ResponseEntity<byte[]> trialExport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var tr = acc.trial(from(from), to(to));
        List<Object[]> rows = tr.rows().stream().map(r -> new Object[] {r.account().getCode(), r.account().getName(),
                r.opening().signum() > 0 ? r.opening() : null, r.opening().signum() < 0 ? r.opening().negate() : null,
                r.debit(), r.credit(), r.closing().signum() > 0 ? r.closing() : null, r.closing().signum() < 0 ? r.closing().negate() : null}).toList();
        return OrderController.xlsx(excel.table("Trial balance", new String[] {"Code", "Account", "Opening Dr", "Opening Cr",
                "Debit", "Credit", "Closing Dr", "Closing Cr"}, rows), "trial-balance.xlsx");
    }

    @GetMapping("/accounting/statements")
    public String statementsRedirect() {
        return "redirect:/accounting/official";        // one set of statements: the official format
    }

    public String statements(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        LocalDate f = from == null ? LocalDate.now().withDayOfYear(1) : from, t = to(to);
        model.addAttribute("st", acc.statements(f, t));
        period(model, f, t);
        return "accounting/statements";
    }

    @GetMapping("/accounting/vat")
    public String vat(@RequestParam(required = false) String type, @RequestParam(required = false) String month, Model model) {
        if (type == null) return "redirect:/accounting/vat?type=SALE" + (month == null ? "" : "&month=" + month);   // so the menu knows which book
        model.addAttribute("book", acc.vatBook("PURCHASE".equals(type) ? OrderType.PURCHASE : OrderType.SALE, ym(month)));
        model.addAttribute("type", "PURCHASE".equals(type) ? "PURCHASE" : "SALE");
        model.addAttribute("month", ym(month).toString());
        return "accounting/vat";
    }

    @GetMapping("/accounting/vat/export")
    public ResponseEntity<byte[]> vatExport(@RequestParam(defaultValue = "SALE") String type, @RequestParam(required = false) String month) {
        boolean sale = !"PURCHASE".equals(type);
        var b = acc.vatBook(sale ? OrderType.SALE : OrderType.PURCHASE, ym(month));
        List<Object[]> rows = b.rows().stream().map(r -> new Object[] {String.valueOf(r.date()), r.number(), r.party(), r.tin(),
                r.noVat(), r.vat(), r.total()}).toList();
        return OrderController.xlsx(excel.table(sale ? "Sales book" : "Purchase book", new String[] {"Date", "Number",
                sale ? "Client" : "Supplier", "NIPT", "Value without VAT", "VAT", "Total"}, rows),
                (sale ? "sales-book-" : "purchase-book-") + b.month() + ".xlsx");
    }

    private static YearMonth ym(String m) {
        try {
            return m == null || m.isBlank() ? YearMonth.now() : YearMonth.parse(m);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    // --------------------------------------------------------------- accounts

    @GetMapping("/accounting/accounts")
    public String accounts(@RequestParam(required = false) Long edit, @RequestParam(defaultValue = "false") boolean all, Model model) {
        var list = acc.accounts();
        model.addAttribute("accounts", all ? list : list.stream().filter(AccAccount::isActive).toList());   // the official chart adds many inactive accounts
        model.addAttribute("showAll", all);
        model.addAttribute("inactiveCount", list.stream().filter(a -> !a.isActive()).count());
        model.addAttribute("editing", edit == null ? null : acc.account(edit));
        model.addAttribute("types", AccAccount.Type.values());
        return "accounting/accounts";
    }

    @PostMapping("/accounting/accounts")
    public String saveAccount(@RequestParam(required = false) Long id, @RequestParam String code, @RequestParam String name,
                              @RequestParam(required = false) AccAccount.Type type, @RequestParam(defaultValue = "false") boolean active,
                              @RequestParam(required = false) String stmtLine, RedirectAttributes redirect) {
        try {
            var saved = acc.saveAccount(id, code, name, type, active);
            acc.setStatementLine(saved.getId(), stmtLine);
            redirect.addFlashAttribute("message", "Account saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/accounts";
    }

    @PostMapping("/accounting/accounts/{id:\\d+}/delete")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            acc.deleteAccount(id);
            redirect.addFlashAttribute("message", "Account deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/accounts";
    }
}
