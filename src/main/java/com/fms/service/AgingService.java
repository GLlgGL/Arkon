package com.fms.service;

import com.fms.domain.OrderType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts receivable (sales) and payable (purchases) by age: what's not yet
 * due, and what's 1–30, 31–60, 61–90 and over 90 days overdue, per party.
 */
@Service
public class AgingService {

    /** b[0] not yet due, b[1] 1–30, b[2] 31–60, b[3] 61–90, b[4] over 90 days overdue. */
    public record Row(String party, BigDecimal[] b, BigDecimal total, int invoices) { }
    public record Aging(OrderType type, List<Row> rows, BigDecimal[] totals, BigDecimal total) { }

    private final PaymentService payments;

    public AgingService(PaymentService payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public Aging aging(OrderType type) {
        Map<String, BigDecimal[]> by = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> count = new HashMap<>();
        BigDecimal[] totals = zero();
        for (var d : payments.debts(type).orders()) {
            if (d.remaining().signum() <= 0) continue;
            long days = d.daysOverdue();
            int i = days <= 0 ? 0 : days <= 30 ? 1 : days <= 60 ? 2 : days <= 90 ? 3 : 4;
            BigDecimal[] a = by.computeIfAbsent(d.party(), k -> zero());
            a[i] = a[i].add(d.remaining());
            totals[i] = totals[i].add(d.remaining());
            count.merge(d.party(), 1, Integer::sum);
        }
        List<Row> rows = new ArrayList<>();
        by.forEach((p, a) -> rows.add(new Row(p, a, Arrays.stream(a).reduce(BigDecimal.ZERO, BigDecimal::add), count.get(p))));
        rows.sort(Comparator.comparing(Row::total).reversed());
        return new Aging(type, rows, totals, Arrays.stream(totals).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal[] zero() {
        BigDecimal[] a = new BigDecimal[5];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }
}
