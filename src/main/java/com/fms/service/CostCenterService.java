package com.fms.service;

import com.fms.domain.CostCenter;
import com.fms.domain.MoneyMovement;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.Product;
import com.fms.repo.CostCenterRepository;
import com.fms.repo.MoneyMovementRepository;
import com.fms.repo.OrderInfoRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cost centres ("qendra e kostos"): sales and purchases are assigned per
 * order, other income and expenses per movement. The report shows, per cost
 * centre: revenue, cost of goods sold, gross profit, purchases, other income,
 * expenses and the result, compared with the previous period.
 */
@Service
public class CostCenterService {

    /** One cost centre's figures for a period. center == null means "not assigned". */
    public record Row(CostCenter center, BigDecimal revenue, BigDecimal cogs, BigDecimal grossProfit, BigDecimal purchases,
                      BigDecimal otherIncome, BigDecimal expenses, BigDecimal result, BigDecimal resultPrev,
                      BigDecimal resultPct) {
    }

    public record Report(LocalDate from, LocalDate to, LocalDate prevFrom, LocalDate prevTo, List<Row> rows, Row total) {
    }

    private final CostCenterRepository repo;
    private final OrderInfoRepository infos;
    private final OrderLineRepository lines;
    private final MoneyMovementRepository moves;
    private final ProductRepository products;
    private final OrderService orders;
    private final AuditService audit;

    public CostCenterService(CostCenterRepository repo, OrderInfoRepository infos, OrderLineRepository lines,
                             MoneyMovementRepository moves, ProductRepository products, OrderService orders, AuditService audit) {
        this.repo = repo;
        this.infos = infos;
        this.lines = lines;
        this.moves = moves;
        this.products = products;
        this.orders = orders;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CostCenter> all() {
        return repo.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<CostCenter> active() {
        return repo.findByActiveTrueOrderByCodeAsc();
    }

    public CostCenter get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Cost centre not found"));
    }

    @Transactional
    public CostCenter save(Long id, String code, String name, String description, boolean active) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new OrderException("Enter a code and a name.");
        }
        String c = code.trim().toUpperCase();
        if (id == null ? repo.existsByCode(c) : repo.existsByCodeAndIdNot(c, id)) {
            throw new OrderException("A cost centre with this code already exists.");
        }
        CostCenter cc = id == null ? new CostCenter() : get(id);
        cc.setCode(c);
        cc.setName(name);
        cc.setDescription(description);
        cc.setActive(active);
        CostCenter saved = repo.save(cc);
        audit.log(id == null ? "CREATE" : "UPDATE", "Cost centre", saved.getLabel(), null);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        CostCenter cc = get(id);
        repo.delete(cc);
        audit.log("DELETE", "Cost centre", cc.getLabel(), "Orders and movements keep working, now without a cost centre");
    }

    /** Assigns a sale or purchase to a cost centre (null removes it). */
    @Transactional
    public void assignOrder(OrderType type, String code, Long costCenterId, com.fms.domain.AppUser user) {
        orders.updateCostCenter(type, code, costCenterId, user);
    }

    // ----------------------------------------------------------------- report

    @Transactional(readOnly = true)
    public Report report(LocalDate from, LocalDate to) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        Map<Long, Figures> now = figures(from, to);
        Map<Long, Figures> prev = figures(prevFrom, prevTo);
        List<Row> rows = new ArrayList<>();
        Figures total = new Figures();
        Figures totalPrev = new Figures();
        List<CostCenter> centers = new ArrayList<>(all());
        centers.add(null);   // "not assigned"
        for (CostCenter cc : centers) {
            Long key = cc == null ? null : cc.getId();
            Figures f = now.getOrDefault(key, new Figures());
            Figures p = prev.getOrDefault(key, new Figures());
            if (cc == null && f.isEmpty() && p.isEmpty()) {
                continue;
            }
            rows.add(f.row(cc, p.result()));
            total.add(f);
            totalPrev.add(p);
        }
        return new Report(from, to, prevFrom, prevTo, rows, total.row(null, totalPrev.result()));
    }

    /** Running totals for one cost centre. */
    private static final class Figures {
        BigDecimal revenue = BigDecimal.ZERO, cogs = BigDecimal.ZERO, purchases = BigDecimal.ZERO,
                otherIncome = BigDecimal.ZERO, expenses = BigDecimal.ZERO;

        BigDecimal result() {
            return revenue.subtract(cogs).add(otherIncome).subtract(expenses);
        }

        boolean isEmpty() {
            return revenue.signum() == 0 && cogs.signum() == 0 && purchases.signum() == 0
                    && otherIncome.signum() == 0 && expenses.signum() == 0;
        }

        void add(Figures o) {
            revenue = revenue.add(o.revenue);
            cogs = cogs.add(o.cogs);
            purchases = purchases.add(o.purchases);
            otherIncome = otherIncome.add(o.otherIncome);
            expenses = expenses.add(o.expenses);
        }

        Row row(CostCenter cc, BigDecimal prevResult) {
            BigDecimal r = result();
            return new Row(cc, Money.round(revenue), Money.round(cogs), Money.round(revenue.subtract(cogs)),
                    Money.round(purchases), Money.round(otherIncome), Money.round(expenses), Money.round(r),
                    Money.round(prevResult), CashService.pct(r, prevResult));
        }
    }

    private Map<Long, Figures> figures(LocalDate from, LocalDate to) {
        Map<Long, Figures> out = new HashMap<>();
        Map<String, Long> saleCenter = centerByOrder(OrderType.SALE);
        Map<String, Long> purchaseCenter = centerByOrder(OrderType.PURCHASE);
        Map<String, Product> productByName = products.findAll().stream()
                .collect(Collectors.toMap(Product::getName, x -> x, (a, b) -> a));
        // Sales: revenue without VAT and cost of goods (returns are minus lines, so they reduce both).
        for (OrderLine l : lines.findLines(OrderType.SALE, null, from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            Figures f = out.computeIfAbsent(saleCenter.get(l.getOrderCode()), k -> new Figures());
            f.revenue = f.revenue.add(l.getValueNoVat());
            BigDecimal unit = l.getUnitCost();
            if (unit == null) {
                Product p = productByName.get(l.getProductName());
                unit = p == null ? BigDecimal.ZERO : p.getBuyPrice();
            }
            f.cogs = f.cogs.add(unit.multiply(BigDecimal.valueOf(l.getQuantity())));
        }
        for (OrderLine l : lines.findLines(OrderType.PURCHASE, null, from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            Figures f = out.computeIfAbsent(purchaseCenter.get(l.getOrderCode()), k -> new Figures());
            f.purchases = f.purchases.add(l.getValueNoVat());
        }
        // Other income and expenses entered by hand in Cash & Bank.
        for (MoneyMovement m : moves.between(null, from, to)) {
            if (m.getPaymentId() != null || m.getTransferId() != null || !m.getCategory().isManual()) {
                continue;
            }
            Figures f = out.computeIfAbsent(m.getCostCenterId(), k -> new Figures());
            if (m.isIn()) {
                f.otherIncome = f.otherIncome.add(m.getAmount());
            } else {
                f.expenses = f.expenses.add(m.getAmount());
            }
        }
        return out;
    }

    private Map<String, Long> centerByOrder(OrderType type) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (OrderInfo i : infos.findByOrderType(type)) {
            if (i.getCostCenterId() != null) {
                m.put(i.getOrderCode(), i.getCostCenterId());
            }
        }
        return m;
    }
}
