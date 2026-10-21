package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Budgets per month compared with actual results, and a 13-week cash-flow
 * forecast. Actual results come from sales (revenue and cost of goods) and
 * from Cash & Bank entries (other income and expenses); with Payroll on,
 * salaries are the approved payrolls' total cost.
 */
@Service
public class BudgetService {

    /** Lines of the budget, in display order. INCOME lines add to the result, the others subtract. */
    public static final List<String> INCOME = List.of("SALES", "OTHER_INCOME");
    public static final List<String> KEYS = List.of("SALES", "COGS", "OTHER_INCOME", "RENT", "SALARIES", "UTILITIES",
            "TRANSPORT", "TAXES", "BANK_FEES", "OTHER_EXPENSE");

    public record CompareRow(String key, BigDecimal budget, BigDecimal actual, BigDecimal diff, BigDecimal pct,
                             BigDecimal budgetYtd, BigDecimal actualYtd, BigDecimal diffYtd, BigDecimal pctYtd, boolean income) { }
    public record Week(LocalDate start, BigDecimal receipts, BigDecimal suppliers, BigDecimal taxes, BigDecimal salaries,
                       BigDecimal expenses, BigDecimal net, BigDecimal balance) { }
    public record Forecast(BigDecimal opening, boolean fromCash, List<Week> weeks, BigDecimal lowest, LocalDate lowestWeek) { }

    private final BudgetLineRepository budget;
    private final OrderLineRepository lines;
    private final MoneyMovementRepository moves;
    private final ProductRepository products;
    private final PaymentService payments;
    private final CashService cash;
    private final TaxService taxes;
    private final PayrollService payroll;
    private final PayrollRunRepository runs;
    private final ConfigService config;
    private final AuditService audit;

    public BudgetService(BudgetLineRepository budget, OrderLineRepository lines, MoneyMovementRepository moves, ProductRepository products,
                         PaymentService payments, CashService cash, TaxService taxes, PayrollService payroll, PayrollRunRepository runs,
                         ConfigService config, AuditService audit) {
        this.budget = budget; this.lines = lines; this.moves = moves; this.products = products; this.payments = payments;
        this.cash = cash; this.taxes = taxes; this.payroll = payroll; this.runs = runs; this.config = config; this.audit = audit;
    }

