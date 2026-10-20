package com.fms.service;

import com.fms.domain.AppConfig;
import com.fms.domain.AppUser;
import com.fms.domain.Employee;
import com.fms.domain.NotFoundException;
import com.fms.domain.PayrollLine;
import com.fms.domain.PayrollRun;
import com.fms.domain.TaxBracket;
import com.fms.repo.EmployeeRepository;
import com.fms.repo.PayrollLineRepository;
import com.fms.repo.PayrollRunRepository;
import com.fms.repo.TaxBracketRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payroll (listëpagesa): employees, monthly payroll runs and the calculation of
 * contributions and income tax. All rates and tax brackets are editable
 * (Payroll settings), because they change by law; defaults are for Albania 2026.
 */
@Service
public class PayrollService {

    private static final BigDecimal H = BigDecimal.valueOf(100);

    public record Totals(BigDecimal gross, BigDecimal empContrib, BigDecimal tax, BigDecimal net,
                         BigDecimal erContrib, BigDecimal totalCost) {
        /** Paid to the tax office: income tax + employee and employer contributions. */
        public BigDecimal taxesDue() { return tax.add(empContrib).add(erContrib); }
    }

    private final EmployeeRepository employees;
    private final PayrollRunRepository runs;
    private final PayrollLineRepository lines;
    private final TaxBracketRepository brackets;
    private final ConfigService config;
    private final CashService cash;
    private final AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired
    private DocText doc;

    public PayrollService(EmployeeRepository employees, PayrollRunRepository runs, PayrollLineRepository lines,
                          TaxBracketRepository brackets, ConfigService config, CashService cash, AuditService audit) {
        this.employees = employees;
        this.runs = runs;
        this.lines = lines;
        this.brackets = brackets;
        this.config = config;
        this.cash = cash;
        this.audit = audit;
    }

    // -------------------------------------------------------------- calculation

    /** Fills a line from its gross salary. */
    public void calculate(PayrollLine l) {
        AppConfig c = config.get();
        BigDecimal gross = Money.nz(l.getGross());
        // contributions are calculated on gross, kept between the minimum wage and the ceiling
        BigDecimal base = gross.max(c.getPrMinBase()).min(c.getPrMaxBase());
        if (gross.signum() == 0) {
            base = BigDecimal.ZERO;
        }
        BigDecimal empSocial = pct(base, c.getPrEmpSocial());
        BigDecimal empHealth = pct(base, c.getPrEmpHealth());
        BigDecimal taxable = c.isPrTaxOnNet() ? gross.subtract(empSocial).subtract(empHealth) : gross;
        BigDecimal tax = incomeTax(taxable);
        l.setEmpSocial(empSocial);
        l.setEmpHealth(empHealth);
        l.setTax(tax);
        l.setNet(Money.round(gross.subtract(empSocial).subtract(empHealth).subtract(tax)));
        l.setErSocial(pct(base, c.getPrErSocial()));
        l.setErHealth(pct(base, c.getPrErHealth()));
        l.setTotalCost(Money.round(gross.add(l.getErSocial()).add(l.getErHealth())));
    }

