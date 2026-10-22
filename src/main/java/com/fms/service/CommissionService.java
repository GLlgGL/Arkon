package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.AppUserRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sales commissions per salesperson (the user who created the sale): a % of the sales
 * without VAT, or of the profit (sales − cost of goods). Returns made in the period are
 * deducted. Optionally only fully paid sales count.
 */
@Service
public class CommissionService {

    public record Row(String user, int sales, BigDecimal noVat, BigDecimal cost, BigDecimal profit, String base, BigDecimal pct, BigDecimal commission) { }
    public record Report(List<Row> rows, BigDecimal noVat, BigDecimal profit, BigDecimal commission) { }

    private final OrderLineRepository lines;
    private final AppUserRepository users;
    private final ProductRepository products;
    private final PaymentService payments;

    public CommissionService(OrderLineRepository lines, AppUserRepository users, ProductRepository products, PaymentService payments) {
        this.lines = lines; this.users = users; this.products = products; this.payments = payments;
    }

    @Transactional(readOnly = true)
    public Report report(LocalDate from, LocalDate to, boolean paidOnly) {
        Set<String> unpaid = new HashSet<>();
        if (paidOnly) payments.debts(OrderType.SALE).orders().forEach(d -> { if (d.remaining().signum() > 0) unpaid.add(d.code()); });
        Map<String, BigDecimal[]> by = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);   // user -> [noVat, cost]
        Map<String, Set<String>> orders = new HashMap<>();
        Map<String, BigDecimal> buy = new HashMap<>();
        for (OrderLine l : lines.linesBetween(OrderType.SALE, from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            if (unpaid.contains(l.getOrderCode())) continue;
            String u = l.getCreatedBy() == null ? "?" : l.getCreatedBy();
            BigDecimal unit = l.getUnitCost();
            if (unit == null) unit = buy.computeIfAbsent(l.getProductName(), n -> products.findByName(n).map(Product::getBuyPrice).orElse(BigDecimal.ZERO));
            BigDecimal[] a = by.computeIfAbsent(u, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(Money.nz(l.getValueNoVat()));
            a[1] = a[1].add(unit.multiply(BigDecimal.valueOf(l.getQuantity())));
            if (!l.isReturn()) orders.computeIfAbsent(u, k -> new HashSet<>()).add(l.getOrderCode());
        }
        Map<String, AppUser> people = new HashMap<>();
        users.findAll().forEach(x -> people.put(x.getUsername(), x));
        List<Row> rows = new ArrayList<>();
        BigDecimal tNoVat = BigDecimal.ZERO, tProfit = BigDecimal.ZERO, tComm = BigDecimal.ZERO;
        for (var e : by.entrySet()) {
            AppUser u = people.get(e.getKey());
            BigDecimal noVat = Money.round(e.getValue()[0]), cost = Money.round(e.getValue()[1]), profit = noVat.subtract(cost);
            BigDecimal pct = u == null ? null : u.getCommissionPct();
            String base = u == null ? "SALES" : u.getCommissionBase();
            BigDecimal comm = pct == null ? BigDecimal.ZERO
                    : Money.round(("PROFIT".equals(base) ? profit : noVat).max(BigDecimal.ZERO).multiply(pct).movePointLeft(2));
            rows.add(new Row(e.getKey(), orders.getOrDefault(e.getKey(), Set.of()).size(), noVat, cost, profit, base, pct, comm));
            tNoVat = tNoVat.add(noVat); tProfit = tProfit.add(profit); tComm = tComm.add(comm);
        }
        rows.sort(Comparator.comparing(Row::noVat).reversed());
        return new Report(rows, tNoVat, tProfit, tComm);
    }
}
