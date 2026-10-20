package com.fms.service;

import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.Product;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Figures for the Statistics page. Replaces ListOfStatistics, which drew one
 * MS Chart at a time from a drop-down:
 *   "Which client bought more"     (pie)
 *   "Which product was sold more"  (column)
 *   "Who sold more"                (funnel)
 * Those three are kept, and joined by headline figures, a trend over time,
 * revenue per product and sales per product class.
 *
 * Aggregation happens in Java over the period's lines, which avoids
 * database-specific date functions and is fast for this app's volumes.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    /** How many named slices/bars a chart shows; the rest are grouped as "Others". */
    private static final int TOP = 10;

    private final OrderLineRepository lines;
    private final ProductRepository products;
    private final com.fms.repo.OrderInfoRepository infos;
    private final org.springframework.context.MessageSource messages;

    public StatisticsService(OrderLineRepository lines, ProductRepository products,
                             com.fms.repo.OrderInfoRepository infos, org.springframework.context.MessageSource messages) {
        this.messages = messages;
        this.lines = lines;
        this.products = products;
        this.infos = infos;
    }

    /** Labels with values, ready to hand to a chart. */
    public record Series(List<String> labels, List<BigDecimal> values) {
        public boolean isEmpty() {
            return labels.isEmpty();
        }
    }

    /** Sales, purchases and offers per day or month. */
    public record Trend(String unit, List<String> labels, List<BigDecimal> sales,
                        List<BigDecimal> purchases, List<BigDecimal> offers) {
    }

    public record Kpis(
            BigDecimal salesWithVat,
            BigDecimal salesNoVat,
            BigDecimal purchasesWithVat,
            BigDecimal costOfGoodsSold,
            BigDecimal estimatedProfit,
            BigDecimal marginPercent,
            long saleOrders,
            BigDecimal averageSale,
            long itemsSold,
            long offerOrders,
            long offersConverted,
            BigDecimal conversionPercent) {
    }

    public record Stats(
            Kpis kpis,
            Trend trend,
            Series topClients,
            Series topProductsByUnits,
            Series topProductsByRevenue,
            Series sellers,
            Series byClass,
            ReturnStats returns,
            Series returnProducts,
            Series returnClients,
            Series returnReasons,
            Series merchantReturns,
            Series merchantReasons) {
    }

    /** Returns in the period: by clients (sales) and to merchants (purchases). */
    public record ReturnStats(BigDecimal clientValue, BigDecimal ratePercent, long clientUnits, long clientCount,
                              BigDecimal merchantValue, long merchantUnits, long merchantCount) {
    }

    public Stats build(Period p) {
        List<OrderLine> sales = lines.findLines(OrderType.SALE, null, p.start(), p.end());
        List<OrderLine> purchases = lines.findLines(OrderType.PURCHASE, null, p.start(), p.end());
        List<OrderLine> offers = lines.findLines(OrderType.OFFER, null, p.start(), p.end());
        Map<String, Product> productByName = products.findAll().stream()
                .collect(Collectors.toMap(Product::getName, x -> x, (a, b) -> a));

        return new Stats(
                kpis(sales, purchases, offers, productByName),
                trend(p, sales, purchases, offers),
                // "Which client bought more" (was a pie)
                topWithOthers(sum(sales, OrderLine::getBuyerName, OrderLine::getValueWithVat)),
                // "Which product was sold more" (was a column chart, by units)
                top(sum(sales, OrderLine::getProductName, l -> BigDecimal.valueOf(l.getQuantity()))),
                top(sum(sales, OrderLine::getProductName, OrderLine::getValueWithVat)),
                // "Who sold more" (was a funnel)
                top(sum(sales, OrderLine::getCreatedBy, OrderLine::getValueWithVat)),
                topWithOthers(sum(sales, l -> className(productByName.get(l.getProductName())), OrderLine::getValueWithVat)),
                returnStats(sales, purchases),
                // "Which product was returned more" (units)
                top(sum(returnsOf(sales), OrderLine::getProductName, l -> BigDecimal.valueOf(-l.getQuantity()))),
                // "Which client returned more" (value with VAT)
                top(sum(returnsOf(sales), OrderLine::getBuyerName, l -> l.getValueWithVat().negate())),
                topWithOthers(sum(returnsOf(sales), this::reasonLabel, l -> BigDecimal.valueOf(-l.getQuantity()))),
                top(sum(returnsOf(purchases), OrderLine::getProviderName, l -> l.getValueWithVat().negate())),
                topWithOthers(sum(returnsOf(purchases), this::reasonLabel, l -> BigDecimal.valueOf(-l.getQuantity()))));
    }

    // --------------------------------------------------------------- returns

    private static List<OrderLine> returnsOf(List<OrderLine> lines) {
        return lines.stream().filter(OrderLine::isReturn).toList();
    }

    private String reasonLabel(OrderLine l) {
        String key = l.getReturnReason() == null ? "OTHER" : l.getReturnReason().name();
        return messages.getMessage("reason.ret." + key, null, key,
                org.springframework.context.i18n.LocaleContextHolder.getLocale());
    }

    private ReturnStats returnStats(List<OrderLine> sales, List<OrderLine> purchases) {
        List<OrderLine> cr = returnsOf(sales);
        List<OrderLine> mr = returnsOf(purchases);
        BigDecimal clientValue = total(cr, OrderLine::getValueWithVat).negate();
        BigDecimal gross = total(sales.stream().filter(l -> !l.isReturn()).toList(), OrderLine::getValueWithVat);
        BigDecimal rate = gross.signum() == 0 ? BigDecimal.ZERO
                : clientValue.multiply(BigDecimal.valueOf(100)).divide(gross, 1, RoundingMode.HALF_UP);
        return new ReturnStats(Money.round(clientValue), rate, cr.stream().mapToLong(l -> -l.getQuantity()).sum(),
                cr.stream().map(l -> l.getOrderCode() + "#" + l.getReturnNo()).distinct().count(),
                Money.round(total(mr, OrderLine::getValueWithVat).negate()), mr.stream().mapToLong(l -> -l.getQuantity()).sum(),
                mr.stream().map(l -> l.getOrderCode() + "#" + l.getReturnNo()).distinct().count());
    }

    // ------------------------------------------------------------------ KPIs

    private Kpis kpis(List<OrderLine> sales, List<OrderLine> purchases, List<OrderLine> offers,
                      Map<String, Product> productByName) {
        BigDecimal salesWithVat = total(sales, OrderLine::getValueWithVat);
        BigDecimal salesNoVat = total(sales, OrderLine::getValueNoVat);
        BigDecimal purchasesWithVat = total(purchases, OrderLine::getValueWithVat);

        // Cost of the goods sold: the average purchase cost saved on each sale
        // line; older lines (before costs were saved) use the current buy price.
        BigDecimal cogs = sales.stream()
                .map(l -> {
                    BigDecimal unit = l.getUnitCost();
                    if (unit == null) {
                        Product prod = productByName.get(l.getProductName());
                        unit = prod == null ? BigDecimal.ZERO : prod.getBuyPrice();
                    }
                    return unit.multiply(BigDecimal.valueOf(l.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = salesNoVat.subtract(cogs);
        BigDecimal margin = salesNoVat.signum() == 0 ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(salesNoVat, 1, RoundingMode.HALF_UP);

        long saleOrders = sales.stream().map(OrderLine::getOrderCode).distinct().count();
        BigDecimal average = saleOrders == 0 ? BigDecimal.ZERO
                : salesWithVat.divide(BigDecimal.valueOf(saleOrders), 2, RoundingMode.HALF_UP);
        long items = sales.stream().mapToLong(OrderLine::getQuantity).sum();
        List<String> offerCodes = offers.stream().map(OrderLine::getOrderCode).distinct().toList();
        long offerOrders = offerCodes.size();
        long converted = offerCodes.isEmpty() ? 0 : infos.findByOrderTypeAndOrderCodeIn(OrderType.OFFER, offerCodes).stream()
                .filter(i -> i.getConvertedTo() != null).count();
        BigDecimal conversion = offerOrders == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(converted * 100).divide(BigDecimal.valueOf(offerOrders), 1, RoundingMode.HALF_UP);

        return new Kpis(Money.round(salesWithVat), Money.round(salesNoVat), Money.round(purchasesWithVat),
                Money.round(cogs), Money.round(profit), margin, saleOrders, average, items, offerOrders,
                converted, conversion);
    }

    // ----------------------------------------------------------------- trend

    /** Per day for periods up to two months, otherwise per month. */
    private Trend trend(Period p, List<OrderLine> sales, List<OrderLine> purchases, List<OrderLine> offers) {
        boolean daily = ChronoUnit.DAYS.between(p.from(), p.to()) <= 62;
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (daily) {
            DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM");
            for (LocalDate d = p.from(); !d.isAfter(p.to()); d = d.plusDays(1)) {
                keys.add(d.toString());
                labels.add(d.format(label));
            }
        } else {
            DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yyyy");
            for (YearMonth m = YearMonth.from(p.from()); !m.isAfter(YearMonth.from(p.to())); m = m.plusMonths(1)) {
                keys.add(m.toString());
                labels.add(m.format(label));
            }
        }
        Function<OrderLine, String> keyOf = daily
                ? l -> l.getCreatedAt().toLocalDate().toString()
                : l -> YearMonth.from(l.getCreatedAt()).toString();
        return new Trend(daily ? "day" : "month", labels,
                bucket(keys, sales, keyOf), bucket(keys, purchases, keyOf), bucket(keys, offers, keyOf));
    }

    private static List<BigDecimal> bucket(List<String> keys, List<OrderLine> data, Function<OrderLine, String> keyOf) {
        Map<String, BigDecimal> sums = sum(data, keyOf, OrderLine::getValueWithVat);
        return keys.stream().map(k -> Money.round(sums.getOrDefault(k, BigDecimal.ZERO))).toList();
    }

    // --------------------------------------------------------------- helpers

    private static String className(Product p) {
        return p == null || p.getProductClass() == null ? "No class" : p.getProductClass().getName();
    }

    private static BigDecimal total(List<OrderLine> data, Function<OrderLine, BigDecimal> value) {
        return data.stream().map(value).map(Money::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Map<String, BigDecimal> sum(List<OrderLine> data, Function<OrderLine, String> key,
                                               Function<OrderLine, BigDecimal> value) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (OrderLine l : data) {
            out.merge(key.apply(l), Money.nz(value.apply(l)), BigDecimal::add);
        }
        return out;
    }

    private static List<Map.Entry<String, BigDecimal>> sortedDesc(Map<String, BigDecimal> m) {
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .toList();
    }

    /** The largest {@value #TOP} entries. */
    private static Series top(Map<String, BigDecimal> m) {
        List<Map.Entry<String, BigDecimal>> sorted = sortedDesc(m);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (var e : sorted.subList(0, Math.min(TOP, sorted.size()))) {
            labels.add(e.getKey());
            values.add(Money.round(e.getValue()));
        }
        return new Series(labels, values);
    }

    /** Like {@link #top} but the remainder is added as one "Others" slice, so a pie still adds up to 100%. */
    private static Series topWithOthers(Map<String, BigDecimal> m) {
        List<Map.Entry<String, BigDecimal>> sorted = sortedDesc(m);
        Series head = top(m);
        if (sorted.size() <= TOP) {
            return head;
        }
        BigDecimal rest = sorted.subList(TOP, sorted.size()).stream()
                .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> labels = new ArrayList<>(head.labels());
        List<BigDecimal> values = new ArrayList<>(head.values());
        labels.add("Others (" + (sorted.size() - TOP) + ")");
        values.add(Money.round(rest));
        return new Series(labels, values);
    }
}