    private static Map<String, BigDecimal[]> empty() {
        Map<String, BigDecimal[]> m = new LinkedHashMap<>();
        for (String k : KEYS) { BigDecimal[] a = new BigDecimal[12]; Arrays.fill(a, BigDecimal.ZERO); m.put(k, a); }
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal[]> budget(int year) {
        Map<String, BigDecimal[]> m = empty();
        for (BudgetLine b : budget.findByBudgetYear(year)) {
            if (m.containsKey(b.getLineKey()) && b.getMonthNo() >= 1 && b.getMonthNo() <= 12) m.get(b.getLineKey())[b.getMonthNo() - 1] = b.getAmount();
        }
        return m;
    }

    @Transactional
    public void save(int year, Map<String, BigDecimal[]> values) {
        Map<String, BudgetLine> existing = new HashMap<>();
        budget.findByBudgetYear(year).forEach(b -> existing.put(b.getLineKey() + "#" + b.getMonthNo(), b));
        for (var e : values.entrySet()) {
            if (!KEYS.contains(e.getKey())) continue;
            for (int m = 1; m <= 12; m++) {
                BigDecimal v = e.getValue()[m - 1] == null ? BigDecimal.ZERO : Money.round(e.getValue()[m - 1]);
                BudgetLine b = existing.get(e.getKey() + "#" + m);
                if (b == null) {
                    if (v.signum() == 0) continue;
                    b = new BudgetLine(); b.setBudgetYear(year); b.setLineKey(e.getKey()); b.setMonthNo(m);
                }
                b.setAmount(v);
                budget.save(b);
            }
        }
        audit.log("UPDATE", "Budget", String.valueOf(year), "saved");
    }

    /** Budget of a year = last year's actual results, plus or minus a percentage. */
    @Transactional
    public void fillFromLastYear(int year, BigDecimal percent) {
        BigDecimal f = BigDecimal.ONE.add(Money.nz(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        Map<String, BigDecimal[]> a = actual(year - 1);
        a.values().forEach(arr -> { for (int i = 0; i < 12; i++) arr[i] = arr[i].multiply(f); });
        save(year, a);
    }

    /** Actual results per line and month. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal[]> actual(int year) {
        Map<String, BigDecimal[]> m = empty();
        LocalDate from = LocalDate.of(year, 1, 1), to = LocalDate.of(year, 12, 31);
        Map<String, Product> byName = new HashMap<>();
        products.findAll().forEach(p -> byName.put(p.getName(), p));
        for (OrderLine l : lines.findLines(OrderType.SALE, null, from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            int i = l.getCreatedAt().getMonthValue() - 1;
            m.get("SALES")[i] = m.get("SALES")[i].add(l.getValueNoVat());
            BigDecimal unit = l.getUnitCost();
            if (unit == null) { Product p = byName.get(l.getProductName()); unit = p == null ? BigDecimal.ZERO : p.getBuyPrice(); }
            m.get("COGS")[i] = m.get("COGS")[i].add(unit.multiply(BigDecimal.valueOf(l.getQuantity())));
        }
        for (MoneyMovement mv : moves.between(null, from, to)) {
            if (mv.getPaymentId() != null || mv.getTransferId() != null || mv.getOrigin() != null || !mv.getCategory().isManual()) continue;
            String k = switch (mv.getCategory()) {
                case OTHER_INCOME -> "OTHER_INCOME"; case RENT -> "RENT"; case SALARIES -> "SALARIES"; case UTILITIES -> "UTILITIES";
                case TRANSPORT -> "TRANSPORT"; case TAXES -> "TAXES"; case BANK_FEES -> "BANK_FEES"; case OTHER_EXPENSE -> "OTHER_EXPENSE";
                default -> null;     // capital, loans, owner withdrawals are not income or expenses
            };
            if (k == null) continue;
            int i = mv.getMovedOn().getMonthValue() - 1;
            m.get(k)[i] = m.get(k)[i].add(mv.getAmount());
        }
        if (config.get().isModPayroll()) {
            for (PayrollRun r : runs.findAll()) {
                YearMonth ym = YearMonth.parse(r.getPeriod());
                if (r.isDraft() || ym.getYear() != year) continue;
                int i = ym.getMonthValue() - 1;
                m.get("SALARIES")[i] = m.get("SALARIES")[i].add(payroll.totals(payroll.lines(r.getId())).totalCost());
            }
        }
        m.values().forEach(a -> { for (int i = 0; i < 12; i++) a[i] = Money.round(a[i]); });
        return m;
    }

    /** Result per month: income lines minus the others. */
    public static BigDecimal[] result(Map<String, BigDecimal[]> m) {
        BigDecimal[] r = new BigDecimal[12];
        Arrays.fill(r, BigDecimal.ZERO);
        m.forEach((k, a) -> { for (int i = 0; i < 12; i++) r[i] = INCOME.contains(k) ? r[i].add(a[i]) : r[i].subtract(a[i]); });
        return r;
    }

    @Transactional(readOnly = true)
    public List<CompareRow> compare(int year, int month) {
        Map<String, BigDecimal[]> b = budget(year), a = actual(year);
        List<CompareRow> out = new ArrayList<>();
        for (String k : KEYS) out.add(row(k, b.get(k), a.get(k), month, INCOME.contains(k)));
        out.add(row("RESULT", result(b), result(a), month, true));
        return out;
    }

    private static CompareRow row(String k, BigDecimal[] b, BigDecimal[] a, int month, boolean income) {
        BigDecimal bm = b[month - 1], am = a[month - 1], by = BigDecimal.ZERO, ay = BigDecimal.ZERO;
        for (int i = 0; i < month; i++) { by = by.add(b[i]); ay = ay.add(a[i]); }
        return new CompareRow(k, bm, am, am.subtract(bm), CashService.pct(am, bm), by, ay, ay.subtract(by), CashService.pct(ay, by), income);
    }

    // ------------------------------------------------------------ forecast

    /**
     * 13 weeks from this Monday. Opens with today's Cash & Bank balance (when
     * that module is on). Overdue items fall in the first week.
     */
    @Transactional(readOnly = true)
    public Forecast forecast() {
        var c = config.get();
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        int n = 13;
        LocalDate end = start.plusWeeks(n).minusDays(1);
        BigDecimal[][] w = new BigDecimal[n][5];     // receipts, suppliers, taxes, salaries, expenses
        for (BigDecimal[] x : w) Arrays.fill(x, BigDecimal.ZERO);
        java.util.function.BiConsumer<LocalDate, Object[]> put = (d, v) -> {
            LocalDate dd = d == null || d.isBefore(start) ? start : d;
            if (dd.isAfter(end)) return;
            int i = (int) (java.time.temporal.ChronoUnit.DAYS.between(start, dd) / 7);
            w[i][(Integer) v[0]] = w[i][(Integer) v[0]].add((BigDecimal) v[1]);
        };
        for (var r : payments.debts(OrderType.SALE).orders()) {
            if (r.remaining().signum() > 0) put.accept(r.dueDate() != null ? r.dueDate() : r.date(), new Object[] {0, r.remaining()});
        }
        for (var r : payments.debts(OrderType.PURCHASE).orders()) {
            if (r.remaining().signum() > 0) put.accept(r.dueDate() != null ? r.dueDate() : r.date(), new Object[] {1, r.remaining()});
        }
        if (c.isModTaxes()) {
            for (var r : taxes.overview()) if (r.open().signum() > 0) put.accept(r.dueDate(), new Object[] {2, r.open()});
        }
        // salaries: the last payroll's net, paid at each month end; its taxes on the payroll due day
        BigDecimal net = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        YearMonth lastRun = null;
        if (c.isModPayroll()) {
            for (PayrollRun r : runs.findAll()) {
                if (r.isDraft()) continue;
                YearMonth ym = YearMonth.parse(r.getPeriod());
                if (lastRun == null || ym.isAfter(lastRun)) {
                    lastRun = ym;
                    var t = payroll.totals(payroll.lines(r.getId()));
                    net = t.net();
                    tax = t.taxesDue();
                }
            }
            if (lastRun != null) {
                for (YearMonth m = YearMonth.from(start); !m.atDay(1).isAfter(end); m = m.plusMonths(1)) {
                    if (!m.isAfter(lastRun)) continue;
                    put.accept(m.atEndOfMonth(), new Object[] {3, net});
                    YearMonth next = m.plusMonths(1);
                    put.accept(next.atDay(Math.min(c.getTaxPayrollDay(), next.lengthOfMonth())), new Object[] {2, tax});
                }
            }
        }
        // budgeted expenses, spread evenly over the days of each month
        Map<Integer, Map<String, BigDecimal[]>> budgets = new HashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            var b = budgets.computeIfAbsent(d.getYear(), this::budget);
            BigDecimal day = BigDecimal.ZERO;
            for (String k : KEYS) {
                if (INCOME.contains(k) || "COGS".equals(k) || ("SALARIES".equals(k) && lastRun != null)) continue;
                day = day.add(b.get(k)[d.getMonthValue() - 1]);
            }
            if (day.signum() != 0) put.accept(d, new Object[] {4, day.divide(BigDecimal.valueOf(d.lengthOfMonth()), 2, RoundingMode.HALF_UP)});
        }
        boolean fromCash = c.isModCash();
        BigDecimal bal = fromCash ? cash.balances().stream().filter(x -> x.account().isActive()).map(CashService.AccountBalance::base)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        BigDecimal opening = bal, lowest = bal;
        LocalDate lowestWeek = start;
        List<Week> weeks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BigDecimal netW = w[i][0].subtract(w[i][1]).subtract(w[i][2]).subtract(w[i][3]).subtract(w[i][4]);
            bal = bal.add(netW);
            if (bal.compareTo(lowest) < 0) { lowest = bal; lowestWeek = start.plusWeeks(i); }
            weeks.add(new Week(start.plusWeeks(i), Money.round(w[i][0]), Money.round(w[i][1]), Money.round(w[i][2]), Money.round(w[i][3]),
                    Money.round(w[i][4]), Money.round(netW), Money.round(bal)));
        }
        return new Forecast(Money.round(opening), fromCash, weeks, Money.round(lowest), lowestWeek);
    }
}
