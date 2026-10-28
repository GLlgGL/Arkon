package com.fms.web;

import com.fms.domain.MoneyCategory;
import com.fms.service.CashService;
import com.fms.service.OrderException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cash & Bank → Register: many money-in / money-out lines typed like a register, saved together. */
@Controller
public class BankRegisterController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.ConfigService config;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.CostCenterService costCenters;

    private final CashService cash;
    private final CurrentUser currentUser;

    public BankRegisterController(CashService cash, CurrentUser currentUser) {
        this.cash = cash;
        this.currentUser = currentUser;
    }

    @GetMapping("/cash/register")
    public String page(@RequestParam(required = false) Long account, @RequestParam(required = false) String dir, Model model) {
        if (!"IN".equals(dir) && !"OUT".equals(dir)) {          // the menu has Money in / Money out
            return "redirect:/cash/register?dir=OUT" + (account == null ? "" : "&account=" + account);
        }
        String d = dir;
        model.addAttribute("dir", d);
        if (config.get().isModCostCenters()) model.addAttribute("costCenters", costCenters.active());
        var accounts = cash.activeAccounts();
        model.addAttribute("accounts", accounts);
        Long acc = account != null ? account : accounts.isEmpty() ? null : accounts.get(0).getId();
        model.addAttribute("accountId", acc);
        if (acc != null) {
            var a = accounts.stream().filter(x -> x.getId().equals(acc)).findFirst().orElse(null);
            model.addAttribute("balance", a == null ? BigDecimal.ZERO : cash.balance(a, LocalDate.now()));
        }
        model.addAttribute("catsIn", Arrays.stream(MoneyCategory.values()).filter(c -> c.isManual() && c.isIn()).toList());
        model.addAttribute("catsOut", Arrays.stream(MoneyCategory.values()).filter(c -> c.isManual() && !c.isIn()).toList());
        model.addAttribute("today", LocalDate.now());
        return "cash/register";
    }

    /** Rows arrive as date[], desc[], cat[], in[], out[]; empty rows are skipped. All or nothing. */
    @PostMapping("/cash/register")
    @Transactional
    public String save(@RequestParam Long accountId, @RequestParam(required = false) String dir, HttpServletRequest req,
                       Principal principal, RedirectAttributes redirect) {
        String[] dates = req.getParameterValues("date"), descs = req.getParameterValues("desc"), cats = req.getParameterValues("cat"),
                ins = req.getParameterValues("in"), outs = req.getParameterValues("out"),
                refs = req.getParameterValues("ref"), ccs = req.getParameterValues("cc");
        String back = "redirect:/cash/register?account=" + accountId + ("IN".equals(dir) || "OUT".equals(dir) ? "&dir=" + dir : "");
        if ("IN".equals(dir) || "OUT".equals(dir)) {            // one direction: the rows have a single "amount"
            String[] amt = req.getParameterValues("amount");
            if ("IN".equals(dir)) ins = amt; else outs = amt;
        }
        if (dates == null) return back;
        var user = currentUser.of(principal);
        int saved = 0;
        List<String> errors = new ArrayList<>();
        try {
            for (int i = 0; i < dates.length; i++) {
                BigDecimal in = num(at(ins, i)), out = num(at(outs, i));
                String desc = at(descs, i);
                if ((in == null || in.signum() == 0) && (out == null || out.signum() == 0)) continue;     // empty row
                if (in != null && in.signum() != 0 && out != null && out.signum() != 0) { errors.add((i + 1) + ""); continue; }
                LocalDate d;
                try { d = LocalDate.parse(at(dates, i)); } catch (Exception e) { errors.add((i + 1) + ""); continue; }
                boolean isIn = in != null && in.signum() != 0;
                MoneyCategory cat = category(at(cats, i), isIn);
                if (cat == null) { errors.add((i + 1) + ""); continue; }
                String ref = at(refs, i);
                Long cc = null;
                try { String c = at(ccs, i); if (c != null && !c.isBlank()) cc = Long.valueOf(c); } catch (NumberFormatException ignored) { }
                cash.addEntry(accountId, d, cat, (isIn ? in : out).abs(), desc == null ? "" : desc, ref == null || ref.isBlank() ? null : ref.trim(), cc, user);
                saved++;
            }
            if (!errors.isEmpty()) throw new OrderException("Check rows " + String.join(", ", errors) + ": a date, one amount (in or out) and a category are needed. Nothing was saved.");
            redirect.addFlashAttribute("message", "Register saved: " + saved + " lines.");
        } catch (OrderException e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back;
    }

    private static String at(String[] a, int i) { return a != null && i < a.length ? a[i] : null; }

    private static BigDecimal num(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.trim().replace(" ", "").replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }

    private static MoneyCategory category(String name, boolean in) {
        try {
            MoneyCategory c = MoneyCategory.valueOf(name);
            if (c.isManual() && c.isIn() == in) return c;
        } catch (Exception ignored) { }
        return null;
    }
}
