package com.fms.web;

import com.fms.domain.MoneyCategory;
import com.fms.service.BankImportService;
import com.fms.service.BankImportService.Csv;
import com.fms.service.BankImportService.Mapping;
import com.fms.service.CashService;
import com.fms.service.OrderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cash & Bank → Import statement: a bank's CSV file, matched and reconciled line by line. */
@Controller
public class BankImportController {

    private final BankImportService bank;
    private final CashService cash;
    private final CurrentUser currentUser;

    public BankImportController(BankImportService bank, CashService cash, CurrentUser currentUser) {
        this.bank = bank; this.cash = cash; this.currentUser = currentUser;
    }

    @GetMapping("/cash/import")
    @SuppressWarnings("unchecked")
    public String page(HttpSession s, Model model) {
        model.addAttribute("accounts", cash.activeAccounts());
        Object lines = s.getAttribute("bankLines");
        if (lines != null) {
            model.addAttribute("lines", lines);
            model.addAttribute("csv", s.getAttribute("bankCsv"));
            model.addAttribute("mapping", s.getAttribute("bankMapping"));
            model.addAttribute("accountId", s.getAttribute("bankAccount"));
            model.addAttribute("fileName", s.getAttribute("bankFile"));
            model.addAttribute("catsIn", Arrays.stream(MoneyCategory.values()).filter(c -> c.isManual() && c.isIn()).toList());
            model.addAttribute("catsOut", Arrays.stream(MoneyCategory.values()).filter(c -> c.isManual() && !c.isIn()).toList());
        }
        return "cash/import";
    }

    @PostMapping("/cash/import/upload")
    public String upload(@RequestParam Long accountId, @RequestParam("file") MultipartFile file, HttpSession s, RedirectAttributes redirect) {
        try {
            if (file == null || file.isEmpty()) throw new OrderException("Choose the bank's CSV file.");
            Csv csv = BankImportService.parse(file.getBytes());
            Mapping m = BankImportService.guess(csv.header());
            s.setAttribute("bankCsv", csv);
            s.setAttribute("bankAccount", accountId);
            s.setAttribute("bankFile", file.getOriginalFilename());
            read(s, m);
        } catch (OrderException | IOException e) {
            clear(s);
            redirect.addFlashAttribute("error", e instanceof OrderException ? e.getMessage() : "The file could not be read.");
        }
        return "redirect:/cash/import";
    }

    /** Read the file again with other columns. */
    @PostMapping("/cash/import/columns")
    public String columns(@RequestParam int date, @RequestParam int description, @RequestParam int amount,
                          @RequestParam int in, @RequestParam int out, HttpSession s) {
        if (s.getAttribute("bankCsv") != null) read(s, new Mapping(date, description, amount, in, out));
        return "redirect:/cash/import";
    }

    private void read(HttpSession s, Mapping m) {
        Csv csv = (Csv) s.getAttribute("bankCsv");
        Long acc = (Long) s.getAttribute("bankAccount");
        s.setAttribute("bankMapping", m);
        s.setAttribute("bankLines", new ArrayList<>(bank.suggest(acc, BankImportService.lines(csv, m, acc))));
    }

    @PostMapping("/cash/import/apply")
    @SuppressWarnings("unchecked")
    public String apply(HttpServletRequest req, HttpSession s, Principal principal, RedirectAttributes redirect) {
        var lines = (List<BankImportService.Line>) s.getAttribute("bankLines");
        Long acc = (Long) s.getAttribute("bankAccount");
        if (lines == null || acc == null) {
            redirect.addFlashAttribute("error", "Upload the file again.");
            return "redirect:/cash/import";
        }
        Map<Integer, BankImportService.Decision> d = new HashMap<>();
        for (var l : lines) {
            String action = req.getParameter("action_" + l.index());
            String cat = req.getParameter("cat_" + l.index());
            MoneyCategory c = null;
            try { if (cat != null && !cat.isBlank()) c = MoneyCategory.valueOf(cat); } catch (IllegalArgumentException ignored) { }
            if (action != null) d.put(l.index(), new BankImportService.Decision(l.index(), action, c));
        }
        try {
            var r = bank.apply(acc, lines, d, currentUser.of(principal));
            clear(s);
            redirect.addFlashAttribute("message", "Statement imported: " + r.matched() + " matched, " + r.paid() + " invoices paid, "
                    + r.created() + " added, " + r.ignored() + " skipped.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash/import";
    }

    @PostMapping("/cash/import/cancel")
    public String cancel(HttpSession s) {
        clear(s);
        return "redirect:/cash/import";
    }

    private static void clear(HttpSession s) {
        for (String a : new String[] {"bankCsv", "bankMapping", "bankLines", "bankAccount", "bankFile"}) s.removeAttribute(a);
    }
}
