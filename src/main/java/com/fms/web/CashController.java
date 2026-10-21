package com.fms.web;

import com.fms.domain.CashAccount;
import com.fms.domain.MoneyCategory;
import com.fms.service.CashService;
import com.fms.service.ExcelService;
import com.fms.service.OrderException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cash & Bank (Arka & Banka). Administrator-only. */
@Controller
public class CashController {

    private final CashService cash;
    private final ExcelService excel;
    private final MessageSource messages;
    private final CurrentUser currentUser;
    private final com.fms.service.CostCenterService costCenters;
    private final com.fms.service.ConfigService config;

    public CashController(CashService cash, ExcelService excel, MessageSource messages, CurrentUser currentUser,
                          com.fms.service.CostCenterService costCenters, com.fms.service.ConfigService config) {
        this.costCenters = costCenters;
        this.config = config;
        this.cash = cash;
        this.excel = excel;
        this.messages = messages;
        this.currentUser = currentUser;
    }

    private void common(Model model) {
        model.addAttribute("accounts", cash.activeAccounts());
        model.addAttribute("today", LocalDate.now());
        if (config.get().isModCostCenters()) {
            model.addAttribute("costCenters", costCenters.active());
        }
    }

    // --------------------------------------------------------------- analytics

    /**
     * Analytics: the chosen month compared with the previous month or the same
     * month last year; the year so far compared with the same period last
     * year; 12-month charts.
     */
    @GetMapping("/cash/analytics")
    public String analytics(@RequestParam(required = false) String month,
                            @RequestParam(defaultValue = "prev") String base, Model model) {
        java.time.YearMonth ym;
        try {
            ym = month == null || month.isBlank() ? java.time.YearMonth.now() : java.time.YearMonth.parse(month);
        } catch (java.time.format.DateTimeParseException e) {
            ym = java.time.YearMonth.now();
        }
        LocalDate today = LocalDate.now();
        boolean current = ym.equals(java.time.YearMonth.from(today));
        LocalDate fromA = ym.atDay(1);
        LocalDate toA = current ? today : ym.atEndOfMonth();          // current month: so far
        boolean lastYear = "lastYear".equals(base);
        java.time.YearMonth ymB = lastYear ? ym.minusYears(1) : ym.minusMonths(1);
        LocalDate fromB = ymB.atDay(1);
        LocalDate toB = current ? fromB.plusDays(Math.min(toA.getDayOfMonth(), ymB.lengthOfMonth()) - 1) : ymB.atEndOfMonth();
        model.addAttribute("monthCmp", cash.compare(fromA, toA, fromB, toB));
        // Year so far vs the same days last year
        LocalDate yEnd = current ? today : ym.atEndOfMonth();
        LocalDate yStart = yEnd.withDayOfYear(1);
        model.addAttribute("yearCmp", cash.compare(yStart, yEnd, yStart.minusYears(1), yEnd.minusYears(1)));
        model.addAttribute("monthly", cash.monthly(toA));
        model.addAttribute("month", ym.toString());
        model.addAttribute("base", lastYear ? "lastYear" : "prev");
        return "cash/analytics";
    }

    // ---------------------------------------------------------------- overview

    @GetMapping("/cash")
    public String overview(Model model) {
        var balances = cash.balances();
        model.addAttribute("balances", balances);
        model.addAttribute("cashTotal", sum(balances.stream().filter(b -> !b.account().isBank()).map(CashService.AccountBalance::base).toList()));
        model.addAttribute("bankTotal", sum(balances.stream().filter(b -> b.account().isBank()).map(CashService.AccountBalance::base).toList()));
        model.addAttribute("total", sum(balances.stream().map(CashService.AccountBalance::base).toList()));
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        model.addAttribute("month", cash.flow(first, LocalDate.now()));
        model.addAttribute("recent", cash.recent());
        return "cash/overview";
    }

    // --------------------------------------------------------------- movements

