package com.fms.web;

import com.fms.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Payroll (listëpagesa). Administrator-only; optional module. */
@Controller
public class PayrollController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.ExcelService excel;

    private final PayrollService payroll;
    private final CashService cash;
    private final CostCenterService centers;
    private final ConfigService config;
    private final CurrentUser currentUser;

    public PayrollController(PayrollService payroll, CashService cash, CostCenterService centers, ConfigService config, CurrentUser currentUser) {
        this.payroll = payroll;
        this.cash = cash;
        this.centers = centers;
        this.config = config;
        this.currentUser = currentUser;
    }

    @GetMapping("/payroll")
    public String runs(Model model) {
        var list = payroll.runs();
        java.util.Map<Long, PayrollService.Totals> totals = new java.util.HashMap<>();
        list.forEach(r -> totals.put(r.getId(), payroll.totals(payroll.lines(r.getId()))));
        model.addAttribute("runs", list);
        model.addAttribute("totals", totals);
        model.addAttribute("nextMonth", java.time.YearMonth.now().toString());
        return "payroll/runs";
    }

    @PostMapping("/payroll")
    public String create(@RequestParam String period, Principal principal, RedirectAttributes redirect) {
        try {
            var r = payroll.createRun(period, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Payroll created.");
            return "redirect:/payroll/" + r.getId();
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/payroll";
        }
    }

    @GetMapping("/payroll/{id:\\d+}")
    public String run(@PathVariable Long id, Model model) {
        var lines = payroll.lines(id);
        model.addAttribute("run", payroll.run(id));
        model.addAttribute("lines", lines);
        model.addAttribute("t", payroll.totals(lines));
        if (config.get().isModCash()) {
            model.addAttribute("accounts", cash.activeAccounts());
        }
        model.addAttribute("today", LocalDate.now());
        return "payroll/run";
    }

    @PostMapping("/payroll/{id:\\d+}")
    public String update(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request, RedirectAttributes redirect) {
        java.util.Map<Long, BigDecimal> g = new java.util.HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("gross_") && v.length > 0 && !v[0].isBlank()) {
                try { g.put(Long.valueOf(k.substring(6)), new BigDecimal(v[0].trim())); } catch (NumberFormatException ignored) { }
            }
        });
        try {
            payroll.updateGross(id, g);
            redirect.addFlashAttribute("message", "Payroll recalculated.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payroll/" + id + "";
    }

    @PostMapping("/payroll/{id:\\d+}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            payroll.approve(id);
            redirect.addFlashAttribute("message", "Payroll approved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payroll/" + id + "";
    }

    @PostMapping("/payroll/{id:\\d+}/pay")
    public String pay(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) Long accountId,
                      Principal principal, RedirectAttributes redirect) {
        try {
            payroll.pay(id, date, accountId, currentUser.of(principal));
            redirect.addFlashAttribute("message", "Payroll marked as paid.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payroll/" + id + "";
    }

    @PostMapping("/payroll/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            payroll.deleteRun(id);
            redirect.addFlashAttribute("message", "Payroll deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payroll";
    }

    @GetMapping("/payroll/employees")
    public String employees(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("employees", payroll.employees());
        model.addAttribute("editing", edit == null ? null : payroll.employee(edit));
        if (config.get().isModCostCenters()) {
            model.addAttribute("costCenters", centers.active());
        }
        model.addAttribute("today", LocalDate.now());
        return "payroll/employees";
    }

    @PostMapping("/payroll/employees")
    public String saveEmployee(@RequestParam(required = false) Long id, @RequestParam String name,
                               @RequestParam(required = false) String personalNo, @RequestParam(required = false) String position,
                               @RequestParam(required = false) BigDecimal grossSalary, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               @RequestParam(required = false) String iban, @RequestParam(required = false) Long costCenterId,
                               @RequestParam(defaultValue = "false") boolean active,
                               @RequestParam(defaultValue = "001") String taxCode, @RequestParam(defaultValue = "false") boolean declared,
                               RedirectAttributes redirect) {
        try {
            payroll.saveEmployee(id, name, personalNo, position, grossSalary, startDate, endDate, iban, costCenterId, active, taxCode, declared);
            redirect.addFlashAttribute("message", "Employee saved.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payroll/employees";
    }

    @PostMapping("/payroll/employees/{id:\\d+}/delete")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            payroll.deleteEmployee(id);
            redirect.addFlashAttribute("message", "Employee deleted.");
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payroll/employees";
    }

    /** Gross salary for a wanted net (employee form). */
    @GetMapping("/payroll/gross-from-net")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> grossFromNet(@RequestParam BigDecimal net, @RequestParam(defaultValue = "001") String taxCode,
                                                      @RequestParam(defaultValue = "true") boolean declared) {
        BigDecimal g = payroll.grossFromNet(net, taxCode, declared);
        var l = new com.fms.domain.PayrollLine();
        l.setGross(g);
        l.setTaxCode(taxCode);
        l.setDeclared(declared);
        payroll.calculate(l);
        return java.util.Map.of("gross", g, "net", l.getNet(), "tax", l.getTax(), "empSocial", l.getEmpSocial(), "empHealth", l.getEmpHealth());
    }

    /** The run in the column layout of the official monthly list (Listëpagesa). */
    @GetMapping("/payroll/{id:\\d+}/listepagesa")
    public org.springframework.http.ResponseEntity<byte[]> listepagesa(@PathVariable Long id) {
        var run = payroll.run(id);
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        int nr = 1;
        BigDecimal[] tot = new BigDecimal[13];
        java.util.Arrays.fill(tot, BigDecimal.ZERO);
        for (var l : payroll.lines(id)) {
            String[] nm = l.getEmployeeName().trim().split("\\s+", 2);
            BigDecimal soc = Money.nz(l.getErSocial()).add(Money.nz(l.getEmpSocial()));
            BigDecimal health = Money.nz(l.getErHealth()).add(Money.nz(l.getEmpHealth()));
            BigDecimal[] v = {l.getGross(), l.getContribBase(), soc, l.getErSocial(), l.getEmpSocial(), BigDecimal.ZERO, health,
                    l.getGross(), BigDecimal.ZERO, l.getTaxAllowance(), l.getTaxBase(), l.getTax(), l.getNet()};
            for (int i = 0; i < v.length; i++) tot[i] = tot[i].add(Money.nz(v[i]));
            rows.add(new Object[] {nr++, nm[0], nm.length > 1 ? nm[1] : "", l.getTaxCode(), v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
                    l.isDeclared() ? "PO" : "JO", v[12]});
        }
        rows.add(new Object[] {"Total", null, null, null, tot[0], tot[1], tot[2], tot[3], tot[4], tot[5], tot[6], tot[7], tot[8], tot[9], tot[10], tot[11], null, tot[12]});
        String[] h = {"NR", "Emer", "Mbiemer", "Kodi ne tatime (part time-002, full time -001)", "10) Paga bruto",
                "11) Paga mbi te cilen llogariten kontributet", "12) Gjithsej (13+14+15)", "13) Kontribute per sigurimet shoqerore Punedhenesi",
                "14) Kontribute per sigurimet shoqerore Punemarresi", "15) Kontribute per sigurimet shoqerore", "16) Kontribute per sigurimet shendetesore gjithsej",
                "17) Paga bruto mbi te cilen llogaritet tatimi mbi te ardhura", "18) Shuma që zbritet nga paga dhe derdhet nga punëdhënësi",
                "19) Zbritja tatimore që përfiton punonjësi", "20) Baza tatimore mbi të cilën llogaritet TAP", "21) Tatimi mbi te ardhurat nga punesimi",
                "Ka plotesuar deklaraten?", "Paga Neto"};
        return OrderController.xlsx(excel.table("Listepagesa " + run.getPeriod(), h, rows), "Listepagesa-" + run.getPeriod() + ".xlsx");
    }

    @GetMapping("/payroll/settings")
    public String settings(Model model) {
        model.addAttribute("brackets", payroll.brackets());
        model.addAttribute("example", sample(payroll));
        return "payroll/settings";
    }

    /** A worked example on the settings page: what 80,000 gross becomes. */
    private static com.fms.domain.PayrollLine sample(PayrollService p) {
        var l = new com.fms.domain.PayrollLine();
        l.setGross(new BigDecimal("80000"));
        p.calculate(l);
        return l;
    }

    @PostMapping("/payroll/settings")
    public String saveSettings(@RequestParam BigDecimal empSocial, @RequestParam BigDecimal empHealth,
                               @RequestParam BigDecimal erSocial, @RequestParam BigDecimal erHealth,
                               @RequestParam BigDecimal minBase, @RequestParam BigDecimal maxBase,
                               @RequestParam(defaultValue = "false") boolean taxOnNet,
                               @RequestParam(name = "bFrom", required = false) java.util.List<String> from,
                               @RequestParam(name = "bTo", required = false) java.util.List<String> to,
                               @RequestParam(name = "bFixed", required = false) java.util.List<String> fixed,
                               @RequestParam(name = "bRate", required = false) java.util.List<String> rate,
                               @RequestParam(name = "bOver", required = false) java.util.List<String> over,
                               RedirectAttributes redirect) {
        java.util.List<com.fms.domain.TaxBracket> list = new java.util.ArrayList<>();
        if (from != null) {
            for (int i = 0; i < from.size(); i++) {
                if (from.get(i) == null || from.get(i).isBlank()) continue;
                var b = new com.fms.domain.TaxBracket();
                b.setFromAmount(num(from.get(i)));
                b.setToAmount(to != null && i < to.size() && !to.get(i).isBlank() ? num(to.get(i)) : null);
                b.setFixedAmount(fixed != null && i < fixed.size() ? num(fixed.get(i)) : BigDecimal.ZERO);
                b.setRate(rate != null && i < rate.size() ? num(rate.get(i)) : BigDecimal.ZERO);
                b.setOverAmount(over != null && i < over.size() ? num(over.get(i)) : BigDecimal.ZERO);
                list.add(b);
            }
        }
        payroll.saveSettings(empSocial, empHealth, erSocial, erHealth, minBase, maxBase, taxOnNet, list);
        redirect.addFlashAttribute("message", "Payroll settings saved.");
        return "redirect:/payroll/settings";
    }

    private static BigDecimal num(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