    /** Monthly income tax from the brackets: FIXED + RATE% x (amount - OVER). */
    public BigDecimal incomeTax(BigDecimal amount) {
        for (TaxBracket b : brackets.findAllByOrderByFromAmountAsc()) {
            boolean inside = amount.compareTo(b.getFromAmount()) >= 0
                    && (b.getToAmount() == null || amount.compareTo(b.getToAmount()) <= 0);
            if (inside) {
                BigDecimal over = amount.subtract(Money.nz(b.getOverAmount())).max(BigDecimal.ZERO);
                return Money.round(Money.nz(b.getFixedAmount()).add(pct(over, Money.nz(b.getRate()))));
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal pct(BigDecimal v, BigDecimal rate) {
        return v.multiply(rate).divide(H, 2, RoundingMode.HALF_UP);
    }

    // --------------------------------------------------------------- employees

    @Transactional(readOnly = true)
    public List<Employee> employees() {
        return employees.findAllByOrderByActiveDescNameAsc();
    }

    public Employee employee(Long id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    @Transactional
    public Employee saveEmployee(Long id, String name, String personalNo, String position, BigDecimal gross,
                                 LocalDate start, LocalDate end, String iban, Long costCenterId, boolean active) {
        if (name == null || name.isBlank() || gross == null || gross.signum() < 0) {
            throw new OrderException("Enter the name and the gross salary.");
        }
        Employee e = id == null ? new Employee() : employee(id);
        e.setName(name.trim());
        e.setPersonalNo(blank(personalNo));
        e.setPosition(blank(position));
        e.setGrossSalary(Money.round(gross));
        e.setStartDate(start == null ? LocalDate.now() : start);
        e.setEndDate(end);
        e.setIban(blank(iban));
        e.setCostCenterId(costCenterId);
        e.setActive(active);
        Employee saved = employees.save(e);
        audit.log(id == null ? "CREATE" : "UPDATE", "Employee", saved.getName(), "gross " + Money.format(saved.getGrossSalary()));
        return saved;
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee e = employee(id);
        employees.delete(e);
        audit.log("DELETE", "Employee", e.getName(), "Payroll history is kept");
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // -------------------------------------------------------------------- runs

    @Transactional(readOnly = true)
    public List<PayrollRun> runs() {
        return runs.findAllByOrderByPeriodDesc();
    }

    public PayrollRun run(Long id) {
        return runs.findById(id).orElseThrow(() -> new NotFoundException("Payroll not found"));
    }

    @Transactional(readOnly = true)
    public List<PayrollLine> lines(Long runId) {
        return lines.findByRunIdOrderByEmployeeNameAsc(runId);
    }

    public Totals totals(List<PayrollLine> ls) {
        BigDecimal g = BigDecimal.ZERO, ec = BigDecimal.ZERO, t = BigDecimal.ZERO, n = BigDecimal.ZERO, rc = BigDecimal.ZERO, tc = BigDecimal.ZERO;
        for (PayrollLine l : ls) {
            g = g.add(l.getGross());
            ec = ec.add(l.getEmpSocial()).add(l.getEmpHealth());
            t = t.add(l.getTax());
            n = n.add(l.getNet());
            rc = rc.add(l.getErSocial()).add(l.getErHealth());
            tc = tc.add(l.getTotalCost());
        }
        return new Totals(g, ec, t, n, rc, tc);
    }

    /** Payroll taxes of a month (approved or paid runs), for Tax due. */
    @Transactional(readOnly = true)
    public BigDecimal taxesDue(String period) {
        return runs.findByPeriod(period).filter(r -> !r.isDraft())
                .map(r -> totals(lines(r.getId())).taxesDue()).orElse(BigDecimal.ZERO);
    }

    /** New payroll for a month (YYYY-MM): one line per employee working that month. */
    @Transactional
    public PayrollRun createRun(String period, AppUser user) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(period);
        } catch (Exception e) {
            throw new OrderException("Choose the month.");
        }
        if (runs.existsByPeriod(ym.toString())) {
            throw new OrderException("There's already a payroll for " + ym + ".");
        }
        PayrollRun r = new PayrollRun();
        r.setPeriod(ym.toString());
        r.setCreatedBy(user.getUsername());
        r = runs.save(r);
        for (Employee e : employees.findAll()) {
            boolean working = e.isActive() && !e.getStartDate().isAfter(ym.atEndOfMonth())
                    && (e.getEndDate() == null || !e.getEndDate().isBefore(ym.atDay(1)));
            if (!working) {
                continue;
            }
            PayrollLine l = new PayrollLine();
            l.setRunId(r.getId());
            l.setEmployeeId(e.getId());
            l.setEmployeeName(e.getName());
            l.setGross(e.getGrossSalary());
            l.setCostCenterId(e.getCostCenterId());
            calculate(l);
            lines.save(l);
        }
        audit.log("CREATE", "Payroll", r.getPeriod(), null);
        return r;
    }

    /** Changes gross amounts on a draft (bonuses, unpaid leave) and recalculates. */
    @Transactional
    public void updateGross(Long runId, java.util.Map<Long, BigDecimal> grossByLine) {
        PayrollRun r = run(runId);
        if (!r.isDraft()) {
            throw new OrderException("Only a draft payroll can be changed.");
        }
        for (PayrollLine l : lines(runId)) {
            BigDecimal g = grossByLine.get(l.getId());
            if (g != null && g.signum() >= 0) {
                l.setGross(Money.round(g));
            }
            calculate(l);       // also picks up changed rates
            lines.save(l);
        }
        audit.log("UPDATE", "Payroll", r.getPeriod(), "recalculated");
    }

    @Transactional
    public void approve(Long runId) {
        PayrollRun r = run(runId);
        if (!r.isDraft()) {
            throw new OrderException("This payroll is already approved.");
        }
        r.setStatus("APPROVED");
        runs.save(r);
        audit.log("APPROVE", "Payroll", r.getPeriod(), Money.format(totals(lines(runId)).net()) + " net");
    }

    /** Net salaries paid; with Cash & Bank on, recorded as a "Salaries" movement from the chosen account. */
    @Transactional
    public void pay(Long runId, LocalDate date, Long accountId, AppUser user) {
        PayrollRun r = run(runId);
        if (!"APPROVED".equals(r.getStatus())) {
            throw new OrderException("Approve the payroll before paying it.");
        }
        LocalDate d = date == null ? LocalDate.now() : date;
        Totals t = totals(lines(runId));
        if (config.get().isModCash() && accountId != null && t.net().signum() > 0) {
            cash.addEntry(accountId, d, com.fms.domain.MoneyCategory.SALARIES, t.net(),
                    doc.t("je.payroll", "Payroll {0}", r.getPeriod()), r.getPeriod(), null, "PAYROLL", user);
        }
        r.setStatus("PAID");
        r.setPaidOn(d);
        runs.save(r);
        audit.log("PAY", "Payroll", r.getPeriod(), Money.format(t.net()) + " net paid on " + d);
    }

    @Transactional
    public void deleteRun(Long runId) {
        PayrollRun r = run(runId);
        if (!r.isDraft()) {
            throw new OrderException("Only a draft payroll can be deleted.");
        }
        runs.delete(r);
        audit.log("DELETE", "Payroll", r.getPeriod(), null);
    }

    // ---------------------------------------------------------------- settings

    @Transactional(readOnly = true)
    public List<TaxBracket> brackets() {
        return brackets.findAllByOrderByFromAmountAsc();
    }

    /** Saves the rates and replaces the tax brackets. */
    @Transactional
    public void saveSettings(BigDecimal empSocial, BigDecimal empHealth, BigDecimal erSocial, BigDecimal erHealth,
                             BigDecimal minBase, BigDecimal maxBase, boolean taxOnNet, List<TaxBracket> newBrackets) {
        AppConfig c = config.get();
        c.setPrEmpSocial(empSocial);
        c.setPrEmpHealth(empHealth);
        c.setPrErSocial(erSocial);
        c.setPrErHealth(erHealth);
        c.setPrMinBase(minBase);
        c.setPrMaxBase(maxBase);
        c.setPrTaxOnNet(taxOnNet);
        config.save(c);
        brackets.deleteAllInBatch();
        brackets.saveAll(newBrackets);
        audit.log("UPDATE", "Settings", "Payroll", "rates and " + newBrackets.size() + " tax brackets");
    }
}
