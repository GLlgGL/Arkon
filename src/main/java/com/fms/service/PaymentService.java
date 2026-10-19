package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderType;
import com.fms.domain.Payment;
import com.fms.repo.OrderInfoRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payments received for sales and paid for purchases, order balances, and
 * the debts overview (who owes us, whom we owe).
 */
@Service
public class PaymentService {

    /** Payment state of one order. */
    public enum Status {
        PAID("Paid"), PARTLY("Partly paid"), UNPAID("Unpaid"), OVERDUE("Overdue");
        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public record Balance(BigDecimal total, BigDecimal paid, BigDecimal remaining, LocalDate dueDate,
                          long daysOverdue, Status status) {
    }

    /** One unpaid or partly paid order on the debts page. */
    public record DebtRow(String code, String party, LocalDate date, LocalDate dueDate,
                          BigDecimal total, BigDecimal paid, BigDecimal remaining, long daysOverdue, Status status,
                          String createdBy) {
    }

    /** All open orders of one client or merchant. */
    public record PartyDebt(String party, int orders, BigDecimal remaining, BigDecimal overdue, LocalDate oldestDue) {
    }

    public record Debts(OrderType type, List<PartyDebt> parties, List<DebtRow> orders,
                        BigDecimal totalRemaining, BigDecimal totalOverdue, long overdueOrders) {
    }

    private final PaymentRepository payments;
    private final OrderLineRepository lines;
    private final OrderInfoRepository infos;
    private final ConfigService config;
    private final AuditService audit;

    public PaymentService(PaymentRepository payments, OrderLineRepository lines, OrderInfoRepository infos,
                          ConfigService config, AuditService audit) {
        this.payments = payments;
        this.lines = lines;
        this.infos = infos;
        this.config = config;
        this.audit = audit;
    }

    public static boolean hasPayments(OrderType type) {
        return type == OrderType.SALE || type == OrderType.PURCHASE;
    }

    @Transactional(readOnly = true)
    public List<Payment> list(OrderType type, String code) {
        return payments.findByOrderTypeAndOrderCodeOrderByPaidOnAscIdAsc(type, code);
    }

    /** Balance of one order, given its total with VAT and its info (due date). */
    public Balance balance(BigDecimal total, BigDecimal paid, LocalDate dueDate) {
        BigDecimal t = Money.nz(total);
        BigDecimal p = Money.nz(paid);
        BigDecimal remaining = t.subtract(p).max(BigDecimal.ZERO);
        long overdue = dueDate == null ? 0 : ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        Status s;
        if (remaining.signum() == 0) {
            s = Status.PAID;
            overdue = 0;
        } else if (overdue > 0) {
            s = Status.OVERDUE;
        } else {
            s = p.signum() > 0 ? Status.PARTLY : Status.UNPAID;
        }
        return new Balance(Money.round(t), Money.round(p), Money.round(remaining), dueDate, Math.max(0, overdue), s);
    }

    @Transactional(readOnly = true)
    public Balance balance(OrderView order, OrderInfo info) {
        BigDecimal paid = list(order.type(), order.code()).stream().map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance(order.valueWithVat(), paid, info.getDueDate());
    }

