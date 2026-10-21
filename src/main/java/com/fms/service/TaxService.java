package com.fms.service;

import com.fms.domain.AppConfig;
import com.fms.domain.AppUser;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.TaxPayment;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.TaxPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tax due (detyrimet tatimore): per month, VAT to pay (VAT on sales minus VAT
 * on purchases, returns included) and payroll taxes (income tax plus employee
 * and employer contributions from approved payrolls), with due dates, what's
 * been paid and what's still open. Paying can record a "Taxes" movement.
 */
@Service
public class TaxService {

    @org.springframework.beans.factory.annotation.Autowired
    private PeriodLock periodLock;

    public record Row(String period, String kind, BigDecimal output, BigDecimal input, BigDecimal due,
                      BigDecimal paid, BigDecimal open, LocalDate dueDate, String status) {
    }

    private final OrderLineRepository lines;
    private final TaxPaymentRepository payments;
    private final PayrollService payroll;
    private final CashService cash;
    private final ConfigService config;
    private final AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired
    private DocText doc;

    public TaxService(OrderLineRepository lines, TaxPaymentRepository payments, PayrollService payroll,
                      CashService cash, ConfigService config, AuditService audit) {
        this.lines = lines;
        this.payments = payments;
        this.payroll = payroll;
        this.cash = cash;
        this.config = config;
        this.audit = audit;
    }

    /** The last 12 months, newest first. */
    @Transactional(readOnly = true)
    public List<Row> overview() {
        AppConfig c = config.get();
        List<TaxPayment> paid = payments.findAll();
        List<Row> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            YearMonth m = now.minusMonths(i);
            BigDecimal outVat = vat(OrderType.SALE, m);
            BigDecimal inVat = vat(OrderType.PURCHASE, m);
            BigDecimal vatDue = outVat.subtract(inVat);
            out.add(row(m, "VAT", outVat, inVat, vatDue, paid, dueDate(m, c.getTaxVatDay())));
            if (c.isModPayroll()) {
                BigDecimal p = payroll.taxesDue(m.toString());
                out.add(row(m, "PAYROLL", p, BigDecimal.ZERO, p, paid, dueDate(m, c.getTaxPayrollDay())));
            }
        }
        return out;
    }

    private Row row(YearMonth m, String kind, BigDecimal output, BigDecimal input, BigDecimal due, List<TaxPayment> paid, LocalDate dueDate) {
        BigDecimal p = paid.stream().filter(x -> x.getKind().equals(kind) && x.getPeriod().equals(m.toString()))
                .map(TaxPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal open = due.subtract(p);
        String status;
        if (due.signum() < 0) {
            status = "CREDIT";               // more VAT paid on purchases than charged on sales
        } else if (due.signum() == 0 || open.signum() <= 0) {
            status = due.signum() == 0 ? "NONE" : "PAID";
        } else if (YearMonth.from(LocalDate.now()).equals(m)) {
            status = "RUNNING";              // month not finished yet
        } else {
            status = LocalDate.now().isAfter(dueDate) ? "OVERDUE" : "DUE";
        }
        return new Row(m.toString(), kind, Money.round(output), Money.round(input), Money.round(due),
                Money.round(p), Money.round(open.max(BigDecimal.ZERO)), dueDate, status);
    }

    private static LocalDate dueDate(YearMonth m, int day) {
        YearMonth next = m.plusMonths(1);
        return next.atDay(Math.min(Math.max(1, day), next.lengthOfMonth()));
    }

    private BigDecimal vat(OrderType type, YearMonth m) {
        return lines.findLines(type, null, m.atDay(1).atStartOfDay(), m.plusMonths(1).atDay(1).atStartOfDay()).stream()
                .map(OrderLine::getVatValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<TaxPayment> payments() {
        return payments.findAllByOrderByPaidOnDesc();
    }

    @Transactional
    public void pay(String kind, String period, BigDecimal amount, LocalDate date, Long accountId, String note, AppUser user) {
        if (!"VAT".equals(kind) && !"PAYROLL".equals(kind)) {
            throw new OrderException("Choose the tax.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new OrderException("Enter an amount greater than 0.");
        }
        periodLock.check(date == null ? LocalDate.now() : date);
        TaxPayment t = new TaxPayment();
        t.setKind(kind);
        t.setPeriod(YearMonth.parse(period).toString());
        t.setAmount(Money.round(amount));
        t.setPaidOn(date == null ? LocalDate.now() : date);
        t.setNote(note == null || note.isBlank() ? null : note.trim());
        t.setCreatedBy(user.getUsername());
        payments.save(t);
        if (config.get().isModCash() && accountId != null) {
            cash.addEntry(accountId, t.getPaidOn(), com.fms.domain.MoneyCategory.TAXES, t.getAmount(),
                    doc.t("VAT".equals(kind) ? "je.vatPaid" : "je.payrollTaxesPaid", "Tax {0}", t.getPeriod()), t.getPeriod(), null, "TAX", user);
        }
        audit.log("PAY_TAX", "Tax", kind + " " + t.getPeriod(), Money.format(t.getAmount()));
    }

    @Transactional
    public void deletePayment(Long id) {
        payments.findById(id).ifPresent(t -> {
            periodLock.check(t.getPaidOn());
            payments.delete(t);
            audit.log("DELETE_TAX_PAYMENT", "Tax", t.getKind() + " " + t.getPeriod(), Money.format(t.getAmount())
                    + " (any Cash & Bank movement stays; delete it there if needed)");
        });
    }
}