    @GetMapping("/cash/movements")
    public String movements(@RequestParam(required = false) Long account,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) MoneyCategory category,
                            @RequestParam(required = false) String q, Model model) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        model.addAttribute("ledger", cash.ledger(account, f, t, category, q));
        model.addAttribute("allAccounts", cash.allAccounts());
        model.addAttribute("categories", MoneyCategory.values());
        model.addAttribute("account", account);
        model.addAttribute("from", f);
        model.addAttribute("to", t);
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        return "cash/movements";
    }

    @GetMapping("/cash/movements/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long account,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) MoneyCategory category,
                                         @RequestParam(required = false) String q) {
        var loc = LocaleContextHolder.getLocale();
        var ledger = cash.ledger(account, from == null ? LocalDate.now().withDayOfMonth(1) : from, to == null ? LocalDate.now() : to, category, q);
        List<Object[]> rows = ledger.rows().stream().map(r -> {
            var m = r.movement();
            return new Object[] {String.valueOf(m.getMovedOn()), m.getAccount().getName(),
                    messages.getMessage("cash.cat." + m.getCategory().name(), null, m.getCategory().name(), loc),
                    m.getDescription(), m.getRefNo() != null ? m.getRefNo() : m.getOrderCode(),
                    m.isIn() ? m.getAmount() : null, m.isIn() ? null : m.getAmount(), ledger.singleAccount() ? r.balance() : null};
        }).toList();
        String[] heads = Arrays.stream(new String[] {"t.date", "cash.account", "cash.category", "cash.description", "cash.reference",
                "cash.in", "cash.out", "cash.balance"}).map(k -> messages.getMessage(k, null, k, loc)).toArray(String[]::new);
        return OrderController.xlsx(excel.table("Movements", heads, rows), "cash-movements.xlsx");
    }

    @PostMapping("/cash/movements/{id}/delete")
    public String deleteMovement(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            cash.deleteMovement(id);
            redirect.addFlashAttribute("message", "Movement deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash/movements";
    }

    // ------------------------------------------------------------ new entry

    @GetMapping("/cash/entry")
    public String entryForm(@RequestParam(defaultValue = "OUT") String dir, Model model) {
        common(model);
        boolean in = "IN".equals(dir);
        model.addAttribute("dirIn", in);
        model.addAttribute("categories", Arrays.stream(MoneyCategory.values()).filter(c -> c.isManual() && c.isIn() == in).toList());
        return "cash/entry";
    }

    @PostMapping("/cash/entry")
    public String entry(@RequestParam Long accountId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) MoneyCategory category, @RequestParam(required = false) BigDecimal amount,
                        @RequestParam(required = false) String description, @RequestParam(required = false) String refNo,
                        @RequestParam(required = false) Long costCenterId,
                        Principal principal, RedirectAttributes redirect) {
        try {
            var m = cash.addEntry(accountId, date, category, amount, description, refNo, costCenterId, currentUser.of(principal));
            redirect.addFlashAttribute("message", m.isIn() ? "Money in recorded." : "Money out recorded.");
            return "redirect:/cash";
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cash/entry?dir=" + (category != null && category.isIn() ? "IN" : "OUT");
        }
    }

    // -------------------------------------------------------------- transfer

    @GetMapping("/cash/transfer")
    public String transferForm(Model model) {
        common(model);
        return "cash/transfer";
    }

    @PostMapping("/cash/transfer")
    public String transfer(@RequestParam Long fromId, @RequestParam Long toId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) BigDecimal amount, @RequestParam(required = false) BigDecimal received,
                           @RequestParam(required = false) String note,
                           Principal principal, RedirectAttributes redirect) {
        try {
            cash.transfer(fromId, toId, date, amount, received, note, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Transfer recorded.");
            return "redirect:/cash";
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cash/transfer";
        }
    }

    // ------------------------------------------------------------- reconcile

    @GetMapping("/cash/reconcile")
    public String reconcileForm(@RequestParam(required = false) Long account, Model model) {
        common(model);
        var balances = cash.balances();
        model.addAttribute("balances", balances);
        model.addAttribute("selected", account);
        return "cash/reconcile";
    }

    @PostMapping("/cash/reconcile")
    public String reconcile(@RequestParam Long accountId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) BigDecimal counted, @RequestParam(required = false) String note,
                            Principal principal, RedirectAttributes redirect) {
        try {
            BigDecimal diff = cash.reconcile(accountId, date, counted, note, currentUser.of(principal));
            var loc = LocaleContextHolder.getLocale();
            redirect.addFlashAttribute("message", diff.signum() == 0
                    ? messages.getMessage("cash.reconcileOk", null, "Balance matches.", loc)
                    : messages.getMessage("cash.reconcileDiff", new Object[] {com.fms.service.Money.format(diff)}, "Difference recorded.", loc));
            return "redirect:/cash";
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cash/reconcile";
        }
    }

    // -------------------------------------------------------------- accounts

    @GetMapping("/cash/accounts")
    public String accounts(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("balances", cash.balances());
        model.addAttribute("editing", edit == null ? null : cash.account(edit));
        model.addAttribute("today", LocalDate.now());
        return "cash/accounts";
    }

    @PostMapping("/cash/accounts")
    public String saveAccount(@RequestParam(required = false) Long id, @RequestParam String name,
                              @RequestParam(defaultValue = "CASH") CashAccount.Kind kind,
                              @RequestParam(required = false) String bankName, @RequestParam(required = false) String iban,
                              @RequestParam(required = false) BigDecimal openingBalance,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openingDate,
                              @RequestParam(defaultValue = "false") boolean active,
                              @RequestParam(defaultValue = "false") boolean defaultAccount,
                              @RequestParam(required = false) String accountCurrency,
                              RedirectAttributes redirect) {
        try {
            cash.saveAccount(id, name, kind, bankName, iban, openingBalance, openingDate, active, defaultAccount,
                    accountCurrency, config.get().isModCurrencies());
            redirect.addFlashAttribute("message", "Account saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash/accounts";
    }

    @PostMapping("/cash/accounts/{id}/delete")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            cash.deleteAccount(id);
            redirect.addFlashAttribute("message", "Account deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash/accounts";
    }

    // ------------------------------------------------------------- cash flow

    @GetMapping("/cash/flow")
    public String flow(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        if (t.isBefore(f)) {
            t = f;
        }
        model.addAttribute("flow", cash.flow(f, t));
        model.addAttribute("from", f);
        model.addAttribute("to", t);
        return "cash/flow";
    }

    private static BigDecimal sum(List<BigDecimal> v) {
        return v.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
