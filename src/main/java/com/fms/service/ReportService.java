package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.Product;
import com.fms.repo.NameCount;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.ProductRepository;
import com.fms.repo.Totals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report figures. Replaces the Today()/Month()/Yearly()/Custom()
 * methods of PurchaseReports and SaleReports, plus ProductReports
 * and WarehouseReport.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderLineRepository lines;
    private final ProductRepository products;
    private final com.fms.repo.StockAdjustmentRepository adjustments;

    public ReportService(OrderLineRepository lines, ProductRepository products,
                         com.fms.repo.StockAdjustmentRepository adjustments) {
        this.lines = lines;
        this.products = products;
        this.adjustments = adjustments;
    }

    private static String scope(AppUser user) {
        return user.isSeesAll() ? null : user.getUsername();
    }

    public Totals totals(OrderType type, AppUser user, Period p) {
        Totals t = lines.totals(type, scope(user), p.start(), p.end());
        return t == null ? Totals.empty() : t.orZero();
    }

    public List<OrderLine> lines(OrderType type, AppUser user, Period p) {
        return lines.findLines(type, scope(user), p.start(), p.end());
    }

    // ------------------------------------------------------------ products

    public record ProductReport(List<OrderLine> sales, List<OrderLine> purchases,
                                long sold, long bought, long remaining) {
    }

    /** Sales and purchases of the products whose name contains the search text. */
    public ProductReport productReport(String search, AppUser user) {
        String like = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        List<OrderLine> sales = lines.searchLines(OrderType.SALE, scope(user), like);
        List<OrderLine> purchases = lines.searchLines(OrderType.PURCHASE, scope(user), like);
        long sold = sales.stream().mapToLong(OrderLine::getQuantity).sum();
        long bought = purchases.stream().mapToLong(OrderLine::getQuantity).sum();
        return new ProductReport(sales, purchases, sold, bought, bought - sold);
    }

    // ----------------------------------------------------------- warehouse

    /** Default start date: the first day anything was recorded. */
    public LocalDate firstActivityDate() {
        var first = lines.firstActivity();
        return first == null ? LocalDate.now() : first.toLocalDate();
    }

    /**
     * Per product: quantity purchased, sold and adjusted (returns, damage,
     * stock counts) in the period, what remains,
     * and its value at the current sale price. One grouped query per side
     * instead of the original's two queries per product.
     */
    public List<WarehouseRow> warehouse(Period p) {
        Map<String, Long> bought = toMap(lines.quantityByProduct(OrderType.PURCHASE, p.start(), p.end()));
        Map<String, Long> sold = toMap(lines.quantityByProduct(OrderType.SALE, p.start(), p.end()));
        Map<String, Long> adjusted = toMap(adjustments.quantityByProduct(p.start(), p.end()));
        List<WarehouseRow> rows = new ArrayList<>();
        int nr = 1;
        for (Product prod : products.findAllByOrderByNameAsc()) {
            long b = bought.getOrDefault(prod.getName(), 0L);
            long s = sold.getOrDefault(prod.getName(), 0L);
            long a = adjusted.getOrDefault(prod.getName(), 0L);
            long remaining = b - s + a;
            BigDecimal value = Money.round(prod.getSalePrice().multiply(BigDecimal.valueOf(remaining)));
            rows.add(new WarehouseRow(nr++, prod.getName(), prod.getUnit(), b, s, a, remaining,
                    prod.getQuantityLimit(), prod.getSalePrice(), value));
        }
        return rows;
    }

    private static Map<String, Long> toMap(List<NameCount> list) {
        return list.stream().collect(Collectors.toMap(NameCount::name,
                c -> c.total() == null ? 0L : c.total(), Long::sum));
    }
}