    @Transactional
    public Payment add(OrderView order, LocalDate paidOn, BigDecimal amount, Payment.Method method, String note, AppUser user) {
        if (!hasPayments(order.type()) || order.isEmpty()) {
            throw new OrderException("Payments can only be recorded on saved sales and purchases.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new OrderException("Enter an amount greater than 0.");
        }
        BigDecimal paid = list(order.type(), order.code()).stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = order.valueWithVat().subtract(paid);
        if (amount.compareTo(remaining) > 0) {
            throw new OrderException("That's more than the " + Money.format(remaining.max(BigDecimal.ZERO))
                    + " still to pay on this order.");
        }
        Payment p = new Payment();
        p.setOrderType(order.type());
        p.setOrderCode(order.code());
        p.setPaidOn(paidOn == null ? LocalDate.now() : paidOn);
        p.setAmount(Money.round(amount));
        p.setMethod(method == null ? Payment.Method.CASH : method);
        p.setNote(note == null || note.isBlank() ? null : note.trim());
        p.setCreatedBy(user.getUsername());
        Payment saved = payments.save(p);
        audit.log("ADD_PAYMENT", order.type().getLabel(), order.code(),
                Money.format(saved.getAmount()) + " by " + saved.getMethod().getLabel() + " on " + saved.getPaidOn());
        return saved;
    }

    @Transactional
    public void delete(OrderType type, String code, Long paymentId, AppUser user) {
        if (!user.isAdmin()) {
            throw new OrderException("Only administrators can delete payments.");
        }
        Payment p = payments.findById(paymentId)
                .filter(x -> x.getOrderType() == type && x.getOrderCode().equals(code))
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        payments.delete(p);
        audit.log("DELETE_PAYMENT", type.getLabel(), code, Money.format(p.getAmount()) + " of " + p.getPaidOn());
    }

    /** Payment status for the orders shown on a list page, by order code. */
    @Transactional(readOnly = true)
    public Map<String, Balance> statusFor(OrderType type, Map<String, BigDecimal> totalsByCode,
                                          Map<String, LocalDate> datesByCode) {
        if (!hasPayments(type) || totalsByCode.isEmpty()) {
            return Map.of();
        }
        Collection<String> codes = totalsByCode.keySet();
        Map<String, BigDecimal> paid = toMap(payments.paidPerOrder(type, codes));
        Map<String, LocalDate> due = infos.findByOrderTypeAndOrderCodeIn(type, codes).stream()
                .filter(i -> i.getDueDate() != null)
                .collect(Collectors.toMap(OrderInfo::getOrderCode, OrderInfo::getDueDate, (a, b) -> a));
        int terms = config.get().getPaymentTermsDays();
        Map<String, Balance> out = new HashMap<>();
        totalsByCode.forEach((code, total) -> {
            LocalDate d = due.getOrDefault(code, datesByCode.getOrDefault(code, LocalDate.now()).plusDays(terms));
            out.put(code, balance(total, paid.get(code), d));
        });
        return out;
    }

    /**
     * Everything still unpaid: sales (clients owe us) or purchases (we owe
     * merchants), per order and per client/merchant, largest overdue first.
     */
    @Transactional(readOnly = true)
    public Debts debts(OrderType type) {
        Map<String, BigDecimal> paid = toMap(payments.paidPerOrder(type));
        Map<String, LocalDate> due = infos.findByOrderType(type).stream()
                .filter(i -> i.getDueDate() != null)
                .collect(Collectors.toMap(OrderInfo::getOrderCode, OrderInfo::getDueDate, (a, b) -> a));
        int terms = config.get().getPaymentTermsDays();

        List<DebtRow> rows = new ArrayList<>();
        for (Object[] r : lines.orderTotals(type)) {
            String code = (String) r[0];
            String party = type == OrderType.PURCHASE ? (String) r[2] : (String) r[1];
            LocalDate date = ((LocalDateTime) r[3]).toLocalDate();
            BigDecimal total = new BigDecimal(r[4].toString());
            LocalDate d = due.getOrDefault(code, date.plusDays(terms));
            Balance b = balance(total, paid.get(code), d);
            if (b.status() != Status.PAID) {
                rows.add(new DebtRow(code, party, date, d, b.total(), b.paid(), b.remaining(),
                        b.daysOverdue(), b.status(), (String) r[5]));
            }
        }
        rows.sort(Comparator.comparingLong(DebtRow::daysOverdue).reversed()
                .thenComparing(DebtRow::dueDate, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, List<DebtRow>> byParty = rows.stream()
                .collect(Collectors.groupingBy(DebtRow::party, LinkedHashMap::new, Collectors.toList()));
        List<PartyDebt> parties = new ArrayList<>();
        byParty.forEach((party, list) -> parties.add(new PartyDebt(party, list.size(),
                sum(list, DebtRow::remaining),
                sum(list.stream().filter(x -> x.status() == Status.OVERDUE).toList(), DebtRow::remaining),
                list.stream().map(DebtRow::dueDate).min(Comparator.naturalOrder()).orElse(null))));
        parties.sort(Comparator.comparing(PartyDebt::remaining).reversed());

        return new Debts(type, parties, rows, sum(rows, DebtRow::remaining),
                sum(rows.stream().filter(x -> x.status() == Status.OVERDUE).toList(), DebtRow::remaining),
                rows.stream().filter(x -> x.status() == Status.OVERDUE).count());
    }

    private static <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Map<String, BigDecimal> toMap(List<Object[]> rows) {
        Map<String, BigDecimal> m = new HashMap<>();
        for (Object[] r : rows) {
            m.put((String) r[0], new BigDecimal(r[1].toString()));
        }
        return m;
    }
}
